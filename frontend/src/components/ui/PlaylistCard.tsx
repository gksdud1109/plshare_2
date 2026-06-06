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
        "group relative flex w-full flex-col overflow-hidden rounded-card text-left",
        "border border-hairline bg-surface-1",
        "shadow-[var(--shadow-card)]",
        "transition-[transform,box-shadow,border-color] duration-300 ease-[var(--ease-spring)]",
        "hover:-translate-y-1 hover:border-hairline-strong hover:shadow-[var(--shadow-pop)]",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent",
        className,
      )}
    >
      {/* square cover with ambient glow */}
      <div className="relative aspect-square w-full overflow-hidden rounded-image bg-surface-3">
        {playlist.imageUrl ? (
          <>
            <img
              src={playlist.imageUrl}
              alt=""
              aria-hidden
              className="absolute inset-0 h-full w-full scale-110 object-cover opacity-40 blur-2xl"
            />
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={playlist.imageUrl}
              alt={playlist.name}
              className="relative h-full w-full object-cover transition-transform duration-500 ease-[var(--ease-spring)] group-hover:scale-105"
            />
          </>
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <span className="text-4xl text-text-low">♪</span>
          </div>
        )}
      </div>

      <div className="flex flex-col gap-1.5 p-4">
        <h3 className="truncate text-[1.0625rem] font-bold leading-snug text-text-hi">
          {playlist.name}
        </h3>
        {playlist.description ? (
          <p className="line-clamp-2 text-sm leading-relaxed text-text-mid">
            {playlist.description}
          </p>
        ) : null}
        <p className="mt-1 text-xs tabular-nums text-text-low">
          {playlist.trackCount} tracks
        </p>
      </div>
    </button>
  );
}
