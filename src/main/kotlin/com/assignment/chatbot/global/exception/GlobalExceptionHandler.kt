package com.assignment.chatbot.global.exception

import com.assignment.chatbot.global.common.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(ex: CustomException): ResponseEntity<ApiResponse<Nothing>> {
        val status = when (ex.errorCode) {
            ErrorCode.USER_NOT_FOUND, ErrorCode.THREAD_NOT_FOUND, 
            ErrorCode.CHAT_NOT_FOUND, ErrorCode.FEEDBACK_NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorCode.DUPLICATE_EMAIL, ErrorCode.DUPLICATE_FEEDBACK -> HttpStatus.CONFLICT
            ErrorCode.INVALID_PASSWORD, ErrorCode.INVALID_INPUT -> HttpStatus.BAD_REQUEST
            ErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED
            ErrorCode.FORBIDDEN -> HttpStatus.FORBIDDEN
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        
        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(ex.errorCode.name, ex.message))
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("UNAUTHORIZED", "인증에 실패했습니다."))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("FORBIDDEN", "권한이 없습니다."))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        ex.printStackTrace()
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."))
    }
}
