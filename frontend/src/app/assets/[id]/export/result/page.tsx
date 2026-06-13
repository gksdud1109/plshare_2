"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { getExportResult } from "@/lib/api/exports";
import { demoExportResult } from "@/lib/api/fixtures";
import type { ExportResult } from "@/types/asset";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { demoFixturesEnabled } from "@/lib/demo";

type State =
  | { kind: "loading" }
  | { kind: "ready"; data: ExportResult; usingFixture: boolean }
  | { kind: "error"; message: string };

export default function ExportResultPage() {
  const params = useParams<{ id: string }>();
  const search = useSearchParams();
  const jobId = search.get("jobId");
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!jobId) {
        if (!cancelled && demoFixturesEnabled())
          setState({ kind: "ready", data: demoExportResult, usingFixture: true });
        else if (!cancelled)
          setState({ kind: "error", message: "내보내기 작업 ID가 없습니다." });
        return;
      }
      try {
        const data = await getExportResult(jobId);
        if (!cancelled)
          setState({ kind: "ready", data, usingFixture: false });
      } catch {
        if (!cancelled && demoFixturesEnabled())
          setState({ kind: "ready", data: demoExportResult, usingFixture: true });
        else if (!cancelled)
          setState({ kind: "error", message: "내보내기 결과를 불러오지 못했어요." });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [jobId]);

  if (state.kind === "loading") {
    return (
      <PageShell>
        <div className="py-32">
          <ProgressNarrative messages={["결과를 정리하는 중이에요…"]} />
        </div>
      </PageShell>
    );
  }

  if (state.kind === "error") {
    return <PageShell><p className="py-20 text-danger">{state.message}</p></PageShell>;
  }

  const r = state.data;
  const total = r.matchedTracks + r.failedTracks;

  return (
    <PageShell>
      {/* Success ambient glow — accent radial */}
      <div
        className="accent-glow pointer-events-none fixed left-1/2 top-1/3 h-[700px] w-[700px] -translate-x-1/2 -translate-y-1/2 opacity-30"
        aria-hidden="true"
      />

      <section className="relative flex min-h-[70vh] max-w-3xl flex-col justify-center py-16">
        {/* Success indicator */}
        <div className="mb-8 flex items-center gap-3">
          <div
            className="flex h-12 w-12 items-center justify-center rounded-full bg-accent"
            style={{ boxShadow: "var(--shadow-glow)" }}
          >
            <svg
              width="22"
              height="22"
              viewBox="0 0 24 24"
              fill="none"
              stroke="white"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            Done
          </p>
        </div>

        <h1
          className="font-display text-text-hi animate-fade-up"
          style={{ fontSize: "clamp(2.5rem, 6vw, 4.5rem)", fontWeight: 800, letterSpacing: "-0.03em", lineHeight: 1.05 }}
        >
          Apple Music에
          <br />
          <span className="text-accent">옮겨두었어요.</span>
        </h1>

        {/* Match summary */}
        <div
          className="glass mt-10 inline-flex w-fit items-center gap-6 rounded-[var(--radius-card)] px-6 py-4 animate-fade-up"
          style={{ animationDelay: "120ms" }}
        >
          <div className="text-center">
            <p
              className="text-3xl font-bold tabular-nums text-success"
              style={{ fontFeatureSettings: "'tnum'" }}
            >
              {r.matchedTracks}
            </p>
            <p className="mt-0.5 text-xs text-text-low uppercase tracking-[0.12em]">매칭됨</p>
          </div>
          <div className="h-8 w-px bg-hairline" />
          <div className="text-center">
            <p
              className="text-3xl font-bold tabular-nums text-text-mid"
              style={{ fontFeatureSettings: "'tnum'" }}
            >
              {total}
            </p>
            <p className="mt-0.5 text-xs text-text-low uppercase tracking-[0.12em]">전체</p>
          </div>
          {r.failedTracks > 0 && (
            <>
              <div className="h-8 w-px bg-hairline" />
              <div className="text-center">
                <p
                  className="text-3xl font-bold tabular-nums text-danger"
                  style={{ fontFeatureSettings: "'tnum'" }}
                >
                  {r.failedTracks}
                </p>
                <p className="mt-0.5 text-xs text-text-low uppercase tracking-[0.12em]">실패</p>
              </div>
            </>
          )}
        </div>

        <p className="mt-6 text-base text-text-mid animate-fade-up" style={{ animationDelay: "180ms" }}>
          {total > 0
            ? `전체 ${total}곡 중 ${r.matchedTracks}곡이 매칭되었어요${
                r.failedTracks > 0 ? `, ${r.failedTracks}곡은 찾지 못했어요.` : "."
              }`
            : "준비가 끝났어요."}
        </p>

        <div className="mt-10 flex flex-wrap gap-3 animate-fade-up" style={{ animationDelay: "240ms" }}>
          <a
            href={r.externalUrl || "https://music.apple.com"}
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-200 hover:bg-accent-hi hover:-translate-y-0.5 focus-ring"
            style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
          >
            Apple Music에서 열기
          </a>
          <Link
            href="/assets"
            className="glass rounded-full border-hairline-strong px-6 text-sm font-semibold text-text-hi transition-all duration-200 hover:bg-surface-3 hover:-translate-y-0.5 focus-ring"
            style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
          >
            다른 자산 보기
          </Link>
          <Link
            href={`/assets/${params.id}`}
            className="rounded-full px-6 text-sm text-text-mid transition-all duration-200 hover:text-text-hi focus-ring"
            style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
          >
            돌아가기
          </Link>
        </div>

        {state.usingFixture ? (
          <p className="mt-10 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
            Demo data · 백엔드 연결 전
          </p>
        ) : null}
      </section>
    </PageShell>
  );
}
