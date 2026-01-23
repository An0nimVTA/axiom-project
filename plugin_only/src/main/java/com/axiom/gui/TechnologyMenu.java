package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.TechnologyTreeService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for technology tree - research, view progress, manage branches.
 */
public class TechnologyMenu implements Listener {
    private final AXIOM plugin;
    private final TechnologyTreeService techService;

    public TechnologyMenu(AXIOM plugin) {
        this.plugin = plugin;
        this.techService = plugin.getTechnologyTreeService();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player player) {
        String nationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (nationId == null) {
            player.sendMessage("§cВы не состоите в нации.");
            return;
        }

        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return;

        // Beautiful header with gradient
        String title = GuiUtils.formatHeader("Технологии: " + n.getName());
        Inventory inv = GuiUtils.createMenu(title, 6);

        // Branch buttons (top row - slots 1-5, skip borders)
        int branchSlot = 1;
        Material[] branchIcons = {Material.IRON_SWORD, Material.FURNACE, Material.GOLD_INGOT, 
                                  Material.RAIL, Material.ENCHANTED_BOOK};
        int iconIndex = 0;
        
        for (TechnologyTreeService.ResearchBranch branch : techService.getAllBranches()) {
            List<String> lore = new ArrayList<>();
            lore.add("§3§l─── " + branch.name.toUpperCase() + " ───");
            lore.add(" ");
            lore.add("§7" + branch.description);
            lore.add(" ");
            double progress = techService.getBranchProgress(nationId, branch.id);
            String progressColor = progress >= 75 ? "§a" : progress >= 50 ? "§e" : progress >= 25 ? "§6" : "§7";
            lore.add("§fПрогресс: " + progressColor + String.format("%.1f", progress) + "%");
            lore.add(" ");
            lore.add("§7ЛКМ: Просмотр технологий");
            
            Material icon = iconIndex < branchIcons.length ? branchIcons[iconIndex] : Material.BOOK;
            inv.setItem(branchSlot++, GuiUtils.button(icon, "§b§l" + branch.name, lore));
            iconIndex++;
        }

        // Available technologies (middle section - slots 10-43)
        List<TechnologyTreeService.Technology> available = techService.getAvailableTechs(nationId);
        int availableSlot = 10;
        for (TechnologyTreeService.Technology tech : available) {
            if (availableSlot > 43) break;
            ItemStack techItem = createTechItem(tech, nationId);
            inv.setItem(availableSlot++, techItem);
            if ((availableSlot - 10) % 7 == 0) availableSlot += 2; // Next row
        }

        // Info panel (bottom center)
        List<String> infoLore = new ArrayList<>();
        double eduLevel = plugin.getEducationService().getEducationLevel(nationId);
        infoLore.add("§3§l─── ИНФОРМАЦИЯ ───");
        infoLore.add(" ");
        infoLore.add("§fУровень образования: §b" + String.format("%.1f", eduLevel));
        infoLore.add("§fКазна: §b" + String.format("%.0f", n.getTreasury()) + " " + n.getCurrencyCode());
        infoLore.add("§fДоступно технологий: §b" + available.size());
        int totalTechs = techService.getAllTechs().size();
        long unlockedCount = techService.getUnlockedTechs(nationId).size();
        infoLore.add("§fИзучено: §b" + unlockedCount + "/" + totalTechs);
        inv.setItem(49, GuiUtils.button(Material.ENCHANTED_BOOK, "§b§lИнформация", infoLore));
        
        // Close button
        inv.setItem(53, GuiUtils.createCloseButton());

        player.openInventory(inv);
    }

    public void openBranchMenu(Player player, String branchId) {
        String nationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (nationId == null) return;

        TechnologyTreeService.ResearchBranch branch = techService.getAllBranches().stream()
            .filter(b -> b.id.equals(branchId))
            .findFirst()
            .orElse(null);
        if (branch == null) return;

        // Beautiful header
        String title = GuiUtils.formatHeader(branch.name + ": Технологии");
        Inventory inv = GuiUtils.createMenu(title, 6);

        // Show technologies in this branch by tier
        int slot = 9;
        for (int tier = 1; tier <= 5; tier++) {
            // Tier header
            List<String> tierLore = new ArrayList<>();
            tierLore.add("§6§lТИР " + tier);
            tierLore.add(" ");
            tierLore.add("§7Базовый уровень");
            tierLore.add("§7технологий ветки");
            inv.setItem(slot, GuiUtils.button(Material.GOLD_INGOT, "§6§l════ ТИР " + tier + " ════", tierLore));
            slot++;
            if ((slot - 9) % 9 == 0) slot++; // Skip border

            // Technologies in this tier
            for (String techId : branch.techIds) {
                TechnologyTreeService.Technology tech = techService.getTech(techId);
                if (tech == null || tech.tier != tier) continue;
                if (slot > 44) break;
                ItemStack techItem = createTechItem(tech, nationId);
                inv.setItem(slot++, techItem);
                if ((slot - 9) % 9 == 0) slot++; // Skip border
            }
            
            if (slot > 44) break;
        }

        // Back button
        inv.setItem(49, GuiUtils.button(Material.ARROW, "§c§lНазад", 
            List.of("§7Вернуться к главному меню")));
        
        // Close button
        inv.setItem(53, GuiUtils.createCloseButton());

        player.openInventory(inv);
    }

    private ItemStack createTechItem(TechnologyTreeService.Technology tech, String nationId) {
        Material icon = Material.BOOK;
        
        // Different icons by branch
        switch (tech.branch) {
            case "military": icon = Material.IRON_SWORD; break;
            case "industry": icon = Material.FURNACE; break;
            case "economy": icon = Material.GOLD_INGOT; break;
            case "infrastructure": icon = Material.RAIL; break;
            case "science": icon = Material.ENCHANTED_BOOK; break;
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Check if unlocked
        boolean unlocked = techService.isTechnologyUnlocked(nationId, tech.id);
        if (!unlocked) {
            // Check if can research
            List<TechnologyTreeService.Technology> available = techService.getAvailableTechs(nationId);
            boolean canResearch = available.stream().anyMatch(t -> t.id.equals(tech.id));
            
            if (canResearch) {
                meta.setDisplayName("§a" + tech.name);
            } else {
                meta.setDisplayName("§c" + tech.name);
            }
        } else {
            meta.setDisplayName("§b✓ " + tech.name);
        }

        List<String> lore = new ArrayList<>();
        lore.add("§3§l─── " + tech.name.toUpperCase() + " ───");
        lore.add(" ");
        lore.add("§7" + tech.description);
        lore.add(" ");
        lore.add("§fВетка: §b" + getBranchName(tech.branch));
        lore.add("§fТир: §b" + tech.tier);
        lore.add("§fСтоимость: §b" + String.format("%.0f", tech.researchCost) + " §7(казна)");
        
        if (tech.requiredMod != null) {
            boolean modAvailable = plugin.getModIntegrationService().isModAvailable(tech.requiredMod);
            if (tech.modOptional) {
                lore.add("§7Мод (опционально): §f" + tech.requiredMod + (modAvailable ? " §a✓" : " §c✗"));
            } else {
                lore.add("§7Требует мод: §f" + tech.requiredMod + (modAvailable ? " §a✓" : " §c✗"));
            }
        }
        
        lore.add("");
        
        // Prerequisites
        if (!tech.prerequisites.isEmpty()) {
            lore.add(" ");
            lore.add("§e§lТребования:");
            for (String prereq : tech.prerequisites) {
                TechnologyTreeService.Technology prereqTech = techService.getTech(prereq);
                if (prereqTech != null) {
                    // Check if prerequisite is unlocked
                    boolean prereqUnlocked = techService.isTechnologyUnlocked(nationId, prereq);
                    String prereqColor = prereqUnlocked ? "§a" : "§c";
                    lore.add("  " + prereqColor + (prereqUnlocked ? "✓" : "✗") + " §f" + prereqTech.name);
                }
            }
        }

        // Bonuses
        if (!tech.bonuses.isEmpty()) {
            lore.add(" ");
            lore.add("§6§lБонусы:");
            for (String bonusType : tech.bonuses.keySet()) {
                double value = tech.bonuses.get(bonusType);
                String bonusName = getBonusName(bonusType);
                lore.add("  §e+§b" + String.format("%.0f", (value - 1.0) * 100) + "% §7" + bonusName);
            }
        }

        // Action
        lore.add(" ");
        List<TechnologyTreeService.Technology> available = techService.getAvailableTechs(nationId);
        boolean canResearch = available.stream().anyMatch(t -> t.id.equals(tech.id));
        if (unlocked) {
            lore.add("§a§l✓ ИЗУЧЕНО");
        } else if (canResearch) {
            lore.add("§a§lЛКМ: Исследовать");
            lore.add("§7Требует двойного клика");
        } else {
            lore.add("§c§lНЕДОСТУПНО");
            lore.add("§7Выполните требования");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String getBranchName(String branchId) {
        switch (branchId) {
            case "military": return "Военные";
            case "industry": return "Промышленность";
            case "economy": return "Экономика";
            case "infrastructure": return "Инфраструктура";
            case "science": return "Наука";
            default: return branchId;
        }
    }

    private String getBonusName(String bonusType) {
        switch (bonusType) {
            case "warStrength": return "Военная сила";
            case "weaponDamage": return "Урон оружия";
            case "siegeStrength": return "Осада";
            case "defenseBonus": return "Защита";
            case "productionBonus": return "Производство";
            case "energyEfficiency": return "Энергоэффективность";
            case "resourceProduction": return "Добыча ресурсов";
            case "tradeBonus": return "Торговля";
            case "resourceEfficiency": return "Эффективность ресурсов";
            case "mobility": return "Мобильность";
            case "energyProduction": return "Производство энергии";
            case "buildSpeed": return "Скорость строительства";
            case "researchSpeed": return "Скорость исследований";
            case "prestige": return "Престиж";
            case "deterrence": return "Сдерживание";
            case "economicEfficiency": return "Экономическая эффективность";
            default: return bonusType;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        
        if (title == null || !title.contains("Технологии")) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String clickedName = clicked.getItemMeta().getDisplayName();
        
        // Close button
        if (slot == 53) {
            p.closeInventory();
            return;
        }
        
        // Branch selection (slots 1-5 in main menu)
        if (title.contains("Технологии:") && !title.contains("|")) {
            if (slot >= 1 && slot <= 5) {
                // Branch button clicked
                String branchId = getBranchIdFromName(clickedName);
                if (branchId != null) {
                    openBranchMenu(p, branchId);
                }
            } else if (slot >= 10 && slot <= 43) {
                // Technology clicked - DOUBLE-CLICK for research
                String techId = getTechIdFromItem(clicked);
                if (techId != null) {
                    String nationId = plugin.getPlayerDataManager().getNation(p.getUniqueId());
                    if (nationId != null) {
                        // Check if already unlocked
                        boolean unlocked = techService.isTechnologyUnlocked(nationId, techId);
                        if (unlocked) {
                            p.sendMessage("§7Технология уже изучена.");
                            return;
                        }
                        
                        // Double-click confirmation
                        if (!plugin.getDoubleClickService().shouldProceed(p.getUniqueId(), "research_" + techId)) {
                            // First click - make item glow
                            ItemMeta meta = clicked.getItemMeta();
                            if (meta != null) {
                                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                                clicked.setItemMeta(meta);
                                e.getInventory().setItem(slot, clicked);
                                p.sendMessage("§eНажмите ещё раз в течение 5 секунд для подтверждения исследования.");
                            }
                            return;
                        }
                        
                        // Second click - proceed with research
                        String result = techService.researchTechnology(nationId, techId);
                        p.sendMessage(result);
                        
                        // Visual feedback for successful research
                        if (result.contains("успешно") || result.contains("изучена")) {
                            plugin.getVisualEffectsService().playNationJoinEffect(p); // Celebration effect
                        }
                        
                        p.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> openMainMenu(p), 5);
                    }
                }
            }
        } else if (title.contains("| Технологии")) {
            // In branch menu
            if (clickedName != null && clickedName.contains("Назад")) {
                openMainMenu(p);
            } else if (slot == 49) {
                // Back button
                openMainMenu(p);
            } else if (slot >= 9 && slot <= 44) {
                // Technology clicked in branch - DOUBLE-CLICK
                String techId = getTechIdFromItem(clicked);
                if (techId != null) {
                    String nationId = plugin.getPlayerDataManager().getNation(p.getUniqueId());
                    if (nationId != null) {
                        boolean unlocked = techService.isTechnologyUnlocked(nationId, techId);
                        if (unlocked) {
                            p.sendMessage("§7Технология уже изучена.");
                            return;
                        }
                        
                        // Double-click confirmation
                        if (!plugin.getDoubleClickService().shouldProceed(p.getUniqueId(), "research_" + techId)) {
                            ItemMeta meta = clicked.getItemMeta();
                            if (meta != null) {
                                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                                clicked.setItemMeta(meta);
                                e.getInventory().setItem(slot, clicked);
                                p.sendMessage("§eНажмите ещё раз в течение 5 секунд для подтверждения исследования.");
                            }
                            return;
                        }
                        
                        String result = techService.researchTechnology(nationId, techId);
                        p.sendMessage(result);
                        
                        // Visual feedback
                        if (result.contains("успешно") || result.contains("изучена")) {
                            plugin.getVisualEffectsService().playNationJoinEffect(p);
                        }
                        
                        p.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            String branchId = getBranchIdFromName(title.replace(GuiUtils.formatHeader(""), "").replace(": Технологии", "").trim());
                            if (branchId != null) {
                                openBranchMenu(p, branchId);
                            } else {
                                openMainMenu(p);
                            }
                        }, 5);
                    }
                }
            }
        }
    }

    private String getBranchIdFromName(String displayName) {
        if (displayName == null) return null;
        if (displayName.contains("Военные")) return "military";
        if (displayName.contains("Промышленность")) return "industry";
        if (displayName.contains("Экономика")) return "economy";
        if (displayName.contains("Инфраструктура")) return "infrastructure";
        if (displayName.contains("Наука")) return "science";
        return null;
    }

    private String getBranchIdFromDisplayName(String name) {
        if (name.contains("Военные технологии")) return "military";
        if (name.contains("Промышленность")) return "industry";
        if (name.contains("Экономика")) return "economy";
        if (name.contains("Инфраструктура")) return "infrastructure";
        if (name.contains("Наука")) return "science";
        return null;
    }

    private String getTechIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        
        // Find tech by name in display name
        String displayName = item.getItemMeta().getDisplayName();
        if (displayName == null) return null;
        
        // Remove color codes and check marks
        String cleanName = displayName.replace("§a", "").replace("§c", "").replace("§b", "")
            .replace("✓ ", "").trim();
        
        // Find matching technology
        for (TechnologyTreeService.Technology tech : techService.getAllTechs()) {
            if (tech.name.equals(cleanName)) {
                return tech.id;
            }
        }
        
        return null;
    }
}

