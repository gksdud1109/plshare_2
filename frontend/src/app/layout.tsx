import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "plshare — 취향을 자산으로",
  description:
    "Spotify에서 가져오고, 감성 맥락을 더해, Apple Music으로 내보내는 취향 자산 레이어.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="h-full">
      <body className="bg-bg text-text flex min-h-full flex-col antialiased">
        {children}
      </body>
    </html>
  );
}
