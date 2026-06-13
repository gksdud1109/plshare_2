"use client";

import { useEffect, useState } from "react";
import type { GiftTrack, GiftView, WrapSkin } from "@/types/gift";
import { WRAP_SKINS } from "@/types/gift";
import { resolveTrackYouTube } from "@/lib/api/gift";

function formatDuration(ms?: number): string {
  if (!ms) return "";
  const total = Math.floor(ms / 1000);
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

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

  // 인라인 재생: 트랙 탭 → YouTube 임베드. videoId 없으면 lazy resolve 후 재생.
  const [playingId, setPlayingId] = useState<string | null>(null);
  const [resolvingId, setResolvingId] = useState<string | null>(null);
  const [resolvedIds, setResolvedIds] = useState<Record<string, string | null>>({});

  function videoIdFor(track: GiftTrack): string | null | undefined {
    return track.youtubeVideoId ?? resolvedIds[track.id];
  }

  async function togglePlay(track: GiftTrack) {
    if (playingId === track.id) {
      setPlayingId(null);
      return;
    }
    if (videoIdFor(track) === undefined) {
      setResolvingId(track.id);
      try {
        const { videoId } = await resolveTrackYouTube(track.id);
        setResolvedIds((m) => ({ ...m, [track.id]: videoId }));
      } catch {
        setResolvedIds((m) => ({ ...m, [track.id]: null }));
      } finally {
        setResolvingId(null);
      }
    }
    setPlayingId(track.id);
  }

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
            {gift.asset.tracks.map((track: GiftTrack, i: number) => {
              const isPlaying = playingId === track.id;
              const vid = videoIdFor(track);
              return (
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
                  <button
                    type="button"
                    onClick={() => togglePlay(track)}
                    className="flex w-full items-center gap-4 px-4 py-3 text-left transition-colors duration-200 hover:bg-surface-2 focus-ring"
                    aria-label={isPlaying ? `${track.name} 닫기` : `${track.name} 재생`}
                  >
                    <span
                      className="flex w-6 shrink-0 items-center justify-center text-xs tabular-nums"
                      style={{ color: isPlaying ? skin.accentColor : "var(--color-text-low)" }}
                    >
                      {resolvingId === track.id ? (
                        <span
                          className="inline-block h-3 w-3 animate-spin rounded-full border border-current border-t-transparent"
                          aria-hidden
                        />
                      ) : isPlaying ? (
                        <svg width="11" height="11" viewBox="0 0 11 11" fill="currentColor" aria-hidden>
                          <rect x="1" y="1" width="3" height="9" rx="1" />
                          <rect x="7" y="1" width="3" height="9" rx="1" />
                        </svg>
                      ) : (
                        i + 1
                      )}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p
                        className="truncate text-[0.9375rem] font-medium"
                        style={{ color: isPlaying ? skin.accentColor : "var(--color-text-hi)" }}
                      >
                        {track.name}
                      </p>
                      <p className="mt-0.5 truncate text-xs text-text-mid">{track.artist}</p>
                    </div>
                    {isPlaying ? (
                      <svg
                        width="14"
                        height="14"
                        viewBox="0 0 14 14"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="1.5"
                        className="shrink-0 text-text-low"
                        aria-hidden
                      >
                        <path d="M11 4L4 11M4 4l7 7" strokeLinecap="round" />
                      </svg>
                    ) : (
                      <span className="flex shrink-0 items-center gap-2 text-text-low">
                        {track.durationMs ? (
                          <span className="text-xs tabular-nums">{formatDuration(track.durationMs)}</span>
                        ) : null}
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
                          <path d="M4 3.5v9a.5.5 0 00.76.43l7.5-4.5a.5.5 0 000-.86l-7.5-4.5A.5.5 0 004 3.5z" />
                        </svg>
                      </span>
                    )}
                  </button>

                  {isPlaying && vid ? (
                    <div className="px-4 pb-3">
                      <div
                        className="overflow-hidden"
                        style={{ borderRadius: "var(--radius-image)", aspectRatio: "16 / 9" }}
                      >
                        <iframe
                          title={`${track.name} — YouTube`}
                          src={`https://www.youtube.com/embed/${vid}?autoplay=1&rel=0`}
                          allow="autoplay; encrypted-media; picture-in-picture"
                          allowFullScreen
                          className="h-full w-full"
                          style={{ border: 0 }}
                        />
                      </div>
                    </div>
                  ) : null}
                  {isPlaying && vid === null ? (
                    <p className="px-4 pb-3 text-xs text-text-low">
                      이 곡은 지금 재생할 수 없어요.
                    </p>
                  ) : null}
                </div>
              );
            })}
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
