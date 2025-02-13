package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;

/**
 * BlockLayout is used for collision detection.
 * If your Game World is tile based with blocks placed on it.
 * You can have your "Tile Map" implement this.
 * <note>
 * Methods do not need to check whether a coordinate is within the bounds
 * of the "Map"(except contains()).
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
     * @param coordinate coordinate
     * @return true if coordinate is within map bounds
     */
    default boolean contains(Vector2f coordinate) {
        return contains(U.floor(coordinate.x),U.floor(coordinate.y));
    }

    /**
     * @param x map x-coordinate
     * @param y map y-coordinate
     * @return true if a block is placed on the map
     * at coordinate (x,y)
     */
    boolean isBlock(int x, int y);

    /**
     * @param coordinate coordinate
     * @return true if a block is placed on the map
     * at coordinate (x,y)
     */
    default boolean isBlock(Vector2f coordinate) {
        return isBlock(U.floor(coordinate.x),U.floor(coordinate.y));
    }


    default float blockRestitution(int x, int y) { return 0; }


    /**
     * block-type should be in the range 0 to 15.
     * (There are 16 configurations of geometry needed for a classical block based game)
     * Block Type is calculated based on neighboring blocks in the 4 cardinal directions.
     * Using the following example will result in correct block type:
     * <pre>
     * <code>
     * int block_type = 0;
     * if neighboring block NORTH: block_type += 1;
     * if neighboring block WEST:  block_type += 2;
     * if neighboring block EAST:  block_type += 4;
     * if neighboring block SOUTH: block_type += 8;
     * return block_type;
     * </pre>
     * </code>
     * So a block with 0 neighboring blocks (N W E S)
     * will have the block type: 0
     * <note>
     * Block type should be stored as an internal value.
     * Updated / recalculated once blocks are placed or removed.
     * (Instead of checking neighbors every time this method is called)
     * </note>
     * @param x map x-coordinate
     * @param y map y-coordinate
     * @return block-type for map coordinate.
     *
     */
    int blockType(int x, int y);
}
