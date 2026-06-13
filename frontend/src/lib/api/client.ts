/**
 * Thin fetch wrapper for the plshare BE API.
 * - Resolves base URL from NEXT_PUBLIC_API_BASE_URL (defaults to http://localhost:8080).
 * - Auto-attaches X-Idempotency-Key when supplied.
 * - Throws ApiError with status + body for predictable handling.
 */

const BACKEND_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  body?: unknown;
  constructor(status: number, message: string, body?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export interface ApiOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  idempotencyKey?: string;
}

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const { body, idempotencyKey, headers, ...rest } = options;

  const finalHeaders: Record<string, string> = {
    Accept: "application/json",
    ...(headers as Record<string, string> | undefined),
  };

  let serializedBody: BodyInit | undefined;
  if (body !== undefined) {
    serializedBody = JSON.stringify(body);
    finalHeaders["Content-Type"] = "application/json";
  }

  if (idempotencyKey) {
    finalHeaders["X-Idempotency-Key"] = idempotencyKey;
  }

  const target =
    typeof window === "undefined"
      ? `${BACKEND_URL}${path}`
      : `/api/backend${path}`;
  const res = await fetch(target, {
    ...rest,
    headers: finalHeaders,
    body: serializedBody,
    cache: "no-store",
  });

  if (!res.ok) {
    let errBody: unknown;
    try {
      errBody = await res.json();
    } catch {
      errBody = await res.text().catch(() => undefined);
    }
    throw new ApiError(res.status, `API ${res.status}: ${path}`, errBody);
  }

  // 204 No Content
  if (res.status === 204) return undefined as T;

  const json: unknown = await res.json();
  // Unwrap the standard ApiResponse envelope `{ code, message, data }`.
  // Anything not shaped like the envelope (e.g. fixtures or legacy shapes) passes through.
  if (
    json !== null &&
    typeof json === "object" &&
    "data" in json &&
    "code" in json &&
    "message" in json
  ) {
    return (json as { data: T }).data;
  }
  return json as T;
}

export function makeIdempotencyKey(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export const apiBaseUrl = BACKEND_URL;
