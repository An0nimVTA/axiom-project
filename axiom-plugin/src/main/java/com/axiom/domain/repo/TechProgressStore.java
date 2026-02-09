package com.axiom.domain.repo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Persists technology progress per nation under plugins/AXIOM/technology.
 */
public class TechProgressStore {
    private final File techDir;
    private final Gson gson;

    public TechProgressStore(File dataFolder, boolean prettyPrint) {
        this.techDir = new File(dataFolder, "technology");
        this.techDir.mkdirs();
        GsonBuilder builder = new GsonBuilder();
        if (prettyPrint) {
            builder.setPrettyPrinting();
        }
        this.gson = builder.create();
    }

    public Set<String> load(String nationId) {
        if (nationId == null || nationId.isBlank()) {
            return new HashSet<>();
        }
        File f = new File(techDir, nationId + ".json");
        if (!f.exists()) {
            return new HashSet<>();
        }
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            return parseTechs(o);
        } catch (Exception ignored) {
            return new HashSet<>();
        }
    }

    public Map<String, Set<String>> loadAll() {
        Map<String, Set<String>> result = new HashMap<>();
        File[] files = techDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) {
            return result;
        }
        for (File f : files) {
            String nationId = f.getName().replace(".json", "");
            result.put(nationId, load(nationId));
        }
        return result;
    }

    public void save(String nationId, Set<String> techs) {
        if (nationId == null || nationId.isBlank()) {
            return;
        }
        File f = new File(techDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("version", 1);
        o.addProperty("updatedAt", System.currentTimeMillis());
        JsonArray arr = new JsonArray();
        if (techs != null) {
            for (String t : techs) {
                arr.add(t);
            }
        }
        o.add("technologies", arr);
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(f.toPath()), StandardCharsets.UTF_8)) {
            gson.toJson(o, w);
        } catch (Exception ignored) {
        }
    }

    private Set<String> parseTechs(JsonObject o) {
        Set<String> techs = new HashSet<>();
        if (o == null) {
            return techs;
        }
        if (o.has("technologies")) {
            JsonArray arr = o.getAsJsonArray("technologies");
            for (var e : arr) {
                if (e != null && e.isJsonPrimitive()) {
                    techs.add(e.getAsString());
                }
            }
        }
        return techs;
    }
}
