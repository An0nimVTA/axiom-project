package com.axiom.launcher

import com.axiom.launcher.ui.LauncherApp
import javafx.application.Application

fun main(args: Array<String>) {
    // Автоматическая установка при первом запуске
    val installer = AutoInstaller()
    val serverManager = ServerManager()
    val gameLauncher = GameLauncher()
    
    println("=== AXIOM LAUNCHER ===")
    println("Проверка установки...")
    
    // Проверить, установлено ли всё
    val baseDir = java.io.File(System.getProperty("user.home"), ".axiom")
    if (!baseDir.exists() || !java.io.File(baseDir, "server/server.jar").exists()) {
        println("Первый запуск - установка...")
        installer.install { message, progress ->
            println("[$progress%] $message")
        }
    }
    
    // Запустить GUI
    Application.launch(LauncherApp::class.java, *args)
}
