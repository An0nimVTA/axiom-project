package com.axiom.ui;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import io.netty.buffer.Unpooled;

@Mod(AxiomUiMod.MOD_ID)
public class AxiomUiMod {
    public static final String MOD_ID = "axiomui";
    private static final String UI_PROTOCOL = "1";
    public static final ResourceLocation UI_CHANNEL = ResourceLocation.fromNamespaceAndPath("axiom", "ui");
    public static final EventNetworkChannel UI_NETWORK = NetworkRegistry.newEventChannel(
        UI_CHANNEL,
        () -> UI_PROTOCOL,
        NetworkRegistry.acceptMissingOr(UI_PROTOCOL),
        NetworkRegistry.acceptMissingOr(UI_PROTOCOL)
    );
    
    public static Map<String, Object> cachedStats = new HashMap<>();
    public static List<Map<String, Object>> cachedTechs = new ArrayList<>();
    public static List<Map<String, Object>> cachedNations = new ArrayList<>();
    public static List<Map<String, Object>> cachedTerritories = new ArrayList<>();
    private static final Map<String, TerritoryTile> territoryIndex = new HashMap<>();
    private static List<TerritoryTile> territoryList = new ArrayList<>();
    private static boolean territoryListDirty = true;
    private static long cachedTerritoryVersion = -1L;
    private static long cachedTerritoryRevision = 0L;
    private static boolean loggedTerritorySync = false;
    private static boolean nationsSynced = false;
    private static boolean territoriesSynced = false;

    public AxiomUiMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            UI_NETWORK.addListener(AxiomUiClientEvents::onClientPayload);
        }
        
        System.out.println("[AXIOM UI] Mod loaded (Side: " + FMLEnvironment.dist + ")");
    }
    
    public static void requestUpdate(String type) {
        // Safe check for client side usage
        if (FMLEnvironment.dist == Dist.CLIENT) {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf("get_" + type);
            player.connection.send(NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf, 0), UI_CHANNEL).getThis());
        }
    }

    public static void applyTerritorySnapshot(List<Map<String, Object>> territories, long version) {
        territoryIndex.clear();
        if (territories != null) {
            for (Map<String, Object> entry : territories) {
                if (entry == null) continue;
                String world = readString(entry.get("world"));
                String nationId = readString(entry.get("nationId"));
                int x = readInt(entry.get("x"), 0);
                int z = readInt(entry.get("z"), 0);
                if (world == null || world.isBlank()) continue;
                territoryIndex.put(chunkKey(world, x, z), new TerritoryTile(world, x, z, nationId));
            }
        }
        cachedTerritoryVersion = version;
        territoriesSynced = true;
        cachedTerritoryRevision++;
        territoryListDirty = true;
        cachedTerritories = territories != null ? territories : new ArrayList<>();
        if (!loggedTerritorySync) {
            System.out.println("[AXIOM UI] territories snapshot received: " + territoryIndex.size() + " chunks, v" + version);
            loggedTerritorySync = true;
        }
    }

    public static void applyTerritoryDelta(List<Map<String, Object>> changes, long version) {
        if (changes == null || changes.isEmpty()) {
            cachedTerritoryVersion = version;
            territoriesSynced = true;
            return;
        }
        int applied = 0;
        for (Map<String, Object> entry : changes) {
            if (entry == null) continue;
            String op = readString(entry.get("op"));
            String world = readString(entry.get("world"));
            int x = readInt(entry.get("x"), 0);
            int z = readInt(entry.get("z"), 0);
            String nationId = readString(entry.get("nationId"));
            if (world == null || world.isBlank()) continue;
            String key = chunkKey(world, x, z);
            if ("claim".equalsIgnoreCase(op)) {
                territoryIndex.put(key, new TerritoryTile(world, x, z, nationId));
                applied++;
            } else if ("unclaim".equalsIgnoreCase(op)) {
                territoryIndex.remove(key);
                applied++;
            }
        }
        cachedTerritoryVersion = version;
        territoriesSynced = true;
        if (applied > 0) {
            cachedTerritoryRevision++;
            territoryListDirty = true;
            if (loggedTerritorySync) {
                System.out.println("[AXIOM UI] territories delta applied: " + applied + " changes, v" + version);
            }
        }
    }

    public static List<TerritoryTile> getTerritoryTiles() {
        if (territoryListDirty) {
            territoryList = new ArrayList<>(territoryIndex.values());
            territoryListDirty = false;
        }
        return territoryList;
    }

    public static String getTerritoryOwner(String world, int x, int z) {
        if (world == null) {
            return null;
        }
        TerritoryTile tile = territoryIndex.get(chunkKey(world, x, z));
        return tile != null ? tile.nationId : null;
    }

    public static long getTerritoryVersion() {
        return cachedTerritoryVersion;
    }

    public static long getTerritoryRevision() {
        return cachedTerritoryRevision;
    }

    public static void markNationsSynced() {
        nationsSynced = true;
    }

    public static boolean hasNationSnapshot() {
        return nationsSynced;
    }

    public static boolean hasTerritorySnapshot() {
        return territoriesSynced;
    }

    private static String readString(Object value) {
        if (value == null) return null;
        String s = value.toString();
        return s.isBlank() ? null : s;
    }

    private static int readInt(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String chunkKey(String world, int x, int z) {
        return world + ":" + x + ":" + z;
    }

    public static final class TerritoryTile {
        public final String world;
        public final int x;
        public final int z;
        public final String nationId;

        public TerritoryTile(String world, int x, int z, String nationId) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.nationId = nationId;
        }
    }
}
