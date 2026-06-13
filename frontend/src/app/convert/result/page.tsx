"use client";

import { Suspense } from "react";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { PageShell } from "@/components/ui/PageShell";
import { MatchConfidenceBadge } from "@/components/ui/MatchConfidenceBadge";
import { EmotionTagPicker } from "@/components/ui/EmotionTagPicker";
import { TrackRow } from "@/components/ui/TrackRow";
import { getExportResult, getExportStatus } from "@/lib/api/exports";
import { getAsset } from "@/lib/api/assets";
import { demoConvertResult, buildDemoAssetDetail } from "@/lib/api/fixtures";
import type { AssetTrack, ExportMappingStatus } from "@/types/asset";
import { cn } from "@/lib/utils/cn";

// ── Types ──────────────────────────────────────────────────────────────────

interface FailedMapping {
  trackId: string;
  status: ExportMappingStatus;
}

interface ResultData {
  assetId: string;
  externalUrl: string | null;
  matchedTracks: number;
  failedTracks: number;
  totalTracks: number;
  title: string;
  coverUrl: string;
  failedMappings: FailedMapping[];
  failedTrackDetails: AssetTrack[];
  usingFixture: boolean;
}

type State =
  | { kind: "loading" }
  | { kind: "ready"; data: ResultData };

// ── Component ──────────────────────────────────────────────────────────────

function ConvertResultPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const assetId = searchParams.get("assetId") ?? "";
  const jobId = searchParams.get("jobId") ?? "";
  const titleParam = searchParams.get("title") ?? "";
  const coverParam = searchParams.get("coverUrl") ?? "";

  const noParams = !assetId && !jobId;

  const [state, setState] = useState<State>(() =>
    noParams
      ? {
          kind: "ready",
          data: {
            ...demoConvertResult,
            failedTrackDetails: [],
            usingFixture: true,
          },
        }
      : { kind: "loading" },
  );
  const [accordionOpen, setAccordionOpen] = useState(false);

  // Assetization invite form state
  const [assetTitle, setAssetTitle] = useState(titleParam);
  const [tags, setTags] = useState<string[]>([]);
  const [memo, setMemo] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (noParams) return;

    let cancelled = false;

    (async () => {
      try {
        const [resultData, assetData] = await Promise.allSettled([
          jobId ? getExportResult(jobId) : Promise.reject(new Error("no jobId")),
          assetId ? getAsset(assetId) : Promise.reject(new Error("no assetId")),
        ]);

        if (cancelled) return;

        // Export result
        let matched = demoConvertResult.matchedTracks;
        let failed = demoConvertResult.failedTracks;
        let total = demoConvertResult.totalTracks;
        let externalUrl: string | null = demoConvertResult.externalUrl;
        let failedMappings: FailedMapping[] = demoConvertResult.failedMappings;
        let usingFixture = true;

        if (resultData.status === "fulfilled") {
          const r = resultData.value;
          matched = r.matchedTracks;
          failed = r.failedTracks;
          total = matched + failed;
          externalUrl = r.externalUrl ?? null;
          usingFixture = false;
        } else {
          // Try getting mappings from status endpoint
          try {
            const status = jobId ? await getExportStatus(jobId) : null;
            if (status && !cancelled) {
              matched = status.matchedTracks;
              failed = status.failedTracks;
              total = status.totalTracks;
              externalUrl = status.externalUrl ?? null;
              failedMappings = status.mappings
                .filter((m) => m.status === "failed")
                .map((m) => ({ trackId: m.trackId, status: m.status }));
              usingFixture = false;
            }
          } catch {
            // Keep fixture values
          }
        }

        // Asset detail for failed track names
        let failedTrackDetails: AssetTrack[] = [];
        if (assetData.status === "fulfilled") {
          const asset = assetData.value;
          const failedIds = new Set(failedMappings.map((m) => m.trackId));
          failedTrackDetails = asset.tracks.filter((t) => failedIds.has(t.id));
          if (!titleParam) setAssetTitle(asset.title);
        } else {
          // Build demo asset detail for track names
          const demo = buildDemoAssetDetail(assetId || "demo");
          const failedIds = new Set(failedMappings.map((m) => m.trackId));
          failedTrackDetails = demo.tracks.filter((t) => failedIds.has(t.id));
        }

        setState({
          kind: "ready",
          data: {
            assetId: assetId || demoConvertResult.assetId,
            externalUrl,
            matchedTracks: matched,
            failedTracks: failed,
            totalTracks: total,
            title: titleParam || demoConvertResult.title,
            coverUrl: coverParam || demoConvertResult.coverUrl,
            failedMappings,
            failedTrackDetails,
            usingFixture,
          },
        });
      } catch {
        if (!cancelled) {
          setState({
            kind: "ready",
            data: {
              ...demoConvertResult,
              failedTrackDetails: [],
              usingFixture: true,
            },
          });
        }
      }
    })();

    return () => { cancelled = true; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assetId, jobId, titleParam, coverParam, noParams]);

  async function handleSaveAsset() {
    if (state.kind !== "ready") return;
    setSaving(true);
    // In production: PATCH /api/assets/{id} with { title, emotionTags, description }
    // For demo: navigate directly.
    await new Promise((r) => setTimeout(r, 600));
    router.push(`/assets/${state.data.assetId}`);
  }

  async function handleLater() {
    if (state.kind !== "ready") return;
    router.push("/assets");
  }

  if (state.kind === "loading") {
    return (
      <PageShell>
        <div className="flex min-h-[60vh] items-center justify-center py-32">
          <p className="text-sm text-text-mid animate-pulse">집계 중이에요…</p>
        </div>
      </PageShell>
    );
  }

  const d = state.data;
  const allFailed = d.matchedTracks === 0 && d.failedTracks > 0;

  return (
    <PageShell>
      {/* Ambient accent glow */}
      <div
        className="accent-glow pointer-events-none fixed left-1/2 top-1/3 h-[700px] w-[700px] -translate-x-1/2 -translate-y-1/2 opacity-25"
        aria-hidden
      />

      <div className="mx-auto max-w-2xl">
        {/* ── Hero ──────────────────────────────────────────────────────── */}
        <section className="pt-10 pb-8 animate-fade-up">
          {/* Cover with glow */}
          {d.coverUrl && (
            <div className="relative mb-8 w-fit">
              <div
                className="cover-glow absolute inset-0 rounded-[var(--radius-image)]"
                style={{
                  backgroundImage: `url(${d.coverUrl})`,
                  backgroundSize: "cover",
                  backgroundPosition: "center",
                }}
                aria-hidden
              />
              <img
                src={d.coverUrl}
                alt="플레이리스트 커버"
                width={160}
                height={160}
                className="relative rounded-[var(--radius-image)] object-cover"
                style={{
                  border: "1px solid rgba(255,255,255,0.10)",
                  boxShadow: "0 20px 60px -20px rgba(0,0,0,0.8), var(--shadow-glow)",
                }}
              />
            </div>
          )}

          {/* Big numeric summary */}
          <div className="mb-2">
            <p
              className={cn(
                "tabular-nums font-display",
                allFailed ? "text-danger" : "text-success",
              )}
              style={{
                fontSize: "clamp(3rem, 8vw, 5rem)",
                fontWeight: 800,
                letterSpacing: "-0.03em",
                lineHeight: 1,
                fontFeatureSettings: "'tnum'",
              }}
            >
              {d.matchedTracks}
              <span
                className="text-text-mid"
                style={{ fontSize: "clamp(1.25rem, 3vw, 1.75rem)", fontWeight: 400 }}
              >
                /{d.totalTracks}
              </span>
            </p>
          </div>

          <h1
            className="font-display text-text-hi"
            style={{
              fontSize: "clamp(1.5rem, 3.5vw, 2.25rem)",
              fontWeight: 700,
              letterSpacing: "-0.02em",
            }}
          >
            {allFailed
              ? "Apple Music에서 찾을 수 없었어요"
              : "Apple Music으로 옮겨졌어요"}
          </h1>
          {d.title && (
            <p className="mt-1 text-sm text-text-mid">{d.title}</p>
          )}

          {/* Open in Apple Music CTA */}
          {!allFailed && (
            <div className="mt-6 flex flex-wrap gap-3">
              <a
                href={d.externalUrl || "https://music.apple.com"}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex h-12 items-center rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-200 hover:bg-accent-hi hover:-translate-y-0.5 focus-ring"
                style={{ boxShadow: "var(--shadow-glow)" }}
              >
                Apple Music에서 열기
              </a>
            </div>
          )}

          {d.usingFixture && (
            <p className="mt-4 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
              Demo data · 백엔드 연결 전
            </p>
          )}
        </section>

        {/* ── Failed tracks accordion ────────────────────────────────────── */}
        {d.failedTracks > 0 && (
          <section
            className="mb-8 rounded-[var(--radius-card)] border border-hairline bg-surface-1 animate-fade-up overflow-hidden"
            style={{ animationDelay: "80ms", boxShadow: "var(--shadow-card)" }}
          >
            <button
              type="button"
              onClick={() => setAccordionOpen((v) => !v)}
              className="flex w-full items-center justify-between px-5 py-4 transition-colors duration-150 hover:bg-surface-2 focus-ring"
              aria-expanded={accordionOpen}
            >
              <span className="text-sm font-semibold text-text-hi">
                {d.totalTracks}곡 중{" "}
                <span className="text-danger tabular-nums">{d.failedTracks}곡</span>{" "}
                매칭 안 됨
              </span>
              <span
                className="text-text-low transition-transform duration-300"
                style={{ transform: accordionOpen ? "rotate(180deg)" : "none" }}
                aria-hidden
              >
                ▾
              </span>
            </button>

            {accordionOpen && (
              <div className="border-t border-hairline">
                {d.failedTrackDetails.length > 0 ? (
                  d.failedTrackDetails.map((track, i) => (
                    <TrackRow
                      key={track.id}
                      track={track}
                      index={i}
                      trailing={<MatchConfidenceBadge status="failed" />}
                    />
                  ))
                ) : (
                  // Fallback when track details unavailable
                  d.failedMappings.map((m, i) => (
                    <TrackRow
                      key={m.trackId}
                      track={{
                        id: m.trackId,
                        name: `트랙 ${i + 1}`,
                        artist: "—",
                        durationMs: 0,
                      }}
                      index={i}
                      trailing={<MatchConfidenceBadge status={m.status} />}
                    />
                  ))
                )}

                {/* "나중에 해결" CTA */}
                <div className="border-t border-hairline px-5 py-4">
                  <Link
                    href={`/assets/${d.assetId}`}
                    className="text-sm text-text-mid transition-colors duration-200 hover:text-text-hi focus-ring rounded-full px-2 py-1"
                  >
                    나중에 해결하기 →
                  </Link>
                </div>
              </div>
            )}
          </section>
        )}

        {/* ── Assetization invite (Option B — post-conversion) ───────────── */}
        {/*
          Option B placement: shown AFTER conversion success, not during.
          Purpose: leverage the achievement moment to invite emotional contextualization.
          User can skip entirely — "나중에 하기" preserves the asset as-is.
        */}
        <section
          className="mb-8 rounded-[var(--radius-card)] border border-hairline bg-surface-1 p-6 animate-fade-up"
          style={{ animationDelay: "140ms", boxShadow: "var(--shadow-card)" }}
        >
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low mb-4">
            이 자산에 이야기 남기기
          </p>

          {/* ── Option B: "이 자산에 이야기 남기기" 초대 CTA (→ /assets/[assetId]) ── */}
          {/* Per social-ux-research §1.5: assetization placed after conversion (Option B).
              The primary CTA links directly to the asset detail where the user
              can add diary text, emotion tags, and title in the full editing UI. */}
          <div className="flex flex-col gap-4">
            <p className="text-sm text-text-mid leading-relaxed">
              이 플레이리스트에 감정 태그와 한 줄 메모를 남겨두면 나중에 기억하기 쉬워요.
            </p>

            {/* Lightweight inline assetization — title + tags */}
            <div className="space-y-4">
              <div>
                <label
                  htmlFor="asset-title-input"
                  className="block text-xs font-medium text-text-mid mb-1.5"
                >
                  제목
                </label>
                <input
                  id="asset-title-input"
                  type="text"
                  value={assetTitle}
                  onChange={(e) => setAssetTitle(e.target.value)}
                  placeholder="플레이리스트 이름"
                  className="w-full rounded-[var(--radius-input)] border border-hairline bg-surface-2 px-4 py-3 text-sm text-text-hi outline-none placeholder:text-text-low transition-colors duration-200 focus:border-accent/50"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-text-mid mb-2">
                  감정 태그
                </label>
                <EmotionTagPicker value={tags} onChange={setTags} />
              </div>

              <div>
                <label
                  htmlFor="asset-memo-input"
                  className="block text-xs font-medium text-text-mid mb-1.5"
                >
                  한 줄 메모 (선택)
                </label>
                <textarea
                  id="asset-memo-input"
                  value={memo}
                  onChange={(e) => setMemo(e.target.value)}
                  placeholder="이 음악들이 생각나는 이유…"
                  rows={2}
                  className="w-full resize-none rounded-[var(--radius-input)] border border-hairline bg-surface-2 px-4 py-3 text-sm text-text-hi outline-none placeholder:text-text-low transition-colors duration-200 focus:border-accent/50"
                />
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center gap-3 mt-1">
              <button
                type="button"
                onClick={handleSaveAsset}
                disabled={saving}
                className={cn(
                  "inline-flex h-11 items-center rounded-full px-6 text-sm font-semibold transition-all duration-200 focus-ring",
                  saving
                    ? "bg-surface-2 text-text-low cursor-not-allowed"
                    : "bg-accent text-white hover:bg-accent-hi hover:-translate-y-0.5",
                )}
                style={!saving ? { boxShadow: "var(--shadow-glow)" } : undefined}
              >
                {saving ? "저장 중…" : "자산으로 저장"}
              </button>
              <button
                type="button"
                onClick={handleLater}
                className="h-11 px-4 text-sm text-text-low transition-colors duration-200 hover:text-text-mid focus-ring rounded-full"
              >
                나중에 하기
              </button>
            </div>
          </div>
        </section>

        {/* ── Footer links ─────────────────────────────────────────────── */}
        <div
          className="flex flex-wrap items-center gap-4 pb-16 text-sm animate-fade-up"
          style={{ animationDelay: "200ms" }}
        >
          {/* "피드에 공유" — /feed route implemented by parallel task, link only */}
          <Link
            href="/feed"
            className="text-text-mid transition-colors duration-200 hover:text-text-hi focus-ring rounded-full px-2 py-1"
          >
            피드에 공유
          </Link>
          <span className="text-text-low">·</span>
          <Link
            href="/convert"
            className="text-text-mid transition-colors duration-200 hover:text-text-hi focus-ring rounded-full px-2 py-1"
          >
            하나 더 변환하기
          </Link>
          <span className="text-text-low">·</span>
          <Link
            href="/assets"
            className="text-text-low transition-colors duration-200 hover:text-text-mid focus-ring rounded-full px-2 py-1"
          >
            라이브러리로
          </Link>
        </div>
      </div>
    </PageShell>
  );
}

export default function ConvertResultPage() {
  return (
    <Suspense>
      <ConvertResultPageInner />
    </Suspense>
  );
}
