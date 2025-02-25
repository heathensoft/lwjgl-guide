package no.hio.jagfw.testing.ecs;

import io.github.heathensoft.jagfw.core.gfx.SpriteBatch;
import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.physics.BlockLayout;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;

/**
 * Frederik Dahl 2/13/2025
 */
public class TileMap implements BlockLayout {

    boolean[][] blocks;
    int width;
    int height;

    public TileMap(int width, int height) {
        this.blocks = new boolean[height][width];
        this.width = width;
        this.height = height;
    }

    public void render(SpriteBatch batch) {
        Rectanglef rect = U.popRect();
        Vector4f uv = U.popVec4();
        for (int r = 0; r < blocks.length; r++) {
            for (int c = 0; c < blocks[r].length; c++) {
                if (blocks[r][c]) {
                    rect.setMin(c,r);
                    rect.setMax(c+1,r+1);
                    batch.draw(null,rect,uv,0x99FFF000,0);
                }
            }
        }
        U.pushRect();
        U.pushVec4();
    }

    public void placeBlock(int x, int y) {
        blocks[y][x] = true;
    }

    public void removeBlock(int x, int y) {
        blocks[y][x] = false;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean isBlock(int x, int y) {
        return blocks[y][x];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
