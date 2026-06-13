/**
 * Social domain types — aligned with the BE PostResponse, CommentResponse,
 * FollowStatsResponse, and UserProfileDto contracts.
 *
 * BE source of truth:
 *   backend/src/main/kotlin/com/plshare/backend/domain/post/dto/PostDtos.kt
 *   backend/src/main/kotlin/com/plshare/backend/domain/comment/dto/CommentDtos.kt
 *   backend/src/main/kotlin/com/plshare/backend/domain/follow/dto/FollowDtos.kt
 *   backend/src/main/kotlin/com/plshare/backend/domain/user/dto/UserDtos.kt
 *   backend/src/main/kotlin/com/plshare/backend/global/response/PageResponse.kt
 */

/** PageResponse<T> envelope from BE */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

/** Mirrors PostResponse DTO */
export interface PostResponse {
  id: string;
  authorId: string;
  authorHandle: string;
  authorDisplayName: string;
  authorAvatarUrl: string | null;
  text: string;
  assetId: string | null;
  trackId: string | null;
  moodTag: string | null;
  likeCount: number;
  commentCount: number;
  likedByMe: boolean;
  createdAt: string; // ISO LocalDateTime string
}

/** Mirrors CommentResponse DTO */
export interface CommentResponse {
  id: string;
  postId: string;
  authorId: string;
  authorHandle: string;
  authorDisplayName: string;
  authorAvatarUrl: string | null;
  text: string;
  deleted: boolean;
  createdAt: string;
}

/** Mirrors FollowStatsResponse DTO */
export interface FollowStatsResponse {
  followerCount: number;
  followingCount: number;
  isFollowing: boolean;
}

/** Mirrors UserProfileDto */
export interface UserProfile {
  id: string;
  displayName: string;
  handle: string;
  avatarUrl: string | null;
}

/** CreatePostRequest body */
export interface CreatePostRequest {
  authorId: string;
  text: string;
  assetId?: string;
  trackId?: string;
  moodTag?: string;
}

/** CreateCommentRequest body */
export interface CreateCommentRequest {
  authorId: string;
  text: string;
}

/** Embedded asset info loaded via GET /api/assets/{id} for a post */
export interface PostAssetEmbed {
  id: string;
  title: string;
  coverUrl: string | null;
  trackCount: number;
  emotionTags: string[];
}

/** 4-state async helper */
export type AsyncState<T> =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "success"; data: T }
  | { kind: "error"; message: string };

/** Feed tab */
export type FeedTab = "all" | "following";
