package com.axiom.launcher

import com.axiom.launcher.config.ConfigManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ConfigManagerTest {
    private var originalUserDir: String? = null
    private lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        originalUserDir = System.getProperty("user.dir")
        tempDir = Files.createTempDirectory("axiom-launcher-test")
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString())
        ConfigManager.load()
    }

    @AfterEach
    fun teardown() {
        if (originalUserDir != null) {
            System.setProperty("user.dir", originalUserDir)
        }
    }

    @Test
    fun `save and load config in portable directory`() {
        ConfigManager.update {
            copy(javaPath = "test-java", minRam = 1234, maxRam = 2345, lastUser = "tester")
        }

        val configFile = tempDir.resolve("launcher_config.json")
        assertTrue(Files.exists(configFile))

        ConfigManager.load()
        val cfg = ConfigManager.config
        assertEquals("test-java", cfg.javaPath)
        assertEquals(1234, cfg.minRam)
        assertEquals(2345, cfg.maxRam)
        assertEquals("tester", cfg.lastUser)
    }
}
