package com.axiom.test;

import com.axiom.AXIOM;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class AutotestSupport {
    private AutotestSupport() {}

    public static boolean isEnabled(String name) {
        String raw = System.getenv(name);
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        return !"0".equals(raw.trim());
    }

    public static boolean isSafeMode() {
        return isEnabled("AXIOM_AUTOTEST_SAFE");
    }

    public static boolean shouldReset() {
        return isEnabled("AXIOM_AUTOTEST_RESET") || isEnabled("AXIOM_AUTOTEST_RESET_ALL");
    }

    public static boolean isResetAll() {
        return isEnabled("AXIOM_AUTOTEST_RESET_ALL");
    }

    public static void maybeResetDataFolder(AXIOM plugin) {
        if (!shouldReset()) {
            return;
        }
        File dataDir = plugin.getDataFolder();
        if (dataDir == null || !dataDir.exists()) {
            return;
        }

        File parent = dataDir.getParentFile();
        File backupRoot = new File(parent, "_autotest_backup");
        if (!backupRoot.exists() && !backupRoot.mkdirs()) {
            plugin.getLogger().warning("Autotest reset: cannot create backup root at " + backupRoot.getAbsolutePath());
            return;
        }

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File backupDir = new File(backupRoot, "AXIOM-" + stamp);
        if (!backupDir.mkdirs()) {
            plugin.getLogger().warning("Autotest reset: cannot create backup dir at " + backupDir.getAbsolutePath());
            return;
        }

        boolean resetAll = isResetAll();
        List<String> preserve = List.of("config.yml", "lang", "recipe-integration.yml", "modpacks.yml");
        File[] children = dataDir.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            try {
                Path target = new File(backupDir, child.getName()).toPath();
                Files.move(child.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().warning("Autotest reset: failed to move " + child.getName() + ": " + e.getMessage());
            }
        }

        if (!dataDir.exists() && !dataDir.mkdirs()) {
            plugin.getLogger().warning("Autotest reset: cannot recreate data folder");
        }

        if (!resetAll) {
            for (String name : preserve) {
                File preserved = new File(backupDir, name);
                if (!preserved.exists()) {
                    continue;
                }
                File dest = new File(dataDir, name);
                try {
                    copyRecursive(preserved.toPath(), dest.toPath());
                } catch (IOException e) {
                    plugin.getLogger().warning("Autotest reset: failed to restore " + name + ": " + e.getMessage());
                }
            }
        }

        plugin.getLogger().info("Autotest reset: data folder cleaned. Backup at " + backupDir.getAbsolutePath());
    }

    private static void copyRecursive(Path src, Path dest) throws IOException {
        if (Files.isDirectory(src)) {
            Files.createDirectories(dest);
            try (var stream = Files.list(src)) {
                stream.forEach(path -> {
                    try {
                        copyRecursive(path, dest.resolve(path.getFileName()));
                    } catch (IOException ignored) {
                    }
                });
            }
        } else {
            Files.createDirectories(dest.getParent());
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
