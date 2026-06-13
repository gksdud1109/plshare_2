"use client";

import { WRAP_SKINS, type WrapSkin } from "@/types/gift";

export function WrapSkinPicker({
  value,
  onChange,
}: {
  value: string;
  onChange: (key: string) => void;
}) {
  return (
    <div className="flex flex-wrap gap-3">
      {WRAP_SKINS.map((skin: WrapSkin) => (
        <button
          key={skin.key}
          type="button"
          onClick={() => onChange(skin.key)}
          className="flex flex-col items-center gap-2 rounded-xl border-2 p-3 transition-all duration-200"
          style={{
            borderColor: value === skin.key ? skin.accentColor : "var(--color-hairline)",
            background: value === skin.key ? "var(--color-surface-2)" : "transparent",
          }}
          aria-pressed={value === skin.key}
        >
          {/* Color swatch */}
          <span
            className="block h-10 w-10 rounded-lg"
            style={{ background: skin.gradient }}
          />
          <span className="text-[0.6875rem] font-medium text-text-mid whitespace-nowrap">
            {skin.label}
          </span>
        </button>
      ))}
    </div>
  );
}
