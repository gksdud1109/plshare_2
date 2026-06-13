import React from "react";
import Link from "next/link";

const COLLAGE_IMAGES = [
  "https://picsum.photos/seed/pl1/400/400",
  "https://picsum.photos/seed/pl2/400/400",
  "https://picsum.photos/seed/pl3/400/400",
  "https://picsum.photos/seed/pl4/400/400",
  "https://picsum.photos/seed/pl5/400/400",
];

export default function LandingPage() {
  return (
    <main className="relative min-h-screen overflow-hidden bg-bg">
      {/* Page-level accent glow */}
      <div
        className="accent-glow pointer-events-none absolute left-1/4 top-1/4 h-[600px] w-[600px] -translate-x-1/2 -translate-y-1/2"
        aria-hidden
      />

      {/* ── Header ─────────────────────────────────────────────── */}
      <header className="relative z-10 flex items-center justify-between px-8 py-6 md:px-16">
        <span className="font-display text-lg text-text-hi">plshare</span>
        <span
          className="text-label text-text-low"
          style={{ fontSize: "0.75rem", letterSpacing: "0.12em" }}
        >
          v0.2 · Demo
        </span>
      </header>

      {/* ── Hero ────────────────────────────────────────────────── */}
      <section className="relative z-10 flex min-h-[calc(100vh-80px)] items-center px-8 md:px-16">
        {/* Left: copy */}
        <div className="flex w-full flex-col lg:w-1/2">
          <p
            className="mb-6 text-text-low"
            style={{
              fontSize: "0.75rem",
              fontWeight: 600,
              letterSpacing: "0.12em",
              textTransform: "uppercase",
            }}
          >
            Refined Asset · Nocturne
          </p>

          <h1
            className="font-display text-text-hi"
            style={{
              fontSize: "clamp(2.5rem, 6vw, 4.5rem)",
              fontWeight: 800,
              letterSpacing: "-0.03em",
              lineHeight: 1.05,
            }}
          >
            당신의 취향을
            <br />
            <span className="text-accent">자산으로.</span>
          </h1>

          <p
            className="mt-8 max-w-lg leading-relaxed text-text-mid"
            style={{ fontSize: "1.0625rem" }}
          >
            플레이리스트에 감성 맥락을 더하고,
            <br className="hidden sm:block" />
            공유하거나 YouTube Music으로 옮기세요.
            <br />
            가볍게 흘려보내지 않도록.
          </p>

          <div className="mt-12 flex flex-wrap items-center gap-4">
            {/* Auth-start is a backend redirect endpoint (creates an OAuth handshake
                and 302s to the provider) — use a plain <a> so Next never prefetches it. */}
            <a
              href="/api/auth/google/start?returnTo=%2Fauth%2Fspotify%3Flive%3D1%26next%3D%2Fimport"
              className="inline-flex h-12 items-center gap-3 rounded-full bg-accent px-6 text-sm font-semibold text-white transition-all duration-300 hover:bg-accent-hi hover:shadow-[0_0_24px_-4px_rgba(124,92,255,0.7)] active:bg-accent-press focus-ring"
              style={{ letterSpacing: "0.01em" }}
            >
              Google로 시작하기
              <svg
                aria-hidden
                width="16"
                height="16"
                viewBox="0 0 16 16"
                fill="none"
              >
                <path
                  d="M3 8h10M9 4l4 4-4 4"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </a>
            <Link
              href="/auth/spotify"
              className="inline-flex h-12 items-center rounded-full px-5 text-sm font-medium text-text-mid transition-colors duration-300 hover:text-text-hi"
            >
              Spotify로 시작하기
            </Link>
            <Link
              href="/assets"
              className="inline-flex h-12 items-center rounded-full px-6 text-sm font-medium text-text-mid transition-colors duration-300 hover:text-text-hi"
            >
              라이브러리 둘러보기
            </Link>
          </div>
        </div>

        {/* Right: album art collage */}
        <div
          className="pointer-events-none absolute inset-y-0 right-0 hidden w-[52%] lg:block"
          aria-hidden
        >
          {/* Ambient glow blobs */}
          <div
            className="absolute right-[10%] top-1/2 h-[360px] w-[360px] -translate-y-1/2 rounded-full animate-pulse-glow"
            style={{
              background:
                "radial-gradient(circle, rgba(124,92,255,0.28) 0%, transparent 70%)",
            }}
          />
          <div
            className="absolute right-[30%] top-[30%] h-[200px] w-[200px] rounded-full animate-pulse-glow"
            style={{
              background:
                "radial-gradient(circle, rgba(155,130,255,0.18) 0%, transparent 70%)",
              animationDelay: "1.2s",
            }}
          />

          {/* Cover images — staggered floating collage */}
          {/* Main large cover — center */}
          <div
            className="absolute animate-fade-up"
            style={{
              right: "18%",
              top: "50%",
              transform: "translateY(-50%) rotate(-3deg)",
              animationDelay: "0.1s",
              width: 220,
              height: 220,
            }}
          >
            <div
              className="cover-glow absolute inset-0 rounded-[16px]"
              style={{
                backgroundImage: `url(${COLLAGE_IMAGES[0]})`,
                backgroundSize: "cover",
              }}
            />
            <img
              src={COLLAGE_IMAGES[0]}
              alt=""
              width={220}
              height={220}
              className="relative rounded-[16px] object-cover shadow-[0_20px_60px_-20px_rgba(0,0,0,0.7)]"
              style={{ display: "block" }}
            />
          </div>

          {/* Top-right */}
          <div
            className="absolute animate-fade-up"
            style={{
              right: "6%",
              top: "18%",
              transform: "rotate(5deg)",
              animationDelay: "0.25s",
              width: 160,
              height: 160,
            }}
          >
            <img
              src={COLLAGE_IMAGES[1]}
              alt=""
              width={160}
              height={160}
              className="rounded-[16px] object-cover shadow-[0_8px_30px_-12px_rgba(0,0,0,0.6)]"
              style={{
                display: "block",
                border: "1px solid rgba(255,255,255,0.08)",
              }}
            />
          </div>

          {/* Bottom-right */}
          <div
            className="absolute animate-fade-up"
            style={{
              right: "4%",
              bottom: "22%",
              transform: "rotate(-2deg)",
              animationDelay: "0.4s",
              width: 180,
              height: 180,
            }}
          >
            <img
              src={COLLAGE_IMAGES[2]}
              alt=""
              width={180}
              height={180}
              className="rounded-[16px] object-cover shadow-[0_8px_30px_-12px_rgba(0,0,0,0.6)]"
              style={{
                display: "block",
                border: "1px solid rgba(255,255,255,0.08)",
              }}
            />
          </div>

          {/* Far left of right panel, small */}
          <div
            className="absolute animate-fade-up"
            style={{
              right: "42%",
              top: "22%",
              transform: "rotate(2deg)",
              animationDelay: "0.55s",
              width: 120,
              height: 120,
            }}
          >
            <img
              src={COLLAGE_IMAGES[3]}
              alt=""
              width={120}
              height={120}
              className="rounded-[16px] object-cover opacity-70 shadow-[0_8px_30px_-12px_rgba(0,0,0,0.6)]"
              style={{
                display: "block",
                border: "1px solid rgba(255,255,255,0.06)",
              }}
            />
          </div>

          {/* Bottom center */}
          <div
            className="absolute animate-fade-up"
            style={{
              right: "32%",
              bottom: "16%",
              transform: "rotate(-5deg)",
              animationDelay: "0.7s",
              width: 130,
              height: 130,
            }}
          >
            <img
              src={COLLAGE_IMAGES[4]}
              alt=""
              width={130}
              height={130}
              className="rounded-[16px] object-cover opacity-60 shadow-[0_8px_30px_-12px_rgba(0,0,0,0.6)]"
              style={{
                display: "block",
                border: "1px solid rgba(255,255,255,0.06)",
              }}
            />
          </div>
        </div>
      </section>

      {/* ── 3-step section ──────────────────────────────────────── */}
      <section className="relative z-10 px-8 pb-20 pt-4 md:px-16 md:pb-28">
        <div
          className="mb-10 h-px w-full"
          style={{ background: "rgba(255,255,255,0.08)" }}
        />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Step
            n="01"
            icon={
              <svg
                width="20"
                height="20"
                viewBox="0 0 20 20"
                fill="none"
                aria-hidden
              >
                <circle cx="10" cy="10" r="8" stroke="currentColor" strokeWidth="1.5" />
                <path
                  d="M7 10l2 2 4-4"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            }
            title="Spotify에서 가져오기"
            body="플레이리스트를 선택해 자산으로 정리합니다."
          />
          <Step
            n="02"
            icon={
              <svg
                width="20"
                height="20"
                viewBox="0 0 20 20"
                fill="none"
                aria-hidden
              >
                <path
                  d="M10 3v7l4 2"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                />
                <circle cx="10" cy="10" r="8" stroke="currentColor" strokeWidth="1.5" />
              </svg>
            }
            title="감성 맥락 더하기"
            body="당시의 기분과 이야기를 함께 남깁니다."
          />
          <Step
            n="03"
            icon={
              <svg
                width="20"
                height="20"
                viewBox="0 0 20 20"
                fill="none"
                aria-hidden
              >
                <path
                  d="M10 14l-4-4h3V4h2v6h3l-4 4z"
                  fill="currentColor"
                  opacity="0.8"
                />
                <path
                  d="M4 16h12"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                />
              </svg>
            }
            title="공유하거나 YouTube Music으로"
            body="취향 자산을 나누고 새 플랫폼에서 다시 듣습니다."
          />
        </div>
      </section>
    </main>
  );
}

function Step({
  n,
  icon,
  title,
  body,
}: {
  n: string;
  icon: React.ReactNode;
  title: string;
  body: string;
}) {
  return (
    <div
      className="rounded-[18px] border p-6 transition-all duration-300 hover:-translate-y-1"
      style={{
        background: "var(--color-surface-1)",
        borderColor: "rgba(255,255,255,0.08)",
        boxShadow: "var(--shadow-card)",
      }}
    >
      <div className="mb-4 flex items-center gap-3">
        <div
          className="flex h-9 w-9 items-center justify-center rounded-full text-accent"
          style={{ background: "var(--accent-soft)" }}
        >
          {icon}
        </div>
        <span
          className="text-text-low"
          style={{
            fontSize: "0.75rem",
            fontWeight: 600,
            letterSpacing: "0.12em",
            textTransform: "uppercase",
          }}
        >
          {n}
        </span>
      </div>
      <p className="text-sm font-semibold text-text-hi">{title}</p>
      <p className="mt-1 text-sm leading-relaxed text-text-mid">{body}</p>
    </div>
  );
}
