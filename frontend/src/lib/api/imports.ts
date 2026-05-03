import type { ImportJob, ImportJobStatus } from "@/types/asset";
import { apiFetch } from "./client";

export async function startImport(
  playlistId: string,
  idempotencyKey: string,
): Promise<ImportJob> {
  return apiFetch<ImportJob>("/api/imports", {
    method: "POST",
    body: { playlistId },
    idempotencyKey,
  });
}

export async function getImportStatus(jobId: string): Promise<ImportJobStatus> {
  return apiFetch<ImportJobStatus>(`/api/imports/${jobId}`);
}
