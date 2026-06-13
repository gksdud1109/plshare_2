"use client";

import { Suspense } from "react";
import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { PageShell } from "@/components/ui/PageShell";
import { parsePlaylistLink } from "@/lib/convert/parsePlaylistLink";
import type { SpotifyPlaylist } from "@/types/asset";
import { demoPlaylists, demoYoutubePlaylists } from "@/lib/api/fixtures";
import { listPlaylists } from "@/lib/api/playlists";
import { listYoutubePlaylists } from "@/lib/api/youtube";
import { cn } from "@/lib/utils/cn";

// ── Types ──────────────────────────────────────────────────────────────────

type SourcePlatform = "spotify" | "youtube";
type DestinationPlatform = "apple" | "youtube_music";

interface ParsePreview {
  platform: SourcePlatform;
  playlistId: string;
  // Metadata fetched from library or inferred from link
  title?: string;
  coverUrl?: string;
  trackCount?: number;
}

type LinkState =
  | { kind: "idle" }
  | { kind: "parsing" }
  | { kind: "error"; message: string }
  | { kind: "ready"; preview: ParsePreview };

type LibraryTab = "spotify" | "youtube";

// ── Component ──────────────────────────────────────────────────────────────

function ConvertPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Pre-fill from ?url= query param (e.g. ranking card "변환⇄" CTA)
  const prefillUrl = searchParams.get("url") ?? "";

  const [linkInput, setLinkInput] = useState(prefillUrl);
  const [linkState, setLinkState] = useState<LinkState>({ kind: "idle" });
  const [destination, setDestination] = useState<DestinationPlatform | null>(null);
  const [libraryTab, setLibraryTab] = useState<LibraryTab>("spotify");
  const [showLibrary, setShowLibrary] = useState(false);

  const [spotifyPlaylists, setSpotifyPlaylists] = useState<SpotifyPlaylist[]>([]);
  const [youtubePlaylists, setYoutubePlaylists] = useState<SpotifyPlaylist[]>([]);
  const [libraryLoading, setLibraryLoading] = useState(false);

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Load library on demand
  useEffect(() => {
    if (!showLibrary) return;
    let cancelled = false;
    setLibraryLoading(true);
    Promise.all([
      listPlaylists().catch(() => demoPlaylists),
      listYoutubePlaylists().catch(() => demoYoutubePlaylists),
    ]).then(([sp, yt]) => {
      if (cancelled) return;
      setSpotifyPlaylists(sp);
      setYoutubePlaylists(yt);
      setLibraryLoading(false);
    });
    return () => { cancelled = true; };
  }, [showLibrary]);

  // Parse link with 300ms debounce
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);

    const trimmed = linkInput.trim();
    if (!trimmed) {
      setLinkState({ kind: "idle" });
      return;
    }

    setLinkState({ kind: "parsing" });

    debounceRef.current = setTimeout(() => {
      const parsed = parsePlaylistLink(trimmed);
      if (!parsed) {
        setLinkState({
          kind: "error",
          message: "올바른 플레이리스트 링크가 아니에요",
        });
        return;
      }
      setLinkState({
        kind: "ready",
        preview: {
          platform: parsed.platform,
          playlistId: parsed.playlistId,
        },
      });
    }, 300);

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [linkInput]);

  // Auto-parse prefill URL on mount
  useEffect(() => {
    if (prefillUrl) {
      const parsed = parsePlaylistLink(prefillUrl);
      if (parsed) {
        setLinkState({
          kind: "ready",
          preview: { platform: parsed.platform, playlistId: parsed.playlistId },
        });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function selectPlaylist(playlist: SpotifyPlaylist, platform: SourcePlatform) {
    setShowLibrary(false);
    setLinkInput(
      platform === "spotify"
        ? `https://open.spotify.com/playlist/${playlist.id}`
        : `https://music.youtube.com/playlist?list=${playlist.id}`,
    );
    setLinkState({
      kind: "ready",
      preview: {
        platform,
        playlistId: playlist.id,
        title: playlist.name,
        coverUrl: playlist.imageUrl,
        trackCount: playlist.trackCount,
      },
    });
  }

  function handleStart() {
    if (linkState.kind !== "ready" || !destination) return;
    const { preview } = linkState;

    // LAZY AUTH NOTE: In production, check session here.
    // If unauthenticated → redirect to /auth/spotify?redirect_after=/convert
    // with preview state preserved in sessionStorage.
    // For demo: proceed directly to /convert/progress.

    const params = new URLSearchParams({
      playlistId: preview.playlistId,
      sourcePlatform: preview.platform,
      destination,
      ...(preview.title ? { title: preview.title } : {}),
      ...(preview.coverUrl ? { coverUrl: preview.coverUrl } : {}),
      ...(preview.trackCount ? { trackCount: String(preview.trackCount) } : {}),
    });
    router.push(`/convert/progress?${params.toString()}`);
  }

  const isReady = linkState.kind === "ready";
  const canStart = isReady && destination !== null;

  return (
    <PageShell>
      <div className="mx-auto max-w-2xl">
        {/* ── Header ──────────────────────────────────────────────────── */}
        <header className="pt-8 pb-10 animate-fade-up">
          <p className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low mb-3">
            딸깍 변환
          </p>
          <h1
            className="font-display text-text-hi"
            style={{
              fontSize: "clamp(2rem, 4vw, 3rem)",
              fontWeight: 700,
              letterSpacing: "-0.02em",
            }}
          >
            플레이리스트를
            <br />
            <span className="text-text-mid font-normal">어디로 옮길까요?</span>
          </h1>
        </header>

        {/* ── URL input ───────────────────────────────────────────────── */}
        <section
          className="animate-fade-up"
          style={{ animationDelay: "60ms" }}
        >
          <div
            className={cn(
              "relative rounded-[var(--radius-input)] border transition-colors duration-200",
              linkState.kind === "error"
                ? "border-danger/60 bg-surface-2"
                : linkState.kind === "ready"
                  ? "border-accent/50 bg-surface-2"
                  : "border-hairline bg-surface-2 focus-within:border-hairline-strong",
            )}
          >
            {/* Platform badge (left icon) */}
            {linkState.kind === "ready" && (
              <span
                className="absolute left-4 top-1/2 -translate-y-1/2 text-xs font-semibold uppercase tracking-[0.12em]"
                style={{
                  color:
                    linkState.preview.platform === "spotify"
                      ? "#1ED760"
                      : "#FF0000",
                }}
              >
                {linkState.preview.platform === "spotify" ? "Spotify" : "YouTube"}
              </span>
            )}

            <input
              type="url"
              value={linkInput}
              onChange={(e) => setLinkInput(e.target.value)}
              placeholder="Spotify 또는 YouTube 링크를 붙여넣으세요"
              aria-label="플레이리스트 링크 입력"
              className={cn(
                "w-full rounded-[var(--radius-input)] bg-transparent py-4 text-[0.9375rem] text-text-hi outline-none placeholder:text-text-low",
                "transition-colors duration-200",
                linkState.kind === "ready" ? "pl-24 pr-4" : "px-4",
              )}
            />

            {/* Parsing indicator */}
            {linkState.kind === "parsing" && (
              <span
                className="absolute right-4 top-1/2 -translate-y-1/2 flex gap-1"
                aria-label="파싱 중"
              >
                {[0, 1, 2].map((i) => (
                  <span
                    key={i}
                    className="h-1.5 w-1.5 rounded-full bg-accent"
                    style={{
                      animation: `parseDot 1s ease-in-out ${i * 0.2}s infinite`,
                    }}
                  />
                ))}
              </span>
            )}
          </div>

          {/* Inline error */}
          {linkState.kind === "error" && (
            <p className="mt-2 text-sm text-danger" role="alert">
              {linkState.message}
            </p>
          )}

          {/* Library toggle */}
          <button
            type="button"
            onClick={() => setShowLibrary((v) => !v)}
            className="mt-3 text-sm text-text-mid transition-colors duration-200 hover:text-text-hi"
          >
            {showLibrary ? "▲ 라이브러리 닫기" : "내 라이브러리에서 선택"}
          </button>
        </section>

        {/* ── Library picker ──────────────────────────────────────────── */}
        {showLibrary && (
          <section
            className="mt-4 rounded-[var(--radius-card)] border border-hairline bg-surface-1 animate-fade-up"
            style={{ boxShadow: "var(--shadow-card)" }}
          >
            {/* Tabs */}
            <div className="flex border-b border-hairline">
              {(["spotify", "youtube"] as LibraryTab[]).map((tab) => (
                <button
                  key={tab}
                  type="button"
                  onClick={() => setLibraryTab(tab)}
                  className={cn(
                    "flex-1 py-3 text-xs font-semibold uppercase tracking-[0.14em] transition-colors duration-200",
                    libraryTab === tab
                      ? "text-accent border-b-2 border-accent -mb-px"
                      : "text-text-low hover:text-text-mid",
                  )}
                >
                  {tab === "spotify" ? "Spotify" : "YouTube"}
                </button>
              ))}
            </div>

            {libraryLoading ? (
              <div className="py-10 text-center text-sm text-text-low">
                불러오는 중…
              </div>
            ) : (
              <ul className="divide-y divide-hairline">
                {(libraryTab === "spotify" ? spotifyPlaylists : youtubePlaylists).map(
                  (pl) => (
                    <li key={pl.id}>
                      <button
                        type="button"
                        onClick={() => selectPlaylist(pl, libraryTab)}
                        className="flex w-full items-center gap-4 px-5 py-4 transition-colors duration-150 hover:bg-surface-2 text-left"
                      >
                        {pl.imageUrl && (
                          <img
                            src={pl.imageUrl}
                            alt=""
                            width={48}
                            height={48}
                            className="shrink-0 rounded-[10px] object-cover"
                          />
                        )}
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-semibold text-text-hi">
                            {pl.name}
                          </p>
                          {pl.description && (
                            <p className="mt-0.5 truncate text-xs text-text-mid">
                              {pl.description}
                            </p>
                          )}
                        </div>
                        <span className="shrink-0 text-xs tabular-nums text-text-low">
                          {pl.trackCount}곡
                        </span>
                      </button>
                    </li>
                  ),
                )}
              </ul>
            )}
          </section>
        )}

        {/* ── Preview card + destination picker — slides up on parse success ── */}
        {isReady && (
          <section
            className="mt-8 animate-fade-up"
            style={{ animationDelay: "0ms" }}
          >
            {/* Preview card */}
            <div
              className="glass flex items-center gap-4 rounded-[var(--radius-card)] border border-hairline p-4"
              style={{ boxShadow: "var(--shadow-card)" }}
            >
              {linkState.preview.coverUrl ? (
                <div className="relative shrink-0">
                  {/* Ambient glow */}
                  <div
                    className="cover-glow absolute inset-0 rounded-[10px]"
                    style={{
                      backgroundImage: `url(${linkState.preview.coverUrl})`,
                      backgroundSize: "cover",
                    }}
                    aria-hidden
                  />
                  <img
                    src={linkState.preview.coverUrl}
                    alt=""
                    width={64}
                    height={64}
                    className="relative rounded-[10px] object-cover"
                    style={{ border: "1px solid rgba(255,255,255,0.1)" }}
                  />
                </div>
              ) : (
                // Placeholder cover
                <div
                  className="shrink-0 rounded-[10px] bg-surface-3 flex items-center justify-center"
                  style={{ width: 64, height: 64 }}
                >
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden>
                    <path
                      d="M9 18V5l12-2v13"
                      stroke="var(--color-text-low)"
                      strokeWidth="1.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                    <circle cx="6" cy="18" r="3" stroke="var(--color-text-low)" strokeWidth="1.5" />
                    <circle cx="18" cy="16" r="3" stroke="var(--color-text-low)" strokeWidth="1.5" />
                  </svg>
                </div>
              )}
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-text-hi">
                  {linkState.preview.title ?? linkState.preview.playlistId}
                </p>
                {linkState.preview.trackCount != null && (
                  <p className="mt-0.5 text-xs text-text-mid tabular-nums">
                    {linkState.preview.trackCount}곡
                  </p>
                )}
                <span
                  className="mt-1 inline-flex rounded-full px-2 py-0.5 text-[0.625rem] font-semibold uppercase tracking-[0.12em]"
                  style={{
                    background:
                      linkState.preview.platform === "spotify"
                        ? "rgba(30,215,96,0.12)"
                        : "rgba(255,0,0,0.12)",
                    color:
                      linkState.preview.platform === "spotify"
                        ? "#1ED760"
                        : "#FF5555",
                  }}
                >
                  {linkState.preview.platform === "spotify" ? "Spotify" : "YouTube"}
                </span>
              </div>
            </div>

            {/* Destination picker */}
            <div className="mt-6">
              <p className="mb-3 text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-text-low">
                어디로 보낼까요?
              </p>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {/* Apple Music */}
                <DestinationCard
                  id="apple"
                  label="Apple Music"
                  description="MusicKit으로 연결"
                  icon={
                    <svg width="28" height="28" viewBox="0 0 28 28" fill="none" aria-hidden>
                      <rect width="28" height="28" rx="7" fill="#FC3C44" />
                      <path
                        d="M18.5 8.5v8.25a2.25 2.25 0 1 1-1.5-2.12V11l-6 1.35v5.4a2.25 2.25 0 1 1-1.5-2.12V10l9-2v.5z"
                        fill="white"
                      />
                    </svg>
                  }
                  selected={destination === "apple"}
                  onSelect={() => setDestination("apple")}
                />

                {/* YouTube Music */}
                <DestinationCard
                  id="youtube_music"
                  label="YouTube Music"
                  description="변환 UI만 완성 (통합 단계)"
                  icon={
                    <svg width="28" height="28" viewBox="0 0 28 28" fill="none" aria-hidden>
                      <rect width="28" height="28" rx="7" fill="#FF0000" />
                      <path
                        d="M21 14c0 3.866-3.134 7-7 7s-7-3.134-7-7 3.134-7 7-7 7 3.134 7 7z"
                        fill="white"
                      />
                      <path d="M12 11l5 3-5 3V11z" fill="#FF0000" />
                    </svg>
                  }
                  selected={destination === "youtube_music"}
                  onSelect={() => setDestination("youtube_music")}
                />
              </div>
            </div>

            {/* CTA */}
            <div className="mt-8 flex gap-3">
              <button
                type="button"
                onClick={handleStart}
                disabled={!canStart}
                className={cn(
                  "inline-flex h-12 items-center rounded-full px-8 text-sm font-semibold transition-all duration-200 focus-ring",
                  canStart
                    ? "bg-accent text-white hover:bg-accent-hi hover:-translate-y-0.5 active:bg-accent-press"
                    : "bg-surface-2 text-text-low cursor-not-allowed",
                )}
                style={canStart ? { boxShadow: "var(--shadow-glow)" } : undefined}
              >
                시작하기
              </button>
            </div>
          </section>
        )}
      </div>

      {/* Keyframe for parsing dots */}
      <style>{`
        @keyframes parseDot {
          0%, 100% { opacity: 0.3; transform: translateY(0); }
          50%       { opacity: 1;   transform: translateY(-3px); }
        }
      `}</style>
    </PageShell>
  );
}

// ── Destination card sub-component ────────────────────────────────────────

function DestinationCard({
  label,
  description,
  icon,
  selected,
  onSelect,
  id,
}: {
  id: string;
  label: string;
  description: string;
  icon: React.ReactNode;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      id={`dest-${id}`}
      onClick={onSelect}
      aria-pressed={selected}
      className={cn(
        "relative flex items-center gap-4 rounded-[var(--radius-card)] border p-5 text-left transition-all duration-200 focus-ring",
        selected
          ? "border-accent bg-surface-1"
          : "border-hairline bg-surface-1 hover:border-hairline-strong hover:-translate-y-0.5",
      )}
      style={selected ? { boxShadow: "0 0 0 1px var(--color-accent), var(--shadow-card)" } : { boxShadow: "var(--shadow-card)" }}
    >
      <div className="shrink-0">{icon}</div>
      <div className="min-w-0">
        <p className="text-sm font-semibold text-text-hi">{label}</p>
        <p className="mt-0.5 text-xs text-text-mid">{description}</p>
      </div>
      {selected && (
        <div
          className="absolute right-4 top-1/2 -translate-y-1/2 flex h-5 w-5 items-center justify-center rounded-full bg-accent"
          aria-hidden
        >
          <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
            <path d="M2 5l2.5 2.5L8 3" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </div>
      )}
    </button>
  );
}

export default function ConvertPage() {
  return (
    <Suspense>
      <ConvertPageInner />
    </Suspense>
  );
}
