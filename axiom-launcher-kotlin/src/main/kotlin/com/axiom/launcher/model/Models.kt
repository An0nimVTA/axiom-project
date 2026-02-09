package com.axiom.launcher.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val login: String,
    val token: String? = null
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: User? = null
)

@Serializable
data class Server(
    val id: String,
    val name: String,
    val address: String,
    val port: Int,
    val modpack: String,
    val online: Boolean = false,
    val players: Int = 0,
    val maxPlayers: Int = 100
)

@Serializable
data class Modpack(
    val id: String,
    val name: String,
    val version: String,
    val minecraftVersion: String,
    val description: String = "",
    val downloadUrl: String,
    val size: Long = 0
)

@Serializable
data class LauncherConfig(
    val javaPath: String = "java",
    val minRam: Int = 2048,
    val maxRam: Int = 4096,
    val gameDir: String = "minecraft",
    val serverStartPath: String = "",
    val serverAddress: String = "localhost",
    val serverPort: Int = 25565,
    val lastUser: String? = null,
    val token: String? = null,
    val localPackagesDir: String = "",
    val offlineMode: Boolean = false,
    val autoLaunch: Boolean = false,
    val autoStartServer: Boolean = false,
    val autoStartServerDelayMs: Long = 15000,
    val autoUiTests: Boolean = false,
    val autoUiTestIncludeScreens: Boolean = true,
    val autoUiTestAutoStartDelayTicks: Int = 60,
    val autoUiTestStepDelayTicks: Int = 5,
    val autoUiTestCommandTimeoutTicks: Int = 200,
    val autoUiTestCommands: List<String> = emptyList(),
    val autoUiTestCommandBlacklist: List<String> = emptyList()
)

@Serializable
data class NewsItem(
    val id: Int,
    val title: String,
    val content: String,
    val date: String
)
