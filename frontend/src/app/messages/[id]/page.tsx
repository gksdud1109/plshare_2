"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useSessionUser } from "@/lib/auth/useSessionUser";
import { getThread, sendMessage, markConversationRead } from "@/lib/api/messages";
import { messageFromError } from "@/lib/errors";
import { useToast } from "@/components/ui/ToastProvider";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { formatClock } from "@/lib/time";
import type { Thread, Message } from "@/types/message";

type State =
  | { kind: "loading" }
  | { kind: "ready"; thread: Thread }
  | { kind: "error"; message: string };

/** 증분 폴링 결과를 기존 메시지에 id 기준으로 병합(중복 방지). 신규 없으면 동일 참조 유지. */
function mergeMessages(existing: Message[], incoming: Message[]): Message[] {
  if (incoming.length === 0) return existing;
  const seen = new Set(existing.map((m) => m.id));
  const fresh = incoming.filter((m) => !seen.has(m.id));
  return fresh.length ? [...existing, ...fresh] : existing;
}

export default function ThreadPage() {
  const params = useParams<{ id: string }>();
  const conversationId = params.id;
  const session = useSessionUser();
  const toast = useToast();

  const [state, setState] = useState<State>({ kind: "loading" });
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const lastCountRef = useRef(0);
  const cursorRef = useRef<string | null>(null); // 마지막으로 받은 메시지 createdAt

  // 초기 전체 로드 + 증분 폴링.
  // (전송계층 중립: Realtime 도입 시 poll() 만 구독 콜백으로 대체 — 병합·읽음처리 동일 재사용.)
  useEffect(() => {
    if (session.status !== "authenticated" || !conversationId) return;
    let cancelled = false;

    const loadInitial = async () => {
      setState({ kind: "loading" });
      try {
        const thread = await getThread(conversationId);
        if (cancelled) return;
        setState({ kind: "ready", thread });
        cursorRef.current = thread.messages.at(-1)?.createdAt ?? null;
        markConversationRead(conversationId).catch(() => {});
      } catch (err) {
        if (!cancelled)
          setState({
            kind: "error",
            message: messageFromError(err, "대화를 불러오지 못했어요."),
          });
      }
    };

    const poll = async () => {
      try {
        const delta = await getThread(conversationId, cursorRef.current ?? undefined);
        if (cancelled) return;
        setState((prev) =>
          prev.kind === "ready"
            ? {
                kind: "ready",
                thread: {
                  conversation: delta.conversation,
                  messages: mergeMessages(prev.thread.messages, delta.messages),
                },
              }
            : prev,
        );
        if (delta.messages.length > 0) {
          cursorRef.current = delta.messages.at(-1)?.createdAt ?? cursorRef.current;
          // 상대가 보낸 신규가 있으면 읽음 처리
          if (delta.messages.some((m) => !m.mine))
            markConversationRead(conversationId).catch(() => {});
        }
      } catch {
        // 폴링 실패는 조용히 무시 — 다음 틱에 재시도
      }
    };

    loadInitial();
    const timer = setInterval(poll, 4000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [session.status, conversationId]);

  // 새 메시지가 늘어났을 때만 맨 아래로 스크롤.
  useEffect(() => {
    if (state.kind !== "ready") return;
    const count = state.thread.messages.length;
    if (count !== lastCountRef.current) {
      lastCountRef.current = count;
      bottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }
  }, [state]);

  async function handleSend() {
    const body = draft.trim();
    if (!body || sending || state.kind !== "ready") return;
    setSending(true);
    try {
      const message = await sendMessage(conversationId, body);
      setDraft("");
      cursorRef.current = message.createdAt;
      setState((prev) =>
        prev.kind === "ready"
          ? {
              kind: "ready",
              thread: { ...prev.thread, messages: mergeMessages(prev.thread.messages, [message]) },
            }
          : prev,
      );
    } catch (err) {
      toast.error(messageFromError(err, "메시지를 보내지 못했어요."));
    } finally {
      setSending(false);
    }
  }

  if (session.status === "unauthenticated") {
    return (
      <PageShell>
        <div className="flex flex-col items-center gap-4 py-32 text-center">
          <p className="text-xl font-semibold text-text-hi">로그인이 필요해요</p>
          <a
            href={`/auth/continue?returnTo=${encodeURIComponent(`/messages/${conversationId}`)}`}
            className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white"
            style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
          >
            로그인하기
          </a>
        </div>
      </PageShell>
    );
  }

  const partner = state.kind === "ready" ? state.thread.conversation.partner : null;

  return (
    <PageShell>
      {/* 헤더 — 뒤로 + 상대 */}
      <div className="flex items-center gap-3 py-5">
        <Link
          href="/messages"
          aria-label="대화 목록으로"
          className="flex h-9 w-9 items-center justify-center rounded-full text-text-mid transition-colors hover:bg-surface-1 hover:text-text-hi focus-ring"
        >
          <svg width="18" height="18" viewBox="0 0 16 16" fill="none" aria-hidden>
            <path d="M10 12L6 8l4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </Link>
        {partner ? (
          <>
            <div className="h-9 w-9 shrink-0 overflow-hidden rounded-full bg-surface-2">
              {partner.avatarUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={partner.avatarUrl} alt={partner.displayName} className="h-full w-full object-cover" />
              ) : (
                <span className="flex h-full w-full items-center justify-center text-sm font-semibold text-text-mid">
                  {partner.displayName.slice(0, 1)}
                </span>
              )}
            </div>
            <p className="text-base font-semibold text-text-hi">{partner.displayName}</p>
          </>
        ) : (
          <p className="text-base font-semibold text-text-hi">대화</p>
        )}
      </div>

      {state.kind === "loading" ? (
        <div className="py-24">
          <ProgressNarrative messages={["대화를 여는 중이에요…"]} />
        </div>
      ) : state.kind === "error" ? (
        <p className="py-16 text-base text-danger">{state.message}</p>
      ) : (
        <div className="flex flex-col gap-2 pb-40 pt-2">
          {state.thread.messages.length === 0 ? (
            <p className="py-16 text-center text-sm text-text-low">
              첫 메시지를 보내 대화를 시작해보세요.
            </p>
          ) : (
            state.thread.messages.map((m) => (
              <div key={m.id} className={`flex ${m.mine ? "justify-end" : "justify-start"}`}>
                <div className={`flex max-w-[78%] items-end gap-1.5 ${m.mine ? "flex-row-reverse" : ""}`}>
                  <div
                    className="whitespace-pre-wrap break-words rounded-[16px] px-3.5 py-2 text-sm leading-relaxed"
                    style={
                      m.mine
                        ? { background: "var(--color-accent)", color: "#fff", borderBottomRightRadius: 4 }
                        : { background: "var(--color-surface-2)", color: "var(--color-text-hi)", borderBottomLeftRadius: 4 }
                    }
                  >
                    {m.body}
                  </div>
                  <span className="shrink-0 pb-0.5 text-[0.625rem] text-text-low">
                    {formatClock(m.createdAt)}
                  </span>
                </div>
              </div>
            ))
          )}
          <div ref={bottomRef} />
        </div>
      )}

      {/* 입력바 */}
      <div className="fixed inset-x-0 bottom-0 z-30 border-t border-hairline bg-bg/90 backdrop-blur">
        <div className="mx-auto flex w-full max-w-3xl items-end gap-2 px-6 py-3">
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value.slice(0, 2000))}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            rows={1}
            placeholder="메시지 보내기…"
            className="max-h-32 min-h-[44px] min-w-0 flex-1 resize-none rounded-2xl border border-hairline bg-surface-1 px-4 py-2.5 text-sm text-text-hi placeholder:text-text-low outline-none focus:border-accent"
          />
          <button
            type="button"
            onClick={handleSend}
            disabled={sending || !draft.trim()}
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-accent text-white transition-all hover:bg-accent-hi disabled:opacity-40"
            aria-label="보내기"
          >
            <svg width="18" height="18" viewBox="0 0 16 16" fill="none" aria-hidden>
              <path d="M2 8h11M9 4l4 4-4 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
        </div>
      </div>
    </PageShell>
  );
}
