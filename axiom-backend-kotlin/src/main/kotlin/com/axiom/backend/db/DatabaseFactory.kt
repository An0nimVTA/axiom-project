package com.axiom.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/axiom_launcher"
            driverClassName = "org.postgresql.Driver"
            username = System.getenv("DB_USER") ?: "axiom_user"
            password = System.getenv("DB_PASSWORD") ?: "axiom_password"
            maximumPoolSize = 10
        }
        
        Database.connect(HikariDataSource(config))
        
        transaction {
            SchemaUtils.create(Users, Servers, Modpacks, News)
            seedData()
        }
    }
    
    private fun seedData() {
        // Add default server if none exist
        if (Servers.selectAll().empty()) {
            Servers.insert {
                it[id] = "main"
                it[name] = "AXIOM Main"
                it[address] = "localhost"
                it[port] = 25565
                it[modpack] = "modern_01"
                it[online] = true
                it[maxPlayers] = 100
            }
        }
        
        // Add default modpack if none exist
        if (Modpacks.selectAll().empty()) {
            Modpacks.insert {
                it[id] = "modern_01"
                it[name] = "AXIOM Modern Warfare"
                it[version] = "1.0.0"
                it[minecraftVersion] = "1.20.1"
                it[description] = "Модпак с оружием, техникой и геополитикой"
                it[downloadUrl] = "/files/modpack_modern_01.zip"
                it[size] = 268423227
            }
        }
        
        // Add welcome news if none exist
        if (News.selectAll().empty()) {
            News.insert {
                it[title] = "Добро пожаловать в AXIOM!"
                it[content] = "Геополитический сервер Minecraft с модами на оружие, технику и экономику."
                it[date] = "2024-12-27"
            }
        }
    }
}

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val login = varchar("login", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = long("created_at").default(System.currentTimeMillis())
    override val primaryKey = PrimaryKey(id)
}

object Servers : Table("servers") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val address = varchar("address", 255)
    val port = integer("port").default(25565)
    val modpack = varchar("modpack", 50)
    val online = bool("online").default(false)
    val players = integer("players").default(0)
    val maxPlayers = integer("max_players").default(100)
    override val primaryKey = PrimaryKey(id)
}

object Modpacks : Table("modpacks") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val version = varchar("version", 20)
    val minecraftVersion = varchar("minecraft_version", 20)
    val description = text("description").default("")
    val downloadUrl = varchar("download_url", 500)
    val size = long("size").default(0)
    override val primaryKey = PrimaryKey(id)
}

object News : Table("news") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 200)
    val content = text("content")
    val date = varchar("date", 20)
    override val primaryKey = PrimaryKey(id)
}
