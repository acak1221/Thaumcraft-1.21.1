package thaumcraft.common.items.wands;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IArchitect;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftArchitect;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;

/** TC4 WandManager's areax/areay/areaz/aread and bounded connected Architect traversal, without recursive world scans. */
public final class WandArchitect {
    public static final int MAX_RADIUS = 13; // Trade's base 3 + five possible Enlarge levels * 2, an absolute data ceiling.
    private static final Direction[] DIRECTIONS = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private WandArchitect() {}

    public record Area(int x, int y, int z, int axis) {
        public static final Codec<Area> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, MAX_RADIUS).fieldOf("x").forGetter(Area::x),
                Codec.intRange(0, MAX_RADIUS).fieldOf("y").forGetter(Area::y),
                Codec.intRange(0, MAX_RADIUS).fieldOf("z").forGetter(Area::z),
                Codec.intRange(0, 3).fieldOf("axis").forGetter(Area::axis)).apply(instance, Area::new));
        public static final StreamCodec<ByteBuf, Area> STREAM_CODEC = new StreamCodec<>() {
            @Override public Area decode(ByteBuf buffer) {
                try { return new Area(buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte()); }
                catch (IllegalArgumentException invalid) { throw new DecoderException("Invalid Architect area", invalid); }
            }
            @Override public void encode(ByteBuf buffer, Area area) { buffer.writeByte(area.x); buffer.writeByte(area.y); buffer.writeByte(area.z); buffer.writeByte(area.axis); }
        };
        public Area {
            if (x < 0 || x > MAX_RADIUS || y < 0 || y > MAX_RADIUS || z < 0 || z > MAX_RADIUS || axis < 0 || axis > 3)
                throw new IllegalArgumentException("Architect dimensions exceed the bounded native range");
        }
        /** G changes the selected size; Shift-G selects the next dimension, as original WandManager.toggleMisc. */
        public Area next(int maximum, int axes, boolean selectAxis) {
            if (maximum < 0 || maximum > MAX_RADIUS || axes < 1 || axes > 3) throw new IllegalArgumentException("Invalid Architect focus limits");
            int selected = axis > axes ? 0 : axis;
            int a = Math.min(x, maximum), b = Math.min(y, maximum), c = Math.min(z, maximum);
            if (selectAxis) return new Area(a, b, c, (selected + 1) % (axes + 1));
            if (selected == 0 || selected == 1) a = (a + 1) % (maximum + 1);
            if (selected == 0 || selected == 2) c = (c + 1) % (maximum + 1);
            if (selected == 0 || selected == 3) b = (b + 1) % (maximum + 1);
            return new Area(a, b, c, selected);
        }
    }

    public static boolean enabled(ItemStack wand) {
        ItemStack focus = FocusData.get(wand);
        return focus.getItem() instanceof IArchitect && FocusUpgradeData.level(focus, FocusUpgradeType.architect) > 0;
    }
    /** Missing dimensions default to the focus maximum on EACH axis (the original's explicit default), not zero. */
    public static Area area(ItemStack wand, ItemStack focus) {
        if (!(focus.getItem() instanceof IArchitect architect)) return new Area(0, 0, 0, 0);
        int maximum = architect.getMaxAreaSize(focus);
        if (maximum < 0 || maximum > MAX_RADIUS) throw new IllegalArgumentException("Unsupported Architect radius");
        Area saved = wand.get(ThaumcraftArchitect.AREA.get());
        return saved == null ? new Area(maximum, maximum, maximum, 0)
                : new Area(Math.min(saved.x, maximum), Math.min(saved.y, maximum), Math.min(saved.z, maximum),
                        saved.axis <= architect.getAxisModes() ? saved.axis : 0);
    }

    /** The original side-specific axis permutation: X is vertical on walls, Y is depth, Z is the other in-plane radius. */
    public static boolean within(BlockPos at, BlockPos anchor, Direction face, Area radii, boolean planar) {
        int dx = Math.abs(at.getX() - anchor.getX()), dy = Math.abs(at.getY() - anchor.getY()), dz = Math.abs(at.getZ() - anchor.getZ());
        return switch (face.getAxis()) {
            case Y -> dx <= radii.x && dz <= radii.z && (planar ? dy == 0 : dy <= radii.y);
            case Z -> dy <= radii.x && dx <= radii.z && (planar ? dz == 0 : dz <= radii.y);
            case X -> dy <= radii.x && dz <= radii.z && (planar ? dx == 0 : dx <= radii.y);
        };
    }
    public static List<BlockPos> planarBlocks(BlockPos anchor, Direction face, Area radii, Predicate<BlockPos> eligible) {
        return connected(anchor, face, radii, true, eligible);
    }

    /**
     * The axes the client draws for the trade focus.
     *
     * <p>Oracle {@code ItemFocusTrade:301-319} - the trade focus draws two axes per face and the
     * selected dimension of the area picks the first or the second of them; the dimension zero
     * draws both.</p>
     *
     * @param area the area of the wand
     * @param side the face of the anchor, the ordinal of the original direction
     * @param axis the axis of the preview
     * @return whether that axis is part of the preview
     */
    public static boolean showAxis(Area area, int side, thaumcraft.api.IArchitect.EnumAxis axis) {
        int dimension = area == null ? 0 : area.axis();
        return switch (side) {
            case 0, 1 -> (axis == thaumcraft.api.IArchitect.EnumAxis.X && (dimension == 0 || dimension == 1))
                    || (axis == thaumcraft.api.IArchitect.EnumAxis.Z && (dimension == 0 || dimension == 2));
            case 2, 3 -> (axis == thaumcraft.api.IArchitect.EnumAxis.Y && (dimension == 0 || dimension == 1))
                    || (axis == thaumcraft.api.IArchitect.EnumAxis.X && (dimension == 0 || dimension == 2));
            case 4, 5 -> (axis == thaumcraft.api.IArchitect.EnumAxis.Y && (dimension == 0 || dimension == 1))
                    || (axis == thaumcraft.api.IArchitect.EnumAxis.Z && (dimension == 0 || dimension == 2));
            default -> false;
        };
    }

    /**
     * The axes the client draws for the warding focus.
     *
     * <p>Oracle {@code ItemFocusWarding:288-309} - the warding focus draws all three axes when
     * the dimension of the area is zero and otherwise exactly the axis of the selected dimension
     * for the face of the anchor.</p>
     *
     * @param area the area of the wand
     * @param side the face of the anchor, the ordinal of the original direction
     * @param axis the axis of the preview
     * @return whether that axis is part of the preview
     */
    public static boolean showAxisWarding(Area area, int side, thaumcraft.api.IArchitect.EnumAxis axis) {
        int dimension = area == null ? 0 : area.axis();
        if (dimension == 0) return true;
        return switch (side) {
            case 0, 1 -> (axis == thaumcraft.api.IArchitect.EnumAxis.X && dimension == 1)
                    || (axis == thaumcraft.api.IArchitect.EnumAxis.Z && dimension == 2)
                    || (axis == thaumcraft.api.IArchitect.EnumAxis.Y && dimension == 3);
            case 2, 3 -> (axis == thaumcraft.api.IArchitect.EnumAxis.Y && dimension == 1)
                    || (axis == thaumcraft.api.IArchitect.EnumAxis.X && dimension == 2)
                    || (axis == thaumcraft.api.IArchitect.EnumAxis.Z && dimension == 3);
            case 4, 5 -> (axis == thaumcraft.api.IArchitect.EnumAxis.Y && dimension == 1)
                    || (axis == thaumcraft.api.IArchitect.EnumAxis.Z && dimension == 2)
                    || (axis == thaumcraft.api.IArchitect.EnumAxis.X && dimension == 3);
            default -> false;
        };
    }

    /**
     * The face of the anchor of the original direction ordinal.
     *
     * <p>Oracle {@code ItemFocusTrade:301-319} and {@code ItemFocusWarding:288-309} switch on the
     * ordinal of the direction of 1.7.10; the native directions keep that order.</p>
     *
     * @param side the ordinal of the original direction
     * @return the native direction, or null when the ordinal is not a face
     */
    public static Direction face(int side) {
        Direction[] directions = Direction.values();
        return side < 0 || side >= directions.length ? null : directions[side];
    }

    /** Bounded DFS, original DOWN/UP/NORTH/SOUTH/WEST/EAST order; an ineligible cell stops traversal in that direction. */
    public static List<BlockPos> connected(BlockPos anchor, Direction face, Area radii, boolean planar, Predicate<BlockPos> eligible) {
        var out = new ArrayList<BlockPos>();
        var seen = new HashSet<BlockPos>();
        var todo = new ArrayDeque<BlockPos>();
        todo.push(anchor.immutable());
        while (!todo.isEmpty()) {
            BlockPos cell = todo.pop();
            if (!seen.add(cell) || !within(cell, anchor, face, radii, planar) || !eligible.test(cell)) continue;
            out.add(cell);
            for (int index = DIRECTIONS.length - 1; index >= 0; index--) {
                Direction direction = DIRECTIONS[index];
                if (planar && direction.getAxis() == face.getAxis()) continue;
                todo.push(cell.relative(direction));
            }
        }
        return List.copyOf(out);
    }
}
