import type { AssetSummary } from "@/types/asset";
import { cn } from "@/lib/utils/cn";

/**
 * AssetCard — per design direction, the playlist is an "asset" with physicality,
 * never a flat list. Used on /assets list and the share-grid surfaces.
 */
export function AssetCard({
  asset,
  className,
}: {
  asset: AssetSummary;
  className?: string;
}) {
  return (
    <article
      className={cn(
        "group relative flex flex-col overflow-hidden rounded-card",
        "border border-hairline bg-surface-1",
        "shadow-[var(--shadow-card)]",
        "transition-[transform,box-shadow,border-color] duration-300 ease-[var(--ease-spring)]",
        "hover:-translate-y-1 hover:border-hairline-strong hover:shadow-[var(--shadow-pop)]",
        className,
      )}
    >
      {/* cover with ambient glow behind */}
      <div className="relative aspect-square w-full overflow-hidden rounded-image bg-surface-3">
        {asset.coverUrl ? (
          <>
            {/* ambient glow layer */}
            <img
              src={asset.coverUrl}
              alt=""
              aria-hidden
              className="cover-glow absolute inset-0 h-full w-full scale-110 object-cover opacity-40 blur-2xl"
            />
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={asset.coverUrl}
              alt={asset.title}
              className="relative h-full w-full object-cover transition-transform duration-500 ease-[var(--ease-spring)] group-hover:scale-105"
            />
          </>
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <span className="text-4xl text-text-low">♪</span>
          </div>
        )}
      </div>

      <div className="flex flex-col gap-2 p-4">
        {asset.assetKind === "MOOD_VIDEO" ? (
          <span
            className="inline-flex w-fit items-center rounded-full border border-accent px-2.5 py-0.5 text-[0.6875rem] font-semibold tracking-wide text-accent"
            style={{ background: "var(--accent-soft)" }}
          >
            영상
          </span>
        ) : asset.description || asset.emotionTags.length > 0 ? (
          <span
            className="inline-flex w-fit items-center rounded-full border border-accent px-2.5 py-0.5 text-[0.6875rem] font-semibold tracking-wide text-accent"
            style={{ background: "var(--accent-soft)" }}
          >
            Emotional
          </span>
        ) : (
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            Playlist
          </p>
        )}
        <h2 className="truncate text-[1.0625rem] font-bold leading-snug text-text-hi">
          {asset.title}
        </h2>
        <div className="flex items-center justify-between text-xs tabular-nums text-text-mid">
          <span>{asset.assetKind === "MOOD_VIDEO" ? "무드영상" : `${asset.trackCount}곡`}</span>
          <time dateTime={asset.createdAt}>
            {new Date(asset.createdAt).toLocaleDateString("ko-KR", {
              year: "numeric",
              month: "short",
              day: "numeric",
            })}
          </time>
        </div>
      </div>
    </article>
  );
}
