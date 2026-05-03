import type { ExportMappingStatus } from "@/types/asset";
import { cn } from "@/lib/utils/cn";

const LABELS: Record<ExportMappingStatus, string> = {
  matched: "정확히 일치",
  alternative: "대체 트랙",
  failed: "찾지 못함",
};

const STYLES: Record<ExportMappingStatus, string> = {
  matched: "border-sage-500/50 bg-sage-500/10 text-sage-500",
  alternative: "border-gold-500/50 bg-gold-500/10 text-gold-600",
  failed: "border-clay-500/50 bg-clay-500/10 text-clay-500",
};

export function MatchConfidenceBadge({
  status,
  className,
}: {
  status: ExportMappingStatus;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-1 text-[0.6875rem] tracking-wide",
        STYLES[status],
        className,
      )}
    >
      {LABELS[status]}
    </span>
  );
}
