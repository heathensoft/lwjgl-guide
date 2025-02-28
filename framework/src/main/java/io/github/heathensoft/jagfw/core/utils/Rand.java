package io.github.heathensoft.jagfw.core.utils;

import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * Rand is position based. Using methods moves the internal position.
 * You can set, save and load position.
 *
 * @author Frederik Dahl
 * 31/03/2023
 */


public class Rand {

    private static final int DEFAULT_POSITION = 1337;
    private int mark = DEFAULT_POSITION;
    private int seed;
    private final int[] position = new int[] {mark};

    public Rand(int seed) { this.seed = seed; }

    public Rand() { this(Rand.nextInt()); }

    public void reset() {
        mark = DEFAULT_POSITION;
        position[0] = mark;
    }

    public void reset(int seed) {
        this.seed = seed;
        mark = DEFAULT_POSITION;
        position[0] = mark;
    }

    public int seed() { return seed; }

    public int position() { return position[0]; }

    public void load_position() { position[0] = mark; }

    public void save_position() {
        mark = position[0];
    }

    public void set_position(int position) {
        this.position[0] = position;
    }

    public void set_seed(int seed) { this.seed = seed; }

    /** next positive float [0,1] */
    public float white_noise() {
        return white_noise(position,seed);
    }

    /** next positive integer */
    public int next_int() {
        return next_int(position,seed);
    }

    /** next positive integer [0,max] */
    public int next_int(int max) {
        return next_int(position,seed,max);
    }

    public void shuffle_list(List<?> list) {
        shuffle(list,position,seed);
    }

    public static float noise2D(float x, float y, int seed, float fq) {
        final float fx = smooth(fract(x * fq));
        final float fy = smooth(fract(y * fq));
        final int ix = floor(x * fq);
        final int iy = floor(y * fq);
        final float b = mix(white_noise(ix,iy,seed), white_noise(ix + 1,iy,seed),fx);
        final float t = mix(white_noise(ix,iy + 1,seed), white_noise(ix+1,iy + 1,seed),fx);
        return mix(b,t,fy);
    }

    public static float noise2D_layered(float x, float y, int seed, float fq) {
        return noise2D_layered(x, y, seed, fq, 8);
    }

    public static float noise2D_layered(float x, float y, int seed, float fq, int octaves) {
        return noise2D_layered(x, y, seed, fq, octaves, 2.0f);
    }

    public static float noise2D_layered(float x, float y, int seed, float fq, int octaves, float lacunarity) {
        float n = 0.0f, amp = 1.0f, acc = 0.0f;
        for (int i = 0; i < octaves; i++) {
            n += noise2D(x,y,seed++,fq) * amp;
            acc += amp; amp *= 0.5f; fq *= lacunarity;
        } return acc == 0 ? 0 : n / acc;
    }

    public static float noise1D(float x, int seed, float fq) {
        final float fx = smooth(fract(x * fq));
        final int ix = floor(x * fq);
        return mix(white_noise(ix,seed),white_noise(ix+1,seed),fx);
    }

    public static float noise1D_layered(float x, int seed, float fq, int octaves) {
        float n = 0.0f, amp = 1.0f, acc = 0.0f;
        for (int i = 0; i < octaves; i++) {
            n += noise1D(x,seed++,fq) * amp;
            acc += amp; amp *= 0.5f; fq *= 2.0f;
        } return acc == 0 ? 0 : n / acc;
    }

    public static float white_noise(int x, int y, int seed) {
        return (hash(x + (0x0BD4BCB5 * y), seed) & 0x7FFF_FFFF) / (float) 0x7FFF_FFFF;
    }

    public static float white_noise(int[] position, int seed) {
        float f = white_noise(position[0],seed);
        position[0]++;
        return f;
    }

    public static float white_noise(int position, int seed) {
        return next_int(position,seed) / (float) 0x7FFF_FFFF;
    }

    public static int next_int(int[] position, int seed) {
        int i = next_int(position[0],seed);
        position[0]++;
        return i;
    }

    public static int next_int(int[] position, int seed, int max) {
        int i = next_int(position[0],seed,max);
        position[0]++;
        return i;
    }

    public static int next_int(int position, int seed, int max) {
        return next_int(position, seed) % (max + 1);
    }

    public static int next_int(int position, int seed) {
        return hash(position, seed) & 0x7FFF_FFFF;
    }

    public static int hash(int value, int seed) { // full range (32 bit)
        long m = (long) value & 0xFFFFFFFFL;
        m *= 0xB5297AAD;
        m += seed;
        m ^= (m >> 8);
        m += 0x68E31DA4;
        m ^= (m << 8);
        m *= 0x1B56C4E9;
        m ^= (m >> 8);
        return (int) m;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void shuffle(List<?> list, int[] position, int seed) {
        int size = list.size();
        if (size < 5 || list instanceof RandomAccess) {
            for (int i=size; i>1; i--)
                swap(list, i-1, next_int(position,seed,i-1));
        } else {
            Object[] arr = list.toArray();
            for (int i=size; i>1; i--) // Shuffle array
                swap(arr, i-1, next_int(position,seed,i-1));
            ListIterator it = list.listIterator();
            for (Object e : arr) {
                it.next();
                it.set(e);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void swap(List<?> list, int i, int j) {
        ((List) list).set(i, ((List) list).set(j, ((List) list).get(i)));
    }

    private static void swap(Object[] arr, int i, int j) {
        Object tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    // ------------------------------------------------------------------------------------
    // If you don't care about state, you can use these:

    private static long inc = System.currentTimeMillis();

    private static long last = inc | 1;

    public static float nextFloat() {
        return (nextInt(0x2B2A_B5E9) / (float)0x2B2A_B5E9);
    }

    public static int nextInt() {
        return nextInt(Integer.MAX_VALUE);
    }

    public static int nextInt(int max) {
        last ^= (last << 21);
        last ^= (last >>> 35);
        last ^= (last << 4);
        inc += 123456789123456789L;
        int out = (int) ((last + inc) % max);
        return (out < 0) ? -out : out;
    }

    public static void shuffle(List<?> list) {
        shuffle(list,new int[] {nextInt()},nextInt());
    }
    // ------------------------------------------------------------------------------------

    private static float fract(double d) {
        return (float) (d - (long) d);
    }

    private static int floor(double d) {
        return (int) Math.floor(d);
    }

    private static float smooth(float f) {
        return (f * f * (3.0f - 2.0f * f));
    }

    private static float mix(float f1, float f2, float a) {
        return (float) (f1 * (1.0 - a) + f2 * a);
    }
}
