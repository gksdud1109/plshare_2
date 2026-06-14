package com.plshare.backend.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
@EnableAsync
class AsyncConfig {
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 8
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("plshare-async-")
        // 큐(50)가 가득 차면 기본 AbortPolicy 는 작업을 던져 버려 import/export 가 무성 유실된다.
        // CallerRuns 로 호출 스레드가 대신 실행 → 작업 손실 대신 백프레셔. (전용 큐 도입 트리거는 별도 기록)
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.initialize()
        return executor
    }
}
