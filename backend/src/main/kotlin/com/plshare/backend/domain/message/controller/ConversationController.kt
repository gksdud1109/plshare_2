package com.plshare.backend.domain.message.controller

import com.plshare.backend.domain.message.dto.*
import com.plshare.backend.domain.message.service.ConversationService
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.security.CurrentUserId
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 1:1 쪽지 API. 신원은 모두 [CurrentUserId](세션 principal)로만 해석한다 — 전부 인증 필수.
 *
 * - GET  /api/conversations                 : 내 대화 목록(최근순 + 안읽은 수)
 * - POST /api/conversations                 : 상대 handle 로 대화 get-or-create(멱등)
 * - GET  /api/conversations/unread-count     : 전체 안 읽은 수(네비 배지)
 * - GET  /api/conversations/{id}/messages    : 스레드 조회(+읽음 처리)
 * - POST /api/conversations/{id}/messages    : 메시지 전송
 */
@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val conversationService: ConversationService,
) {

    @GetMapping
    fun list(@CurrentUserId meId: UUID): ApiResponse<List<ConversationSummaryDto>> =
        ApiResponse.ok(conversationService.listConversations(meId))

    @PostMapping
    fun start(
        @RequestBody req: StartConversationRequest,
        @CurrentUserId meId: UUID,
    ): ApiResponse<ConversationSummaryDto> =
        ApiResponse.ok(conversationService.startConversation(meId, req.recipientHandle))

    /** literal 경로라 /{id}/... 보다 우선 매칭. */
    @GetMapping("/unread-count")
    fun unreadCount(@CurrentUserId meId: UUID): ApiResponse<UnreadCountResponse> =
        ApiResponse.ok(UnreadCountResponse(conversationService.unreadTotal(meId)))

    @GetMapping("/{id}/messages")
    fun thread(
        @PathVariable id: UUID,
        @CurrentUserId meId: UUID,
    ): ApiResponse<ThreadResponse> =
        ApiResponse.ok(conversationService.getThread(id, meId))

    @PostMapping("/{id}/messages")
    fun send(
        @PathVariable id: UUID,
        @RequestBody req: SendMessageRequest,
        @CurrentUserId meId: UUID,
    ): ApiResponse<MessageDto> =
        ApiResponse.ok(conversationService.sendMessage(id, meId, req.body))
}
