import type { PlaylistAsset } from "@/types/asset";
import { cn } from "@/lib/utils/cn";

/**
 * AssetCard — per design direction, the playlist is an "asset" with physicality,
 * never a flat list. This is the baseline component the Import / Assetize / Export
 * screens all lean on. Do not simplify to a plain list (see handoff v0.2).
 */
export function AssetCard({
  asset,
  className,
}: {
  asset: PlaylistAsset;
  className?: string;
}) {
  return (
    <article
      className={cn(
        "group relative flex flex-col overflow-hidden rounded-[var(--radius-card)]",
        "border border-stone-200 bg-bone-100",
        "shadow-[0_1px_0_rgba(20,18,16,0.04),0_24px_60px_-30px_rgba(20,18,16,0.25)]",
        "transition-[transform,box-shadow] duration-[var(--duration-weighted)] ease-[var(--ease-weighted)]",
        "hover:-translate-y-0.5 hover:shadow-[0_1px_0_rgba(20,18,16,0.05),0_36px_80px_-30px_rgba(20,18,16,0.35)]",
        className,
      )}
    >
      <div className="aspect-[4/5] w-full bg-stone-200">
        {asset.coverUrl ? (
          // Raw <img> kept intentional for initial scaffold; swap to next/image when
          // the cover source domains are finalized.
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={asset.coverUrl}
            alt={asset.title}
            className="h-full w-full object-cover"
          />
        ) : null}
      </div>

      <div className="flex flex-col gap-3 p-6">
        <p className="text-[0.6875rem] uppercase tracking-[0.22em] text-ink-500">
          {asset.source === "spotify" ? "From Spotify" : "From Apple Music"}
        </p>
        <h2 className="font-display text-2xl leading-tight text-ink-900">
          {asset.title}
        </h2>
        {asset.curatorNote ? (
          <p className="font-sans text-sm leading-relaxed text-ink-600">
            {asset.curatorNote}
          </p>
        ) : null}
        <p className="mt-2 text-xs tracking-wide text-ink-400">
          {asset.tracks.length} tracks
        </p>
      </div>
    </article>
  );
}
