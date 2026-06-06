"use client";

import { cn } from "@/lib/utils/cn";
import { EMOTION_TAGS } from "@/lib/api/fixtures";

export function EmotionTagPicker({
  value,
  onChange,
  options = [...EMOTION_TAGS],
  className,
}: {
  value: string[];
  onChange: (next: string[]) => void;
  options?: string[];
  className?: string;
}) {
  const toggle = (tag: string) => {
    if (value.includes(tag)) {
      onChange(value.filter((t) => t !== tag));
    } else {
      onChange([...value, tag]);
    }
  };
  return (
    <div className={cn("flex flex-wrap gap-2", className)}>
      {options.map((tag) => {
        const active = value.includes(tag);
        return (
          <button
            key={tag}
            type="button"
            onClick={() => toggle(tag)}
            className={cn(
              "rounded-full border px-4 py-1.5 text-xs font-medium tracking-wide",
              "transition-all duration-200 ease-[var(--ease-spring)]",
              active
                ? "border-accent text-accent"
                : "border-hairline bg-surface-2 text-text-mid hover:border-hairline-strong hover:text-text-hi",
            )}
            style={active ? { background: "var(--accent-soft)" } : undefined}
          >
            {tag}
          </button>
        );
      })}
    </div>
  );
}
