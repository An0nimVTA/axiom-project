package com.axiom.technology.service;

import com.axiom.core.EventPublisher;
import com.axiom.core.service.NationManager;
import com.axiom.technology.model.TechNode;

import java.util.*;

/**
 * Manages technology tree and nation research progress.
 */
public class TechnologyService {
    private final NationManager nationManager;
    private final EventPublisher eventPublisher;
    
    // nationId -> Set<TechID>
    private final Map<String, Set<String>> unlockedTechs = new HashMap<>();
    
    // TechID -> Node
    private final Map<String, TechNode> techTree = new HashMap<>();

    public TechnologyService(NationManager nationManager, EventPublisher eventPublisher) {
        this.nationManager = nationManager;
        this.eventPublisher = eventPublisher;
        initDefaultTree();
    }

    private void initDefaultTree() {
        // --- STONE AGE ---
        TechNode mining = new TechNode("mining", "Горное дело", "minecraft:iron_pickaxe", 150, "economy");
        mining.shortDescription = "Добыча руды и камня.";
        mining.deepDescription = "Позволяет строить шахты 2 уровня и эффективно добывать железо. Открывает доступ к геологии.";
        mining.addStatModifier("minecraft:iron_pickaxe", "efficiency", 1.2);
        registerTech(mining);

        // --- BRONZE AGE ---
        TechNode bronze = new TechNode("bronze_working", "Бронза", "minecraft:copper_ingot", 200, "military");
        bronze.requirements.add("mining");
        bronze.shortDescription = "Сплав меди и олова.";
        bronze.deepDescription = "Бронзовое оружие прочнее каменного. Увеличивает прочность всех инструментов на 20%.";
        bronze.addStatModifier("minecraft:golden_sword", "damage", 1.5); // Buff gold/bronze items
        registerTech(bronze);

        // --- IRON AGE ---
        TechNode iron = new TechNode("iron_working", "Железо", "minecraft:iron_ingot", 300, "military");
        iron.requirements.add("bronze_working");
        iron.shortDescription = "Обработка железа.";
        iron.deepDescription = "Железная революция. Открывает доступ к тяжелой броне и ковке оружия.";
        iron.addRecipeUnlock("minecraft:iron_chestplate");
        registerTech(iron);
        
        // --- INDUSTRIAL ---
        TechNode steam = new TechNode("steam_power", "Сила Пара", "minecraft:campfire", 500, "science");
        steam.requirements.add("iron_working");
        steam.shortDescription = "Энергия пара и машин.";
        steam.deepDescription = "Начало индустриализации. Позволяет создавать паровые двигатели и автоматизировать производство.";
        registerTech(steam);
    }
    
    public void registerTech(TechNode node) {
        techTree.put(node.id, node);
    }

    public boolean hasTech(String nationId, String techId) {
        return unlockedTechs.getOrDefault(nationId, Collections.emptySet()).contains(techId);
    }
    
    public boolean canResearch(String nationId, String techId) {
        if (hasTech(nationId, techId)) return false;
        TechNode node = techTree.get(techId);
        if (node == null) return false;
        
        for (String req : node.requirements) {
            if (!hasTech(nationId, req)) return false;
        }
        return true;
    }
    
    public String research(String nationId, String techId) {
        if (!canResearch(nationId, techId)) return "Технология недоступна (нужны требования).";
        
        // TODO: Check and deduct research points/money
        
        unlockedTechs.computeIfAbsent(nationId, k -> new HashSet<>()).add(techId);
        return "Технология изучена: " + techTree.get(techId).name;
    }
    
    public List<TechNode> getAvailableTechs(String nationId) {
        List<TechNode> available = new ArrayList<>();
        for (TechNode node : techTree.values()) {
            if (canResearch(nationId, node.id)) {
                available.add(node);
            }
        }
        return available;
    }
    
    public TechNode getNode(String id) {
        return techTree.get(id);
    }
}
