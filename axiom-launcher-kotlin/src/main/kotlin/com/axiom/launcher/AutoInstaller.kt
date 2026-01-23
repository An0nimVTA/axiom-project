package com.axiom.launcher

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class AutoInstaller {
    private val baseDir = File(System.getProperty("user.home"), ".axiom")
    private val serverDir = File(baseDir, "server")
    private val clientDir = File(baseDir, "client")
    private val modsDir = File(clientDir, "mods")
    
    fun install(onProgress: (String, Int) -> Unit) {
        onProgress("Создание директорий...", 0)
        baseDir.mkdirs()
        serverDir.mkdirs()
        clientDir.mkdirs()
        modsDir.mkdirs()
        
        onProgress("Скачивание Forge...", 10)
        downloadForge()
        
        onProgress("Скачивание модов...", 30)
        downloadMods()
        
        onProgress("Скачивание сервера...", 60)
        downloadServer()
        
        onProgress("Настройка конфигурации...", 80)
        setupConfig()
        
        onProgress("Готово!", 100)
    }
    
    private fun downloadForge() {
        // Пропустить автоустановку Forge - игрок установит вручную
        println("Forge нужно установить вручную:")
        println("1. Скачать: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html")
        println("2. Версия: 1.20.1-47.4.4")
        println("3. Запустить installer и выбрать 'Install client'")
        println("Пропускаем автоустановку...")
    }
    
    private fun downloadMods() {
        // Копировать моды напрямую с сервера
        val localMods = File("/home/an0nimvta/axiom plugin/server/mods")
        
        if (!localMods.exists()) {
            println("Локальные моды не найдены: ${localMods.absolutePath}")
            return
        }
        
        println("Копирование модов с сервера...")
        var copied = 0
        
        localMods.listFiles()?.forEach { mod ->
            if (mod.extension == "jar") {
                val target = File(modsDir, mod.name)
                try {
                    Files.copy(mod.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    println("✓ ${mod.name}")
                    copied++
                } catch (e: Exception) {
                    println("✗ ${mod.name}: ${e.message}")
                }
            }
        }
        
        println("Скопировано модов: $copied")
    }
    
    private fun downloadServer() {
        val serverJar = File(serverDir, "server.jar")
        if (!serverJar.exists()) {
            // Копировать с локального сервера
            val localServer = File("/home/an0nimvta/axiom plugin/server/mohist-1.20.1-forge-47.4.4-1.0.0-server.jar")
            if (localServer.exists()) {
                Files.copy(localServer.toPath(), serverJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        
        // Копировать моды сервера
        val serverModsDir = File(serverDir, "mods")
        serverModsDir.mkdirs()
        
        val localMods = File("/home/an0nimvta/axiom plugin/server/mods")
        if (localMods.exists()) {
            localMods.listFiles()?.forEach { mod ->
                val target = File(serverModsDir, mod.name)
                if (!target.exists()) {
                    Files.copy(mod.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
    
    private fun setupConfig() {
        // Создать eula.txt
        File(serverDir, "eula.txt").writeText("eula=true")
        
        // Создать server.properties
        File(serverDir, "server.properties").writeText("""
            server-port=25565
            max-players=100
            motd=AXIOM Server
            online-mode=false
        """.trimIndent())
    }
    
    private fun downloadFile(url: String, target: File) {
        URL(url).openStream().use { input ->
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
