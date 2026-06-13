import { NextResponse } from "next/server";
import {
  clearSessionCookie,
  getSession,
  setSessionCookie,
  type Session,
} from "@/lib/auth/session";

const API_BASE_URL =
  process.env.API_BASE_INTERNAL_URL?.trim() ||
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() ||
  "http://localhost:8080";

interface BackendSession {
  userId: string;
  spotifyGrantId?: string | null;
}

export async function GET() {
  const session = await getSession();
  if (!session) {
    return NextResponse.json({ authenticated: false });
  }

  try {
    if (!session.sessionToken) throw new Error("Missing application session token");
    const response = await fetch(`${API_BASE_URL}/api/auth/session`, {
      cache: "no-store",
      headers: { Authorization: `Bearer ${session.sessionToken}` },
    });

    if (!response.ok) {
      throw new Error(`Application session validation failed with ${response.status}`);
    }

    const payload = (await response.json()) as unknown;
    // Unwrap the BE ApiResponse envelope `{ code, message, data }` if present.
    const backendSession = (
      payload !== null && typeof payload === "object" && "data" in payload
        ? (payload as { data: BackendSession }).data
        : payload
    ) as BackendSession;

    const verifiedSession: Session = {
      ...session,
      userId: backendSession.userId,
      grantId: backendSession.spotifyGrantId ?? undefined,
    };

    return NextResponse.json({
      authenticated: true,
      session: verifiedSession,
    });
  } catch {
    if (session.demo) {
      /**
       * DESIGN NOTE (pre-session integration, fe-feed-001):
       * Demo sessions have grantId="demo-grant" and no userId.
       * We resolve the demo user's UUID by calling GET /api/users/demo
       * (handle "demo" is seeded by DemoDataSeeder on @Profile("demo")).
       * On failure we fall back to the bare session so existing demo flows
       * (E2E, convert, assets) are not disrupted.
       *
       * Once Spring Security + JWT lands, this entire block is replaced by
       * @AuthenticationPrincipal on the BE side — the session cookie will
       * carry userId directly.
       */
      let resolvedSession = session;
      if (!session.userId) {
        try {
          const demoRes = await fetch(`${API_BASE_URL}/api/users/demo`, {
            cache: "no-store",
          });
          if (demoRes.ok) {
            const demoPayload = (await demoRes.json()) as unknown;
            const demoData =
              demoPayload !== null &&
              typeof demoPayload === "object" &&
              "data" in demoPayload
                ? (demoPayload as { data: { id?: string } }).data
                : (demoPayload as { id?: string });
            if (demoData?.id) {
              resolvedSession = { ...session, userId: demoData.id };
            }
          }
        } catch {
          // fall back to bare session — existing demo flow unaffected
        }
      }
      return NextResponse.json({ authenticated: true, session: resolvedSession });
    }

    await clearSessionCookie();
    return NextResponse.json({ authenticated: false });
  }
}

export async function POST(request: Request) {
  const requestHost = new URL(request.url).hostname;
  const isLocalRequest = ["localhost", "127.0.0.1"].includes(requestHost);

  if (!isLocalRequest) {
    return NextResponse.json(
      { error: "Demo sessions are only available locally" },
      { status: 403 },
    );
  }

  let body: unknown;

  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const demoSession =
    body &&
    typeof body === "object" &&
    (body as Record<string, unknown>).demo === true &&
    (body as Record<string, unknown>).grantId === "demo-grant";

  if (!demoSession) {
    return NextResponse.json(
      { error: "Only the local demo session can be created here" },
      { status: 400 },
    );
  }

  const session: Session = { grantId: "demo-grant", demo: true };
  await setSessionCookie(session);

  return NextResponse.json({ authenticated: true, session });
}
