"use client";

import type { AssetTrack } from "@/types/asset";
import { useTrackPlayer } from "@/components/gift/useTrackPlayer";
import { PlayableTrackRow } from "@/components/gift/PlayableTrackRow";

/**
 * 공유 페이지 트랙 리스트 — 선물 언박싱과 동일한 탭-재생(PlayableTrackRow + useTrackPlayer 공유).
 */
export function ShareTrackList({ tracks }: { tracks: AssetTrack[] }) {
  const player = useTrackPlayer();

  return (
    <div
      className="mt-4 overflow-hidden border border-hairline bg-surface-1"
      style={{ borderRadius: "var(--radius-card)", boxShadow: "var(--shadow-card)" }}
    >
      {tracks.map((track, i) => (
        <div key={track.id} className="border-b border-hairline last:border-b-0">
          <PlayableTrackRow
            track={track}
            index={i}
            player={player}
            accentColor="var(--color-accent)"
          />
        </div>
      ))}
    </div>
  );
}
