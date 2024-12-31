package io.github.heathensoft.guide.game;

import io.github.heathensoft.guide.core.gfx.SpriteBatch;
import io.github.heathensoft.guide.utils.Coordinate;
import io.github.heathensoft.guide.utils.U;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Frederik Dahl 12/30/2024
 */
public class World {

    private final static int CHUNK_SIZE = 16;

    private final Map<Coordinate,Chunk> chunk_map = new HashMap<>();
    private final List<Coordinate> chunks_for_removal = new LinkedList<>();

    private static final class Chunk {
        int count;
        boolean[][] blocks = new boolean[CHUNK_SIZE][CHUNK_SIZE];
        void flip(int x, int y) {
            boolean block_exist = blocks[y][x];
            if (block_exist) {
                blocks[y][x] = false;
                count--;
            } else {
                blocks[y][x] = true;
                count++;
            }
        }
    }

    public World() { }

    public void update(float delta_time) {

    }

    public void render(SpriteBatch batch, Rectanglef camera_view) {
        if (chunk_map.isEmpty()) return;
        for (var entry : chunk_map.entrySet()) {
            Chunk chunk = entry.getValue();
            Coordinate coordinate = entry.getKey();
            if (chunk.count == 0) {
                chunks_for_removal.add(coordinate);
            } else {
                int count = chunk.count;
                out:
                for (int r = 0; r < CHUNK_SIZE; r++) {
                    for (int c = 0; c < CHUNK_SIZE; c++) {
                        if (chunk.blocks[r][c]) {
                            float x0 = coordinate.x * CHUNK_SIZE + c;
                            float y0 = coordinate.y * CHUNK_SIZE + r;
                            Rectanglef rect = U.popSetRect(x0,y0,x0+1,y0+1);
                            Vector4f uv = U.popVec4();
                            batch.draw(null,rect,uv,0xFF000000);
                            U.pushRect();
                            U.pushVec4();
                            if ((count--) == 0) {
                                break out;
                            }
                        }
                    }
                }
            }
        }
        while (!chunks_for_removal.isEmpty()) {
            chunk_map.remove(chunks_for_removal.removeFirst());
        }
    }

    public void toggleBlock(int x, int y) {
        int local_x, local_y;
        int chunk_x, chunk_y;
        if (x < 0) {
            chunk_x = U.floor(x / (float) CHUNK_SIZE);
            local_x = (CHUNK_SIZE + (x % CHUNK_SIZE)) % CHUNK_SIZE;
        } else {
            chunk_x = x / CHUNK_SIZE;
            local_x = x % CHUNK_SIZE;
        } if (y < 0) {
            chunk_y = U.floor(y / (float) CHUNK_SIZE);
            local_y = (CHUNK_SIZE + (y % CHUNK_SIZE)) % CHUNK_SIZE;
        } else {
            chunk_y = y / CHUNK_SIZE;
            local_y = y % CHUNK_SIZE;
        }
        Chunk chunk;
        Coordinate coordinate = new Coordinate(chunk_x,chunk_y);
        if (!chunk_map.containsKey(coordinate)) {
            chunk = new Chunk();
            chunk_map.put(coordinate,chunk);
        } else chunk = chunk_map.get(coordinate);
        chunk.flip(local_x,local_y);

    }

}
