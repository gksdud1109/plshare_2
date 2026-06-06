import type { ExportMappingStatus } from "@/types/asset";
import { cn } from "@/lib/utils/cn";

const LABELS: Record<ExportMappingStatus, string> = {
  matched: "정확히 일치",
  alternative: "대체 트랙",
  failed: "찾지 못함",
};

const STYLES: Record<
  ExportMappingStatus,
  { pill: string; dot: string }
> = {
  matched: {
    pill: "border-success/30 bg-success/10 text-success",
    dot: "bg-success",
  },
  alternative: {
    pill: "border-warning/30 bg-warning/10 text-warning",
    dot: "bg-warning",
  },
  failed: {
    pill: "border-danger/30 bg-danger/10 text-danger",
    dot: "bg-danger",
  },
};

export function MatchConfidenceBadge({
  status,
  className,
}: {
  status: ExportMappingStatus;
  className?: string;
}) {
  const s = STYLES[status];
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[0.6875rem] font-semibold tracking-wide",
        s.pill,
        className,
      )}
    >
      <span className={cn("h-1.5 w-1.5 shrink-0 rounded-full", s.dot)} aria-hidden />
      {LABELS[status]}
    </span>
  );
}
