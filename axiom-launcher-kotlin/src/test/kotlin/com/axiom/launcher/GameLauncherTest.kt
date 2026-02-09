package com.axiom.launcher

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class GameLauncherTest {
    private var originalUserDir: String? = null
    private lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        originalUserDir = System.getProperty("user.dir")
        tempDir = Files.createTempDirectory("axiom-launcher-test")
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString())
    }

    @AfterEach
    fun teardown() {
        if (originalUserDir != null) {
            System.setProperty("user.dir", originalUserDir)
        }
    }

    @Test
    fun `buildClasspath includes version jar and libraries`() {
        val launcherRoot = tempDir.toFile()
        val gameDir = File(launcherRoot, "minecraft")
        val versionsDir = File(gameDir, "versions/forge-1.20.1")
        val librariesDir = File(gameDir, "libraries/a/b")
        versionsDir.mkdirs()
        librariesDir.mkdirs()

        val versionJar = File(versionsDir, "forge-1.20.1.jar")
        val libJar = File(librariesDir, "c.jar")
        versionJar.writeText("")
        libJar.writeText("")

        val launcher = GameLauncher()
        val method = GameLauncher::class.java.getDeclaredMethod(
            "buildClasspath",
            File::class.java,
            String::class.java
        )
        method.isAccessible = true

        val classpath = method.invoke(launcher, gameDir, "forge-1.20.1") as String
        assertTrue(classpath.contains(versionJar.absolutePath))
        assertTrue(classpath.contains(libJar.absolutePath))
    }

    @Test
    fun `findJava prefers bundled runtime`() {
        val launcherRoot = tempDir.toFile()
        val runtimeDir = File(launcherRoot, "runtime/bin")
        runtimeDir.mkdirs()
        val javaBin = File(runtimeDir, "java")
        javaBin.writeText("")

        val launcher = GameLauncher()
        val method = GameLauncher::class.java.getDeclaredMethod("findJava")
        method.isAccessible = true

        val found = method.invoke(launcher) as File?
        assertNotNull(found)
        assertEquals(javaBin.absolutePath, found!!.absolutePath)
    }
}
