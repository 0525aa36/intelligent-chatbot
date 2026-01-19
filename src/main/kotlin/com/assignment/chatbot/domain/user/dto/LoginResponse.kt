package com.assignment.chatbot.domain.user.dto

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val email: String,
    val name: String,
    val role: String
)
