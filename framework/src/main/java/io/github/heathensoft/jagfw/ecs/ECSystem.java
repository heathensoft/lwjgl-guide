package io.github.heathensoft.jagfw.ecs;

import java.util.List;

/**
 * Frederik Dahl 2/16/2025
 */
public abstract class ECSystem {
    protected boolean paused;
    protected abstract void defineAccess(
            List<Class<?>> required_components,
            List<Class<?>> blocking_components);
    public void processSystem(ECS ecs, float dt) { /* */ }
    public void renderSystem(ECS ecs, float alpha) { /* */ }
    public boolean isPaused() { return paused; }
    public void pause() { paused = true; }
    public void unpause() { paused = false; }
}
