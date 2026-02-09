package com.axiom.launcher.config

import com.axiom.launcher.model.LauncherConfig
import kotlinx.serialization.json.Json
import java.io.File

object ConfigManager {
    
    // Portable config: store in the launcher directory
    private val configDir = File(System.getProperty("user.dir")).absoluteFile
    private val configFile = File(configDir, "launcher_config.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    var config: LauncherConfig = LauncherConfig()
        private set
    
    fun load() {
        if (configFile.exists()) {
            runCatching {
                config = json.decodeFromString(configFile.readText())
            }
        }
    }
    
    fun save() {
        configDir.mkdirs()
        configFile.writeText(json.encodeToString(LauncherConfig.serializer(), config))
    }
    
    fun update(block: LauncherConfig.() -> LauncherConfig) {
        config = config.block()
        save()
    }
    
    // Use the portable folder for game instances/files
    fun getGameDir(): File = File(configDir, config.gameDir.ifBlank { "minecraft" })
}
