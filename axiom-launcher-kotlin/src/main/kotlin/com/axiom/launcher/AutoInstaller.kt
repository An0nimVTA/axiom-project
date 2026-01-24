package com.axiom.launcher

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

class AutoInstaller {
    private val baseDir = File(System.getProperty("user.home"), ".axiom")
    private val serverDir = File(baseDir, "server")
    private val clientDir = File(baseDir, "client")
    private val modsDir = File(clientDir, "mods")

    // GitHub Config
    private val REPO_OWNER = "An0nimVTA"
    private val REPO_NAME = "axiom-project"
    private val RELEASE_TAG = "v2.0.0"
    private val BASE_URL = "https://github.com/$REPO_OWNER/$REPO_NAME/releases/download/$RELEASE_TAG"

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
        println("Forge нужно установить вручную:")
        println("1. Скачать: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html")
        println("2. Версия: 1.20.1-47.4.4")
        println("3. Запустить installer и выбрать 'Install client'")
    }
    
    private fun downloadMods() {
        println("Скачивание пакета модов с GitHub...")
        
        // 1. Скачать mods.zip (библиотеки и зависимости)
        val zipFile = File(clientDir, "mods.zip")
        try {
            downloadFile("$BASE_URL/mods.zip", zipFile)
            println("Распаковка модов...")
            unzip(zipFile, modsDir)
            zipFile.delete()
        } catch (e: Exception) {
            println("Ошибка скачивания mods.zip: ${e.message}")
            println("Пропускаем, возможно это первая версия...")
        }

        // 2. Скачать наш мод (Axiom UI)
        try {
            val modFile = File(modsDir, "axiomui-mod.jar")
            downloadFile("$BASE_URL/axiomui-mod.jar", modFile)
            println("✓ axiomui-mod.jar загружен")
        } catch (e: Exception) {
             println("Ошибка скачивания axiomui-mod.jar: ${e.message}")
        }
    }
    
    private fun downloadServer() {
        val serverJar = File(serverDir, "server.jar")
        if (!serverJar.exists()) {
            println("Скачивание ядра сервера...")
            try {
                downloadFile("$BASE_URL/server-core.jar", serverJar)
            } catch (e: Exception) {
                println("Ошибка скачивания ядра сервера: ${e.message}")
            }
        }
        
        // Копировать моды в папку сервера
        val serverModsDir = File(serverDir, "mods")
        serverModsDir.mkdirs()
        
        println("Синхронизация модов с сервером...")
        modsDir.listFiles()?.forEach { mod ->
            if (mod.extension == "jar") {
                mod.copyTo(File(serverModsDir, mod.name), true)
            }
        }
    }
    
    private fun setupConfig() {
        File(serverDir, "eula.txt").writeText("eula=true")
        File(serverDir, "server.properties").writeText("""
            server-port=25565
            max-players=100
            motd=AXIOM Server
            online-mode=false
        """.trimIndent())
    }
    
    private fun downloadFile(urlStr: String, target: File) {
        println("Загрузка: $urlStr")
        val url = URL(urlStr)
        val connection = url.openConnection()
        connection.connectTimeout = 5000
        connection.readTimeout = 10000
        
        connection.getInputStream().use { input ->
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}
