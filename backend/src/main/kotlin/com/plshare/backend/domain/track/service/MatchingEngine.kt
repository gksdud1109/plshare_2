package com.plshare.backend.domain.track.service

import com.plshare.backend.domain.track.model.CanonicalTrack
import com.plshare.backend.domain.track.repository.CanonicalTrackRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

/**
 * Canonical Track 정규화 엔진의 fuzzy matching 구현.
 *
 * 매칭 우선순위 (docs/20-product/data/canonical-track-normalization-v0.1.md §2):
 *   1) ISRC primary           — confidence 1.0, ISRC_EXACT
 *   2) 동일 source ID 재사용   — confidence 1.0, ISRC_EXACT (이미 같은 트랙)
 *   3) Fuzzy fallback          — title/artist/duration 가중합 (§3)
 *
 * Fuzzy weights (§3): title 0.5 · artist 0.3 · duration 0.2.
 * Threshold 0.7 미만이면 새 canonical 을 만든다 (MANUAL_NEEDED 후보로 보지 않음 — 기록은 별도 큐에서).
 *
 * 통합 메모(별도 PR):
 *   현재 NormalizationEngine.saveNormalizedAsset 은 Track 엔티티만 저장한다.
 *   다음 단계로 saveNormalizedAsset 안에서 각 track 마다 MatchingEngine.matchOrCreate 를
 *   호출해 CanonicalTrack 매핑을 채운다.
 */
@Service
class MatchingEngine(
    private val canonicalTrackRepository: CanonicalTrackRepository
) {

    data class MatchInput(
        val isrc: String?,
        val title: String,
        val artists: List<String>,
        val durationMs: Long,
        val sourcePlatform: String,
        val sourceId: String  // spotifyId 또는 appleSongId
    )

    data class MatchResult(
        val canonical: CanonicalTrack,
        val confidence: Double,
        val matchedBy: MatchStrategy,
        val isNewlyCreated: Boolean
    )

    enum class MatchStrategy { ISRC_EXACT, FUZZY, MANUAL_NEEDED }

    /**
     * 입력 트랙에 대해 canonical 을 찾거나 새로 만든다.
     */
    @Transactional
    fun matchOrCreate(input: MatchInput): MatchResult {
        // 1) ISRC primary
        if (!input.isrc.isNullOrBlank()) {
            val byIsrc = canonicalTrackRepository.findByIsrc(input.isrc)
            if (byIsrc != null) {
                mergeMetadataIfChanged(byIsrc, input)
                addSourceLinkIfMissing(byIsrc, input)
                val saved = canonicalTrackRepository.save(byIsrc)
                return MatchResult(saved, 1.0, MatchStrategy.ISRC_EXACT, isNewlyCreated = false)
            }
        }

        // 2) Source ID 재사용 — 동일 플랫폼의 같은 track id 가 이미 매핑되어 있다면 그 canonical 사용
        val bySourceId = when (input.sourcePlatform.lowercase()) {
            "spotify" -> canonicalTrackRepository.findBySpotifyTrackId(input.sourceId)
            "apple", "applemusic", "apple_music" -> canonicalTrackRepository.findByAppleSongId(input.sourceId)
            else -> null
        }
        if (bySourceId != null) {
            // ISRC 가 새로 들어왔는데 기존 canonical 에 없으면 채워준다
            if (bySourceId.isrc.isNullOrBlank() && !input.isrc.isNullOrBlank()) {
                bySourceId.isrc = input.isrc
            }
            mergeMetadataIfChanged(bySourceId, input)
            val saved = canonicalTrackRepository.save(bySourceId)
            return MatchResult(saved, 1.0, MatchStrategy.ISRC_EXACT, isNewlyCreated = false)
        }

        // 3) Fuzzy fallback
        val normalizedTitle = normalizeTitle(input.title)
        val prefix = normalizedTitle.take(min(4, normalizedTitle.length))
        val candidates = if (prefix.isNotEmpty()) {
            canonicalTrackRepository.findCandidatesForFuzzyMatch(prefix)
        } else {
            emptyList()
        }

        var best: CanonicalTrack? = null
        var bestScore = 0.0
        for (c in candidates) {
            val score = fuzzyScore(input, c)
            if (score > bestScore) {
                bestScore = score
                best = c
            }
        }

        if (best != null && bestScore >= FUZZY_THRESHOLD) {
            // §5 충돌 처리: 더 높은 confidence 보유 trace 의 메타 유지.
            // 이번 입력의 confidence(=bestScore)가 기존 matchConfidence 보다 높을 때만 메타 갱신.
            if (bestScore > best.matchConfidence) {
                best.title = normalizeTitle(input.title)
                best.artists = input.artists.joinToString(",") { normalizeArtist(it) }
                best.durationMs = input.durationMs
                best.matchConfidence = bestScore
            }
            addSourceLinkIfMissing(best, input)
            if (best.isrc.isNullOrBlank() && !input.isrc.isNullOrBlank()) {
                best.isrc = input.isrc
            }
            best.updatedAt = LocalDateTime.now()
            val saved = canonicalTrackRepository.save(best)
            return MatchResult(saved, bestScore, MatchStrategy.FUZZY, isNewlyCreated = false)
        }

        // 신규 canonical 생성
        val created = CanonicalTrack(
            isrc = input.isrc?.takeIf { it.isNotBlank() },
            spotifyTrackId = if (input.sourcePlatform.lowercase() == "spotify") input.sourceId else null,
            appleSongId = if (input.sourcePlatform.lowercase() in APPLE_KEYS) input.sourceId else null,
            title = normalizeTitle(input.title),
            artists = input.artists.joinToString(",") { normalizeArtist(it) },
            durationMs = input.durationMs,
            matchConfidence = 1.0,
            sources = input.sourcePlatform.lowercase()
        )
        val saved = canonicalTrackRepository.save(created)
        return MatchResult(saved, 1.0, MatchStrategy.ISRC_EXACT, isNewlyCreated = true)
    }

    /**
     * 입력과 후보 canonical 간 fuzzy score (0..1).
     * weights: title 0.5, artist 0.3, duration 0.2.
     */
    fun fuzzyScore(input: MatchInput, candidate: CanonicalTrack): Double {
        val titleA = normalizeTitle(input.title)
        val titleB = candidate.title  // candidate 는 이미 정규화 저장 가정. 보수적으로 재정규화.
        val titleScore = stringSimilarity(titleA, normalizeTitle(titleB))

        val artistA = input.artists.joinToString(",") { normalizeArtist(it) }
        val artistB = candidate.artists
        val artistScore = stringSimilarity(artistA, artistB)

        val durDiff = kotlin.math.abs(input.durationMs - candidate.durationMs)
        val durationScore = when {
            durDiff <= 2_000L -> 1.0
            durDiff <= 5_000L -> 0.5
            else -> 0.0
        }

        return titleScore * 0.5 + artistScore * 0.3 + durationScore * 0.2
    }

    /**
     * lowercase, "(Remastered ...)", "(feat. X)", "[Remix]" 같은 부가 표기 제거.
     */
    fun normalizeTitle(raw: String): String {
        var s = raw.lowercase().trim()
        // "(...)" 또는 "[...]" 안에 remaster / remix / feat / featuring / live 가 들어간 블록 제거
        s = s.replace(Regex("""[\(\[][^\)\]]*\b(remaster(ed)?|remix|feat\.?|featuring|live|version|edit|mono|stereo)\b[^\)\]]*[\)\]]"""), "")
        // 뒤따르는 " - Remastered 2011" 등 dash-style 표기 제거
        s = s.replace(Regex("""\s-\s.*\b(remaster(ed)?|remix|feat\.?|featuring|live|version|edit)\b.*$"""), "")
        // 다중 공백 정리
        s = s.replace(Regex("""\s+"""), " ").trim()
        return s
    }

    /**
     * lowercase, "feat." 제거, "&" → ",". 양쪽 공백 정리.
     */
    fun normalizeArtist(raw: String): String {
        var s = raw.lowercase().trim()
        s = s.replace(Regex("""\bfeat\.?\b"""), "")
        s = s.replace(Regex("""\bfeaturing\b"""), "")
        s = s.replace("&", ",")
        s = s.replace(Regex("""\s*,\s*"""), ",")
        s = s.replace(Regex("""\s+"""), " ").trim().trim(',')
        return s
    }

    // ───────────────────────── helpers ─────────────────────────

    private fun mergeMetadataIfChanged(existing: CanonicalTrack, input: MatchInput) {
        val newTitle = normalizeTitle(input.title)
        val newArtists = input.artists.joinToString(",") { normalizeArtist(it) }
        // most-recent wins for title/artists (§5)
        existing.title = newTitle
        existing.artists = newArtists
        // durationMs 는 그대로 둔다 (필요 시 평균 도입). most-recent 와 충돌 시 정확한 ISRC 매칭 기준으로 신규 입력 사용.
        existing.durationMs = input.durationMs
        existing.updatedAt = LocalDateTime.now()
    }

    private fun addSourceLinkIfMissing(existing: CanonicalTrack, input: MatchInput) {
        when (input.sourcePlatform.lowercase()) {
            "spotify" -> if (existing.spotifyTrackId.isNullOrBlank()) existing.spotifyTrackId = input.sourceId
            in APPLE_KEYS -> if (existing.appleSongId.isNullOrBlank()) existing.appleSongId = input.sourceId
        }
        val sources = existing.sources.split(",").filter { it.isNotBlank() }.toMutableSet()
        sources.add(input.sourcePlatform.lowercase())
        existing.sources = sources.joinToString(",")
        existing.updatedAt = LocalDateTime.now()
    }

    private fun stringSimilarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        val maxLen = max(a.length, b.length)
        if (maxLen == 0) return 1.0
        val dist = levenshtein(a, b)
        return 1.0 - dist.toDouble() / maxLen.toDouble()
    }

    /**
     * 표준 Levenshtein DP. O(n*m) 시간, O(min(n,m)) 메모리 (rolling row).
     */
    internal fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        // b 가 더 짧도록 swap → 메모리 최소화
        val (s, t) = if (a.length < b.length) Pair(b, a) else Pair(a, b)
        // s.length >= t.length
        val n = s.length
        val m = t.length
        var prev = IntArray(m + 1) { it }
        var curr = IntArray(m + 1)

        for (i in 1..n) {
            curr[0] = i
            val sc = s[i - 1]
            for (j in 1..m) {
                val cost = if (sc == t[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[m]
    }

    companion object {
        const val FUZZY_THRESHOLD = 0.7
        private val APPLE_KEYS = setOf("apple", "applemusic", "apple_music")
    }
}
