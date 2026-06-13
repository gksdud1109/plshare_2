import type { ImportJob, ImportJobStatus } from "@/types/asset";
import { apiFetch } from "./client";

export async function startImport(
  playlistId: string,
  idempotencyKey: string,
  // BE contract: POST /api/imports body { playlistId, sourcePlatform? }
  // Defaults to "spotify" — existing callers that omit this param are unaffected.
  sourcePlatform: "spotify" | "youtube" = "spotify",
): Promise<ImportJob> {
  return apiFetch<ImportJob>("/api/imports", {
    method: "POST",
    body: { playlistId, sourcePlatform },
    idempotencyKey,
  });
}

export async function getImportStatus(jobId: string): Promise<ImportJobStatus> {
  return apiFetch<ImportJobStatus>(`/api/imports/${jobId}`);
}
