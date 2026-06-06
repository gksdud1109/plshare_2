import { cookies } from "next/headers";

export interface Session {
  grantId: string;
  userId?: string;
  demo?: boolean;
}

export const SESSION_COOKIE_NAME = "plshare_session";

const SESSION_COOKIE_OPTIONS = {
  httpOnly: true,
  sameSite: "lax" as const,
  path: "/",
  secure: process.env.NODE_ENV === "production",
  maxAge: 60 * 60 * 24 * 30,
};

function isSession(value: unknown): value is Session {
  if (!value || typeof value !== "object") return false;

  const session = value as Record<string, unknown>;
  return (
    typeof session.grantId === "string" &&
    session.grantId.length > 0 &&
    (session.userId === undefined || typeof session.userId === "string") &&
    (session.demo === undefined || typeof session.demo === "boolean")
  );
}

function serializeSession(session: Session): string {
  return encodeURIComponent(JSON.stringify(session));
}

function parseSession(value: string): Session | null {
  try {
    const parsed: unknown = JSON.parse(decodeURIComponent(value));
    return isSession(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

export async function getSession(): Promise<Session | null> {
  const cookieStore = await cookies();
  const value = cookieStore.get(SESSION_COOKIE_NAME)?.value;
  return value ? parseSession(value) : null;
}

export async function setSessionCookie(session: Session): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.set(
    SESSION_COOKIE_NAME,
    serializeSession(session),
    SESSION_COOKIE_OPTIONS,
  );
}

export async function clearSessionCookie(): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.set(SESSION_COOKIE_NAME, "", {
    ...SESSION_COOKIE_OPTIONS,
    maxAge: 0,
  });
}
