"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listAssets } from "@/lib/api/assets";
import { demoAssets } from "@/lib/api/fixtures";
import type { AssetSummary } from "@/types/asset";
import { AssetCard } from "@/components/ui/AssetCard";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";

type State =
  | { kind: "loading" }
  | { kind: "ready"; data: AssetSummary[]; usingFixture: boolean };

export default function AssetsPage() {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await listAssets();
        if (!cancelled) setState({ kind: "ready", data, usingFixture: false });
      } catch {
        if (!cancelled)
          setState({ kind: "ready", data: demoAssets, usingFixture: true });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <PageShell showHomeLink={false}>
      <header className="flex flex-wrap items-end justify-between gap-6 py-12 md:py-16">
        <div>
          <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
            Library
          </p>
          <h1 className="mt-3 text-4xl leading-tight md:text-5xl">
            나의 자산
          </h1>
        </div>
        <Link
          href="/import"
          className="rounded-full bg-ink-900 px-5 py-2.5 text-sm tracking-wide text-bone-50 transition-colors duration-500 hover:bg-ink-700"
        >
          + 새로 가져오기
        </Link>
      </header>

      {state.kind === "loading" ? (
        <ProgressNarrative
          messages={["라이브러리를 불러오는 중이에요…"]}
          intervalMs={2400}
        />
      ) : state.data.length === 0 ? (
        <div className="flex flex-col items-start gap-6 py-16">
          <p className="font-display text-2xl text-ink-700">
            아직 자산이 없어요.
          </p>
          <Link
            href="/import"
            className="rounded-full bg-ink-900 px-5 py-2.5 text-sm text-bone-50 transition-colors duration-500 hover:bg-ink-700"
          >
            첫 자산 만들기
          </Link>
        </div>
      ) : (
        <>
          {state.usingFixture ? (
            <p className="mb-6 inline-flex rounded-full border border-stone-200 bg-bone-100 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-ink-500">
              Demo data · 백엔드 연결 전
            </p>
          ) : null}
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {state.data.map((a, i) => (
              <Link
                key={a.id}
                href={`/assets/${a.id}`}
                className="animate-fade-up block"
                style={{ animationDelay: `${i * 60}ms` }}
              >
                <AssetCard asset={a} />
              </Link>
            ))}
          </div>
        </>
      )}
    </PageShell>
  );
}
