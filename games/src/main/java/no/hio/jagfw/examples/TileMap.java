package no.hio.jagfw.examples;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.core.gfx.Bitmap;
import io.github.heathensoft.jagfw.core.gfx.SpriteBatch;
import io.github.heathensoft.jagfw.core.gfx.Texture;
import io.github.heathensoft.jagfw.physics.RigidBody;
import io.github.heathensoft.jagfw.physics.Shape;
import io.github.heathensoft.jagfw.utils.Coordinate;
import io.github.heathensoft.jagfw.utils.Resources;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;

import java.util.*;

import static no.hio.jagfw.examples.MapSize.CHUNK_SIZE;
import static org.lwjgl.opengl.GL11.*;

/**
 * Frederik Dahl 12/31/2024
 */
public class TileMap implements Disposable {


    private final int[] tiles;
    private final int[] chunks;
    private final MapSize size;
    private final Texture block_texture;
    private final Vector4f[] block_uvs;

    public TileMap(MapSize size) throws Exception {

        Bitmap bitmap = Resources.image("blocks.png",32 * 1024,false);
        this.block_texture = bitmap.asTexture(true);
        this.block_texture.textureFilter(GL_LINEAR_MIPMAP_LINEAR,GL_LINEAR);
        this.block_texture.clampToBorder();
        this.block_texture.generateMipmap();
        bitmap.dispose();

        this.block_uvs = new Vector4f[6 * 8];
        {
            int block_size_pixels = 16;
            int tex_width = block_texture.width();
            int tex_height = block_texture.height();
            for (int r = 0; r < 6; r++) {
                int region_y = r * block_size_pixels;
                for (int c = 0; c < 8; c++) {
                    int region_x = c * block_size_pixels;
                    Vector4f block_uv = new Vector4f();
                    U.texRegionToUV(block_uv,tex_width,tex_height,region_x,region_y,
                    block_size_pixels,block_size_pixels,true);
                    block_uvs[r * 8 + c] = block_uv;
                }
            }
        }
        this.size = size;
        this.tiles = new int[size.tiles_count];
        this.chunks = new int[size.chunks_count];

    }

    public void renderBlocks(SpriteBatch batch, Rectanglef camera_view) {
        for (int chunk_index = 0; chunk_index < chunks.length; chunk_index++) {
            int chunk_block_count = chunkBlockCount(chunk_index);
            if (chunk_block_count > 0) {
                int chunk_y = chunk_index / widthChunks();
                int chunk_x = chunk_index % widthChunks();
                int x0 = chunk_x * CHUNK_SIZE;
                int y0 = chunk_y * CHUNK_SIZE;
                int x1 = x0 + CHUNK_SIZE;
                int y1 = y0 + CHUNK_SIZE;
                boolean chunk_is_visible;
                Rectanglef rect = U.popSetRect(x0,y0,x1,y1);
                if (camera_view.intersectsRectangle(rect)) {
                    int count = chunk_block_count;
                    early_out:
                    for (int r = 0; r < CHUNK_SIZE; r++) {
                        int y = y0 + r;
                        for (int c = 0; c < CHUNK_SIZE; c++) {
                            int x = x0 + c;
                            int tile_index = tileIndex(x,y);
                            if (isBlock(tile_index)) {
                                rect.setMin(x,y);
                                rect.setMax(x+1,y+1);
                                int uv_index = block_uv_map[getTileMask(tile_index)];
                                batch.draw(block_texture,rect,block_uvs[uv_index],0xFF66BBEE,4);
                                if (--count == 0) break early_out;
                            }
                        }
                    }
                } U.pushRect();
            }
        }
    }

    public void addBlock(int x, int y) {
        int index = tileIndex(x, y);
        if (!isBlock(index)) {
            toggleBlock(index,true);
            updateTileMask(x,y);
            index = chunkIndex(x, y);
            chunkIncrementBlockCount(index,1);
        }
    }



    public void removeBlock(int x, int y) {
        int index = tileIndex(x, y);
        if (isBlock(index)) {
            toggleBlock(index,false);
            updateTileMask(x,y);
            index = chunkIndex(x, y);
            chunkIncrementBlockCount(index,-1);
        }
    }


    public boolean isBlock(int x, int y) {
        return isBlock(tileIndex(x, y));
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x < widthTiles() && y >= 0 && y < heightTiles();
    }

    public boolean contains(Vector2f position) {
        return contains(U.floor(position.x),U.floor(position.y));
    }

    public MapSize mapSize() {
        return size;
    }

    public int widthTiles() {
        return size.tiles_across;
    }

    public int heightTiles() {
        return size.tiles_across;
    }

    public int widthChunks() {
        return size.chunks_across;
    }

    public int heightChunks() {
        return size.chunks_across;
    }

    public void clear() {
        Arrays.fill(tiles, 0);
        Arrays.fill(chunks, 0);
    }

    public void dispose() {
        Disposable.dispose(block_texture);
    }

    private int tileIndex(int x, int y) {
        return y * widthTiles() + x;
    }

    private int chunkIndex(int x, int y) {
        return (y/CHUNK_SIZE) * widthChunks() + (x/CHUNK_SIZE);
    }

    private boolean isBlock(int index) {
        return tiles[index] < 0;
    }

    private int chunkBlockCount(int index) {
        return chunks[index];
    }

    private void chunkIncrementBlockCount(int index, int amount) {
        chunks[index] += amount;
    }

    private void setTile(int index, int value) {
        tiles[index] = value;
    }

    private void toggleBlock(int index, boolean on) {
        tiles[index] = (tiles[index] &~ 0x8000_0000) | ((on ? 1 : 0) << 31);
    }

    private void setTileMask(int index, int mask) {
        tiles[index] = (tiles[index] &~ 0xFF) | (mask & 0xFF);
    }

    private int getTileMask(int index) {
        return tiles[index] & 0xFF;
    }


    private void updateTileMask(int x, int y) {
        for (int[] offset : adjacent9) {
            int tile_x = x + offset[0];
            int tile_y = y + offset[1];
            if (contains(tile_x, tile_y)) {
                int index = tileIndex(tile_x, tile_y);
                if (isBlock(tile_x, tile_y)) {
                    int mask = calculateTileMask(tile_x, tile_y);
                    setTileMask(index, mask);
                } else setTile(index, 0);
            }
        }
    }

    private int calculateTileMask(int x, int y) {
        int mask = 0;
        for (int i = 0; i < adjacent8.length; i++) {
            int tile_x = x + adjacent8[i][0];
            int tile_y = y + adjacent8[i][1];
            if (contains(tile_x,tile_y)) {
                if (isBlock(tile_x,tile_y))
                    mask |= (1 << i);
            } else mask |= (1 << i);
        } return mask;
    }

    public static final int[][] adjacent8 = {
            {-1, 1},{ 0, 1},{ 1, 1},
            {-1, 0}        ,{ 1, 0},
            {-1,-1},{ 0,-1},{ 1,-1}
    };

    public static final int[][] adjacent9 = {
            {-1, 1},{ 0, 1},{ 1, 1},
            {-1, 0},{ 0, 0},{ 1, 0},
            {-1,-1},{ 0,-1},{ 1,-1}
    };

    public static final byte[] block_uv_map = {
            47, 47, 1 , 1 , 47, 47, 1 , 1 , 2 , 2 , 3 , 4 , 2 , 2 , 3 , 4 ,
            5 , 5 , 6 , 6 , 5 , 5 , 7 , 7 , 8 , 8 , 9 , 10, 8 , 8 , 11, 12,
            47, 47, 1 , 1 , 47, 47, 1 , 1 , 2 , 2 , 3 , 4 , 2 , 2 , 3 , 4 ,
            5 , 5 , 6 , 6 , 5 , 5 , 7 , 7 , 8 , 8 , 9 , 10, 8 , 8 , 11, 12,
            13, 13, 14, 14, 13, 13, 14, 14, 15, 15, 16, 17, 15, 15, 16, 17,
            18, 18, 19, 19, 18, 18, 20, 20, 21, 21, 22, 23, 21, 21, 24, 25,
            13, 13, 14, 14, 13, 13, 14, 14, 26, 26, 27, 28, 26, 26, 27, 28,
            18, 18, 19, 19, 18, 18, 20, 20, 29, 29, 30, 31, 29, 29, 32, 33,
            47, 47, 1 , 1 , 47, 47, 1 , 1 , 2 , 2 , 3 , 4 , 2 , 2 , 3 , 4 ,
            5 , 5 , 6 , 6 , 5 , 5 , 7 , 7 , 8 , 8 , 9 , 10, 8 , 8 , 11, 12,
            47, 47, 1 , 1 , 47, 47, 1 , 1 , 2 , 2 , 3 , 4 , 2 , 2 , 3 , 4 ,
            5 , 5 , 6 , 6 , 5 , 5 , 7 , 7 , 8 , 8 , 9 , 10, 8 , 8 , 11, 12,
            13, 13, 14, 14, 13, 13, 14, 14, 15, 15, 16, 17, 15, 15, 16, 17,
            34, 34, 35, 35, 34, 34, 36, 36, 37, 37, 38, 39, 37, 37, 40, 41,
            13, 13, 14, 14, 13, 13, 14, 14, 26, 26, 27, 28, 26, 26, 27, 28,
            34, 34, 35, 35, 34, 34, 36, 36, 42, 42, 43, 44, 42, 42, 45, 46
    };

}
