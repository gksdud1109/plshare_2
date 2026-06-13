import { type NextRequest, NextResponse } from "next/server";
import { getSession } from "@/lib/auth/session";

const API_BASE_URL =
  process.env.API_BASE_INTERNAL_URL?.trim() ||
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() ||
  "http://localhost:8080";

function safeReturnPath(value: string | null): string {
  return value?.startsWith("/") && !value.startsWith("//") ? value : "/import";
}

export async function GET(request: NextRequest) {
  const returnTo = safeReturnPath(request.nextUrl.searchParams.get("returnTo"));
  const target = new URL("/api/auth/spotify/start", API_BASE_URL);
  target.searchParams.set("returnTo", returnTo);

  const session = await getSession();
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
      { error: "Spotify authorization could not be started" },
      { status: 502 },
    );
  }
  return NextResponse.redirect(location);
}
