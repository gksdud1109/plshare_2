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
    <main className="mx-auto flex min-h-screen w-full max-w-3xl flex-col items-center justify-center gap-6 bg-bone-50 px-6 text-center">
      <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
        Spotify
      </p>
      <ProgressNarrative
        messages={[
          "Spotify에 연결하고 있어요…",
          "당신의 라이브러리를 불러오는 중이에요…",
        ]}
        intervalMs={750}
      />
      <p className="text-xs tracking-wide text-ink-400">
        잠시만요, 곧 라이브러리로 안내할게요.
      </p>
    </main>
  );
}
