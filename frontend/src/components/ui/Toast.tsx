"use client";

import { useEffect } from "react";
import { cn } from "@/lib/utils/cn";

export function Toast({
  message,
  open,
  onClose,
  durationMs = 2400,
  className,
}: {
  message: string;
  open: boolean;
  onClose: () => void;
  durationMs?: number;
  className?: string;
}) {
  useEffect(() => {
    if (!open) return;
    const t = setTimeout(onClose, durationMs);
    return () => clearTimeout(t);
  }, [open, durationMs, onClose]);

  return (
    <div
      aria-live="polite"
      className={cn(
        "pointer-events-none fixed inset-x-0 bottom-8 z-50 flex justify-center",
        "transition-all duration-500 ease-[var(--ease-out)]",
        open ? "translate-y-0 opacity-100" : "translate-y-4 opacity-0",
        className,
      )}
    >
      <div
        className={cn(
          "pointer-events-auto glass",
          "flex items-center gap-3 rounded-2xl px-5 py-3",
          "border-l-2 border-l-accent",
          "shadow-[var(--shadow-pop)]",
          "text-sm font-medium text-text-hi",
        )}
      >
        {message}
      </div>
    </div>
  );
}
