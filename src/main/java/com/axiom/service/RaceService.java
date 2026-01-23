package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages player races and racial bonuses. */
public class RaceService {
    private final AXIOM plugin;
    private final File racesDir;
    private final Map<String, Race> races = new HashMap<>(); // raceId -> race
    private final Map<UUID, String> playerRaces = new HashMap<>(); // playerUUID -> raceId

    public static class Race {
        String id;
        String name;
        Map<String, Double> bonuses = new HashMap<>(); // type -> value
        List<String> abilities = new ArrayList<>();
        String description;
    }

    public RaceService(AXIOM plugin) {
        this.plugin = plugin;
        this.racesDir = new File(plugin.getDataFolder(), "races");
        this.racesDir.mkdirs();
        initializeDefaultRaces();
        loadPlayerRaces();
    }

    private void initializeDefaultRaces() {
        // Human - balanced
        Race human = new Race();
        human.id = "human";
        human.name = "Человек";
        human.bonuses.put("tradeBonus", 1.1); // +10% trade
        human.abilities.add("Adaptability");
        human.description = "Адаптивные и универсальные";
        races.put(human.id, human);

        // Elf - magic/agility
        Race elf = new Race();
        elf.id = "elf";
        elf.name = "Эльф";
        elf.bonuses.put("researchBonus", 1.15); // +15% research
        elf.bonuses.put("cultureBonus", 1.2); // +20% culture
        elf.abilities.add("Long Life");
        elf.abilities.add("Natural Magic");
        elf.description = "Долгоживущие и интеллектуальные";
        races.put(elf.id, elf);

        // Dwarf - mining/crafting
        Race dwarf = new Race();
        dwarf.id = "dwarf";
        dwarf.name = "Дварф";
        dwarf.bonuses.put("miningBonus", 1.25); // +25% mining
        dwarf.bonuses.put("defenseBonus", 1.15); // +15% defense
        dwarf.abilities.add("Underground Vision");
        dwarf.abilities.add("Crafting Mastery");
        dwarf.description = "Искусные мастера и защитники";
        races.put(dwarf.id, dwarf);

        // Orc - combat
        Race orc = new Race();
        orc.id = "orc";
        orc.name = "Орк";
        orc.bonuses.put("combatBonus", 1.2); // +20% combat
        orc.bonuses.put("strengthBonus", 1.3); // +30% strength
        orc.abilities.add("Berserker");
        orc.abilities.add("Warrior Culture");
        orc.description = "Свирепые воины";
        races.put(orc.id, orc);

        // Goblin - trade/stealth
        Race goblin = new Race();
        goblin.id = "goblin";
        goblin.name = "Гоблин";
        goblin.bonuses.put("tradeBonus", 1.3); // +30% trade
        goblin.bonuses.put("stealthBonus", 1.25); // +25% stealth
        goblin.abilities.add("Merchant");
        goblin.abilities.add("Sneaky");
        goblin.description = "Хитрые торговцы";
        races.put(goblin.id, goblin);

        saveRace(human);
        saveRace(elf);
        saveRace(dwarf);
        saveRace(orc);
        saveRace(goblin);
    }

    public synchronized String setPlayerRace(UUID playerId, String raceId) {
        Race race = races.get(raceId);
        if (race == null) return "Раса не найдена.";
        playerRaces.put(playerId, raceId);
        savePlayerRace(playerId, raceId);
        return "Раса установлена: " + race.name;
    }

    public synchronized String getPlayerRace(UUID playerId) {
        return playerRaces.getOrDefault(playerId, "human"); // Default to human
    }

    public synchronized Race getRace(String raceId) {
        return races.get(raceId);
    }

    public synchronized double getBonus(UUID playerId, String bonusType) {
        String raceId = getPlayerRace(playerId);
        Race race = races.get(raceId);
        if (race == null) return 1.0;
        return race.bonuses.getOrDefault(bonusType, 1.0);
    }

    private void loadPlayerRaces() {
        File f = new File(racesDir, "players.json");
        if (!f.exists()) return;
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            for (var entry : o.entrySet()) {
                playerRaces.put(UUID.fromString(entry.getKey()), entry.getValue().getAsString());
            }
        } catch (Exception ignored) {}
    }

    private void savePlayerRace(UUID playerId, String raceId) {
        File f = new File(racesDir, "players.json");
        JsonObject o = new JsonObject();
        for (var entry : playerRaces.entrySet()) {
            o.addProperty(entry.getKey().toString(), entry.getValue());
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }

    private void saveRace(Race race) {
        File f = new File(racesDir, race.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", race.id);
        o.addProperty("name", race.name);
        o.addProperty("description", race.description);
        JsonObject bonuses = new JsonObject();
        for (var entry : race.bonuses.entrySet()) {
            bonuses.addProperty(entry.getKey(), entry.getValue());
        }
        o.add("bonuses", bonuses);
        com.google.gson.JsonArray abilities = new com.google.gson.JsonArray();
        for (String ability : race.abilities) {
            abilities.add(ability);
        }
        o.add("abilities", abilities);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }

    public synchronized List<Race> getAllRaces() {
        return new ArrayList<>(races.values());
    }
    
    /**
     * Get comprehensive race statistics.
     */
    public synchronized Map<String, Object> getRaceStatistics(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        String raceId = getPlayerRace(playerId);
        Race race = getRace(raceId);
        
        if (race != null) {
            stats.put("raceId", race.id);
            stats.put("raceName", race.name);
            stats.put("description", race.description);
            stats.put("bonuses", new HashMap<>(race.bonuses));
            stats.put("abilities", new ArrayList<>(race.abilities));
        }
        
        // Race distribution (how many players per race)
        Map<String, Integer> raceDistribution = new HashMap<>();
        for (String r : playerRaces.values()) {
            raceDistribution.put(r, raceDistribution.getOrDefault(r, 0) + 1);
        }
        stats.put("serverRaceDistribution", raceDistribution);
        
        // Calculate total bonuses for player
        Map<String, Double> totalBonuses = new HashMap<>();
        if (race != null) {
            for (Map.Entry<String, Double> entry : race.bonuses.entrySet()) {
                totalBonuses.put(entry.getKey(), entry.getValue());
            }
        }
        stats.put("totalBonuses", totalBonuses);
        
        return stats;
    }
    
    /**
     * Get race popularity (most common race).
     */
    public synchronized String getMostPopularRace() {
        Map<String, Integer> distribution = new HashMap<>();
        for (String raceId : playerRaces.values()) {
            distribution.put(raceId, distribution.getOrDefault(raceId, 0) + 1);
        }
        
        if (distribution.isEmpty()) return "human"; // Default
        
        return distribution.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("human");
    }
    
    /**
     * Calculate nation racial diversity (bonus from having diverse races).
     */
    public synchronized double getNationRacialDiversity(String nationId) {
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return 1.0;
        
        Set<String> uniqueRaces = new HashSet<>();
        for (UUID citizenId : n.getCitizens()) {
            String raceId = getPlayerRace(citizenId);
            uniqueRaces.add(raceId);
        }
        
        // +2% bonus per unique race (capped at +10%)
        double diversityBonus = Math.min(0.10, uniqueRaces.size() * 0.02);
        return 1.0 + diversityBonus;
    }
    
    /**
     * Get all players of a specific race.
     */
    public synchronized List<UUID> getPlayersOfRace(String raceId) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerRaces.entrySet()) {
            if (entry.getValue().equals(raceId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Get global race statistics across all players.
     */
    public synchronized Map<String, Object> getGlobalRaceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRaces", races.size());
        stats.put("totalPlayersWithRace", playerRaces.size());
        
        // Race distribution
        Map<String, Integer> raceDistribution = new HashMap<>();
        for (String raceId : playerRaces.values()) {
            raceDistribution.put(raceId, raceDistribution.getOrDefault(raceId, 0) + 1);
        }
        stats.put("raceDistribution", raceDistribution);
        
        // Calculate percentages
        Map<String, Double> racePercentages = new HashMap<>();
        int totalPlayers = playerRaces.size();
        for (Map.Entry<String, Integer> entry : raceDistribution.entrySet()) {
            double percentage = totalPlayers > 0 ? (entry.getValue() / (double) totalPlayers) * 100 : 0;
            racePercentages.put(entry.getKey(), percentage);
        }
        stats.put("racePercentages", racePercentages);
        
        // Most popular race
        String mostPopular = getMostPopularRace();
        stats.put("mostPopularRace", mostPopular);
        stats.put("mostPopularRaceCount", raceDistribution.getOrDefault(mostPopular, 0));
        
        // Race bonuses summary
        Map<String, Map<String, Double>> raceBonuses = new HashMap<>();
        for (Race race : races.values()) {
            raceBonuses.put(race.id, new HashMap<>(race.bonuses));
        }
        stats.put("raceBonuses", raceBonuses);
        
        // Top races by player count
        List<Map.Entry<String, Integer>> topRaces = raceDistribution.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topRaces", topRaces);
        
        // Average diversity across nations
        double totalDiversity = 0.0;
        int nationsWithPlayers = 0;
        for (com.axiom.model.Nation n : plugin.getNationManager().getAll()) {
            if (!n.getCitizens().isEmpty()) {
                Set<String> uniqueRaces = new HashSet<>();
                for (UUID citizenId : n.getCitizens()) {
                    String raceId = getPlayerRace(citizenId);
                    uniqueRaces.add(raceId);
                }
                totalDiversity += uniqueRaces.size();
                nationsWithPlayers++;
            }
        }
        stats.put("averageRacialDiversityPerNation", nationsWithPlayers > 0 ? 
            totalDiversity / nationsWithPlayers : 0);
        
        // Races by bonuses (most powerful)
        List<Map.Entry<String, Double>> racesByPower = new ArrayList<>();
        for (Race race : races.values()) {
            double totalPower = race.bonuses.values().stream().mapToDouble(Double::doubleValue).sum();
            racesByPower.add(new java.util.AbstractMap.SimpleEntry<>(race.id, totalPower));
        }
        racesByPower.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("racesByPower", racesByPower);
        
        return stats;
    }
}

