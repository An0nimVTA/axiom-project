package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.gui.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Tutorial and new player guidance system.
 * Provides soft start for newcomers with interactive tutorials.
 */
public class TutorialService implements Listener {
    private final AXIOM plugin;
    private final Set<UUID> completedTutorial = new HashSet<>();
    private final Map<UUID, Integer> tutorialStep = new HashMap<>(); // playerId -> current step
    
    public TutorialService(AXIOM plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadTutorialProgress();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // First time player
        if (!completedTutorial.contains(player.getUniqueId()) && 
            plugin.getPlayerDataManager().getNation(player.getUniqueId()) == null) {
            
            // Wait 5 seconds, then show welcome
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    showWelcomeMessage(player);
                }
            }, 20 * 5);
        }
        
        // Show hints if player hasn't joined a nation
        if (plugin.getPlayerDataManager().getNation(player.getUniqueId()) == null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    showHint(player);
                }
            }, 20 * 30); // After 30 seconds
        }
    }
    
    private void showWelcomeMessage(Player player) {
        player.sendMessage(" ");
        player.sendMessage("§b╔════════════════════════════════════════╗");
        player.sendMessage("§b║   §fДобро пожаловать в AXIOM! §b║");
        player.sendMessage("§b╚════════════════════════════════════════╝");
        player.sendMessage(" ");
        player.sendMessage("§7AXIOM — это геополитический движок для создания");
        player.sendMessage("§7наций, управления экономикой, ведения войн и многого другого.");
        player.sendMessage(" ");
        player.sendMessage("§bПолезные команды:");
        player.sendMessage("§7/axiom tutorial §f- Интерактивный туториал");
        player.sendMessage("§7/nation §f- Создать или открыть меню нации");
        player.sendMessage("§7/axiom help §f- Список всех команд");
        player.sendMessage(" ");
        player.sendMessage("§aНажмите §e/axiom tutorial §aдля начала обучения!");
        player.sendMessage(" ");
    }
    
    private void showHint(Player player) {
        String nationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (nationId == null) {
            player.sendMessage("§7[AXIOM Подсказка] Вы не в нации. Используйте §b/nation §7для создания!");
        }
    }
    
    /**
     * Start interactive tutorial with beautiful GUI book.
     */
    public void startTutorial(Player player) {
        if (completedTutorial.contains(player.getUniqueId())) {
            player.sendMessage("§7Туториал уже пройден. Используйте §b/axiom tutorial reset §7для сброса.");
            return;
        }
        
        // Open tutorial GUI book
        openTutorialBook(player);
        
        UUID playerId = player.getUniqueId();
        if (!tutorialStep.containsKey(playerId)) {
            tutorialStep.put(playerId, 0);
            showTutorialStep(player, 0);
        }
    }
    
    /**
     * Open interactive tutorial book GUI.
     */
    private void openTutorialBook(Player player) {
        // Create CHEST inventory with custom holder to force CHEST type (not crafting table)
        org.bukkit.inventory.InventoryHolder holder = new org.bukkit.inventory.InventoryHolder() {
            @Override
            public org.bukkit.inventory.Inventory getInventory() {
                return null; // Not used
            }
        };
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(holder, 54, 
            GuiUtils.colorize(GuiUtils.formatHeader("Руководство AXIOM")));
        
        // Fill background
        ItemStack bg = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, bg);
        }
        
        // Chapters
        inv.setItem(10, createChapterItem(Material.BOOK, "§b§lГЛАВА 1: Первые шаги", 
            Arrays.asList("§7• Создание нации", "§7• Основные команды", "§7• Первая территория")));
        
        inv.setItem(11, createChapterItem(Material.EMERALD, "§a§lГЛАВА 2: Экономика", 
            Arrays.asList("§7• Казна и валюта", "§7• Печать денег", "§7• Налоги")));
        
        inv.setItem(12, createChapterItem(Material.MAP, "§e§lГЛАВА 3: Территория", 
            Arrays.asList("§7• Клейм чанков", "§7• Защита", "§7• Лимиты")));
        
        inv.setItem(13, createChapterItem(Material.IRON_SWORD, "§c§lГЛАВА 4: Война", 
            Arrays.asList("§7• Объявление войны", "§7• Warzone", "§7• Мобилизация")));
        
        inv.setItem(14, createChapterItem(Material.ENCHANTED_BOOK, "§d§lГЛАВА 5: Технологии", 
            Arrays.asList("§7• Дерево исследований", "§7• Бонусы", "§7• Предварительные условия")));
        
        inv.setItem(15, createChapterItem(Material.WRITTEN_BOOK, "§6§lГЛАВА 6: Дипломатия", 
            Arrays.asList("§7• Альянсы", "§7• Репутация", "§7• Договоры")));
        
        inv.setItem(16, createChapterItem(Material.ENCHANTING_TABLE, "§5§lГЛАВА 7: Религия", 
            Arrays.asList("§7• Основание религии", "§7• Десятина", "§7• Святые места")));
        
        inv.setItem(19, createChapterItem(Material.BRICKS, "§b§lГЛАВА 8: Города", 
            Arrays.asList("§7• Создание городов", "§7• Развитие", "§7• Население")));
        
        inv.setItem(20, createChapterItem(Material.GOLDEN_APPLE, "§c§lГЛАВА 9: Выживание", 
            Arrays.asList("§7• Защита территории", "§7• Экономическая безопасность", "§7• Дипломатические советы")));
        
        inv.setItem(21, createChapterItem(Material.COMPASS, "§e§lГЛАВА 10: Продвинутые функции", 
            Arrays.asList("§7• Интеграция с модами", "§7• Команды админа", "§7• Web-дашборд")));
        
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§cЗакрыть");
            closeMeta.setLore(Arrays.asList("§7Нажмите для закрытия"));
        }
        close.setItemMeta(closeMeta);
        inv.setItem(49, close);
        
        player.openInventory(inv);
        
        // Show welcome message
        player.sendMessage(" ");
        player.sendMessage("§b╔════════════════════════════════════════╗");
        player.sendMessage("§b║   §fДобро пожаловать в AXIOM! §b║");
        player.sendMessage("§b╚════════════════════════════════════════╝");
        player.sendMessage(" ");
        player.sendMessage("§7Открыто интерактивное руководство.");
        player.sendMessage("§7Выберите главу для изучения!");
        player.sendMessage(" ");
    }
    
    private ItemStack createChapterItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private void showTutorialStep(Player player, int step) {
        switch (step) {
            case 0:
                player.sendMessage(" ");
                player.sendMessage("§b══════ §fШАГ 1: Создание нации §b══════");
                player.sendMessage("§7Нация — это основа AXIOM. Она позволяет:");
                player.sendMessage("§7• Заклеймить территорию");
                player.sendMessage("§7• Управлять экономикой");
                player.sendMessage("§7• Вести дипломатию");
                player.sendMessage(" ");
                player.sendMessage("§aДля создания нации используйте:");
                player.sendMessage("§b/axiom create <название>");
                player.sendMessage(" ");
                player.sendMessage("§7Или откройте §b/nation §7и следуйте инструкциям.");
                player.sendMessage(" ");
                scheduleNextStep(player, step, 30); // 30 seconds
                break;
                
            case 1:
                player.sendMessage(" ");
                player.sendMessage("§b══════ §fШАГ 2: Территория §b══════");
                player.sendMessage("§7Территория — это заклейменные чанки (16×16 блоков).");
                player.sendMessage("§7Первые 5 чанков — бесплатные!");
                player.sendMessage(" ");
                player.sendMessage("§aДля клейма чанка:");
                player.sendMessage("§b/axiom claim §7или §b/nation §7→ Территория");
                player.sendMessage(" ");
                player.sendMessage("§7Лимит чанков: §b100 + (население × 2)");
                player.sendMessage(" ");
                scheduleNextStep(player, step, 30);
                break;
                
            case 2:
                player.sendMessage(" ");
                player.sendMessage("§b══════ §fШАГ 3: Экономика §b══════");
                player.sendMessage("§7Каждая нация имеет свою казну и валюту.");
                player.sendMessage("§7Начальная казна: §b10,000 валюты");
                player.sendMessage(" ");
                player.sendMessage("§7Управление экономикой:");
                player.sendMessage("§b/nation §7→ Экономика");
                player.sendMessage("§7• Печать денег (лимит: 20% казны в день)");
                player.sendMessage("§7• Установка налогов");
                player.sendMessage("§7• Просмотр инфляции и ВВП");
                player.sendMessage(" ");
                scheduleNextStep(player, step, 30);
                break;
                
            case 3:
                player.sendMessage(" ");
                player.sendMessage("§b══════ §fШАГ 4: Технологии §b══════");
                player.sendMessage("§7Изучайте технологии для развития нации!");
                player.sendMessage("§75 веток: Военные, Промышленность, Экономика,");
                player.sendMessage("§7          Инфраструктура, Наука");
                player.sendMessage(" ");
                player.sendMessage("§aОткройте дерево технологий:");
                player.sendMessage("§b/nation §7→ Технологии");
                player.sendMessage(" ");
                player.sendMessage("§7Технологии требуют:");
                player.sendMessage("§7• Образование (тир × 10)");
                player.sendMessage("§7• Предварительные технологии");
                player.sendMessage("§7• Наличие модов (для некоторых)");
                player.sendMessage(" ");
                scheduleNextStep(player, step, 30);
                break;
                
            case 4:
                player.sendMessage(" ");
                player.sendMessage("§b══════ §fШАГ 5: Дипломатия §b══════");
                player.sendMessage("§7Дипломатия — ключ к выживанию:");
                player.sendMessage("§7• Альянсы — защита и сотрудничество");
                player.sendMessage("§7• Войны — стоимость 5,000, длительность 72ч");
                player.sendMessage("§7• Репутация — влияет на отношения");
                player.sendMessage(" ");
                player.sendMessage("§aУправление дипломатией:");
                player.sendMessage("§b/nation §7→ Дипломатия");
                player.sendMessage(" ");
                scheduleNextStep(player, step, 30);
                break;
                
            case 5:
                player.sendMessage(" ");
                player.sendMessage("§b══════ §fШАГ 6: Интеграция с модами §b══════");
                player.sendMessage("§7AXIOM поддерживает 30+ модов!");
                player.sendMessage("§7• Tacz, PointBlank — оружие");
                player.sendMessage("§7• Immersive Engineering — заводы");
                player.sendMessage("§7• Applied Energistics 2 — автоматизация");
                player.sendMessage("§7• И многие другие...");
                player.sendMessage(" ");
                player.sendMessage("§7Модовые блоки автоматически защищаются!");
                player.sendMessage("§7Технологии требуют установленные моды.");
                player.sendMessage(" ");
                scheduleNextStep(player, step, 30);
                break;
                
            case 6:
                player.sendMessage(" ");
                player.sendMessage("§b══════ §fТУТОРИАЛ ЗАВЕРШЁН! §b══════");
                player.sendMessage("§7Теперь вы знаете основы AXIOM!");
                player.sendMessage(" ");
                player.sendMessage("§aДополнительная помощь:");
                player.sendMessage("§7/axiom help §f- Список команд");
                player.sendMessage("§7/wiki §f- Полная документация (если доступна)");
                player.sendMessage(" ");
                player.sendMessage("§bПриятной игры в AXIOM!");
                player.sendMessage(" ");
                
                completedTutorial.add(player.getUniqueId());
                tutorialStep.remove(player.getUniqueId());
                saveTutorialProgress(player.getUniqueId(), true);
                break;
        }
    }
    
    private void scheduleNextStep(Player player, int currentStep, int delaySeconds) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                int nextStep = currentStep + 1;
                tutorialStep.put(player.getUniqueId(), nextStep);
                showTutorialStep(player, nextStep);
            }
        }, 20 * delaySeconds);
    }
    
    /**
     * Skip to next tutorial step.
     */
    public void skipStep(Player player) {
        Integer current = tutorialStep.get(player.getUniqueId());
        if (current != null) {
            int nextStep = current + 1;
            tutorialStep.put(player.getUniqueId(), nextStep);
            showTutorialStep(player, nextStep);
        }
    }
    
    /**
     * Reset tutorial progress.
     */
    public void resetTutorial(Player player) {
        completedTutorial.remove(player.getUniqueId());
        tutorialStep.remove(player.getUniqueId());
        player.sendMessage("§aТуториал сброшен. Используйте §b/axiom tutorial §aдля начала.");
    }
    
    private void loadTutorialProgress() {
        // Load from file (would need to implement persistence)
        // For now, keep in memory
    }
    
    private void saveTutorialProgress(UUID playerId, boolean completed) {
        // Save to file (would need to implement persistence)
    }
    
    public boolean hasCompletedTutorial(UUID playerId) {
        return completedTutorial.contains(playerId);
    }
    
    /**
     * Handle tutorial book clicks.
     */
    @EventHandler
    public void onTutorialClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        // Check for tutorial menu title (remove color codes for check)
        if (title == null) return;
        String cleanTitle = org.bukkit.ChatColor.stripColor(title);
        if (!cleanTitle.contains("Руководство") && !cleanTitle.contains("АТОМ") && !cleanTitle.contains("AXIOM")) return;
        
        e.setCancelled(true); // Always cancel to prevent item pickup
        
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        
        // Check if clicked outside inventory (player inventory slots)
        if (slot >= e.getInventory().getSize()) return;
        
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        
        // Play click sound
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        
        // Close button
        if (item.getType() == Material.BARRIER || slot == 49) {
            p.closeInventory();
            return;
        }
        
        // Show chapter content
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String chapterName = meta.getDisplayName();
        if (chapterName == null || chapterName.trim().equals("") || chapterName.equals(" ")) return;
        
        showChapterContent(p, chapterName);
    }
    
    private void showChapterContent(Player player, String chapterName) {
        player.closeInventory();
        
        // Remove color codes for comparison
        String cleanName = org.bukkit.ChatColor.stripColor(chapterName);
        
        if (cleanName.contains("ГЛАВА 1") || cleanName.contains("Первые шаги")) {
            showChapter1(player);
        } else if (cleanName.contains("ГЛАВА 2") || cleanName.contains("Экономика")) {
            showChapter2(player);
        } else if (cleanName.contains("ГЛАВА 3") || cleanName.contains("Территория")) {
            showChapter3(player);
        } else if (cleanName.contains("ГЛАВА 4") || cleanName.contains("Война")) {
            showChapter4(player);
        } else if (cleanName.contains("ГЛАВА 5") || cleanName.contains("Технологии")) {
            showChapter5(player);
        } else if (cleanName.contains("ГЛАВА 6") || cleanName.contains("Дипломатия")) {
            showChapter6(player);
        } else if (cleanName.contains("ГЛАВА 7") || cleanName.contains("Религия")) {
            showChapter7(player);
        } else if (cleanName.contains("ГЛАВА 8") || cleanName.contains("Города")) {
            showChapter8(player);
        } else if (cleanName.contains("ГЛАВА 9") || cleanName.contains("Выживание")) {
            showChapter9(player);
        } else if (cleanName.contains("ГЛАВА 10") || cleanName.contains("Продвинутые")) {
            showChapter10(player);
        }
    }
    
    private void showChapter1(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§b══════ §fГЛАВА 1: ПЕРВЫЕ ШАГИ §b══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l1.1. Создание нации");
        p.sendMessage("§7Нация — основа AXIOM. Без неё вы не сможете:");
        p.sendMessage("§7  • Клеймить территорию");
        p.sendMessage("§7  • Управлять экономикой");
        p.sendMessage("§7  • Вести дипломатию");
        p.sendMessage(" ");
        p.sendMessage("§aСоздать нацию:");
        p.sendMessage("§b  /axiom nation create <название>");
        p.sendMessage(" ");
        p.sendMessage("§7§l1.2. Первая команда");
        p.sendMessage("§7Откройте главное меню:");
        p.sendMessage("§b  /nation");
        p.sendMessage("§7Или:");
        p.sendMessage("§b  /axiom nation");
        p.sendMessage(" ");
        p.sendMessage("§7§l1.3. Первая территория");
        p.sendMessage("§7При создании нации текущий чанк автоматически клеймится.");
        p.sendMessage("§7Для клейма новых чанков:");
        p.sendMessage("§b  /axiom claim");
        p.sendMessage("§7(Требуется двойной клик для подтверждения)");
        p.sendMessage(" ");
        p.sendMessage("§aСледующий шаг: §b/nation §7→ §aЭкономика");
        p.sendMessage(" ");
    }
    
    private void showChapter2(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§a══════ §fГЛАВА 2: ЭКОНОМИКА §a══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l2.1. Казна и валюта");
        p.sendMessage("§7Каждая нация имеет:");
        p.sendMessage("§7  • Свою валюту (по умолчанию: AXC)");
        p.sendMessage("§7  • Казна (начальная: 10,000)");
        p.sendMessage(" ");
        p.sendMessage("§7§l2.2. Печать денег");
        p.sendMessage("§7Лимиты для защиты от инфляции:");
        p.sendMessage("§7  • Максимум: 20% казны в день");
        p.sendMessage("§7  • Кулдаун: 1 минута между печатью");
        p.sendMessage("§7  • Команда: §b/axiom economy print <сумма>");
        p.sendMessage(" ");
        p.sendMessage("§7§l2.3. Налоги");
        p.sendMessage("§7Налоги собираются с доходов игроков.");
        p.sendMessage("§7Управление: §b/nation §7→ §aЭкономика §7→ Налоги");
        p.sendMessage(" ");
        p.sendMessage("§7§l2.4. Инфляция и ВВП");
        p.sendMessage("§7Чрезмерная печать денег вызывает инфляцию!");
        p.sendMessage("§7ВВП рассчитывается за последние 24 часа.");
        p.sendMessage(" ");
    }
    
    private void showChapter3(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§e══════ §fГЛАВА 3: ТЕРРИТОРИЯ §e══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l3.1. Клейм чанков");
        p.sendMessage("§7Чанк = 16×16 блоков.");
        p.sendMessage("§7Команда: §b/axiom claim");
        p.sendMessage("§7Первые 5 чанков — бесплатные!");
        p.sendMessage(" ");
        p.sendMessage("§7§l3.2. Лимиты территории");
        p.sendMessage("§7Максимум чанков:");
        p.sendMessage("§7  §b100 + (население × 2)");
        p.sendMessage("§7Пример: 10 граждан = 120 чанков");
        p.sendMessage(" ");
        p.sendMessage("§7§l3.3. Защита территории");
        p.sendMessage("§7В заклейменных чанках:");
        p.sendMessage("§7  • Запрещено строительство (не членам)");
        p.sendMessage("§7  • Запрещено PvP (если не warzone)");
        p.sendMessage(" ");
        p.sendMessage("§7§l3.4. Анклейм");
        p.sendMessage("§7Команда: §b/axiom unclaim");
        p.sendMessage("§7Кулдаун восстановления: 5 минут");
        p.sendMessage(" ");
    }
    
    private void showChapter4(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§c══════ §fГЛАВА 4: ВОЙНА §c══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l4.1. Объявление войны");
        p.sendMessage("§7Стоимость: §c5,000 валюты");
        p.sendMessage("§7Длительность: §c72 часа");
        p.sendMessage("§7Команда: §b/axiom diplomacy declare-war <nationId>");
        p.sendMessage(" ");
        p.sendMessage("§7§l4.2. Warzone");
        p.sendMessage("§7При активной войне территория противника становится Warzone:");
        p.sendMessage("§7  • Темное небо и дым");
        p.sendMessage("§7  • Таймер в чате");
        p.sendMessage("§7  • PvP разрешен");
        p.sendMessage(" ");
        p.sendMessage("§7§l4.3. Мобилизация");
        p.sendMessage("§7Мобилизация дает военные бонусы.");
        p.sendMessage("§7Управление: §b/nation §7→ §cДипломатия");
        p.sendMessage(" ");
    }
    
    private void showChapter5(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§d══════ §fГЛАВА 5: ТЕХНОЛОГИИ §d══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l5.1. Дерево технологий");
        p.sendMessage("§75 веток исследований:");
        p.sendMessage("§7  • Военные технологии");
        p.sendMessage("§7  • Промышленность");
        p.sendMessage("§7  • Экономика");
        p.sendMessage("§7  • Инфраструктура");
        p.sendMessage("§7  • Наука");
        p.sendMessage(" ");
        p.sendMessage("§7§l5.2. Исследование");
        p.sendMessage("§7Требования:");
        p.sendMessage("§7  • Образование: тир × 10 (тир 1 = 10, тир 5 = 50)");
        p.sendMessage("§7  • Предварительные технологии");
        p.sendMessage("§7  • Достаточно средств в казне");
        p.sendMessage(" ");
        p.sendMessage("§7§l5.3. Открыть дерево");
        p.sendMessage("§b  /nation §7→ §dТехнологии");
        p.sendMessage(" ");
    }
    
    private void showChapter6(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§6══════ §fГЛАВА 6: ДИПЛОМАТИЯ §6══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l6.1. Альянсы");
        p.sendMessage("§7Альянсы защищают от войны.");
        p.sendMessage("§7Управление: §b/nation §7→ §6Дипломатия");
        p.sendMessage(" ");
        p.sendMessage("§7§l6.2. Репутация");
        p.sendMessage("§7Репутация влияет на отношения (от -100 до +100).");
        p.sendMessage("§7Высокая репутация = лучше торговля.");
        p.sendMessage(" ");
        p.sendMessage("§7§l6.3. Договоры");
        p.sendMessage("§7Можно заключать торговые и военные договоры.");
        p.sendMessage(" ");
    }
    
    private void showChapter7(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§5══════ §fГЛАВА 7: РЕЛИГИЯ §5══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l7.1. Основание религии");
        p.sendMessage("§7Команда: §b/axiom religion found <id> <название>");
        p.sendMessage(" ");
        p.sendMessage("§7§l7.2. Десятина");
        p.sendMessage("§7С верующих собирается 5% дохода.");
        p.sendMessage(" ");
        p.sendMessage("§7§l7.3. Святые места");
        p.sendMessage("§7Святые места дают бонусы верующим.");
        p.sendMessage(" ");
    }
    
    private void showChapter8(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§b══════ §fГЛАВА 8: ГОРОДА §b══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l8.1. Создание города");
        p.sendMessage("§7Команда: §b/axiom city found <название>");
        p.sendMessage("§7Город создается в текущем чанке.");
        p.sendMessage(" ");
        p.sendMessage("§7§l8.2. Развитие");
        p.sendMessage("§7Города растут автоматически.");
        p.sendMessage("§7Управление: §b/nation §7→ §bГорода");
        p.sendMessage(" ");
    }
    
    private void showChapter9(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§c══════ §fГЛАВА 9: ВЫЖИВАНИЕ §c══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l9.1. Защита территории");
        p.sendMessage("§7• Клеймите важные участки");
        p.sendMessage("§7• Стройте укрепления");
        p.sendMessage("§7• Держите казну пополненной");
        p.sendMessage(" ");
        p.sendMessage("§7§l9.2. Экономическая безопасность");
        p.sendMessage("§7• Не печатайте слишком много денег");
        p.sendMessage("§7• Собирайте налоги разумно");
        p.sendMessage("§7• Торгуйте с другими нациями");
        p.sendMessage(" ");
        p.sendMessage("§7§l9.3. Дипломатические советы");
        p.sendMessage("§7• Создавайте альянсы для защиты");
        p.sendMessage("§7• Поддерживайте репутацию");
        p.sendMessage("§7• Избегайте ненужных войн");
        p.sendMessage(" ");
    }
    
    private void showChapter10(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§e══════ §fГЛАВА 10: ПРОДВИНУТЫЕ ФУНКЦИИ §e══════");
        p.sendMessage(" ");
        p.sendMessage("§7§l10.1. Интеграция с модами");
        p.sendMessage("§7AXIOM поддерживает 30+ модов:");
        p.sendMessage("§7  • Tacz, PointBlank — оружие");
        p.sendMessage("§7  • Immersive Engineering — заводы");
        p.sendMessage("§7  • Applied Energistics 2 — логистика");
        p.sendMessage("§7  • Xaeros Minimap — карты");
        p.sendMessage(" ");
        p.sendMessage("§7§l10.2. Команды админа");
        p.sendMessage("§7  /axiom reload §7— Перезагрузка конфига");
        p.sendMessage("§7  /axiom save §7— Сохранение данных");
        p.sendMessage("§7  /testbot start §7— Автотесты");
        p.sendMessage(" ");
        p.sendMessage("§7§l10.3. Web-дашборд");
        p.sendMessage("§7Данные экспортируются в §bweb/data.json");
        p.sendMessage("§7Каждые 5 минут обновление.");
        p.sendMessage(" ");
    }
    
    /**
     * Get comprehensive tutorial statistics.
     */
    public synchronized Map<String, Object> getTutorialStatistics(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        boolean completed = completedTutorial.contains(playerId);
        Integer currentStep = tutorialStep.get(playerId);
        
        stats.put("hasCompleted", completed);
        stats.put("currentStep", currentStep != null ? currentStep : -1);
        stats.put("totalSteps", 7);
        
        if (currentStep != null) {
            double progress = ((double) currentStep / 7.0) * 100;
            stats.put("progressPercent", progress);
        } else if (completed) {
            stats.put("progressPercent", 100.0);
        } else {
            stats.put("progressPercent", 0.0);
        }
        
        // Tutorial rating
        String rating = "НЕ НАЧАТ";
        if (completed) rating = "ЗАВЕРШЁН";
        else if (currentStep != null && currentStep >= 5) rating = "ПОЧТИ ЗАВЕРШЁН";
        else if (currentStep != null && currentStep >= 3) rating = "В ПРОЦЕССЕ";
        else if (currentStep != null && currentStep >= 1) rating = "НАЧАТ";
        stats.put("rating", rating);
        
        // Player info
        org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(playerId);
        stats.put("playerName", player.getName());
        
        // Nation status
        String nationId = plugin.getPlayerDataManager().getNation(playerId);
        stats.put("hasNation", nationId != null);
        if (nationId != null) {
            stats.put("nationId", nationId);
        }
        
        return stats;
    }
    
    /**
     * Get global tutorial statistics.
     */
    public synchronized Map<String, Object> getGlobalTutorialStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalCompleted", completedTutorial.size());
        stats.put("totalInProgress", tutorialStep.size());
        
        // Completion rate (simplified)
        int totalPlayers = plugin.getServer().getOfflinePlayers().length;
        double completionRate = totalPlayers > 0 ? ((double) completedTutorial.size() / totalPlayers) * 100 : 0;
        stats.put("completionRate", completionRate);
        
        // Steps distribution
        Map<Integer, Integer> stepDistribution = new HashMap<>();
        for (Integer step : tutorialStep.values()) {
            stepDistribution.put(step, stepDistribution.getOrDefault(step, 0) + 1);
        }
        stats.put("stepDistribution", stepDistribution);
        
        return stats;
    }
    
    /**
     * Complete tutorial manually.
     */
    public synchronized String completeTutorial(UUID playerId) {
        if (completedTutorial.contains(playerId)) {
            return "Туториал уже завершён.";
        }
        
        completedTutorial.add(playerId);
        tutorialStep.remove(playerId);
        saveTutorialProgress(playerId, true);
        
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage("§aТуториал завершён!");
        }
        
        return "Туториал завершён.";
    }
    
    /**
     * Get tutorial progress for player.
     */
    public synchronized int getTutorialProgress(UUID playerId) {
        if (completedTutorial.contains(playerId)) {
            return 100;
        }
        
        Integer step = tutorialStep.get(playerId);
        if (step == null) {
            return 0;
        }
        
        return (int)((step / 7.0) * 100);
    }
    
    /**
     * Check if player needs tutorial.
     */
    public synchronized boolean needsTutorial(UUID playerId) {
        return !completedTutorial.contains(playerId) && 
               plugin.getPlayerDataManager().getNation(playerId) == null;
    }
}

