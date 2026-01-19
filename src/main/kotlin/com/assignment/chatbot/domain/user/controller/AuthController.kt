package com.assignment.chatbot.domain.user.controller

import com.assignment.chatbot.domain.user.dto.LoginRequest
import com.assignment.chatbot.domain.user.dto.LoginResponse
import com.assignment.chatbot.domain.user.dto.SignUpRequest
import com.assignment.chatbot.domain.user.service.UserService
import com.assignment.chatbot.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService
) {

    @PostMapping("/signup")
    fun signUp(@Valid @RequestBody request: SignUpRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        val response = userService.signUp(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "회원가입이 완료되었습니다."))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        val response = userService.login(request)
        return ResponseEntity.ok(ApiResponse.success(response, "로그인이 완료되었습니다."))
    }
}
