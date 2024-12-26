package io.github.heathensoft.guide.core;

import io.github.heathensoft.guide.utils.IntQueue;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;

/**
 *
 * <a href="https://learn.parallax.com/support/reference/ascii-table-0-127">ascii-table</a>
 * Frederik Dahl 12/26/2024
 */
public class Keyboard {

    private final List<TextProcessor> text_processors = new ArrayList<>(); // listeners
    private final IntQueue queued_keys = new IntQueue(48);     // queued key events
    private final IntQueue queued_chars = new IntQueue(16);    // queued chars
    private final boolean[] c_keys = new boolean[GLFW_KEY_LAST];    // currently pressed
    private final boolean[] p_keys = new boolean[GLFW_KEY_LAST];    // previously pressed
    private boolean key_event;

    Keyboard() { /* */ }


    protected void processInput() {
        while (!queued_chars.isEmpty()) {
            // stream queued characters to text processors (listeners)
            int character = queued_chars.dequeue();
            for (TextProcessor processor : text_processors) {
                processor.charPress((byte) character);
            }
        }
        if (key_event) {
            // if key event, set previous key states to current
            System.arraycopy(c_keys,0,
            p_keys,0, GLFW_KEY_LAST);
            key_event = false;
        }
        if (queued_keys.isEmpty()) {
            key_event = false;
        }
        else {
            while (!queued_keys.isEmpty()) {
                int key = queued_keys.dequeue();
                int mods = queued_keys.dequeue();
                int action = queued_keys.dequeue();
                boolean repeat = action == GLFW_REPEAT;
                if (key > 0) {
                    c_keys[key] = true;
                    for (TextProcessor processor : text_processors) {
                        processor.keyPress(key,mods,repeat);
                    }
                } else {
                    key = Math.abs(key);
                    c_keys[key] = false;
                    for (TextProcessor processor : text_processors) {
                        processor.keyRelease(key,mods);
                    }
                }
            }
            key_event = true;
        }
    }

    protected void onKeyEvent(int key, int mods, int action) {
        if (queued_keys.size() == 48) {
            queued_keys.dequeue();
            queued_keys.dequeue();
            queued_keys.dequeue();
        } queued_keys.enqueue(key);
        queued_keys.enqueue(mods);
        queued_keys.enqueue(action);
    }

    protected void onCharPress(int codepoint) {
        if (queued_chars.size() == 16) {
            queued_chars.dequeue();
        } queued_chars.enqueue(codepoint);
    }

    public boolean pressed(int key) {
        if (key > GLFW_KEY_LAST) return false;
        return c_keys[key];
    }

    public boolean pressed(int key1, int key2) {
        return pressed(key1) && pressed(key2);
    }

    public boolean justPressed(int key) {
        if (key >= GLFW_KEY_LAST) return false;
        return c_keys[key] && !p_keys[key];
    }

    /**
     * @param key key
     * @param mod mod (i.e. ctrl, alt, shift etc.)
     * @return mod is pressed, and key is just pressed
     */
    public boolean justPressed(int key, int mod) {
        return pressed(mod) && justPressed(key);
    }

    public boolean justReleased(int key) {
        return p_keys[key] && !c_keys[key];
    }

    public void addTextProcessor(TextProcessor processor) {
        if (processor != null) {
            boolean exist = false;
            for (TextProcessor tp : text_processors) {
                if (tp == processor) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                text_processors.addLast(processor);
            }
        }
    }

    public void removeTextProcessor(final TextProcessor processor) {
        if (processor != null) {
            text_processors.removeIf(tp -> tp == processor);
        }
    }

    public boolean isTextProcessorActive(TextProcessor processor) {
        return text_processors.contains(processor);
    }

    public int numActiveTextProcessors() {
        return text_processors.size();
    }

}
