package io.github.heathensoft.jagfw.core.gfx;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.GLInfo;
import io.github.heathensoft.jagfw.core.utils.Color;
import io.github.heathensoft.jagfw.core.utils.U;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS;
import static org.lwjgl.opengl.GL32.glFramebufferTexture;

/**
 * A framebuffer is a render target.
 * Frederik Dahl 1/29/2025
 */
public class Framebuffer implements Disposable {

    private static final Vector4f DEFAULT_CLEAR_COLOR = new Vector4f(0f,0f,0f,0f);
    private static int DEFAULT_CLEAR_MASK = GL_COLOR_BUFFER_BIT;
    private static Framebuffer readBuffer = null;
    private static Framebuffer drawBuffer = null;

    private final int id;
    private final int width;
    private final int height;
    private int clear_mask;
    private final Vector4f clear_color;
    private final ColorAttachment[] colorAttachments;
    private NonColorAttachment depthAttachment;
    private NonColorAttachment stencilAttachment;
    private NonColorAttachment depthStencilAttachment;


    public Framebuffer(int width, int height) {
        this.id = glGenFramebuffers();
        this.width = width;
        this.height = height;
        this.clear_mask = GL_COLOR_BUFFER_BIT;
        this.clear_color = new Vector4f(DEFAULT_CLEAR_COLOR);
        this.colorAttachments = new ColorAttachment[16];
    }

    public int handle() {
        return id;
    }
    public int width() {
        return width;
    }
    public int height() {
        return height;
    }
    public Texture texture(int slot) {
        ColorAttachment attachment = colorAttachments[slot];
        if (attachment != null) {
            return attachment.texture();
        } return null;
    }

    public void dispose() {
        if (this == readBuffer) bindRead(null);
        if (this == drawBuffer) bindDraw(null);
        for (ColorAttachment colorAttachment : colorAttachments) {
            Disposable.dispose(colorAttachment);
        } Disposable.dispose(depthStencilAttachment);
        Disposable.dispose(stencilAttachment);
        Disposable.dispose(depthAttachment);
        glDeleteFramebuffers(id);
    }

    /**
     * Detaches the currently bound color texture from the framebuffer attachment slot.
     * You must use this before attaching a new texture to the same slot.
     * The only reason being, I want it to be done explicitly. So there are no
     * accidental overwrites where the old texture is not freed.
     * @param attachment_slot the attachment slot [0-15]
     * @return The previously bound color texture
     * @throws Exception If the current draw-buffer is the glfw default,
     * or if no textures are bound to the slot.
     */
    public static Texture detachColor(int attachment_slot) throws Exception {
        if (drawBuffer != null) {
            ColorAttachment colorAttachment = drawBuffer.colorAttachments[attachment_slot];
            if (colorAttachment != null) {
                int slot = GL_COLOR_ATTACHMENT0 + attachment_slot;
                glFramebufferTexture(GL_FRAMEBUFFER,slot,0,0);
                drawBuffer.colorAttachments[attachment_slot] = null;
                return colorAttachment.texture();
            } throw new Exception("no texture attached to slot: " + attachment_slot);
        } throw new Exception("cannot detach from default framebuffer");
    }

    public static void attachColor(Texture texture, int attachment_slot, boolean dispose_with_fbo) throws Exception {
        if (drawBuffer != null) {
            if (drawBuffer.colorAttachments[attachment_slot] == null) {
                if (texture.width() == drawBuffer.width && texture.height() == drawBuffer.height) {
                    TextureFormat format = texture.format();
                    if (format.is_color_format) {
                        int slot = GL_COLOR_ATTACHMENT0 + attachment_slot;
                        glFramebufferTexture(GL_FRAMEBUFFER,slot,texture.id(),0);
                        drawBuffer.colorAttachments[attachment_slot] =
                                new ColorAttachment(texture,dispose_with_fbo);
                        return;
                    } throw new Exception("cannot attach non-color texture to color attachment ");
                } throw new Exception("texture dimensions does not match framebuffer");
            } throw new Exception("framebuffer already has color attachment in slot");
        } throw new Exception("cannot attach to default framebuffer");
    }

    public static void attachDepth(Texture texture, boolean dispose_with_fbo) throws Exception {
        if (drawBuffer != null) {
            if (drawBuffer.depthAttachment == null && drawBuffer.depthStencilAttachment == null) {
                if (texture.width() == drawBuffer.width && texture.height() == drawBuffer.height) {
                    TextureFormat format = texture.format();
                    if (!format.is_color_format) {
                        if (format.pixel_format == GL_DEPTH_COMPONENT) {
                            glFramebufferTexture(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,texture.id(),0);
                            drawBuffer.depthAttachment = new NonColorAttachment(texture,dispose_with_fbo);
                            return;
                        } throw new Exception("wrong format for depth attachment");
                    } throw new Exception("cannot attach color format texture to non-color attachment");
                } throw new Exception("texture dimensions does not match framebuffer");
            } throw new Exception("framebuffer already has non-color attachment of type");
        } throw new Exception("cannot attach to default framebuffer");
    }

    public static void attachStencil(Texture texture, boolean dispose_with_fbo) throws Exception {
        if (drawBuffer != null) {
            if (drawBuffer.stencilAttachment == null && drawBuffer.depthStencilAttachment == null) {
                if (texture.width() == drawBuffer.width && texture.height() == drawBuffer.height) {
                    TextureFormat format = texture.format();
                    if (!format.is_color_format) {
                        if (format.pixel_format == GL_STENCIL_INDEX) {
                            glFramebufferTexture(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT,texture.id(),0);
                            drawBuffer.stencilAttachment = new NonColorAttachment(texture,dispose_with_fbo);
                            return;
                        } throw new Exception("wrong format for stencil attachment");
                    } throw new Exception("cannot attach color format texture to non-color attachment");
                } throw new Exception("texture dimensions does not match framebuffer");
            } throw new Exception("framebuffer already has non-color attachment of type");
        } throw new Exception("cannot attach to default framebuffer");
    }

    public static void attachDepthStencil(Texture texture, boolean dispose_with_fbo) throws Exception {
        if (drawBuffer != null) {
            if (drawBuffer.stencilAttachment == null && drawBuffer.depthAttachment == null
                    && drawBuffer.depthStencilAttachment == null) {
                if (texture.width() == drawBuffer.width && texture.height() == drawBuffer.height) {
                    TextureFormat format = texture.format();
                    if (!format.is_color_format) {
                        if (format.pixel_format == GL_DEPTH_STENCIL) {
                            glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT,texture.id(),0);
                            drawBuffer.stencilAttachment = new NonColorAttachment(texture,dispose_with_fbo);
                            return;
                        } throw new Exception("wrong format for depth-stencil attachment");
                    } throw new Exception("cannot attach color format texture to non-color attachment");
                } throw new Exception("texture dimensions does not match framebuffer");
            } throw new Exception("framebuffer already has non-color attachment of type");
        } throw new Exception("cannot attach to default framebuffer");
    }

    public static void attachDepth16() throws Exception {
        if (drawBuffer != null) {
            if (drawBuffer.depthAttachment == null && drawBuffer.depthStencilAttachment == null) {
                TextureFormat format = TextureFormat.DEPTH16;
                drawBuffer.depthAttachment = new NonColorAttachment(drawBuffer,format,GL_DEPTH_ATTACHMENT);
                return;
            } throw new Exception("framebuffer already has non-color attachment of type");
        } throw new Exception("cannot attach to default framebuffer");
    }

    public static void attachStencil8() throws Exception {
        if (drawBuffer != null) {
            if (drawBuffer.stencilAttachment == null && drawBuffer.depthStencilAttachment == null) {
                TextureFormat format = TextureFormat.STENCIL8;
                drawBuffer.stencilAttachment = new NonColorAttachment(drawBuffer,format,GL_STENCIL_ATTACHMENT);
                return;
            } throw new Exception("framebuffer already has non-color attachment of type");
        } throw new Exception("cannot attach to default framebuffer");
    }

    public static void attachDepth24_Stencil8() throws Exception {
        if (drawBuffer != null) {
            if (drawBuffer.depthAttachment == null && drawBuffer.stencilAttachment == null
                    && drawBuffer.depthStencilAttachment == null) {
                TextureFormat format = TextureFormat.DEPTH24_STENCIL8;
                drawBuffer.depthStencilAttachment = new NonColorAttachment(drawBuffer,format,GL_DEPTH_STENCIL_ATTACHMENT);
                return;
            } throw new Exception("framebuffer already has non-color attachment of type");
        } throw new Exception("cannot attach to default framebuffer");
    }

    public static void bindDefault() {
        bind(null);
    }

    public static void bind(Framebuffer buffer) {
        int bufferID = buffer == null ? 0 : buffer.id;
        int readBufferID = readBuffer == null ? 0 : readBuffer.id;
        int drawBufferID = drawBuffer == null ? 0 : drawBuffer.id;
        if (bufferID != readBufferID || bufferID != drawBufferID) {
            readBuffer = buffer; drawBuffer = buffer;
            glBindFramebuffer(GL_FRAMEBUFFER, bufferID);
        }
    }

    public static void bindRead(Framebuffer buffer) {
        int bufferID = buffer == null ? 0 : buffer.id;
        int readBufferID = readBuffer == null ? 0 : readBuffer.id;
        if (bufferID != readBufferID) {
            readBuffer = buffer;
            glBindFramebuffer(GL_READ_FRAMEBUFFER, bufferID);
        }
    }

    public static void bindDraw(Framebuffer buffer) {
        int bufferID = buffer == null ? 0 : buffer.id;
        int drawBufferID = drawBuffer == null ? 0 : drawBuffer.id;
        if (bufferID != drawBufferID) {
            drawBuffer = buffer;
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, bufferID);
        }
    }

    public static void readBuffer(int color_slot) {
        glReadBuffer(GL_COLOR_ATTACHMENT0 + color_slot);
    }

    public static void drawBuffer(int color_slot) {
        glDrawBuffer(GL_COLOR_ATTACHMENT0 + color_slot);
    }

    public static Framebuffer boundFramebufferDraw() {
        return usingDefaultDrawBuffer() ? null : drawBuffer;
    }

    public static Framebuffer boundFramebufferRead() {
        return usingDefaultReadBuffer() ? null : readBuffer;
    }

    public static void drawBuffers(int ...color_slots) {
        if (color_slots != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer intBuff = stack.mallocInt(color_slots.length);
                for (int slot : color_slots) {
                    intBuff.put(GL_COLOR_ATTACHMENT0 + slot);
                } glDrawBuffers(intBuff.flip());
            }
        }
    }

    public static void setClearColor(Vector4f color) {
        setClearColor(color.x(),color.y(),color.z(),color.w());
    }

    public static void setClearColor(int color) {
        Vector4f tmp = Color.intColorToRgb(color, U.popVec4());
        setClearColor(tmp);
        U.pushVec4();
    }

    public static void setClearColor(float r, float g, float b, float a) {
        if (usingDefaultDrawBuffer()) {
            DEFAULT_CLEAR_COLOR.set(r, g, b, a);
        } else drawBuffer.clear_color.set(r, g, b, a);
    }

    public static void setClearMask(int mask) {
        if (usingDefaultDrawBuffer()) {
            DEFAULT_CLEAR_MASK = mask;
        } else { drawBuffer.clear_mask = mask;
        }
    }

    public static void viewport() {
        if (usingDefaultDrawBuffer()) {
            Engine.get().window().useWindowViewport();
        } else Engine.get().window().setGLViewport(0,0,drawBuffer.width,drawBuffer.height);
    }

    public static void clear() {
        int clearMask; Vector4f c;
        if (usingDefaultDrawBuffer()) {
            c = DEFAULT_CLEAR_COLOR;
            clearMask = DEFAULT_CLEAR_MASK;
        } else { c = drawBuffer.clear_color;
            clearMask = drawBuffer.clear_mask;
        } glClearColor(c.x,c.y,c.z,c.w);
        glClear(clearMask);
    }

    /**
     * Clear single drawbuffer
     * @param index not GL_COLOR_ATTACHMENT, but the index of that attachment
     * @param value the color value
     */
    public static void clearColorBufferSignedInt(int index, IntBuffer value) {
        glClearBufferiv(GL_COLOR,index,value);
    }

    /**
     * Clear single drawbuffer
     * @param index not GL_COLOR_ATTACHMENT, but the index of that attachment
     * @param value the color value
     */
    public static void clearColorBufferUnsignedInt(int index, IntBuffer value) {
        glClearBufferuiv(GL_COLOR,index,value);
    }

    public static void clearColorBufferNormalized(int index, Vector4f clearColor) {
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer color = stack.mallocFloat(4);
            color.put(clearColor.x);
            color.put(clearColor.y);
            color.put(clearColor.z);
            color.put(clearColor.w);
            clearColorBufferNormalized(index,color.flip());
        }
    }

    public static void clearColorBufferNormalized(int index, FloatBuffer clearColor) {
        glClearBufferfv(GL_COLOR,index,clearColor);
    }

    public static void clearDepthBuffer(float value) {
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer v = stack.mallocFloat(1);
            v.put(value).flip();
            glClearBufferfv(GL_DEPTH,0,v);
        }
    }

    public static void clearStencilBuffer(int value) {
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer v = stack.mallocInt(1);
            v.put(value).flip();
            glClearBufferiv(GL_STENCIL,0,v);
        }
    }

    public static void checkStatus() throws Exception {
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String message = switch (status) {
                case GL_FRAMEBUFFER_UNDEFINED                       -> ": Framebuffer undefined";
                case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT           -> ": Incomplete attachment";
                case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT   -> ": Missing attachment";
                case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER          -> ": Incomplete draw buffer";
                case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER          -> ": Incomplete read buffer";
                case GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE          -> ": Attachment object type";
                case GL_FRAMEBUFFER_UNSUPPORTED                     -> ": Framebuffer unsupported";
                case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE          -> ": Incomplete multi-sample";
                case GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS        -> ": Incomplete layer targets";
                default                                             -> ": Unknown error";
            };
            throw new Exception("Incomplete framebuffer " + status + message);
        }
    }

    public static Bitmap screenshot(int colorAttachment) {
        if (usingDefaultReadBuffer()) return screenshot();
        Texture t = readBuffer.texture(colorAttachment);
        Framebuffer.readBuffer(colorAttachment);
        ByteBuffer buffer = MemoryUtil.memAlloc(t.width()*t.height()*t.format().channels);
        glPixelStorei(GL_PACK_ALIGNMENT,t.format().pack_alignment);
        glReadPixels(0,0,t.width(),t.height(),t.format().pixel_format,t.format().pixel_data_type, buffer);
        return new Bitmap(buffer,t.width(),t.height(),t.format().channels);
    }

    /** Bitmap is flipped vertically */
    public static Bitmap screenshot() {
        Logger.debug("Attempting to take screenshot of default framebuffer, backbuffer");
        Framebuffer currentRead = readBuffer;
        Bitmap screenshot = null;
        if (currentRead != null) {
            Logger.debug("Switching target readBuffer to default framebuffer");
            bindRead(null);
        } boolean RGB_8_FORMAT;
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer red = stack.mallocInt(1);
            IntBuffer gre = stack.mallocInt(1);
            IntBuffer blu = stack.mallocInt(1);
            glGetFramebufferAttachmentParameteriv(GL_READ_FRAMEBUFFER,GL_BACK_LEFT,GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE,red);
            glGetFramebufferAttachmentParameteriv(GL_READ_FRAMEBUFFER,GL_BACK_LEFT,GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE,gre);
            glGetFramebufferAttachmentParameteriv(GL_READ_FRAMEBUFFER,GL_BACK_LEFT,GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE,blu);
            Logger.debug("Framebuffer color depth: RED_BITS {} | GREEN_BITS {} | BLUE_BITS {}", red.get(0),gre.get(0),blu.get(0));
            RGB_8_FORMAT = (red.get(0) == 8 && gre.get(0) == 8 && blu.get(0) == 8);
            GLInfo.checkError();
        } if (RGB_8_FORMAT) {
            int width, height;
            try (MemoryStack stack = MemoryStack.stackPush()){
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                long window = Engine.get().window().handle();
                glfwGetFramebufferSize(window,w,h);
                width = w.get(0); height = h.get(0);
            } Logger.debug("Framebuffer: width: {}, height {}",width,height);
            ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 3);
            glPixelStorei(GL_PACK_ALIGNMENT,1);
            glReadPixels(0, 0, width, height,GL_RGB,GL_UNSIGNED_BYTE, pixels);
            GLInfo.checkError();
            screenshot = new Bitmap(pixels,width,height,3);
        } else Logger.debug("Unable to take screenshot: Not RGB8 format");
        if (currentRead != null) {
            bindRead(currentRead);
        } return screenshot;
    }

    private static boolean usingDefaultDrawBuffer() {
        return drawBuffer == null;
    }

    private static boolean usingDefaultReadBuffer() {
        return readBuffer == null;
    }

    private static abstract class Attachment implements Disposable {
        Texture texture;
        boolean dispose_with_fbo;
        Texture texture() {
            return texture;
        }
    }

    private static final class ColorAttachment extends Attachment {
        ColorAttachment(Texture texture, boolean dispose_with_fbo) {
            this.dispose_with_fbo = dispose_with_fbo;
            this.texture = texture;
        } public void dispose() {
            if (dispose_with_fbo) {
                Disposable.dispose(texture);
            }
        }
    }

    private static final class NonColorAttachment extends Attachment {
        private int rbo;
        NonColorAttachment(Texture texture, boolean dispose_with_fbo) {
            this.dispose_with_fbo = dispose_with_fbo;
            this.texture = texture;
        } NonColorAttachment(Framebuffer fbo, TextureFormat format, int attachment) {
            this.dispose_with_fbo = true;
            this.rbo = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER,rbo);
            glRenderbufferStorage(GL_RENDERBUFFER,format.sized_format,fbo.width,fbo.height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER,attachment,GL_RENDERBUFFER,rbo);
        } public void dispose() {
            if (dispose_with_fbo) {
                if (texture == null) {
                    glDeleteRenderbuffers(rbo);
                } else { Disposable.dispose(texture); }
            }
        }
    }
}
