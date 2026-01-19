package com.assignment.chatbot.domain.feedback.dto

import com.assignment.chatbot.domain.feedback.entity.FeedbackStatus
import jakarta.validation.constraints.NotNull

data class FeedbackRequest(
    @field:NotNull(message = "대화 ID는 필수입니다.")
    val chatId: Long,
    
    @field:NotNull(message = "긍정/부정 여부는 필수입니다.")
    val isPositive: Boolean
)

data class FeedbackStatusUpdateRequest(
    @field:NotNull(message = "상태는 필수입니다.")
    val status: FeedbackStatus
)
