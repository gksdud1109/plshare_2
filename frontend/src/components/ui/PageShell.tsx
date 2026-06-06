import Link from "next/link";
import { SessionBadge } from "@/components/ui/SessionBadge";
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
    <div className="min-h-screen">
      <nav className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-6 md:px-10">
        <Link
          href="/"
          className="font-display text-lg tracking-tight text-ink-900"
        >
          plshare
        </Link>
        <div className="flex items-center gap-5">
          {showHomeLink ? (
            <Link
              href="/assets"
              className="text-xs uppercase tracking-[0.18em] text-ink-500 transition-colors duration-500 hover:text-ink-900"
            >
              Library
            </Link>
          ) : null}
          <SessionBadge />
        </div>
      </nav>
      <main className={cn("mx-auto w-full max-w-6xl px-6 pb-20 md:px-10", className)}>
        {children}
      </main>
    </div>
  );
}
