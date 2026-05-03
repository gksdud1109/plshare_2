import Link from "next/link";

export default function LandingPage() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-5xl flex-1 flex-col justify-between px-6 py-16 md:px-12 md:py-24">
      <header className="flex items-center justify-between">
        <span className="font-display text-lg tracking-tight">plshare</span>
        <span className="text-xs uppercase tracking-[0.24em] text-ink-500">
          v0.2 · Demo
        </span>
      </header>

      <section className="max-w-2xl py-16 md:py-24">
        <p className="mb-6 text-xs uppercase tracking-[0.24em] text-ink-500">
          Refined Asset
        </p>
        <h1 className="text-5xl leading-[0.95] md:text-7xl">
          당신의 취향을
          <br />
          <em className="font-light italic text-ink-600">자산으로.</em>
        </h1>
        <p className="mt-8 max-w-lg font-sans text-base leading-relaxed text-ink-600">
          Spotify에서 가져오고, 감성 맥락을 더해, Apple Music으로 내보내세요.
          가볍게 흘려보내지 않도록.
        </p>

        <div className="mt-12 flex items-center gap-6">
          <Link
            href="/auth/spotify"
            className="inline-flex items-center gap-3 rounded-full bg-ink-900 px-6 py-3 text-sm tracking-[0.18em] text-bone-50 transition-colors duration-500 ease-[var(--ease-weighted)] hover:bg-ink-700"
          >
            Spotify로 시작하기
            <span aria-hidden>→</span>
          </Link>
          <Link
            href="/assets"
            className="text-sm tracking-wide text-ink-500 underline-offset-4 transition-colors duration-500 hover:text-ink-900 hover:underline"
          >
            라이브러리 둘러보기
          </Link>
        </div>
      </section>

      <footer className="grid grid-cols-1 gap-6 border-t border-stone-200 pt-10 text-sm text-ink-500 md:grid-cols-3">
        <Step
          n="01"
          title="Spotify에서 가져오기"
          body="플레이리스트를 선택해 자산으로 정리합니다."
        />
        <Step
          n="02"
          title="감성 맥락 더하기"
          body="당시의 기분과 이야기를 함께 남깁니다."
        />
        <Step
          n="03"
          title="Apple Music으로 내보내기"
          body="ISRC 기반으로 매핑해 새 플랫폼에서 다시 듣습니다."
        />
      </footer>
    </main>
  );
}

function Step({ n, title, body }: { n: string; title: string; body: string }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-[0.24em] text-ink-400">{n}</p>
      <p className="mt-2 font-display text-base text-ink-900">{title}</p>
      <p className="mt-1 leading-relaxed text-ink-500">{body}</p>
    </div>
  );
}
