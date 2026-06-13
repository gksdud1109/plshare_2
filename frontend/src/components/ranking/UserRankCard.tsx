"use client";

import Link from "next/link";
import { cn } from "@/lib/utils/cn";
import type { UserRankItem } from "@/types/ranking";

interface UserRankCardProps {
  item: UserRankItem;
  featured?: boolean;
  className?: string;
}

const RANK_COLORS: Record<number, string> = {
  1: "text-[#FFD700]",
  2: "text-[#C0C0C0]",
  3: "text-[#CD7F32]",
};

const AVATAR_FALLBACK = (handle: string) =>
  `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(handle)}&backgroundColor=7c5cff&textColor=ffffff`;

/**
 * 사용자 랭킹 카드.
 * featured=true(1~3위)이면 큰 아바타 + 글로우.
 */
export function UserRankCard({
  item,
  featured = false,
  className,
}: UserRankCardProps) {
  const rankColor = RANK_COLORS[item.rank] ?? "text-text-low";
  const avatarSrc = item.avatarUrl ?? AVATAR_FALLBACK(item.handle);
  const avatarSize = featured ? "h-14 w-14" : "h-10 w-10";

  return (
    <Link
      href={`/u/${item.handle}`}
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

      {/* Avatar */}
      <div className={cn("relative shrink-0 overflow-hidden rounded-full", avatarSize)}>
        {featured && (
          <div
            className="absolute inset-0 rounded-full opacity-50 blur-lg"
            style={{
              backgroundImage: `url(${avatarSrc})`,
              backgroundSize: "cover",
              transform: "scale(1.3)",
            }}
            aria-hidden
          />
        )}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={avatarSrc}
          alt={item.displayName}
          className="relative h-full w-full object-cover"
        />
      </div>

      {/* Info */}
      <div className="min-w-0 flex-1">
        <p
          className={cn(
            "truncate font-semibold text-text-hi",
            featured ? "text-base" : "text-sm",
          )}
        >
          {item.displayName}
        </p>
        <p className="mt-0.5 text-xs text-text-low">@{item.handle}</p>
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
