"use client";

import { Suspense } from "react";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { PageShell } from "@/components/ui/PageShell";
import { EmotionTagPicker } from "@/components/ui/EmotionTagPicker";
import { MatchReviewSection } from "@/components/convert/MatchReviewSection";
import { getExportStatus } from "@/lib/api/exports";
import { getAsset } from "@/lib/api/assets";
import { demoConvertResult, buildDemoAssetDetail } from "@/lib/api/fixtures";
import { demoFixturesEnabled } from "@/lib/demo";
import type { ExportMapping } from "@/types/asset";
import { cn } from "@/lib/utils/cn";

// ── Types ──────────────────────────────────────────────────────────────────

interface ResultData {
  assetId: string;
  externalUrl: string | null;
  matchedTracks: number;
  failedTracks: number;
  totalTracks: number;
  title: string;
  coverUrl: string;
  /** Mappings needing review (status !== "matched"): low-confidence + failed. */
  reviewMappings: ExportMapping[];
  usingFixture: boolean;
}

/** ResultData built purely from demo fixtures (no backend). */
function fixtureResultData(): ResultData {
  return {
    assetId: demoConvertResult.assetId,
    externalUrl: demoConvertResult.externalUrl,
    matchedTracks: demoConvertResult.matchedTracks,
    failedTracks: demoConvertResult.failedTracks,
    totalTracks: demoConvertResult.totalTracks,
    title: demoConvertResult.title,
    coverUrl: demoConvertResult.coverUrl,
    reviewMappings: demoConvertResult.failedMappings.map((m) => ({
      trackId: m.trackId,
      status: m.status,
    })),
    usingFixture: true,
  };
}

type State =
  | { kind: "loading" }
  | { kind: "error" }
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
    noParams && demoFixturesEnabled()
      ? { kind: "ready", data: fixtureResultData() }
      : noParams
        ? { kind: "error" }
        : { kind: "loading" },
  );
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
        const allowFixture = demoFixturesEnabled();
        const [statusResult, assetResult] = await Promise.allSettled([
          jobId ? getExportStatus(jobId) : Promise.reject(new Error("no jobId")),
          assetId ? getAsset(assetId) : Promise.reject(new Error("no assetId")),
        ]);

        if (cancelled) return;

        // Primary path: the export status carries counts, external URL, and the
        // per-track mappings used for manual review.
        if (statusResult.status === "fulfilled") {
          const s = statusResult.value;
          const asset =
            assetResult.status === "fulfilled" ? assetResult.value : null;
          if (asset && !titleParam) setAssetTitle(asset.title);

          setState({
            kind: "ready",
            data: {
              assetId: assetId || "",
              externalUrl: s.externalUrl ?? null,
              matchedTracks: s.matchedTracks,
              failedTracks: s.failedTracks,
              totalTracks: s.totalTracks,
              title: titleParam || asset?.title || "변환 결과",
              coverUrl: coverParam || asset?.coverUrl || "",
              reviewMappings: (s.mappings ?? []).filter(
                (m) => m.status !== "matched",
              ),
              usingFixture: false,
            },
          });
          return;
        }

        // Backend unavailable — fall back to demo fixtures, enriched with names.
        if (allowFixture) {
          const demo = buildDemoAssetDetail(assetId || "demo");
          const byId = new Map(demo.tracks.map((t) => [t.id, t]));
          setState({
            kind: "ready",
            data: {
              ...fixtureResultData(),
              assetId: assetId || demoConvertResult.assetId,
              title: titleParam || demoConvertResult.title,
              coverUrl: coverParam || demoConvertResult.coverUrl,
              reviewMappings: demoConvertResult.failedMappings.map((m) => {
                const t = byId.get(m.trackId);
                return {
                  trackId: m.trackId,
                  status: m.status,
                  title: t?.name,
                  artist: t?.artist,
                };
              }),
            },
          });
          return;
        }

        throw new Error("Export result unavailable");
      } catch {
        if (!cancelled) {
          if (demoFixturesEnabled()) {
            setState({ kind: "ready", data: fixtureResultData() });
          } else {
            setState({ kind: "error" });
          }
        }
      }
    })();

    return () => { cancelled = true; };
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

  if (state.kind === "error") {
    return (
      <PageShell>
        <div className="flex min-h-[60vh] flex-col items-center justify-center gap-5 py-32 text-center">
          <p className="text-xl font-semibold text-text-hi">변환 결과를 불러오지 못했어요.</p>
          <Link className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white" href="/convert">
            다시 시도하기
          </Link>
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
              ? "YouTube Music에서 찾지 못했어요"
              : "YouTube Music으로 옮겼어요"}
          </h1>
          {d.title && (
            <p className="mt-1 text-sm text-text-mid">{d.title}</p>
          )}

          {/* Open in Apple Music CTA */}
          {!allFailed && (
            <div className="mt-6 flex flex-wrap gap-3">
              <a
                href={d.externalUrl || "https://music.youtube.com"}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex h-12 items-center rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-200 hover:bg-accent-hi hover:-translate-y-0.5 focus-ring"
                style={{ boxShadow: "var(--shadow-glow)" }}
              >
                YouTube Music에서 열기
              </a>
            </div>
          )}

          {d.usingFixture && (
            <p className="mt-4 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
              Demo data · 백엔드 연결 전
            </p>
          )}
        </section>

        {/* ── Match review (low-confidence + failed) ──────────────────────── */}
        {d.reviewMappings.length > 0 && (
          <MatchReviewSection
            jobId={jobId}
            mappings={d.reviewMappings}
            interactive={!d.usingFixture}
          />
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
            이 플레이리스트에 이야기 남기기
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
                {saving ? "저장 중…" : "플레이리스트로 저장"}
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
