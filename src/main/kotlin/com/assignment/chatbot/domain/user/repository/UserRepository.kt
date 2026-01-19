package com.assignment.chatbot.domain.user.repository

import com.assignment.chatbot.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    
    /**
     * 특정 기간 내 가입한 사용자 수 집계 (통계용)
     */
    fun countByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): Long
}
