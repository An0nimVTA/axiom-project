package com.axiom.backend.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private val secret = System.getenv("JWT_SECRET") ?: "axiom-secret-key"
    private val algorithm = Algorithm.HMAC256(secret)
    private const val validityMs = 30L * 24 * 60 * 60 * 1000 // 30 days
    
    val verifier = JWT.require(algorithm).build()
    
    fun generateToken(userId: Int, login: String): String = JWT.create()
        .withClaim("userId", userId)
        .withClaim("login", login)
        .withExpiresAt(Date(System.currentTimeMillis() + validityMs))
        .sign(algorithm)
}
