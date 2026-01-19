package com.assignment.chatbot.infrastructure.ai

import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.global.config.WebClientConfig
import com.assignment.chatbot.global.exception.CustomException
import com.assignment.chatbot.global.exception.ErrorCode
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Google Gemini를 사용한 AI Client 구현체
 * WebClient를 활용하여 Gemini REST API와 통신
 *
 * 에러 처리:
 * - 4xx: 클라이언트 에러 (잘못된 요청, 인증 실패 등) → 재시도 X
 * - 5xx: 서버 에러 (일시적 장애) → 재시도 O
 * - Timeout: 네트워크 지연 → 재시도 O
 */
@Component
class OpenAiClientImpl(
    @Value("\${gemini.api-key}") private val apiKey: String,
    @Value("\${gemini.model:gemini-1.5-flash}") private val defaultModel: String,
    private val webClient: WebClient.Builder,
    private val webClientConfig: WebClientConfig
) : AiClient {

    private val logger = LoggerFactory.getLogger(OpenAiClientImpl::class.java)

    private val client: WebClient

    init {
        logger.info("Gemini API Key: {}", apiKey)
        client = webClient
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .defaultHeader("x-goog-api-key", apiKey)
            .defaultHeader("Content-Type", "application/json") // Explicitly set Content-Type
            .build()
    }

    override fun generateResponse(
        question: String,
        chatHistory: List<Chat>,
        context: String?,
        model: String?
    ): String {
        return try {
            val requestBody = buildRequestBody(question, chatHistory, context)
            val modelName = model ?: defaultModel

            val response = client.post()
                .uri("/models/{model}:generateContent", modelName)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { clientResponse ->
                    clientResponse.bodyToMono(String::class.java).flatMap { errorBody ->
                        logger.error("Gemini API 4xx Error: $errorBody")
                        Mono.error(CustomException(ErrorCode.AI_SERVICE_ERROR, "잘못된 요청입니다."))
                    }
                }
                .onStatus(HttpStatusCode::is5xxServerError) { clientResponse ->
                    clientResponse.bodyToMono(String::class.java).flatMap { errorBody ->
                        logger.error("Gemini API 5xx Error: $errorBody")
                        Mono.error(CustomException(ErrorCode.AI_SERVICE_ERROR, "AI 서비스가 일시적으로 사용 불가능합니다."))
                    }
                }
                .bodyToMono(JsonNode::class.java)
                .retryWhen(webClientConfig.getRetrySpec())
                .block() ?: throw CustomException(ErrorCode.AI_SERVICE_ERROR)

            extractContent(response)
        } catch (e: WebClientResponseException) {
            logger.error("WebClient Error: ${e.statusCode} - ${e.responseBodyAsString}", e)
            throw CustomException(ErrorCode.AI_SERVICE_ERROR, "AI 서비스 호출 실패: ${e.statusCode}")
        } catch (e: CustomException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected Error in generateResponse", e)
            throw CustomException(ErrorCode.AI_SERVICE_ERROR, "AI 응답 생성 중 오류가 발생했습니다.")
        }
    }

    override fun generateStreamingResponse(
        question: String,
        chatHistory: List<Chat>,
        context: String?,
        model: String?
    ): Flux<String> {
        return try {
            val requestBody = buildRequestBody(question, chatHistory, context)
            val modelName = model ?: defaultModel

            client.post()
                .uri("/models/{model}:streamGenerateContent?alt=sse", modelName)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { clientResponse ->
                    clientResponse.bodyToMono(String::class.java).flatMap { errorBody ->
                        logger.error("Gemini Streaming API 4xx Error: $errorBody")
                        Mono.error(CustomException(ErrorCode.AI_SERVICE_ERROR, "잘못된 요청입니다."))
                    }
                }
                .onStatus(HttpStatusCode::is5xxServerError) { clientResponse ->
                    clientResponse.bodyToMono(String::class.java).flatMap { errorBody ->
                        logger.error("Gemini Streaming API 5xx Error: $errorBody")
                        Mono.error(CustomException(ErrorCode.AI_SERVICE_ERROR, "AI 서비스가 일시적으로 사용 불가능합니다."))
                    }
                }
                .bodyToFlux(String::class.java) // This returns a Flux<String>
                .log("Gemini Streaming WebClient") // This is where it should be
                .filter { it.startsWith("data: ") }
                .map { it.removePrefix("data: ") }
                .filter { it.isNotBlank() }
                .flatMap { parseStreamChunk(it) }
                .retryWhen(webClientConfig.getRetrySpec())
                .onErrorResume { error ->
                    logger.error("Streaming Error", error)
                    Flux.error(CustomException(ErrorCode.AI_SERVICE_ERROR, "스트리밍 응답 생성 중 오류가 발생했습니다."))
                }
        } catch (e: Exception) {
            logger.error("Unexpected Error in generateStreamingResponse", e)
            Flux.error(CustomException(ErrorCode.AI_SERVICE_ERROR))
        }
    }

    /**
     * Gemini API 요청 바디 생성
     * 대화 히스토리와 RAG 컨텍스트를 Gemini의 contents 형식으로 변환
     *
     * @param question 사용자 질문
     * @param chatHistory 이전 대화 히스토리
     * @param context RAG 컨텍스트 (Vector DB 검색 결과, 문서 내용 등)
     */
    private fun buildRequestBody(
        question: String,
        chatHistory: List<Chat>,
        context: String?
    ): Map<String, Any> {
        val contents = mutableListOf<Map<String, Any>>()

        // RAG 컨텍스트가 있는 경우 시스템 메시지로 먼저 추가
        if (!context.isNullOrBlank()) {
            contents.add(mapOf(
                "role" to "user",
                "parts" to listOf(mapOf("text" to buildRagPrompt(context)))
            ))
            contents.add(mapOf(
                "role" to "model",
                "parts" to listOf(mapOf("text" to "제공된 컨텍스트를 이해했습니다. 이를 바탕으로 답변하겠습니다."))
            ))
        }

        // 이전 대화 히스토리 추가
        chatHistory.forEach { chat ->
            contents.add(mapOf(
                "role" to "user",
                "parts" to listOf(mapOf("text" to chat.question))
            ))
            contents.add(mapOf(
                "role" to "model",
                "parts" to listOf(mapOf("text" to chat.answer))
            ))
        }

        // 현재 질문 추가
        contents.add(mapOf(
            "role" to "user",
            "parts" to listOf(mapOf("text" to question))
        ))

        return mapOf("contents" to contents)
    }

    /**
     * RAG 프롬프트 생성
     *
     * 컨텍스트를 AI 모델이 이해하기 쉬운 형태로 포맷팅
     */
    private fun buildRagPrompt(context: String): String {
        return """
            다음은 관련 문서에서 검색된 정보입니다. 이 정보를 참고하여 사용자의 질문에 답변해주세요.

            [검색된 컨텍스트]
            $context

            위 정보를 바탕으로 정확하고 구체적인 답변을 제공해주세요.
            만약 제공된 컨텍스트에 관련 정보가 없다면, 일반적인 지식으로 답변해주세요.
        """.trimIndent()
    }

    /**
     * Gemini API 응답에서 텍스트 추출
     */
    private fun extractContent(response: JsonNode): String {
        return response.path("candidates")
            .firstOrNull()
            ?.path("content")
            ?.path("parts")
            ?.firstOrNull()
            ?.path("text")
            ?.asText()
            ?: throw CustomException(ErrorCode.AI_SERVICE_ERROR)
    }

    /**
     * 스트리밍 응답 청크 파싱
     */
    private fun parseStreamChunk(chunk: String): Mono<String> {
        return try {
            val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
            val node = objectMapper.readTree(chunk)
            val text = node.path("candidates")
                .firstOrNull()
                ?.path("content")
                ?.path("parts")
                ?.firstOrNull()
                ?.path("text")
                ?.asText() ?: ""
            Mono.just(text)
        } catch (e: Exception) {
            Mono.empty()
        }
    }
}
