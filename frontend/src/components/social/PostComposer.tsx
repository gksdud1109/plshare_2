"use client";

import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils/cn";
import type { PostResponse, PostAssetEmbed } from "@/types/social";
import type { AssetSummary } from "@/types/asset";
import { createPost, getPostAssetEmbed, listMyAssets } from "@/lib/api/social";
import { MOOD_TAGS } from "@/lib/api/fixtures-social";
import { MoodTagPicker } from "./MoodTagChip";

const MAX_TEXT = 500;
const AVATAR_FALLBACK = (handle: string) =>
  `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(handle)}&backgroundColor=7c5cff&textColor=ffffff`;

interface PostComposerProps {
  userId: string;
  userHandle: string;
  userAvatarUrl?: string | null;
  onPost: (newPost: PostResponse) => void;
  className?: string;
}

export function PostComposer({
  userId,
  userHandle,
  userAvatarUrl,
  onPost,
  className,
}: PostComposerProps) {
  const [text, setText] = useState("");
  const [moodTag, setMoodTag] = useState<string | null>(null);
  const [selectedAsset, setSelectedAsset] = useState<AssetSummary | null>(null);
  const [assetEmbed, setAssetEmbed] = useState<PostAssetEmbed | null>(null);
  const [showPicker, setShowPicker] = useState(false);
  const [myAssets, setMyAssets] = useState<AssetSummary[]>([]);
  const [loadingAssets, setLoadingAssets] = useState(false);
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-grow textarea
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${el.scrollHeight}px`;
  }, [text]);

  // Load asset embed when selectedAsset changes
  useEffect(() => {
    if (!selectedAsset) {
      setAssetEmbed(null);
      return;
    }
    void getPostAssetEmbed(selectedAsset.id).then(setAssetEmbed);
  }, [selectedAsset]);

  async function loadAssets() {
    if (myAssets.length > 0) return;
    setLoadingAssets(true);
    try {
      const list = await listMyAssets();
      setMyAssets(list);
    } catch {
      setMyAssets([]);
    } finally {
      setLoadingAssets(false);
    }
  }

  async function handlePost() {
    const trimmed = text.trim();
    if (!trimmed || posting) return;
    setPosting(true);
    setError(null);
    try {
      const newPost = await createPost({
        authorId: userId,
        text: trimmed,
        assetId: selectedAsset?.id,
        moodTag: moodTag ?? undefined,
      });
      setText("");
      setMoodTag(null);
      setSelectedAsset(null);
      setAssetEmbed(null);
      setShowPicker(false);
      onPost(newPost);
    } catch {
      setError("게시에 실패했어요. 다시 시도해주세요.");
    } finally {
      setPosting(false);
    }
  }

  const canPost = text.trim().length > 0 && !posting;
  const remaining = MAX_TEXT - text.length;
  const nearLimit = remaining <= 50;

  return (
    <div
      className={cn(
        "rounded-[18px] border border-hairline bg-surface-1 p-4",
        "shadow-[var(--shadow-card)]",
        className,
      )}
    >
      <div className="flex gap-3">
        {/* Avatar */}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={userAvatarUrl ?? AVATAR_FALLBACK(userHandle)}
          alt={userHandle}
          width={36}
          height={36}
          className="h-9 w-9 shrink-0 rounded-full object-cover ring-1 ring-hairline"
        />

        {/* Input area */}
        <div className="flex flex-1 flex-col gap-3">
          <textarea
            ref={textareaRef}
            value={text}
            onChange={(e) => {
              if (e.target.value.length <= MAX_TEXT) setText(e.target.value);
            }}
            placeholder="오늘의 플레이리스트를 공유해보세요"
            rows={2}
            aria-label="포스트 작성"
            className={cn(
              "w-full resize-none overflow-hidden rounded-[10px] bg-surface-2",
              "px-3 py-2.5 text-sm text-text placeholder:text-text-low",
              "border border-hairline focus:border-accent focus:outline-none",
              "transition-colors duration-200 min-h-[72px]",
            )}
          />

          {/* Char counter */}
          <div className="flex justify-end">
            <span
              className={cn(
                "text-xs tabular-nums",
                nearLimit ? "text-warning" : "text-text-low",
                remaining < 0 && "text-danger",
              )}
            >
              {remaining}
            </span>
          </div>

          {/* Selected asset embed */}
          {selectedAsset && (
            <div className="relative flex items-center gap-3 rounded-[12px] border border-hairline bg-surface-2 p-3">
              {assetEmbed?.coverUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={assetEmbed.coverUrl}
                  alt={assetEmbed.title}
                  width={48}
                  height={48}
                  className="h-12 w-12 rounded-[8px] object-cover"
                />
              )}
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-text-hi">
                  {selectedAsset.title}
                </p>
                <p className="text-xs text-text-mid">
                  {selectedAsset.trackCount}개 트랙
                </p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedAsset(null)}
                aria-label="자산 첨부 제거"
                className="rounded-full p-1 text-text-low hover:text-text-hi transition-colors duration-150"
              >
                <CloseIcon className="h-4 w-4" />
              </button>
            </div>
          )}

          {/* Mood tag picker */}
          {showPicker && (
            <div className="rounded-[12px] border border-hairline bg-surface-2 p-3">
              <p className="mb-2 text-xs font-semibold uppercase tracking-[0.12em] text-text-low">
                무드태그
              </p>
              <MoodTagPicker
                value={moodTag}
                onChange={setMoodTag}
                options={MOOD_TAGS}
              />
            </div>
          )}

          {/* Asset picker dropdown */}
          {showPicker && (
            <div className="rounded-[12px] border border-hairline bg-surface-2 p-3">
              <p className="mb-2 text-xs font-semibold uppercase tracking-[0.12em] text-text-low">
                자산 첨부
              </p>
              {loadingAssets ? (
                <p className="text-xs text-text-low">불러오는 중…</p>
              ) : myAssets.length === 0 ? (
                <p className="text-xs text-text-low">자산이 없어요.</p>
              ) : (
                <div className="flex flex-col gap-1.5 max-h-48 overflow-y-auto">
                  {myAssets.map((asset) => (
                    <button
                      key={asset.id}
                      type="button"
                      onClick={() => {
                        setSelectedAsset(asset);
                        setShowPicker(false);
                      }}
                      className={cn(
                        "flex items-center gap-2.5 rounded-[10px] px-2.5 py-2 text-left",
                        "transition-colors duration-150 hover:bg-surface-3",
                        selectedAsset?.id === asset.id && "bg-surface-3",
                      )}
                    >
                      {asset.coverUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={asset.coverUrl}
                          alt={asset.title}
                          width={36}
                          height={36}
                          className="h-9 w-9 rounded-[6px] object-cover"
                        />
                      ) : (
                        <div className="flex h-9 w-9 items-center justify-center rounded-[6px] bg-surface-3">
                          <span className="text-sm text-text-low">♪</span>
                        </div>
                      )}
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-text-hi">
                          {asset.title}
                        </p>
                        <p className="text-xs text-text-mid">
                          {asset.trackCount}개 트랙
                        </p>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Error message */}
          {error && (
            <p className="text-xs text-danger">{error}</p>
          )}

          {/* Toolbar */}
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-1">
              {/* Asset attach toggle */}
              <button
                type="button"
                onClick={() => {
                  setShowPicker((v) => !v);
                  void loadAssets();
                }}
                aria-label="자산 첨부"
                className={cn(
                  "rounded-full p-2 text-text-mid transition-colors duration-150 hover:text-accent hover:bg-accent-soft",
                  showPicker && "text-accent",
                )}
                style={showPicker ? { background: "var(--accent-soft)" } : undefined}
              >
                <MusicIcon className="h-4 w-4" />
              </button>

              {/* Tag toggle */}
              <button
                type="button"
                onClick={() => setShowPicker((v) => !v)}
                aria-label="무드태그 선택"
                className={cn(
                  "rounded-full p-2 text-text-mid transition-colors duration-150 hover:text-accent hover:bg-accent-soft",
                  moodTag && "text-accent",
                )}
                style={moodTag ? { background: "var(--accent-soft)" } : undefined}
              >
                <TagIcon className="h-4 w-4" />
              </button>
            </div>

            <button
              type="button"
              onClick={handlePost}
              disabled={!canPost}
              className={cn(
                "rounded-full px-5 py-2 text-sm font-semibold text-white",
                "transition-all duration-200 ease-[var(--ease-spring)]",
                canPost
                  ? "bg-accent hover:bg-accent-hi hover:-translate-y-0.5 active:bg-accent-press"
                  : "bg-surface-3 text-text-low cursor-not-allowed opacity-50",
              )}
            >
              {posting ? "게시 중…" : "게시"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Icon helpers ──────────────────────────────────────────────────────

function CloseIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round">
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}

function MusicIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 18V5l12-2v13" />
      <circle cx="6" cy="18" r="3" />
      <circle cx="18" cy="16" r="3" />
    </svg>
  );
}

function TagIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
      <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
      <line x1="7" y1="7" x2="7.01" y2="7" />
    </svg>
  );
}
