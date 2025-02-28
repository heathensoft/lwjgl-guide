package no.hio.jagfw.testing.ecs;

import io.github.heathensoft.jagfw.physics.Geometry;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/27/2025
 */
public class World {

    public final TileMap tilemap;
    public final Geometry bounds;
    public float friction = 300;
    public float drag = 20;

    public World(TileMap tilemap) {
        this.tilemap = tilemap;
        this.bounds = new Geometry(4);
        Vector2f[] vertices = bounds.vertices;
        vertices[0].set(0,tilemap.height);
        vertices[1].set(tilemap.width,tilemap.height);
        vertices[2].set(tilemap.width,0);
        vertices[3].set(0,0);
        bounds.should_treat_as_polygon = true;
        bounds.one_way_collision = true;
    }
}
