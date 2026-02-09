package com.axiom.launcher

import java.io.File

class GameLauncher {
    // Portable paths: relative to where the launcher is running
    private val launcherDir = File(System.getProperty("user.dir")).absoluteFile
    private val runtimeDir = File(launcherDir, "runtime")
    private val gameDir = File(launcherDir, "minecraft") // Rename 'client' to 'minecraft' for standard structure
    
    private var gameProcess: Process? = null
    
    // Server connection details
    private val SERVER_IP = "193.23.201.6" 
    private val SERVER_PORT = "25565"

    fun launchGame(username: String, onLog: (String) -> Unit) {
        if (isRunning()) {
            onLog("Игра уже запущена")
            return
        }
        
        onLog("Подготовка к запуску...")
        onLog("Рабочая директория: ${launcherDir.absolutePath}")

        // 1. Locate Java
        val javaBin = findJava()
        if (javaBin == null) {
            onLog("Ошибка: Не найдена Java! Проверьте папку runtime.")
            return
        }
        onLog("Используется Java: ${javaBin.absolutePath}")
        
        // 2. Locate Forge
        val versionsDir = File(gameDir, "versions")
        // Try to find a directory containing "forge"
        val forgeVersionDir = versionsDir.listFiles()?.find { 
            it.isDirectory && it.name.contains("forge") 
        }
        
        if (forgeVersionDir == null) {
            onLog("Ошибка: Версия Forge не найдена в ${versionsDir.absolutePath}")
            return
        }
        val versionId = forgeVersionDir.name
        onLog("Обнаружена версия: $versionId")

        // 3. Build Classpath
        val cp = buildClasspath(gameDir, versionId)
        if (cp.isEmpty()) {
             onLog("Ошибка: Не удалось собрать classpath. Проверьте папку libraries.")
             return
        }

        // 4. Native libraries path
        val nativesDir = File(gameDir, "versions/$versionId/natives")
        if (!nativesDir.exists()) nativesDir.mkdirs()

        // 5. Construct Process
        val command = ArrayList<String>()
        command.add(javaBin.absolutePath)
        command.add("-Xmx4G")
        command.add("-Xms2G")
        command.add("-Djava.library.path=${nativesDir.absolutePath}")
        command.add("-cp")
        command.add(cp)
        command.add("net.minecraft.client.main.Main") // Main class
        
        // Game Arguments
        command.add("--username"); command.add(username)
        command.add("--version"); command.add(versionId)
        command.add("--gameDir"); command.add(gameDir.absolutePath)
        command.add("--assetsDir"); command.add(File(gameDir, "assets").absolutePath)
        command.add("--assetIndex"); command.add("1.20") // Ensure this matches your asset index
        command.add("--accessToken"); command.add("0")
        command.add("--userType"); command.add("legacy")
        
        // Direct Connect Arguments (Skip Main Menu)
        command.add("--server"); command.add(SERVER_IP)
        command.add("--port"); command.add(SERVER_PORT)

        onLog("Запуск процесса...")
        
        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(gameDir)
            processBuilder.redirectErrorStream(true)
            
            gameProcess = processBuilder.start()
            
            // Log output
            Thread {
                gameProcess?.inputStream?.bufferedReader()?.use { reader ->
                    reader.lineSequence().forEach { line ->
                        onLog(line)
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            onLog("Критическая ошибка запуска: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun stopGame() {
        gameProcess?.destroy()
        gameProcess = null
    }
    
    fun isRunning(): Boolean {
        return gameProcess?.isAlive == true
    }
    
    private fun findJava(): File? {
        // 1. Check bundled runtime (Windows)
        val winJava = File(runtimeDir, "bin/java.exe")
        if (winJava.exists()) return winJava
        
        // 2. Check bundled runtime (Linux/Mac)
        val unixJava = File(runtimeDir, "bin/java")
        if (unixJava.exists()) return unixJava
        
        // 3. Fallback to system java (Not ideal for portable, but backup)
        // Check if JAVA_HOME is set
        val javaHome = System.getProperty("java.home")
        if (javaHome != null) {
            val sysJava = File(javaHome, "bin/java" + if (System.getProperty("os.name").contains("Windows")) ".exe" else "")
            if (sysJava.exists()) return sysJava
        }
        
        return null
    }
    
    private fun buildClasspath(baseDir: File, versionId: String): String {
        val classpath = mutableListOf<String>()
        val librariesDir = File(baseDir, "libraries")
        val versionJar = File(baseDir, "versions/$versionId/$versionId.jar")
        
        // Add Forge/Minecraft Jar
        if (versionJar.exists()) {
            classpath.add(versionJar.absolutePath)
        } else {
            println("Warning: Version JAR not found: ${versionJar.absolutePath}")
        }
        
        // Add all libraries recursively
        // Note: Real launchers parse the JSON to get exact order and exclusions.
        // For a simple custom launcher, dumping all JARs usually works if no conflicting versions exist.
        if (librariesDir.exists()) {
            librariesDir.walkTopDown()
                .filter { it.extension == "jar" }
                .forEach { classpath.add(it.absolutePath) }
        }
        
        return classpath.joinToString(File.pathSeparator)
    }
}
