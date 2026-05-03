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
              "rounded-full border px-4 py-1.5 text-xs tracking-wide",
              "transition-all duration-500 ease-[var(--ease-weighted)]",
              active
                ? "border-ink-900 bg-ink-900 text-bone-50"
                : "border-stone-200 bg-bone-50 text-ink-600 hover:border-ink-400",
            )}
          >
            {tag}
          </button>
        );
      })}
    </div>
  );
}
