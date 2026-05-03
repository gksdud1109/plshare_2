"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { listPlaylists } from "@/lib/api/playlists";
import { demoPlaylists } from "@/lib/api/fixtures";
import type { SpotifyPlaylist } from "@/types/asset";
import { PlaylistCard } from "@/components/ui/PlaylistCard";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";

type State =
  | { kind: "loading" }
  | { kind: "ready"; data: SpotifyPlaylist[]; usingFixture: boolean }
  | { kind: "error"; message: string };

export default function ImportPage() {
  const router = useRouter();
  const [state, setState] = useState<State>({ kind: "loading" });
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await listPlaylists();
        if (!cancelled)
          setState({ kind: "ready", data, usingFixture: false });
      } catch {
        // BE unreachable — fall back to fixtures so the demo is still inspectable.
        if (!cancelled)
          setState({ kind: "ready", data: demoPlaylists, usingFixture: true });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  const retry = () => {
    setState({ kind: "loading" });
    setReloadKey((k) => k + 1);
  };

  const handleSelect = (id: string) => {
    router.push(`/import/${id}/progress`);
  };

  return (
    <PageShell>
      <header className="max-w-3xl py-12 md:py-16">
        <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
          Step 1 · Import
        </p>
        <h1 className="mt-4 text-4xl leading-tight md:text-5xl">
          어떤 플레이리스트를
          <br />
          <em className="font-light italic text-ink-600">가져올까요?</em>
        </h1>
        <p className="mt-6 max-w-xl text-base leading-relaxed text-ink-600">
          하나를 골라 자산으로 옮겨봅니다. 트랙 정보와 ISRC를 함께 보존합니다.
        </p>
      </header>

      {state.kind === "loading" ? (
        <div className="py-24">
          <ProgressNarrative
            messages={[
              "Spotify에서 플레이리스트를 가져오는 중이에요…",
              "곧 보여드릴게요.",
            ]}
          />
        </div>
      ) : null}

      {state.kind === "error" ? (
        <div className="flex flex-col items-start gap-4 py-16">
          <p className="text-base text-ink-600">{state.message}</p>
          <button
            type="button"
            onClick={retry}
            className="rounded-full border border-ink-900 px-5 py-2 text-sm text-ink-900 transition-colors duration-500 hover:bg-ink-900 hover:text-bone-50"
          >
            다시 시도
          </button>
        </div>
      ) : null}

      {state.kind === "ready" ? (
        <>
          {state.usingFixture ? (
            <p className="mb-6 inline-flex rounded-full border border-stone-200 bg-bone-100 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-ink-500">
              Demo data · 백엔드 연결 전
            </p>
          ) : null}
          {state.data.length === 0 ? (
            <p className="py-16 text-base text-ink-500">
              표시할 플레이리스트가 없습니다.
            </p>
          ) : (
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {state.data.map((p, i) => (
                <div
                  key={p.id}
                  className="animate-fade-up"
                  style={{ animationDelay: `${i * 60}ms` }}
                >
                  <PlaylistCard playlist={p} onSelect={handleSelect} />
                </div>
              ))}
            </div>
          )}
        </>
      ) : null}
    </PageShell>
  );
}
