package com.assignment.chatbot.domain.chat.entity

import com.assignment.chatbot.domain.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "threads")
@EntityListeners(AuditingEntityListener::class)
class Thread(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @OneToMany(mappedBy = "thread", cascade = [CascadeType.ALL], orphanRemoval = true)
    val chats: MutableList<Chat> = mutableListOf(),

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @Column(nullable = false)
    var lastActivityAt: LocalDateTime = LocalDateTime.now()
) {
    fun addChat(chat: Chat) {
        chats.add(chat)
        updateLastActivity()
    }

    /**
     * 마지막 활동 시간 갱신
     * Thread가 활성 상태임을 표시
     */
    fun updateLastActivity() {
        lastActivityAt = LocalDateTime.now()
    }

    /**
     * 스레드 만료 여부 판단 (기본 로직)
     *
     * Note: 실제 비즈니스 로직에서는 ThreadExpirationPolicy 사용 권장
     * 이 메서드는 정책 객체를 주입받을 수 없는 상황에서만 사용
     *
     * @param expirationMinutes 만료 시간(분)
     */
    fun isExpired(expirationMinutes: Long = 30): Boolean {
        return LocalDateTime.now().isAfter(lastActivityAt.plusMinutes(expirationMinutes))
    }
}
