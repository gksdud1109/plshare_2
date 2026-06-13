"use client";

import { cn } from "@/lib/utils/cn";

/**
 * MoodTagChip — read-only display chip for mood tags on PostCards.
 * For interactive selection use MoodTagPicker.
 */
export function MoodTagChip({
  tag,
  className,
}: {
  tag: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5",
        "text-[0.6875rem] font-semibold tracking-wide text-accent",
        "border border-accent",
        className,
      )}
      style={{ background: "var(--accent-soft)" }}
    >
      #{tag}
    </span>
  );
}

/**
 * MoodTagPicker — interactive multi-select for post composer.
 * Options is the list of available MOOD_TAGS from fixtures-social.
 */
export function MoodTagPicker({
  value,
  onChange,
  options,
  className,
}: {
  value: string | null;
  onChange: (next: string | null) => void;
  options: readonly string[];
  className?: string;
}) {
  return (
    <div className={cn("flex flex-wrap gap-2", className)}>
      {options.map((tag) => {
        const active = value === tag;
        return (
          <button
            key={tag}
            type="button"
            onClick={() => onChange(active ? null : tag)}
            className={cn(
              "rounded-full border px-3 py-1 text-xs font-medium tracking-wide",
              "transition-all duration-200 ease-[var(--ease-spring)]",
              active
                ? "border-accent text-accent"
                : "border-hairline bg-surface-2 text-text-mid hover:border-hairline-strong hover:text-text-hi",
            )}
            style={active ? { background: "var(--accent-soft)" } : undefined}
            aria-pressed={active}
          >
            #{tag}
          </button>
        );
      })}
    </div>
  );
}
