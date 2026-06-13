"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { PageShell } from "@/components/ui/PageShell";
import { PostCard } from "@/components/social/PostCard";
import { CommentSection } from "@/components/social/CommentSection";
import type {
  PostResponse,
  PostAssetEmbed,
  CommentResponse,
  PageResponse,
} from "@/types/social";
import { getPost, listComments, getPostAssetEmbed } from "@/lib/api/social";
import { demoPostAssets, demoCommentsPage } from "@/lib/api/fixtures-social";
import { useSessionUser } from "@/lib/auth/useSessionUser";

type PageState =
  | { kind: "loading" }
  | { kind: "not-found" }
  | { kind: "error" }
  | {
      kind: "success";
      post: PostResponse;
      assetEmbed: PostAssetEmbed | null;
      commentsPage: PageResponse<CommentResponse>;
    };

export default function PostDetailPage() {
  const { postId } = useParams<{ postId: string }>();
  const sessionState = useSessionUser();
  const viewerId =
    sessionState.status === "authenticated"
      ? sessionState.user.userId
      : null;

  const [state, setState] = useState<PageState>({ kind: "loading" });
  const [usingFixture, setUsingFixture] = useState(false);

  useEffect(() => {
    if (!postId) return;
    let cancelled = false;

    async function load() {
      setState({ kind: "loading" });
      try {
        const [post, commentsPage] = await Promise.all([
          getPost(postId, viewerId),
          listComments(postId, 0, 50),
        ]);
        if (cancelled) return;

        let assetEmbed: PostAssetEmbed | null = null;
        if (post.assetId) {
          assetEmbed =
            demoPostAssets[post.assetId] ??
            (await getPostAssetEmbed(post.assetId));
        }

        setState({ kind: "success", post, assetEmbed, commentsPage });
        setUsingFixture(false);
      } catch (err: unknown) {
        if (cancelled) return;
        // 404 → not-found state
        if (
          err !== null &&
          typeof err === "object" &&
          "status" in err &&
          (err as { status: number }).status === 404
        ) {
          setState({ kind: "not-found" });
        } else {
          setState({ kind: "error" });
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [postId, viewerId]);

  // Demo fixture fallback: if error, try fixture data
  useEffect(() => {
    if (state.kind !== "error") return;
    // Try matching a demo post
    import("@/lib/api/fixtures-social").then(({ demoPosts, demoPostAssets: embeds, demoCommentsPage: cp }) => {
      const found = demoPosts.find((p) => p.id === postId);
      if (!found) return;
      setState({
        kind: "success",
        post: found,
        assetEmbed: found.assetId ? embeds[found.assetId] ?? null : null,
        commentsPage: cp,
      });
      setUsingFixture(true);
    }).catch(() => {/* ignore */});
  }, [state.kind, postId]);

  return (
    <PageShell>
      <div className="mx-auto max-w-2xl py-8">
        {/* Back link */}
        <Link
          href="/feed"
          className="mb-6 inline-flex items-center gap-1.5 text-sm text-text-mid hover:text-text-hi transition-colors duration-150"
        >
          <ChevronLeftIcon className="h-4 w-4" />
          피드로 돌아가기
        </Link>

        {/* Fixture badge */}
        {usingFixture && (
          <p className="mb-4 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
            Demo data · 백엔드 연결 전
          </p>
        )}

        {state.kind === "loading" && <PostDetailSkeleton />}

        {state.kind === "not-found" && (
          <div className="flex flex-col items-center gap-4 py-24 text-center">
            <p className="text-xl font-semibold text-text-mid">
              이 포스트는 없어요
            </p>
            <p className="text-sm text-text-low">
              삭제됐거나 주소가 잘못됐을 수 있어요.
            </p>
            <Link
              href="/feed"
              className="rounded-full bg-accent px-6 py-2.5 text-sm font-semibold text-white hover:bg-accent-hi transition-colors duration-200"
            >
              피드 보기
            </Link>
          </div>
        )}

        {state.kind === "error" && (
          <div className="flex flex-col items-center gap-4 py-24 text-center">
            <p className="text-xl font-semibold text-text-mid">
              포스트를 불러오지 못했어요
            </p>
            <button
              type="button"
              onClick={() => setState({ kind: "loading" })}
              className="rounded-full border border-accent px-5 py-2 text-sm font-semibold text-accent hover:bg-accent hover:text-white transition-colors duration-200"
            >
              다시 시도
            </button>
          </div>
        )}

        {state.kind === "success" && (
          <div className="flex flex-col gap-6 animate-fade-up">
            <PostCard
              post={state.post}
              assetEmbed={state.assetEmbed}
              viewerId={viewerId}
              detail
            />
            <div className="border-t border-hairline pt-6">
              <CommentSection
                postId={postId}
                initialComments={state.commentsPage.content}
                viewerId={viewerId}
              />
            </div>
          </div>
        )}
      </div>
    </PageShell>
  );
}

// ── Skeleton ──────────────────────────────────────────────────────────

function PostDetailSkeleton() {
  return (
    <div className="flex flex-col gap-6" aria-busy aria-label="로딩 중">
      <div className="animate-pulse rounded-[18px] border border-hairline bg-surface-1 p-4">
        <div className="flex gap-3">
          <div className="h-8 w-8 shrink-0 rounded-full bg-surface-3" />
          <div className="flex flex-1 flex-col gap-3">
            <div className="h-3 w-40 rounded-full bg-surface-3" />
            <div className="h-4 w-full rounded-full bg-surface-3" />
            <div className="h-4 w-5/6 rounded-full bg-surface-3" />
            <div className="h-4 w-2/3 rounded-full bg-surface-3" />
            <div className="h-24 w-full rounded-[12px] bg-surface-2" />
          </div>
        </div>
      </div>
      <div className="flex flex-col gap-3 pt-2">
        {[0, 1].map((i) => (
          <div key={i} className="flex animate-pulse gap-3">
            <div className="h-7 w-7 shrink-0 rounded-full bg-surface-3" />
            <div className="flex flex-1 flex-col gap-2">
              <div className="h-3 w-24 rounded-full bg-surface-3" />
              <div className="h-3 w-full rounded-full bg-surface-3" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Icon ──────────────────────────────────────────────────────────────

function ChevronLeftIcon({ className }: { className?: string }) {
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
      <polyline points="15 18 9 12 15 6" />
    </svg>
  );
}
