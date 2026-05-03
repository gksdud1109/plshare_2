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
        "transition-all duration-500 ease-[var(--ease-weighted)]",
        open ? "translate-y-0 opacity-100" : "translate-y-3 opacity-0",
        className,
      )}
    >
      <div className="pointer-events-auto rounded-full border border-stone-200 bg-ink-900 px-5 py-2.5 text-sm text-bone-50 shadow-[0_18px_40px_-20px_rgba(20,18,16,0.5)]">
        {message}
      </div>
    </div>
  );
}
