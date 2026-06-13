"use client";

/**
 * /u/[handle] — 사용자 프로필 페이지
 *
 * 포스트 탭: GET /api/users/{handle}/posts (fe-feed-001 BLOCKER 해소로 BE에 추가됨).
 * 자산 탭: Asset에 소유자(ownerId) 스키마가 아직 없어 "준비 중" 유지 —
 * 자산 소유권 모델(V7)과 함께 ranking/gift 웨이브에서 해소 예정.
 */

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { PageShell } from "@/components/ui/PageShell";
import { FollowButton } from "@/components/social/FollowButton";
import { PostCard } from "@/components/social/PostCard";
import { cn } from "@/lib/utils/cn";
import type {
  UserProfile,
  FollowStatsResponse,
  PostResponse,
} from "@/types/social";
import {
  getUserProfile,
  getFollowStats,
  listAuthorPosts,
} from "@/lib/api/social";
import { demoUsers } from "@/lib/api/fixtures-social";
import { useSessionUser } from "@/lib/auth/useSessionUser";
import { demoFixturesEnabled } from "@/lib/demo";

type ProfileState =
  | { kind: "loading" }
  | { kind: "not-found" }
  | { kind: "error" }
  | {
      kind: "success";
      profile: UserProfile;
      stats: FollowStatsResponse;
    };

type ProfileTab = "posts" | "assets";

const AVATAR_FALLBACK = (handle: string) =>
  `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(handle)}&backgroundColor=7c5cff&textColor=ffffff`;

export default function ProfilePage() {
  const { handle } = useParams<{ handle: string }>();
  const sessionState = useSessionUser();
  const viewerId =
    sessionState.status === "authenticated"
      ? sessionState.user.userId
      : null;

  const [state, setState] = useState<ProfileState>({ kind: "loading" });
  const [tab, setTab] = useState<ProfileTab>("posts");
  const [usingFixture, setUsingFixture] = useState(false);

  const isOwnProfile =
    state.kind === "success" && viewerId === state.profile.id;

  useEffect(() => {
    if (!handle) return;
    let cancelled = false;

    async function load() {
      setState({ kind: "loading" });
      setUsingFixture(false);

      try {
        const [profile, stats] = await Promise.all([
          getUserProfile(handle),
          getFollowStats(handle, viewerId),
        ]);
        if (cancelled) return;
        setState({ kind: "success", profile, stats });
      } catch (err: unknown) {
        if (cancelled) return;
        // Try fixture fallback first
        const demoUser = demoFixturesEnabled()
          ? demoUsers.find((u) => u.handle === handle)
          : undefined;
        if (demoUser) {
          setState({
            kind: "success",
            profile: demoUser,
            stats: { followerCount: 12, followingCount: 5, isFollowing: false },
          });
          setUsingFixture(true);
          return;
        }
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
  }, [handle, viewerId]);

  function handleFollowToggle(isFollowing: boolean) {
    if (state.kind !== "success") return;
    setState({
      ...state,
      stats: {
        ...state.stats,
        isFollowing,
        followerCount: state.stats.followerCount + (isFollowing ? 1 : -1),
      },
    });
  }

  return (
    <PageShell>
      {/* Accent glow */}
      <div
        className="accent-glow pointer-events-none fixed left-0 top-0 h-[500px] w-[500px] -translate-x-1/3 -translate-y-1/4 opacity-25"
        aria-hidden
      />

      <div className="mx-auto max-w-2xl py-8">
        {/* Back */}
        <Link
          href="/feed"
          className="mb-6 inline-flex items-center gap-1.5 text-sm text-text-mid hover:text-text-hi transition-colors duration-150"
        >
          <ChevronLeftIcon className="h-4 w-4" />
          피드
        </Link>

        {/* Fixture badge */}
        {usingFixture && (
          <p className="mb-4 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
            Demo data · 백엔드 연결 전
          </p>
        )}

        {state.kind === "loading" && <ProfileSkeleton />}

        {state.kind === "not-found" && (
          <div className="flex flex-col items-center gap-4 py-24 text-center">
            <p className="text-xl font-semibold text-text-mid">
              이 사용자는 없어요
            </p>
            <p className="text-sm text-text-low">
              주소가 잘못됐거나 계정이 삭제됐을 수 있어요.
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
              프로필을 불러오지 못했어요
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
          <div className="animate-fade-up flex flex-col gap-8">
            {/* Profile header */}
            <header className="flex flex-col gap-5">
              {/* Cover banner — accent glow behind avatar */}
              <div className="relative h-24 overflow-hidden rounded-[18px] bg-surface-2">
                <div
                  className="accent-glow absolute inset-0"
                  aria-hidden
                />
              </div>

              {/* Avatar + info */}
              <div className="flex items-end justify-between gap-4 -mt-12 px-1">
                <div className="relative">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={
                      state.profile.avatarUrl ??
                      AVATAR_FALLBACK(state.profile.handle)
                    }
                    alt={state.profile.displayName}
                    width={80}
                    height={80}
                    className="h-20 w-20 rounded-full object-cover ring-4 ring-bg"
                  />
                </div>

                {/* Follow button — hidden for own profile */}
                {!isOwnProfile && (
                  <FollowButton
                    handle={handle}
                    viewerId={viewerId}
                    initialFollowing={state.stats.isFollowing}
                    onToggle={handleFollowToggle}
                  />
                )}
              </div>

              {/* Names + stats */}
              <div className="flex flex-col gap-2">
                <div>
                  <h1 className="text-xl font-bold text-text-hi leading-tight">
                    {state.profile.displayName}
                  </h1>
                  <p className="text-sm text-text-mid">
                    @{state.profile.handle}
                  </p>
                </div>

                <div className="flex items-center gap-4 text-sm text-text-mid">
                  <span>
                    <span className="font-semibold tabular-nums text-text-hi">
                      {state.stats.followerCount.toLocaleString()}
                    </span>{" "}
                    팔로워
                  </span>
                  <span>
                    <span className="font-semibold tabular-nums text-text-hi">
                      {state.stats.followingCount.toLocaleString()}
                    </span>{" "}
                    팔로잉
                  </span>
                </div>
              </div>
            </header>

            {/* Tabs */}
            <div
              className="flex gap-0 rounded-[12px] border border-hairline bg-surface-2 p-1"
              role="tablist"
              aria-label="프로필 탭"
            >
              {(["posts", "assets"] as ProfileTab[]).map((t) => {
                const label = t === "posts" ? "포스트" : "자산";
                const active = tab === t;
                return (
                  <button
                    key={t}
                    role="tab"
                    aria-selected={active}
                    type="button"
                    onClick={() => setTab(t)}
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

            {/* Tab content */}
            {tab === "posts" && (
              <ProfilePosts
                handle={handle}
                viewerId={viewerId}
                isOwn={isOwnProfile}
              />
            )}
            {tab === "assets" && (
              <AssetsComingSoon isOwn={isOwnProfile} />
            )}
          </div>
        )}
      </div>
    </PageShell>
  );
}

// ── Profile posts ─────────────────────────────────────────────────────

const PROFILE_PAGE_SIZE = 10;

/**
 * 작성자 포스트 탭 — GET /api/users/{handle}/posts.
 * 4-상태(loading/error/empty/success) + "더 보기" 페이지네이션. 피드와 동일한 PostCard 재사용.
 */
function ProfilePosts({
  handle,
  viewerId,
  isOwn,
}: {
  handle: string;
  viewerId: string | null;
  isOwn: boolean;
}) {
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [status, setStatus] = useState<"loading" | "error" | "ready">(
    "loading",
  );
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setStatus("loading");
    setPosts([]);
    setPage(0);

    listAuthorPosts(handle, viewerId, 0, PROFILE_PAGE_SIZE)
      .then((res) => {
        if (cancelled) return;
        setPosts(res.content);
        setHasNext(res.hasNext);
        setStatus("ready");
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });

    return () => {
      cancelled = true;
    };
  }, [handle, viewerId]);

  async function loadMore() {
    setLoadingMore(true);
    try {
      const next = page + 1;
      const res = await listAuthorPosts(
        handle,
        viewerId,
        next,
        PROFILE_PAGE_SIZE,
      );
      setPosts((prev) => [...prev, ...res.content]);
      setPage(next);
      setHasNext(res.hasNext);
    } catch {
      // 조용히 유지 — 버튼 재시도 가능
    } finally {
      setLoadingMore(false);
    }
  }

  if (status === "loading") {
    return (
      <div className="flex flex-col gap-3">
        {[0, 1].map((i) => (
          <div
            key={i}
            className="h-28 animate-pulse rounded-[18px] border border-hairline bg-surface-1"
          />
        ))}
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="flex flex-col items-center gap-3 rounded-[18px] border border-hairline bg-surface-1 py-16 text-center">
        <p className="text-sm font-semibold text-text-mid">
          포스트를 불러오지 못했어요
        </p>
        <p className="max-w-xs text-xs text-text-low">
          잠시 후 다시 시도해 주세요.
        </p>
      </div>
    );
  }

  if (posts.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 rounded-[18px] border border-hairline bg-surface-1 py-16 text-center">
        <p className="text-sm font-semibold text-text-mid">
          {isOwn ? "아직 포스트가 없어요" : "아직 포스트가 없어요"}
        </p>
        <p className="max-w-xs text-xs text-text-low">
          {isOwn
            ? "피드에서 첫 포스트를 작성해보세요."
            : "이 사용자가 포스트를 올리면 여기에 표시돼요."}
        </p>
        {isOwn && (
          <Link
            href="/feed"
            className="mt-2 rounded-full bg-accent px-5 py-2 text-sm font-semibold text-white hover:bg-accent-hi transition-colors duration-200"
          >
            포스트 작성하기
          </Link>
        )}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {posts.map((post) => (
        <PostCard key={post.id} post={post} viewerId={viewerId} />
      ))}
      {hasNext && (
        <button
          type="button"
          onClick={loadMore}
          disabled={loadingMore}
          className="mx-auto mt-2 rounded-full border border-hairline-strong px-5 py-2 text-sm font-medium text-text-hi transition-colors duration-200 hover:bg-surface-3 disabled:opacity-50"
        >
          {loadingMore ? "불러오는 중…" : "더 보기"}
        </button>
      )}
    </div>
  );
}

/**
 * Assets tab placeholder.
 * BLOCKER: GET /api/assets has no userId/handle filter parameter (confirmed).
 * Cannot fetch user-specific assets without client-side filtering.
 */
function AssetsComingSoon({ isOwn }: { isOwn: boolean }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-[18px] border border-hairline bg-surface-1 py-16 text-center">
      <p className="text-sm font-semibold text-text-mid">
        {isOwn ? "아직 자산이 없어요" : "자산 목록 준비 중"}
      </p>
      <p className="max-w-xs text-xs text-text-low">
        {isOwn
          ? "Spotify에서 플레이리스트를 가져와 첫 자산을 만들어보세요."
          : "사용자별 자산 조회 API가 추가되면 여기에 표시돼요."}
      </p>
      {isOwn && (
        <Link
          href="/import"
          className="mt-2 rounded-full bg-accent px-5 py-2 text-sm font-semibold text-white hover:bg-accent-hi transition-colors duration-200"
        >
          Spotify에서 가져오기
        </Link>
      )}
    </div>
  );
}

// ── Skeleton ──────────────────────────────────────────────────────────

function ProfileSkeleton() {
  return (
    <div className="flex flex-col gap-6" aria-busy aria-label="로딩 중">
      <div className="animate-pulse">
        <div className="h-24 rounded-[18px] bg-surface-2" />
        <div className="-mt-8 flex items-end gap-4 px-1">
          <div className="h-20 w-20 rounded-full bg-surface-3 ring-4 ring-bg" />
          <div className="mb-2 h-9 w-24 rounded-full bg-surface-3" />
        </div>
        <div className="mt-4 flex flex-col gap-2">
          <div className="h-5 w-32 rounded-full bg-surface-3" />
          <div className="h-4 w-20 rounded-full bg-surface-3" />
          <div className="flex gap-4 mt-2">
            <div className="h-4 w-20 rounded-full bg-surface-3" />
            <div className="h-4 w-20 rounded-full bg-surface-3" />
          </div>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div
            key={i}
            className="animate-pulse h-40 rounded-[18px] bg-surface-2"
          />
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
