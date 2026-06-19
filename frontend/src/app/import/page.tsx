"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  indicatesMissingYoutubeScope,
  listYoutubePlaylists,
} from "@/lib/api/playlists";
import { demoYoutubePlaylists } from "@/lib/api/fixtures";
import type { ImportSourcePlatform, PlaylistSummary } from "@/types/asset";
import { PlaylistCard } from "@/components/ui/PlaylistCard";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { demoFixturesEnabled } from "@/lib/demo";
import { messageFromError } from "@/lib/errors";

const PRIMARY_SOURCE: ImportSourcePlatform = "youtube";
const YOUTUBE_CONSENT_HREF =
  "/api/auth/google/start?scope=youtube&returnTo=%2Fimport";

type State =
  | { kind: "loading" }
  | { kind: "ready"; data: PlaylistSummary[]; usingFixture: boolean }
  | { kind: "consent" }
  | { kind: "error"; message: string };

export default function ImportPage() {
  const router = useRouter();
  const [state, setState] = useState<State>({ kind: "loading" });
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await listYoutubePlaylists();
        if (!cancelled)
          setState({ kind: "ready", data, usingFixture: false });
      } catch (error) {
        if (cancelled) return;
        if (demoFixturesEnabled()) {
          setState({
            kind: "ready",
            data: demoYoutubePlaylists,
            usingFixture: true,
          });
        } else if (indicatesMissingYoutubeScope(error)) {
          setState({ kind: "consent" });
        } else {
          setState({
            kind: "error",
            message: messageFromError(
              error,
              "YouTube 플레이리스트를 불러오지 못했어요.",
            ),
          });
        }
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
    const query = new URLSearchParams({ sourcePlatform: PRIMARY_SOURCE });
    router.push(`/import/${encodeURIComponent(id)}/progress?${query}`);
  };

  return (
    <PageShell>
      {/* Eyebrow + heading */}
      <header className="max-w-3xl pb-10 pt-12 md:pt-16 animate-fade-up">
        <p
          className="text-text-low"
          style={{
            fontSize: "0.75rem",
            fontWeight: 600,
            letterSpacing: "0.12em",
            textTransform: "uppercase",
          }}
        >
          Step 1 · Import
        </p>
        <h1
          className="mt-4 font-display text-text-hi"
          style={{
            fontSize: "clamp(2rem, 4vw, 3rem)",
            fontWeight: 700,
            letterSpacing: "-0.02em",
            lineHeight: 1.15,
          }}
        >
          어떤 플레이리스트를
          <br />
          <span className="text-accent">가져올까요?</span>
        </h1>
        <p className="mt-5 max-w-xl text-base leading-relaxed text-text-mid">
          YouTube와 YouTube Music에서 만든 목록을 골라 내 플레이리스트로 가져옵니다.
        </p>
      </header>

      {/* Loading state */}
      {state.kind === "loading" ? (
        <div
          className="flex flex-col items-start gap-6 py-20 animate-fade-up"
          style={{ animationDelay: "0.1s" }}
        >
          <ProgressNarrative
            messages={[
              "YouTube 플레이리스트를 불러오는 중이에요…",
              "곧 보여드릴게요.",
            ]}
          />
          {/* Skeleton grid */}
          <div className="mt-6 grid w-full grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="rounded-[18px] border"
                style={{
                  background: "var(--color-surface-1)",
                  borderColor: "rgba(255,255,255,0.08)",
                  aspectRatio: "1 / 1.2",
                  opacity: 0.5 - i * 0.06,
                }}
              />
            ))}
          </div>
        </div>
      ) : null}

      {/* YouTube incremental consent state */}
      {state.kind === "consent" ? (
        <div className="flex flex-col items-start gap-5 py-16 animate-fade-up">
          <div
            className="max-w-xl rounded-[18px] border px-5 py-5"
            style={{
              background: "var(--color-surface-1)",
              borderColor: "rgba(255,255,255,0.08)",
            }}
            role="status"
          >
            <h2 className="text-base font-semibold text-text-hi">
              YouTube 플레이리스트 권한이 필요해요
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-text-mid">
              Google 계정에 YouTube 권한을 추가하면 YouTube와 YouTube Music
              플레이리스트를 불러올 수 있어요.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <a
              href={YOUTUBE_CONSENT_HREF}
              className="inline-flex h-12 items-center rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-300 hover:bg-accent-hi active:bg-accent-press focus-ring"
            >
              YouTube 권한 연결
            </a>
            <button
              type="button"
              onClick={retry}
              className="glass inline-flex h-12 items-center rounded-full px-6 text-sm font-medium text-text-hi transition-colors duration-300 hover:border-hairline-strong focus-ring"
            >
              다시 확인
            </button>
          </div>
        </div>
      ) : null}

      {/* Error state */}
      {state.kind === "error" ? (
        <div className="flex flex-col items-start gap-5 py-16 animate-fade-up">
          <p
            className="rounded-[18px] border px-5 py-4 text-sm text-danger"
            style={{
              background: "rgba(251,113,133,0.08)",
              borderColor: "rgba(251,113,133,0.2)",
            }}
            role="alert"
          >
            {state.message}
          </p>
          <button
            type="button"
            onClick={retry}
            className="inline-flex h-12 items-center rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-300 hover:bg-accent-hi active:bg-accent-press focus-ring"
          >
            다시 시도
          </button>
        </div>
      ) : null}

      {/* Ready state */}
      {state.kind === "ready" ? (
        <div className="pb-20">
          {state.usingFixture ? (
            <p
              className="mb-6 inline-flex rounded-full border px-3 py-1"
              style={{
                fontSize: "0.6875rem",
                fontWeight: 600,
                letterSpacing: "0.12em",
                textTransform: "uppercase",
                color: "var(--color-text-low)",
                borderColor: "rgba(255,255,255,0.08)",
                background: "var(--color-surface-1)",
              }}
            >
              Demo data · 백엔드 연결 전
            </p>
          ) : null}

          {state.data.length === 0 ? (
            <div
              className="flex flex-col items-center gap-4 rounded-[18px] border py-24 text-center animate-fade-up"
              style={{
                borderColor: "rgba(255,255,255,0.08)",
                background: "var(--color-surface-1)",
              }}
            >
              <svg
                width="48"
                height="48"
                viewBox="0 0 48 48"
                fill="none"
                aria-hidden
              >
                <circle
                  cx="24"
                  cy="24"
                  r="20"
                  stroke="rgba(255,255,255,0.08)"
                  strokeWidth="2"
                />
                <path
                  d="M16 24h16M24 16v16"
                  stroke="var(--color-text-low)"
                  strokeWidth="2"
                  strokeLinecap="round"
                />
              </svg>
              <p className="text-sm text-text-mid">
                YouTube 또는 YouTube Music에 표시할 플레이리스트가 없습니다.
              </p>
            </div>
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
        </div>
      ) : null}
    </PageShell>
  );
}
