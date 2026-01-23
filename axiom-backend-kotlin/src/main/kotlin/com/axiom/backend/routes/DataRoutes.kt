package com.axiom.backend.routes

import com.axiom.backend.db.Servers
import com.axiom.backend.db.Modpacks
import com.axiom.backend.db.News
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ServerDto(
    val id: String, val name: String, val address: String, val port: Int,
    val modpack: String, val online: Boolean, val players: Int, val maxPlayers: Int
)

@Serializable
data class ModpackDto(
    val id: String, val name: String, val version: String, 
    val minecraftVersion: String, val description: String, 
    val downloadUrl: String, val size: Long
)

@Serializable
data class NewsDto(val id: Int, val title: String, val content: String, val date: String)

fun Route.serverRoutes() {
    authenticate("auth-jwt", optional = true) {
        get("/servers") {
            val servers = transaction {
                Servers.selectAll().map {
                    ServerDto(it[Servers.id], it[Servers.name], it[Servers.address],
                        it[Servers.port], it[Servers.modpack], it[Servers.online],
                        it[Servers.players], it[Servers.maxPlayers])
                }
            }
            call.respond(servers)
        }
    }
}

fun Route.modpackRoutes() {
    authenticate("auth-jwt", optional = true) {
        get("/modpacks") {
            val modpacks = transaction {
                Modpacks.selectAll().map {
                    ModpackDto(it[Modpacks.id], it[Modpacks.name], it[Modpacks.version],
                        it[Modpacks.minecraftVersion], it[Modpacks.description],
                        it[Modpacks.downloadUrl], it[Modpacks.size])
                }
            }
            call.respond(modpacks)
        }
        
        get("/modpacks/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
            val modpack = transaction {
                Modpacks.select { Modpacks.id eq id }.singleOrNull()?.let {
                    ModpackDto(it[Modpacks.id], it[Modpacks.name], it[Modpacks.version],
                        it[Modpacks.minecraftVersion], it[Modpacks.description],
                        it[Modpacks.downloadUrl], it[Modpacks.size])
                }
            }
            if (modpack != null) call.respond(modpack)
            else call.respond(HttpStatusCode.NotFound, "Modpack not found")
        }
    }
}

fun Route.newsRoutes() {
    get("/news") {
        val news = transaction {
            News.selectAll().map {
                NewsDto(it[News.id], it[News.title], it[News.content], it[News.date])
            }
        }
        call.respond(news)
    }
}
