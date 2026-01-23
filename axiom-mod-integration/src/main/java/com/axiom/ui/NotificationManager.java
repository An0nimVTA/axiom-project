package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private static final NotificationManager INSTANCE = new NotificationManager();
    private final List<Notification> notifications = new ArrayList<>();
    private final int MAX_NOTIFICATIONS = 5;
    private final int NOTIFICATION_DURATION = 5000; // 5 seconds

    public enum NotificationType {
        INFO(0xFF2196F3, "ℹ"),      // Blue
        SUCCESS(0xFF4CAF50, "✓"),   // Green
        WARNING(0xFFFFC107, "⚠"),   // Yellow
        ERROR(0xFFF44336, "✗");     // Red

        private final int color;
        private final String icon;

        NotificationType(int color, String icon) {
            this.color = color;
            this.icon = icon;
        }

        public int getColor() { return color; }
        public String getIcon() { return icon; }
    }

    public static class Notification {
        private final String message;
        private final NotificationType type;
        private final long createdAt;
        private float alpha = 0f;
        private boolean fadingOut = false;

        public Notification(String message, NotificationType type) {
            this.message = message;
            this.type = type;
            this.createdAt = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > INSTANCE.NOTIFICATION_DURATION;
        }

        public void update() {
            if (isExpired() && !fadingOut) {
                fadingOut = true;
            }

            if (fadingOut) {
                alpha = Math.max(0, alpha - 0.05f);
            } else {
                alpha = Math.min(1, alpha + 0.1f);
            }
        }

        public boolean shouldRemove() {
            return fadingOut && alpha <= 0;
        }
    }

    private NotificationManager() {}

    public static NotificationManager getInstance() {
        return INSTANCE;
    }

    public void show(String message, NotificationType type) {
        // Remove oldest if at max
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }

        notifications.add(new Notification(message, type));

        // Play sound
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            switch (type) {
                case SUCCESS -> mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                case WARNING -> mc.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.5f, 0.8f);
                case ERROR -> mc.player.playSound(SoundEvents.ANVIL_LAND, 0.3f, 1.0f);
                default -> mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
            }
        }
    }

    public void info(String message) {
        show(message, NotificationType.INFO);
    }

    public void success(String message) {
        show(message, NotificationType.SUCCESS);
    }

    public void warning(String message) {
        show(message, NotificationType.WARNING);
    }

    public void error(String message) {
        show(message, NotificationType.ERROR);
    }

    public void render(GuiGraphics gfx, int screenWidth, int screenHeight) {
        notifications.removeIf(Notification::shouldRemove);

        int y = 10;
        for (Notification notif : notifications) {
            notif.update();
            renderNotification(gfx, notif, screenWidth, y);
            y += 35;
        }
    }

    private void renderNotification(GuiGraphics gfx, Notification notif, int screenWidth, int y) {
        Minecraft mc = Minecraft.getInstance();
        String fullMessage = notif.type.getIcon() + " " + notif.message;
        int width = mc.font.width(fullMessage) + 20;
        int x = screenWidth - width - 10;

        // Background with alpha
        int bgColor = (notif.type.getColor() & 0x00FFFFFF) | ((int)(notif.alpha * 180) << 24);
        gfx.fill(x, y, x + width, y + 25, bgColor);

        // Border
        int borderColor = notif.type.getColor() | ((int)(notif.alpha * 255) << 24);
        gfx.renderOutline(x, y, width, 25, borderColor);

        // Text with alpha
        int textColor = 0xFFFFFFFF | ((int)(notif.alpha * 255) << 24);
        gfx.drawString(mc.font, fullMessage, x + 10, y + 8, textColor, false);
    }

    public void clear() {
        notifications.clear();
    }
}
