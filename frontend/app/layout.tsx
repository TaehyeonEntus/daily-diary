import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { RootProvider } from "@/components/providers/root-provider";
import Link from "next/link";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "일기를 써봅시다",
  description: "매일매일 기록하는 나만의 일기",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${geistSans.variable} ${geistMono.variable}`}>
      <body className="antialiased font-sans">
        <RootProvider>
          <header className="border-b sticky top-0 z-10" style={{ backgroundColor: 'var(--header-bg)', borderColor: 'var(--border-color)' }}>
            <div className="max-w-4xl mx-auto px-4 h-16 flex items-center justify-between">
              <Link href="/" className="text-xl font-bold hover:opacity-80 transition-opacity" style={{ color: 'var(--primary-color)' }}>
                Daily Diary
              </Link>
              <nav className="flex gap-6">
                <Link href="/write" className="text-sm font-medium hover:opacity-80 transition-opacity" style={{ color: 'var(--text-main)' }}>
                  일기 쓰기
                </Link>
                <Link href="/profile" className="text-sm font-medium hover:opacity-80 transition-opacity" style={{ color: 'var(--text-main)' }}>
                  내 프로필
                </Link>
              </nav>
            </div>
          </header>
          <main className="max-w-4xl mx-auto px-4 py-8">
            {children}
          </main>
        </RootProvider>
      </body>
    </html>
  );
}
