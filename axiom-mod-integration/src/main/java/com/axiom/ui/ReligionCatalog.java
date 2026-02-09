package com.axiom.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ReligionCatalog {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation RESOURCE = ResourceLocation.fromNamespaceAndPath(AxiomUiMod.MOD_ID, "religions.json");
    private static List<ReligionCard> cached;

    private ReligionCatalog() {
    }

    public static List<ReligionCard> get() {
        if (cached == null) {
            cached = load();
        }
        return cached;
    }

    private static List<ReligionCard> load() {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(RESOURCE);
        if (resource.isEmpty()) {
            return Collections.emptyList();
        }
        try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, new TypeToken<List<ReligionCard>>() {}.getType());
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
