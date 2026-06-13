import { ImageResponse } from "next/og";
import { fetchGiftServer } from "@/lib/api/gift";
import type { GiftView } from "@/types/gift";

export const runtime = "nodejs";
export const alt = "plshare 선물 카드";
export const size = { width: 1200, height: 630 } as const;
export const contentType = "image/png";
export const revalidate = 60;

type RouteProps = { params: Promise<{ token: string }> };

function clampLine(text: string | undefined, max: number): string {
  if (!text) return "";
  const t = text.replace(/\s+/g, " ").trim();
  return t.length <= max ? t : `${t.slice(0, max - 1)}…`;
}

export default async function GiftOpenGraphImage({ params }: RouteProps) {
  const { token } = await params;

  let gift: GiftView | null = null;
  try {
    gift = await fetchGiftServer(token);
  } catch {
    gift = null;
  }

  const title = clampLine(gift?.asset.title ?? "음악 선물", 50);
  const sender = gift?.sender.handle ? `@${gift.sender.handle}` : "plshare";
  const message = clampLine(gift?.message, 130);
  const cover = gift?.asset.coverUrl;
  const trackCount = gift?.asset.tracks.length ?? 0;

  // Refined / luxury palette — warm dark, violet 선물 accent.
  const BG = "#161320";
  const BG_PANEL = "#1f1a2e";
  const BORDER = "rgba(240, 236, 248, 0.12)";
  const ACCENT = "#a78bfa";
  const INK_HI = "#f3f0f8";
  const INK_MID = "rgba(243, 240, 248, 0.74)";
  const INK_LO = "rgba(243, 240, 248, 0.52)";

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          backgroundColor: BG,
          color: INK_HI,
          fontFamily: "serif",
          padding: 56,
          gap: 48,
        }}
      >
        {/* Left: cover */}
        <div
          style={{
            display: "flex",
            width: 380,
            height: 518,
            borderRadius: 18,
            overflow: "hidden",
            backgroundColor: BG_PANEL,
            border: `1px solid ${BORDER}`,
            boxShadow: "0 30px 60px -30px rgba(0,0,0,0.6)",
          }}
        >
          {cover ? (
            // eslint-disable-next-line jsx-a11y/alt-text
            <img
              src={cover}
              width={380}
              height={518}
              style={{ objectFit: "cover", width: "100%", height: "100%" }}
            />
          ) : (
            <div
              style={{
                display: "flex",
                width: "100%",
                height: "100%",
                alignItems: "center",
                justifyContent: "center",
                fontSize: 28,
                color: INK_LO,
              }}
            >
              plshare
            </div>
          )}
        </div>

        {/* Right: meta */}
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            flex: 1,
            justifyContent: "space-between",
            paddingTop: 8,
            paddingBottom: 8,
          }}
        >
          <div style={{ display: "flex", flexDirection: "column" }}>
            <span
              style={{
                fontFamily: "sans-serif",
                fontSize: 18,
                letterSpacing: 6,
                textTransform: "uppercase",
                color: ACCENT,
              }}
            >
              A gift from {sender}
            </span>
            <span
              style={{
                fontSize: 64,
                lineHeight: 1.08,
                marginTop: 18,
                color: INK_HI,
                letterSpacing: -0.5,
              }}
            >
              {title}
            </span>

            {message ? (
              <span
                style={{
                  marginTop: 28,
                  fontSize: 28,
                  lineHeight: 1.45,
                  fontStyle: "italic",
                  color: INK_MID,
                }}
              >
                “{message}”
              </span>
            ) : null}
          </div>

          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              alignItems: "baseline",
              gap: 10,
            }}
          >
            <span
              style={{
                fontFamily: "sans-serif",
                fontSize: 14,
                letterSpacing: 4,
                textTransform: "uppercase",
                color: INK_LO,
              }}
            >
              {trackCount} tracks · 계정 없이 재생
            </span>
            <span style={{ fontSize: 22, letterSpacing: 1, color: INK_HI }}>
              plshare
            </span>
          </div>
        </div>
      </div>
    ),
    { ...size },
  );
}
