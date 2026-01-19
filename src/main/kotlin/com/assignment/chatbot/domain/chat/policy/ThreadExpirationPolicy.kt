package com.assignment.chatbot.domain.chat.policy

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

/**
 * 스레드 만료 정책
 *
 * 책임:
 * - 스레드의 만료 여부 판단
 * - 만료 시간 계산
 *
 * 확장성:
 * - 향후 만료 시간을 사용자 등급별로 다르게 설정 가능
 * - 특정 시간대에는 만료 시간을 늘리는 등의 비즈니스 로직 추가 가능
 */
@Component
class ThreadExpirationPolicy(
    @Value("\${thread.expiration.minutes:30}")
    private val expirationMinutes: Long
) {

    /**
     * 스레드가 만료되었는지 판단
     *
     * @param lastActivityAt 마지막 활동 시간
     * @return 만료 여부 (true: 만료됨, false: 활성 상태)
     */
    fun isExpired(lastActivityAt: LocalDateTime): Boolean {
        val now = LocalDateTime.now()
        val expirationTime = lastActivityAt.plusMinutes(expirationMinutes)
        return now.isAfter(expirationTime)
    }

    /**
     * 스레드의 남은 유효 시간 계산
     *
     * @param lastActivityAt 마지막 활동 시간
     * @return 남은 시간 (Duration), 만료된 경우 Duration.ZERO
     */
    fun getRemainingTime(lastActivityAt: LocalDateTime): Duration {
        val now = LocalDateTime.now()
        val expirationTime = lastActivityAt.plusMinutes(expirationMinutes)

        return if (now.isAfter(expirationTime)) {
            Duration.ZERO
        } else {
            Duration.between(now, expirationTime)
        }
    }

    /**
     * 만료 시간(분) 조회
     */
    fun getExpirationMinutes(): Long = expirationMinutes
}
