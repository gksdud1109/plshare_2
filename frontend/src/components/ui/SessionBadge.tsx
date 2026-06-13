"use client";

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
        className="h-3 w-16 animate-pulse rounded-full bg-surface-3"
      />
    );
  }

  if (!authenticated) {
    return (
      /* Backend redirect endpoint (OAuth handshake + 302) — plain <a>, never prefetched. */
      <a
        href="/api/auth/google/start?returnTo=%2Fauth%2Fspotify%3Flive%3D1%26next%3D%2Fimport"
        className="inline-flex items-center rounded-full bg-accent px-4 py-1.5 text-xs font-semibold text-white transition-colors duration-200 hover:bg-accent-hi active:bg-accent-press"
      >
        Google로 시작
      </a>
    );
  }

  return (
    <div className="flex items-center gap-3 text-xs tracking-wide">
      <span className="inline-flex items-center gap-1.5 font-medium text-success">
        <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden />
        연결됨
      </span>
      <button
        type="button"
        onClick={logout}
        disabled={loggingOut}
        className="text-text-mid transition-colors duration-200 hover:text-text-hi disabled:cursor-wait disabled:opacity-50"
      >
        {loggingOut ? "해제 중…" : "로그아웃"}
      </button>
    </div>
  );
}
