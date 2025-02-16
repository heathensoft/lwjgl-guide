package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.core.utils.Coordinate;
import io.github.heathensoft.jagfw.core.utils.U;
import org.joml.Vector2f;

/**
 * BlockLayout is used in collision detection.
 * If your game world is tile based with "blocks" placed on it.
 * You could have your Tile Map implement this.
 * <note>
 * Other methods than "contains()"  do not need to check whether a coordinate is within map bounds
 * </note>
 * Frederik Dahl 2/12/2025
 */
public interface BlockLayout {

    /**
     * @param x map x-coordinate
     * @param y map y-coordinate
     * @return true if coordinate is within map bounds
     */
    boolean contains(int x, int y);

    /**
     * @param pos position
     * @return true if pos is within map bounds
     */
    default boolean contains(Vector2f pos) {
        return contains(U.floor(pos.x),U.floor(pos.y));
    }

    /**
     * @param coordinate discrete coordinate
     * @return true if coordinate is within map bounds
     */
    default boolean contains(Coordinate coordinate) {
        return contains(coordinate.x,coordinate.y);
    }

    /**
     * @param x map x-coordinate
     * @param y map y-coordinate
     * @return true if a block is placed on the map
     * at coordinate (x,y)
     */
    boolean isBlock(int x, int y);

    /**
     * @param coordinate discrete coordinate
     * @return true if a block is placed on the map
     * at coordinate
     */
    default boolean isBlock(Coordinate coordinate) {
        return isBlock(coordinate.x,coordinate.y);
    }

    /**
     * currently returns default geometry restitution
     * Override if you want custom restitution
     * Based on block type or anything like that.
     * (Ice block, Wood, Stone etc.)
     * @param x map x-coordinate
     * @param y map y-coordinate
     * @return restitution of block(x,y)
     */
    default float blockRestitution(int x, int y) { return Geometry.DEFAULT_RESTITUTION; }

    /**
     * @see #blockRestitution(int, int)
     * @param coordinate discrete coordinate
     * @return restitution of block at coordinate
     */
    default float blockRestitution(Coordinate coordinate) {
        return blockRestitution(coordinate.x,coordinate.y);
    }

    /**
     * currently returns default geometry friction
     * Override if you want custom friction
     * based on block type or anything like that.
     * (Ice block, Wood, Stone etc.)
     * @param x map x-coordinate
     * @param y map y-coordinate
     * @return friction of block(x,y)
     */
    default float blockFriction(int x, int y) { return Geometry.DEFAULT_FRICTION; }

    /**
     * @see #blockFriction(int, int)
     * @param coordinate discrete coordinate
     * @return friction of block at coordinate
     */
    default float blockFriction(Coordinate coordinate) {
        return blockFriction(coordinate.x,coordinate.y);
    }



}
