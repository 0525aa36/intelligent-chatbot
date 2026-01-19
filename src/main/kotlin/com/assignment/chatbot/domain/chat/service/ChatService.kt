package com.assignment.chatbot.domain.chat.service

import com.assignment.chatbot.domain.chat.dto.ChatRequest
import com.assignment.chatbot.domain.chat.dto.ChatResponse
import com.assignment.chatbot.domain.chat.dto.ThreadListResponse
import com.assignment.chatbot.domain.chat.dto.ThreadResponse
import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.domain.chat.entity.Thread
import com.assignment.chatbot.domain.chat.policy.ThreadExpirationPolicy
import com.assignment.chatbot.domain.chat.repository.ChatRepository
import com.assignment.chatbot.domain.chat.repository.ThreadRepository
import com.assignment.chatbot.domain.chat.strategy.NormalResponseStrategy
import com.assignment.chatbot.domain.chat.strategy.StreamingResponseStrategy
import com.assignment.chatbot.domain.user.entity.User
import com.assignment.chatbot.domain.user.entity.UserRole
import com.assignment.chatbot.domain.user.repository.UserRepository
import com.assignment.chatbot.global.exception.CustomException
import com.assignment.chatbot.global.exception.ErrorCode
import com.assignment.chatbot.global.security.AuthorizationChecker
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val threadRepository: ThreadRepository,
    private val userRepository: UserRepository,
    private val threadExpirationPolicy: ThreadExpirationPolicy,
    private val normalResponseStrategy: NormalResponseStrategy,
    private val streamingResponseStrategy: StreamingResponseStrategy,
    private val authorizationChecker: AuthorizationChecker
) {

    /**
     * 대화 생성 (일반 응답) - 전략 패턴 적용
     *
     * 동작:
     * 1. 현재 사용자 조회
     * 2. 활성 스레드 확인 또는 새 스레드 생성 (30분 만료 로직)
     * 3. 전략 패턴으로 응답 방식 선택
     * 4. AI 응답 생성 및 저장
     *
     * 확장성:
     * - isStreaming 플래그에 따라 전략 자동 선택
     * - 향후 새로운 응답 방식 추가 용이
     */
    @Transactional
    fun createChat(request: ChatRequest): ChatResponse {
        val user = getCurrentUser()

        // 스레드 자동 관리: 30분 만료 체크
        val thread = getOrCreateActiveThread(user)

        // 이전 대화 히스토리 조회 (컨텍스트 유지)
        val chatHistory = chatRepository.findByThreadOrderByCreatedAtAsc(thread)

        // 전략 선택 및 응답 생성
        val strategy = normalResponseStrategy
        val savedChat = strategy.generateAndSaveResponse(
            question = request.question,
            thread = thread,
            chatHistory = chatHistory,
            model = request.model
        ) as Chat

        return ChatResponse.from(savedChat)
    }

    /**
     * 대화 생성 (스트리밍 응답) - 전략 패턴 적용
     *
     * 동작:
     * 1. 현재 사용자 조회
     * 2. 활성 스레드 확인 또는 새 스레드 생성
     * 3. 스트리밍 전략으로 실시간 응답 생성
     * 4. 스트리밍 완료 후 DB 저장 (전략 내부 처리)
     *
     * 사용자 경험:
     * - 타이핑 효과로 응답을 실시간으로 확인
     * - 긴 응답도 기다림 없이 즉시 시작
     */
    @Transactional
    fun createStreamingChat(request: ChatRequest): Flux<String> {
        val user = getCurrentUser()
        val thread = getOrCreateActiveThread(user)
        val chatHistory = chatRepository.findByThreadOrderByCreatedAtAsc(thread)

        // 스트리밍 전략으로 응답 생성
        val strategy = streamingResponseStrategy
        return strategy.generateAndSaveResponse(
            question = request.question,
            thread = thread,
            chatHistory = chatHistory,
            model = request.model
        ) as Flux<String>
    }

    /**
     * 대화 목록 조회 (스레드별 그룹화, 페이지네이션)
     * 일반 사용자: 본인의 스레드만 조회
     * 관리자: 모든 스레드 조회
     */
    @Transactional(readOnly = true)
    fun getChatList(
        page: Int,
        size: Int,
        sortDirection: String
    ): ThreadListResponse {
        val user = getCurrentUser()
        val sort = if (sortDirection.equals("asc", ignoreCase = true)) {
            Sort.by("createdAt").ascending()
        } else {
            Sort.by("createdAt").descending()
        }
        
        val pageable = PageRequest.of(page, size, sort)
        
        val threadPage = if (user.role == UserRole.ADMIN) {
            threadRepository.findAll(pageable)
        } else {
            threadRepository.findByUser(user, pageable)
        }
        
        val threadResponses = threadPage.content.map { thread ->
            ThreadResponse.from(thread)
        }
        
        return ThreadListResponse(
            threads = threadResponses,
            totalElements = threadPage.totalElements,
            totalPages = threadPage.totalPages,
            currentPage = threadPage.number,
            pageSize = threadPage.size
        )
    }

    /**
     * 스레드 삭제 (권한 체크 강화)
     *
     * 권한:
     * - 일반 사용자: 본인의 스레드만 삭제 가능
     * - 관리자: 모든 스레드 삭제 가능
     *
     * 보안:
     * - AuthorizationChecker로 권한 검증
     * - 스레드가 존재하지 않으면 404 반환 (정보 노출 방지)
     */
    @Transactional
    fun deleteThread(threadId: Long) {
        val user = getCurrentUser()

        // 스레드 조회
        val thread = threadRepository.findById(threadId)
            .orElseThrow { CustomException(ErrorCode.THREAD_NOT_FOUND) }

        // 권한 확인: 소유자 또는 관리자만 삭제 가능
        authorizationChecker.checkOwnerOrAdmin(
            currentUser = user,
            resourceOwnerId = thread.user.id,
            resourceName = "스레드"
        )

        // 삭제
        threadRepository.delete(thread)
    }

    /**
     * 활성 스레드 조회 또는 생성 (동시성 안전)
     *
     * 로직:
     * 1. 사용자의 가장 최근 스레드 조회 (Pessimistic Lock 적용)
     * 2. ThreadExpirationPolicy로 만료 여부 판단
     * 3. 스레드가 없거나 만료된 경우 새 스레드 생성
     * 4. 그렇지 않으면 기존 스레드 반환
     *
     * 동시성 제어:
     * - Pessimistic Lock으로 동일 사용자의 동시 요청 시 스레드 중복 생성 방지
     * - 트랜잭션이 완료될 때까지 다른 요청은 대기
     *
     * 성능 고려:
     * - 일반적으로 사용자별 동시 요청은 드물어 성능 영향 미미
     * - 필요 시 Optimistic Lock 또는 비즈니스 로직 레벨 제어로 전환 가능
     */
    private fun getOrCreateActiveThread(user: User): Thread {
        // Pessimistic Lock으로 스레드 조회
        val latestThread = threadRepository.findFirstByUserOrderByLastActivityAtDesc(user)

        // 정책 객체로 만료 여부 판단
        return if (latestThread == null || threadExpirationPolicy.isExpired(latestThread.lastActivityAt)) {
            // 새 스레드 생성
            val newThread = Thread(user = user)
            threadRepository.save(newThread)
        } else {
            // 기존 스레드 반환
            latestThread
        }
    }

    /**
     * 현재 인증된 사용자 조회
     */
    private fun getCurrentUser(): User {
        val email = SecurityContextHolder.getContext().authentication.name
        return userRepository.findByEmail(email)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
    }
}
