package io.github.heathensoft.jagfw.tiles;

/**
 * Frederik Dahl 2/28/2025
 */
public enum MapSize {

    TINY(64,"Tiny"),        // 64 x 64
    SMALL(128,"Small"),     // 128 x 128
    MEDIUM(256,"Medium"),   // 256 x 256
    LARGE(512,"Large"),     // 512 x 512
    HUGE(1024,"Huge");      // 1024 x 1024

    public static final int CHUNK_SIZE = 16;
    public static final String DESCRIPTOR = "Map Size";
    public static final MapSize[] ALL = values();
    public static final int COUNT = ALL.length;

    public final String descriptor;
    public final int size_tiles;
    public final int size_chunks;
    public final int tiles_count;
    public final int chunks_count;

    MapSize(int size, String descriptor) {
        this.size_tiles = size;
        this.size_chunks = size / CHUNK_SIZE;
        this.tiles_count = size * size;
        this.chunks_count = size_chunks * size_chunks;
        this.descriptor = descriptor;
    }

    public static MapSize get(int id) {
        return ALL[id % COUNT];
    }

    public MapSize next() {
        return get(ordinal() + 1);
    }

    public MapSize prev() {
        return ordinal() == 0 ? ALL[(COUNT - 1)] : ALL[ordinal() - 1];
    }
}
