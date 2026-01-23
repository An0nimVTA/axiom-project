package com.axiom.backend.routes

import com.axiom.backend.db.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

@Serializable
data class AuthRequest(val login: String, val password: String)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: UserDto? = null
)

@Serializable
data class UserDto(val id: Int, val login: String)

fun Route.authRoutes() {
    route("/auth") {
        post("/register") {
            val req = call.receive<AuthRequest>()
            
            if (req.login.length < 3 || req.login.length > 20) {
                call.respond(HttpStatusCode.BadRequest, 
                    AuthResponse(false, "Логин должен быть 3-20 символов"))
                return@post
            }
            
            if (req.password.length < 8) {
                call.respond(HttpStatusCode.BadRequest,
                    AuthResponse(false, "Пароль минимум 8 символов"))
                return@post
            }
            
            val hash = BCrypt.hashpw(req.password, BCrypt.gensalt())
            
            val result = transaction {
                val exists = Users.select { Users.login eq req.login }.count() > 0
                if (exists) return@transaction null
                
                Users.insert {
                    it[login] = req.login
                    it[passwordHash] = hash
                }[Users.id]
            }
            
            if (result == null) {
                call.respond(HttpStatusCode.Conflict,
                    AuthResponse(false, "Пользователь уже существует"))
                return@post
            }
            
            val token = JwtConfig.generateToken(result, req.login)
            call.respond(HttpStatusCode.Created,
                AuthResponse(true, "Регистрация успешна", token, UserDto(result, req.login)))
        }
        
        post("/login") {
            val req = call.receive<AuthRequest>()
            
            val user = transaction {
                Users.select { Users.login eq req.login }.singleOrNull()
            }
            
            if (user == null || !BCrypt.checkpw(req.password, user[Users.passwordHash])) {
                call.respond(HttpStatusCode.Unauthorized,
                    AuthResponse(false, "Неверный логин или пароль"))
                return@post
            }
            
            val token = JwtConfig.generateToken(user[Users.id], user[Users.login])
            call.respond(AuthResponse(true, "Успешный вход", token, 
                UserDto(user[Users.id], user[Users.login])))
        }
    }
}
