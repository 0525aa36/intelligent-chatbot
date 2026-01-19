package com.assignment.chatbot.domain.feedback.dto

import com.assignment.chatbot.domain.feedback.entity.Feedback
import com.assignment.chatbot.domain.feedback.entity.FeedbackStatus
import java.time.LocalDateTime

data class FeedbackResponse(
    val id: Long,
    val userId: Long,
    val userName: String,
    val chatId: Long,
    val question: String,
    val answer: String,
    val isPositive: Boolean,
    val status: FeedbackStatus,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(feedback: Feedback): FeedbackResponse {
            return FeedbackResponse(
                id = feedback.id!!,
                userId = feedback.user.id!!,
                userName = feedback.user.name,
                chatId = feedback.chat.id!!,
                question = feedback.chat.question,
                answer = feedback.chat.answer,
                isPositive = feedback.isPositive,
                status = feedback.status,
                createdAt = feedback.createdAt!!
            )
        }
    }
}

data class FeedbackListResponse(
    val feedbacks: List<FeedbackResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)
