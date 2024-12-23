package io.github.heathensoft.guide.utils;

import org.joml.Math;

/**
 * Frederik Dahl 12/21/2024
 */
public class U {

    private static final int[] logTable = new int[256];


    static {

        logTable[0] = logTable[1] = 0;
        for (int i=2; i<256; i++) logTable[i] = 1 + logTable[i/2];
        logTable[0] = -1;

    }


    public static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min,Math.min(v,max));
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min,Math.min(v,max));
    }

    public static int floor(float v) {
        return (int) Math.floor(v);
    }

    public static int ceil(float v) {
        return (int) Math.ceil(v);
    }

    public static float fract(float v) {
        return Math.abs(v - Math.floor(v));
    }

    public static float square(float v) {
        return v * v;
    }

    public static float abs(float f) {
        return Math.abs(f);
    }

    public static float sqrt(float v) {
        return Math.sqrt(v);
    }

    public static float pow(float v, float e) {
        return (float) java.lang.Math.pow(v,e);
    }

    public static int round(float f) {
        return Math.round(f);
    }

    public static int round(double d) {
        return (int) Math.round(d);
    }

    public static float round(double d, int digits) {
        if (digits <= 0) return round(d);
        double e = 10 * digits;
        return (float) (Math.round(d * e) / e);
    }

    public static float lerp(float a, float b, float t) {
        return a * (1-t) + b * t;
    }

    public static float unLerp(float a, float b, float t) {
        return clamp((t - a) / (b - a));
    }

    public static float remap(float v, float v_min, float v_max, float out_min, float out_max) {
        return lerp(out_min,out_max,unLerp(v_min,v_max,v));
    }

    public static float smooth(float v) {
        return v * v * (3.0f - 2.0f * v);
    }

    public static boolean floatEquals(double a, double b) {
        return floatEquals(a, b,(1e-9));
    }

    public static boolean floatEquals(double a, double b, double epsilon) {
        return Math.abs(a - b) < epsilon;
    }

    public static int nextPowerOfTwo(int value) {
        if (value-- == 0) return 1;
        value |= value >>> 1;
        value |= value >>> 2;
        value |= value >>> 4;
        value |= value >>> 8;
        value |= value >>> 16;
        return value + 1;
    }

    public static int log2(float f) {
        int x = Float.floatToIntBits(f);
        int c = x >> 23;
        if (c != 0) return c - 127; //Compute directly from exponent.
        else { //Subnormal, must compute from mantissa.
            int t = x >> 16;
            if (t != 0) return logTable[t] - 133;
            else return (x >> 8 != 0) ? logTable[t] - 141 : logTable[x] - 149;
        }
    }



}
