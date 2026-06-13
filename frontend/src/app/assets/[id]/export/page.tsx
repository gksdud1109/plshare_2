"use client";

import { useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  getExportStatus,
  startExport,
} from "@/lib/api/exports";
import { getAsset } from "@/lib/api/assets";
import { makeIdempotencyKey } from "@/lib/api/client";
import {
  buildDemoAssetDetail,
  buildDemoExportStatus,
} from "@/lib/api/fixtures";
import type { AssetDetail, ExportJobStatus } from "@/types/asset";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { MatchConfidenceBadge } from "@/components/ui/MatchConfidenceBadge";
import { TrackRow } from "@/components/ui/TrackRow";
import { demoFixturesEnabled } from "@/lib/demo";

const NARRATIVE = [
  "Apple Music에서 같은 트랙을 찾고 있어요…",
  "ISRC가 일치하는 곡을 우선 선택하고 있어요…",
  "대체 가능한 트랙도 살펴보고 있어요…",
];

type Mode = "live" | "demo";

export default function ExportPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const assetId = params.id;

  const [asset, setAsset] = useState<AssetDetail | null>(null);
  const [status, setStatus] = useState<ExportJobStatus | null>(null);
  const [jobId, setJobId] = useState<string | null>(null);
  const [mode, setMode] = useState<Mode>("live");
  const [error, setError] = useState<string | null>(null);
  const startedRef = useRef(false);

  // Load asset metadata for the track titles.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const a = await getAsset(assetId);
        if (!cancelled) setAsset(a);
      } catch {
        if (!cancelled && demoFixturesEnabled()) setAsset(buildDemoAssetDetail(assetId));
        else if (!cancelled) setError("자산 정보를 불러오지 못했어요.");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [assetId]);

  // Kick off the export job + poll.
  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | null = null;
    let demoStep = 0;

    const finish = (id: string) => {
      if (cancelled) return;
      router.push(`/assets/${assetId}/export/result?jobId=${id}`);
    };

    const runDemo = (existingJobId?: string) => {
      setMode("demo");
      const fakeId = existingJobId ?? `demo-export-${assetId}`;
      setJobId(fakeId);
      const tick = () => {
        if (cancelled) return;
        const snap = buildDemoExportStatus(demoStep);
        setStatus(snap);
        if (snap.status === "completed") {
          finish(fakeId);
          return;
        }
        demoStep = Math.min(demoStep + 1, snap.totalTracks);
        timer = setTimeout(tick, 700);
      };
      tick();
    };

    const runLive = async () => {
      try {
        const job = await startExport(
          assetId,
          makeIdempotencyKey(`export-${assetId}`),
          "apple",
        );
        setJobId(job.jobId);
        const poll = async () => {
          if (cancelled) return;
          try {
            const s = await getExportStatus(job.jobId);
            setStatus(s);
            // completed / partial / failed are all terminal — navigate to result page
            if (s.status === "completed" || s.status === "partial") {
              finish(job.jobId);
              return;
            }
            if (s.status === "failed") {
              finish(job.jobId);
              return;
            }
            timer = setTimeout(poll, 1100);
          } catch {
            if (demoFixturesEnabled()) runDemo(job.jobId);
            else setError("내보내기 상태를 확인하지 못했어요.");
          }
        };
        poll();
      } catch {
        if (demoFixturesEnabled()) runDemo();
        else setError("내보내기를 시작하지 못했어요.");
      }
    };

    runLive();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, [assetId, router]);

  const trackById = new Map((asset?.tracks ?? []).map((t) => [t.id, t]));

  if (error) {
    return (
      <PageShell>
        <p className="py-20 text-danger">{error}</p>
      </PageShell>
    );
  }

  // Progress ratio for the accent bar
  const total = status?.totalTracks ?? 0;
  const matched = status?.matchedTracks ?? 0;
  const progressPct = total > 0 ? Math.round((matched / total) * 100) : 0;

  return (
    <PageShell>
      {/* Page accent glow */}
      <div
        className="accent-glow pointer-events-none fixed right-0 top-1/4 h-[500px] w-[500px] translate-x-1/2 opacity-30"
        aria-hidden="true"
      />

      <header className="relative max-w-3xl py-12 md:py-16">
        <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
          Step 3 · Export
        </p>
        <h1
          className="mt-4 font-display text-text-hi"
          style={{ fontSize: "clamp(2rem, 4vw, 3rem)", fontWeight: 700, letterSpacing: "-0.02em" }}
        >
          Apple Music으로 내보낼
          <br />
          <span className="text-text-mid font-normal" style={{ fontWeight: 400 }}>준비가 됐습니다.</span>
        </h1>
        {mode === "demo" ? (
          <p className="mt-6 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
            Demo data · 백엔드 연결 전
          </p>
        ) : null}

        {/* Accent progress bar */}
        {total > 0 && (
          <div className="mt-8 h-1 w-full max-w-md overflow-hidden rounded-full bg-surface-2">
            <div
              className="h-full rounded-full bg-accent transition-all duration-500"
              style={{
                width: `${progressPct}%`,
                boxShadow: "var(--shadow-glow)",
              }}
            />
          </div>
        )}
      </header>

      <section className="grid grid-cols-1 gap-6 md:grid-cols-[minmax(0,1fr)_300px]">
        {/* Track mapping list */}
        <div
          className="border border-hairline bg-surface-1"
          style={{ borderRadius: "var(--radius-card)", boxShadow: "var(--shadow-card)" }}
        >
          {(status?.mappings ?? []).length === 0 ? (
            <div className="py-12 px-8">
              <ProgressNarrative messages={NARRATIVE} intervalMs={2200} />
            </div>
          ) : (
            (status?.mappings ?? []).map((m, i) => {
              const t =
                trackById.get(m.trackId) ?? {
                  id: m.trackId,
                  name: "Unknown track",
                  artist: "—",
                  durationMs: 0,
                };
              return (
                <TrackRow
                  key={m.trackId}
                  track={t}
                  index={i}
                  trailing={<MatchConfidenceBadge status={m.status} />}
                />
              );
            })
          )}
        </div>

        {/* Summary sidebar */}
        <aside
          className="glass p-6"
          style={{ borderRadius: "var(--radius-card)", boxShadow: "var(--shadow-card)" }}
        >
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            Summary
          </p>
          <div className="mt-4 space-y-3">
            <div className="flex justify-between text-sm">
              <span className="text-text-mid">전체 트랙</span>
              <span className="tabular-nums font-semibold text-text-hi">{status?.totalTracks ?? "—"}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-text-mid">매칭됨</span>
              <span className="tabular-nums font-semibold text-success">
                {status?.matchedTracks ?? "—"}
              </span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-text-mid">실패</span>
              <span className="tabular-nums font-semibold text-danger">
                {status?.failedTracks ?? "—"}
              </span>
            </div>
          </div>

          {/* Divider */}
          <div className="my-6 h-px bg-hairline" />

          <ProgressNarrative
            className="text-sm"
            messages={NARRATIVE}
            active={status?.status !== "completed"}
            intervalMs={2400}
          />
          {jobId ? (
            <p className="mt-6 break-all text-[0.6875rem] tracking-wide text-text-low">
              job · {jobId}
            </p>
          ) : null}
        </aside>
      </section>
    </PageShell>
  );
}
