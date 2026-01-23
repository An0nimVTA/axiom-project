package com.axiom.ui;

public enum CommandCategory {
    NATION("Нация", 0xFF4CAF50, "Управление нацией и территорией"),
    ECONOMY("Экономика", 0xFFFFC107, "Деньги, банки, торговля"),
    DIPLOMACY("Дипломатия", 0xFF2196F3, "Договоры, союзы, война"),
    TECHNOLOGY("Технологии", 0xFF9C27B0, "Исследования и развитие"),
    MILITARY("Военное дело", 0xFFF44336, "Войны, осады, оружие"),
    CITY("Города", 0xFF00BCD4, "Управление городами"),
    RELIGION("Религия", 0xFFFF9800, "Вера и культура"),
    PROFILE("Профиль", 0xFF607D8B, "Личная информация"),
    ADMIN("Администрирование", 0xFF9E9E9E, "Команды для админов");

    private final String displayName;
    private final int color;
    private final String description;

    CommandCategory(String displayName, int color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }
    public String getDescription() { return description; }
}
