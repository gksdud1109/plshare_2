/**
 * 상대 경로(예: BE가 반환하는 "/share/{token}", "/gift/{token}")를 현재 origin 기준
 * 절대 URL로 변환한다. 클립보드 복사·표시에는 반드시 절대 URL을 써야 한다 —
 * 상대 경로를 복사해 브라우저에 붙여넣으면 `file:///share/...` 로 잘못 열린다.
 *
 * SSR(window 없음)에서는 NEXT_PUBLIC_APP_URL 또는 빈 문자열로 폴백한다.
 */
export function toAbsoluteUrl(pathOrUrl: string): string {
  if (/^https?:\/\//i.test(pathOrUrl)) return pathOrUrl; // 이미 절대 URL
  const origin =
    typeof window !== "undefined"
      ? window.location.origin
      : process.env.NEXT_PUBLIC_APP_URL?.trim() || "";
  const path = pathOrUrl.startsWith("/") ? pathOrUrl : `/${pathOrUrl}`;
  return `${origin}${path}`;
}
