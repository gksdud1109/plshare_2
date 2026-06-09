import type { SharedAsset } from "@/types/asset";
import { apiFetch } from "./client";

/**
 * Client-side fetch (browser): cache disabled, used by interactive components.
 */
export async function getShared(token: string): Promise<SharedAsset> {
  return apiFetch<SharedAsset>(`/api/share/${token}`);
}

/**
 * Server-side fetch (RSC / generateMetadata / OG image route).
 *
 * - Uses an internal base URL when available (server-only env), falling back to
 *   the public one. Allows the server runtime to talk to the BE over a private
 *   network in production while still working in local dev.
 * - ISR: cached for 5 minutes, tagged so it can be revalidated on demand.
 * - Throws on non-OK so callers (generateMetadata / OG route) can fall back.
 */
export async function fetchShareDataServer(
  token: string,
): Promise<SharedAsset> {
  const base =
    process.env.API_BASE_INTERNAL_URL?.trim() ||
    process.env.NEXT_PUBLIC_API_BASE_URL?.trim() ||
    "http://localhost:8080";

  const res = await fetch(`${base}/api/share/${token}`, {
    headers: { Accept: "application/json" },
    cache: "force-cache",
    next: { revalidate: 300, tags: [`share-${token}`] },
  });

  if (!res.ok) {
    throw new Error(`fetchShareDataServer: ${res.status} ${res.statusText}`);
  }

  const json: unknown = await res.json();
  // Unwrap the ApiResponse envelope `{ code, message, data }` if present.
  const payload =
    json !== null && typeof json === "object" && "data" in json
      ? (json as { data: unknown }).data
      : json;
  return payload as SharedAsset;
}
