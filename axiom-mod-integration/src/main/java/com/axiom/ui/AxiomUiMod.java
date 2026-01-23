package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.MCRegisterPacketHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

import io.netty.buffer.Unpooled;

@Mod(AxiomUiMod.MOD_ID)
public class AxiomUiMod {
    public static final String MOD_ID = "axiomui";
    private static final String UI_PROTOCOL = "1";
    private static final ResourceLocation UI_CHANNEL = new ResourceLocation("axiom", "ui");
    private static final EventNetworkChannel UI_NETWORK = NetworkRegistry.newEventChannel(
        UI_CHANNEL,
        () -> UI_PROTOCOL,
        NetworkRegistry.acceptMissingOr(UI_PROTOCOL),
        NetworkRegistry.acceptMissingOr(UI_PROTOCOL)
    );

    public AxiomUiMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onClientLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onKeyInput);
        MinecraftForge.EVENT_BUS.addListener(this::onRenderOverlay);
        UI_NETWORK.addListener(this::onClientPayload);
        
        // Загрузить балансировку
        System.out.println("[AXIOM UI] Загрузка автоматической балансировки...");
    }

    private void onRenderOverlay(net.minecraftforge.client.event.RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().toString().equals("minecraft:hotbar")) {
            NotificationManager.getInstance().render(
                event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight()
            );
        }
    }

    private void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
        // F1 - open main menu
        if (event.getKey() == 290 && event.getAction() == 1) { // F1 pressed
            Minecraft.getInstance().setScreen(new CommandMenuScreen());
        }
    }

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("axiomui")
                .executes(context -> {
                    Minecraft.getInstance().setScreen(new CommandMenuScreen());
                    return 1;
                })
                .then(Commands.literal("commands")
                    .executes(context -> {
                        Minecraft.getInstance().setScreen(new CommandMenuScreen());
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
                .then(Commands.literal("test")
                    .executes(context -> {
                        NotificationManager.getInstance().info("Тестовое уведомление INFO");
                        NotificationManager.getInstance().success("Тестовое уведомление SUCCESS");
                        NotificationManager.getInstance().warning("Тестовое уведомление WARNING");
                        NotificationManager.getInstance().error("Тестовое уведомление ERROR");
                        return 1;
                    })
                )
        );
    }

    private void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Connection connection = event.getConnection();
        if (connection == null) {
            return;
        }
        MCRegisterPacketHandler.INSTANCE.addChannels(Set.of(UI_CHANNEL), connection);
        MCRegisterPacketHandler.INSTANCE.sendRegistry(connection, NetworkDirection.PLAY_TO_SERVER);
        sendToServer(connection, "hello");
    }

    private void onClientPayload(NetworkEvent.ClientCustomPayloadEvent event) {
        FriendlyByteBuf buf = event.getPayload();
        if (buf == null || buf.readableBytes() == 0) {
            event.getSource().get().setPacketHandled(true);
            return;
        }
        String action = buf.readUtf(64);
        event.getSource().get().enqueueWork(() -> {
            if ("open_religions".equalsIgnoreCase(action)) {
                Minecraft.getInstance().setScreen(new ReligionCardsScreen());
            }
        });
        event.getSource().get().setPacketHandled(true);
    }

    private void sendToServer(Connection connection, String action) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(action);
        connection.send(NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf, 0), UI_CHANNEL).getThis());
    }
}
