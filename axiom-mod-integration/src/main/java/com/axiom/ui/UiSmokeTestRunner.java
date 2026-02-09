package com.axiom.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UiSmokeTestRunner {
    private static final File CONFIG_FILE = new File("config/axiomui/autotest.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<UiStep> steps = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private TestConfig config = TestConfig.defaults();

    private boolean running = false;
    private int stepIndex = 0;
    private int tickDelay = 0;
    private long startedAtMs = 0;
    private int autoStartCountdown = -1;
    private PendingCommand pendingCommand;
    private long lastSummaryAtMs = -1;
    private boolean lastSummaryOk = false;
    private int lastSummaryFailures = 0;
    private String lastSummaryDuration = "";
    private String lastSummaryState = "";

    public void start() {
        if (running) {
            NotificationManager.getInstance().warning("UI тесты уже запущены");
            return;
        }
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        loadConfig();
        startWithCurrentConfig();
    }

    public void startWithOverridesJson(String json) {
        if (running) {
            NotificationManager.getInstance().warning("UI тесты уже запущены");
            return;
        }
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        TestConfig override = null;
        if (json != null && !json.trim().isEmpty()) {
            try {
                override = GSON.fromJson(json, TestConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (override != null) {
            override.applyDefaults();
            config = override;
        } else {
            loadConfig();
        }
        startWithCurrentConfig();
    }

    private void startWithCurrentConfig() {
        if (!UiText.hasServerPreference()) {
            UiText.setLanguage(UiLanguage.RU, true);
            AxiomUiClientEvents.sendLanguageToServer("ru");
        }
        buildSteps();
        failures.clear();
        stepIndex = 0;
        tickDelay = 1;
        pendingCommand = null;
        running = true;
        startedAtMs = System.currentTimeMillis();
        NotificationManager.getInstance().info("Запуск UI smoke-тестов (" + steps.size() + " шагов)");
    }

    public void stop() {
        if (!running) {
            NotificationManager.getInstance().warning("UI тесты не запущены");
            return;
        }
        finish(true);
    }

    public boolean isRunning() {
        return running;
    }

    public void scheduleAutoStart() {
        loadConfig();
        if (!config.enabled) {
            autoStartCountdown = -1;
            return;
        }
        autoStartCountdown = Math.max(0, config.autoStartDelayTicks);
    }

    public void onCommandResult(String command, boolean success, String message) {
        if (pendingCommand == null) {
            return;
        }
        if (!pendingCommand.command.equals(command)) {
            return;
        }
        pendingCommand.completed = true;
        pendingCommand.success = success;
        pendingCommand.message = message;
    }

    public void tick() {
        if (autoStartCountdown >= 0 && !running) {
            if (autoStartCountdown == 0) {
                autoStartCountdown = -1;
                start();
            } else {
                autoStartCountdown--;
            }
        }
        if (!running) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (pendingCommand != null) {
            handlePendingCommand();
            return;
        }
        if (tickDelay > 0) {
            tickDelay--;
            return;
        }
        if (stepIndex >= steps.size()) {
            finish(false);
            return;
        }
        UiStep step = steps.get(stepIndex);
        if (step.command != null) {
            startCommandStep(step);
            return;
        }
        runStep(step);
        stepIndex++;
        tickDelay = Math.max(1, resolveDelayTicks(step));
    }

    private void handlePendingCommand() {
        if (pendingCommand.completed) {
            if (!pendingCommand.success) {
                String message = pendingCommand.message == null ? "" : (": " + pendingCommand.message);
                failures.add(pendingCommand.stepName + " -> команда не выполнена" + message);
            }
            int delay = pendingCommand.postDelayTicks;
            pendingCommand = null;
            stepIndex++;
            tickDelay = Math.max(1, delay);
            return;
        }

        pendingCommand.remainingTicks--;
        if (pendingCommand.remainingTicks <= 0) {
            failures.add(pendingCommand.stepName + " -> таймаут ожидания результата");
            int delay = pendingCommand.postDelayTicks;
            pendingCommand = null;
            stepIndex++;
            tickDelay = Math.max(1, delay);
        }
    }

    private void startCommandStep(UiStep step) {
        int delay = resolveDelayTicks(step);
        if (isCommandNoAck(step.command)) {
            AxiomUiClientEvents.sendCommandToServer(step.command);
            stepIndex++;
            tickDelay = Math.max(1, delay);
            return;
        }
        pendingCommand = new PendingCommand(step.name, step.command, Math.max(1, config.commandTimeoutTicks), delay);
        AxiomUiClientEvents.sendCommandToServer(step.command);
    }

    private void runStep(UiStep step) {
        try {
            if (step.action != null) {
                step.action.run();
            }
        } catch (Exception e) {
            failures.add(step.name + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        if (step.expectedScreen != null) {
            Screen current = Minecraft.getInstance().screen;
            if (current == null || !step.expectedScreen.isInstance(current)) {
                String actual = current == null ? "null" : current.getClass().getSimpleName();
                failures.add(step.name + " -> ожидали " + step.expectedScreen.getSimpleName() + ", получили " + actual);
            }
        }
    }

    private void finish(boolean interrupted) {
        running = false;
        long durationMs = System.currentTimeMillis() - startedAtMs;
        String duration = String.format("%.1fs", durationMs / 1000.0);

        lastSummaryAtMs = System.currentTimeMillis();
        lastSummaryDuration = duration;
        lastSummaryFailures = failures.size();
        if (interrupted) {
            lastSummaryOk = false;
            lastSummaryState = UiText.pick("Остановлено", "Interrupted");
            NotificationManager.getInstance().warning("UI тесты остановлены (" + duration + ")");
            return;
        }

        if (failures.isEmpty()) {
            lastSummaryOk = true;
            lastSummaryState = UiText.pick("Успех", "Success");
            NotificationManager.getInstance().success("UI тесты пройдены (" + duration + ")");
        } else {
            lastSummaryOk = false;
            lastSummaryState = UiText.pick("Ошибки", "Failures");
            NotificationManager.getInstance().error("UI тесты: ошибок " + failures.size() + " (" + duration + ")");
            for (String failure : failures) {
                System.out.println("[AXIOM UI TEST] " + failure);
            }
        }
    }

    public void renderOverlay(GuiGraphics gfx, int width, int height) {
        long now = System.currentTimeMillis();
        boolean show = running || (lastSummaryAtMs > 0 && (now - lastSummaryAtMs) < 12000);
        if (!show) {
            return;
        }
        int panelWidth = Math.min(360, Math.max(260, width - 40));
        int panelX = 12;
        int panelY = 48;
        int lineHeight = 12;
        int lines = running ? 6 : 5;
        int panelHeight = 18 + lines * lineHeight + 8;

        UiTheme.drawPanel(gfx, panelX, panelY, panelX + panelWidth, panelY + panelHeight);
        String title = UiText.pick("UI проверка", "UI Tests");
        gfx.drawString(Minecraft.getInstance().font, title, panelX + 10, panelY + 6, UiTheme.TEXT_PRIMARY, false);

        int y = panelY + 22;
        if (running) {
            int total = steps.size();
            int current = Math.min(stepIndex + 1, Math.max(total, 1));
            String progress = UiText.pick("Шаг", "Step") + " " + current + "/" + Math.max(total, 1);
            gfx.drawString(Minecraft.getInstance().font, progress, panelX + 10, y, UiTheme.TEXT_MUTED, false);
            y += lineHeight;

            String stepName = current <= steps.size() ? steps.get(Math.min(stepIndex, steps.size() - 1)).name : "-";
            gfx.drawString(Minecraft.getInstance().font, UiText.pick("Сейчас:", "Now:") + " " + stepName,
                panelX + 10, y, UiTheme.TEXT_PRIMARY, false);
            y += lineHeight;

            String failLine = UiText.pick("Ошибок: ", "Failures: ") + failures.size();
            gfx.drawString(Minecraft.getInstance().font, failLine, panelX + 10, y, failures.isEmpty() ? UiTheme.TEXT_MUTED : 0xFFFF6B6B, false);
            y += lineHeight;

            if (pendingCommand != null) {
                String waiting = UiText.pick("Ожидание команды:", "Waiting:") + " " + pendingCommand.command;
                gfx.drawString(Minecraft.getInstance().font, waiting, panelX + 10, y, UiTheme.TEXT_DIM, false);
                y += lineHeight;
            }
        }

        String state = running ? UiText.pick("Статус: Выполняется", "Status: Running") :
            UiText.pick("Статус: ", "Status: ") + lastSummaryState;
        int stateColor = running ? UiTheme.ACCENT : (lastSummaryOk ? 0xFF4ADE80 : 0xFFFF6B6B);
        gfx.drawString(Minecraft.getInstance().font, state, panelX + 10, y, stateColor, false);
        y += lineHeight;

        if (!running && lastSummaryAtMs > 0) {
            String dur = UiText.pick("Время: ", "Time: ") + lastSummaryDuration;
            gfx.drawString(Minecraft.getInstance().font, dur, panelX + 10, y, UiTheme.TEXT_MUTED, false);
            y += lineHeight;
            String failLine = UiText.pick("Ошибок: ", "Failures: ") + lastSummaryFailures;
            gfx.drawString(Minecraft.getInstance().font, failLine, panelX + 10, y, lastSummaryFailures == 0 ? UiTheme.TEXT_MUTED : 0xFFFF6B6B, false);
            y += lineHeight;
        }

        if (!failures.isEmpty()) {
            int maxPreview = 2;
            for (int i = 0; i < Math.min(maxPreview, failures.size()); i++) {
                String msg = failures.get(i);
                gfx.drawString(Minecraft.getInstance().font, "• " + truncate(msg, panelWidth - 20), panelX + 10, y, 0xFFFFB3B3, false);
                y += lineHeight;
            }
        }
    }

    private String truncate(String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        var font = Minecraft.getInstance().font;
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            out.append(value.charAt(i));
            if (font.width(out.toString()) + ellipsisWidth > maxWidth) {
                out.append(ellipsis);
                break;
            }
        }
        return out.toString();
    }

    private void buildSteps() {
        steps.clear();

        if (config.actions != null && !config.actions.isEmpty()) {
            buildActionSteps();
            return;
        }

        MainMenuScreen mainMenu = new MainMenuScreen();
        CommandMenuScreen menu = new CommandMenuScreen();

        if (config.includeUiScreens) {
            addStep("Открыть главное меню", () -> setScreen(mainMenu), MainMenuScreen.class);
            addStep("Открыть первый раздел", () -> {
                Screen current = Minecraft.getInstance().screen;
                if (current instanceof MainMenuScreen menuScreen) {
                    if (!menuScreen.openCard(0)) {
                        failures.add("Главное меню -> не удалось открыть первую карточку");
                    }
                } else {
                    failures.add("Главное меню -> экран не открыт");
                }
            }, CommandMenuScreen.class);
            addStep("Открыть детали первой команды", () -> {
                Screen current = Minecraft.getInstance().screen;
                if (current instanceof CommandMenuScreen commandMenu) {
                    if (!commandMenu.openFirstCardDetails()) {
                        failures.add("Команды -> не удалось открыть первую карточку");
                    }
                } else {
                    failures.add("Команды -> экран не открыт");
                }
            }, CommandDetailScreen.class);
            addStep("Открыть меню команд", () -> setScreen(menu), CommandMenuScreen.class);
            addStep("Открыть дерево технологий", () -> setScreen(new TechnologyTreeScreen(menu)), TechnologyTreeScreen.class);
            addStep("Открыть статистику", () -> setScreen(new StatsOverlayScreen(menu)), StatsOverlayScreen.class);
            addStep("Открыть карту", () -> setScreen(new NationMapScreen(menu)), NationMapScreen.class);
            addStepWithDelay("Подождать данные карты", () -> {}, NationMapScreen.class, 80);
            addStep("Проверить карту", () -> {
                Screen current = Minecraft.getInstance().screen;
                if (current instanceof NationMapScreen map) {
                    if (!AxiomUiMod.hasNationSnapshot() && !AxiomUiMod.hasTerritorySnapshot()) {
                        return;
                    }
                    if (!map.hasData()) {
                        failures.add("Карта -> нет данных о нациях");
                    }
                } else {
                    failures.add("Карта -> экран не открыт");
                }
            }, NationMapScreen.class);
            addStepWithDelay("Подождать данные территорий", () -> {}, NationMapScreen.class, 80);
            addStep("Проверить синхронизацию территорий", () -> {
                if (!AxiomUiMod.hasTerritorySnapshot()) {
                    return;
                }
                if (AxiomUiMod.getTerritoryVersion() < 0) {
                    failures.add("Карта -> нет версии снапшота территорий");
                }
            }, NationMapScreen.class);
            addStep("Открыть образование", () -> setScreen(new EducationScreen(menu)), EducationScreen.class);
            addStep("Открыть культуру", () -> setScreen(new CultureScreen(menu)), CultureScreen.class);
            addStep("Открыть экологию", () -> setScreen(new EcologyScreen(menu)), EcologyScreen.class);
            addStep("Открыть шпионаж", () -> setScreen(new EspionageScreen(menu)), EspionageScreen.class);
            addStep("Открыть аналитику", () -> setScreen(new AnalyticsScreen(menu)), AnalyticsScreen.class);
            addStep("Открыть баланс", () -> setScreen(new ItemBalanceScreen(menu)), ItemBalanceScreen.class);
            addStep("Открыть крафты", () -> setScreen(new RecipeEditorScreen(menu)), RecipeEditorScreen.class);
            addStep("Открыть религии", () -> setScreen(new ReligionCardsScreen()), ReligionCardsScreen.class);

            for (CommandInfo cmd : CommandCatalog.getAllCommands()) {
                addStep("Команда: детали " + cmd.getCommand(),
                    () -> setScreen(new CommandDetailScreen(menu, cmd)),
                    CommandDetailScreen.class);
                addStep("Команда: ввод " + cmd.getCommand(),
                    () -> setScreen(new CommandInputScreen(menu, cmd)),
                    CommandInputScreen.class);
            }

            addStep("Вернуться в меню команд", () -> setScreen(menu), CommandMenuScreen.class);
        }

        for (String command : config.commands) {
            String cleaned = command == null ? "" : command.trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            if (isCommandBlocked(cleaned)) {
                continue;
            }
            addCommandStep("Команда: " + cleaned, cleaned);
        }
    }

    private void addStep(String name, Runnable action, Class<? extends Screen> expectedScreen) {
        steps.add(new UiStep(name, action, expectedScreen, null, 0));
    }

    private void addCommandStep(String name, String command) {
        steps.add(new UiStep(name, () -> {}, null, command, 0));
    }

    private void addStepWithDelay(String name, Runnable action, Class<? extends Screen> expectedScreen, int delayTicks) {
        steps.add(new UiStep(name, action, expectedScreen, null, delayTicks));
    }

    private void addCommandStepWithDelay(String name, String command, int delayTicks) {
        steps.add(new UiStep(name, () -> {}, null, command, delayTicks));
    }

    private int resolveDelayTicks(UiStep step) {
        if (step.delayTicks > 0) {
            return step.delayTicks;
        }
        return config.stepDelayTicks;
    }

    private void setScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    private boolean isCommandBlocked(String command) {
        if (config.commandBlacklist == null || config.commandBlacklist.isEmpty()) {
            return false;
        }
        String normalized = command.trim();
        for (String blocked : config.commandBlacklist) {
            if (blocked == null) continue;
            String entry = blocked.trim();
            if (entry.isEmpty()) continue;
            if (entry.endsWith("*")) {
                String prefix = entry.substring(0, entry.length() - 1);
                if (normalized.startsWith(prefix)) {
                    return true;
                }
            } else if (normalized.equalsIgnoreCase(entry)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCommandNoAck(String command) {
        if (command == null) {
            return false;
        }
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("/stop") || normalized.equals("stop");
    }

    private void buildActionSteps() {
        for (BotAction action : config.actions) {
            if (action == null) {
                continue;
            }
            String type = normalize(action.type);
            int delayTicks = action.delayTicks != null ? action.delayTicks : 0;
            String name = action.name != null && !action.name.isBlank()
                ? action.name
                : type + (action.value == null ? "" : (": " + action.value));

            switch (type) {
                case "open_screen": {
                    String id = action.value == null ? "" : action.value.trim();
                    addStepWithDelay(name, () -> {
                        Screen screen = createScreen(id);
                        if (screen != null) {
                            setScreen(screen);
                        } else {
                            failures.add(name + " -> неизвестный экран: " + id);
                        }
                    }, resolveScreenClass(action.expect), delayTicks);
                    break;
                }
                case "close_screen": {
                    addStepWithDelay(name, () -> {
                        Screen current = Minecraft.getInstance().screen;
                        if (current != null) {
                            current.onClose();
                        }
                    }, resolveScreenClass(action.expect), delayTicks);
                    break;
                }
                case "click_button": {
                    String label = action.value == null ? "" : action.value.trim();
                    addStepWithDelay(name, () -> {
                        if (!clickButton(label)) {
                            failures.add(name + " -> кнопка не найдена: " + label);
                        }
                    }, resolveScreenClass(action.expect), delayTicks);
                    break;
                }
                case "command": {
                    if (action.value != null && !action.value.trim().isEmpty()) {
                        addCommandStepWithDelay(name, action.value.trim(), delayTicks);
                    }
                    break;
                }
                case "wait": {
                    int waitTicks = parseWaitTicks(action.value, delayTicks);
                    addStepWithDelay(name, () -> {}, null, waitTicks);
                    break;
                }
                default:
                    failures.add("Неизвестный тип шага: " + type);
            }
        }
    }

    private int parseWaitTicks(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return Math.max(1, fallback);
        }
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return Math.max(1, fallback);
        }
    }

    private Screen createScreen(String id) {
        String key = normalize(id);
        Screen parent = Minecraft.getInstance().screen;
        switch (key) {
            case "main":
            case "main_menu":
            case "home":
                return new MainMenuScreen();
            case "command_menu":
            case "commands":
            case "menu":
                return new CommandMenuScreen();
            case "tech":
            case "tech_tree":
                return new TechnologyTreeScreen(parent);
            case "stats":
                return new StatsOverlayScreen(parent);
            case "map":
                return new NationMapScreen(parent);
            case "education":
                return new EducationScreen(parent);
            case "culture":
                return new CultureScreen(parent);
            case "ecology":
                return new EcologyScreen(parent);
            case "espionage":
                return new EspionageScreen(parent);
            case "analytics":
                return new AnalyticsScreen(parent);
            case "balance":
                return new ItemBalanceScreen(parent);
            case "recipes":
                return new RecipeEditorScreen(parent);
            case "religions":
                return new ReligionCardsScreen();
            default:
                return null;
        }
    }

    private Class<? extends Screen> resolveScreenClass(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        String key = normalize(id);
        switch (key) {
            case "main":
            case "main_menu":
            case "home":
                return MainMenuScreen.class;
            case "command_menu":
            case "commands":
            case "menu":
                return CommandMenuScreen.class;
            case "tech":
            case "tech_tree":
                return TechnologyTreeScreen.class;
            case "stats":
                return StatsOverlayScreen.class;
            case "map":
                return NationMapScreen.class;
            case "education":
                return EducationScreen.class;
            case "culture":
                return CultureScreen.class;
            case "ecology":
                return EcologyScreen.class;
            case "espionage":
                return EspionageScreen.class;
            case "analytics":
                return AnalyticsScreen.class;
            case "balance":
                return ItemBalanceScreen.class;
            case "recipes":
                return RecipeEditorScreen.class;
            case "religions":
                return ReligionCardsScreen.class;
            default:
                return null;
        }
    }

    private boolean clickButton(String label) {
        Screen current = Minecraft.getInstance().screen;
        if (current == null) {
            return false;
        }
        String needle = normalize(label);
        if (needle.isEmpty()) {
            return false;
        }
        for (var child : current.children()) {
            if (child instanceof Button button) {
                String text = normalize(button.getMessage().getString());
                if (text.contains(needle)) {
                    button.onPress();
                    return true;
                }
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void loadConfig() {
        TestConfig loaded = null;
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                loaded = GSON.fromJson(reader, TestConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (loaded == null) {
            loaded = TestConfig.defaults();
            writeConfig(loaded);
        } else {
            loaded.applyDefaults();
        }
        config = loaded;
    }

    private void writeConfig(TestConfig data) {
        try {
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final class UiStep {
        private final String name;
        private final Runnable action;
        private final Class<? extends Screen> expectedScreen;
        private final String command;
        private final int delayTicks;

        private UiStep(String name, Runnable action, Class<? extends Screen> expectedScreen, String command, int delayTicks) {
            this.name = name;
            this.action = action;
            this.expectedScreen = expectedScreen;
            this.command = command;
            this.delayTicks = delayTicks;
        }
    }

    private static final class PendingCommand {
        private final String stepName;
        private final String command;
        private int remainingTicks;
        private final int postDelayTicks;
        private boolean completed;
        private boolean success;
        private String message;

        private PendingCommand(String stepName, String command, int timeoutTicks, int postDelayTicks) {
            this.stepName = stepName;
            this.command = command;
            this.remainingTicks = timeoutTicks;
            this.postDelayTicks = Math.max(1, postDelayTicks);
        }
    }

    private static final class TestConfig {
        boolean enabled = false;
        int autoStartDelayTicks = 60;
        int stepDelayTicks = 5;
        int commandTimeoutTicks = 200;
        boolean includeUiScreens = true;
        List<String> commands = new ArrayList<>();
        List<String> commandBlacklist = new ArrayList<>();
        List<BotAction> actions = new ArrayList<>();

        static TestConfig defaults() {
            TestConfig config = new TestConfig();
            config.commands = new ArrayList<>(List.of("/testbot run all"));
            config.commandBlacklist = new ArrayList<>();
            return config;
        }

        void applyDefaults() {
            if (autoStartDelayTicks < 0) autoStartDelayTicks = 0;
            if (stepDelayTicks < 1) stepDelayTicks = 1;
            if (commandTimeoutTicks < 1) commandTimeoutTicks = 1;
            if (commands == null || commands.isEmpty()) {
                commands = new ArrayList<>(List.of("/testbot run all"));
            }
            if (commandBlacklist == null) {
                commandBlacklist = new ArrayList<>();
            }
            if (actions == null) {
                actions = new ArrayList<>();
            }
        }
    }

    private static final class BotAction {
        String type;
        String value;
        String name;
        String expect;
        Integer delayTicks;
    }
}
