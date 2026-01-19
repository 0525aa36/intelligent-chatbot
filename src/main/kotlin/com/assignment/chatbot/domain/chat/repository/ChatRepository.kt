package com.assignment.chatbot.domain.chat.repository

import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.domain.chat.entity.Thread
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import jakarta.persistence.QueryHint
import java.time.LocalDateTime
import java.util.stream.Stream

@Repository
interface ChatRepository : JpaRepository<Chat, Long> {
    
    /**
     * 특정 스레드의 대화 목록 조회 (생성일 기준 오름차순)
     */
    fun findByThreadOrderByCreatedAtAsc(thread: Thread): List<Chat>
    
    /**
     * 특정 기간 내 생성된 대화 수 집계 (통계용)
     */
    @Query("SELECT COUNT(c) FROM Chat c WHERE c.createdAt >= :startTime")
    fun countByCreatedAtAfter(startTime: LocalDateTime): Long
    
    /**
     * 특정 기간 내 생성된 모든 대화 조회 (보고서용)
     * fetch join으로 user 정보도 함께 조회하여 N+1 문제 방지
     */
    @Query("""
        SELECT c FROM Chat c
        JOIN FETCH c.thread t
        JOIN FETCH t.user u
        WHERE c.createdAt BETWEEN :startTime AND :endTime
        ORDER BY c.createdAt DESC
    """)
    fun findAllByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<Chat>

    /**
     * 특정 기간 내 생성된 대화를 Stream으로 조회 (대용량 데이터 처리)
     *
     * 메모리 효율:
     * - Stream은 데이터를 한 번에 로드하지 않고 필요할 때마다 Fetch
     * - READ_ONLY Hint로 변경 감지 비활성화 → 성능 향상
     * - 사용 후 반드시 close() 호출 필요 (try-with-resources 권장)
     *
     * 주의:
     * - @Transactional(readOnly = true) 필수
     * - Stream은 단일 트랜잭션 내에서만 유효
     *
     * 사용 예시:
     * ```kotlin
     * chatRepository.streamAllByCreatedAtBetween(start, end).use { stream ->
     *     stream.forEach { chat -> /* 처리 */ }
     * }
     * ```
     */
    @QueryHints(QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "100"))
    @Query("""
        SELECT c FROM Chat c
        JOIN FETCH c.thread t
        JOIN FETCH t.user u
        WHERE c.createdAt BETWEEN :startTime AND :endTime
        ORDER BY c.createdAt DESC
    """)
    fun streamAllByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): Stream<Chat>
}
