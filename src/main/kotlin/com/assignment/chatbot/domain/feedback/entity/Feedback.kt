package com.assignment.chatbot.domain.feedback.entity

import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.domain.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "feedbacks",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "chat_id"])]
)
@EntityListeners(AuditingEntityListener::class)
class Feedback(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    val chat: Chat,

    @Column(nullable = false)
    val isPositive: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FeedbackStatus = FeedbackStatus.PENDING,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
) {
    fun updateStatus(newStatus: FeedbackStatus) {
        this.status = newStatus
    }
}
