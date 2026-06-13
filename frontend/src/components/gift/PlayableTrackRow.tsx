"use client";

import { TrackEmbed } from "@/components/gift/TrackEmbed";
import type { useTrackPlayer } from "@/components/gift/useTrackPlayer";

interface RowTrack {
  id: string;
  name: string;
  artist: string;
  durationMs?: number | null;
  youtubeVideoId?: string | null;
}

function formatDuration(ms?: number | null): string {
  if (!ms) return "";
  const total = Math.floor(ms / 1000);
  return `${Math.floor(total / 60)}:${(total % 60).toString().padStart(2, "0")}`;
}

/**
 * 탭하여 재생하는 트랙 행 — 선물 언박싱과 공유 페이지가 공유한다. 재생 중 강조색은
 * accentColor 로 주입(선물=wrapSkin accent, 공유=서비스 accent). 한 곡 재생 상태와
 * lazy resolve 는 [useTrackPlayer] 가 관리한다.
 */
export function PlayableTrackRow({
  track,
  index,
  player,
  accentColor,
}: {
  track: RowTrack;
  index: number;
  player: ReturnType<typeof useTrackPlayer>;
  accentColor: string;
}) {
  const isPlaying = player.playingId === track.id;
  const vid = player.videoIdFor(track);

  return (
    <>
      <button
        type="button"
        onClick={() => player.toggle(track)}
        className="flex w-full items-center gap-4 px-4 py-3 text-left transition-colors duration-200 hover:bg-surface-2 focus-ring"
        aria-label={isPlaying ? `${track.name} 닫기` : `${track.name} 재생`}
      >
        <span
          className="flex w-6 shrink-0 items-center justify-center text-xs tabular-nums"
          style={{ color: isPlaying ? accentColor : "var(--color-text-low)" }}
        >
          {player.resolvingId === track.id ? (
            <span
              className="inline-block h-3 w-3 animate-spin rounded-full border border-current border-t-transparent"
              aria-hidden
            />
          ) : isPlaying ? (
            <svg width="11" height="11" viewBox="0 0 11 11" fill="currentColor" aria-hidden>
              <rect x="1" y="1" width="3" height="9" rx="1" />
              <rect x="7" y="1" width="3" height="9" rx="1" />
            </svg>
          ) : (
            index + 1
          )}
        </span>
        <div className="min-w-0 flex-1">
          <p
            className="truncate text-[0.9375rem] font-medium"
            style={{ color: isPlaying ? accentColor : "var(--color-text-hi)" }}
          >
            {track.name}
          </p>
          <p className="mt-0.5 truncate text-xs text-text-mid">{track.artist}</p>
        </div>
        {isPlaying ? (
          <svg
            width="14"
            height="14"
            viewBox="0 0 14 14"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.5"
            className="shrink-0 text-text-low"
            aria-hidden
          >
            <path d="M11 4L4 11M4 4l7 7" strokeLinecap="round" />
          </svg>
        ) : (
          <span className="flex shrink-0 items-center gap-2 text-text-low">
            {track.durationMs ? (
              <span className="text-xs tabular-nums">{formatDuration(track.durationMs)}</span>
            ) : null}
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
              <path d="M4 3.5v9a.5.5 0 00.76.43l7.5-4.5a.5.5 0 000-.86l-7.5-4.5A.5.5 0 004 3.5z" />
            </svg>
          </span>
        )}
      </button>
      {isPlaying ? <TrackEmbed videoId={vid ?? null} title={track.name} /> : null}
    </>
  );
}
