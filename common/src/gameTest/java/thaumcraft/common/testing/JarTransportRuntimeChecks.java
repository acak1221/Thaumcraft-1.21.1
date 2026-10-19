package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;

/** Real registered endpoints/tickers; supplied stock is counted exactly after every endpoint tick. */
public final class JarTransportRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private JarTransportRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos jarPos = test.absolutePos(new BlockPos(3, 2, 3)), tubePos = jarPos.above(), sourcePos = tubePos.east();
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, BlockEntity> originals = new LinkedHashMap<>();
        // The three endpoints and their immediate neighbor halo only. Detach
        // originals before replacement, retaining inventories and transient state.
        for (BlockPos mutable : BlockPos.betweenClosed(jarPos.offset(-1, -1, -1), jarPos.offset(2, 2, 1))) {
            BlockPos pos = mutable.immutable();
            states.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) originals.put(pos, level.getBlockEntity(pos));
        }
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var position = player.position();
        var motion = player.getDeltaMovement();
        var gameType = player.gameMode.getGameModeForPlayer();
        var abilities = new CompoundTag();
        player.getAbilities().addSaveData(abilities);
        int selected = player.getInventory().selected;
        boolean sneaking = player.isShiftKeyDown();
        try {
            for (BlockPos pos : originals.keySet()) level.removeBlockEntity(pos);
            for (BlockPos pos : states.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(Vec3.atCenterOf(jarPos).add(0, 0, 2));
            level.setBlock(jarPos, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            level.setBlock(tubePos, ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), QUIET);
            level.setBlock(sourcePos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), QUIET);
            var jar = (TileJar) level.getBlockEntity(jarPos);
            var source = (TileAlembic) level.getBlockEntity(sourcePos);
            var tube = (IEssentiaTransport) level.getBlockEntity(tubePos);
            test.assertTrue(jar instanceof IAspectSource && !((BlockEntity) source instanceof IAspectSource)
                    && !(level.getBlockEntity(tubePos) instanceof IAspectSource), "only the jar implements TC4's source marker");
            for (Direction face : Direction.values()) test.assertTrue(jar.isConnectable(face) == (face == Direction.UP)
                    && jar.canInputFrom(face) == (face == Direction.UP) && jar.canOutputTo(face) == (face == Direction.UP), "jar top-only transport: " + face);
            test.assertTrue(!jar.isConnectable(null) && !jar.canInputFrom(null) && !jar.canOutputTo(null)
                    && jar.getSuctionAmount(null) == 32 && jar.getMinimumSuction() == 32 && jar.renderExtendedTube(), "native null/UNKNOWN is query-only, with original unlabelled suction");
            for (BlockPos pos : new BlockPos[] {jarPos, tubePos, sourcePos}) {
                var endpoint = (EssentiaStorageEndpoint) level.getBlockEntity(pos);
                boolean rejected = false;
                try { endpoint.setEssentiaStorageRaw(Aspect.FIRE, 1); }
                catch (IllegalStateException expected) { rejected = true; }
                test.assertTrue(rejected && endpoint.getStoredEssentiaAmount() == 0, "raw storage refuses calls outside a transaction: " + pos);
            }

            source.setFilter(Aspect.WATER);
            test.assertTrue(source.addToContainer(Aspect.WATER, 32) == 0 && jar.setFilter(Aspect.WATER), "supplied stock is exactly thirty-two and a label creates no units");
            wrongPhial(test, player, jar, Aspect.FIRE);
            test.assertTrue(jar.getAmount() == 0 && jar.getSuctionType(null) == Aspect.WATER
                    && jar.getSuctionAmount(null) == 64 && jar.getMinimumSuction() == 64, "empty labelled jar has typed suction/minimum64");
            test.assertTrue(EssentiaTransfers.pullOne(level.getBlockEntity(tubePos), Direction.EAST, Aspect.WATER) == 1,
                    "shared storage transaction moves one actual alembic unit into the real tube without imposing caller pressure policy");
            jar.setFilter(Aspect.FIRE);
            ticks(test, jarPos, tubePos, sourcePos, 5, false, 32);
            test.assertTrue(jar.getAmount() == 0 && tube.getEssentiaAmount(null) == 1, "wrong filter cannot drain or erase a tube's buffered aspect");
            jar.setFilter(Aspect.WATER);
            tube.setSuction(Aspect.WATER, 64);
            ticks(test, jarPos, tubePos, sourcePos, 5, false, 32);
            test.assertTrue(jar.getAmount() == 0, "equal source suction is not enough to pull");
            tube.setSuction(Aspect.WATER, 63);
            ticks(test, jarPos, tubePos, sourcePos, 4, false, 32);
            test.assertTrue(jar.getAmount() == 0 && tube.getEssentiaAmount(null) == 1, "four jar ticks cannot advance a five-tick transfer");
            ticks(test, jarPos, tubePos, sourcePos, 1, false, 32);
            test.assertTrue(jar.getAmount() == 1 && tube.getEssentiaAmount(null) == 0 && source.getAmount() == 31,
                    "the fifth jar tick moves exactly one, not eight or the whole source");
            jar.setFilter(null);
            test.assertTrue(jar.doesContainerAccept(Aspect.FIRE), "unlabelled acceptance is not a contents-type test");
            wrongPhial(test, player, jar, Aspect.FIRE);
            jar.setFilter(Aspect.WATER);
            test.assertTrue(jar.takeEssentia(Aspect.WATER, 1, Direction.DOWN) == 0
                    && jar.takeEssentia(Aspect.WATER, 1, null) == 0 && jar.getAmount() == 1, "blocked and UNKNOWN output never remove contents");
            var before = jar.saveWithoutMetadata(level.registryAccess());
            jar.setAspects(new AspectList().add(Aspect.FIRE, 64));
            test.assertTrue(before.equals(jar.saveWithoutMetadata(level.registryAccess())) && jar.containerContains(Aspect.WATER) == 0
                    && !jar.takeFromContainer(new AspectList().add(Aspect.WATER, 1))
                    && jar.doesContainerContain(new AspectList().add(Aspect.FIRE, 99).add(Aspect.WATER, 0)), "original no-op setAspects, zero containerContains, false list-take and type-intersection list-containment");
            test.assertTrue(jar.takeFromContainer(Aspect.WATER, 0) && !jar.takeFromContainer(Aspect.WATER, -1)
                    && jar.addToContainer(Aspect.WATER, -1) == -1 && jar.getAmount() == 1, "matching zero take is preserved; negative requests cannot duplicate essentia");

            // Each step ticks BOTH registered endpoint tickers once, in order;
            // assert equality after each, not merely an upper bound at the end.
            ticks(test, jarPos, tubePos, sourcePos, 160, true, 32);
            test.assertTrue(jar.getAmount() == 32 && source.getAmount() == 0 && tube.getEssentiaAmount(null) == 0, "real tube and jar ticks drain exactly the first source batch");
            test.assertTrue(source.addToContainer(Aspect.WATER, 32) == 0, "second supplied batch is exactly thirty-two more units");
            ticks(test, jarPos, tubePos, sourcePos, 160, true, 64);
            test.assertTrue(jar.getAmount() == 64 && jar.getSuctionAmount(null) == 0 && jar.getMinimumSuction() == 64,
                    "labelled jar reaches exactly64; full suction stops while minimum64 remains");
            jar.setFilter(null);
            test.assertTrue(jar.getSuctionAmount(null) == 0 && jar.getMinimumSuction() == 32, "full unlabelled jar retains minimum32, not zero");
            jar.setFilter(Aspect.WATER);
            source.addToContainer(Aspect.WATER, 8);
            ticks(test, jarPos, tubePos, sourcePos, 10, true, 72);
            test.assertTrue(jar.getAmount() == 64, "a full jar cannot draw more supplied stock");
            if (tube.getEssentiaAmount(null) == 0) test.assertTrue(EssentiaTransfers.pullOne(level.getBlockEntity(tubePos), Direction.EAST, Aspect.WATER) == 1, "reload fixture includes a paid, nonempty tube");
            jar.setFacing(Direction.EAST);
            TileJar oldJar = jar;
            TileAlembic oldSource = source;
            for (BlockPos pos : new BlockPos[] {jarPos, tubePos, sourcePos}) reload(level, pos);
            jar = (TileJar) level.getBlockEntity(jarPos);
            source = (TileAlembic) level.getBlockEntity(sourcePos);
            tube = (IEssentiaTransport) level.getBlockEntity(tubePos);
            conserved(test, level, jarPos, tubePos, sourcePos, 72);
            test.assertTrue(jar.getAmount() == 64 && jar.getAspect() == Aspect.WATER && jar.getFilter() == Aspect.WATER
                    && jar.getFacing() == Direction.EAST && source.getFilter() == Aspect.WATER && tube.getEssentiaAmount(null) == 1
                    && jar.getUpdatePacket() != null, "native registered BE save/load retains contents, buffered unit, filters, display facing and update data");
            test.assertTrue(!oldJar.takeFromContainer(Aspect.WATER, 1) && !oldSource.takeFromContainer(Aspect.WATER, 1), "replaced endpoint identities cannot mutate old buffers");
            ticks(test, jarPos, tubePos, sourcePos, 10, true, 72);

            // Scalar/container semantics are tested separately from the conserved
            // supplied-stock runs above; these takes/clears explicitly remove stock.
            test.assertTrue(jar.takeFromContainer(Aspect.WATER, 64) && jar.getAspect() == null && jar.getFilter() == Aspect.WATER, "taking the final unit clears type, not the attached filter");
            test.assertTrue(jar.addToContainer(Aspect.FIRE, 8) == 0 && jar.getAspect() == Aspect.FIRE
                    && !jar.doesContainerAccept(Aspect.FIRE), "low-level add ignores a label but acceptance still honors it");
            wrongPhial(test, player, jar, Aspect.WATER);
            jar.clearContents();
            test.assertTrue(jar.getAmount() == 0 && jar.getAspect() == Aspect.FIRE && jar.getFilter() == Aspect.WATER,
                    "sneak-clear retains the previous empty type while labelled, without replacing it with the filter");
            jar.setFilter(null);
            test.assertTrue(jar.getAmount() == 0 && jar.getAspect() == Aspect.FIRE, "removing the label retains the empty aspect field without creating units");
            test.assertTrue(jar.takeFromContainer(Aspect.FIRE, 0) && jar.getAspect() == null
                    && jar.takeFromContainer((Aspect) null, 0) && jar.addToContainer(Aspect.WATER, 0) == 0
                    && jar.getAmount() == 0 && jar.getAspect() == null, "original matching zero-take cleanup and zero-add no-op do not seed or create contents");
        } finally {
            try {
                for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
                for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), QUIET);
                for (BlockEntity original : originals.values()) { original.clearRemoved(); level.setBlockEntity(original); }
            } finally {
                player.getInventory().clearContent();
                for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
                player.getInventory().selected = selected;
                player.setGameMode(gameType);
                player.getAbilities().loadSaveData(abilities);
                player.onUpdateAbilities();
                player.setShiftKeyDown(sneaking);
                player.setPos(position);
                player.setDeltaMovement(motion);
            }
        }
        test.succeed();
    }

    private static void wrongPhial(GameTestHelper test, ServerPlayer player, TileJar jar, Aspect aspect) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled(aspect.getTag()));
        ItemStack held = player.getMainHandItem().copy();
        var before = jar.saveWithoutMetadata(test.getLevel().registryAccess());
        BlockPos pos = jar.getBlockPos();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(Direction.UP, .5), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, test.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        test.assertTrue(ItemStack.matches(held, player.getMainHandItem()) && before.equals(jar.saveWithoutMetadata(test.getLevel().registryAccess())),
                "manual wrong-filter/type phial is refused whole and retains its native components");
    }

    private static void ticks(GameTestHelper test, BlockPos jar, BlockPos tube, BlockPos source, int count, boolean network, int total) {
        for (int tick = 0; tick < count; tick++) {
            if (network) {
                tick(test.getLevel(), tube, ThaumcraftTubes.TUBE_BLOCK_ENTITY.get());
                conserved(test, test.getLevel(), jar, tube, source, total);
            }
            tick(test.getLevel(), jar, ThaumcraftContent.JAR_BLOCK_ENTITY.get());
            conserved(test, test.getLevel(), jar, tube, source, total);
        }
    }

    private static <T extends BlockEntity> void tick(ServerLevel level, BlockPos pos, BlockEntityType<T> type) {
        BlockState state = level.getBlockState(pos);
        var ticker = ((EntityBlock) state.getBlock()).getTicker(level, state, type);
        if (ticker == null) throw new IllegalStateException("Missing native server ticker at " + pos);
        ticker.tick(level, pos, state, type.getBlockEntity(level, pos));
    }

    private static void conserved(GameTestHelper test, ServerLevel level, BlockPos jar, BlockPos tube, BlockPos source, int total) {
        int actual = 0;
        for (BlockPos pos : new BlockPos[] {jar, tube, source}) {
            var endpoint = (EssentiaStorageEndpoint) level.getBlockEntity(pos);
            test.assertTrue(endpoint.getStoredEssentiaAmount() >= 0 && endpoint.getStoredEssentiaAmount() <= endpoint.getEssentiaCapacity()
                    && (endpoint.getStoredEssentiaAmount() == 0 || endpoint.getStoredEssentiaType() == Aspect.WATER), "each real endpoint preserves capacity and aspect");
            actual += endpoint.getStoredEssentiaAmount();
        }
        test.assertTrue(actual == total, "exact per-tick jar+tube+alembic conservation: expected " + total + ", got " + actual);
    }

    private static void reload(ServerLevel level, BlockPos pos) {
        BlockEntity before = level.getBlockEntity(pos);
        var saved = before.saveWithFullMetadata(level.registryAccess());
        BlockState state = level.getBlockState(pos);
        level.removeBlockEntity(pos);
        BlockEntity restored = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
        if (restored == null || restored.getType() != before.getType()) throw new IllegalStateException("Wrong registered endpoint factory at " + pos);
        restored.loadWithComponents(saved, level.registryAccess());
        level.setBlockEntity(restored);
    }
}
