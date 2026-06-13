"use client";

import { useState } from "react";
import { resolveTrackYouTube } from "@/lib/api/gift";

export interface PlayableTrack {
  id: string;
  youtubeVideoId?: string | null;
}

/**
 * 인라인 트랙 재생 상태 — 한 번에 한 곡만 열린다. videoId가 없으면 탭 시 lazy로
 * resolve(YouTube)해 캐시한다. 선물 언박싱과 공유 페이지가 공유한다.
 */
export function useTrackPlayer() {
  const [playingId, setPlayingId] = useState<string | null>(null);
  const [resolvingId, setResolvingId] = useState<string | null>(null);
  const [resolvedIds, setResolvedIds] = useState<Record<string, string | null>>({});

  function videoIdFor(track: PlayableTrack): string | null | undefined {
    return track.youtubeVideoId ?? resolvedIds[track.id];
  }

  async function toggle(track: PlayableTrack) {
    if (playingId === track.id) {
      setPlayingId(null);
      return;
    }
    if (videoIdFor(track) === undefined) {
      setResolvingId(track.id);
      try {
        const { videoId } = await resolveTrackYouTube(track.id);
        setResolvedIds((m) => ({ ...m, [track.id]: videoId }));
      } catch {
        setResolvedIds((m) => ({ ...m, [track.id]: null }));
      } finally {
        setResolvingId(null);
      }
    }
    setPlayingId(track.id);
  }

  return { playingId, resolvingId, videoIdFor, toggle };
}
