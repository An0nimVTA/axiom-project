package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Service for visual effects: particles, sounds, titles, and actionbar messages.
 * Implements the visual identity of AXIOM.
 */
public class VisualEffectsService {
    private final AXIOM plugin;
    
    public VisualEffectsService(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Show effect when player joins a nation.
     */
    public void playNationJoinEffect(Player player) {
        if (player == null) return;
        Location loc = player.getLocation();
        if (!isWorldAvailable(loc)) return;
        
        // Gold particles
        for (int i = 0; i < 20; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 1, 
                        0.5, 1, 0.5, 0.1);
                }
            }.runTaskLater(plugin, i);
        }
        
        // Sound
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        
        // Title
        player.sendTitle("§6§lДобро пожаловать!", 
            "§fВы вступили в нацию!", 10, 40, 10);
    }
    
    /**
     * Show war declaration effect.
     */
    public void playWarDeclarationEffect(Player player, String attackerName, String defenderName) {
        if (player == null) return;
        Location loc = player.getLocation();
        if (!isWorldAvailable(loc)) return;
        
        // Red particles (explosion-like)
        for (int i = 0; i < 30; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 1, 
                        1, 1, 1, 0, new Particle.DustOptions(Color.RED, 1.5f));
                }
            }.runTaskLater(plugin, i);
        }
        
        // Sound
        player.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.8f);
        
        // Actionbar
        sendActionBar(player, "§4[ВОЙНА] §cНация '" + attackerName + "' объявила войну '" + defenderName + "'!");
    }
    
    /**
     * Show warzone visual effect.
     */
    public void playWarzoneEffect(Player player, Location loc) {
        if (!isWorldAvailable(loc)) return;
        // Dark smoke particles
        loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 3, 5, 2, 5, 0.1);
        
        // Red particles at ground level
        loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 5, 2, 0.1, 2, 0,
            new Particle.DustOptions(Color.RED, 1.0f));
    }
    
    /**
     * Show holy site effect.
     */
    public void playHolySiteEffect(Player player, Location loc) {
        if (player == null || !isWorldAvailable(loc)) return;
        // White glowing particles
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 10, 0.5, 1, 0.5, 0.05);
        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 5, 1, 1, 1, 0.1);
        
        // Bell sound
        player.playSound(loc, Sound.BLOCK_BELL_USE, 0.5f, 1.5f);
    }
    
    /**
     * Show festival/fireworks effect over capital.
     */
    public void playFestivalEffect(Location capitalLoc) {
        playHolidayEffect(capitalLoc);
    }
    
    /**
     * Show holiday/fireworks effect at location.
     */
    public void playHolidayEffect(Location loc) {
        if (!isWorldAvailable(loc)) return;
        World world = loc.getWorld();
        
        // Fireworks
        for (int i = 0; i < 5; i++) {
            Location fireworkLoc = loc.clone().add(
                (Math.random() - 0.5) * 20,
                10 + Math.random() * 5,
                (Math.random() - 0.5) * 20
            );
            world.spawn(fireworkLoc, Firework.class, fw -> {
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST)
                    .withColor(Color.AQUA, Color.WHITE, Color.BLUE)
                    .build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            });
        }
        
        // Gold particles
        for (int i = 0; i < 10; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 1, 
                        2, 2, 2, 0.1);
                }
            }.runTaskLater(plugin, i * 2);
        }
    }
    
    /**
     * Show economic crisis effect (gray fog).
     */
    public void playEconomicCrisisEffect(Player player, Location loc) {
        if (player == null || !isWorldAvailable(loc)) return;
        // Gray smoke (fog)
        loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 20, 10, 2, 10, 0.2);
        
        // Dark particles
        loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 10, 5, 1, 5, 0,
            new Particle.DustOptions(Color.GRAY, 2.0f));
    }
    
    /**
     * Show election announcement effect.
     */
    public void playElectionEffect(Player player, String nationName, String electionType) {
        if (player == null) return;
        Location loc = player.getLocation();
        if (!isWorldAvailable(loc)) return;
        
        // Blue particles (democracy)
        for (int i = 0; i < 15; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 
                        0.5, 1, 0.5, 0.05);
                }
            }.runTaskLater(plugin, i * 2);
        }
        
        // Sound
        player.playSound(loc, Sound.ENTITY_VILLAGER_CELEBRATE, 0.8f, 1.0f);
        
        // Title
        player.sendTitle("§e§l[ДЕМОКРАТИЯ]", 
            "§fНачались выборы " + electionType + " в '" + nationName + "'!", 10, 60, 10);
    }
    
    /**
     * Send actionbar message.
     */
    public void sendActionBar(Player player, String message) {
        if (player == null) return;
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }
    
    /**
     * Send global event message (title + actionbar).
     */
    public void sendGlobalEvent(Player player, String title, String subtitle, String actionBar) {
        if (player == null) return;
        player.sendTitle(title, subtitle, 10, 100, 20);
        if (actionBar != null) {
            sendActionBar(player, actionBar);
        }
    }

    /**
     * Send visual effect for a nation border to a player.
     */
    public void sendNationBorderEffect(Player player, int color) {
        if (player == null) return;
        // TODO: Implement actual nation border visual effects
        player.spawnParticle(Particle.END_ROD, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.01);
    }

    /**
     * Send visual effect for a city border to a player.
     */
    public void sendCityBorderEffect(Player player, int color) {
        if (player == null) return;
        // TODO: Implement actual city border visual effects
        player.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.01);
    }
    
    /**
     * Create glowing effect on item (for confirmation).
     */
    public org.bukkit.enchantments.Enchantment getGlowEnchantment() {
        // Use fake enchantment for glow effect
        return org.bukkit.enchantments.Enchantment.getByKey(
            org.bukkit.NamespacedKey.minecraft("glow"));
    }
    
    /**
     * Show legendary achievement effect (LEGENDARY/MYTHIC tier).
     */
    public void playLegendaryAchievementEffect(Player player, String achievementName, String rarity) {
        if (player == null) return;
        Location loc = player.getLocation();
        if (!isWorldAvailable(loc)) return;
        
        // Colored title based on rarity
        String titleColor = rarity.equals("MYTHIC") ? "§d§l[MYTHIC]" : "§5§l[LEGENDARY]";
        String subtitle = rarity.equals("MYTHIC") ? "§6" + achievementName : "§e" + achievementName;
        player.sendTitle(titleColor + " ДОСТИЖЕНИЕ", subtitle, 10, 100, 20);
        
        // Broadcast to all players nearby
        for (Player nearby : Bukkit.getOnlinePlayers()) {
            if (nearby != null && nearby.getWorld().equals(loc.getWorld()) && nearby.getLocation().distance(loc) <= 50) {
                nearby.playSound(nearby.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
            }
        }
        
        // Epic particle effects - spiral up
        for (int ring = 0; ring < 10; ring++) {
            final int ringNum = ring;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double angle = Math.PI * 2 * ringNum / 10;
                    double x = Math.cos(angle) * 2;
                    double z = Math.sin(angle) * 2;
                    Location ringLoc = loc.clone().add(x, ringNum * 0.5, z);
                    
                    // Multiple particle types
                    ringLoc.getWorld().spawnParticle(Particle.END_ROD, ringLoc, 3, 0.1, 0.1, 0.1, 0.05);
                    ringLoc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, ringLoc, 10, 0.3, 0.3, 0.3, 0.05);
                    ringLoc.getWorld().spawnParticle(Particle.TOTEM, ringLoc, 1, 0, 0, 0, 0.1);
                }
            }.runTaskLater(plugin, ring * 2);
        }
        
        // Fireworks explosion
        for (int i = 0; i < 10; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location fwLoc = loc.clone().add(
                        (Math.random() - 0.5) * 15,
                        5 + Math.random() * 10,
                        (Math.random() - 0.5) * 15
                    );
                    loc.getWorld().spawn(fwLoc, Firework.class, fw -> {
                        FireworkMeta meta = fw.getFireworkMeta();
                        FireworkEffect.Type type = rarity.equals("MYTHIC") ? 
                            FireworkEffect.Type.BURST : FireworkEffect.Type.STAR;
                        Color[] colors = rarity.equals("MYTHIC") ?
                            new Color[]{Color.PURPLE, Color.FUCHSIA, Color.YELLOW, Color.WHITE} :
                            new Color[]{Color.ORANGE, Color.RED, Color.YELLOW};
                        meta.addEffect(FireworkEffect.builder()
                            .with(type)
                            .withColor(colors)
                            .build());
                        meta.setPower(2);
                        fw.setFireworkMeta(meta);
                    });
                }
            }.runTaskLater(plugin, i * 3);
        }
        
        // Glow effect around player
        for (int i = 0; i < 30; i++) {
            final int tick = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double angle = Math.PI * 2 * tick / 30;
                    double x = Math.cos(angle) * 3;
                    double z = Math.sin(angle) * 3;
                    Location glowLoc = loc.clone().add(x, 2, z);
                    glowLoc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, glowLoc, 5, 0.1, 0.1, 0.1, 0.05);
                }
            }.runTaskLater(plugin, i * 2);
        }
        
        // Sound effects
        player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> 
            player.playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.6f, 0.7f), 10);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> 
            player.playSound(loc, Sound.UI_TOAST_IN, 1.0f, 1.0f), 20);
        
        // Actionbar message
        String message = rarity.equals("MYTHIC") ?
            "§d§l⚡ MYTHIC ДОСТИЖЕНИЕ: §6" + achievementName + " §d⚡" :
            "§5§l✨ LEGENDARY ДОСТИЖЕНИЕ: §e" + achievementName + " §5✨";
        sendActionBar(player, message);
    }
    
    /**
     * Get comprehensive visual effects statistics.
     */
    public synchronized Map<String, Object> getVisualEffectsStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        // Count effects by type (would need tracking to be fully accurate)
        stats.put("availableEffects", java.util.Arrays.asList(
            "nation_join", "war_declaration", "warzone", "holy_site", 
            "festival", "holiday", "economic_crisis", "election", "legendary_achievement"
        ));
        
        // Effect categories
        Map<String, Integer> categories = new java.util.HashMap<>();
        categories.put("particle_effects", 9);
        categories.put("sound_effects", 7);
        categories.put("title_effects", 4);
        categories.put("actionbar_effects", 6);
        stats.put("effectCategories", categories);
        
        // Integration status
        stats.put("particleSystemAvailable", true);
        stats.put("soundSystemAvailable", true);
        stats.put("titleSystemAvailable", true);
        stats.put("actionbarSystemAvailable", true);
        
        return stats;
    }
    
    /**
     * Play custom particle effect at location.
     */
    public void playCustomParticle(org.bukkit.Location loc, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra, Object data) {
        if (loc == null || loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, extra, data);
    }
    
    /**
     * Play custom sound effect at location.
     */
    public void playCustomSound(org.bukkit.Location loc, Sound sound, float volume, float pitch) {
        if (loc == null || loc.getWorld() == null) return;
        loc.getWorld().playSound(loc, sound, volume, pitch);
    }
    
    /**
     * Send title to multiple players.
     */
    public void broadcastTitle(java.util.Collection<Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }
    
    /**
     * Send actionbar to multiple players.
     */
    public void broadcastActionBar(java.util.Collection<Player> players, String message) {
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                sendActionBar(player, message);
            }
        }
    }

    private boolean isWorldAvailable(Location loc) {
        return loc != null && loc.getWorld() != null;
    }
}

