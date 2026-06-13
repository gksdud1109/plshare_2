"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { PageShell } from "@/components/ui/PageShell";
import { PlaylistRankCard } from "@/components/ranking/PlaylistRankCard";
import { UserRankCard } from "@/components/ranking/UserRankCard";
import { cn } from "@/lib/utils/cn";
import type { PlaylistRankItem, UserRankItem, RankingPeriod, RankingTab } from "@/types/ranking";
import { listPlaylistRanking, listUserRanking } from "@/lib/api/ranking";
import {
  demoPlaylistRankingPage,
  demoUserRankingPage,
} from "@/lib/api/fixtures-ranking";

// ── 기간 레이블 ───────────────────────────────────────────────────────

const PERIOD_LABELS: Record<RankingPeriod, string> = {
  realtime: "실시간",
  weekly: "주간",
  "all-time": "누적",
};

const PERIODS: RankingPeriod[] = ["realtime", "weekly", "all-time"];

// ── Page ──────────────────────────────────────────────────────────────

export default function RankingPage() {
  const [tab, setTab] = useState<RankingTab>("playlists");
  const [period, setPeriod] = useState<RankingPeriod>("realtime");

  const [playlists, setPlaylists] = useState<PlaylistRankItem[]>([]);
  const [users, setUsers] = useState<UserRankItem[]>([]);
  const [playlistPage, setPlaylistPage] = useState(0);
  const [userPage, setUserPage] = useState(0);
  const [playlistHasNext, setPlaylistHasNext] = useState(false);
  const [userHasNext, setUserHasNext] = useState(false);

  const [status, setStatus] = useState<
    "idle" | "loading" | "success" | "error" | "loadingMore"
  >("idle");
  const [usingFixture, setUsingFixture] = useState(false);

  const loadingRef = useRef(false);

  const loadPlaylists = useCallback(
    async (nextPage: number, reset: boolean) => {
      if (loadingRef.current) return;
      loadingRef.current = true;
      setStatus(reset ? "loading" : "loadingMore");
      try {
        const data = await listPlaylistRanking(period, nextPage, 20);
        setUsingFixture(false);
        setPlaylists(reset ? data.content : (prev) => [...prev, ...data.content]);
        setPlaylistPage(nextPage);
        setPlaylistHasNext(data.hasNext);
        setStatus("success");
      } catch {
        if (reset) {
          setPlaylists(demoPlaylistRankingPage.content);
          setPlaylistHasNext(demoPlaylistRankingPage.hasNext);
          setPlaylistPage(0);
          setUsingFixture(true);
          setStatus("success");
        } else {
          setStatus("error");
        }
      } finally {
        loadingRef.current = false;
      }
    },
    [period],
  );

  const loadUsers = useCallback(
    async (nextPage: number, reset: boolean) => {
      if (loadingRef.current) return;
      loadingRef.current = true;
      setStatus(reset ? "loading" : "loadingMore");
      try {
        const data = await listUserRanking(period, nextPage, 20);
        setUsingFixture(false);
        setUsers(reset ? data.content : (prev) => [...prev, ...data.content]);
        setUserPage(nextPage);
        setUserHasNext(data.hasNext);
        setStatus("success");
      } catch {
        if (reset) {
          setUsers(demoUserRankingPage.content);
          setUserHasNext(demoUserRankingPage.hasNext);
          setUserPage(0);
          setUsingFixture(true);
          setStatus("success");
        } else {
          setStatus("error");
        }
      } finally {
        loadingRef.current = false;
      }
    },
    [period],
  );

  useEffect(() => {
    if (tab === "playlists") {
      void loadPlaylists(0, true);
    } else {
      void loadUsers(0, true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, period]);

  function handleLoadMore() {
    if (tab === "playlists") {
      void loadPlaylists(playlistPage + 1, false);
    } else {
      void loadUsers(userPage + 1, false);
    }
  }

  const hasNext = tab === "playlists" ? playlistHasNext : userHasNext;

  return (
    <PageShell>
      {/* Page glow */}
      <div
        className="accent-glow pointer-events-none fixed left-0 top-0 h-[500px] w-[500px] -translate-x-1/3 -translate-y-1/4 opacity-20"
        aria-hidden
      />

      <div className="mx-auto max-w-2xl py-8">
        {/* Page header */}
        <header className="mb-6">
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
            Discover
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
            랭킹
          </h1>
        </header>

        {/* Fixture badge */}
        {usingFixture && (
          <p className="mb-4 inline-flex rounded-full border border-hairline bg-surface-1 px-3 py-1 text-[0.6875rem] uppercase tracking-[0.18em] text-text-low">
            Demo data · 백엔드 연결 전
          </p>
        )}

        {/* Tab + Period selectors */}
        <div className="flex flex-col gap-3">
          <RankingTabs tab={tab} onChange={(t) => { setTab(t); }} />
          <PeriodSelector period={period} onChange={(p) => { setPeriod(p); }} />
        </div>

        {/* Content */}
        <div className="mt-6">
          {status === "loading" ? (
            <SkeletonList />
          ) : status === "error" ? (
            <FailureState onRetry={() => void (tab === "playlists" ? loadPlaylists(0, true) : loadUsers(0, true))} />
          ) : tab === "playlists" ? (
            playlists.length === 0 ? (
              <EmptyState />
            ) : (
              <RankingList
                items={playlists}
                renderItem={(item) => (
                  <PlaylistRankCard
                    key={item.assetId}
                    item={item}
                    featured={item.rank <= 3}
                  />
                )}
              />
            )
          ) : users.length === 0 ? (
            <EmptyState />
          ) : (
            <RankingList
              items={users}
              renderItem={(item) => (
                <UserRankCard
                  key={item.userId}
                  item={item}
                  featured={item.rank <= 3}
                />
              )}
            />
          )}

          {/* Load more */}
          {hasNext && status !== "loading" && (
            <div className="mt-6 flex justify-center">
              <button
                type="button"
                onClick={handleLoadMore}
                disabled={status === "loadingMore"}
                className={cn(
                  "rounded-full border border-hairline-strong px-6 py-2.5 text-sm font-semibold text-text-hi",
                  "transition-all duration-200 hover:bg-surface-2",
                  status === "loadingMore" && "cursor-wait opacity-60",
                )}
              >
                {status === "loadingMore" ? "불러오는 중…" : "더 보기"}
              </button>
            </div>
          )}
        </div>
      </div>
    </PageShell>
  );
}

// ── Sub-components ────────────────────────────────────────────────────

function RankingTabs({
  tab,
  onChange,
}: {
  tab: RankingTab;
  onChange: (t: RankingTab) => void;
}) {
  return (
    <div
      className="flex gap-0 rounded-[12px] border border-hairline bg-surface-2 p-1"
      role="tablist"
      aria-label="랭킹 탭"
    >
      {(["playlists", "users"] as RankingTab[]).map((t) => {
        const label = t === "playlists" ? "플레이리스트" : "사용자";
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

function PeriodSelector({
  period,
  onChange,
}: {
  period: RankingPeriod;
  onChange: (p: RankingPeriod) => void;
}) {
  return (
    <div className="flex gap-2" role="group" aria-label="기간 선택">
      {PERIODS.map((p) => {
        const active = period === p;
        return (
          <button
            key={p}
            type="button"
            onClick={() => onChange(p)}
            className={cn(
              "rounded-full px-4 py-1.5 text-xs font-semibold transition-all duration-200",
              active
                ? "bg-accent text-white"
                : "border border-hairline text-text-mid hover:border-hairline-strong hover:text-text-hi",
            )}
          >
            {PERIOD_LABELS[p]}
          </button>
        );
      })}
    </div>
  );
}

function RankingList<T>({
  items,
  renderItem,
}: {
  items: T[];
  renderItem: (item: T) => React.ReactNode;
}) {
  return (
    <ul className="flex flex-col gap-3">
      {items.map((item, i) => (
        <li
          key={i}
          className="animate-fade-up"
          style={{ animationDelay: `${Math.min(i, 5) * 40}ms` }}
        >
          {renderItem(item)}
        </li>
      ))}
    </ul>
  );
}

function SkeletonList() {
  return (
    <ul className="flex flex-col gap-3" aria-busy aria-label="로딩 중">
      {[0, 1, 2, 3, 4].map((i) => (
        <li
          key={i}
          className="animate-pulse flex items-center gap-4 rounded-[18px] border border-hairline bg-surface-1 p-4"
        >
          <div className={cn("shrink-0 rounded-full bg-surface-3", i < 3 ? "h-6 w-8" : "h-4 w-6")} />
          <div className={cn("shrink-0 rounded-[10px] bg-surface-3", i < 3 ? "h-16 w-16" : "h-11 w-11")} />
          <div className="flex-1 space-y-2">
            <div className="h-3 w-40 rounded-full bg-surface-3" />
            <div className="h-2 w-24 rounded-full bg-surface-2" />
          </div>
          <div className="h-4 w-10 rounded-full bg-surface-3" />
        </li>
      ))}
    </ul>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center gap-4 py-20 text-center">
      <p className="text-lg font-semibold text-text-mid">아직 랭킹 데이터가 없어요</p>
      <p className="text-sm text-text-low">포스트를 공유하면 랭킹이 집계됩니다.</p>
    </div>
  );
}

function FailureState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-4 py-20 text-center">
      <p className="text-lg font-semibold text-text-mid">랭킹을 불러오지 못했어요</p>
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
