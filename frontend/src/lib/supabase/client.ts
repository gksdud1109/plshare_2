/**
 * Supabase auth client — placeholder. The handoff locks Supabase Auth as the
 * auth provider; we leave this file as a clear integration point so the later
 * wiring is obvious. Install @supabase/supabase-js + env vars when BE is ready.
 */

export const SUPABASE_URL = process.env.NEXT_PUBLIC_SUPABASE_URL ?? "";
export const SUPABASE_ANON_KEY =
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ?? "";
