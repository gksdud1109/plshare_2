"use client";

import { useEffect, useState } from "react";
import type { GiftTrack, GiftView, WrapSkin } from "@/types/gift";
import { WRAP_SKINS } from "@/types/gift";
import { useTrackPlayer } from "@/components/gift/useTrackPlayer";
import { PlayableTrackRow } from "@/components/gift/PlayableTrackRow";

/**
 * 언박싱 연출 컴포넌트.
 *
 * 진입 시:
 * 1. 포장 스킨 배경 + 메시지 페이드인.
 * 2. 트랙이 한 곡씩 순차 페이드-인 공개 (스프링 딜레이).
 * 3. "내 라이브러리에 저장" CTA.
 */
export function UnboxingView({
  gift,
  onSave,
  saving,
  saved,
  isAuthenticated,
}: {
  gift: GiftView;
  onSave: () => void;
  saving: boolean;
  saved: boolean;
  isAuthenticated: boolean;
}) {
  const skin: WrapSkin =
    WRAP_SKINS.find((s) => s.key === gift.wrapSkin) ?? WRAP_SKINS[0];

  // 트랙 순차 공개: 200ms 간격으로 한 곡씩 visibleCount 증가
  const [visibleCount, setVisibleCount] = useState(0);

  useEffect(() => {
    const tracks = gift.asset.tracks;
    if (visibleCount >= tracks.length) return;
    const timer = setTimeout(() => {
      setVisibleCount((n) => n + 1);
    }, visibleCount === 0 ? 800 : 350);
    return () => clearTimeout(timer);
  }, [visibleCount, gift.asset.tracks]);

  // 인라인 재생 (선물 언박싱 + 공유 페이지가 공유하는 훅).
  const player = useTrackPlayer();

  return (
    <div className="relative min-h-screen overflow-hidden" style={{ background: "#0b0b0f" }}>
      {/* Wrap skin fullbleed background glow */}
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0"
        style={{ zIndex: 0 }}
      >
        <div
          className="absolute inset-0"
          style={{
            background: skin.gradient,
            opacity: 0.18,
          }}
        />
        <div
          className="absolute inset-0"
          style={{
            background:
              "linear-gradient(to bottom, rgba(11,11,15,0.4) 0%, rgba(11,11,15,0.75) 50%, rgba(11,11,15,0.97) 100%)",
          }}
        />
      </div>

      {/* Accent radial glow from skin color */}
      <div
        aria-hidden="true"
        className="pointer-events-none fixed left-1/2 top-1/4 h-[700px] w-[700px] -translate-x-1/2 -translate-y-1/2 rounded-full opacity-20"
        style={{
          background: `radial-gradient(circle, ${skin.accentColor} 0%, transparent 70%)`,
          zIndex: 0,
        }}
      />

      <main
        className="relative mx-auto w-full max-w-2xl px-6 pb-24 pt-16 md:px-10"
        style={{ zIndex: 10 }}
      >
        {/* Sender + cover */}
        <section className="flex flex-col items-center text-center">
          {gift.sender.avatarUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={gift.sender.avatarUrl}
              alt={gift.sender.displayName}
              className="mb-4 h-14 w-14 rounded-full object-cover ring-2"
              style={{ ["--tw-ring-color" as string]: skin.accentColor }}
            />
          ) : (
            <div
              className="mb-4 flex h-14 w-14 items-center justify-center rounded-full text-xl font-bold"
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

          {/* Cover art */}
          {gift.asset.coverUrl && (
            <div className="relative mt-8 mb-6 w-48 h-48">
              <div
                className="absolute inset-0 rounded-2xl animate-pulse-glow"
                style={{
                  background: skin.gradient,
                  opacity: 0.5,
                  filter: "blur(24px)",
                }}
                aria-hidden="true"
              />
              <div
                className="relative overflow-hidden rounded-2xl"
                style={{ boxShadow: "var(--shadow-pop)" }}
              >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={gift.asset.coverUrl}
                  alt={gift.asset.title}
                  className="h-48 w-48 object-cover"
                />
              </div>
            </div>
          )}

          <h1
            className="font-display text-text-hi"
            style={{ fontSize: "clamp(1.5rem, 3vw, 2rem)", fontWeight: 700 }}
          >
            {gift.asset.title}
          </h1>

          {/* Message */}
          <blockquote
            className="mt-6 max-w-sm whitespace-pre-line rounded-xl border px-6 py-4 text-base leading-relaxed text-text animate-fade-up"
            style={{
              borderColor: skin.accentColor,
              background: `color-mix(in srgb, ${skin.accentColor} 8%, transparent)`,
            }}
          >
            {gift.message}
          </blockquote>
        </section>

        {/* Track list — sequential fade-in */}
        <section className="mt-12">
          <p
            className="mb-4 text-[0.6875rem] font-semibold uppercase tracking-[0.18em]"
            style={{ color: skin.accentColor }}
          >
            Tracks
          </p>
          <div
            className="overflow-hidden border border-hairline bg-surface-1"
            style={{ borderRadius: "var(--radius-card)" }}
          >
            {gift.asset.tracks.map((track: GiftTrack, i: number) => (
              <div
                key={track.id}
                className="border-b border-hairline last:border-b-0 transition-all duration-500"
                style={{
                  opacity: i < visibleCount ? 1 : 0,
                  transform: i < visibleCount ? "translateY(0)" : "translateY(10px)",
                  transitionDelay: `${i * 80}ms`,
                }}
                aria-hidden={i >= visibleCount}
              >
                <PlayableTrackRow
                  track={track}
                  index={i}
                  player={player}
                  accentColor={skin.accentColor}
                />
              </div>
            ))}
          </div>
        </section>

        {/* CTA */}
        <section className="mt-12 flex flex-col items-center gap-4 text-center">
          {isAuthenticated ? (
            saved ? (
              <p className="text-sm font-semibold" style={{ color: skin.accentColor }}>
                라이브러리에 저장됐어요
              </p>
            ) : (
              <button
                type="button"
                onClick={onSave}
                disabled={saving}
                className="rounded-full px-8 text-sm font-semibold text-white transition-all duration-200 hover:-translate-y-0.5 disabled:opacity-60"
                style={{
                  height: "48px",
                  display: "inline-flex",
                  alignItems: "center",
                  background: skin.accentColor,
                  color: "#0b0b0f",
                }}
              >
                {saving ? "저장 중…" : "내 라이브러리에 저장"}
              </button>
            )
          ) : (
            <a
              href="/import"
              className="rounded-full px-8 text-sm font-semibold transition-all duration-200 hover:-translate-y-0.5"
              style={{
                height: "48px",
                display: "inline-flex",
                alignItems: "center",
                background: skin.accentColor,
                color: "#0b0b0f",
              }}
            >
              내 라이브러리 만들기
            </a>
          )}
        </section>
      </main>
    </div>
  );
}
