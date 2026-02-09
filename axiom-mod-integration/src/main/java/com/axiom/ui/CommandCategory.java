package com.axiom.ui;

public enum CommandCategory {
    NATION("Нация", "Nation", 0xFF4CAF50, "Управление нацией и территорией", "Nation and territory management", "minecraft:white_banner"),
    ECONOMY("Экономика", "Economy", 0xFFFFC107, "Деньги, банки, торговля", "Money, banks, trade", "minecraft:gold_ingot"),
    DIPLOMACY("Дипломатия", "Diplomacy", 0xFF2196F3, "Договоры, союзы, война", "Treaties, alliances, war", "minecraft:book"),
    TECHNOLOGY("Технологии", "Technology", 0xFF9C27B0, "Исследования и развитие", "Research and progress", "minecraft:redstone"),
    MILITARY("Военное дело", "Military", 0xFFF44336, "Войны, осады, оружие", "Wars, sieges, weapons", "minecraft:iron_sword"),
    CITY("Города", "Cities", 0xFF00BCD4, "Управление городами", "City management", "minecraft:bricks"),
    RELIGION("Религия", "Religion", 0xFFFF9800, "Вера и культура", "Faith and culture", "minecraft:enchanting_table"),
    PROFILE("Профиль", "Profile", 0xFF607D8B, "Личная информация", "Personal info", "minecraft:player_head"),
    ADMIN("Администрирование", "Administration", 0xFF9E9E9E, "Команды для админов", "Admin commands", "minecraft:command_block");

    private final String displayNameRu;
    private final String displayNameEn;
    private final int color;
    private final String descriptionRu;
    private final String descriptionEn;
    private final String iconItemId;

    CommandCategory(String displayNameRu, String displayNameEn, int color, String descriptionRu, String descriptionEn, String iconItemId) {
        this.displayNameRu = displayNameRu;
        this.displayNameEn = displayNameEn;
        this.color = color;
        this.descriptionRu = descriptionRu;
        this.descriptionEn = descriptionEn;
        this.iconItemId = iconItemId;
    }

    public String getDisplayName() { return UiText.pick(displayNameRu, displayNameEn); }
    public int getColor() { return color; }
    public String getDescription() { return UiText.pick(descriptionRu, descriptionEn); }
    public String getIconItemId() { return iconItemId; }
}
