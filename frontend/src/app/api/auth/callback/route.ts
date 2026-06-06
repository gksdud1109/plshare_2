import { NextResponse, type NextRequest } from "next/server";
import { setSessionCookie } from "@/lib/auth/session";

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export async function GET(request: NextRequest) {
  const grantId = request.nextUrl.searchParams.get("session")?.trim();

  if (!grantId || !UUID_PATTERN.test(grantId)) {
    return NextResponse.json(
      { error: "A valid Spotify grant session is required" },
      { status: 400 },
    );
  }

  await setSessionCookie({ grantId });
  return NextResponse.redirect(new URL("/import", request.url));
}
