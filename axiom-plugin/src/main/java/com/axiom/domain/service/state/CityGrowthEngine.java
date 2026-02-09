package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import com.axiom.domain.model.City;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.infrastructure.VisualEffectsService;

/** Simulates city growth, demographics and productivity. */
public class CityGrowthEngine {
    private final AXIOM plugin;
    private final NationManager nationManager;

    private final File citiesDir;
    private final Map<String, City> cities = new HashMap<>();

    public CityGrowthEngine(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.citiesDir = new File(plugin.getDataFolder(), "cities");
        this.citiesDir.mkdirs();
        loadAll();
    }

    public synchronized City createCity(String nationId, String name, String centerChunk) throws IOException {
        String id = name.toLowerCase().replaceAll("[^a-z0-9_-]", "-");
        City c = new City(id, name, nationId, centerChunk);
        cities.put(id, c);
        save(c);
        
        // VISUAL EFFECTS: Announce city creation
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            VisualEffectsService effectsService = plugin.getVisualEffectsService();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                for (UUID citizenId : n.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        citizen.sendTitle("§b§l[НОВЫЙ ГОРОД]", "§fГород '" + name + "' основан!", 10, 60, 10);
                        if (effectsService != null) {
                            effectsService.sendActionBar(citizen, "§b🏙️ Город '" + name + "' создан!");
                        }
                        // Blue particles
                        org.bukkit.Location loc = citizen.getLocation();
                        for (int i = 0; i < 10; i++) {
                            loc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.05);
                        }
                        citizen.playSound(loc, org.bukkit.Sound.ENTITY_VILLAGER_CELEBRATE, 0.7f, 1.0f);
                    }
                }
            });
        }
        
        return c;
    }

    public synchronized void buildInfrastructure(String cityId, String type) throws IOException {
        City c = cities.get(cityId);
        if (c == null) throw new IOException("Город не найден.");
        final String infraName;
        switch (type.toLowerCase()) {
            case "hospital": 
                c.setHasHospital(true); 
                infraName = "Больница";
                break;
            case "school": 
                c.setHasSchool(true); 
                infraName = "Школа";
                break;
            case "university": 
                c.setHasUniversity(true); 
                infraName = "Университет";
                break;
            default:
                infraName = "";
                break;
        }
        save(c);
        
        // VISUAL EFFECTS: Announce infrastructure construction
        Nation n = nationManager.getNationById(c.getNationId());
        if (n != null && !infraName.isEmpty()) {
            VisualEffectsService effectsService = plugin.getVisualEffectsService();
            final String cityName = c.getName();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String msg = "§a🏗️ " + infraName + " построена в городе '" + cityName + "'!";
                for (UUID citizenId : n.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        if (effectsService != null) {
                            effectsService.sendActionBar(citizen, msg);
                        }
                        // Green particles
                        org.bukkit.Location loc = citizen.getLocation();
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc, 10, 0.5, 1, 0.5, 0.1);
                        citizen.playSound(loc, org.bukkit.Sound.BLOCK_ANVIL_USE, 0.5f, 1.2f);
                    }
                }
            });
        }
    }

    public synchronized List<City> getCitiesOf(String nationId) {
        List<City> out = new ArrayList<>();
        for (City c : cities.values()) if (c.getNationId().equals(nationId)) out.add(c);
        return out;
    }
    
    /**
     * Get all cities (for map boundaries).
     */
    public synchronized Collection<City> getAllCities() {
        return new ArrayList<>(cities.values());
    }

    public void tickGrowth() {
        for (City c : cities.values()) {
            int oldPop = c.getPopulation();
            int newPop = Math.min(oldPop + Math.max(1, oldPop / 20), 1_000_000);
            c.setPopulation(newPop);
            
            // VISUAL EFFECTS: Notify citizens of significant population milestones (every 10k)
            if (oldPop / 10000 != newPop / 10000 && newPop % 10000 < 1000) { // Within first 1000 of new milestone
                Nation n = nationManager.getNationById(c.getNationId());
                if (n != null) {
                    VisualEffectsService effectsService = plugin.getVisualEffectsService();
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        String msg = "§b📈 Население '" + c.getName() + "': " + String.format("%,d", newPop) + " жителей";
                        for (UUID citizenId : n.getCitizens()) {
                            org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                            if (citizen != null && citizen.isOnline()) {
                                if (effectsService != null) {
                                    effectsService.sendActionBar(citizen, msg);
                                }
                                // Subtle particles for population growth
                                org.bukkit.Location loc = citizen.getLocation();
                                loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.05);
                            }
                        }
                    });
                }
            }
            
            // Check for level up (every 10k population = 1 level)
            int oldLevel = oldPop / 10000;
            int newLevel = newPop / 10000;
            if (newLevel > oldLevel && newLevel <= 10) { // Max level 10
                c.setLevel(newLevel);
                try {
                    save(c);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to save city after level up: " + e.getMessage());
                }
                
                // VISUAL EFFECTS: City level up!
                Nation n = nationManager.getNationById(c.getNationId());
                if (n != null) {
                    VisualEffectsService effectsService = plugin.getVisualEffectsService();
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        String msg = "§b🏙️ Город '" + c.getName() + "' достиг уровня " + newLevel + "!";
                        for (UUID citizenId : n.getCitizens()) {
                            org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                            if (citizen != null && citizen.isOnline()) {
                                citizen.sendTitle("§b§l[УРОВЕНЬ ГОРОДА]", "§f" + c.getName() + " - Уровень " + newLevel, 10, 60, 10);
                                if (effectsService != null) {
                                    effectsService.sendActionBar(citizen, msg);
                                }
                                // Gold particles for level up
                                org.bukkit.Location loc = citizen.getLocation();
                                for (int i = 0; i < 20; i++) {
                                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.1);
                                }
                                citizen.playSound(loc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                            }
                        }
                    });
                }
            }
        }
    }

    private void loadAll() {
        File[] files = citiesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                City c = new City(
                    o.get("id").getAsString(),
                    o.get("name").getAsString(),
                    o.get("nationId").getAsString(),
                    o.get("centerChunk").getAsString()
                );
                c.setLevel(o.has("level") ? o.get("level").getAsInt() : 1);
                c.setPopulation(o.has("population") ? o.get("population").getAsInt() : 10);
                if (o.has("hasHospital")) c.setHasHospital(o.get("hasHospital").getAsBoolean());
                if (o.has("hasSchool")) c.setHasSchool(o.get("hasSchool").getAsBoolean());
                if (o.has("hasUniversity")) c.setHasUniversity(o.get("hasUniversity").getAsBoolean());
                if (o.has("happiness")) c.setHappiness(o.get("happiness").getAsDouble());
                cities.put(c.getId(), c);
            } catch (Exception ignored) {}
        }
    }

    private void save(City c) throws IOException {
        File f = new File(citiesDir, c.getId() + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", c.getId());
        o.addProperty("name", c.getName());
        o.addProperty("nationId", c.getNationId());
        o.addProperty("level", c.getLevel());
        o.addProperty("population", c.getPopulation());
        o.addProperty("centerChunk", c.getCenterChunk());
        o.addProperty("hasHospital", c.hasHospital());
        o.addProperty("hasSchool", c.hasSchool());
        o.addProperty("hasUniversity", c.hasUniversity());
        o.addProperty("happiness", c.getHappiness());
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) { w.write(o.toString()); }
    }
    
    /**
     * Get city by ID.
     */
    public synchronized City getCity(String cityId) {
        return cities.get(cityId);
    }
    
    /**
     * Get city statistics.
     */
    public synchronized Map<String, Object> getCityStatistics(String cityId) {
        City city = cities.get(cityId);
        if (city == null) return Collections.emptyMap();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("id", city.getId());
        stats.put("name", city.getName());
        stats.put("level", city.getLevel());
        stats.put("population", city.getPopulation());
        stats.put("happiness", city.getHappiness());
        stats.put("hasHospital", city.hasHospital());
        stats.put("hasSchool", city.hasSchool());
        stats.put("hasUniversity", city.hasUniversity());
        
        // Calculate city productivity based on infrastructure
        double productivity = 1.0;
        if (city.hasHospital()) productivity += 0.1; // +10% productivity
        if (city.hasSchool()) productivity += 0.15; // +15% productivity
        if (city.hasUniversity()) productivity += 0.2; // +20% productivity
        
        // Level bonus
        productivity += city.getLevel() * 0.05; // +5% per level
        
        stats.put("productivity", productivity);
        
        // Population density (for future features)
        stats.put("populationDensity", calculatePopulationDensity(city));
        
        return stats;
    }
    
    /**
     * Calculate population density (simplified).
     */
    private double calculatePopulationDensity(City city) {
        // Based on city level (level 1 = 1 chunk, level 5+ = 5x5 chunks)
        int chunks = city.getLevel() >= 5 ? 25 : (city.getLevel() >= 3 ? 9 : 1);
        return city.getPopulation() / (double) chunks;
    }
    
    /**
     * Upgrade city infrastructure.
     */
    public synchronized String upgradeInfrastructure(String cityId, String type, double cost) throws IOException {
        City city = cities.get(cityId);
        if (city == null) return "Город не найден.";
        
        Nation nation = nationManager.getNationById(city.getNationId());
        if (nation == null) return "Нация не найдена.";
        if (nation.getTreasury() < cost) return "Недостаточно средств.";
        
        boolean alreadyBuilt = false;
        switch (type.toLowerCase()) {
            case "hospital":
                alreadyBuilt = city.hasHospital();
                city.setHasHospital(true);
                break;
            case "school":
                alreadyBuilt = city.hasSchool();
                city.setHasSchool(true);
                break;
            case "university":
                alreadyBuilt = city.hasUniversity();
                city.setHasUniversity(true);
                break;
            default:
                return "Неизвестный тип инфраструктуры.";
        }
        
        if (alreadyBuilt) return "Инфраструктура уже построена.";
        
        nation.setTreasury(nation.getTreasury() - cost);
        save(city);
        nationManager.save(nation);
        
        // VISUAL EFFECTS
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String infraName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
            String msg = "§a🏗️ " + infraName + " построена в '" + city.getName() + "'!";
            for (UUID citizenId : nation.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc, 15, 0.5, 1, 0.5, 0.1);
                    citizen.playSound(loc, org.bukkit.Sound.BLOCK_ANVIL_USE, 0.6f, 1.3f);
                }
            }
        });
        
        return "Инфраструктура построена.";
    }
    
    /**
     * Calculate city income (based on population and infrastructure).
     */
    public synchronized double calculateCityIncome(String cityId) {
        City city = cities.get(cityId);
        if (city == null) return 0.0;
        
        double baseIncome = city.getPopulation() * 0.1; // 0.1 per citizen
        
        // Infrastructure bonuses
        if (city.hasHospital()) baseIncome *= 1.1;
        if (city.hasSchool()) baseIncome *= 1.15;
        if (city.hasUniversity()) baseIncome *= 1.2;
        
        // Level bonus
        baseIncome *= (1.0 + city.getLevel() * 0.05);
        
        // Happiness modifier
        baseIncome *= (0.5 + city.getHappiness() / 100.0);
        
        return baseIncome;
    }
    
    /**
     * Get all cities with statistics.
     */
    public synchronized Map<String, Map<String, Object>> getAllCitiesStatistics() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (City city : cities.values()) {
            result.put(city.getId(), getCityStatistics(city.getId()));
        }
        return result;
    }
    
    /**
     * Rename city.
     */
    public synchronized String renameCity(String cityId, String newName) throws IOException {
        City city = cities.get(cityId);
        if (city == null) return "Город не найден.";
        
        String oldName = city.getName();
        city.setName(newName);
        save(city);
        
        Nation nation = nationManager.getNationById(city.getNationId());
        if (nation != null) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String msg = "§e🏙️ Город '" + oldName + "' переименован в '" + newName + "'";
                for (UUID citizenId : nation.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                    }
                }
            });
        }
        
        return "Город переименован.";
    }
    
    /**
     * Get total population of all cities for a nation.
     */
    public synchronized int getTotalPopulation(String nationId) {
        return getCitiesOf(nationId).stream()
            .mapToInt(City::getPopulation)
            .sum();
    }
    
    /**
     * Get top cities by population.
     */
    public synchronized List<City> getTopCitiesByPopulation(int limit) {
        return cities.values().stream()
            .sorted((a, b) -> Integer.compare(b.getPopulation(), a.getPopulation()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get global city growth statistics.
     */
    public synchronized Map<String, Object> getGlobalCityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalCities", cities.size());
        
        // Population statistics
        int totalPopulation = 0;
        int maxPopulation = 0;
        int minPopulation = Integer.MAX_VALUE;
        int totalLevel = 0;
        
        for (City city : cities.values()) {
            int pop = city.getPopulation();
            totalPopulation += pop;
            maxPopulation = Math.max(maxPopulation, pop);
            minPopulation = Math.min(minPopulation, pop);
            totalLevel += city.getLevel();
        }
        
        stats.put("totalPopulation", totalPopulation);
        stats.put("maxPopulation", maxPopulation);
        stats.put("minPopulation", minPopulation == Integer.MAX_VALUE ? 0 : minPopulation);
        stats.put("averagePopulation", cities.size() > 0 ? (double) totalPopulation / cities.size() : 0);
        stats.put("averageLevel", cities.size() > 0 ? (double) totalLevel / cities.size() : 0);
        
        // Infrastructure statistics
        int hospitals = 0;
        int schools = 0;
        int universities = 0;
        double totalHappiness = 0.0;
        
        for (City city : cities.values()) {
            if (city.hasHospital()) hospitals++;
            if (city.hasSchool()) schools++;
            if (city.hasUniversity()) universities++;
            totalHappiness += city.getHappiness();
        }
        
        stats.put("totalHospitals", hospitals);
        stats.put("totalSchools", schools);
        stats.put("totalUniversities", universities);
        stats.put("averageHappiness", cities.size() > 0 ? totalHappiness / cities.size() : 0);
        
        // Level distribution
        Map<Integer, Integer> levelDistribution = new HashMap<>();
        for (City city : cities.values()) {
            levelDistribution.put(city.getLevel(), levelDistribution.getOrDefault(city.getLevel(), 0) + 1);
        }
        stats.put("levelDistribution", levelDistribution);
        
        // Top cities
        List<City> topCities = getTopCitiesByPopulation(10);
        List<Map<String, Object>> topCitiesData = new ArrayList<>();
        for (City city : topCities) {
            Map<String, Object> cityData = new HashMap<>();
            cityData.put("id", city.getId());
            cityData.put("name", city.getName());
            cityData.put("population", city.getPopulation());
            cityData.put("level", city.getLevel());
            cityData.put("nationId", city.getNationId());
            topCitiesData.add(cityData);
        }
        stats.put("topCitiesByPopulation", topCitiesData);
        
        return stats;
    }
    
    /**
     * Get cities by nation with statistics.
     */
    public synchronized Map<String, Map<String, Object>> getNationCitiesStatistics(String nationId) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (City city : getCitiesOf(nationId)) {
            result.put(city.getId(), getCityStatistics(city.getId()));
        }
        return result;
    }
}


