package com.assignment.chatbot.domain.chat.strategy

import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.domain.chat.entity.Thread
import com.assignment.chatbot.domain.chat.repository.ChatRepository
import com.assignment.chatbot.domain.chat.repository.ThreadRepository
import com.assignment.chatbot.infrastructure.ai.AiClient
import org.springframework.stereotype.Component

/**
 * 일반 응답 전략 (Non-Streaming)
 *
 * 동작:
 * 1. AI API 호출하여 전체 응답 한 번에 받기
 * 2. DB에 저장
 * 3. 저장된 Chat 엔티티 반환
 *
 * 사용 시나리오:
 * - 짧은 응답 (1-2문장)
 * - 실시간 응답이 불필요한 경우
 * - 응답 전체를 한 번에 받아야 하는 경우
 */
@Component
class NormalResponseStrategy(
    private val aiClient: AiClient,
    private val chatRepository: ChatRepository,
    private val threadRepository: ThreadRepository
) : ChatResponseStrategy {

    override fun generateAndSaveResponse(
        question: String,
        thread: Thread,
        chatHistory: List<Chat>,
        context: String?,
        model: String?
    ): Chat {
        // AI 응답 생성 (동기 방식)
        val answer = aiClient.generateResponse(
            question = question,
            chatHistory = chatHistory,
            context = context,
            model = model
        )

        // 대화 저장
        val chat = Chat(
            thread = thread,
            question = question,
            answer = answer
        )
        val savedChat = chatRepository.save(chat)

        // 스레드 업데이트 (마지막 활동 시간 갱신)
        thread.addChat(savedChat)
        threadRepository.save(thread)

        return savedChat
    }

    override fun isStreaming(): Boolean = false
}
