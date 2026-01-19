package com.assignment.chatbot.domain.admin.controller

import com.assignment.chatbot.domain.user.entity.User
import com.assignment.chatbot.domain.user.entity.UserRole
import com.assignment.chatbot.domain.user.repository.UserRepository
import com.assignment.chatbot.global.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var memberToken: String
    private lateinit var adminToken: String

    @BeforeEach
    fun setUp() {
        // 일반 사용자 생성 및 토큰 발급
        val member = userRepository.save(
            User(
                email = "member@example.com",
                password = passwordEncoder.encode("password"),
                name = "Member User",
                role = UserRole.MEMBER
            )
        )
        memberToken = jwtTokenProvider.generateToken(member.email)

        // 관리자 생성 및 토큰 발급
        val admin = userRepository.save(
            User(
                email = "admin@example.com",
                password = passwordEncoder.encode("password"),
                name = "Admin User",
                role = UserRole.ADMIN
            )
        )
        adminToken = jwtTokenProvider.generateToken(admin.email)
    }

    @Test
    @DisplayName("인증 없이 통계 조회 시 403 Forbidden 반환")
    fun `should return 403 when accessing statistics without authentication`() {
        mockMvc.perform(get("/api/admin/statistics"))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("일반 사용자가 통계 조회 시 403 Forbidden 반환")
    fun `should return 403 when member accesses statistics`() {
        mockMvc.perform(
            get("/api/admin/statistics")
                .header("Authorization", "Bearer $memberToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("관리자가 통계 조회 시 200 OK 반환")
    fun `should return 200 when admin accesses statistics`() {
        mockMvc.perform(
            get("/api/admin/statistics")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.period").value("최근 24시간"))
            .andExpect(jsonPath("$.data.signUpCount").exists())
            .andExpect(jsonPath("$.data.loginCount").exists())
            .andExpect(jsonPath("$.data.chatCount").exists())
    }

    @Test
    @DisplayName("인증 없이 보고서 다운로드 시 403 Forbidden 반환")
    fun `should return 403 when downloading report without authentication`() {
        mockMvc.perform(get("/api/admin/reports/chat"))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("일반 사용자가 보고서 다운로드 시 403 Forbidden 반환")
    fun `should return 403 when member downloads report`() {
        mockMvc.perform(
            get("/api/admin/reports/chat")
                .header("Authorization", "Bearer $memberToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("관리자가 보고서 다운로드 시 200 OK 및 CSV 파일 반환")
    fun `should return 200 and CSV when admin downloads report`() {
        mockMvc.perform(
            get("/api/admin/reports/chat")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("text/csv;charset=UTF-8"))
    }
}
