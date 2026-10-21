package thaumcraft.common.testing;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileCentrifuge;
import thaumcraft.common.tiles.TileTube;

/** M06d SERVER INPUT fixtures: raw ingredients, research, funded wands, genuine
 * discount gear, empty devices and raw essentia are supplied explicitly. Finished
 * construct/centrifuge recipe outputs are EARNED, not granted. No claim of full
 * survival acquisition; no phase/clock/reflection/RNG writes or game execution. */
public final class CentrifugeRuntimeChecks {
    private CentrifugeRuntimeChecks() {}

    public static void recipesAndPlainNativeBlocks(GameTestHelper test) {
        try (var f = new Fixture(test)) { f.open(); recipes(f); lateBindingReturnsPhialIntoPayerSlot(f); }
        test.succeed();
    }

    public static void twoCompartmentsTimingPowerAndPersistence(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open(); rawFacades(f); nativeTiming(f); poweredAndBlocked(f); persistenceAndQuarantine(f);
        }
        test.succeed();
    }

    private static void recipes(Fixture f) {
        f.reset(); f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench, "real native survival actor opens the actual workbench block/menu");
        var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base); var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
        var constructHolder = f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("arcane/alchemical_construct")).orElseThrow();
        var centrifugeHolder = f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("arcane/centrifuge")).orElseThrow();
        f.check(constructHolder.value() instanceof ArcaneShapedRecipe && centrifugeHolder.value() instanceof ArcaneShapedRecipe, "BOTH real native manager holders are shaped arcane recipes, not injected fixtures");
        var construct = (ArcaneShapedRecipe) constructHolder.value(); var machine = (ArcaneShapedRecipe) centrifugeHolder.value();
        f.check(construct.research().equals("DISTILESSENTIA") && construct.pattern().equals(List.of("VTF", "TWT", "FTV"))
                && construct.vis().equals(Map.of("aqua", 5, "ordo", 5)) && construct.result().is(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get()) && construct.result().getCount() == 1,
                "actual construct data preserves DISTILESSENTIA, literal shape with FOUR basic tubes, two valves/two Vis Filters/one greatwood, aqua5/ordo5/output1");
        f.check(machine.research().equals("CENTRIFUGE") && machine.pattern().equals(List.of(" T ", "ACP", " T "))
                && machine.vis().equals(Map.of("aqua", 5, "ordo", 5, "perditio", 5)) && machine.result().is(ThaumcraftTubes.CENTRIFUGE_ITEM.get()) && machine.result().getCount() == 1,
                "actual centrifuge data retains all explicit blank borders, CENTRIFUGE, aqua5/ordo5/perditio5 and own output1");
        constructMatrix(table, 3); table.setItem(10, wand("iron", 5000));
        KnowledgeAccess.get(f.player).addResearch("CENTRIFUGE"); KnowledgeAccess.get(f.player).addResearch("TUBEFILTER"); KnowledgeAccess.get(f.player).removeResearch("DISTILESSENTIA");
        refuse(f, table, menu, "CENTRIFUGE/TUBEFILTER do not substitute for construct's actual DISTILESSENTIA gate");
        KnowledgeAccess.get(f.player).addResearch("DISTILESSENTIA");
        for (Item wrong : List.of(Items.OAK_PLANKS, ThaumcraftNaturals.SILVERWOOD_PLANKS.get().asItem())) {
            table.setItem(4, new ItemStack(wrong, 3)); refuse(f, table, menu, "ordinary/silverwood plank is NOT greatwood");
        }
        constructMatrix(table, 3); table.setItem(2, new ItemStack(ThaumcraftTubes.ESSENTIA_FILTER_ITEM.get(), 3)); refuse(f, table, menu, "filter TUBE is not the original Vis Filter RESOURCE");
        constructMatrix(table, 3); table.setItem(7, ItemStack.EMPTY); refuse(f, table, menu, "three basic tubes cannot pay the actual FOUR-basic construct shape");
        constructMatrix(table, 3); table.setItem(3, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(), 3)); refuse(f, table, menu, "flattened valve cannot replace a basic tube");
        constructMatrix(table, 3); ItemStack funded = table.removeItemNoUpdate(10); refuse(f, table, menu, "researched construct without an inserted paying wand");
        table.setItem(10, shortWand("iron", "ordo", 549)); refuse(f, table, menu, "one centivis short of iron-cap550 refuses ALL construct ingredients/payment");
        table.setItem(10, funded); menu.broadcastChanges();
        f.check(menu.craftable() && menu.requiredVis("aqua") == 550 && menu.requiredVis("ordo") == 550
                && f.level.getRecipeManager().getRecipeFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(), ArcaneRecipeInput.snapshot(table), f.level).orElseThrow().id().equals(constructHolder.id()), "real recipe manager/menu, not a special output path, prices the actual construct");
        var mirrored = new java.util.ArrayList<>(ArcaneRecipeInput.snapshot(table).items());
        for (int row = 0; row < 3; row++) java.util.Collections.swap(mirrored, row * 3, row * 3 + 2);
        f.check(construct.matches(new ArcaneRecipeInput(mirrored), f.level), "actual original construct recipe supports its real horizontal mirror");
        takeToInventory(f, menu, ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get());
        for (int slot = 0; slot < 9; slot++) f.check(table.getItem(slot).getCount() == 2, "one actual ingredient per occupied construct cell, including each of FOUR distinct basic positions: " + slot);
        KnowledgeAccess.get(f.player).removeResearch("DISTILESSENTIA"); var paid = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.quickMoveStack(f.player, 0).isEmpty() && paid.equals(table.saveWithoutMetadata(f.level.registryAccess())), "stale funded construct preview revalidates real actor research without spending stock/vis");
        KnowledgeAccess.get(f.player).addResearch("DISTILESSENTIA"); menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        f.check(f.player.getInventory().countItem(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get()) == 3 && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty)
                && menu.quickMoveStack(f.player, 0).isEmpty(), "native quick-move earns exactly two remaining constructs and cannot replay empty ingredients");
        checkVis(f, table.getItem(10), 3350, 3350, 5000, "three REAL construct payments"); f.player.closeContainer();
        plainConstruct(f);

        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench, "reopen same native shared workbench to spend the genuinely earned dependency");
        menu = (ContainerArcaneWorkbench) f.player.containerMenu;
        ItemStack earned = removeInventory(f, ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get(), 3);
        centrifugeMatrix(table, earned, 3); table.setItem(10, wand("iron", 5000)); KnowledgeAccess.get(f.player).removeResearch("CENTRIFUGE");
        refuse(f, table, menu, "DISTILESSENTIA does not unlock the centrifuge's separate CENTRIFUGE research"); KnowledgeAccess.get(f.player).addResearch("CENTRIFUGE");
        table.setItem(5, new ItemStack(Items.STICKY_PISTON, 3)); refuse(f, table, menu, "ordinary piston means ordinary piston, not sticky"); table.setItem(5, new ItemStack(Items.PISTON, 3));
        table.setItem(1, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(), 3)); refuse(f, table, menu, "centrifuge's two basic tubes cannot be flattened variant aliases"); table.setItem(1, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), 3));
        for (int blank : new int[] {0, 2, 6, 8}) { table.setItem(blank, new ItemStack(Items.STICK)); refuse(f, table, menu, "explicit centrifuge border cell must remain blank: " + blank); table.setItem(blank, ItemStack.EMPTY); }
        table.setItem(10, shortWand("iron", "perditio", 549)); refuse(f, table, menu, "short third primal refuses the entire centrifuge transaction"); table.setItem(10, wand("iron", 5000)); menu.broadcastChanges();
        f.check(menu.craftable() && menu.requiredVis("perditio") == 550 && machine.matches(ArcaneRecipeInput.snapshot(table), f.level), "actual centrifuge recipe and native menu recognize the three earned constructs/real original matrix");
        takeToInventory(f, menu, ThaumcraftTubes.CENTRIFUGE_ITEM.get()); checkVis(f, table.getItem(10), 4450, 4450, 4450, "one iron-cap centrifuge");
        for (int slot : new int[] {1, 3, 4, 5, 7}) f.check(table.getItem(slot).getCount() == 2, "centrifuge extraction consumes ONE real ingredient/dependency per occupied cell");
        KnowledgeAccess.get(f.player).removeResearch("CENTRIFUGE"); paid = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.quickMoveStack(f.player, 0).isEmpty() && paid.equals(table.saveWithoutMetadata(f.level.registryAccess())), "stale centrifuge research preview is not take authority"); KnowledgeAccess.get(f.player).addResearch("CENTRIFUGE");
        f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get())); table.setItem(10, shortWand("thaumium", "perditio", 423)); menu.broadcastChanges();
        f.check(menu.requiredVis("aqua") == 424 && menu.requiredVis("ordo") == 424 && menu.requiredVis("perditio") == 424, "REAL worn goggles + actual thaumium cap apply original float truncation:5vis costs424, NOT425 or cap-only450");
        refuse(f, table, menu, "one short DISCOUNTED third primal still refuses all ingredients/vis"); table.setItem(10, wand("thaumium", 5000)); menu.broadcastChanges(); menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        f.check(f.player.getInventory().countItem(ThaumcraftTubes.CENTRIFUGE_ITEM.get()) == 3 && table.getItem(4).isEmpty()
                && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty) && menu.quickMoveStack(f.player, 0).isEmpty(), "native discounted quick-move pays two remaining centrifuges using ONLY earned construct stock; no replay or supplied finished machine");
        checkVis(f, table.getItem(10), 4152, 4152, 4152, "two actually paid discounted centrifuges"); f.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); f.player.closeContainer();
        plainCentrifuge(f);
    }

    private static void lateBindingReturnsPhialIntoPayerSlot(Fixture f) {
        f.reset(); f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
        // Explicit fresh SERVER INPUT fixture: reproduce the client's actual
        // occupied hotbar0..6/empty selected7, not a synthetic cell remainder.
        for (int slot = 0; slot < 7; slot++) f.player.getInventory().setItem(slot, new ItemStack(Items.STONE));
        f.player.getInventory().selected = 7; f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench, "real late-binding menu opens with empty selected payer slot7");
        var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
        var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
        ItemStack phial = ItemEssence.filled("ignis");
        phial.set(DataComponents.CUSTOM_NAME, Component.literal("actual-unchanged-late-binding-catalyst"));
        ItemStack phialBefore = phial.copy(), payerBefore = wand("gold", 1337);
        table.setItem(0, ItemJarLabel.blank()); table.setItem(8, phial); table.setItem(10, payerBefore.copy()); menu.broadcastChanges();
        f.check(menu.craftable() && ItemJarLabel.aspect(menu.getSlot(0).getItem()) == Aspect.FIRE, "actual native ordinary recipe binds a genuine one-unit label to the supplied phial");
        menu.clicked(0, 0, ClickType.PICKUP, f.player);
        int outputSlot = playerSlot(menu, f, 9), phialSlot = playerSlot(menu, f, 10), payerSlot = playerSlot(menu, f, 7);
        menu.clicked(outputSlot, 0, ClickType.PICKUP, f.player);
        f.check(ItemStack.matches(f.player.getInventory().getItem(7), phialBefore) && ItemStack.matches(table.getItem(10), payerBefore)
                && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty),
                "original inventory-FIRST remainder really occupies empty payer7 with identical components/count, NOT a matrix refund or vis debit");
        menu.clicked(1, 0, ClickType.PICKUP, f.player); menu.clicked(payerSlot, 0, ClickType.PICKUP, f.player);
        f.check(ItemStack.matches(f.player.getInventory().getItem(7), payerBefore) && ItemStack.matches(menu.getCarried(), phialBefore),
                "real native PICKUP swaps the ACTUAL payer into7 and ACTUAL remainder onto cursor without deleting/duplicating either");
        menu.clicked(phialSlot, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().isEmpty() && table.isEmpty() && ItemStack.matches(f.player.getInventory().getItem(7), payerBefore)
                && ItemStack.matches(f.player.getInventory().getItem(10), phialBefore)
                && f.player.getInventory().countItem(ThaumcraftContent.FILLED_PHIAL.get()) == 1
                && f.player.getInventory().countItem(ThaumcraftContent.WAND.get()) == 1
                && f.player.getInventory().getItem(9).getCount() == 1 && ItemJarLabel.aspect(f.player.getInventory().getItem(9)) == Aspect.FIRE,
                "all three real resources remain exactly once: zero-vis payer7, unchanged catalyst10, earned bound label9; menu/cursor empty");
        f.player.closeContainer();
    }
    private static int playerSlot(ContainerArcaneWorkbench menu, Fixture f, int inventory) {
        return menu.slots.stream().filter(slot -> slot.container == f.player.getInventory() && slot.getContainerSlot() == inventory).findFirst().orElseThrow().index;
    }

    private static void plainConstruct(Fixture f) {
        // The vanilla GameTest grid packs the scenarios a few blocks apart, so an actor of a
        // neighbouring scenario can stand in the spot this fixture wants to use; the native
        // placement then refuses the block because that actor occupies it. The fixture therefore
        // takes the first spot of its own area that no actor occupies, and it never touches an
        // actor of another scenario.
        BlockPos pos = null;
        for (BlockPos candidate : List.of(f.base.east(), f.base.east(2), f.base.east(3), f.base.south(), f.base.south(2), f.base.south(3))) {
            f.put(candidate.below(), Blocks.STONE);
            if (f.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(candidate)).isEmpty()) { pos = candidate; break; }
        }
        if (pos == null) pos = f.base.east();
        hand(f, removeInventory(f, ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get(), 1)); f.aim(Vec3.atCenterOf(pos.below()));
        var useResult = f.use(pos.below(), Direction.UP, InteractionHand.MAIN_HAND);
        f.check(useResult.consumesAction() && f.player.getMainHandItem().isEmpty(), "ordinary native BlockItem placement pays ONE earned construct, without a crucible/assembly prerequisite");
        var state = f.level.getBlockState(pos); var full = new AABB(0, 0, 0, 1, 1, 1);
        f.check(state.is(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get()) && !state.hasBlockEntity() && !(state.getBlock() instanceof EntityBlock)
                && f.level.getBlockEntity(pos) == null && f.level.getChunkAt(pos).getBlockEntity(pos) == null
                && state.getShape(f.level, pos, CollisionContext.empty()).bounds().equals(full) && state.getCollisionShape(f.level, pos, CollisionContext.of(f.player)).bounds().equals(full)
                && state.getDestroySpeed(f.level, pos) == 3 && state.getBlock().getExplosionResistance() == 10.2F && !state.requiresCorrectToolForDrops()
                && !state.canOcclude() && state.getSoundType() == SoundType.METAL && state.getAnalogOutputSignal(f.level, pos) == 0,
                "genuine plain TE-less construct: full selection/collision cube, original3/effective10.2/metal/nonopaque/no invented correct-tier gate/comparator0, including native chunk BE lookup");
        for (boolean secondary : new boolean[] {false, true}) {
            f.player.setShiftKeyDown(secondary); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); f.aim(Vec3.atCenterOf(pos)); var menu = f.player.containerMenu;
            f.use(pos); f.check(f.player.containerMenu == menu && f.level.getBlockEntity(pos) == null && f.level.getBlockState(pos) == state, "construct use does NOT invent a GUI/BE/facing, primary or secondary");
        }
        f.player.setShiftKeyDown(false); ItemStack drop = breakPlain(f, pos, ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get());
        hand(f, drop); f.aim(Vec3.atCenterOf(pos.below())); f.check(f.use(pos.below(), Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && !f.level.getBlockState(pos).hasBlockEntity(), "own plain dropped construct places again without a fake TE/ticker");
        f.player.getInventory().add(breakPlain(f, pos, ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get()));
        f.check(f.player.getInventory().countItem(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get()) == 3, "all three PAID constructs remain available: two untouched plus the one actually placed/broken/replaced");
    }

    private static void plainCentrifuge(Fixture f) {
        BlockPos pos = f.base.east(2); f.put(pos.below(), Blocks.STONE); hand(f, removeInventory(f, ThaumcraftTubes.CENTRIFUGE_ITEM.get(), 1)); f.aim(Vec3.atCenterOf(pos.below()));
        f.check(f.use(pos.below(), Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && f.player.getMainHandItem().isEmpty() && f.level.getBlockEntity(pos) instanceof TileCentrifuge,
                "ONE genuinely earned centrifuge places through the native BlockItem");
        var tile = (TileCentrifuge) f.level.getBlockEntity(pos); var state = tile.getBlockState();
        f.check(tile.getFacing() == Direction.NORTH && tile.getInputAspect() == null && tile.getOutputAspect() == null && tile.getTickCount() == 0 && tile.getProcess() == 0
                && state.getShape(f.level, pos, CollisionContext.empty()).bounds().equals(new AABB(.25, 0, .25, .75, 1, .75))
                && state.getCollisionShape(f.level, pos, CollisionContext.of(f.player)).bounds().equals(new AABB(0, 0, 0, 1, 1, 1)),
                "centrifuge defaults NORTH (NOT clicked UP), compact original selection but FULL entity collision, fresh separate empty slots");
        for (boolean secondary : new boolean[] {false, true}) for (boolean tool : new boolean[] {false, true}) {
            f.player.setShiftKeyDown(secondary); ItemStack held = tool ? wand("iron", 731) : ItemStack.EMPTY; f.player.setItemInHand(InteractionHand.MAIN_HAND, held); var beforeHand = held.copy(); f.aim(Vec3.atCenterOf(pos)); var menu = f.player.containerMenu; var before = tile.saveWithFullMetadata(f.level.registryAccess());
            f.use(pos); f.check(f.player.containerMenu == menu && before.equals(tile.saveWithFullMetadata(f.level.registryAccess())) && ItemStack.matches(beforeHand, f.player.getMainHandItem()), "actual bare-hand/wand primary/secondary use invents no centrifuge IWandable-facing/choke/menu/payment action");
        }
        f.player.setShiftKeyDown(false); tile.addEssentia(Aspect.MAGIC, 1, null); tile.addToContainer(Aspect.FIRE, 1);
        ItemStack own = breakPlain(f, pos, ThaumcraftTubes.CENTRIFUGE_ITEM.get()); hand(f, own); f.aim(Vec3.atCenterOf(pos.below()));
        f.check(f.use(pos.below(), Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && f.level.getBlockEntity(pos) instanceof TileCentrifuge fresh
                && fresh.getFacing() == Direction.NORTH && units(fresh) == 0 && fresh.getTickCount() == 0 && fresh.getProcess() == 0,
                "native break drops ONE plain OWN machine, not its two occupied compartments, and own-item placement restores fresh NORTH/empty defaults");
    }

    private static void rawFacades(Fixture f) {
        f.reset(); TileCentrifuge tile = centrifuge(f, f.base);
        f.check(!TileTube.class.isAssignableFrom(tile.getClass()) && !EssentiaStorageEndpoint.class.isAssignableFrom(tile.getClass()) && tile.getFacing() == Direction.NORTH
                && tile.getRotation() == 0 && tile.getRotationSpeed() == 0 && tile.getTickCount() == 0 && tile.getProcess() == 0, "independent native two-slot BE is neither sealed tube nor anonymous singleton; transient server/client fields start honestly at0");
        for (Direction face : Direction.values()) f.check(tile.isConnectable(face) == (face.getAxis() == Direction.Axis.Y) && tile.canInputFrom(face) == (face == Direction.DOWN)
                && tile.canOutputTo(face) == (face == Direction.UP) && tile.getSuctionType(face) == null && tile.getSuctionAmount(face) == (face == Direction.DOWN ? 128 : 0), "canonical vertical raw ports, untyped128 DOWN only: " + face);
        f.check(!tile.isConnectable(null) && !tile.canInputFrom(null) && !tile.canOutputTo(null) && tile.getSuctionAmount(null) == 0 && tile.getMinimumSuction() == 0 && !tile.renderExtendedTube(), "safe UNKNOWN advertised port; zero minimum, no extended pipe renderer");
        tile.setSuction(Aspect.FIRE, 999); f.check(tile.getSuctionAmount(Direction.DOWN) == 128 && tile.getSuctionType(Direction.DOWN) == null, "setSuction is an original no-op");
        var empty = tile.saveWithFullMetadata(f.level.registryAccess());
        for (Aspect primal : Aspect.getPrimalAspects()) f.check(tile.addEssentia(primal, 1, Direction.DOWN) == 0 && empty.equals(tile.saveWithFullMetadata(f.level.registryAccess())), "each of SIX primal INPUT types refuses without a free paid unit: " + primal.getTag());
        for (int invalid : new int[] {-1, 0}) f.check(tile.addEssentia(Aspect.MAGIC, invalid, null) == 0 && tile.addToContainer(Aspect.FIRE, invalid) == invalid, "explicit authority hardening refuses original nonpositive free INPUT, not the valid face-blind positive API");
        f.check(tile.addEssentia(null, 1, Direction.DOWN) == 0 && tile.addToContainer(null, 1) == 1 && tile.containerContains(null) == 0
                && !tile.doesContainerContainAmount(null, 1) && !tile.doesContainerContain(null) && empty.equals(tile.saveWithFullMetadata(f.level.registryAccess())), "null-ASPECT safety has no hidden stock or empty-OUT false positive; null container list is safe");
        Direction[] faces = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
        for (Direction face : faces) {
            f.reset(); tile = centrifuge(f, f.base);
            f.check(tile.addEssentia(Aspect.MAGIC, 99, face) == 1 && tile.getInputAspect() == Aspect.MAGIC && tile.getProcess() == 39
                    && tile.getOutputAspect() == null && tile.getEssentiaAmount(null) == 0 && tile.getAspects().size() == 0 && tile.getSuctionAmount(Direction.DOWN) == 64,
                    "valid positive transport INPUT is face-blind INCLUDING null FACE, pays only one and installs39 without exposing input as output: " + face);
            f.check(tile.addToContainer(Aspect.FIRE, 99) == 98 && units(tile) == 2 && tile.addEssentia(Aspect.ENERGY, 1, face) == 0
                    && tile.doesContainerAccept(Aspect.WATER) && tile.addToContainer(Aspect.WATER, 1) == 1, "IN compound + OUT primal legitimately coexist; original acceptance predicate is not a new empty/capacity/type gate, but actual full OUT insertion refuses");
            for (Direction view : faces) f.check(tile.getEssentiaType(view) == Aspect.FIRE && tile.getEssentiaAmount(view) == 1, "EVERY public sided/UNKNOWN stock query describes OUT only, never pending IN");
            AspectList copy = tile.getAspects(); copy.aspects.clear(); copy.add(Aspect.WATER, 99); tile.setAspects(copy);
            f.check(tile.getAspects().size() == 1 && tile.getAspects().getAmount(Aspect.FIRE) == 1 && tile.containerContains(Aspect.MAGIC) == 0 && tile.containerContains(Aspect.FIRE) == 1
                    && tile.doesContainerContainAmount(Aspect.FIRE, 1) && !tile.doesContainerContainAmount(Aspect.FIRE, 2)
                    && tile.doesContainerContain(new AspectList().add(Aspect.FIRE, 99)), "defensive OUT-only list, setAspects stub, exact-quantity amount query and original deprecated intersection/type query");
            for (int invalid : new int[] {-1, 0, 2}) f.check(!tile.takeFromContainer(Aspect.FIRE, invalid), "container output take requires exactly1; original arbitrary-amount deletion is explicitly hardened");
            for (Direction output : faces) if (output != Direction.UP) f.check(tile.takeEssentia(Aspect.FIRE, 99, output) == 0 && units(tile) == 2, "transport OUT can only leave through UP, regardless of advertised total on every face");
            f.check(tile.takeEssentia(Aspect.FIRE, -1, Direction.UP) == 0 && tile.takeEssentia(Aspect.FIRE, 0, Direction.UP) == 0
                    && tile.takeEssentia(Aspect.FIRE, 99, Direction.UP) == 1 && tile.getInputAspect() == Aspect.MAGIC && tile.getProcess() == 39 && tile.getOutputAspect() == null,
                    "positive oversized output clips/reports ACTUAL1, never99; nonpositive output refuses; input/process remain independent");
        }
    }

    private static void nativeTiming(Fixture f) {
        f.reset(); TileCentrifuge tile = centrifuge(f, f.base); f.put(f.base.below(), ThaumcraftAlembics.ALEMBIC.get()); var source = (TileAlembic) f.level.getBlockEntity(f.base.below()); source.addToContainer(Aspect.ENERGY, 3);
        for (int call = 1; call <= 4; call++) { tick(f, tile); f.check(tile.getTickCount() == call && units(tile) == 0 && source.getAmount() == 3, "native idle increments1..4 do NOT draw"); }
        tick(f, tile); f.check(tile.getTickCount() == 5 && tile.getInputAspect() == Aspect.ENERGY && tile.getProcess() == 38 && source.getAmount() == 2, "native fifth idle tick draws exactly1, installs39, THEN decrements to38 in the SAME tick");
        for (int left = 37; left >= 1; left--) { tick(f, tile); f.check(tile.getProcess() == left && tile.getTickCount() == 5 && tile.getInputAspect() == Aspect.ENERGY && tile.getOutputAspect() == null && units(tile) + source.getAmount() == 3, "next37 unpowered ticks freeze idle count and preserve exactly one paid processing input"); }
        tick(f, tile); Aspect first = tile.getOutputAspect(); f.check(tile.getInputAspect() == null && tile.getProcess() == 0 && component(Aspect.ENERGY, first) && units(tile) + source.getAmount() == 3, "next tick creates exactly ONE native random IMMEDIATE component, not both, with no seeded sequence/distribution expectation");
        ticks(f, tile, 10); f.check(tile.getTickCount() == 5 && tile.getOutputAspect() == first && source.getAmount() == 2, "occupied OUT freezes idle counting/drawing, not a new global-time schedule");
        f.check(tile.takeFromContainer(first, 1), "one actual OUT unit is explicitly removed/accounted");
        ticks(f, tile, 4); f.check(tile.getTickCount() == 9 && units(tile) == 0 && source.getAmount() == 2, "after extraction, only genuinely empty idle ticks6..9 advance");
        tick(f, tile); f.check(tile.getTickCount() == 10 && tile.getProcess() == 38 && tile.getInputAspect() == Aspect.ENERGY && source.getAmount() == 1 && units(tile) + source.getAmount() + 1 == 3, "next fifth EMPTY idle increment draws one more; removed output is explicitly counted, never hidden/refunded");
    }

    private static void poweredAndBlocked(Fixture f) {
        f.reset(); TileCentrifuge tile = centrifuge(f, f.base); f.put(f.base.below(), ThaumcraftAlembics.ALEMBIC.get()); var source = (TileAlembic) f.level.getBlockEntity(f.base.below()); source.addToContainer(Aspect.ENERGY, 1); power(f, tile, true); ticks(f, tile, 7);
        f.check(tile.isPowered() && tile.getTickCount() == 0 && units(tile) == 0 && source.getAmount() == 1 && tile.getSuctionAmount(Direction.DOWN) == 0, "actual adjacent native redstone pauses EMPTY idle/draw despite genuinely available below-source stock, and reports DOWNpressure0");
        f.check(tile.addToContainer(Aspect.AIR, 2) == 1 && tile.addEssentia(Aspect.MAGIC, 99, null) == 1 && tile.getProcess() == 39 && units(tile) == 2, "powered positive null-FACE INPUT remains valid EVEN with OUT already occupied; raw container OUT insertion is also valid while powered");
        ticks(f, tile, 50); f.check(tile.getProcess() == 39 && tile.getTickCount() == 0 && units(tile) == 2 && tile.takeEssentia(Aspect.AIR, 99, Direction.UP) == 1, "power freezes process/idle but does NOT forbid actual UP output extraction");
        tile.addToContainer(Aspect.AIR, 1); power(f, tile, false);
        for (int left = 38; left >= 0; left--) { tick(f, tile); f.check(tile.getProcess() == left && tile.getTickCount() == 0 && tile.getInputAspect() == Aspect.MAGIC && tile.getOutputAspect() == Aspect.AIR && units(tile) == 2 && source.getAmount() == 1, "OUT blockage does NOT stop the process countdown; both legitimate slots survive to0 without drawing another source unit"); }
        ticks(f, tile, 9); f.check(tile.getProcess() == 0 && tile.getTickCount() == 0 && units(tile) == 2, "ready input waits behind occupied output, without overwrite, deletion or repeat RNG conversion");
        f.check(tile.takeEssentia(Aspect.AIR, 1, Direction.UP) == 1 && tile.getInputAspect() == Aspect.MAGIC && tile.getProcess() == 0, "draining OUT preserves its separately ready compound input");
        tick(f, tile); Aspect result = tile.getOutputAspect();
        f.check(tile.getInputAspect() == null && tile.getTickCount() == 0 && units(tile) == 1 && component(Aspect.MAGIC, result) && !result.isPrimal(), "NEXT unpowered tick converts MAGIC into ONE immediate COMPOUND component, not recursively to primals; completion itself does not advance idle count");
        ticks(f, tile, 3); f.check(tile.getOutputAspect() == result && units(tile) == 1, "completed output is not rerolled or duplicated on later native ticks");
    }

    private static void persistenceAndQuarantine(Fixture f) {
        f.reset(); TileCentrifuge tile = centrifuge(f, f.base); ticks(f, tile, 3); tile.addEssentia(Aspect.MAGIC, 1, Direction.WEST); ticks(f, tile, 7); tile.addToContainer(Aspect.FIRE, 1);
        var saved = tile.saveWithFullMetadata(f.level.registryAccess()); var update = tile.getUpdateTag(f.level.registryAccess());
        f.check(saved.getString("aspectIn").equals("praecantatio") && saved.getString("aspectOut").equals("ignis") && update.getString("aspectIn").equals("praecantatio") && update.getString("aspectOut").equals("ignis")
                && !saved.contains("process") && !saved.contains("count") && !saved.contains("rotation") && !saved.contains("rotationSpeed")
                && !update.contains("process") && !update.contains("count"), "disk/custom update preserve BOTH independent slots/facing, NEVER process/count/rotation/speed");
        saved.putInt("facing", Direction.EAST.get3DDataValue()); TileCentrifuge old = tile; tile = reload(f, saved);
        f.check(tile.getFacing() == Direction.EAST && tile.canInputFrom(Direction.DOWN) && tile.canOutputTo(Direction.UP) && !tile.canInputFrom(Direction.WEST)
                && tile.getInputAspect() == Aspect.MAGIC && tile.getOutputAspect() == Aspect.FIRE && tile.getProcess() == 0 && tile.getTickCount() == 0
                && tile.getRotation() == 0 && tile.getRotationSpeed() == 0 && old.addEssentia(Aspect.ENERGY, 1, null) == 0 && old.takeEssentia(Aspect.FIRE, 1, Direction.UP) == 0,
                "actual registered loadStatic preserves valid IN+OUT/facing, resets only transient state and rejects stale identity; facing is persisted metadata, NOT rotated ports");
        var mirror = ThaumcraftTubes.CENTRIFUGE_BLOCK_ENTITY.get().create(f.base, tile.getBlockState()); f.check(mirror != null, "separate registered native update-target factory exists"); mirror.loadWithComponents(update, f.level.registryAccess());
        f.check(mirror.getInputAspect() == Aspect.MAGIC && mirror.getOutputAspect() == Aspect.FIRE && mirror.getProcess() == 0 && tile.getUpdatePacket() != null, "native custom update carries both real compartments without smuggling in process39; the actual attached server BE supplies its real update packet");
        ticks(f, tile, 8); f.check(tile.getTickCount() == 0 && tile.getProcess() == 0 && units(tile) == 2, "reloaded pending input waits at0 behind existing OUT, never restarts39 or advances idle");
        power(f, tile, true); f.check(tile.takeEssentia(Aspect.FIRE, 1, Direction.UP) == 1, "powered reload still permits raw output extraction"); ticks(f, tile, 4);
        f.check(tile.getInputAspect() == Aspect.MAGIC && tile.getOutputAspect() == null && tile.getProcess() == 0, "powered ready reload remains paused without erasing paid pending input");
        power(f, tile, false); tick(f, tile); f.check(tile.getInputAspect() == null && component(Aspect.MAGIC, tile.getOutputAspect()) && tile.getTickCount() == 0, "first unpowered tick AFTER raw drain completes reloaded process0 immediately");
        f.reset(); tile = centrifuge(f, f.base); tile.addEssentia(Aspect.MAGIC, 1, null); saved = tile.saveWithFullMetadata(f.level.registryAccess()); tile = reload(f, saved);
        f.check(tile.getProcess() == 0 && tile.getInputAspect() == Aspect.MAGIC && tile.getOutputAspect() == null
                && EssentiaTransfers.completeCentrifuge(tile).committedUnits() == 0 && tile.prepareCompletion(tile.snapshotAtomicState(), Aspect.VOID) == null
                && tile.getInputAspect() == Aspect.MAGIC && tile.getOutputAspect() == null, "even genuinely READY reloaded IN/process0 cannot be completed or assigned a chosen component by an external caller outside the private native tick phase");
        tick(f, tile); f.check(tile.getTickCount() == 0 && tile.getInputAspect() == null && component(Aspect.MAGIC, tile.getOutputAspect()), "genuine pending-IN-only native reload likewise completes on NEXT unpowered tick, no externally forced timer/completion");

        f.reset(); tile = centrifuge(f, f.base); tile.addEssentia(Aspect.MAGIC, 1, null); tile.addToContainer(Aspect.FIRE, 1); var valid = tile.saveWithFullMetadata(f.level.registryAccess());
        var badSaves = new LinkedHashMap<String, CompoundTag>();
        for (String badInput : List.of("ignis", "centrifuge-unknown-input", "")) { var bad = valid.copy(); bad.putString("aspectIn", badInput); badSaves.put("invalid/primal IN " + badInput, bad); }
        var bad = valid.copy(); bad.putString("aspectOut", "centrifuge-unknown-output"); badSaves.put("unknown OUT alongside valid paid IN", bad);
        for (String key : List.of("aspectIn", "aspectOut", "facing")) { bad = valid.copy(); bad.put(key, new net.minecraft.nbt.ListTag()); badSaves.put("wrong NBT type " + key, bad); }
        for (int facing : new int[] {-1, 6, 999}) { bad = valid.copy(); bad.putInt("facing", facing); badSaves.put("invalid facing " + facing, bad); }
        for (var entry : badSaves.entrySet()) {
            tile = reload(f, entry.getValue()); var quarantined = tile;
            f.check(tile.isQuarantined() && units(tile) == 0 && tile.getSuctionAmount(Direction.DOWN) == 0 && !tile.isConnectable(Direction.UP)
                    && tile.addEssentia(Aspect.MAGIC, 1, null) == 0 && tile.addToContainer(Aspect.FIRE, 1) == 1 && !tile.takeFromContainer(Aspect.FIRE, 1), "whole malformed payload is disabled/quarantined, not a truncated spendable subset: " + entry.getKey());
            ticks(f, tile, 43); var rejected = tile.saveWithFullMetadata(f.level.registryAccess());
            f.check(tile.getTickCount() == 0 && tile.getProcess() == 0 && rejected.getCompound("RejectedCentrifuge").equals(entry.getValue())
                    && tile.getUpdateTag(f.level.registryAccess()).getCompound("RejectedCentrifuge").equals(entry.getValue()), "retain ENTIRE malformed IN/OUT/policy payload verbatim through disk/update, without ticking into free output: " + entry.getKey());
            tile = reload(f, rejected); f.check(tile != quarantined && tile.isQuarantined() && tile.getUpdateTag(f.level.registryAccess()).getCompound("RejectedCentrifuge").equals(entry.getValue()), "second actual reload retains the SAME original rejection, not nested quarantine or deleted valid units");
        }
    }

    static TileCentrifuge centrifuge(Fixture f, BlockPos pos) { f.put(pos, ThaumcraftTubes.CENTRIFUGE.get()); return (TileCentrifuge) f.level.getBlockEntity(pos); }
    static void tick(Fixture f, TileCentrifuge tile) { var pos = tile.getBlockPos(); var state = f.level.getBlockState(pos); var ticker = state.getTicker(f.level, ThaumcraftTubes.CENTRIFUGE_BLOCK_ENTITY.get()); if (ticker == null) throw new IllegalStateException("Missing real centrifuge ticker"); ticker.tick(f.level, pos, state, tile); }
    static void ticks(Fixture f, TileCentrifuge tile, int calls) { for (int i = 0; i < calls; i++) tick(f, tile); }
    static int units(TileCentrifuge tile) { return (tile.getInputAspect() == null ? 0 : 1) + (tile.getOutputAspect() == null ? 0 : 1); }
    static boolean component(Aspect input, Aspect output) { return output != null && Arrays.asList(input.getComponents()).contains(output); }
    static void power(Fixture f, TileCentrifuge tile, boolean on) { f.level.setBlock(tile.getBlockPos().east(), (on ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); f.check(tile.isPowered() == on, "actual adjacent native redstone state, never a fake power field"); }
    static TileCentrifuge reload(Fixture f, CompoundTag saved) { f.level.removeBlockEntity(f.base); var loaded = BlockEntity.loadStatic(f.base, f.level.getBlockState(f.base), saved, f.level.registryAccess()); f.check(loaded instanceof TileCentrifuge, "actual separate BEType/loadStatic constructs centrifuge, not tube/buffer"); f.level.setBlockEntity(loaded); return (TileCentrifuge) loaded; }
    private static void constructMatrix(TileArcaneWorkbench table, int count) {
        for (int slot = 0; slot < 9; slot++) table.setItem(slot, ItemStack.EMPTY);
        for (int slot : new int[] {0, 8}) table.setItem(slot, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(), count));
        for (int slot : new int[] {1, 3, 5, 7}) table.setItem(slot, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), count));
        for (int slot : new int[] {2, 6}) table.setItem(slot, new ItemStack(ThaumcraftContent.MATERIALS.get("vis_filter").get(), count));
        table.setItem(4, new ItemStack(ThaumcraftNaturals.GREATWOOD_PLANKS.get(), count));
    }
    private static void centrifugeMatrix(TileArcaneWorkbench table, ItemStack earned, int count) {
        for (int slot = 0; slot < 9; slot++) table.setItem(slot, ItemStack.EMPTY);
        for (int slot : new int[] {1, 7}) table.setItem(slot, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), count));
        table.setItem(3, new ItemStack(ThaumcraftAlembics.ALEMBIC_ITEM.get(), count)); table.setItem(4, earned); table.setItem(5, new ItemStack(Items.PISTON, count));
    }
    private static ItemStack wand(String cap, int amount) { var stack = new ItemStack(ThaumcraftContent.WAND.get()); var vis = new LinkedHashMap<String, Integer>(); for (String primal : WandData.PRIMALS) vis.put(primal, amount); stack.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", cap, vis)); return stack; }
    private static ItemStack shortWand(String cap, String primal, int amount) { ItemStack stack = wand(cap, 5000); stack.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(stack).withAmount(primal, amount)); return stack; }
    private static void checkVis(Fixture f, ItemStack wand, int aqua, int ordo, int perditio, String why) { for (String primal : WandData.PRIMALS) f.check(ItemWandCasting.getVis(wand, primal) == (primal.equals("aqua") ? aqua : primal.equals("ordo") ? ordo : primal.equals("perditio") ? perditio : 5000), why + ": " + primal); }
    private static void refuse(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu, String why) { menu.broadcastChanges(); var before = table.saveWithoutMetadata(f.level.registryAccess()); menu.clicked(0, 0, ClickType.PICKUP, f.player); f.check(menu.getCarried().isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), why); }
    private static void takeToInventory(Fixture f, ContainerArcaneWorkbench menu, Item own) { menu.clicked(0, 0, ClickType.PICKUP, f.player); ItemStack earned = menu.getCarried(); f.check(earned.is(own) && earned.getCount() == 1, "native extraction earns ONE real own output"); menu.setCarried(ItemStack.EMPTY); f.check(f.player.getInventory().add(earned), "owned native inventory receives the actually earned output"); }
    private static ItemStack removeInventory(Fixture f, Item own, int count) { for (int slot = 0; slot < 36; slot++) if (f.player.getInventory().getItem(slot).is(own) && f.player.getInventory().getItem(slot).getCount() >= count) return f.player.getInventory().removeItem(slot, count); throw new AssertionError("Missing genuinely earned inventory output " + own + " x" + count); }
    private static void hand(Fixture f, ItemStack earned) { for (int slot = 0; slot < 9; slot++) if (f.player.getInventory().getItem(slot).isEmpty()) { f.player.getInventory().selected = slot; f.player.setItemInHand(InteractionHand.MAIN_HAND, earned); return; } throw new AssertionError("No actual empty hand slot; never overwrite another paid output"); }
    private static ItemStack breakPlain(Fixture f, BlockPos pos, Item own) { f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); f.check(f.level.destroyBlock(pos, true, f.player), "actual native block break"); var drops = f.drops().stream().filter(drop -> drop.getItem().is(own)).toList(); f.check(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), new ItemStack(own)) && !drops.getFirst().getItem().has(DataComponents.BLOCK_ENTITY_DATA) && f.pendingRows().isEmpty(), "ONE plain OWN native loot item, no stored IN/OUT/facing/component payload or invented label/refund"); ItemStack out = drops.getFirst().getItem(); drops.getFirst().discard(); return out; }
}
