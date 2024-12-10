package io.github.heathensoft.guide.utils;

import org.joml.Vector3f;
import org.joml.Vector4f;

import static java.lang.Math.min;

/**
 * -------------------------
 * MSB | a | b | g | r | LSB
 * -------------------------
 * Frederik Dahl 12/9/2024
 */
public class Color {

    public static float GAMMA = 2.2f;
    public static float GAMMA_INV = 1f / GAMMA;
    public static final int ERROR_BITS = 0xFF5E2ADC;
    public static final int WHITE_BITS = 0xFFFFFFFF;
    public static final int BLACK_BITS = 0xFF000000;
    private static final Vector4f tmpV4 = new Vector4f();

    public static int rgbToIntColor(Vector4f value) {
        int r = (int)(value.x * 255) & 0xFF;
        int g = (int)(value.y * 255) & 0xFF;
        int b = (int)(value.z * 255) & 0xFF;
        int a = (int)(value.w * 255) & 0xFF;
        return r | (g << 8) | (b << 16) | (a << 24);
    }

    public static Vector4f rgbToHsv(Vector4f value) {
        float r = value.x;
        float g = value.y;
        float b = value.z;
        float max = Math.max(Math.max(r, g), b);
        float min = min(min(r, g), b);
        float range = max - min;
        if (range == 0) value.x = 0;
        else if (max == r) value.x = (60 * (g - b) / range + 360) % 360;
        else if (max == g) value. x = 60 * (b - r) / range + 120;
        else value.x = 60 * (r - g) / range + 240;
        if (max > 0) value.y = 1 - min / max;
        else value.y = 0;
        value.z = max;
        return value;
    }

    public static Vector4f rgbToHsl(Vector4f value) {
        float r = value.x;
        float g = value.y;
        float b = value.z;
        float max = Math.max(Math.max(r, g), b);
        float min = min(min(r, g), b);
        float range = max - min;
        if (range == 0) value.x = 0;
        else if (max == r) value.x = (60 * (g - b) / range + 360) % 360;
        else if (max == g) value. x = 60 * (b - r) / range + 120;
        else value.x = 60 * (r - g) / range + 240;
        float minMax = (max + min);
        value.z = 0.5f * minMax;
        if (value.z < 1f) value.y = range / (1 - Math.abs(2 * value.z - 1));
        else value.y = 0;
        return value;
    }

    public static Vector4f rgbToXyz(Vector4f value) {
        float r = value.x; float g = value.y; float b = value.z;
        if (r > 0.04045) r = (float) Math.pow( ((r + 0.055f) / 1.055f ), 2.4f); else r /= 12.92f;
        if (g > 0.04045) g = (float) Math.pow( ((g + 0.055f) / 1.055f ), 2.4f); else g /= 12.92f;
        if (b > 0.04045) b = (float) Math.pow( ((b + 0.055f) / 1.055f ), 2.4f); else b /= 12.92f;
        r *= 100; g *= 100; b *= 100;
        return value.set(
                r * 0.4124 + g * 0.3576 + b * 0.1805,
                r * 0.2126 + g * 0.7152 + b * 0.0722,
                r * 0.0193 + g * 0.1192 + b * 0.9505,
                value.w
        );
    }

    public static Vector4f rgbToLab(Vector4f value) {
        return xyzToLab(rgbToXyz(value));
    }

    public static String rgbToHex(Vector4f value) {
        return intColorToHex(rgbToIntColor(value));
    }

    public static Vector4f intColorToRgb(int value, Vector4f dst) {
        dst.x = rBits(value) / 255f;
        dst.y = gBits(value) / 255f;
        dst.z = bBits(value) / 255f;
        dst.w = aBits(value) / 255f;
        return dst;
    }

    public static Vector4f intColorToHsv(int value, Vector4f dst) {
        return rgbToHsv(intColorToRgb(value,dst));
    }

    public static Vector4f intColorToHsl(int value, Vector4f dst) {
        return rgbToHsl(intColorToRgb(value,dst));
    }

    public static Vector4f intColorToXyz(int value, Vector4f dst) {
        return rgbToXyz(intColorToRgb(value,dst));
    }

    public static Vector4f intColorToLab(int value, Vector4f dst) {
        return rgbToLab(intColorToRgb(value,dst));
    }

    public static String intColorToHex(int value) {
        int r = rBits(value);
        int g = gBits(value);
        int b = bBits(value);
        int a = aBits(value);
        StringBuilder sb = new StringBuilder(8);
        String string = (Integer.toHexString((r << 24) | (g << 16) | (b << 8) | a)).toUpperCase();
        sb.append(string);
        while (sb.length() < 8) sb.insert(0,'0');
        return sb.toString();
    }

    public static float intColorToFloat(int value) {
        return Float.intBitsToFloat(value & 0xfeffffff);
    }

    public static int floatToIntColor(float value) {
        int intBits = Float.floatToRawIntBits(value);
        intBits |= (int)((intBits >>> 24) * (255f / 254f)) << 24;
        return intBits;
    }

    public static Vector4f hsvToRgb(Vector4f value) {
        float h = value.x;
        float s = value.y;
        float v = value.z;
        float x = (h / 60f + 6) % 6;
        int i = (int)x;
        float f = x - i;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        switch (i) {
            case 0: value.x = v; value.y = t; value.z = p; break;
            case 1: value.x = q; value.y = v; value.z = p; break;
            case 2: value.x = p; value.y = v; value.z = t; break;
            case 3: value.x = p; value.y = q; value.z = v; break;
            case 4: value.x = t; value.y = p; value.z = v; break;
            default:value.x = v; value.y = p; value.z = q; break;
        } return value;
    }

    public static Vector4f hsvToHsl(Vector4f value) {
        float l = value.z - value.z * value.y * 0.5f;
        if (l <= 0 || l >= 1) { value.y = 0;
            value.z = clampNormalized(l);
        } else { value.y = (value.z - l) / min(l,1-l);
            value.z = l;
        } return value;
    }

    public static Vector4f hsvToXyz(Vector4f value) {
        return rgbToXyz(hsvToRgb(value));
    }

    public static Vector4f hsvToLab(Vector4f value) {
        return rgbToLab(hsvToRgb(value));
    }

    public static String hsvToHex(Vector4f value) {
        return rgbToHex(hsvToRgb(tmpV4.set(value)));
    }

    public static Vector4f hslToHsv(Vector4f value) {
        float v = value.z + value.y * min(value.z,1-value.z);
        if (v > 0) { value.y = 2 - (2 * value.z) / v;
        } else value.y = 0;
        value.z = v;
        return value;
    }

    public static String hslToHex(Vector4f value) {
        return rgbToHex(hslToRgb(tmpV4.set(value)));
    }

    public static Vector4f hslToRgb(Vector4f value) {
        return hsvToRgb(hslToHsv(value));
    }

    public static Vector4f xyzToLab(Vector4f dst) {
        dst.div(95.047f,100.000f,108.883f,1.0f); // CIE_1964_DAYLIGHT_sRGB
        float x = dst.x; float y = dst.y; float z = dst.z;
        float a = 1/3f; float b = 16/116f;
        if(x > 0.008856) x = (float) Math.pow(x,a); else x = ( 7.787f * x ) + b;
        if(y > 0.008856) y = (float) Math.pow(y,a); else y = ( 7.787f * y ) + b;
        if(z > 0.008856) z = (float) Math.pow(z,a); else z = ( 7.787f * z ) + b;
        return dst.set(( 116f * y ) - 16,500f * ( x - y ),200f * ( y - z ));
    }

    public static int hexToIntColor(String value) {
        if (value == null || value.isBlank()) { return ERROR_BITS; }
        value = value.charAt(0) == '#' ? value.substring(1) : value;
        if (value.length() < 6) { return ERROR_BITS; }
        try { int r = Integer.parseInt(value.substring(0, 2), 16);
            int g = Integer.parseInt(value.substring(2, 4), 16);
            int b = Integer.parseInt(value.substring(4, 6), 16);
            int a = value.length() != 8 ? 0xFF : Integer.parseInt(value.substring(6, 8), 16);
            return (r | (g << 8) | (b << 16) | (a << 24));
        } catch (NumberFormatException e) { return ERROR_BITS; }
    }


    public static float labDistance(Vector4f lab1, Vector4f lab2) {
        Vector3f v1 = new Vector3f(lab1.x,lab1.y,lab1.z);
        Vector3f v2 = new Vector3f(lab2.x,lab2.y,lab2.z);
        return v1.distance(v2);
    }

    public static Vector4f sRGBToLinear(Vector4f value) {
        value.x = (float) java.lang.Math.pow(value.x,GAMMA);
        value.y = (float) java.lang.Math.pow(value.y,GAMMA);
        value.z = (float) java.lang.Math.pow(value.z,GAMMA);
        return value;
    }

    public static Vector4f linearToSRGB(Vector4f value) {
        value.x = (float) java.lang.Math.pow(value.x,GAMMA_INV);
        value.y = (float) java.lang.Math.pow(value.y,GAMMA_INV);
        value.z = (float) java.lang.Math.pow(value.z,GAMMA_INV);
        return value;
    }

    public static Vector4f preMultiplyAlpha(Vector4f value) {
        value.x *= value.w;
        value.y *= value.w;
        value.z *= value.w;
        return value;
    }

    public static Vector4f unMultiplyAlpha(Vector4f value) {
        if (value.w > 0) {
            float inv = 1f / value.w;
            value.x *= inv;
            value.y *= inv;
            value.z *= inv;
        } return value;
    }

    public static Vector4f clampRGBA(Vector4f value) {
        value.x = clampNormalized(value.x);
        value.y = clampNormalized(value.y);
        value.z = clampNormalized(value.z);
        value.w = clampNormalized(value.w);
        return value;
    }

    public static Vector4f clampHSV(Vector4f value) {
        value.x = value.x % 360;
        value.y = clampNormalized(value.y);
        value.z = clampNormalized(value.z);
        value.w = clampNormalized(value.w);
        return value;
    }

    public static Vector4f clampHSL(Vector4f value) {
        return clampHSV(value);
    }

    public static int rBits(int value) { return value & 0xFF; }

    public static int gBits(int value) { return (value >> 8) & 0xFF; }

    public static int bBits(int value) { return (value >> 16) & 0xFF; }

    public static int aBits(int value) { return (value >> 24) & 0xFF; }

    private static float clampNormalized(float value) {
        return value < 0 ? 0 : value > 1 ? 1 : value;
    }

}
