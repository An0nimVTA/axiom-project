package com.axiom.launcher

import com.axiom.launcher.api.ApiClient
import com.axiom.launcher.config.ConfigManager
import com.axiom.launcher.minecraft.MinecraftLauncher
import com.axiom.launcher.model.Server
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.TimeUnit

object HeadlessRunner {

    fun run() {
        println("=== HEADLESS MODE ===")
        val cfg = ConfigManager.config
        val api = ApiClient()
        if (!cfg.token.isNullOrBlank() && cfg.token != "OFFLINE") {
            api.setToken(cfg.token)
        }

        val server = runBlocking {
            api.getServers().getOrNull()?.firstOrNull()
        } ?: Server("1", "AXIOM Server (Local)", cfg.serverAddress, cfg.serverPort, "default", true)

        val modpack = runBlocking { api.getModpack(server.modpack).getOrNull() }

        val serverProcess = if (cfg.autoStartServer) startServer(cfg) else null
        if (cfg.autoStartServer && cfg.autoStartServerDelayMs > 0) {
            Thread.sleep(cfg.autoStartServerDelayMs)
        }

        val launcher = MinecraftLauncher("http://localhost:5000")
        val username = cfg.lastUser ?: "Player"
        val token = cfg.token ?: ""

        val result = runBlocking {
            launcher.launch(
                server = server,
                username = username,
                token = token,
                modpack = modpack
            ) { msg, p ->
                val percent = if (p >= 0f) (p * 100).toInt() else -1
                if (percent >= 0) {
                    println("[$percent%] $msg")
                } else {
                    println(msg)
                }
            }
        }

        result.onFailure { e ->
            System.err.println("Ошибка запуска игры: ${e.message}")
        }

        val maxRuntimeMs = System.getenv("AXIOM_AUTOTEST_MAX_RUNTIME_MS")?.toLongOrNull() ?: 0L
        val process = result.getOrNull()
        if (process != null) {
            if (maxRuntimeMs > 0) {
                val finished = process.waitFor(maxRuntimeMs, TimeUnit.MILLISECONDS)
                if (!finished) {
                    println("Таймаут автотеста (${maxRuntimeMs}ms). Остановка клиента.")
                    process.destroy()
                }
            } else {
                process.waitFor()
            }
        }
        serverProcess?.let {
            if (it.isAlive) {
                println("Сервер продолжает работать (PID ${it.pid()})")
            }
        }

        api.close()
    }

    private fun startServer(cfg: com.axiom.launcher.model.LauncherConfig): Process? {
        val script = resolveServerStartScript(cfg)
        if (script == null) {
            println("Скрипт запуска сервера не найден.")
            return null
        }
        println("Запуск сервера: ${script.absolutePath}")
        val builder = ProcessBuilder("bash", script.absolutePath)
            .directory(script.parentFile)
            .redirectErrorStream(true)
            .inheritIO()
        ServerStartEnv.apply(builder)
        return builder.start()
    }

    private fun resolveServerStartScript(cfg: com.axiom.launcher.model.LauncherConfig): File? {
        val configPath = cfg.serverStartPath.trim()
        val envPath = System.getenv("AXIOM_SERVER_START")?.trim().orEmpty()
        val cwd = File(System.getProperty("user.dir"))
        val candidates = listOfNotNull(
            if (configPath.isNotBlank()) File(configPath) else null,
            if (envPath.isNotBlank()) File(envPath) else null,
            File(cwd, "../server/start.sh"),
            File(cwd, "server/start.sh"),
            File(System.getProperty("user.home"), "axiom plugin/server/start.sh")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
    }
}
