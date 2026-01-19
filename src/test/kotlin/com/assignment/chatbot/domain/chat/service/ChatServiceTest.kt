package com.assignment.chatbot.domain.chat.service

import com.assignment.chatbot.domain.chat.dto.ChatRequest
import com.assignment.chatbot.domain.chat.entity.Thread
import com.assignment.chatbot.domain.chat.repository.ChatRepository
import com.assignment.chatbot.domain.chat.repository.ThreadRepository
import com.assignment.chatbot.domain.user.entity.User
import com.assignment.chatbot.domain.user.entity.UserRole
import com.assignment.chatbot.domain.user.repository.UserRepository
import com.assignment.chatbot.infrastructure.ai.AiClient
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
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
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatServiceTest {

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var threadRepository: ThreadRepository

    @Autowired
    private lateinit var chatRepository: ChatRepository

    @MockkBean
    private lateinit var aiClient: AiClient

    private lateinit var testUser: User

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

        // Spring Security Context 설정
        val authorities = listOf(SimpleGrantedAuthority("ROLE_MEMBER"))
        val authentication = UsernamePasswordAuthenticationToken(
            testUser.email,
            null,
            authorities
        )
        SecurityContextHolder.getContext().authentication = authentication

        // AI Client Mock 설정
        every { aiClient.generateResponse(any(), any(), any()) } returns "Mock AI Response"
    }

    @Test
    @DisplayName("첫 대화 시 새로운 스레드가 생성되어야 한다")
    fun `should create new thread for first chat`() {
        // given
        val request = ChatRequest(question = "Hello, AI!")

        // when
        val response = chatService.createChat(request)

        // then
        assertNotNull(response)
        assertEquals("Hello, AI!", response.question)
        assertEquals("Mock AI Response", response.answer)

        val threads = threadRepository.findAll()
        assertEquals(1, threads.size)
    }

    @Test
    @DisplayName("30분 이내 대화 시 기존 스레드가 유지되어야 한다")
    fun `should reuse existing thread within 30 minutes`() {
        // given
        val request1 = ChatRequest(question = "First question")
        val request2 = ChatRequest(question = "Second question")

        // when
        val response1 = chatService.createChat(request1)
        val response2 = chatService.createChat(request2)

        // then
        assertEquals(response1.threadId, response2.threadId, "두 대화는 같은 스레드에 속해야 함")

        val threads = threadRepository.findAll()
        assertEquals(1, threads.size, "스레드는 하나만 생성되어야 함")

        val chats = chatRepository.findAll()
        assertEquals(2, chats.size, "대화는 2개가 생성되어야 함")
    }

    @Test
    @DisplayName("30분 경과 후 대화 시 새로운 스레드가 생성되어야 한다")
    fun `should create new thread after 30 minutes`() {
        // given
        val request1 = ChatRequest(question = "First question")
        chatService.createChat(request1)

        // 첫 번째 스레드의 lastActivityAt을 31분 전으로 설정
        val firstThread = threadRepository.findAll().first()
        val expiredThread = Thread(
            id = firstThread.id,
            user = firstThread.user,
            chats = firstThread.chats,
            createdAt = firstThread.createdAt,
            lastActivityAt = LocalDateTime.now().minusMinutes(31)
        )
        threadRepository.save(expiredThread)

        val request2 = ChatRequest(question = "Second question")

        // when
        val response2 = chatService.createChat(request2)

        // then
        val threads = threadRepository.findAll()
        assertEquals(2, threads.size, "30분 경과 후 새로운 스레드가 생성되어야 함")

        val latestThread = threads.maxByOrNull { it.lastActivityAt }
        assertEquals(response2.threadId, latestThread?.id, "새로운 대화는 새 스레드에 속해야 함")
    }

    @Test
    @DisplayName("대화 히스토리가 AI 요청에 포함되어야 한다")
    fun `should include chat history in AI request`() {
        // given
        val request1 = ChatRequest(question = "First question")
        val request2 = ChatRequest(question = "Second question")

        // when
        chatService.createChat(request1)
        chatService.createChat(request2)

        // then
        // aiClient.generateResponse가 두 번째 호출 시 chatHistory 파라미터에
        // 첫 번째 대화를 포함하는지 검증
        io.mockk.verify(exactly = 1) {
            aiClient.generateResponse(
                question = "Second question",
                chatHistory = match { it.size == 1 && it[0].question == "First question" },
                model = null
            )
        }
    }
}
