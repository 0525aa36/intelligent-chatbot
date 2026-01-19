package com.assignment.chatbot.domain.chat.dto

import com.assignment.chatbot.domain.chat.entity.Thread
import java.time.LocalDateTime

data class ThreadResponse(
    val threadId: Long,
    val userId: Long,
    val createdAt: LocalDateTime,
    val lastActivityAt: LocalDateTime,
    val chats: List<ChatResponse>
) {
    companion object {
        fun from(thread: Thread): ThreadResponse {
            return ThreadResponse(
                threadId = thread.id!!,
                userId = thread.user.id!!,
                createdAt = thread.createdAt!!,
                lastActivityAt = thread.lastActivityAt,
                chats = thread.chats.map { ChatResponse.from(it) }
            )
        }
    }
}

data class ThreadListResponse(
    val threads: List<ThreadResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)
