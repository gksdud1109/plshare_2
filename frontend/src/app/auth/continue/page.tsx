"use client";

import { useEffect } from "react";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";

function safeReturnPath(value: string | null): string {
  return value?.startsWith("/") && !value.startsWith("//") ? value : "/import";
}

/**
 * 모든 로그인 진입을 수렴시키는 단일 데모-aware 게이트.
 *
 * - 외부 OAuth 리다이렉트는 미들웨어가 아닌 이 클라이언트 effect 에서 일어난다 — Next.js
 *   prefetch(서버 렌더만, effect 미실행)가 외부 공급자로 튕기지 못하게 하기 위함.
 * - localhost(데모)면 외부 화면 없이 원클릭 데모 세션(쿠키)을 만들고 returnTo 로 복귀한다.
 * - 비-localhost 이거나 `?live` 면 실 Google OAuth(start)로 hop 하며 returnTo 를 보존한다.
 *
 * 홈 CTA·gift/send 로그인벽·SessionBadge·미들웨어가 모두 이 게이트를 가리켜 "어디서 로그인을
 * 시작해도 같은 경험"을 보장한다.
 */
export default function AuthContinuePage() {
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const returnTo = safeReturnPath(params.get("returnTo"));
    const scope = params.get("scope");
    const isLocalDemo = ["localhost", "127.0.0.1"].includes(
      window.location.hostname,
    );
    const liveMode = params.has("live");

    async function proceed() {
      if (isLocalDemo && !liveMode) {
        // 데모: 외부 OAuth 없이 원클릭 데모 세션(httpOnly 쿠키, BE 가 실 서명 토큰 발급) → 복귀.
        try {
          await fetch("/api/auth/session", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ demo: true, grantId: "demo-grant" }),
          });
        } catch {
          // 쿠키 세팅 실패해도 returnTo 로 진행 — GET /api/auth/session 의 데모 폴백이 신원 해석.
        }
        window.location.assign(returnTo); // 하드 네비로 세션 재평가 보장
        return;
      }

      // 실인증(prod 또는 ?live): Google OAuth start 로 hop, returnTo 보존.
      const target = new URL("/api/auth/google/start", window.location.origin);
      target.searchParams.set("returnTo", returnTo);
      if (scope === "youtube") target.searchParams.set("scope", scope);
      window.location.assign(`${target.pathname}${target.search}`);
    }

    void proceed();
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
