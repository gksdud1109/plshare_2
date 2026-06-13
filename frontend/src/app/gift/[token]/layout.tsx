import type { Metadata } from "next";
import { fetchGiftServer } from "@/lib/api/gift";

function excerpt(text: string | undefined, max = 120): string | undefined {
  if (!text) return undefined;
  const t = text.replace(/\s+/g, " ").trim();
  return t.length <= max ? t : `${t.slice(0, max - 1)}…`;
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ token: string }>;
}): Promise<Metadata> {
  const { token } = await params;
  try {
    const gift = await fetchGiftServer(token);
    const title = `${gift.sender.displayName}님이 보낸 선물`;
    const description =
      excerpt(gift.message) ?? `${gift.asset.title} — 당신을 위한 음악 선물`;
    const image = `/gift/${token}/opengraph-image`;
    return {
      title: `${title} — plshare`,
      description,
      openGraph: {
        title,
        description,
        type: "music.playlist",
        images: [{ url: image, width: 1200, height: 630, alt: "plshare 선물 카드" }],
      },
      twitter: {
        card: "summary_large_image",
        title,
        description,
        images: [image],
      },
    };
  } catch {
    return {
      title: "plshare 선물",
      description: "당신을 위한 음악 선물",
    };
  }
}

export default function GiftTokenLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
