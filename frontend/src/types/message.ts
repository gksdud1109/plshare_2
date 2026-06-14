/** 1:1 쪽지(DM) 타입 — BE /api/conversations 계약과 일치. */

export interface ConversationPartner {
  handle: string;
  displayName: string;
  avatarUrl?: string | null;
}

export interface ConversationSummary {
  id: string;
  partner: ConversationPartner;
  lastMessagePreview?: string | null;
  lastMessageAt: string;
  /** 마지막 메시지를 내가 보냈는지(목록 "나: …" 접두 표시용). */
  lastMessageMine: boolean;
  unreadCount: number;
}

export interface Message {
  id: string;
  senderId: string;
  /** 내가 보낸 메시지인지(말풍선 좌/우 정렬). */
  mine: boolean;
  body: string;
  createdAt: string;
}

export interface Thread {
  conversation: ConversationSummary;
  messages: Message[];
}
