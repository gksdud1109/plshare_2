"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useSessionUser } from "@/lib/auth/useSessionUser";
import {
  composeAsset,
  composeMoodVideo,
  getCatalog,
  searchYouTubeCatalog,
} from "@/lib/api/catalog";
import { ApiError, makeIdempotencyKey } from "@/lib/api/client";
import { messageFromError } from "@/lib/errors";
import { useToast } from "@/components/ui/ToastProvider";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { TrackEmbed } from "@/components/gift/TrackEmbed";
import {
  MOOD_LABELS,
  MOOD_ORDER,
  type CatalogTrack,
  type ComposedAsset,
  type YouTubeCatalogSearchResult,
} from "@/types/catalog";

function formatDuration(ms?: number | null): string {
  if (!ms) return "";
  const total = Math.floor(ms / 1000);
  return `${Math.floor(total / 60)}:${(total % 60).toString().padStart(2, "0")}`;
}

/** YouTube URL/raw id 에서 11자 videoId 추출(임베드 프리뷰용). */
function extractVideoId(input: string): string | null {
  const s = input.trim();
  const pats = [
    /[?&]v=([A-Za-z0-9_-]{11})/,
    /youtu\.be\/([A-Za-z0-9_-]{11})/,
    /\/embed\/([A-Za-z0-9_-]{11})/,
    /\/shorts\/([A-Za-z0-9_-]{11})/,
  ];
  for (const p of pats) {
    const m = s.match(p);
    if (m) return m[1];
  }
  return /^[A-Za-z0-9_-]{11}$/.test(s) ? s : null;
}

type Format = "tracklist" | "video";
type SearchStatus = "idle" | "loading" | "success" | "error";
type SearchErrorKind = "quota" | "configuration" | "upstream";

interface SelectedTrack {
  selectionId: string;
  title: string;
  artist: string;
  videoId: string;
  thumbnailUrl?: string | null;
  durationMs?: number | null;
}

function fromCatalogTrack(track: CatalogTrack): SelectedTrack {
  return {
    selectionId: track.id,
    title: track.title,
    artist: track.artist,
    videoId: track.youtubeVideoId,
    thumbnailUrl: track.coverUrl,
    durationMs: track.durationMs,
  };
}

function fromSearchResult(result: YouTubeCatalogSearchResult): SelectedTrack {
  return {
    selectionId: result.selectionId,
    title: result.title,
    artist: result.channelTitle?.trim() || "YouTube",
    videoId: result.videoId,
    thumbnailUrl: result.thumbnailUrl,
  };
}

function searchErrorKind(error: unknown): SearchErrorKind {
  if (error instanceof ApiError) {
    const code = (error.body as { code?: string } | undefined)?.code;
    if (code === "QUOTA_EXCEEDED" || error.status === 429) return "quota";
    if (code === "CONFIGURATION_ERROR") return "configuration";
  }
  return "upstream";
}

function TrackChoiceCard({
  track,
  selected,
  previewing,
  onToggle,
  onTogglePreview,
}: {
  track: SelectedTrack;
  selected: boolean;
  previewing: boolean;
  onToggle: () => void;
  onTogglePreview: () => void;
}) {
  return (
    <div
      className="overflow-hidden rounded-[14px] border bg-surface-1"
      style={{ borderColor: selected ? "var(--color-accent)" : "var(--color-hairline)" }}
    >
      <div className="flex items-center gap-3 p-3">
        <button
          type="button"
          onClick={onTogglePreview}
          aria-label={previewing ? `${track.title} 미리듣기 닫기` : `${track.title} 미리듣기`}
          className="relative h-14 w-14 shrink-0 overflow-hidden rounded-[10px] bg-surface-2 focus-ring"
        >
          {track.thumbnailUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={track.thumbnailUrl}
              alt=""
              className="h-full w-full object-cover"
            />
          ) : (
            <span className="flex h-full w-full items-center justify-center text-text-low">♪</span>
          )}
          <span className="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 transition-opacity hover:opacity-100">
            <svg width="18" height="18" viewBox="0 0 16 16" fill="#fff" aria-hidden>
              {previewing ? (
                <>
                  <rect x="3" y="2" width="3.5" height="12" rx="1" />
                  <rect x="9.5" y="2" width="3.5" height="12" rx="1" />
                </>
              ) : (
                <path d="M4 3.5v9a.5.5 0 00.76.43l7.5-4.5a.5.5 0 000-.86l-7.5-4.5A.5.5 0 004 3.5z" />
              )}
            </svg>
          </span>
        </button>
        <div className="min-w-0 flex-1">
          <p className="truncate text-[0.9375rem] font-medium text-text-hi">{track.title}</p>
          <p className="mt-0.5 truncate text-xs text-text-mid">
            {track.artist}
            {track.durationMs ? ` · ${formatDuration(track.durationMs)}` : ""}
          </p>
        </div>
        <button
          type="button"
          onClick={onToggle}
          aria-pressed={selected}
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border transition-colors focus-ring"
          style={{
            borderColor: selected ? "var(--color-accent)" : "var(--color-hairline-strong)",
            background: selected ? "var(--color-accent)" : "transparent",
            color: selected ? "#fff" : "var(--color-text-mid)",
          }}
          aria-label={selected ? `${track.title} 선택 해제` : `${track.title} 선택`}
        >
          {selected ? "✓" : "+"}
        </button>
      </div>
      {previewing && <TrackEmbed videoId={track.videoId} title={track.title} />}
    </div>
  );
}

export default function CreatePage() {
  const session = useSessionUser();
  const toast = useToast();

  const [format, setFormat] = useState<Format>("tracklist");
  const [title, setTitle] = useState("");
  const [composing, setComposing] = useState(false);
  const [created, setCreated] = useState<ComposedAsset | null>(null);
  const trackComposeKey = useRef<string | null>(null);
  const videoComposeKey = useRef<string | null>(null);

  // tracklist 모드
  const [tracks, setTracks] = useState<CatalogTrack[]>([]);
  const [loading, setLoading] = useState(true);
  const [mood, setMood] = useState<string>("all");
  const [selected, setSelected] = useState<SelectedTrack[]>([]);
  const [previewId, setPreviewId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchedQuery, setSearchedQuery] = useState("");
  const [searchResults, setSearchResults] = useState<YouTubeCatalogSearchResult[]>([]);
  const [searchStatus, setSearchStatus] = useState<SearchStatus>("idle");
  const [searchError, setSearchError] = useState<SearchErrorKind | null>(null);

  // video 모드
  const [videoUrl, setVideoUrl] = useState("");
  const [channelName, setChannelName] = useState("");
  const [trackListText, setTrackListText] = useState("");

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const data = await getCatalog();
        if (!cancelled) setTracks(data);
      } catch (err) {
        if (!cancelled) toast.error(messageFromError(err, "카탈로그를 불러오지 못했어요."));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [toast]);

  const visible = useMemo(
    () => (mood === "all" ? tracks : tracks.filter((t) => t.mood === mood)),
    [tracks, mood],
  );
  const selectedIds = useMemo(
    () => new Set(selected.map((track) => track.selectionId)),
    [selected],
  );
  const videoPreviewId = useMemo(() => extractVideoId(videoUrl), [videoUrl]);

  function toggle(track: SelectedTrack) {
    if (!selectedIds.has(track.selectionId) && selected.length >= 30) {
      toast.error("트랙은 최대 30곡까지 담을 수 있어요.");
      return;
    }
    trackComposeKey.current = null;
    setSelected((current) => {
      if (current.some((item) => item.selectionId === track.selectionId)) {
        return current.filter((item) => item.selectionId !== track.selectionId);
      }
      return [...current, track];
    });
  }

  async function handleSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (searchStatus === "loading") return;

    const query = searchQuery.trim();
    if (!query) {
      setSearchedQuery("");
      setSearchResults([]);
      setSearchError(null);
      setSearchStatus("idle");
      return;
    }

    setSearchedQuery(query);
    setSearchStatus("loading");
    setSearchError(null);
    try {
      const results = await searchYouTubeCatalog(query);
      setSearchResults(results);
      setSearchStatus("success");
    } catch (error) {
      setSearchResults([]);
      setSearchError(searchErrorKind(error));
      setSearchStatus("error");
    }
  }

  async function handleCompose() {
    if (selected.length === 0) {
      toast.error("곡을 한 곡 이상 선택해주세요.");
      return;
    }
    const finalTitle = title.trim() || "내 플레이리스트";
    setComposing(true);
    try {
      const key =
        trackComposeKey.current ??
        makeIdempotencyKey("compose-tracklist");
      trackComposeKey.current = key;
      const asset = await composeAsset(
        {
          title: finalTitle,
          selectionIds: selected.map((track) => track.selectionId),
        },
        key,
      );
      toast.success("플레이리스트를 만들었어요");
      trackComposeKey.current = null;
      setCreated(asset);
      setComposing(false);
    } catch (err) {
      toast.error(messageFromError(err, "플레이리스트 생성에 실패했어요"));
      setComposing(false);
    }
  }

  async function handleComposeVideo() {
    if (!videoUrl.trim()) {
      toast.error("YouTube 영상 URL을 입력해주세요.");
      return;
    }
    if (!videoPreviewId) {
      toast.error("유효한 YouTube 영상 URL이 아니에요.");
      return;
    }
    const finalTitle = title.trim() || "무드영상";
    setComposing(true);
    try {
      const key =
        videoComposeKey.current ??
        makeIdempotencyKey("compose-mood-video");
      videoComposeKey.current = key;
      const asset = await composeMoodVideo(
        {
          title: finalTitle,
          videoUrlOrId: videoUrl.trim(),
          channelName: channelName.trim() || undefined,
          trackListText: trackListText.trim() || undefined,
        },
        key,
      );
      toast.success("무드영상을 담았어요");
      videoComposeKey.current = null;
      setCreated(asset);
      setComposing(false);
    } catch (err) {
      toast.error(messageFromError(err, "무드영상 생성에 실패했어요"));
      setComposing(false);
    }
  }

  if (session.status === "unauthenticated") {
    return (
      <PageShell>
        <div className="flex flex-col items-center gap-4 py-32 text-center">
          <p className="text-xl font-semibold text-text-hi">로그인이 필요해요</p>
          <p className="text-sm text-text-low">플레이리스트를 만들려면 먼저 로그인해주세요.</p>
          <a
            href="/auth/continue?returnTo=%2Fcreate"
            className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white"
            style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
          >
            로그인하기
          </a>
        </div>
      </PageShell>
    );
  }

  if (created) {
    return (
      <PageShell>
        <section className="mx-auto flex min-h-[70vh] max-w-xl flex-col items-center justify-center gap-6 py-20 text-center">
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-accent">
            Playlist ready
          </p>
          <h1 className="font-display text-3xl font-bold text-text-hi">
            {created.title}
          </h1>
          <p className="text-sm leading-relaxed text-text-mid">
            플레이리스트가 라이브러리에 저장됐어요. 지금 선물하거나 상세 내용을 확인할 수 있어요.
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            <Link
              href={`/gift/send?assetId=${created.id}`}
              className="inline-flex h-12 items-center rounded-full bg-accent px-6 text-sm font-semibold text-white"
            >
              선물하기
            </Link>
            <Link
              href={`/assets/${created.id}`}
              className="glass inline-flex h-12 items-center rounded-full px-6 text-sm font-semibold text-text-hi"
            >
              내 플레이리스트 보기
            </Link>
            <button
              type="button"
              onClick={() => {
                setCreated(null);
                setTitle("");
                setSelected([]);
                setVideoUrl("");
                setChannelName("");
                setTrackListText("");
              }}
              className="inline-flex h-12 items-center rounded-full px-5 text-sm font-semibold text-text-mid hover:text-text-hi"
            >
              계속 만들기
            </button>
          </div>
        </section>
      </PageShell>
    );
  }

  const canSubmit = format === "video" ? !!videoUrl.trim() : selected.length > 0;

  return (
    <PageShell>
      <header className="py-8">
        <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
          Create
        </p>
        <h1
          className="mt-3 font-display text-text-hi"
          style={{ fontSize: "clamp(1.75rem, 4vw, 2.5rem)", fontWeight: 700, letterSpacing: "-0.02em" }}
        >
          만들기
        </h1>
        <p className="mt-2 text-sm text-text-low">
          큐레이션 곡을 골라 담거나, 감성 플레이리스트 영상 하나를 통째로 — 받는 사람은 계정 없이 바로 들어요.
        </p>
      </header>

      {/* 포맷 토글 */}
      <div className="mb-6 inline-flex gap-1 rounded-full border border-hairline bg-surface-1 p-1">
        {(
          [
            ["tracklist", "클래식 플레이리스트"],
            ["video", "무드영상"],
          ] as const
        ).map(([f, label]) => (
          <button
            key={f}
            type="button"
            onClick={() => setFormat(f)}
            className="rounded-full px-5 py-2 text-sm font-semibold transition-colors duration-200 focus-ring"
            style={{
              background: format === f ? "var(--color-accent)" : "transparent",
              color: format === f ? "#fff" : "var(--color-text-mid)",
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {format === "video" ? (
        /* ── 무드영상 모드 ── */
        <div className="flex flex-col gap-6 pb-40">
          <p className="text-sm text-text-low">
            후알유 같은 감성 플레이리스트 영상의 링크를 붙여넣으면, 끊김 없는 그 영상을 통째로 선물할 수 있어요.
          </p>
          <div>
            <label className="mb-2 block text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
              YouTube 영상 URL
            </label>
            <input
              type="text"
              value={videoUrl}
              onChange={(e) => {
                videoComposeKey.current = null;
                setVideoUrl(e.target.value);
              }}
              placeholder="https://www.youtube.com/watch?v=…"
              className="w-full rounded-xl border border-hairline bg-surface-1 px-4 py-3 text-sm text-text-hi placeholder:text-text-low outline-none focus:border-accent"
            />
            {videoPreviewId && (
              <div
                className="mt-3 overflow-hidden rounded-[14px] border border-hairline"
                style={{ aspectRatio: "16 / 9" }}
              >
                <iframe
                  title="미리보기"
                  src={`https://www.youtube.com/embed/${videoPreviewId}?rel=0&playsinline=1`}
                  allow="encrypted-media; picture-in-picture"
                  allowFullScreen
                  className="h-full w-full"
                  style={{ border: 0 }}
                />
              </div>
            )}
          </div>
          <div>
            <label className="mb-2 block text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
              채널명 (선택)
            </label>
            <input
              type="text"
              value={channelName}
              onChange={(e) => {
                videoComposeKey.current = null;
                setChannelName(e.target.value.slice(0, 120));
              }}
              placeholder="후알유"
              className="w-full rounded-xl border border-hairline bg-surface-1 px-4 py-3 text-sm text-text-hi placeholder:text-text-low outline-none focus:border-accent"
            />
          </div>
          <div>
            <label className="mb-2 block text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
              수록곡 (선택)
            </label>
            <textarea
              value={trackListText}
              onChange={(e) => {
                videoComposeKey.current = null;
                setTrackListText(e.target.value);
              }}
              rows={4}
              placeholder={"00:00 마틴 스미스 - 봄 그리고 너\n03:21 ..."}
              className="w-full resize-none rounded-xl border border-hairline bg-surface-1 px-4 py-3 text-sm text-text-hi placeholder:text-text-low outline-none focus:border-accent"
            />
          </div>
        </div>
      ) : loading ? (
        <div className="py-24">
          <ProgressNarrative messages={["곡을 불러오는 중이에요…"]} />
        </div>
      ) : (
        /* ── 클래식 플레이리스트 모드 ── */
        <>
          {selected.length > 0 && (
            <section className="mb-10 rounded-[14px] border border-hairline bg-surface-1 p-4">
              <div className="mb-3 flex items-center justify-between gap-3">
                <h2 className="text-sm font-semibold text-text-hi">선택한 곡 순서</h2>
                <span className="text-xs text-text-low">{selected.length}/30곡</span>
              </div>
              <ol className="space-y-2">
                {selected.map((track, index) => (
                  <li key={track.selectionId} className="flex items-center gap-3">
                    <span className="w-5 shrink-0 text-right text-xs tabular-nums text-text-low">
                      {index + 1}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm text-text-hi">{track.title}</p>
                      <p className="truncate text-xs text-text-low">{track.artist}</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => toggle(track)}
                      className="shrink-0 rounded-full px-2 py-1 text-xs text-text-mid hover:text-danger focus-ring"
                      aria-label={`${track.title} 선택 해제`}
                    >
                      제거
                    </button>
                  </li>
                ))}
              </ol>
            </section>
          )}

          <section className="mb-10">
            <div className="mb-3">
              <h2 className="text-base font-semibold text-text-hi">YouTube에서 곡 찾기</h2>
              <p className="mt-1 text-xs text-text-low">
                검색 버튼을 눌렀을 때만 YouTube 검색을 사용해요.
              </p>
            </div>
            <form onSubmit={handleSearch} className="flex gap-2">
              <label htmlFor="youtube-catalog-search" className="sr-only">
                곡, 아티스트 또는 영상 검색
              </label>
              <input
                id="youtube-catalog-search"
                type="search"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value.slice(0, 120))}
                maxLength={120}
                placeholder="곡, 아티스트 또는 영상 검색"
                className="min-w-0 flex-1 rounded-xl border border-hairline bg-surface-1 px-4 py-3 text-sm text-text-hi placeholder:text-text-low outline-none focus:border-accent"
              />
              <button
                type="submit"
                disabled={searchStatus === "loading" || !searchQuery.trim()}
                className="shrink-0 rounded-xl bg-surface-3 px-5 py-3 text-sm font-semibold text-text-hi transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
              >
                {searchStatus === "loading" ? "검색 중…" : "검색"}
              </button>
            </form>

            <div className="mt-4" aria-live="polite">
              {searchStatus === "loading" && (
                <p className="text-sm text-text-mid">
                  &ldquo;{searchedQuery}&rdquo; 검색 결과를 불러오는 중이에요…
                </p>
              )}

              {searchStatus === "success" && searchResults.length === 0 && (
                <div className="rounded-[14px] border border-hairline bg-surface-1 px-4 py-5">
                  <p className="text-sm font-medium text-text-hi">검색 결과가 없어요.</p>
                  <p className="mt-1 text-xs text-text-low">
                    다른 곡명이나 아티스트 이름으로 검색해 보세요.
                  </p>
                </div>
              )}

              {searchStatus === "error" && searchError === "quota" && (
                <div
                  role="alert"
                  className="rounded-[14px] border border-warning/30 bg-warning/10 px-4 py-4"
                >
                  <p className="text-sm font-medium text-warning">
                    오늘 사용할 수 있는 YouTube 검색 한도를 모두 사용했어요.
                  </p>
                  <p className="mt-1 text-xs text-text-mid">
                    내일 다시 검색하거나 아래 큐레이션 곡을 선택해 주세요.
                  </p>
                </div>
              )}

              {searchStatus === "error" && searchError === "configuration" && (
                <div
                  role="alert"
                  className="rounded-[14px] border border-warning/30 bg-warning/10 px-4 py-4"
                >
                  <p className="text-sm font-medium text-warning">
                    YouTube 검색 설정이 아직 완료되지 않았어요.
                  </p>
                  <p className="mt-1 text-xs text-text-mid">
                    아래 큐레이션 곡은 계속 선택할 수 있어요.
                  </p>
                </div>
              )}

              {searchStatus === "error" && searchError === "upstream" && (
                <div
                  role="alert"
                  className="rounded-[14px] border border-danger/30 bg-danger/10 px-4 py-4"
                >
                  <p className="text-sm font-medium text-danger">
                    YouTube 검색 서비스에 연결하지 못했어요.
                  </p>
                  <p className="mt-1 text-xs text-text-mid">
                    잠시 후 검색 버튼을 다시 눌러 주세요.
                  </p>
                </div>
              )}

              {searchStatus === "success" && searchResults.length > 0 && (
                <>
                  <p className="mb-2 text-xs text-text-low">
                    &ldquo;{searchedQuery}&rdquo; 검색 결과 {searchResults.length}개
                  </p>
                  <div className="grid grid-cols-1 gap-2">
                    {searchResults.map((result) => {
                      const track = fromSearchResult(result);
                      return (
                        <TrackChoiceCard
                          key={result.selectionId}
                          track={track}
                          selected={selectedIds.has(result.selectionId)}
                          previewing={previewId === result.selectionId}
                          onToggle={() => toggle(track)}
                          onTogglePreview={() =>
                            setPreviewId((current) =>
                              current === result.selectionId ? null : result.selectionId,
                            )
                          }
                        />
                      );
                    })}
                  </div>
                </>
              )}
            </div>
          </section>

          <section>
            <div className="mb-4">
              <h2 className="text-base font-semibold text-text-hi">큐레이션 곡</h2>
              <p className="mt-1 text-xs text-text-low">
                검색 결과와 함께 골라도 선택한 순서대로 담겨요.
              </p>
            </div>

            <div className="mb-6 flex flex-wrap gap-2">
              {["all", ...MOOD_ORDER].map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => setMood(m)}
                  className="rounded-full border px-4 py-1.5 text-xs font-semibold transition-colors duration-200 focus-ring"
                  style={{
                    borderColor: mood === m ? "var(--color-accent)" : "var(--color-hairline)",
                    background: mood === m ? "var(--color-accent)" : "transparent",
                    color: mood === m ? "#fff" : "var(--color-text-mid)",
                  }}
                >
                  {m === "all" ? "전체" : MOOD_LABELS[m] ?? m}
                </button>
              ))}
            </div>

            <div className="grid grid-cols-1 gap-2 pb-40">
              {visible.map((catalogTrack) => {
                const track = fromCatalogTrack(catalogTrack);
                return (
                  <TrackChoiceCard
                    key={catalogTrack.id}
                    track={track}
                    selected={selectedIds.has(catalogTrack.id)}
                    previewing={previewId === catalogTrack.id}
                    onToggle={() => toggle(track)}
                    onTogglePreview={() =>
                      setPreviewId((current) =>
                        current === catalogTrack.id ? null : catalogTrack.id,
                      )
                    }
                  />
                );
              })}
            </div>
          </section>
        </>
      )}

      {/* Sticky compose bar */}
      <div className="fixed inset-x-0 bottom-0 z-30 border-t border-hairline bg-bg/90 backdrop-blur">
        <div className="mx-auto flex w-full max-w-3xl items-center gap-3 px-6 py-3">
          <input
            type="text"
            value={title}
            onChange={(e) => {
              trackComposeKey.current = null;
              videoComposeKey.current = null;
              setTitle(e.target.value.slice(0, 100));
            }}
            placeholder={format === "video" ? "무드영상 이름" : "플레이리스트 이름"}
            className="min-w-0 flex-1 rounded-xl border border-hairline bg-surface-1 px-4 py-2.5 text-sm text-text-hi placeholder:text-text-low outline-none focus:border-accent"
          />
          <button
            type="button"
            onClick={format === "video" ? handleComposeVideo : handleCompose}
            disabled={composing || !canSubmit}
            className="shrink-0 rounded-full bg-accent px-6 py-2.5 text-sm font-semibold text-white transition-all hover:bg-accent-hi disabled:opacity-50"
          >
            {composing
              ? format === "video"
                ? "담는 중…"
                : "만드는 중…"
              : format === "video"
                ? "무드영상 저장"
                : `${selected.length}곡 플레이리스트 만들기`}
          </button>
        </div>
        {format === "tracklist" && selected.length > 0 && (
          <div className="mx-auto -mt-1 w-full max-w-3xl px-6 pb-2">
            <p className="truncate text-[0.6875rem] text-text-low">
              {selected.map((track) => track.title).join(" · ")}
            </p>
          </div>
        )}
      </div>
    </PageShell>
  );
}
