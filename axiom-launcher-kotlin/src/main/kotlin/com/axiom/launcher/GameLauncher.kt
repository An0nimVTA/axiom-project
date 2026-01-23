package com.axiom.launcher

import java.io.File

class GameLauncher {
    private val clientDir = File(System.getProperty("user.home"), ".axiom/client")
    private var gameProcess: Process? = null
    
    fun launchGame(username: String, onLog: (String) -> Unit) {
        if (isRunning()) {
            onLog("Игра уже запущена")
            return
        }
        
        onLog("Запуск Minecraft...")
        
        // Найти Forge
        val forgeDir = File(System.getProperty("user.home"), ".minecraft")
        val versionsDir = File(forgeDir, "versions")
        
        val forgeVersion = versionsDir.listFiles()
            ?.find { it.name.contains("forge") && it.name.contains("1.20.1") }
            ?.name
        
        if (forgeVersion == null) {
            onLog("Ошибка: Forge не установлен")
            return
        }
        
        val processBuilder = ProcessBuilder(
            "java",
            "-Xmx4G",
            "-Xms2G",
            "-Djava.library.path=${File(forgeDir, "natives").absolutePath}",
            "-cp", buildClasspath(forgeDir, forgeVersion),
            "net.minecraft.client.main.Main",
            "--username", username,
            "--version", forgeVersion,
            "--gameDir", clientDir.absolutePath,
            "--assetsDir", File(forgeDir, "assets").absolutePath,
            "--assetIndex", "1.20",
            "--accessToken", "0",
            "--userType", "legacy"
        )
        
        processBuilder.directory(clientDir)
        processBuilder.redirectErrorStream(true)
        
        gameProcess = processBuilder.start()
        
        // Читать логи игры
        Thread {
            gameProcess?.inputStream?.bufferedReader()?.use { reader ->
                reader.lineSequence().forEach { line ->
                    onLog(line)
                }
            }
        }.start()
    }
    
    fun stopGame() {
        gameProcess?.destroy()
        gameProcess = null
    }
    
    fun isRunning(): Boolean {
        return gameProcess?.isAlive == true
    }
    
    private fun buildClasspath(forgeDir: File, version: String): String {
        val versionDir = File(File(forgeDir, "versions"), version)
        val versionJar = File(versionDir, "$version.jar")
        val librariesDir = File(forgeDir, "libraries")
        
        val classpath = mutableListOf<String>()
        classpath.add(versionJar.absolutePath)
        
        // Добавить все библиотеки
        librariesDir.walkTopDown()
            .filter { it.extension == "jar" }
            .forEach { classpath.add(it.absolutePath) }
        
        return classpath.joinToString(File.pathSeparator)
    }
}
