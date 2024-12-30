package io.github.heathensoft.guide.core.gfx;

import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * Frederik Dahl 12/30/2024
 */
public class SamplerArray {

    private final Texture[] slots;
    private final int glTextureTarget;
    private final int glActiveSlotOffset;
    private final int length;

    private int prev_slot = 0;
    private int next_slot = 0;

    public SamplerArray(int target, int length, int offset) {
        this.glActiveSlotOffset = offset;
        this.glTextureTarget = target;
        this.slots = new Texture[length];
        this.length = length;
    }

    /**
     * Assigns a shader texture slot to texture.
     * If the array is full it returns the array capacity (length)
     * @param texture texture to bind
     * @return assigned slot to upload to shader
     */
    public int assignSlot(Texture texture) {
        if (texture.target() != glTextureTarget)
            throw new RuntimeException("invalid texture target");
        if (next_slot == 0) {
            slots[next_slot] = texture;
            prev_slot = next_slot++;
            return prev_slot;
        } if (slots[prev_slot] == texture) return prev_slot;
        for (int slot = 0; slot < next_slot; slot++) {
            if (slots[slot] == texture) {
                prev_slot = slot;
                return prev_slot;
            }
        }
        if (next_slot == length) return length;
        else { slots[next_slot] = texture;
            prev_slot = next_slot++;
            return prev_slot;
        }
    }

    /**
     * Uploads the sampler array uniform to the currently bound program
     * @param uniform name of the uniform
     */
    public void uploadUniform(String uniform) {
        if (next_slot > 0) {
            try (MemoryStack stack = MemoryStack.stackPush()){
                IntBuffer buffer = stack.mallocInt(next_slot);
                for (int slot = 0; slot < next_slot; slot++) { buffer.put(slot);
                } ShaderProgram.setUniform(uniform,buffer.flip());
                for (int slot = 0; slot < next_slot; slot++) {
                    slots[slot].bindToSlot(slot + glActiveSlotOffset);
                    slots[slot] = null;
                } next_slot = prev_slot = 0;
            }
        }
    }

    public int length() {
        return length;
    }
}
