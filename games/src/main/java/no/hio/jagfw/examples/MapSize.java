package no.hio.jagfw.examples;

/**
 * Frederik Dahl 12/31/2024
 */
public enum MapSize {

    TINY(64,"Tiny"),        // 64 x 64
    SMALL(128,"Small"),     // 128 x 128
    MEDIUM(256,"Medium"),   // 256 x 256
    LARGE(512,"Large"),     // 512 x 512
    HUGE(1024,"Huge");      // 1024 x 1024

    public static final int CHUNK_SIZE = 16;
    public static final MapSize[] ALL = values();

    public final String descriptor;
    public final int tiles_across;
    public final int chunks_across;
    public final int tiles_count;
    public final int chunks_count;

    MapSize(int tiles_across, String descriptor) {
        this.tiles_across = tiles_across;
        this.chunks_across = tiles_across / CHUNK_SIZE;
        this.tiles_count = tiles_across * tiles_across;
        this.chunks_count = chunks_across * chunks_across;
        this.descriptor = descriptor;
    }

    public static MapSize get(int id) {
        return ALL[id % ALL.length];
    }

    public MapSize toggleNext() {
        return get(ordinal() + 1);
    }

    public MapSize togglePrev() {
        return ordinal() == 0 ? ALL[(ALL.length - 1)] : ALL[ordinal() - 1];
    }
}
