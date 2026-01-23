package com.axiom.gui;

import com.axiom.model.Nation;
import com.axiom.service.DiplomacySystem;
import com.axiom.service.NationManager;
import com.axiom.service.ServiceLocator;
import com.axiom.service.TreatyService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.axiom.service.AdvancedWarSystem;

public class DiplomacyMenu implements Listener {

    public DiplomacyMenu() {
        // Empty constructor
    }

    public void open(Player p) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage("§cВы не в нации."); return; }
        Nation n = opt.get();
        
        String title = GuiUtils.formatHeader("Дипломатия: " + n.getName());
        Inventory inv = GuiUtils.createMenu(title, 4);
        
        List<String> allies = new ArrayList<>(n.getAllies());
        List<String> allyLore = new ArrayList<>();
        allyLore.add("§a§lСоюзники: §f" + allies.size());
        allyLore.add(" ");
        if (allies.isEmpty()) {
            allyLore.add("§7Нет союзников");
        } else {
            for (String allyId : allies) {
                Nation ally = nationManager.getNationById(allyId);
                if (ally != null) {
                    allyLore.add("§a✓ " + ally.getName());
                } else {
                    allyLore.add("§7" + allyId);
                }
            }
        }
        allyLore.add(" ");
        allyLore.add("§7ЛКМ: Запросить альянс");
        inv.setItem(11, GuiUtils.button(Material.TURTLE_HELMET, "§a§lАльянсы", allyLore));
        
        List<String> enemies = new ArrayList<>(n.getEnemies());
        List<String> warLore = new ArrayList<>();
        warLore.add("§c§lВРАГИ: §f" + enemies.size());
        warLore.add(" ");
        
        int activeWars = 0;
        AdvancedWarSystem advancedWarSystem = ServiceLocator.get(AdvancedWarSystem.class);
        if (advancedWarSystem != null) {
            List<AdvancedWarSystem.War> wars = advancedWarSystem.getNationWars(n.getId());
            activeWars = wars.size();
            
            if (!wars.isEmpty()) {
                warLore.add("§c§lАКТИВНЫЕ ВОЙНЫ:");
                for (AdvancedWarSystem.War war : wars) {
                    boolean isAttacker = war.attackerId.equals(n.getId());
                    String enemyId = isAttacker ? war.defenderId : war.attackerId;
                    Nation enemy = nationManager.getNationById(enemyId);
                    String enemyName = enemy != null ? enemy.getName() : enemyId;
                    
                    warLore.add("§c⚔ " + enemyName);
                    warLore.add("§7  Тип: §f" + war.type.name());
                    warLore.add("§7  Статус: §f" + war.status.name());
                    warLore.add("§7  Битв: §f" + war.battlesFought);
                    
                    if (!war.fronts.isEmpty()) {
                        double avgProgress = war.fronts.values().stream()
                            .mapToDouble(f -> f.attackerProgress)
                            .average()
                            .orElse(0);
                        warLore.add("§7  Прогресс: §" + (isAttacker ? "a" : "c") + String.format("%.1f", isAttacker ? avgProgress : 100 - avgProgress) + "%");
                    }
                }
                warLore.add(" ");
            }
        }
        
        DiplomacySystem diplomacySystem = ServiceLocator.get(DiplomacySystem.class);
        for (String enemyId : enemies) {
            if (diplomacySystem.isAtWar(n.getId(), enemyId)) {
                if (activeWars == 0) activeWars++;
                Nation enemy = nationManager.getNationById(enemyId);
                if (!warLore.contains("§c⚔ " + (enemy != null ? enemy.getName() : enemyId))) {
                    warLore.add("§c⚔ " + (enemy != null ? enemy.getName() : enemyId) + " §7(Активна)");
                }
            } else {
                Nation enemy = nationManager.getNationById(enemyId);
                warLore.add("§7" + (enemy != null ? enemy.getName() : enemyId));
            }
        }
        if (enemies.isEmpty() && activeWars == 0) {
            warLore.add("§7Нет врагов");
        }
        warLore.add(" ");
        warLore.add("§fВсего врагов: §c" + enemies.size());
        warLore.add("§fАктивных войн: §c" + activeWars);
        warLore.add(" ");
        
        if (diplomacySystem != null) {
            Map<String, Object> dipStats = diplomacySystem.getDiplomaticStatistics(n.getId());
            double avgRep = dipStats.containsKey("averageReputation") ? (Double) dipStats.get("averageReputation") : 0;
            warLore.add("§7Средняя репутация: §" + (avgRep >= 0 ? "a" : "c") + String.format("%.1f", avgRep));
        }
        
        Nation.Role role = n.getRole(p.getUniqueId());
        boolean canDeclare = (role == Nation.Role.LEADER || role == Nation.Role.GENERAL);
        if (canDeclare) {
            warLore.add(" ");
            warLore.add("§7ЛКМ: Объявить войну");
            warLore.add("§c⚠ Требует подтверждения!");
        } else {
            warLore.add(" ");
            warLore.add("§cНедостаточно прав");
        }
        inv.setItem(13, GuiUtils.button(Material.CROSSBOW, canDeclare ? "§c§lВойна" : "§7Война", warLore));
        
        List<String> repLore = new ArrayList<>();
        if (!n.getReputation().isEmpty()) {
            repLore.add("§6§lРепутация:");
            repLore.add(" ");
            int count = 0;
            for (Map.Entry<String, Integer> entry : n.getReputation().entrySet()) {
                if (count++ > 7) break;
                Nation target = nationManager.getNationById(entry.getKey());
                String name = target != null ? target.getName() : entry.getKey();
                int rep = entry.getValue();
                String color = rep >= 50 ? "§a" : rep >= 0 ? "§e" : "§c";
                repLore.add(color + name + ": §f" + rep);
            }
        } else {
            repLore.add("§7Нет данных о репутации");
        }
        inv.setItem(15, GuiUtils.button(Material.PAPER, "§6§lРепутация", repLore));
        
        List<String> treatiesList = new ArrayList<>();
        treatiesList.add("§b§lДоговоры:");
        treatiesList.add(" ");
        
        List<String> treaties = getTreatiesForNation(n);
        if (treaties.isEmpty()) {
            treatiesList.add("§7Нет активных договоров");
        } else {
            for (String treaty : treaties) {
                treatiesList.add("§a✓ " + treaty);
            }
        }
        treatiesList.add(" ");
        Nation.Role treatyRole = n.getRole(p.getUniqueId());
        boolean canCreate = (treatyRole == Nation.Role.LEADER || treatyRole == Nation.Role.MINISTER);
        if (canCreate) {
            treatiesList.add("§7ЛКМ: Создать договор");
        } else {
            treatiesList.add("§cНедостаточно прав");
        }
        inv.setItem(17, GuiUtils.button(Material.WRITTEN_BOOK, "§b§lДоговоры", treatiesList));
        
        List<String> pending = new ArrayList<>(n.getPendingAlliance());
        if (!pending.isEmpty()) {
            List<String> pendingLore = new ArrayList<>();
            pendingLore.add("§e§lОжидающие запросы:");
            pendingLore.add(" ");
            for (String req : pending) {
                if (req.startsWith("in:")) {
                    String reqNationId = req.substring(3);
                    Nation reqNation = nationManager.getNationById(reqNationId);
                    pendingLore.add("§e→ От " + (reqNation != null ? reqNation.getName() : reqNationId));
                    pendingLore.add("§7ЛКМ: Принять");
                } else if (req.startsWith("out:")) {
                    String reqNationId = req.substring(4);
                    Nation reqNation = nationManager.getNationById(reqNationId);
                    pendingLore.add("§7→ К " + (reqNation != null ? reqNation.getName() : reqNationId));
                    pendingLore.add("§7Ожидание ответа...");
                }
            }
            inv.setItem(22, GuiUtils.button(Material.EMERALD, "§e§lЗапросы альянса", pendingLore));
        }
        
        inv.setItem(31, GuiUtils.createCloseButton());
        
        p.openInventory(inv);
    }


    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Дипломатия")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
        int slot = e.getRawSlot();
        
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) return;
        Nation n = opt.get();
        
        if (slot == 13) {
            Nation.Role role = n.getRole(p.getUniqueId());
            if (role != Nation.Role.LEADER && role != Nation.Role.GENERAL) {
                p.sendMessage("§cНедостаточно прав. Требуется: LEADER или GENERAL");
                return;
            }
            p.closeInventory();
            p.sendMessage("§7Объявление войны:");
            p.sendMessage("§b/axiom diplomacy declare-war <nationId>");
            p.sendMessage("§7Откроется окно подтверждения.");
        } else if (slot == 11) {
            p.closeInventory();
            p.sendMessage("§7Запрос альянса:");
            p.sendMessage("§b/axiom diplomacy ally <nationId>");
        } else if (slot == 22) {
            List<String> pending = new ArrayList<>(n.getPendingAlliance());
            for (String req : pending) {
                if (req.startsWith("in:")) {
                    String reqNationId = req.substring(3);
                    p.sendMessage("§7Принять альянс:");
                    p.sendMessage("§b/axiom diplomacy accept-ally " + reqNationId);
                    break;
                }
            }
        } else if (slot == 17) {
            Nation.Role role = n.getRole(p.getUniqueId());
            if (role != Nation.Role.LEADER && role != Nation.Role.MINISTER) {
                p.sendMessage("§cНедостаточно прав. Требуется: LEADER или MINISTER");
                return;
            }
            p.closeInventory();
            p.sendMessage("§7Создание договора:");
            p.sendMessage("§b/axiom diplomacy treaty create <nationId> <type> <days>");
            p.sendMessage("§7Типы: nap (ненападение), trade (торговля), military (военный)");
        } else if (slot == 31) {
            p.closeInventory();
        }
    }
    
    private List<String> getTreatiesForNation(Nation nation) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        TreatyService treatyService = ServiceLocator.get(TreatyService.class);
        List<String> result = new ArrayList<>();
        for (Nation other : nationManager.getAll()) {
            if (other.getId().equals(nation.getId())) continue;
            
            if (treatyService.hasTreaty(nation.getId(), other.getId(), "nap")) {
                result.add("§aНенападение: " + other.getName());
            }
            if (treatyService.hasTreaty(nation.getId(), other.getId(), "trade")) {
                result.add("§eТорговля: " + other.getName());
            }
            if (treatyService.hasTreaty(nation.getId(), other.getId(), "military")) {
                result.add("§cВоенный: " + other.getName());
            }
        }
        return result;
    }
}


