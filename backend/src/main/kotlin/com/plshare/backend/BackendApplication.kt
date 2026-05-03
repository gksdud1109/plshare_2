package com.plshare.backend

import com.plshare.backend.infrastructure.apple.AppleMusicConfig
import com.plshare.backend.infrastructure.storage.StorageConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppleMusicConfig::class, StorageConfig::class)
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
