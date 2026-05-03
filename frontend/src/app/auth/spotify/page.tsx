"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";

export default function SpotifyAuthPage() {
  const router = useRouter();

  useEffect(() => {
    const t = setTimeout(() => router.push("/import"), 1500);
    return () => clearTimeout(t);
  }, [router]);

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-3xl flex-col items-center justify-center gap-6 px-6 text-center">
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
