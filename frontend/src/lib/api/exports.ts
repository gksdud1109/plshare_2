import type {
  ExportCandidate,
  ExportJob,
  ExportJobStatus,
  ExportMapping,
  ExportResult,
} from "@/types/asset";
import { apiFetch } from "./client";

export async function startExport(
  assetId: string,
  idempotencyKey: string,
  targetPlatform: "apple" | "youtube",
): Promise<ExportJob> {
  return apiFetch<ExportJob>("/api/exports", {
    method: "POST",
    body: { assetId, targetPlatform },
    idempotencyKey,
  });
}

export async function getExportStatus(jobId: string): Promise<ExportJobStatus> {
  return apiFetch<ExportJobStatus>(`/api/exports/${jobId}`);
}

export async function getExportResult(jobId: string): Promise<ExportResult> {
  return apiFetch<ExportResult>(`/api/exports/${jobId}/result`);
}

/** Re-search alternative videos for a low-confidence/failed track match. */
export async function getMatchCandidates(
  jobId: string,
  trackId: string,
): Promise<ExportCandidate[]> {
  return apiFetch<ExportCandidate[]>(
    `/api/exports/${jobId}/matches/${trackId}/candidates`,
  );
}

/** Confirm a user-chosen video for a track; returns the updated mapping. */
export async function overrideMatch(
  jobId: string,
  trackId: string,
  videoId: string,
  title?: string,
): Promise<ExportMapping> {
  return apiFetch<ExportMapping>(`/api/exports/${jobId}/matches/${trackId}`, {
    method: "POST",
    body: { videoId, title },
  });
}
