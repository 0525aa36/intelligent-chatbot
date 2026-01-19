package com.assignment.chatbot.global.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * WebClient 설정
 *
 * 외부 API 호출의 안정성과 성능을 위한 설정:
 * - Connection Timeout: 연결 시도 제한 시간
 * - Read Timeout: 응답 대기 제한 시간
 * - Write Timeout: 요청 전송 제한 시간
 * - Retry: 일시적 장애 시 재시도
 * - Logging: 요청/응답 로깅
 */
@Configuration
class WebClientConfig {

    private val logger = LoggerFactory.getLogger(WebClientConfig::class.java)

    @Bean
    fun webClientBuilder(): WebClient.Builder {
        val httpClient = HttpClient.create()
            // Connection Timeout: 10초
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .doOnConnected { connection ->
                // Read Timeout: 60초 (AI 응답은 시간이 걸릴 수 있음)
                connection.addHandlerLast(ReadTimeoutHandler(60, TimeUnit.SECONDS))
                // Write Timeout: 10초
                connection.addHandlerLast(WriteTimeoutHandler(10, TimeUnit.SECONDS))
            }

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(logRequest())
            .filter(logResponse())
    }

    /**
     * 요청 로깅 필터
     *
     * 외부 API 호출 추적 및 디버깅 용도
     */
    private fun logRequest(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request ->
            logger.debug("Request: ${request.method()} ${request.url()}")
            Mono.just(request)
        }
    }

    /**
     * 응답 로깅 필터
     *
     * 에러 발생 시 상세 정보 기록
     */
    private fun logResponse(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofResponseProcessor { response ->
            if (response.statusCode().isError) {
                logger.error("Error Response: ${response.statusCode()}")
            } else {
                logger.debug("Response: ${response.statusCode()}")
            }
            Mono.just(response)
        }
    }

    /**
     * Retry 정책 (선택적 사용)
     *
     * 일시적 네트워크 장애나 서버 부하 시 재시도
     * - 최대 3회 재시도
     * - 지수 백오프 (1초 → 2초 → 4초)
     * - 5xx 에러에만 재시도 (4xx는 재시도 불필요)
     *
     * 사용 예시:
     * ```kotlin
     * webClient.get()
     *     .retrieve()
     *     .bodyToMono(String::class.java)
     *     .retryWhen(getRetrySpec())
     * ```
     */
    fun getRetrySpec(): Retry {
        return Retry.backoff(3, Duration.ofSeconds(1))
            .maxBackoff(Duration.ofSeconds(10))
            .filter { throwable ->
                // 재시도 대상: 네트워크 에러, 5xx 서버 에러
                throwable is java.net.ConnectException ||
                throwable is java.io.IOException ||
                throwable.message?.contains("5") == true
            }
            .doBeforeRetry { signal ->
                logger.warn("Retrying due to: ${signal.failure().message}, attempt: ${signal.totalRetries() + 1}")
            }
    }
}
