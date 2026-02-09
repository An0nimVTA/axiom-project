package com.axiom.domain.service.technology;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.repo.TechProgressStore;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/**
 * Comprehensive technology tree with research branches and progression paths.
 * Supports mod-based technologies and gradual game progression.
 */
public class TechnologyTreeService {
    private final AXIOM plugin;
    private final TechProgressStore techProgressStore;
    private final Map<String, Set<String>> unlockedTechs = new HashMap<>(); // nationId -> techs
    private final Map<String, Technology> technologies = new HashMap<>(); // techId -> tech
    private final Map<String, ResearchBranch> branches = new HashMap<>(); // branchId -> branch

    public enum ProgressStage {
        EARLY("early", "Ранний", 1, 2),
        MID("mid", "Средний", 3, 4),
        LATE("late", "Поздний", 5, 5);

        private final String id;
        private final String displayName;
        private final int minTier;
        private final int maxTier;

        ProgressStage(String id, String displayName, int minTier, int maxTier) {
            this.id = id;
            this.displayName = displayName;
            this.minTier = minTier;
            this.maxTier = maxTier;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean includesTier(int tier) {
            return tier >= minTier && tier <= maxTier;
        }

        public static ProgressStage fromTier(int tier) {
            for (ProgressStage stage : values()) {
                if (stage.includesTier(tier)) {
                    return stage;
                }
            }
            return LATE;
        }
    }

    public static class Technology {
        public String id;
        public String name;
        public String description;
        public String branch; // "military", "industry", "economy", "infrastructure", "science"
        public int tier; // 1-5 (difficulty level)
        public ProgressStage stage; // EARLY/MID/LATE
        public List<String> prerequisites = new ArrayList<>();
        public double researchCost;
        public double researchTimeHours; // estimated time to research
        public Map<String, Double> bonuses = new HashMap<>(); // type -> value
        public String requiredMod; // mod ID if required
        public boolean modOptional; // true if mod is optional but provides bonus
    }

    public static class ResearchBranch {
        public String id;
        public String name;
        public String description;
        public List<String> techIds = new ArrayList<>(); // ordered by tier
    }

    public static class ResearchStatus {
        public Technology technology;
        public boolean unlocked;
        public List<String> missingPrerequisites = new ArrayList<>();
        public boolean prerequisitesMet;
        public boolean modRequirementMet;
        public boolean educationMet;
        public double requiredEducation;
        public double currentEducation;
        public boolean treasuryEnough;
        public double treasury;

        public boolean canResearch() {
            return technology != null
                && !unlocked
                && prerequisitesMet
                && modRequirementMet
                && educationMet
                && treasuryEnough;
        }
    }

    public static class ResearchResult {
        public final boolean success;
        public final String message;
        public final ResearchStatus status;

        public ResearchResult(boolean success, String message, ResearchStatus status) {
            this.success = success;
            this.message = message;
            this.status = status;
        }
    }

    public TechnologyTreeService(AXIOM plugin) {
        this.plugin = plugin;
        this.techProgressStore = new TechProgressStore(
            plugin.getDataFolder(),
            plugin.getConfig().getBoolean("storage.prettyPrintJson", true)
        );
        initializeBranches();
        initializeDefaultTechs();
        loadUnlockedTechs();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processResearch, 0, 20 * 60 * 5); // every 5 minutes
    }

    private void initializeBranches() {
        // Military Branch
        ResearchBranch military = new ResearchBranch();
        military.id = "military";
        military.name = "Военные технологии";
        military.description = "Развитие военной мощи и обороноспособности";
        branches.put("military", military);

        // Industrial Branch
        ResearchBranch industry = new ResearchBranch();
        industry.id = "industry";
        industry.name = "Промышленность";
        industry.description = "Производство и автоматизация";
        branches.put("industry", industry);

        // Economic Branch
        ResearchBranch economy = new ResearchBranch();
        economy.id = "economy";
        economy.name = "Экономика";
        economy.description = "Торговля и финансы";
        branches.put("economy", economy);

        // Infrastructure Branch
        ResearchBranch infrastructure = new ResearchBranch();
        infrastructure.id = "infrastructure";
        infrastructure.name = "Инфраструктура";
        infrastructure.description = "Транспорт и связь";
        branches.put("infrastructure", infrastructure);

        // Science Branch
        ResearchBranch science = new ResearchBranch();
        science.id = "science";
        science.name = "Наука";
        science.description = "Исследования и образование";
        branches.put("science", science);
    }

    private void initializeDefaultTechs() {
        // ========== TIER 1 - FOUNDATION ==========
        
        // Military Tier 1
        createTech("basic_military", "Базовая военная тактика", "Основа военного дела", "military", 1,
            Collections.emptyList(), 5000, 2.0, 
            Map.of("warStrength", 1.1), null);

        createTech("basic_weapons", "Простое оружие", "Мечи, луки, копья", "military", 1,
            Collections.emptyList(), 3000, 1.5,
            Map.of("warStrength", 1.05), null);

        // Industry Tier 1
        createTech("basic_construction", "Базовое строительство", "Основы строительства", "infrastructure", 1,
            Collections.emptyList(), 4000, 2.0,
            Map.of("buildSpeed", 1.2), null);

        createTech("basic_mining", "Базовая добыча", "Ручная добыча ресурсов", "industry", 1,
            Collections.emptyList(), 3000, 1.5,
            Map.of("resourceExtraction", 1.1), null);

        // Economy Tier 1
        createTech("basic_trade", "Базовая торговля", "Простая торговля между игроками", "economy", 1,
            Collections.emptyList(), 3000, 1.5,
            Map.of("tradeBonus", 1.15), null);

        createTech("basic_currency", "Национальная валюта", "Создание собственной валюты", "economy", 1,
            Collections.emptyList(), 5000, 2.0,
            Map.of("economicEfficiency", 1.1), null);

        // Science Tier 1
        createTech("basic_education", "Базовое образование", "Начальные школы", "science", 1,
            Collections.emptyList(), 4000, 2.0,
            Map.of("researchSpeed", 1.1), null);

        // ========== TIER 2 - EARLY DEVELOPMENT ==========

        // Military Tier 2
        createTech("fortifications", "Фортификации", "Стены и укрепления", "military", 2,
            Arrays.asList("basic_military"), 8000, 3.0,
            Map.of("defenseBonus", 1.3), null);

        createTech("tactical_warfare", "Тактическая война", "Улучшенная тактика", "military", 2,
            Arrays.asList("basic_military", "basic_weapons"), 10000, 3.5,
            Map.of("warStrength", 1.2), null);

        // Industry Tier 2
        createTech("basic_industry", "Базовая промышленность", "Простые станки", "industry", 2,
            Arrays.asList("basic_construction"), 10000, 3.0,
            Map.of("productionBonus", 1.3), null);

        createTech("improved_mining", "Улучшенная добыча", "Более эффективная добыча", "industry", 2,
            Arrays.asList("basic_mining"), 8000, 2.5,
            Map.of("resourceExtraction", 1.3), null);

        // Economy Tier 2
        createTech("trade_networks", "Торговые сети", "Расширенные торговые пути", "economy", 2,
            Arrays.asList("basic_trade"), 8000, 2.5,
            Map.of("tradeBonus", 1.25), null);

        createTech("banking", "Банковское дело", "Система займов и процентов", "economy", 2,
            Arrays.asList("basic_currency"), 10000, 3.0,
            Map.of("economicEfficiency", 1.2), null);

        // Infrastructure Tier 2
        createTech("roads", "Дороги", "Транспортная сеть", "infrastructure", 2,
            Arrays.asList("basic_construction"), 6000, 2.0,
            Map.of("mobility", 1.2), null);

        // Science Tier 2
        createTech("advanced_education", "Продвинутое образование", "Университеты", "science", 2,
            Arrays.asList("basic_education"), 12000, 4.0,
            Map.of("researchSpeed", 1.3), null);

        // ========== TIER 3 - MOD INTEGRATION STARTS ==========

        // Military Tier 3 - Firearms
        createTech("firearms_tech", "Стрелковое оружие", "Винтовки и пистолеты", "military", 3,
            Arrays.asList("tactical_warfare"), 15000, 5.0,
            Map.of("warStrength", 1.3, "weaponDamage", 1.25), "tacz");

        createTech("firearms_tech_alt", "Стрелковое оружие (альтернатива)", "Винтовки через PointBlank", "military", 3,
            Arrays.asList("tactical_warfare"), 15000, 5.0,
            Map.of("warStrength", 1.3, "weaponDamage", 1.25), "pointblank");

        createTech("artillery_tech", "Артиллерия", "Пушки и ракеты", "military", 3,
            Arrays.asList("firearms_tech"), 20000, 6.0,
            Map.of("siegeStrength", 1.5, "defenseBonus", 1.2), "ballistix");

        createTech("military_vehicles", "Военная техника", "Танки и БТРы", "military", 3,
            Arrays.asList("firearms_tech"), 25000, 7.0,
            Map.of("warStrength", 1.4, "mobility", 1.3), "superwarfare");

        // Industry Tier 3 - Immersive Engineering
        createTech("industrial_engineering", "Промышленное производство", "Заводы и автоматизация", "industry", 3,
            Arrays.asList("basic_industry"), 18000, 6.0,
            Map.of("productionBonus", 1.5, "energyEfficiency", 1.3), "immersiveengineering");

        createTech("resource_extraction", "Массовая добыча", "Автоматические карьеры", "industry", 3,
            Arrays.asList("improved_mining", "industrial_engineering"), 15000, 5.0,
            Map.of("resourceExtraction", 2.0), "simplyquarries");

        // Economy Tier 3 - AE2
        createTech("automation_tech", "Автоматизация и логистика", "ME-сети и паттерны", "economy", 3,
            Arrays.asList("trade_networks", "industrial_engineering"), 20000, 7.0,
            Map.of("tradeBonus", 1.4, "resourceEfficiency", 1.35), "appliedenergistics2");

        // Infrastructure Tier 3 - Vehicles
        createTech("transportation_tech", "Транспортная инфраструктура", "Машины и поезда", "infrastructure", 3,
            Arrays.asList("roads"), 15000, 5.0,
            Map.of("tradeBonus", 1.25, "mobility", 1.5), "immersivevehicles");

        // Science Tier 3
        createTech("research_labs", "Исследовательские лаборатории", "Ускорение исследований", "science", 3,
            Arrays.asList("advanced_education"), 18000, 6.0,
            Map.of("researchSpeed", 1.5), null);

        // ========== TIER 4 - ADVANCED MODS ==========

        // Military Tier 4
        createTech("elite_equipment", "Элитное снаряжение", "Тактическое оборудование", "military", 4,
            Arrays.asList("firearms_tech"), 20000, 6.0,
            Map.of("warStrength", 1.2, "defenseBonus", 1.15), "capsawims", true);

        createTech("elite_equipment_alt", "Элитное снаряжение (альтернатива)", "Warium броня", "military", 4,
            Arrays.asList("firearms_tech"), 20000, 6.0,
            Map.of("warStrength", 1.2, "defenseBonus", 1.15), "warium", true);

        // Industry Tier 4
        createTech("advanced_industry", "Продвинутая индустрия", "Модернизация машин", "industry", 4,
            Arrays.asList("industrial_engineering"), 25000, 8.0,
            Map.of("productionBonus", 1.8, "energyEfficiency", 1.5), "industrialupgrade");

        // Infrastructure Tier 4 - Energy
        createTech("quantum_energy", "Квантовая энергетика", "Сверхмощные генераторы", "infrastructure", 4,
            Arrays.asList("advanced_industry"), 30000, 10.0,
            Map.of("energyProduction", 3.0, "energyEfficiency", 2.0), "quantumgenerators");

        createTech("power_networks", "Энергосети", "Управление энергией", "infrastructure", 4,
            Arrays.asList("industrial_engineering"), 20000, 6.0,
            Map.of("energyEfficiency", 1.4), "powerutils", true);

        // Economy Tier 4
        createTech("advanced_trade", "Продвинутая торговля", "Международные рынки", "economy", 4,
            Arrays.asList("automation_tech", "transportation_tech"), 25000, 8.0,
            Map.of("tradeBonus", 1.6), null);

        // Science Tier 4
        createTech("space_program", "Космическая программа", "Исследования космоса", "science", 4,
            Arrays.asList("research_labs"), 30000, 12.0,
            Map.of("researchSpeed", 2.0, "prestige", 1.5), null);

        // ========== TIER 5 - ENDGAME ==========

        // Military Tier 5
        createTech("nuclear_weapons", "Ядерное оружие", "Атомное сдерживание", "military", 5,
            Arrays.asList("space_program", "quantum_energy"), 50000, 15.0,
            Map.of("warStrength", 2.0, "deterrence", 3.0), null);

        createTech("total_warfare", "Тотальная война", "Максимальная военная мощь", "military", 5,
            Arrays.asList("military_vehicles", "artillery_tech", "elite_equipment"), 40000, 12.0,
            Map.of("warStrength", 1.8), null);

        // Industry Tier 5
        createTech("mega_production", "Мегапроизводство", "Промышленная сверхдержава", "industry", 5,
            Arrays.asList("advanced_industry", "automation_tech"), 45000, 14.0,
            Map.of("productionBonus", 2.5), null);

        // Economy Tier 5
        createTech("global_economy", "Глобальная экономика", "Доминирование в торговле", "economy", 5,
            Arrays.asList("advanced_trade", "automation_tech"), 40000, 12.0,
            Map.of("tradeBonus", 2.0), null);

        // Infrastructure Tier 5
        createTech("mega_infrastructure", "Мегаинфраструктура", "Сверхразвитая инфраструктура", "infrastructure", 5,
            Arrays.asList("quantum_energy", "transportation_tech"), 45000, 14.0,
            Map.of("mobility", 2.0, "energyProduction", 2.5), null);

        // Science Tier 5
        createTech("transcendent_science", "Трансцендентная наука", "Пик научного развития", "science", 5,
            Arrays.asList("space_program", "research_labs"), 50000, 15.0,
            Map.of("researchSpeed", 3.0), null);
    }

    private void createTech(String id, String name, String description, String branch, int tier,
                           List<String> prerequisites, double cost, double timeHours,
                           Map<String, Double> bonuses, String requiredMod) {
        createTech(id, name, description, branch, tier, prerequisites, cost, timeHours, bonuses, requiredMod, false);
    }

    private void createTech(String id, String name, String description, String branch, int tier,
                           List<String> prerequisites, double cost, double timeHours,
                           Map<String, Double> bonuses, String requiredMod, boolean modOptional) {
        Technology tech = new Technology();
        tech.id = id;
        tech.name = name;
        tech.description = description;
        tech.branch = branch;
        tech.tier = tier;
        tech.stage = ProgressStage.fromTier(tier);
        tech.prerequisites = prerequisites == null ? new ArrayList<>() : new ArrayList<>(prerequisites);
        tech.researchCost = cost;
        tech.researchTimeHours = timeHours;
        tech.bonuses = bonuses;
        tech.requiredMod = requiredMod;
        tech.modOptional = modOptional;
        technologies.put(id, tech);
        
        // Add to branch
        ResearchBranch branchObj = branches.get(branch);
        if (branchObj != null && !branchObj.techIds.contains(id)) {
            branchObj.techIds.add(id);
        }
    }

    private double getRequiredEducation(Technology tech) {
        if (tech == null) return 0.0;
        // Keep legacy rule: tier * 10
        return tech.tier * 10.0;
    }

    private double getEducationLevel(String nationId) {
        if (plugin.getEducationService() == null || nationId == null) return 0.0;
        return plugin.getEducationService().getEducationLevel(nationId);
    }

    private List<String> getMissingPrerequisites(Set<String> unlocked, Technology tech) {
        List<String> missing = new ArrayList<>();
        if (tech == null) return missing;
        if (unlocked == null) unlocked = Collections.emptySet();
        for (String prereq : tech.prerequisites) {
            if (!unlocked.contains(prereq)) {
                missing.add(prereq);
            }
        }
        return missing;
    }

    private boolean arePrerequisitesMet(Set<String> unlocked, Technology tech) {
        return getMissingPrerequisites(unlocked, tech).isEmpty();
    }

    public synchronized ResearchStatus getResearchStatus(String nationId, String techId) {
        ResearchStatus status = new ResearchStatus();
        Technology tech = technologies.get(techId);
        status.technology = tech;
        if (tech == null || nationId == null) {
            return status;
        }

        Set<String> unlocked = unlockedTechs.getOrDefault(nationId, new HashSet<>());
        status.unlocked = unlocked.contains(techId);
        status.missingPrerequisites = getMissingPrerequisites(unlocked, tech);
        status.prerequisitesMet = status.missingPrerequisites.isEmpty();
        status.modRequirementMet = isModRequirementMet(tech);
        status.requiredEducation = getRequiredEducation(tech);
        if (plugin.getEducationService() == null) {
            status.currentEducation = status.requiredEducation;
            status.educationMet = true;
        } else {
            status.currentEducation = getEducationLevel(nationId);
            status.educationMet = status.currentEducation >= status.requiredEducation;
        }

        Nation n = plugin.getNationManager() != null ? plugin.getNationManager().getNationById(nationId) : null;
        status.treasury = n != null ? n.getTreasury() : 0.0;
        status.treasuryEnough = n != null && status.treasury >= tech.researchCost;
        return status;
    }

    public synchronized boolean canResearchTechnology(String nationId, String techId) {
        return getResearchStatus(nationId, techId).canResearch();
    }

    public synchronized ProgressStage getNationStage(String nationId) {
        Set<String> unlocked = getUnlockedTechs(nationId);
        int maxTier = 1;
        for (String techId : unlocked) {
            Technology tech = technologies.get(techId);
            if (tech != null && tech.tier > maxTier) {
                maxTier = tech.tier;
            }
        }
        return ProgressStage.fromTier(maxTier);
    }

    public synchronized Map<ProgressStage, Double> getStageProgress(String nationId) {
        Map<ProgressStage, Double> progress = new EnumMap<>(ProgressStage.class);
        Set<String> unlocked = getUnlockedTechs(nationId);
        for (ProgressStage stage : ProgressStage.values()) {
            long total = technologies.values().stream().filter(t -> t.stage == stage).count();
            long unlockedCount = unlocked.stream()
                .map(technologies::get)
                .filter(t -> t != null && t.stage == stage)
                .count();
            double pct = total == 0 ? 0.0 : (unlockedCount / (double) total) * 100.0;
            progress.put(stage, pct);
        }
        return progress;
    }

    public synchronized List<Technology> getTechnologiesByStage(ProgressStage stage) {
        if (stage == null) return Collections.emptyList();
        List<Technology> result = new ArrayList<>();
        for (Technology tech : technologies.values()) {
            if (tech.stage == stage) {
                result.add(tech);
            }
        }
        return result;
    }

    /**
     * Start researching a technology (requires prerequisites and resources).
     */
    public synchronized String researchTechnology(String nationId, String techId) {
        return attemptResearchInternal(nationId, techId).message;
    }

    public synchronized ResearchResult attemptResearch(UUID playerUUID, String techId) {
        String nationId = getNationIdForPlayer(playerUUID);
        if (nationId == null) {
            return new ResearchResult(false, "Вы не состоите в нации.", new ResearchStatus());
        }
        return attemptResearchInternal(nationId, techId);
    }

    private ResearchResult attemptResearchInternal(String nationId, String techId) {
        if (nationId == null || nationId.isBlank()) {
            return new ResearchResult(false, "Нация не найдена.", new ResearchStatus());
        }
        Technology tech = technologies.get(techId);
        if (tech == null) {
            return new ResearchResult(false, "Технология не найдена.", new ResearchStatus());
        }

        ResearchStatus status = getResearchStatus(nationId, techId);
        if (status.unlocked) {
            return new ResearchResult(false, "Технология уже изучена.", status);
        }
        if (!status.prerequisitesMet) {
            return new ResearchResult(false, "Не выполнены предварительные условия: " + String.join(", ", status.missingPrerequisites), status);
        }
        if (!status.modRequirementMet) {
            return new ResearchResult(false, "Технология требует мод: " + tech.requiredMod, status);
        }
        if (!status.educationMet) {
            return new ResearchResult(false,
                "Недостаточный уровень образования (нужно " + status.requiredEducation + ", сейчас " + status.currentEducation + ").",
                status);
        }

        Nation n = plugin.getNationManager() != null ? plugin.getNationManager().getNationById(nationId) : null;
        if (n == null) {
            return new ResearchResult(false, "Нация не найдена.", status);
        }
        if (!status.treasuryEnough) {
            return new ResearchResult(false, "Недостаточно средств (нужно " + tech.researchCost + ").", status);
        }

        // Pay cost
        n.setTreasury(n.getTreasury() - tech.researchCost);

        // Start research (instant for now, but could be time-based)
        Set<String> unlocked = unlockedTechs.computeIfAbsent(nationId, k -> new HashSet<>());
        unlocked.add(techId);
        saveUnlockedTechs(nationId, unlocked);

        // Log in history
        if (n.getHistory() != null) {
            n.getHistory().add("Технология изучена: " + tech.name);
        }
        try {
            plugin.getNationManager().save(n);
        } catch (Exception ignored) {}

        // VISUAL EFFECTS: Celebrate technology research completion
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID citizenId : n.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§e§l[ИССЛЕДОВАНИЕ]", "§fТехнология '" + tech.name + "' изучена!", 10, 80, 20);

                    String msg = "§e🔬 Технология '" + tech.name + "' завершена!";
                    if (tech.requiredMod != null && tech.modOptional &&
                        plugin.getModIntegrationService() != null &&
                        plugin.getModIntegrationService().isModAvailable(tech.requiredMod)) {
                        msg += " §6(бонус от мода активен)";
                    }
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg);

                    // Purple/blue particles for research
                    org.bukkit.Location loc = citizen.getLocation();
                    for (int i = 0; i < 20; i++) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.05);
                        if (i % 3 == 0) {
                            loc.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTMENT_TABLE, loc, 1, 0.3, 0.3, 0.3, 0.1);
                        }
                    }
                    citizen.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                }
            }
        });

        // If mod is optional but available, provide bonus
        if (tech.requiredMod != null && tech.modOptional && isModAvailable(tech.requiredMod)) {
            return new ResearchResult(true, "Технология изучена: " + tech.name + " (бонус от мода активен)", status);
        }

        return new ResearchResult(true, "Технология изучена: " + tech.name, status);
    }

    /**
     * Get available technologies for research (prerequisites met).
     */
    public synchronized List<Technology> getAvailableTechs(String nationId) {
        Set<String> unlocked = unlockedTechs.getOrDefault(nationId, new HashSet<>());
        List<Technology> available = new ArrayList<>();
        
        for (Technology tech : technologies.values()) {
            if (unlocked.contains(tech.id)) continue;
            if (!isModRequirementMet(tech)) continue;
            
            // Check prerequisites
            if (arePrerequisitesMet(unlocked, tech)) {
                available.add(tech);
            }
        }
        
        // Sort by tier
        available.sort(Comparator.comparingInt(t -> t.tier));
        return available;
    }

    /**
     * Get research progress for a branch.
     */
    public synchronized double getBranchProgress(String nationId, String branchId) {
        ResearchBranch branch = branches.get(branchId);
        if (branch == null) return 0.0;
        
        Set<String> unlocked = unlockedTechs.getOrDefault(nationId, new HashSet<>());
        int unlockedCount = 0;
        for (String techId : branch.techIds) {
            if (unlocked.contains(techId)) unlockedCount++;
        }
        
        return branch.techIds.isEmpty() ? 0.0 : (double) unlockedCount / branch.techIds.size() * 100.0;
    }

    private void processResearch() {
        // Future: process time-based research
        // Currently research is instant after payment
    }

    private boolean isModAvailable(String modId) {
        return modId != null
            && plugin.getModIntegrationService() != null
            && plugin.getModIntegrationService().isModAvailable(modId);
    }

    private boolean isModRequirementMet(Technology tech) {
        if (tech.requiredMod == null || tech.requiredMod.isEmpty()) {
            return true;
        }
        if (tech.modOptional) {
            return true;
        }
        return isModAvailable(tech.requiredMod);
    }

    private boolean isBonusActive(Technology tech) {
        if (tech.requiredMod == null || tech.requiredMod.isEmpty()) {
            return true;
        }
        return isModAvailable(tech.requiredMod);
    }

    public synchronized double getBonus(String nationId, String bonusType) {
        Set<String> unlocked = unlockedTechs.get(nationId);
        if (unlocked == null) return 1.0;
        double bonus = 1.0;
        for (String techId : unlocked) {
            Technology tech = technologies.get(techId);
            if (tech != null && isBonusActive(tech) && tech.bonuses.containsKey(bonusType)) {
                bonus *= tech.bonuses.get(bonusType);
            }
        }
        return bonus;
    }

    public synchronized void addResearchPoints(String nationId, String category, double points) {
        plugin.getEducationService().addResearchProgress(nationId, points);
    }

    public synchronized List<Technology> getAllTechs() {
        return new ArrayList<>(technologies.values());
    }

    public synchronized List<ResearchBranch> getAllBranches() {
        return new ArrayList<>(branches.values());
    }

    public synchronized Technology getTech(String techId) {
        return technologies.get(techId);
    }
    
    /**
     * Check if a technology is unlocked for a nation.
     */
    public synchronized boolean isTechnologyUnlocked(String nationId, String techId) {
        Set<String> unlocked = unlockedTechs.get(nationId);
        return unlocked != null && unlocked.contains(techId);
    }
    
    /**
     * Get unlocked technologies for a nation.
     */
    public synchronized Set<String> getUnlockedTechs(String nationId) {
        return new HashSet<>(unlockedTechs.getOrDefault(nationId, new HashSet<>()));
    }

    private void loadUnlockedTechs() {
        unlockedTechs.clear();
        unlockedTechs.putAll(techProgressStore.loadAll());
    }

    private void saveUnlockedTechs(String nationId, Set<String> techs) {
        techProgressStore.save(nationId, techs);
    }
    
    /**
     * Get technology research progress for a nation.
     */
    public synchronized Map<String, Object> getResearchProgress(String nationId, String techId) {
        Map<String, Object> progress = new HashMap<>();
        ResearchStatus status = getResearchStatus(nationId, techId);
        Technology tech = status.technology;
        if (tech == null) return progress;

        progress.put("unlocked", status.unlocked);
        progress.put("missingPrerequisites", status.missingPrerequisites);
        progress.put("canResearch", status.canResearch());
        progress.put("modRequirementMet", status.modRequirementMet);
        progress.put("educationRequired", status.requiredEducation);
        progress.put("educationLevel", status.currentEducation);
        progress.put("treasuryEnough", status.treasuryEnough);
        progress.put("stage", tech.stage != null ? tech.stage.getId() : null);
        progress.put("technology", tech);
        return progress;
    }
    
    /**
     * Get all researchable technologies for a nation.
     */
    public synchronized List<Technology> getResearchableTechs(String nationId) {
        List<Technology> result = new ArrayList<>();
        Set<String> unlocked = getUnlockedTechs(nationId);
        
        for (Technology tech : technologies.values()) {
            if (unlocked.contains(tech.id)) continue;
            if (canResearchTechnology(nationId, tech.id)) {
                result.add(tech);
            }
        }
        
        return result;
    }
    
    /**
     * Get technology statistics for a nation.
     */
    public synchronized Map<String, Object> getTechnologyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        Set<String> unlocked = getUnlockedTechs(nationId);
        stats.put("unlockedCount", unlocked.size());
        stats.put("totalCount", technologies.size());
        stats.put("progressPercentage", technologies.size() > 0 ? (unlocked.size() / (double) technologies.size()) * 100 : 0);
        
        // Count by branch
        Map<String, Integer> branchCounts = new HashMap<>();
        for (String techId : unlocked) {
            Technology tech = technologies.get(techId);
            if (tech != null) {
                branchCounts.put(tech.branch, branchCounts.getOrDefault(tech.branch, 0) + 1);
            }
        }
        stats.put("byBranch", branchCounts);
        
        // Count by tier
        Map<Integer, Integer> tierCounts = new HashMap<>();
        for (String techId : unlocked) {
            Technology tech = technologies.get(techId);
            if (tech != null) {
                tierCounts.put(tech.tier, tierCounts.getOrDefault(tech.tier, 0) + 1);
            }
        }
        stats.put("byTier", tierCounts);

        // Count by stage
        Map<String, Integer> stageCounts = new HashMap<>();
        for (String techId : unlocked) {
            Technology tech = technologies.get(techId);
            if (tech != null && tech.stage != null) {
                stageCounts.put(tech.stage.getId(), stageCounts.getOrDefault(tech.stage.getId(), 0) + 1);
            }
        }
        stats.put("byStage", stageCounts);
        
        // Total bonuses
        Map<String, Double> totalBonuses = new HashMap<>();
        for (String techId : unlocked) {
            Technology tech = technologies.get(techId);
            if (tech != null && tech.bonuses != null) {
                for (Map.Entry<String, Double> entry : tech.bonuses.entrySet()) {
                    totalBonuses.put(entry.getKey(), totalBonuses.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
                }
            }
        }
        stats.put("totalBonuses", totalBonuses);
        
        return stats;
    }
    
    /**
     * Get technologies by branch.
     */
    public synchronized List<Technology> getTechnologiesByBranch(String branchId) {
        return technologies.values().stream()
            .filter(t -> t.branch.equals(branchId))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get technologies by tier.
     */
    public synchronized List<Technology> getTechnologiesByTier(int tier) {
        return technologies.values().stream()
            .filter(t -> t.tier == tier)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get next tier technologies that can be researched.
     */
    public synchronized List<Technology> getNextTierTechs(String nationId) {
        Set<String> unlocked = getUnlockedTechs(nationId);
        int maxUnlockedTier = 0;
        
        for (String techId : unlocked) {
            Technology tech = technologies.get(techId);
            if (tech != null && tech.tier > maxUnlockedTier) {
                maxUnlockedTier = tech.tier;
            }
        }
        
        int nextTier = maxUnlockedTier + 1;
        List<Technology> result = new ArrayList<>();
        
        for (Technology tech : getTechnologiesByTier(nextTier)) {
            if (unlocked.contains(tech.id)) continue;
            if (arePrerequisitesMet(unlocked, tech)) {
                result.add(tech);
            }
        }
        
        return result;
    }
    
    /**
     * Calculate technology power score for a nation.
     */
    public synchronized double calculateTechnologyPower(String nationId) {
        Set<String> unlocked = getUnlockedTechs(nationId);
        double power = 0.0;
        
        for (String techId : unlocked) {
            Technology tech = technologies.get(techId);
            if (tech != null) {
                // Base power from tier
                power += tech.tier * 10.0;
                
                // Bonus power from bonuses
                if (tech.bonuses != null) {
                    for (Double bonus : tech.bonuses.values()) {
                        power += bonus * 5.0;
                    }
                }
            }
        }
        
        return power;
    }
    
    /**
     * Get global technology statistics.
     */
    public synchronized Map<String, Object> getGlobalTechnologyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalTechnologies", technologies.size());
        stats.put("totalBranches", branches.size());
        
        // Technologies by branch
        Map<String, Integer> techsByBranch = new HashMap<>();
        for (Technology tech : technologies.values()) {
            techsByBranch.put(tech.branch, techsByBranch.getOrDefault(tech.branch, 0) + 1);
        }
        stats.put("technologiesByBranch", techsByBranch);
        
        // Technologies by tier
        Map<Integer, Integer> techsByTier = new HashMap<>();
        for (Technology tech : technologies.values()) {
            techsByTier.put(tech.tier, techsByTier.getOrDefault(tech.tier, 0) + 1);
        }
        stats.put("technologiesByTier", techsByTier);

        // Technologies by stage
        Map<String, Integer> techsByStage = new HashMap<>();
        for (Technology tech : technologies.values()) {
            if (tech.stage != null) {
                techsByStage.put(tech.stage.getId(), techsByStage.getOrDefault(tech.stage.getId(), 0) + 1);
            }
        }
        stats.put("technologiesByStage", techsByStage);
        
        // Average unlock rate (if nation data available)
        if (plugin.getNationManager() != null) {
            int nationsWithTechs = 0;
            int totalUnlocked = 0;
            for (Nation n : plugin.getNationManager().getAll()) {
                Set<String> unlocked = getUnlockedTechs(n.getId());
                if (unlocked.size() > 0) {
                    nationsWithTechs++;
                    totalUnlocked += unlocked.size();
                }
            }
            stats.put("nationsWithTechnologies", nationsWithTechs);
            stats.put("averageUnlockedPerNation", nationsWithTechs > 0 ? (double) totalUnlocked / nationsWithTechs : 0);
        }
        
        // Mod requirements
        int modRequiredTechs = 0;
        int modOptionalTechs = 0;
        for (Technology tech : technologies.values()) {
            if (tech.requiredMod != null && !tech.requiredMod.isEmpty()) {
                if (tech.modOptional) {
                    modOptionalTechs++;
                } else {
                    modRequiredTechs++;
                }
            }
        }
        stats.put("modRequiredTechnologies", modRequiredTechs);
        stats.put("modOptionalTechnologies", modOptionalTechs);
        
        return stats;
    }
    
    /**
     * Get technologies requiring a specific mod.
     */
    public synchronized List<Technology> getTechnologiesByMod(String modId) {
        return technologies.values().stream()
            .filter(t -> t.requiredMod != null && t.requiredMod.equalsIgnoreCase(modId))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get research progress for a nation.
     */
    public synchronized Map<String, Object> getResearchProgress(String nationId) {
        Map<String, Object> progress = new HashMap<>();
        
        Set<String> unlocked = getUnlockedTechs(nationId);
        List<Technology> researchable = getResearchableTechs(nationId);
        
        progress.put("unlockedCount", unlocked.size());
        progress.put("researchableCount", researchable.size());
        progress.put("totalTechnologies", technologies.size());
        progress.put("completionPercentage", technologies.size() > 0 ? (unlocked.size() / (double) technologies.size()) * 100 : 0);
        
        // Next tier info
        List<Technology> nextTier = getNextTierTechs(nationId);
        progress.put("nextTierAvailable", !nextTier.isEmpty());
        progress.put("nextTierCount", nextTier.size());

        ProgressStage stage = getNationStage(nationId);
        progress.put("stage", stage.getId());
        progress.put("stageDisplay", stage.getDisplayName());
        progress.put("stageProgress", getStageProgress(nationId));
        
        return progress;
    }
    
    /**
     * Get top nations by technology power.
     */
    public synchronized List<Map.Entry<String, Double>> getTopNationsByTechnology(int limit) {
        Map<String, Double> powers = new HashMap<>();
        
        if (plugin.getNationManager() != null) {
            for (Nation n : plugin.getNationManager().getAll()) {
                powers.put(n.getId(), calculateTechnologyPower(n.getId()));
            }
        }
        
        return powers.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Метод для получения всех технологий в формате, подходящем для GUI-карточек
     * Используется новой системой GUI с карточками
     */
    public List<com.axiom.domain.model.TechNode> getTechNodesForGUI(UUID playerUUID) {
        List<com.axiom.domain.model.TechNode> guiTechNodes = new ArrayList<>();
        String nationId = getNationIdForPlayer(playerUUID);
        
        // Преобразуем существующие технологии в формат TechNode для GUI
        for (Technology tech : technologies.values()) {
            // Создаем новый объект TechNode с информацией о статусе для конкретного игрока/нации
            com.axiom.domain.model.TechNode guiTech = new com.axiom.domain.model.TechNode(
                tech.id,
                tech.name,
                tech.description, 
                getMaterialForTech(tech),
                (int) tech.researchCost // Преобразуем double в int для GUI
            );
            
            // Устанавливаем пререквизиты
            guiTech.setPrerequisites(new ArrayList<>(tech.prerequisites));
            
            // Устанавливаем статус технологии
            if (nationId == null) {
                guiTech.setStatus(com.axiom.domain.model.TechNode.TechStatus.LOCKED);
            } else if (isTechnologyUnlocked(nationId, tech.id)) {
                guiTech.setStatus(com.axiom.domain.model.TechNode.TechStatus.UNLOCKED);
            } else {
                if (canResearchTechnology(nationId, tech.id)) {
                    guiTech.setStatus(com.axiom.domain.model.TechNode.TechStatus.AVAILABLE);
                } else {
                    guiTech.setStatus(com.axiom.domain.model.TechNode.TechStatus.LOCKED);
                }
            }
            
            guiTechNodes.add(guiTech);
        }
        
        return guiTechNodes;
    }
    
    /**
     * Получить Material для отображения технологии в GUI
     */
    public org.bukkit.Material getMaterialForTech(Technology tech) {
        // Определяем материал в зависимости от ветки и уровня технологии
        switch (tech.branch) {
            case "military":
                switch (tech.tier) {
                    case 1: return org.bukkit.Material.WOODEN_SWORD;
                    case 2: return org.bukkit.Material.STONE_SWORD;
                    case 3: return org.bukkit.Material.IRON_SWORD;
                    case 4: return org.bukkit.Material.DIAMOND_SWORD;
                    case 5: return org.bukkit.Material.NETHERITE_SWORD;
                    default: return org.bukkit.Material.IRON_SWORD;
                }
            case "economy":
                switch (tech.tier) {
                    case 1: return org.bukkit.Material.EMERALD;
                    case 2: return org.bukkit.Material.EMERALD_BLOCK;
                    case 3: return org.bukkit.Material.BEACON;
                    case 4: return org.bukkit.Material.DIAMOND_BLOCK;
                    case 5: return org.bukkit.Material.NETHER_STAR;
                    default: return org.bukkit.Material.EMERALD;
                }
            case "industry":
                switch (tech.tier) {
                    case 1: return org.bukkit.Material.CRAFTING_TABLE;
                    case 2: return org.bukkit.Material.BLAST_FURNACE;
                    case 3: return org.bukkit.Material.SMOKER;
                    case 4: return org.bukkit.Material.ENCHANTING_TABLE;
                    case 5: return org.bukkit.Material.BEACON;
                    default: return org.bukkit.Material.CRAFTING_TABLE;
                }
            case "infrastructure":
                switch (tech.tier) {
                    case 1: return org.bukkit.Material.OAK_PLANKS;
                    case 2: return org.bukkit.Material.COBBLESTONE;
                    case 3: return org.bukkit.Material.BRICKS;
                    case 4: return org.bukkit.Material.NETHER_BRICKS;
                    case 5: return org.bukkit.Material.END_STONE;
                    default: return org.bukkit.Material.OAK_PLANKS;
                }
            case "science":
                switch (tech.tier) {
                    case 1: return org.bukkit.Material.BOOK;
                    case 2: return org.bukkit.Material.EXPERIENCE_BOTTLE;
                    case 3: return org.bukkit.Material.ENCHANTING_TABLE;
                    case 4: return org.bukkit.Material.END_CRYSTAL;
                    case 5: return org.bukkit.Material.CONDUIT;
                    default: return org.bukkit.Material.BOOK;
                }
            default:
                return org.bukkit.Material.BEACON;
        }
    }
    
    /**
     * Получить ID нации по UUID игрока
     * Временная реализация - в реальности должна использовать NationManager
     */
    private String getNationIdForPlayer(UUID playerUUID) {
        // В реальной реализации нужно получить нацию игрока из NationManager
        com.axiom.domain.model.Nation nation = plugin.getNationManager().getPlayerNation(playerUUID);
        return nation != null ? nation.getId() : null;
    }
    
    /**
     * Изучить технологию игроком (новый метод для GUI-системы)
     * В отличие от старого метода researchTechnology, этот работает с UUID игрока
     */
    public boolean learnTech(UUID playerUUID, String techId) {
        ResearchResult result = attemptResearch(playerUUID, techId);
        return result.success;
    }
    
    /**
     * Проверить, изучена ли технология игроком
     */
    public boolean isPlayerHasTech(UUID playerUUID, String techId) {
        String nationId = getNationIdForPlayer(playerUUID);
        if (nationId == null) return false;
        return isTechnologyUnlocked(nationId, techId);
    }
    
    /**
     * Проверить, может ли игрок изучить технологию
     */
    public boolean canPlayerLearnTech(UUID playerUUID, String techId) {
        Technology tech = technologies.get(techId);
        if (tech == null) return false;
        String nationId = getNationIdForPlayer(playerUUID);
        if (nationId == null) return false;
        return canResearchTechnology(nationId, techId);
    }
}
