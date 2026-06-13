import Link from "next/link";
import { SessionBadge } from "@/components/ui/SessionBadge";
import { conversionEnabled } from "@/lib/flags";
import { cn } from "@/lib/utils/cn";

export function PageShell({
  children,
  className,
  showHomeLink = true,
}: {
  children: React.ReactNode;
  className?: string;
  showHomeLink?: boolean;
}) {
  return (
    <div className="relative min-h-screen bg-bg text-text">
      {/* accent radial glow — sits behind all content */}
      <div
        className="accent-glow pointer-events-none absolute left-1/2 top-0 -z-0 h-[600px] w-[900px] -translate-x-1/2 -translate-y-1/4"
        aria-hidden
      />

      {/* sticky glass header */}
      <nav className="glass sticky top-0 z-30 w-full border-b border-hairline">
        <div className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-4 md:px-10">
          <Link
            href="/"
            className="text-lg font-extrabold tracking-tight text-text-hi transition-opacity duration-200 hover:opacity-80"
          >
            plshare
          </Link>
          <div className="flex items-center gap-5">
            {showHomeLink ? (
              <>
                <Link
                  href="/gift/send"
                  className="text-xs font-semibold uppercase tracking-[0.18em] text-accent transition-colors duration-200 hover:text-accent-hi"
                >
                  선물
                </Link>
                {conversionEnabled() ? (
                  <Link
                    href="/convert"
                    className="text-xs font-semibold uppercase tracking-[0.18em] text-text-mid transition-colors duration-200 hover:text-text-hi"
                  >
                    변환
                  </Link>
                ) : null}
                <Link
                  href="/feed"
                  className="text-xs font-semibold uppercase tracking-[0.18em] text-text-mid transition-colors duration-200 hover:text-text-hi"
                >
                  피드
                </Link>
                <Link
                  href="/ranking"
                  className="hidden text-xs font-semibold uppercase tracking-[0.18em] text-text-mid transition-colors duration-200 hover:text-text-hi sm:inline"
                >
                  랭킹
                </Link>
                <Link
                  href="/assets"
                  className="text-xs font-semibold uppercase tracking-[0.18em] text-text-mid transition-colors duration-200 hover:text-text-hi"
                >
                  Library
                </Link>
              </>
            ) : null}
            <SessionBadge />
          </div>
        </div>
      </nav>

      <main
        className={cn(
          "relative z-10 mx-auto w-full max-w-6xl px-6 pb-24 pt-10 md:px-10",
          className,
        )}
      >
        {children}
      </main>
    </div>
  );
}
