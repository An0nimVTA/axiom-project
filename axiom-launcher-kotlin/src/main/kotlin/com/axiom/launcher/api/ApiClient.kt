package com.axiom.launcher.api

import com.axiom.launcher.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

class ApiClient(private val baseUrl: String = "http://localhost:5000") {
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }
    
    private var authToken: String? = null
    
    fun setToken(token: String?) { authToken = token }
    
    suspend fun login(login: String, password: String): Result<AuthResponse> = runCatching {
        client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("login" to login, "password" to password))
        }.body()
    }
    
    suspend fun register(login: String, password: String): Result<AuthResponse> = runCatching {
        client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("login" to login, "password" to password))
        }.body()
    }
    
    suspend fun getServers(): Result<List<Server>> = runCatching {
        val servers: List<Server> = client.get("$baseUrl/servers") {
            authToken?.let { header("Authorization", "Bearer $it") }
        }.body()
        
        // Ping each server to get real status
        servers.map { server ->
            val status = pingServer(server.address, server.port)
            server.copy(
                online = status.online,
                players = status.players,
                maxPlayers = status.maxPlayers
            )
        }
    }
    
    suspend fun getModpacks(): Result<List<Modpack>> = runCatching {
        client.get("$baseUrl/modpacks") {
            authToken?.let { header("Authorization", "Bearer $it") }
        }.body()
    }
    
    suspend fun getModpack(id: String): Result<Modpack?> = runCatching {
        val list: List<Modpack> = client.get("$baseUrl/modpacks") {
            authToken?.let { header("Authorization", "Bearer $it") }
        }.body()
        list.find { it.id == id }
    }
    
    suspend fun getNews(): Result<List<NewsItem>> = runCatching {
        client.get("$baseUrl/news").body()
    }
    
    fun close() = client.close()
    
    // Minecraft server ping (SLP protocol)
    private fun pingServer(host: String, port: Int): ServerStatus {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3000)
                socket.soTimeout = 3000
                
                val out = DataOutputStream(socket.getOutputStream())
                val inp = DataInputStream(socket.getInputStream())
                
                // Handshake packet
                val handshake = buildPacket {
                    writeVarInt(0x00) // Packet ID
                    writeVarInt(763)  // Protocol version (1.20.1)
                    writeString(host)
                    writeShort(port)
                    writeVarInt(1)    // Next state: status
                }
                out.writeVarInt(handshake.size)
                out.write(handshake)
                
                // Status request
                out.writeVarInt(1)
                out.writeByte(0x00)
                out.flush()
                
                // Read response
                val length = inp.readVarInt()
                val packetId = inp.readVarInt()
                val jsonLength = inp.readVarInt()
                val jsonBytes = ByteArray(jsonLength)
                inp.readFully(jsonBytes)
                val json = String(jsonBytes)
                
                // Parse JSON response
                parseServerStatus(json)
            }
        } catch (e: Exception) {
            ServerStatus(false, 0, 0)
        }
    }
    
    private fun parseServerStatus(json: String): ServerStatus {
        // Simple regex parsing
        val onlineRegex = """"online"\s*:\s*(\d+)""".toRegex()
        val maxRegex = """"max"\s*:\s*(\d+)""".toRegex()
        
        val online = onlineRegex.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val max = maxRegex.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 20
        
        return ServerStatus(true, online, max)
    }
    
    private fun buildPacket(block: PacketBuilder.() -> Unit): ByteArray {
        return PacketBuilder().apply(block).toByteArray()
    }
    
    private class PacketBuilder {
        private val data = mutableListOf<Byte>()
        
        fun writeVarInt(value: Int) {
            var v = value
            while (true) {
                if ((v and 0x7F.inv()) == 0) {
                    data.add(v.toByte())
                    return
                }
                data.add(((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
        }
        
        fun writeString(s: String) {
            val bytes = s.toByteArray(Charsets.UTF_8)
            writeVarInt(bytes.size)
            data.addAll(bytes.toList())
        }
        
        fun writeShort(v: Int) {
            data.add((v shr 8).toByte())
            data.add(v.toByte())
        }
        
        fun toByteArray() = data.toByteArray()
    }
    
    private fun DataOutputStream.writeVarInt(value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                writeByte(v)
                return
            }
            writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }
    
    private fun DataInputStream.readVarInt(): Int {
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = readByte().toInt()
            result = result or ((b and 0x7F) shl shift)
            shift += 7
        } while ((b and 0x80) != 0)
        return result
    }
    
    data class ServerStatus(val online: Boolean, val players: Int, val maxPlayers: Int)
}
