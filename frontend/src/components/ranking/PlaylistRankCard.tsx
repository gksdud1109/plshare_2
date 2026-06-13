"use client";

import Link from "next/link";
import { cn } from "@/lib/utils/cn";
import type { PlaylistRankItem } from "@/types/ranking";

interface PlaylistRankCardProps {
  item: PlaylistRankItem;
  featured?: boolean;
  className?: string;
}

const RANK_COLORS: Record<number, string> = {
  1: "text-[#FFD700]",
  2: "text-[#C0C0C0]",
  3: "text-[#CD7F32]",
};

/**
 * 플레이리스트(자산) 랭킹 카드.
 * featured=true(1~3위)이면 커버 이미지 글로우 + 큰 레이아웃.
 */
export function PlaylistRankCard({
  item,
  featured = false,
  className,
}: PlaylistRankCardProps) {
  const rankColor = RANK_COLORS[item.rank] ?? "text-text-low";

  return (
    <Link
      href={`/assets/${item.assetId}`}
      className={cn(
        "group flex items-center gap-4 rounded-[18px] border border-hairline bg-surface-1",
        "transition-all duration-200 hover:border-hairline-strong hover:bg-surface-2",
        featured ? "p-4" : "p-3",
        className,
      )}
    >
      {/* Rank badge */}
      <span
        className={cn(
          "shrink-0 font-display font-black tabular-nums",
          featured ? "w-8 text-2xl" : "w-6 text-base",
          rankColor,
        )}
        aria-label={`${item.rank}위`}
      >
        {item.rank}
      </span>

      {/* Cover image */}
      <div
        className={cn(
          "relative shrink-0 overflow-hidden rounded-[10px]",
          featured ? "h-16 w-16" : "h-11 w-11",
        )}
      >
        {item.coverUrl ? (
          <>
            {/* Glow effect for top 3 */}
            {featured && (
              <div
                className="absolute inset-0 rounded-[10px] opacity-60 blur-lg"
                style={{
                  backgroundImage: `url(${item.coverUrl})`,
                  backgroundSize: "cover",
                  transform: "scale(1.2)",
                }}
                aria-hidden
              />
            )}
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={item.coverUrl}
              alt={item.title}
              className="relative h-full w-full object-cover"
            />
          </>
        ) : (
          <div className="h-full w-full bg-surface-3 flex items-center justify-center">
            <span className="text-xl">🎵</span>
          </div>
        )}
      </div>

      {/* Info */}
      <div className="min-w-0 flex-1">
        <p
          className={cn(
            "truncate font-semibold text-text-hi",
            featured ? "text-base" : "text-sm",
          )}
        >
          {item.title}
        </p>
        <p className="mt-0.5 text-xs text-text-low">
          트랙 {item.trackCount}곡
        </p>
      </div>

      {/* Score */}
      <div className="shrink-0 text-right">
        <p
          className={cn(
            "font-bold tabular-nums",
            featured ? "text-lg text-text-hi" : "text-sm text-text-mid",
          )}
        >
          {item.score.toLocaleString()}
        </p>
        <p className="text-[0.625rem] text-text-low">점수</p>
      </div>
    </Link>
  );
}
