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
    <p
      className={cn(
        "font-serif text-2xl italic leading-relaxed text-ink-700 md:text-3xl",
        "transition-opacity duration-500 ease-[var(--ease-weighted)]",
        visible ? "opacity-100" : "opacity-0",
        className,
      )}
    >
      {messages[index] ?? ""}
    </p>
  );
}
