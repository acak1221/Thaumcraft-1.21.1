package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.config.ThaumcraftResources;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileNodeStabilizer;

/** M09a SERVER INPUT fixtures: two sets of raw ingredients, researched knowledge,
 * two independently funded greatwood/gold and greatwood/thaumium wands, discount gear and an empty bench.
 * Both stabilizers are earned through the actual native menu. No VisNet, special
 * node, full survival acquisition, client animation or elapsed-client-time claim. */
public final class NodeStabilizerRuntimeChecks {
    private static final int[] OCCUPIED = {1, 3, 4, 5, 6, 7, 8};
    private static final List<String> PAID_PRIMALS = List.of("aqua", "terra", "ordo");

    private NodeStabilizerRuntimeChecks() {}

    /** Suggested Fabric wrapper timeout: 240 ticks; no loader registration here. */
    public static void recipesPlacementAndTransientDevice(GameTestHelper test) {
        requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            recipes(f);
        }
        test.succeed();
    }

    private static void recipes(Fixture f) {
        f.reset();
        f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
        f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench,
                "actual connected SURVIVAL actor opens the native arcane workbench");
        var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
        var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
        var holder = f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("arcane/node_stabilizer")).orElseThrow();
        f.check(holder.value() instanceof ArcaneShapedRecipe, "registered recipe-manager holder, not an injected recipe/output");
        var recipe = (ArcaneShapedRecipe) holder.value();
        f.check(recipe.research().equals("NODESTABILIZER") && recipe.pattern().equals(List.of(" G ", "QPQ", "SNS"))
                        && recipe.vis().equals(Map.of("aqua", 32, "terra", 32, "ordo", 32))
                        && recipe.result().is(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get()) && recipe.result().getCount() == 1,
                "original ConfigRecipes199: seven occupied cells, literal gold1/quartz BLOCK2/piston1/arcane BRICKS2/Nitor ITEM1, output1 and whole vis32/32/32");
        var literals = Map.of("G", Items.GOLD_INGOT, "Q", Items.QUARTZ_BLOCK, "P", Items.PISTON,
                "S", ThaumcraftMasonry.ARCANE_STONE_BRICKS.get().asItem(), "N", ThaumcraftContent.MATERIALS.get("nitor").get());
        f.check(recipe.key().keySet().equals(literals.keySet()), "exact five original literal ingredient symbols");
        literals.forEach((symbol, item) -> f.check(recipe.key().get(symbol).getItems().length == 1
                        && recipe.key().get(symbol).getItems()[0].is(item),
                "actual resolved literal maps to ONE exact native item, not broad ore/variant alternatives: " + symbol));
        var research = ResearchCatalog.get("NODESTABILIZER");
        f.check(research != null && research.category().equals("THAUMATURGY") && research.column() == -7 && research.row() == -4
                        && research.complexity() == 1 && research.flags().equals(Set.of()) && !research.isSecondary()
                        && research.parents().equals(List.of("NODEPRESERVE"))
                        && research.aspectCost().equals(Map.of("auram", 4, "ordo", 4, "potentia", 4))
                        && research.iconExpression().contains("blockStoneDevice") && research.iconExpression().contains("9")
                        && research.pages().stream().anyMatch(page -> "NodeStabilizer".equals(page.recipeKey())),
                "original primary note, parent, research-point costs, book key and stoneDevice9 icon provenance; not Secondary/concealed or a vis bill");

        matrix(table, 2);
        table.setItem(10, wand("greatwood", "gold", 5000));
        KnowledgeAccess.get(f.player).addResearch("NODEPRESERVE");
        KnowledgeAccess.get(f.player).addResearch("VISPOWER");
        KnowledgeAccess.get(f.player).removeResearch("NODESTABILIZER");
        refuse(f, table, menu, "NODEPRESERVE/VISPOWER do not replace actual NODESTABILIZER completion");
        KnowledgeAccess.get(f.player).addResearch("NODESTABILIZER");
        var correct = ArcaneRecipeInput.snapshot(table).items().stream().map(ItemStack::copy).toList();
        for (int slot : OCCUPIED) {
            table.setItem(slot, ItemStack.EMPTY);
            refuse(f, table, menu, "each original occupied cell is required: " + slot);
            table.setItem(slot, correct.get(slot).copy());
        }
        for (int slot : new int[] {0, 2}) {
            table.setItem(slot, new ItemStack(Items.STICK));
            refuse(f, table, menu, "literal blank upper border cannot be filled or shrunk away: " + slot);
            table.setItem(slot, ItemStack.EMPTY);
        }
        for (int slot : new int[] {3, 5}) {
            wrong(f, table, menu, slot, List.of(Items.QUARTZ, Items.NETHER_QUARTZ_ORE, Items.CHISELED_QUARTZ_BLOCK, Items.SMOOTH_QUARTZ), correct);
        }
        wrong(f, table, menu, 1, List.of(Items.GOLD_NUGGET, Items.GOLD_BLOCK, Items.RAW_GOLD, Items.IRON_INGOT), correct);
        wrong(f, table, menu, 4, List.of(Items.STICKY_PISTON, Items.DISPENSER, Items.OBSERVER), correct);
        for (int slot : new int[] {6, 8}) {
            wrong(f, table, menu, slot, List.of(ThaumcraftMasonry.ARCANE_STONE.get().asItem(), Items.STONE_BRICKS,
                    ThaumcraftMasonry.SLAB.get().asItem()), correct);
        }
        wrong(f, table, menu, 7, List.of(ThaumcraftContent.MATERIALS.get("alumentum").get(),
                Items.GLOWSTONE_DUST, Items.GLOWSTONE), correct);
        f.check(ThaumcraftResources.NITOR.get().asItem() == Items.AIR,
                "placed Nitor has NO separately registered block-item alias; recipe N is the actual resource ITEM");
        f.put(f.base.east(), ThaumcraftResources.NITOR.get());
        table.setItem(7, ItemStack.EMPTY);
        refuse(f, table, menu, "a real placed Nitor light is not an ingredient in the empty N ITEM cell");
        table.setItem(7, correct.get(7).copy());
        f.put(f.base.east(), Blocks.AIR);

        table.setItem(10, ItemStack.EMPTY);
        refuse(f, table, menu, "no inserted paying wand");
        table.setItem(10, wand("wood", "iron", 2500));
        menu.broadcastChanges();
        f.check(!menu.craftable() && menu.requiredVis("aqua") == 3520 && ItemWandCasting.getMaxVis(table.getItem(10)) == 2500,
                "wood25 cannot pay the real iron-cap35.2-vis price; no above-capacity fake funding");
        refuse(f, table, menu, "wood/iron insufficient capacity refuses all seven ingredients");
        for (String primal : PAID_PRIMALS) {
            var poor = wand("greatwood", "gold", 5000);
            poor.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(poor).withAmount(primal, 3199));
            table.setItem(10, poor);
            refuse(f, table, menu, "one centivis short in EACH actual gold-cap primal refuses the whole transaction: " + primal);
        }

        // Metadata-unrelated modern components must neither reject a literal
        // ingredient nor leak onto output, and leftovers must keep ALL of them.
        for (int slot : OCCUPIED) table.setItem(slot, named(correct.get(slot).copy(), "ingredient-" + slot));
        correct = ArcaneRecipeInput.snapshot(table).items().stream().map(ItemStack::copy).toList();
        var payer = named(wand("greatwood", "gold", 5000), "first-declared-funded-wand");
        table.setItem(10, payer);
        menu.broadcastChanges();
        f.check(menu.craftable() && PAID_PRIMALS.stream().allMatch(tag -> menu.requiredVis(tag) == 3200)
                        && f.level.getRecipeManager().getRecipeFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(), ArcaneRecipeInput.snapshot(table), f.level)
                        .orElseThrow().id().equals(holder.id()),
                "actual manager/menu: bare GOLD cap modifier1F, 3200 EACH; the client's2880 comes from actual saved worn10% gear, NOT a gold-cap discount");

        var before = table.saveWithoutMetadata(f.level.registryAccess());
        KnowledgeAccess.get(f.player).removeResearch("NODESTABILIZER");
        f.check(menu.getSlot(0).remove(1).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())),
                "stale funded result revalidates research at extraction, no ingredients/vis spent");
        KnowledgeAccess.get(f.player).addResearch("NODESTABILIZER");
        menu.broadcastChanges();
        table.setItem(3, new ItemStack(Items.QUARTZ, 2));
        before = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.getSlot(0).remove(1).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())),
                "stale correct-matrix preview cannot extract after quartz BLOCK becomes quartz ITEM");
        table.setItem(3, correct.get(3).copy());
        table.setItem(10, payer);
        menu.broadcastChanges();
        var poor = payer.copy();
        poor.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(poor).withAmount("terra", 3199));
        table.setItem(10, poor);
        before = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.getSlot(0).remove(1).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())),
                "stale price preview rechecks the actual paying wand and ALL three debts");

        table.setItem(10, wand("greatwood", "gold", 3040));
        f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
        menu.broadcastChanges();
        int gogglesPrice = (int) (3200 * (1F - 5 / 100.0F));
        f.check(gogglesPrice == 3040 && menu.craftable() && PAID_PRIMALS.stream().allMatch(tag -> menu.requiredVis(tag) == gogglesPrice),
                "actual GOLD1F with worn goggles5% gives3040, not a thaumium .9F-cap price or assumed minus-one");
        ItemStack movedGoggles = f.player.getItemBySlot(EquipmentSlot.HEAD);
        f.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        f.check(f.player.getInventory().add(movedGoggles), "real goggles moved from worn slot to native inventory");
        before = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.getSlot(0).remove(1).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())),
                "stale discounted preview cannot spend3040 after goggles move to inventory; carried gear grants no discount");

        table.setItem(10, payer);
        menu.broadcastChanges();
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < 36; slot++) {
            inventory.add(f.player.getInventory().getItem(slot).copy());
            f.player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        before = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.quickMoveStack(f.player, 0).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())),
                "full native quick-move destination refuses without spending a stabilizer's raw materials or vis");
        for (int slot = 0; slot < 36; slot++) f.player.getInventory().setItem(slot, inventory.get(slot));
        menu.setCarried(new ItemStack(Items.APPLE));
        before = table.saveWithoutMetadata(f.level.registryAccess());
        menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().is(Items.APPLE) && menu.getCarried().getCount() == 1 && before.equals(table.saveWithoutMetadata(f.level.registryAccess())),
                "incompatible cursor refuses atomically, never overwrites or drops the cursor");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, f.player);
        ItemStack earned = menu.getCarried();
        f.check(earned.is(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get()) && earned.getCount() == 1 && earned.getComponentsPatch().isEmpty(),
                "actual paid plain OWN stabilizer output, not wrong original metadata, a node or a component-bearing ghost");
        menu.setCarried(ItemStack.EMPTY);
        f.check(f.player.getInventory().add(earned), "first earned output enters native owned inventory");
        checkWand(f, table.getItem(10), payer, 1800, "first actual unarmored GOLD3200-per-primal payment");
        for (int slot : OCCUPIED) f.check(ItemStack.matches(table.getItem(slot), correct.get(slot).copyWithCount(1)),
                "exactly one per occupied cell consumed; named/unrelated leftover components survive: " + slot);
        f.check(table.getItem(0).isEmpty() && table.getItem(2).isEmpty(), "blank border remains empty");

        ItemStack firstPaidWand = table.removeItemNoUpdate(10);
        f.check(f.player.getInventory().add(firstPaidWand), "first debited wand retained; never silently refilled for the second craft");
        var secondPayer = named(wand("greatwood", "thaumium", 5000), "second-independent-funded-thaumium-wand-INPUT");
        table.setItem(10, secondPayer);
        f.player.setItemSlot(EquipmentSlot.HEAD, removeInventory(f, ThaumcraftWearables.GOGGLES.get(), 1));
        f.player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get()));
        f.player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ThaumcraftWearables.ROBE_LEGGINGS.get()));
        f.player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ThaumcraftWearables.ROBE_BOOTS.get()));
        table.setItem(10, wand("greatwood", "gold", 2880));
        menu.broadcastChanges();
        f.check(menu.craftable() && PAID_PRIMALS.stream().allMatch(tag -> menu.requiredVis(tag) == 2880),
                "actual client loadout: GOLD1F minus worn10% = .9F, exactly2880each, no gear removal or fictitious cap modifier");
        table.setItem(10, secondPayer);
        int wornPrice = (int) (3200 * (.9F - 10 / 100.0F));
        menu.broadcastChanges();
        f.check(wornPrice == 2559 && PAID_PRIMALS.stream().allMatch(tag -> menu.requiredVis(tag) == wornPrice),
                "goggles5+chest2+legs2+boots1 use the actual single float subtraction:2559 EACH");
        for (String primal : PAID_PRIMALS) {
            var oneShort = secondPayer.copy();
            oneShort.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(oneShort).withAmount(primal, wornPrice - 1));
            table.setItem(10, oneShort);
            refuse(f, table, menu, "actual worn float price one centivis short refuses WHOLE payment: " + primal);
        }
        table.setItem(10, wand("greatwood", "thaumium", wornPrice));
        menu.broadcastChanges();
        f.check(menu.craftable(), "exact2559-each full-worn input is affordable before moving a robe");
        ItemStack movedBoots = f.player.getItemBySlot(EquipmentSlot.FEET);
        f.player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        f.check(f.player.getInventory().add(movedBoots), "actual robe boots move from worn slot to native inventory");
        before = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.getSlot(0).remove(1).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())),
                "stale full-worn preview refuses after boots leave worn slot; inventory robes do not discount payment");
        f.player.setItemSlot(EquipmentSlot.FEET, removeInventory(f, ThaumcraftWearables.ROBE_BOOTS.get(), 1));
        table.setItem(10, secondPayer);
        menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        f.check(f.player.getInventory().countItem(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get()) == 2
                        && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty)
                        && menu.quickMoveStack(f.player, 0).isEmpty(),
                "finite second native quick-move consumes the last SEVEN inputs and yields exactly output2 total; no empty-matrix replay");
        checkWand(f, table.getItem(10), secondPayer, 5000 - wornPrice, "second actual worn-discount payment");
        var inventoryWands = f.player.getInventory().items.stream().filter(stack -> stack.is(ThaumcraftContent.WAND.get())).toList();
        f.check(inventoryWands.size() == 1, "only the first already-paid wand is in native inventory");
        checkWand(f, inventoryWands.getFirst(), payer, 1800, "moving/using another payer never refunds the first");
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) f.player.setItemSlot(slot, ItemStack.EMPTY);
        f.player.closeContainer();
        placement(f, removeInventory(f, ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get(), 1));
    }

    private static void placement(Fixture f, ItemStack earned) {
        for (Direction clicked : Direction.values()) {
            BlockPos support = f.base.east(2), pos = support.relative(clicked);
            f.put(support, Blocks.STONE);
            f.put(pos, Blocks.AIR);
            hand(f, earned);
            f.aim(Vec3.atCenterOf(support));
            f.check(f.use(support, clicked, InteractionHand.MAIN_HAND).consumesAction() && f.player.getMainHandItem().isEmpty(),
                    "native ordinary BlockItem placement spends one earned output on face " + clicked);
            var state = f.level.getBlockState(pos);
            var tile = (TileNodeStabilizer) f.level.getBlockEntity(pos);
            f.check(state.is(ThaumcraftNodeDevices.NODE_STABILIZER.get()) && state.getValues().isEmpty() && tile.getLock() == 1
                            && tile.getAnimationCount() == 0 && tile.getCount() == 0
                            && state.getShape(f.level, pos, CollisionContext.empty()).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                            && state.getCollisionShape(f.level, pos, CollisionContext.of(f.player)).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                            && state.getDestroySpeed(f.level, pos) == 3.0F && state.getBlock().getExplosionResistance() == 25.0F,
                    "original basic1 FULLCUBE outline AND entity collision, hardness3/resistance25, no invented facing property");
            f.check(state.getTicker(f.level, ThaumcraftNodeDevices.NODE_STABILIZER_BLOCK_ENTITY.get()) == null,
                    "storage-less original stabilizer has no server animation/processing ticker");
            var saved = tile.saveWithFullMetadata(f.level.registryAccess());
            var update = tile.getUpdateTag(f.level.registryAccess());
            for (String key : List.of("count", "lock", "progress", "Count", "Lock", "Progress")) {
                f.check(!saved.contains(key) && !update.contains(key), "animation/basic variant is not resource state persisted in NBT: " + key);
            }
            for (boolean sneak : new boolean[] {false, true}) {
                f.player.setShiftKeyDown(sneak);
                f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                f.aim(Vec3.atCenterOf(pos));
                var menu = f.player.containerMenu;
                f.use(pos);
                f.check(f.player.containerMenu == menu && f.level.getBlockState(pos).equals(state)
                                && saved.equals(tile.saveWithFullMetadata(f.level.registryAccess())),
                        "empty-hand/sneaking use invents neither GUI nor manual toggle/rotation");
            }
            f.player.setShiftKeyDown(false);
            // Actual registered native factory/load, same location; not a second
            // installed stock-bearing clone or a claim of chunk unload/restart.
            f.level.removeBlockEntity(pos);
            BlockEntity loaded = BlockEntity.loadStatic(pos, state, saved, f.level.registryAccess());
            f.check(loaded instanceof TileNodeStabilizer, "independent registered stabilizer loadStatic factory");
            f.level.setBlockEntity(loaded);
            var restored = (TileNodeStabilizer) loaded;
            f.check(restored != tile && restored.getLock() == 1 && restored.getAnimationCount() == 0 && restored.getCount() == 0,
                    "storage-less native reload keeps basic1 and fresh transient animation0; no whole-NBT quarantine claim");
            f.check(f.level.destroyBlock(pos, true, f.player), "native destruction of actual placed stabilizer");
            var drops = f.drops().stream().filter(drop -> drop.getItem().is(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get())).toList();
            f.check(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), new ItemStack(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get())),
                    "exactly ONE plain OWN drop, no phantom node/resources/old-metadata device");
            earned = drops.getFirst().getItem().copy();
            drops.getFirst().discard();
            f.put(support, Blocks.AIR);
        }
        f.check(f.player.getInventory().add(earned)
                        && f.player.getInventory().countItem(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get()) == 2,
                "six native placement/break cycles conserve BOTH paid stabilizers, never award another finished device");
    }

    private static void matrix(TileArcaneWorkbench table, int count) {
        for (int slot = 0; slot < 9; slot++) table.setItem(slot, ItemStack.EMPTY);
        table.setItem(1, new ItemStack(Items.GOLD_INGOT, count));
        for (int slot : new int[] {3, 5}) table.setItem(slot, new ItemStack(Items.QUARTZ_BLOCK, count));
        table.setItem(4, new ItemStack(Items.PISTON, count));
        for (int slot : new int[] {6, 8}) table.setItem(slot, new ItemStack(ThaumcraftMasonry.ARCANE_STONE_BRICKS.get(), count));
        table.setItem(7, new ItemStack(ThaumcraftContent.MATERIALS.get("nitor").get(), count));
    }

    private static void wrong(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu, int slot, List<Item> wrong, List<ItemStack> correct) {
        for (Item item : wrong) {
            table.setItem(slot, new ItemStack(item, 2));
            refuse(f, table, menu, "literal original ingredient is NOT substitute " + item + " at " + slot);
        }
        table.setItem(slot, correct.get(slot).copy());
    }

    private static void refuse(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu, String why) {
        menu.broadcastChanges();
        var before = table.saveWithoutMetadata(f.level.registryAccess());
        menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), why);
    }

    static ItemStack wand(String rod, String cap, int amount) {
        var vis = new LinkedHashMap<String, Integer>();
        for (String tag : WandData.PRIMALS) vis.put(tag, amount);
        var stack = new ItemStack(ThaumcraftContent.WAND.get());
        stack.set(ThaumcraftContent.WAND_DATA.get(), new WandData(rod, cap, vis));
        return stack;
    }

    /** Preflight BEFORE the retained shared shadow fixture reads any cell. The
     * extra horizontal margin also covers ordinary support-power queries. */
    static void requireLoadedFootprint(GameTestHelper test) {
        BlockPos base = test.absolutePos(new BlockPos(3, 3, 3));
        var level = test.getLevel();
        test.assertTrue(!level.isOutsideBuildHeight(base.below(2)) && !level.isOutsideBuildHeight(base.above(3)),
                "owned native fixture footprint is inside build height");
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            test.assertTrue(level.hasChunkAt(base.offset(x, 0, z)),
                    "owned shadow and power-query footprint must ALREADY be loaded; never force chunks for this check");
        }
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        var data = new CompoundTag();
        data.putString("Unrelated", name);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    private static void checkWand(Fixture f, ItemStack actual, ItemStack original, int remaining, String why) {
        var expected = original.copy();
        var data = ItemWandCasting.data(expected);
        for (String tag : PAID_PRIMALS) data = data.withAmount(tag, remaining);
        expected.set(ThaumcraftContent.WAND_DATA.get(), data);
        f.check(ItemStack.matches(actual, expected), why + "; ALL other primals/name/unrelated components conserved");
    }

    private static ItemStack removeInventory(Fixture f, Item item, int count) {
        for (int slot = 0; slot < 36; slot++) {
            if (f.player.getInventory().getItem(slot).is(item) && f.player.getInventory().getItem(slot).getCount() >= count)
                return f.player.getInventory().removeItem(slot, count);
        }
        throw new AssertionError("Missing actual owned output/input " + item + " x" + count);
    }

    private static void hand(Fixture f, ItemStack stack) {
        for (int slot = 0; slot < 9; slot++) if (f.player.getInventory().getItem(slot).isEmpty()) {
            f.player.getInventory().selected = slot;
            f.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            return;
        }
        throw new AssertionError("No empty native hotbar slot; never overwrite another paid output");
    }
}
