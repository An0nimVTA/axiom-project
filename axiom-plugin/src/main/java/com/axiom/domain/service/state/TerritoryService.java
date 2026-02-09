package com.axiom.domain.service.state;

import com.axiom.domain.model.ChunkPos;
import com.axiom.domain.model.Nation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Canonical territory model based on chunk squares (world:x:z).
 * Provides fast lookup and sync utilities for map rendering and exports.
 */
public class TerritoryService {
    private static final int MAX_CHANGE_LOG = 10000;
    private final Logger logger;
    private final NationManager nationManager;
    private final File storageFile;
    private final Gson gson;
    private boolean dirty;
    private long version;
    private final Deque<TerritoryChange> changeLog = new ArrayDeque<>();

    private final Map<String, Set<ChunkPos>> claimsByNation = new HashMap<>();
    private final Map<String, Map<ChunkPos, String>> claimsByWorld = new HashMap<>();
    private final Map<ChunkPos, String> ownerIndex = new HashMap<>();

    public TerritoryService(Logger logger, NationManager nationManager) {
        this(logger, nationManager, null);
    }

    public TerritoryService(Logger logger, NationManager nationManager, File storageFile) {
        this.logger = logger != null ? logger : Logger.getLogger(TerritoryService.class.getName());
        this.nationManager = nationManager;
        this.storageFile = storageFile;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadOrRebuild();
    }

    public TerritoryService(com.axiom.AXIOM plugin, NationManager nationManager) {
        this(
            plugin != null ? plugin.getLogger() : Logger.getLogger(TerritoryService.class.getName()),
            nationManager,
            plugin != null ? new File(plugin.getDataFolder(), "territories.json") : null
        );
    }

    public synchronized void loadOrRebuild() {
        boolean loaded = loadFromDisk();
        if (loaded) {
            if (ownerIndex.isEmpty() && hasNationClaims()) {
                rebuildFromNations();
                saveIfNeeded();
                return;
            }
            boolean cleaned = cleanupInvalidEntries();
            syncNationsFromTerritory();
            if (cleaned) {
                saveIfNeeded();
            }
            return;
        }
        rebuildFromNations();
        saveIfNeeded();
    }

    public synchronized void rebuildFromNations() {
        claimsByNation.clear();
        claimsByWorld.clear();
        ownerIndex.clear();
        changeLog.clear();
        version = 0;

        if (nationManager == null) {
            return;
        }

        for (Nation nation : nationManager.getAll()) {
            String nationId = nation.getId();
            if (nationId == null) {
                continue;
            }
            Set<String> keys = nation.getClaimedChunkKeys();
            if (keys == null) {
                continue;
            }
            for (String key : keys) {
                ChunkPos pos = parseChunkKey(key);
                if (pos == null) {
                    continue;
                }
                addClaimIfFree(nationId, pos);
            }
        }
        markDirty();
    }

    private boolean hasNationClaims() {
        if (nationManager == null) {
            return false;
        }
        for (Nation nation : nationManager.getAll()) {
            if (nation != null && nation.getClaimedChunkKeys() != null && !nation.getClaimedChunkKeys().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public synchronized void claim(String nationId, String world, int x, int z) {
        if (isBlank(nationId) || isBlank(world)) {
            return;
        }
        ChunkPos pos = new ChunkPos(world, x, z);
        String previous = ownerIndex.get(pos);
        if (nationId.equals(previous)) {
            return;
        }
        addClaimInternal(nationId, pos);
        recordChange("claim", pos, nationId);
        markDirty();
        saveIfNeeded();
    }

    public synchronized void unclaim(String nationId, String world, int x, int z) {
        if (isBlank(nationId) || isBlank(world)) {
            return;
        }
        ChunkPos pos = new ChunkPos(world, x, z);
        String currentOwner = ownerIndex.get(pos);
        if (currentOwner != null && !currentOwner.equals(nationId)) {
            // Clean up stale entries for the nation without removing the actual owner.
            removeNationClaim(nationId, pos);
            removeWorldClaimIfOwner(world, pos, nationId);
            return;
        }
        if (currentOwner == null) {
            return;
        }
        ownerIndex.remove(pos);

        removeNationClaim(nationId, pos);

        removeWorldClaimIfOwner(world, pos, nationId);
        recordChange("unclaim", pos, nationId);
        markDirty();
        saveIfNeeded();
    }

    public synchronized String getNationAt(String world, int x, int z) {
        return ownerIndex.get(new ChunkPos(world, x, z));
    }

    public synchronized Set<ChunkPos> getNationClaims(String nationId) {
        Set<ChunkPos> claims = claimsByNation.get(nationId);
        return claims == null ? Collections.emptySet() : new HashSet<>(claims);
    }

    public synchronized Map<String, Set<ChunkPos>> getWorldClaims(String world) {
        Map<ChunkPos, String> worldClaims = claimsByWorld.get(world);
        if (worldClaims == null) {
            return Collections.emptyMap();
        }
        Map<String, Set<ChunkPos>> result = new HashMap<>();
        for (Map.Entry<ChunkPos, String> entry : worldClaims.entrySet()) {
            result.computeIfAbsent(entry.getValue(), key -> new HashSet<>()).add(entry.getKey());
        }
        return result;
    }

    public synchronized List<TerritorySquare> getAllSquares() {
        List<TerritorySquare> squares = new ArrayList<>(ownerIndex.size());
        for (Map.Entry<ChunkPos, String> entry : ownerIndex.entrySet()) {
            ChunkPos pos = entry.getKey();
            squares.add(new TerritorySquare(pos.getWorld(), pos.getX(), pos.getZ(), entry.getValue()));
        }
        return squares;
    }

    public synchronized int getTotalClaimedChunks() {
        return ownerIndex.size();
    }

    public synchronized long getVersion() {
        return version;
    }

    public synchronized DeltaResult getDeltaSince(long sinceVersion) {
        if (sinceVersion < 0) {
            return new DeltaResult(true, version, Collections.emptyList());
        }
        if (sinceVersion > version) {
            return new DeltaResult(true, version, Collections.emptyList());
        }
        if (changeLog.isEmpty()) {
            return new DeltaResult(false, version, Collections.emptyList());
        }
        long oldest = changeLog.peekFirst().version;
        if (sinceVersion + 1 < oldest) {
            return new DeltaResult(true, version, Collections.emptyList());
        }
        List<TerritoryChange> changes = new ArrayList<>();
        for (TerritoryChange change : changeLog) {
            if (change.version > sinceVersion) {
                changes.add(change);
            }
        }
        return new DeltaResult(false, version, changes);
    }

    private void addClaimInternal(String nationId, ChunkPos pos) {
        String previousOwner = ownerIndex.put(pos, nationId);
        if (previousOwner != null && !previousOwner.equals(nationId)) {
            removeNationClaim(previousOwner, pos);
        }
        claimsByNation.computeIfAbsent(nationId, key -> new HashSet<>()).add(pos);
        claimsByWorld.computeIfAbsent(pos.getWorld(), key -> new HashMap<>()).put(pos, nationId);
    }

    private void recordChange(String op, ChunkPos pos, String nationId) {
        if (pos == null) {
            return;
        }
        version++;
        changeLog.addLast(new TerritoryChange(version, op, pos.getWorld(), pos.getX(), pos.getZ(), nationId));
        while (changeLog.size() > MAX_CHANGE_LOG) {
            changeLog.removeFirst();
        }
    }

    private void addClaimIfFree(String nationId, ChunkPos pos) {
        String existing = ownerIndex.get(pos);
        if (existing != null && !existing.equals(nationId)) {
            logger.warning("Duplicate claim detected, keeping " + existing + " for " + pos.getWorld() + ":" + pos.getX() + ":" + pos.getZ());
            return;
        }
        addClaimInternal(nationId, pos);
    }

    private ChunkPos parseChunkKey(String key) {
        if (isBlank(key)) {
            return null;
        }
        String[] parts = key.split(":");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new ChunkPos(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ex) {
            logger.fine("Invalid chunk key: " + key);
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void markDirty() {
        if (storageFile == null) {
            return;
        }
        dirty = true;
    }

    private void saveIfNeeded() {
        if (!dirty || storageFile == null) {
            return;
        }
        save();
    }

    public synchronized void save() {
        if (storageFile == null) {
            return;
        }
        try {
            File parent = storageFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            List<StoredTerritory> stored = new ArrayList<>();
            for (TerritorySquare square : getAllSquares()) {
                stored.add(new StoredTerritory(square.world, square.x, square.z, square.nationId));
            }
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(storageFile.toPath()), StandardCharsets.UTF_8)) {
                gson.toJson(stored, writer);
            }
            dirty = false;
        } catch (Exception ex) {
            logger.warning("Failed to save territories: " + ex.getMessage());
        }
    }

    private boolean loadFromDisk() {
        if (storageFile == null || !storageFile.exists()) {
            return false;
        }
        try (Reader reader = new InputStreamReader(Files.newInputStream(storageFile.toPath()), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<StoredTerritory>>() {}.getType();
            List<StoredTerritory> stored = gson.fromJson(reader, listType);
            if (stored == null) {
                return false;
            }
            claimsByNation.clear();
            claimsByWorld.clear();
            ownerIndex.clear();
            for (StoredTerritory square : stored) {
                if (square == null || isBlank(square.world) || isBlank(square.nationId)) {
                    continue;
                }
                addClaimIfFree(square.nationId, new ChunkPos(square.world, square.x, square.z));
            }
            changeLog.clear();
            version = 0;
            dirty = false;
            return true;
        } catch (Exception ex) {
            logger.warning("Failed to load territories: " + ex.getMessage());
            return false;
        }
    }

    private boolean cleanupInvalidEntries() {
        boolean changed = false;
        var iterator = ownerIndex.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChunkPos, String> entry = iterator.next();
            ChunkPos pos = entry.getKey();
            String nationId = entry.getValue();
            if (pos == null || isBlank(pos.getWorld()) || isBlank(nationId)) {
                iterator.remove();
                changed = true;
                continue;
            }
            if (nationManager != null && nationManager.getNationById(nationId) == null) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            rebuildIndexesFromOwnerIndex();
            markDirty();
        }
        return changed;
    }

    private void syncNationsFromTerritory() {
        if (nationManager == null) {
            return;
        }
        Map<String, Set<String>> byNation = new HashMap<>();
        for (Map.Entry<ChunkPos, String> entry : ownerIndex.entrySet()) {
            ChunkPos pos = entry.getKey();
            String nationId = entry.getValue();
            if (pos == null || isBlank(nationId)) {
                continue;
            }
            String key = pos.getWorld() + ":" + pos.getX() + ":" + pos.getZ();
            byNation.computeIfAbsent(nationId, k -> new HashSet<>()).add(key);
        }
        for (Nation nation : nationManager.getAll()) {
            if (nation == null || nation.getId() == null) {
                continue;
            }
            Set<String> target = byNation.getOrDefault(nation.getId(), Collections.emptySet());
            Set<String> current = nation.getClaimedChunkKeys();
            boolean changed = current.size() != target.size() || !current.containsAll(target);
            if (changed) {
                current.clear();
                current.addAll(target);
            }
            String capital = nation.getCapitalChunkStr();
            if (capital != null && !current.contains(capital)) {
                nation.setCapitalChunkStr(current.isEmpty() ? null : current.iterator().next());
                changed = true;
            } else if (capital == null && !current.isEmpty()) {
                nation.setCapitalChunkStr(current.iterator().next());
                changed = true;
            }
            if (changed) {
                try {
                    nationManager.save(nation);
                } catch (Exception ex) {
                    logger.warning("Failed to sync nation territory for " + nation.getId() + ": " + ex.getMessage());
                }
            }
        }
    }

    private void rebuildIndexesFromOwnerIndex() {
        claimsByNation.clear();
        claimsByWorld.clear();
        for (Map.Entry<ChunkPos, String> entry : ownerIndex.entrySet()) {
            ChunkPos pos = entry.getKey();
            String nationId = entry.getValue();
            claimsByNation.computeIfAbsent(nationId, key -> new HashSet<>()).add(pos);
            claimsByWorld.computeIfAbsent(pos.getWorld(), key -> new HashMap<>()).put(pos, nationId);
        }
    }

    private void removeNationClaim(String nationId, ChunkPos pos) {
        Set<ChunkPos> nationClaims = claimsByNation.get(nationId);
        if (nationClaims != null) {
            nationClaims.remove(pos);
            if (nationClaims.isEmpty()) {
                claimsByNation.remove(nationId);
            }
        }
    }

    private void removeWorldClaimIfOwner(String world, ChunkPos pos, String nationId) {
        Map<ChunkPos, String> worldClaims = claimsByWorld.get(world);
        if (worldClaims != null) {
            String owner = worldClaims.get(pos);
            if (owner == null || owner.equals(nationId)) {
                worldClaims.remove(pos);
                if (worldClaims.isEmpty()) {
                    claimsByWorld.remove(world);
                }
            }
        }
    }

    public static final class DeltaResult {
        private final boolean requiresSnapshot;
        private final long version;
        private final List<TerritoryChange> changes;

        public DeltaResult(boolean requiresSnapshot, long version, List<TerritoryChange> changes) {
            this.requiresSnapshot = requiresSnapshot;
            this.version = version;
            this.changes = changes;
        }

        public boolean requiresSnapshot() { return requiresSnapshot; }
        public long getVersion() { return version; }
        public List<TerritoryChange> getChanges() { return changes; }
    }

    public static final class TerritoryChange {
        private final long version;
        private final String op;
        private final String world;
        private final int x;
        private final int z;
        private final String nationId;

        public TerritoryChange(long version, String op, String world, int x, int z, String nationId) {
            this.version = version;
            this.op = op;
            this.world = world;
            this.x = x;
            this.z = z;
            this.nationId = nationId;
        }

        public long getVersion() { return version; }
        public String getOp() { return op; }
        public String getWorld() { return world; }
        public int getX() { return x; }
        public int getZ() { return z; }
        public String getNationId() { return nationId; }
    }

    private static final class StoredTerritory {
        private String world;
        private int x;
        private int z;
        private String nationId;

        private StoredTerritory() {
        }

        private StoredTerritory(String world, int x, int z, String nationId) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.nationId = nationId;
        }
    }

    public static final class TerritorySquare {
        private final String world;
        private final int x;
        private final int z;
        private final String nationId;

        public TerritorySquare(String world, int x, int z, String nationId) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.nationId = nationId;
        }

        public String getWorld() { return world; }
        public int getX() { return x; }
        public int getZ() { return z; }
        public String getNationId() { return nationId; }
        public String getChunkKey() { return world + ":" + x + ":" + z; }
    }
}
