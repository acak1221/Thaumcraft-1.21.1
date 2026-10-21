package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import thaumcraft.common.blocks.BlockEldritchNothing;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.FamilyVariant;
import thaumcraft.common.tiles.TileEldritchAltar;
import thaumcraft.common.tiles.TileEldritchNothing;
import thaumcraft.common.tiles.TileWardingStone;

/**
 * What a chunk does with the {@code DUMMY} block entity marker that {@code WorldGenRegion#setBlock} stores for
 * every state of an {@code EntityBlock} while a chunk is generated.
 *
 * <p>The cosmetic stones, the eldritch blocks and the eldritch nothing carry a block entity on some variants
 * only (the original decided per metadata, 1.21.1 decides per block). The chunk built here is the one vanilla
 * builds from a generated proto chunk: the state is stored, the marker is pending, and asking for the block
 * entity promotes it. The warning that vanilla writes when the block hands out no entity was written once per
 * placed block of the totems, the mounds and the Outer Lands.
 */
public final class BlockEntityMarkerRuntimeChecks {
    private static final String PROMOTION_WARNING = "Tried to load a block entity for block";
    private static final BlockPos ORIGIN = new BlockPos(480_000, 64, 480_000);

    private BlockEntityMarkerRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        try (VanillaLogCapture log = new VanillaLogCapture()) {
            // States whose block is an EntityBlock but which have no block entity: nothing to create, nothing to say.
            int index = 0;
            for (BlockState state : withoutBlockEntity()) {
                BlockPos pos = ORIGIN.offset(index++ * 16, 0, 0);
                BlockEntity entity = promote(level, pos, state, dummy(pos));
                test.assertTrue(entity == null, state + " has no block entity, so promoting its marker yields none");
            }
            test.assertTrue(log.containing(PROMOTION_WARNING).isEmpty(),
                    "promoting the marker of a state without a block entity writes no warning, " + log.sample(PROMOTION_WARNING));

            // States that do have one still get it from the unchanged vanilla promotion.
            BlockPos warding = ORIGIN.offset(0, 0, 16);
            test.assertTrue(promote(level, warding, withVariant(ThaumcraftEldritchBlocks.COSMETIC_SOLID.get().defaultBlockState(),
                    EldritchBlockRules.COSMETIC_WARDING_STONE), dummy(warding)) instanceof TileWardingStone,
                    "the warding stone gets its block entity from the marker");
            BlockPos altar = ORIGIN.offset(16, 0, 16);
            test.assertTrue(promote(level, altar, withVariant(ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(),
                    EldritchBlockRules.ELDRITCH_ALTAR), dummy(altar)) instanceof TileEldritchAltar,
                    "the eldritch altar gets its block entity from the marker");
            BlockPos exposed = ORIGIN.offset(32, 0, 16);
            test.assertTrue(promote(level, exposed, ThaumcraftEldritchBlocks.ELDRITCH_NOTHING.get().defaultBlockState()
                    .setValue(BlockEldritchNothing.EXPOSED, 1), dummy(exposed)) instanceof TileEldritchNothing,
                    "the exposed eldritch nothing gets its block entity from the marker");
            test.assertTrue(log.containing(PROMOTION_WARNING).isEmpty(),
                    "promoting the marker of a state with a block entity writes no warning, " + log.sample(PROMOTION_WARNING));

            // The warning is not switched off for everything: a saved entity that does not load, and a block that is
            // not marked, still say so.
            BlockPos unknown = ORIGIN.offset(0, 0, 32);
            CompoundTag saved = dummy(unknown);
            saved.putString("id", "thaumcraft:no_such_block_entity");
            promote(level, unknown, withVariant(ThaumcraftEldritchBlocks.COSMETIC_SOLID_TOUGH.get().defaultBlockState(), 1), saved);
            test.assertTrue(!log.containing(PROMOTION_WARNING).isEmpty(),
                    "a saved block entity that cannot be loaded is still reported on a marked block");
            int afterSaved = log.containing(PROMOTION_WARNING).size();
            BlockPos plain = ORIGIN.offset(16, 0, 32);
            promote(level, plain, Blocks.STONE.defaultBlockState(), dummy(plain));
            test.assertTrue(log.containing(PROMOTION_WARNING).size() > afterSaved,
                    "the marker of a block that is not marked and is no entity block is still reported");
        }
        test.succeed();
    }

    /** The states a worldgen structure places whose block has no block entity for them. */
    private static java.util.List<BlockState> withoutBlockEntity() {
        BlockState tough = ThaumcraftEldritchBlocks.COSMETIC_SOLID_TOUGH.get().defaultBlockState();
        BlockState ordinary = ThaumcraftEldritchBlocks.COSMETIC_SOLID.get().defaultBlockState();
        BlockState eldritch = ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState();
        return java.util.List.of(
                withVariant(tough, EldritchBlockRules.COSMETIC_OBSIDIAN_TILE),
                withVariant(tough, EldritchBlockRules.COSMETIC_OBSIDIAN_TOTEM_BASE),
                withVariant(ordinary, EldritchBlockRules.COSMETIC_ELDRITCH_TILE),
                withVariant(ordinary, EldritchBlockRules.COSMETIC_ELDRITCH_TILE_ALT),
                withVariant(eldritch, EldritchBlockRules.ELDRITCH_OBELISK_MIDDLE),
                ThaumcraftEldritchBlocks.ELDRITCH_NOTHING.get().defaultBlockState().setValue(BlockEldritchNothing.EXPOSED, 0));
    }

    private static BlockState withVariant(BlockState state, int variant) { return FamilyVariant.with(state, variant); }

    private static CompoundTag dummy(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("id", "DUMMY");
        return tag;
    }

    /** The block entity that a chunk generated with the state and the pending marker hands out. */
    private static BlockEntity promote(ServerLevel level, BlockPos pos, BlockState state, CompoundTag pending) {
        ProtoChunk proto = new ProtoChunk(new ChunkPos(pos), UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        proto.setBlockState(pos, state, false);
        proto.setBlockEntityNbt(pending);
        LevelChunk chunk = new LevelChunk(level, proto, ignored -> {});
        return chunk.getBlockEntity(pos);
    }
}
