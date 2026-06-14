"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getUnreadCount } from "@/lib/api/messages";

/**
 * 네비 '메시지' 링크 — 안 읽은 쪽지가 있으면 점 배지를 표시한다.
 * 미인증이면 unread-count 가 401 → getUnreadCount 가 0 으로 폴백(점 숨김).
 */
export function MessagesNavLink() {
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const load = () =>
      getUnreadCount()
        .then((n) => {
          if (!cancelled) setUnread(n);
        })
        .catch(() => {});
    load();
    const timer = setInterval(load, 15000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  return (
    <Link
      href="/messages"
      className="relative text-xs font-semibold uppercase tracking-[0.18em] text-text-mid transition-colors duration-200 hover:text-text-hi"
    >
      메시지
      {unread > 0 && (
        <span
          className="absolute -right-2.5 -top-1 h-2 w-2 rounded-full"
          style={{ background: "var(--color-accent)" }}
          aria-label={`안 읽은 쪽지 ${unread}개`}
        />
      )}
    </Link>
  );
}
