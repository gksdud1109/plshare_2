"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { getExportResult } from "@/lib/api/exports";
import { demoExportResult } from "@/lib/api/fixtures";
import type { ExportResult } from "@/types/asset";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";

type State =
  | { kind: "loading" }
  | { kind: "ready"; data: ExportResult; usingFixture: boolean };

export default function ExportResultPage() {
  const params = useParams<{ id: string }>();
  const search = useSearchParams();
  const jobId = search.get("jobId");
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!jobId) {
        if (!cancelled)
          setState({ kind: "ready", data: demoExportResult, usingFixture: true });
        return;
      }
      try {
        const data = await getExportResult(jobId);
        if (!cancelled)
          setState({ kind: "ready", data, usingFixture: false });
      } catch {
        if (!cancelled)
          setState({ kind: "ready", data: demoExportResult, usingFixture: true });
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

  const r = state.data;
  const total = r.matchedTracks + r.failedTracks;

  return (
    <PageShell>
      <section className="flex min-h-[60vh] max-w-3xl flex-col justify-center py-16">
        <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
          Done
        </p>
        <h1 className="mt-4 text-4xl leading-tight md:text-6xl">
          Apple Music에
          <br />
          <em className="font-light italic text-ink-600">옮겨두었어요.</em>
        </h1>
        <p className="mt-8 max-w-md font-serif text-xl italic text-ink-600">
          {total > 0
            ? `전체 ${total}곡 중 ${r.matchedTracks}곡이 매칭되었어요${
                r.failedTracks > 0 ? `, ${r.failedTracks}곡은 찾지 못했어요.` : "."
              }`
            : "준비가 끝났어요."}
        </p>

        <div className="mt-12 flex flex-wrap gap-3">
          <a
            href={r.externalUrl || "https://music.apple.com"}
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-full bg-ink-900 px-5 py-2.5 text-sm tracking-wide text-bone-50 transition-colors duration-500 hover:bg-ink-700"
          >
            Apple Music에서 열기
          </a>
          <Link
            href="/assets"
            className="rounded-full border border-stone-300 px-5 py-2.5 text-sm text-ink-700 transition-colors duration-500 hover:bg-bone-100"
          >
            다른 자산 보기
          </Link>
          <Link
            href={`/assets/${params.id}`}
            className="rounded-full px-5 py-2.5 text-sm text-ink-500 transition-colors duration-500 hover:text-ink-900"
          >
            돌아가기
          </Link>
        </div>

        {state.usingFixture ? (
          <p className="mt-10 inline-flex rounded-full border border-stone-200 bg-bone-100 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-ink-500">
            Demo data · 백엔드 연결 전
          </p>
        ) : null}
      </section>
    </PageShell>
  );
}
