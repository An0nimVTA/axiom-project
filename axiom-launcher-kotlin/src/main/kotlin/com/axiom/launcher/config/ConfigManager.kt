package com.axiom.launcher.config

import com.axiom.launcher.model.LauncherConfig
import kotlinx.serialization.json.Json
import java.io.File

object ConfigManager {
    
    private val configDir = File(System.getProperty("user.home"), ".axiom-launcher")
    private val configFile = File(configDir, "config.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    var config: LauncherConfig = LauncherConfig()
        private set
    
    fun load() {
        configDir.mkdirs()
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
    
    fun getGameDir(): File = File(configDir, "instances")
    fun getModpacksDir(): File = File(configDir, "modpacks")
}
