package io.github.heathensoft.jagfw.core.gfx;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.core.utils.Color;
import org.joml.Math;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static io.github.heathensoft.jagfw.core.utils.U.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.stb.STBImageWrite.stbi_flip_vertically_on_write;
import static org.lwjgl.stb.STBImageWrite.stbi_write_png;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * 4 channels: | a | b | g | r | LSB
 * 3 channels: | b | g | r |
 * 2 channels: | g | r |
 * 1 channels: | r |
 * Frederik Dahl 12/10/2024
 */
public class Bitmap implements Disposable {

    private final ByteBuffer pixels;
    private final int width;
    private final int height;
    private final int channels;
    private final boolean stb_allocated;    // allocated by stb library (else allocated by lwjgl)


    public Bitmap(int width, int height, int channels) {
        this.pixels = MemoryUtil.memCalloc(width * height * channels);
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.stb_allocated = false;
    }

    public Bitmap(ByteBuffer bitmap, int width, int height, int channels) {
        if (!bitmap.isDirect()) {
            int size = width * height * channels;
            this.pixels = MemoryUtil.memAlloc(size);
            for (int i = 0; i < size; i++)
                this.pixels.put(i,bitmap.get(i));
        } else this.pixels = bitmap;
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.stb_allocated = false;
    }

    public Bitmap(ByteBuffer png, boolean vFlip) throws Exception {
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);
            if (!stbi_info_from_memory(png, w, h, c))
                throw new Exception(stbi_failure_reason());
            stbi_set_flip_vertically_on_load(vFlip);
            this.pixels = stbi_load_from_memory(png, w, h, c, 0);
            if (pixels == null) throw new Exception(stbi_failure_reason());
            this.width = w.get(0);
            this.height = h.get(0);
            this.channels = c.get(0);
            this.stb_allocated = true;
        }
    }

    public Bitmap(int[][] bitmap) {
        width = bitmap[0].length;
        height = bitmap.length;
        channels = 4;
        this.stb_allocated = false;
        this.pixels = MemoryUtil.memAlloc(sizeOf());
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int idx = (r * width + c) * channels;
                this.pixels.putInt(idx,bitmap[r][c]);
            }
        }
    }

    public Bitmap(float[][] heightmap) {
        this.width = heightmap[0].length;
        this.height = heightmap.length;
        this.channels = 1;
        this.stb_allocated = false;
        this.pixels = MemoryUtil.memAlloc(sizeOf());
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                float f = clamp(heightmap[r][c]);
                pixels.put(r * width + c,(byte) (round(f*255f) & 0xFF));
            }
        }
    }

    public Bitmap(byte[][] red) {
        this.width = red[0].length;
        this.height = red.length;
        this.channels = 1;
        this.stb_allocated = false;
        this.pixels = MemoryUtil.memAlloc(sizeOf());
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int idx = (r * width + c);
                pixels.put(idx,red[r][c]);
            }
        }
    }

    public void clear(int value) {
        byte[] rgba = new byte[channels];
        for (int c = 0; c < channels; c++) {
            rgba[c] = (byte) ((value >> (8 * c)) & 0xFF);
        } for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (y * width + x) * channels;
                for (int c = 0; c < channels; c++) {
                    pixels.put(i + c, rgba[c]);
                }
            }
        }
    }

    public void preMultiplyAlpha() {
        if (channels == 4) {
            final ByteBuffer b = pixels;
            int stride = width * 4;
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    int i = r * stride + c * 4;
                    float alpha = (b.get(i + 3) & 0xFF) / 255.0f;
                    b.put(i, (byte) round(((b.get(i) & 0xFF) * alpha)));
                    b.put(i + 1, (byte) round(((b.get(i + 1) & 0xFF) * alpha)));
                    b.put(i + 2, (byte) round(((b.get(i + 2) & 0xFF) * alpha)));
                }
            }
        }
    }

    public void unMultiplyAlpha() {
        if (channels == 4) {
            final ByteBuffer b = pixels;
            int stride = width * 4;
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    int i = r * stride + c * 4;
                    float alpha_inv = 1f / (b.get(i + 3) & 0xFF) / 255.0f;
                    b.put(i, (byte) round(((b.get(i) & 0xFF) * alpha_inv)));
                    b.put(i + 1, (byte)round(((b.get(i + 1) & 0xFF) * alpha_inv)));
                    b.put(i + 2, (byte)round(((b.get(i + 2) & 0xFF) * alpha_inv)));
                }
            }
        }
    }

    public void setPixelUnchecked(int x, int y, int value) {
        int idx = (y * width + x) * channels;
        if (channels == 4) pixels.putInt(idx,value);
        else { for (int i = 0; i < channels; i++) {
            pixels.put(idx + i, (byte) ((value >> (i * 8)) & 0xFF)); }
        }
    }

    public void drawPixelUnchecked(int x, int y, int value) {
        if (channels == 4) {
            value = alphaBlend(value,getPixelUnchecked(x,y));
        } setPixelUnchecked(x,y,value);
    }

    public int getPixelUnchecked(int x, int y) {
        int idx = (y * width + x) * channels;
        if (channels == 4) {
            return pixels.getInt(idx);
        } int return_value = 0;
        for (int i = 0; i < channels; i++) {
            return_value |= (pixels.get(idx + i) & 0xFF) << (i * 8);
        } return return_value;
    }

    public int getColorComponent(int x, int y, int channel) {
        int idx = ((clampToEdgeY(y) * width + clampToEdgeX(x)) * channels + channel);
        return pixels.get(idx) & 0xFF;
    }

    public int sampleNearest(float u, float v) {
        int return_value = 0;
        int x = clampToEdgeX((int) (u * width));
        int y = clampToEdgeY((int) (v * height));
        int idx = (y * width + x) * channels;
        for (int i = 0; i < channels; i++) {
            return_value |= (pixels.get(idx + i) & 0xFF) << (i * 8);
        } return return_value;
    }

    public int sampleLinear(float u, float v) {
        int return_value = 0;
        float px = (u * width  - 0.5f); // half-pixel offset
        float py = (v * height - 0.5f); // half-pixel offset
        int floor_x = floor(px); float fract_x = px - floor_x;
        int floor_y = floor(py); float fract_y = py - floor_y;
        for (int i = 0; i < channels; i++) {
            int bl = getColorComponent(floor_x, floor_y,i);
            int br = getColorComponent(floor_x + 1,floor_y + 0,i);
            int tl = getColorComponent(floor_x + 0,floor_y + 1,i);
            int tr = getColorComponent(floor_x + 1,floor_y + 1,i);
            float tx = lerp(tl,tr,fract_x);
            float bx = lerp(bl,br,fract_x);
            int c =  round(lerp(bx,tx,fract_y));
            return_value |= ((c & 0xFF) << (i * 8));
        } return return_value;
    }

    public void compressToDisk(String path, boolean v_flip) {
        stbi_flip_vertically_on_write(v_flip);
        stbi_write_png(path,width,height,channels,pixels,stride());
    }

    public Bitmap copy() {
        ByteBuffer copy = MemoryUtil.memAlloc(sizeOf());
        MemoryUtil.memCopy(pixels,copy);
        return new Bitmap(copy,width,height,channels);
    }

    public Bitmap greyScale() {
        if (channels == 1) { return copy();
        } else { int size = width * height;
            ByteBuffer src = pixels;
            ByteBuffer dst = MemoryUtil.memAlloc(size);
            Vector3f luma = new Vector3f(0.2126f,0.7152f,0.0722f);
            Vector3f color = new Vector3f();
            if (channels == 2) {
                for (int i = 0; i < size; i++) {
                    int idx = i * 2;
                    float avg = (src.get(idx) & 0xFF);
                    avg += (src.get(idx + 1)  & 0xFF);
                    dst.put(i,(byte) (round(avg / 2f) & 0xFF)); }
            } else if (channels == 3) {
                for (int i = 0; i < size; i++) {
                    float r = (src.get(i*channels+0) & 0xFF) / 255.0f;
                    float g = (src.get(i*channels+1) & 0xFF) / 255.0f;
                    float b = (src.get(i*channels+2) & 0xFF) / 255.0f;
                    float v = color.set(r,g,b).dot(luma);
                    dst.put(i,(byte) (round(v * 255f) & 0xFF)); }
            } else if (channels == 4) {
                for (int i = 0; i < size; i++) {
                    float r = (src.get(i*channels+0) & 0xFF) / 255.0f;
                    float g = (src.get(i*channels+1) & 0xFF) / 255.0f;
                    float b = (src.get(i*channels+2) & 0xFF) / 255.0f;
                    float a = (src.get(i*channels+3) & 0xFF) / 255.0f;
                    float v = color.set(r,g,b).dot(luma) * a;
                    dst.put(i,(byte) (round(v * 255f) & 0xFF)); }
            } else throw new IllegalStateException("color channels: " + channels);
            return new Bitmap(dst,width,height,1);
        }
    }

    public Bitmap normalMap(float amp) {
        Bitmap bm = channels == 1 ? this : greyScale();
        float[][] hm = new float[height][width];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                float d = ((bm.pixels.get(r*width+c)&0xff)/255f);
                hm[r][c] = (2 * d - 1) * amp; }
        } if (bm != this) bm.dispose();
        ByteBuffer dst = MemoryUtil.memAlloc(width * height * 3);
        float hu, hr, hd, hl;
        final int cBounds = width - 1;
        final int rBounds = height - 1;
        Vector3f n = new Vector3f();
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                hr = c < cBounds ? hm[r][c+1] : hm[r][c];
                hd = r < rBounds ? hm[r+1][c] : hm[r][c];
                hu = r > 0 ? hm[r-1][c] : hm[r][c];
                hl = c > 0 ? hm[r][c-1] : hm[r][c];
                n.set(hl - hr,hd - hu,2).normalize();
                dst.put((byte) Math.round((n.x * 0.5f + 0.5f) * 255));
                dst.put((byte) Math.round((n.y * 0.5f + 0.5f) * 255));
                dst.put((byte) Math.round((n.z * 0.5f + 0.5f) * 255));}
        } return new Bitmap(dst.flip(),width,height,3);
    }

    /** combine bitmaps in order and return new. (Exception if channel overflow or wrong dim )*/
    public static Bitmap combine(Bitmap b0, Bitmap b1) throws Exception {
        if ((b0.channels + b1.channels) <= 4) {
            if (b0.width == b1.width && b0.height == b1.height) {
                int channels = b0.channels + b1.channels;
                int num_pixels = b0.height * b0.width;
                int size = num_pixels * channels;
                ByteBuffer dst = MemoryUtil.memAlloc(size);
                int b0_idx = 0, b1_idx = 0, dst_idx = 0;
                for (int i = 0; i < num_pixels; i++) {
                    for (int j = 0; j < b0.channels; j++) {
                        dst.put(dst_idx++,b0.pixels.get(b0_idx++));
                    } for (int j = 0; j < b1.channels; j++) {
                        dst.put(dst_idx++,b1.pixels.get(b1_idx++)); }
                } return new Bitmap(dst,b0.width,b0.height,channels);
            } else throw new Exception("Combining bitmaps != dimensions");
        } else throw new Exception("Combining bitmaps with sum channels > 4");
    }

    /** combine bitmaps in order and return new. (Exception if channel overflow or wrong dim )*/
    public static Bitmap combine(Bitmap b0, Bitmap b1, Bitmap b2) throws Exception {
        if ((b0.channels + b1.channels + b2.channels) <= 4) {
            if (b0.width == b1.width && b0.height == b1.height && b0.width == b2.width && b0.height == b2.height) {
                int channels = b0.channels + b1.channels + b2.channels;
                int num_pixels = b0.height * b0.width;
                int size = num_pixels * channels;
                ByteBuffer dst = MemoryUtil.memAlloc(size);
                int b0_idx = 0, b1_idx = 0, b2_idx = 0, dst_idx = 0;
                for (int i = 0; i < num_pixels; i++) {
                    for (int j = 0; j < b0.channels; j++) {
                        dst.put(dst_idx++,b0.pixels.get(b0_idx++));
                    } for (int j = 0; j < b1.channels; j++) {
                        dst.put(dst_idx++,b1.pixels.get(b1_idx++));
                    } for (int j = 0; j < b2.channels; j++) {
                        dst.put(dst_idx++,b2.pixels.get(b2_idx++)); }
                } return new Bitmap(dst,b0.width,b0.height,channels);
            } else throw new Exception("Combining bitmaps != dimensions");
        } else throw new Exception("Combining bitmaps with sum channels > 4");
    }

    public Texture asTexture() { return asTexture(false); }

    public Texture asTexture(boolean allocate_mipmap) { return asTexture(allocate_mipmap,false); }

    public Texture asTexture(boolean allocate_mipmap, boolean srgb) {
        Texture texture = Texture.generate2D(width,height);
        TextureFormat format;
        switch (channels) {
            case 1  -> format = TextureFormat.R8_UNSIGNED_NORMALIZED;
            case 2  -> format = TextureFormat.RG8_UNSIGNED_NORMALIZED;
            case 3  -> format = srgb ? TextureFormat.SRGB8_UNSIGNED_NORMALIZED : TextureFormat.RGB8_UNSIGNED_NORMALIZED;
            case 4  -> format = srgb ? TextureFormat.SRGBA8_UNSIGNED_NORMALIZED : TextureFormat.RGBA8_UNSIGNED_NORMALIZED;
            default -> format = TextureFormat.INVALID;
        } texture.bindToActiveSlot();
        texture.allocate(format,allocate_mipmap);
        texture.uploadSubData(pixels);
        return texture;
    }

    public ByteBuffer pixels() { return pixels; }

    public int channels() { return channels; }

    public int stride() { return width * channels; }

    public int sizeOf() { return width * height * channels; }

    public int surfaceArea() { return width * height; }

    public int width() { return width; }

    public int height() { return height; }

    @Override
    public void dispose() {
        if (stb_allocated) stbi_image_free(pixels);
        else MemoryUtil.memFree(pixels);
    }

    /** src channels must <= dst channels.
     * if src == 2 channel and dst == 4, we assume the src is 16-bit grayscale. where the red channel is value and green channel is alpha */
    public void drawNearest(Bitmap source, float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
        float x2 = x + w;
        float y2 = y + h;
        int ix1 = Math.max(floor(x),0);
        int iy1 = Math.max(floor(y),0);
        @SuppressWarnings("SuspiciousNameCombination")
        int ix2 = Math.min(ceil(x2),width);
        int iy2 = Math.min(ceil(y2),height);
        if (source.channels == 4 && channels == 4) {
            for (int r = iy1; r < iy2; r++) {
                float v = remap(r,y,y2,v1,v2);
                for (int c = ix1; c < ix2; c++) {
                    float u = remap(c,x,x2,u1,u2);
                    int dst = getPixelUnchecked(c,r);
                    int src = source.sampleNearest(u,v);
                    setPixelUnchecked(c,r,alphaBlend(src,dst));
                }
            }
        } else if (source.channels == channels) {
            for (int r = iy1; r < iy2; r++) {
                float v = remap(r,y,y2,v1,v2);
                for (int c = ix1; c < ix2; c++) {
                    float u = remap(c,x,x2,u1,u2);
                    int src = source.sampleNearest(u,v);
                    setPixelUnchecked(c,r,src);
                }
            }
        } else if (source.channels < channels) {
            if (channels == 4) {
                if (source.channels == 2) {
                    for (int r = iy1; r < iy2; r++) {
                        float v = remap(r,y,y2,v1,v2);
                        for (int c = ix1; c < ix2; c++) {
                            float u = remap(c,x,x2,u1,u2);
                            int dst = getPixelUnchecked(c,r);
                            int src = source.sampleNearest(u,v);
                            int src_red = Color.rBits(src);
                            int src_gre = Color.gBits(src);
                            src = Color.rgbToIntColor(src_red,src_red,src_red,src_gre);
                            setPixelUnchecked(c,r,alphaBlend(src,dst));
                        }
                    }
                } else {
                    for (int r = iy1; r < iy2; r++) {
                        float v = remap(r,y,y2,v1,v2);
                        for (int c = ix1; c < ix2; c++) {
                            float u = remap(c,x,x2,u1,u2);
                            int dst = getPixelUnchecked(c,r);
                            int src = source.sampleNearest(u,v) | 0xFF000000;
                            setPixelUnchecked(c,r,alphaBlend(src,dst));
                        }
                    }
                }
            } else {
                for (int r = iy1; r < iy2; r++) {
                    float v = remap(r,y,y2,v1,v2);
                    for (int c = ix1; c < ix2; c++) {
                        float u = remap(c,x,x2,u1,u2);
                        int src = source.sampleNearest(u,v);
                        setPixelUnchecked(c,r,src);
                    }
                }
            }
        }
    }

    /** src channels must <= dst channels.
     * if src == 2 channel and dst == 4, we assume the src is 16-bit grayscale. where the red channel is value and green channel is alpha */
    public void drawLinear(Bitmap source, float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
        float x2 = x + w;
        float y2 = y + h;
        int ix1 = Math.max(floor(x),0);
        int iy1 = Math.max(floor(y),0);
        @SuppressWarnings("SuspiciousNameCombination")
        int ix2 = Math.min(ceil(x2),width);
        int iy2 = Math.min(ceil(y2),height);
        if (source.channels == 4 && channels == 4) {
            for (int r = iy1; r < iy2; r++) {
                float v = remap(r,y,y2,v1,v2);
                for (int c = ix1; c < ix2; c++) {
                    float u = remap(c,x,x2,u1,u2);
                    int dst = getPixelUnchecked(c,r);
                    int src = source.sampleLinear(u,v);
                    setPixelUnchecked(c,r,alphaBlend(src,dst));
                }
            }
        } else if (source.channels == channels) {
            for (int r = iy1; r < iy2; r++) {
                float v = remap(r,y,y2,v1,v2);
                for (int c = ix1; c < ix2; c++) {
                    float u = remap(c,x,x2,u1,u2);
                    int src = source.sampleLinear(u,v);
                    setPixelUnchecked(c,r,src);
                }
            }
        } else if (source.channels < channels) {
            if (channels == 4) {
                if (source.channels == 2) {
                    for (int r = iy1; r < iy2; r++) {
                        float v = remap(r,y,y2,v1,v2);
                        for (int c = ix1; c < ix2; c++) {
                            float u = remap(c,x,x2,u1,u2);
                            int dst = getPixelUnchecked(c,r);
                            int src = source.sampleLinear(u,v);
                            int src_red = Color.rBits(src);
                            int src_gre = Color.gBits(src);
                            src = Color.rgbToIntColor(src_red,src_red,src_red,src_gre);
                            setPixelUnchecked(c,r,alphaBlend(src,dst));
                        }
                    }
                } else {
                    for (int r = iy1; r < iy2; r++) {
                        float v = remap(r,y,y2,v1,v2);
                        for (int c = ix1; c < ix2; c++) {
                            float u = remap(c,x,x2,u1,u2);
                            int dst = getPixelUnchecked(c,r);
                            int src = source.sampleLinear(u,v) | 0xFF000000;
                            setPixelUnchecked(c,r,alphaBlend(src,dst));
                        }
                    }
                }
            } else {
                for (int r = iy1; r < iy2; r++) {
                    float v = remap(r,y,y2,v1,v2);
                    for (int c = ix1; c < ix2; c++) {
                        float u = remap(c,x,x2,u1,u2);
                        int src = source.sampleLinear(u,v);
                        setPixelUnchecked(c,r,src);
                    }
                }
            }

        }
    }

    public void linearToSrgb() {
        float inv255 = 1 / 255f;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = (y * width + x) * channels;
                float fSRGB = sqrt((pixels.get(idx) & 0xFF) * inv255);
                int value = ((int)(fSRGB * 255f) & 0xFF);
                pixels.put(idx,(byte) value);
                if (channels > 1) {
                    fSRGB = sqrt((pixels.get(idx + 1) & 0xFF)  * inv255);
                    value = ((int)(fSRGB * 255f) & 0xFF);
                    pixels.put(idx + 1,(byte) value);
                    if (channels > 2) {
                        fSRGB = sqrt((pixels.get(idx + 2) & 0xFF)  * inv255);
                        value = ((int)(fSRGB * 255f) & 0xFF);
                        pixels.put(idx + 2,(byte) value);
                    }
                }
            }
        }
    }

    public void srgbToLinear() {
        float inv255 = 1 / 255f;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = (y * width + x) * channels;
                int iSRGB = (pixels.get(idx) & 0xFF);
                float fLinear = square(inv255 * iSRGB);
                int value = ((int) (fLinear * 255f) & 0xFF);
                pixels.put(idx,(byte) value);
                if (channels > 1) {
                    iSRGB = (pixels.get(idx + 1) & 0xFF) ;
                    fLinear = square(inv255 * iSRGB);
                    value = ((int) (fLinear * 255f) & 0xFF);
                    pixels.put(idx + 1,(byte) value);
                    if (channels > 2) {
                        iSRGB = (pixels.get(idx + 2) & 0xFF);
                        fLinear = square(inv255 * iSRGB);
                        value = ((int) (fLinear * 255f) & 0xFF);
                        pixels.put(idx + 2,(byte) value);
                    }
                }
            }
        }
    }

    private int alphaBlend(int src, int dst) {
        final float src_alpha = ((src >> 24) & 0xFF) / 255f;
        final float dst_alpha = ((dst >> 24) & 0xFF) / 255f;
        float rf = ((src & 0xFF) / 255f) + ((dst & 0xFF) / 255f) * (1 - src_alpha);
        float gf = (((src >> 8) & 0xFF) / 255f) + (((dst >> 8) & 0xFF) / 255f) * (1 - src_alpha);
        float bf = (((src >> 16) & 0xFF) / 255f) + (((dst >> 16) & 0xFF) / 255f) * (1 - src_alpha);
        float af = src_alpha + dst_alpha * (1 - src_alpha);
        int color = ((int) (rf * 255) & 0xFF);
        color |= (((int)(gf * 255) & 0xFF) << 8 );
        color |= (((int)(bf * 255) & 0xFF) << 16);
        color |= (((int)(af * 255) & 0xFF) << 24);
        return color;
    }

    private int clampToEdgeX(int x) {
        //noinspection SuspiciousNameCombination
        return Math.max(0,Math.min(width - 1, x));
    }

    private int clampToEdgeY(int y) {
        return Math.max(0,Math.min(height - 1, y));
    }

}
