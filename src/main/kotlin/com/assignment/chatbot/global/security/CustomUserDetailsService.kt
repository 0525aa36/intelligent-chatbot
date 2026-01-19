package com.assignment.chatbot.global.security

import com.assignment.chatbot.domain.user.repository.UserRepository
import com.assignment.chatbot.global.exception.CustomException
import com.assignment.chatbot.global.exception.ErrorCode
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))

        return User(
            user.email,
            user.password,
            authorities
        )
    }
}
