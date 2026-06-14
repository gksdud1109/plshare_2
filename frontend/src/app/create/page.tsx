"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useSessionUser } from "@/lib/auth/useSessionUser";
import { getCatalog, composeAsset } from "@/lib/api/catalog";
import { messageFromError } from "@/lib/errors";
import { useToast } from "@/components/ui/ToastProvider";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { TrackEmbed } from "@/components/gift/TrackEmbed";
import { MOOD_LABELS, MOOD_ORDER, type CatalogTrack } from "@/types/catalog";

function formatDuration(ms?: number | null): string {
  if (!ms) return "";
  const total = Math.floor(ms / 1000);
  return `${Math.floor(total / 60)}:${(total % 60).toString().padStart(2, "0")}`;
}

export default function CreatePage() {
  const session = useSessionUser();
  const toast = useToast();
  const router = useRouter();

  const [tracks, setTracks] = useState<CatalogTrack[]>([]);
  const [loading, setLoading] = useState(true);
  const [mood, setMood] = useState<string>("all");
  const [selected, setSelected] = useState<string[]>([]);
  const [previewId, setPreviewId] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [composing, setComposing] = useState(false);

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
  const selectedTracks = useMemo(
    () => selected.map((id) => tracks.find((t) => t.id === id)).filter(Boolean) as CatalogTrack[],
    [selected, tracks],
  );

  function toggle(id: string) {
    setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id]));
  }

  async function handleCompose() {
    if (selected.length === 0) {
      toast.error("곡을 한 곡 이상 선택해주세요.");
      return;
    }
    const finalTitle = title.trim() || "내 플레이리스트";
    setComposing(true);
    try {
      const asset = await composeAsset({ title: finalTitle, trackIds: selected });
      toast.success("플레이리스트를 만들었어요");
      router.push(`/gift/send?assetId=${asset.id}`);
    } catch (err) {
      toast.error(messageFromError(err, "플레이리스트 생성에 실패했어요"));
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
          플레이리스트 만들기
        </h1>
        <p className="mt-2 text-sm text-text-low">
          큐레이션된 곡에서 골라 담으면, 받는 사람이 계정 없이 바로 들을 수 있어요.
        </p>
      </header>

      {/* Mood filter */}
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

      {loading ? (
        <div className="py-24">
          <ProgressNarrative messages={["곡을 불러오는 중이에요…"]} />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-2 pb-40">
          {visible.map((t) => {
            const isSel = selected.includes(t.id);
            const isPreview = previewId === t.id;
            return (
              <div
                key={t.id}
                className="overflow-hidden rounded-[14px] border bg-surface-1"
                style={{ borderColor: isSel ? "var(--color-accent)" : "var(--color-hairline)" }}
              >
                <div className="flex items-center gap-3 p-3">
                  <button
                    type="button"
                    onClick={() => setPreviewId(isPreview ? null : t.id)}
                    aria-label={isPreview ? "미리듣기 닫기" : "미리듣기"}
                    className="relative h-14 w-14 shrink-0 overflow-hidden rounded-[10px] bg-surface-2 focus-ring"
                  >
                    {t.coverUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={t.coverUrl} alt={t.title} className="h-full w-full object-cover" />
                    ) : (
                      <span className="flex h-full w-full items-center justify-center text-text-low">♪</span>
                    )}
                    <span className="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 transition-opacity hover:opacity-100">
                      <svg width="18" height="18" viewBox="0 0 16 16" fill="#fff" aria-hidden>
                        {isPreview ? (
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
                    <p className="truncate text-[0.9375rem] font-medium text-text-hi">{t.title}</p>
                    <p className="mt-0.5 truncate text-xs text-text-mid">
                      {t.artist}
                      {t.durationMs ? ` · ${formatDuration(t.durationMs)}` : ""}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => toggle(t.id)}
                    aria-pressed={isSel}
                    className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border transition-colors focus-ring"
                    style={{
                      borderColor: isSel ? "var(--color-accent)" : "var(--color-hairline-strong)",
                      background: isSel ? "var(--color-accent)" : "transparent",
                      color: isSel ? "#fff" : "var(--color-text-mid)",
                    }}
                    aria-label={isSel ? "선택 해제" : "선택"}
                  >
                    {isSel ? "✓" : "+"}
                  </button>
                </div>
                {isPreview && <TrackEmbed videoId={t.youtubeVideoId} title={t.title} />}
              </div>
            );
          })}
        </div>
      )}

      {/* Sticky compose bar */}
      <div className="fixed inset-x-0 bottom-0 z-30 border-t border-hairline bg-bg/90 backdrop-blur">
        <div className="mx-auto flex w-full max-w-3xl items-center gap-3 px-6 py-3">
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value.slice(0, 100))}
            placeholder="플레이리스트 이름"
            className="min-w-0 flex-1 rounded-xl border border-hairline bg-surface-1 px-4 py-2.5 text-sm text-text-hi placeholder:text-text-low outline-none focus:border-accent"
          />
          <button
            type="button"
            onClick={handleCompose}
            disabled={composing || selected.length === 0}
            className="shrink-0 rounded-full bg-accent px-6 py-2.5 text-sm font-semibold text-white transition-all hover:bg-accent-hi disabled:opacity-50"
          >
            {composing ? "만드는 중…" : `${selected.length}곡 담아 선물 →`}
          </button>
        </div>
        {selectedTracks.length > 0 && (
          <div className="mx-auto -mt-1 w-full max-w-3xl px-6 pb-2">
            <p className="truncate text-[0.6875rem] text-text-low">
              {selectedTracks.map((t) => t.title).join(" · ")}
            </p>
          </div>
        )}
      </div>
    </PageShell>
  );
}
