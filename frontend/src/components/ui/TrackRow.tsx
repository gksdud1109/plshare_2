import type { AssetTrack } from "@/types/asset";
import { cn } from "@/lib/utils/cn";

function formatDuration(ms: number): string {
  const total = Math.floor(ms / 1000);
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export function TrackRow({
  track,
  index,
  trailing,
  className,
}: {
  track: AssetTrack;
  index?: number;
  trailing?: React.ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex items-center gap-4 border-b border-stone-200/70 py-4 last:border-b-0",
        className,
      )}
    >
      {typeof index === "number" ? (
        <span className="w-6 text-right text-xs tracking-wide text-ink-400 tabular-nums">
          {index + 1}
        </span>
      ) : null}
      <div className="min-w-0 flex-1">
        <p className="truncate font-sans text-[0.95rem] text-ink-900">
          {track.name}
        </p>
        <p className="mt-0.5 truncate text-xs text-ink-500">{track.artist}</p>
      </div>
      <span className="shrink-0 text-xs tabular-nums text-ink-400">
        {formatDuration(track.durationMs)}
      </span>
      {trailing ? <div className="shrink-0">{trailing}</div> : null}
    </div>
  );
}
