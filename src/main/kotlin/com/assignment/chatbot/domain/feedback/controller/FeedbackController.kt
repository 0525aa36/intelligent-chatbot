package com.assignment.chatbot.domain.feedback.controller

import com.assignment.chatbot.domain.feedback.dto.FeedbackListResponse
import com.assignment.chatbot.domain.feedback.dto.FeedbackRequest
import com.assignment.chatbot.domain.feedback.dto.FeedbackResponse
import com.assignment.chatbot.domain.feedback.dto.FeedbackStatusUpdateRequest
import com.assignment.chatbot.domain.feedback.service.FeedbackService
import com.assignment.chatbot.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/feedbacks")
class FeedbackController(
    private val feedbackService: FeedbackService
) {

    /**
     * 피드백 생성 API
     * 
     * 권한: 인증된 모든 사용자
     * 제약: 본인의 대화만 피드백 가능 (관리자는 모든 대화 가능)
     */
    @PostMapping
    fun createFeedback(
        @Valid @RequestBody request: FeedbackRequest
    ): ResponseEntity<ApiResponse<FeedbackResponse>> {
        val response = feedbackService.createFeedback(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "피드백이 생성되었습니다."))
    }

    /**
     * 피드백 목록 조회 API
     * 
     * 권한:
     * - 일반 사용자: 본인의 피드백만 조회
     * - 관리자: 모든 피드백 조회
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 방향 (asc/desc)
     * @param isPositive 긍정/부정 필터 (nullable)
     */
    @GetMapping
    fun getFeedbackList(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "desc") sort: String,
        @RequestParam(required = false) isPositive: Boolean?
    ): ResponseEntity<ApiResponse<FeedbackListResponse>> {
        val response = feedbackService.getFeedbackList(page, size, sort, isPositive)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 피드백 상태 변경 API
     * 
     * 권한: 관리자만 가능 (ADMIN)
     * @PreAuthorize를 통한 메서드 레벨 보안
     */
    @PatchMapping("/{feedbackId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateFeedbackStatus(
        @PathVariable feedbackId: Long,
        @Valid @RequestBody request: FeedbackStatusUpdateRequest
    ): ResponseEntity<ApiResponse<FeedbackResponse>> {
        val response = feedbackService.updateFeedbackStatus(feedbackId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "피드백 상태가 변경되었습니다."))
    }
}
