package io.github.heathensoft.jagfw.core.gfx;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_SHORT_4_4_4_4;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT16;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT32;
import static org.lwjgl.opengl.GL21.GL_SRGB8;
import static org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.GL_RG;
import static org.lwjgl.opengl.GL31.*;

/**
 * <a href="https://www.khronos.org/opengl/wiki/Image_Format">OpenGL Image Formats</a>
 * Frederik Dahl 12/23/2024
 */
public enum TextureFormat {

    INVALID(0,0,0,0,0,false),
    STENCIL8(GL_STENCIL_INDEX8,GL_STENCIL_INDEX,GL_UNSIGNED_BYTE,0,1,false), // OpenGL 4.4
    DEPTH16(GL_DEPTH_COMPONENT16,GL_DEPTH_COMPONENT,GL_UNSIGNED_SHORT,1,2,false),
    DEPTH32(GL_DEPTH_COMPONENT32,GL_DEPTH_COMPONENT,GL_UNSIGNED_INT,1,4,false),
    DEPTH32F(GL_DEPTH_COMPONENT32F,GL_DEPTH_COMPONENT,GL_FLOAT,1,4,false),
    DEPTH24_STENCIL8(GL_DEPTH24_STENCIL8,GL_DEPTH_STENCIL,GL_UNSIGNED_INT_24_8,2,4,false),
    R32_FLOAT(GL_R32F,GL_R,GL_FLOAT,1,4,true),
    RG32_FLOAT(GL_RG32F,GL_RG,GL_FLOAT,2,8,true),
    RGB32_FLOAT(GL_RGB32F,GL_RGB,GL_FLOAT,3,4,true),
    RGBA32_FLOAT(GL_RGBA32F,GL_RGBA,GL_FLOAT,4,8,true),
    R8_SIGNED_NORMALIZED(GL_R8_SNORM,GL_RED,GL_BYTE,1,1,true),
    R8_UNSIGNED_NORMALIZED(GL_R8,GL_RED,GL_UNSIGNED_BYTE,1,1,true),
    R16_SIGNED_NORMALIZED(GL_R16_SNORM,GL_RED,GL_SHORT,1,2,true),
    R16_UNSIGNED_NORMALIZED(GL_R16,GL_RED,GL_UNSIGNED_SHORT,1,2,true),
    R32_SIGNED_INTEGER(GL_R32I,GL_RED_INTEGER,GL_INT,1,4,true),
    R32_UNSIGNED_INTEGER(GL_R32UI,GL_RED_INTEGER,GL_UNSIGNED_INT,1,4,true),
    RG8_SIGNED_NORMALIZED(GL_RG8_SNORM,GL_RG,GL_BYTE,2,2,true),
    RG8_UNSIGNED_NORMALIZED(GL_RG8,GL_RG,GL_UNSIGNED_BYTE,2,2,true),
    RG16_SIGNED_NORMALIZED(GL_RG16_SNORM,GL_RG,GL_SHORT,2,4,true),
    RG16_UNSIGNED_NORMALIZED(GL_RG16,GL_RG,GL_UNSIGNED_SHORT,2,4,true),
    RGB8_SIGNED_NORMALIZED(GL_RGB8_SNORM,GL_RGB,GL_BYTE,3,1,true),
    RGB8_UNSIGNED_NORMALIZED(GL_RGB8,GL_RGB,GL_UNSIGNED_BYTE,3,1,true),
    SRGB8_UNSIGNED_NORMALIZED(GL_SRGB8,GL_RGB,GL_UNSIGNED_BYTE,3,1,true),
    RGB16_SIGNED_NORMALIZED(GL_RGB16_SNORM,GL_RGB,GL_SHORT,3,2,true),
    RGB16_UNSIGNED_NORMALIZED(GL_RGB16,GL_RGB,GL_UNSIGNED_SHORT,3,2,true),
    RGBA4_UNSIGNED_NORMALIZED(GL_RGBA4,GL_RGBA,GL_UNSIGNED_SHORT_4_4_4_4,4,2,true),
    RGBA8_SIGNED_NORMALIZED(GL_RGBA8_SNORM,GL_RGBA,GL_BYTE,4,4,true),
    RGBA8_UNSIGNED_NORMALIZED(GL_RGBA8,GL_RGBA,GL_UNSIGNED_BYTE,4,4,true),
    SRGBA8_UNSIGNED_NORMALIZED(GL_SRGB8_ALPHA8,GL_RGBA,GL_UNSIGNED_BYTE,4,4,true);

    public final int sized_format; // Internal formal
    public final int pixel_format; // Un-sized
    public final int pixel_data_type;
    public final int channels;
    public final int pack_alignment;
    public final boolean is_color_format;


    TextureFormat(int sized_format, int pixel_format, int pixel_data_type, int channels, int pack_alignment, boolean is_color_format) {
        this.sized_format = sized_format;
        this.pixel_format = pixel_format;
        this.pixel_data_type = pixel_data_type;
        this.channels = channels;
        this.pack_alignment = pack_alignment;
        this.is_color_format = is_color_format;
    }

}
