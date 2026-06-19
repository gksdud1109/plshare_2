/**
 * Social demo fixtures — used when the BE is unreachable.
 * Pure data, no fetch calls. Separate from fixtures.ts (do not modify that file).
 *
 * NOTE: fixtures.ts must NOT be modified (parallel agent constraint).
 */

import type {
  PostResponse,
  CommentResponse,
  FollowStatsResponse,
  UserProfile,
  PageResponse,
  PostAssetEmbed,
} from "@/types/social";

export const MOOD_TAGS = [
  "새벽",
  "그리움",
  "비오는날",
  "드라이브",
  "카페",
  "집중",
  "위로",
  "파티",
] as const;

export type MoodTag = (typeof MOOD_TAGS)[number];

// ── Demo users ────────────────────────────────────────────────────────

const DEMO_USER_A: UserProfile = {
  id: "00000000-0000-0000-0000-000000000001",
  displayName: "지민",
  handle: "jimin",
  avatarUrl: "https://picsum.photos/seed/jimin/200/200",
};

const DEMO_USER_B: UserProfile = {
  id: "00000000-0000-0000-0000-000000000002",
  displayName: "소율",
  handle: "soyul",
  avatarUrl: "https://picsum.photos/seed/soyul/200/200",
};

const DEMO_USER_C: UserProfile = {
  id: "00000000-0000-0000-0000-000000000003",
  displayName: "현우",
  handle: "hyunwoo",
  avatarUrl: null,
};

export const demoUsers = [DEMO_USER_A, DEMO_USER_B, DEMO_USER_C];

// ── Demo asset embeds ─────────────────────────────────────────────────

export const demoPostAssets: Record<string, PostAssetEmbed> = {
  "asset-001": {
    id: "asset-001",
    title: "새벽 다섯시의 산책",
    coverUrl:
      "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
    trackCount: 18,
    emotionTags: ["새벽", "그리움"],
    assetKind: "TRACKLIST",
  },
  "asset-002": {
    id: "asset-002",
    title: "해안도로 드라이브",
    coverUrl:
      "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=600&q=80",
    trackCount: 24,
    emotionTags: ["드라이브"],
    assetKind: "TRACKLIST",
  },
  "asset-003": {
    id: "asset-003",
    title: "오후의 카페",
    coverUrl:
      "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&q=80",
    trackCount: 12,
    emotionTags: ["카페", "집중"],
    assetKind: "TRACKLIST",
  },
};

// ── Demo posts ────────────────────────────────────────────────────────

export const demoPosts: PostResponse[] = [
  {
    id: "post-001",
    authorId: DEMO_USER_A.id,
    authorHandle: DEMO_USER_A.handle,
    authorDisplayName: DEMO_USER_A.displayName,
    authorAvatarUrl: DEMO_USER_A.avatarUrl,
    text: "처음에는 잠이 오지 않아서 걷기 시작했다. 그러다 새벽 공기에서만 들리는 소리가 있다는 걸 알게 됐다. 이 노래들은 그 시간을 함께 걸어준 친구들.",
    assetId: "asset-001",
    trackId: null,
    moodTag: "새벽",
    likeCount: 42,
    commentCount: 7,
    likedByMe: false,
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: "post-002",
    authorId: DEMO_USER_B.id,
    authorHandle: DEMO_USER_B.handle,
    authorDisplayName: DEMO_USER_B.displayName,
    authorAvatarUrl: DEMO_USER_B.avatarUrl,
    text: "창문 열고 해안도로 달리는 기분. 바람 소리랑 이 플레이리스트가 완벽하게 맞아떨어졌다.",
    assetId: "asset-002",
    trackId: null,
    moodTag: "드라이브",
    likeCount: 28,
    commentCount: 3,
    likedByMe: true,
    createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: "post-003",
    authorId: DEMO_USER_C.id,
    authorHandle: DEMO_USER_C.handle,
    authorDisplayName: DEMO_USER_C.displayName,
    authorAvatarUrl: null,
    text: "비 오는 날 카페에서 집중할 때 듣는 리스트. 가사 없는 곡들 위주로 골랐어요.",
    assetId: "asset-003",
    trackId: null,
    moodTag: "카페",
    likeCount: 15,
    commentCount: 1,
    likedByMe: false,
    createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: "post-004",
    authorId: DEMO_USER_A.id,
    authorHandle: DEMO_USER_A.handle,
    authorDisplayName: DEMO_USER_A.displayName,
    authorAvatarUrl: DEMO_USER_A.avatarUrl,
    text: "아무것도 안 하고 그냥 듣는 날.",
    assetId: null,
    trackId: null,
    moodTag: "그리움",
    likeCount: 9,
    commentCount: 0,
    likedByMe: false,
    createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
  },
];

export const demoPostsPage: PageResponse<PostResponse> = {
  content: demoPosts,
  page: 0,
  size: 20,
  totalElements: demoPosts.length,
  totalPages: 1,
  hasNext: false,
};

// ── Demo comments ─────────────────────────────────────────────────────

export const demoComments: CommentResponse[] = [
  {
    id: "cmt-001",
    postId: "post-001",
    authorId: DEMO_USER_B.id,
    authorHandle: DEMO_USER_B.handle,
    authorDisplayName: DEMO_USER_B.displayName,
    authorAvatarUrl: DEMO_USER_B.avatarUrl,
    text: "이 느낌 너무 좋아요. 저도 새벽 산책할 때 자주 들었어요.",
    deleted: false,
    createdAt: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: "cmt-002",
    postId: "post-001",
    authorId: DEMO_USER_C.id,
    authorHandle: DEMO_USER_C.handle,
    authorDisplayName: DEMO_USER_C.displayName,
    authorAvatarUrl: null,
    text: "Bon Iver 들어있나요?",
    deleted: false,
    createdAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
  },
];

export const demoCommentsPage: PageResponse<CommentResponse> = {
  content: demoComments,
  page: 0,
  size: 20,
  totalElements: demoComments.length,
  totalPages: 1,
  hasNext: false,
};

// ── Demo follow stats ─────────────────────────────────────────────────

export const demoFollowStats: FollowStatsResponse = {
  followerCount: 127,
  followingCount: 43,
  isFollowing: false,
};

// ── Helpers ───────────────────────────────────────────────────────────

export function formatRelativeTime(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime();
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 1) return "방금";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}일 전`;
  return new Date(isoString).toLocaleDateString("ko-KR", {
    month: "short",
    day: "numeric",
  });
}
