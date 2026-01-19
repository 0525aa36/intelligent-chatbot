package com.assignment.chatbot.infrastructure.ai

import com.assignment.chatbot.domain.chat.entity.Chat
import reactor.core.publisher.Flux

/**
 * AI 응답 클라이언트 인터페이스
 * OpenAI 외 다른 Provider로 교체 가능하도록 추상화
 *
 * 확장성:
 * - RAG(Retrieval-Augmented Generation) 지원을 위한 context 파라미터 제공
 * - Vector DB, Knowledge Base 등과 연동하여 컨텍스트 전달 가능
 */
interface AiClient {

    /**
     * 일반 응답 생성
     *
     * @param question 사용자 질문
     * @param chatHistory 이전 대화 히스토리 (대화 컨텍스트 유지)
     * @param context RAG 컨텍스트 (Vector DB 검색 결과, 문서 내용 등)
     * @param model 사용할 모델 (nullable, 기본값 사용 가능)
     * @return AI 응답 문자열
     *
     * RAG 사용 예시:
     * ```kotlin
     * val ragContext = vectorDb.search(question, topK = 5)
     * aiClient.generateResponse(
     *     question = question,
     *     chatHistory = chatHistory,
     *     context = ragContext
     * )
     * ```
     */
    fun generateResponse(
        question: String,
        chatHistory: List<Chat> = emptyList(),
        context: String? = null,
        model: String? = null
    ): String

    /**
     * 스트리밍 응답 생성
     *
     * @param question 사용자 질문
     * @param chatHistory 이전 대화 히스토리
     * @param context RAG 컨텍스트 (Vector DB 검색 결과, 문서 내용 등)
     * @param model 사용할 모델 (nullable, 기본값 사용 가능)
     * @return AI 응답 스트림 (Server-Sent Events)
     */
    fun generateStreamingResponse(
        question: String,
        chatHistory: List<Chat> = emptyList(),
        context: String? = null,
        model: String? = null
    ): Flux<String>
}
