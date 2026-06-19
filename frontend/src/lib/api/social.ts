/**
 * Social API client — wraps all social-layer BE endpoints.
 *
 * Endpoint source of truth (confirmed from BE controllers):
 *   GET  /api/posts?viewerId&page&size           → PageResponse<PostResponse>
 *   GET  /api/feed?userId&page&size              → PageResponse<PostResponse>
 *   GET  /api/posts/{id}?viewerId                → PostResponse
 *   POST /api/posts                              body: CreatePostRequest → PostResponse
 *   DEL  /api/posts/{id}?requesterId            → void
 *   POST /api/posts/{id}/like?userId=            → Long (likeCount)
 *   DEL  /api/posts/{id}/like?userId=            → Long (likeCount)
 *   GET  /api/posts/{postId}/comments?page&size  → PageResponse<CommentResponse>
 *   POST /api/posts/{postId}/comments            body: CreateCommentRequest → CommentResponse
 *   DEL  /api/comments/{commentId}?requesterId  → void
 *   POST /api/users/{handle}/follow?followerId=  → void
 *   DEL  /api/users/{handle}/follow?followerId=  → void
 *   GET  /api/users/{handle}/follow-stats?viewerId= → FollowStatsResponse
 *   GET  /api/users/{handle}                    → UserProfileDto
 *   GET  /api/assets                            → AssetSummaryDto[] (for composer picker)
 *   GET  /api/assets/{id}                       → AssetDetailDto
 */

import { apiFetch } from "./client";
import type {
  PageResponse,
  PostResponse,
  CommentResponse,
  FollowStatsResponse,
  UserProfile,
  CreatePostRequest,
  CreateCommentRequest,
  PostAssetEmbed,
} from "@/types/social";
import type { AssetSummary } from "@/types/asset";

// ── Posts ──────────────────────────────────────────────────────────────

export async function listAllPosts(
  viewerId: string | null,
  page: number,
  size: number,
): Promise<PageResponse<PostResponse>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (viewerId) params.set("viewerId", viewerId);
  return apiFetch<PageResponse<PostResponse>>(`/api/posts?${params}`);
}

export async function listFeedPosts(
  userId: string,
  page: number,
  size: number,
): Promise<PageResponse<PostResponse>> {
  const params = new URLSearchParams({
    userId,
    page: String(page),
    size: String(size),
  });
  return apiFetch<PageResponse<PostResponse>>(`/api/feed?${params}`);
}

/** 프로필 피드: handle 작성자의 포스트 (GET /api/users/{handle}/posts). */
export async function listAuthorPosts(
  handle: string,
  viewerId: string | null,
  page: number,
  size: number,
): Promise<PageResponse<PostResponse>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (viewerId) params.set("viewerId", viewerId);
  return apiFetch<PageResponse<PostResponse>>(
    `/api/users/${encodeURIComponent(handle)}/posts?${params}`,
  );
}

export async function getPost(
  id: string,
  viewerId: string | null,
): Promise<PostResponse> {
  const params = new URLSearchParams();
  if (viewerId) params.set("viewerId", viewerId);
  const qs = params.toString() ? `?${params}` : "";
  return apiFetch<PostResponse>(`/api/posts/${id}${qs}`);
}

export async function createPost(req: CreatePostRequest): Promise<PostResponse> {
  return apiFetch<PostResponse>("/api/posts", { method: "POST", body: req });
}

export async function deletePost(
  id: string,
  requesterId: string,
): Promise<void> {
  await apiFetch<null>(`/api/posts/${id}?requesterId=${encodeURIComponent(requesterId)}`, {
    method: "DELETE",
  });
}

// ── Reactions ─────────────────────────────────────────────────────────

export async function likePost(
  postId: string,
  userId: string,
): Promise<number> {
  return apiFetch<number>(
    `/api/posts/${postId}/like?userId=${encodeURIComponent(userId)}`,
    { method: "POST" },
  );
}

export async function unlikePost(
  postId: string,
  userId: string,
): Promise<number> {
  return apiFetch<number>(
    `/api/posts/${postId}/like?userId=${encodeURIComponent(userId)}`,
    { method: "DELETE" },
  );
}

// ── Comments ──────────────────────────────────────────────────────────

export async function listComments(
  postId: string,
  page: number,
  size: number,
): Promise<PageResponse<CommentResponse>> {
  return apiFetch<PageResponse<CommentResponse>>(
    `/api/posts/${postId}/comments?page=${page}&size=${size}`,
  );
}

export async function createComment(
  postId: string,
  req: CreateCommentRequest,
): Promise<CommentResponse> {
  return apiFetch<CommentResponse>(`/api/posts/${postId}/comments`, {
    method: "POST",
    body: req,
  });
}

export async function deleteComment(
  commentId: string,
  requesterId: string,
): Promise<void> {
  await apiFetch<null>(
    `/api/comments/${commentId}?requesterId=${encodeURIComponent(requesterId)}`,
    { method: "DELETE" },
  );
}

// ── Follow ────────────────────────────────────────────────────────────

export async function followUser(
  handle: string,
  followerId: string,
): Promise<void> {
  await apiFetch<null>(
    `/api/users/${encodeURIComponent(handle)}/follow?followerId=${encodeURIComponent(followerId)}`,
    { method: "POST" },
  );
}

export async function unfollowUser(
  handle: string,
  followerId: string,
): Promise<void> {
  await apiFetch<null>(
    `/api/users/${encodeURIComponent(handle)}/follow?followerId=${encodeURIComponent(followerId)}`,
    { method: "DELETE" },
  );
}

export async function getFollowStats(
  handle: string,
  viewerId: string | null,
): Promise<FollowStatsResponse> {
  const qs = viewerId ? `?viewerId=${encodeURIComponent(viewerId)}` : "";
  return apiFetch<FollowStatsResponse>(
    `/api/users/${encodeURIComponent(handle)}/follow-stats${qs}`,
  );
}

// ── User profile ──────────────────────────────────────────────────────

export async function getUserProfile(handle: string): Promise<UserProfile> {
  return apiFetch<UserProfile>(`/api/users/${encodeURIComponent(handle)}`);
}

// ── Assets (for composer picker) ──────────────────────────────────────

export async function listMyAssets(): Promise<AssetSummary[]> {
  return apiFetch<AssetSummary[]>("/api/assets");
}

/**
 * Load a lightweight asset embed for a PostCard.
 * Returns null on error so the card degrades gracefully.
 */
export async function getPostAssetEmbed(
  assetId: string,
): Promise<PostAssetEmbed | null> {
  try {
    const detail = await apiFetch<{
      id: string;
      title: string;
      coverUrl?: string | null;
      emotionTags?: string[];
      tracks?: unknown[];
      assetKind?: "TRACKLIST" | "MOOD_VIDEO";
    }>(`/api/public/assets/${assetId}`);
    return {
      id: detail.id,
      title: detail.title,
      coverUrl: detail.coverUrl ?? null,
      trackCount: Array.isArray(detail.tracks) ? detail.tracks.length : 0,
      emotionTags: Array.isArray(detail.emotionTags) ? detail.emotionTags : [],
      assetKind: detail.assetKind ?? "TRACKLIST",
    };
  } catch {
    return null;
  }
}
