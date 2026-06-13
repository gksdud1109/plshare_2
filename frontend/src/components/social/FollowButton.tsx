"use client";

import { useState } from "react";
import { cn } from "@/lib/utils/cn";
import { followUser, unfollowUser } from "@/lib/api/social";

interface FollowButtonProps {
  handle: string;
  /** Current viewer's userId — null means not logged in, hide button */
  viewerId: string | null;
  /** Whether the viewer is already following this handle */
  initialFollowing: boolean;
  /** Called after server confirms the follow state change */
  onToggle?: (isFollowing: boolean) => void;
  className?: string;
}

export function FollowButton({
  handle,
  viewerId,
  initialFollowing,
  onToggle,
  className,
}: FollowButtonProps) {
  const [following, setFollowing] = useState(initialFollowing);
  const [loading, setLoading] = useState(false);

  // Don't render if not logged in
  if (!viewerId) return null;

  async function handleToggle() {
    if (!viewerId || loading) return;
    setLoading(true);
    const wasFollowing = following;
    setFollowing(!wasFollowing);
    try {
      if (wasFollowing) {
        await unfollowUser(handle, viewerId);
      } else {
        await followUser(handle, viewerId);
      }
      onToggle?.(!wasFollowing);
    } catch {
      // Revert optimistic update
      setFollowing(wasFollowing);
    } finally {
      setLoading(false);
    }
  }

  return (
    <button
      type="button"
      onClick={() => void handleToggle()}
      disabled={loading}
      aria-pressed={following}
      aria-label={following ? "팔로잉 중 — 클릭하면 언팔로우" : "팔로우"}
      className={cn(
        "inline-flex items-center rounded-full px-5 py-2 text-sm font-semibold",
        "transition-all duration-200 ease-[var(--ease-spring)]",
        "focus-ring",
        following
          ? "border border-hairline-strong bg-surface-2 text-text-hi hover:border-danger hover:text-danger"
          : "bg-accent text-white hover:bg-accent-hi hover:-translate-y-0.5 active:bg-accent-press",
        loading && "cursor-wait opacity-70",
        className,
      )}
    >
      {loading ? "…" : following ? "팔로잉" : "팔로우"}
    </button>
  );
}
