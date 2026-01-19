package com.assignment.chatbot.domain.chat.dto

import jakarta.validation.constraints.NotBlank

data class ChatRequest(
    @field:NotBlank(message = "질문은 필수입니다.")
    val question: String,
    
    val isStreaming: Boolean = false,
    
    val model: String? = null
)
