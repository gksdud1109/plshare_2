import { NextResponse, type NextRequest } from "next/server";
import { setSessionCookie } from "@/lib/auth/session";

export async function GET(request: NextRequest) {
  const sessionToken = request.nextUrl.searchParams.get("session")?.trim();

  if (!sessionToken || sessionToken.length < 32 || sessionToken.length > 4096) {
    return NextResponse.json(
      { error: "A valid application session is required" },
      { status: 400 },
    );
  }

  await setSessionCookie({ sessionToken });
  const requestedNext = request.nextUrl.searchParams.get("next");
  const next =
    requestedNext?.startsWith("/") && !requestedNext.startsWith("//")
      ? requestedNext
      : "/import";
  return NextResponse.redirect(new URL(next, request.url));
}
