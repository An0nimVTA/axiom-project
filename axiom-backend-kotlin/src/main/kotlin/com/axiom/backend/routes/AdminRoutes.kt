package com.axiom.backend.routes

import com.axiom.backend.db.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ServerRequest(
    val id: String, val name: String, val address: String,
    val port: Int = 25565, val modpack: String, val maxPlayers: Int = 100
)

@Serializable
data class ModpackRequest(
    val id: String, val name: String, val version: String,
    val minecraftVersion: String, val description: String = "",
    val downloadUrl: String, val size: Long = 0
)

@Serializable
data class NewsRequest(val title: String, val content: String)

fun Route.adminRoutes() {
    route("/admin") {
        // Servers
        post("/servers") {
            val req = call.receive<ServerRequest>()
            transaction {
                Servers.insertIgnore {
                    it[id] = req.id; it[name] = req.name; it[address] = req.address
                    it[port] = req.port; it[modpack] = req.modpack; it[maxPlayers] = req.maxPlayers
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        
        put("/servers/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val req = call.receive<ServerRequest>()
            transaction {
                Servers.update({ Servers.id eq id }) {
                    it[name] = req.name; it[address] = req.address
                    it[port] = req.port; it[modpack] = req.modpack; it[maxPlayers] = req.maxPlayers
                }
            }
            call.respond(mapOf("status" to "updated"))
        }
        
        delete("/servers/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            transaction { Servers.deleteWhere { Servers.id eq id } }
            call.respond(mapOf("status" to "deleted"))
        }
        
        // Modpacks
        post("/modpacks") {
            val req = call.receive<ModpackRequest>()
            transaction {
                Modpacks.insertIgnore {
                    it[id] = req.id; it[name] = req.name; it[version] = req.version
                    it[minecraftVersion] = req.minecraftVersion; it[description] = req.description
                    it[downloadUrl] = req.downloadUrl; it[size] = req.size
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        
        delete("/modpacks/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            transaction { Modpacks.deleteWhere { Modpacks.id eq id } }
            call.respond(mapOf("status" to "deleted"))
        }
        
        // News
        post("/news") {
            val req = call.receive<NewsRequest>()
            transaction {
                News.insert {
                    it[title] = req.title; it[content] = req.content
                    it[date] = java.time.LocalDate.now().toString()
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        
        delete("/news/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            transaction { News.deleteWhere { News.id eq id } }
            call.respond(mapOf("status" to "deleted"))
        }
    }
}
