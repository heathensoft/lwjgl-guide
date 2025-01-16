package io.github.heathensoft.jagfw.physics.old;


import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.*;


/**
 * Utility class to calculate collisions
 * (Two static Bodies are never colliding)
 * @author Frederik Dahl
 * 14/01/2025
 */


public class CollisionDetection {
    
    
    public static boolean check(Body a, Body b, BodyContact contact) {
        if (a.isStatic() && b.isStatic()) return false;
        if (a.shape instanceof Shape2.Circle) {
            if (b.shape instanceof Shape2.Circle) {
                return circleCircle(a,b,contact);
            } else if (b.shape instanceof Shape2.PolygonShape) {
                return polygonCircle(b,a,contact);
            }
        } else if (a.shape instanceof Shape2.PolygonShape) {
            if (b.shape instanceof Shape2.PolygonShape) {
                return polygonPolygon(a,b,contact);
            } else if (b.shape instanceof Shape2.Circle) {
                return polygonCircle(a,b,contact);
            }
        }
        return false;
    }
    
    
    
    private static boolean circleCircle(Body a, Body b, BodyContact contact) {
        Shape2.Circle a_shape = (Shape2.Circle) a.shape;
        Shape2.Circle b_shape = (Shape2.Circle) b.shape;
        try { Vector2f ab = popSetVec2(b.position).sub(a.position);
            float radius_sum = a_shape.radius + b_shape.radius;
            if (ab.lengthSquared() <= (radius_sum * radius_sum)) {
                Vector2f start_end = popVec2();
                contact.a = a;
                contact.b = b;
                contact.normal.set(ab);
                contact.normal.normalize();
                contact.start.set(contact.normal).negate();
                contact.start.mul(b_shape.radius).add(b.position);
                contact.end.set(contact.normal);
                contact.end.mul(a_shape.radius).add(a.position);
                start_end.set(contact.end).sub(contact.start);
                contact.depth = start_end.length();
                pushVec2();
                return true;
            } return false;
        } finally {
            pushVec2();
        }
    }
    
    private static boolean polygonPolygon(Body a, Body b, BodyContact contact) {
        Shape2.PolygonShape a_polygon = (Shape2.PolygonShape) a.shape;
        Shape2.PolygonShape b_polygon = (Shape2.PolygonShape) b.shape;
        
        /*
            Should check if polygons are boxes and if they are axis aligned.
            If they are we can do cheaper collision checks
         */
        
        
        {
            Vector2f a_edge_normal = popVec2();
            Vector2f b_edge_normal = popVec2();
            Vector2f a_point = popVec2();
            Vector2f b_point = popVec2();
            
            float ab_separation = findMinSeparation(a_polygon,b_polygon,a_edge_normal,a_point);
            if (ab_separation >= 0) {
                pushVec2(4);
                return false;
            }
            
            float ba_separation = findMinSeparation(b_polygon,a_polygon,b_edge_normal,b_point);
            if (ba_separation >= 0) {
                pushVec2(4);
                return false;
            }
            
            contact.a = a;
            contact.b = b;
            if (ab_separation > ba_separation) {
                contact.depth = -ab_separation;
                contact.normal.set(a_edge_normal);
                contact.start.set(a_point);
                contact.end.set(a_edge_normal).mul(contact.depth);
                contact.end.add(contact.start);
            } else {
                contact.depth = -ba_separation;
                contact.normal.set(b_edge_normal).negate();
                contact.end.set(b_point);
                contact.start.set(b_edge_normal).mul(contact.depth);
                contact.start.add(contact.end);
            }
            
            
            pushVec2(4);
        }
        
        
        
        return true;
    }
    
    private static boolean polygonCircle(Body polygon, Body circle, BodyContact contact) {
        Shape2.PolygonShape polygonShape = (Shape2.PolygonShape) polygon.shape;
        Shape2.Circle circleShape = (Shape2.Circle) circle.shape;
        final Vector2f[] vertices = polygonShape.vertices();
        Vector2f minCurrVertex;
        Vector2f minNextVertex;
        Vector2f edge = popVec2();
        Vector2f normal = popVec2();
        Vector2f circleCenter = popVec2();
        // Loop all the edges of the polygon
        for (int i = 0; i < vertices.length; i++) {
            Vector2f vertex = vertices[i];
            edge = polygonShape.edge(i,edge);
            normal.set(edge).perpendicular().normalize();
            // compare the circle center with the rectangle vertex
            circleCenter.set(circle.position).sub(vertex);
            // project circle center onto the edge normal
            float projection = circleCenter.dot(normal);
            // if we found a dot product projection that is in the positive side of the normal
            if (projection > 0) {
                // circle center is outside the rectangle
                
            }
            
        }
        pushVec2(3);
        return false;
    }
    
    
    private static float findMinSeparation(Shape2.PolygonShape a, Shape2.PolygonShape b, Vector2f edge_normal, Vector2f point) {
        float separation = -Float.MAX_VALUE;
        final Vector2f[] a_vertices = a.vertices();
        final Vector2f[] b_vertices = b.vertices();
        // borrow 3 vectors
        Vector2f edge = popVec2();
        Vector2f va_vb = popVec2();
        Vector2f normal = popVec2();
        for (int i = 0; i < a_vertices.length; i++) {
            Vector2f va = a_vertices[i];
            // edge between this vertex and the next
            // then get the perpendicular normal vector
            edge = a.edge(i,edge);
            normal.set(edge).perpendicular().normalize();
            // store minimum separation of projections
            float min_sep = Float.MAX_VALUE;
            Vector2f min_vertex = b_vertices[0];
            // Loop through all the vertices of polygon b
            for (Vector2f vb : b_vertices) {
                va_vb.set(vb).sub(va);
                float proj = va_vb.dot(normal);
                if (proj < min_sep) {
                    min_sep = proj;
                    min_vertex = vb;
                }
            }
            if (min_sep > separation) {
                separation = min_sep;
                edge_normal.set(normal);
                point.set(min_vertex);
            }
        } // pop the vectors
        pushVec2(3);
        return separation;
    }
    
    
    private static float findMinSeparation(Shape2.PolygonShape a, Shape2.PolygonShape b) {
        float separation = -Float.MAX_VALUE;
        Vector2f[] a_vertices = a.vertices();
        Vector2f[] b_vertices = b.vertices();
        Vector2f edge = popVec2();
        Vector2f va_vb = popVec2();
        for (int i = 0; i < a_vertices.length; i++) {
            Vector2f va = a_vertices[i];
            int next = (i + 1) % a_vertices.length;
            // edge between this vertex and the next
            edge.set(a_vertices[next]).sub(va);
            // the edge's normal vector
            Vector2f normal = edge.perpendicular();
            normal.normalize(); // Not sure if needed. check
            // store minimum separation of projections
            float min_sep = Float.MAX_VALUE;
            for (Vector2f vb : b_vertices) {
                va_vb.set(vb).sub(va);
                min_sep = Math.min(va_vb.dot(normal), min_sep);
            } separation = Math.max(separation, min_sep);
        } pushVec2(2);
        return separation;
    }
}
