package io.github.heathensoft.guide.utils;

import org.joml.*;
import org.joml.Math;
import org.joml.primitives.Rectanglef;

/**
 * Frederik Dahl 12/21/2024
 */
public class U {

    private static final int[] logTable = new int[256];

    private static final byte v2Count = 32;
    private static final byte v3Count = 16;
    private static final byte v4Count = 16;
    private static final byte m3Count = 4;
    private static final byte m4Count = 8;
    private static final byte rfCount = 32;
    private static int rfIdx = rfCount - 1;
    private static int v4Idx = v4Count - 1;
    private static int v3Idx = v3Count - 1;
    private static int v2Idx = v2Count - 1;
    private static int m4Idx = m4Count - 1;
    private static int m3Idx = m3Count - 1;
    private static final Vector2f[] vec2_stack = new Vector2f[v2Count];
    private static final Vector3f[] vec3_stack = new Vector3f[v3Count];
    private static final Vector4f[] vec4_stack = new Vector4f[v4Count];
    private static final Matrix3f[] mat3_stack = new Matrix3f[m3Count];
    private static final Matrix4f[] mat4_stack = new Matrix4f[m4Count];
    private static final Rectanglef[] rect_stack = new Rectanglef[rfCount];

    public static void pushVec2() { v2Idx++; }
    public static void pushVec3() { v3Idx++; }
    public static void pushVec4() { v4Idx++; }
    public static void pushMat3() { m3Idx++; }
    public static void pushMat4() { m4Idx++; }
    public static void pushRect() { rfIdx++; }
    public static void pushVec2(int count) { v2Idx += count; }
    public static void pushVec3(int count) { v3Idx += count; }
    public static void pushVec4(int count) { v4Idx += count; }
    public static void pushMat3(int count) { m3Idx += count; }
    public static void pushMat4(int count) { m4Idx += count; }
    public static void pushRect(int count) { rfIdx += count; }

    public static Vector2f popVec2() { return vec2_stack[v2Idx--]; }
    public static Vector3f popVec3() { return vec3_stack[v3Idx--]; }
    public static Vector4f popVec4() { return vec4_stack[v4Idx--]; }
    public static Matrix3f popMat3() { return mat3_stack[m3Idx--]; }
    public static Matrix4f popMat4() { return mat4_stack[m4Idx--]; }
    public static Rectanglef popRect() { return rect_stack[rfIdx--]; }
    public static Vector2f popSetVec2(Vector2f vec) { return popVec2().set(vec); }
    public static Vector3f popSetVec3(Vector3f vec) { return popVec3().set(vec); }
    public static Vector4f popSetVec4(Vector4f vec) { return popVec4().set(vec); }
    public static Rectanglef popSetRect(Rectanglef rect) { return popRect().set(rect); }
    public static Vector2f popSetVec2(float x, float y) { return popVec2().set(x,y); }
    public static Vector3f popSetVec3(float x, float y, float z) { return popVec3().set(x,y,z); }
    public static Vector4f popSetVec4(float x, float y, float z, float w) { return popVec4().set(x,y,z,w); }
    public static Rectanglef popSetRect(float minX, float minY, float maxX, float maxY) {
        Rectanglef rect = popRect();
        rect.minX = minX;
        rect.minY = minY;
        rect.maxX = maxX;
        rect.maxY = maxY;
        return rect;
    }


    static {

        logTable[0] = logTable[1] = 0;
        for (int i=2; i<256; i++) logTable[i] = 1 + logTable[i/2];
        logTable[0] = -1;

        for (int i = 0; i < vec2_stack.length; i++) vec2_stack[i] = new Vector2f();
        for (int i = 0; i < vec3_stack.length; i++) vec3_stack[i] = new Vector3f();
        for (int i = 0; i < vec4_stack.length; i++) vec4_stack[i] = new Vector4f();
        for (int i = 0; i < mat3_stack.length; i++) mat3_stack[i] = new Matrix3f();
        for (int i = 0; i < mat4_stack.length; i++) mat4_stack[i] = new Matrix4f();
        for (int i = 0; i < rect_stack.length; i++) rect_stack[i] = new Rectanglef();

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

    public static int modRepeat(int v, int range) { return v < 0 ? (range + (v % range)) % range : v % range; }

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

    public static float angle2D(float x, float y) { return Math.atan2(y,x); }

    public static float angle2D(Vector2f v) { return Math.atan2(v.y,v.x); }


    public static Vector4f texRegionToUV(Vector4f dst, int texture_w, int texture_h, int region_x, int region_y, int region_w, int region_h) {
        return texRegionToUV(dst,texture_w,texture_h,region_x,region_y,region_w,region_h,false);
    }

    public static Vector4f texRegionToUV(Vector4f dst, int texture_w, int texture_h, int region_x, int region_y, int region_w, int region_h, boolean pixel_centered) {
        if (texture_w <= 0 || texture_h <= 0) throw new RuntimeException("invalid texture region size");
        texture_w = Math.max(1,texture_w);
        texture_h = Math.max(1,texture_h);
        region_x = region_x % texture_w;
        region_y = region_y % texture_h;
        if (pixel_centered) {
            dst.x = (region_x + 0.5f) / texture_w;
            dst.y = (region_y + 0.5f) / texture_h;
            dst.z = (region_x + region_w - 0.5f) / texture_w;
            dst.w = (region_y + region_h - 0.5f) / texture_h;
        } else {
            dst.x = (float) region_x / texture_w;
            dst.y = (float) region_y / texture_h;
            dst.z = (float) (region_x + region_w) / texture_w;
            dst.w = (float) (region_y + region_h) / texture_h;
        } return dst;
    }

    public static Vector4f uvFlipV(Vector4f dst) {
        float tmp = dst.y;
        dst.y = dst.w;
        dst.w = tmp;
        return dst;
    }

    public static Vector4f uvFlipH(Vector4f dst) {
        float tmp = dst.x;
        dst.x = dst.z;
        dst.z = tmp;
        return dst;
    }

}
