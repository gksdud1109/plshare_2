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

  const requestedPath = `${request.nextUrl.pathname}${request.nextUrl.search}`;
  const spotifyConnect = new URL("/auth/spotify", request.url);
  spotifyConnect.searchParams.set("live", "1");
  spotifyConnect.searchParams.set("next", requestedPath);
  // Redirect to the INTERNAL /auth/continue interstitial, not straight to the
  // external OAuth start. Next.js strips Next-Router-Prefetch / RSC headers
  // before middleware, so we cannot tell a prefetch from a real navigation here
  // — and bouncing a prefetch through the external provider burns an OAuth
  // handshake and (in demo) hits an unresolvable mock host. /auth/continue does
  // the external hop in a client effect, which prefetch never executes, so
  // prefetch probes stop at the internal shell while real nav still auto-forwards.
  const continueUrl = new URL("/auth/continue", request.url);
  continueUrl.searchParams.set(
    "returnTo",
    `${spotifyConnect.pathname}${spotifyConnect.search}`,
  );
  return NextResponse.redirect(continueUrl);
}

export const config = {
  matcher: ["/assets/:path*", "/import/:path*"],
};
