"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import Link from "next/link";
import { openGift, saveGift } from "@/lib/api/gift";
import { buildDemoGiftView } from "@/lib/api/fixtures-gift";
import { useSessionUser } from "@/lib/auth/useSessionUser";
import { UnboxingView } from "@/components/gift/UnboxingView";
import type { GiftView } from "@/types/gift";

type PageState =
  | { kind: "loading" }
  | { kind: "not-found" }
  | { kind: "success"; gift: GiftView };

export default function GiftUnboxPage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = use(params);
  const session = useSessionUser();

  const [pageState, setPageState] = useState<PageState>({ kind: "loading" });
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const gift = await openGift(token);
        if (!cancelled) setPageState({ kind: "success", gift });
      } catch (err: unknown) {
        if (cancelled) return;
        // 404 → not-found; anything else → fall back to demo fixture
        const status = err instanceof Error && "status" in err
          ? (err as { status: number }).status
          : 0;
        if (status === 404) {
          setPageState({ kind: "not-found" });
        } else {
          setPageState({ kind: "success", gift: buildDemoGiftView(token) });
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token]);

  async function handleSave() {
    if (session.status !== "authenticated") return;
    if (pageState.kind !== "success") return;
    setSaving(true);
    try {
      const updated = await saveGift(token, session.user.userId);
      setPageState({ kind: "success", gift: updated });
      setSaved(true);
    } catch {
      setSaved(true); // demo graceful degradation
    } finally {
      setSaving(false);
    }
  }

  if (pageState.kind === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg">
        <p className="text-text-low text-sm animate-pulse">선물을 여는 중이에요…</p>
      </div>
    );
  }

  if (pageState.kind === "not-found") {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-bg px-6 text-center">
        <p className="text-text-hi font-semibold text-xl">선물을 찾을 수 없어요</p>
        <p className="text-text-low text-sm">링크가 만료됐거나 잘못됐을 수 있어요.</p>
        <Link
          href="/"
          className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white"
          style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
        >
          홈으로
        </Link>
      </div>
    );
  }

  return (
    <UnboxingView
      gift={pageState.gift}
      onSave={handleSave}
      saving={saving}
      saved={saved}
      isAuthenticated={session.status === "authenticated"}
    />
  );
}
