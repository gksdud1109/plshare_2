"use client";

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { cn } from "@/lib/utils/cn";

type ToastVariant = "success" | "error" | "info";

interface ToastItem {
  id: number;
  message: string;
  variant: ToastVariant;
}

interface ToastApi {
  show: (message: string, variant?: ToastVariant) => void;
  success: (message: string) => void;
  error: (message: string) => void;
  info: (message: string) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

const ACCENT: Record<ToastVariant, string> = {
  success: "var(--color-success)",
  error: "var(--color-danger)",
  info: "var(--color-accent)",
};

const DURATION_MS = 3200;

/**
 * 앱 전역 토스트. 페이지/컴포넌트는 useToast() 로 success/error/info 를 띄운다.
 * 에러는 messageFromError 로 변환한 한국어 카피를 넘긴다. RootLayout 에서 1회 마운트.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const idRef = useRef(0);

  const remove = useCallback((id: number) => {
    setToasts((list) => list.filter((t) => t.id !== id));
  }, []);

  const show = useCallback(
    (message: string, variant: ToastVariant = "info") => {
      const id = ++idRef.current;
      setToasts((list) => [...list, { id, message, variant }]);
      setTimeout(() => remove(id), DURATION_MS);
    },
    [remove],
  );

  const api = useMemo<ToastApi>(
    () => ({
      show,
      success: (m) => show(m, "success"),
      error: (m) => show(m, "error"),
      info: (m) => show(m, "info"),
    }),
    [show],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-8 z-50 flex flex-col items-center gap-2 px-4"
      >
        {toasts.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => remove(t.id)}
            className={cn(
              "pointer-events-auto glass animate-fade-up",
              "flex max-w-md items-center gap-3 rounded-2xl px-5 py-3",
              "border-l-2 shadow-[var(--shadow-pop)]",
              "text-sm font-medium text-text-hi text-left",
            )}
            style={{ borderLeftColor: ACCENT[t.variant] }}
          >
            {t.message}
          </button>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within <ToastProvider>");
  return ctx;
}
