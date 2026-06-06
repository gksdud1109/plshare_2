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

interface GrantStatus {
  grantId: string;
  userId?: string;
  expiresAt?: string;
  expiringSoon?: boolean;
  scope?: string;
}

export async function GET() {
  const session = await getSession();
  if (!session) {
    return NextResponse.json({ authenticated: false });
  }

  try {
    const response = await fetch(
      `${API_BASE_URL}/api/auth/spotify/me?grantId=${encodeURIComponent(session.grantId)}`,
      { cache: "no-store" },
    );

    if (!response.ok) {
      throw new Error(`Spotify grant validation failed with ${response.status}`);
    }

    const grant = (await response.json()) as GrantStatus;
    if (grant.grantId !== session.grantId) {
      throw new Error("Spotify grant validation returned a different grant");
    }

    const verifiedSession: Session = {
      ...session,
      userId: grant.userId ?? session.userId,
    };

    return NextResponse.json({
      authenticated: true,
      session: verifiedSession,
    });
  } catch {
    if (session.demo) {
      return NextResponse.json({ authenticated: true, session });
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
