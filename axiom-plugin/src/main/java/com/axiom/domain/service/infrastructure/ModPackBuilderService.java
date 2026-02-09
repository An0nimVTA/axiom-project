package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import com.axiom.domain.model.Mod;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

/**
 * Улучшенная система модов с возможностью создания кастомных модпаков
 */
public class ModPackBuilderService {
    private final AXIOM plugin;
    private final File modPackDir;
    private final Map<String, ModPack> modPacks;
    
    /**
     * Класс, представляющий модпак
     */
    public static class ModPack {
        private String id;
        private String name;
        private String description;
        private String version;
        private String serverType; // modern, medieval, magic, minigames
        private List<String> requiredMods;
        private List<String> optionalMods;
        private Map<String, Boolean> modConfig; // мод -> включен ли
        private String createdBy;
        private long createdAt;
        
        public ModPack(String id, String name, String description, String version) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.version = version;
            this.requiredMods = new ArrayList<>();
            this.optionalMods = new ArrayList<>();
            this.modConfig = new HashMap<>();
            this.createdAt = System.currentTimeMillis();
        }
        
        // Геттеры и сеттеры
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getVersion() { return version; }
        public String getServerType() { return serverType; }
        public List<String> getRequiredMods() { return requiredMods; }
        public List<String> getOptionalMods() { return optionalMods; }
        public Map<String, Boolean> getModConfig() { return modConfig; }
        public String getCreatedBy() { return createdBy; }
        public long getCreatedAt() { return createdAt; }
        
        public void setServerType(String serverType) { this.serverType = serverType; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        
        public void addRequiredMod(String modName) {
            if (modName == null) return;
            requiredMods.add(modName);
            modConfig.put(modName, true);
        }
        
        public void addOptionalMod(String modName) {
            if (modName == null) return;
            optionalMods.add(modName);
            modConfig.put(modName, false);
        }
        
        
        public boolean isModEnabled(String modName) {
            return modConfig.getOrDefault(modName, false);
        }
        
        // Новые методы для работы с объектами модов
        private final List<Mod> modObjects = new ArrayList<>();
        
        /**
         * Добавить мод в модпак
         */
        public void addMod(Mod mod) {
            if (mod == null) return;
            if (!modObjects.contains(mod)) {
                modObjects.add(mod);
                // Устанавливаем начальное состояние для мода
                modConfig.put(mod.getId(), mod.isEnabled());
            }
        }
        
        /**
         * Удалить мод из модпака
         */
        public void removeMod(String modId) {
            if (modId == null) return;
            modObjects.removeIf(mod -> mod.getId().equals(modId));
            modConfig.remove(modId);
        }
        
        /**
         * Получить мод по ID
         */
        public Mod getMod(String modId) {
            if (modId == null) return null;
            return modObjects.stream()
                .filter(mod -> mod.getId().equals(modId))
                .findFirst()
                .orElse(null);
        }
        
        /**
         * Получить все моды в модпаке
         */
        public List<Mod> getMods() {
            return new ArrayList<>(modObjects);
        }
        
        /**
         * Установить состояние мода (через объект мода)
         */
        public void setModEnabled(String modId, boolean enabled) {
            if (modId == null) return;
            Mod mod = getMod(modId);
            if (mod != null) {
                mod.setEnabled(enabled);
            }
            modConfig.put(modId, enabled);
        }
    }
    
    public ModPackBuilderService(AXIOM plugin) {
        this.plugin = plugin;
        this.modPackDir = new File(plugin.getDataFolder(), "modpacks");
        this.modPacks = new HashMap<>();
        
        modPackDir.mkdirs();
        
        // Инициализация базовых модпаков
        initializeDefaultModPacks();
    }
    
    /**
     * Инициализация дефолтных модпаков
     */
    private void initializeDefaultModPacks() {
        // Современный модпак (технологии, боевая техника)
        ModPack modernModPack = new ModPack("modern", "Современные технологии", 
            "Модпак с современными технологиями и вооружением", "1.0.0");
        modernModPack.setServerType("modern");
        
        // Добавляем основные моды для современного сервера
        modernModPack.addRequiredMod("ImmersiveEngineering"); // промышленность
        modernModPack.addRequiredMod("Mekanism"); // энергия и машины
        modernModPack.addRequiredMod("TinkersConstruct"); // инструменты
        modernModPack.addRequiredMod("SimplyJetpacks2"); // реактивные ранцы
        modernModPack.addRequiredMod("TeslaCoils"); // тесла-катушки
        modernModPack.addRequiredMod("AdvancedRocketry"); // космос
        
        // Добавляем опциональные моды
        modernModPack.addOptionalMod("IC2"); // энергетика
        modernModPack.addOptionalMod("Forestry"); // ведение хозяйства
        modernModPack.addOptionalMod("Railcraft"); // железные дороги
        modernModPack.addOptionalMod("ThermalFoundation"); // термал-серия
        
        modPacks.put("modern", modernModPack);
        
        // Средневековый модпак
        ModPack medievalModPack = new ModPack("medieval", "Средневековье", 
            "Модпак, воссоздающий средневековую атмосферу", "1.0.0");
        medievalModPack.setServerType("medieval");
        
        medievalModPack.addRequiredMod("MedievalMachines"); // средневековые механизмы
        medievalModPack.addRequiredMod("ArchitecturyAPI"); // строительство
        medievalModPack.addRequiredMod("Structurize"); // постройки
        medievalModPack.addRequiredMod("Bountiful"); // задания
        medievalModPack.addRequiredMod("FarmingForBlockheads"); // сельское хозяйство
        
        modPacks.put("medieval", medievalModPack);
        
        // Магический модпак
        ModPack magicModPack = new ModPack("magic", "Магия и чародейство", 
            "Модпак с магическими системами и волшебством", "1.0.0");
        magicModPack.setServerType("magic");
        
        magicModPack.addRequiredMod("Botania"); // магическая флора
        magicModPack.addRequiredMod("BloodMagic"); // ритуалы крови
        magicModPack.addRequiredMod("Thaumcraft"); // тауматургия
        magicModPack.addRequiredMod("ArsMagica2"); // арс магика
        magicModPack.addRequiredMod("EmbersRekindled"); // эмбер-магия
        
        modPacks.put("magic", magicModPack);
        
        // Модпак мини-игр
        ModPack minigamesModPack = new ModPack("minigames", "Мини-игры", 
            "Модпак для веселых мини-игр и развлечений", "1.0.0");
        minigamesModPack.setServerType("minigames");
        
        minigamesModPack.addRequiredMod("GameStages"); // стадии игры
        minigamesModPack.addRequiredMod("Placebo"); // утилиты
        minigamesModPack.addRequiredMod("Quark"); // улучшения игры
        minigamesModPack.addRequiredMod("AutoRegLib"); // библиотека модов
        minigamesModPack.addRequiredMod("Charm"); // колдовские улучшения
        
        modPacks.put("minigames", minigamesModPack);
    }
    
    /**
     * Создать новый модпак
     */
    public boolean createModPack(Player creator, String modPackId, String name, String description, String version, String serverType) {
        if (creator == null || isBlank(modPackId) || isBlank(name) || isBlank(version)) {
            return false;
        }
        if (modPacks.containsKey(modPackId)) {
            return false; // Модпак с таким ID уже существует
        }
        
        ModPack newModPack = new ModPack(modPackId, name, description, version);
        newModPack.setCreatedBy(creator.getName());
        newModPack.setServerType(serverType);
        
        modPacks.put(modPackId, newModPack);
        
        // Сохранить модпак в файл
        saveModPack(newModPack);
        
        return true;
    }
    
    /**
     * Получить модпак по ID
     */
    public ModPack getModPack(String modPackId) {
        return modPacks.get(modPackId);
    }
    
    /**
     * Получить все модпаки
     */
    public Collection<ModPack> getAllModPacks() {
        return modPacks.values();
    }
    
    /**
     * Метод для получения всех модпаков (для внутреннего использования)
     */
    public Map<String, ModPack> getAllModPacksMap() {
        return modPacks;
    }
    
    /**
     * Получить модпаки для определенного типа сервера
     */
    public List<ModPack> getModPacksByServerType(String serverType) {
        List<ModPack> result = new ArrayList<>();
        if (serverType == null) return result;
        for (ModPack modPack : modPacks.values()) {
            if (serverType.equals(modPack.getServerType())) {
                result.add(modPack);
            }
        }
        return result;
    }
    
    /**
     * Удалить модпак
     */
    public boolean deleteModPack(String modPackId) {
        if (isBlank(modPackId)) return false;
        ModPack removed = modPacks.remove(modPackId);
        if (removed != null) {
            // Удалить файл модпака
            File modPackFile = new File(modPackDir, modPackId + ".json");
            if (modPackFile.exists()) {
                modPackFile.delete();
            }
            return true;
        }
        return false;
    }
    
    /**
     * Сохранить модпак в файл
     */
    private void saveModPack(ModPack modPack) {
        // В реальной реализации можно сохранять в JSON
        // Для упрощения в этой версии мы сохраняем в памяти
        // Используйте Gson для сериализации в файл
    }
    
    /**
     * Загрузить модпак из файла
     */
    public ModPack loadModPack(String modPackId) {
        // В реальной реализации загружаем из JSON
        return modPacks.get(modPackId);
    }
    
    /**
     * Запустить сборку модпака
     */
    public boolean isModCompatibleWithTech(String modName, String techId) {
        if (isBlank(modName) || isBlank(techId)) return false;
        // Определяем, какой технологии соответствует мод
        Map<String, List<String>> modTechCompatibility = new HashMap<>();
        
        // Заполняем маппинг модов и технологий
        modTechCompatibility.put("ImmersiveEngineering", Arrays.asList("advanced_industry", "industrial_engineering"));
        modTechCompatibility.put("Mekanism", Arrays.asList("advanced_energy", "energy_processing"));
        modTechCompatibility.put("TinkersConstruct", Arrays.asList("advanced_tools", "metallurgy"));
        modTechCompatibility.put("SimplyJetpacks2", Arrays.asList("aviation", "transportation_tech"));
        modTechCompatibility.put("TeslaCoils", Arrays.asList("electrical_engineering", "energy_weapons"));
        modTechCompatibility.put("AdvancedRocketry", Arrays.asList("space_program", "rocket_science"));
        modTechCompatibility.put("IC2", Arrays.asList("energy_generation", "industrialization"));
        modTechCompatibility.put("Forestry", Arrays.asList("agriculture", "resource_production"));
        modTechCompatibility.put("Railcraft", Arrays.asList("transportation_networks", "logistics"));
        modTechCompatibility.put("ThermalFoundation", Arrays.asList("thermal_energy", "resource_processing"));
        modTechCompatibility.put("Botania", Arrays.asList("botanical_magic", "mana_theory"));
        modTechCompatibility.put("BloodMagic", Arrays.asList("life_magic", "rituals"));
        modTechCompatibility.put("Thaumcraft", Arrays.asList("thaumaturgy", "arcane_knowledge"));
        modTechCompatibility.put("ArsMagica2", Arrays.asList("elemental_magic", "spell_crafting"));
        modTechCompatibility.put("EmbersRekindled", Arrays.asList("ember_mastery", "arcane_mechanics"));
        
        List<String> compatibleTechs = modTechCompatibility.get(modName);
        return compatibleTechs != null && compatibleTechs.contains(techId);
    }
    
    /**
     * Собрать модпак (создать файл архива)
     */
    public boolean buildModPack(String modPackId, Player player) {
        ModPack modPack = getModPack(modPackId);
        if (modPack == null) {
            return false;
        }
        
        // Проверяем, какие моды разблокированы технологиями игрока
        List<String> enabledMods = new ArrayList<>();
        if (modPack.getModConfig() != null) {
            for (String modName : modPack.getModConfig().keySet()) {
                // Проверяем, что мод включен и совместим с изученными технологиями
                if (modPack.isModEnabled(modName)) {
                    enabledMods.add(modName);
                }
            }
        }
        
        // Здесь произошла бы реальная сборка модпака
        // Включая создание архива ZIP с необходимыми JAR файлами
        return true;
    }
    
    /**
     * Получить список модпаков, доступных для игрока
     */
    public List<ModPack> getAvailableModPacksForPlayer(Player player) {
        List<ModPack> available = new ArrayList<>();
        
        for (ModPack modPack : getAllModPacks()) {
            // Проверяем, может ли игрок использовать этот модпак
            boolean canUse = true; // В реальной системе проверяется на основе технологий
            
            if (canUse) {
                available.add(modPack);
            }
        }
        
        return available;
    }
    
    /**
     * Добавить мод в модпак
     */
    public void addModToModPack(String modPackId, Mod mod) {
        if (isBlank(modPackId) || mod == null) return;
        ModPack modPack = modPacks.get(modPackId);
        if (modPack != null) {
            modPack.addMod(mod);
            saveModPack(modPack);
        }
    }
    
    /**
     * Удалить мод из модпака
     */
    public void removeModFromModPack(String modPackId, String modId) {
        if (isBlank(modPackId) || isBlank(modId)) return;
        ModPack modPack = modPacks.get(modPackId);
        if (modPack != null) {
            modPack.removeMod(modId);
            saveModPack(modPack);
        }
    }
    
    /**
     * Проверить совместимость модов в модпаке
     */
    public List<String> checkModCompatibility(String modPackId) {
        ModPack modPack = modPacks.get(modPackId);
        if (modPack == null) return new ArrayList<>();
        
        List<String> conflicts = new ArrayList<>();
        
        // Проверяем конфликты между модами
        for (Mod mod1 : modPack.getMods()) {
            if (mod1 == null) continue;
            if (!mod1.isEnabled()) continue; // Проверяем только включенные моды
            
            for (Mod mod2 : modPack.getMods()) {
                if (mod2 == null) continue;
                if (!mod2.isEnabled() || mod1.getId().equals(mod2.getId())) continue;
                
                // Проверяем конфликты
                if (mod1.getConflicts() != null && mod1.getConflicts().contains(mod2.getId())) {
                    conflicts.add("Моды " + mod1.getName() + " и " + mod2.getName() + " несовместимы!");
                }
                
                // Проверяем зависимости
                if (mod1.getDependencies() != null && mod1.getDependencies().contains(mod2.getId()) && !mod2.isEnabled()) {
                    conflicts.add("Мод " + mod1.getName() + " требует включенный мод " + mod2.getName() + "!");
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Установить состояние мода в модпаке
     */
    public void setModState(String modPackId, String modId, boolean enabled) {
        if (isBlank(modPackId) || isBlank(modId)) return;
        ModPack modPack = modPacks.get(modPackId);
        if (modPack != null) {
            modPack.setModEnabled(modId, enabled);
        }
    }
    
    /**
     * Проверить, есть ли мод в модпаке
     */
    public boolean hasMod(String modPackId, String modId) {
        if (isBlank(modPackId) || isBlank(modId)) return false;
        ModPack modPack = modPacks.get(modPackId);
        return modPack != null && modPack.getMod(modId) != null;
    }
    
    /**
     * Получить список доступных модов
     */
    public List<Mod> getAvailableMods() {
        List<Mod> availableMods = new ArrayList<>();
        
        // Создаем список базовых модов, доступных в системе
        availableMods.add(new Mod("ImmersiveEngineering", "Immersive Engineering", "Промышленные машины и технологии", "1.0.0"));
        availableMods.add(new Mod("Mekanism", "Mekanism", "Энергия и автоматизация", "1.0.0"));
        availableMods.add(new Mod("TinkersConstruct", "Tinkers Construct", "Кастомные инструменты", "1.0.0"));
        availableMods.add(new Mod("SimplyJetpacks2", "Simply Jetpacks 2", "Реактивные ранцы", "1.0.0"));
        availableMods.add(new Mod("TeslaCoils", "Tesla Coils", "Электрические устройства", "1.0.0"));
        availableMods.add(new Mod("AdvancedRocketry", "Advanced Rocketry", "Космические технологии", "1.0.0"));
        availableMods.add(new Mod("Botania", "Botania", "Магическая флора", "1.0.0"));
        availableMods.add(new Mod("BloodMagic", "Blood Magic", "Ритуалы крови", "1.0.0"));
        availableMods.add(new Mod("Thaumcraft", "Thaumcraft", "Тауматургия", "1.0.0"));
        availableMods.add(new Mod("ArsMagica2", "Ars Magica 2", "Магические системы", "1.0.0"));
        
        return availableMods;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
