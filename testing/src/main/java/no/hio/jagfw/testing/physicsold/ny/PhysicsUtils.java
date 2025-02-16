package no.hio.jagfw.testing.physicsold.ny;

import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.utils.LineSegment;
import org.joml.Vector2f;

/**
 * Frederik Dahl 1/31/2025
 */
public class PhysicsUtils {

    private static final int CIRCLE_RESOLUTION = 32;
    private static final int COLOR_RAY = 0xFF00FFF;
    private static final int COLOR_STATIC = 0xFFFFF00;
    private static final int COLOR_DYNAMIC = 0xFF00FF00;
    // <- hitbox color


    public static void drawBody(Body body, LineBatch batch) {
        int color = body.isStatic() ? COLOR_STATIC : COLOR_DYNAMIC;
        drawShape(body.shape,color,batch);
    }

    public static void drawGeometry(Geometry geometry, LineBatch batch) {
        int num_segments = geometry.numSegments();
        for (int i = 0; i < num_segments; i++) {
            Vector2f v0 = geometry.vertices[i];
            Vector2f v1 = geometry.vertices[(i + 1) % geometry.vertices.length];
            batch.drawLine(v0,v1,COLOR_STATIC);
        }
    }

    public static void drawShape(Shape shape, int color, LineBatch batch) {
        if (shape instanceof Circle circle) {
            batch.drawCircle(circle.position,circle.radius,CIRCLE_RESOLUTION,color);
        } else if (shape instanceof Polygon polygon) {
            Vector2f[] vertices = polygon.vertices();
            for (int i = 0; i < vertices.length; i++) {
                Vector2f v0 = vertices[i];
                Vector2f v1 = vertices[((i + 1) % vertices.length)];
                batch.drawLine(v0,v1,color);
            }
        }
    }

    public static void drawRay(LineSegment ray, LineBatch batch) {
        batch.drawLine(ray,COLOR_RAY);
    }

}
