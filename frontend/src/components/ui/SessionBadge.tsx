"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

interface SessionResponse {
  authenticated: boolean;
}

export function SessionBadge() {
  const router = useRouter();
  const [authenticated, setAuthenticated] = useState<boolean | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    fetch("/api/auth/session", {
      cache: "no-store",
      signal: controller.signal,
    })
      .then((response) => response.json() as Promise<SessionResponse>)
      .then((data) => setAuthenticated(data.authenticated))
      .catch((error: unknown) => {
        if (!(error instanceof DOMException && error.name === "AbortError")) {
          setAuthenticated(false);
        }
      });

    return () => controller.abort();
  }, []);

  async function logout() {
    setLoggingOut(true);

    try {
      const response = await fetch("/api/auth/logout", { method: "POST" });
      if (!response.ok) throw new Error("Logout failed");

      setAuthenticated(false);
      router.replace("/");
      router.refresh();
    } finally {
      setLoggingOut(false);
    }
  }

  if (authenticated === null) {
    return (
      <span
        aria-label="세션 확인 중"
        className="h-3 w-16 animate-pulse rounded-full bg-stone-200"
      />
    );
  }

  if (!authenticated) {
    return (
      <Link
        href="/auth/spotify"
        className="text-xs tracking-wide text-ink-500 transition-colors duration-500 hover:text-ink-900"
      >
        Spotify 연결
      </Link>
    );
  }

  return (
    <div className="flex items-center gap-3 text-xs tracking-wide">
      <span className="inline-flex items-center gap-1.5 text-sage-500">
        <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden />
        연결됨
      </span>
      <button
        type="button"
        onClick={logout}
        disabled={loggingOut}
        className="text-ink-400 transition-colors duration-500 hover:text-ink-900 disabled:cursor-wait disabled:opacity-60"
      >
        {loggingOut ? "해제 중…" : "로그아웃"}
      </button>
    </div>
  );
}
