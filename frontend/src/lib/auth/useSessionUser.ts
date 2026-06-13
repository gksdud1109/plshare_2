"use client";

/**
 * useSessionUser — client-side hook that fetches the current session
 * from /api/auth/session and returns userId + handle for social operations.
 *
 * The /api/auth/session GET endpoint is extended to resolve userId for
 * demo sessions via GET /api/users/demo (handle="demo", seeded by DemoDataSeeder).
 * See: src/app/api/auth/session/route.ts
 */

import { useEffect, useState } from "react";

export interface SessionUser {
  userId: string;
  /** derived from session; may be undefined until fully resolved */
  handle?: string;
}

interface SessionResponse {
  authenticated: boolean;
  session?: {
    sessionToken?: string;
    grantId?: string;
    userId?: string;
    demo?: boolean;
  };
}

type SessionUserState =
  | { status: "loading" }
  | { status: "unauthenticated" }
  | { status: "authenticated"; user: SessionUser };

export function useSessionUser(): SessionUserState {
  const [state, setState] = useState<SessionUserState>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;
    fetch("/api/auth/session", { cache: "no-store" })
      .then((r) => r.json() as Promise<SessionResponse>)
      .then((data) => {
        if (cancelled) return;
        if (!data.authenticated || !data.session?.userId) {
          setState({ status: "unauthenticated" });
          return;
        }
        setState({
          status: "authenticated",
          user: { userId: data.session.userId },
        });
      })
      .catch(() => {
        if (!cancelled) setState({ status: "unauthenticated" });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return state;
}
