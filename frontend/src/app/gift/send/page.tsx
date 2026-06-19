"use client";

import { useEffect, useState } from "react";
import { useSessionUser } from "@/lib/auth/useSessionUser";
import { listAssets } from "@/lib/api/assets";
import { createGift } from "@/lib/api/gift";
import { demoAssets } from "@/lib/api/fixtures";
import { buildDemoGiftCreated } from "@/lib/api/fixtures-gift";
import { demoFixturesEnabled } from "@/lib/demo";
import { WrapSkinPicker } from "@/components/gift/WrapSkinPicker";
import { WRAP_SKINS, OCCASION_LABELS, type Occasion } from "@/types/gift";
import type { AssetSummary } from "@/types/asset";
import { messageFromError } from "@/lib/errors";
import { toAbsoluteUrl } from "@/lib/url";
import { useToast } from "@/components/ui/ToastProvider";

/** 선물 메시지 최대 길이(자). 편지처럼 충분히 길게. */
const MESSAGE_MAX = 3000;

type SendState =
  | { kind: "idle" }
  | { kind: "submitting" }
  | { kind: "done"; token: string; url: string };

export default function GiftSendPage() {
  const session = useSessionUser();
  const toast = useToast();
  const [assets, setAssets] = useState<AssetSummary[]>([]);
  const [assetsLoading, setAssetsLoading] = useState(true);
  const [usingFixture, setUsingFixture] = useState(false);
  const [selectedAssetId, setSelectedAssetId] = useState("");
  const [dedicationTo, setDedicationTo] = useState("");
  const [occasion, setOccasion] = useState<Occasion | "">("");
  const [message, setMessage] = useState("");
  const [wrapSkin, setWrapSkin] = useState(WRAP_SKINS[0].key);
  const [sendState, setSendState] = useState<SendState>({ kind: "idle" });
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await listAssets();
        if (!cancelled) {
          setAssets(data);
          // /create 에서 갓 만든 플레이리스트(?assetId)를 우선 선택.
          const wanted =
            typeof window !== "undefined"
              ? new URLSearchParams(window.location.search).get("assetId")
              : null;
          const pre =
            wanted && data.some((a) => a.id === wanted) ? wanted : data[0]?.id ?? "";
          if (pre) setSelectedAssetId(pre);
        }
      } catch {
        if (!cancelled && demoFixturesEnabled()) {
          setAssets(demoAssets);
          setUsingFixture(true);
          if (demoAssets.length > 0) setSelectedAssetId(demoAssets[0].id);
        }
      } finally {
        if (!cancelled) setAssetsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (session.status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg">
        <p className="text-text-low text-sm">로딩 중…</p>
      </div>
    );
  }

  if (session.status === "unauthenticated") {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-bg px-6">
        <p className="text-text-hi font-semibold text-xl">로그인이 필요해요</p>
        <p className="text-text-low text-sm">선물을 보내려면 먼저 로그인해주세요.</p>
        <a
          href="/auth/continue?returnTo=%2Fgift%2Fsend"
          className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white"
          style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
        >
          로그인하고 선물 만들기
        </a>
      </div>
    );
  }

  const remaining = MESSAGE_MAX - message.length;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedAssetId || !message.trim()) return;
    setSendState({ kind: "submitting" });
    try {
      const result = usingFixture && demoFixturesEnabled()
        ? buildDemoGiftCreated()
        : await createGift({
            assetId: selectedAssetId,
            message: message.trim(),
            wrapSkin,
            dedicationTo: dedicationTo.trim() || undefined,
            occasion: occasion || undefined,
          });
      setSendState({ kind: "done", token: result.token, url: result.url });
    } catch (err) {
      toast.error(messageFromError(err, "선물 생성에 실패했어요"));
      setSendState({ kind: "idle" });
    }
  }

  async function handleCopy(url: string) {
    try {
      const fullUrl = toAbsoluteUrl(url);
      await navigator.clipboard.writeText(fullUrl);
      setCopied(true);
      toast.success("링크를 복사했어요");
      setTimeout(() => setCopied(false), 2500);
    } catch {
      toast.error("링크 복사에 실패했어요");
    }
  }

  return (
    <div className="relative min-h-screen bg-bg">
      {/* Accent glow */}
      <div
        className="accent-glow pointer-events-none fixed left-1/4 top-0 h-[600px] w-[600px] -translate-x-1/2 opacity-30"
        aria-hidden="true"
      />

      <div className="relative mx-auto w-full max-w-xl px-6 py-16 md:px-10" style={{ zIndex: 10 }}>
        <header className="mb-10">
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            Gift
          </p>
          <h1
            className="mt-3 font-display text-text-hi"
            style={{ fontSize: "clamp(1.75rem, 4vw, 2.5rem)", fontWeight: 700, letterSpacing: "-0.02em" }}
          >
            선물 보내기
          </h1>
          <p className="mt-2 text-sm text-text-low">
            나의 플레이리스트에 감성 메시지를 담아 보내세요.
          </p>
          {usingFixture && (
            <p className="mt-4 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
              Demo data · 백엔드 연결 전
            </p>
          )}
        </header>

        {sendState.kind === "done" ? (
          /* ── 완료 뷰 ── */
          <div className="rounded-2xl border border-hairline bg-surface-1 p-8 text-center">
            <p className="text-lg font-semibold text-text-hi mb-2">선물이 준비됐어요</p>
            <p className="text-sm text-text-low mb-6">
              아래 링크를 공유하면 상대방이 언박싱할 수 있어요.
            </p>
            <div className="mb-6 rounded-xl border border-hairline bg-surface-2 px-4 py-3 text-sm text-text-mid font-mono break-all">
              {toAbsoluteUrl(sendState.url)}
            </div>
            <button
              type="button"
              onClick={() => handleCopy(sendState.url)}
              className="rounded-full bg-accent px-8 text-sm font-semibold text-white transition-all duration-200 hover:bg-accent-hi hover:-translate-y-0.5"
              style={{ height: "48px", display: "inline-flex", alignItems: "center" }}
            >
              {copied ? "복사됐어요!" : "링크 복사"}
            </button>
          </div>
        ) : (
          /* ── 폼 ── */
          <form onSubmit={handleSubmit} className="flex flex-col gap-8">
            {/* 플레이리스트 선택 */}
            <section>
              <label className="mb-3 block text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
                플레이리스트 선택
              </label>
              {assetsLoading ? (
                <p className="text-sm text-text-low">플레이리스트를 불러오는 중이에요…</p>
              ) : assets.length === 0 ? (
                <div className="rounded-xl border border-hairline bg-surface-1 p-4">
                  <p className="text-sm text-text-low">선물할 플레이리스트가 아직 없어요.</p>
                  <a href="/create" className="mt-3 inline-flex text-sm font-semibold text-accent">
                    플레이리스트 만들기
                  </a>
                </div>
              ) : (
                <div className="flex flex-col gap-2">
                  {assets.map((a) => (
                    <button
                      key={a.id}
                      type="button"
                      onClick={() => setSelectedAssetId(a.id)}
                      className="flex items-center gap-4 rounded-xl border-2 p-3 text-left transition-all duration-200"
                      style={{
                        borderColor:
                          selectedAssetId === a.id
                            ? "var(--color-accent)"
                            : "var(--color-hairline)",
                        background:
                          selectedAssetId === a.id
                            ? "var(--color-surface-2)"
                            : "transparent",
                      }}
                    >
                      {a.coverUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={a.coverUrl}
                          alt={a.title}
                          className="h-12 w-12 rounded-lg object-cover shrink-0"
                        />
                      ) : (
                        <div className="h-12 w-12 rounded-lg bg-surface-2 shrink-0 flex items-center justify-center">
                          <span className="text-xl opacity-30">♪</span>
                        </div>
                      )}
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <p className="truncate text-sm font-medium text-text-hi">{a.title}</p>
                          <span className="shrink-0 rounded-full border border-hairline px-2 py-0.5 text-[0.625rem] font-semibold text-text-low">
                            {a.assetKind === "MOOD_VIDEO" ? "영상" : `${a.trackCount}곡`}
                          </span>
                        </div>
                        <p className="mt-1 truncate text-xs text-text-low">
                          {a.assetKind === "MOOD_VIDEO"
                            ? a.moodChannelName || a.moodTrackListText?.split("\n")[0] || "YouTube 무드영상"
                            : a.previewTracks?.join(" · ") || "수록곡 미리보기 없음"}
                        </p>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </section>

            {/* 받는 사람 + 계기 (키프세이크) */}
            <section>
              <label
                htmlFor="gift-dedication"
                className="mb-3 block text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low"
              >
                받는 사람 (선택)
              </label>
              <input
                id="gift-dedication"
                type="text"
                value={dedicationTo}
                onChange={(e) => setDedicationTo(e.target.value.slice(0, 40))}
                placeholder="To. 지영에게"
                className="w-full rounded-xl border border-hairline bg-surface-1 px-4 py-3 text-sm text-text placeholder:text-text-low focus:border-accent focus:outline-none transition"
              />
              <div className="mt-3 flex flex-wrap gap-2">
                {(Object.keys(OCCASION_LABELS) as Occasion[]).map((o) => {
                  const active = occasion === o;
                  return (
                    <button
                      key={o}
                      type="button"
                      onClick={() => setOccasion(active ? "" : o)}
                      className="rounded-full border px-3 py-1.5 text-xs font-semibold transition-colors"
                      style={{
                        borderColor: active ? "var(--color-accent)" : "var(--color-hairline)",
                        background: active ? "var(--color-accent)" : "transparent",
                        color: active ? "#fff" : "var(--color-text-mid)",
                      }}
                    >
                      {OCCASION_LABELS[o]}
                    </button>
                  );
                })}
              </div>
            </section>

            {/* 메시지 */}
            <section>
              <label
                htmlFor="gift-message"
                className="mb-3 block text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low"
              >
                메시지
              </label>
              <textarea
                id="gift-message"
                value={message}
                onChange={(e) => setMessage(e.target.value.slice(0, MESSAGE_MAX))}
                placeholder="이 곡들을 고른 이유, 이 계절의 기분, 그냥 하고 싶은 말…"
                rows={4}
                className="w-full resize-none rounded-xl border border-hairline bg-surface-1 px-4 py-3 text-sm text-text placeholder:text-text-low focus:outline-none focus:ring-2 transition"
                style={{ focusRingColor: "var(--color-accent)" } as React.CSSProperties}
              />
              <p
                className="mt-1 text-right text-xs"
                style={{ color: remaining < 50 ? "var(--color-accent)" : "var(--color-text-low)" }}
              >
                {remaining}자 남음
              </p>
            </section>

            {/* 포장 스킨 */}
            <section>
              <p className="mb-3 text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
                포장 스킨
              </p>
              <WrapSkinPicker value={wrapSkin} onChange={setWrapSkin} />
            </section>

            <button
              type="submit"
              disabled={
                sendState.kind === "submitting" ||
                !selectedAssetId ||
                !message.trim()
              }
              className="rounded-full bg-accent px-8 text-sm font-semibold text-white transition-all duration-200 hover:bg-accent-hi hover:-translate-y-0.5 disabled:opacity-50"
              style={{ height: "52px", display: "inline-flex", alignItems: "center", justifyContent: "center" }}
            >
              {sendState.kind === "submitting" ? "선물 만드는 중…" : "선물 보내기"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
