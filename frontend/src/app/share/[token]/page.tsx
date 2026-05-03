"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { getShared } from "@/lib/api/share";
import { buildDemoSharedAsset } from "@/lib/api/fixtures";
import type { SharedAsset } from "@/types/asset";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { TrackRow } from "@/components/ui/TrackRow";

type State =
  | { kind: "loading" }
  | { kind: "ready"; data: SharedAsset; usingFixture: boolean };

export default function SharePage() {
  const params = useParams<{ token: string }>();
  const token = params.token;
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getShared(token);
        if (!cancelled) setState({ kind: "ready", data, usingFixture: false });
      } catch {
        if (!cancelled)
          setState({
            kind: "ready",
            data: buildDemoSharedAsset(token),
            usingFixture: true,
          });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token]);

  return (
    <div className="min-h-screen">
      <nav className="mx-auto flex w-full max-w-5xl items-center justify-between px-6 py-6 md:px-10">
        <Link href="/" className="font-display text-lg tracking-tight">
          plshare
        </Link>
        <Link
          href="/"
          className="rounded-full border border-ink-900 px-4 py-2 text-xs tracking-wide text-ink-900 transition-colors duration-500 hover:bg-ink-900 hover:text-bone-50"
        >
          내 라이브러리 만들기
        </Link>
      </nav>

      <main className="mx-auto w-full max-w-5xl px-6 pb-20 md:px-10">
        {state.kind === "loading" ? (
          <div className="py-32">
            <ProgressNarrative messages={["공유된 자산을 불러오는 중이에요…"]} />
          </div>
        ) : (
          <ShareView data={state.data} usingFixture={state.usingFixture} />
        )}
      </main>
    </div>
  );
}

function ShareView({
  data,
  usingFixture,
}: {
  data: SharedAsset;
  usingFixture: boolean;
}) {
  return (
    <article>
      {usingFixture ? (
        <p className="mb-6 inline-flex rounded-full border border-stone-200 bg-bone-100 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-ink-500">
          Demo data · 백엔드 연결 전
        </p>
      ) : null}

      <section className="grid grid-cols-1 gap-10 py-8 md:grid-cols-[minmax(0,360px)_1fr] md:gap-14">
        <div className="aspect-square w-full overflow-hidden rounded-[var(--radius-card)] border border-stone-200 bg-stone-200 shadow-[0_24px_60px_-30px_rgba(20,18,16,0.35)]">
          {data.coverUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={data.coverUrl}
              alt={data.title}
              className="h-full w-full object-cover"
            />
          ) : null}
        </div>

        <div>
          <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
            Shared
          </p>
          <h1 className="mt-3 text-4xl leading-tight md:text-5xl">
            {data.title}
          </h1>
          {data.description ? (
            <p className="mt-6 font-serif text-xl italic text-ink-600">
              {data.description}
            </p>
          ) : null}

          {data.emotionTags.length > 0 ? (
            <div className="mt-6 flex flex-wrap gap-2">
              {data.emotionTags.map((t) => (
                <span
                  key={t}
                  className="rounded-full border border-stone-200 bg-bone-100 px-3 py-1 text-xs text-ink-600"
                >
                  {t}
                </span>
              ))}
            </div>
          ) : null}

          {data.diaryText ? (
            <div className="mt-8 max-w-md whitespace-pre-line border-l border-stone-300 pl-4 text-[0.95rem] leading-relaxed text-ink-700">
              {data.diaryText}
            </div>
          ) : null}
        </div>
      </section>

      <section className="mt-12">
        <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
          Tracks
        </p>
        <div className="mt-4 rounded-[var(--radius-card)] border border-stone-200 bg-bone-100 px-6 md:px-8">
          {data.tracks.map((t, i) => (
            <TrackRow key={t.id} track={t} index={i} />
          ))}
        </div>
      </section>
    </article>
  );
}
