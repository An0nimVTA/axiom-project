package com.axiom.launcher

import com.axiom.launcher.config.ConfigManager
import com.axiom.launcher.ui.LauncherApp
import javafx.application.Application
import java.io.File

fun main(args: Array<String>) {
    println("=== AXIOM CLIENT LAUNCHER (Portable) ===")
    
    // 1. Initialize Config
    println("Инициализация конфигурации...")
    ConfigManager.load()
    val headless = args.contains("--headless") || System.getenv("AXIOM_HEADLESS") == "1"
    
    // 2. Run Auto-Installer / Updater
    // In a real GUI app, this might be better inside the GUI with a progress bar,
    // but for simplicity and robustness, we do a quick check here.
    val installer = AutoInstaller()
    val gameDir = ConfigManager.getGameDir()
    
    // Check if we need a full install or just an update
    if (!gameDir.exists() || !File(gameDir, "versions").exists()) {
        println("Обнаружена первая установка или поврежденные файлы.")
    }
    
    println("Проверка обновлений...")
    try {
        installer.install { message, progress ->
            println("[$progress%] $message")
        }
    } catch (e: Exception) {
        println("Ошибка при обновлении: ${e.message}")
        println("Продолжаем запуск (возможно, оффлайн режим)...")
    }
    
    if (headless) {
        HeadlessRunner.run()
        return
    }

    // 3. Launch GUI
    println("Запуск интерфейса...")
    try {
        Application.launch(LauncherApp::class.java, *args)
    } catch (e: Exception) {
        e.printStackTrace()
        println("Ошибка запуска GUI. Убедитесь, что JavaFX доступен.")
    }
}
