package com.axiom.command;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.EconomyService;
import com.axiom.service.NationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Root /axiom dispatcher with subcommands. */
public class AxiomCommand implements CommandExecutor {
    private final AXIOM plugin;

    public AxiomCommand(AXIOM plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                sendHelpMessage(sender);
                return true;
            case "nation":
                if (sender instanceof Player) {
                    plugin.openNationMainMenu((Player) sender);
                } else {
                    sender.sendMessage("This command can only be run by a player.");
                }
                return true;
            case "reload":
                if (!sender.hasPermission("axiom.admin")) {
                    sender.sendMessage("§cНедостаточно прав.");
                    return true;
                }
                plugin.reloadAxiomConfig();
                sender.sendMessage("§aКонфигурация перезагружена.");
                return true;
            // ... keep other cases but remove the old 'nation' logic if it was complex
            default:
                sender.sendMessage("§cНеизвестная команда. Используйте §b/axiom help §7для списка команд.");
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6§l=== AXIOM - Помощь по командам ===");
        sender.sendMessage(formatHelpCommand("/axiom nation", "Открыть меню управления нацией."));
        sender.sendMessage(formatHelpCommand("/axiom help", "Показать это справочное сообщение."));
        sender.sendMessage(formatHelpCommand("/claim", "Захватить территорию для вашей нации."));
        sender.sendMessage(formatHelpCommand("/unclaim", "Отказаться от территории."));
        sender.sendMessage("§7Для получения дополнительной информации, пожалуйста, посетите наш веб-сайт.");
        sender.sendMessage("§6§l=====================================");
    }

    private String formatHelpCommand(String cmd, String description) {
        return "§b" + cmd + " §7- §f" + description;
    }

    private boolean handleNation(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        if (args.length >= 2 && args[1].equalsIgnoreCase("create")) {
            if (args.length < 3) { sender.sendMessage("§cИспользование: /axiom nation create <name>"); return true; }
            String name = args[2];
            Player p = (Player) sender;
            NationManager nm = plugin.getNationManager();
            String currency = plugin.getEconomyService().getDefaultCurrencyCode();
            double start = plugin.getConfig().getDouble("economy.startingTreasury", 1000.0);
            try {
                nm.createNation(p, name, currency, start);
                sender.sendMessage("§aНация создана: " + name + ". Первая территория — текущий чанк.");
            } catch (Exception e) {
                sender.sendMessage("§cОшибка при создании нации: " + e.getMessage());
            }
            return true;
        }
        sender.sendMessage("§cДоступно: /axiom nation create <name>");
        return true;
    }

    private boolean handleEconomy(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        if (args.length >= 2 && args[1].equalsIgnoreCase("print")) {
            if (args.length < 3) { sender.sendMessage("§cИспользование: /axiом economy print <amount>"); return true; }
            try {
                double amt = Double.parseDouble(args[2]);
                Player p = (Player) sender;
                var opt = plugin.getNationManager().getNationOfPlayer(p.getUniqueId());
                if (opt.isEmpty()) {
                    sender.sendMessage("§cВы не в нации.");
                    return true;
                }
                Nation n = opt.get();
                
                // Beautiful confirmation with description
                String description = String.format(
                    "§fВы уверены, что хотите напечатать §a%.2f %s§f?\n\n" +
                    "§7Текущая казна: §b%.2f %s\n" +
                    "§7После печати: §b%.2f %s\n" +
                    "§7Лимит в день (20%%): §b%.2f %s\n\n" +
                    "§c⚠ Это увеличит инфляцию!",
                    amt, n.getCurrencyCode(),
                    n.getTreasury(), n.getCurrencyCode(),
                    n.getTreasury() + amt, n.getCurrencyCode(),
                    n.getTreasury() * 0.2, n.getCurrencyCode()
                );
                
                plugin.getConfirmMenu().open(p, "Печать денег", description, () -> {
                    boolean ok = plugin.getEconomyService().printMoney(p.getUniqueId(), amt);
                    if (ok) {
                        p.sendMessage("§aНапечатано: §f" + String.format("%.2f", amt) + " " + n.getCurrencyCode());
                        // Visual feedback
                        plugin.getVisualEffectsService().sendActionBar(p, "§a💰 Напечатано " + String.format("%.2f", amt) + " " + n.getCurrencyCode());
                    } else {
                        p.sendMessage("§cОперация отклонена (права/лимит/нация).");
                    }
                }, () -> p.sendMessage("§eОтменено."));
            } catch (NumberFormatException e) {
                sender.sendMessage("§cСумма должна быть числом.");
            }
            return true;
        }
        sender.sendMessage("§cДоступно: /axiom economy print <amount>");
        return true;
    }

    private boolean handleClaim(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        try {
            String msg = plugin.getNationManager().claimChunk((Player) sender);
            sender.sendMessage("§e" + msg);
        } catch (Exception e) { sender.sendMessage("§cОшибка: " + e.getMessage()); }
        return true;
    }

    private boolean handleUnclaim(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        try {
            String msg = plugin.getNationManager().unclaimChunk((Player) sender);
            sender.sendMessage("§e" + msg);
        } catch (Exception e) { sender.sendMessage("§cОшибка: " + e.getMessage()); }
        return true;
    }

    private boolean handlePvp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        if (args.length < 2) { sender.sendMessage("§cИспользование: /axiom pvp <on|off>"); return true; }
        boolean on = args[1].equalsIgnoreCase("on");
        plugin.getPvpService().set(((Player) sender).getUniqueId(), on);
        sender.sendMessage(on ? "§aPvP включено." : "§ePvP выключено.");
        return true;
    }

    private boolean handleDiplomacy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        if (args.length >= 2 && args[1].equalsIgnoreCase("declare-war")) {
            if (args.length < 3) { sender.sendMessage("§cИспользование: /axiom diplomacy declare-war <nationId>"); return true; }
            String targetId = args[2];
            var nm = plugin.getNationManager();
            var attackerOpt = nm.getNationOfPlayer(((Player) sender).getUniqueId());
            var defender = nm.getNationById(targetId);
            if (attackerOpt.isEmpty() || defender == null) { sender.sendMessage("§cНация не найдена."); return true; }
            Player p = (Player) sender;
            Nation attacker = attackerOpt.get();
            
            // Beautiful confirmation with description
            String description = String.format(
                "§fВы уверены, что хотите объявить войну нации §c'%s'§f?\n\n" +
                "§7Стоимость: §c%.0f %s\n" +
                "§7Ваша казна: §b%.0f %s\n" +
                "§7После объявления: §b%.0f %s\n\n" +
                "§c⚠ Война продлится 24 часа!\n" +
                "§c⚠ Warzone будет активна на территориях обеих наций!\n" +
                "§c⚠ Кулдаун на следующую войну: 72 часа!",
                defender.getName(),
                5000.0, attacker.getCurrencyCode(),
                attacker.getTreasury(), attacker.getCurrencyCode(),
                attacker.getTreasury() - 5000.0, attacker.getCurrencyCode()
            );
            
            plugin.getConfirmMenu().open(p, "Объявление войны", description, () -> {
                try {
                    String res = plugin.getDiplomacySystem().declareWar(attacker, defender);
                    p.sendMessage("§e" + res);
                    // Visual effects are handled in DiplomacySystem.declareWar
                } catch (Exception ex) { 
                    p.sendMessage("§cОшибка: " + ex.getMessage()); 
                }
            }, () -> p.sendMessage("§eОтменено."));
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("ally")) {
            if (args.length < 3) { sender.sendMessage("§cИспользование: /axiom diplomacy ally <nationId>"); return true; }
            var nm = plugin.getNationManager();
            var me = nm.getNationOfPlayer(((Player) sender).getUniqueId());
            var other = nm.getNationById(args[2]);
            if (me.isEmpty() || other == null) { sender.sendMessage("§cНация не найдена."); return true; }
            Player p = (Player) sender;
            plugin.getConfirmMenu().open(p, "Альянс с " + args[2], () -> {
                try { p.sendMessage("§e" + plugin.getDiplomacySystem().requestAlliance(me.get(), other)); }
                catch (Exception ex) { p.sendMessage("§cОшибка: " + ex.getMessage()); }
            }, () -> p.sendMessage("§eОтменено."));
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("accept-ally")) {
            if (args.length < 3) { sender.sendMessage("§cИспользование: /axiom diplomacy accept-ally <nationId>"); return true; }
            var nm = plugin.getNationManager();
            var me = nm.getNationOfPlayer(((Player) sender).getUniqueId());
            var other = nm.getNationById(args[2]);
            if (me.isEmpty() || other == null) { sender.sendMessage("§cНация не найдена."); return true; }
            try { sender.sendMessage("§e" + plugin.getDiplomacySystem().acceptAlliance(me.get(), other)); }
            catch (Exception ex) { sender.sendMessage("§cОшибка: " + ex.getMessage()); }
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("rep")) {
            if (args.length < 4) { sender.sendMessage("§cИспользование: /axiom diplomacy rep <nationId> <value>"); return true; }
            var nm = plugin.getNationManager();
            var me = nm.getNationOfPlayer(((Player) sender).getUniqueId());
            var other = nm.getNationById(args[2]);
            if (me.isEmpty() || other == null) { sender.sendMessage("§cНация не найдена."); return true; }
            try {
                int v = Integer.parseInt(args[3]);
                plugin.getDiplomacySystem().setReputation(me.get(), other, v);
                sender.sendMessage("§aРепутация установлена: " + v);
            } catch (NumberFormatException e) { sender.sendMessage("§cЗначение должно быть числом."); }
            catch (Exception ex) { sender.sendMessage("§cОшибка: " + ex.getMessage()); }
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("treaty")) {
            if (args.length >= 3 && args[2].equalsIgnoreCase("create")) {
                if (args.length < 6) { sender.sendMessage("§cИспользование: /axiom diplomacy treaty create <nationId> <type> <days>"); return true; }
                var nm = plugin.getNationManager();
                var me = nm.getNationOfPlayer(((Player) sender).getUniqueId());
                var other = nm.getNationById(args[3]);
                if (me.isEmpty() || other == null) { sender.sendMessage("§cНация не найдена."); return true; }
                String type = args[4].toLowerCase();
                if (!type.equals("nap") && !type.equals("trade") && !type.equals("military")) {
                    sender.sendMessage("§cТип должен быть: nap, trade или military"); return true;
                }
                try {
                    long days = Long.parseLong(args[5]);
                    if (days < 1 || days > 365) {
                        sender.sendMessage("§cДлительность должна быть от 1 до 365 дней"); return true;
                    }
                    String res = plugin.getTreatyService().createTreaty(me.get().getId(), other.getId(), type, days);
                    sender.sendMessage("§a" + res);
                    // Notify other nation
                    for (UUID citizenId : other.getCitizens()) {
                        org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                        if (citizen != null && citizen.isOnline()) {
                            citizen.sendMessage("§bДоговор предложен от '" + me.get().getName() + "'");
                            plugin.getVisualEffectsService().sendActionBar(citizen, "§b📜 Новый договор от '" + me.get().getName() + "'");
                        }
                    }
                } catch (NumberFormatException e) { sender.sendMessage("§cДни должны быть числом."); }
                catch (Exception ex) { sender.sendMessage("§cОшибка: " + ex.getMessage()); }
                return true;
            }
            sender.sendMessage("§cДоступно: /axiom diplomacy treaty create <nationId> <type> <days>");
            return true;
        }
        sender.sendMessage("§cДоступно: /axiom diplomacy declare-war <nationId> | ally | accept-ally | treaty create");
        return true;
    }

    private boolean handleSave(CommandSender sender) {
        try { plugin.getNationManager().flush(); sender.sendMessage("§aДанные сохранены."); }
        catch (Exception e) { sender.sendMessage("§cОшибка сохранения: " + e.getMessage()); }
        return true;
    }

    private boolean handleReligion(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        if (args.length >= 2 && args[1].equalsIgnoreCase("found")) {
            if (args.length < 4) { sender.sendMessage("§cИспользование: /axiom religion found <id> <name>"); return true; }
            try {
                String id = args[2];
                String name = args[3];
                String res = plugin.getReligionManager().foundReligion(((Player) sender).getUniqueId(), id, name);
                sender.sendMessage("§e" + res);
            } catch (Exception e) { sender.sendMessage("§cОшибка: " + e.getMessage()); }
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("add-holy")) {
            if (args.length < 4) { sender.sendMessage("§cИспользование: /axiom religion add-holy <id> <world:x:z>"); return true; }
            try { plugin.getReligionManager().addHolySite(args[2], args[3]); sender.sendMessage("§aСвятое место добавлено."); }
            catch (Exception e) { sender.sendMessage("§cОшибка: " + e.getMessage()); }
            return true;
        }
        // open menu fallback
        plugin.openReligionMenu((Player) sender);
        return true;
    }

    private boolean handleCitizenship(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        var pdm = plugin.getPlayerDataManager();
        var nm = plugin.getNationManager();
        Player p = (Player) sender;
        if (args.length >= 2 && args[1].equalsIgnoreCase("leave")) {
            pdm.clearNation(p.getUniqueId());
            sender.sendMessage("§eВы покинули нацию.");
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("invite")) {
            if (args.length < 3) { sender.sendMessage("§cИспользование: /axiom citizenship invite <player>"); return true; }
            var meNation = nm.getNationOfPlayer(p.getUniqueId());
            if (meNation.isEmpty()) { sender.sendMessage("§cВы не в нации."); return true; }
            NationManager tmp = nm; // keep import
            var target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage("§cИгрок не в сети."); return true; }
            pdm.setField(target.getUniqueId(), "inviteNation", meNation.get().getId());
            target.sendMessage("§bВас пригласили в нацию: §f" + meNation.get().getName() + " §7(/axiom citizenship accept)");
            
            // VISUAL EFFECTS: Invitation notification
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                target.sendTitle("§b§l[ПРИГЛАШЕНИЕ]", "§fНация '" + meNation.get().getName() + "' приглашает вас!", 10, 80, 20);
                plugin.getVisualEffectsService().sendActionBar(target, "§b📨 Приглашение от '" + meNation.get().getName() + "'. Используйте §e/axiom citizenship accept");
                // Blue particles
                org.bukkit.Location loc = target.getLocation();
                loc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
                target.playSound(loc, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
            });
            
            sender.sendMessage("§aПриглашение отправлено.");
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("accept")) {
            String nid = pdm.getField(p.getUniqueId(), "inviteNation");
            if (nid == null) { sender.sendMessage("§cНет приглашений."); return true; }
            var nation = nm.getNationById(nid);
            pdm.setNation(p.getUniqueId(), nid, "CITIZEN");
            pdm.setField(p.getUniqueId(), "inviteNation", null);
            sender.sendMessage("§aВы вступили в нацию" + (nation != null ? ": " + nation.getName() : "") + ".");
            
            // VISUAL EFFECTS: Play join effect
            plugin.getVisualEffectsService().playNationJoinEffect(p);
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("set-role")) {
            if (args.length < 4) { sender.sendMessage("§cИспользование: /axiom citizenship set-role <player> <role>"); return true; }
            var meNation = nm.getNationOfPlayer(p.getUniqueId());
            if (meNation.isEmpty()) { sender.sendMessage("§cВы не в нации."); return true; }
            Nation.Role actorRole = meNation.get().getRole(p.getUniqueId());
            if (actorRole != Nation.Role.LEADER && actorRole != Nation.Role.MINISTER) {
                sender.sendMessage("§cНедостаточно прав. Требуется: LEADER или MINISTER");
                return true;
            }
            var target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage("§cИгрок не в сети."); return true; }
            if (!meNation.get().isMember(target.getUniqueId())) {
                sender.sendMessage("§cИгрок не является членом вашей нации.");
                return true;
            }
            String roleStr = args[3].toUpperCase();
            Nation.Role newRole;
            try {
                newRole = Nation.Role.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cНеверная роль. Доступно: LEADER, MINISTER, GENERAL, GOVERNOR, CITIZEN");
                return true;
            }
            if (newRole == Nation.Role.LEADER && actorRole != Nation.Role.LEADER) {
                sender.sendMessage("§cТолько лидер может назначать другого лидера.");
                return true;
            }
            
            Nation.Role oldRole = meNation.get().getRole(target.getUniqueId());
            meNation.get().getRoles().put(target.getUniqueId(), newRole);
            try {
                nm.save(meNation.get());
                sender.sendMessage("§aРоль игрока " + target.getName() + " изменена: " + oldRole + " → " + newRole);
                
                // VISUAL EFFECTS: Notify player of role change
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    String roleDisplay = getRoleDisplayName(newRole);
                    target.sendTitle("§e§l[ИЗМЕНЕНИЕ РОЛИ]", "§fВаша роль: " + roleDisplay, 10, 80, 20);
                    plugin.getVisualEffectsService().sendActionBar(target, "§e⭐ Новая роль в '" + meNation.get().getName() + "': " + roleDisplay);
                    // Gold particles
                    org.bukkit.Location loc = target.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
                    target.playSound(loc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                });
            } catch (Exception ex) {
                sender.sendMessage("§cОшибка сохранения: " + ex.getMessage());
            }
            return true;
        }
        
        sender.sendMessage("§cДоступно: /axiom citizenship invite <player> | accept | leave | set-role <player> <role>");
        return true;
    }
    
    private String getRoleDisplayName(Nation.Role role) {
        if (role == null) return "§7Нет";
        switch (role) {
            case LEADER: return "§6§lКОРОЛЬ";
            case MINISTER: return "§dМИНИСТР";
            case GENERAL: return "§cГЕНЕРАЛ";
            case GOVERNOR: return "§bГУБЕРНАТОР";
            case CITIZEN: return "§aГРАЖДАНИН";
            default: return "§7Нет";
        }
    }

    private boolean handleElection(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        Player p = (Player) sender;
        var nm = plugin.getNationManager();
        var meOpt = nm.getNationOfPlayer(p.getUniqueId());
        if (meOpt.isEmpty()) { sender.sendMessage("§cВы не в нации."); return true; }
        Nation me = meOpt.get();
        if (args.length >= 2 && args[1].equalsIgnoreCase("start")) {
            if (args.length < 5) { sender.sendMessage("§cИспользование: /axiom election start <president|parliament|law|minister> <durationMinutes> <candidate1> [candidate2] ..."); return true; }
            String type = args[2];
            try {
                long duration = Long.parseLong(args[3]);
                List<String> candidates = new ArrayList<>();
                for (int i = 4; i < args.length; i++) candidates.add(args[i]);
                String res = plugin.getElectionService().startElection(me.getId(), type, duration, candidates);
                sender.sendMessage("§e" + res);
            } catch (NumberFormatException e) { sender.sendMessage("§cДлительность должна быть числом (в минутах)."); }
            catch (Exception e) { sender.sendMessage("§cОшибка: " + e.getMessage()); }
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("vote")) {
            if (args.length < 4) { sender.sendMessage("§cИспользование: /axiom election vote <type> <candidate>"); return true; }
            try {
                String res = plugin.getElectionService().vote(p.getUniqueId(), me.getId(), args[2], args[3]);
                sender.sendMessage("§e" + res);
            } catch (Exception e) { sender.sendMessage("§cОшибка: " + e.getMessage()); }
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("results")) {
            if (args.length < 3) { sender.sendMessage("§cИспользование: /axiom election results <type>"); return true; }
            Map<String, Integer> results = plugin.getElectionService().getResults(me.getId(), args[2]);
            if (results == null) { sender.sendMessage("§cВыборы не найдены."); return true; }
            sender.sendMessage("§bРезультаты выборов (" + args[2] + "):");
            results.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> sender.sendMessage("§f" + entry.getKey() + ": §a" + entry.getValue()));
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("finish")) {
            if (args.length < 3) { sender.sendMessage("§cИспользование: /axiom election finish <type>"); return true; }
            try {
                String res = plugin.getElectionService().finishElection(me.getId(), args[2]);
                sender.sendMessage("§e" + res);
            } catch (Exception e) { sender.sendMessage("§cОшибка: " + e.getMessage()); }
            return true;
        }
        // Open GUI
        new com.axiom.gui.ElectionMenu(plugin, p).open();
        return true;
    }
    
    private boolean handleBackup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.admin")) {
            sender.sendMessage("§cТребуются права администратора.");
            return true;
        }
        if (sender instanceof Player) {
            Player p = (Player) sender;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String result = plugin.getBackupService().createBackup();
                p.sendMessage("§e" + result);
            });
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String result = plugin.getBackupService().createBackup();
                sender.sendMessage("§e" + result);
            });
        }
        return true;
    }
    
    private boolean handleRestore(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.admin")) {
            sender.sendMessage("§cТребуются права администратора.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /axiom restore <backupFileName>");
            sender.sendMessage("§7Доступные backup:");
            for (String backup : plugin.getBackupService().listBackups()) {
                sender.sendMessage("§7  - " + backup);
            }
            return true;
        }
        String backupName = args[1];
        if (sender instanceof Player) {
            Player p = (Player) sender;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String result = plugin.getBackupService().restoreBackup(backupName);
                p.sendMessage("§e" + result);
            });
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String result = plugin.getBackupService().restoreBackup(backupName);
                sender.sendMessage("§e" + result);
            });
        }
        return true;
    }
    
    private boolean handleTutorial(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            plugin.getTutorialService().resetTutorial(p);
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("skip")) {
            plugin.getTutorialService().skipStep(p);
            return true;
        }
        plugin.getTutorialService().startTutorial(p);
        return true;
    }
    
    private boolean handleExport(CommandSender sender) {
        if (!sender.hasPermission("axiom.admin")) {
            sender.sendMessage("§cТребуются права администратора.");
            return true;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String result = plugin.getWebExportService().exportNow();
            sender.sendMessage("§a" + result);
        });
        return true;
    }
    
    private boolean handlePerformance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.admin")) {
            sender.sendMessage("§cТребуются права администратора.");
            return true;
        }
        sender.sendMessage("§b=== Метрики производительности ===");
        sender.sendMessage("§7Для полного отчёта проверьте консоль.");
        sender.sendMessage("§7Медленных операций (>100ms): §b" + plugin.getPerformanceMetricsService().getSlowOperations().size());
        sender.sendMessage("§7Используйте §b/spark profiler §7если установлен Spark.");
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            plugin.getPerformanceMetricsService().resetMetrics();
            sender.sendMessage("§aМетрики сброшены.");
        }
        return true;
    }
    
    private boolean handleDashboard(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("toggle")) {
            // Toggle dashboard on/off
            plugin.getPlayerDashboardService().toggleDashboard(p);
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("refresh")) {
            // Force refresh
            plugin.getPlayerDashboardService().forceUpdate(p);
            p.sendMessage("§aDashboard обновлён!");
            return true;
        }
        
        // Show dashboard info
        plugin.getPlayerDashboardService().forceUpdate(p);
        p.sendMessage("§bDashboard обновлён!");
        p.sendMessage("§7Используйте §b/axiom dashboard toggle §7для скрытия/показа");
        return true;
    }
    
    private boolean handleWar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        var opt = plugin.getNationManager().getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) {
            sender.sendMessage("§cВы не в нации.");
            return true;
        }
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("declare")) {
            if (args.length < 3) {
                sender.sendMessage("§cИспользование: /axiom war declare <nationId>");
                return true;
            }
            var defender = plugin.getNationManager().getNationById(args[2]);
            if (defender == null) {
                sender.sendMessage("§cНация не найдена.");
                return true;
            }
            try {
                // Use DiplomacySystem for war declaration (AdvancedWarSystem uses it internally)
                String res = plugin.getDiplomacySystem().declareWar(opt.get(), defender);
                sender.sendMessage("§e" + res);
            } catch (Exception e) {
                sender.sendMessage("§cОшибка: " + e.getMessage());
            }
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("status")) {
            if (plugin.getAdvancedWarSystem() != null) {
                List<com.axiom.service.AdvancedWarSystem.War> wars = plugin.getAdvancedWarSystem().getNationWars(opt.get().getId());
                if (wars.isEmpty()) {
                    sender.sendMessage("§aНет активных войн.");
                } else {
                    sender.sendMessage("§c§lАКТИВНЫЕ ВОЙНЫ: " + wars.size());
                    for (com.axiom.service.AdvancedWarSystem.War war : wars) {
                        boolean isAttacker = war.attackerId.equals(opt.get().getId());
                        String enemyId = isAttacker ? war.defenderId : war.attackerId;
                        Nation enemy = plugin.getNationManager().getNationById(enemyId);
                        String enemyName = enemy != null ? enemy.getName() : enemyId;
                        sender.sendMessage("§c⚔ Война с: §f" + enemyName);
                        sender.sendMessage("§7  Тип: §f" + war.type.name() + " §7| Статус: §f" + war.status.name());
                        sender.sendMessage("§7  Побед: §a" + (isAttacker ? war.attackerWins : war.defenderWins) + " §7| Битв: §f" + war.battlesFought);
                    }
                }
            }
            return true;
        }
        sender.sendMessage("§cИспользование: /axiom war declare <nationId> | status");
        return true;
    }
    
    private boolean handleBanking(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        var opt = plugin.getNationManager().getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) {
            sender.sendMessage("§cВы не в нации.");
            return true;
        }
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            com.axiom.service.BankingService banking = plugin.getBankingService();
            if (banking != null) {
                List<com.axiom.service.BankingService.Loan> loans = banking.getActiveLoans(opt.get().getId());
                if (loans.isEmpty()) {
                    sender.sendMessage("§aНет активных кредитов.");
                } else {
                    sender.sendMessage("§bАктивные кредиты: §f" + loans.size());
                    for (com.axiom.service.BankingService.Loan loan : loans) {
                        Nation lender = plugin.getNationManager().getNationById(loan.lenderNationId);
                        String lenderName = lender != null ? lender.getName() : loan.lenderNationId;
                        sender.sendMessage("§7  Кредит от §f" + lenderName + "§7: §b" + String.format("%.2f", loan.remaining) + " §7(ставка: §e" + loan.interestRate + "%§7)");
                    }
                }
            } else {
                sender.sendMessage("§cБанковский сервис недоступен.");
            }
            return true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("repay")) {
            if (args.length < 4) {
                sender.sendMessage("§cИспользование: /axiom banking repay <lenderNationId> <amount>");
                return true;
            }
            try {
                double amount = Double.parseDouble(args[3]);
                com.axiom.service.BankingService banking = plugin.getBankingService();
                if (banking != null) {
                    String res = banking.repayLoan(opt.get().getId(), args[2], amount);
                    sender.sendMessage("§e" + res);
                } else {
                    sender.sendMessage("§cБанковский сервис недоступен.");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cСумма должна быть числом.");
            } catch (Exception e) {
                sender.sendMessage("§cОшибка: " + e.getMessage());
            }
            return true;
        }
        sender.sendMessage("§cИспользование: /axiom banking list | repay <lenderNationId> <amount>");
        sender.sendMessage("§7Для выдачи кредитов используйте дипломатическое меню");
        return true;
    }
    
    private boolean handleStock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        var opt = plugin.getNationManager().getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) {
            sender.sendMessage("§cВы не в нации.");
            return true;
        }
        
        com.axiom.service.StockMarketService stock = plugin.getStockMarketService();
        if (stock != null) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                List<com.axiom.service.StockMarketService.Corporation> corps = stock.getCorporationsOf(opt.get().getId());
                sender.sendMessage("§bКорпорации вашей нации: §f" + corps.size());
                for (com.axiom.service.StockMarketService.Corporation corp : corps) {
                    String pubStatus = corp.isPublic ? "§aПубличная" : "§7Приватная";
                    sender.sendMessage("§7  - §f" + corp.name + " §7(§e" + corp.type + "§7): §b" + String.format("%.2f", corp.value) + " §7| " + pubStatus);
                }
                return true;
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("create")) {
                if (args.length < 4) {
                    sender.sendMessage("§cИспользование: /axiom stock create <name> <type>");
                    sender.sendMessage("§7Типы: mine, farm, factory, tech, bank, trading");
                    return true;
                }
                try {
                    String res = stock.createCorporation(opt.get().getId(), args[2], args[3]);
                    sender.sendMessage("§e" + res);
                } catch (Exception e) {
                    sender.sendMessage("§cОшибка: " + e.getMessage());
                }
                return true;
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("ipo")) {
                if (args.length < 5) {
                    sender.sendMessage("§cИспользование: /axiom stock ipo <corporationId> <shares> <pricePerShare>");
                    return true;
                }
                try {
                    int shares = Integer.parseInt(args[3]);
                    double price = Double.parseDouble(args[4]);
                    String res = stock.conductIPO(opt.get().getId(), args[2], shares, price);
                    sender.sendMessage("§6" + res);
                } catch (Exception e) {
                    sender.sendMessage("§cОшибка: " + e.getMessage());
                }
                return true;
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("buy")) {
                if (args.length < 5) {
                    sender.sendMessage("§cИспользование: /axiom stock buy <corporationId> <shares> <pricePerShare>");
                    return true;
                }
                try {
                    int shares = Integer.parseInt(args[3]);
                    double price = Double.parseDouble(args[4]);
                    String res = stock.buyShares(opt.get().getId(), args[2], shares, price);
                    sender.sendMessage("§a" + res);
                } catch (Exception e) {
                    sender.sendMessage("§cОшибка: " + e.getMessage());
                }
                return true;
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("global")) {
                List<com.axiom.service.StockMarketService.Corporation> allCorps = stock.getAllCorporations();
                sender.sendMessage("§bГлобальный фондовый рынок");
                sender.sendMessage("§7Всего корпораций: §f" + allCorps.size());
                sender.sendMessage("§7Индекс рынка: §f" + String.format("%.2f", stock.calculateMarketIndex()));
                List<com.axiom.service.StockMarketService.Corporation> top = stock.getTopCorporationsByValue(10);
                sender.sendMessage("§bТоп-10 по стоимости:");
                for (int i = 0; i < Math.min(10, top.size()); i++) {
                    com.axiom.service.StockMarketService.Corporation corp = top.get(i);
                    sender.sendMessage("§7  " + (i+1) + ". §f" + corp.name + " §7(§e" + corp.type + "§7): §b" + String.format("%.2f", corp.value));
                }
                return true;
            }
            sender.sendMessage("§6§l=== ФОНОВЫЙ РЫНОК ===");
            sender.sendMessage("§b/axiom stock list §7— ваши корпорации");
            sender.sendMessage("§b/axiom stock create <name> <type> §7— создать корпорацию (§650,000)");
            sender.sendMessage("§b/axiom stock ipo <corpId> <shares> <price> §7— провести IPO");
            sender.sendMessage("§b/axiom stock buy <corpId> <shares> <price> §7— купить акции");
            sender.sendMessage("§b/axiom stock global §7— глобальный рынок");
        } else {
            sender.sendMessage("§cФондовый рынок недоступен.");
        }
        return true;
    }
    
    private boolean handleWallet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("balance")) {
            double balance = plugin.getWalletService().getBalance(p.getUniqueId());
            sender.sendMessage("§bВаш баланс: §f" + String.format("%.2f", balance));
            return true;
        } else if (args.length >= 3 && args[1].equalsIgnoreCase("pay")) {
            if (args.length < 4) {
                sender.sendMessage("§cИспользование: /axiom wallet pay <player> <amount>");
                return true;
            }
            org.bukkit.entity.Player target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден.");
                return true;
            }
            try {
                double amount = Double.parseDouble(args[3]);
                if (plugin.getWalletService().transfer(p.getUniqueId(), target.getUniqueId(), amount)) {
                    sender.sendMessage("§aПереведено §f" + String.format("%.2f", amount) + " §7игроку §f" + target.getName());
                    target.sendMessage("§aПолучено §f" + String.format("%.2f", amount) + " §7от §f" + p.getName());
                } else {
                    sender.sendMessage("§cНедостаточно средств.");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cСумма должна быть числом.");
            }
            return true;
        }
        sender.sendMessage("§cИспользование: /axiom wallet balance | pay <player> <amount>");
        return true;
    }
    
    private boolean handleResources(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        var opt = plugin.getNationManager().getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) {
            sender.sendMessage("§cВы не в нации.");
            return true;
        }
        
        if (plugin.getResourceService() != null) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                Map<String, Double> resources = plugin.getResourceService().getNationResources(opt.get().getId());
                sender.sendMessage("§bРесурсы нации (§f" + resources.size() + " типов§b):");
                resources.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> sender.sendMessage("§7  §f" + entry.getKey() + ": §b" + String.format("%.2f", entry.getValue())));
                return true;
            }
            sender.sendMessage("§cИспользование: /axiom resources list");
        }
        return true;
    }
    
    private boolean handleRaid(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        var opt = plugin.getNationManager().getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) {
            sender.sendMessage("§cВы не в нации.");
            return true;
        }
        
        sender.sendMessage("§eРейды доступны во время войны.");
        sender.sendMessage("§7Используйте §b/axiom war declare §7для объявления войны");
        return true;
    }
    
    private boolean handleSiege(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        var opt = plugin.getNationManager().getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) {
            sender.sendMessage("§cВы не в нации.");
            return true;
        }
        
        sender.sendMessage("§eОсады городов доступны во время войны.");
        sender.sendMessage("§7Используйте §b/axiom war declare §7для объявления войны");
        return true;
    }
    
    private boolean handleCulture(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        var opt = plugin.getNationManager().getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) {
            sender.sendMessage("§cВы не в нации.");
            return true;
        }
        
        com.axiom.service.CultureService culture = plugin.getCultureService();
        if (culture != null) {
            // Get culture stats
            Map<String, Object> stats = culture.getCultureStatistics(opt.get().getId());
            if (stats != null && stats.containsKey("culturalInfluence")) {
                double influence = ((Number) stats.get("culturalInfluence")).doubleValue();
                sender.sendMessage("§bКультурное влияние вашей нации: §f" + String.format("%.1f", influence));
            } else {
                sender.sendMessage("§bКультура вашей нации развивается");
            }
            sender.sendMessage("§7Культура развивается автоматически");
        } else {
            sender.sendMessage("§7Культурный сервис недоступен");
        }
        return true;
    }
    
    private boolean handleEspionage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        var opt = plugin.getNationManager().getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) {
            sender.sendMessage("§cВы не в нации.");
            return true;
        }
        
        sender.sendMessage("§eШпионаж требует специальных прав.");
        sender.sendMessage("§7Используйте главное меню нации для доступа к функциям шпионажа");
        return true;
    }
    
    private boolean handleAdvancedFeatures(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        new com.axiom.gui.AdvancedFeaturesMenu(plugin, p).open();
        return true;
    }
    
    private boolean handleModPack(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        plugin.openModPackBuilderMenu(p);
        return true;
    }
    
    private boolean handleMapVisualization(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        new com.axiom.gui.MapBoundaryVisualizationMenu(plugin, p).open();
        return true;
    }
    
    private boolean handleEconomicIndicators(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;
        new com.axiom.gui.EconomicIndicatorsMenu(plugin, p).open();
        return true;
    }
}


