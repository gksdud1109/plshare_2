package com.plshare.backend.domain.user.repository

import com.plshare.backend.domain.user.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByGoogleSubject(googleSubject: String): User?
    fun findByHandle(handle: String): User?
    fun findByEmail(email: String): User?
}
