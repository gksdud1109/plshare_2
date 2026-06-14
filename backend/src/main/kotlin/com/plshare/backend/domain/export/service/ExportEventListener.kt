package com.plshare.backend.domain.export.service

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

/** export 요청이 영속(커밋)됐음을 알리는 도메인 이벤트. */
data class ExportRequestedEvent(val jobId: UUID)

/**
 * export 실행 디스패처.
 *
 * [ExportService.requestExport] 의 트랜잭션이 **커밋된 뒤**(AFTER_COMMIT) 별도 스레드(@Async)에서
 * 실제 export 를 돌린다. 이렇게 분리하면:
 *  - 같은 빈 @Async self-invoke 로 export 가 요청 트랜잭션 안에서 동기 실행되던 버그가 사라지고
 *    (NFR-012: 요청은 3초 내 ack),
 *  - 커밋 이후 실행이므로 job 이 항상 가시적이라 커밋 가시성 재시도(Thread.sleep)도 불필요하다.
 */
@Component
class ExportEventListener(
    private val exportService: ExportService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onExportRequested(event: ExportRequestedEvent) {
        exportService.runExport(event.jobId)
    }
}
