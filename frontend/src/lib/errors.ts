import { ApiError } from "@/lib/api/client";

/**
 * 어떤 에러든 사용자에게 보여줄 한국어 카피로 변환한다.
 * raw "API 500: /api/gifts" 같은 메시지를 그대로 노출하지 않기 위함.
 *
 * 우선순위: BE ApiResponse 의 한국어 message → HTTP 상태별 기본 카피 → fallback.
 */
export function messageFromError(err: unknown, fallback = "문제가 발생했어요. 잠시 후 다시 시도해 주세요."): string {
  if (err instanceof ApiError) {
    // BE 는 ApiResponse { code, message } 를 반환 — 한국어 message 가 있으면 우선 사용.
    const body = err.body as { message?: string; code?: string } | undefined;
    const beMessage = body?.message;
    if (beMessage && beMessage !== "OK" && !/^[A-Z_]+$/.test(beMessage)) return beMessage;

    switch (err.status) {
      case 401:
        return "로그인이 필요해요.";
      case 403:
        return "이 작업을 수행할 권한이 없어요.";
      case 404:
        return "요청한 정보를 찾을 수 없어요.";
      case 409:
        return "이미 처리된 요청이에요.";
      case 429:
        return "요청이 너무 많아요. 잠시 후 다시 시도해 주세요.";
      default:
        if (err.status >= 500) return "일시적인 오류가 발생했어요. 잠시 후 다시 시도해 주세요.";
        return fallback;
    }
  }
  return fallback;
}
