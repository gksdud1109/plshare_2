import type {
  AssetDetail,
  AssetSummary,
  PublicAssetDetail,
  ShareLink,
} from "@/types/asset";
import { apiFetch } from "./client";

export async function listAssets(): Promise<AssetSummary[]> {
  return apiFetch<AssetSummary[]>("/api/assets");
}

export async function getAsset(id: string): Promise<AssetDetail> {
  return apiFetch<AssetDetail>(`/api/assets/${id}`);
}

export async function getPublicAsset(id: string): Promise<PublicAssetDetail> {
  return apiFetch<PublicAssetDetail>(`/api/public/assets/${id}`);
}

export interface UpdateAssetInput {
  diaryText?: string;
  description?: string;
  emotionTags?: string[];
  coverUrl?: string;
}

export async function updateAsset(
  id: string,
  patch: UpdateAssetInput,
): Promise<AssetDetail> {
  return apiFetch<AssetDetail>(`/api/assets/${id}`, {
    method: "PATCH",
    body: patch,
  });
}

export async function createShareLink(id: string): Promise<ShareLink> {
  return apiFetch<ShareLink>(`/api/assets/${id}/share`, {
    method: "POST",
  });
}

export async function deleteAsset(id: string): Promise<void> {
  await apiFetch<boolean>(`/api/assets/${id}`, { method: "DELETE" });
}
