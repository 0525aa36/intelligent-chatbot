package com.assignment.chatbot.global.exception

class CustomException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message)
