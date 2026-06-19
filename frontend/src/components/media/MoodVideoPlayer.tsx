import { cn } from "@/lib/utils/cn";

export function MoodVideoPlayer({
  title,
  videoId,
  channelName,
  trackListText,
  className,
}: {
  title: string;
  videoId: string;
  channelName?: string | null;
  trackListText?: string | null;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "overflow-hidden border border-hairline bg-surface-1",
        className,
      )}
      style={{ borderRadius: "var(--radius-card)" }}
    >
      <div style={{ aspectRatio: "16 / 9" }}>
        <iframe
          title={title}
          src={`https://www.youtube.com/embed/${videoId}?rel=0&playsinline=1`}
          allow="autoplay; encrypted-media; picture-in-picture"
          allowFullScreen
          className="h-full w-full"
          style={{ border: 0 }}
        />
      </div>
      {channelName && (
        <p className="px-4 pt-3 text-xs text-text-low">
          채널 · {channelName}
        </p>
      )}
      {trackListText && (
        <div className="px-4 py-3">
          <p className="mb-1.5 text-[0.625rem] font-semibold uppercase tracking-[0.14em] text-text-low">
            수록곡
          </p>
          <p className="whitespace-pre-line text-sm leading-relaxed text-text-mid">
            {trackListText}
          </p>
        </div>
      )}
    </div>
  );
}
