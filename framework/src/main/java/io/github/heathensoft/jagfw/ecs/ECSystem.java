package io.github.heathensoft.jagfw.ecs;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.core.Resolution;

import java.util.List;

/**
 * Frederik Dahl 2/16/2025
 */
public abstract class ECSystem implements Disposable {
    protected boolean paused;
    protected void defineAccess(
            List<Class<?>> required_components,
            List<Class<?>> blocking_components) { /* */ }
    protected void processSystem(ECS ecs, float dt) { /* */ }
    protected void renderSystem(ECS ecs, float alpha) { /* */ }
    protected void resizeEvent(ECS ecs, Resolution resolution) { /* */ }
    public boolean isPaused() { return paused; }
    public void pause() { paused = true; }
    public void unpause() { paused = false; }
    public void dispose() { /* */ }
}
