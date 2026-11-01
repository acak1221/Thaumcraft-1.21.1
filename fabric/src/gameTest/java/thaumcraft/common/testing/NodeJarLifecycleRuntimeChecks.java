package thaumcraft.common.testing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.blocks.ItemJarNode;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileJarNode;
import thaumcraft.common.tiles.TileNode;

/** Real Fabric BE LOAD/UNLOAD callbacks inside native setBlock, including its quiet flag path. */
public final class NodeJarLifecycleRuntimeChecks {
    private static final int QUIET = Block.UPDATE_KNOWN_SHAPE;
    private static Scope active;
    private static boolean installed;

    private NodeJarLifecycleRuntimeChecks() {}

    /** Fabric events cannot unregister. One permanent listener retains no fixture after its scoped active field clears. */
    private static synchronized void install() {
        if (installed) return;
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((tile, level) -> {
            Scope scope = active;
            if (scope != null && scope.fixture.a.level == level && scope.fixture.center.equals(tile.getBlockPos())) scope.load(tile);
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((tile, level) -> {
            Scope scope = active;
            if (scope != null && scope.fixture.a.level == level && scope.fixture.center.equals(tile.getBlockPos())) scope.unload(tile);
        });
        installed = true;
    }

    public static void captureLifecycleSeesPaidRetiredSource(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            try (var scope = new Scope(f, Mode.OBSERVE_CAPTURE)) {
                f.capture();
                scope.healthy();
                f.a.require(scope.sourceUnloads == 1 && scope.jarLoads == 1 && scope.emptySourceAtUnload && scope.paidAtJarLoad
                                && scope.savedWandPaidAtJarLoad && scope.payloadAtJarLoad,
                        "actual Fabric UNLOAD sees retired source; LOAD sees filled jar and both live/saved wand paid exactly once");
                f.requireJar(f.current, f.identity);
                f.requireVis(3000);
                f.a.require(f.shellEmpty() && f.jars().isEmpty(), "ordinary capture removes exactly its physical shell and spawns no extra jar");
            }
        }
        test.succeed();
    }

    public static void captureLoadCanHarvestExactlyOneFilledJar(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            try (var scope = new Scope(f, Mode.BREAK_CAPTURED_JAR)) {
                f.capture();
                scope.healthy();
                f.a.require(scope.jarLoads == 1 && scope.harvested && scope.paidAtJarLoad && scope.payloadAtJarLoad
                                && f.a.level.isEmptyBlock(f.center) && f.shellEmpty(),
                        "native LOAD may harvest its already-owned paid jar without restoring its source or shell");
                f.requireVis(3000);
                var jars = f.jars();
                f.a.require(jars.size() == 1 && jars.getFirst().getItem().getCount() == 1, "actual LOAD harvest produces exactly one physical jar");
                f.requireItem(jars.getFirst().getItem());
                DeferredItemDrops.tick(f.a.level);
                f.a.require(f.essences() == 0 && f.jars().size() == 1, "capturing/harvesting the jar never also harvests the retired source node");
            }
        }
        test.succeed();
    }

    public static void releaseLoadCanHarvestExactlyOneNode(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.capture();
            TileJarNode originalJar = f.requireJar(f.current, f.identity);
            try (var scope = new Scope(f, Mode.BREAK_RELEASED_NODE)) {
                scope.sourceJar = originalJar;
                f.click(f.center);
                scope.healthy();
                DeferredItemDrops.tick(f.a.level);
                f.a.require(scope.nodeLoads == 1 && scope.harvested && scope.payloadAtNodeLoad && f.a.level.isEmptyBlock(f.center),
                        "actual native node LOAD sees the complete released payload and can harvest it once");
                f.a.require(f.essences() == 7 && f.jars().isEmpty() && !originalJar.drop
                                && scope.sourceJarUnloads == 1 && scope.sourceJarRetiredAtUnload,
                        "release UNLOAD retires the jar's current/base/drop right before its node yields exactly seven essences");
                f.requireVis(3000);
                f.click(f.center);
                DeferredItemDrops.tick(f.a.level);
                f.a.require(f.essences() == 7 && f.jars().isEmpty(), "a later click/queue retry cannot repeat release or node harvest");
            }
        }
        test.succeed();
    }

    public static void captureLoadCannotReplayPreparedJar(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            try (var scope = new Scope(f, Mode.RECYCLE_CAPTURED_JAR)) {
                f.capture(); scope.healthy();
                var jars = f.jars();
                var filled = jars.stream().filter(entity -> ((ItemJarNode)entity.getItem().getItem()).getAspects(entity.getItem()) != null).toList();
                f.a.require(scope.jarLoads == 2 && scope.harvested && scope.recreated && scope.reharvested && scope.secondLoadEmpty,
                        "after harvesting the admitted jar, a callback's fresh jar placement uses a new empty BE instead of replaying the prepared owner");
                f.a.require(jars.size() == 2 && filled.size() == 1 && jars.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 2,
                        "native callback harvests one paid filled jar plus exactly its own declared empty replacement jar");
                f.requireItem(filled.getFirst().getItem());
                f.requireVis(3000);
                f.a.require(f.a.level.isEmptyBlock(f.center) && f.shellEmpty() && f.source.getAspects().visSize() == 0,
                        "recreating the block cannot revive the retired source or any consumed shell");
            }
        }
        test.succeed();
    }

    public static void releaseLoadCannotReplayPreparedNode(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.capture(); TileJarNode jar = f.requireJar(f.current, f.identity);
            try (var scope = new Scope(f, Mode.RECYCLE_RELEASED_NODE)) {
                scope.sourceJar = jar;
                f.click(f.center); scope.healthy(); DeferredItemDrops.tick(f.a.level);
                f.a.require(scope.nodeLoads == 2 && scope.harvested && scope.recreated && scope.reharvested && scope.secondNodeFresh,
                        "a callback's second aura-node placement has a fresh identity/stock and cannot reuse the prepared released node");
                f.a.require(f.essences() == 7 + scope.recreatedNodeEssences && f.jars().isEmpty() && f.a.level.isEmptyBlock(f.center)
                                && scope.sourceJarUnloads == 1 && scope.sourceJarRetiredAtUnload,
                        "two native harvests yield the original seven essences plus only the newly created native node's own stock");
                f.requireVis(3000);
            }
        }
        test.succeed();
    }

    public static void unloadMutationCannotReviveConsumedShell(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            try (var scope = new Scope(f, Mode.MUTATE_SHELL)) {
                f.capture();
                scope.healthy();
                f.a.require(scope.sourceUnloads == 1 && scope.shellGoneAtUnload && scope.foreignWritten,
                        "the source UNLOAD callback runs only after all 35 claimed shell blocks have been consumed");
                f.requireJar(f.current, f.identity);
                f.requireVis(3000);
                f.a.require(f.a.level.getBlockState(f.changed).is(Blocks.DIAMOND_BLOCK), "a callback's new foreign block is never overwritten by the remainder of a stale shell plan");
                f.a.require(f.shellCells().entrySet().stream().allMatch(row -> row.getKey().equals(f.changed)
                                ? row.getValue().is(Blocks.DIAMOND_BLOCK) : row.getValue().isAir()),
                        "only the callback's newly placed foreign block remains beside the single paid jar");
                f.a.require(f.jars().isEmpty(), "neither native source unload nor feedback creates another node jar");
            }
        }
        test.succeed();
    }

    public static void preAdmissionCancellationRollsBackAllResources(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            var shell = f.shellCells();
            CompoundTag before = f.source.saveWithFullMetadata(f.a.level.registryAccess());
            try (var scope = new Scope(f, Mode.CANCEL_BEFORE_FACTORY)) {
                f.capture();
                scope.healthy();
                f.a.require(scope.canceled && scope.jarLoads == 0 && scope.restoredSourceLoads == 1
                                && scope.restoredPayloadAtLoad && scope.restoredWandAtLoad && scope.restoredShellAtLoad,
                        "UNLOAD cancellation before target admission restores complete original metadata, shell and refund before its native LOAD");
                f.a.require(f.a.level.getBlockEntity(f.center) == scope.restoredSource
                                && scope.restoredSource != null && scope.restoredSource.saveWithFullMetadata(f.a.level.registryAccess()).equals(before)
                                && f.shellCells().equals(shell) && f.jars().isEmpty(),
                        "failed pre-admission transfer returns the exact original node data, 26 glass and nine slabs without an output");
                f.a.require(f.source.getAspects().visSize() == 0 && f.source.getAspectsBase().visSize() == 0,
                        "the retired old Java instance keeps no second stock after the prefilled clone takes ownership");
                f.requireVis(10000);
            }
            // The real event listener remains installed but inert; the failed frame cannot poison a following ordinary capture.
            f.capture(); f.requireJar(f.current, f.identity); f.requireVis(3000);
        }
        test.succeed();
    }

    public static void preAdmissionRejectedRestorationPreservesPayload(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            var shell = f.shellCells();
            try (var scope = new Scope(f, Mode.FOREIGN_BEFORE_FACTORY)) {
                f.capture(); scope.healthy();
                f.a.require(scope.sourceUnloads == 1 && scope.jarLoads == 0 && scope.restoredSourceLoads == 0
                                && scope.foreignWritten && scope.foreignRejected,
                        "native source UNLOAD installs a foreign block and throws before the target factory can admit an owner");
                f.a.require(f.a.level.getBlockState(f.center).is(Blocks.DIAMOND_BLOCK) && f.shellCells().equals(shell)
                                && f.source.getAspects().visSize() == 0 && f.source.getAspectsBase().visSize() == 0,
                        "a rejected reverse write restores all shell material and leaves the old source retired, with no foreign overwrite");
                f.requireVis(10000);
                DeferredItemDrops.tick(f.a.level);
                var jars = f.jars();
                f.a.require(jars.size() == 1 && jars.getFirst().getItem().getCount() == 1 && f.essences() == 0,
                        "the sole unrecoverable node payload is retained and emitted as exactly one filled rescue jar, with no essence harvest");
                f.requireItem(jars.getFirst().getItem());
                DeferredItemDrops.tick(f.a.level);
                f.click(f.center.east());
                DeferredItemDrops.tick(f.a.level);
                f.a.require(f.jars().size() == 1 && f.essences() == 0 && f.a.level.getBlockState(f.center).is(Blocks.DIAMOND_BLOCK),
                        "queue retry and a later native activation cannot repeat the rescue output or revive its source");
                f.requireVis(10000);
            }
        }
        test.succeed();
    }

    public static void rawLoadRemovalKeepsOneTransferredOwner(GameTestHelper test) {
        for (boolean release : new boolean[]{false, true}) {
            try (var f = new Fixture(test)) {
                TileJarNode sourceJar = null;
                if (release) { f.capture(); sourceJar = f.requireJar(f.current, f.identity); }
                try (var scope = new Scope(f, release ? Mode.RAW_REMOVE_NODE : Mode.RAW_REMOVE_JAR)) {
                    scope.sourceJar = sourceJar;
                    f.click(release ? f.center : f.center.east()); scope.healthy();
                    DeferredItemDrops.tick(f.a.level);
                    f.a.require(scope.rawLoads == 1 && scope.rawRemoved && scope.cachedTarget != null
                                    && f.a.level.isEmptyBlock(f.center) && f.shellEmpty() && f.jars().isEmpty() && f.essences() == 0,
                            "a native LOAD callback can cache then raw-remove its new owner without restoring or dropping another source: release=" + release);
                    f.requireVis(3000);
                    f.a.require(f.source.getAspects().visSize() == 0 && f.source.getAspectsBase().visSize() == 0,
                            "the old source remains retired after raw target removal");
                    if (release) f.a.require(scope.sourceJarUnloads == 1 && scope.sourceJarRetiredAtUnload,
                            "release retires the old jar before raw node removal");
                    if (release) f.a.require(scope.cachedTarget instanceof TileNode node && f.matchesNode(node), "cached node owns the original transferred payload");
                    else f.a.require(scope.cachedTarget instanceof TileJarNode jar && f.matchesJar(jar), "cached jar owns the original transferred payload");
                    var owner = (IAspectContainer)scope.cachedTarget;
                    f.a.require(owner.takeFromContainer(Aspect.AIR, 37) && !owner.takeFromContainer(Aspect.AIR, 37)
                                    && owner.takeFromContainer(Aspect.FIRE, 29) && !owner.takeFromContainer(Aspect.FIRE, 29)
                                    && owner.getAspects().visSize() == 0,
                            "after the native frame the callback's cached owner can spend its AIR37/FIRE29 once, with no duplicate stock");
                    DeferredItemDrops.tick(f.a.level);
                    f.a.require(f.jars().isEmpty() && f.essences() == 0 && f.a.level.isEmptyBlock(f.center),
                            "settled raw removal never emits a rescue copy or ordinary harvest drops afterward");
                    f.requireVis(3000);
                }
            }
        }
        test.succeed();
    }

    private enum Mode { OBSERVE_CAPTURE, BREAK_CAPTURED_JAR, BREAK_RELEASED_NODE, RECYCLE_CAPTURED_JAR, RECYCLE_RELEASED_NODE, MUTATE_SHELL, CANCEL_BEFORE_FACTORY, FOREIGN_BEFORE_FACTORY, RAW_REMOVE_JAR, RAW_REMOVE_NODE }

    private static final class Scope implements AutoCloseable {
        final Fixture fixture; final Mode mode;
        final CompoundTag originalMetadata;
        final Map<BlockPos, BlockState> originalShell;
        final IllegalStateException rejectionFailure = new IllegalStateException("Declared native Node Jar pre-admission foreign-block rejection");
        TileJarNode sourceJar;
        TileNode restoredSource, firstReleasedNode;
        BlockEntity cachedTarget;
        int sourceUnloads, jarLoads, nodeLoads, restoredSourceLoads, sourceJarUnloads, recreatedNodeEssences, rawLoads;
        boolean emptySourceAtUnload, paidAtJarLoad, savedWandPaidAtJarLoad, payloadAtJarLoad,
                payloadAtNodeLoad, shellGoneAtUnload, foreignWritten, harvested, canceled,
                restoredPayloadAtLoad, restoredWandAtLoad, restoredShellAtLoad, sourceJarRetiredAtUnload,
                recreated, reharvested, secondLoadEmpty, secondNodeFresh, foreignRejected, rawRemoved;
        Throwable failure;
        Scope(Fixture fixture, Mode mode) {
            install();
            if (active != null) throw new IllegalStateException("Another native Node Jar lifecycle scope is active");
            this.fixture = fixture; this.mode = mode;
            originalMetadata = fixture.source.saveWithFullMetadata(fixture.a.level.registryAccess());
            originalShell = fixture.shellCells();
            active = this;
        }
        void load(BlockEntity tile) {
            try {
                if (mode == Mode.RAW_REMOVE_JAR && tile instanceof TileJarNode || mode == Mode.RAW_REMOVE_NODE && tile instanceof TileNode) {
                    if (++rawLoads == 1) {
                        cachedTarget = tile;
                        // FIRST target operation: no getter/save/playerWillDestroy is allowed to acknowledge admission for us.
                        rawRemoved = fixture.a.level.setBlock(fixture.center, Blocks.AIR.defaultBlockState(), QUIET);
                    }
                    return;
                }
                if (tile instanceof TileNode node && mode == Mode.CANCEL_BEFORE_FACTORY) {
                    ++restoredSourceLoads;
                    restoredSource = node;
                    restoredPayloadAtLoad = fixture.matchesNode(node)
                            && node.saveWithFullMetadata(fixture.a.level.registryAccess()).equals(originalMetadata);
                    restoredWandAtLoad = fixture.allVis(10000) && fixture.savedWandHasVis(10000);
                    restoredShellAtLoad = fixture.shellCells().equals(originalShell);
                } else if (tile instanceof TileJarNode jar) {
                    ++jarLoads;
                    if (jarLoads == 1) {
                        paidAtJarLoad = fixture.allVis(3000);
                        savedWandPaidAtJarLoad = fixture.savedWandHasVis(3000);
                        // Exercise native getDrops as the FIRST target operation: an earlier getter must not be what admits it.
                        if (mode == Mode.BREAK_CAPTURED_JAR || mode == Mode.RECYCLE_CAPTURED_JAR)
                            harvested = fixture.a.player.gameMode.destroyBlock(fixture.center);
                        payloadAtJarLoad = fixture.matchesJar(jar);
                        if (mode == Mode.RECYCLE_CAPTURED_JAR) {
                            // This empty replacement block is a declared callback input, never a second supplied node payload.
                            recreated = fixture.a.level.setBlock(fixture.center, ThaumcraftEldritchBlocks.JAR_NODE.get().defaultBlockState(), QUIET);
                            reharvested = fixture.a.player.gameMode.destroyBlock(fixture.center);
                        }
                    } else if (jarLoads == 2 && mode == Mode.RECYCLE_CAPTURED_JAR) {
                        secondLoadEmpty = jar.getAspects().visSize() == 0 && jar.getAspectsBase().visSize() == 0;
                    }
                } else if (tile instanceof TileNode node && (mode == Mode.BREAK_RELEASED_NODE || mode == Mode.RECYCLE_RELEASED_NODE)) {
                    ++nodeLoads;
                    if (nodeLoads == 1) {
                        firstReleasedNode = node;
                        // Likewise NodeHarvest must itself observe a current admitted target, with no preparatory read.
                        harvested = fixture.a.player.gameMode.destroyBlock(fixture.center);
                        payloadAtNodeLoad = fixture.matchesNode(node);
                        if (mode == Mode.RECYCLE_RELEASED_NODE) {
                            recreated = fixture.a.level.setBlock(fixture.center, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), QUIET);
                            reharvested = fixture.a.player.gameMode.destroyBlock(fixture.center);
                        }
                    } else if (nodeLoads == 2) {
                        AspectList newlyCreatedStock = node.getAspects();
                        secondNodeFresh = node != firstReleasedNode && !node.getId().equals(fixture.identity)
                                && !newlyCreatedStock.aspects.equals(fixture.current.aspects);
                        // The retained native creative node fixture starts with its own primal stock. It is a declared
                        // callback-created input, so account for that stock rather than pretending a fresh node is empty.
                        for (Aspect aspect : newlyCreatedStock.getAspects()) {
                            int amount = newlyCreatedStock.getAmount(aspect);
                            if (amount >= 5) recreatedNodeEssences += 1 + amount / 10;
                        }
                    }
                }
            } catch (RuntimeException | Error caught) { failure = caught; }
        }
        void unload(BlockEntity tile) {
            if (tile == fixture.source && mode == Mode.FOREIGN_BEFORE_FACTORY) {
                ++sourceUnloads;
                shellGoneAtUnload = fixture.shellEmpty();
                // FIRST BE operation, before a getter could observe/create a staged target. The foreign owner must survive rollback.
                foreignWritten = fixture.a.level.setBlock(fixture.center, Blocks.DIAMOND_BLOCK.defaultBlockState(), QUIET);
                foreignRejected = true;
                throw rejectionFailure; // Intentionally outside the fixture catch: production must reconcile this native exception.
            }
            try {
                if (tile == sourceJar && (mode == Mode.BREAK_RELEASED_NODE || mode == Mode.RECYCLE_RELEASED_NODE || mode == Mode.RAW_REMOVE_NODE)) {
                    ++sourceJarUnloads;
                    sourceJarRetiredAtUnload = sourceJar.getAspects().visSize() == 0
                            && sourceJar.getAspectsBase().visSize() == 0 && !sourceJar.drop;
                    return;
                }
                if (tile != fixture.source) return;
                ++sourceUnloads;
                shellGoneAtUnload = fixture.shellEmpty();
                // A retired BE getter observes admission and may trigger the native immediate factory lookup.
                // Cancellation must therefore be the FIRST BE operation, before any getter can admit the destination.
                if (sourceUnloads == 1 && mode == Mode.CANCEL_BEFORE_FACTORY) {
                    canceled = true;
                    fixture.a.level.setBlock(fixture.center, Blocks.AIR.defaultBlockState(), QUIET);
                }
                emptySourceAtUnload = fixture.source.getAspects().visSize() == 0 && fixture.source.getAspectsBase().visSize() == 0;
                if (sourceUnloads == 1 && mode == Mode.MUTATE_SHELL)
                    foreignWritten = fixture.a.level.setBlock(fixture.changed, Blocks.DIAMOND_BLOCK.defaultBlockState(), QUIET);
            } catch (RuntimeException | Error caught) { failure = caught; }
        }
        void healthy() { if (failure != null) throw new AssertionError("actual Fabric Node Jar lifecycle callback", failure); }
        @Override public void close() { if (active == this) active = null; }
    }

    private static final class Fixture implements AutoCloseable {
        final FocusUpgradeRuntimeChecks.Actor a;
        final BlockPos center, changed;
        final TileNode source;
        final String identity = UUID.randomUUID().toString();
        final AspectList current = new AspectList().add(Aspect.AIR, 37).add(Aspect.FIRE, 29);
        Fixture(GameTestHelper test) {
            a = new FocusUpgradeRuntimeChecks.Actor(test); center = a.root; changed = center.west().below();
            a.clearLane(); a.player.getInventory().clearContent();
            for (int y = -1; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                a.put(center.offset(x, y, z), y == 2 ? Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM) : Blocks.GLASS.defaultBlockState());
            }
            a.put(center, ThaumcraftContent.AURA_NODE.get().defaultBlockState());
            a.require(a.level.getBlockEntity(center) instanceof TileNode, "declared source is the registered native aura node");
            source = (TileNode)a.level.getBlockEntity(center);
            source.restoreFromJar(new AspectList().add(Aspect.AIR, 80).add(Aspect.FIRE, 60), NodeType.PURE, NodeModifier.FADING, identity);
            a.require(source.takeFromContainer(Aspect.AIR, 43) && source.takeFromContainer(Aspect.FIRE, 31), "declared native current stock is AIR37/FIRE29 below its old base");
            KnowledgeAccess.get(a.player).addResearch("NODEJAR");
            a.equip(ItemStack.EMPTY, "gold", 10000);
            a.require(shellCells().size() == 35 && shellCells().values().stream().filter(state -> state.is(Blocks.GLASS)).count() == 26
                    && shellCells().values().stream().filter(state -> state.is(Blocks.OAK_SLAB)).count() == 9, "exact original shell is declared input");
        }
        void capture() { click(center.east()); }
        void click(BlockPos pos) {
            a.player.gameMode.useItemOn(a.player, a.level, a.player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
        }
        boolean allVis(int amount) { return WandData.PRIMALS.stream().allMatch(primal -> ItemWandCasting.getVis(a.wand, primal) == amount); }
        void requireVis(int amount) { a.require(allVis(amount), "every actual wand primal holds exactly " + amount + " centivis"); }
        boolean savedWandHasVis(int amount) {
            CompoundTag saved = a.player.saveWithoutId(new CompoundTag());
            for (Tag raw : saved.getList("Inventory", Tag.TAG_COMPOUND)) {
                CompoundTag slot = (CompoundTag)raw;
                if ((slot.getByte("Slot") & 255) != a.player.getInventory().selected) continue;
                ItemStack item = ItemStack.parse(a.level.registryAccess(), slot).orElse(ItemStack.EMPTY);
                return item.getItem() instanceof ItemWandCasting && WandData.PRIMALS.stream().allMatch(primal -> ItemWandCasting.getVis(item, primal) == amount);
            }
            return false;
        }
        boolean matchesJar(TileJarNode jar) {
            return jar.getAspects().aspects.equals(current.aspects) && jar.getAspectsBase().aspects.equals(current.aspects)
                    && jar.getNodeType() == NodeType.PURE && jar.getNodeModifier() == NodeModifier.FADING && jar.getId().equals(identity);
        }
        boolean matchesNode(TileNode node) {
            return node.getAspects().aspects.equals(current.aspects) && node.getNodeType() == NodeType.PURE
                    && node.getNodeModifier() == NodeModifier.FADING && node.getId().equals(identity);
        }
        TileJarNode requireJar(AspectList aspects, String id) {
            a.require(a.level.getBlockEntity(center) instanceof TileJarNode, "native capture leaves its registered node jar");
            var jar = (TileJarNode)a.level.getBlockEntity(center);
            a.require(matchesJar(jar), "the native jar retains current stock as base, type, fading modifier and identity");
            return jar;
        }
        void requireItem(ItemStack stack) {
            a.require(stack.getItem() instanceof ItemJarNode, "native jar harvest yields the actual registered node-jar item");
            var item = (ItemJarNode)stack.getItem();
            a.require(item.getAspects(stack) != null && item.getAspects(stack).aspects.equals(current.aspects)
                            && item.getNodeType(stack) == NodeType.PURE && item.getNodeModifier(stack) == NodeModifier.FADING && item.getNodeId(stack).equals(identity),
                    "actual native callback drop keeps every stored aspect and attribute");
        }
        List<ItemEntity> jars() { return a.items().stream().filter(entity -> entity.getItem().is(ThaumcraftEldritchBlocks.JAR_NODE_ITEM.get())).toList(); }
        int essences() { return a.items().stream().filter(entity -> entity.getItem().is(ThaumcraftEssences.WISP_ESSENCE.get())).mapToInt(entity -> entity.getItem().getCount()).sum(); }
        Map<BlockPos, BlockState> shellCells() {
            var result = new LinkedHashMap<BlockPos, BlockState>();
            for (int y = -1; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                BlockPos pos = center.offset(x, y, z); result.put(pos, a.level.getBlockState(pos));
            }
            return result;
        }
        boolean shellEmpty() { return shellCells().values().stream().allMatch(BlockState::isAir); }
        @Override public void close() { a.close(); }
    }
}
