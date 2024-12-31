package io.github.heathensoft.guide.game;

import io.github.heathensoft.guide.core.Disposable;
import io.github.heathensoft.guide.core.gfx.Bitmap;
import io.github.heathensoft.guide.core.gfx.SpriteBatch;
import io.github.heathensoft.guide.core.gfx.Texture;
import io.github.heathensoft.guide.utils.Color;
import io.github.heathensoft.guide.utils.Coordinate;
import io.github.heathensoft.guide.utils.Resources;
import io.github.heathensoft.guide.utils.U;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;

import java.util.*;

import static org.lwjgl.opengl.GL11.*;

/**
 * Frederik Dahl 12/30/2024
 */
public class World implements Disposable {

    private final static int CHUNK_SIZE = 16;

    private final Map<Coordinate,Chunk> chunk_map = new HashMap<>();
    private final List<Coordinate> chunks_for_removal = new LinkedList<>();
    private final Texture block_texture;
    private final Vector4f block_uv;

    public World() throws Exception {
        Bitmap bitmap = Resources.image("blocks.png",32 * 1024,false);
        block_texture = bitmap.asTexture(true);
        block_texture.textureFilter(GL_LINEAR_MIPMAP_LINEAR,GL_LINEAR);
        block_texture.clampToBorder();
        block_texture.generateMipmap();
        block_uv = new Vector4f();
        U.texRegionToUV(block_uv,block_texture.width(),block_texture.height(),
        112,80,16,16,true);
        bitmap.dispose();
    }

    private static final class Chunk {
        int count;
        boolean[][] blocks = new boolean[CHUNK_SIZE][CHUNK_SIZE];
        void place(int x, int y) {
            boolean block_exist = blocks[y][x];
            if (!block_exist) {
                blocks[y][x] = true;
                count++;
            }
        }
        void remove(int x, int y) {
            boolean block_exist = blocks[y][x];
            if (block_exist) {
                blocks[y][x] = false;
                count--;
            }
        }
    }

    public void render(SpriteBatch batch, Rectanglef camera_view) {
        if (chunk_map.isEmpty()) return;
        for (var entry : chunk_map.entrySet()) {
            Chunk chunk = entry.getValue();
            Coordinate chunk_pos = entry.getKey();
            if (chunk.count == 0) {
                chunks_for_removal.add(chunk_pos);
            }
            else {
                boolean chunk_is_visible;
                {
                    float chunk_min_x = chunk_pos.x * CHUNK_SIZE;
                    float chunk_min_y = chunk_pos.y * CHUNK_SIZE;
                    float chunk_max_x = chunk_min_x + CHUNK_SIZE;
                    float chunk_max_y = chunk_min_y + CHUNK_SIZE;
                    Rectanglef chunk_area = U.popRect();
                    chunk_area.setMin(chunk_min_x,chunk_min_y);
                    chunk_area.setMax(chunk_max_x,chunk_max_y);
                    chunk_is_visible = camera_view.intersectsRectangle(chunk_area);
                    U.pushRect();
                }
                if (chunk_is_visible) {
                    Rectanglef rect = U.popRect();
                    int count = chunk.count;
                    early_out:
                    for (int r = 0; r < CHUNK_SIZE; r++) {
                        for (int c = 0; c < CHUNK_SIZE; c++) {
                            if (chunk.blocks[r][c]) {
                                float x_min = chunk_pos.x * CHUNK_SIZE + c;
                                float y_min = chunk_pos.y * CHUNK_SIZE + r;
                                float x_max = x_min + 1;
                                float y_max = y_min + 1;
                                rect.setMin(x_min,y_min);
                                rect.setMax(x_max,y_max);
                                batch.draw(block_texture,rect,block_uv,Color.WHITE_BITS);
                                if (--count == 0) break early_out;
                            }
                        }
                    }
                    U.pushRect();
                }
            }
        }
        while (!chunks_for_removal.isEmpty()) {
            chunk_map.remove(chunks_for_removal.removeFirst());
        }
    }

    public void clear() {
        chunk_map.clear();
        chunks_for_removal.clear();
    }

    public void placeBlock(int x, int y) {
        int chunk_x = U.floor(x / (float) CHUNK_SIZE);
        int chunk_y = U.floor(y / (float) CHUNK_SIZE);
        int local_x = U.modRepeat(x,CHUNK_SIZE);
        int local_y = U.modRepeat(y,CHUNK_SIZE);
        Chunk chunk;
        Coordinate coordinate = new Coordinate(chunk_x,chunk_y);
        if (!chunk_map.containsKey(coordinate)) {
            chunk = new Chunk();
            chunk_map.put(coordinate,chunk);
        } else chunk = chunk_map.get(coordinate);
        chunk.place(local_x,local_y);
    }

    public void removeBlock(int x, int y) {
        int chunk_x = U.floor(x / (float) CHUNK_SIZE);
        int chunk_y = U.floor(y / (float) CHUNK_SIZE);
        int local_x = U.modRepeat(x,CHUNK_SIZE);
        int local_y = U.modRepeat(y,CHUNK_SIZE);
        Coordinate coordinate = new Coordinate(chunk_x,chunk_y);
        Chunk chunk = chunk_map.get(coordinate);
        if (chunk != null) chunk.remove(local_x,local_y);
    }

    @Override
    public void dispose() {
        Disposable.dispose(block_texture);
    }
}
