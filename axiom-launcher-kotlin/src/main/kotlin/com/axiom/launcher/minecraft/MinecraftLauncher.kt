package com.axiom.launcher.minecraft

import com.axiom.launcher.config.ConfigManager
import com.axiom.launcher.model.Modpack
import com.axiom.launcher.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

@Serializable
data class LauncherFilesInfo(
    val forgeUrl: String = "",
    val librariesUrl: String = "",
    val forgeVersion: String = "47.1.3",
    val minecraftVersion: String = "1.20.1",
    val installerUrl: String = "",
    val totalSize: Long = 0
)

class MinecraftLauncher(private val apiUrl: String = "http://localhost:5000") {
    
    private val baseDir = File(System.getProperty("user.home"), ".axiom-launcher")
    private val instancesDir = File(baseDir, "instances")
    private val librariesDir = File(baseDir, "libraries")
    private val assetsDir = File(baseDir, "assets")
    private val nativesDir = File(baseDir, "natives")
    
    private val json = Json { ignoreUnknownKeys = true }
    private val sep = File.pathSeparator
    
    companion object {
        const val MC_VERSION = "1.20.1"
        const val FORGE_VERSION = "47.1.3"
    }

    private data class ResolvedVersions(
        val forge: JsonObject,
        val vanilla: JsonObject,
        val assetIndexId: String
    )
    
    init {
        listOf(baseDir, instancesDir, librariesDir, assetsDir, nativesDir).forEach { it.mkdirs() }
    }
    
    suspend fun launch(
        server: Server,
        username: String,
        token: String,
        modpack: Modpack? = null,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<Process> = withContext(Dispatchers.IO) {
        runCatching {
            val instanceDir = File(instancesDir, modpack?.id ?: "default").apply { mkdirs() }
            val modsDir = File(instanceDir, "mods").apply { mkdirs() }
            onProgress("Подготовка окружения...", 0.05f)
            val versions = ensureForgeAndMinecraftInstalled(onProgress)
            
            if (modpack != null && !isModpackInstalled(modsDir)) {
                downloadModpack(modpack, modsDir, onProgress)
            } else if (modpack != null) {
                onProgress("Модпак: ${modsDir.listFiles()?.size ?: 0} модов", 0.7f)
            }
            
            onProgress("Запуск Forge...", 0.9f)
            val process = launchForge(server, username, instanceDir, versions)
            onProgress("Игра запущена!", 1.0f)
            process
        }
    }
    
    private fun isForgeInstalled(): Boolean {
        val versionJson = File(baseDir, "versions/$MC_VERSION-forge-$FORGE_VERSION/$MC_VERSION-forge-$FORGE_VERSION.json")
        return versionJson.exists()
    }
    
    private fun isModpackInstalled(modsDir: File) = (modsDir.listFiles()?.size ?: 0) > 3
    
    private suspend fun downloadModpack(modpack: Modpack, modsDir: File, onProgress: (String, Float) -> Unit) = withContext(Dispatchers.IO) {
        onProgress("Скачивание модпака...", 0.35f)
        val url = if (modpack.downloadUrl.startsWith("http")) modpack.downloadUrl else "$apiUrl${modpack.downloadUrl}"
        val zipFile = File(baseDir, "modpack.zip")
        downloadFile(url, zipFile) { p -> onProgress("Модпак: ${(p * 100).toInt()}%", 0.35f + p * 0.3f) }
        onProgress("Распаковка...", 0.66f)
        modsDir.listFiles()?.forEach { it.delete() }
        var count = 0
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".jar")) {
                    FileOutputStream(File(modsDir, File(entry.name).name)).use { zis.copyTo(it) }
                    count++
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        zipFile.delete()
        onProgress("Установлено $count модов", 0.7f)
    }

    private fun launchForge(server: Server, username: String, instanceDir: File, versions: ResolvedVersions): Process {
        val config = ConfigManager.config
        val java = findJava(config.javaPath)
        
        // Parse version JSON
        val versionData = versions.forge
        
        val mainClass = versionData["mainClass"]?.jsonPrimitive?.content ?: "cpw.mods.bootstraplauncher.BootstrapLauncher"
        
        // Build JVM args from JSON
        val jvmArgs = mutableListOf<String>()
        jvmArgs.add("-Xms${config.minRam}M")
        jvmArgs.add("-Xmx${config.maxRam}M")
        jvmArgs.add("-Djava.library.path=${nativesDir.absolutePath}")
        
        versionData["arguments"]?.jsonObject?.get("jvm")?.jsonArray?.forEach { arg ->
            if (arg is JsonPrimitive) {
                val processed = arg.content
                    .replace("\${library_directory}", librariesDir.absolutePath)
                    .replace("\${classpath_separator}", sep)
                    .replace("\${version_name}", "$MC_VERSION-forge-$FORGE_VERSION")
                    .replace("\${natives_directory}", nativesDir.absolutePath)
                    .replace("\${launcher_name}", "AXIOM")
                    .replace("\${launcher_version}", "1.0")
                jvmArgs.add(processed)
            }
        }
        
        // Build classpath from libraries
        val cpSet = linkedSetOf<String>()
        buildLibraryClasspath(versionData, cpSet)
        buildLibraryClasspath(versions.vanilla, cpSet)
        
        // Add client jar
        val clientJar = File(baseDir, "versions/1.20.1/1.20.1.jar")
        if (clientJar.exists()) cpSet.add(clientJar.absolutePath)
        
        val classpath = cpSet.joinToString(sep)
        
        // Build game args
        val gameArgs = mutableListOf<String>()
        versionData["arguments"]?.jsonObject?.get("game")?.jsonArray?.forEach { arg ->
            if (arg is JsonPrimitive) gameArgs.add(arg.content)
        }
        
        // Add standard MC args
        gameArgs.addAll(listOf(
            "--username", username,
            "--version", "$MC_VERSION-forge-$FORGE_VERSION",
            "--gameDir", instanceDir.absolutePath,
            "--assetsDir", assetsDir.absolutePath,
            "--assetIndex", versions.assetIndexId,
            "--uuid", offlineUUID(username),
            "--accessToken", "0",
            "--userType", "legacy",
            "--userProperties", "{}",
            "--versionType", "release",
            "--server", server.address,
            "--port", server.port.toString()
        ))
        
        // Final command
        val cmd = mutableListOf(java)
        cmd.addAll(jvmArgs)
        cmd.addAll(listOf("-cp", classpath, mainClass))
        cmd.addAll(gameArgs)
        
        println("=== FORGE LAUNCH ===")
        println("Main class: $mainClass")
        println("Game dir: ${instanceDir.absolutePath}")
        println("Mods: ${File(instanceDir, "mods").listFiles()?.size ?: 0}")
        println("Server: ${server.address}:${server.port}")
        println("Libraries: ${cpSet.size}")
        
        return ProcessBuilder(cmd).directory(instanceDir).inheritIO().start()
    }
    
    private fun findJava(path: String): String {
        if (File(path).exists()) return path
        listOf("/usr/lib/jvm/java-17-openjdk-amd64/bin/java", "/usr/lib/jvm/java-21-openjdk-amd64/bin/java", "/usr/bin/java")
            .forEach { if (File(it).exists()) return it }
        return "java"
    }
    
    private fun offlineUUID(name: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val b = md.digest("OfflinePlayer:$name".toByteArray())
        b[6] = (b[6].toInt() and 0x0f or 0x30).toByte(); b[8] = (b[8].toInt() and 0x3f or 0x80).toByte()
        val h = b.joinToString("") { "%02x".format(it) }
        return "${h.substring(0,8)}-${h.substring(8,12)}-${h.substring(12,16)}-${h.substring(16,20)}-${h.substring(20)}"
    }

    private suspend fun ensureForgeAndMinecraftInstalled(onProgress: (String, Float) -> Unit): ResolvedVersions {
        val forgeVersionId = "$MC_VERSION-forge-$FORGE_VERSION"
        val forgeVersionDir = File(baseDir, "versions/$forgeVersionId").apply { mkdirs() }
        val forgeVersionJson = File(forgeVersionDir, "$forgeVersionId.json")

        val launcherInfo = runCatching { fetchLauncherFilesInfo() }.getOrNull()

        if (!forgeVersionJson.exists()) {
            onProgress("Скачивание Forge файлов...", 0.1f)
            val info = launcherInfo ?: throw RuntimeException("Сервер недоступен для Forge файлов")
            if (info.installerUrl.isBlank()) {
                throw RuntimeException("Файлы Forge не найдены на сервере")
            }
            val installerJar = File(baseDir, "forge-installer-$FORGE_VERSION.jar")
            if (!installerJar.exists()) {
                downloadFile(apiUrl + info.installerUrl, installerJar) { p ->
                    onProgress("Forge installer: ${(p * 100).toInt()}%", 0.1f + p * 0.1f)
                }
            }
            extractJsonFromJar(installerJar, "version.json", forgeVersionJson)
        }

        val forgeJson = json.parseToJsonElement(forgeVersionJson.readText()).jsonObject
        val vanillaJson = ensureVanillaVersionInstalled(onProgress)
        val assetIndexId = vanillaJson["assetIndex"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: "1.20"

        if (launcherInfo != null && launcherInfo.librariesUrl.isNotBlank() && !librariesPresent()) {
            onProgress("Установка библиотек...", 0.45f)
            val libsZip = File(baseDir, "forge-libraries-$MC_VERSION.zip")
            if (!libsZip.exists()) {
                downloadFile(apiUrl + launcherInfo.librariesUrl, libsZip) { p ->
                    onProgress("Библиотеки: ${(p * 100).toInt()}%", 0.45f + p * 0.1f)
                }
            }
            extractZip(libsZip, librariesDir)
        }

        ensureLibrariesInstalled(forgeJson, vanillaJson, onProgress)
        ensureAssetsInstalled(vanillaJson, onProgress)

        return ResolvedVersions(forgeJson, vanillaJson, assetIndexId)
    }

    private suspend fun ensureVanillaVersionInstalled(onProgress: (String, Float) -> Unit): JsonObject {
        val versionDir = File(baseDir, "versions/$MC_VERSION").apply { mkdirs() }
        val versionJsonFile = File(versionDir, "$MC_VERSION.json")
        val clientJar = File(versionDir, "$MC_VERSION.jar")

        if (!versionJsonFile.exists()) {
            onProgress("Скачивание Minecraft $MC_VERSION...", 0.25f)
            val manifest = downloadText("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
            val manifestJson = json.parseToJsonElement(manifest).jsonObject
            val versions = manifestJson["versions"]?.jsonArray ?: throw RuntimeException("Не удалось получить версии Minecraft")
            val entry = versions.firstOrNull { it.jsonObject["id"]?.jsonPrimitive?.content == MC_VERSION }
                ?: throw RuntimeException("Версия Minecraft $MC_VERSION не найдена")
            val versionUrl = entry.jsonObject["url"]?.jsonPrimitive?.content ?: throw RuntimeException("Нет URL версии")
            val versionText = downloadText(versionUrl)
            versionJsonFile.writeText(versionText)
        }

        val vanillaJson = json.parseToJsonElement(versionJsonFile.readText()).jsonObject
        if (!clientJar.exists()) {
            val clientUrl = vanillaJson["downloads"]?.jsonObject?.get("client")?.jsonObject?.get("url")?.jsonPrimitive?.content
                ?: throw RuntimeException("Не удалось получить URL клиента")
            downloadFile(clientUrl, clientJar) { p ->
                onProgress("Minecraft клиент: ${(p * 100).toInt()}%", 0.35f + p * 0.15f)
            }
        }

        return vanillaJson
    }

    private suspend fun ensureAssetsInstalled(vanillaJson: JsonObject, onProgress: (String, Float) -> Unit) {
        val assetIndex = vanillaJson["assetIndex"]?.jsonObject ?: return
        val indexId = assetIndex["id"]?.jsonPrimitive?.content ?: return
        val indexUrl = assetIndex["url"]?.jsonPrimitive?.content ?: return
        val indexesDir = File(assetsDir, "indexes").apply { mkdirs() }
        val objectsDir = File(assetsDir, "objects").apply { mkdirs() }
        val indexFile = File(indexesDir, "$indexId.json")

        if (!indexFile.exists()) {
            onProgress("Загрузка asset index...", 0.55f)
            downloadFile(indexUrl, indexFile)
        }

        val indexJson = json.parseToJsonElement(indexFile.readText()).jsonObject
        val objects = indexJson["objects"]?.jsonObject ?: return
        val total = objects.size
        var done = 0
        for ((_, obj) in objects) {
            val hash = obj.jsonObject["hash"]?.jsonPrimitive?.content ?: continue
            val sub = hash.substring(0, 2)
            val outFile = File(objectsDir, "$sub/$hash")
            if (!outFile.exists()) {
                val url = "https://resources.download.minecraft.net/$sub/$hash"
                downloadFile(url, outFile)
            }
            done++
            if (done % 200 == 0 || done == total) {
                val percent = if (total > 0) (done * 100 / total) else 100
                val progress = if (total > 0) done.toFloat() / total else 1.0f
                onProgress("Assets: $percent%", 0.55f + progress * 0.15f)
            }
        }
    }

    private suspend fun ensureLibrariesInstalled(forgeJson: JsonObject, vanillaJson: JsonObject, onProgress: (String, Float) -> Unit) {
        val libs = mutableListOf<JsonObject>()
        forgeJson["libraries"]?.jsonArray?.forEach { libs.add(it.jsonObject) }
        vanillaJson["libraries"]?.jsonArray?.forEach { libs.add(it.jsonObject) }

        val total = libs.size.coerceAtLeast(1)
        var processed = 0
        for (lib in libs) {
            if (!isLibraryAllowed(lib)) continue
            val downloads = lib["downloads"]?.jsonObject ?: continue
            val artifact = downloads["artifact"]?.jsonObject
            if (artifact != null) {
                val path = artifact["path"]?.jsonPrimitive?.content
                val url = artifact["url"]?.jsonPrimitive?.content
                if (path != null && url != null) {
                    val file = File(librariesDir, path)
                    if (!file.exists()) {
                        downloadFile(url, file)
                    }
                }
            }

            val natives = lib["natives"]?.jsonObject
            if (natives != null) {
                var osKey = natives[osName()]?.jsonPrimitive?.content
                if (osKey != null && osKey.contains("\${arch}")) {
                    val arch = if (System.getProperty("os.arch").contains("64")) "64" else "32"
                    osKey = osKey.replace("\${arch}", arch)
                }
                if (osKey != null) {
                    val classifier = downloads["classifiers"]?.jsonObject?.get(osKey)?.jsonObject
                    val path = classifier?.get("path")?.jsonPrimitive?.content
                    val url = classifier?.get("url")?.jsonPrimitive?.content
                    if (path != null && url != null) {
                        val file = File(librariesDir, path)
                        if (!file.exists()) {
                            downloadFile(url, file)
                        }
                        extractNatives(file, nativesDir, lib["extract"]?.jsonObject)
                    }
                }
            }

            processed++
            if (processed % 50 == 0) {
                val percent = (processed * 100) / total
                onProgress("Библиотеки: $percent%", 0.7f)
            }
        }
    }

    private fun buildLibraryClasspath(versionData: JsonObject, cpList: MutableSet<String>) {
        versionData["libraries"]?.jsonArray?.forEach { lib ->
            val obj = lib.jsonObject
            if (!isLibraryAllowed(obj)) return@forEach
            val path = obj["downloads"]?.jsonObject?.get("artifact")?.jsonObject?.get("path")?.jsonPrimitive?.content
            if (path != null) {
                val file = File(librariesDir, path)
                if (file.exists()) cpList.add(file.absolutePath)
            }
        }
    }

    private fun librariesPresent(): Boolean {
        if (!librariesDir.exists()) return false
        return librariesDir.walkTopDown().any { it.isFile }
    }

    private fun isLibraryAllowed(lib: JsonObject): Boolean {
        val rules = lib["rules"]?.jsonArray ?: return true
        var allowed = false
        for (rule in rules) {
            val r = rule.jsonObject
            val action = r["action"]?.jsonPrimitive?.content ?: continue
            val osRule = r["os"]?.jsonObject?.get("name")?.jsonPrimitive?.content
            if (osRule == null || osRule == osName()) {
                allowed = action == "allow"
            }
        }
        return allowed
    }

    private fun osName(): String {
        val name = System.getProperty("os.name").lowercase()
        return when {
            name.contains("win") -> "windows"
            name.contains("mac") -> "osx"
            else -> "linux"
        }
    }

    private fun extractNatives(jar: File, targetDir: File, extract: JsonObject?) {
        val excludes = extract?.get("exclude")?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        ZipInputStream(FileInputStream(jar)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && !name.startsWith("META-INF/") && excludes.none { name.startsWith(it) }) {
                    val out = File(targetDir, name)
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun extractZip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = File(targetDir, entry.name)
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun extractJsonFromJar(jarFile: File, entryName: String, outFile: File) {
        ZipFile(jarFile).use { zip ->
            val entry = zip.getEntry(entryName) ?: throw RuntimeException("Не найден $entryName в $jarFile")
            outFile.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input ->
                outFile.outputStream().use { input.copyTo(it) }
            }
        }
    }

    private suspend fun fetchLauncherFilesInfo(): LauncherFilesInfo {
        val url = "$apiUrl/launcher/files"
        val text = downloadText(url)
        return json.decodeFromString(LauncherFilesInfo.serializer(), text)
    }

    private suspend fun downloadText(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 30000; conn.readTimeout = 300000
        conn.setRequestProperty("User-Agent", "AXIOM-Launcher/1.0")
        if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
        conn.inputStream.bufferedReader().use { it.readText() }
    }
    
    private suspend fun downloadFile(url: String, dest: File, onProgress: (Float) -> Unit = {}) = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()
        if (dest.exists() && dest.length() > 0) return@withContext
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 30000; conn.readTimeout = 300000
        conn.setRequestProperty("User-Agent", "AXIOM-Launcher/1.0")
        if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
        val total = conn.contentLengthLong; var dl = 0L
        conn.inputStream.buffered().use { i -> FileOutputStream(dest).buffered().use { o ->
            val buf = ByteArray(65536); var n: Int
            while (i.read(buf).also { n = it } != -1) { o.write(buf, 0, n); dl += n; if (total > 0) onProgress(dl.toFloat() / total) }
        }}
    }
}
