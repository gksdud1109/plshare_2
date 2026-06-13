/**
 * Gift 도메인 demo fixtures — BE 미연결 시 UI를 항상 inspectable 하게 유지.
 * 순수 데이터만, fetch 호출 없음.
 */

import type { GiftCreated, GiftView } from "@/types/gift";

export function buildDemoGiftView(token: string): GiftView {
  return {
    token,
    status: "CREATED",
    message:
      "노을이 질 때 들으면 좋은 곡들을 골라봤어요. 오늘 하루도 수고했어요.",
    wrapSkin: "nocturne-violet",
    sender: {
      handle: "demo",
      displayName: "Demo User",
      avatarUrl: "https://picsum.photos/seed/demouser/200/200",
    },
    asset: {
      id: "demo-asset",
      title: "Sample - Sunset Walks",
      coverUrl: "https://picsum.photos/seed/sunset/600/600",
      tracks: [
        { id: "t1", name: "Space Song", artist: "Beach House", durationMs: 320000 },
        { id: "t2", name: "Chamber of Reflection", artist: "Mac DeMarco", durationMs: 268000 },
        { id: "t3", name: "Apocalypse", artist: "Cigarettes After Sex", durationMs: 290000 },
        { id: "t4", name: "Coffee", artist: "Beabadoobee", durationMs: 117000 },
      ],
    },
  };
}

export function buildDemoGiftCreated(token = "demo0000000001"): GiftCreated {
  return { token, url: `/gift/${token}` };
}
