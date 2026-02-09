package com.axiom.domain.service.military;

import com.axiom.AXIOM;
import com.axiom.domain.model.CityCore;
import com.axiom.domain.model.CountryCore;
import org.bukkit.Location;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.*;
import com.axiom.domain.service.politics.DiplomacySystem;

/**
 * Сервис для динамичной системы захвата городов и стран
 * Включает ядра городов и стран, которые нужно уничтожить для захвата
 */
public class CountryCaptureService {
    private final AXIOM plugin;
    private final Map<String, CountryCore> countryCores;
    private final Map<String, CityCore> cityCores;
    private final Map<String, Set<String>> attackerNations; // nationId -> set of attacking nation IDs
    private final Map<String, Long> siegeTimers; // время, когда началась осада
    private final Map<Chunk, String> trenchChunks; // захваченные чанки для окопов и т.д.
    
    public CountryCaptureService(AXIOM plugin) {
        this.plugin = plugin;
        this.countryCores = new HashMap<>();
        this.cityCores = new HashMap<>();
        this.attackerNations = new HashMap<>();
        this.siegeTimers = new HashMap<>();
        this.trenchChunks = new HashMap<>();
        
        initializeDefaultCores();
    }
    
    /**
     * Инициализация базовых ядер стран и городов
     */
    private void initializeDefaultCores() {
        // Инициализация будет происходить при захвате территорий
        // или через конфигурацию карты мира
    }
    
    /**
     * Создание ядра страны
     */
    public CountryCore createCountryCore(String nationId, String nationName, Location capitalLocation) {
        CountryCore core = new CountryCore(nationId, nationName, capitalLocation);
        countryCores.put(nationId, core);
        return core;
    }
    
    /**
     * Создание ядра города
     */
    public CityCore createCityCore(String cityId, String cityName, String nationId, Location coreLocation) {
        CityCore core = new CityCore(cityId, cityName, nationId, coreLocation);
        cityCores.put(cityId, core);
        
        // Добавляем ядро города к ядру страны
        CountryCore countryCore = countryCores.get(nationId);
        if (countryCore != null) {
            countryCore.addSubordinateCore(core);
        }
        
        return core;
    }
    
    /**
     * Проверка, можно ли начать осаду страны
     */
    public boolean canBeginSiege(String attackerNationId, String defenderNationId) {
        // Проверяем, есть ли дипломатические отношения (война объявлена?)
        // Проверяем, есть ли у атакующего достаточная военная сила
        // В реальной системе нужно проверять через DiplomacySystem
        
        // Проверяем, уже ли ведется осада
        if (isUnderSiege(defenderNationId)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Начать осаду страны
     */
    public boolean beginSiege(String attackerNationId, String defenderNationId) {
        if (!canBeginSiege(attackerNationId, defenderNationId)) {
            return false;
        }
        
        // Записываем, что осада началась
        siegeTimers.put(defenderNationId, System.currentTimeMillis());
        attackerNations.computeIfAbsent(defenderNationId, k -> new HashSet<>()).add(attackerNationId);
        
        return true;
    }
    
    /**
     * Проверка, подвергается ли страна осаде
     */
    public boolean isUnderSiege(String nationId) {
        if (!siegeTimers.containsKey(nationId)) {
            return false;
        }
        
        // Проверяем, не истекло ли время осады
        long siegeStartTime = siegeTimers.get(nationId);
        // Осада может длиться определенное время (например, 1 час в реальном времени)
        // или до полного разрушения ядра страны
        
        return true; // Пока упрощенно - если есть запись, считаем, что под осадой
    }
    
    /**
     * Нанести урон ядру страны
     */
    public boolean damageCountryCore(String nationId, int damage) {
        if (damage <= 0) {
            return false;
        }
        CountryCore core = countryCores.get(nationId);
        if (core == null) {
            return false;
        }
        
        boolean destroyed = core.damageCountry(damage);
        
        if (destroyed) {
            // Страна захвачена!
            onCountryCaptured(nationId);
        }
        
        return destroyed;
    }
    
    /**
     * Нанести урон ядру города
     */
    public boolean damageCityCore(String cityId, int damage) {
        if (damage <= 0) {
            return false;
        }
        CityCore core = cityCores.get(cityId);
        if (core == null) {
            return false;
        }
        
        boolean destroyed = core.damage(damage);
        
        if (destroyed) {
            // Город захвачен!
            onCityCaptured(cityId);
        }
        
        return destroyed;
    }
    
    /**
     * Захват чанка для окопов и укреплений
     */
    public boolean captureChunkForTrenches(Chunk chunk, String nationId, Player player) {
        if (chunk == null || nationId == null) {
            return false;
        }
        // Проверяем, принадлежит ли чанк какой-то нации
        String ownerNation = getChunkOwner(chunk);
        
        // Проверяем, можно ли захватить (осада, дистанция и т.д.)
        if (ownerNation != null && !ownerNation.equals(nationId)) {
            // Это территория врага, но можно создать окопы только рядом с ядром
            if (isNearEnemyCore(chunk, ownerNation)) {
                // Разрешаем создание окопов
                trenchChunks.put(chunk, nationId);
                
                // Отправляем сообщения игроку
                if (player != null) {
                    player.sendMessage("§aВы создали укрепления (окопы) в чанке " + 
                        chunk.getX() + "," + chunk.getZ() + "!");
                }
                    
                return true;
            } else {
                if (player != null) {
                    player.sendMessage("§cВы можете создавать укрепления только рядом с вражеским ядром!");
                }
                return false;
            }
        } else if (ownerNation == null) {
            // Нейтральная территория
            trenchChunks.put(chunk, nationId);
            if (player != null) {
                player.sendMessage("§aВы захватили чанк " + chunk.getX() + "," + chunk.getZ() + " для укреплений!");
            }
            return true;
        } else {
            // Свой чанк
            trenchChunks.put(chunk, nationId);
            if (player != null) {
                player.sendMessage("§eВы укрепляете чанк " + chunk.getX() + "," + chunk.getZ() + "!");
            }
            return true;
        }
    }
    
    /**
     * Проверка, рядом ли чанк с ядром нации
     */
    private boolean isNearEnemyCore(Chunk chunk, String enemyNationId) {
        // Проверяем, рядом ли чанк с каким-либо ядром вражеской нации
        if (chunk == null || enemyNationId == null || chunk.getWorld() == null) {
            return false;
        }
        CountryCore enemyCore = countryCores.get(enemyNationId);
        if (enemyCore != null) {
            Location coreLoc = enemyCore.getCapitalLocation();
            if (coreLoc != null && coreLoc.getWorld() != null && coreLoc.getWorld().equals(chunk.getWorld())) {
                // Проверяем, в пределах ли чанк от ядра (например, в пределах 10 чанков)
                int distance = (int) coreLoc.distance(new Location(chunk.getWorld(),
                    chunk.getX() * 16, coreLoc.getBlockY(), chunk.getZ() * 16));
                
                return distance <= 160; // 10 чанков * 16 блоков = 160 блоков
            }
        }
        
        // Также проверяем ядра городов
        for (CityCore cityCore : cityCores.values()) {
            if (cityCore.getNationId().equals(enemyNationId)) {
                Location coreLoc = cityCore.getCoreLocation();
                if (coreLoc != null && coreLoc.getWorld() != null && coreLoc.getWorld().equals(chunk.getWorld())) {
                    int distance = (int) coreLoc.distance(new Location(chunk.getWorld(),
                        chunk.getX() * 16, coreLoc.getBlockY(), chunk.getZ() * 16));
                    
                    if (distance <= 160) { // Аналогично 10 чанков
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Получить владельца чанка
     */
    private String getChunkOwner(Chunk chunk) {
        // В реальной системе это будет связано с_claim системой
        // Пока используем упрощенную логику
        if (chunk == null || chunk.getWorld() == null || plugin.getNationManager() == null) {
            return null;
        }
        return plugin.getNationManager().getNationIdAtLocation(chunk.getWorld(), 
            chunk.getX() * 16, chunk.getZ() * 16);
    }
    
    /**
     * Событие захвата страны
     */
    private void onCountryCaptured(String nationId) {
        CountryCore capturedCore = countryCores.get(nationId);
        if (capturedCore == null) return;
        
        // Находим атакующую нацию
        Set<String> attackers = attackerNations.get(nationId);
        String conqueringNation = attackers != null && !attackers.isEmpty() ? 
            attackers.iterator().next() : "unknown";
        
        // Сообщение о захвате
        plugin.getLogger().info("Страна " + capturedCore.getNationName() + 
            " захвачена нацией " + conqueringNation);
        
        // Отправляем сообщения в чат
        broadcastCaptureMessage("§c§lСТРАНА ЗАХВАЧЕНА!", 
            "§fНация §b" + capturedCore.getNationName() + " §fбыла завоевана нацией §e" + conqueringNation + "§f!");
        
        // Очищаем осаду
        siegeTimers.remove(nationId);
        attackerNations.remove(nationId);
        
        // Передаем территорию новому владельцу
        transferNationToConqueror(nationId, conqueringNation);
    }
    
    /**
     * Событие захвата города
     */
    private void onCityCaptured(String cityId) {
        CityCore capturedCore = cityCores.get(cityId);
        if (capturedCore == null) return;
        
        // Находим атакующую нацию (упрощенно - через последние осады)
        String conqueringNation = "unknown"; // Нужно определить более точно
        
        // Сообщение о захвате
        plugin.getLogger().info("Город " + capturedCore.getCityName() + 
            " захвачен");
        
        broadcastCaptureMessage("§c§lГОРОД ЗАХВАЧЕН!", 
            "§fГород §b" + capturedCore.getCityName() + " §fбыл захвачен!");
    }
    
    /**
     * Передача нации завоевателю
     */
    private void transferNationToConqueror(String defeatedNationId, String conqueringNationId) {
        // В реальной системе это будет сложный процесс передачи:
        // - Территорий
        // - Ресурсов казны
        // - Населения
        // - Прав
        
        // Пока просто логируем
        plugin.getLogger().info("Нация " + defeatedNationId + " передана " + conqueringNationId);
    }
    
    /**
     * Трансляция сообщения о захвате
     */
    private void broadcastCaptureMessage(String title, String message) {
        // Отправляем сообщение всем игрокам на сервере
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            // Также можем использовать тайтлы
            player.sendTitle(title, message, 10, 70, 20);
        }
    }
    
    /**
     * Получить ядро страны
     */
    public CountryCore getCountryCore(String nationId) {
        return countryCores.get(nationId);
    }
    
    /**
     * Получить ядро города
     */
    public CityCore getCityCore(String cityId) {
        return cityCores.get(cityId);
    }
    
    /**
     * Получить список укрепленных чанков для нации
     */
    public Set<Chunk> getTrenchChunksForNation(String nationId) {
        if (nationId == null) return Collections.emptySet();
        Set<Chunk> result = new HashSet<>();
        for (Map.Entry<Chunk, String> entry : trenchChunks.entrySet()) {
            if (entry.getValue().equals(nationId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Удалить укрепленный чанк
     */
    public void removeTrenchChunk(Chunk chunk) {
        trenchChunks.remove(chunk);
    }
    
    /**
     * Проверить, является ли чанк укрепленным
     */
    public boolean isTrenchChunk(Chunk chunk) {
        return trenchChunks.containsKey(chunk);
    }
    
    /**
     * Получить владельца укрепленного чанка
     */
    public String getTrenchChunkOwner(Chunk chunk) {
        return trenchChunks.get(chunk);
    }
    
    /**
     * Получить все ядра стран
     */
    public Collection<CountryCore> getAllCountryCores() {
        return countryCores.values();
    }
    
    /**
     * Получить все ядра городов
     */
    public Collection<CityCore> getAllCityCores() {
        return cityCores.values();
    }
    
}
