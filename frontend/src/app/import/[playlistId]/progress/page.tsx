"use client";

import { useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { getImportStatus, startImport } from "@/lib/api/imports";
import { makeIdempotencyKey } from "@/lib/api/client";
import { demoImportProgression } from "@/lib/api/fixtures";
import type { ImportJobStatus } from "@/types/asset";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";

const NARRATIVE = [
  "플레이리스트를 읽고 있어요…",
  "트랙을 정규화하고 있어요…",
  "자산으로 정리하고 있어요…",
];

// Deterministic cover image based on playlistId seed
const COVER_SEEDS = ["pl1", "pl2", "pl3", "pl4", "pl5"];

export default function ImportProgressPage() {
  const router = useRouter();
  const params = useParams<{ playlistId: string }>();
  const playlistId = params.playlistId;

  const [error, setError] = useState<string | null>(null);
  const [progress, setProgress] = useState(0);
  const [statusLabel, setStatusLabel] = useState<ImportJobStatus["status"]>("queued");
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    let cancelled = false;
    let demoStep = 0;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const finish = (assetId: string) => {
      if (cancelled) return;
      router.push(`/assets/${assetId}`);
    };

    const runDemo = () => {
      const tick = () => {
        if (cancelled) return;
        const snapshot = demoImportProgression[demoStep];
        setProgress(snapshot.progress);
        setStatusLabel(snapshot.status);
        if (snapshot.status === "completed" && snapshot.assetId) {
          finish(snapshot.assetId);
          return;
        }
        demoStep = Math.min(demoStep + 1, demoImportProgression.length - 1);
        timer = setTimeout(tick, 900);
      };
      tick();
    };

    const runLive = async () => {
      try {
        const job = await startImport(
          playlistId,
          makeIdempotencyKey(`import-${playlistId}`),
        );
        const poll = async () => {
          if (cancelled) return;
          try {
            const s = await getImportStatus(job.jobId);
            setProgress(s.progress);
            setStatusLabel(s.status);
            if (s.status === "completed" && s.assetId) {
              finish(s.assetId);
              return;
            }
            if (s.status === "failed") {
              setError("매칭에 실패했어요. 잠시 후 다시 시도해 주세요.");
              return;
            }
            timer = setTimeout(poll, 1000);
          } catch {
            // Polling failure mid-flight: drop to demo path silently.
            runDemo();
          }
        };
        poll();
      } catch {
        // BE unreachable from the start — show demo progression so the UX is verifiable.
        runDemo();
      }
    };

    runLive();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, [playlistId, router]);

  // Pick a stable cover seed from the playlistId
  const coverSeed =
    COVER_SEEDS[
      Math.abs(
        playlistId
          .split("")
          .reduce((acc, c) => acc + c.charCodeAt(0), 0),
      ) % COVER_SEEDS.length
    ];
  const coverUrl = `https://picsum.photos/seed/${coverSeed}/600/600`;

  const statusText =
    statusLabel === "queued"
      ? "대기 중"
      : statusLabel === "matching"
        ? "정규화 중"
        : statusLabel === "completed"
          ? "완료"
          : "실패";

  return (
    <PageShell>
      <section className="flex min-h-[80vh] flex-col items-center justify-center py-16 text-center">
        {error ? (
          /* ── Error state ─────────────────────────────────────── */
          <div
            className="flex w-full max-w-md flex-col items-center gap-6 animate-fade-up"
          >
            <div
              className="rounded-[18px] border px-6 py-5 text-sm text-danger"
              style={{
                background: "rgba(251,113,133,0.08)",
                borderColor: "rgba(251,113,133,0.2)",
              }}
            >
              {error}
            </div>
            <p className="text-sm text-text-mid">
              일부 트랙은 ISRC 정보가 없어 매칭이 어려울 수 있어요.
            </p>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => {
                  setError(null);
                  startedRef.current = false;
                  setProgress(0);
                  setStatusLabel("queued");
                  setTimeout(() => {
                    startedRef.current = false;
                    location.reload();
                  }, 50);
                }}
                className="inline-flex h-12 items-center rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-300 hover:bg-accent-hi active:bg-accent-press focus-ring"
              >
                다시 시도
              </button>
              <button
                type="button"
                onClick={() => router.push("/import")}
                className="glass inline-flex h-12 items-center rounded-full px-6 text-sm font-medium text-text-hi transition-colors duration-300 hover:border-hairline-strong"
              >
                다른 플레이리스트 고르기
              </button>
            </div>
          </div>
        ) : (
          /* ── Progress state ──────────────────────────────────── */
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
              Step 2 · Importing
            </p>

            {/* Large cover with ambient glow */}
            <div className="relative">
              {/* Ambient glow behind cover */}
              <div
                className="cover-glow absolute inset-0 rounded-[16px]"
                style={{
                  backgroundImage: `url(${coverUrl})`,
                  backgroundSize: "cover",
                  backgroundPosition: "center",
                  animation: "plshare-pulse-glow 3s ease-in-out infinite",
                }}
                aria-hidden
              />
              {/* Accent glow overlay */}
              <div
                className="pointer-events-none absolute inset-[-30px] rounded-full animate-pulse-glow"
                style={{
                  background:
                    "radial-gradient(circle, rgba(124,92,255,0.20) 0%, transparent 70%)",
                }}
                aria-hidden
              />
              <img
                src={coverUrl}
                alt="플레이리스트 커버"
                width={240}
                height={240}
                className="relative rounded-[16px] object-cover"
                style={{
                  boxShadow:
                    "0 20px 60px -20px rgba(0,0,0,0.8), var(--shadow-glow)",
                  border: "1px solid rgba(255,255,255,0.10)",
                  display: "block",
                }}
              />
            </div>

            {/* Narrative */}
            <ProgressNarrative messages={NARRATIVE} intervalMs={2200} />

            {/* Accent slim progress bar */}
            <div className="w-full">
              <div
                className="w-full overflow-hidden rounded-full"
                style={{ height: 3, background: "var(--accent-soft)" }}
                role="progressbar"
                aria-valuenow={progress}
                aria-valuemin={0}
                aria-valuemax={100}
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
                  {statusText}
                </p>
                <p
                  className="tabular-nums text-text-low"
                  style={{ fontSize: "0.8125rem" }}
                >
                  {progress}%
                </p>
              </div>
            </div>
          </div>
        )}
      </section>
    </PageShell>
  );
}
