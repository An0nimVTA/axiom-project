package com.axiom.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.MCRegisterPacketHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.Unpooled;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = AxiomUiMod.MOD_ID, value = Dist.CLIENT)
public class AxiomUiClientEvents {

    private static final Gson gson = new Gson();
    private static final UiSmokeTestRunner UI_TEST_RUNNER = new UiSmokeTestRunner();
    private static Connection pendingConnection;
    private static int pendingInitTicks = -1;
    private static int territoryDeltaCooldown = 0;
    private static final KeyMapping OPEN_MENU_KEY = new KeyMapping(
        "key.axiomui.open_menu",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_I,
        "key.categories.axiomui"
    );

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("axiomui")
                .executes(context -> {
                    MainMenuScreen.open();
                    return 1;
                })
                .then(Commands.literal("commands")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new CommandMenuScreen());
                        return 1;
                    })
                )
                .then(Commands.literal("menu")
                    .executes(context -> {
                        MainMenuScreen.open();
                        return 1;
                    })
                )
                .then(Commands.literal("religions")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new ReligionCardsScreen());
                        return 1;
                    })
                )
                .then(Commands.literal("tech")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new TechnologyTreeScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("stats")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new StatsOverlayScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("map")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new NationMapScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("education")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new EducationScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("culture")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new CultureScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("ecology")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new EcologyScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("espionage")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new EspionageScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("analytics")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new AnalyticsScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("balance")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new ItemBalanceScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("recipes")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new RecipeEditorScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("language")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new LanguageSelectScreen(null));
                        return 1;
                    })
                )
                .then(Commands.literal("test")
                    .executes(context -> {
                        NotificationManager.getInstance().info("Тестовое уведомление INFO");
                        NotificationManager.getInstance().success("Тестовое уведомление SUCCESS");
                        NotificationManager.getInstance().warning("Тестовое уведомление WARNING");
                        NotificationManager.getInstance().error("Тестовое уведомление ERROR");
                        return 1;
                    })
                    .then(Commands.literal("run")
                        .executes(context -> {
                            UI_TEST_RUNNER.start();
                            return 1;
                        })
                    )
                    .then(Commands.literal("stop")
                        .executes(context -> {
                            UI_TEST_RUNNER.stop();
                            return 1;
                        })
                    )
                )
        );
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU_KEY);
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Connection connection = event.getConnection();
        if (connection == null) return;
        
        MCRegisterPacketHandler.INSTANCE.addChannels(Set.of(AxiomUiMod.UI_CHANNEL), connection);
        MCRegisterPacketHandler.INSTANCE.sendRegistry(connection, NetworkDirection.PLAY_TO_SERVER);

        pendingConnection = connection;
        pendingInitTicks = 20;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (OPEN_MENU_KEY.consumeClick()) {
            MainMenuScreen.open();
        }
        UI_TEST_RUNNER.tick();

        if (pendingInitTicks >= 0) {
            if (pendingInitTicks > 0) {
                pendingInitTicks--;
                return;
            }
            Connection connection = pendingConnection;
            if (connection != null) {
                sendToServer(connection, "get_stats");
                sendToServer(connection, "get_techs");
                sendToServer(connection, "get_nations");
                sendToServer(connection, "get_territories_snapshot");
                sendToServer(connection, "get_language");
            }
            UI_TEST_RUNNER.scheduleAutoStart();
            pendingInitTicks = -1;
            pendingConnection = null;
        }

        if (territoryDeltaCooldown > 0) {
            territoryDeltaCooldown--;
        }
        boolean mapOpen = Minecraft.getInstance().screen instanceof NationMapScreen;
        if (mapOpen && territoryDeltaCooldown == 0) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                if (AxiomUiMod.getTerritoryVersion() < 0) {
                    sendToServer(player.connection, "get_territories_snapshot");
                }
                territoryDeltaCooldown = 40;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(net.minecraftforge.client.event.RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().toString().equals("minecraft:hotbar")) {
            NotificationManager.getInstance().render(
                event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight()
            );
            UI_TEST_RUNNER.renderOverlay(
                event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight()
            );
        }
    }
    
    public static void onClientPayload(NetworkEvent.ClientCustomPayloadEvent event) {
        FriendlyByteBuf buf = event.getPayload();
        if (buf == null || buf.readableBytes() == 0) {
            event.getSource().get().setPacketHandled(true);
            return;
        }
        
        String type = buf.readUtf(64);
        String json = buf.readUtf(32767);
        
        event.getSource().get().enqueueWork(() -> {
            switch (type) {
                case "stats":
                    AxiomUiMod.cachedStats = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                    break;
                case "techs":
                    AxiomUiMod.cachedTechs = gson.fromJson(json, new TypeToken<List<Map<String, Object>>>(){}.getType());
                    break;
                case "nations":
                    AxiomUiMod.cachedNations = gson.fromJson(json, new TypeToken<List<Map<String, Object>>>(){}.getType());
                    AxiomUiMod.markNationsSynced();
                    break;
                case "territories_snapshot": {
                    Map<String, Object> payload = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                    long version = readLong(payload.get("version"), -1L);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> territories = (List<Map<String, Object>>) payload.get("territories");
                    AxiomUiMod.applyTerritorySnapshot(territories, version);
                    break;
                }
                case "territories_delta": {
                    Map<String, Object> payload = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                    long version = readLong(payload.get("version"), -1L);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> changes = (List<Map<String, Object>>) payload.get("changes");
                    AxiomUiMod.applyTerritoryDelta(changes, version);
                    break;
                }
                case "command_result":
                    Map<String, Object> result = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                    String cmd = result.getOrDefault("command", "").toString();
                    boolean success = Boolean.parseBoolean(result.getOrDefault("success", "false").toString());
                    String message = result.getOrDefault("message", "").toString();
                    UI_TEST_RUNNER.onCommandResult(cmd, success, message);
                    break;
                case "language":
                    String code = gson.fromJson(json, String.class);
                    UiText.setLanguageCode(code, true);
                    break;
                case "open_main_menu":
                    MainMenuScreen.open();
                    break;
                case "ui_autotest_start":
                    UI_TEST_RUNNER.startWithOverridesJson(json);
                    break;
            }
        });
        event.getSource().get().setPacketHandled(true);
    }

    private static void sendToServer(Connection connection, String action) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(action);
        connection.send(NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf, 0), AxiomUiMod.UI_CHANNEL).getThis());
    }

    private static void sendToServer(Connection connection, String action, long value) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(action);
        buf.writeLong(value);
        connection.send(NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf, 0), AxiomUiMod.UI_CHANNEL).getThis());
    }

    private static void sendToServer(ClientPacketListener connection, String action) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(action);
        connection.send(NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf, 0), AxiomUiMod.UI_CHANNEL).getThis());
    }

    private static void sendToServer(ClientPacketListener connection, String action, long value) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(action);
        buf.writeLong(value);
        connection.send(NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf, 0), AxiomUiMod.UI_CHANNEL).getThis());
    }

    private static void sendTerritoryDeltaRequest(long sinceVersion) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        sendToServer(player.connection, "get_territories_delta", sinceVersion);
    }

    private static long readLong(Object value, long fallback) {
        if (value == null) return fallback;
        if (value instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static void sendCommandToServer(String command) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("execute_command");
        buf.writeUtf(command);
        player.connection.send(NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf, 0), AxiomUiMod.UI_CHANNEL).getThis());
    }

    public static void sendLanguageToServer(String languageCode) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("set_language");
        buf.writeUtf(languageCode == null ? "" : languageCode);
        player.connection.send(NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf, 0), AxiomUiMod.UI_CHANNEL).getThis());
    }
}
