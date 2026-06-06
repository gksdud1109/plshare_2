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
        "group flex items-center gap-4 border-b border-hairline py-3 last:border-b-0",
        "rounded-lg px-3 transition-colors duration-150 hover:bg-surface-2",
        className,
      )}
    >
      {typeof index === "number" ? (
        <span className="w-6 shrink-0 text-right text-xs tabular-nums text-text-low">
          {index + 1}
        </span>
      ) : null}
      <div className="min-w-0 flex-1">
        <p className="truncate text-[0.9375rem] font-medium text-text-hi">
          {track.name}
        </p>
        <p className="mt-0.5 truncate text-xs text-text-mid">{track.artist}</p>
      </div>
      <span className="shrink-0 text-xs tabular-nums text-text-low">
        {formatDuration(track.durationMs)}
      </span>
      {trailing ? <div className="shrink-0">{trailing}</div> : null}
    </div>
  );
}
