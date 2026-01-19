package com.assignment.chatbot.domain.chat.strategy

import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.domain.chat.entity.Thread
import reactor.core.publisher.Flux

/**
 * AI 응답 전략 인터페이스
 *
 * 전략 패턴(Strategy Pattern) 적용:
 * - 일반 응답과 스트리밍 응답의 처리 로직을 분리
 * - isStreaming 플래그에 따른 분기 제거
 * - 각 전략의 책임 명확화
 *
 * 확장 가능성:
 * - 향후 새로운 응답 방식 추가 용이 (배치 응답, 캐시 응답 등)
 * - 테스트 용이성 향상
 */
interface ChatResponseStrategy {

    /**
     * AI 응답 생성 및 저장
     *
     * @param question 사용자 질문
     * @param thread 현재 스레드
     * @param chatHistory 이전 대화 히스토리
     * @param context RAG 컨텍스트 (옵션)
     * @param model AI 모델 (옵션)
     * @return 저장된 Chat 엔티티
     */
    fun generateAndSaveResponse(
        question: String,
        thread: Thread,
        chatHistory: List<Chat>,
        context: String? = null,
        model: String? = null
    ): Any // Chat 또는 Flux<String> 반환

    /**
     * 전략이 스트리밍 방식인지 여부
     */
    fun isStreaming(): Boolean
}
