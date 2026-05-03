/**
 * Thin fetch wrapper that will front the BE API once the contract is locked.
 * Keeping it intentionally minimal — swap to generated SDK (e.g. openapi-typescript
 * or Supabase client) once BE handoff lands.
 */

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (!res.ok) {
    throw new ApiError(res.status, `API ${res.status}: ${path}`);
  }

  return (await res.json()) as T;
}
