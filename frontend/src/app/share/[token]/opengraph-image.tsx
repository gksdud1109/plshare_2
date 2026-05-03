import { ImageResponse } from "next/og";
import { fetchShareDataServer } from "@/lib/api/share";
import { buildDemoSharedAsset } from "@/lib/api/fixtures";
import type { SharedAsset } from "@/types/asset";

export const runtime = "nodejs";
export const alt = "plshare 취향 자산 카드";
export const size = { width: 1200, height: 630 } as const;
export const contentType = "image/png";

// Re-render the OG card on the same cadence as the page.
export const revalidate = 300;

type RouteProps = {
  params: Promise<{ token: string }>;
};

function clampLine(text: string | undefined, max: number): string {
  if (!text) return "";
  const t = text.replace(/\s+/g, " ").trim();
  return t.length <= max ? t : `${t.slice(0, max - 1)}…`;
}

export default async function OpenGraphImage({ params }: RouteProps) {
  const { token } = await params;

  let data: SharedAsset;
  try {
    data = await fetchShareDataServer(token);
  } catch {
    data = buildDemoSharedAsset(token);
  }

  const excerpt = clampLine(data.diaryText ?? data.description, 140);
  const title = clampLine(data.title, 60);
  const tags = data.emotionTags.slice(0, 4);

  // Refined / luxury palette — warm dark.
  const BG = "#1a1614";
  const BG_PANEL = "#221d1a";
  const BORDER = "rgba(245, 240, 232, 0.10)";
  const INK_HI = "#f5f0e8";
  const INK_MID = "rgba(245, 240, 232, 0.74)";
  const INK_LO = "rgba(245, 240, 232, 0.52)";

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
          {data.coverUrl ? (
            // eslint-disable-next-line jsx-a11y/alt-text
            <img
              src={data.coverUrl}
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
                color: INK_LO,
              }}
            >
              Shared
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

            {excerpt ? (
              <span
                style={{
                  marginTop: 28,
                  fontSize: 28,
                  lineHeight: 1.45,
                  fontStyle: "italic",
                  color: INK_MID,
                }}
              >
                “{excerpt}”
              </span>
            ) : null}

            {tags.length > 0 ? (
              <div
                style={{
                  display: "flex",
                  marginTop: 32,
                  gap: 10,
                  flexWrap: "wrap",
                }}
              >
                {tags.map((t) => (
                  <span
                    key={t}
                    style={{
                      display: "flex",
                      fontFamily: "sans-serif",
                      fontSize: 18,
                      padding: "8px 16px",
                      borderRadius: 999,
                      border: `1px solid ${BORDER}`,
                      color: INK_MID,
                    }}
                  >
                    {t}
                  </span>
                ))}
              </div>
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
              {data.tracks.length} tracks
            </span>
            <span
              style={{
                fontSize: 22,
                letterSpacing: 1,
                color: INK_HI,
              }}
            >
              plshare
            </span>
          </div>
        </div>
      </div>
    ),
    { ...size },
  );
}
