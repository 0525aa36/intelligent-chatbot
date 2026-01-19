package com.assignment.chatbot.domain.admin.dto

import java.time.LocalDateTime

data class StatisticsResponse(
    val period: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val signUpCount: Long,
    val loginCount: Long,
    val chatCount: Long
)
