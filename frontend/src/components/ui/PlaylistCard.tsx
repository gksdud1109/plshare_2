"use client";

import type { SpotifyPlaylist } from "@/types/asset";
import { cn } from "@/lib/utils/cn";

export function PlaylistCard({
  playlist,
  onSelect,
  className,
}: {
  playlist: SpotifyPlaylist;
  onSelect?: (id: string) => void;
  className?: string;
}) {
  return (
    <button
      type="button"
      onClick={() => onSelect?.(playlist.id)}
      className={cn(
        "group relative flex w-full flex-col overflow-hidden rounded-[var(--radius-card)]",
        "border border-stone-200 bg-bone-100 text-left",
        "shadow-[0_1px_0_rgba(20,18,16,0.04),0_18px_40px_-30px_rgba(20,18,16,0.2)]",
        "transition-[transform,box-shadow] duration-[var(--duration-weighted)] ease-[var(--ease-weighted)]",
        "hover:-translate-y-0.5 hover:shadow-[0_1px_0_rgba(20,18,16,0.05),0_28px_60px_-30px_rgba(20,18,16,0.32)]",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold-500/60",
        className,
      )}
    >
      <div className="aspect-square w-full overflow-hidden bg-stone-200">
        {playlist.imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={playlist.imageUrl}
            alt={playlist.name}
            className="h-full w-full object-cover transition-transform duration-[var(--duration-weighted)] ease-[var(--ease-weighted)] group-hover:scale-[1.02]"
          />
        ) : null}
      </div>
      <div className="flex flex-col gap-2 p-5">
        <h3 className="font-display text-lg leading-snug text-ink-900">
          {playlist.name}
        </h3>
        {playlist.description ? (
          <p className="line-clamp-2 text-sm leading-relaxed text-ink-600">
            {playlist.description}
          </p>
        ) : null}
        <p className="mt-1 text-xs tracking-wide text-ink-400">
          {playlist.trackCount} tracks
        </p>
      </div>
    </button>
  );
}
