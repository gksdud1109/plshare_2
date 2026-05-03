import Link from "next/link";
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
        {showHomeLink ? (
          <Link
            href="/assets"
            className="text-xs uppercase tracking-[0.18em] text-ink-500 transition-colors duration-500 hover:text-ink-900"
          >
            Library
          </Link>
        ) : null}
      </nav>
      <main className={cn("mx-auto w-full max-w-6xl px-6 pb-20 md:px-10", className)}>
        {children}
      </main>
    </div>
  );
}
