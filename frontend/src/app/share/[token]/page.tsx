import type { Metadata } from "next";
import Link from "next/link";
import { fetchShareDataServer } from "@/lib/api/share";
import { buildDemoSharedAsset } from "@/lib/api/fixtures";
import type { SharedAsset } from "@/types/asset";
import { ShareTrackList } from "@/components/share/ShareTrackList";
import { ShareCallToAction } from "@/components/share/ShareCallToAction";
import { notFound } from "next/navigation";
import { demoFixturesEnabled } from "@/lib/demo";

// ISR: regenerate the public share page at most every 5 minutes.
export const revalidate = 300;

type PageProps = {
  params: Promise<{ token: string }>;
};

function excerpt(text: string | undefined, max = 140): string | undefined {
  if (!text) return undefined;
  const trimmed = text.replace(/\s+/g, " ").trim();
  if (trimmed.length <= max) return trimmed;
  return `${trimmed.slice(0, max - 1)}…`;
}

export async function generateMetadata({
  params,
}: PageProps): Promise<Metadata> {
  const { token } = await params;
  try {
    const data = await fetchShareDataServer(token);
    const description =
      excerpt(data.diaryText) ??
      excerpt(data.description) ??
      "취향 자산을 함께 들어보세요";
    return {
      title: `${data.title} — plshare`,
      description,
      openGraph: {
        title: data.title,
        description,
        type: "music.playlist",
        images: [
          {
            url: `/share/${token}/opengraph-image`,
            width: 1200,
            height: 630,
            alt: "plshare 취향 자산 카드",
          },
        ],
      },
      twitter: {
        card: "summary_large_image",
        title: data.title,
        description,
        images: [`/share/${token}/opengraph-image`],
      },
    };
  } catch {
    return {
      title: "plshare",
      description: "취향 자산 매니지먼트",
    };
  }
}

export default async function SharePage({ params }: PageProps) {
  const { token } = await params;

  let data: SharedAsset;
  let usingFixture = false;
  try {
    data = await fetchShareDataServer(token);
  } catch {
    if (!demoFixturesEnabled()) notFound();
    data = buildDemoSharedAsset(token);
    usingFixture = true;
  }

  return (
    <div className="relative min-h-screen bg-bg overflow-hidden">
      {/* Cinematic fullbleed cover glow — server rendered */}
      {data.coverUrl && (
        <div
          aria-hidden="true"
          className="pointer-events-none fixed inset-0"
          style={{ zIndex: 0 }}
        >
          {/* Blurred cover as ambient light */}
          <div
            className="absolute inset-0 cover-glow"
            style={{
              backgroundImage: `url(${data.coverUrl})`,
              backgroundSize: "cover",
              backgroundPosition: "center",
              opacity: 0.18,
            }}
          />
          {/* Dark gradient overlay to keep content readable */}
          <div
            className="absolute inset-0"
            style={{
              background:
                "linear-gradient(to bottom, rgba(11,11,15,0.55) 0%, rgba(11,11,15,0.80) 50%, rgba(11,11,15,0.97) 100%)",
            }}
          />
        </div>
      )}

      {/* Accent radial glow */}
      <div
        className="accent-glow pointer-events-none fixed left-1/4 top-1/4 h-[600px] w-[600px] -translate-x-1/2 -translate-y-1/2 opacity-25"
        aria-hidden="true"
        style={{ zIndex: 0 }}
      />

      {/* Nav */}
      <nav
        className="relative mx-auto flex w-full max-w-5xl items-center justify-between px-6 py-6 md:px-10"
        style={{ zIndex: 10 }}
      >
        <Link
          href="/"
          className="font-display text-lg tracking-tight text-text-hi hover:text-accent transition-colors duration-200"
        >
          plshare
        </Link>
        <ShareCallToAction />
      </nav>

      <main
        className="relative mx-auto w-full max-w-5xl px-6 pb-24 md:px-10"
        style={{ zIndex: 10 }}
      >
        <ShareView data={data} usingFixture={usingFixture} />
      </main>
    </div>
  );
}

function ShareView({
  data,
  usingFixture,
}: {
  data: SharedAsset;
  usingFixture: boolean;
}) {
  return (
    <article>
      {usingFixture ? (
        <p className="mb-6 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
          Demo data · 백엔드 연결 전
        </p>
      ) : null}

      {/* Hero: cover + meta */}
      <section className="grid grid-cols-1 gap-10 py-8 md:grid-cols-[minmax(0,340px)_1fr] md:gap-14">
        {/* Cover with ambient glow */}
        <div className="relative">
          {data.coverUrl && (
            <div
              className="cover-glow absolute inset-0 animate-pulse-glow"
              style={{
                backgroundImage: `url(${data.coverUrl})`,
                backgroundSize: "cover",
                backgroundPosition: "center",
                borderRadius: "var(--radius-image)",
                zIndex: 0,
              }}
              aria-hidden="true"
            />
          )}
          <div
            className="relative aspect-square w-full overflow-hidden bg-surface-2"
            style={{
              borderRadius: "var(--radius-image)",
              boxShadow: "var(--shadow-pop)",
              zIndex: 1,
            }}
          >
            {data.coverUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={data.coverUrl}
                alt={data.title}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center">
                <span className="text-4xl opacity-20">♪</span>
              </div>
            )}
          </div>
        </div>

        {/* Meta */}
        <div className="flex flex-col justify-center">
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            Shared
          </p>
          <h1
            className="mt-3 font-display text-text-hi"
            style={{
              fontSize: "clamp(2rem, 4vw, 3rem)",
              fontWeight: 700,
              letterSpacing: "-0.02em",
            }}
          >
            {data.title}
          </h1>

          {data.description ? (
            <p className="mt-4 text-lg font-medium text-text-mid leading-snug">
              {data.description}
            </p>
          ) : null}

          {data.emotionTags.length > 0 ? (
            <div className="mt-6 flex flex-wrap gap-2">
              {data.emotionTags.map((t) => (
                <span
                  key={t}
                  className="rounded-full border border-accent px-3 py-1 text-xs font-medium text-accent"
                  style={{ background: "var(--accent-soft)" }}
                >
                  {t}
                </span>
              ))}
            </div>
          ) : null}

          {data.diaryText ? (
            <blockquote
              className="mt-8 max-w-md whitespace-pre-line border-l-2 border-accent pl-5 text-base leading-relaxed text-text"
              style={{ borderColor: "var(--color-accent)" }}
            >
              {data.diaryText}
            </blockquote>
          ) : null}
        </div>
      </section>

      {/* Track list */}
      <section className="mt-12">
        <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
          Tracks
        </p>
        <ShareTrackList tracks={data.tracks} />
      </section>

      {/* CTA */}
      <section className="mt-16 flex flex-col items-center gap-4 py-8 text-center">
        <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
          나도 취향을 기록하고 싶다면
        </p>
        <p
          className="font-display text-text-hi"
          style={{ fontSize: "clamp(1.5rem, 3vw, 2.25rem)", fontWeight: 700 }}
        >
          나만의 취향 자산을 만들어보세요.
        </p>
        <Link
          href="/import"
          className="mt-2 rounded-full bg-accent px-8 text-sm font-semibold text-white transition-all duration-200 hover:bg-accent-hi hover:-translate-y-0.5"
          style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
        >
          내 라이브러리 만들기
        </Link>
      </section>
    </article>
  );
}
