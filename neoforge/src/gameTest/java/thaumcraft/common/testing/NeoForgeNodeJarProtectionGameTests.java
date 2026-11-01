package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
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

/** Actual connected actors, original shells and native protection listeners on the NeoForge bus. */
@PrefixGameTestTemplate(false)
public final class NeoForgeNodeJarProtectionGameTests {
    @GameTest(template = "empty", templateNamespace = "thaumcraft", timeoutTicks = 400, batch = "node_jar_native_protection")
    public void neoNodeJarNativeBreakClaimsProtectCaptureAndRelease(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            for (BlockPos veto : new BlockPos[]{f.center.west(), f.center.above(2), f.center}) {
                f.seed();
                try (var claims = new Claims(f)) {
                    claims.breakVeto = veto;
                    f.unchanged(() -> f.click(f.center.east()), "native break veto at remote glass, slab or source node");
                    f.a.require(claims.nativeBreaks.getOrDefault(veto, 0) == 1
                                    && claims.commonBreaks.getOrDefault(veto, 0) == 1,
                            "each vetoed cell receives one native and one shared break attempt");
                }
            }
            f.seed();
            try (var claims = new Claims(f)) {
                f.click(f.center.east());
                f.jar();
                f.a.require(claims.nativeBreaks.size() == 36 && claims.commonBreaks.size() == 36
                                && claims.nativeBreaks.values().stream().allMatch(count -> count == 1)
                                && claims.commonBreaks.values().stream().allMatch(count -> count == 1)
                                && claims.nativePlaces == 1 && claims.commonPlaces == 1,
                        "successful capture asks each native/common break once and asks native/common placement once");
                f.a.require(claims.nativeUses == 1, "the actual offhand click runs once without synthetic native item-use replay");
                f.vis(3000);
            }
            TileJarNode jar = f.jar();
            try (var claims = new Claims(f)) {
                claims.breakVeto = f.center;
                f.unchanged(() -> f.release(jar), "native break claim on the jar being unpacked");
                f.a.require(claims.nativeBreaks.getOrDefault(f.center, 0) == 1
                                && claims.commonBreaks.getOrDefault(f.center, 0) == 1
                                && claims.nativePlaces == 0 && claims.commonPlaces == 0,
                        "a native jar-break veto stops unpack before placement and preserves the sole jar");
            }
            try (var claims = new Claims(f)) {
                f.release(jar);
                f.a.require(f.a.level.getBlockEntity(f.center) instanceof TileNode
                                && claims.nativeBreaks.getOrDefault(f.center, 0) == 1
                                && claims.commonBreaks.getOrDefault(f.center, 0) == 1
                                && claims.nativePlaces == 1 && claims.commonPlaces == 1 && claims.nativeUses == 0,
                        "direct free unpack uses one native/common break and place, with no synthetic click");
                f.vis(3000);
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft", timeoutTicks = 400, batch = "node_jar_native_protection")
    public void neoNodeJarPlaceClaimsSeeProposedOwnerBeforeMutation(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.seed();
            try (var claims = new Claims(f)) {
                claims.nativePlaceVeto = true;
                f.unchanged(() -> f.click(f.center.east()), "native placement veto on the proposed filled jar");
                f.a.require(claims.nativePlaces == 1 && claims.commonPlaces == 1,
                        "one native placement veto retains exact shell, node payload and vis");
            }
            try (var claims = new Claims(f)) {
                claims.commonPlaceVeto = true;
                f.unchanged(() -> f.click(f.center.east()), "shared placement veto on the proposed filled jar");
                f.a.require(claims.nativePlaces == 1 && claims.commonPlaces == 1 && claims.sharedVetoReachedNative,
                        "the native event carries the one shared placement veto without duplicate dispatch");
            }
            f.click(f.center.east());
            TileJarNode jar = f.jar();
            try (var claims = new Claims(f)) {
                claims.nativePlaceVeto = true;
                f.unchanged(() -> f.release(jar), "native placement veto on the proposed released aura node");
                f.a.require(claims.nativePlaces == 1 && claims.commonPlaces == 1,
                        "native aura-node placement veto preserves the filled jar and its drop right");
            }
            try (var claims = new Claims(f)) {
                claims.commonPlaceVeto = true;
                f.unchanged(() -> f.release(jar), "shared placement veto on the proposed released aura node");
                f.a.require(claims.nativePlaces == 1 && claims.commonPlaces == 1 && claims.sharedVetoReachedNative,
                        "native unpack also delivers the shared placement veto exactly once");
            }
            try (var claims = new Claims(f)) {
                f.release(jar);
                f.a.require(f.a.level.getBlockEntity(f.center) instanceof TileNode
                                && claims.nativePlaces == 1 && claims.commonPlaces == 1,
                        "both proposed-state getters and the original snapshot permit a legitimate release");
                f.vis(3000);
            }
        }
        test.succeed();
    }

    private static final class Fixture implements AutoCloseable {
        final FocusUpgradeRuntimeChecks.Actor a;
        final BlockPos center;
        final InteractionHand hand = InteractionHand.OFF_HAND;
        final Direction face = Direction.NORTH;

        Fixture(GameTestHelper test) {
            a = new FocusUpgradeRuntimeChecks.Actor(test); center = a.root;
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
            ((TileNode) a.level.getBlockEntity(center)).restoreFromJar(new AspectList().add(Aspect.AIR, 80).add(Aspect.FIRE, 60),
                    NodeType.PURE, NodeModifier.FADING, UUID.randomUUID().toString());
        }
        void click(BlockPos pos) {
            a.player.gameMode.useItemOn(a.player, a.level, a.wand, hand, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
        }
        void release(TileJarNode jar) {
            jar.onWandRightClick(a.level, a.wand, a.player, center.getX(), center.getY(), center.getZ(), face.ordinal(), 0);
        }
        TileJarNode jar() {
            a.require(a.level.getBlockEntity(center) instanceof TileJarNode, "native action produced its actual filled node jar");
            return (TileJarNode) a.level.getBlockEntity(center);
        }
        void vis(int amount) { for (String primal : WandData.PRIMALS) a.require(a.vis(primal) == amount, "exact centivis balance for " + primal); }
        Map<BlockPos, BlockState> cells() {
            var cells = new LinkedHashMap<BlockPos, BlockState>();
            for (int y = -1; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos pos = center.offset(x, y, z); cells.put(pos, a.level.getBlockState(pos));
            }
            return cells;
        }
        void unchanged(Runnable action, String why) {
            var cells = cells(); BlockEntity source = a.level.getBlockEntity(center);
            CompoundTag saved = source.saveWithFullMetadata(a.level.registryAccess());
            WandData wand = ItemWandCasting.data(a.wand);
            action.run();
            a.require(cells().equals(cells) && a.level.getBlockEntity(center) == source
                            && source.saveWithFullMetadata(a.level.registryAccess()).equals(saved)
                            && (!(source instanceof TileJarNode jar) || jar.drop)
                            && ItemWandCasting.data(a.wand).equals(wand) && a.items().isEmpty(),
                    "claim refusal preserves exact shell/source/drop/payload/vis: " + why);
        }
        @Override public void close() { a.close(); }
    }

    private static final class Claims implements AutoCloseable {
        final Fixture f;
        final Map<BlockPos, Integer> nativeBreaks = new LinkedHashMap<>(), commonBreaks = new LinkedHashMap<>();
        BlockPos breakVeto;
        boolean nativePlaceVeto, commonPlaceVeto, sharedVetoReachedNative;
        int nativePlaces, commonPlaces, nativeUses;
        final Consumer<BlockEvent.BreakEvent> nativeBreak;
        final Consumer<BlockEvent.EntityPlaceEvent> nativePlace;
        final Consumer<PlayerInteractEvent.RightClickBlock> nativeUse;
        final dev.architectury.event.events.common.BlockEvent.Break commonBreak;
        final dev.architectury.event.events.common.BlockEvent.Place commonPlace;

        Claims(Fixture f) {
            this.f = f;
            nativeBreak = event -> {
                if (event.getLevel() != f.a.level || event.getPlayer() != f.a.player) return;
                nativeBreaks.merge(event.getPos().immutable(), 1, Integer::sum);
                f.a.require(event.getState() == f.a.level.getBlockState(event.getPos()), "native break sees the untouched source cell");
                if (event.getPos().equals(breakVeto)) event.setCanceled(true);
            };
            commonBreak = (world, pos, state, player, xp) -> {
                if (world == f.a.level && player == f.a.player) commonBreaks.merge(pos.immutable(), 1, Integer::sum);
                return EventResult.pass();
            };
            nativePlace = event -> {
                if (event.getLevel() != f.a.level || event.getEntity() != f.a.player || !event.getPos().equals(f.center)) return;
                ++nativePlaces;
                BlockState before = f.a.level.getBlockState(f.center), after = replacement(before);
                f.a.require(event.getState() == after && event.getPlacedBlock() == after,
                        "both native placement getters expose the proposed node owner");
                f.a.require(event.getBlockSnapshot().getLevel() == f.a.level
                                && event.getBlockSnapshot().getState() == before && event.getBlockSnapshot().getCurrentState() == before
                                && event.getPlacedAgainst() == before && f.a.level.getBlockEntity(f.center) != null,
                        "native placement retains the real level, original snapshot and live source before mutation");
                sharedVetoReachedNative |= commonPlaceVeto && event.isCanceled();
                if (nativePlaceVeto) event.setCanceled(true);
            };
            commonPlace = (world, pos, state, player) -> {
                if (world != f.a.level || player != f.a.player || !pos.equals(f.center)) return EventResult.pass();
                ++commonPlaces;
                f.a.require(state == replacement(f.a.level.getBlockState(f.center)), "shared placement also receives proposed state before mutation");
                return commonPlaceVeto ? EventResult.interruptFalse() : EventResult.pass();
            };
            nativeUse = event -> {
                if (event.getLevel() != f.a.level || event.getEntity() != f.a.player) return;
                ++nativeUses;
                f.a.require(event.getHand() == f.hand && event.getFace() == f.face, "native click keeps the declared offhand and face");
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, BlockEvent.BreakEvent.class, nativeBreak);
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, BlockEvent.EntityPlaceEvent.class, nativePlace);
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, PlayerInteractEvent.RightClickBlock.class, nativeUse);
            dev.architectury.event.events.common.BlockEvent.BREAK.register(commonBreak);
            dev.architectury.event.events.common.BlockEvent.PLACE.register(commonPlace);
        }
        private BlockState replacement(BlockState before) {
            f.a.require(before.is(ThaumcraftContent.AURA_NODE.get()) || before.is(ThaumcraftEldritchBlocks.JAR_NODE.get()),
                    "preflight source is the actual node or node jar");
            return before.is(ThaumcraftContent.AURA_NODE.get()) ? ThaumcraftEldritchBlocks.JAR_NODE.get().defaultBlockState()
                    : ThaumcraftContent.AURA_NODE.get().defaultBlockState();
        }
        @Override public void close() {
            NeoForge.EVENT_BUS.unregister(nativeBreak); NeoForge.EVENT_BUS.unregister(nativePlace); NeoForge.EVENT_BUS.unregister(nativeUse);
            dev.architectury.event.events.common.BlockEvent.BREAK.unregister(commonBreak);
            dev.architectury.event.events.common.BlockEvent.PLACE.unregister(commonPlace);
        }
    }
}
