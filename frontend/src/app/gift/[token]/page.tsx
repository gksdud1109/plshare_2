"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import Link from "next/link";
import { getGift, openGift, saveGift } from "@/lib/api/gift";
import { buildDemoGiftView } from "@/lib/api/fixtures-gift";
import { useSessionUser } from "@/lib/auth/useSessionUser";
import { demoFixturesEnabled } from "@/lib/demo";
import { UnboxingView } from "@/components/gift/UnboxingView";
import { WrappedCover } from "@/components/gift/WrappedCover";
import type { GiftView } from "@/types/gift";

type PageState =
  | { kind: "loading" }
  | { kind: "not-found" }
  | { kind: "wrapped"; gift: GiftView }
  | { kind: "opened"; gift: GiftView };

export default function GiftUnboxPage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = use(params);
  const session = useSessionUser();

  const [pageState, setPageState] = useState<PageState>({ kind: "loading" });
  const [opening, setOpening] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        // 읽기 전용 조회 — 포장된 상태로 진입(아직 열지 않음).
        const gift = await getGift(token);
        if (!cancelled) setPageState({ kind: "wrapped", gift });
      } catch (err: unknown) {
        if (cancelled) return;
        const status =
          err instanceof Error && "status" in err
            ? (err as { status: number }).status
            : 0;
        if (status === 404) {
          setPageState({ kind: "not-found" });
        } else if (demoFixturesEnabled()) {
          setPageState({ kind: "wrapped", gift: buildDemoGiftView(token) });
        } else {
          setPageState({ kind: "not-found" });
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token]);

  async function handleOpen() {
    if (pageState.kind !== "wrapped") return;
    setOpening(true);
    try {
      // 멱등 — 선물을 OPENED로 마킹하고 언박싱으로 전환.
      const opened = await openGift(token);
      setPageState({ kind: "opened", gift: opened });
    } catch {
      // 데모/네트워크 폴백: 받은 gift 그대로 연다.
      setPageState({ kind: "opened", gift: pageState.gift });
    } finally {
      setOpening(false);
    }
  }

  async function handleSave() {
    if (session.status !== "authenticated") return;
    if (pageState.kind !== "opened") return;
    setSaving(true);
    try {
      const updated = await saveGift(token, session.user.userId);
      setPageState({ kind: "opened", gift: updated });
      setSaved(true);
    } catch {
      if (demoFixturesEnabled()) setSaved(true);
    } finally {
      setSaving(false);
    }
  }

  if (pageState.kind === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg">
        <p className="text-text-low text-sm animate-pulse">선물을 불러오는 중이에요…</p>
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

  if (pageState.kind === "wrapped") {
    return (
      <WrappedCover gift={pageState.gift} onOpen={handleOpen} opening={opening} />
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
