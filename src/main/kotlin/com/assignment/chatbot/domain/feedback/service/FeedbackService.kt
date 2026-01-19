package com.assignment.chatbot.domain.feedback.service

import com.assignment.chatbot.domain.chat.repository.ChatRepository
import com.assignment.chatbot.domain.feedback.dto.FeedbackListResponse
import com.assignment.chatbot.domain.feedback.dto.FeedbackRequest
import com.assignment.chatbot.domain.feedback.dto.FeedbackResponse
import com.assignment.chatbot.domain.feedback.dto.FeedbackStatusUpdateRequest
import com.assignment.chatbot.domain.feedback.entity.Feedback
import com.assignment.chatbot.domain.feedback.repository.FeedbackRepository
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

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authorizationChecker: AuthorizationChecker
) {

    /**
     * 피드백 생성 (권한 체크 강화)
     *
     * 비즈니스 제약:
     * 1. 사용자는 본인이 생성한 대화에만 피드백 가능 (관리자는 모든 대화 가능)
     * 2. 하나의 대화에 사용자당 하나의 피드백만 가능
     *
     * 보안:
     * - AuthorizationChecker로 권한 검증
     * - 중복 피드백 방지 (DB 제약 조건 + 비즈니스 로직)
     */
    @Transactional
    fun createFeedback(request: FeedbackRequest): FeedbackResponse {
        val user = getCurrentUser()

        // 대화 존재 여부 확인
        val chat = chatRepository.findById(request.chatId)
            .orElseThrow { CustomException(ErrorCode.CHAT_NOT_FOUND) }

        // 권한 확인: 본인의 대화 또는 관리자만 피드백 가능
        authorizationChecker.checkOwnerOrAdmin(
            currentUser = user,
            resourceOwnerId = chat.thread.user.id,
            resourceName = "대화"
        )

        // 중복 피드백 확인
        if (feedbackRepository.existsByUserAndChat(user, chat)) {
            throw CustomException(
                ErrorCode.DUPLICATE_FEEDBACK,
                "이미 해당 대화에 피드백을 작성했습니다."
            )
        }

        // 피드백 생성
        val feedback = Feedback(
            user = user,
            chat = chat,
            isPositive = request.isPositive
        )

        val savedFeedback = feedbackRepository.save(feedback)
        return FeedbackResponse.from(savedFeedback)
    }

    /**
     * 피드백 목록 조회
     * 
     * 권한:
     * - 일반 사용자: 본인이 작성한 피드백만 조회
     * - 관리자: 모든 피드백 조회
     * 
     * 필터링: isPositive (nullable)
     */
    @Transactional(readOnly = true)
    fun getFeedbackList(
        page: Int,
        size: Int,
        sortDirection: String,
        isPositive: Boolean?
    ): FeedbackListResponse {
        val user = getCurrentUser()
        val sort = if (sortDirection.equals("asc", ignoreCase = true)) {
            Sort.by("createdAt").ascending()
        } else {
            Sort.by("createdAt").descending()
        }
        
        val pageable = PageRequest.of(page, size, sort)
        
        // 권한 분리 및 필터링
        val feedbackPage = when {
            user.role == UserRole.ADMIN && isPositive != null -> {
                feedbackRepository.findByIsPositive(isPositive, pageable)
            }
            user.role == UserRole.ADMIN && isPositive == null -> {
                feedbackRepository.findAll(pageable)
            }
            user.role == UserRole.MEMBER && isPositive != null -> {
                feedbackRepository.findByUserAndIsPositive(user, isPositive, pageable)
            }
            else -> {
                feedbackRepository.findByUser(user, pageable)
            }
        }
        
        val feedbackResponses = feedbackPage.content.map { FeedbackResponse.from(it) }
        
        return FeedbackListResponse(
            feedbacks = feedbackResponses,
            totalElements = feedbackPage.totalElements,
            totalPages = feedbackPage.totalPages,
            currentPage = feedbackPage.number,
            pageSize = feedbackPage.size
        )
    }

    /**
     * 피드백 상태 변경
     * 
     * 권한: 관리자만 가능
     * 이 메서드는 Controller에서 @PreAuthorize로 권한 체크됨
     */
    @Transactional
    fun updateFeedbackStatus(
        feedbackId: Long,
        request: FeedbackStatusUpdateRequest
    ): FeedbackResponse {
        val feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow { CustomException(ErrorCode.FEEDBACK_NOT_FOUND) }
        
        feedback.updateStatus(request.status)
        val updatedFeedback = feedbackRepository.save(feedback)
        
        return FeedbackResponse.from(updatedFeedback)
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
