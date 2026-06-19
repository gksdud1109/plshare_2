"use client";

import { useState } from "react";
import Link from "next/link";
import { cn } from "@/lib/utils/cn";
import type { PostResponse, PostAssetEmbed } from "@/types/social";
import { MoodTagChip } from "./MoodTagChip";
import { formatRelativeTime } from "@/lib/api/fixtures-social";
import { likePost, unlikePost } from "@/lib/api/social";

interface PostCardProps {
  post: PostResponse;
  assetEmbed?: PostAssetEmbed | null;
  viewerId?: string | null;
  /** If true, renders as detail view (no truncation, no link) */
  detail?: boolean;
  className?: string;
  /** Called after optimistic like toggle completes (with server-confirmed count) */
  onLikeChange?: (postId: string, liked: boolean, count: number) => void;
}

const AVATAR_FALLBACK = (handle: string) =>
  `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(handle)}&backgroundColor=7c5cff&textColor=ffffff`;

export function PostCard({
  post,
  assetEmbed,
  viewerId,
  detail = false,
  className,
  onLikeChange,
}: PostCardProps) {
  const [liked, setLiked] = useState(post.likedByMe);
  const [likeCount, setLikeCount] = useState(post.likeCount);
  const [liking, setLiking] = useState(false);
  const [expanded, setExpanded] = useState(detail);

  const TEXT_LIMIT = 160;
  const needsExpand = !detail && post.text.length > TEXT_LIMIT;
  const displayText =
    needsExpand && !expanded
      ? post.text.slice(0, TEXT_LIMIT) + "…"
      : post.text;

  async function handleLike() {
    if (!viewerId || liking) return;
    setLiking(true);
    const wasLiked = liked;
    // Optimistic update
    setLiked(!wasLiked);
    setLikeCount((c) => c + (wasLiked ? -1 : 1));
    try {
      const serverCount = wasLiked
        ? await unlikePost(post.id, viewerId)
        : await likePost(post.id, viewerId);
      setLikeCount(serverCount);
      onLikeChange?.(post.id, !wasLiked, serverCount);
    } catch {
      // Revert on failure
      setLiked(wasLiked);
      setLikeCount((c) => c + (wasLiked ? 1 : -1));
    } finally {
      setLiking(false);
    }
  }

  const avatarSrc =
    post.authorAvatarUrl ?? AVATAR_FALLBACK(post.authorHandle);

  return (
    <article
      className={cn(
        "relative flex flex-col gap-3 overflow-hidden rounded-[18px]",
        "border border-hairline bg-surface-1",
        "shadow-[var(--shadow-card)]",
        !detail &&
          "transition-[transform,box-shadow,border-color] duration-300 ease-[var(--ease-spring)] hover:-translate-y-0.5 hover:border-hairline-strong hover:shadow-[var(--shadow-pop)]",
        "p-4",
        className,
      )}
    >
      {/* Author row */}
      <div className="flex items-center justify-between gap-3">
        <Link
          href={`/u/${post.authorHandle}`}
          className="flex items-center gap-2.5 group"
        >
          {/* eslint-disable-next-line @next/next/no-img-element -- external avatar URLs, no next/image domain config */}
          <img
            src={avatarSrc}
            alt={post.authorDisplayName}
            width={32}
            height={32}
            className="h-8 w-8 rounded-full object-cover ring-1 ring-hairline group-hover:ring-accent transition-all duration-200"
          />
          <div className="flex flex-col leading-tight">
            <span className="text-[0.8125rem] font-semibold text-text-hi group-hover:text-accent transition-colors duration-200">
              {post.authorDisplayName}
            </span>
            <span className="text-xs text-text-mid">@{post.authorHandle}</span>
          </div>
        </Link>
        <time
          dateTime={post.createdAt}
          className="text-xs text-text-low tabular-nums shrink-0"
        >
          {formatRelativeTime(post.createdAt)}
        </time>
      </div>

      {/* Post text */}
      <div className="text-sm leading-relaxed text-text">
        {detail ? (
          <p className="whitespace-pre-wrap">{post.text}</p>
        ) : (
          <p>
            {displayText}
            {needsExpand && !expanded && (
              <button
                type="button"
                onClick={() => setExpanded(true)}
                className="ml-1 text-accent hover:text-accent-hi text-xs font-medium"
              >
                더 보기
              </button>
            )}
          </p>
        )}
      </div>

      {/* Mood tag */}
      {post.moodTag && (
        <div>
          <MoodTagChip tag={post.moodTag} />
        </div>
      )}

      {/* Embedded asset card */}
      {assetEmbed && (
        <AssetEmbedCard asset={assetEmbed} />
      )}

      {/* Action row */}
      <div className="flex items-center gap-5 pt-1 text-sm text-text-mid">
        {/* Like */}
        <button
          type="button"
          onClick={handleLike}
          disabled={!viewerId || liking}
          aria-pressed={liked}
          aria-label={liked ? "좋아요 취소" : "좋아요"}
          className={cn(
            "flex items-center gap-1.5 tabular-nums transition-colors duration-150",
            liked ? "text-accent" : "hover:text-text-hi",
            (!viewerId) && "cursor-default",
          )}
        >
          <HeartIcon filled={liked} className="h-4 w-4" />
          <span className="text-xs font-medium">{likeCount}</span>
        </button>

        {/* Comment */}
        {detail ? (
          <span className="flex items-center gap-1.5 tabular-nums">
            <CommentIcon className="h-4 w-4" />
            <span className="text-xs font-medium">{post.commentCount}</span>
          </span>
        ) : (
          <Link
            href={`/p/${post.id}`}
            className="flex items-center gap-1.5 tabular-nums hover:text-text-hi transition-colors duration-150"
            aria-label="댓글 보기"
          >
            <CommentIcon className="h-4 w-4" />
            <span className="text-xs font-medium">{post.commentCount}</span>
          </Link>
        )}

        {/* Share */}
        <button
          type="button"
          onClick={() => {
            if (typeof navigator !== "undefined" && navigator.share) {
              void navigator.share({
                url: `${window.location.origin}/p/${post.id}`,
              });
            }
          }}
          className="ml-auto hover:text-text-hi transition-colors duration-150"
          aria-label="공유"
        >
          <ShareIcon className="h-4 w-4" />
        </button>
      </div>
    </article>
  );
}

// ── Embedded asset mini-card ───────────────────────────────────────────

function AssetEmbedCard({ asset }: { asset: PostAssetEmbed }) {
  return (
    <Link
      href={`/assets/${asset.id}`}
      className="relative flex items-center gap-3 overflow-hidden rounded-[14px] border border-hairline bg-surface-2 p-3 transition-colors duration-200 hover:border-accent-soft hover:bg-surface-3 focus-ring"
    >
      {/* Ambient cover glow */}
      {asset.coverUrl && (
        <img
          src={asset.coverUrl}
          alt=""
          aria-hidden
          className="cover-glow pointer-events-none absolute inset-0 h-full w-full object-cover opacity-20"
        />
      )}
      <div className="relative shrink-0">
        {asset.coverUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={asset.coverUrl}
            alt={asset.title}
            width={64}
            height={64}
            className="h-16 w-16 rounded-[10px] object-cover shadow-[var(--shadow-card)]"
          />
        ) : (
          <div className="flex h-16 w-16 items-center justify-center rounded-[10px] bg-surface-3">
            <span className="text-2xl text-text-low">♪</span>
          </div>
        )}
      </div>
      <div className="relative flex min-w-0 flex-col gap-1">
        <p className="truncate text-[0.9375rem] font-semibold text-text-hi leading-tight">
          {asset.title}
        </p>
        <p className="text-xs text-text-mid">
          {asset.assetKind === "MOOD_VIDEO" ? "무드영상" : `트랙 ${asset.trackCount}개`}
        </p>
        {asset.emotionTags.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-0.5">
            {asset.emotionTags.slice(0, 3).map((tag) => (
              <MoodTagChip key={tag} tag={tag} />
            ))}
          </div>
        )}
      </div>
    </Link>
  );
}

// ── SVG icon helpers ──────────────────────────────────────────────────

function HeartIcon({
  filled,
  className,
}: {
  filled: boolean;
  className?: string;
}) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={className}
      aria-hidden
      fill={filled ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
    </svg>
  );
}

function CommentIcon({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={className}
      aria-hidden
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
    </svg>
  );
}

function ShareIcon({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={className}
      aria-hidden
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" />
      <polyline points="16 6 12 2 8 6" />
      <line x1="12" y1="2" x2="12" y2="15" />
    </svg>
  );
}
