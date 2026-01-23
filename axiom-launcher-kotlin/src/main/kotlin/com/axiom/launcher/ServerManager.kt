package com.axiom.launcher

import java.io.File

class ServerManager {
    private val serverDir = File(System.getProperty("user.home"), ".axiom/server")
    private var serverProcess: Process? = null
    
    fun startServer(onLog: (String) -> Unit) {
        if (isRunning()) {
            onLog("Сервер уже запущен")
            return
        }
        
        onLog("Запуск сервера...")
        
        val serverJar = File(serverDir, "server.jar")
        if (!serverJar.exists()) {
            onLog("Ошибка: server.jar не найден")
            return
        }
        
        val processBuilder = ProcessBuilder(
            "java",
            "-Xmx4G",
            "-Xms2G",
            "-jar",
            serverJar.absolutePath,
            "nogui"
        )
        
        processBuilder.directory(serverDir)
        processBuilder.redirectErrorStream(true)
        
        serverProcess = processBuilder.start()
        
        // Читать логи сервера
        Thread {
            serverProcess?.inputStream?.bufferedReader()?.use { reader ->
                reader.lineSequence().forEach { line ->
                    onLog(line)
                    
                    // Проверить, когда сервер готов
                    if (line.contains("Done") && line.contains("For help, type")) {
                        onLog("✅ Сервер готов!")
                    }
                }
            }
        }.start()
    }
    
    fun stopServer() {
        serverProcess?.destroy()
        serverProcess = null
    }
    
    fun isRunning(): Boolean {
        return serverProcess?.isAlive == true
    }
    
    fun sendCommand(command: String) {
        serverProcess?.outputStream?.write("$command\n".toByteArray())
        serverProcess?.outputStream?.flush()
    }
}
