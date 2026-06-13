import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_APP_URL?.trim() || "http://localhost:3000",
  ),
  title: "plshare — 취향을 자산으로",
  description:
    "플레이리스트에 감성 맥락을 더하고 공유하며 YouTube Music으로 옮기는 취향 자산 레이어.",
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
