"use client";

import { useState } from "react";
import { cn } from "@/lib/utils/cn";
import type { CommentResponse } from "@/types/social";
import { createComment, deleteComment } from "@/lib/api/social";
import { formatRelativeTime } from "@/lib/api/fixtures-social";

const AVATAR_FALLBACK = (handle: string) =>
  `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(handle)}&backgroundColor=7c5cff&textColor=ffffff`;

interface CommentSectionProps {
  postId: string;
  initialComments: CommentResponse[];
  viewerId?: string | null;
  className?: string;
}

export function CommentSection({
  postId,
  initialComments,
  viewerId,
  className,
}: CommentSectionProps) {
  const [comments, setComments] = useState<CommentResponse[]>(initialComments);
  const [text, setText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed || !viewerId || submitting) return;
    setSubmitting(true);
    setError(null);
    // Optimistic add
    const tempId = `temp-${Date.now()}`;
    const optimistic: CommentResponse = {
      id: tempId,
      postId,
      authorId: viewerId,
      authorHandle: "me",
      authorDisplayName: "나",
      authorAvatarUrl: null,
      text: trimmed,
      deleted: false,
      createdAt: new Date().toISOString(),
    };
    setComments((prev) => [...prev, optimistic]);
    setText("");
    try {
      const confirmed = await createComment(postId, {
        authorId: viewerId,
        text: trimmed,
      });
      setComments((prev) =>
        prev.map((c) => (c.id === tempId ? confirmed : c)),
      );
    } catch {
      setComments((prev) => prev.filter((c) => c.id !== tempId));
      setText(trimmed);
      setError("댓글 게시에 실패했어요. 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(commentId: string) {
    if (!viewerId) return;
    setComments((prev) => prev.filter((c) => c.id !== commentId));
    try {
      await deleteComment(commentId, viewerId);
    } catch {
      // silently ignore — server-side soft delete may already be done
    }
  }

  return (
    <section className={cn("flex flex-col gap-4", className)}>
      <h3 className="text-sm font-semibold uppercase tracking-[0.12em] text-text-low">
        댓글 {comments.length > 0 ? `(${comments.length})` : ""}
      </h3>

      {/* Comment list */}
      {comments.length === 0 ? (
        <p className="text-sm text-text-low">
          아직 댓글이 없어요. 첫 댓글을 남겨보세요.
        </p>
      ) : (
        <ul className="flex flex-col gap-3">
          {comments.map((comment) => (
            <li key={comment.id} className="flex gap-3">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={
                  comment.authorAvatarUrl ??
                  AVATAR_FALLBACK(comment.authorHandle)
                }
                alt={comment.authorDisplayName}
                width={28}
                height={28}
                className="h-7 w-7 shrink-0 rounded-full object-cover ring-1 ring-hairline mt-0.5"
              />
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <div className="flex items-baseline gap-2">
                  <span className="text-xs font-semibold text-text-hi">
                    {comment.authorDisplayName}
                  </span>
                  <span className="text-xs text-text-low tabular-nums">
                    {formatRelativeTime(comment.createdAt)}
                  </span>
                  {viewerId && comment.authorId === viewerId && (
                    <button
                      type="button"
                      onClick={() => void handleDelete(comment.id)}
                      className="ml-auto text-xs text-text-low hover:text-danger transition-colors duration-150"
                      aria-label="댓글 삭제"
                    >
                      삭제
                    </button>
                  )}
                </div>
                <p
                  className={cn(
                    "text-sm leading-relaxed",
                    comment.deleted ? "text-text-low italic" : "text-text",
                  )}
                >
                  {comment.text}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* Error */}
      {error && <p className="text-xs text-danger">{error}</p>}

      {/* Input */}
      {viewerId ? (
        <form onSubmit={(e) => void handleSubmit(e)} className="flex gap-2">
          <input
            type="text"
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="댓글을 입력하세요…"
            maxLength={300}
            aria-label="댓글 입력"
            className={cn(
              "flex-1 rounded-[10px] border border-hairline bg-surface-2",
              "px-3 py-2 text-sm text-text placeholder:text-text-low",
              "focus:border-accent focus:outline-none transition-colors duration-200",
            )}
          />
          <button
            type="submit"
            disabled={!text.trim() || submitting}
            className={cn(
              "rounded-[10px] px-4 py-2 text-sm font-semibold text-white shrink-0",
              "transition-all duration-200",
              text.trim() && !submitting
                ? "bg-accent hover:bg-accent-hi active:bg-accent-press"
                : "bg-surface-3 text-text-low cursor-not-allowed opacity-50",
            )}
          >
            {submitting ? "…" : "게시"}
          </button>
        </form>
      ) : (
        <p className="text-xs text-text-low">
          댓글을 남기려면 로그인이 필요해요.
        </p>
      )}
    </section>
  );
}
