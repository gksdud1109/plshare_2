"use client";

import { Suspense } from "react";
import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { startImport, getImportStatus } from "@/lib/api/imports";
import { startExport, getExportStatus } from "@/lib/api/exports";
import { makeIdempotencyKey } from "@/lib/api/client";
import {
  demoConvertImportProgression,
  demoConvertExportNarratives,
  buildDemoExportStatus,
} from "@/lib/api/fixtures";

// ── Narrative copy ─────────────────────────────────────────────────────────

const IMPORT_NARRATIVES = [
  "트랙 정보를 모으는 중이에요",
  "ISRC 기준으로 정규화하는 중이에요",
  "플레이리스트를 자산으로 변환하는 중이에요",
];

const EXPORT_NARRATIVES = demoConvertExportNarratives;

// ── Types ──────────────────────────────────────────────────────────────────

type Stage = "import" | "export";

// ── Component ──────────────────────────────────────────────────────────────

function ConvertProgressPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const playlistId = searchParams.get("playlistId") ?? "demo";
  const sourcePlatform =
    (searchParams.get("sourcePlatform") as "spotify" | "youtube") ?? "spotify";
  const destination =
    (searchParams.get("destination") as "apple" | "youtube_music") ?? "apple";
  const coverUrl = searchParams.get("coverUrl") ?? "";
  const title = searchParams.get("title") ?? "";

  const [stage, setStage] = useState<Stage>("import");
  const [progress, setProgress] = useState(0);

  const startedRef = useRef(false);

  // Cover seed for when no coverUrl passed
  const fallbackCover = `https://picsum.photos/seed/${playlistId}/600/600`;
  const displayCover = coverUrl || fallbackCover;

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const navigateToResult = (assetId: string, jobId: string) => {
      if (cancelled) return;
      const params = new URLSearchParams({
        assetId,
        jobId,
        ...(title ? { title } : {}),
        ...(coverUrl ? { coverUrl } : {}),
      });
      router.push(`/convert/result?${params.toString()}`);
    };

    // ── Export phase ────────────────────────────────────────────────────
    const runExportDemo = (assetId: string) => {
      if (cancelled) return;
      setStage("export");
      setProgress(0);
      let step = 0;
      const tick = () => {
        if (cancelled) return;
        const snap = buildDemoExportStatus(step);
        const pct =
          snap.totalTracks > 0
            ? Math.round((snap.matchedTracks / snap.totalTracks) * 100)
            : 0;
        setProgress(pct);
        if (snap.status === "completed") {
          navigateToResult(assetId, `demo-export-${assetId}`);
          return;
        }
        step = Math.min(step + 1, snap.totalTracks);
        timer = setTimeout(tick, 700);
      };
      tick();
    };

    const runExportLive = async (assetId: string) => {
      if (cancelled) return;
      setStage("export");
      setProgress(0);
      try {
        const job = await startExport(
          assetId,
          makeIdempotencyKey(`convert-export-${assetId}`),
          // targetPlatform: only "apple" is live; ytm is ui-only per task spec
          "apple",
        );
        const poll = async () => {
          if (cancelled) return;
          try {
            const s = await getExportStatus(job.jobId);
            const pct =
              s.totalTracks > 0
                ? Math.round((s.matchedTracks / s.totalTracks) * 100)
                : 0;
            setProgress(pct);
            if (
              s.status === "completed" ||
              s.status === "partial" ||
              s.status === "failed"
            ) {
              navigateToResult(assetId, job.jobId);
              return;
            }
            timer = setTimeout(poll, 1100);
          } catch {
            runExportDemo(assetId);
          }
        };
        poll();
      } catch {
        runExportDemo(assetId);
      }
    };

    // ── Import phase ────────────────────────────────────────────────────
    const runImportDemo = () => {
      let step = 0;
      const tick = () => {
        if (cancelled) return;
        const snap = demoConvertImportProgression[step];
        setProgress(snap.progress);
        if (snap.status === "completed" && snap.assetId) {
          runExportLive(snap.assetId);
          return;
        }
        step = Math.min(step + 1, demoConvertImportProgression.length - 1);
        timer = setTimeout(tick, 900);
      };
      tick();
    };

    const runImportLive = async () => {
      try {
        const job = await startImport(
          playlistId,
          makeIdempotencyKey(`convert-import-${playlistId}`),
          sourcePlatform,
        );
        const poll = async () => {
          if (cancelled) return;
          try {
            const s = await getImportStatus(job.jobId);
            setProgress(s.progress);
            if (s.status === "completed" && s.assetId) {
              runExportLive(s.assetId);
              return;
            }
            if (s.status === "failed") {
              // Fall through to demo for UX continuity
              runImportDemo();
              return;
            }
            timer = setTimeout(poll, 1000);
          } catch {
            runImportDemo();
          }
        };
        poll();
      } catch {
        // BE unreachable — demo mode
        runImportDemo();
      }
    };

    runImportLive();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, [playlistId, sourcePlatform, coverUrl, title, router]);

  const narratives = stage === "import" ? IMPORT_NARRATIVES : EXPORT_NARRATIVES;
  const stageLabel =
    stage === "import" ? "가져오는 중" : "변환 중";
  const destLabel =
    destination === "apple" ? "Apple Music" : "YouTube Music";

  return (
    <PageShell>
      {/* Accent glow */}
      <div
        className="accent-glow pointer-events-none fixed left-1/2 top-1/4 h-[600px] w-[600px] -translate-x-1/2 -translate-y-1/2 opacity-25"
        aria-hidden
      />

      <section className="flex min-h-[80vh] flex-col items-center justify-center py-16 text-center">
        <div className="flex w-full max-w-sm flex-col items-center gap-8 animate-fade-up">
          {/* Eyebrow */}
          <p
            className="text-text-low"
            style={{
              fontSize: "0.75rem",
              fontWeight: 600,
              letterSpacing: "0.12em",
              textTransform: "uppercase",
            }}
          >
            {stageLabel} · {destLabel}으로
          </p>

          {/* Cover with ambient glow */}
          <div className="relative">
            <div
              className="cover-glow absolute inset-0 rounded-[16px]"
              style={{
                backgroundImage: `url(${displayCover})`,
                backgroundSize: "cover",
                backgroundPosition: "center",
                animation: "plshare-pulse-glow 3s ease-in-out infinite",
              }}
              aria-hidden
            />
            {/* Accent glow overlay */}
            <div
              className="pointer-events-none absolute inset-[-40px] rounded-full animate-pulse-glow"
              style={{
                background:
                  "radial-gradient(circle, rgba(124,92,255,0.18) 0%, transparent 70%)",
              }}
              aria-hidden
            />
            <img
              src={displayCover}
              alt="플레이리스트 커버"
              width={200}
              height={200}
              className="relative rounded-[16px] object-cover"
              style={{
                boxShadow: "0 20px 60px -20px rgba(0,0,0,0.8), var(--shadow-glow)",
                border: "1px solid rgba(255,255,255,0.10)",
                display: "block",
              }}
            />
          </div>

          {title && (
            <p className="text-sm font-medium text-text-mid">
              {title}
            </p>
          )}

          {/* Narrative */}
          <ProgressNarrative messages={narratives} intervalMs={2200} />

          {/* Progress bar */}
          <div className="w-full">
            <div
              className="w-full overflow-hidden rounded-full"
              style={{ height: 3, background: "var(--accent-soft)" }}
              role="progressbar"
              aria-valuenow={progress}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-label={`변환 진행률 ${progress}%`}
            >
              <div
                className="h-full rounded-full bg-accent transition-all duration-700"
                style={{
                  width: `${Math.max(4, progress)}%`,
                  boxShadow: "0 0 8px 0 rgba(124,92,255,0.7)",
                  transitionTimingFunction: "var(--ease-out)",
                }}
              />
            </div>
            <div className="mt-3 flex items-center justify-between">
              <p
                className="text-text-mid"
                style={{ fontSize: "0.8125rem", letterSpacing: "0.02em" }}
              >
                {stageLabel}
              </p>
              <p
                className="tabular-nums text-text-low"
                style={{ fontSize: "0.8125rem" }}
              >
                {progress}%
              </p>
            </div>
          </div>

          {/* Ghost "continue in background" link */}
          <button
            type="button"
            onClick={() => router.push("/assets")}
            className="text-sm text-text-low transition-colors duration-200 hover:text-text-mid focus-ring rounded-full px-3 py-1"
          >
            백그라운드에서 계속 →
          </button>
        </div>
      </section>
    </PageShell>
  );
}

export default function ConvertProgressPage() {
  return (
    <Suspense>
      <ConvertProgressPageInner />
    </Suspense>
  );
}
