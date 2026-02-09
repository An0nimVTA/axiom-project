package com.axiom.ui;

import java.util.List;

public class CommandInfo {
    private final String command;
    private final String displayName;
    private final String shortDesc;
    private final String fullDesc;
    private final String iconItemId;
    private final UiRarity rarity;
    private final CommandCategory category;
    private final List<String> aliases;
    private final List<String> examples;
    private final String permission;
    private final boolean requiresNation;

    public CommandInfo(String command, String displayName, String shortDesc, String fullDesc,
                      CommandCategory category, List<String> aliases, List<String> examples,
                      String permission, boolean requiresNation) {
        this(command, displayName, shortDesc, fullDesc, null, UiRarity.COMMON,
            category, aliases, examples, permission, requiresNation);
    }

    public CommandInfo(String command, String displayName, String shortDesc, String fullDesc,
                      String iconItemId, UiRarity rarity,
                      CommandCategory category, List<String> aliases, List<String> examples,
                      String permission, boolean requiresNation) {
        this.command = command;
        this.displayName = displayName;
        this.shortDesc = shortDesc;
        this.fullDesc = fullDesc;
        this.iconItemId = iconItemId;
        this.rarity = rarity == null ? UiRarity.COMMON : rarity;
        this.category = category;
        this.aliases = aliases;
        this.examples = examples;
        this.permission = permission;
        this.requiresNation = requiresNation;
    }

    public String getCommand() { return command; }
    public String getDisplayName() { return displayName; }
    public String getShortDesc() { return shortDesc; }
    public String getFullDesc() { return fullDesc; }
    public String getIconItemId() { return iconItemId; }
    public UiRarity getRarity() { return rarity; }
    public CommandCategory getCategory() { return category; }
    public List<String> getAliases() { return aliases; }
    public List<String> getExamples() { return examples; }
    public String getPermission() { return permission; }
    public boolean requiresNation() { return requiresNation; }
}
