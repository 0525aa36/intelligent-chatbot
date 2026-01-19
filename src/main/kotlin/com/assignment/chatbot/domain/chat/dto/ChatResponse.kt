package com.assignment.chatbot.domain.chat.dto

import com.assignment.chatbot.domain.chat.entity.Chat
import java.time.LocalDateTime

data class ChatResponse(
    val id: Long,
    val threadId: Long,
    val question: String,
    val answer: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(chat: Chat): ChatResponse {
            return ChatResponse(
                id = chat.id!!,
                threadId = chat.thread.id!!,
                question = chat.question,
                answer = chat.answer,
                createdAt = chat.createdAt!!
            )
        }
    }
}
