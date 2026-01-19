package com.assignment.chatbot.global.exception

enum class ErrorCode(val message: String) {
    // User
    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL("이미 존재하는 이메일입니다."),
    INVALID_PASSWORD("비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED("인증이 필요합니다."),
    FORBIDDEN("권한이 없습니다."),
    
    // Chat
    THREAD_NOT_FOUND("스레드를 찾을 수 없습니다."),
    CHAT_NOT_FOUND("대화를 찾을 수 없습니다."),
    
    // Feedback
    FEEDBACK_NOT_FOUND("피드백을 찾을 수 없습니다."),
    DUPLICATE_FEEDBACK("이미 피드백을 작성했습니다."),
    
    // AI
    AI_SERVICE_ERROR("AI 서비스 오류가 발생했습니다."),
    
    // Common
    INVALID_INPUT("잘못된 입력입니다."),
    INTERNAL_SERVER_ERROR("서버 오류가 발생했습니다.")
}
