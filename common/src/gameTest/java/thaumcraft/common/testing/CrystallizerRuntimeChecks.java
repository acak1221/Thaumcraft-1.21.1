package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.blocks.BlockEssentiaCrystalizer;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.CrystalEssenceData;
import thaumcraft.common.items.ItemCrystalEssence;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileEssentiaCrystalizer;
import thaumcraft.common.tiles.TileTube;

/** M06e declared SERVER INPUTS: raw materials/research/wands/gear, empty native
 * devices and raw essentia. Recipes/dependency outputs and timed crystals are
 * earned. Not full survival acquisition or VisNet/acceleration/client evidence.
 * Only real production ticker calls advance phase; no seeds/clock/counter writes. */
public final class CrystallizerRuntimeChecks {
    private CrystallizerRuntimeChecks() {}

    public static void recipesAndOrderedCrystalItem(GameTestHelper test) {
        try (var f = new Fixture(test)) { f.open(); recipes(f); flattenedMachineItemsDoNotHandToggleValves(f); orderedItem(f); }
        test.succeed();
    }

    public static void singleSlotTimingPowerAndPersistence(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open(); rawFacades(f); baseline(f); powerAndProgressPhase(f); persistence(f);
        }
        test.succeed();
    }

    private static void recipes(Fixture f) {
        f.reset(); f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get()); f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench, "real connected SURVIVAL actor opens the native arcane menu");
        var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base); var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
        var holder = f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("arcane/essentia_crystallizer")).orElseThrow();
        f.check(holder.value() instanceof ArcaneShapedRecipe, "actual native recipe holder, not an injected output fixture");
        var recipe = (ArcaneShapedRecipe) holder.value(); var research = ResearchCatalog.get("ESSENTIACRYSTAL");
        f.check(recipe.research().equals("ESSENTIACRYSTAL") && recipe.pattern().equals(List.of("IDI", "QCQ", "WTW"))
                && recipe.vis().equals(Map.of("aqua", 5, "terra", 15, "ordo", 5)) && recipe.result().is(ThaumcraftTubes.ESSENTIA_CRYSTALLIZER_ITEM.get()) && recipe.result().getCount() == 1,
                "original ConfigRecipes293: NINE occupied cells, iron2/DISPENSER1/balanced2/planks2/basic1/construct1, separate actual output1 and vis5/15/5");
        f.check(research != null && research.flags().equals(java.util.Set.of("Concealed")) && !research.isSecondary() && research.parents().equals(List.of("TUBES"))
                && research.complexity() == 1 && research.column() == 8 && research.row() == -2
                && research.aspectCost().equals(Map.of("aqua", 5, "vitreus", 5, "permutatio", 3, "praecantatio", 5))
                && research.pages().stream().anyMatch(page -> "EssentiaCrystalizer".equals(page.recipeKey())),
                "original concealed PRIMARY note gate/cost/book key is not Secondary, a gameplay Stub, the primal-vis bill or CENTRIFUGE");
        // Earn all three plain dependencies through the real existing recipe;
        // finished constructs/crystallizers are never supplied in this case.
        constructMatrix(table, 3); table.setItem(10, wand("iron", 5000)); KnowledgeAccess.get(f.player).addResearch("DISTILESSENTIA");
        menu.broadcastChanges(); menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        f.check(f.player.getInventory().countItem(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get()) == 3 && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty), "three earned construct dependencies consume all FOUR distinct basic-tube positions per recipe");
        ItemStack earned = removeInventory(f, ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get(), 3);
        matrix(table, earned, 3); table.setItem(10, wand("iron", 5000));
        KnowledgeAccess.get(f.player).addResearch("TUBES"); KnowledgeAccess.get(f.player).addResearch("CENTRIFUGE"); KnowledgeAccess.get(f.player).removeResearch("ESSENTIACRYSTAL");
        refuse(f, table, menu, "TUBES/CENTRIFUGE do NOT substitute for ESSENTIACRYSTAL"); KnowledgeAccess.get(f.player).addResearch("ESSENTIACRYSTAL");
        var correct = ArcaneRecipeInput.snapshot(table).items().stream().map(ItemStack::copy).toList();
        for (int slot = 0; slot < 9; slot++) { table.setItem(slot, ItemStack.EMPTY); refuse(f, table, menu, "each original occupied cell is required: " + slot); table.setItem(slot, correct.get(slot).copy()); }
        for (Item wrong : List.of(Items.QUARTZ_BLOCK, Items.NETHER_QUARTZ_ORE, Items.QUARTZ, ThaumcraftContent.CRYSTAL_ESSENCE.get(), ThaumcraftEssences.WISP_ESSENCE.get())) {
            table.setItem(3, new ItemStack(wrong, 3)); refuse(f, table, menu, "native quartz BLOCK/ORE/ITEM and essence identities are NOT balanced-shard aliases: " + wrong);
        }
        for (Aspect primal : Aspect.getPrimalAspects()) { table.setItem(3, new ItemStack(ThaumcraftContent.SHARDS.get(primal.getTag()).get(), 3)); refuse(f, table, menu, "none of SIX primal shards is balanced: " + primal.getTag()); }
        table.setItem(3, correct.get(3).copy());
        for (Item wrong : List.of(Items.DIAMOND, Items.DIAMOND_BLOCK, Items.DROPPER)) { table.setItem(1, new ItemStack(wrong, 3)); refuse(f, table, menu, "D means DISPENSER, not diamond/dropper"); }
        table.setItem(1, correct.get(1).copy());
        for (Item wrong : List.of(Items.OAK_LOG, Items.OAK_SLAB)) { table.setItem(6, new ItemStack(wrong, 3)); refuse(f, table, menu, "plank tag is not a log/slab"); }
        table.setItem(6, correct.get(6).copy());
        table.setItem(0, new ItemStack(Items.IRON_BLOCK, 3)); refuse(f, table, menu, "ore iron ingot adapter does not accept iron block"); table.setItem(0, correct.get(0).copy());
        table.setItem(7, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(), 3)); refuse(f, table, menu, "T must be a BASIC tube"); table.setItem(7, correct.get(7).copy());
        table.setItem(4, new ItemStack(ThaumcraftAlembics.ALEMBIC_ITEM.get(), 3)); refuse(f, table, menu, "C must be the plain construct, not another machine"); table.setItem(4, correct.get(4).copy());
        for (Item planks : List.of(Items.OAK_PLANKS, Items.BIRCH_PLANKS, ThaumcraftNaturals.GREATWOOD_PLANKS.get().asItem(), ThaumcraftNaturals.SILVERWOOD_PLANKS.get().asItem())) {
            table.setItem(6, new ItemStack(planks, 3)); menu.broadcastChanges(); f.check(menu.craftable(), "legitimate ore plank may differ across left/right W; no construct greatwood-only restriction: " + planks);
        }
        table.setItem(6, correct.get(6).copy()); ItemStack payer = table.removeItemNoUpdate(10); refuse(f, table, menu, "no inserted paying wand");
        for (String primal : List.of("aqua", "terra", "ordo")) { table.setItem(10, shortWand("iron", primal, primal.equals("terra") ? 1649 : 549)); refuse(f, table, menu, "one centivis short, including larger TERRA, refuses WHOLE payment: " + primal); }
        table.setItem(10, payer); menu.broadcastChanges();
        f.check(menu.craftable() && menu.requiredVis("aqua") == 550 && menu.requiredVis("terra") == 1650 && menu.requiredVis("ordo") == 550
                && f.level.getRecipeManager().getRecipeFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(), ArcaneRecipeInput.snapshot(table), f.level).orElseThrow().id().equals(holder.id()), "actual recipe manager/menu prices iron caps550/1650/550");
        var ownedInventory = new ArrayList<ItemStack>(); for (int slot = 0; slot < 36; slot++) { ownedInventory.add(f.player.getInventory().getItem(slot).copy()); f.player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64)); }
        var fullDestination = table.saveWithoutMetadata(f.level.registryAccess()); f.check(menu.quickMoveStack(f.player, 0).isEmpty() && fullDestination.equals(table.saveWithoutMetadata(f.level.registryAccess())), "real full native quick-move destination refuses ALL ingredients/vis rather than spending and losing a machine");
        for (int slot = 0; slot < 36; slot++) f.player.getInventory().setItem(slot, ownedInventory.get(slot));
        var before = table.saveWithoutMetadata(f.level.registryAccess()); menu.setCarried(new ItemStack(Items.APPLE)); menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().is(Items.APPLE) && menu.getCarried().getCount() == 1 && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), "incompatible native cursor refuses extraction WITHOUT consuming nine ingredients/vis"); menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, f.player); ItemStack paid = menu.getCarried();
        f.check(paid.is(ThaumcraftTubes.ESSENTIA_CRYSTALLIZER_ITEM.get()) && paid.getCount() == 1 && paid.getComponentsPatch().isEmpty(), "actual own plain machine output on native cursor, no fabricated contents/BE components");
        menu.setCarried(ItemStack.EMPTY); f.check(f.player.getInventory().add(paid), "actual paid output transferred into owned inventory");
        checkVis(f, table.getItem(10), 4450, 3350, 4450, "ONE actual iron-cap payment");
        for (int slot = 0; slot < 9; slot++) f.check(table.getItem(slot).getCount() == 2, "ONE consumed in every occupied cell, total iron2/balanced2: " + slot);
        KnowledgeAccess.get(f.player).removeResearch("ESSENTIACRYSTAL"); before = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.quickMoveStack(f.player, 0).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), "stale previously funded extraction revalidates research without replay/spend"); KnowledgeAccess.get(f.player).addResearch("ESSENTIACRYSTAL");
        f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get())); table.setItem(10, shortWand("thaumium", "terra", 1274)); menu.broadcastChanges();
        // Float multiplication rounds BEFORE the int cast:500*.84999996F
        // remains424.99997, but1500*.84999996F rounds toEXACT1275.0F.
        f.check(menu.requiredVis("aqua") == 424 && menu.requiredVis("terra") == 1275 && menu.requiredVis("ordo") == 424, "actual original float evaluation gives424/1275/424; not a generic minus-one on every whole-vis price");
        refuse(f, table, menu, "discounted large primal one-short stays atomic"); table.setItem(10, wand("thaumium", 5000)); menu.broadcastChanges(); menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        f.check(f.player.getInventory().countItem(ThaumcraftTubes.ESSENTIA_CRYSTALLIZER_ITEM.get()) == 3 && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty) && menu.quickMoveStack(f.player, 0).isEmpty(), "real quick-move pays last two with EARNED dependencies only; no empty-recipe replay");
        checkVis(f, table.getItem(10), 4152, 2450, 4152, "two real discounted payments"); f.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); f.player.closeContainer();
        placementAndPlainLoot(f, removeInventory(f, ThaumcraftTubes.ESSENTIA_CRYSTALLIZER_ITEM.get(), 1));
    }

    private static void placementAndPlainLoot(Fixture f, ItemStack earned) {
        f.check(ThaumcraftTubes.ESSENTIA_CRYSTALLIZER.get().defaultBlockState().getValue(BlockEssentiaCrystalizer.FACING) == Direction.DOWN, "native defaultDOWN, unlike CF NORTH");
        // Reuse ONE paid item through six true native place/break cycles.
        for (Direction clicked : Direction.values()) {
            BlockPos support = f.base.east(2), pos = support.relative(clicked); f.put(support, Blocks.STONE); f.level.setBlock(pos, Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            hand(f, earned); f.aim(Vec3.atCenterOf(support)); f.check(f.use(support, clicked, InteractionHand.MAIN_HAND).consumesAction() && f.player.getMainHandItem().isEmpty(), "native BlockItem spends ONE earned output on clicked face " + clicked);
            var state = f.level.getBlockState(pos); var tile = (TileEssentiaCrystalizer) f.level.getBlockEntity(pos);
            f.check(state.is(ThaumcraftTubes.ESSENTIA_CRYSTALLIZER.get()) && tile.getFacing() == clicked.getOpposite()
                    && state.getShape(f.level, pos, CollisionContext.empty()).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                    && state.getCollisionShape(f.level, pos, CollisionContext.of(f.player)).bounds().equals(new AABB(0, 0, 0, 1, 1, 1)), "original BlockTube metadata7 has FULLCUBE outline AND collision, opposite-facing native placement");
            tile.addToContainer(Aspect.MAGIC, 1); ticks(f, tile, 13); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (boolean secondary : new boolean[] {false, true}) { f.player.setShiftKeyDown(secondary); f.aim(Vec3.atCenterOf(pos)); var menu = f.player.containerMenu; var saved = tile.saveWithFullMetadata(f.level.registryAccess()); f.use(pos); f.check(menu == f.player.containerMenu && saved.equals(tile.saveWithFullMetadata(f.level.registryAccess())), "no invented menu/hand rotation or raw storage action"); }
            f.player.setShiftKeyDown(false); f.check(f.level.destroyBlock(pos, true, f.player), "actual native break of pending input");
            var drops = f.drops().stream().filter(drop -> drop.getItem().is(ThaumcraftTubes.ESSENTIA_CRYSTALLIZER_ITEM.get())).toList();
            f.check(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), new ItemStack(ThaumcraftTubes.ESSENTIA_CRYSTALLIZER_ITEM.get())) && f.pendingRows().isEmpty() && crystalDrops(f) == 0, "break returns ONE plain OWN machine, not old pending essentia/progress/facing or an unearned crystal");
            earned = drops.getFirst().getItem().copy(); drops.getFirst().discard(); f.level.setBlock(support, Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
        }
    }

    private static void flattenedMachineItemsDoNotHandToggleValves(Fixture f) {
        // Explicit pre-existing SERVER INPUT items for dispatch, not another
        // acquisition claim. Both once shared the original old tube ItemBlock.
        for (Item item : List.of(ThaumcraftTubes.CENTRIFUGE_ITEM.get(), ThaumcraftTubes.ESSENTIA_CRYSTALLIZER_ITEM.get())) {
            f.reset(); TileTube valve = f.tube(f.base, BlockTube.Kind.VALVE);
            ItemStack held = new ItemStack(item); hand(f, held); f.aim(Vec3.atCenterOf(f.base));
            var before = valve.saveWithFullMetadata(f.level.registryAccess());
            f.check(BlockTube.tryHandInteraction(f.context(f.base)) == net.minecraft.world.InteractionResult.PASS
                    && before.equals(valve.saveWithFullMetadata(f.level.registryAccess())) && f.player.getMainHandItem() == held && held.getCount() == 1,
                    "original all-metadata tube-item exclusion: held independent machine does NOT hand-toggle/pay the supporting valve: " + item);
            f.check(f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && f.player.getMainHandItem().isEmpty()
                    && f.level.getBlockState(f.base.above()).getBlock().asItem() == item && valve.isFlowAllowed(),
                    "real flattened BlockItem spends ONE item to place above the actual still-open valve: " + item);
            if (item == ThaumcraftTubes.ESSENTIA_CRYSTALLIZER_ITEM.get()) f.check(((TileEssentiaCrystalizer) f.level.getBlockEntity(f.base.above())).getFacing() == Direction.DOWN,
                    "placing on valve UP still assigns original crystallizer opposite-facing DOWN");
        }
        f.reset(); TileTube valve = f.tube(f.base, BlockTube.Kind.VALVE); ItemStack ordinary = new ItemStack(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get());
        hand(f, ordinary); f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && !valve.isFlowAllowed()
                && f.player.getMainHandItem() == ordinary && ordinary.getCount() == 1 && f.level.isEmptyBlock(f.base.above()),
                "do NOT broaden exclusion to all BlockItems: ordinary held construct still toggles valve without placing or spending it");
    }

    private static void orderedItem(Fixture f) {
        f.reset(); var item = (ItemCrystalEssence) ThaumcraftContent.CRYSTAL_ESSENCE.get(); ItemStack stack = ItemCrystalEssence.create(Aspect.FIRE);
        f.check(item instanceof IEssentiaContainerItem && stack.getMaxStackSize() == 64 && !stack.isDamageableItem() && ItemCrystalEssence.data(stack).amounts().equals(Map.of("ignis", 1)), "separate original64-stack real container item; production factory canonical typed1 only");
        f.check(invalid(() -> ItemCrystalEssence.create(null)), "null cannot mint a factory output");
        var order = new AspectList().add(Aspect.FIRE, 7).add(Aspect.AIR, 3); stack.set(DataComponents.CUSTOM_NAME, Component.literal("preserve-me"));
        var custom = new CompoundTag(); custom.putString("Unrelated", "paid-fixture"); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        item.setAspects(stack, order); order.aspects.clear();
        f.check(ItemCrystalEssence.data(stack).amounts().keySet().stream().toList().equals(List.of("ignis", "aer")) && ItemCrystalEssence.data(stack).amount("ignis") == 7 && ItemCrystalEssence.color(stack) == (0xff000000 | Aspect.FIRE.getColor()), "full positive ordered API preserves first entry, NOT singleton/amount1 truncation or sorting");
        var returned = item.getAspects(stack); returned.aspects.clear(); f.check(item.getAspects(stack).getAmount(Aspect.AIR) == 3, "defensive mutable API read cannot edit component state");
        ItemStack before = stack.copy();
        var invalids = new ArrayList<AspectList>(); invalids.add(null); invalids.add(new AspectList().add(Aspect.FIRE, 2).add(Aspect.AIR, -1)); invalids.add(new AspectList().add(Aspect.FIRE, 0)); invalids.add(new AspectList().add(Aspect.FIRE, Integer.MAX_VALUE).add(Aspect.AIR, 1));
        var nullKey = new AspectList().add(Aspect.FIRE, 1); nullKey.aspects.put(null, 1); invalids.add(nullKey);
        for (AspectList bad : invalids) f.check(invalid(() -> item.setAspects(stack, bad)) && ItemStack.matches(stack, before), "WHOLE invalid write leaves previous ordered data AND every unrelated component unchanged");
        String unregisteredTag = "crystal_unregistered_" + java.util.UUID.randomUUID().toString().replace("-", ""); var unregistered = new Aspect(unregisteredTag, 0x123456, new Aspect[] {Aspect.AIR, Aspect.FIRE}); Aspect.aspects.remove(unregisteredTag, unregistered);
        f.check(invalid(() -> ItemCrystalEssence.create(unregistered)) && invalid(() -> item.setAspects(stack, new AspectList().add(unregistered, 1))) && ItemStack.matches(stack, before), "noncanonical/unregistered type object cannot mint a typed factory output or partially replace a valid list");
        roundTrip(f, stack); var encoded = CrystalEssenceData.CODEC.encodeStart(NbtOps.INSTANCE, ItemCrystalEssence.data(stack)).getOrThrow();
        f.check(CrystalEssenceData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow().equals(ItemCrystalEssence.data(stack)), "persistent list codec retains ALL types/amount/order");
        f.player.getInventory().setItem(0, stack.copyWithCount(63)); ItemStack reversed = stack.copyWithCount(1); item.setAspects(reversed, new AspectList().add(Aspect.AIR, 3).add(Aspect.FIRE, 7));
        f.check(f.player.getInventory().add(reversed) && f.player.getInventory().getItem(0).getCount() == 63 && f.player.getInventory().getItem(1).getCount() == 1 && ItemCrystalEssence.color(f.player.getInventory().getItem(1)) == (0xff000000 | Aspect.AIR.getColor()), "native inventory MUST NOT merge differently ordered lists even with equal maps/name/custom data and spare room");
        f.check(f.player.getInventory().add(stack.copyWithCount(1)) && f.player.getInventory().getItem(0).getCount() == 64 && f.player.getInventory().getItem(1).getCount() == 1, "same COMPLETE ordered components really merge63+1 to64 without changing per-item amounts or the differently tinted stack"); f.player.getInventory().clearContent();
        // Fresh native knowledge ALREADY knows the six primals. Use genuinely
        // undiscovered compounds instead of demanding that normal Aer be hidden.
        ItemStack hidden = ItemCrystalEssence.create(Aspect.MAGIC); item.setAspects(hidden, new AspectList().add(Aspect.MAGIC, 7).add(Aspect.ENERGY, 3));
        var lines = new ArrayList<Component>(); KnowledgeAccess.get(f.player).copyFrom(new PlayerKnowledge()); item.appendAspectTooltip(hidden, f.player, lines);
        f.check(lines.equals(List.of(Component.translatable("tc.aspect.unknown"), Component.translatable("tc.aspect.unknown"))), "unknown tooltip does not leak type/amount");
        KnowledgeAccess.get(f.player).addDiscoveredAspect("potentia"); lines.clear(); item.appendAspectTooltip(hidden, f.player, lines);
        f.check(lines.size() == 2 && lines.getFirst().getString().equals("Potentia x3") && lines.get(1).equals(Component.translatable("tc.aspect.unknown"))
                && ItemCrystalEssence.data(hidden).firstTag().equals("praecantatio"), "original tooltip sorts tags for presentation WITHOUT sorting stored first-entry tint; discovered-only visibility, not primordial amnesia");
        for (int count : new int[] {1, 64}) { var tags = ObjectAspectResolver.get(stack.copyWithCount(count), f.level); f.check(tags.size() == 2 && tags.getAmount(Aspect.FIRE) == 7 && tags.getAmount(Aspect.AIR) == 3 && tags.getAmount(Aspect.AURA) == 0, "EMPTY base + dynamic contents once per ITEM, not stack multiplier or auram/shard/recipe base"); }
        var id = TubeVariantsRuntimeChecks.id("crystal_essence"); f.check(ObjectAspectCatalog.registeredIds().contains(id) && ObjectAspectCatalog.get(id).size() == 0, "native crystal ID has explicit original431 EMPTY binding, not unknown fallback");
        ItemStack fresh = new ItemStack(item), empty = new ItemStack(item), named = new ItemStack(item), nbt = new ItemStack(item);
        item.setAspects(empty, new AspectList()); named.set(DataComponents.CUSTOM_NAME, Component.literal("untyped-NBT")); nbt.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        f.check(fresh.getComponentsPatch().isEmpty() && ItemCrystalEssence.data(fresh) == null && item.getAspects(empty) == null && ItemCrystalEssence.data(empty).isEmpty(), "NO prototype component; fresh absent and explicit initialized EMPTY remain different");
        for (ItemStack untyped : List.of(fresh, empty, named, nbt)) { var snapshot = untyped.copy(); for (int i = 0; i < 8; i++) { ItemCrystalEssence.color(untyped); item.appendAspectTooltip(untyped, f.player, new ArrayList<>()); } f.check(ItemStack.matches(snapshot, untyped), "500ms tint/tooltip never bind contents or draw production contents RNG"); }
        for (ItemStack untyped : List.of(fresh, empty, named, nbt)) item.inventoryTick(untyped, f.level, f.player, 0, true);
        CrystalEssenceData chosen = ItemCrystalEssence.data(fresh);
        f.check(chosen != null && chosen.amounts().size() == 1 && chosen.amount(chosen.firstTag()) == 1 && Aspect.getAspect(chosen.firstTag()) != null && fresh.getCount() == 1, "fresh no-NBT inventory update uses original ITEM-local RNG: membership/type1 only, no seeding/distribution claim");
        f.check(ItemCrystalEssence.data(empty).isEmpty() && ItemCrystalEssence.data(named) == null && ItemCrystalEssence.data(nbt) == null, "explicitEMPTY, CUSTOM_NAME and even CUSTOM_DATA{} prevent original no-NBT lazy binding");
        for (int i = 0; i < 20; i++) { item.inventoryTick(fresh, f.level, f.player, 0, true); item.inventoryTick(stack, f.level, f.player, 0, true); }
        f.check(chosen.equals(ItemCrystalEssence.data(fresh)) && ItemStack.matches(stack, before), "typed output AND valid full-list items never reroll or lose unrelated data");
        item.onCraftedBy(stack, f.level, f.player); item.onCraftedBy(empty, f.level, f.player); item.onCraftedBy(named, f.level, f.player);
        f.check(ItemStack.matches(stack, before) && ItemCrystalEssence.data(empty).isEmpty() && ItemCrystalEssence.data(named) == null, "onCreated mapping likewise NEVER rerolls typed, initializedEMPTY or unrelated named-NBT stacks");
        var fresh64 = new ItemStack(item, 64); item.inventoryTick(fresh64, f.level, f.player, 0, false); var type64 = ItemCrystalEssence.data(fresh64);
        f.check(fresh64.getCount() == 64 && type64 != null && type64.amounts().size() == 1 && type64.toAspects().visSize() == 1, "lazy initialization of raw stack64 binds per-ITEM1, not contents64, without consuming physical items");
        ItemStack crafted = new ItemStack(item); item.onCraftedBy(crafted, f.level, f.player); f.check(ItemCrystalEssence.data(crafted) != null && ItemCrystalEssence.data(crafted).toAspects().visSize() == 1, "native onCraftedBy maps original no-NBT onCreated typing");
        item.setAspects(stack, new AspectList()); f.check(ItemCrystalEssence.data(stack).equals(CrystalEssenceData.EMPTY) && stack.getHoverName().getString().equals("preserve-me") && stack.get(DataComponents.CUSTOM_DATA).copyTag().equals(custom), "valid empty setter leaves explicitEMPTY and preserves unrelated name/custom data"); roundTrip(f, stack);
    }

    private static void rawFacades(Fixture f) {
        for (Direction facing : Direction.values()) {
            f.reset(); var tile = machine(f, f.base, facing); f.check(!TileTube.class.isAssignableFrom(tile.getClass()) && !EssentiaStorageEndpoint.class.isAssignableFrom(tile.getClass()) && tile.getAspect() == null && tile.getProgress() == 0 && tile.getTickCount() == 0 && tile.getSpin() == 0 && tile.getSpinIncrement() == 0 && tile.getRed() == 1 && tile.getGreen() == 1 && tile.getBlue() == 1 && tile.getVenting() == 0, "independent original single-slot endpoint, honest fresh transient defaults");
            for (Direction face : faces()) f.check(tile.isConnectable(face) == (face == facing) && tile.canInputFrom(face) == (face == facing) && !tile.canOutputTo(face) && tile.getSuctionType(face) == null && tile.getSuctionAmount(face) == (face == facing ? 128 : 64), "empty facing128/ALL other faces64 INCLUDING null/nonports, no transport output: " + facing + "/" + face);
            tile.setSuction(Aspect.FIRE, 999); f.check(tile.getMinimumSuction() == 0 && !tile.renderExtendedTube() && tile.getSuctionAmount(facing) == 128, "minimum0/no extended tube/setSuction is a no-op");
            var saved = tile.saveWithFullMetadata(f.level.registryAccess());
            for (Direction face : faces()) if (face != facing) f.check(tile.addEssentia(Aspect.FIRE, 8, face) == 0 && saved.equals(tile.saveWithFullMetadata(f.level.registryAccess())), "positive transport insert only through actual facing, not face-blind CF");
            for (int amount : new int[] {-1, 0}) f.check(tile.addToContainer(Aspect.AIR, amount) == amount && tile.addEssentia(Aspect.AIR, amount, facing) == 0, "negative hardened and zero original no-op never create stock");
            f.check(tile.addToContainer(null, 1) == 1 && tile.addEssentia(null, 1, facing) == 0 && !tile.takeFromContainer(null, 1) && saved.equals(tile.saveWithFullMetadata(f.level.registryAccess())), "null authority hardening leaves WHOLE paid state unchanged");
            power(f, tile, true); for (Direction face : faces()) f.check(tile.getSuctionAmount(face) == 0, "actual power sets EVERY face pressure0");
            f.check(tile.addEssentia(Aspect.AIR, 99, facing) == 1 && tile.addToContainer(Aspect.FIRE, 1) == 1 && tile.getAspect() == Aspect.AIR, "raw positive primal insertion clips1 and is NOT power-locked; full slot refuses next");
            f.check(tile.doesContainerAccept(Aspect.MAGIC) && tile.doesContainerAccept(Aspect.WATER) && !tile.doesContainerAccept(null), "original valid-type acceptance predicate is NOT a new capacity/power gate; only invalid type is hardened");
            for (Direction face : faces()) f.check(tile.getEssentiaType(face) == Aspect.AIR && tile.getEssentiaAmount(face) == 1 && tile.takeEssentia(Aspect.AIR, 99, face) == 0, "all-face public amount/type describe input1, raw transport TAKE always0");
            var copy = tile.getAspects(); copy.aspects.clear(); tile.setAspects(new AspectList().add(Aspect.FIRE, 99));
            f.check(tile.getAspects().getAmount(Aspect.AIR) == 1 && tile.containerContains(Aspect.AIR) == 1 && tile.containerContains(Aspect.FIRE) == 0 && tile.doesContainerContainAmount(Aspect.AIR, 1) && !tile.doesContainerContainAmount(Aspect.AIR, 2)
                    && tile.doesContainerContain(new AspectList()) && tile.doesContainerContain(new AspectList().add(Aspect.AIR, 1)) && !tile.doesContainerContain(new AspectList().add(Aspect.AIR, 2)) && !tile.doesContainerContain(null) && !tile.takeFromContainer(new AspectList().add(Aspect.AIR, 1)), "defensive list/set no-op/deprecated ALL exact1 query and list-take alwaysfalse");
            for (int amount : new int[] {-1, 0, 2}) f.check(!tile.takeFromContainer(Aspect.AIR, amount), "container take exact1 only");
            f.check(!tile.takeFromContainer(Aspect.FIRE, 1) && tile.getAspect() == Aspect.AIR && tile.takeFromContainer(Aspect.AIR, 1), "EXPLICIT wrong-type-take authority fix: only actual type1 removes stock, even while powered");
            power(f, tile, false); f.check(tile.addToContainer(Aspect.MAGIC, 99) == 98 && tile.getAspect() == Aspect.MAGIC, "unsided container offer accepts ONE positive compound, not CF's random split");
            for (Direction face : faces()) f.check(tile.getSuctionAmount(face) == 64, "occupied unpowered pressure64 on ALL faces");
        }
    }

    private static void baseline(Fixture f) {
        f.reset(); var tile = machine(f, f.base, Direction.DOWN); f.put(f.base.below(), ThaumcraftAlembics.ALEMBIC.get()); var source = (TileAlembic) f.level.getBlockEntity(f.base.below()); source.addToContainer(Aspect.ENERGY, 2);
        for (int call = 1; call <= 4; call++) { tick(f, tile); f.check(tile.getTickCount() == call && tile.getAspect() == null && tile.getProgress() == 0 && source.getAmount() == 2, "native fresh ticks1..4 do not fill"); }
        tick(f, tile); f.check(tile.getTickCount() == 5 && tile.getAspect() == Aspect.ENERGY && tile.getProgress() == 0 && source.getAmount() == 1, "native fill5 pays source1, then resets progress0 EVEN after successful fill");
        ticks(f, tile, 4); f.check(tile.getProgress() == 0 && tile.getTickCount() == 9, "no first progress before10"); tick(f, tile); f.check(tile.getProgress() == 1, "first baseline +1 exactly10, no fake EARTH source/stub");
        ticks(f, tile, 990); f.check(tile.getTickCount() == 1000 && tile.getProgress() == 199 && tile.getAspect() == Aspect.ENERGY && f.pendingRows().isEmpty() && crystalDrops(f) == 0 && source.getAmount() == 1, "1000 calls FROM EMPTY is still only199 progress, not premature output");
        ticks(f, tile, 4); f.check(tile.getProgress() == 199 && tile.getAspect() == Aspect.ENERGY, "1004 still pending"); tick(f, tile);
        f.check(tile.getTickCount() == 1005 && tile.getAspect() == null && tile.getProgress() == 0 && f.pendingRows().size() == 1 && source.getAmount() == 1 && crystalDrops(f) == 0, "1005 converts exact paid input to ONE durable escrow BEFORE level-post ADD");
        f.flushFresh(); var drop = CrystallizerOutputRuntimeChecks.onlyCrystal(f); assertTyped(f, drop.getItem(), Aspect.ENERGY, 1); drop.discard();
        f.check(tile.getTickCount() == 1005 && source.getAmount() == 1, "completion neither pulls another source unit nor imposes a CF39 reset");
    }

    private static void powerAndProgressPhase(Fixture f) {
        f.reset(); var tile = machine(f, f.base, Direction.DOWN); power(f, tile, true); ticks(f, tile, 3);
        f.check(tile.getTickCount() == 3 && tile.getAspect() == null && tile.getProgress() == 0, "count++ while POWERED even empty, unlike CF idle freeze");
        tile.addToContainer(Aspect.FIRE, 1); power(f, tile, false); tick(f, tile); f.check(tile.getTickCount() == 4 && tile.getProgress() == 0, "unpower inherits3 phase, not a reset timer"); tick(f, tile); f.check(tile.getProgress() == 1, "real phase5 steps");
        power(f, tile, true); ticks(f, tile, 8); f.check(tile.getTickCount() == 13 && tile.getProgress() == 1 && tile.getAspect() == Aspect.FIRE, "powered occupied ticks keep advancing count but pause progress"); power(f, tile, false); ticks(f, tile, 2); f.check(tile.getTickCount() == 15 && tile.getProgress() == 2, "next actual fifth phase resumes one step");
        ticks(f, tile, 25); f.check(tile.getTickCount() == 40 && tile.getProgress() == 7, "natural progress7, never clock/counter writes");
        f.check(tile.takeFromContainer(Aspect.FIRE, 1) && tile.getProgress() == 7 && tile.addToContainer(Aspect.WATER, 1) == 0 && tile.getProgress() == 7, "raw removal/re-add BEFORE empty fifth branch preserves transient progress"); ticks(f, tile, 5); f.check(tile.getProgress() == 8 && tile.getAspect() == Aspect.WATER, "replacement actual stock inherits old progress, not a fabricated reset");
        tile.takeFromContainer(Aspect.WATER, 1); ticks(f, tile, 5); f.check(tile.getTickCount() == 50 && tile.getProgress() == 0 && tile.getAspect() == null, "empty fifth branch resets progress0 even with no source");
        tile.addToContainer(Aspect.MAGIC, 1); ticks(f, tile, 999); f.check(tile.getProgress() == 199 && tile.getAspect() == Aspect.MAGIC, "direct phase0 input requires full baseline1000 calls"); tick(f, tile); f.check(tile.getAspect() == null && f.pendingRows().size() == 1, "direct input outputs only at1000, without public completion shortcut"); f.flushFresh(); CrystallizerOutputRuntimeChecks.onlyCrystal(f).discard();
    }

    private static void persistence(Fixture f) {
        f.reset(); var tile = machine(f, f.base, Direction.WEST); tile.addToContainer(Aspect.MAGIC, 1); ticks(f, tile, 37);
        var saved = tile.saveWithFullMetadata(f.level.registryAccess()); var update = tile.getUpdateTag(f.level.registryAccess());
        f.check(saved.getString("Aspect").equals("praecantatio") && saved.getByte("face") == Direction.WEST.get3DDataValue() && update.getString("Aspect").equals("praecantatio"), "original Aspect + BYTE face disk/custom update, not an item component or public output store");
        for (String key : List.of("progress", "count", "spin", "spinIncrement", "red", "green", "blue", "venting")) f.check(!saved.contains(key) && !update.contains(key), "transient field never saved: " + key);
        var old = tile; tile = reload(f, saved);
        f.check(tile != old && tile.getAspect() == Aspect.MAGIC && tile.getFacing() == Direction.WEST && tile.getProgress() == 0 && tile.getTickCount() == 0 && tile.getSpin() == 0 && tile.getRed() == 1 && old.addToContainer(Aspect.FIRE, 1) == 1 && !old.takeFromContainer(Aspect.MAGIC, 1), "real registered loadStatic pending reload resets ONLY transient progress/count/display; stale owner cannot withdraw");
        var mirror = ThaumcraftTubes.CRYSTALLIZER_BLOCK_ENTITY.get().create(f.base, tile.getBlockState()); f.check(mirror != null, "actual independent native update factory"); mirror.loadWithComponents(update, f.level.registryAccess());
        f.check(mirror.getAspect() == Aspect.MAGIC && mirror.getProgress() == 0 && mirror.getTickCount() == 0 && tile.getUpdatePacket() != null, "actual custom update retains valid stock/facing without progress/timer injection");
        f.check(EssentiaTransfers.finishCrystallizer(tile).committedUnits() == 0 && tile.prepareCompletion(tile.snapshotAtomicState()) == null, "pending reload0 is NOT externally completable; no chosen output/free native phase");
        ticks(f, tile, 999); f.check(tile.getAspect() == Aspect.MAGIC && tile.getProgress() == 199 && tile.getTickCount() == 999 && f.pendingRows().isEmpty(), "pending reload requires full baseline200 increments, NOT CF immediate process0 completion"); tick(f, tile);
        f.check(tile.getAspect() == null && tile.getProgress() == 0 && tile.getTickCount() == 1000 && f.pendingRows().size() == 1, "1000th native reload tick owns exactly one paid output"); f.flushFresh(); var drop = CrystallizerOutputRuntimeChecks.onlyCrystal(f); assertTyped(f, drop.getItem(), Aspect.MAGIC, 1); roundTrip(f, drop.getItem()); drop.discard();
        f.reset(); tile = machine(f, f.base, Direction.DOWN); tile.addToContainer(Aspect.AIR, 1); var valid = tile.saveWithFullMetadata(f.level.registryAccess());
        var malformed = new LinkedHashMap<String, CompoundTag>();
        for (String type : List.of("", "unknown-crystallizer")) { var bad = valid.copy(); bad.putString("Aspect", type); malformed.put("unknown aspect " + type, bad); }
        for (String key : List.of("Aspect", "face")) { var bad = valid.copy(); bad.put(key, new ListTag()); malformed.put("wrong type " + key, bad); }
        for (int face : new int[] {-1, 6, 127}) { var bad = valid.copy(); bad.putByte("face", (byte) face); malformed.put("bad face " + face, bad); }
        var intFace = valid.copy(); intFace.putInt("face", 0); malformed.put("numeric but NOT BYTE facing", intFace);
        for (var entry : malformed.entrySet()) {
            tile = reload(f, entry.getValue()); f.check(tile.isQuarantined() && tile.getAspect() == null && tile.getSuctionAmount(null) == 0 && !tile.isConnectable(Direction.DOWN) && tile.addToContainer(Aspect.AIR, 1) == 1 && !tile.takeFromContainer(Aspect.AIR, 1), "whole malformed payload disabled, not a truncated spendable paid subset: " + entry.getKey());
            ticks(f, tile, 1001); var rejected = tile.saveWithFullMetadata(f.level.registryAccess()); f.check(tile.getTickCount() == 0 && tile.getProgress() == 0 && f.pendingRows().isEmpty() && rejected.getCompound("RejectedCrystallizer").equals(entry.getValue()) && tile.getUpdateTag(f.level.registryAccess()).getCompound("RejectedCrystallizer").equals(entry.getValue()), "quarantine retains WHOLE original evidence without output");
            tile = reload(f, rejected); f.check(tile.isQuarantined() && tile.getUpdateTag(f.level.registryAccess()).getCompound("RejectedCrystallizer").equals(entry.getValue()), "second reload neither nests nor forgets rejection");
        }
    }

    static TileEssentiaCrystalizer machine(Fixture f, BlockPos pos, Direction facing) { f.put(pos, Blocks.AIR); f.level.setBlock(pos, ThaumcraftTubes.ESSENTIA_CRYSTALLIZER.get().defaultBlockState().setValue(BlockEssentiaCrystalizer.FACING, facing), TubeVariantsRuntimeChecks.QUIET); return (TileEssentiaCrystalizer) f.level.getBlockEntity(pos); }
    static void tick(Fixture f, TileEssentiaCrystalizer tile) { var pos = tile.getBlockPos(); var state = f.level.getBlockState(pos); var ticker = state.getTicker(f.level, ThaumcraftTubes.CRYSTALLIZER_BLOCK_ENTITY.get()); if (ticker == null) throw new IllegalStateException("Missing actual crystallizer ticker"); ticker.tick(f.level, pos, state, tile); }
    static void ticks(Fixture f, TileEssentiaCrystalizer tile, int count) { for (int call = 0; call < count; call++) tick(f, tile); }
    static void power(Fixture f, TileEssentiaCrystalizer tile, boolean powered) { BlockPos pos = tile.getBlockPos().east(); f.level.setBlock(pos, (powered ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); f.check(tile.isPowered() == powered, "actual adjacent redstone, not a forged power field"); }
    static TileEssentiaCrystalizer reload(Fixture f, CompoundTag tag) { f.level.removeBlockEntity(f.base); var loaded = BlockEntity.loadStatic(f.base, f.level.getBlockState(f.base), tag, f.level.registryAccess()); f.check(loaded instanceof TileEssentiaCrystalizer, "actual separate BEType/loadStatic factory"); f.level.setBlockEntity(loaded); return (TileEssentiaCrystalizer) loaded; }
    static void assertTyped(Fixture f, ItemStack stack, Aspect type, int count) { var data = ItemCrystalEssence.data(stack); f.check(stack.is(ThaumcraftContent.CRYSTAL_ESSENCE.get()) && stack.getCount() == count && data != null && data.amounts().size() == 1 && data.amount(type.getTag()) == 1, "actual paid output retains exact type1 per ITEM, count=" + count); }
    static void roundTrip(Fixture f, ItemStack stack) { f.check(ItemStack.matches(stack, ItemStack.parse(f.level.registryAccess(), stack.save(f.level.registryAccess())).orElseThrow()), "native item save/load retains complete count/components"); var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), f.level.registryAccess()); try { ItemStack.STREAM_CODEC.encode(wire, stack); f.check(ItemStack.matches(stack, ItemStack.STREAM_CODEC.decode(wire)) && !wire.isReadable(), "registered native component packet roundtrip"); } finally { wire.release(); } }
    static int crystalDrops(Fixture f) { return f.drops().stream().filter(drop -> drop.getItem().is(ThaumcraftContent.CRYSTAL_ESSENCE.get())).mapToInt(drop -> drop.getItem().getCount()).sum(); }
    private static Direction[] faces() { return new Direction[] {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null}; }
    private static boolean invalid(Runnable action) { try { action.run(); return false; } catch (IllegalArgumentException expected) { return true; } }
    private static void matrix(TileArcaneWorkbench table, ItemStack earned, int count) { for (int slot = 0; slot < 9; slot++) table.setItem(slot, ItemStack.EMPTY); for (int slot : new int[] {0, 2}) table.setItem(slot, new ItemStack(Items.IRON_INGOT, count)); table.setItem(1, new ItemStack(Items.DISPENSER, count)); for (int slot : new int[] {3, 5}) table.setItem(slot, new ItemStack(ThaumcraftContent.SHARDS.get("balanced").get(), count)); table.setItem(4, earned); table.setItem(6, new ItemStack(Items.OAK_PLANKS, count)); table.setItem(7, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), count)); table.setItem(8, new ItemStack(ThaumcraftNaturals.SILVERWOOD_PLANKS.get(), count)); }
    private static void constructMatrix(TileArcaneWorkbench table, int count) { for (int slot : new int[] {0, 8}) table.setItem(slot, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(), count)); for (int slot : new int[] {1, 3, 5, 7}) table.setItem(slot, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), count)); for (int slot : new int[] {2, 6}) table.setItem(slot, new ItemStack(ThaumcraftContent.MATERIALS.get("vis_filter").get(), count)); table.setItem(4, new ItemStack(ThaumcraftNaturals.GREATWOOD_PLANKS.get(), count)); }
    private static ItemStack wand(String cap, int amount) { var stack = new ItemStack(ThaumcraftContent.WAND.get()); var vis = new LinkedHashMap<String, Integer>(); for (String primal : WandData.PRIMALS) vis.put(primal, amount); stack.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", cap, vis)); return stack; }
    private static ItemStack shortWand(String cap, String primal, int amount) { var stack = wand(cap, 5000); stack.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(stack).withAmount(primal, amount)); return stack; }
    private static void checkVis(Fixture f, ItemStack stack, int aqua, int terra, int ordo, String why) { for (String primal : WandData.PRIMALS) f.check(ItemWandCasting.getVis(stack, primal) == (primal.equals("aqua") ? aqua : primal.equals("terra") ? terra : primal.equals("ordo") ? ordo : 5000), why + ": " + primal); }
    private static void refuse(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu, String why) { menu.broadcastChanges(); var before = table.saveWithoutMetadata(f.level.registryAccess()); menu.clicked(0, 0, ClickType.PICKUP, f.player); f.check(menu.getCarried().isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), why); }
    private static ItemStack removeInventory(Fixture f, Item item, int count) { for (int slot = 0; slot < 36; slot++) if (f.player.getInventory().getItem(slot).is(item) && f.player.getInventory().getItem(slot).getCount() >= count) return f.player.getInventory().removeItem(slot, count); throw new AssertionError("Missing earned dependency/output " + item + " x" + count); }
    private static void hand(Fixture f, ItemStack stack) { for (int slot = 0; slot < 9; slot++) if (f.player.getInventory().getItem(slot).isEmpty()) { f.player.getInventory().selected = slot; f.player.setItemInHand(InteractionHand.MAIN_HAND, stack); return; } throw new AssertionError("No free hand slot; never overwrite another paid output"); }
}
