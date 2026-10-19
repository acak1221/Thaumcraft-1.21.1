package thaumcraft.common.testing;

import static thaumcraft.common.testing.CrystallizerRuntimeChecks.assertTyped;
import static thaumcraft.common.testing.CrystallizerRuntimeChecks.crystalDrops;
import static thaumcraft.common.testing.CrystallizerRuntimeChecks.machine;
import static thaumcraft.common.testing.CrystallizerRuntimeChecks.roundTrip;
import static thaumcraft.common.testing.CrystallizerRuntimeChecks.tick;
import static thaumcraft.common.testing.CrystallizerRuntimeChecks.ticks;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.items.ItemCrystalEssence;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileEssentiaCrystalizer;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;

/** M06e finite SERVER fixtures. Raw stock, pre-existing inventory stacks and
 * ordinary fuel/research are supplied. Every consumed/scanned crystal comes from
 * a real1000-call production cycle; no forced clocks/RNG/completion. Native
 * allowlist only, NOT a promise of arbitrary Container/addon or VisNet parity. */
public final class CrystallizerOutputRuntimeChecks {
    private CrystallizerOutputRuntimeChecks() {}

    /** Wrapper timeout >=120 ticks; only the actual thaumometer use waits20 ticks. */
    public static void nativeInventoryRoutingAndProducedConsumers(GameTestHelper test) {
        var f = new Fixture(test);
        try {
            f.open(); nativeMixedAndQueries(f); nativeInventories(f); unsupportedNeverCalled(f);
            startProducedScanAndDistillation(test, f);
        } catch (RuntimeException | Error failure) { f.close(); throw failure; }
    }

    /** Wrapper timeout >=200 ticks; failed ADD retries use real level-post time. */
    public static void durableEscrowCallbacksAndSpawnRetries(GameTestHelper test) {
        var f = new Fixture(test);
        try {
            f.open(); nativeDirtyInputPair(f); completionAndInventoryCallbacks(f);
            startDurableRetries(test, f);
        } catch (RuntimeException | Error failure) { f.close(); throw failure; }
    }

    private static void nativeMixedAndQueries(Fixture f) {
        f.reset(); var tile = machine(f, f.base, Direction.DOWN); TileTubeBuffer source = TubeBufferRuntimeChecks.buffer(f, f.base.below());
        source.addToContainer(Aspect.FIRE, 1); source.addToContainer(Aspect.MAGIC, 1); source.addToContainer(Aspect.FIRE, 1); ticks(f, tile, 5);
        Aspect selected = tile.getAspect();
        f.check((selected == Aspect.FIRE || selected == Aspect.MAGIC) && source.getAmount() == 2 && source.containerContains(selected) == (selected == Aspect.FIRE ? 1 : 0)
                && source.containerContains(selected == Aspect.FIRE ? Aspect.MAGIC : Aspect.FIRE) == (selected == Aspect.FIRE ? 1 : 2) && tile.getProgress() == 0,
                "GENUINE mixed buffer: one native held type selection pays primal OR compound1; exact conservation, no seeded sequence/both-outcomes requirement");
        for (String branch : List.of("primal", "compound", "null-type", "minimum-equality", "minimum-high", "tie", "second-tie", "closed", "opposite", "mutated-other-stock", "replaced-source", "replaced-receiver")) {
            f.reset(); var into = machine(f, f.base, Direction.DOWN); var probe = probe(f, f.base.below());
            probe.selected = branch.equals("null-type") ? null : branch.equals("primal") ? Aspect.FIRE : Aspect.MAGIC;
            if (branch.equals("minimum-equality")) probe.minimum = 128;
            if (branch.equals("minimum-high")) probe.minimum = 129;
            if (branch.equals("tie")) probe.pressure = 128;
            if (branch.equals("closed")) probe.output = false;
            if (branch.equals("opposite")) probe.open = false;
            if (branch.equals("second-tie")) probe.query = () -> probe.pressure = 128;
            if (branch.equals("mutated-other-stock")) probe.query = () -> probe.stock = immutable(Map.of(Aspect.FIRE, 2, Aspect.MAGIC, 1, Aspect.WATER, 1));
            if (branch.equals("replaced-source")) probe.query = () -> f.level.setBlock(probe.getBlockPos(), Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            if (branch.equals("replaced-receiver")) probe.query = () -> { f.level.setBlock(f.base, Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); f.level.setBlock(f.base, ThaumcraftTubes.ESSENTIA_CRYSTALLIZER.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); };
            f.check(EssentiaTransfers.pullCrystallizerOne(into).committedUnits() == 0 && probe.queries == 0, "public profile cannot externally acquire native fill permission");
            ticks(f, into, 4); f.check(probe.queries == 0, "no query before actual phase5"); tick(f, into);
            boolean accepted = List.of("primal", "compound", "minimum-equality").contains(branch);
            boolean queried = accepted || List.of("null-type", "second-tie", "mutated-other-stock", "replaced-source", "replaced-receiver").contains(branch);
            f.check(probe.unexpected == null && probe.queries == (queried ? 1 : 0) && probe.faces.stream().allMatch(face -> face == Direction.UP) && probe.writes == (accepted ? 1 : 0)
                    && (accepted ? into.getAspect() == probe.selected && into.getProgress() == 0 && probe.total() == 3 : into.getAspect() == null && probe.total() == 4)
                    && !EssentiaTransfers.isLocked(probe) && !EssentiaTransfers.isLocked(into), "actual CRYSTALLIZER native ticker: ONE query/no resample/full snapshot/stale identity/positive pressure/minimum equality: " + branch);
            if (branch.equals("second-tie")) f.check(probe.pressures == 2, "second strict pressure test occurs AFTER original one type query");
        }
    }

    private static void nativeInventories(Fixture f) {
        for (String branch : List.of("selected-merge", "partner-merge", "selected-empty", "no-far-empty", "strict-components")) {
            f.reset(); var tile = machine(f, f.base, Direction.DOWN); BlockPos selectedPos = f.base.above();
            f.put(selectedPos, Blocks.CHEST); var selected = (ChestBlockEntity) f.level.getBlockEntity(selectedPos);
            ChestBlockEntity partner = null;
            if (!branch.equals("strict-components")) {
                f.level.setBlock(selectedPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT), TubeVariantsRuntimeChecks.QUIET);
                BlockPos partnerPos = selectedPos.relative(ChestBlock.getConnectedDirection(f.level.getBlockState(selectedPos)));
                f.level.setBlock(partnerPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT), TubeVariantsRuntimeChecks.QUIET); partner = (ChestBlockEntity) f.level.getBlockEntity(partnerPos);
                f.check(partner != null && partner.getBlockPos().relative(ChestBlock.getConnectedDirection(partner.getBlockState())).equals(selectedPos), "real complementary native double-chest topology, not combined54-slot wrapper");
            }
            if (branch.equals("selected-merge")) selected.setItem(10, ItemCrystalEssence.create(Aspect.FIRE).copyWithCount(63));
            if (branch.equals("partner-merge")) partner.setItem(12, ItemCrystalEssence.create(Aspect.FIRE).copyWithCount(63));
            if (branch.equals("selected-empty") || branch.equals("no-far-empty")) { fill(selected); if (branch.equals("selected-empty")) selected.setItem(5, ItemStack.EMPTY); }
            if (branch.equals("strict-components")) {
                fill(selected); selected.setItem(0, ItemCrystalEssence.create(Aspect.WATER).copyWithCount(63));
                var named = ItemCrystalEssence.create(Aspect.FIRE).copyWithCount(63); named.set(DataComponents.CUSTOM_NAME, Component.literal("not-the-same-crystal")); selected.setItem(1, named);
                var listed = ItemCrystalEssence.create(Aspect.FIRE).copyWithCount(63); ((ItemCrystalEssence) listed.getItem()).setAspects(listed, new AspectList().add(Aspect.FIRE, 1).add(Aspect.AIR, 1)); selected.setItem(2, listed);
            }
            var before = selected.saveWithFullMetadata(f.level.registryAccess()); produce(f, tile, Aspect.FIRE);
            boolean residual = branch.equals("no-far-empty") || branch.equals("strict-components");
            f.check(tile.getAspect() == null && tile.getProgress() == 0 && f.pendingRows().size() == (residual ? 1 : 0) && crystalDrops(f) == 0, "completion retires INPUT and exactly ONE owner: native slot OR level escrow, never early ADD/dual ownership: " + branch);
            if (branch.equals("selected-merge")) f.check(selected.getItem(0).isEmpty() && selected.getItem(10).getCount() == 64 && partner.isEmpty(), "selected MERGE precedes its empty earlier slot and partner");
            if (branch.equals("partner-merge")) f.check(selected.isEmpty() && partner.getItem(12).getCount() == 64, "far-half MATCHING merge precedes selected empty slots");
            if (branch.equals("selected-empty")) { assertTyped(f, selected.getItem(5), Aspect.FIRE, 1); f.check(partner.isEmpty(), "only the selected half may fill an empty slot"); }
            if (residual) { f.check(before.equals(selected.saveWithFullMetadata(f.level.registryAccess())) && (partner == null || partner.isEmpty()), "full/strict-component refusal changes NEITHER chest; empty far half is NOT a destination"); f.flushFresh(); var drop = onlyCrystal(f); geometry(f, drop, f.base, Direction.DOWN); assertTyped(f, drop.getItem(), Aspect.FIRE, 1); drop.discard(); }
        }
        for (boolean initiallyEmpty : new boolean[] {true, false}) {
            f.reset(); var tile = machine(f, f.base, Direction.DOWN); BlockPos pos = f.base.above();
            // A disabled hopper needs REAL adjacent power. Merely setting its
            // property is immediately corrected by native onPlace, potentially
            // leaving a stale constructor state that MUST NOT receive a credit.
            f.put(pos.above(), Blocks.REDSTONE_BLOCK);
            f.level.setBlock(pos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.ENABLED, false), TubeVariantsRuntimeChecks.QUIET); var hopper = (HopperBlockEntity) f.level.getBlockEntity(pos);
            f.check(!tile.isPowered() && f.level.hasNeighborSignal(pos) && !f.level.getBlockState(pos).getValue(HopperBlock.ENABLED)
                    && hopper.getBlockState() == f.level.getBlockState(pos), "physically powered disabled hopper has coherent native identity/state while the producer remains UNPOWERED");
            if (!initiallyEmpty) hopper.setItem(3, ItemCrystalEssence.create(Aspect.FIRE).copyWithCount(63));
            produce(f, tile, Aspect.FIRE); int slot = initiallyEmpty ? 0 : 3;
            f.check(!hopper.getItem(slot).isEmpty(), "native hopper route diagnostic (assertions unchanged): initiallyEmpty=" + initiallyEmpty
                    + "; actual=" + hopper.saveWithFullMetadata(f.level.registryAccess()) + "; queue=" + f.pendingRows()
                    + "; implementsNative=" + (hopper instanceof thaumcraft.common.lib.essentia.NativeCrystalInventoryAccess)
                    + "; cached=" + hopper.getBlockState() + "; world=" + f.level.getBlockState(pos));
            assertTyped(f, hopper.getItem(slot), Aspect.FIRE, initiallyEmpty ? 1 : 64);
            f.check(hopper.saveWithFullMetadata(f.level.registryAccess()).getInt("TransferCooldown") == 8 && f.pendingRows().isEmpty(), "real native hopper commits cooldownEXACT8 with credit, including nonempty/powered target; no residual right");
        }
        for (Direction inputFace : List.of(Direction.UP, Direction.DOWN, Direction.EAST)) {
            f.reset(); var tile = machine(f, f.base, inputFace); BlockPos pos = f.base.relative(inputFace.getOpposite()); f.put(pos, Blocks.FURNACE); var furnace = (FurnaceBlockEntity) f.level.getBlockEntity(pos);
            produce(f, tile, Aspect.FIRE);
            if (inputFace == Direction.UP) { assertTyped(f, furnace.getItem(0), Aspect.FIRE, 1); f.check(furnace.getItem(1).isEmpty() && furnace.getItem(2).isEmpty() && f.pendingRows().isEmpty(), "native furnace top exposes input0 and its real slot validity permits ordinary input; no invented smelting-output shortcut"); }
            else { f.check(furnace.isEmpty() && f.pendingRows().size() == 1, "native furnace side/fuel and bottom/result restrictions refuse crystal rather than bypass sided validity: " + inputFace); f.flushFresh(); var drop = onlyCrystal(f); geometry(f, drop, f.base, inputFace); drop.discard(); }
        }
    }

    private static void unsupportedNeverCalled(Fixture f) {
        f.reset(); var tile = machine(f, f.base, Direction.NORTH); BlockPos target = f.base.south(); f.put(target, Blocks.CHEST); f.level.removeBlockEntity(target);
        var malicious = new WriteThenThrowChest(target, f.level.getBlockState(target)); f.level.setBlockEntity(malicious);
        produce(f, tile, Aspect.FIRE);
        f.check(malicious.reads == 0 && malicious.writes == 0 && malicious.policies == 0 && tile.getAspect() == null && f.pendingRows().size() == 1, "unproven Container SUBCLASS is refused before ANY getter/policy/write; a setter that writes-then-throws is NEVER tried");
        f.flushFresh(); var drop = onlyCrystal(f); geometry(f, drop, f.base, Direction.NORTH); assertTyped(f, drop.getItem(), Aspect.FIRE, 1); drop.discard();
    }

    private static void startProducedScanAndDistillation(GameTestHelper test, Fixture f) {
        f.reset(); var tile = machine(f, f.base, Direction.DOWN); produce(f, tile, Aspect.ENERGY); f.flushFresh(); ItemEntity actual = onlyCrystal(f); geometry(f, actual, f.base, Direction.DOWN); assertTyped(f, actual.getItem(), Aspect.ENERGY, 1); roundTrip(f, actual.getItem());
        var saved = new CompoundTag(); f.check(actual.save(saved), "real produced physical crystal entity saves native metadata");
        var decoded = EntityType.loadEntityRecursive(saved, f.level, java.util.function.Function.identity());
        f.check(decoded instanceof ItemEntity && ItemStack.matches(actual.getItem(), ((ItemEntity) decoded).getItem()) && actual.getUUID().equals(decoded.getUUID()), "produced drop native entity save/load retains type1/count/UUID without adding another world owner");
        KnowledgeAccess.get(f.player).copyFrom(new PlayerKnowledge()); for (Aspect primal : Aspect.getPrimalAspects()) KnowledgeAccess.get(f.player).addDiscoveredAspect(primal.getTag());
        f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.THAUMOMETER.get())); f.aim(actual.getBoundingBox().getCenter());
        var tool = f.player.getMainHandItem(); f.check(tool.getItem().use(f.level, f.player, InteractionHand.MAIN_HAND).getResult().consumesAction() && tool.has(ThaumcraftContent.SCAN_SESSION.get()), "actual native thaumometer target ray resolves the REAL produced typed crystal, starting its real20-tick use");
        long started = f.level.getGameTime();
        test.onEachTick(() -> {
            if (f.closed) return;
            try {
                f.check(!actual.isRemoved(), "scan target was not replaced/granted/relocated or auto-picked up"); f.aim(actual.getBoundingBox().getCenter());
                if (f.player.isUsingItem()) tool.getItem().onUseTick(f.level, f.player, tool, f.player.getUseItemRemainingTicks());
                if (!KnowledgeAccess.get(f.player).getScannedItems().contains("@thaumcraft:crystal_essence")) return;
                f.check(f.level.getGameTime() - started >= 20 && KnowledgeAccess.get(f.player).hasDiscoveredAspect("potentia") && KnowledgeAccess.get(f.player).getAspectPoolFor("potentia") == 3 && KnowledgeAccess.get(f.player).getAspectPoolFor("auram") == 0, "real elapsed20 scan awards ONLY dynamic potentia1 plus original discovery2; no auram/shard/item recipe base");
                actual.playerTouch(f.player); f.check(actual.isRemoved() && f.player.getInventory().countItem(ThaumcraftContent.CRYSTAL_ESSENCE.get()) == 1, "actual zero-delay physical output enters native SURVIVAL inventory without relocating output");
                f.check(tile.addToContainer(Aspect.ENERGY, 1) == 0, "second declared finite raw essentia input");
                for (int calls = 0; calls < 1000 && tile.getAspect() != null; calls++) tick(f, tile);
                f.check(tile.getAspect() == null && f.pendingRows().size() == 1, "second actual production cycle reaches its own native fifth phase, never public complete"); f.flushFresh(); var second = onlyCrystal(f); second.playerTouch(f.player);
                f.check(second.isRemoved() && f.player.getInventory().countItem(ThaumcraftContent.CRYSTAL_ESSENCE.get()) == 2, "two actually produced same-component typed1 crystals merge in native inventory to count2");
                distillProducedPair(f); f.close(); test.succeed();
            } catch (RuntimeException | Error failure) { f.close(); throw failure; }
        });
        test.runAfterDelay(90, () -> { if (!f.closed) { f.close(); test.fail("produced crystal did not complete actual stable native20-tick thaumometer scan within90 ticks"); } });
    }

    private static void distillProducedPair(Fixture f) {
        int earned = -1; for (int slot = 0; slot < 36; slot++) if (f.player.getInventory().getItem(slot).is(ThaumcraftContent.CRYSTAL_ESSENCE.get())) { earned = slot; break; }
        f.check(earned >= 0, "actually earned crystal stack is still in actor inventory"); var stack = f.player.getInventory().getItem(earned); assertTyped(f, stack, Aspect.ENERGY, 2);
        for (int count : new int[] {1, 2, 64}) { var tags = ObjectAspectResolver.get(stack.copyWithCount(count), f.level); f.check(tags.size() == 1 && tags.getAmount(Aspect.ENERGY) == 1, "dynamic resolver uses contained1 PER ITEM, never count2/64 multiplier or doubled base"); }
        BlockPos pos = f.base.east(2); f.put(pos, ThaumcraftDistillation.ALCHEMY_FURNACE.get()); f.put(pos.above(), ThaumcraftAlembics.ALEMBIC.get()); var furnace = (TileAlchemyFurnace) f.level.getBlockEntity(pos); var alembic = (TileAlembic) f.level.getBlockEntity(pos.above());
        f.player.stopUsingItem(); f.aim(Vec3.atCenterOf(pos)); f.check(f.use(pos).consumesAction() && f.player.containerMenu instanceof ContainerAlchemyFurnace, "real SURVIVAL native block/menu opens to distill produced stock"); var menu = (ContainerAlchemyFurnace) f.player.containerMenu;
        int ownedSlot = earned; int nativeSlot = menu.slots.stream().filter(slot -> slot.container == f.player.getInventory() && slot.getContainerSlot() == ownedSlot).findFirst().orElseThrow().index;
        f.check(menu.quickMoveStack(f.player, nativeSlot).getCount() == 2 && furnace.getItem(0).getCount() == 2, "real menu quick-moves ONLY the two produced physical crystals");
        f.player.getInventory().setItem(9, new ItemStack(Items.COAL)); nativeSlot = menu.slots.stream().filter(slot -> slot.container == f.player.getInventory() && slot.getContainerSlot() == 9).findFirst().orElseThrow().index;
        f.check(menu.quickMoveStack(f.player, nativeSlot).is(Items.COAL) && furnace.getItem(1).getCount() == 1, "one finite ordinary COAL fixture enters actual fuel slot"); f.player.closeContainer();
        for (int call = 0; call < 200; call++) {
            TileAlchemyFurnace.tick(f.level, pos, f.level.getBlockState(pos), furnace);
            f.check(furnace.getItem(0).getCount() + furnace.getVis() + alembic.getAmount() == 2 && furnace.getAspects().getAmount(Aspect.AURA) == 0 && (alembic.getAmount() == 0 || alembic.getAspect() == Aspect.ENERGY), "EVERY native consumption/extraction conserves exact two earned units, never2xcount/auram/shard aspects");
        }
        f.check(furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty() && furnace.getVis() == 0 && alembic.getAmount() == 2 && alembic.getAspect() == Aspect.ENERGY, "real produced2 -> native alchemy furnace/alembic potentia2, no item remainder/free output or double count");
    }

    private static void nativeDirtyInputPair(Fixture f) {
        f.reset(); var tile = machine(f, f.base, Direction.DOWN); var source = TubeBufferRuntimeChecks.buffer(f, f.base.below());
        source.addToContainer(Aspect.FIRE, 1); source.addToContainer(Aspect.MAGIC, 1); source.addToContainer(Aspect.FIRE, 1);
        TileTube paid = f.tube(f.base.south(), BlockTube.Kind.FILTER); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("aqua")); f.aim(Vec3.atCenterOf(paid.getBlockPos()));
        f.check(f.use(paid.getBlockPos()).consumesAction() && f.player.getMainHandItem().isEmpty() && paid.getFilter() == Aspect.WATER, "a genuine accessible native filter label is paid BEFORE nested retirement");
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("aqua").copyWithCount(3));
        var sourceDirty = comparator(f, source.getBlockPos().west(), Direction.EAST); var receiverDirty = comparator(f, f.base.east(), Direction.WEST); Aspect[] chosen = {null};
        sourceDirty.action = () -> {
            chosen[0] = tile.getAspect();
            f.check((chosen[0] == Aspect.FIRE || chosen[0] == Aspect.MAGIC) && source.getAmount() == 2 && tile.getProgress() == 0 && tile.getTickCount() == 5
                    && EssentiaTransfers.isLocked(source) && EssentiaTransfers.isLocked(tile), "FIRST real source dirty callback sees BOTH source-1 and input1 at progress0 under shared locks");
            fence(f, tile); f.check(source.addToContainer(Aspect.WATER, 1) == 1 && !source.takeFromContainer(chosen[0], 1), "whole native source pool cannot reenter during paired draw");
            f.level.setBlock(paid.getBlockPos(), Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); f.level.setBlock(paid.getBlockPos(), ThaumcraftTubes.ESSENTIA_FILTER.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            f.aim(Vec3.atCenterOf(paid.getBlockPos()));
            f.check(paid.getFilter() == null && paid.isRemoved() && f.pendingRows().size() == 1 && EssentiaTransfers.isLocked(source) && EssentiaTransfers.isLocked(tile)
                    && BlockTube.tryLabelInteraction(f.context(paid.getBlockPos())) == InteractionResult.FAIL && f.player.getMainHandItem().getCount() == 3, "nested paid-filter retirement enqueues ONE blank, retains crystallizer/source parent locks, refuses another attachment");
        };
        sourceDirty.failure = "crystallizer-source-dirty-fixture";
        receiverDirty.action = () -> { f.check(chosen[0] != null && sourceDirty.observations == 1 && source.getAmount() == 2 && tile.getAspect() == chosen[0] && tile.getProgress() == 0, "real receiver dirty is attempted in FINALLY after source notice throws; both paid fields already committed"); fence(f, tile); };
        receiverDirty.failure = "crystallizer-receiver-dirty-fixture"; ticks(f, tile, 5);
        f.check(sourceDirty.unexpected == null && receiverDirty.unexpected == null && sourceDirty.observations == 1 && receiverDirty.observations == 1 && tile.getAspect() == chosen[0] && tile.getProgress() == 0 && tile.getTickCount() == 5
                && source.getAmount() + (tile.getAspect() == null ? 0 : 1) == 3 && !EssentiaTransfers.isLocked(source) && !EssentiaTransfers.isLocked(tile), "both actual native dirty failures preserve exact draw1, original fill progress0 and all locks unwind");
        f.flushFresh(); f.check(f.blankDrops() == 1 && crystalDrops(f) == 0, "only the independently paid label retirement is deliverable; processing input is NOT free output");
    }

    private static void completionAndInventoryCallbacks(Fixture f) {
        for (boolean replace : new boolean[] {false, true}) {
            f.reset(); var tile = machine(f, f.base, Direction.DOWN); f.put(f.base.above(), Blocks.CHEST); var chest = (ChestBlockEntity) f.level.getBlockEntity(f.base.above()); chest.setItem(10, ItemCrystalEssence.create(Aspect.FIRE).copyWithCount(63));
            tile.addToContainer(Aspect.FIRE, 1); ticks(f, tile, 999); f.check(tile.getProgress() == 199, "completion callback reached ONLY by999 real native baseline calls");
            var dirty = comparator(f, f.base.east(), Direction.WEST); CompoundTag[] savedRouting = {null}, consumedBE = {null}; UUID[] id = {null};
            dirty.action = () -> {
                f.check(tile.getAspect() == null && tile.getProgress() == 0 && tile.getTickCount() == 1000 && chest.getItem(10).getCount() == 63 && f.pendingRows().size() == 1 && EssentiaTransfers.isLocked(tile), "FIRST completion notice: input ALREADY consumed, one paid ROUTING escrow ALREADY saved, native inventory NOT credited yet");
                var row = f.pendingRows().getFirst(); id[0] = row.getUUID("Id"); f.check(row.getBoolean("Routing") && row.getInt("Count") == 1, "pre-callback durable owner is the original stable-ID routing record");
                savedRouting[0] = ownedSnapshot(f, queue(f).save(new CompoundTag(), f.level.registryAccess()), id[0]); consumedBE[0] = tile.saveWithFullMetadata(f.level.registryAccess());
                fence(f, tile); queue(f).flush(f.level);
                var restored = DeferredItemDrops.load(savedRouting[0], f.level.registryAccess()); restored.flush(f.level);
                f.check(restored.pendingIds().equals(List.of(id[0])) && crystalDrops(f) == 0 && f.pendingRows().size() == 1, "neither nested same queue nor a snapshot-loaded queue may deliver during the active native completion frame");
                if (replace) {
                    f.level.setBlock(f.base, Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); f.level.setBlock(f.base, ThaumcraftTubes.ESSENTIA_CRYSTALLIZER.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                    var fresh = (TileEssentiaCrystalizer) f.level.getBlockEntity(f.base); f.check(fresh != tile && fresh.getAspect() == null && fresh.addToContainer(Aspect.WATER, 1) == 1, "new native replacement cannot escape parent scope to accept a second input while paid output is routing");
                }
            };
            dirty.failure = "crystallizer-completion-dirty-fixture"; tick(f, tile);
            f.check(dirty.unexpected == null && dirty.observations == 1 && savedRouting[0] != null && consumedBE[0] != null && tile.getAspect() == null && tile.getProgress() == 0 && !EssentiaTransfers.isLocked(tile), "throwing completion notice never refunds input, repeats conversion or leaks shared scope");
            if (replace) {
                f.check(chest.getItem(10).getCount() == 63 && f.pendingRows().size() == 1 && f.pendingRows().getFirst().getUUID("Id").equals(id[0]), "replacement prevents inventory route, but level owns the SAME one paid output");
                restoreOwnedRow(f, savedRouting[0], id[0]);
                var fresh = CrystallizerRuntimeChecks.reload(f, consumedBE[0]); ticks(f, fresh, 11);
                f.check(fresh.getAspect() == null && fresh.getProgress() == 0 && fresh.getTickCount() == 11 && f.pendingRows().size() == 1 && !f.pendingRows().getFirst().getBoolean("Routing"), "coherent consumed-BE and savedROUTING reload resets transient phase; paid row restores DROP-only, never INPUT or inventory insertion");
                quarantinePending(f, ownedSnapshot(f, queue(f).save(new CompoundTag(), f.level.registryAccess()), id[0]));
                f.flushFresh(); var drop = onlyCrystal(f); f.check(drop.getUUID().equals(id[0]) && chest.getItem(10).getCount() == 63, "source replacement/reload cannot lose UUID or replay native inventory route"); assertTyped(f, drop.getItem(), Aspect.FIRE, 1); drop.discard();
            } else {
                f.check(chest.getItem(10).getCount() == 64 && f.pendingRows().isEmpty() && crystalDrops(f) == 0, "native slot credit retires escrow even after earlier machine notice failed");
                var oldSnapshot = DeferredItemDrops.load(savedRouting[0], f.level.registryAccess()); oldSnapshot.flush(f.level);
                f.check(oldSnapshot.pendingCount() == 0 && chest.getItem(10).getCount() == 64 && crystalDrops(f) == 0, "same-running-level receipt recognizes prior settled inventory credit; stale snapshot cannot also spawn a crystal (not a power-loss guarantee)");
                ticks(f, tile, 8); f.check(tile.getAspect() == null && tile.getProgress() == 0 && f.pendingRows().isEmpty(), "later ticks do not retry paid conversion/escrow");
            }
        }
        f.reset(); var tile = machine(f, f.base, Direction.DOWN); BlockPos selectedPos = f.base.above();
        f.level.setBlock(selectedPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT), TubeVariantsRuntimeChecks.QUIET);
        BlockPos partnerPos = selectedPos.relative(ChestBlock.getConnectedDirection(f.level.getBlockState(selectedPos)));
        f.level.setBlock(partnerPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT), TubeVariantsRuntimeChecks.QUIET);
        var selected = (ChestBlockEntity) f.level.getBlockEntity(selectedPos); var partner = (ChestBlockEntity) f.level.getBlockEntity(partnerPos); selected.setItem(10, ItemCrystalEssence.create(Aspect.FIRE).copyWithCount(63));
        tile.addToContainer(Aspect.FIRE, 1); ticks(f, tile, 999); var inventoryDirty = comparator(f, selectedPos.south(), Direction.NORTH);
        inventoryDirty.action = () -> {
            f.check(tile.getAspect() == null && selected.getItem(10).getCount() == 64 && partner.isEmpty() && f.pendingRows().isEmpty() && EssentiaTransfers.isLocked(tile) && EssentiaTransfers.isLocked(selected) && EssentiaTransfers.isLocked(partner), "FIRST real inventory dirty sees native credit AND escrow retirement committed together, with BOTH chest halves and machine locked");
            fence(f, tile); ItemStack moved = selected.removeItem(10, 1); f.check(moved.getCount() == 1 && f.player.getInventory().add(moved) && selected.getItem(10).getCount() == 63, "actual post-commit callback can MOVE the earned unit into player inventory; readback equality cannot be an ownership oracle");
            f.level.setBlock(partnerPos, Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            f.check(EssentiaTransfers.isLocked(partner) && f.pendingRows().isEmpty(), "post-commit partner replacement cannot reopen residual escrow or unlock parent scope early");
        };
        inventoryDirty.failure = "crystallizer-inventory-dirty-fixture"; tick(f, tile);
        f.check(inventoryDirty.unexpected == null && inventoryDirty.observations == 1 && tile.getAspect() == null && selected.getItem(10).getCount() == 63 && f.player.getInventory().countItem(ThaumcraftContent.CRYSTAL_ESSENCE.get()) == 1 && f.pendingRows().isEmpty() && !EssentiaTransfers.isLocked(selected) && !EssentiaTransfers.isLocked(partner), "dirty write-THEN-move-THEN-throw preserves exactly one NEW native unit, never catch-and-drop/refund/replay escrow");
        f.flushFresh(); ticks(f, tile, 8); f.check(crystalDrops(f) == 0 && f.pendingRows().isEmpty(), "later delivery/ticks cannot also emit the already-moved inventory crystal");
    }

    private static void fence(Fixture f, TileEssentiaCrystalizer tile) {
        var before = tile.saveWithFullMetadata(f.level.registryAccess()); int count = tile.getTickCount(), progress = tile.getProgress();
        f.check(tile.addToContainer(Aspect.WATER, 1) == 1 && tile.addEssentia(Aspect.WATER, 1, tile.getFacing()) == 0 && !tile.takeFromContainer(tile.getAspect(), 1)
                && tile.takeEssentia(Aspect.FIRE, 1, tile.getFacing()) == 0 && EssentiaTransfers.finishCrystallizer(tile).committedUnits() == 0 && EssentiaTransfers.pullCrystallizerOne(tile).committedUnits() == 0
                && denied(() -> tile.loadWithComponents(before, f.level.registryAccess())), "shared native notice fences raw add/take/transport, pending reload, fill and output phase reentry");
        var insert = tile.prepareInsert(tile.snapshotAtomicState(), Aspect.WATER, 1); if (insert != null) f.check(denied(() -> tile.applyAtomicRaw(insert)), "prepared exact changes cannot bypass executor-bound authorization");
        tick(f, tile); f.check(tile.getTickCount() == count && tile.getProgress() == progress && before.equals(tile.saveWithFullMetadata(f.level.registryAccess())), "reentrant REAL native ticker cannot advance count/progress or mint another paid output");
    }

    private static void quarantinePending(Fixture f, CompoundTag valid) {
        var original = valid.getList("Pending", Tag.TAG_COMPOUND).getCompound(0);
        for (String branch : List.of("count", "face", "geometry", "route-type")) {
            var bad = valid.copy(); var row = bad.getList("Pending", Tag.TAG_COMPOUND).getCompound(0);
            switch (branch) { case "count" -> row.putInt("Count", 2); case "face" -> row.putInt("CrystalFace", 6); case "geometry" -> row.putDouble("X", row.getDouble("X") + .1); case "route-type" -> row.putString("Routing", "false"); default -> throw new AssertionError(branch); }
            var invalid = DeferredItemDrops.load(bad, f.level.registryAccess()); var saved = invalid.save(new CompoundTag(), f.level.registryAccess());
            f.check(invalid.pendingCount() == 0 && invalid.quarantinedCount() == 1 && saved.getList("Quarantined", Tag.TAG_COMPOUND).getCompound(0).equals(row) && !row.equals(original), "whole malformed PAID crystal row is quarantined verbatim, not truncated/rerouted/free output: " + branch);
        }
    }

    private static void startDurableRetries(GameTestHelper test, Fixture f) {
        f.reset(); fizzSounds(f); var attempts = new LinkedHashMap<UUID, Delivery>();
        BlockPos[] positions = {f.base, f.base.east(2), f.base.above(2)}; Direction[] facings = {Direction.WEST, Direction.NORTH, Direction.UP}; Aspect[] types = {Aspect.FIRE, Aspect.WATER, Aspect.ENERGY};
        for (int index = 0; index < positions.length; index++) {
            var tile = machine(f, positions[index], facings[index]); produce(f, tile, types[index]);
            var row = f.pendingRows().stream().filter(entry -> BlockPos.of(entry.getLong("Source")).equals(tile.getBlockPos())).findFirst().orElseThrow();
            attempts.put(row.getUUID("Id"), new Delivery(tile, facings[index], types[index], index));
        }
        f.check(f.pendingRows().size() == 3 && crystalDrops(f) == 0 && fizzSounds(f) == 3, "three finite paid completions: three escrow owners, no ADD before level-post, native fizz exactly ONCE per ejection");
        var collision = attempts.entrySet().stream().filter(entry -> entry.getValue().kind == 2).findFirst().orElseThrow();
        var blocker = new ItemEntity(f.level, f.base.getX() + .5, f.base.getY() + 1.85, f.base.getZ() + .5, new ItemStack(Items.PAPER)); blocker.setUUID(collision.getKey());
        f.check(f.level.addFreshEntity(blocker), "real unrelated world UUID collision fixture, NOT a relocated crystal");
        f.check(f.level.destroyBlock(positions[0], true, f.player), "actual source break AFTER paid detachment BEFORE delivery"); f.drops();
        EntityEvent.Add listener = (entity, world) -> {
            if (world != f.level || !(entity instanceof ItemEntity drop) || !drop.getItem().is(ThaumcraftContent.CRYSTAL_ESSENCE.get())) return EventResult.pass();
            Delivery expected = attempts.get(drop.getUUID()); if (expected == null) return EventResult.pass(); expected.calls++;
            try {
                f.check(expected.old.getAspect() == null && expected.old.getProgress() == 0 && f.pendingRows().stream().filter(row -> row.getUUID("Id").equals(drop.getUUID())).count() == 1, "real ADD hook observes input cleared and SAME one authoritative IN_FLIGHT saved row, not only a local Java variable");
                assertTyped(f, drop.getItem(), expected.type, 1); geometry(f, drop, expected.old.getBlockPos(), expected.facing);
                expected.snapshot = ownedSnapshot(f, queue(f).save(new CompoundTag(), f.level.registryAccess()), drop.getUUID());
                var restored = DeferredItemDrops.load(expected.snapshot, f.level.registryAccess()); f.check(restored.pendingIds().equals(List.of(drop.getUUID())), "save FROM actual ADD round-trips complete in-flight paid owner with stable UUID");
                restored.flush(f.level); queue(f).flush(f.level);
                f.check(restored.pendingIds().equals(List.of(drop.getUUID())) && f.level.getEntity(drop.getUUID()) == null, "nested loaded or live flush cannot deliver/reserve a SECOND entity during exact active ADD attempt");
                int carried = f.player.getInventory().countItem(ThaumcraftContent.CRYSTAL_ESSENCE.get()); drop.playerTouch(f.player);
                f.check(f.player.getInventory().countItem(ThaumcraftContent.CRYSTAL_ESSENCE.get()) == carried && !drop.isRemoved(), "unadmitted ADD candidate cannot be picked up before native world ownership exists");
                f.check(EssentiaTransfers.finishCrystallizer(expected.old).committedUnits() == 0, "ADD reentry cannot replay private completion permission");
                if (expected.calls == 1) {
                    expected.rejectedAt = f.level.getGameTime();
                    if (expected.kind == 1) {
                        var state = f.level.getBlockState(expected.old.getBlockPos()); var consumed = expected.old.saveWithFullMetadata(f.level.registryAccess()); f.level.removeBlockEntity(expected.old.getBlockPos());
                        var reloaded = BlockEntity.loadStatic(expected.old.getBlockPos(), state, consumed, f.level.registryAccess()); f.check(reloaded instanceof TileEssentiaCrystalizer, "actual native BE detach/load while output in flight"); f.level.setBlockEntity(reloaded);
                        var fresh = (TileEssentiaCrystalizer) reloaded; f.check(fresh.getAspect() == null && fresh.getTickCount() == 0 && fresh.getProgress() == 0 && f.level.destroyBlock(fresh.getBlockPos(), true, f.player), "consumed source reload/break cannot resurrect paid input or lose level escrow"); f.drops();
                        throw new IllegalStateException("crystallizer-ADD-failure-fixture");
                    }
                    return EventResult.interruptFalse();
                }
                if (expected.kind == 2 && !expected.restored) return EventResult.interruptFalse();
                f.check(f.level.getGameTime() - expected.rejectedAt >= 20, "ordinary native retry waits >=20 actual world ticks, never rewritten clocks");
                return EventResult.pass();
            } catch (RuntimeException | Error failure) {
                if (!(failure instanceof IllegalStateException && "crystallizer-ADD-failure-fixture".equals(failure.getMessage()))) expected.unexpected = failure;
                throw failure;
            }
        };
        boolean[] registered = {true}; EntityEvent.ADD.register(listener);
        Runnable close = () -> { if (registered[0]) { EntityEvent.ADD.unregister(listener); registered[0] = false; } f.close(); };
        try { park(f); queue(f).flush(f.level); }
        catch (RuntimeException | Error failure) { close.run(); throw failure; }
        test.runAfterDelay(2, () -> { if (!f.closed) { try { f.check(f.level.getEntity(collision.getKey()) == blocker && collision.getValue().calls == 0, "conflicting native UUID is retained, never overwritten or evaded with a new ticket ID"); blocker.discard(); } catch (RuntimeException | Error failure) { close.run(); throw failure; } } });
        test.onEachTick(() -> {
            if (f.closed) return;
            try {
                park(f); boolean complete = true;
                for (var entry : attempts.entrySet()) {
                    UUID id = entry.getKey(); Delivery expected = entry.getValue(); f.check(expected.unexpected == null && expected.old.getAspect() == null, "unexpected ADD callback assertions are retained OUTSIDE production's caught exception path");
                    long pending = f.pendingRows().stream().filter(row -> row.getUUID("Id").equals(id)).count(); var current = f.level.getEntity(id); boolean visible = current instanceof ItemEntity item && item.getItem().is(ThaumcraftContent.CRYSTAL_ESSENCE.get()) && !item.isRemoved();
                    f.check(pending + (visible ? 1 : 0) == 1, "every paid UUID has exactly ONE escrow-or-real-world owner through collision/cancel/throw/reload/break");
                    if (expected.kind == 2 && expected.calls >= 1 && !expected.restored && f.level.getGameTime() - expected.rejectedAt >= 20) { restoreOwnedRow(f, expected.snapshot, id); expected.restored = true; }
                    if (!visible) { complete = false; continue; }
                    assertTyped(f, ((ItemEntity) current).getItem(), expected.type, 1); f.check(expected.calls >= 2 && expected.rejectedAt >= 0 && f.level.getGameTime() - expected.rejectedAt >= 20, "actual retry delivered SAME UUID/type/count after real rejection");
                    // Reloading an ADD-hook save after accepted admission recognizes
                    // the actual live entity/receipt, without a second native ADD.
                    int calls = expected.calls; var replay = DeferredItemDrops.load(expected.snapshot, f.level.registryAccess()); replay.flush(f.level);
                    f.check(replay.pendingCount() == 0 && expected.calls == calls && f.level.getEntity(id) == current, "in-flight save replay recognizes accepted owner; no second ADD or inventory retry");
                }
                if (!complete) return;
                f.check(f.pendingRows().isEmpty() && crystalDrops(f) == 3 && fizzSounds(f) == 0, "successful delivery leaves exactly three distinct typed physical outputs, zero replay escrow and NO repeated fizz/RNG effect on retries");
                close.run(); test.succeed();
            } catch (RuntimeException | Error failure) { close.run(); throw failure; }
        });
        test.runAfterDelay(160, () -> { if (!f.closed) { String detail = attempts.entrySet().stream().map(entry -> entry.getKey() + ":calls=" + entry.getValue().calls + ",first=" + entry.getValue().rejectedAt + ",restored=" + entry.getValue().restored).toList().toString(); close.run(); test.fail("native crystallizer output retry did not settle within160 real ticks: " + detail); } });
    }

    private static final class Delivery {
        final TileEssentiaCrystalizer old; final Direction facing; final Aspect type; final int kind;
        int calls; long rejectedAt = -1; boolean restored; CompoundTag snapshot; Throwable unexpected;
        Delivery(TileEssentiaCrystalizer old, Direction facing, Aspect type, int kind) { this.old = old; this.facing = facing; this.type = type; this.kind = kind; }
    }
    private static DeferredItemDrops queue(Fixture f) { var queue = f.level.getDataStorage().get(Fixture.QUEUE, Fixture.QUEUE_ID); if (queue == null) throw new AssertionError("Missing actual level SavedData output owner"); return queue; }
    private static CompoundTag ownedSnapshot(Fixture f, CompoundTag saved, UUID id) { var copy = saved.copy(); var rows = copy.getList("Pending", Tag.TAG_COMPOUND); for (int i = rows.size() - 1; i >= 0; i--) if (!rows.getCompound(i).getUUID("Id").equals(id)) rows.remove(i); f.check(rows.size() == 1, "snapshot contains exactly this owned paid UUID, never another test's pending output"); return copy; }
    /** Merge ONLY this fixture's still-pending row into current SavedData. Do not
     * restore an older whole-level snapshot over independently created outputs. */
    private static void restoreOwnedRow(Fixture f, CompoundTag saved, UUID id) { var current = queue(f).save(new CompoundTag(), f.level.registryAccess()); var rows = current.getList("Pending", Tag.TAG_COMPOUND); f.check(f.pendingRows().stream().anyMatch(row -> row.getUUID("Id").equals(id)), "restore only our EXISTING pending owner, not a settled paid item"); for (int i = rows.size() - 1; i >= 0; i--) if (rows.getCompound(i).getUUID("Id").equals(id)) rows.remove(i); var stored = saved.getList("Pending", Tag.TAG_COMPOUND).getCompound(0); f.check(stored.getUUID("Id").equals(id), "restore exact ID"); rows.add(stored.copy()); f.level.getDataStorage().set(Fixture.QUEUE_ID, DeferredItemDrops.load(current, f.level.registryAccess())); }
    private static void park(Fixture f) { f.player.setPos(f.base.getX() - 1.5, f.base.getY() - 2, f.base.getZ() - 1.5); f.player.setDeltaMovement(Vec3.ZERO); }
    private static int fizzSounds(Fixture f) { f.connection.flushChannel(); f.channel.runPendingTasks(); int count = 0; Object packet; while ((packet = f.channel.readOutbound()) != null) { try { if (packet instanceof ClientboundSoundPacket sound && sound.getSound().value().getLocation().toString().equals("minecraft:block.fire.extinguish")) { f.check(sound.getVolume() == .25F && sound.getPitch() >= 1.799F && sound.getPitch() <= 3.401F, "native original fizz volume/pitch RANGE, no seeded RNG or distribution claim"); count++; } } finally { io.netty.util.ReferenceCountUtil.release(packet); } } return count; }
    private static boolean denied(Runnable action) { try { action.run(); return false; } catch (IllegalStateException expected) { return true; } }
    private static DirtyComparator comparator(Fixture f, BlockPos pos, Direction input) { f.put(pos.below(), Blocks.STONE); f.level.setBlock(pos, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, input), TubeVariantsRuntimeChecks.QUIET); f.level.removeBlockEntity(pos); var tile = new DirtyComparator(pos, f.level.getBlockState(pos)); f.level.setBlockEntity(tile); return tile; }
    /** Vanilla setChanged -> comparator neighbor check -> getOutputSignal. Never
     * manually trigger a dirty callback; retain unexpected assertions outside
     * production's intentional catch/log path. Vanilla may wrap our deliberate
     * failures in "Exception while updating neighbours". */
    private static final class DirtyComparator extends ComparatorBlockEntity {
        Runnable action; String failure; Throwable unexpected; int observations;
        DirtyComparator(BlockPos pos, BlockState state) { super(pos, state); }
        @Override public int getOutputSignal() { if (action != null) { var once = action; action = null; observations++; try { once.run(); } catch (RuntimeException | Error error) { unexpected = error; throw error; } if (failure != null) throw new IllegalStateException(failure); } return super.getOutputSignal(); }
    }

    static ItemEntity onlyCrystal(Fixture f) { var drops = f.drops().stream().filter(drop -> drop.getItem().is(ThaumcraftContent.CRYSTAL_ESSENCE.get())).toList(); f.check(drops.size() == 1, "exactly ONE actual native crystal entity"); return drops.getFirst(); }
    private static void produce(Fixture f, TileEssentiaCrystalizer tile, Aspect type) { f.check(tile.getProgress() == 0 && tile.getTickCount() % 5 == 0 && tile.addToContainer(type, 1) == 0, "declared raw input1, real fresh baseline phase0"); ticks(f, tile, 999); f.check(tile.getAspect() == type && tile.getProgress() == 199, "999 genuine native calls do NOT output early"); tick(f, tile); f.check(tile.getAspect() == null && tile.getProgress() == 0, "1000th real production call consumes ONE typed input"); }
    private static void geometry(Fixture f, ItemEntity drop, BlockPos source, Direction facing) { Direction out = facing.getOpposite(); Vec3 expected = Vec3.atCenterOf(source).relative(out, .65); Vec3 motion = new Vec3(out.getStepX() * .04F, out.getStepY() * .04F, out.getStepZ() * .04F); f.check(drop.position().distanceToSqr(expected) < 1e-18 && drop.getDeltaMovement().distanceToSqr(motion) < 1e-18 && drop.saveWithoutId(new CompoundTag()).getShort("PickupDelay") == 0, "original opposite-facing center+.65, axis*.04F motion, pickup0; no world RNG scatter/relocation"); }
    private static void fill(Container container) { for (int slot = 0; slot < container.getContainerSize(); slot++) container.setItem(slot, new ItemStack(Items.STONE, 64)); }

    private static final class WriteThenThrowChest extends ChestBlockEntity {
        int reads, writes, policies;
        WriteThenThrowChest(BlockPos pos, BlockState state) { super(pos, state); }
        @Override public ItemStack getItem(int slot) { reads++; throw new AssertionError("Unproven chest getter was called"); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { policies++; throw new AssertionError("Unproven chest policy was called"); }
        @Override public void setItem(int slot, ItemStack stack) { writes++; super.setItem(slot, stack); throw new IllegalStateException("crystallizer-unsupported-write-then-throw-MUST-NOT-RUN"); }
    }

    // Callback/escrow and real retry cases follow below; isolated probe is only
    // for public query-count/identity adversaries, never replacement world RNG.
    private static SourceProbe probe(Fixture f, BlockPos pos) { f.put(pos, ThaumcraftTubes.ESSENTIA_TUBE.get()); f.level.removeBlockEntity(pos); var probe = new SourceProbe(pos, f.level.getBlockState(pos)); f.level.setBlockEntity(probe); return probe; }
    private static Map<Aspect, Integer> immutable(Map<Aspect, Integer> stock) { return Collections.unmodifiableMap(new LinkedHashMap<>(stock)); }
    private static final class SourceProbe extends BlockEntity implements EssentiaAtomicEndpoint {
        Map<Aspect, Integer> stock = immutable(Map.of(Aspect.FIRE, 2, Aspect.MAGIC, 2)); Aspect selected = Aspect.MAGIC;
        final List<Direction> faces = new ArrayList<>(); int queries, pressures, minimum, pressure, writes; boolean output = true, open = true; Runnable query; Throwable unexpected;
        SourceProbe(BlockPos pos, BlockState state) { super(ThaumcraftTubes.TUBE_BLOCK_ENTITY.get(), pos, state); }
        private record Stock(Map<Aspect, Integer> contents, boolean open, boolean output, int minimum, int pressure) implements State {}
        int total() { return stock.values().stream().mapToInt(Integer::intValue).sum(); }
        @Override public State snapshotAtomicState() { return new Stock(stock, open, output, minimum, pressure); }
        @Override public boolean matchesAtomicState(State before) { return before instanceof Stock old && old.contents() == stock && old.equals(snapshotAtomicState()); }
        @Override public Change prepareExtract(State before, Aspect type, int units) { if (!(before instanceof Stock old) || units != 1 || !EssentiaAtomicEndpoint.validAspect(type) || old.contents().getOrDefault(type, 0) < 1) return null; var after = new LinkedHashMap<>(old.contents()); int left = after.get(type) - 1; if (left == 0) after.remove(type); else after.put(type, left); return new Change(this, before, new Stock(immutable(after), old.open(), old.output(), old.minimum(), old.pressure()), 1, true); }
        @Override public Change prepareInsert(State before, Aspect type, int units) { return null; }
        @Override public void applyAtomicRaw(Change change) { EssentiaTransfers.requireAtomicChange(this, change); stock = ((Stock) change.after()).contents(); writes++; }
        @Override public void notifyEssentiaStorageChanged() {}
        @Override public boolean isConnectable(Direction face) { return open && face != null; }
        @Override public boolean canOutputTo(Direction face) { return output && isConnectable(face); }
        @Override public boolean canInputFrom(Direction face) { return false; }
        @Override public void setSuction(Aspect type, int amount) {}
        @Override public Aspect getSuctionType(Direction face) { return null; }
        @Override public int getSuctionAmount(Direction face) { pressures++; return pressure; }
        @Override public Aspect getEssentiaType(Direction face) { queries++; faces.add(face); if (query != null) { var once = query; query = null; try { once.run(); } catch (RuntimeException | Error error) { unexpected = error; throw error; } } return selected; }
        @Override public int getEssentiaAmount(Direction face) { return total(); }
        @Override public int getMinimumSuction() { return minimum; }
        @Override public boolean renderExtendedTube() { return false; }
        @Override public int takeEssentia(Aspect type, int units, Direction face) { throw new AssertionError("CRYSTALLIZER must never public take/add/refund"); }
        @Override public int addEssentia(Aspect type, int units, Direction face) { throw new AssertionError("CRYSTALLIZER must never public take/add/refund"); }
    }
}
