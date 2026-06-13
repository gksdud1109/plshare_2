import { type NextRequest, NextResponse } from "next/server";
import { getSession } from "@/lib/auth/session";

const API_BASE_URL =
  process.env.API_BASE_INTERNAL_URL?.trim() ||
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() ||
  "http://localhost:8080";

function safeReturnPath(value: string | null): string {
  return value?.startsWith("/") && !value.startsWith("//") ? value : "/convert";
}

export async function GET(request: NextRequest) {
  const returnTo = safeReturnPath(request.nextUrl.searchParams.get("returnTo"));
  const scope = request.nextUrl.searchParams.get("scope") === "youtube"
    ? "youtube"
    : undefined;
  const session = await getSession();

  const target = new URL("/api/auth/google/start", API_BASE_URL);
  if (scope) target.searchParams.set("scope", scope);
  target.searchParams.set("returnTo", returnTo);

  const response = await fetch(target, {
    cache: "no-store",
    redirect: "manual",
    headers: session?.sessionToken
      ? { Authorization: `Bearer ${session.sessionToken}` }
      : undefined,
  });
  const location = response.headers.get("location");
  if (!location || response.status < 300 || response.status >= 400) {
    return NextResponse.json(
      { error: "Google authorization could not be started" },
      { status: 502 },
    );
  }
  return NextResponse.redirect(location);
}
