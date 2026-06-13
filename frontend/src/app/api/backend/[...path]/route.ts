import { type NextRequest, NextResponse } from "next/server";
import { getSession } from "@/lib/auth/session";

const API_BASE_URL =
  process.env.API_BASE_INTERNAL_URL?.trim() ||
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() ||
  "http://localhost:8080";

async function proxy(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> },
) {
  const { path } = await context.params;
  const target = new URL(`/${path.join("/")}`, API_BASE_URL);
  target.search = request.nextUrl.search;

  const headers = new Headers();
  for (const name of ["accept", "content-type", "x-idempotency-key"]) {
    const value = request.headers.get(name);
    if (value) headers.set(name, value);
  }

  const session = await getSession();
  if (session?.sessionToken) {
    headers.set("Authorization", `Bearer ${session.sessionToken}`);
  }

  const hasBody = !["GET", "HEAD"].includes(request.method);
  const response = await fetch(target, {
    method: request.method,
    headers,
    body: hasBody ? await request.arrayBuffer() : undefined,
    cache: "no-store",
    redirect: "manual",
  });

  const responseHeaders = new Headers({
    "content-type": response.headers.get("content-type") ?? "application/json",
  });
  const location = response.headers.get("location");
  if (location) responseHeaders.set("location", location);

  return new NextResponse(response.body, {
    status: response.status,
    headers: responseHeaders,
  });
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
