package com.axiom.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class LauncherFilesInfo(
    val forgeUrl: String,
    val librariesUrl: String,
    val installerUrl: String,
    val forgeVersion: String,
    val minecraftVersion: String,
    val totalSize: Long
)

fun Route.launcherRoutes() {
    val baseDir = File(System.getenv("MODPACKS_DIR") ?: "modpacks").parentFile
    val filesDir = File(baseDir, "launcher-files")
    
    get("/launcher/files") {
        val forgeJar = filesDir.listFiles()?.find { it.name.contains("universal") && it.extension == "jar" }
        val librariesZip = filesDir.listFiles()?.find { it.name.contains("libraries") && it.extension == "zip" }
        val installerJar = filesDir.listFiles()?.find { it.name.contains("installer") && it.extension == "jar" }
        
        val totalSize = (forgeJar?.length() ?: 0) + (librariesZip?.length() ?: 0)
        
        call.respond(LauncherFilesInfo(
            forgeUrl = if (forgeJar != null) "/launcher/download/${forgeJar.name}" else "",
            librariesUrl = if (librariesZip != null) "/launcher/download/${librariesZip.name}" else "",
            installerUrl = if (installerJar != null) "/launcher/download/${installerJar.name}" else "",
            forgeVersion = "47.1.3",
            minecraftVersion = "1.20.1",
            totalSize = totalSize
        ))
    }
    
    get("/launcher/download/{filename}") {
        val filename = call.parameters["filename"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val file = File(filesDir, filename)
        
        if (file.exists() && file.isFile && file.parentFile.canonicalPath == filesDir.canonicalPath) {
            call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.name}\"")
            call.respondFile(file)
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found"))
        }
    }
}
