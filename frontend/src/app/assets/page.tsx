"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listAssets } from "@/lib/api/assets";
import { demoAssets } from "@/lib/api/fixtures";
import type { AssetSummary } from "@/types/asset";
import { AssetCard } from "@/components/ui/AssetCard";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { demoFixturesEnabled } from "@/lib/demo";

type State =
  | { kind: "loading" }
  | { kind: "ready"; data: AssetSummary[]; usingFixture: boolean }
  | { kind: "error"; message: string };

export default function AssetsPage() {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await listAssets();
        if (!cancelled) setState({ kind: "ready", data, usingFixture: false });
      } catch {
        if (!cancelled && demoFixturesEnabled())
          setState({ kind: "ready", data: demoAssets, usingFixture: true });
        else if (!cancelled)
          setState({ kind: "error", message: "라이브러리를 불러오지 못했어요." });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <PageShell showHomeLink={false}>
      {/* Page accent glow */}
      <div
        className="accent-glow pointer-events-none fixed left-1/4 top-0 h-[600px] w-[600px] -translate-x-1/2 opacity-40"
        aria-hidden="true"
      />

      <header className="relative flex flex-wrap items-end justify-between gap-6 py-12 md:py-16">
        <div>
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            Library
          </p>
          <h1
            className="mt-3 font-display text-text-hi"
            style={{ fontSize: "clamp(2rem, 4vw, 3rem)", fontWeight: 700, letterSpacing: "-0.02em" }}
          >
            나의 플레이리스트
          </h1>
        </div>
        <Link
          href="/import"
          className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white transition-all duration-200 hover:bg-accent-hi hover:-translate-y-0.5 focus-ring"
          style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
        >
          + 새로 가져오기
        </Link>
      </header>

      {state.kind === "loading" ? (
        <div className="py-24">
          <ProgressNarrative
            messages={["라이브러리를 불러오는 중이에요…"]}
            intervalMs={2400}
          />
        </div>
      ) : state.kind === "error" ? (
        <p className="py-20 text-danger">{state.message}</p>
      ) : state.data.length === 0 ? (
        <div className="flex flex-col items-start gap-6 py-20">
          <p className="text-2xl font-semibold text-text-mid">
            아직 플레이리스트가 없어요.
          </p>
          <p className="text-text-low text-sm">Spotify에서 첫 플레이리스트를 가져와보세요.</p>
          <Link
            href="/import"
            className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white transition-all duration-200 hover:bg-accent-hi hover:-translate-y-0.5 focus-ring"
            style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
          >
            첫 플레이리스트 만들기
          </Link>
        </div>
      ) : (
        <>
          {state.usingFixture ? (
            <p className="mb-6 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
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
