package thaumcraft.common.tiles;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.BlockStoneDevice;
import thaumcraft.common.blocks.BlockWandPedestal;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.foci.FocalUpgradeCosts;

/** Actual registered block tickers, node stock and native chunk update queue; no copied charge arithmetic. */
public final class WandPedestalRegressionChecks {
    private WandPedestalRegressionChecks() {}

    public static void conservesWholeNodeVis(GameTestHelper test) {
        for (boolean stoneDevice : new boolean[]{false, true}) {
            try (Fixture f = new Fixture(test, stoneDevice)) {
                for (String cap : new String[]{"iron", "gold"}) {
                    ItemStack wand = wand("greatwood", cap, Map.of());
                    f.prepare(wand, new AspectList().add(Aspect.AIR, 50));
                    f.draw();
                    f.check(ItemWandCasting.getVis(wand, "aer") == 100 && f.node().getAspects().getAmount(Aspect.AIR) == 49,
                            cap + ": one actual draw credits100cV and debits one whole node vis");
                    f.draw();
                    f.check(ItemWandCasting.getVis(wand, "aer") == 200 && f.node().getAspects().getAmount(Aspect.AIR) == 48,
                            cap + ": a second draw cannot create free vis");
                }
                ItemStack staff = wand("obsidian_staff", "gold", Map.of());
                f.prepare(staff, new AspectList().add(Aspect.AIR, 200));
                f.draw();
                f.check(ItemWandCasting.getVis(staff, "aer") == 100 && f.node().getAspects().getAmount(Aspect.AIR) == 199,
                        "a staff also receives one whole vis without losing100 node units");

                for (var supplier : java.util.List.of(ThaumcraftBaubles.VIS_AMULET_LESSER, ThaumcraftBaubles.VIS_AMULET)) {
                    ItemStack amulet = new ItemStack(supplier.get());
                    f.prepare(amulet, new AspectList().add(Aspect.AIR, 50));
                    f.draw();
                    f.check(((ItemAmuletVis) amulet.getItem()).getVis(amulet, Aspect.AIR) == 100
                                    && f.node().getAspects().getAmount(Aspect.AIR) == 49,
                            "both native amulets use one whole node vis, not100");
                }

                ItemStack preserving = wand("greatwood", "gold", Map.of());
                f.prepare(preserving, new AspectList().add(Aspect.AIR, 1));
                f.draw();
                f.check(ItemWandCasting.getVis(preserving, "aer") == 0 && f.node().getAspects().getAmount(Aspect.AIR) == 1,
                        "a better cap and rod preserve the last whole node vis");
                ItemStack cheap = wand("wood", "iron", Map.of());
                f.prepare(cheap, new AspectList().add(Aspect.AIR, 1));
                f.draw();
                f.check(ItemWandCasting.getVis(cheap, "aer") == 100 && f.node().getAspects().getAmount(Aspect.AIR) == 0,
                        "the cheap original wand may consume the final whole vis");

                ItemStack busy = wand("greatwood", "gold", Map.of());
                f.prepare(busy, new AspectList().add(Aspect.AIR, 50));
                f.check(f.node().beginTransduction(), "the real source enters its exclusive transition");
                try {
                    f.draw();
                    f.check(ItemWandCasting.getVis(busy, "aer") == 0 && f.node().getAspects().getAmount(Aspect.AIR) == 50,
                            "a declined source draw cannot grant a free destination credit");
                } finally { f.node().endTransduction(); }
                f.check(!f.node().drawForPedestal(f.pedestal(), Aspect.AIR), "an unprepared caller cannot debit a node");
            }
        }
        test.succeed();
    }

    public static void fillsPartialVisAndConvertsOnlyWithFocus(GameTestHelper test) {
        for (boolean stoneDevice : new boolean[]{false, true}) {
            try (Fixture f = new Fixture(test, stoneDevice)) {
                for (int room : new int[]{1, 50, 99, 100}) {
                    ItemStack wand = wand("greatwood", "gold", Map.of("aer", 5000 - room));
                    f.prepare(wand, new AspectList().add(Aspect.AIR, 50));
                    f.draw();
                    f.check(ItemWandCasting.getVis(wand, "aer") == 5000 && f.node().getAspects().getAmount(Aspect.AIR) == 49,
                            "the final" + room + "centivis fill while one whole node unit pays, as in TC4");
                }
                ItemStack full = wand("greatwood", "gold", Map.of("aer", 5000));
                f.prepare(full, new AspectList().add(Aspect.AIR, 50));
                f.draw();
                f.check(ItemWandCasting.getVis(full, "aer") == 5000 && f.node().getAspects().getAmount(Aspect.AIR) == 50,
                        "a full target does not consume source stock");

                for (boolean amulet : new boolean[]{false, true}) {
                    ItemStack target = amulet ? new ItemStack(ThaumcraftBaubles.VIS_AMULET.get()) : wand("greatwood", "gold", Map.of());
                    f.prepare(target, new AspectList().add(Aspect.MAGIC, 50));
                    f.draw();
                    f.check(f.node().getAspects().getAmount(Aspect.MAGIC) == 50 && totalVis(target) == 0,
                            "without an installed focus a compound is neither consumed nor credited");
                    f.level.setBlock(f.focusPos, ThaumcraftLegacyTiles.WAND_PEDESTAL.get().defaultBlockState()
                            .setValue(BlockWandPedestal.FOCUS, true), Block.UPDATE_ALL);
                    f.check(f.pedestal().hasFocus(f.level) && f.level.getBlockEntity(f.focusPos) == null,
                            "the actual placed focus state is detected and has no pedestal tile");
                    f.draw();
                    f.check(f.node().getAspects().getAmount(Aspect.MAGIC) == 49 && totalVis(target) == 100,
                            "one compound pays exactly one compatible whole primal, not all of its components");
                    var allowed = FocalUpgradeCosts.reduceToPrimals(new AspectList().add(Aspect.MAGIC, 1));
                    for (Aspect primal : Aspect.getPrimalAspects()) {
                        int amount = amount(target, primal);
                        f.check(amount == 0 || amount == 100 && allowed.getAmount(primal) > 0,
                                "the credited primal actually belongs to the compound");
                    }
                    f.level.setBlock(f.focusPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                ItemStack amulet = new ItemStack(ThaumcraftBaubles.VIS_AMULET_LESSER.get());
                ((ItemAmuletVis) amulet.getItem()).storeVis(amulet, Aspect.AIR, 2499);
                f.prepare(amulet, new AspectList().add(Aspect.AIR, 50));
                f.draw();
                f.check(amount(amulet, Aspect.AIR) == 2500 && f.node().getAspects().getAmount(Aspect.AIR) == 49,
                        "an amulet's final single centivis also fills with original whole-unit rounding");
            }
        }
        test.succeed();
    }

    public static void inventoryChangesQueueNativeClientUpdates(GameTestHelper test) {
        try (Fixture f = new Fixture(test, false)) {
            f.placePedestal();
            f.check(f.pedestal().nodes().isEmpty(), "no source or charge tick is needed for inventory synchronization");
            ChunkHolder holder = holder(f.level, f.pedestalPos);
            holder.broadcastChanges(f.level.getChunkAt(f.pedestalPos));
            f.check(!pending(holder), "the native chunk's previous changes have been flushed");
            ItemStack wand = wand("greatwood", "gold", Map.of("aer", 123));
            f.pedestal().setItem(0, wand);
            f.check(pending(holder), "ordinary insertion queued a real native block update");
            TileWandPedestal receiving = new TileWandPedestal(f.pedestalPos, f.pedestal().getBlockState());
            receiving.loadWithComponents(f.pedestal().getUpdatePacket().getTag(), f.level.registryAccess());
            f.check(ItemStack.matches(receiving.getItem(0), wand), "the actual update packet carries the inserted item and components");

            holder.broadcastChanges(f.level.getChunkAt(f.pedestalPos));
            ItemStack removed = f.pedestal().removeItemNoUpdate(0);
            f.check(removed == wand && pending(holder), "removing the final item queues an update without any nearby nodes");
            receiving.loadWithComponents(f.pedestal().getUpdatePacket().getTag(), f.level.registryAccess());
            f.check(receiving.isEmpty() && !receiving.isDraining(), "the empty snapshot clears the previous client wand and beam");
        }
        test.succeed();
    }

    private static ItemStack wand(String rod, String cap, Map<String, Integer> vis) {
        ItemStack stack = new ItemStack(ThaumcraftContent.WAND.get());
        ItemWandCasting.withData(stack, new WandData(rod, cap, vis));
        return stack;
    }

    private static int amount(ItemStack stack, Aspect primal) {
        return stack.getItem() instanceof ItemWandCasting ? ItemWandCasting.getVis(stack, primal.getTag())
                : ((ItemAmuletVis) stack.getItem()).getVis(stack, primal);
    }

    private static int totalVis(ItemStack stack) {
        int amount = 0;
        for (Aspect primal : Aspect.getPrimalAspects()) amount += amount(stack, primal);
        return amount;
    }

    private static ChunkHolder holder(ServerLevel level, BlockPos pos) {
        try {
            Method visible = ChunkMap.class.getDeclaredMethod("getVisibleChunkIfPresent", long.class);
            visible.setAccessible(true);
            ChunkHolder holder = (ChunkHolder) visible.invoke(level.getChunkSource().chunkMap, net.minecraft.world.level.ChunkPos.asLong(pos));
            if (holder == null) throw new AssertionError("Registered native test chunk has no holder");
            return holder;
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Cannot observe native chunk update scheduling", failure); }
    }

    private static boolean pending(ChunkHolder holder) {
        try {
            Field changes = ChunkHolder.class.getDeclaredField("hasChangedSections");
            changes.setAccessible(true);
            return changes.getBoolean(holder);
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Cannot inspect native chunk update queue", failure); }
    }

    private static final class Fixture implements AutoCloseable {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final BlockPos pedestalPos, nodePos, focusPos;
        private final boolean stoneDevice;
        private final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        private final Map<BlockPos, BlockEntity> tiles = new LinkedHashMap<>();
        private Fixture(GameTestHelper test, boolean stoneDevice) {
            this.test = test; this.level = test.getLevel(); this.stoneDevice = stoneDevice;
            pedestalPos = test.absolutePos(new BlockPos(3, 3, 3));
            nodePos = pedestalPos.offset(2, 0, 0); focusPos = pedestalPos.above();
            for (BlockPos pos : java.util.List.of(pedestalPos, nodePos, focusPos)) {
                states.put(pos, level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos));
                level.removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        private void placePedestal() {
            if (level.getBlockEntity(pedestalPos) instanceof TileWandPedestal previous) previous.removeItemNoUpdate(0);
            level.removeBlockEntity(pedestalPos);
            level.setBlock(pedestalPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            BlockState state = stoneDevice ? ThaumcraftEldritchBlocks.STONE_DEVICE.get().defaultBlockState()
                    .setValue(BlockStoneDevice.VARIANT, 5) : ThaumcraftLegacyTiles.WAND_PEDESTAL.get().defaultBlockState();
            level.setBlock(pedestalPos, state, Block.UPDATE_ALL);
            check(level.getBlockEntity(pedestalPos) instanceof TileWandPedestal, "native registered block creates its own pedestal");
        }
        private void prepare(ItemStack target, AspectList stock) {
            placePedestal();
            level.removeBlockEntity(nodePos);
            level.setBlock(nodePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(nodePos, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), Block.UPDATE_ALL);
            node().setAspects(stock);
            pedestal().setItem(0, target);
        }
        private TileNode node() { return (TileNode) level.getBlockEntity(nodePos); }
        private TileWandPedestal pedestal() { return (TileWandPedestal) level.getBlockEntity(pedestalPos); }
        private void draw() {
            BlockEntityType<TileWandPedestal> type = stoneDevice ? ThaumcraftEldritchBlocks.WAND_PEDESTAL_TILE.get()
                    : ThaumcraftLegacyTiles.WAND_PEDESTAL_BLOCK_ENTITY.get();
            for (int i = 0; i < 5; i++) {
                BlockState state = level.getBlockState(pedestalPos);
                var ticker = state.getTicker(level, type);
                check(ticker != null, "actual registered native pedestal ticker exists");
                ticker.tick(level, pedestalPos, state, pedestal());
            }
        }
        private void check(boolean valid, String why) { test.assertTrue(valid, (stoneDevice ? "stone device: " : "pedestal: ") + why); }
        @Override public void close() {
            if (level.getBlockEntity(pedestalPos) instanceof TileWandPedestal tile) tile.removeItemNoUpdate(0);
            for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL);
            for (BlockEntity tile : tiles.values()) { tile.clearRemoved(); level.setBlockEntity(tile); }
        }
    }
}
