"use client";

import type { GiftView, WrapSkin } from "@/types/gift";
import { WRAP_SKINS } from "@/types/gift";

/**
 * 포장된 선물 표지 — 언박싱 전 게이트.
 * wrap skin 색으로 칠해진 '리본 포장' 위에 흐릿한 커버를 두고, "선물 열기"를
 * 탭하면 [UnboxingView]의 순차 공개 + 인라인 재생으로 넘어간다.
 */
export function WrappedCover({
  gift,
  onOpen,
  opening,
}: {
  gift: GiftView;
  onOpen: () => void;
  opening: boolean;
}) {
  const skin: WrapSkin =
    WRAP_SKINS.find((s) => s.key === gift.wrapSkin) ?? WRAP_SKINS[0];

  return (
    <div
      className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden px-6"
      style={{ background: "#0b0b0f" }}
    >
      {/* wrap skin 풀블리드 배경 */}
      <div aria-hidden className="pointer-events-none fixed inset-0" style={{ zIndex: 0 }}>
        <div className="absolute inset-0" style={{ background: skin.gradient, opacity: 0.3 }} />
        <div
          className="absolute inset-0"
          style={{
            background:
              "linear-gradient(to bottom, rgba(11,11,15,0.35) 0%, rgba(11,11,15,0.85) 100%)",
          }}
        />
      </div>
      <div
        aria-hidden
        className="animate-pulse-glow pointer-events-none fixed left-1/2 top-1/3 h-[640px] w-[640px] -translate-x-1/2 -translate-y-1/2 rounded-full"
        style={{
          background: `radial-gradient(circle, ${skin.accentColor} 0%, transparent 70%)`,
          opacity: 0.22,
          zIndex: 0,
        }}
      />

      <div
        className="relative flex flex-col items-center text-center animate-fade-up"
        style={{ zIndex: 10 }}
      >
        {/* 보낸 사람 */}
        {gift.sender.avatarUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={gift.sender.avatarUrl}
            alt={gift.sender.displayName}
            className="mb-5 h-16 w-16 rounded-full object-cover ring-2"
            style={{ ["--tw-ring-color" as string]: skin.accentColor }}
          />
        ) : (
          <div
            className="mb-5 flex h-16 w-16 items-center justify-center rounded-full text-2xl font-bold"
            style={{ background: skin.gradient, color: skin.accentColor }}
          >
            {gift.sender.displayName[0]}
          </div>
        )}
        <p className="text-sm text-text-low">
          <span style={{ color: skin.accentColor }} className="font-semibold">
            @{gift.sender.handle}
          </span>
          님이 보낸 선물
        </p>

        {/* 포장된 커버 — 흐릿한 커버 + 리본 십자 */}
        <div className="relative mt-8 mb-9 h-56 w-56">
          <div
            className="animate-pulse-glow absolute inset-0 rounded-3xl"
            style={{ background: skin.gradient, opacity: 0.6, filter: "blur(28px)" }}
            aria-hidden
          />
          <div
            className="relative h-56 w-56 overflow-hidden rounded-3xl"
            style={{
              boxShadow: "var(--shadow-pop)",
              border: `1px solid color-mix(in srgb, ${skin.accentColor} 45%, transparent)`,
            }}
          >
            {gift.asset.coverUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={gift.asset.coverUrl}
                alt=""
                className="h-full w-full object-cover"
                style={{ filter: "blur(9px) brightness(0.65)" }}
              />
            ) : (
              <div className="h-full w-full" style={{ background: skin.gradient }} />
            )}
            <div
              className="absolute inset-0"
              style={{
                background: `linear-gradient(135deg, color-mix(in srgb, ${skin.accentColor} 28%, transparent), transparent 60%)`,
              }}
              aria-hidden
            />
            {/* 리본 */}
            <div
              className="absolute inset-x-0 top-1/2 -translate-y-1/2"
              style={{ height: 13, background: skin.accentColor, opacity: 0.92 }}
              aria-hidden
            />
            <div
              className="absolute inset-y-0 left-1/2 -translate-x-1/2"
              style={{ width: 13, background: skin.accentColor, opacity: 0.92 }}
              aria-hidden
            />
          </div>
        </div>

        {gift.dedicationTo && (
          <p
            className="mb-2 font-display text-text-hi"
            style={{ fontSize: "clamp(1.1rem, 2.5vw, 1.4rem)", fontWeight: 700 }}
          >
            {gift.dedicationTo}
          </p>
        )}
        <p className="mb-1 text-[0.6875rem] uppercase tracking-[0.2em] text-text-low">
          도착한 선물
        </p>
        <h1
          className="font-display text-text-hi"
          style={{ fontSize: "clamp(1.4rem, 3vw, 1.9rem)", fontWeight: 700 }}
        >
          열어볼 준비가 됐어요
        </h1>

        <button
          type="button"
          onClick={onOpen}
          disabled={opening}
          className="mt-8 rounded-full px-10 text-sm font-semibold transition-all duration-200 hover:-translate-y-0.5 disabled:opacity-70"
          style={{
            height: 52,
            display: "inline-flex",
            alignItems: "center",
            background: skin.accentColor,
            color: "#0b0b0f",
            boxShadow: `0 0 32px -6px ${skin.accentColor}`,
          }}
        >
          {opening ? "여는 중…" : "선물 열기"}
        </button>
      </div>
    </div>
  );
}
