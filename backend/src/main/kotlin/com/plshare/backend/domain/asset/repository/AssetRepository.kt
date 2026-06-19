package com.plshare.backend.domain.asset.repository

import com.plshare.backend.domain.asset.model.Asset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface AssetRepository : JpaRepository<Asset, UUID> {
    fun findByShareToken(shareToken: String): Asset?
    fun findByOwnerIdAndComposeIdempotencyKey(ownerId: UUID, composeIdempotencyKey: String): Asset?

    /**
     * tracks 를 즉시(JOIN FETCH) 로딩해 반환한다. export 비동기 실행은 요청 트랜잭션 밖
     * (별도 스레드)에서 asset.tracks 를 순회하므로, LAZY 컬렉션을 미리 초기화해
     * LazyInitializationException 을 막는다.
     */
    @Query("select a from Asset a left join fetch a.tracks where a.id = :id")
    fun findWithTracksById(@Param("id") id: UUID): Asset?
}
