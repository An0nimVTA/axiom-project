package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.repo.NationJsonCodec;
import com.google.gson.*;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.military.AdvancedWarSystem;

/**
 * Manages nations, claims, and persistence under plugins/AXIOM/nations
 */
public class NationManager {
    private final AXIOM plugin;
    private final File nationsDir;
    private final Gson gson;
    private final Map<String, Nation> idToNation = new HashMap<>();
    private final Map<String, Long> unclaimCooldownUntil = new HashMap<>(); // key: world:x:z

    public NationManager(AXIOM plugin) {
        this.plugin = plugin;
        this.nationsDir = new File(plugin.getDataFolder(), "nations");
        this.nationsDir.mkdirs();
        GsonBuilder builder = new GsonBuilder();
        if (plugin.getConfig().getBoolean("storage.prettyPrintJson", true)) {
            builder.setPrettyPrinting();
        }
        this.gson = builder.create();
        loadAll();
    }

    public synchronized Nation createNation(Player founder, String name, String currencyCode, double startingTreasury) throws IOException {
        String id = sanitizeId(name);
        if (idToNation.containsKey(id)) throw new IOException("Нация с таким id уже существует: " + id);
        Nation nation = new Nation(id, name, founder.getUniqueId(), currencyCode, startingTreasury);
        // Initial capital and claim
        Chunk chunk = founder.getLocation().getChunk();
        String key = chunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        nation.setCapitalChunkStr(key);
        nation.getClaimedChunkKeys().add(key);
        TerritoryService territoryService = plugin.getTerritoryService();
        if (territoryService != null) {
            territoryService.claim(nation.getId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        }
        long timestamp = System.currentTimeMillis();
        nation.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Нация основана лидером " + founder.getName());
        idToNation.put(id, nation);
        save(nation);
        // Grant achievement
        plugin.getAchievementService().grantAchievement(founder.getUniqueId(), "founder");
        return nation;
    }


    public synchronized void save(Nation nation) throws IOException {
        File f = new File(nationsDir, nation.getId() + ".json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            gson.toJson(NationJsonCodec.serialize(nation), writer);
        }
    }

    public synchronized void flush() throws IOException {
        for (Nation n : idToNation.values()) {
            save(n);
        }
    }

    public synchronized Nation getNationById(String id) { return idToNation.get(id); }
    public synchronized java.util.Collection<Nation> getAll() { return new java.util.ArrayList<>(idToNation.values()); }

    public synchronized Optional<Nation> getNationOfPlayer(UUID uuid) {
        return idToNation.values().stream().filter(n -> n.isMember(uuid)).findFirst();
    }

    public synchronized Nation getPlayerNation(UUID playerId) {
        return getNationOfPlayer(playerId).orElse(null);
    }

    public synchronized String getPlayerNationId(UUID playerId) {
        Nation nation = getPlayerNation(playerId);
        return nation != null ? nation.getId() : null;
    }

    /**
     * Compatibility alias for getPlayerNationId
     */
    public synchronized String getNationIdOfPlayer(UUID playerId) {
        return getPlayerNationId(playerId);
    }

    public synchronized Optional<Nation> getNationClaiming(World world, int chunkX, int chunkZ) {
        TerritoryService territoryService = plugin.getTerritoryService();
        if (territoryService != null) {
            String nationId = territoryService.getNationAt(world.getName(), chunkX, chunkZ);
            return Optional.ofNullable(nationId == null ? null : getNationById(nationId));
        }
        String key = chunkKey(world.getName(), chunkX, chunkZ);
        return idToNation.values().stream().filter(n -> n.getClaimedChunkKeys().contains(key)).findFirst();
    }

    public synchronized String getNationIdAtLocation(World world, int chunkX, int chunkZ) {
        return getNationClaiming(world, chunkX, chunkZ).map(Nation::getId).orElse(null);
    }

    public synchronized java.util.Collection<Nation> getAllNations() {
        return getAll();
    }

    private void loadAll() {
        File[] files = nationsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                Nation n = NationJsonCodec.deserialize(o,
                    plugin.getConfig().getString("economy.defaultCurrencyCode", "AXC"));
                idToNation.put(n.getId(), n);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load nation file " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    public synchronized String claimChunk(Player actor) throws IOException {
        Optional<Nation> opt = getNationOfPlayer(actor.getUniqueId());
        if (opt.isEmpty()) return "§cВы не в нации.";
        Nation nation = opt.get();
        Nation.Role role = nation.getRole(actor.getUniqueId());
        if (role != Nation.Role.LEADER && role != Nation.Role.GOVERNOR) return "§cНедостаточно прав.";

        Chunk c = actor.getLocation().getChunk();
        String key = chunkKey(c.getWorld().getName(), c.getX(), c.getZ());
        if (getNationClaiming(c.getWorld(), c.getX(), c.getZ()).isPresent()) return "§cЧанк уже занят.";
        Long until = unclaimCooldownUntil.get(key);
        if (until != null && until > System.currentTimeMillis()) return "§cЧанк в восстановлении. Повторите позже.";
        
        // ANTI-GRIEF: Check multi-world and distance restrictions
        if (plugin.getBalancingService() != null) {
            if (!plugin.getBalancingService().canClaimChunk(nation.getId(), c.getWorld().getName(), c.getX(), c.getZ())) {
                return "§cЧанк слишком далеко от столицы или требует инфраструктуру.";
            }
        }
        
        // Multi-world check
        if (plugin.getMultiWorldService() != null) {
            if (!plugin.getMultiWorldService().canClaimInWorld(c.getWorld().getName())) {
                return "§cКлейм в этом мире запрещён.";
            }
        }

        int current = nation.getClaimedChunkKeys().size();
        int population = nation.getCitizens().size();
        int maxClaims = 100 + (population * 2);
        if (current >= maxClaims) return "§cДостигнут лимит клеймов.";
        double cost = current < 5 ? 0 : 100.0 * current;
        if (cost > 0 && nation.getTreasury() < cost) return "§cНедостаточно средств. Нужно: " + cost + ".";
        if (cost > 0) nation.setTreasury(nation.getTreasury() - cost);
        nation.getClaimedChunkKeys().add(key);
        save(nation);

        TerritoryService territoryService = plugin.getTerritoryService();
        if (territoryService != null) {
            territoryService.claim(nation.getId(), c.getWorld().getName(), c.getX(), c.getZ());
        }
        
        // Update map boundaries
        if (plugin.getMapBoundaryService() != null) {
            plugin.getMapBoundaryService().forceUpdate();
        }
        
        // VISUAL EFFECTS: Successful claim
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getVisualEffectsService().sendActionBar(actor, "§a✓ Территория захвачена!");
            org.bukkit.Location loc = actor.getLocation();
            // Green particles
            for (int i = 0; i < 15; i++) {
                loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.1);
            }
            actor.playSound(loc, org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.2f);
        });
        
        return cost == 0 ? "§aЧанк клеймнут. Бесплатно." : "§aЧанк клеймнут. Списано: " + cost + ".";
    }

    public synchronized String unclaimChunk(Player actor) throws IOException {
        Optional<Nation> opt = getNationOfPlayer(actor.getUniqueId());
        if (opt.isEmpty()) return "§cВы не в нации.";
        Nation nation = opt.get();
        Nation.Role role = nation.getRole(actor.getUniqueId());
        if (role != Nation.Role.LEADER && role != Nation.Role.GOVERNOR) return "§cНедостаточно прав.";
        Chunk c = actor.getLocation().getChunk();
        String key = chunkKey(c.getWorld().getName(), c.getX(), c.getZ());
        if (!nation.getClaimedChunkKeys().contains(key)) return "§cЭтот чанк не принадлежит вашей нации.";
        nation.getClaimedChunkKeys().remove(key);
        unclaimCooldownUntil.put(key, System.currentTimeMillis() + 5 * 60_000L);
        save(nation);

        TerritoryService territoryService = plugin.getTerritoryService();
        if (territoryService != null) {
            territoryService.unclaim(nation.getId(), c.getWorld().getName(), c.getX(), c.getZ());
        }
        
        // Update map boundaries
        if (plugin.getMapBoundaryService() != null) {
            plugin.getMapBoundaryService().forceUpdate();
        }
        
        // VISUAL EFFECTS: Successful unclaim
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getVisualEffectsService().sendActionBar(actor, "§e⚠ Территория освобождена (восстановление: 5 мин)");
            org.bukkit.Location loc = actor.getLocation();
            // Orange particles
            loc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, loc.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0,
                new org.bukkit.Particle.DustOptions(org.bukkit.Color.ORANGE, 1.5f));
            actor.playSound(loc, org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 0.9f);
        });
        
        return "§eАнклейм. Восстановление: 5 минут.";
    }

    private String chunkKey(String world, int x, int z) { return world + ":" + x + ":" + z; }
    private String sanitizeId(String name) { return name.toLowerCase().replaceAll("[^a-z0-9_-]", "-"); }
    
    /**
     * Get comprehensive nation statistics.
     */
    public synchronized Map<String, Object> getNationStatistics(String nationId) {
        Nation nation = getNationById(nationId);
        if (nation == null) return Collections.emptyMap();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("id", nation.getId());
        stats.put("name", nation.getName());
        stats.put("citizens", nation.getCitizens().size());
        stats.put("territories", nation.getClaimedChunkKeys().size());
        stats.put("treasury", nation.getTreasury());
        stats.put("inflation", nation.getInflation());
        stats.put("taxRate", nation.getTaxRate());
        stats.put("allies", nation.getAllies().size());
        stats.put("enemies", nation.getEnemies().size());
        stats.put("governmentType", nation.getGovernmentType());
        
        // Advanced statistics
        if (plugin.getEconomyService() != null) {
            stats.put("gdp", plugin.getEconomyService().getGDP(nationId));
            stats.put("economicHealth", plugin.getEconomyService().getEconomicHealth(nationId));
        }
        
        if (plugin.getMilitaryService() != null) {
            stats.put("militaryStrength", plugin.getMilitaryService().getMilitaryStrength(nationId));
        }
        
        if (plugin.getCityGrowthEngine() != null) {
            stats.put("cities", plugin.getCityGrowthEngine().getCitiesOf(nationId).size());
        }
        
        if (plugin.getAdvancedWarSystem() != null) {
            List<AdvancedWarSystem.War> wars = plugin.getAdvancedWarSystem().getNationWars(nationId);
            stats.put("activeWars", wars.size());
            Map<String, Object> warStats = plugin.getAdvancedWarSystem().getWarStatistics(nationId);
            stats.putAll(warStats);
        }
        
        // Technology progress
        if (plugin.getTechnologyTreeService() != null) {
            stats.put("unlockedTechnologies", plugin.getTechnologyTreeService().getUnlockedTechs(nationId).size());
        }
        
        return stats;
    }
    
    /**
     * Add citizen to nation with role.
     */
    public synchronized String addCitizen(String nationId, UUID playerId, Nation.Role role) throws IOException {
        Nation nation = getNationById(nationId);
        if (nation == null) return "Нация не найдена.";
        if (nation.isMember(playerId)) return "Игрок уже в нации.";
        
        nation.getCitizens().add(playerId);
        nation.getRoles().put(playerId, role);
        save(nation);
        
        // VISUAL EFFECTS
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendTitle("§a§l[НАЦИЯ]", "§fВы приняты в '" + nation.getName() + "'", 10, 60, 10);
                plugin.getVisualEffectsService().sendActionBar(player, "§a✓ Должность: " + role);
                org.bukkit.Location loc = player.getLocation();
                loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                player.playSound(loc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            });
        }
        
        return "Гражданин добавлен.";
    }
    
    /**
     * Remove citizen from nation.
     */
    public synchronized String removeCitizen(String nationId, UUID playerId) throws IOException {
        Nation nation = getNationById(nationId);
        if (nation == null) return "Нация не найдена.";
        if (!nation.isMember(playerId)) return "Игрок не в нации.";
        if (nation.getLeader().equals(playerId)) return "Нельзя исключить лидера.";
        
        nation.getCitizens().remove(playerId);
        nation.getRoles().remove(playerId);
        save(nation);
        
        return "Гражданин исключён.";
    }
    
    /**
     * Set citizen role.
     */
    public synchronized String setCitizenRole(String nationId, UUID playerId, Nation.Role role) throws IOException {
        Nation nation = getNationById(nationId);
        if (nation == null) return "Нация не найдена.";
        if (!nation.isMember(playerId)) return "Игрок не в нации.";
        
        nation.getRoles().put(playerId, role);
        save(nation);
        
        // VISUAL EFFECTS
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getVisualEffectsService().sendActionBar(player, "§e📋 Новая должность: " + role);
            });
        }
        
        return "Должность изменена.";
    }
    
    /**
     * Transfer leadership.
     */
    public synchronized String transferLeadership(String nationId, UUID newLeaderId) throws IOException {
        Nation nation = getNationById(nationId);
        if (nation == null) return "Нация не найдена.";
        if (!nation.isMember(newLeaderId)) return "Игрок не в нации.";
        
        UUID oldLeader = nation.getLeader();
        nation.setLeader(newLeaderId);
        nation.getRoles().put(oldLeader, Nation.Role.MINISTER);
        nation.getRoles().put(newLeaderId, Nation.Role.LEADER);
        save(nation);
        
        // VISUAL EFFECTS
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player newLeader = org.bukkit.Bukkit.getPlayer(newLeaderId);
            if (newLeader != null && newLeader.isOnline()) {
                newLeader.sendTitle("§a§l[ЛИДЕР]", "§fВы новый лидер '" + nation.getName() + "'", 10, 80, 20);
                plugin.getVisualEffectsService().sendActionBar(newLeader, "§a✓ Вы стали лидером!");
                org.bukkit.Location loc = newLeader.getLocation();
                for (int i = 0; i < 20; i++) {
                    loc.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.05);
                }
                newLeader.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            }
        });
        
        return "Лидерство передано.";
    }
    
    /**
     * Change capital.
     */
    public synchronized String setCapital(Player actor, Chunk newCapital) throws IOException {
        Optional<Nation> opt = getNationOfPlayer(actor.getUniqueId());
        if (opt.isEmpty()) return "Вы не в нации.";
        Nation nation = opt.get();
        if (nation.getRole(actor.getUniqueId()) != Nation.Role.LEADER) return "Только лидер может сменить столицу.";
        
        String key = chunkKey(newCapital.getWorld().getName(), newCapital.getX(), newCapital.getZ());
        if (!nation.getClaimedChunkKeys().contains(key)) return "Столица должна быть в вашей территории.";
        
        nation.setCapitalChunkStr(key);
        save(nation);
        
        // VISUAL EFFECTS
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getVisualEffectsService().sendActionBar(actor, "§a✓ Столица перенесена!");
            org.bukkit.Location loc = actor.getLocation();
            loc.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, loc.add(0, 2, 0), 20, 1, 1, 1, 0.05);
            actor.playSound(loc, org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        });
        
        return "Столица изменена.";
    }
    
    /**
     * Calculate nation power score (0-100).
     */
    public synchronized double calculateNationPower(String nationId) {
        Nation nation = getNationById(nationId);
        if (nation == null) return 0.0;
        
        double score = 0.0;
        
        // Territory power (30%)
        int territories = nation.getClaimedChunkKeys().size();
        score += Math.min(30.0, territories / 10.0);
        
        // Population power (20%)
        int citizens = nation.getCitizens().size();
        score += Math.min(20.0, citizens / 5.0);
        
        // Economic power (25%)
        if (plugin.getEconomyService() != null) {
            double gdp = plugin.getEconomyService().getGDP(nationId);
            score += Math.min(25.0, gdp / 10000.0);
        }
        
        // Military power (15%)
        if (plugin.getMilitaryService() != null) {
            double military = plugin.getMilitaryService().getMilitaryStrength(nationId);
            score += Math.min(15.0, military / 1000.0);
        }
        
        // Technology power (10%)
        if (plugin.getTechnologyTreeService() != null) {
            int techs = plugin.getTechnologyTreeService().getUnlockedTechs(nationId).size();
            score += Math.min(10.0, techs * 0.5);
        }
        
        return Math.min(100.0, score);
    }
    
    /**
     * Get top nations by power.
     */
    public synchronized List<Map.Entry<String, Double>> getTopNations(int limit) {
        Map<String, Double> powers = new HashMap<>();
        for (Nation nation : getAll()) {
            powers.put(nation.getId(), calculateNationPower(nation.getId()));
        }
        
        return powers.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get comprehensive nation manager statistics.
     */
    public synchronized Map<String, Object> getNationManagerStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalNations", getAll().size());
        
        // Population statistics
        int totalCitizens = 0;
        int maxCitizens = 0;
        int minCitizens = Integer.MAX_VALUE;
        for (Nation n : getAll()) {
            int citizens = n.getCitizens().size();
            totalCitizens += citizens;
            maxCitizens = Math.max(maxCitizens, citizens);
            minCitizens = Math.min(minCitizens, citizens);
        }
        stats.put("totalCitizens", totalCitizens);
        stats.put("maxCitizens", maxCitizens);
        stats.put("minCitizens", minCitizens == Integer.MAX_VALUE ? 0 : minCitizens);
        stats.put("averageCitizens", getAll().size() > 0 ? (double) totalCitizens / getAll().size() : 0);
        
        // Territory statistics
        int totalChunks = 0;
        int maxChunks = 0;
        for (Nation n : getAll()) {
            int chunks = n.getClaimedChunkKeys().size();
            totalChunks += chunks;
            maxChunks = Math.max(maxChunks, chunks);
        }
        stats.put("totalChunks", totalChunks);
        stats.put("maxChunks", maxChunks);
        stats.put("averageChunks", getAll().size() > 0 ? (double) totalChunks / getAll().size() : 0);
        
        // Government type distribution
        Map<String, Integer> governmentDistribution = new HashMap<>();
        for (Nation n : getAll()) {
            governmentDistribution.put(n.getGovernmentType(), 
                governmentDistribution.getOrDefault(n.getGovernmentType(), 0) + 1);
        }
        stats.put("governmentDistribution", governmentDistribution);
        
        // Top nations by power
        List<Map.Entry<String, Double>> topNations = getTopNations(10);
        stats.put("topNationsByPower", topNations);
        
        // Economic statistics
        double totalTreasury = 0.0;
        double maxTreasury = 0.0;
        for (Nation n : getAll()) {
            double treasury = n.getTreasury();
            totalTreasury += treasury;
            maxTreasury = Math.max(maxTreasury, treasury);
        }
        stats.put("totalTreasury", totalTreasury);
        stats.put("maxTreasury", maxTreasury);
        stats.put("averageTreasury", getAll().size() > 0 ? totalTreasury / getAll().size() : 0);
        
        // Alliance statistics
        int totalAllies = 0;
        int totalEnemies = 0;
        for (Nation n : getAll()) {
            totalAllies += n.getAllies().size();
            totalEnemies += n.getEnemies().size();
        }
        stats.put("totalAlliances", totalAllies / 2); // Each alliance counted twice
        stats.put("totalEnemyRelations", totalEnemies);
        
        return stats;
    }
    
    /**
     * Get nation power ranking.
     */
    public synchronized List<Map<String, Object>> getNationPowerRanking(int limit) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        List<Map.Entry<String, Double>> topNations = getTopNations(limit);
        
        for (Map.Entry<String, Double> entry : topNations) {
            Map<String, Object> nationData = new HashMap<>();
            Nation nation = getNationById(entry.getKey());
            if (nation != null) {
                nationData.put("nationId", entry.getKey());
                nationData.put("nationName", nation.getName());
                nationData.put("power", entry.getValue());
                nationData.put("citizens", nation.getCitizens().size());
                nationData.put("chunks", nation.getClaimedChunkKeys().size());
                nationData.put("treasury", nation.getTreasury());
                ranking.add(nationData);
            }
        }
        
        return ranking;
    }
    
    /**
     * Get nations by government type.
     */
    public synchronized List<Nation> getNationsByGovernmentType(String governmentType) {
        List<Nation> result = new ArrayList<>();
        for (Nation n : getAll()) {
            if (n.getGovernmentType().equalsIgnoreCase(governmentType)) {
                result.add(n);
            }
        }
        return result;
    }
    
    /**
     * Get largest nations by territory.
     */
    public synchronized List<Nation> getLargestNationsByTerritory(int limit) {
        return getAll().stream()
            .sorted((a, b) -> Integer.compare(b.getClaimedChunkKeys().size(), a.getClaimedChunkKeys().size()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get most populated nations.
     */
    public synchronized List<Nation> getMostPopulatedNations(int limit) {
        return getAll().stream()
            .sorted((a, b) -> Integer.compare(b.getCitizens().size(), a.getCitizens().size()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get global nation statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalNationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Collection<Nation> allNations = getAll();
        stats.put("totalNations", allNations.size());
        
        if (allNations.isEmpty()) {
            return stats;
        }
        
        // Territory statistics
        int totalChunks = 0;
        int maxChunks = 0;
        int minChunks = Integer.MAX_VALUE;
        Map<String, Integer> chunksByNation = new HashMap<>();
        
        // Population statistics
        int totalCitizens = 0;
        int maxCitizens = 0;
        int minCitizens = Integer.MAX_VALUE;
        Map<String, Integer> citizensByNation = new HashMap<>();
        
        // Treasury statistics
        double totalTreasury = 0.0;
        double maxTreasury = 0.0;
        double minTreasury = Double.MAX_VALUE;
        
        // Government types
        Map<String, Integer> governmentTypes = new HashMap<>();
        
        // Currency codes
        Map<String, Integer> currencyCodes = new HashMap<>();
        
        // Leaders
        Set<UUID> allLeaders = new HashSet<>();
        
        for (Nation n : allNations) {
            int chunks = n.getClaimedChunkKeys().size();
            int citizens = n.getCitizens().size();
            double treasury = n.getTreasury();
            
            totalChunks += chunks;
            maxChunks = Math.max(maxChunks, chunks);
            minChunks = Math.min(minChunks, chunks);
            chunksByNation.put(n.getId(), chunks);
            
            totalCitizens += citizens;
            maxCitizens = Math.max(maxCitizens, citizens);
            minCitizens = Math.min(minCitizens, citizens);
            citizensByNation.put(n.getId(), citizens);
            
            totalTreasury += treasury;
            maxTreasury = Math.max(maxTreasury, treasury);
            minTreasury = Math.min(minTreasury, treasury);
            
            governmentTypes.put(n.getGovernmentType(), governmentTypes.getOrDefault(n.getGovernmentType(), 0) + 1);
            currencyCodes.put(n.getCurrencyCode(), currencyCodes.getOrDefault(n.getCurrencyCode(), 0) + 1);
            
            UUID leader = n.getLeader();
            if (leader != null) {
                allLeaders.add(leader);
            }
        }
        
        stats.put("totalChunks", totalChunks);
        stats.put("averageChunks", (double) totalChunks / allNations.size());
        stats.put("maxChunks", maxChunks);
        stats.put("minChunks", minChunks == Integer.MAX_VALUE ? 0 : minChunks);
        
        stats.put("totalCitizens", totalCitizens);
        stats.put("averageCitizens", (double) totalCitizens / allNations.size());
        stats.put("maxCitizens", maxCitizens);
        stats.put("minCitizens", minCitizens == Integer.MAX_VALUE ? 0 : minCitizens);
        
        stats.put("totalTreasury", totalTreasury);
        stats.put("averageTreasury", totalTreasury / allNations.size());
        stats.put("maxTreasury", maxTreasury);
        stats.put("minTreasury", minTreasury == Double.MAX_VALUE ? 0 : minTreasury);
        
        stats.put("governmentTypes", governmentTypes);
        stats.put("currencyCodes", currencyCodes);
        stats.put("uniqueLeaders", allLeaders.size());
        
        // Top nations by chunks
        List<Map.Entry<String, Integer>> topByChunks = chunksByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByChunks", topByChunks);
        
        // Top nations by citizens
        List<Map.Entry<String, Integer>> topByCitizens = citizensByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCitizens", topByCitizens);
        
        // Top nations by treasury
        List<Map.Entry<String, Double>> topByTreasury = new ArrayList<>();
        for (Nation n : allNations) {
            topByTreasury.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), n.getTreasury()));
        }
        topByTreasury.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByTreasury", topByTreasury.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }

    /**
     * Create nation without player actor (for testing/admin)
     */
    public synchronized Nation createNation(String name, UUID leaderId) throws IOException {
        String id = sanitizeId(name);
        if (idToNation.containsKey(id)) throw new IOException("Nation exists: " + id);
        Nation nation = new Nation(id, name, leaderId, "AXC", 1000.0);
        // Default capital
        nation.setCapitalChunkStr("world:0:0"); 
        nation.getClaimedChunkKeys().add("world:0:0");
        idToNation.put(id, nation);
        save(nation);
        return nation;
    }
}


