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
          className="rounded-full border border-stone-300 px-4 py-2 text-xs tracking-wide text-ink-700 transition-colors duration-500 hover:border-ink-900 hover:text-ink-900"
        >
          {copied ? "링크가 복사되었어요" : "공유 링크 복사"}
        </button>
      ) : null}
      <Link
        href="/"
        className="rounded-full border border-ink-900 px-4 py-2 text-xs tracking-wide text-ink-900 transition-colors duration-500 hover:bg-ink-900 hover:text-bone-50"
      >
        내 라이브러리 만들기
      </Link>
    </div>
  );
}
