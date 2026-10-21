package thaumcraft.common.lib.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Exact port of the original TC4 1.7.10 labyrinth generator.
 *
 * Oracle: {@code thaumcraft/common/lib/world/dim/MazeGenerator.java:15-444} and
 * {@code thaumcraft/common/lib/world/dim/Cell.java:8-56}. The algorithm, the
 * {@link java.util.Random} draw order and the cell bit layout are preserved so
 * that a given (width, height, seed) produces the identical labyrinth.
 *
 * Direction bits: N=1, S=2, E=4, W=8 live in bits 0-3 of a cell, the feature
 * marker in bits 8-15 (oracle {@code Cell.unpack}/{@code Cell.pack}).
 */
public final class MazeGenerator {
    public static final int N = 1;
    public static final int S = 2;
    public static final int E = 4;
    public static final int W = 8;
    public static final int A = 16;
    public static final int B = 32;

    /** Oracle MazeGenerator.getOPP:15-31; -99 for a non direction. */
    public static int opposite(int in) {
        return switch (in) {
            case N -> S;
            case S -> N;
            case E -> W;
            case W -> E;
            default -> -99;
        };
    }

    /** Oracle MazeGenerator.getDX:33-48. */
    public static int dx(int in) {
        return switch (in) {
            case N, S -> 0;
            case E -> 1;
            case W -> -1;
            default -> -99;
        };
    }

    /** Oracle MazeGenerator.getDY:50-67. */
    public static int dy(int in) {
        return switch (in) {
            case N -> -1;
            case S -> 1;
            case E, W -> 0;
            default -> -99;
        };
    }

    /** Oracle Cell:8-56. north/south/east/west are the low bits, feature the high byte. */
    public static final class Cell {
        public boolean north;
        public boolean south;
        public boolean east;
        public boolean west;
        public boolean above;
        public boolean below;
        public byte feature;

        public Cell() {
        }

        public Cell(short data) {
            this.north = bit(data, 0);
            this.south = bit(data, 1);
            this.east = bit(data, 2);
            this.west = bit(data, 3);
            this.above = bit(data, 4);
            this.below = bit(data, 5);
            this.feature = (byte) (data >> 8);
        }

        public short pack() {
            int out = 0;
            if (this.north) out |= 1;
            if (this.south) out |= 2;
            if (this.east) out |= 4;
            if (this.west) out |= 8;
            if (this.above) out |= 16;
            if (this.below) out |= 32;
            return (short) (out | (this.feature << 8));
        }

        /** Number of open exits; the oracle uses it to find dead ends. */
        public int exits() {
            return (this.north ? 1 : 0) + (this.south ? 1 : 0) + (this.east ? 1 : 0) + (this.west ? 1 : 0);
        }

        private static boolean bit(short data, int index) {
            return ((data >> index) & 1) != 0;
        }
    }

    private final int width;
    private final int height;
    private final long seed;
    private final Random rand;
    public final int[][] grid;

    public MazeGenerator(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.rand = new Random(seed);
        this.grid = new int[height][width];
        for (int y = 0; y < height; ++y) for (int x = 0; x < width; ++x) this.grid[y][x] = 0;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public long seed() {
        return this.seed;
    }

    /**
     * Oracle MazeGenerator.generate:95-364. Returns false when the layout could
     * not be completed; the oracle then retries with the next seed.
     */
    public boolean generate() {
        int bx = 0;
        int by = 0;
        switch (this.rand.nextInt(4)) {
            case 0 -> {
                bx = 0;
                by = 0;
            }
            case 1 -> {
                bx = this.width - 2;
                by = this.height - 2;
            }
            case 2 -> {
                bx = this.width - 2;
                by = 0;
            }
            default -> {
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
            int index = nextIndex(cells.size());
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
        for (int ax = 0; ax < 2 && !connected; ++ax) {
            for (int ay = 0; ay < 2 && !connected; ++ay) {
                for (int dir : directions) {
                    int nx = bx + ax + dx(dir);
                    int ny = by + ay + dy(dir);
                    if (0 >= nx || nx >= this.width - 1 || 0 >= ny || ny >= this.height - 1) continue;
                    if (this.grid[ny][nx] <= 0) continue;
                    if (new Cell((short) this.grid[ny][nx]).feature != 0) continue;
                    this.grid[ny][nx] |= opposite(dir);
                    this.grid[by + ay][bx + ax] |= dir;
                    connected = true;
                    break;
                }
            }
        }
        if (!connected) {
            List<Integer> directions2 = Arrays.asList(N, S, E, W);
            Collections.shuffle(directions2, this.rand);
            success = false;
            // Oracle "block25": a successful reconnection abandons the whole quadrant/direction sweep.
            quadrantLoop:
            for (int ax = 0; ax < 2; ++ax) {
                for (int ay = 0; ay < 2; ++ay) {
                    for (int dir2 : directions2) {
                        int qx = bx + ax + dx(dir2);
                        int qy = by + ay + dy(dir2);
                        if (0 >= qx || qx >= this.width - 1 || 0 >= qy || qy >= this.height - 1) continue;
                        if (this.grid[qy][qx] != 0) continue;
                        cells.add(new Loc(qx, qy));
                        while (!cells.isEmpty()) {
                            int index = nextIndex(cells.size());
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
                                } else if (new Cell((short) this.grid[ny][nx]).feature == 0) {
                                    this.grid[y][x] |= dir;
                                    this.grid[ny][nx] |= opposite(dir);
                                    this.grid[qy][qx] |= opposite(dir2);
                                    this.grid[by + ay][bx + ax] |= dir2;
                                    success = true;
                                    break quadrantLoop;
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
                Cell c = new Cell((short) this.grid[aa][bb]);
                if (c.feature != 99) continue;
                c.feature = 0;
                this.grid[aa][bb] = c.pack();
            }
        }
        ArrayList<Loc> deadEnds = new ArrayList<>();
        for (int aa = 0; aa < this.height; ++aa) {
            for (int bb = 0; bb < this.width; ++bb) {
                Cell c = new Cell((short) this.grid[aa][bb]);
                if (c.exits() != 1 || c.feature != 0) continue;
                deadEnds.add(new Loc(aa, bb));
            }
        }
        if (deadEnds.isEmpty()) return false;
        int r = this.rand.nextInt(deadEnds.size());
        Loc ll = deadEnds.get(r);
        Cell c = new Cell((short) this.grid[ll.x][ll.y]);
        c.feature = (byte) 6;
        this.grid[ll.x][ll.y] = c.pack();
        deadEnds.remove(r);
        if (!deadEnds.isEmpty()) {
            int count = 0;
            while (count < deadEnds.size() / 2) {
                int r2 = this.rand.nextInt(deadEnds.size());
                Loc ll2 = deadEnds.get(r2);
                Cell c2 = new Cell((short) this.grid[ll2.x][ll2.y]);
                if (c2.feature != 0) continue;
                c2.feature = (byte) (7 + this.rand.nextInt(3));
                this.grid[ll2.x][ll2.y] = c2.pack();
                deadEnds.remove(r2);
                ++count;
            }
        }
        for (int aa = 0; aa < this.height; ++aa) {
            for (int bb = 0; bb < this.width; ++bb) {
                c = new Cell((short) this.grid[aa][bb]);
                // Oracle MazeGenerator:332 - the skip condition is one single expression:
                // "the cell already carries a feature, or the cell has no exit at all, or the
                // roll of twenty five does not hit zero". The short circuit evaluation of the
                // original means that a cell without a single exit is skipped without drawing
                // a random number, while a cell with an exit always draws the twenty five.
                if (c.feature != 0) continue;
                if (!c.north && !c.south && !c.west && !c.east) continue;
                if (this.rand.nextInt(25) != 0) continue;
                switch (this.rand.nextInt(8)) {
                    case 0 -> c.feature = (byte) 8;
                    case 1 -> c.feature = (byte) 10;
                    case 2, 3 -> c.feature = (byte) 11;
                    case 4, 5 -> c.feature = (byte) 12;
                    case 6 -> c.feature = (byte) 13;
                    default -> c.feature = (byte) 14;
                }
                this.grid[aa][bb] = c.pack();
            }
        }
        return true;
    }

    /** Oracle MazeGenerator.getNextIndex:366-375: 45% newest, 45% random, 10% oldest. */
    private int nextIndex(int ceil) {
        float r = this.rand.nextFloat();
        if (r <= 0.45f) return ceil - 1;
        if (r <= 0.9f) return this.rand.nextInt(ceil);
        return 0;
    }

    /** The oracle's private Loc record; grid coordinates, not chunk coordinates. */
    private static final class Loc {
        final int x;
        final int y;

        Loc(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
