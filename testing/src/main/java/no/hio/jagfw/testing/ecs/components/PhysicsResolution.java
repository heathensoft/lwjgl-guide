package no.hio.jagfw.testing.ecs.components;

/**
 * Frederik Dahl 2/28/2025
 */
public enum PhysicsResolution {
    SLEEP(64),
    LOW(32),
    HIGH(0);
    public final float range;
    PhysicsResolution(float range) {
        this.range = range;
    }
}
