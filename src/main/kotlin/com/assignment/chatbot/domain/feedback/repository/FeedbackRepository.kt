package com.assignment.chatbot.domain.feedback.repository

import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.domain.feedback.entity.Feedback
import com.assignment.chatbot.domain.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface FeedbackRepository : JpaRepository<Feedback, Long> {
    
    /**
     * 특정 사용자와 대화에 대한 피드백 존재 여부 확인
     * 중복 피드백 방지용
     */
    fun existsByUserAndChat(user: User, chat: Chat): Boolean
    
    /**
     * 특정 사용자의 피드백 목록 조회 (페이지네이션, 필터링)
     */
    fun findByUser(user: User, pageable: Pageable): Page<Feedback>
    
    /**
     * 긍정/부정 필터링된 사용자 피드백 조회
     */
    fun findByUserAndIsPositive(user: User, isPositive: Boolean, pageable: Pageable): Page<Feedback>
    
    /**
     * 모든 피드백 조회 (관리자용)
     */
    override fun findAll(pageable: Pageable): Page<Feedback>
    
    /**
     * 긍정/부정 필터링된 전체 피드백 조회 (관리자용)
     */
    fun findByIsPositive(isPositive: Boolean, pageable: Pageable): Page<Feedback>
}
