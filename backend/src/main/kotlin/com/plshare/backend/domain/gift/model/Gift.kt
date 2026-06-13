package com.plshare.backend.domain.gift.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * 감성 선물 엔티티.
 *
 * 발신자(senderId)가 자신의 자산(assetId)에 메시지와 포장 스킨을 입혀
 * 고유 토큰으로 수신자에게 링크를 전달하는 도메인 객체.
 *
 * 수명 주기:
 * - CREATED  : 선물 생성됨, 아직 열리지 않음.
 * - OPENED   : 수신자가 언박싱 진입 — openedAt 기록(멱등).
 * - SAVED    : 수신자가 라이브러리에 저장 — savedByUserId 기록.
 *
 * v1 제약: 저장(SAVED) 시 자산 복제 없이 수령 기록만 남긴다.
 * 향후 자산 소유권 모델(ownerId) 도입 시 savedByUserId를 기준으로
 * 수령자 라이브러리로 Asset을 복제하는 흐름 추가 필요.
 */
@Entity
@Table(
    name = "gifts",
    indexes = [
        Index(name = "idx_gifts_token", columnList = "token", unique = true),
        Index(name = "idx_gifts_sender_id", columnList = "sender_id"),
    ]
)
class Gift(
    @Id
    val id: UUID = UUID.randomUUID(),

    /** 선물 발신자의 User UUID. */
    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,

    /** 포장 대상 자산 UUID. */
    @Column(name = "asset_id", nullable = false)
    val assetId: UUID,

    /** 발신자가 작성한 감성 메시지 (최대 500자). */
    @Column(name = "message", nullable = false, length = 500)
    val message: String,

    /**
     * 포장 스킨 키.
     * 예: "nocturne-violet", "nocturne-rose", "nocturne-teal", "nocturne-gold"
     */
    @Column(name = "wrap_skin", nullable = false, length = 64)
    val wrapSkin: String,

    /**
     * 공개 링크용 고유 토큰 (16자 hex).
     * share token 패턴 차용: UUID.randomUUID().toString().replace("-","").take(16).
     * DB UNIQUE 인덱스로 충돌 방지.
     */
    @Column(name = "token", nullable = false, unique = true, length = 32)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: GiftStatus = GiftStatus.CREATED,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /** 최초 언박싱 시각. open 호출 시 멱등하게 기록된다. */
    @Column(name = "opened_at")
    var openedAt: LocalDateTime? = null,

    /**
     * 저장한 수신자의 User UUID.
     * v1: 자산 복제 없이 수령 기록 전용.
     */
    @Column(name = "saved_by_user_id")
    var savedByUserId: UUID? = null,
)

enum class GiftStatus {
    CREATED,
    OPENED,
    SAVED,
}
