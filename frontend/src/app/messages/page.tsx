"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useSessionUser } from "@/lib/auth/useSessionUser";
import { getConversations } from "@/lib/api/messages";
import { messageFromError } from "@/lib/errors";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { formatTimeAgo } from "@/lib/time";
import type { ConversationSummary } from "@/types/message";

type State =
  | { kind: "loading" }
  | { kind: "ready"; conversations: ConversationSummary[] }
  | { kind: "error"; message: string };

export default function MessagesPage() {
  const session = useSessionUser();
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    if (session.status !== "authenticated") return;
    let cancelled = false;

    const load = async (initial: boolean) => {
      if (initial) setState({ kind: "loading" });
      try {
        const conversations = await getConversations();
        if (!cancelled) setState({ kind: "ready", conversations });
      } catch (err) {
        if (!cancelled && initial)
          setState({
            kind: "error",
            message: messageFromError(err, "대화 목록을 불러오지 못했어요."),
          });
      }
    };

    load(true);
    // 가벼운 폴링 — 새 메시지/안읽음 갱신.
    const timer = setInterval(() => load(false), 6000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [session.status]);

  if (session.status === "unauthenticated") {
    return (
      <PageShell>
        <div className="flex flex-col items-center gap-4 py-32 text-center">
          <p className="text-xl font-semibold text-text-hi">로그인이 필요해요</p>
          <p className="text-sm text-text-low">쪽지를 주고받으려면 먼저 로그인해주세요.</p>
          <a
            href="/auth/continue?returnTo=%2Fmessages"
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
          Messages
        </p>
        <h1
          className="mt-3 font-display text-text-hi"
          style={{ fontSize: "clamp(1.75rem, 4vw, 2.5rem)", fontWeight: 700, letterSpacing: "-0.02em" }}
        >
          쪽지
        </h1>
        <p className="mt-2 text-sm text-text-low">
          선물을 주고받은 사람들과 나눈 대화가 여기 모여요.
        </p>
      </header>

      {state.kind === "loading" ? (
        <div className="py-24">
          <ProgressNarrative messages={["대화를 불러오는 중이에요…"]} />
        </div>
      ) : state.kind === "error" ? (
        <p className="py-16 text-base text-danger">{state.message}</p>
      ) : state.conversations.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-24 text-center">
          <span className="text-4xl opacity-20">✉️</span>
          <p className="text-base text-text-mid">아직 주고받은 쪽지가 없어요.</p>
          <Link href="/library" className="text-sm font-semibold text-accent hover:text-accent-hi">
            받은 선물에서 답장 보내기 →
          </Link>
        </div>
      ) : (
        <ul className="flex flex-col gap-1 pb-16">
          {state.conversations.map((c) => (
            <li key={c.id}>
              <ConversationRow conversation={c} />
            </li>
          ))}
        </ul>
      )}
    </PageShell>
  );
}

function ConversationRow({ conversation: c }: { conversation: ConversationSummary }) {
  const unread = c.unreadCount > 0;
  const preview = c.lastMessagePreview ?? "";
  return (
    <Link
      href={`/messages/${c.id}`}
      className="flex items-center gap-3 rounded-[14px] border border-transparent px-3 py-3 transition-colors duration-150 hover:border-hairline hover:bg-surface-1 focus-ring"
    >
      <Avatar partner={c.partner} />
      <div className="min-w-0 flex-1">
        <div className="flex items-baseline gap-2">
          <p className="truncate text-[0.9375rem] font-semibold text-text-hi">
            {c.partner.displayName}
          </p>
          <span className="ml-auto shrink-0 text-[0.6875rem] text-text-low">
            {formatTimeAgo(c.lastMessageAt)}
          </span>
        </div>
        <div className="mt-0.5 flex items-center gap-2">
          <p
            className={`truncate text-sm ${unread ? "font-medium text-text-mid" : "text-text-low"}`}
          >
            {c.lastMessageMine && preview ? "나: " : ""}
            {preview}
          </p>
          {unread && (
            <span
              className="ml-auto flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full px-1.5 text-[0.625rem] font-bold text-white"
              style={{ background: "var(--color-accent)" }}
            >
              {c.unreadCount}
            </span>
          )}
        </div>
      </div>
    </Link>
  );
}

function Avatar({ partner }: { partner: ConversationSummary["partner"] }) {
  return (
    <div className="relative h-12 w-12 shrink-0 overflow-hidden rounded-full bg-surface-2">
      {partner.avatarUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={partner.avatarUrl} alt={partner.displayName} className="h-full w-full object-cover" />
      ) : (
        <span className="flex h-full w-full items-center justify-center text-base font-semibold text-text-mid">
          {partner.displayName.slice(0, 1)}
        </span>
      )}
    </div>
  );
}
