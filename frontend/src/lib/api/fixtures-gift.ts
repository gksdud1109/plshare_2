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
      "늦은 밤, 창문 내리고 이 노래들 들으면서 달려봐. 네 생각하면서 골랐어. 오늘 하루도 수고했어 🌙",
    wrapSkin: "nocturne-violet",
    sender: {
      handle: "demo",
      displayName: "Demo User",
      avatarUrl: "https://picsum.photos/seed/demouser/200/200",
    },
    asset: {
      id: "demo-asset",
      title: "Late Night Drives",
      coverUrl: "https://picsum.photos/seed/latenight/600/600",
      tracks: [
        { id: "t1", name: "Blinding Lights", artist: "The Weeknd", durationMs: 200000 },
        { id: "t2", name: "Something About Us", artist: "Daft Punk", durationMs: 232000 },
        { id: "t3", name: "Midnight City", artist: "M83", durationMs: 244000 },
        { id: "t4", name: "The Less I Know the Better", artist: "Tame Impala", durationMs: 216000 },
      ],
    },
  };
}

export function buildDemoGiftCreated(token = "demo0000000001"): GiftCreated {
  return { token, url: `/gift/${token}` };
}
