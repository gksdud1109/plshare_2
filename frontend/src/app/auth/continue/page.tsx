"use client";

import { useEffect } from "react";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";

function safeReturnPath(value: string | null): string {
  return value?.startsWith("/") && !value.startsWith("//") ? value : "/import";
}

/**
 * Login interstitial reached when middleware gates an unauthenticated request to
 * a protected route (/assets, /import). The redirect to the *external* OAuth
 * start happens here in a client effect — NOT in the middleware — so Next.js
 * route prefetches (which render this page on the server but never run effects)
 * cannot bounce through the external provider. Real navigations still auto-
 * forward seamlessly; only prefetch probes stop at this internal shell.
 */
export default function AuthContinuePage() {
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const returnTo = safeReturnPath(params.get("returnTo"));
    const scope = params.get("scope");

    const target = new URL("/api/auth/google/start", window.location.origin);
    target.searchParams.set("returnTo", returnTo);
    if (scope === "youtube") target.searchParams.set("scope", scope);

    window.location.assign(`${target.pathname}${target.search}`);
  }, []);

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-bg px-6">
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

      <div
        className="glass relative z-10 flex w-full max-w-sm flex-col items-center gap-6 rounded-[18px] px-8 py-10 text-center animate-fade-up"
        style={{ boxShadow: "var(--shadow-pop)" }}
      >
        <p
          className="text-text-low"
          style={{
            fontSize: "0.75rem",
            fontWeight: 600,
            letterSpacing: "0.12em",
            textTransform: "uppercase",
          }}
        >
          plshare
        </p>

        <div className="flex items-center gap-2">
          <span
            className="inline-block h-2 w-2 rounded-full bg-accent animate-pulse-glow"
            aria-hidden
          />
          <span className="text-sm font-medium text-text-hi">로그인 안내 중</span>
        </div>

        <ProgressNarrative
          messages={[
            "안전하게 로그인 화면으로 옮기고 있어요…",
            "잠시만 기다려 주세요…",
          ]}
          intervalMs={750}
        />

        <noscript>
          <p className="text-text-low" style={{ fontSize: "0.8125rem" }}>
            계속하려면 자바스크립트를 켜고 새로고침해 주세요.
          </p>
        </noscript>
      </div>
    </main>
  );
}
