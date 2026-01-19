package com.assignment.chatbot.domain.chat.controller

import com.assignment.chatbot.domain.chat.dto.ChatRequest
import com.assignment.chatbot.domain.chat.dto.ChatResponse
import com.assignment.chatbot.domain.chat.dto.ThreadListResponse
import com.assignment.chatbot.domain.chat.service.ChatService
import com.assignment.chatbot.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val chatService: ChatService
) {

    /**
     * 대화 생성 API
     * isStreaming 파라미터에 따라 일반 응답 또는 스트리밍 응답 반환
     */
    @PostMapping
    fun createChat(@Valid @RequestBody request: ChatRequest): ResponseEntity<*> {
        return if (request.isStreaming) {
            // 스트리밍 응답 (Server-Sent Events)
            ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(chatService.createStreamingChat(request))
        } else {
            // 일반 응답
            val response = chatService.createChat(request)
            ResponseEntity.ok(ApiResponse.success(response, "대화가 생성되었습니다."))
        }
    }

    /**
     * 대화 목록 조회 API (스레드별 그룹화, 페이지네이션)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 방향 (asc/desc)
     */
    @GetMapping
    fun getChatList(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "desc") sort: String
    ): ResponseEntity<ApiResponse<ThreadListResponse>> {
        val response = chatService.getChatList(page, size, sort)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 스레드 삭제 API
     * 본인의 스레드만 삭제 가능
     */
    @DeleteMapping("/threads/{threadId}")
    fun deleteThread(@PathVariable threadId: Long): ResponseEntity<ApiResponse<Nothing>> {
        chatService.deleteThread(threadId)
        return ResponseEntity.ok(ApiResponse.success("스레드가 삭제되었습니다."))
    }
}
