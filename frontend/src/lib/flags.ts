/**
 * Feature flags.
 *
 * Cross-platform conversion (Spotify/Apple import & export) is dormant per
 * product-baseline-v2.1 — the code is preserved but kept out of the gift-first
 * face. Off by default; set NEXT_PUBLIC_CONVERSION_ENABLED=true to resurface it
 * once a business entity / membership unlocks the real adapters.
 */
export function conversionEnabled(): boolean {
  return process.env.NEXT_PUBLIC_CONVERSION_ENABLED === "true";
}
