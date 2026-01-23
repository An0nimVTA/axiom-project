package com.axiom.model;

import java.util.Objects;

/** Immutable chunk coordinate with world name. */
public class ChunkPos {
    private final String world;
    private final int x;
    private final int z;

    public ChunkPos(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getZ() { return z; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkPos)) return false;
        ChunkPos chunkPos = (ChunkPos) o;
        return x == chunkPos.x && z == chunkPos.z && Objects.equals(world, chunkPos.world);
    }

    @Override
    public int hashCode() { return Objects.hash(world, x, z); }
}


