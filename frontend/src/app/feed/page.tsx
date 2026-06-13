"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { PageShell } from "@/components/ui/PageShell";
import { PostCard } from "@/components/social/PostCard";
import { PostComposer } from "@/components/social/PostComposer";
import { cn } from "@/lib/utils/cn";
import type { PostResponse, PostAssetEmbed, FeedTab } from "@/types/social";
import {
  listAllPosts,
  listFeedPosts,
  getPostAssetEmbed,
} from "@/lib/api/social";
import {
  demoPostsPage,
  demoPostAssets,
} from "@/lib/api/fixtures-social";
import { useSessionUser } from "@/lib/auth/useSessionUser";

// ── Feed page ─────────────────────────────────────────────────────────

export default function FeedPage() {
  const sessionState = useSessionUser();
  const viewerId =
    sessionState.status === "authenticated"
      ? sessionState.user.userId
      : null;

  const [tab, setTab] = useState<FeedTab>("all");
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [assetEmbeds, setAssetEmbeds] = useState<
    Record<string, PostAssetEmbed | null>
  >({});
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [status, setStatus] = useState<
    "idle" | "loading" | "success" | "error" | "loadingMore"
  >("idle");
  const [usingFixture, setUsingFixture] = useState(false);

  const loadingRef = useRef(false);

  const fetchEmbeds = useCallback(
    async (newPosts: PostResponse[]) => {
      const ids = [
        ...new Set(
          newPosts
            .map((p) => p.assetId)
            .filter((id): id is string => id !== null),
        ),
      ];
      const results = await Promise.all(
        ids.map(async (id) => {
          if (demoPostAssets[id]) return [id, demoPostAssets[id]] as const;
          const embed = await getPostAssetEmbed(id);
          return [id, embed] as const;
        }),
      );
      setAssetEmbeds((prev) => {
        const next = { ...prev };
        for (const [id, embed] of results) next[id] = embed;
        return next;
      });
    },
    [],
  );

  const loadPosts = useCallback(
    async (nextPage: number, reset: boolean) => {
      if (loadingRef.current) return;
      loadingRef.current = true;
      setStatus(reset ? "loading" : "loadingMore");
      try {
        let data;
        if (tab === "following" && viewerId) {
          data = await listFeedPosts(viewerId, nextPage, 20);
        } else {
          data = await listAllPosts(viewerId, nextPage, 20);
        }
        setUsingFixture(false);
        const newPosts = reset ? data.content : [...posts, ...data.content];
        setPosts(newPosts);
        setPage(nextPage);
        setHasNext(data.hasNext);
        setStatus("success");
        void fetchEmbeds(data.content);
      } catch {
        if (reset) {
          // Fall back to fixture data
          const fixtureData = demoPostsPage;
          setPosts(fixtureData.content);
          setHasNext(fixtureData.hasNext);
          setPage(0);
          setUsingFixture(true);
          setStatus("success");
          setAssetEmbeds(
            Object.fromEntries(
              Object.entries(demoPostAssets).map(([k, v]) => [k, v]),
            ),
          );
        } else {
          setStatus("error");
        }
      } finally {
        loadingRef.current = false;
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [tab, viewerId, fetchEmbeds],
  );

  // Load on mount and tab change
  useEffect(() => {
    void loadPosts(0, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, viewerId]);

  function handleNewPost(newPost: PostResponse) {
    setPosts((prev) => [newPost, ...prev]);
    if (newPost.assetId) {
      void fetchEmbeds([newPost]);
    }
  }

  return (
    <PageShell>
      {/* Page glow */}
      <div
        className="accent-glow pointer-events-none fixed right-0 top-0 h-[500px] w-[500px] translate-x-1/3 -translate-y-1/4 opacity-30"
        aria-hidden
      />

      <div className="mx-auto max-w-2xl py-8">
        {/* Page header */}
        <header className="mb-6">
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            Social
          </p>
          <h1
            className="mt-2 font-display text-text-hi"
            style={{
              fontSize: "clamp(1.75rem, 4vw, 2.5rem)",
              fontWeight: 800,
              letterSpacing: "-0.03em",
              lineHeight: 1.05,
            }}
          >
            피드
          </h1>
        </header>

        {/* Fixture badge */}
        {usingFixture && (
          <p className="mb-4 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
            Demo data · 백엔드 연결 전
          </p>
        )}

        {/* Tabs */}
        <FeedTabs tab={tab} onChange={setTab} />

        {/* Composer */}
        {sessionState.status === "authenticated" && (
          <div className="mt-5 mb-6 animate-fade-up">
            <PostComposer
              userId={sessionState.user.userId}
              userHandle={sessionState.user.handle ?? "me"}
              onPost={handleNewPost}
            />
          </div>
        )}

        {/* Feed states */}
        <div className="mt-2">
          {status === "loading" ? (
            <SkeletonList />
          ) : status === "error" ? (
            <FailureState onRetry={() => void loadPosts(0, true)} />
          ) : posts.length === 0 ? (
            <EmptyState tab={tab} />
          ) : (
            <>
              <ul className="flex flex-col gap-4">
                {posts.map((post, i) => (
                  <li
                    key={post.id}
                    className="animate-fade-up"
                    style={{ animationDelay: `${Math.min(i, 5) * 50}ms` }}
                  >
                    <PostCard
                      post={post}
                      assetEmbed={
                        post.assetId ? assetEmbeds[post.assetId] : null
                      }
                      viewerId={viewerId}
                    />
                  </li>
                ))}
              </ul>

              {/* Load more */}
              {hasNext && (
                <div className="mt-6 flex justify-center">
                  <button
                    type="button"
                    onClick={() => void loadPosts(page + 1, false)}
                    disabled={status === "loadingMore"}
                    className={cn(
                      "rounded-full border border-hairline-strong px-6 py-2.5 text-sm font-semibold text-text-hi",
                      "transition-all duration-200 hover:bg-surface-2",
                      status === "loadingMore" &&
                        "cursor-wait opacity-60",
                    )}
                  >
                    {status === "loadingMore" ? "불러오는 중…" : "더 보기"}
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </PageShell>
  );
}

// ── Sub-components ────────────────────────────────────────────────────

function FeedTabs({
  tab,
  onChange,
}: {
  tab: FeedTab;
  onChange: (t: FeedTab) => void;
}) {
  return (
    <div
      className="flex gap-0 rounded-[12px] border border-hairline bg-surface-2 p-1"
      role="tablist"
      aria-label="피드 탭"
    >
      {(["all", "following"] as FeedTab[]).map((t) => {
        const label = t === "all" ? "전체" : "팔로잉";
        const active = tab === t;
        return (
          <button
            key={t}
            role="tab"
            aria-selected={active}
            type="button"
            onClick={() => onChange(t)}
            className={cn(
              "flex-1 rounded-[9px] py-2 text-sm font-semibold",
              "transition-all duration-200 ease-[var(--ease-spring)]",
              active
                ? "bg-surface-3 text-text-hi shadow-[var(--shadow-card)]"
                : "text-text-mid hover:text-text-hi",
            )}
          >
            {label}
          </button>
        );
      })}
    </div>
  );
}

function SkeletonList() {
  return (
    <ul className="flex flex-col gap-4" aria-busy aria-label="로딩 중">
      {[0, 1, 2].map((i) => (
        <li
          key={i}
          className="animate-pulse rounded-[18px] border border-hairline bg-surface-1 p-4"
        >
          <div className="flex gap-3">
            <div className="h-8 w-8 shrink-0 rounded-full bg-surface-3" />
            <div className="flex flex-1 flex-col gap-3">
              <div className="h-3 w-32 rounded-full bg-surface-3" />
              <div className="h-4 w-full rounded-full bg-surface-3" />
              <div className="h-4 w-3/4 rounded-full bg-surface-3" />
              <div className="h-16 w-full rounded-[12px] bg-surface-2" />
            </div>
          </div>
        </li>
      ))}
    </ul>
  );
}

function EmptyState({ tab }: { tab: FeedTab }) {
  if (tab === "following") {
    return (
      <div className="flex flex-col items-center gap-4 py-20 text-center">
        <p className="text-lg font-semibold text-text-mid">
          아직 팔로우한 사람이 없어요
        </p>
        <p className="text-sm text-text-low">
          관심 있는 사람을 팔로우하면 여기에 포스트가 보여요.
        </p>
      </div>
    );
  }
  return (
    <div className="flex flex-col items-center gap-4 py-20 text-center">
      <p className="text-lg font-semibold text-text-mid">
        아직 포스트가 없어요
      </p>
      <p className="text-sm text-text-low">
        첫 포스트를 작성해보세요.
      </p>
    </div>
  );
}

function FailureState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-4 py-20 text-center">
      <p className="text-lg font-semibold text-text-mid">
        피드를 불러오지 못했어요
      </p>
      <button
        type="button"
        onClick={onRetry}
        className="rounded-full border border-accent px-5 py-2 text-sm font-semibold text-accent hover:bg-accent hover:text-white transition-colors duration-200"
      >
        다시 시도
      </button>
    </div>
  );
}
