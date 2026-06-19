"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  createShareLink,
  deleteAsset,
  getAsset,
  getPublicAsset,
  updateAsset,
} from "@/lib/api/assets";
import { ApiError } from "@/lib/api/client";
import { buildDemoAssetDetail } from "@/lib/api/fixtures";
import type { AssetDetail, PublicAssetDetail } from "@/types/asset";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { ShareTrackList } from "@/components/share/ShareTrackList";
import { EmotionTagPicker } from "@/components/ui/EmotionTagPicker";
import { useToast } from "@/components/ui/ToastProvider";
import { demoFixturesEnabled } from "@/lib/demo";
import { toAbsoluteUrl } from "@/lib/url";
import { MoodVideoPlayer } from "@/components/media/MoodVideoPlayer";

type State =
  | { kind: "loading" }
  | {
      kind: "ready";
      data: AssetDetail | PublicAssetDetail;
      usingFixture: boolean;
      readOnly: boolean;
    }
  | { kind: "error"; message: string };

export default function AssetDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const router = useRouter();
  const [state, setState] = useState<State>({ kind: "loading" });
  const [diary, setDiary] = useState("");
  const [description, setDescription] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const toast = useToast();

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getAsset(id);
        if (cancelled) return;
        setState({ kind: "ready", data, usingFixture: false, readOnly: false });
        setDiary(data.diaryText ?? "");
        setDescription(data.description ?? "");
        setTags(data.emotionTags ?? []);
      } catch (error) {
        if (cancelled) return;
        if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
          try {
            const data = await getPublicAsset(id);
            if (cancelled) return;
            setState({ kind: "ready", data, usingFixture: false, readOnly: true });
            setDescription(data.description ?? "");
            setTags(data.emotionTags ?? []);
            return;
          } catch {
            // Fall through to the normal unavailable state.
          }
        }
        if (demoFixturesEnabled()) {
          const fallback = buildDemoAssetDetail(id);
          setState({ kind: "ready", data: fallback, usingFixture: true, readOnly: false });
          setDiary(fallback.diaryText ?? "");
          setDescription(fallback.description ?? "");
          setTags(fallback.emotionTags ?? []);
        } else {
          setState({ kind: "error", message: "플레이리스트를 불러오지 못했어요." });
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const persist = async (
    patch: Parameters<typeof updateAsset>[1],
  ) => {
    if (state.kind !== "ready" || state.readOnly) return;
    setSaving(true);
    try {
      if (!state.usingFixture) {
        const updated = await updateAsset(id, patch);
        setState({ kind: "ready", data: updated, usingFixture: false, readOnly: false });
      }
      setSavedAt(Date.now());
    } catch {
      toast.error("저장에 실패했어요.");
    } finally {
      setSaving(false);
    }
  };

  const handleShare = async () => {
    try {
      let url: string;
      if (state.kind === "ready" && !state.usingFixture) {
        const link = await createShareLink(id);
        url = toAbsoluteUrl(link.url);
      } else {
        // Fixture share — use a demo token.
        url = toAbsoluteUrl(`/share/${id}`);
      }
      await navigator.clipboard.writeText(url);
      toast.success("공유 링크를 복사했어요.");
    } catch {
      toast.error("공유 링크를 만들지 못했어요.");
    }
  };

  const handleDelete = async () => {
    if (state.kind !== "ready" || state.readOnly) return;
    if (!window.confirm("이 플레이리스트를 삭제할까요? 이 작업은 되돌릴 수 없어요.")) return;
    setDeleting(true);
    try {
      if (!state.usingFixture) await deleteAsset(id);
      toast.success("플레이리스트를 삭제했어요.");
      router.push("/assets");
    } catch {
      toast.error("선물에 사용 중이거나 삭제할 수 없는 플레이리스트예요.");
      setDeleting(false);
    }
  };

  if (state.kind === "loading") {
    return (
      <PageShell>
        <div className="py-32">
          <ProgressNarrative messages={["플레이리스트를 불러오는 중이에요…"]} />
        </div>
      </PageShell>
    );
  }

  if (state.kind === "error") {
    return (
      <PageShell>
        <p className="py-16 text-base text-danger">{state.message}</p>
      </PageShell>
    );
  }

  const a = state.data;

  return (
    <PageShell>
      {state.usingFixture ? (
        <p className="mb-6 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
          Demo data · 백엔드 연결 전
        </p>
      ) : null}

      {/* Hero: cover + meta */}
      <section className="grid grid-cols-1 gap-10 py-8 md:grid-cols-[minmax(0,360px)_1fr] md:gap-14">
        {/* Cover with ambient glow */}
        <div className="relative">
          {/* Ambient glow behind cover */}
          {a.coverUrl && (
            <div
              className="cover-glow absolute inset-0 animate-pulse-glow rounded-[var(--radius-image)]"
              style={{
                backgroundImage: `url(${a.coverUrl})`,
                backgroundSize: "cover",
                backgroundPosition: "center",
                zIndex: 0,
              }}
              aria-hidden="true"
            />
          )}
          <div
            className="relative aspect-square w-full overflow-hidden bg-surface-2"
            style={{
              borderRadius: "var(--radius-image)",
              boxShadow: "var(--shadow-pop)",
              zIndex: 1,
            }}
          >
            {a.coverUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={a.coverUrl}
                alt={a.title}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center">
                <span className="text-4xl opacity-20">♪</span>
              </div>
            )}
          </div>
        </div>

        {/* Meta + actions */}
        <div className="flex flex-col justify-center">
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            {a.assetKind === "MOOD_VIDEO" ? "Mood video" : "Playlist"}
          </p>
          <h1
            className="mt-3 font-display text-text-hi"
            style={{ fontSize: "clamp(2rem, 4vw, 3rem)", fontWeight: 700, letterSpacing: "-0.02em" }}
          >
            {a.title}
          </h1>
          <p className="mt-3 text-sm text-text-mid">
            {a.assetKind === "MOOD_VIDEO"
              ? "YouTube 무드영상"
              : `${a.tracks.length}곡`}
          </p>
          {a.description && (
            <p className="mt-4 text-base text-text-mid leading-relaxed">
              {a.description}
            </p>
          )}
          {state.readOnly && a.emotionTags.length > 0 && (
            <div className="mt-5 flex flex-wrap gap-2">
              {a.emotionTags.map((tag) => (
                <span
                  key={tag}
                  className="rounded-full border border-accent px-3 py-1 text-xs font-medium text-accent"
                  style={{ background: "var(--accent-soft)" }}
                >
                  {tag}
                </span>
              ))}
            </div>
          )}

          {!state.readOnly && (
            <div className="mt-8 flex flex-wrap gap-3">
              {a.assetKind === "TRACKLIST" ? (
                <Link
                  href={`/assets/${a.id}/export`}
                  className="rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-200 hover:bg-accent-hi hover:-translate-y-0.5 focus-ring"
                  style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
                >
                  음악 플랫폼으로 내보내기
                </Link>
              ) : (
                <p className="flex h-12 items-center rounded-full border border-hairline px-5 text-sm text-text-low">
                  무드영상은 음악 플랫폼으로 내보낼 수 없어요.
                </p>
              )}
              <button
                type="button"
                onClick={handleShare}
                className="glass rounded-full border-hairline-strong px-6 text-sm font-semibold text-text-hi transition-all duration-200 hover:bg-surface-3 hover:-translate-y-0.5 focus-ring"
                style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
              >
                공유 링크 만들기
              </button>
              <button
                type="button"
                onClick={handleDelete}
                disabled={deleting}
                className="rounded-full px-4 text-sm font-semibold text-danger disabled:opacity-50"
              >
                {deleting ? "삭제 중…" : "삭제"}
              </button>
            </div>
          )}
        </div>
      </section>

      {/* Body: track list + emotional context */}
      <section
        className={`mt-12 grid grid-cols-1 gap-10 ${
          state.readOnly ? "" : "md:grid-cols-[minmax(0,1fr)_360px] md:gap-10"
        }`}
      >
        {/* Track list */}
        <div>
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            {a.assetKind === "MOOD_VIDEO" ? "Mood video" : "Tracks"}
          </p>
          {a.assetKind === "MOOD_VIDEO" && a.moodVideoId ? (
            <MoodVideoPlayer
              title={a.title}
              videoId={a.moodVideoId}
              channelName={a.moodChannelName}
              trackListText={a.moodTrackListText}
              className="mt-4"
            />
          ) : (
            <ShareTrackList tracks={a.tracks} />
          )}
        </div>

        {/* Emotional Context glass card */}
        {!state.readOnly && <aside>
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            Emotional Context
          </p>
          <div
            className="glass mt-4 p-6 md:p-8"
            style={{ borderRadius: "var(--radius-card)", boxShadow: "var(--shadow-card)" }}
          >
            <label className="block text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
              한 줄 요약
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              onBlur={() => persist({ description })}
              placeholder="예: 혼자 걷던 새벽들"
              className="mt-2 w-full bg-surface-2 border border-hairline rounded-[var(--radius-input)] px-4 py-3 text-base text-text-hi placeholder:text-text-low outline-none transition-all duration-200 focus:border-accent focus:bg-surface-3 focus-ring"
            />

            <label className="mt-6 block text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
              이 플레이리스트의 이야기
            </label>
            <textarea
              value={diary}
              onChange={(e) => setDiary(e.target.value)}
              onBlur={() => persist({ diaryText: diary })}
              rows={6}
              placeholder="이 플레이리스트가 함께한 시간을 남겨두세요."
              className="mt-2 w-full resize-none bg-surface-2 border border-hairline rounded-[var(--radius-input)] px-4 py-3 text-[0.95rem] leading-relaxed text-text-hi placeholder:text-text-low outline-none transition-all duration-200 focus:border-accent focus:bg-surface-3 focus-ring"
            />

            <label className="mt-6 block text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
              감정 태그
            </label>
            <EmotionTagPicker
              className="mt-3"
              value={tags}
              onChange={(next) => {
                setTags(next);
                persist({ emotionTags: next });
              }}
            />

            <p className="mt-6 text-xs text-text-low">
              {saving
                ? "저장 중…"
                : savedAt
                  ? "저장됨"
                  : "변경 사항은 자동으로 저장돼요."}
            </p>
          </div>
        </aside>}
      </section>
    </PageShell>
  );
}
