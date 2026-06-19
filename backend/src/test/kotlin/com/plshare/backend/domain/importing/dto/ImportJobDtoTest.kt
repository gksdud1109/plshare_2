package com.plshare.backend.domain.importing.dto

import com.plshare.backend.domain.importing.model.ImportJob
import com.plshare.backend.domain.importing.model.ImportJobStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportJobDtoTest {

    @Test
    fun `RUNNING 상태는 프론트 계약의 matching으로 변환한다`() {
        val job = ImportJob(
            idempotencyKey = "import-status-contract",
            status = ImportJobStatus.RUNNING,
        )

        assertEquals("matching", ImportJobDto.from(job).status)
    }
}
