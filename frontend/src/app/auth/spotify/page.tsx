"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || "http://localhost:8080";

export default function SpotifyAuthPage() {
  const router = useRouter();

  useEffect(() => {
    let cancelled = false;
    let redirectTimer: ReturnType<typeof setTimeout> | undefined;

    async function connectSpotify() {
      const isLocalDemo = ["localhost", "127.0.0.1"].includes(
        window.location.hostname,
      );
      const liveMode = new URLSearchParams(window.location.search).has("live");

      if (!isLocalDemo || liveMode) {
        redirectTimer = setTimeout(() => {
          window.location.assign(`${API_BASE_URL}/api/auth/spotify/start`);
        }, 1500);
        return;
      }

      const response = await fetch("/api/auth/session", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ demo: true, grantId: "demo-grant" }),
      });

      if (!response.ok) {
        throw new Error("Failed to create the demo session");
      }

      const elapsedDelay = new Promise((resolve) =>
        setTimeout(resolve, 1500),
      );
      await elapsedDelay;

      if (!cancelled) {
        router.push("/import");
      }
    }

    void connectSpotify();

    return () => {
      cancelled = true;
      if (redirectTimer) clearTimeout(redirectTimer);
    };
  }, [router]);

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-bg px-6">
      {/* Pulsing accent glow orb */}
      <div
        className="animate-pulse-glow pointer-events-none absolute inset-0 flex items-center justify-center"
        aria-hidden
      >
        <div
          className="h-[480px] w-[480px] rounded-full"
          style={{
            background:
              "radial-gradient(circle, rgba(124,92,255,0.30) 0%, rgba(124,92,255,0.10) 40%, transparent 70%)",
          }}
        />
      </div>

      {/* Secondary offset glow */}
      <div
        className="pointer-events-none absolute"
        style={{
          top: "30%",
          left: "55%",
          width: 280,
          height: 280,
          borderRadius: "50%",
          background:
            "radial-gradient(circle, rgba(155,130,255,0.14) 0%, transparent 70%)",
          animation: "plshare-pulse-glow 4s ease-in-out infinite",
          animationDelay: "1.5s",
        }}
        aria-hidden
      />

      {/* Content card */}
      <div
        className="glass relative z-10 flex w-full max-w-sm flex-col items-center gap-6 rounded-[18px] px-8 py-10 text-center animate-fade-up"
        style={{ boxShadow: "var(--shadow-pop)" }}
      >
        {/* Spotify eyebrow */}
        <p
          className="text-text-low"
          style={{
            fontSize: "0.75rem",
            fontWeight: 600,
            letterSpacing: "0.12em",
            textTransform: "uppercase",
          }}
        >
          Spotify
        </p>

        {/* Accent dot indicator */}
        <div className="flex items-center gap-2">
          <span
            className="inline-block h-2 w-2 rounded-full bg-accent animate-pulse-glow"
            aria-hidden
          />
          <span className="text-sm font-medium text-text-hi">연결 중</span>
        </div>

        <ProgressNarrative
          messages={[
            "Spotify에 연결하고 있어요…",
            "당신의 라이브러리를 불러오는 중이에요…",
          ]}
          intervalMs={750}
        />

        <p
          className="text-text-low"
          style={{ fontSize: "0.8125rem", letterSpacing: "0.02em" }}
        >
          잠시만요, 곧 라이브러리로 안내할게요.
        </p>

        {/* Slim accent progress bar — indeterminate shimmer */}
        <div
          className="w-full overflow-hidden rounded-full"
          style={{ height: 2, background: "var(--accent-soft)" }}
          aria-hidden
        >
          <div
            className="h-full rounded-full bg-accent"
            style={{
              width: "40%",
              animation:
                "plshare-indeterminate 1.8s ease-in-out infinite",
            }}
          />
        </div>
      </div>

      {/* Indeterminate bar keyframe (inline for self-containment) */}
      <style>{`
        @keyframes plshare-indeterminate {
          0%   { transform: translateX(-100%) scaleX(0.5); }
          50%  { transform: translateX(150%) scaleX(1); }
          100% { transform: translateX(300%) scaleX(0.5); }
        }
      `}</style>
    </main>
  );
}
