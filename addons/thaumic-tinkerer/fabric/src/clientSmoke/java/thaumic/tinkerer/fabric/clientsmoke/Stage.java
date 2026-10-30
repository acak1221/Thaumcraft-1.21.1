package thaumic.tinkerer.fabric.clientsmoke;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The fixed place of the pictures: a smooth-stone floor on the flat ground, a row of cells at 3-block spacing, a camera in
 * front. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>Every picture is taken at the same place: the stage is cleared, the next cells are put on it, the player is teleported
 * to the camera. Labels are invisible armor stands with a visible custom name, which the game draws as a name tag above the
 * cell. All of it runs on the server thread ({@link SmokeRun#onServer}).</p>
 */
final class Stage {
    /** Distance between two cells of a row, in blocks. */
    static final int SPACING = 2;
    static final int CELLS_PER_ROW = 6;
    /** The row runs along x from {@code X0}; z is the row line; the camera stands at {@code z + distance}. */
    static final int X0 = -9;
    static final int Z = -14;

    /** The y of the first free block above the flat ground, read once before anything stands on it (a block raises the height map). */
    private static int ground = Integer.MIN_VALUE;

    private Stage() {}

    static void reset() {
        ground = Integer.MIN_VALUE;
    }

    static int groundY(ServerLevel level) {
        if (ground == Integer.MIN_VALUE) ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, X0, Z);
        return ground;
    }

    static BlockPos cell(ServerLevel level, int index) {
        return new BlockPos(X0 + index * SPACING, groundY(level), Z);
    }

    /** Smooth stone under the whole stage (the grass of the flat world hides pale blocks and gases). */
    static void floor(ServerLevel level) {
        int y = groundY(level) - 1;
        for (int x = X0 - 8; x <= X0 + 16; x++) {
            for (int z = Z - 6; z <= Z + 12; z++) level.setBlock(new BlockPos(x, y, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2 | 16);
        }
    }

    /** Air in the volume of the cells, the player's space and the labels. */
    static void clear(MinecraftServer server, ServerLevel level) {
        int y0 = groundY(level);
        for (int x = X0 - 8; x <= X0 + 16; x++) {
            for (int z = Z - 6; z <= Z + 12; z++) {
                for (int y = y0; y <= y0 + 7; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
                }
            }
        }
        command(server, "kill @e[type=minecraft:armor_stand]");
        command(server, "kill @e[type=minecraft:item]");
    }

    static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2 | 16 | 32);
    }

    /** A name tag over a cell; {@code level} 0..2 staggers the height so that long names do not overlap. */
    static void label(MinecraftServer server, BlockPos pos, String text, int level) {
        String json = Component.Serializer.toJson(Component.literal(text), server.registryAccess()).replace("\\", "\\\\").replace("'", "");
        command(server, String.format("summon minecraft:armor_stand %.2f %.2f %.2f {Invisible:1b,NoGravity:1b,Marker:1b,CustomNameVisible:1b,CustomName:'%s'}",
                pos.getX() + 0.5, pos.getY() + 1.15 + 0.38 * level, pos.getZ() + 0.5, json));
    }

    /** The player on the camera spot looking at the middle of {@code cells} cells of a row. */
    static void camera(ServerPlayer player, int cells, double distanceExtra) {
        double width = SPACING * cells + 1;
        // vertical field of view 70 degrees at 16:9: half of the horizontal angle is about 51 degrees
        double distance = Math.max(5.0, width / 2 / 1.2447 + 1.5) + distanceExtra;
        double centerX = X0 + (cells - 1) * SPACING / 2.0 + 0.5;
        double eyeAbove = 2.6;
        double y = groundY(player.serverLevel()) + eyeAbove - player.getEyeHeight();
        float pitch = (float) Math.toDegrees(Math.atan2(eyeAbove - 0.8, distance));
        player.connection.teleport(centerX, y, Z + 0.5 + distance, 180.0F, pitch);
    }

    static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    static String describe(BlockState state, BlockState defaults) {
        StringBuilder text = new StringBuilder();
        for (var property : state.getProperties()) {
            if (state.getValue(property).equals(defaults.getValue(property))) continue;
            if (text.length() > 0) text.append(',');
            text.append(property.getName()).append('=').append(net.minecraft.Util.getPropertyName(property, state.getValue(property)));
        }
        return text.toString();
    }

    /**
     * Up to {@code cap} representative states: the default and, for every property, its other values; a property with more than
     * three values contributes its first, middle and last only (the age of a fire: 0, 7, 15).
     */
    static List<BlockState> representatives(net.minecraft.world.level.block.Block block, int cap) {
        BlockState defaults = block.defaultBlockState();
        java.util.LinkedHashSet<BlockState> all = new java.util.LinkedHashSet<>();
        all.add(defaults);
        for (var property : defaults.getProperties()) {
            List<Comparable<?>> values = new java.util.ArrayList<>(property.getPossibleValues());
            if (values.size() > 3) values = List.of(values.get(0), values.get(values.size() / 2), values.get(values.size() - 1));
            for (var value : values) all.add(set(defaults, property, value));
        }
        List<BlockState> list = new java.util.ArrayList<>(all);
        if (list.size() <= cap) return list;
        // keep the default, then spread the rest evenly over the list
        List<BlockState> chosen = new java.util.ArrayList<>();
        chosen.add(list.get(0));
        for (int i = 1; i < cap; i++) chosen.add(list.get((int) ((long) i * (list.size() - 1) / (cap - 1))));
        return chosen;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static BlockState set(BlockState state, net.minecraft.world.level.block.state.properties.Property property, Comparable value) {
        return state.setValue(property, value);
    }
}
