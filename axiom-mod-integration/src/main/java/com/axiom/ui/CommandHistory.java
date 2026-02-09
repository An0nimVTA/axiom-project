package com.axiom.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CommandHistory {
    private static final File CONFIG_FILE = new File("config/axiomui/history.json");
    private static final int MAX_HISTORY = 20;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Set<String> favorites = new LinkedHashSet<>();
    private final List<HistoryEntry> history = new ArrayList<>();

    public static class HistoryEntry {
        public String command;
        public long timestamp;
        public int useCount;

        public HistoryEntry(String command) {
            this.command = command;
            this.timestamp = System.currentTimeMillis();
            this.useCount = 1;
        }
    }

    public CommandHistory() {
        load();
    }

    public void addToHistory(String command) {
        HistoryEntry existing = null;
        for (HistoryEntry entry : history) {
            if (entry.command.equals(command)) {
                existing = entry;
                break;
            }
        }
        if (existing != null) {
            history.remove(existing);
            existing.useCount += 1;
            existing.timestamp = System.currentTimeMillis();
            history.add(0, existing);
        } else {
            HistoryEntry entry = new HistoryEntry(command);
            history.add(0, entry);
        }
        
        // Limit size
        while (history.size() > MAX_HISTORY) {
            history.remove(history.size() - 1);
        }
        
        save();
    }

    public int getUseCount(String command) {
        if (command == null) return 0;
        for (HistoryEntry entry : history) {
            if (command.equals(entry.command)) {
                return entry.useCount;
            }
        }
        return 0;
    }

    public void addFavorite(String commandId) {
        favorites.add(commandId);
        save();
    }

    public void removeFavorite(String commandId) {
        favorites.remove(commandId);
        save();
    }

    public boolean isFavorite(String commandId) {
        return favorites.contains(commandId);
    }

    public List<String> getFavorites() {
        return new ArrayList<>(favorites);
    }

    public List<HistoryEntry> getHistory() {
        return new ArrayList<>(history);
    }

    public void clearHistory() {
        history.clear();
        save();
    }

    private void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            
            SaveData data = new SaveData();
            data.favorites = new ArrayList<>(favorites);
            data.history = history;
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!CONFIG_FILE.exists()) return;
        
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            SaveData data = GSON.fromJson(reader, SaveData.class);
            if (data != null) {
                if (data.favorites != null) favorites.addAll(data.favorites);
                if (data.history != null) history.addAll(data.history);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class SaveData {
        List<String> favorites;
        List<HistoryEntry> history;
    }
}
