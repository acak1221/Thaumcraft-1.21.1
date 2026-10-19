package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

/** TC4 axial hex geometry, independent of Minecraft worlds and rendering. */
public final class HexUtils {
    private static final int[][] NEIGHBOURS = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};

    private HexUtils() {}

    public static int getDistance(Hex first, Hex second) {
        return (Math.abs(first.q - second.q) + Math.abs(first.r - second.r)
                + Math.abs(first.q + first.r - second.q - second.r)) / 2;
    }

    public static Hex getRoundedHex(double q, double cubicY) {
        return getRoundedCubicHex(q, cubicY, -q - cubicY).toHex();
    }

    public static CubicHex getRoundedCubicHex(double x, double y, double z) {
        int rx = (int) Math.round(x), ry = (int) Math.round(y), rz = (int) Math.round(z);
        double dx = Math.abs(rx - x), dy = Math.abs(ry - y), dz = Math.abs(rz - z);
        if (dx > dy && dx > dz) rx = -ry - rz;
        else if (dy > dz) ry = -rx - rz;
        else rz = -rx - ry;
        return new CubicHex(rx, ry, rz);
    }

    public static ArrayList<Hex> getRing(int radius) {
        if (radius < 0) throw new IllegalArgumentException("Negative hex radius");
        ArrayList<Hex> ring = new ArrayList<>();
        if (radius == 0) {
            ring.add(new Hex(0, 0));
            return ring;
        }
        Hex current = new Hex(0, 0);
        for (int i = 0; i < radius; i++) current = current.getNeighbour(4);
        for (int direction = 0; direction < 6; direction++) {
            for (int i = 0; i < radius; i++) {
                ring.add(current);
                current = current.getNeighbour(direction);
            }
        }
        return ring;
    }

    public static ArrayList<Hex> distributeRingRandomly(int radius, int entries, Random random) {
        ArrayList<Hex> ring = getRing(radius);
        if (entries < 0 || entries > ring.size()) throw new IllegalArgumentException("Too many fixed hexes for the ring");
        ArrayList<Hex> result = new ArrayList<>();
        // Quirk kept: TC4 4.2.3.5 HexUtils.distributeRingRandomly consumes a
        // nextInt(ring.size()) into unused "start", then begins pos at zero.
        // Keep the RNG advancement as well as the fixed endpoint orientation.
        random.nextInt(ring.size());
        if (entries == 0) return result;
        float spacing = (float) ring.size() / entries;
        float position = 0.0F;
        for (int i = 0; i < entries; i++) {
            result.add(ring.get(Math.round(position) % ring.size()));
            position += spacing;
        }
        return result;
    }

    public static HashMap<String, Hex> generateHexes(int radius) {
        if (radius < 0) throw new IllegalArgumentException("Negative hex radius");
        HashMap<String, Hex> result = new LinkedHashMap<>();
        Hex origin = new Hex(0, 0);
        result.put(origin.toString(), origin);
        for (int ring = 1; ring <= radius; ring++) {
            for (Hex hex : getRing(ring)) result.put(hex.toString(), hex);
        }
        return result;
    }

    public static final class Pixel {
        public final double x, y;
        public Pixel(double x, double y) { this.x = x; this.y = y; }
        public Hex toHex(int size) {
            if (size <= 0) throw new IllegalArgumentException("Hex size must be positive");
            double q = (2.0 / 3.0) * this.x / size;
            double cubicY = (Math.sqrt(3.0) * -this.y - this.x) / (3.0 * size);
            return getRoundedHex(q, cubicY);
        }
    }

    public static final class CubicHex {
        public final int x, y, z;
        public CubicHex(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        public Hex toHex() { return new Hex(this.x, this.z); }
    }

    public static final class Hex {
        public final int q, r;
        public Hex(int q, int r) { this.q = q; this.r = r; }
        // TC4 toHex reads x/z. Use the matching inverse here; the original
        // toCubicHex used q/r as x/y and failed its own round-trip invariant.
        public CubicHex toCubicHex() { return new CubicHex(this.q, -this.q - this.r, this.r); }
        public Pixel toPixel(int size) {
            return new Pixel(size * 1.5 * this.q, size * Math.sqrt(3.0) * (this.r + this.q / 2.0));
        }
        public Hex getNeighbour(int direction) {
            if (direction < 0 || direction >= 6) throw new IllegalArgumentException("Invalid hex direction");
            int[] delta = NEIGHBOURS[direction];
            return new Hex(this.q + delta[0], this.r + delta[1]);
        }
        @Override public boolean equals(Object other) { return other instanceof Hex hex && hex.q == this.q && hex.r == this.r; }
        @Override public int hashCode() { return 31 * this.q + this.r; }
        @Override public String toString() { return this.q + ":" + this.r; }
    }
}
