package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileJarNode;
import thaumcraft.common.tiles.TileNode;

/** Native Fabric listeners, scoped to an actual connected actor and its real shell. */
public final class NodeJarProtectionRuntimeChecks {
    private NodeJarProtectionRuntimeChecks() {}
    private static Claims active;
    private static boolean installed;
    private static CrossLevelClaim crossLevelClaim;
    private static boolean crossLevelInstalled;

    public static void nativeBreakClaimsProtectFormation(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            // Each refused cell differs from the glass activation target. A native click veto alone
            // therefore cannot make these checks pass without the formation's own BEFORE bridge.
            for (BlockPos veto : new BlockPos[]{f.center.west(), f.center.above(2), f.center}) {
                f.seed();
                try (var claims = new Claims(f)) {
                    claims.breakVeto = veto;
                    f.unchanged(() -> f.click(f.center.east()), "native break claim at " + veto);
                    f.a.require(claims.nativeBreaks.containsKey(veto), "formation actually consulted the native vetoed cell");
                }
            }
            f.seed();
            try (var claims = new Claims(f)) {
                f.click(f.center.east());
                f.a.require(f.a.level.getBlockEntity(f.center) instanceof TileJarNode, "allowed native callbacks commit the jar");
                f.a.require(claims.commonBreaks.size() == 36 && claims.nativeBreaks.size() == 36
                                && claims.commonBreaks.values().stream().allMatch(count -> count == 1)
                                && claims.nativeBreaks.values().stream().allMatch(count -> count == 1),
                        "each of the 36 original cells receives one shared and one native break consultation");
                f.a.require(claims.commonPlaces == 1 && claims.nativeUses.getOrDefault(f.center.east(), 0) == 1
                                && !claims.nativeUses.containsKey(f.center),
                        "replacement receives one common PLACE and the native glass click runs once without synthetic item-use replay");
                for (String primal : WandData.PRIMALS) f.a.require(f.a.vis(primal) == 3000, "one capture debit after allowed native callbacks: " + primal);
            }
        }
        test.succeed();
    }

    public static void nativeClaimsProtectCaptureAndRelease(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.seed();
            try (var claims = new Claims(f)) {
                claims.useVeto = f.center.east();
                f.unchanged(() -> f.click(f.center.east()), "native use claim on the actual glass click");
                f.a.require(claims.nativeUses.getOrDefault(f.center.east(), 0) == 1 && claims.nativeBreaks.isEmpty(),
                        "native use refusal prevents formation before its shell preflight");
            }
            try (var claims = new Claims(f)) { f.click(f.center.east()); }
            f.a.require(f.a.level.getBlockEntity(f.center) instanceof TileJarNode, "fixture captured its actual jar");
            var jar = (TileJarNode) f.a.level.getBlockEntity(f.center);
            CompoundTag saved = jar.saveWithFullMetadata(f.a.level.registryAccess());
            WandData paid = ItemWandCasting.data(f.a.wand);
            try (var claims = new Claims(f)) {
                claims.breakVeto = f.center;
                f.release(jar);
                f.sameJar(jar, saved, paid, "native break veto on the jar being unpacked");
                f.a.require(claims.nativeBreaks.getOrDefault(f.center, 0) == 1, "unpack consulted its native break permission");
            }
            try (var claims = new Claims(f)) {
                claims.useVeto = f.center;
                f.click(f.center);
                f.sameJar(jar, saved, paid, "native use veto on the actual jar click");
                f.a.require(claims.nativeUses.getOrDefault(f.center, 0) == 1 && claims.nativeBreaks.isEmpty(),
                        "native use refusal prevents unpack before the jar preflight");
            }
            try (var claims = new Claims(f)) {
                f.release(jar);
                f.a.require(f.a.level.getBlockEntity(f.center) instanceof TileNode
                                && claims.nativeBreaks.getOrDefault(f.center, 0) == 1
                                && claims.commonBreaks.getOrDefault(f.center, 0) == 1
                                && claims.nativeUses.isEmpty() && claims.commonPlaces == 1,
                        "allowed direct unpack delivers one native/common break and common place without synthetic item-use replay");
                f.a.require(ItemWandCasting.data(f.a.wand).equals(paid), "original free unpack spends no further vis");
            }
        }
        test.succeed();
    }

    public static void nativeFactoryCannotPublishPayloadInAnotherLevel(GameTestHelper test) {
        // The native GameTest floor is below Nether minY. Declare both-dimension-valid terrain.
        try (var f = new Fixture(test, 128)) {
            ServerLevel foreign = f.a.level.getServer().getLevel(Level.NETHER);
            f.a.require(foreign != null && foreign != f.a.level, "declared second native server level exists");
            // Explicitly supplied second-dimension terrain, restored after both native factory attempts.
            foreign.getChunk(f.center);
            BlockState original = foreign.getBlockState(f.center);
            BlockEntity originalTile = foreign.getBlockEntity(f.center);
            CompoundTag originalData = originalTile == null ? null : originalTile.saveWithFullMetadata(foreign.registryAccess());
            try {
                for (boolean release : new boolean[]{false, true}) {
                    f.seed();
                    if (release) f.click(f.center.east());
                    BlockEntity source = f.a.level.getBlockEntity(f.center);
                    f.a.require(release ? source instanceof TileJarNode : source instanceof TileNode, "declared source owner exists before cross-level attempt");
                    CompoundTag saved = source.saveWithFullMetadata(f.a.level.registryAccess());
                    var cells = f.cells();
                    WandData wand = ItemWandCasting.data(f.a.wand);
                    String id = source instanceof TileNode node ? node.getId() : ((TileJarNode) source).getId();
                    BlockState proposed = release ? ThaumcraftContent.AURA_NODE.get().defaultBlockState()
                            : ThaumcraftEldritchBlocks.JAR_NODE.get().defaultBlockState();
                    foreign.setBlock(f.center, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE);
                    try (var claim = new CrossLevelClaim(f, foreign, source, proposed, id)) {
                        if (release) f.release((TileJarNode) source); else f.click(f.center.east());
                        BlockEntity restored = f.a.level.getBlockEntity(f.center);
                        f.a.require(claim.attempted && claim.rejected,
                                "native source UNLOAD attempted the actual factory at matching coordinates in the other level, rejected before registration");
                        f.a.require(restored != null && restored.getType() == source.getType()
                                        && restored.saveWithFullMetadata(f.a.level.registryAccess()).equals(saved)
                                        && f.cells().equals(cells) && ItemWandCasting.data(f.a.wand).equals(wand)
                                        && f.a.items().isEmpty(),
                                "unadmitted cross-level factory restores exactly one source, shell and vis: release=" + release);
                        // Native lazy creation may now create an ordinary empty/creative fixture. It
                        // must never reuse the transferred payload or its identity after the failed frame.
                        BlockEntity ordinary = foreign.getBlockEntity(f.center);
                        String foreignId = ordinary instanceof TileNode node ? node.getId()
                                : ordinary instanceof TileJarNode jar ? jar.getId() : "";
                        f.a.require(!claim.payloadPublished && !id.equals(foreignId),
                                "foreign native LOAD never receives the original source identity or its filled staged owner");
                    }
                }
            } finally {
                foreign.removeBlockEntity(f.center);
                foreign.setBlock(f.center, original, Block.UPDATE_KNOWN_SHAPE);
                if (originalData != null && foreign.getBlockEntity(f.center) != null)
                    foreign.getBlockEntity(f.center).loadWithComponents(originalData, foreign.registryAccess());
            }
        }
        test.succeed();
    }

    /** A real nested foreign setBlock from the source's native UNLOAD, never a mocked factory. */
    private static final class CrossLevelClaim implements AutoCloseable {
        final Fixture f;
        final ServerLevel foreign;
        final BlockEntity source;
        final BlockState proposed;
        final String identity;
        boolean attempted, rejected, payloadPublished;

        CrossLevelClaim(Fixture f, ServerLevel foreign, BlockEntity source, BlockState proposed, String identity) {
            if (crossLevelClaim != null) throw new IllegalStateException("overlapping cross-level node owner scopes");
            this.f = f; this.foreign = foreign; this.source = source; this.proposed = proposed; this.identity = identity;
            if (!crossLevelInstalled) {
                ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((tile, level) -> {
                    CrossLevelClaim claim = crossLevelClaim;
                    if (claim == null || claim.attempted || level != claim.f.a.level || tile != claim.source) return;
                    claim.attempted = true;
                    try { claim.foreign.setBlock(claim.f.center, claim.proposed, Block.UPDATE_KNOWN_SHAPE); }
                    catch (IllegalStateException expected) {
                        if (expected.getMessage() == null || !expected.getMessage().contains("prepared node owner")) throw expected;
                        claim.rejected = true;
                    }
                });
                ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((tile, level) -> {
                    CrossLevelClaim claim = crossLevelClaim;
                    if (claim == null || level != claim.foreign || !tile.getBlockPos().equals(claim.f.center)) return;
                    String id = tile instanceof TileNode node ? node.getId() : tile instanceof TileJarNode jar ? jar.getId() : "";
                    claim.payloadPublished |= claim.identity.equals(id);
                });
                crossLevelInstalled = true;
            }
            crossLevelClaim = this;
        }
        @Override public void close() { if (crossLevelClaim == this) crossLevelClaim = null; }
    }

    private static final class Fixture implements AutoCloseable {
        final FocusUpgradeRuntimeChecks.Actor a;
        final BlockPos center;
        TileNode node;
        final InteractionHand hand = InteractionHand.OFF_HAND;
        final Direction face = Direction.NORTH;

        Fixture(GameTestHelper test) { this(test, 0); }
        Fixture(GameTestHelper test, int verticalOffset) {
            a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 4 + verticalOffset, 4)); center = a.root;
            a.clearLane(); a.player.getInventory().clearContent();
            KnowledgeAccess.get(a.player).addResearch("NODEJAR");
        }
        void seed() {
            a.clearLane(); a.equip(ItemStack.EMPTY, "gold", 10000);
            a.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            a.player.setItemInHand(hand, a.wand);
            for (int y = -1; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                a.put(center.offset(x, y, z), y == 2 ? Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM) : Blocks.GLASS.defaultBlockState());
            }
            a.put(center, ThaumcraftContent.AURA_NODE.get().defaultBlockState());
            node = (TileNode) a.level.getBlockEntity(center);
            node.restoreFromJar(new AspectList().add(Aspect.AIR, 80).add(Aspect.FIRE, 60), NodeType.UNSTABLE, NodeModifier.BRIGHT, UUID.randomUUID().toString());
        }
        void click(BlockPos pos) {
            a.player.gameMode.useItemOn(a.player, a.level, a.wand, hand,
                    new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
        }
        void release(TileJarNode jar) {
            // Exercise the actual public wand target directly, so an outer native useItemOn
            // claim cannot mask a missing BEFORE bridge for the jar being replaced.
            jar.onWandRightClick(a.level, a.wand, a.player, center.getX(), center.getY(), center.getZ(), face.ordinal(), 0);
        }
        Map<BlockPos, BlockState> cells() {
            var cells = new LinkedHashMap<BlockPos, BlockState>();
            for (int y = -1; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos pos = center.offset(x, y, z); cells.put(pos, a.level.getBlockState(pos));
            }
            return cells;
        }
        void unchanged(Runnable action, String why) {
            var cells = cells(); var saved = node.saveWithFullMetadata(a.level.registryAccess());
            WandData wand = ItemWandCasting.data(a.wand);
            action.run();
            a.require(cells().equals(cells) && a.level.getBlockEntity(center) == node
                            && node.saveWithFullMetadata(a.level.registryAccess()).equals(saved)
                            && ItemWandCasting.data(a.wand).equals(wand) && a.items().isEmpty(),
                    "native claim refusal retains exact shell/node/payload/vis: " + why);
        }
        void sameJar(TileJarNode jar, CompoundTag saved, WandData paid, String why) {
            a.require(a.level.getBlockEntity(center) == jar && jar.drop
                            && jar.saveWithFullMetadata(a.level.registryAccess()).equals(saved)
                            && ItemWandCasting.data(a.wand).equals(paid) && a.items().isEmpty(),
                    "native unpack refusal retains jar/drop/payload/vis: " + why);
        }
        @Override public void close() { a.close(); }
    }

    /** Permanent Fabric listeners hold no world or actor while a scope is closed. */
    private static final class Claims implements AutoCloseable {
        final Fixture f;
        final Map<BlockPos, Integer> commonBreaks = new LinkedHashMap<>(), nativeBreaks = new LinkedHashMap<>(), nativeUses = new LinkedHashMap<>();
        BlockPos breakVeto, useVeto;
        int commonPlaces;
        final BlockEvent.Break commonBreak;
        final BlockEvent.Place commonPlace;

        Claims(Fixture f) {
            if (active != null) throw new IllegalStateException("overlapping node-jar protection scopes");
            this.f = f;
            commonBreak = (world, pos, state, player, xp) -> {
                if (world == f.a.level && player == f.a.player) commonBreaks.merge(pos.immutable(), 1, Integer::sum);
                return EventResult.pass();
            };
            commonPlace = (world, pos, state, player) -> {
                if (world == f.a.level && player == f.a.player && pos.equals(f.center)) {
                    ++commonPlaces;
                    f.a.require(state.is(ThaumcraftEldritchBlocks.JAR_NODE.get()) || state.is(ThaumcraftContent.AURA_NODE.get()),
                            "shared placement reports the proposed replacement before changing the real world");
                }
                return EventResult.pass();
            };
            if (!installed) {
                PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, tile) -> {
                    Claims current = active;
                    if (current == null || world != current.f.a.level || player != current.f.a.player) return true;
                    current.nativeBreaks.merge(pos.immutable(), 1, Integer::sum);
                    current.f.a.require(world.getBlockState(pos) == state && world.getBlockEntity(pos) == tile,
                            "native BEFORE receives the actual untouched source state and block entity");
                    return !pos.equals(current.breakVeto);
                });
                UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
                    Claims current = active;
                    if (current == null || world != current.f.a.level || player != current.f.a.player) return InteractionResult.PASS;
                    current.nativeUses.merge(hit.getBlockPos().immutable(), 1, Integer::sum);
                    current.f.a.require(hand == current.f.hand && hit.getDirection() == current.f.face,
                            "native placement consultation retains the actual offhand and clicked face");
                    return hit.getBlockPos().equals(current.useVeto) ? InteractionResult.FAIL : InteractionResult.PASS;
                });
                installed = true;
            }
            BlockEvent.BREAK.register(commonBreak); BlockEvent.PLACE.register(commonPlace); active = this;
        }
        @Override public void close() {
            if (active == this) active = null;
            BlockEvent.BREAK.unregister(commonBreak); BlockEvent.PLACE.unregister(commonPlace);
        }
    }
}
