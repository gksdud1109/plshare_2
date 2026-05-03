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
          {asset.hasEmotionalContext ? "Emotional Context" : "Asset"}
        </p>
        <h2 className="font-display text-2xl leading-tight text-ink-900">
          {asset.title}
        </h2>
        <div className="mt-2 flex items-center justify-between text-xs tracking-wide text-ink-400">
          <span>{asset.trackCount} tracks</span>
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
