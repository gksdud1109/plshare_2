import type { Metadata } from "next";
import Link from "next/link";
import { fetchShareDataServer } from "@/lib/api/share";
import { buildDemoSharedAsset } from "@/lib/api/fixtures";
import type { SharedAsset } from "@/types/asset";
import { TrackRow } from "@/components/ui/TrackRow";
import { ShareCallToAction } from "@/components/share/ShareCallToAction";

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
    data = buildDemoSharedAsset(token);
    usingFixture = true;
  }

  return (
    <div className="min-h-screen">
      <nav className="mx-auto flex w-full max-w-5xl items-center justify-between px-6 py-6 md:px-10">
        <Link href="/" className="font-display text-lg tracking-tight">
          plshare
        </Link>
        <ShareCallToAction />
      </nav>

      <main className="mx-auto w-full max-w-5xl px-6 pb-20 md:px-10">
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
        <p className="mb-6 inline-flex rounded-full border border-stone-200 bg-bone-100 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-ink-500">
          Demo data · 백엔드 연결 전
        </p>
      ) : null}

      <section className="grid grid-cols-1 gap-10 py-8 md:grid-cols-[minmax(0,360px)_1fr] md:gap-14">
        <div className="aspect-square w-full overflow-hidden rounded-[var(--radius-card)] border border-stone-200 bg-stone-200 shadow-[0_24px_60px_-30px_rgba(20,18,16,0.35)]">
          {data.coverUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={data.coverUrl}
              alt={data.title}
              className="h-full w-full object-cover"
            />
          ) : null}
        </div>

        <div>
          <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
            Shared
          </p>
          <h1 className="mt-3 text-4xl leading-tight md:text-5xl">
            {data.title}
          </h1>
          {data.description ? (
            <p className="mt-6 font-serif text-xl italic text-ink-600">
              {data.description}
            </p>
          ) : null}

          {data.emotionTags.length > 0 ? (
            <div className="mt-6 flex flex-wrap gap-2">
              {data.emotionTags.map((t) => (
                <span
                  key={t}
                  className="rounded-full border border-stone-200 bg-bone-100 px-3 py-1 text-xs text-ink-600"
                >
                  {t}
                </span>
              ))}
            </div>
          ) : null}

          {data.diaryText ? (
            <div className="mt-8 max-w-md whitespace-pre-line border-l border-stone-300 pl-4 text-[0.95rem] leading-relaxed text-ink-700">
              {data.diaryText}
            </div>
          ) : null}
        </div>
      </section>

      <section className="mt-12">
        <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
          Tracks
        </p>
        <div className="mt-4 rounded-[var(--radius-card)] border border-stone-200 bg-bone-100 px-6 md:px-8">
          {data.tracks.map((t, i) => (
            <TrackRow key={t.id} track={t} index={i} />
          ))}
        </div>
      </section>
    </article>
  );
}
