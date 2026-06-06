"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils/cn";

/**
 * ProgressNarrative — sentence-form progress indicator.
 * Cycles through messages with a stagger fade. No spinners.
 */
export function ProgressNarrative({
  messages,
  intervalMs = 2400,
  active = true,
  className,
}: {
  messages: string[];
  intervalMs?: number;
  active?: boolean;
  className?: string;
}) {
  const [index, setIndex] = useState(0);
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    if (!active || messages.length <= 1) return;
    const fadeOut = setInterval(() => {
      setVisible(false);
      setTimeout(() => {
        setIndex((i) => (i + 1) % messages.length);
        setVisible(true);
      }, 400);
    }, intervalMs);
    return () => clearInterval(fadeOut);
  }, [active, intervalMs, messages.length]);

  return (
    <div className={cn("flex flex-col items-center gap-4", className)}>
      <p
        className={cn(
          "text-center text-2xl font-semibold leading-relaxed text-text-hi md:text-3xl",
          "transition-opacity duration-500 ease-[var(--ease-out)]",
          visible ? "opacity-100" : "opacity-0",
        )}
      >
        {messages[index] ?? ""}
      </p>

      {/* slim accent progress bar with glow — no spinner */}
      {active && (
        <div className="relative h-0.5 w-48 overflow-hidden rounded-full bg-surface-3">
          <div
            className="animate-pulse-glow absolute inset-y-0 left-0 rounded-full bg-accent"
            style={{
              width: "40%",
              boxShadow: "var(--shadow-glow)",
              animation: "progressSlide 2.4s ease-in-out infinite",
            }}
          />
        </div>
      )}

      <style>{`
        @keyframes progressSlide {
          0%   { transform: translateX(-100%); width: 40%; }
          50%  { transform: translateX(150%); width: 60%; }
          100% { transform: translateX(300%); width: 40%; }
        }
      `}</style>
    </div>
  );
}
