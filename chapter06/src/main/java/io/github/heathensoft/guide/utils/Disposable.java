package io.github.heathensoft.guide.utils;

/**
 * Disposable is meant for resources allocated outside the Java heap.
 * Native memory or GPU storage.
 * Frederik Dahl 12/5/2024
 */
public interface Disposable {

    static void dispose(Disposable disposable) {
        if (disposable != null) disposable.dispose();
    }

    static void dispose(Disposable ...disposables) {
        if (disposables != null) {
            for (Disposable disposable : disposables)
                disposable.dispose();
        }
    }

    void dispose();
}
