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
