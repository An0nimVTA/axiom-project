package com.axiom.model;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.*;

/**
 * Класс, представляющий ядро города
 */
public class CityCore {
    private String id;
    private String cityName;
    private String nationId;
    private Location coreLocation; // координаты ядра
    private int level; // уровень ядра (1-10)
    private int health; // здоровье ядра
    private int maxHealth; // максимальное здоровье
    private List<Chunk> protectedChunks; // защищенные ядром чанки
    private Map<String, Object> defenses; // оборонительные сооружения
    
    public CityCore(String id, String cityName, String nationId, Location coreLocation) {
        this.id = id;
        this.cityName = cityName;
        this.nationId = nationId;
        this.coreLocation = coreLocation;
        this.level = 1;
        this.maxHealth = 1000; // базовое здоровье
        this.health = this.maxHealth;
        this.protectedChunks = new ArrayList<>();
        this.defenses = new HashMap<>();
    }
    
    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    
    public String getNationId() { return nationId; }
    public void setNationId(String nationId) { this.nationId = nationId; }
    
    public Location getCoreLocation() { return coreLocation; }
    public void setCoreLocation(Location coreLocation) { this.coreLocation = coreLocation; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = Math.max(0, Math.min(health, maxHealth)); }
    
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    
    public List<Chunk> getProtectedChunks() { return new ArrayList<>(protectedChunks); }
    public void addProtectedChunk(Chunk chunk) { 
        if (!protectedChunks.contains(chunk)) {
            protectedChunks.add(chunk);
        }
    }
    public void removeProtectedChunk(Chunk chunk) { 
        protectedChunks.remove(chunk);
    }
    
    public Map<String, Object> getDefenses() { return new HashMap<>(defenses); }
    public void setDefense(String defenseType, Object value) { 
        defenses.put(defenseType, value); 
    }
    
    /**
     * Проверка, находится ли чанк под защитой ядра
     */
    public boolean isChunkProtected(Chunk chunk) {
        return protectedChunks.contains(chunk);
    }
    
    /**
     * Повреждение ядра
     */
    public boolean damage(int damageAmount) {
        this.health -= damageAmount;
        if (this.health <= 0) {
            this.health = 0;
            return true; // ядро разрушено
        }
        return false; // ядро не разрушено
    }
    
    /**
     * Восстановление ядра
     */
    public void heal(int healAmount) {
        this.health = Math.min(this.health + healAmount, this.maxHealth);
    }
    
    /**
     * Уровень ядра влияет на радиус защиты
     */
    public int getProtectionRadius() {
        return 5 + (level * 2); // радиус защиты в чанках
    }
    
    /**
     * Проверить, включен ли мод
     */
    public boolean isModEnabled(String modName) {
        return modConfig.getOrDefault(modName, false);
    }
    
    /**
     * Установить статус мода
     */
    public void setModEnabled(String modName, boolean enabled) {
        modConfig.put(modName, enabled);
    }
}