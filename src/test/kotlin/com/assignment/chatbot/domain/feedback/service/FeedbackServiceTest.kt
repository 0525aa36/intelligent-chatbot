package com.assignment.chatbot.domain.feedback.service

import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.domain.chat.entity.Thread
import com.assignment.chatbot.domain.chat.repository.ChatRepository
import com.assignment.chatbot.domain.chat.repository.ThreadRepository
import com.assignment.chatbot.domain.feedback.dto.FeedbackRequest
import com.assignment.chatbot.domain.feedback.repository.FeedbackRepository
import com.assignment.chatbot.domain.user.entity.User
import com.assignment.chatbot.domain.user.entity.UserRole
import com.assignment.chatbot.domain.user.repository.UserRepository
import com.assignment.chatbot.global.exception.CustomException
import com.assignment.chatbot.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FeedbackServiceTest {

    @Autowired
    private lateinit var feedbackService: FeedbackService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var threadRepository: ThreadRepository

    @Autowired
    private lateinit var chatRepository: ChatRepository

    @Autowired
    private lateinit var feedbackRepository: FeedbackRepository

    private lateinit var testUser: User
    private lateinit var otherUser: User
    private lateinit var adminUser: User
    private lateinit var testChat: Chat

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = userRepository.save(
            User(
                email = "test@example.com",
                password = "password",
                name = "Test User",
                role = UserRole.MEMBER
            )
        )

        otherUser = userRepository.save(
            User(
                email = "other@example.com",
                password = "password",
                name = "Other User",
                role = UserRole.MEMBER
            )
        )

        adminUser = userRepository.save(
            User(
                email = "admin@example.com",
                password = "password",
                name = "Admin User",
                role = UserRole.ADMIN
            )
        )

        // 테스트 스레드 및 대화 생성
        val thread = threadRepository.save(Thread(user = testUser))
        testChat = chatRepository.save(
            Chat(
                thread = thread,
                question = "Test question",
                answer = "Test answer"
            )
        )

        // Spring Security Context 설정 (기본: testUser)
        setSecurityContext(testUser)
    }

    @Test
    @DisplayName("본인의 대화에 피드백을 생성할 수 있다")
    fun `should create feedback for own chat`() {
        // given
        val request = FeedbackRequest(chatId = testChat.id!!, isPositive = true)

        // when
        val response = feedbackService.createFeedback(request)

        // then
        assertNotNull(response)
        assertEquals(testChat.id, response.chatId)
        assertEquals(true, response.isPositive)
    }

    @Test
    @DisplayName("한 대화에 중복 피드백을 생성할 수 없다")
    fun `should not create duplicate feedback for same chat`() {
        // given
        val request = FeedbackRequest(chatId = testChat.id!!, isPositive = true)
        feedbackService.createFeedback(request)

        // when & then
        val exception = assertThrows(CustomException::class.java) {
            feedbackService.createFeedback(request)
        }
        assertEquals(ErrorCode.DUPLICATE_FEEDBACK, exception.errorCode)
    }

    @Test
    @DisplayName("일반 사용자는 다른 사용자의 대화에 피드백을 생성할 수 없다")
    fun `should not create feedback for other user's chat`() {
        // given
        setSecurityContext(otherUser)
        val request = FeedbackRequest(chatId = testChat.id!!, isPositive = true)

        // when & then
        val exception = assertThrows(CustomException::class.java) {
            feedbackService.createFeedback(request)
        }
        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    @DisplayName("관리자는 모든 대화에 피드백을 생성할 수 있다")
    fun `admin should create feedback for any chat`() {
        // given
        setSecurityContext(adminUser)
        val request = FeedbackRequest(chatId = testChat.id!!, isPositive = true)

        // when
        val response = feedbackService.createFeedback(request)

        // then
        assertNotNull(response)
        assertEquals(testChat.id, response.chatId)
        assertEquals(adminUser.id, response.userId)
    }

    @Test
    @DisplayName("서로 다른 사용자는 같은 대화에 각각 피드백을 생성할 수 있다")
    fun `different users can create feedback for same chat`() {
        // given
        val request = FeedbackRequest(chatId = testChat.id!!, isPositive = true)
        
        // 첫 번째 사용자 피드백
        setSecurityContext(testUser)
        val response1 = feedbackService.createFeedback(request)

        // 관리자 피드백
        setSecurityContext(adminUser)
        val response2 = feedbackService.createFeedback(request)

        // then
        assertNotEquals(response1.id, response2.id)
        assertEquals(testChat.id, response1.chatId)
        assertEquals(testChat.id, response2.chatId)

        val feedbacks = feedbackRepository.findAll()
        assertEquals(2, feedbacks.size)
    }

    private fun setSecurityContext(user: User) {
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        val authentication = UsernamePasswordAuthenticationToken(
            user.email,
            null,
            authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }
}
