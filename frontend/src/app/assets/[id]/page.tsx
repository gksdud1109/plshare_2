"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { createShareLink, getAsset, updateAsset } from "@/lib/api/assets";
import { buildDemoAssetDetail } from "@/lib/api/fixtures";
import type { AssetDetail } from "@/types/asset";
import { PageShell } from "@/components/ui/PageShell";
import { ProgressNarrative } from "@/components/ui/ProgressNarrative";
import { TrackRow } from "@/components/ui/TrackRow";
import { EmotionTagPicker } from "@/components/ui/EmotionTagPicker";
import { Toast } from "@/components/ui/Toast";

type State =
  | { kind: "loading" }
  | { kind: "ready"; data: AssetDetail; usingFixture: boolean }
  | { kind: "error"; message: string };

export default function AssetDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [state, setState] = useState<State>({ kind: "loading" });
  const [diary, setDiary] = useState("");
  const [description, setDescription] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [toast, setToast] = useState<{ open: boolean; msg: string }>({
    open: false,
    msg: "",
  });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getAsset(id);
        if (cancelled) return;
        setState({ kind: "ready", data, usingFixture: false });
        setDiary(data.diaryText ?? "");
        setDescription(data.description ?? "");
        setTags(data.emotionTags ?? []);
      } catch {
        if (cancelled) return;
        const fallback = buildDemoAssetDetail(id);
        setState({ kind: "ready", data: fallback, usingFixture: true });
        setDiary(fallback.diaryText ?? "");
        setDescription(fallback.description ?? "");
        setTags(fallback.emotionTags ?? []);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const persist = async (
    patch: Parameters<typeof updateAsset>[1],
  ) => {
    if (state.kind !== "ready") return;
    setSaving(true);
    try {
      if (!state.usingFixture) {
        const updated = await updateAsset(id, patch);
        setState({ kind: "ready", data: updated, usingFixture: false });
      }
      setSavedAt(Date.now());
    } catch {
      setToast({ open: true, msg: "저장에 실패했어요." });
    } finally {
      setSaving(false);
    }
  };

  const handleShare = async () => {
    try {
      let url: string;
      if (state.kind === "ready" && !state.usingFixture) {
        const link = await createShareLink(id);
        url = link.url;
      } else {
        // Fixture share — use a demo token.
        url = `${window.location.origin}/share/${id}`;
      }
      await navigator.clipboard.writeText(url);
      setToast({ open: true, msg: "공유 링크를 복사했어요." });
    } catch {
      setToast({ open: true, msg: "공유 링크를 만들지 못했어요." });
    }
  };

  if (state.kind === "loading") {
    return (
      <PageShell>
        <div className="py-32">
          <ProgressNarrative messages={["자산을 불러오는 중이에요…"]} />
        </div>
      </PageShell>
    );
  }

  if (state.kind === "error") {
    return (
      <PageShell>
        <p className="py-16 text-base text-clay-500">{state.message}</p>
      </PageShell>
    );
  }

  const a = state.data;

  return (
    <PageShell>
      <Toast
        open={toast.open}
        message={toast.msg}
        onClose={() => setToast({ open: false, msg: "" })}
      />

      {state.usingFixture ? (
        <p className="mb-6 inline-flex rounded-full border border-stone-200 bg-bone-100 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-ink-500">
          Demo data · 백엔드 연결 전
        </p>
      ) : null}

      <section className="grid grid-cols-1 gap-10 py-8 md:grid-cols-[minmax(0,360px)_1fr] md:gap-14">
        <div>
          <div className="aspect-square w-full overflow-hidden rounded-[var(--radius-card)] border border-stone-200 bg-stone-200 shadow-[0_24px_60px_-30px_rgba(20,18,16,0.35)]">
            {a.coverUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={a.coverUrl}
                alt={a.title}
                className="h-full w-full object-cover"
              />
            ) : null}
          </div>
        </div>

        <div className="flex flex-col">
          <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
            Asset
          </p>
          <h1 className="mt-3 text-4xl leading-tight md:text-5xl">{a.title}</h1>
          <p className="mt-3 text-sm text-ink-500">
            {a.tracks.length} tracks
          </p>

          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              href={`/assets/${a.id}/export`}
              className="rounded-full bg-ink-900 px-5 py-2.5 text-sm tracking-wide text-bone-50 transition-colors duration-500 hover:bg-ink-700"
            >
              Apple Music으로 내보내기
            </Link>
            <button
              type="button"
              onClick={handleShare}
              className="rounded-full border border-stone-300 px-5 py-2.5 text-sm text-ink-700 transition-colors duration-500 hover:bg-bone-100"
            >
              공유 링크 만들기
            </button>
          </div>
        </div>
      </section>

      <section className="mt-12 grid grid-cols-1 gap-10 md:grid-cols-[minmax(0,360px)_1fr] md:gap-14">
        <aside className="md:order-2">
          <div className="rounded-[var(--radius-card)] border border-stone-200 bg-bone-100 p-6 md:p-8">
            <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
              Emotional Context
            </p>

            <label className="mt-6 block text-xs uppercase tracking-[0.18em] text-ink-500">
              한 줄 요약
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              onBlur={() => persist({ description })}
              placeholder="예: 혼자 걷던 새벽들"
              className="mt-2 w-full border-b border-stone-300 bg-transparent py-2 font-serif text-lg italic text-ink-800 outline-none transition-colors duration-500 focus:border-ink-900"
            />

            <label className="mt-8 block text-xs uppercase tracking-[0.18em] text-ink-500">
              이 자산의 이야기
            </label>
            <textarea
              value={diary}
              onChange={(e) => setDiary(e.target.value)}
              onBlur={() => persist({ diaryText: diary })}
              rows={6}
              placeholder="이 플레이리스트가 함께한 시간을 남겨두세요."
              className="mt-2 w-full resize-none rounded-md border border-stone-200 bg-bone-50 p-4 font-sans text-[0.95rem] leading-relaxed text-ink-800 outline-none transition-colors duration-500 focus:border-ink-900"
            />

            <label className="mt-8 block text-xs uppercase tracking-[0.18em] text-ink-500">
              감정 태그
            </label>
            <EmotionTagPicker
              className="mt-3"
              value={tags}
              onChange={(next) => {
                setTags(next);
                persist({ emotionTags: next });
              }}
            />

            <p className="mt-6 text-xs text-ink-400">
              {saving
                ? "저장 중…"
                : savedAt
                  ? "저장됨"
                  : "변경 사항은 자동으로 저장돼요."}
            </p>
          </div>
        </aside>

        <div className="md:order-1">
          <p className="text-xs uppercase tracking-[0.24em] text-ink-500">
            Tracks
          </p>
          <div className="mt-4 rounded-[var(--radius-card)] border border-stone-200 bg-bone-100 px-6 md:px-8">
            {a.tracks.map((t, i) => (
              <TrackRow key={t.id} track={t} index={i} />
            ))}
          </div>
        </div>
      </section>
    </PageShell>
  );
}
