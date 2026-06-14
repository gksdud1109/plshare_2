import type { Metadata, Viewport } from "next";
import "./globals.css";
import { ToastProvider } from "@/components/ui/ToastProvider";

// 모바일/인앱 WebView 대응 — 디바이스 폭 기준, 노치 safe-area 커버.
export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export const metadata: Metadata = {
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_APP_URL?.trim() || "http://localhost:3000",
  ),
  title: "plshare — 취향을 선물로",
  description:
    "좋아하는 곡에 감성 맥락을 담아 소중한 사람에게 선물하는 음악 선물 레이어. 받는 사람은 앱·계정 없이 바로 듣습니다.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="h-full">
      <body className="bg-bg text-text flex min-h-full flex-col antialiased">
        <ToastProvider>{children}</ToastProvider>
      </body>
    </html>
  );
}
