"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSessionUser } from "@/lib/auth/useSessionUser";
import { getReceivedGifts, getSentGifts } from "@/lib/api/gift";
import { startConversation } from "@/lib/api/messages";
import { messageFromError } from "@/lib/errors";
import { useToast } from "@/components/ui/ToastProvider";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { WRAP_SKINS, type GiftSummary } from "@/types/gift";

type Tab = "received" | "sent";
type State =
  | { kind: "loading" }
  | { kind: "ready"; gifts: GiftSummary[] }
  | { kind: "error"; message: string };

function wrapAccent(wrapSkin: string): string {
  return (
    WRAP_SKINS.find((w) => w.key === wrapSkin)?.accentColor ??
    "var(--color-accent)"
  );
}

const STATUS_LABEL: Record<GiftSummary["status"], string> = {
  CREATED: "아직 안 열림",
  OPENED: "열어봤어요",
  SAVED: "보관함에 담음",
};

export default function LibraryPage() {
  const session = useSessionUser();
  const router = useRouter();
  const toast = useToast();
  const [tab, setTab] = useState<Tab>("received");
  const [state, setState] = useState<State>({ kind: "loading" });
  const [replyingTo, setReplyingTo] = useState<string | null>(null);

  async function handleReply(handle: string) {
    setReplyingTo(handle);
    try {
      const conv = await startConversation(handle);
      router.push(`/messages/${conv.id}`);
    } catch (err) {
      toast.error(messageFromError(err, "답장을 시작하지 못했어요."));
      setReplyingTo(null);
    }
  }

  useEffect(() => {
    if (session.status !== "authenticated") return;
    let cancelled = false;
    (async () => {
      setState({ kind: "loading" });
      try {
        const gifts =
          tab === "received" ? await getReceivedGifts() : await getSentGifts();
        if (!cancelled) setState({ kind: "ready", gifts });
      } catch (err) {
        if (!cancelled)
          setState({
            kind: "error",
            message: messageFromError(err, "선물함을 불러오지 못했어요."),
          });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [tab, session.status]);

  if (session.status === "unauthenticated") {
    return (
      <PageShell>
        <div className="flex flex-col items-center gap-4 py-32 text-center">
          <p className="text-xl font-semibold text-text-hi">로그인이 필요해요</p>
          <p className="text-sm text-text-low">
            받은 선물과 보낸 선물을 보려면 먼저 로그인해주세요.
          </p>
          <a
            href="/auth/continue?returnTo=%2Flibrary"
            className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white"
            style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
          >
            로그인하기
          </a>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <header className="py-8">
        <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
          Library
        </p>
        <h1
          className="mt-3 font-display text-text-hi"
          style={{ fontSize: "clamp(1.75rem, 4vw, 2.5rem)", fontWeight: 700, letterSpacing: "-0.02em" }}
        >
          선물함
        </h1>
        <p className="mt-2 text-sm text-text-low">
          주고받은 음악 선물이 여기 모여요.
        </p>
      </header>

      {/* Tabs */}
      <div className="mb-8 inline-flex gap-1 rounded-full border border-hairline bg-surface-1 p-1">
        {(["received", "sent"] as const).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTab(t)}
            className="rounded-full px-5 py-2 text-sm font-semibold transition-colors duration-200 focus-ring"
            style={{
              background: tab === t ? "var(--color-accent)" : "transparent",
              color: tab === t ? "#fff" : "var(--color-text-mid)",
            }}
          >
            {t === "received" ? "받은 선물" : "보낸 선물"}
          </button>
        ))}
      </div>

      {state.kind === "loading" ? (
        <div className="py-24">
          <ProgressNarrative messages={["선물함을 여는 중이에요…"]} />
        </div>
      ) : state.kind === "error" ? (
        <p className="py-16 text-base text-danger">{state.message}</p>
      ) : state.gifts.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-24 text-center">
          <span className="text-4xl opacity-20">🎁</span>
          <p className="text-base text-text-mid">
            {tab === "received"
              ? "아직 받은 선물이 없어요."
              : "아직 보낸 선물이 없어요."}
          </p>
          {tab === "sent" && (
            <Link href="/gift/send" className="text-sm font-semibold text-accent hover:text-accent-hi">
              첫 선물 보내기 →
            </Link>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 pb-16 sm:grid-cols-2">
          {state.gifts.map((g) => (
            <GiftCard
              key={g.token}
              gift={g}
              tab={tab}
              onReply={tab === "received" ? handleReply : undefined}
              replying={replyingTo === g.sender.handle}
            />
          ))}
        </div>
      )}
    </PageShell>
  );
}

function GiftCard({
  gift,
  tab,
  onReply,
  replying,
}: {
  gift: GiftSummary;
  tab: Tab;
  onReply?: (handle: string) => void;
  replying?: boolean;
}) {
  const accent = wrapAccent(gift.wrapSkin);
  return (
    <div
      className="group relative flex flex-col overflow-hidden rounded-[16px] border bg-surface-1 transition-all duration-200 hover:-translate-y-0.5"
      style={{ borderColor: "var(--color-hairline)", boxShadow: "var(--shadow-card)" }}
    >
      {/* Wrap accent edge */}
      <span
        className="absolute inset-y-0 left-0 z-10 w-1"
        style={{ background: accent }}
        aria-hidden
      />
      <Link href={`/gift/${gift.token}`} className="flex gap-4 p-4 focus-ring">
        <div className="relative h-20 w-20 shrink-0 overflow-hidden rounded-[12px] bg-surface-2">
          {gift.assetCoverUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={gift.assetCoverUrl} alt={gift.assetTitle} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-2xl text-text-low">♪</div>
          )}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span
              className="rounded-full px-2 py-0.5 text-[0.625rem] font-semibold uppercase tracking-wide"
              style={{ background: "var(--color-surface-3)", color: accent }}
            >
              {STATUS_LABEL[gift.status]}
            </span>
          </div>
          <p className="mt-1.5 truncate text-[0.9375rem] font-semibold text-text-hi">
            {gift.assetTitle}
          </p>
          <p className="mt-0.5 text-xs text-text-mid">
            {tab === "received"
              ? `${gift.sender.displayName}님이 보냈어요`
              : `${gift.trackCount}곡 · 내가 보낸 선물`}
          </p>
          <p className="mt-2 line-clamp-2 text-xs text-text-low">{gift.message}</p>
        </div>
      </Link>
      {onReply && (
        <div className="border-t border-hairline px-4 py-2.5">
          <button
            type="button"
            onClick={() => onReply(gift.sender.handle)}
            disabled={replying}
            className="flex items-center gap-1.5 text-xs font-semibold text-accent transition-colors hover:text-accent-hi disabled:opacity-50 focus-ring"
          >
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden>
              <path d="M8 13s-5-3-5-6.5A2.5 2.5 0 018 4a2.5 2.5 0 015 2.5C13 10 8 13 8 13z" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            {replying ? "여는 중…" : `${gift.sender.displayName}님에게 답장`}
          </button>
        </div>
      )}
    </div>
  );
}
