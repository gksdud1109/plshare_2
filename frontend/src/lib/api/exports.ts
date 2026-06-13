import type {
  ExportJob,
  ExportJobStatus,
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
