package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.*;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Manages nations, claims, and persistence under plugins/AXIOM/nations
 */
public class NationManager {
    private final File nationsDir;
    private final Gson gson;
    private final Map<String, Nation> idToNation = new HashMap<>();
    private final Map<String, Long> unclaimCooldownUntil = new HashMap<>(); // key: world:x:z

    public NationManager(AXIOM plugin) {
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
        long timestamp = System.currentTimeMillis();
        nation.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Нация основана лидером " + founder.getName());
        idToNation.put(id, nation);
        save(nation);
        
        ServiceLocator.get(AchievementService.class).grantAchievement(founder.getUniqueId(), "founder");

        // Update Dynmap
        // MapIntegrationService mapService = ServiceLocator.get(MapIntegrationService.class);
        if (false) {
            // mapService.updateNationTerritory(nation);
        }

        return nation;
    }


    public synchronized void save(Nation nation) throws IOException {
        File f = new File(nationsDir, nation.getId() + ".json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            writer.write(serializeNation(nation).toString());
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

    public synchronized Optional<Nation> getNationClaiming(World world, int chunkX, int chunkZ) {
        String key = chunkKey(world.getName(), chunkX, chunkZ);
        return idToNation.values().stream().filter(n -> n.getClaimedChunkKeys().contains(key)).findFirst();
    }

    private void loadAll() {
        File[] files = nationsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                Nation n = deserializeNation(o);
                idToNation.put(n.getId(), n);
            } catch (Exception e) {
                ServiceLocator.get(AXIOM.class).getLogger().severe("Failed to load nation file " + f.getName() + ": " + e.getMessage());
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
        
        BalancingService balancingService = ServiceLocator.get(BalancingService.class);
        if (balancingService != null) {
            if (!balancingService.canClaimChunk(nation.getId(), c.getWorld().getName(), c.getX(), c.getZ())) {
                return "§cЧанк слишком далеко от столицы или требует инфраструктуру.";
            }
        }
        
        MultiWorldService multiWorldService = ServiceLocator.get(MultiWorldService.class);
        if (multiWorldService != null) {
            if (!multiWorldService.canClaimInWorld(c.getWorld().getName())) {
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

        // MapIntegrationService mapService = ServiceLocator.get(MapIntegrationService.class);
        if (false) {
            // mapService.updateNationTerritory(nation);
        }
        
        MapBoundaryService mapBoundaryService = ServiceLocator.get(MapBoundaryService.class);
        if (mapBoundaryService != null) {
            mapBoundaryService.forceUpdate();
        }
        
        org.bukkit.Bukkit.getScheduler().runTask(ServiceLocator.get(AXIOM.class), () -> {
            ServiceLocator.get(VisualEffectsService.class).sendActionBar(actor, "§a✓ Территория захвачена!");
            org.bukkit.Location loc = actor.getLocation();
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

        // MapIntegrationService mapService = ServiceLocator.get(MapIntegrationService.class);
        if (false) {
            // mapService.updateNationTerritory(nation);
        }
        
        MapBoundaryService mapBoundaryService = ServiceLocator.get(MapBoundaryService.class);
        if (mapBoundaryService != null) {
            mapBoundaryService.forceUpdate();
        }
        
        org.bukkit.Bukkit.getScheduler().runTask(ServiceLocator.get(AXIOM.class), () -> {
            ServiceLocator.get(VisualEffectsService.class).sendActionBar(actor, "§e⚠ Территория освобождена (восстановление: 5 мин)");
            org.bukkit.Location loc = actor.getLocation();
            loc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, loc.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0,
                new org.bukkit.Particle.DustOptions(org.bukkit.Color.ORANGE, 1.5f));
            actor.playSound(loc, org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 0.9f);
        });
        
        return "§eАнклейм. Восстановление: 5 минут.";
    }

    private String chunkKey(String world, int x, int z) { return world + ":" + x + ":" + z; }
    private String sanitizeId(String name) { return name.toLowerCase().replaceAll("[^a-z0-9_-]", "-"); }

    private JsonObject serializeNation(Nation n) {
        JsonObject o = new JsonObject();
        o.addProperty("id", n.getId());
        o.addProperty("name", n.getName());
        o.addProperty("leader", n.getLeader().toString());
        o.addProperty("capitalChunk", n.getCapitalChunkStr());
        o.addProperty("currency", n.getCurrencyCode());
        o.addProperty("exchangeRateToAXC", n.getExchangeRateToAXC());
        if (n.getMotto() != null) o.addProperty("motto", n.getMotto());
        if (n.getFlagIconMaterial() != null) o.addProperty("flagIcon", n.getFlagIconMaterial());
        o.addProperty("treasury", n.getTreasury());
        JsonArray arr = new JsonArray();
        for (String key : n.getClaimedChunkKeys()) arr.add(key);
        o.add("claimedChunks", arr);
        o.addProperty("inflation", n.getInflation());
        o.addProperty("taxRate", n.getTaxRate());
        JsonArray allies = new JsonArray(); n.getAllies().forEach(allies::add); o.add("allies", allies);
        JsonArray enemies = new JsonArray(); n.getEnemies().forEach(enemies::add); o.add("enemies", enemies);
        if (!n.getTabIcons().isEmpty()) {
            JsonObject tabs = new JsonObject();
            for (var e : n.getTabIcons().entrySet()) tabs.addProperty(e.getKey(), e.getValue());
            o.add("tabIcons", tabs);
        }
        if (!n.getReputation().isEmpty()) {
            JsonObject rep = new JsonObject();
            for (var e : n.getReputation().entrySet()) rep.addProperty(e.getKey(), e.getValue());
            o.add("reputation", rep);
        }
        if (!n.getPendingAlliance().isEmpty()) {
            JsonArray pa = new JsonArray();
            for (String s : n.getPendingAlliance()) pa.add(s);
            o.add("pendingAlliance", pa);
        }
        if (!n.getHistory().isEmpty()) {
            JsonArray h = new JsonArray();
            for (String s : n.getHistory()) h.add(s);
            o.add("history", h);
        }
        if (n.getBudgetMilitary() > 0 || n.getBudgetHealth() > 0 || n.getBudgetEducation() > 0) {
            o.addProperty("budgetMilitary", n.getBudgetMilitary());
            o.addProperty("budgetHealth", n.getBudgetHealth());
            o.addProperty("budgetEducation", n.getBudgetEducation());
        }
        if (n.getGovernmentType() != null) o.addProperty("governmentType", n.getGovernmentType());
        return o;
    }

    private Nation deserializeNation(JsonObject o) {
        String id = o.get("id").getAsString();
        String name = o.get("name").getAsString();
        UUID leader = UUID.fromString(o.get("leader").getAsString());
        String currency = "AXC"; // Placeholder
        double treasury = o.get("treasury").getAsDouble();
        Nation n = new Nation(id, name, leader, currency, treasury);
        n.setCapitalChunkStr(o.get("capitalChunk").getAsString());
        if (o.has("exchangeRateToAXC")) n.setExchangeRateToAXC(o.get("exchangeRateToAXC").getAsDouble());
        if (o.has("motto")) n.setMotto(o.get("motto").getAsString());
        if (o.has("flagIcon")) n.setFlagIconMaterial(o.get("flagIcon").getAsString());
        JsonArray claims = o.getAsJsonArray("claimedChunks");
        for (JsonElement e : claims) n.getClaimedChunkKeys().add(e.getAsString());
        n.setInflation(o.has("inflation") ? o.get("inflation").getAsDouble() : 0.0);
        n.setTaxRate(o.has("taxRate") ? o.get("taxRate").getAsInt() : 10);
        if (o.has("allies")) for (JsonElement e : o.getAsJsonArray("allies")) n.getAllies().add(e.getAsString());
        if (o.has("enemies")) for (JsonElement e : o.getAsJsonArray("enemies")) n.getEnemies().add(e.getAsString());
        if (o.has("tabIcons")) {
            JsonObject tabs = o.getAsJsonObject("tabIcons");
            for (var entry : tabs.entrySet()) n.getTabIcons().put(entry.getKey(), entry.getValue().getAsString());
        }
        if (o.has("reputation")) {
            JsonObject rep = o.getAsJsonObject("reputation");
            for (var entry : rep.entrySet()) n.getReputation().put(entry.getKey(), entry.getValue().getAsInt());
        }
        if (o.has("pendingAlliance")) {
            JsonArray pa = o.getAsJsonArray("pendingAlliance");
            for (JsonElement e : pa) n.getPendingAlliance().add(e.getAsString());
        }
        if (o.has("history")) {
            JsonArray h = o.getAsJsonArray("history");
            for (JsonElement e : h) n.getHistory().add(e.getAsString());
        }
        if (o.has("budgetMilitary")) n.setBudgetMilitary(o.get("budgetMilitary").getAsDouble());
        if (o.has("budgetHealth")) n.setBudgetHealth(o.get("budgetHealth").getAsDouble());
        if (o.has("budgetEducation")) n.setBudgetEducation(o.get("budgetEducation").getAsDouble());
        if (o.has("governmentType")) n.setGovernmentType(o.get("governmentType").getAsString());
        n.getCitizens().add(leader);
        n.getRoles().put(leader, Nation.Role.LEADER);
        return n;
    }
    
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
        
        EconomyService economyService = ServiceLocator.get(EconomyService.class);
        if (economyService != null) {
            stats.put("gdp", economyService.getGDP(nationId));
            stats.put("economicHealth", economyService.getEconomicHealth(nationId));
        }
        
        MilitaryService militaryService = ServiceLocator.get(MilitaryService.class);
        if (militaryService != null) {
            stats.put("militaryStrength", militaryService.getMilitaryStrength(nationId));
        }
        
        CityGrowthEngine cityGrowthEngine = ServiceLocator.get(CityGrowthEngine.class);
        if (cityGrowthEngine != null) {
            stats.put("cities", cityGrowthEngine.getCitiesOf(nationId).size());
        }
        
        AdvancedWarSystem advancedWarSystem = ServiceLocator.get(AdvancedWarSystem.class);
        if (advancedWarSystem != null) {
            List<AdvancedWarSystem.War> wars = advancedWarSystem.getNationWars(nationId);
            stats.put("activeWars", wars.size());
            Map<String, Object> warStats = advancedWarSystem.getWarStatistics(nationId);
            stats.putAll(warStats);
        }
        
        TechnologyTreeService technologyTreeService = ServiceLocator.get(TechnologyTreeService.class);
        if (technologyTreeService != null) {
            stats.put("unlockedTechnologies", technologyTreeService.getUnlockedTechs(nationId).size());
        }
        
        return stats;
    }
    
    public synchronized String addCitizen(String nationId, UUID playerId, Nation.Role role) throws IOException {
        Nation nation = getNationById(nationId);
        if (nation == null) return "Нация не найдена.";
        if (nation.isMember(playerId)) return "Игрок уже в нации.";
        
        nation.getCitizens().add(playerId);
        nation.getRoles().put(playerId, role);
        save(nation);
        
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            org.bukkit.Bukkit.getScheduler().runTask(ServiceLocator.get(AXIOM.class), () -> {
                player.sendTitle("§a§l[НАЦИЯ]", "§fВы приняты в '" + nation.getName() + "'", 10, 60, 10);
                ServiceLocator.get(VisualEffectsService.class).sendActionBar(player, "§a✓ Должность: " + role);
                org.bukkit.Location loc = player.getLocation();
                loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                player.playSound(loc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            });
        }
        
        return "Гражданин добавлен.";
    }
    
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
    
    public synchronized String setCitizenRole(String nationId, UUID playerId, Nation.Role role) throws IOException {
        Nation nation = getNationById(nationId);
        if (nation == null) return "Нация не найдена.";
        if (!nation.isMember(playerId)) return "Игрок не в нации.";
        
        nation.getRoles().put(playerId, role);
        save(nation);
        
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            org.bukkit.Bukkit.getScheduler().runTask(ServiceLocator.get(AXIOM.class), () -> {
                ServiceLocator.get(VisualEffectsService.class).sendActionBar(player, "§e📋 Новая должность: " + role);
            });
        }
        
        return "Должность изменена.";
    }
    
    public synchronized String transferLeadership(String nationId, UUID newLeaderId) throws IOException {
        Nation nation = getNationById(nationId);
        if (nation == null) return "Нация не найдена.";
        if (!nation.isMember(newLeaderId)) return "Игрок не в нации.";
        
        UUID oldLeader = nation.getLeader();
        nation.setLeader(newLeaderId);
        nation.getRoles().put(oldLeader, Nation.Role.MINISTER);
        nation.getRoles().put(newLeaderId, Nation.Role.LEADER);
        save(nation);
        
        org.bukkit.Bukkit.getScheduler().runTask(ServiceLocator.get(AXIOM.class), () -> {
            org.bukkit.entity.Player newLeader = org.bukkit.Bukkit.getPlayer(newLeaderId);
            if (newLeader != null && newLeader.isOnline()) {
                newLeader.sendTitle("§a§l[ЛИДЕР]", "§fВы новый лидер '" + nation.getName() + "'", 10, 80, 20);
                ServiceLocator.get(VisualEffectsService.class).sendActionBar(newLeader, "§a✓ Вы стали лидером!");
                org.bukkit.Location loc = newLeader.getLocation();
                for (int i = 0; i < 20; i++) {
                    loc.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.05);
                }
                newLeader.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            }
        });
        
        return "Лидерство передано.";
    }
    
    public synchronized String setCapital(Player actor, Chunk newCapital) throws IOException {
        Optional<Nation> opt = getNationOfPlayer(actor.getUniqueId());
        if (opt.isEmpty()) return "Вы не в нации.";
        Nation nation = opt.get();
        if (nation.getRole(actor.getUniqueId()) != Nation.Role.LEADER) return "Только лидер может сменить столицу.";
        
        String key = chunkKey(newCapital.getWorld().getName(), newCapital.getX(), newCapital.getZ());
        if (!nation.getClaimedChunkKeys().contains(key)) return "Столица должна быть в вашей территории.";
        
        nation.setCapitalChunkStr(key);
        save(nation);
        
        org.bukkit.Bukkit.getScheduler().runTask(ServiceLocator.get(AXIOM.class), () -> {
            ServiceLocator.get(VisualEffectsService.class).sendActionBar(actor, "§a✓ Столица перенесена!");
            org.bukkit.Location loc = actor.getLocation();
            loc.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, loc.add(0, 2, 0), 20, 1, 1, 1, 0.05);
            actor.playSound(loc, org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        });
        
        return "Столица изменена.";
    }
    
    public synchronized double calculateNationPower(String nationId) {
        Nation nation = getNationById(nationId);
        if (nation == null) return 0.0;
        
        double score = 0.0;
        
        score += Math.min(30.0, nation.getClaimedChunkKeys().size() / 10.0);
        score += Math.min(20.0, nation.getCitizens().size() / 5.0);
        
        EconomyService economyService = ServiceLocator.get(EconomyService.class);
        if (economyService != null) {
            double gdp = economyService.getGDP(nationId);
            score += Math.min(25.0, gdp / 10000.0);
        }
        
        MilitaryService militaryService = ServiceLocator.get(MilitaryService.class);
        if (militaryService != null) {
            double military = militaryService.getMilitaryStrength(nationId);
            score += Math.min(15.0, military / 1000.0);
        }
        
        TechnologyTreeService technologyTreeService = ServiceLocator.get(TechnologyTreeService.class);
        if (technologyTreeService != null) {
            int techs = technologyTreeService.getUnlockedTechs(nationId).size();
            score += Math.min(10.0, techs * 0.5);
        }
        
        return Math.min(100.0, score);
    }
    
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
    
    public synchronized Map<String, Object> getNationManagerStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNations", getAll().size());
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
        
        Map<String, Integer> governmentDistribution = new HashMap<>();
        for (Nation n : getAll()) {
            governmentDistribution.put(n.getGovernmentType(), 
                governmentDistribution.getOrDefault(n.getGovernmentType(), 0) + 1);
        }
        stats.put("governmentDistribution", governmentDistribution);
        
        stats.put("topNationsByPower", getTopNations(10));
        
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
        
        int totalAllies = 0;
        int totalEnemies = 0;
        for (Nation n : getAll()) {
            totalAllies += n.getAllies().size();
            totalEnemies += n.getEnemies().size();
        }
        stats.put("totalAlliances", totalAllies / 2);
        stats.put("totalEnemyRelations", totalEnemies);
        
        return stats;
    }
    
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
    
    public synchronized List<Nation> getNationsByGovernmentType(String governmentType) {
        List<Nation> result = new ArrayList<>();
        for (Nation n : getAll()) {
            if (n.getGovernmentType().equalsIgnoreCase(governmentType)) {
                result.add(n);
            }
        }
        return result;
    }
    
    public synchronized List<Nation> getLargestNationsByTerritory(int limit) {
        return getAll().stream()
            .sorted((a, b) -> Integer.compare(b.getClaimedChunkKeys().size(), a.getClaimedChunkKeys().size()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public synchronized List<Nation> getMostPopulatedNations(int limit) {
        return getAll().stream()
            .sorted((a, b) -> Integer.compare(b.getCitizens().size(), a.getCitizens().size()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public synchronized Map<String, Object> getGlobalNationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Collection<Nation> allNations = getAll();
        stats.put("totalNations", allNations.size());
        
        if (allNations.isEmpty()) {
            return stats;
        }
        
        int totalChunks = 0;
        int maxChunks = 0;
        int minChunks = Integer.MAX_VALUE;
        Map<String, Integer> chunksByNation = new HashMap<>();
        
        int totalCitizens = 0;
        int maxCitizens = 0;
        int minCitizens = Integer.MAX_VALUE;
        Map<String, Integer> citizensByNation = new HashMap<>();
        
        double totalTreasury = 0.0;
        double maxTreasury = 0.0;
        double minTreasury = Double.MAX_VALUE;
        
        Map<String, Integer> governmentTypes = new HashMap<>();
        Map<String, Integer> currencyCodes = new HashMap<>();
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
        
        List<Map.Entry<String, Integer>> topByChunks = chunksByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByChunks", topByChunks);
        
        List<Map.Entry<String, Integer>> topByCitizens = citizensByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCitizens", topByCitizens);
        
        List<Map.Entry<String, Double>> topByTreasury = new ArrayList<>();
        for (Nation n : allNations) {
            topByTreasury.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), n.getTreasury()));
        }
        topByTreasury.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByTreasury", topByTreasury.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}
