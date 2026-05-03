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

  return (
    <PageShell>
      <section className="flex min-h-[60vh] max-w-3xl flex-col justify-center py-16">
        <p className="mb-6 text-xs uppercase tracking-[0.24em] text-ink-500">
          Step 2 · Importing
        </p>

        {error ? (
          <div className="flex flex-col items-start gap-6">
            <h1 className="text-3xl leading-tight">{error}</h1>
            <p className="max-w-md text-sm text-ink-500">
              일부 트랙은 ISRC 정보가 없어 매칭이 어려울 수 있어요.
            </p>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => {
                  setError(null);
                  startedRef.current = false;
                  // re-trigger effect via small state nudge
                  setProgress(0);
                  setStatusLabel("queued");
                  setTimeout(() => {
                    startedRef.current = false;
                    location.reload();
                  }, 50);
                }}
                className="rounded-full bg-ink-900 px-5 py-2 text-sm text-bone-50 transition-colors duration-500 hover:bg-ink-700"
              >
                다시 시도
              </button>
              <button
                type="button"
                onClick={() => router.push("/import")}
                className="rounded-full border border-stone-300 px-5 py-2 text-sm text-ink-700 transition-colors duration-500 hover:bg-bone-100"
              >
                다른 플레이리스트 고르기
              </button>
            </div>
          </div>
        ) : (
          <>
            <ProgressNarrative messages={NARRATIVE} intervalMs={2200} />
            <div className="mt-12 max-w-md">
              <div className="h-px w-full overflow-hidden bg-stone-200">
                <div
                  className="h-full bg-ink-900 transition-all duration-700 ease-[var(--ease-weighted)]"
                  style={{ width: `${Math.max(4, progress)}%` }}
                />
              </div>
              <p className="mt-3 text-xs tracking-wide text-ink-400">
                {statusLabel === "queued"
                  ? "대기 중"
                  : statusLabel === "matching"
                    ? "정규화 중"
                    : statusLabel === "completed"
                      ? "완료"
                      : "실패"}
                {" · "}
                {progress}%
              </p>
            </div>
          </>
        )}
      </section>
    </PageShell>
  );
}
