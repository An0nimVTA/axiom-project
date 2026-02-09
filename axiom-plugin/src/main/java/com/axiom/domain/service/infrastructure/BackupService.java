package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Automatic backup service for AXIOM data.
 * Creates backups every 24 hours, keeps last 7 backups.
 */
public class BackupService {
    private final AXIOM plugin;
    private final File backupDir;
    private static final int MAX_BACKUPS = 7;
    
    public BackupService(AXIOM plugin) {
        this.plugin = plugin;
        this.backupDir = new File(plugin.getDataFolder().getParentFile(), "AXIOM_backups");
        this.backupDir.mkdirs();
        
        // Schedule automatic backups every 24 hours
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::createBackup, 
            20 * 60 * 60, // First backup after 1 hour
            20 * 60 * 60 * 24); // Then every 24 hours
        
        // Clean old backups on startup
        cleanOldBackups();
    }
    
    /**
     * Create a backup of all AXIOM data.
     */
    public synchronized String createBackup() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File backupFile = new File(backupDir, "axiom_backup_" + timestamp + ".zip");
            
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
                File dataFolder = plugin.getDataFolder();
                addDirectoryToZip(dataFolder, dataFolder, zos);
            }
            
            cleanOldBackups();
            
            plugin.getLogger().info("Backup создан: " + backupFile.getName());
            return "Backup создан: " + backupFile.getName();
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка создания backup: " + e.getMessage());
            e.printStackTrace();
            return "Ошибка создания backup: " + e.getMessage();
        }
    }
    
    private void addDirectoryToZip(File sourceDir, File rootDir, ZipOutputStream zos) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                addDirectoryToZip(file, rootDir, zos);
            } else {
                String relativePath = rootDir.toPath().relativize(file.toPath()).toString().replace("\\", "/");
                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
        }
    }
    
    /**
     * Restore from a backup (OP only).
     */
    public synchronized String restoreBackup(String backupFileName) {
        File backupFile = new File(backupDir, backupFileName);
        if (!backupFile.exists()) {
            return "Backup не найден: " + backupFileName;
        }
        
        try {
            // Create temporary backup of current data
            String tempBackup = createBackup();
            
            // Extract backup
            File dataFolder = plugin.getDataFolder();
            
            // Delete current data (except config.yml and plugin.yml)
            deleteDirectoryExceptConfig(dataFolder);
            
            // Extract zip
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(backupFile))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File file = new File(dataFolder, entry.getName());
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        file.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
            
            // Reload plugin data
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.onDisable();
                plugin.onEnable();
            });
            
            plugin.getLogger().info("Backup восстановлен: " + backupFileName);
            return "Backup восстановлен: " + backupFileName + " (временный backup: " + tempBackup + ")";
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка восстановления backup: " + e.getMessage());
            e.printStackTrace();
            return "Ошибка восстановления: " + e.getMessage();
        }
    }
    
    private void deleteDirectoryExceptConfig(File dir) {
        if (!dir.exists()) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().equals("backups")) {
                    deleteDirectory(file);
                }
            } else {
                if (!file.getName().equals("config.yml") && !file.getName().equals("plugin.yml")) {
                    file.delete();
                }
            }
        }
    }
    
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    
    /**
     * List available backups.
     */
    public synchronized List<String> listBackups() {
        File[] files = backupDir.listFiles((d, n) -> n.endsWith(".zip"));
        if (files == null) return new ArrayList<>();
        
        List<String> backups = new ArrayList<>();
        for (File f : files) {
            backups.add(f.getName());
        }
        backups.sort(Collections.reverseOrder()); // Newest first
        return backups;
    }
    
    /**
     * Clean old backups, keep only last MAX_BACKUPS.
     */
    private void cleanOldBackups() {
        File[] files = backupDir.listFiles((d, n) -> n.endsWith(".zip"));
        if (files == null || files.length <= MAX_BACKUPS) return;
        
        // Sort by modification time (oldest first)
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        
        // Delete oldest
        int toDelete = files.length - MAX_BACKUPS;
        for (int i = 0; i < toDelete; i++) {
            files[i].delete();
            plugin.getLogger().info("Старый backup удалён: " + files[i].getName());
        }
    }
    
    /**
     * Get comprehensive backup statistics.
     */
    public synchronized Map<String, Object> getBackupStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<String> backups = listBackups();
        stats.put("totalBackups", backups.size());
        stats.put("maxBackups", MAX_BACKUPS);
        stats.put("backupsList", backups);
        
        // Backup sizes and dates
        List<Map<String, Object>> backupsDetails = new ArrayList<>();
        for (String backupName : backups) {
            File backupFile = new File(backupDir, backupName);
            Map<String, Object> backupData = new HashMap<>();
            backupData.put("name", backupName);
            backupData.put("size", backupFile.length());
            backupData.put("sizeMB", backupFile.length() / 1024.0 / 1024.0);
            backupData.put("lastModified", backupFile.lastModified());
            backupData.put("lastModifiedDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(backupFile.lastModified())));
            backupsDetails.add(backupData);
        }
        stats.put("backupsDetails", backupsDetails);
        
        // Total size
        long totalSize = 0;
        for (File f : backupDir.listFiles((d, n) -> n.endsWith(".zip"))) {
            if (f != null) totalSize += f.length();
        }
        stats.put("totalSize", totalSize);
        stats.put("totalSizeMB", totalSize / 1024.0 / 1024.0);
        
        // Next backup time (simplified - would need to track last backup)
        stats.put("backupIntervalHours", 24);
        
        // Backup rating
        String rating = "НЕТ BACKUP'ОВ";
        if (backups.size() >= 7) rating = "ПОЛНАЯ ЗАЩИТА";
        else if (backups.size() >= 5) rating = "ХОРОШАЯ";
        else if (backups.size() >= 3) rating = "ДОСТАТОЧНАЯ";
        else if (backups.size() >= 1) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get backup file size.
     */
    public synchronized long getBackupSize(String backupFileName) {
        File backupFile = new File(backupDir, backupFileName);
        return backupFile.exists() ? backupFile.length() : 0;
    }
    
    /**
     * Delete specific backup.
     */
    public synchronized String deleteBackup(String backupFileName) {
        File backupFile = new File(backupDir, backupFileName);
        if (!backupFile.exists()) {
            return "Backup не найден: " + backupFileName;
        }
        
        boolean deleted = backupFile.delete();
        if (deleted) {
            plugin.getLogger().info("Backup удалён: " + backupFileName);
            return "Backup удалён: " + backupFileName;
        } else {
            return "Ошибка удаления backup: " + backupFileName;
        }
    }
    
    /**
     * Get latest backup.
     */
    public synchronized String getLatestBackup() {
        List<String> backups = listBackups();
        return backups.isEmpty() ? null : backups.get(0); // First is newest (sorted reverse)
    }
    
    /**
     * Check if backup exists.
     */
    public synchronized boolean backupExists(String backupFileName) {
        File backupFile = new File(backupDir, backupFileName);
        return backupFile.exists();
    }
    
    /**
     * Get global backup statistics (alias for getBackupStatistics for consistency).
     */
    public synchronized Map<String, Object> getGlobalBackupStatistics() {
        return getBackupStatistics();
    }
}

