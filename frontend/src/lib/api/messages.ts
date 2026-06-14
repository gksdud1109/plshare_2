import { apiFetch } from "./client";
import type { ConversationSummary, Thread, Message } from "@/types/message";

/** GET /api/conversations — 내 대화 목록(최근순 + 안읽은 수). 인증 필요. */
export async function getConversations(): Promise<ConversationSummary[]> {
  return apiFetch<ConversationSummary[]>("/api/conversations");
}

/** POST /api/conversations — 상대 handle 로 대화 get-or-create(멱등). 인증 필요. */
export async function startConversation(
  recipientHandle: string,
): Promise<ConversationSummary> {
  return apiFetch<ConversationSummary>("/api/conversations", {
    method: "POST",
    body: { recipientHandle },
  });
}

/**
 * GET /api/conversations/{id}/messages — 스레드 조회. 인증 필요.
 * after(ISO-8601)를 주면 그 시각 이후 신규 메시지만(증분 폴링). 읽음 처리는 하지 않음.
 */
export async function getThread(
  conversationId: string,
  after?: string,
): Promise<Thread> {
  const qs = after ? `?after=${encodeURIComponent(after)}` : "";
  return apiFetch<Thread>(`/api/conversations/${conversationId}/messages${qs}`);
}

/** POST /api/conversations/{id}/read — 읽음 처리(멱등). GET 조회와 분리. 인증 필요. */
export async function markConversationRead(conversationId: string): Promise<void> {
  await apiFetch<unknown>(`/api/conversations/${conversationId}/read`, {
    method: "POST",
  });
}

/** POST /api/conversations/{id}/messages — 메시지 전송. 인증 필요. */
export async function sendMessage(
  conversationId: string,
  body: string,
): Promise<Message> {
  return apiFetch<Message>(`/api/conversations/${conversationId}/messages`, {
    method: "POST",
    body: { body },
  });
}

/** GET /api/conversations/unread-count — 전체 안 읽은 수(네비 배지). 미인증이면 0. */
export async function getUnreadCount(): Promise<number> {
  try {
    const r = await apiFetch<{ total: number }>(
      "/api/conversations/unread-count",
    );
    return r.total ?? 0;
  } catch {
    return 0;
  }
}
