package com.assignment.chatbot.global.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {
    
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray())
    }

    fun generateToken(authentication: Authentication): String {
        val userDetails = authentication.principal as UserDetails
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userDetails.username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun generateToken(email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(email)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getUserEmailFromToken(token: String): String {
        val claims = parseClaims(token)
        return claims.subject
    }

    fun validateToken(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
