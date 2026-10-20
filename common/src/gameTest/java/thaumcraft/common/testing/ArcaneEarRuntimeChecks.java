package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockArcaneEar;
import thaumcraft.common.config.ThaumcraftArcaneEar;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.lib.ArcaneEarNotes;
import thaumcraft.common.tiles.TileSensor;

/** Real vanilla note playback hook, native sensor ticks and a final ordinary server-tick pulse; no client or saved-world fixture. */
public final class ArcaneEarRuntimeChecks {
    private ArcaneEarRuntimeChecks() {}
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> {
            try {
                check.contract(); check.notes(); check.persistence();
                check.ordinaryTicks();
            } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        });
    }

    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos ear, source, boundary, beyond;
        final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        TileSensor tile;
        boolean finished;
        Check(GameTestHelper test) {
            this.test = test; level = test.getLevel(); player = test.makeMockServerPlayerInLevel();
            ear = test.absolutePos(new BlockPos(3, 100, 3)); source = ear.east(3); boundary = ear.east(64); beyond = ear.east(65);
        }
        void set(BlockPos pos, BlockState state) {
            original.putIfAbsent(pos, level.getBlockState(pos));
            level.setBlock(pos, state, QUIET);
        }
        void prepare() {
            forced.addAll(GolemChunks.hold(level, List.of(ear, beyond)));
            for (BlockPos pos : List.of(ear, source, boundary, beyond)) {
                set(pos.below(), Blocks.DIRT.defaultBlockState());
                set(pos.above(), Blocks.AIR.defaultBlockState());
            }
            set(ear, ThaumcraftArcaneEar.ARCANE_EAR.get().defaultBlockState());
            tile = (TileSensor) level.getBlockEntity(ear);
            test.assertTrue(tile != null, "a real Arcane Ear tile is installed");
            set(ear.below(), Blocks.STONE.defaultBlockState());
            level.neighborChanged(ear, Blocks.STONE, ear.below());
            test.assertTrue(tile.getTone() == 1, "a real neighbour notification updates the instrument without a direct tile setter");
            set(ear, Blocks.AIR.defaultBlockState());
            set(ear.below(), Blocks.OAK_PLANKS.defaultBlockState());
            set(ear, ThaumcraftArcaneEar.ARCANE_EAR.get().defaultBlockState());
            tile = (TileSensor) level.getBlockEntity(ear);
            test.assertTrue(tile != null && tile.getTone() == 4, "placing the ear on wood initializes its bass instrument");
            set(ear.below(), Blocks.DIRT.defaultBlockState());
            level.neighborChanged(ear, Blocks.DIRT, ear.below());
            set(source, Blocks.NOTE_BLOCK.defaultBlockState());
        }
        void tick() { TileSensor.tick(level, ear, level.getBlockState(ear), tile); }
        void reset(int note, int tone) {
            ArcaneEarNotes.clear(level);
            var tag = new CompoundTag(); tag.putByte("note", (byte) note); tag.putByte("tone", (byte) tone);
            tile.loadWithComponents(tag, level.registryAccess());
            tick();
        }
        void play(BlockPos pos, int tone, int note) {
            set(pos, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.INSTRUMENT, ArcaneEarNotes.instrument(tone)).setValue(NoteBlock.NOTE, note));
            level.getBlockState(pos).attack(level, pos, player); // Real NoteBlock.playNote, not a direct queue grant.
        }
        void signal(int expected) {
            BlockState state = level.getBlockState(ear);
            for (Direction side : Direction.values()) {
                test.assertTrue(state.getSignal(level, ear, side) == expected && state.getDirectSignal(level, ear, side) == expected,
                        "strong and weak signal are " + expected + " on " + side);
            }
            test.assertTrue(state.getValue(BlockArcaneEar.POWERED) == (expected > 0), "the powered model follows the actual timer");
        }
        void contract() {
            var block = ThaumcraftArcaneEar.ARCANE_EAR.get();
            test.assertTrue(block.defaultBlockState().getDestroySpeed(level, ear) == 2.5F && block.getExplosionResistance() == 6.0F,
                    "original wooden-device hardness and converted resistance");
            var recipe = (ArcaneShapedRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/arcane_ear")).orElseThrow().value();
            ItemStack gold = new ItemStack(Items.GOLD_INGOT), iron = new ItemStack(Items.IRON_INGOT), redstone = new ItemStack(Items.REDSTONE);
            ItemStack brain = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "zombie_brain")));
            ItemStack wood = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "greatwood_planks")));
            var input = new ArcaneRecipeInput(List.of(gold, iron, gold, gold, brain, gold, wood, redstone, wood));
            test.assertTrue(recipe.matches(input, level) && recipe.research().equals("ARCANEEAR") && recipe.vis().equals(Map.of("aer", 10, "ordo", 10))
                            && recipe.assemble(input, level.registryAccess()).is(block.asItem()), "original recipe, research and exact vis costs");
            test.assertTrue(!recipe.matches(new ArcaneRecipeInput(List.of(gold, iron, gold, gold, new ItemStack(Items.ROTTEN_FLESH), gold, wood, redstone, wood)), level),
                    "rotten flesh does not replace the zombie brain");
            reset(24, 0);
            var hit = new BlockHitResult(Vec3.atCenterOf(ear), Direction.UP, ear, false);
            level.getBlockState(ear).useWithoutItem(level, player, hit);
            test.assertTrue(tile.getNote() == 0, "actual right click wraps note 24 to 0");
            for (int i = 0; i < 25; i++) level.getBlockState(ear).useWithoutItem(level, player, hit);
            test.assertTrue(tile.getNote() == 0, "twenty-five tuning clicks complete the whole pitch circle");
        }
        void notes() {
            List<Block> floors = List.of(Blocks.DIRT, Blocks.STONE, Blocks.SAND, Blocks.GLASS, Blocks.OAK_PLANKS);
            for (int tone = 0; tone < 5; tone++) {
                reset(7, tone);
                set(ear.below(), floors.get(tone).defaultBlockState()); tile.updateTone();
                test.assertTrue(tile.getTone() == tone, "original instrument " + tone + " from its real support block");
                play(source, tone, 7); tick();
                test.assertTrue(tile.getRedstoneTicks() == 10, "each original instrument triggers exactly ten ticks"); signal(15);
                ArcaneEarNotes.clear(level);
                for (int count = 0; count < 9; count++) tick();
                test.assertTrue(tile.getRedstoneTicks() == 1, "the pulse remains through its ninth decrement"); signal(15);
                tick(); signal(0);
            }
            set(ear.below(), Blocks.DIRT.defaultBlockState()); tile.updateTone(); reset(7, 0);
            play(source, 0, 8); tick(); signal(0);
            ArcaneEarNotes.clear(level); play(source, 1, 7); tick(); signal(0);
            ArcaneEarNotes.clear(level);
            set(source, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL).setValue(NoteBlock.NOTE, 7));
            level.getBlockState(source).attack(level, source, player); tick(); signal(0);
            ArcaneEarNotes.clear(level); play(boundary, 0, 7); tick(); signal(15);
            reset(7, 0); play(beyond, 0, 7); tick(); signal(0);

            reset(7, 0); set(source.above(), Blocks.STONE.defaultBlockState()); play(source, 0, 7); tick(); signal(0);
            set(source.above(), Blocks.AIR.defaultBlockState()); set(ear.above(), Blocks.STONE.defaultBlockState());
            play(source, 0, 7); tick(); signal(15); // Only feedback is blocked over the sensor, not reception.
            ArcaneEarNotes.clear(level);
            for (int count = 0; count < 6; count++) tick();
            play(source, 0, 7); tick();
            test.assertTrue(tile.getRedstoneTicks() == 10, "another matching note retriggers the full pulse");
            ArcaneEarNotes.clear(level); set(ear.above(), Blocks.AIR.defaultBlockState()); tile.triggerNote(true);
            test.assertTrue(!ArcaneEarNotes.matches(level, ear, 0, 7), "the ear's own preview never enters the vanilla-note queue");
            set(ear.above(), Blocks.AIR.defaultBlockState()); reset(7, 0);
        }
        void persistence() {
            var tag = new CompoundTag(); tag.putByte("note", (byte) 99); tag.putByte("tone", (byte) 4);
            tile.loadWithComponents(tag, level.registryAccess());
            test.assertTrue(tile.getNote() == 24 && tile.getTone() == 4 && tile.getRedstoneTicks() == 0, "NBT clamps high pitch and has no saved pulse");
            var restored = new TileSensor(ear, level.getBlockState(ear));
            restored.loadWithComponents(tile.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
            test.assertTrue(restored.getNote() == 24 && restored.getTone() == 4 && restored.getRedstoneTicks() == 0, "tile NBT round trip keeps only configuration");
            tag.putByte("note", (byte) -1); tag.putByte("tone", (byte) -1);
            restored.loadWithComponents(tag, level.registryAccess());
            test.assertTrue(restored.getNote() == 0 && restored.getTone() == 0, "low pitch clamps and an invalid instrument is safe");
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                ClientboundBlockEventPacket.STREAM_CODEC.encode(wire, new ClientboundBlockEventPacket(ear, ThaumcraftArcaneEar.ARCANE_EAR.get(), -1, 7));
                var decoded = ClientboundBlockEventPacket.STREAM_CODEC.decode(wire);
                test.assertTrue(decoded.getB0() == 255 && level.getBlockState(ear).triggerEvent(level, ear, decoded.getB0(), decoded.getB1()),
                        "the actual block-event codec's unsigned silent action is accepted for particle-only feedback");
            } finally { wire.release(); }
            reset(7, 0);
        }
        void ordinaryTicks() {
            // These checks leave the sensor entirely to the real server ticker; no manual sensor ticks after playback.
            set(source, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.INSTRUMENT, NoteBlockInstrument.HARP).setValue(NoteBlock.NOTE, 7));
            set(source.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            level.neighborChanged(source, Blocks.REDSTONE_BLOCK, source.east()); // A real rising edge, not a queue insertion.
            test.runAfterDelay(2, () -> {
                try {
                    test.assertTrue(tile.getRedstoneTicks() > 0 && tile.getRedstoneTicks() <= 10, "real server ticks heard the real vanilla note");
                    signal(15);
                    test.runAfterDelay(15, () -> {
                        try { signal(0); test.succeed(); } finally { finish(); }
                    });
                } catch (RuntimeException | Error failure) { finish(); throw failure; }
            });
        }
        void finish() {
            if (finished) return;
            finished = true;
            ArcaneEarNotes.clear(level);
            original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
}
