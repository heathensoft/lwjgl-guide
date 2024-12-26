package io.github.heathensoft.guide.core;

/**
 * Key stream listener.
 * Can be implemented by text input fields, text editors and similar.
 * Frederik Dahl 12/26/2024
 */
public interface TextProcessor {

    default void keyPress(int key, int mods, boolean repeat) { /* */ }

    default void keyRelease(int key, int mods) { /* */ }

    default void charPress(byte character) { /* */ }

    default void activateTextProcessor() {
        Engine.get().window().input().keys().addTextProcessor(this);
    }

    default void deactivateTextProcessor() {
        Engine.get().window().input().keys().removeTextProcessor(this);
    }

    default boolean isActiveTextProcessor() {
        return Engine.get().window().input().keys().isTextProcessorActive(this);
    }


}
