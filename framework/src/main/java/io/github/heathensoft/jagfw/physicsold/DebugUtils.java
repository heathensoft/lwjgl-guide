package io.github.heathensoft.jagfw.physicsold;

import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.physicsold.shape.*;
import io.github.heathensoft.jagfw.utils.LineSegment;
import org.joml.Vector2f;

/**
 * Frederik Dahl 1/26/2025
 */
public class DebugUtils {

    public static final int COLOR_BODY = 0xFF00FF00;
    public static final int COLOR_HITBOX = 0xFF00FFF;
    public static final int COLOR_GEOMETRY = 0xFFFFF00;
    public static final int COLOR_COLLISION = 0xFF0000FF;
    public static final int CIRCLE_RESOLUTION = 32;


    public static void drawGeometry(PhysicsGeometry geometry, LineBatch batch) {
        int num_segments = geometry.numSegments();
        for (int i = 0; i < num_segments; i++) {
            LineSegment s = geometry.segment(i, i+1);
            batch.drawLine(s.x0, s.y0, s.x1, s.y1, COLOR_GEOMETRY);
        }
    }

    public static void drawBody(PhysicsBody body, LineBatch batch) {
        int color = body.isStatic() ? COLOR_GEOMETRY : body.colliding() ? COLOR_COLLISION : COLOR_BODY;
        if (body.shape instanceof Circle circle) {
            batch.drawCircle(body.position,circle.radius(),CIRCLE_RESOLUTION,color);
            batch.drawLine(body.position,body.rotation,circle.radius(),color);
        } else if (body.shape instanceof PolygonShape polygon) {
            Vector2f[] vertices = polygon.vertices();
            for (int i = 0; i < vertices.length; i++) {
                Vector2f v0 = vertices[i];
                Vector2f v1 = vertices[((i + 1) % vertices.length)];
                batch.drawLine(v0,v1,color);
            }
        }
    }

    public static void drawHitBox(HitBoxCluster hitbox, LineBatch batch) {
        for (HitBox hb : hitbox) drawHitBox(hb,batch);
    }

    public static void drawHitBox(HitBox hitbox, LineBatch batch) {
        if (hitbox.shape() instanceof Circle circle) {
            batch.drawCircle(hitbox.position(),circle.radius(),CIRCLE_RESOLUTION,COLOR_HITBOX);
        } else if (hitbox.shape() instanceof PolygonShape polygon) {
            Vector2f[] vertices = polygon.vertices();
            for (int i = 0; i < vertices.length; i++) {
                Vector2f v0 = vertices[i];
                Vector2f v1 = vertices[((i + 1) % vertices.length)];
                batch.drawLine(v0,v1,COLOR_HITBOX);
            }
        }
    }



}
