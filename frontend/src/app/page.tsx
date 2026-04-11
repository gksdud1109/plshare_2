import Link from "next/link";

export default function Page() {
  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col justify-between px-6 py-16 md:px-12 md:py-24">
      <header className="flex items-center justify-between">
        <span className="font-display text-lg tracking-tight">plshare</span>
        <span className="text-xs uppercase tracking-[0.24em] text-ink-500">
          v0.2 · Scaffold
        </span>
      </header>

      <section className="max-w-2xl py-24">
        <p className="mb-6 text-xs uppercase tracking-[0.24em] text-ink-500">
          Refined Asset
        </p>
        <h1 className="text-5xl leading-[0.95] md:text-7xl">
          Keep the playlist
          <br />
          <em className="font-light italic text-ink-600">as an asset.</em>
        </h1>
        <p className="mt-8 max-w-lg font-sans text-base leading-relaxed text-ink-600">
          스포티파이의 플레이리스트를 가져와, 하나의 물성을 가진 카드로 다듬고,
          애플 뮤직으로 건네줍니다. 가볍게 흘려보내지 않도록.
        </p>

        <div className="mt-12 flex items-center gap-6">
          <Link
            href="/import"
            className="inline-flex items-center gap-3 border-b border-ink-900 pb-1 text-sm uppercase tracking-[0.18em] text-ink-900 transition-colors duration-500 ease-[cubic-bezier(0.22,0.61,0.36,1)] hover:text-gold-600"
          >
            Begin import
            <span aria-hidden>→</span>
          </Link>
          <span className="text-xs tracking-wide text-ink-400">
            Backend contract pending
          </span>
        </div>
      </section>

      <footer className="border-t border-stone-200 pt-6 text-xs tracking-wide text-ink-400">
        © {new Date().getFullYear()} plshare. A quiet object.
      </footer>
    </main>
  );
}
