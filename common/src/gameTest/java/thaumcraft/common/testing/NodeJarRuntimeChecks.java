package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.blocks.ItemJarNode;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.NodeJarFormation;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileJarNode;
import thaumcraft.common.tiles.TileNode;

/** Declared native shell/node/player inputs. Capture, payment, carried payload and release all use real item/block paths. */
public final class NodeJarRuntimeChecks {
    private NodeJarRuntimeChecks() {}

    public static void captureCarryUnpackPreservesNodeAndPaysOnce(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.seed(); f.learn();
            f.a.equip(ItemStack.EMPTY, "gold", 10000);
            // Original current stock is below its base: capture must reset the new base to CURRENT, never refill it.
            f.a.require(f.node.takeFromContainer(Aspect.AIR, 43) && f.node.takeFromContainer(Aspect.FIRE, 31), "native source was partially drained");
            AspectList current = f.node.getAspects();
            f.a.require(current.getAmount(Aspect.AIR) == 37 && current.getAmount(Aspect.FIRE) == 29,
                    "declared source has AIR37/FIRE29 current and AIR80/FIRE60 base");
            String id = f.node.getId();
            try (var ignored = new CaptureRoll(f.a.level, .5F)) { f.click(f.center.east()); }
            var jar = f.jar();
            f.a.require(jar.getNodeType() == NodeType.UNSTABLE && jar.getNodeModifier() == null && jar.getId().equals(id),
                    "native glass activation retains type/id and degrades BRIGHT to normal on the original 75-percent branch");
            same(current, jar.getAspects(), f.a, "captured current aspects");
            same(current, jar.getAspectsBase(), f.a, "captured base resets to current");
            f.a.require(f.cells().entrySet().stream().allMatch(row -> row.getKey().equals(f.center)
                    ? row.getValue().is(ThaumcraftEldritchBlocks.JAR_NODE.get()) : row.getValue().isAir()),
                    "exactly 26 glass and nine wooden slabs collapse into the one node jar");
            for (String primal : WandData.PRIMALS) f.a.require(f.a.vis(primal) == 3000, "one original 70-whole-vis payment: " + primal);
            WandData paid = ItemWandCasting.data(f.a.wand);
            f.click(f.center.east().above());
            f.a.require(ItemWandCasting.data(f.a.wand).equals(paid), "clicking a consumed shell cannot pay a second capture");

            f.a.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            f.a.require(f.a.player.gameMode.destroyBlock(f.center), "the actual captured jar is harvested through the native player path");
            ItemEntity physical = f.jarDrops().getFirst();
            f.a.require(f.jarDrops().size() == 1 && physical.getItem().getCount() == 1, "native break yields exactly one carried node jar");
            ItemStack carried = ItemStack.parse(f.a.level.registryAccess(), physical.getItem().save(f.a.level.registryAccess())).orElseThrow();
            var item = (ItemJarNode)carried.getItem();
            same(current, item.getAspects(carried), f.a, "actual drop survives native ItemStack codec");
            f.a.require(item.getNodeType(carried) == NodeType.UNSTABLE && item.getNodeModifier(carried) == null && item.getNodeId(carried).equals(id),
                    "item codec retains the captured node attributes");
            // Transfer the actual codec-round-tripped drop to the owned actor; no second item remains in the world.
            physical.discard();
            BlockPos destination = f.center.east(2);
            f.a.put(destination.below(), Blocks.STONE.defaultBlockState());
            f.a.player.setItemInHand(InteractionHand.MAIN_HAND, carried);
            f.click(destination.below());
            f.a.require(carried.isEmpty() && f.a.level.getBlockEntity(destination) instanceof TileJarNode, "native block-item placement consumes the carried jar once");
            TileJarNode placed = (TileJarNode)f.a.level.getBlockEntity(destination);
            same(current, placed.getAspects(), f.a, "placed jar aspects");
            f.a.require(placed.getNodeType() == NodeType.UNSTABLE && placed.getNodeModifier() == null && placed.getId().equals(id), "placed jar attributes");
            f.a.player.setItemInHand(InteractionHand.MAIN_HAND, f.a.wand);
            f.click(destination);
            f.a.require(f.a.level.getBlockState(destination).is(ThaumcraftContent.AURA_NODE.get())
                    && f.a.level.getBlockEntity(destination) instanceof TileNode, "wand release creates the registered native aura-node host");
            TileNode released = (TileNode)f.a.level.getBlockEntity(destination);
            same(current, released.getAspects(), f.a, "released current aspects");
            same(current, released.getAspectsBase(), f.a, "released node base");
            f.a.require(released.getNodeType() == NodeType.UNSTABLE && released.getNodeModifier() == null && released.getId().equals(id)
                    && ItemWandCasting.data(f.a.wand).equals(paid) && f.jarDrops().isEmpty(), "release retains exact node identity and costs no second vis/jar");
            f.a.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            f.a.require(f.a.player.gameMode.destroyBlock(destination), "the released ordinary native node can safely be broken");
            DeferredItemDrops.tick(f.a.level);
            int essences = f.a.items().stream().filter(entity -> entity.getItem().is(ThaumcraftEssences.WISP_ESSENCE.get()))
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
            f.a.require(f.a.level.getBlockEntity(destination) == null && f.jarDrops().isEmpty() && essences == 7,
                    "breaking the released AIR37/FIRE29 node yields only its original seven essence items, never a second jar");
        }
        test.succeed();
    }

    public static void formationRejectsInvalidInputsWithoutPayment(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.seed(); f.a.equip(ItemStack.EMPTY, "gold", 10000);
            f.unchangedAfter(() -> f.click(f.center.east()), "missing NODEJAR research");
            f.learn();
            f.a.wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(f.a.wand).withAmount("perditio", 6999));
            f.unchangedAfter(() -> f.click(f.center.east()), "one centivis short in the last primal refuses the entire payment");
            f.a.equip(ItemStack.EMPTY, "gold", 10000);
            f.unchangedAfter(() -> f.click(f.center.above(2)), "original activation is glass, never a wooden lid slab");
            f.a.put(f.center.west().below(), Blocks.AIR.defaultBlockState());
            f.unchangedAfter(() -> f.click(f.center.east()), "missing one of the 26 glass blocks");
            f.seed(); f.a.put(f.center.above(2), Blocks.STONE_SLAB.defaultBlockState());
            f.unchangedAfter(() -> f.click(f.center.east().above()), "stone slabs are not the original wooden lid");
            f.seed();
            BlockEvent.Break denyShell = (world, pos, state, actor, xp) -> world == f.a.level && actor == f.a.player && pos.equals(f.center.west())
                    ? EventResult.interruptFalse() : EventResult.pass();
            BlockEvent.BREAK.register(denyShell);
            try { f.unchangedAfter(() -> f.click(f.center.east()), "protection veto on a different shell cell"); }
            finally { BlockEvent.BREAK.unregister(denyShell); }
            BlockEvent.Place denyJar = (world, pos, state, actor) -> world == f.a.level && actor == f.a.player && pos.equals(f.center)
                    && state.is(ThaumcraftEldritchBlocks.JAR_NODE.get()) ? EventResult.interruptFalse() : EventResult.pass();
            BlockEvent.PLACE.register(denyJar);
            try { f.unchangedAfter(() -> f.click(f.center.east()), "protection veto on the proposed jar"); }
            finally { BlockEvent.PLACE.unregister(denyJar); }
        }
        test.succeed();
    }

    public static void callbacksCannotReenterOrChangePaidCapture(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.learn();
            for (int action = 0; action < 6; action++) {
                f.a.player.setGameMode(GameType.SURVIVAL);
                f.a.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                f.seed(); f.a.equip(ItemStack.EMPTY, "gold", 10000);
                if (action == 3) {
                    f.a.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
                    f.a.require(ArcaneVis.price(NodeJarFormation.FORM_VIS, f.a.wand, f.a.player).values().stream().allMatch(price -> price == 6650),
                            "real gold wand and worn goggles quote6650 centivis for every primal before permission callbacks");
                }
                WandData before = ItemWandCasting.data(f.a.wand);
                int selected = action; int[] entries = {0};
                BlockPos changed = f.center.west().below();
                BlockEvent.Place mutate = (world, pos, proposed, actor) -> {
                    if (world != f.a.level || actor != f.a.player || !pos.equals(f.center) || !proposed.is(ThaumcraftEldritchBlocks.JAR_NODE.get())) return EventResult.pass();
                    ++entries[0];
                    if (entries[0] > 1) throw new AssertionError("capture recursively dispatched its permission callback");
                    f.click(f.center.east());
                    f.a.require(f.a.level.getBlockEntity(f.center) == f.node && ItemWandCasting.data(f.a.wand).equals(before),
                            "a nested native glass click has no capture/payment right");
                    if (selected == 0) f.a.put(changed, Blocks.STONE.defaultBlockState());
                    if (selected == 1) f.a.wand.set(ThaumcraftContent.WAND_DATA.get(), before.withAmount("aer", 9999));
                    if (selected == 2) f.node.setNodeModifier(NodeModifier.PALE);
                    if (selected == 3) f.a.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    if (selected == 4) f.a.player.setGameMode(GameType.SPECTATOR);
                    if (selected == 5) f.a.player.setGameMode(GameType.ADVENTURE);
                    return EventResult.pass();
                };
                BlockEvent.PLACE.register(mutate);
                try { f.click(f.center.east()); } finally { BlockEvent.PLACE.unregister(mutate); }
                f.a.require(entries[0] == 1 && f.a.level.getBlockEntity(f.center) == f.node && f.jarDrops().isEmpty(),
                        "shell/wand/source/price/permission changes during native preflight abort capture: " + action);
                f.a.require(ItemWandCasting.data(f.a.wand).equals(action == 1 ? before.withAmount("aer", 9999) : before),
                        "only the callback's own one-centivis mutation remains, with no recipe debit: " + action);
                f.a.require(f.a.level.getBlockState(changed).is(action == 0 ? Blocks.STONE : Blocks.GLASS)
                        && f.cells().values().stream().noneMatch(state -> state.isAir() || state.is(ThaumcraftEldritchBlocks.JAR_NODE.get())),
                        "abort never partly consumes the rest of the shell: " + action);
                f.a.require(f.node.getAspects().getAmount(Aspect.AIR) == 80 && f.node.getAspects().getAmount(Aspect.FIRE) == 60,
                        "preflight never empties the source node");
            }
            f.a.player.setGameMode(GameType.SURVIVAL);
            f.a.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            f.seed(); f.a.equip(ItemStack.EMPTY, "copper", 10000);
            f.a.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
            int[] entries = {0};
            BlockEvent.Place reenter = (world, pos, state, actor) -> {
                if (world == f.a.level && actor == f.a.player && pos.equals(f.center) && state.is(ThaumcraftEldritchBlocks.JAR_NODE.get())) {
                    ++entries[0]; if (entries[0] > 1) throw new AssertionError("capture reentered"); f.click(f.center.east());
                }
                return EventResult.pass();
            };
            BlockEvent.PLACE.register(reenter);
            try { f.click(f.center.east().above()); } finally { BlockEvent.PLACE.unregister(reenter); }
            f.jar(); f.a.require(entries[0] == 1, "upper glass uses the original candidate search and admits one capture despite nested activation");
            for (String primal : WandData.PRIMALS) f.a.require(f.a.vis(primal) == (primal.equals("ordo") || primal.equals("perditio") ? 3350 : 2650),
                    "one real copper-cap and worn-goggles discounted payment for " + primal);
        }
        test.succeed();
    }

    public static void rejectedUnpackKeepsJarDropAndAspects(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.a.clearLane(); f.a.equip(ItemStack.EMPTY, "gold", 10000);
            AspectList contents = new AspectList().add(Aspect.AIR, 40).add(Aspect.FIRE, 30);
            String id = UUID.randomUUID().toString();
            ItemStack stock = new ItemStack(ThaumcraftEldritchBlocks.JAR_NODE_ITEM.get());
            var item = (ItemJarNode)stock.getItem(); item.setAspects(stock, contents); item.setNodeAttributes(stock, NodeType.PURE, NodeModifier.PALE, id);
            f.a.put(f.center.below(), Blocks.STONE.defaultBlockState());
            f.a.player.setItemInHand(InteractionHand.MAIN_HAND, stock); f.click(f.center.below());
            TileJarNode jar = f.jar();
            f.a.player.setItemInHand(InteractionHand.MAIN_HAND, f.a.wand);
            WandData before = ItemWandCasting.data(f.a.wand); CompoundTag saved = jar.saveWithFullMetadata(f.a.level.registryAccess());
            int[] entries = {0};
            BlockEvent.Place veto = (world, pos, state, actor) -> {
                if (world == f.a.level && actor == f.a.player && pos.equals(f.center) && state.is(ThaumcraftContent.AURA_NODE.get())) {
                    ++entries[0]; if (entries[0] > 1) throw new AssertionError("release reentered its callback");
                    f.click(f.center); return EventResult.interruptFalse();
                }
                return EventResult.pass();
            };
            BlockEvent.PLACE.register(veto);
            try { f.click(f.center); } finally { BlockEvent.PLACE.unregister(veto); }
            f.a.require(entries[0] == 1 && f.a.level.getBlockEntity(f.center) == jar && jar.drop
                    && jar.saveWithFullMetadata(f.a.level.registryAccess()).equals(saved) && ItemWandCasting.data(f.a.wand).equals(before),
                    "a denied/reentered native unpack keeps the exact jar, its drop right and all payload/vis");
            for (GameType mode : List.of(GameType.SPECTATOR, GameType.ADVENTURE)) {
                f.a.player.setGameMode(GameType.SURVIVAL);
                int[] changed = {0};
                BlockEvent.Place changePermission = (world, pos, state, actor) -> {
                    if (world == f.a.level && actor == f.a.player && pos.equals(f.center) && state.is(ThaumcraftContent.AURA_NODE.get())) {
                        ++changed[0]; f.a.player.setGameMode(mode);
                    }
                    return EventResult.pass();
                };
                BlockEvent.PLACE.register(changePermission);
                try { f.click(f.center); } finally { BlockEvent.PLACE.unregister(changePermission); }
                f.a.require(changed[0] == 1 && f.a.level.getBlockEntity(f.center) == jar && jar.drop
                                && jar.saveWithFullMetadata(f.a.level.registryAccess()).equals(saved) && ItemWandCasting.data(f.a.wand).equals(before),
                        "a late " + mode + " permission change retains the original jar and all its resources");
            }
            f.a.player.setGameMode(GameType.SURVIVAL);
            f.a.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            f.a.require(f.a.player.gameMode.destroyBlock(f.center) && f.jarDrops().size() == 1, "breaking the rejected jar still produces one ordinary native jar drop");
            ItemStack returned = f.jarDrops().getFirst().getItem();
            same(contents, item.getAspects(returned), f.a, "failed release preserves the physical drop's complete aspects");
            f.a.require(item.getNodeType(returned) == NodeType.PURE && item.getNodeModifier(returned) == NodeModifier.PALE && item.getNodeId(returned).equals(id),
                    "failed release preserves the physical drop's complete attributes");
            f.a.player.setItemInHand(InteractionHand.MAIN_HAND, f.a.wand);
            jar.onWandRightClick(f.a.level, f.a.wand, f.a.player, f.center.getX(), f.center.getY(), f.center.getZ(), Direction.UP.ordinal(), 0);
            f.a.require(f.a.level.isEmptyBlock(f.center) && f.jarDrops().size() == 1 && ItemWandCasting.data(f.a.wand).equals(before),
                    "a removed stale jar cannot release a second node over the harvested position");
        }
        test.succeed();
    }

    public static void unloadedBlueprintDoesNotLoadChunks(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.learn(); f.a.equip(ItemStack.EMPTY, "gold", 10000);
            BlockPos far = new BlockPos(768007, 64, 768007);
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                f.a.require(!f.a.level.hasChunkAt(far.offset(x * 16, 0, z * 16)), "declared far candidate terrain is not loaded");
            WandData before = ItemWandCasting.data(f.a.wand);
            f.a.require(!NodeJarFormation.fits(f.a.level, far), "unloaded native terrain is not a complete shell");
            NodeJarFormation.tryForm(new UseOnContext(f.a.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(far), Direction.UP, far, false)));
            f.a.require(!NodeJarFormation.fits(f.a.level, far.atY(f.a.level.getMinBuildHeight() - 1)), "out-of-height candidate is refused before terrain lookup");
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                f.a.require(!f.a.level.hasChunkAt(far.offset(x * 16, 0, z * 16)), "blueprint fitting/search acquired no native terrain ticket");
            f.a.require(ItemWandCasting.data(f.a.wand).equals(before), "unloaded/out-of-height candidates spend no vis");
        }
        test.succeed();
    }

    private static void same(AspectList expected, AspectList actual, FocusUpgradeRuntimeChecks.Actor a, String message) {
        a.require(actual != null && expected.aspects.equals(actual.aspects), message + "; expected=" + expected.aspects + ", actual=" + (actual == null ? null : actual.aspects));
    }

    private static final class Fixture implements AutoCloseable {
        final FocusUpgradeRuntimeChecks.Actor a;
        final BlockPos center;
        TileNode node;
        Fixture(GameTestHelper test) { a = new FocusUpgradeRuntimeChecks.Actor(test); center = a.root; a.clearLane(); a.player.getInventory().clearContent(); }
        void learn() { KnowledgeAccess.get(a.player).addResearch("NODEJAR"); }
        void seed() {
            a.clearLane();
            for (int y = -1; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                a.put(center.offset(x, y, z), y == 2 ? Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM) : Blocks.GLASS.defaultBlockState());
            }
            a.put(center, ThaumcraftContent.AURA_NODE.get().defaultBlockState());
            a.require(a.level.getBlockEntity(center) instanceof TileNode, "fixture is the actual registered aura node");
            node = (TileNode)a.level.getBlockEntity(center);
            node.restoreFromJar(new AspectList().add(Aspect.AIR, 80).add(Aspect.FIRE, 60), NodeType.UNSTABLE, NodeModifier.BRIGHT, UUID.randomUUID().toString());
            a.require(cells().values().stream().filter(state -> state.is(Blocks.GLASS)).count() == 26
                    && cells().values().stream().filter(state -> state.is(Blocks.OAK_SLAB)).count() == 9,
                    "declared original blueprint contains exactly 26 glass, nine slabs and its one node in the third layer from the top");
        }
        void click(BlockPos pos) {
            a.player.gameMode.useItemOn(a.player, a.level, a.player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
        }
        TileJarNode jar() { a.require(a.level.getBlockEntity(center) instanceof TileJarNode, "native action produced the registered node jar"); return (TileJarNode)a.level.getBlockEntity(center); }
        Map<BlockPos, BlockState> cells() {
            var rows = new LinkedHashMap<BlockPos, BlockState>();
            for (int y = -1; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos pos = center.offset(x, y, z); rows.put(pos, a.level.getBlockState(pos));
            }
            return rows;
        }
        void unchangedAfter(Runnable action, String why) {
            var shell = cells(); var source = node.saveWithFullMetadata(a.level.registryAccess()); WandData wand = ItemWandCasting.data(a.wand);
            action.run();
            a.require(cells().equals(shell) && a.level.getBlockEntity(center) == node
                    && node.saveWithFullMetadata(a.level.registryAccess()).equals(source)
                    && ItemWandCasting.data(a.wand).equals(wand) && jarDrops().isEmpty(), "refusal preserves shell/node/vis: " + why);
        }
        List<ItemEntity> jarDrops() { return a.items().stream().filter(entity -> entity.getItem().is(ThaumcraftEldritchBlocks.JAR_NODE_ITEM.get())).toList(); }
        @Override public void close() { a.close(); }
    }

    /** Declares the original one degradation draw, advancing the actual world stream just as ChampionJoinRoll does. */
    private static final class CaptureRoll implements AutoCloseable {
        private final Level level; private final Field field; private final RandomSource original;
        CaptureRoll(Level level, float roll) {
            this.level = level; original = level.random;
            var declared = new LegacyRandomSource(0) {
                @Override public float nextFloat() { original.nextFloat(); return roll; }
                @Override public int nextInt() { return original.nextInt(); }
                @Override public int nextInt(int bound) { return original.nextInt(bound); }
                @Override public long nextLong() { return original.nextLong(); }
                @Override public boolean nextBoolean() { return original.nextBoolean(); }
                @Override public double nextDouble() { return original.nextDouble(); }
                @Override public double nextGaussian() { return original.nextGaussian(); }
            };
            try { field = Level.class.getDeclaredField("random"); field.setAccessible(true); field.set(level, declared); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
        @Override public void close() {
            try { field.set(level, original); } catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
        }
    }
}
