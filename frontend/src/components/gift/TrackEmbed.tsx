"use client";

/**
 * 인라인 YouTube 임베드(곡별 샘플 재생). videoId가 null이면 재생 불가 안내.
 * 선물 언박싱과 공유 페이지가 공유한다.
 */
export function TrackEmbed({
  videoId,
  title,
}: {
  videoId: string | null;
  title: string;
}) {
  if (videoId === null) {
    return (
      <p className="px-4 pb-3 text-xs text-text-low">이 곡은 지금 재생할 수 없어요.</p>
    );
  }
  return (
    <div className="px-4 pb-3">
      <div
        className="overflow-hidden"
        style={{ borderRadius: "var(--radius-image)", aspectRatio: "16 / 9" }}
      >
        <iframe
          title={`${title} — YouTube`}
          src={`https://www.youtube.com/embed/${videoId}?autoplay=1&rel=0&playsinline=1`}
          allow="autoplay; encrypted-media; picture-in-picture"
          allowFullScreen
          className="h-full w-full"
          style={{ border: 0 }}
        />
      </div>
    </div>
  );
}
