package com.assignment.chatbot.domain.chat.strategy

import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.domain.chat.entity.Thread
import com.assignment.chatbot.domain.chat.repository.ChatRepository
import com.assignment.chatbot.domain.chat.repository.ThreadRepository
import com.assignment.chatbot.infrastructure.ai.AiClient
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * 스트리밍 응답 전략 (Server-Sent Events)
 *
 * 동작:
 * 1. AI API로부터 실시간 스트리밍 응답 받기 (Hot Stream으로 변환)
 * 2. 한 구독자는 각 청크를 클라이언트에 즉시 전송
 * 3. 다른 구독자는 스트리밍 완료 후 전체 응답을 비동기적으로 DB에 저장
 *
 * 기술적 구현:
 * - Reactive Streams (Project Reactor)
 * - Hot Stream: .publish().autoConnect(2)
 * - 비동기 DB 저장: .subscribeOn(Schedulers.boundedElastic())
 */
@Component
class StreamingResponseStrategy(
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
    ): Flux<String> {
        // AI 스트리밍 응답을 Hot Stream으로 변환하여 여러 구독자가 공유할 수 있도록 함
        val sharedResponseFlux = aiClient.generateStreamingResponse(
            question = question,
            chatHistory = chatHistory,
            context = context,
            model = model
        ).publish().autoConnect(2) // 2개의 구독자가 생기면 스트림 시작

        // 구독자 1: 스트림의 모든 청크를 수집하여 DB에 저장하는 비동기 작업
        val saveOperation = sharedResponseFlux
            .collectList()
            .map { chunks -> chunks.joinToString("") }
            .flatMap { fullAnswer ->
                Mono.fromRunnable<Void> {
                    if (fullAnswer.isNotEmpty()) {
                        saveChat(thread, question, fullAnswer)
                    }
                }.subscribeOn(Schedulers.boundedElastic())
            }
            .onErrorResume {
                // 스트림 자체에서 에러가 발생한 경우 (예: AI 서비스 다운)
                // 이 경우 fullAnswer를 수집할 수 없으므로 별도 처리는 하지 않음
                Mono.empty()
            }

        // 비동기 저장 작업을 fire-and-forget 방식으로 실행
        saveOperation.subscribe()

        // 구독자 2: 원본 스트림을 클라이언트에게 직접 반환하여 SSE 전송
        return sharedResponseFlux
    }

    private fun saveChat(thread: Thread, question: String, answer: String) {
        val chat = Chat(
            thread = thread,
            question = question,
            answer = answer
        )
        chatRepository.save(chat)
        thread.addChat(chat)
        threadRepository.save(thread)
    }

    override fun isStreaming(): Boolean = true
}
