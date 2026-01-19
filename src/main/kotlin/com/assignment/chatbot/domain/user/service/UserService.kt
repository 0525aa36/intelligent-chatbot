package com.assignment.chatbot.domain.user.service

import com.assignment.chatbot.domain.user.dto.LoginRequest
import com.assignment.chatbot.domain.user.dto.LoginResponse
import com.assignment.chatbot.domain.user.dto.SignUpRequest
import com.assignment.chatbot.domain.user.entity.User
import com.assignment.chatbot.domain.user.entity.UserRole
import com.assignment.chatbot.domain.user.repository.UserRepository
import com.assignment.chatbot.global.exception.CustomException
import com.assignment.chatbot.global.exception.ErrorCode
import com.assignment.chatbot.global.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun signUp(request: SignUpRequest): LoginResponse {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.email)) {
            throw CustomException(ErrorCode.DUPLICATE_EMAIL)
        }

        // 사용자 생성
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name,
            role = UserRole.MEMBER
        )

        val savedUser = userRepository.save(user)

        // JWT 토큰 생성
        val token = jwtTokenProvider.generateToken(savedUser.email)

        return LoginResponse(
            accessToken = token,
            email = savedUser.email,
            name = savedUser.name,
            role = savedUser.role.name
        )
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): LoginResponse {
        // 사용자 조회
        val user = userRepository.findByEmail(request.email)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw CustomException(ErrorCode.INVALID_PASSWORD)
        }

        // JWT 토큰 생성
        val token = jwtTokenProvider.generateToken(user.email)

        return LoginResponse(
            accessToken = token,
            email = user.email,
            name = user.name,
            role = user.role.name
        )
    }
}
