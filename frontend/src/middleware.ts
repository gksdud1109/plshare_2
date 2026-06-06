import { NextResponse, type NextRequest } from "next/server";

const SESSION_COOKIE_NAME = "plshare_session";

export function middleware(request: NextRequest) {
  const callbackSession = request.nextUrl.searchParams.get("session");

  // The backend returns to /import?session=...; hand that grant to the
  // same-origin route handler so it can set the httpOnly cookie.
  if (request.nextUrl.pathname === "/import" && callbackSession) {
    const callbackUrl = new URL("/api/auth/callback", request.url);
    callbackUrl.searchParams.set("session", callbackSession);
    return NextResponse.redirect(callbackUrl);
  }

  if (request.cookies.has(SESSION_COOKIE_NAME)) {
    return NextResponse.next();
  }

  const authUrl = new URL("/auth/spotify", request.url);
  authUrl.searchParams.set(
    "next",
    `${request.nextUrl.pathname}${request.nextUrl.search}`,
  );
  return NextResponse.redirect(authUrl);
}

export const config = {
  matcher: ["/assets/:path*", "/import/:path*"],
};
