"use client";

import { useState } from "react";
import type { ExportCandidate, ExportMapping } from "@/types/asset";
import { MatchConfidenceBadge } from "@/components/ui/MatchConfidenceBadge";
import { getMatchCandidates, overrideMatch } from "@/lib/api/exports";
import { cn } from "@/lib/utils/cn";

type RowState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "candidates"; list: ExportCandidate[] }
  | { kind: "saving" }
  | { kind: "resolved"; title: string }
  | { kind: "error"; message: string };

/**
 * Review surface for low-confidence (`alternative`) and `failed` track matches
 * in a YouTube Music export. Each row lets the user re-search candidates and
 * confirm the correct video — the override is persisted and added to the
 * playlist by the backend.
 */
export function MatchReviewSection({
  jobId,
  mappings,
  interactive = true,
}: {
  jobId: string;
  /** Only reviewable mappings (status !== "matched"). */
  mappings: ExportMapping[];
  /** false when rendering demo fixtures without a real export job. */
  interactive?: boolean;
}) {
  const [rows, setRows] = useState<Record<string, RowState>>({});
  const [open, setOpen] = useState(false);

  if (mappings.length === 0) return null;

  const setRow = (trackId: string, s: RowState) =>
    setRows((prev) => ({ ...prev, [trackId]: s }));

  async function loadCandidates(trackId: string) {
    setRow(trackId, { kind: "loading" });
    try {
      const list = await getMatchCandidates(jobId, trackId);
      setRow(trackId, { kind: "candidates", list });
    } catch {
      setRow(trackId, { kind: "error", message: "후보를 불러오지 못했어요." });
    }
  }

  async function choose(trackId: string, c: ExportCandidate) {
    setRow(trackId, { kind: "saving" });
    try {
      await overrideMatch(jobId, trackId, c.videoId, c.title);
      setRow(trackId, { kind: "resolved", title: c.title });
    } catch {
      setRow(trackId, { kind: "error", message: "교체에 실패했어요. 다시 시도해 주세요." });
    }
  }

  const resolvedCount = Object.values(rows).filter((s) => s.kind === "resolved").length;
  const remaining = mappings.length - resolvedCount;

  return (
    <section
      className="mb-8 overflow-hidden rounded-[var(--radius-card)] border border-hairline bg-surface-1 animate-fade-up"
      style={{ animationDelay: "80ms", boxShadow: "var(--shadow-card)" }}
    >
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex w-full items-center justify-between px-5 py-4 transition-colors duration-150 hover:bg-surface-2 focus-ring"
        aria-expanded={open}
      >
        <span className="text-sm font-semibold text-text-hi">
          {remaining > 0 ? (
            <>
              <span className="text-warning tabular-nums">{remaining}곡</span> 검토가 필요해요
            </>
          ) : (
            <span className="text-success">모든 검토를 마쳤어요</span>
          )}
        </span>
        <span
          className="text-text-low transition-transform duration-300"
          style={{ transform: open ? "rotate(180deg)" : "none" }}
          aria-hidden
        >
          ▾
        </span>
      </button>

      {open && (
        <ul className="border-t border-hairline">
          {mappings.map((m) => {
            const state: RowState = rows[m.trackId] ?? { kind: "idle" };
            const resolved = state.kind === "resolved";
            return (
              <li key={m.trackId} className="border-b border-hairline px-5 py-4 last:border-b-0">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-text-hi">
                      {m.title ?? "트랙"}
                      <span className="text-text-low"> · {m.artist ?? "—"}</span>
                    </p>
                    {resolved ? (
                      <p className="mt-0.5 truncate text-xs text-success">
                        확정: {state.title}
                      </p>
                    ) : m.matchedTitle ? (
                      <p className="mt-0.5 truncate text-xs text-text-mid">
                        현재 매칭: {m.matchedTitle}
                        {typeof m.confidence === "number" && (
                          <span className="text-text-low">
                            {" "}· {Math.round(m.confidence * 100)}%
                          </span>
                        )}
                      </p>
                    ) : (
                      <p className="mt-0.5 text-xs text-text-low">매칭된 곡이 없어요.</p>
                    )}
                  </div>
                  <MatchConfidenceBadge
                    status={resolved ? "matched" : m.status}
                    className="shrink-0"
                  />
                </div>

                {interactive && !resolved && (
                  <div className="mt-3">
                    {state.kind === "idle" || state.kind === "error" ? (
                      <>
                        <button
                          type="button"
                          onClick={() => loadCandidates(m.trackId)}
                          className="rounded-full border border-hairline bg-surface-2 px-4 py-1.5 text-xs font-semibold text-text-hi transition-colors duration-150 hover:border-accent/50 hover:text-accent focus-ring"
                        >
                          다른 곡 선택
                        </button>
                        {state.kind === "error" && (
                          <span className="ml-3 text-xs text-danger">{state.message}</span>
                        )}
                      </>
                    ) : state.kind === "loading" ? (
                      <p className="text-xs text-text-mid animate-pulse">후보를 검색하고 있어요…</p>
                    ) : state.kind === "saving" ? (
                      <p className="text-xs text-text-mid animate-pulse">교체하고 있어요…</p>
                    ) : state.kind === "candidates" ? (
                      <div className="flex flex-col gap-1.5">
                        {state.list.length === 0 ? (
                          <p className="text-xs text-text-low">검색 결과가 없어요.</p>
                        ) : (
                          state.list.map((c) => (
                            <button
                              key={c.videoId}
                              type="button"
                              onClick={() => choose(m.trackId, c)}
                              className={cn(
                                "flex flex-col items-start rounded-[var(--radius-input)] border border-hairline bg-surface-2 px-3 py-2 text-left transition-colors duration-150",
                                "hover:border-accent/50 hover:bg-surface-3 focus-ring",
                              )}
                            >
                              <span className="truncate text-xs font-medium text-text-hi">
                                {c.title}
                              </span>
                              {c.channelTitle && (
                                <span className="truncate text-[0.6875rem] text-text-low">
                                  {c.channelTitle}
                                </span>
                              )}
                            </button>
                          ))
                        )}
                      </div>
                    ) : null}
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
