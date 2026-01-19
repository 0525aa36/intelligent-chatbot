package com.assignment.chatbot.domain.chat.repository

import com.assignment.chatbot.domain.chat.entity.Thread
import com.assignment.chatbot.domain.user.entity.User
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ThreadRepository : JpaRepository<Thread, Long> {

    /**
     * 특정 사용자의 가장 최근 스레드 조회 (N+1 문제 방지를 위해 fetch join)
     * 스레드 자동 생성 로직에서 사용
     */
    @Query("""
        SELECT t FROM Thread t
        LEFT JOIN FETCH t.chats
        WHERE t.user = :user
        ORDER BY t.lastActivityAt DESC
    """)
    fun findTopByUserOrderByLastActivityAtDesc(@Param("user") user: User): Thread?

    /**
     * 특정 사용자의 가장 최근 스레드 조회 (Pessimistic Lock)
     *
     * 동시성 제어:
     * - 동일 사용자의 여러 요청이 동시에 들어와도 스레드 중복 생성 방지
     * - PESSIMISTIC_WRITE 락으로 조회 시점부터 트랜잭션 종료까지 잠금 유지
     *
     * 사용 시나리오:
     * - 스레드 생성/조회 로직에서 Race Condition 방지 필요 시
     *
     * 주의:
     * - 성능 영향: 동시 요청 시 대기 발생 가능
     * - 데드락 위험: 여러 테이블에 락을 걸 때 순서 주의
     *
     * 구현:
     * - Spring Data JPA의 First 키워드로 첫 번째 결과만 반환
     * - PESSIMISTIC_WRITE 락 적용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findFirstByUserOrderByLastActivityAtDesc(user: User): Thread?
    
    /**
     * 특정 사용자의 모든 스레드 조회 (페이지네이션)
     */
    fun findByUser(user: User, pageable: Pageable): Page<Thread>
    
    /**
     * 모든 스레드 조회 (관리자용, 페이지네이션)
     */
    override fun findAll(pageable: Pageable): Page<Thread>
    
    /**
     * 스레드 ID와 소유자 확인 (권한 검증용)
     */
    fun findByIdAndUser(id: Long, user: User): Thread?
}
