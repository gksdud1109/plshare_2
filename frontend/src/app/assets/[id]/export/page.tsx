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
  const startedRef = useRef(false);

  // Load asset metadata for the track titles.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const a = await getAsset(assetId);
        if (!cancelled) setAsset(a);
      } catch {
        if (!cancelled) setAsset(buildDemoAssetDetail(assetId));
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
        );
        setJobId(job.jobId);
        const poll = async () => {
          if (cancelled) return;
          try {
            const s = await getExportStatus(job.jobId);
            setStatus(s);
            if (s.status === "completed") {
              finish(job.jobId);
              return;
            }
            if (s.status === "failed") {
              return;
            }
            timer = setTimeout(poll, 1100);
          } catch {
            runDemo(job.jobId);
          }
        };
        poll();
      } catch {
        runDemo();
      }
    };

    runLive();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, [assetId, router]);

  const trackById = new Map((asset?.tracks ?? []).map((t) => [t.id, t]));

  return (
    <PageShell>
      <header className="max-w-3xl py-12 md:py-16">
        <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
          Step 3 · Export
        </p>
        <h1 className="mt-4 text-4xl leading-tight md:text-5xl">
          Apple Music으로 내보낼
          <br />
          <em className="font-light italic text-ink-600">준비가 됐습니다.</em>
        </h1>
        {mode === "demo" ? (
          <p className="mt-6 inline-flex rounded-full border border-stone-200 bg-bone-100 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-ink-500">
            Demo data · 백엔드 연결 전
          </p>
        ) : null}
      </header>

      <section className="grid grid-cols-1 gap-10 md:grid-cols-[minmax(0,1fr)_360px]">
        <div className="rounded-[var(--radius-card)] border border-stone-200 bg-bone-100 px-6 md:px-8">
          {(status?.mappings ?? []).length === 0 ? (
            <div className="py-12">
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

        <aside className="rounded-[var(--radius-card)] border border-stone-200 bg-bone-100 p-6">
          <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
            Summary
          </p>
          <div className="mt-4 space-y-3 text-sm text-ink-700">
            <div className="flex justify-between">
              <span>전체 트랙</span>
              <span className="tabular-nums">{status?.totalTracks ?? "—"}</span>
            </div>
            <div className="flex justify-between">
              <span>매칭됨</span>
              <span className="tabular-nums text-sage-500">
                {status?.matchedTracks ?? "—"}
              </span>
            </div>
            <div className="flex justify-between">
              <span>실패</span>
              <span className="tabular-nums text-clay-500">
                {status?.failedTracks ?? "—"}
              </span>
            </div>
          </div>
          <ProgressNarrative
            className="mt-8 text-base"
            messages={NARRATIVE}
            active={status?.status !== "completed"}
            intervalMs={2400}
          />
          {jobId ? (
            <p className="mt-6 break-all text-[0.6875rem] tracking-wide text-ink-400">
              job · {jobId}
            </p>
          ) : null}
        </aside>
      </section>
    </PageShell>
  );
}
