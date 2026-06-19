"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { getImportStatus, startImport } from "@/lib/api/imports";
import { makeIdempotencyKey } from "@/lib/api/client";
import { demoImportProgression } from "@/lib/api/fixtures";
import { demoFixturesEnabled } from "@/lib/demo";
import type { ImportJobStatus, ImportSourcePlatform } from "@/types/asset";
import { indicatesMissingYoutubeScope } from "@/lib/api/playlists";
import { messageFromError } from "@/lib/errors";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";

const NARRATIVE = [
  "플레이리스트를 읽고 있어요…",
  "트랙을 정규화하고 있어요…",
  "플레이리스트로 정리하고 있어요…",
];
const MAX_POLL_ATTEMPTS = 120;

// Deterministic cover image based on playlistId seed
const COVER_SEEDS = ["pl1", "pl2", "pl3", "pl4", "pl5"];

function ImportProgressPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const params = useParams<{ playlistId: string }>();
  const playlistId = params.playlistId;
  const sourcePlatform: ImportSourcePlatform =
    searchParams.get("sourcePlatform") === "spotify" ? "spotify" : "youtube";

  const [error, setError] = useState<string | null>(null);
  const [needsConsent, setNeedsConsent] = useState(false);
  const [timedOut, setTimedOut] = useState(false);
  const [progress, setProgress] = useState(0);
  const [statusLabel, setStatusLabel] = useState<ImportJobStatus["status"]>("queued");
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    let cancelled = false;
    let demoStep = 0;
    let pollAttempts = 0;
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
          makeIdempotencyKey(`import-${sourcePlatform}-${playlistId}`),
          sourcePlatform,
        );
        const poll = async () => {
          if (cancelled) return;
          if (pollAttempts >= MAX_POLL_ATTEMPTS) {
            setTimedOut(true);
            setError("가져오기가 예상보다 오래 걸리고 있어요.");
            return;
          }
          pollAttempts += 1;
          try {
            const s = await getImportStatus(job.jobId);
            setProgress(s.progress);
            setStatusLabel(s.status);
            if (s.status === "completed" && s.assetId) {
              finish(s.assetId);
              return;
            }
            if (s.status === "failed") {
              if (
                sourcePlatform === "youtube" &&
                indicatesMissingYoutubeScope(s)
              ) {
                setNeedsConsent(true);
                setError("YouTube 플레이리스트 권한이 필요해요.");
              } else {
                setError(
                  s.errorMessage ??
                    "매칭에 실패했어요. 잠시 후 다시 시도해 주세요.",
                );
              }
              return;
            }
            timer = setTimeout(poll, 1000);
          } catch (error) {
            if (demoFixturesEnabled()) runDemo();
            else if (
              sourcePlatform === "youtube" &&
              indicatesMissingYoutubeScope(error)
            ) {
              setNeedsConsent(true);
              setError("YouTube 플레이리스트 권한이 필요해요.");
            } else {
              setError(
                messageFromError(
                  error,
                  "가져오기 상태를 확인하지 못했어요.",
                ),
              );
            }
          }
        };
        poll();
      } catch (error) {
        if (demoFixturesEnabled()) runDemo();
        else if (
          sourcePlatform === "youtube" &&
          indicatesMissingYoutubeScope(error)
        ) {
          setNeedsConsent(true);
          setError("YouTube 플레이리스트 권한이 필요해요.");
        } else {
          setError(
            messageFromError(error, "가져오기를 시작하지 못했어요."),
          );
        }
      }
    };

    runLive();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, [playlistId, router, sourcePlatform]);

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
      : statusLabel === "matching" || statusLabel === "running"
        ? "정규화 중"
        : statusLabel === "completed"
          ? "완료"
          : "실패";
  const progressPath = `/import/${encodeURIComponent(playlistId)}/progress?${new URLSearchParams({
    sourcePlatform,
  })}`;
  const consentHref = `/api/auth/google/start?${new URLSearchParams({
    scope: "youtube",
    returnTo: progressPath,
  })}`;

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
              role="alert"
            >
              {error}
            </div>
            <p className="text-sm text-text-mid">
              {needsConsent
                ? "Google 계정에 YouTube 권한을 추가한 뒤 가져오기를 다시 시작할 수 있어요."
                : timedOut
                ? "작업은 백그라운드에서 계속될 수 있어요. 라이브러리에서 결과를 확인해주세요."
                : "일부 트랙은 ISRC 정보가 없어 매칭이 어려울 수 있어요."}
            </p>
            <div className="flex flex-wrap justify-center gap-3">
              {needsConsent ? (
                <a
                  href={consentHref}
                  className="inline-flex h-12 items-center rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-300 hover:bg-accent-hi active:bg-accent-press focus-ring"
                >
                  YouTube 권한 연결
                </a>
              ) : timedOut ? (
                <button
                  type="button"
                  onClick={() => router.push("/assets")}
                  className="inline-flex h-12 items-center rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-300 hover:bg-accent-hi active:bg-accent-press focus-ring"
                >
                  라이브러리에서 계속 확인
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => {
                    setError(null);
                    setNeedsConsent(false);
                    setTimedOut(false);
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
              )}
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

export default function ImportProgressPage() {
  return (
    <Suspense fallback={null}>
      <ImportProgressPageInner />
    </Suspense>
  );
}
