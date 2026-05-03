import type { SharedAsset } from "@/types/asset";
import { apiFetch } from "./client";

export async function getShared(token: string): Promise<SharedAsset> {
  return apiFetch<SharedAsset>(`/api/share/${token}`);
}
