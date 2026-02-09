package com.axiom.launcher

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

class AutoInstaller {
    // Portable paths
    private val baseDir = File(".").absoluteFile
    private val clientDir = File(baseDir, "minecraft")
    
    // URL сервера обновлений
    private val BASE_URL = "http://193.23.201.6:8080/updates"

    fun install(onProgress: (String, Int) -> Unit) {
        onProgress("Проверка структуры папок...", 0)
        clientDir.mkdirs()
        
        onProgress("Проверка Java...", 5)
        checkJava()
        
        onProgress("Загрузка ядра клиента...", 20)
        downloadClientCore()
        
        onProgress("Загрузка модов...", 60)
        downloadMods()
        
        onProgress("Готово к запуску!", 100)
    }
    
    private fun downloadClientCore() {
        println("Проверка файлов клиента...")
        
        // Проверяем наличие критичных файлов
        val versionsDir = File(clientDir, "versions")
        val librariesDir = File(clientDir, "libraries")
        
        if (versionsDir.exists() && librariesDir.exists()) {
            println("Файлы клиента уже установлены.")
            return
        }
        
        val coreZip = findLocalFile("client_core.zip")
        if (coreZip != null) {
            println("Найден локальный client_core.zip, распаковка...")
            unzip(coreZip, clientDir)
            return
        }

        println("⚠️  Файлы клиента не найдены!")
        println("client_core.zip не найден. Продолжаем без ручной установки.")
    }
    
    private fun downloadMods() {
        println("Проверка модов...")
        
        val modsDir = File(clientDir, "mods")
        if (modsDir.exists() && modsDir.listFiles()?.isNotEmpty() == true) {
            println("Моды уже установлены (${modsDir.listFiles()?.size} файлов).")
            return
        }
        
        val modsZip = findLocalFile("modpack.zip")
        if (modsZip != null) {
            println("Найден локальный modpack.zip, распаковка модов...")
            var count = 0
            ZipInputStream(modsZip.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".jar")) {
                        val outFile = File(modsDir, File(entry.name).name)
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { zis.copyTo(it) }
                        count++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            println("Установлено модов: $count")
            return
        }

        println("⚠️  Моды не найдены!")
        println("Продолжаем — модпак будет установлен при запуске игры.")
    }
    
    private fun checkJava() {
        val runtimeDir = File(baseDir, "runtime")
        val javaExe = File(runtimeDir, "bin/java.exe")
        val javaBin = File(runtimeDir, "bin/java")
        
        if (javaExe.exists() || javaBin.exists()) {
            println("Java уже установлена")
            return
        }
        
        println("⚠️  Java не найдена!")
        println("Будет использована системная Java (если установлена)")
    }
    
    private fun downloadFile(urlStr: String, target: File) {
        println("Загрузка: $urlStr")
        val url = URL(urlStr)
        val connection = url.openConnection()
        connection.connectTimeout = 5000
        connection.readTimeout = 60000 // 60 sec timeout
        
        connection.getInputStream().use { input ->
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun unzip(zipFile: File, targetDir: File, ignorePath: String? = null) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // Логика пропуска модов
                if (ignorePath != null && entry.name.startsWith(ignorePath)) {
                    println("Пропуск: ${entry.name}")
                    entry = zis.nextEntry
                    continue
                }

                val newFile = File(targetDir, entry.name)
                // Защита от Zip Slip
                if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("Zip Slip vulnerability detected: ${entry.name}")
                }
                
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

    private fun findLocalFile(name: String): File? {
        val cwd = File(System.getProperty("user.dir")).absoluteFile
        val candidates = listOf(
            File(cwd, name),
            File(cwd, "packages/$name"),
            File(cwd, "build_portable/AxiomClient/$name"),
            File(cwd, "../build_portable/AxiomClient/$name")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
    }
}
