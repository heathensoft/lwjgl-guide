package io.github.heathensoft.jagfw.ecs;

import java.util.List;

/**
 * Frederik Dahl 2/18/2025
 */
public abstract class ProcessSystem extends ECSystem {
    protected abstract void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components);
    protected void preProcessing(ECS ecs, float dt) { /* */ }
    protected abstract void process(ECS ecs, int entity, float dt);
    protected void postProcessing(ECS ecs, float dt) { /* */ };
}
