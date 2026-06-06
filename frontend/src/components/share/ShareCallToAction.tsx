"use client";

import Link from "next/link";
import { useState } from "react";

/**
 * Client-side CTAs for the public share page.
 * Kept separate from the RSC body so the page itself can stream from the server.
 */
export function ShareCallToAction({ shareUrl }: { shareUrl?: string }) {
  const [copied, setCopied] = useState(false);

  async function handleCopy() {
    if (!shareUrl) return;
    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      // Clipboard unavailable — silently no-op.
    }
  }

  return (
    <div className="flex items-center gap-3">
      {shareUrl ? (
        <button
          type="button"
          onClick={handleCopy}
          className="glass focus-ring rounded-full border-hairline-strong px-5 py-2.5 text-sm font-medium text-text-hi transition hover:bg-surface-3"
        >
          {copied ? "링크가 복사되었어요" : "공유 링크 복사"}
        </button>
      ) : null}
      <Link
        href="/"
        className="focus-ring rounded-full bg-accent px-5 py-2.5 text-sm font-semibold text-white shadow-[var(--shadow-glow)] transition hover:bg-accent-hi"
      >
        내 라이브러리 만들기
      </Link>
    </div>
  );
}
