package io.github.heathensoft.jagfw.physics;


import org.joml.Vector2f;

/**
 * Package private utility class
 * (various constants and shared math objects)
 * @author Frederik Dahl
 * 15/01/2025
 */


class PhysicsUtil {
    
    protected static float PI = (float) Math.PI;
    protected static float PI2 = (float) (Math.PI * 2.0);
    protected static float PI_HALF = (float) (Math.PI / 2.0);
    protected static float ROT_EPSILON = 1e-3f;
    
    private static final int v2Count = 32;
    private static int v2Idx = v2Count - 1;
    private static final Vector2f[] vec2_stack = new Vector2f[v2Count];
    
    static {
        for (int i = 0; i < vec2_stack.length; i++) {
            vec2_stack[i] = new Vector2f();
        }
    }
    
    protected static void pushVec2() { v2Idx++; }
    protected static void pushVec2(int count) { v2Idx += count; }
    protected static Vector2f popVec2() { return vec2_stack[v2Idx--]; }
    protected static Vector2f popSetVec2(Vector2f vec) { return popVec2().set(vec); }
    protected static Vector2f popSetVec2(float x, float y) { return popVec2().set(x,y); }
    
    /**
     * "Cross product" of two 2D vectors.
     * The scalar magnitude of the z component of
     * the resulting perpendicular vector
     * @param a vector a
     * @param b vector b
     * @return the z value of the cross product
     */
    protected static float cross(Vector2f a, Vector2f b) {
        return a.x * b.y - a.y * a.x;
    }
    
}
