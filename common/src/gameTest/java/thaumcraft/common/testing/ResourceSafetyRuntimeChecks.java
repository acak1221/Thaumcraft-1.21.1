package thaumcraft.common.testing;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.LevelChunkSection;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.blocks.BlockFluidDeath;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.fluid.DeathBucketActions;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileEldritchAltar;
import thaumcraft.common.tiles.TileNode;

/** Connected survival actors and real finite sources/nodes are declared inputs.
 * Native neighbor callbacks exercise hand changes, reentry and partial replacement.
 * A scoped storage-boundary refusal leaves native reads/owners unchanged while
 * the real LevelChunk/Level mutation path answers false. No production hooks. */
public final class ResourceSafetyRuntimeChecks {
    private static NeighborProbe probe;
    private ResourceSafetyRuntimeChecks() {}

    public static synchronized void registerTestProbes() {
        if (probe == null) probe = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "resource_safety_neighbor_probe"), new NeighborProbe());
    }

    public static void deathBucketConservesInputs(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open();
            source(f);
            ItemStack named = new ItemStack(Items.BUCKET);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("physical rejected death input"));
            ItemStack namedBefore = named.copy();
            f.player.setItemInHand(InteractionHand.MAIN_HAND, named);
            try (var refusal = new StorageRefusal(f, f.base, Blocks.AIR.defaultBlockState())) {
                f.check(BlockFluidDeath.isBucketCell(f.level.getBlockState(f.base)), "injected storage refusal preserves the actual source read before use");
                InteractionResult result = useBucket(f);
                f.check(result == InteractionResult.FAIL, "actual empty-bucket use returns FAIL after source storage refusal; got " + result);
                f.check(refusal.attempts() == 1, "one actual source-removal attempt reaches the refusing native storage boundary");
                f.check(ItemStack.matches(namedBefore, f.player.getMainHandItem()), "refused capture refunds the exact component-bearing physical empty input");
                f.check(BlockFluidDeath.isBucketCell(f.level.getBlockState(f.base)), "refused capture retains the original finite death source");
                f.check(f.player.getInventory().countItem(ThaumcraftLegacyItems.BUCKET_DEATH.get()) == 0,
                        "refused source removal grants no filled death bucket");
                f.check(((BlockFluidDeath) f.level.getBlockState(f.base).getBlock()).pickupBlock(f.player, f.level, f.base, f.level.getBlockState(f.base)).isEmpty(),
                        "native BucketPickup also refuses to mint an output when real Level.setBlock returns false");
                f.check(refusal.attempts() == 2, "native BucketPickup independently reaches the same real storage refusal path");
            }
            f.check(BlockFluidDeath.isBucketCell(f.level.getBlockState(f.base)), "storage fixture restores the original native section with its source intact");

            f.reset(); source(f);
            NeighborProbe notice = notice(f, f.base.north());
            f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            notice.callback = () -> {
                f.check(f.player.getMainHandItem().isEmpty(), "source removal callback already sees the physical empty bucket paid");
                f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
            };
            try {
                f.check(useBucket(f).consumesAction() && f.level.getBlockState(f.base).isAir()
                        && f.player.getMainHandItem().is(Items.DIAMOND)
                        && f.player.getInventory().countItem(ThaumcraftLegacyItems.BUCKET_DEATH.get()) == 1
                        && f.player.getInventory().countItem(Items.BUCKET) == 0 && notice.calls == 1,
                        "one paid capture credits exactly one filled bucket without overwriting a callback's new hand owner");
            } finally { notice.clear(); }

            f.reset(); source(f); notice = notice(f, f.base.north());
            ItemStack empty = new ItemStack(Items.BUCKET);
            empty.set(DataComponents.CUSTOM_NAME, Component.literal("ABA source refund preserves components"));
            ItemStack original = empty.copy(); f.player.setItemInHand(InteractionHand.MAIN_HAND, empty);
            notice.callback = () -> {
                f.check(f.player.getMainHandItem().isEmpty(), "reentrant death callback starts after payment");
                f.level.setBlock(f.base, BlockFluidDeath.bucketCellState(), TubeVariantsRuntimeChecks.QUIET);
                BlockState replacement = f.level.getBlockState(f.base);
                f.check(((BlockFluidDeath) replacement.getBlock()).pickupBlock(f.player, f.level, f.base, replacement).isEmpty()
                                && f.level.getBlockState(f.base) == replacement,
                        "native reentrant pickup cannot steal a source coordinate claimed by the outer transaction");
                f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
                var nested = DeathBucketActions.fill(f.level, f.player, InteractionHand.OFF_HAND);
                f.check(nested != null && !nested.getResult().consumesAction() && f.player.getOffhandItem().is(Items.BUCKET),
                        "same-actor reentry on the other hand is refused before a second physical debit");
            };
            try {
                f.check(!useBucket(f).consumesAction() && ItemStack.matches(original, f.player.getMainHandItem())
                        && f.player.getOffhandItem().is(Items.BUCKET) && BlockFluidDeath.isBucketCell(f.level.getBlockState(f.base))
                        && f.player.getInventory().countItem(ThaumcraftLegacyItems.BUCKET_DEATH.get()) == 0 && notice.calls == 1,
                        "an ABA source replacement refuses capture, refunds the original component-bearing input and grants no output");
            } finally { notice.clear(); }

            f.reset(); source(f); f.mode(GameType.CREATIVE);
            f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 3));
            f.check(useBucket(f).consumesAction() && f.level.getBlockState(f.base).isAir()
                            && f.player.getMainHandItem().is(Items.BUCKET) && f.player.getMainHandItem().getCount() == 3
                            && f.player.getInventory().countItem(ThaumcraftLegacyItems.BUCKET_DEATH.get()) == 0,
                    "the original creative ALLOW rule still removes the source without spending empties or granting a filled bucket");
        }
        test.succeed();
    }

    public static void oculusPreservesNodeOnFailure(GameTestHelper test) {
        try (var f = new Fixture(test); var maze = new OwnedMazeRows(f)) {
            f.open();
            Gate gate = gate(f, maze);
            var original = gate.node.saveWithFullMetadata(f.level.registryAccess());
            var unpaid = gate.wand.copy();
            try (var refusal = new StorageRefusal(f, f.base.above(), ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get().defaultBlockState())) {
                f.check(f.level.getBlockEntity(f.base.above()) == gate.node, "storage refusal leaves the actual dark-node owner readable before wand use");
                InteractionResult result = f.use(f.base);
                f.check(result == InteractionResult.FAIL, "actual wand/altar use returns FAIL after gate storage refusal; got " + result);
                f.check(refusal.attempts() == 1, "one actual gate-replacement attempt reaches the refusing native storage boundary");
                f.check(f.level.getBlockEntity(f.base.above()) == gate.node, "refused gate placement retains the same dark-node block entity");
                f.check(original.equals(gate.node.saveWithFullMetadata(f.level.registryAccess())), "refused gate placement preserves complete node aura, capacity, identity, type and modifier NBT");
                f.check(ItemStack.matches(unpaid, gate.wand), "refused gate placement does not charge the wand");
                f.check(!gate.altar.isOpen(), "refused gate placement leaves the altar closed");
            }

            f.reset(); gate = gate(f, maze);
            NeighborProbe notice = notice(f, f.base.above().north());
            notice.callback = () -> f.check(f.level.getBlockState(f.base.above()).is(ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get()),
                    "first gate notification observes the portal directly, never an intermediate AIR that can strand the original node");
            try {
                f.check(f.use(f.base).consumesAction() && gate.altar.isOpen() && notice.calls == 1
                                && f.level.getBlockState(f.base.above()).is(ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get()),
                        "ordinary native wand/altar interaction atomically replaces the dark node with the real gate");
                for (String primal : WandData.PRIMALS) f.check(ItemWandCasting.getVis(gate.wand, primal) == 0,
                        "successful atomic formation retains the original exact100vis per primal payment: " + primal);
            } finally { notice.clear(); }

            f.reset(); gate = gate(f, maze);
            original = gate.node.saveWithFullMetadata(f.level.registryAccess()); unpaid = gate.wand.copy();
            notice = notice(f, f.base.above().north());
            notice.callback = () -> {
                f.check(f.level.getBlockState(f.base.above()).is(ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get()), "partial native replacement reached the portal before cancellation");
                f.level.setBlock(f.base.above(), Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            };
            try {
                InteractionResult result = f.use(f.base);
                f.check(result == InteractionResult.FAIL && f.level.getBlockEntity(f.base.above()) instanceof TileNode restored
                                && original.equals(restored.saveWithFullMetadata(f.level.registryAccess()))
                                && ItemStack.matches(unpaid, gate.wand) && !gate.altar.isOpen() && notice.calls == 1,
                        "cancelled partial gate replacement restores complete node NBT including non-default aura/type/modifier/identity, without vis payment or open altar");
            } finally { notice.clear(); }

            f.reset(); gate = gate(f, maze); unpaid = gate.wand.copy();
            notice = notice(f, f.base.above().north());
            TileNode[] foreign = {null}; net.minecraft.nbt.CompoundTag[] foreignData = {null};
            notice.callback = () -> {
                f.level.setBlock(f.base.above(), ThaumcraftContent.AURA_NODE.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                foreign[0] = (TileNode) f.level.getBlockEntity(f.base.above());
                foreign[0].restoreFromJar(new AspectList().add(Aspect.FIRE, 91), NodeType.PURE, NodeModifier.PALE, "foreign-node-owner");
                foreignData[0] = foreign[0].saveWithFullMetadata(f.level.registryAccess());
            };
            try {
                f.check(f.use(f.base) == InteractionResult.FAIL && f.level.getBlockEntity(f.base.above()) == foreign[0]
                                && foreignData[0].equals(foreign[0].saveWithFullMetadata(f.level.registryAccess()))
                                && ItemStack.matches(unpaid, gate.wand) && !gate.altar.isOpen() && notice.calls == 1,
                        "rollback preserves a deliberately installed foreign node owner even when its block state equals the original node state");
            } finally { notice.clear(); }
        }
        test.succeed();
    }

    private static void source(Fixture f) {
        f.level.setBlock(f.base, BlockFluidDeath.bucketCellState(), TubeVariantsRuntimeChecks.QUIET);
        f.aim(net.minecraft.world.phys.Vec3.atCenterOf(f.base));
    }
    private static InteractionResult useBucket(Fixture f) {
        return f.player.gameMode.useItem(f.player, f.level, f.player.getMainHandItem(), InteractionHand.MAIN_HAND);
    }
    private record Gate(TileEldritchAltar altar, TileNode node, ItemStack wand) {}
    private static Gate gate(Fixture f, OwnedMazeRows maze) {
        f.put(f.base, ThaumcraftEldritchBlocks.ELDRITCH.get());
        f.put(f.base.above(), ThaumcraftContent.AURA_NODE.get());
        var altar = (TileEldritchAltar) f.level.getBlockEntity(f.base); altar.setEyes((byte)4);
        var node = (TileNode) f.level.getBlockEntity(f.base.above());
        node.restoreFromJar(new AspectList().add(Aspect.AIR, 37).add(Aspect.EARTH, 19).add(Aspect.MAGIC, 11),
                NodeType.DARK, NodeModifier.BRIGHT, "resource-safety-dark-node");
        node.takeFromContainer(Aspect.AIR, 7);
        KnowledgeAccess.get(f.player).addResearch("OCULUS");
        maze.cover(altar);
        var vis = new LinkedHashMap<String, Integer>(); for (String primal : WandData.PRIMALS) vis.put(primal, 10000);
        var wand = new ItemStack(ThaumcraftContent.WAND.get()); ItemWandCasting.withData(wand, new WandData("primal_staff", "gold", vis));
        f.player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        return new Gate(altar, node, wand);
    }
    /** Only the rows newly created for this fixture's own altar region are ours.
     * Preserve the live SavedData object and all independently added world rows. */
    private static final class OwnedMazeRows implements AutoCloseable {
        final Fixture f; final OuterLandsProvider.MazeSavedData owner;
        final Map<Long, Short> cells; final Map<Long, Short> owned = new HashMap<>();
        @SuppressWarnings("unchecked")
        OwnedMazeRows(Fixture f) {
            this.f = f; owner = OuterLandsProvider.maze(f.level);
            try {
                var field = OuterLandsProvider.MazeSavedData.class.getDeclaredField("cells"); field.setAccessible(true);
                cells = (Map<Long, Short>) field.get(owner);
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
        void cover(TileEldritchAltar altar) {
            Map<Long, Short> before = new HashMap<>(cells);
            boolean existed;
            try { existed = altar.checkForMaze(); }
            finally {
                int chunkX = altar.getBlockPos().getX() >> 4, chunkZ = altar.getBlockPos().getZ() >> 4;
                // Original altar sizes15..29: its largest grid spans ring-15..ring+13.
                for (var entry : cells.entrySet()) {
                    long key = entry.getKey(); int x = (int)(key >> 32), z = (int)key;
                    if (!before.containsKey(key) && x >= chunkX - 15 && x <= chunkX + 13 && z >= chunkZ - 15 && z <= chunkZ + 13)
                        owned.put(key, entry.getValue());
                }
            }
            if (!existed) f.check(altar.checkForMaze(), "declared altar has its actual persisted covering labyrinth before formation");
        }
        @Override public void close() {
            f.check(OuterLandsProvider.maze(f.level) == owner, "maze fixture cleanup retains the exact live SavedData owner");
            boolean removed = false;
            for (var entry : owned.entrySet()) removed |= cells.remove(entry.getKey(), entry.getValue());
            if (removed) owner.setDirty();
            for (var entry : owned.entrySet()) f.check(!entry.getValue().equals(cells.get(entry.getKey())),
                    "maze fixture removes only its own unchanged region row: " + entry.getKey());
        }
    }
    private static NeighborProbe notice(Fixture f, BlockPos pos) {
        if (probe == null) throw new IllegalStateException("Register resource probes before registry freeze");
        probe.clear(); f.level.setBlock(pos, probe.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); return probe;
    }
    private static final class NeighborProbe extends Block {
        Runnable callback; int calls;
        NeighborProbe() { super(BlockBehaviour.Properties.of().strength(-1).noLootTable()); getStateDefinition().getPossibleStates().forEach(BlockState::initCache); }
        void clear() { callback = null; calls = 0; }
        @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
            if (callback == null) return;
            Runnable once = callback; callback = null; calls++; once.run();
        }
    }
    /** Explicit injected storage failure, not a natural debug-world simulation.
     * LevelChunk's actual setBlockState returns null when its section returns the
     * requested state, and actual Level.setBlock then returns false. Reads and
     * every non-target mutation delegate to the original real native section. */
    private static final class StorageRefusal implements AutoCloseable {
        final Fixture f; final LevelChunkSection[] sections; final int index;
        final LevelChunkSection original; final RefusingSection injected;
        StorageRefusal(Fixture f, BlockPos pos, BlockState rejected) {
            this.f = f;
            var chunk = f.level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            f.check(chunk != null, "storage-refusal fixture owns an already loaded native chunk");
            sections = chunk.getSections(); index = f.level.getSectionIndex(pos.getY()); original = sections[index];
            BlockState state = f.level.getBlockState(pos); var owner = f.level.getBlockEntity(pos);
            injected = new RefusingSection(original, pos, rejected); sections[index] = injected;
            if (f.level.getBlockState(pos) != state || f.level.getBlockEntity(pos) != owner) {
                sections[index] = original; throw new IllegalStateException("Storage refusal changed the native source/owner read view");
            }
        }
        int attempts() { return injected.attempts; }
        @Override public void close() {
            boolean owned = sections[index] == injected; sections[index] = original;
            f.check(owned && sections[index] == original, "finally restores the exact original native section identity");
        }
    }
    private static final class RefusingSection extends LevelChunkSection {
        final LevelChunkSection original; final int x, y, z; final BlockState rejected; int attempts;
        RefusingSection(LevelChunkSection original, BlockPos pos, BlockState rejected) {
            super(original.getStates(), original.getBiomes()); this.original = original;
            x = pos.getX() & 15; y = pos.getY() & 15; z = pos.getZ() & 15; this.rejected = rejected;
        }
        @Override public BlockState getBlockState(int x, int y, int z) { return original.getBlockState(x, y, z); }
        @Override public net.minecraft.world.level.material.FluidState getFluidState(int x, int y, int z) { return original.getFluidState(x, y, z); }
        @Override public boolean hasOnlyAir() { return original.hasOnlyAir(); }
        @Override public BlockState setBlockState(int x, int y, int z, BlockState state) {
            return setBlockState(x, y, z, state, true);
        }
        @Override public BlockState setBlockState(int x, int y, int z, BlockState state, boolean lock) {
            if (x == this.x && y == this.y && z == this.z && state == rejected) { attempts++; return state; }
            return original.setBlockState(x, y, z, state, lock);
        }
    }
}
