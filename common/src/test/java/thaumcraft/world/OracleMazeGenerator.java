package thaumcraft.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The reference labyrinth generator of the original TC4 4.2.3.5.
 *
 * <p>This is the test side of the fidelity proof of the Outer Lands labyrinth. It is a
 * transcription of the oracle {@code thaumcraft/common/lib/world/dim/MazeGenerator.java}
 * (lines 15-444) into a self contained class that only needs a {@link Random}, so that the
 * native {@link thaumcraft.common.lib.world.MazeGenerator} can be compared with it cell by
 * cell.
 *
 * <p>Every statement of the transcription keeps the order of the original, because the
 * original draws from a single {@link Random} stream and the draw order is part of the
 * layout. The oracle sources are the only source of truth; the native port must reproduce
 * them byte for byte.
 *
 * <p>The oracle map is a {@code int[width][height]} grid in which the low byte carries the
 * four exit bits and the high byte carries the feature marker:
 * <ul>
 *   <li>{@code N = 1}, {@code S = 2}, {@code E = 4}, {@code W = 8} - the exits,</li>
 *   <li>{@code A = 16}, {@code B = 32} - the two unused bits,</li>
 *   <li>bits 8-15 - the feature marker of the cell.</li>
 * </ul>
 */
final class OracleMazeGenerator {
    /** The oracle direction bits. */
    static final int N = 1;
    static final int S = 2;
    static final int E = 4;
    static final int W = 8;

    private final int width;
    private final int height;
    private final Random rand;
    /** The generated grid: {@code grid[y][x]}, the oracle layout. */
    final int[][] grid;

    OracleMazeGenerator(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.rand = new Random(seed);
        this.grid = new int[height][width];
    }

    /** The oracle {@code getOPP}. */
    private static int opposite(int in) {
        switch (in) {
            case N: return S;
            case S: return N;
            case E: return W;
            case W: return E;
            default: return -99;
        }
    }

    /** The oracle {@code getDX}. */
    private static int dx(int in) {
        switch (in) {
            case N: case S: return 0;
            case E: return 1;
            case W: return -1;
            default: return -99;
        }
    }

    /** The oracle {@code getDY}. */
    private static int dy(int in) {
        switch (in) {
            case N: return -1;
            case S: return 1;
            case E: case W: return 0;
            default: return -99;
        }
    }

    /** The oracle {@code getNextIndex}: forty five percent newest, forty five percent random, ten percent oldest. */
    private int nextIndex(int ceil) {
        float r = this.rand.nextFloat();
        if (r <= 0.45F) return ceil - 1;
        if (r <= 0.9F) return this.rand.nextInt(ceil);
        return 0;
    }

    /** The oracle {@code generate}: the whole layout, draw for draw. */
    boolean generate() {
        int bx;
        int by;
        switch (this.rand.nextInt(4)) {
            case 0: {
                bx = 0;
                by = 0;
                break;
            }
            case 1: {
                bx = this.width - 2;
                by = this.height - 2;
                break;
            }
            case 2: {
                bx = this.width - 2;
                by = 0;
                break;
            }
            default: {
                bx = 0;
                by = this.height - 2;
            }
        }
        this.grid[by][bx] = 512;
        this.grid[by][bx + 1] = 768;
        this.grid[by + 1][bx] = 1024;
        this.grid[by + 1][bx + 1] = 1280;
        int px = 1 + this.width / 2;
        int py = 1 + this.height / 2;
        this.grid[py][px] = 256;
        ArrayList<Loc> cells = new ArrayList<>();
        int l = (this.width + this.height) / 4;
        for (int z = 0; z < l; ++z) {
            int w = 1 + this.rand.nextInt(3);
            if (w > 2) --l;
            int qq = this.rand.nextInt(this.width - w);
            int ww = this.rand.nextInt(this.height - w);
            for (int a = qq; a < qq + w; ++a) {
                for (int b = ww; b < ww + w; ++b) {
                    if (this.grid[b][a] != 0) continue;
                    this.grid[b][a] = -1;
                }
            }
        }
        List<Integer> directions = Arrays.asList(N, S, E, W);
        Collections.shuffle(directions, this.rand);
        int xx = px + dx(directions.get(0));
        int yy = py + dy(directions.get(0));
        this.grid[py][px] |= directions.get(0);
        if (this.grid[yy][xx] < 0) this.grid[yy][xx] = 0;
        this.grid[yy][xx] |= opposite(directions.get(0));
        cells.add(new Loc(xx, yy));
        boolean success = false;
        while (!cells.isEmpty()) {
            int index = this.nextIndex(cells.size());
            int x = cells.get(index).x;
            int y = cells.get(index).y;
            Collections.shuffle(directions, this.rand);
            boolean carved = false;
            for (int dir : directions) {
                int nx = x + dx(dir);
                int ny = y + dy(dir);
                if (0 >= nx || nx >= this.width - 1 || 0 >= ny || ny >= this.height - 1) continue;
                if (this.grid[ny][nx] == 0) {
                    this.grid[y][x] |= dir;
                    this.grid[ny][nx] |= opposite(dir);
                    cells.add(new Loc(nx, ny));
                    carved = true;
                }
                if (!carved) continue;
                success = true;
                break;
            }
            if (carved) continue;
            cells.remove(index);
        }
        if (!success) return false;
        for (int aa = 0; aa < this.height; ++aa) {
            for (int bb = 0; bb < this.width; ++bb) {
                if (this.grid[aa][bb] >= 0) continue;
                this.grid[aa][bb] = 0;
            }
        }
        Collections.shuffle(directions, this.rand);
        for (int dir : directions) {
            int nx = px + dx(dir);
            int ny = py + dy(dir);
            if (0 >= nx || nx >= this.width - 1 || 0 >= ny || ny >= this.height - 1) continue;
            if (this.grid[ny][nx] <= 0) continue;
            if (!this.rand.nextBoolean()) continue;
            this.grid[ny][nx] |= opposite(dir);
            this.grid[py][px] |= dir;
        }
        Collections.shuffle(directions, this.rand);
        boolean connected = false;
        outer:
        for (int ax = 0; ax < 2; ++ax) {
            for (int ay = 0; ay < 2; ++ay) {
                for (int dir : directions) {
                    int nx = bx + ax + dx(dir);
                    int ny = by + ay + dy(dir);
                    if (0 >= nx || nx >= this.width - 1 || 0 >= ny || ny >= this.height - 1) continue;
                    if (this.grid[ny][nx] <= 0) continue;
                    if (feature(this.grid[ny][nx]) != 0) continue;
                    this.grid[ny][nx] |= opposite(dir);
                    this.grid[by + ay][bx + ax] |= dir;
                    connected = true;
                    break outer;
                }
            }
        }
        if (!connected) {
            List<Integer> directions2 = Arrays.asList(N, S, E, W);
            Collections.shuffle(directions2, this.rand);
            success = false;
            quadrant:
            for (int ax = 0; ax < 2; ++ax) {
                for (int ay = 0; ay < 2; ++ay) {
                    for (int dir2 : directions2) {
                        int qx = bx + ax + dx(dir2);
                        int qy = by + ay + dy(dir2);
                        if (0 >= qx || qx >= this.width - 1 || 0 >= qy || qy >= this.height - 1) continue;
                        if (this.grid[qy][qx] != 0) continue;
                        cells.add(new Loc(qx, qy));
                        while (!cells.isEmpty()) {
                            int index = this.nextIndex(cells.size());
                            int x = cells.get(index).x;
                            int y = cells.get(index).y;
                            Collections.shuffle(directions, this.rand);
                            boolean carved = false;
                            for (int dir : directions) {
                                int nx = x + dx(dir);
                                int ny = y + dy(dir);
                                if (0 >= nx || nx >= this.width - 1 || 0 >= ny || ny >= this.height - 1) continue;
                                if (this.grid[ny][nx] == 0) {
                                    this.grid[y][x] |= dir;
                                    this.grid[y][x] |= 0x6300;
                                    this.grid[ny][nx] |= opposite(dir);
                                    this.grid[ny][nx] |= 0x6300;
                                    cells.add(new Loc(nx, ny));
                                    carved = true;
                                } else if (feature(this.grid[ny][nx]) == 0) {
                                    this.grid[y][x] |= dir;
                                    this.grid[ny][nx] |= opposite(dir);
                                    this.grid[qy][qx] |= opposite(dir2);
                                    this.grid[by + ay][bx + ax] |= dir2;
                                    success = true;
                                    break quadrant;
                                }
                                if (!carved) continue;
                                break;
                            }
                            if (carved) continue;
                            cells.remove(index);
                        }
                    }
                }
            }
            if (!success) return false;
        }
        for (int aa = 0; aa < this.height; ++aa) {
            for (int bb = 0; bb < this.width; ++bb) {
                if (feature(this.grid[aa][bb]) != 99) continue;
                this.grid[aa][bb] = (this.grid[aa][bb] & 0xFF) | (0 << 8);
            }
        }
        ArrayList<Loc> deadEnds = new ArrayList<>();
        for (int aa = 0; aa < this.height; ++aa) {
            for (int bb = 0; bb < this.width; ++bb) {
                int cell = this.grid[aa][bb];
                int exits = ((cell & 1) != 0 ? 1 : 0) + ((cell & 2) != 0 ? 1 : 0)
                        + ((cell & 4) != 0 ? 1 : 0) + ((cell & 8) != 0 ? 1 : 0);
                if (exits != 1) continue;
                if (feature(cell) != 0) continue;
                deadEnds.add(new Loc(aa, bb));
            }
        }
        if (deadEnds.isEmpty()) return false;
        int r = this.rand.nextInt(deadEnds.size());
        Loc ll = deadEnds.get(r);
        this.grid[ll.x][ll.y] = (this.grid[ll.x][ll.y] & 0xFF) | (6 << 8);
        deadEnds.remove(r);
        if (!deadEnds.isEmpty()) {
            int count = 0;
            while (count < deadEnds.size() / 2) {
                int r2 = this.rand.nextInt(deadEnds.size());
                Loc ll2 = deadEnds.get(r2);
                if (feature(this.grid[ll2.x][ll2.y]) != 0) continue;
                this.grid[ll2.x][ll2.y] = (this.grid[ll2.x][ll2.y] & 0xFF) | ((7 + this.rand.nextInt(3)) << 8);
                deadEnds.remove(r2);
                ++count;
            }
        }
        for (int aa = 0; aa < this.height; ++aa) {
            for (int bb = 0; bb < this.width; ++bb) {
                int cell = this.grid[aa][bb];
                if (feature(cell) != 0) continue;
                // The oracle expression: "the cell carries a feature, or the cell has no exit
                // at all, or the roll of twenty five is not zero". A cell without a single
                // exit is skipped without drawing a random number.
                if (!hasExit(cell)) continue;
                if (this.rand.nextInt(25) != 0) continue;
                int marker;
                switch (this.rand.nextInt(8)) {
                    case 0: marker = 8; break;
                    case 1: marker = 10; break;
                    case 2: case 3: marker = 11; break;
                    case 4: case 5: marker = 12; break;
                    case 6: marker = 13; break;
                    default: marker = 14;
                }
                this.grid[aa][bb] = (cell & 0xFF) | (marker << 8);
            }
        }
        return true;
    }

    /** The feature marker of a packed cell. */
    private static int feature(int cell) {
        return (short) cell >> 8;
    }

    /** True when the cell has at least one exit. */
    private static boolean hasExit(int cell) {
        return (cell & (N | S | E | W)) != 0;
    }

    /** The oracle {@code Loc}: the row and the column of a cell. */
    private static final class Loc {
        final int x;
        final int y;

        Loc(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
