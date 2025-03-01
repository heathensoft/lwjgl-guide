package no.hio.jagfw.testing.ecs.components;

import io.github.heathensoft.jagfw.ecs.ECS;

/**
 * Frederik Dahl 2/28/2025
 */
public interface ProjectileEffect {

    void onHit(ECS ecs, int source, Dude target);
}
