package io.github.heathensoft.guide.game;

import io.github.heathensoft.guide.core.Disposable;
import io.github.heathensoft.guide.core.gfx.Texture;
import org.joml.Vector2f;

/**
 * Frederik Dahl 1/4/2025
 */
public class Player implements Disposable {

    private Texture texture;
    public float orientation;
    public Vector2f position = new Vector2f();




    @Override
    public void dispose() {
        Disposable.dispose(texture);
    }
}
