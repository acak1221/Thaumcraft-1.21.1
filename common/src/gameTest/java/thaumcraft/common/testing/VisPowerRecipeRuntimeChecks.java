package thaumcraft.common.testing;

import java.util.ArrayList;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.common.blocks.BlockVisRelay;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileNodeConverter;
import thaumcraft.common.tiles.TileVisRelay;

/** Original recipes199-201 / device placement and relay interactions. SERVER
 * INPUTS: ALREADY-CRAFTED Nitor2/bricks2/balanced-shard1, mundane raw ingredients,
 * researched knowledge, three independently funded greatwood/gold wands and worn
 * 10% gear. Acquisition of those ingredients/knowledge is NOT claimed. The actual
 * native menu EARNS the basic dependency, consumes it into a transducer, and earns
 * two relays. No finished new device or counterfeit discount is supplied. */
public final class VisPowerRecipeRuntimeChecks {
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private VisPowerRecipeRuntimeChecks() {}

    /** Suggested timeout240, own separate Fabric batch. */
    public static void recipesPlacementAndAttunement(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open(); recipes(f);
        }
        test.succeed();
    }

    private static void recipes(Fixture f) {
        f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get()); f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench,
                "real connected SURVIVAL actor opens actual native recipe/payment manager");
        var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
        var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
        equip(f);
        KnowledgeAccess.get(f.player).addResearch("NODESTABILIZER");
        clear(table);
        put(table, 1, Items.GOLD_INGOT); put(table, 3, Items.QUARTZ_BLOCK); put(table, 5, Items.QUARTZ_BLOCK); put(table, 4, Items.PISTON);
        put(table, 6, ThaumcraftMasonry.ARCANE_STONE_BRICKS.get().asItem()); put(table, 8, ThaumcraftMasonry.ARCANE_STONE_BRICKS.get().asItem());
        put(table, 7, ThaumcraftContent.MATERIALS.get("nitor").get());
        ItemStack basicPayer = named(wand("greatwood", "gold", 5000), "declared-basic-payer"); table.setItem(10, basicPayer);
        menu.broadcastChanges();
        f.check(menu.craftable() && List.of("aqua", "terra", "ordo").stream().allMatch(tag -> menu.requiredVis(tag) == 2880),
                "EARN dependency through native literal quartzBLOCK/NitorITEM/basic recipe; GOLD cap1F minus actual WORN10% costs2880 each");
        menu.clicked(0, 0, ClickType.PICKUP, f.player); ItemStack basic = menu.getCarried(); menu.setCarried(ItemStack.EMPTY);
        f.check(basic.is(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get()) && basic.getCount() == 1 && basic.getComponentsPatch().isEmpty()
                        && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty),
                "ONE genuinely paid basic stabilizer, exact seven ingredient positions consumed; not a granted dependency");
        paid(f, table.getItem(10), basicPayer, Map.of("aqua", 2880, "terra", 2880, "ordo", 2880));
        retainPayer(f, table);

        var holder = f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("arcane/node_transducer")).orElseThrow();
        f.check(holder.value() instanceof ArcaneShapedRecipe, "registered actual transducer holder");
        var transducer = (ArcaneShapedRecipe) holder.value();
        literalRecipe(f, transducer, "VISPOWER", List.of("RCR", "ISI", "RAR"), Map.of("aer", 32, "ignis", 32, "perditio", 32),
                ThaumcraftNodeDevices.NODE_TRANSDUCER_ITEM.get(), 1,
                Map.of("R", Items.REDSTONE_BLOCK, "C", Items.COMPARATOR, "I", Items.IRON_INGOT,
                        "S", ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get(), "A", ThaumcraftContent.MATERIALS.get("nitor").get()));
        clear(table);
        for (int slot : new int[] {0, 2, 6, 8}) put(table, slot, Items.REDSTONE_BLOCK);
        put(table, 1, Items.COMPARATOR); put(table, 3, Items.IRON_INGOT); put(table, 5, Items.IRON_INGOT);
        table.setItem(4, named(basic, "ACTUALLY-paid-basic-dependency")); put(table, 7, ThaumcraftContent.MATERIALS.get("nitor").get());
        ItemStack transducerPayer = named(wand("greatwood", "gold", 5000), "declared-independent-transducer-payer"); table.setItem(10, transducerPayer);
        KnowledgeAccess.get(f.player).removeResearch("VISPOWER"); refuse(f, table, menu, "NODESTABILIZER does NOT substitute for locked VISPOWER");
        KnowledgeAccess.get(f.player).addResearch("VISPOWER");
        var correct = matrixCopy(table);
        for (int slot = 0; slot < 9; slot++) {
            table.setItem(slot, ItemStack.EMPTY); refuse(f, table, menu, "EACH of nine literal transducer cells is required: " + slot); table.setItem(slot, correct.get(slot).copy());
        }
        for (int slot : new int[] {0, 2, 6, 8}) wrong(f, table, menu, slot, Items.REDSTONE, correct);
        wrong(f, table, menu, 1, Items.REPEATER, correct); wrong(f, table, menu, 3, Items.IRON_BLOCK, correct);
        wrong(f, table, menu, 4, ThaumcraftMasonry.ARCANE_STONE_BRICKS.get().asItem(), correct); wrong(f, table, menu, 7, Items.GLOWSTONE_DUST, correct);
        var armor = new LinkedHashMap<EquipmentSlot, ItemStack>();
        for (var slot : ARMOR) { armor.put(slot, f.player.getItemBySlot(slot)); f.player.setItemSlot(slot, ItemStack.EMPTY); }
        menu.broadcastChanges();
        f.check(List.of("aer", "ignis", "perditio").stream().allMatch(tag -> menu.requiredVis(tag) == 3200),
                "bare GOLD costs3200 EACH, NOT intrinsic.9; separate wand materials remain authoritative");
        table.setItem(10, wand("wood", "iron", 2500)); menu.broadcastChanges();
        f.check(!menu.craftable() && menu.requiredVis("aer") == 3520 && ItemWandCasting.getMaxVis(table.getItem(10)) == 2500,
                "real wood25 cannot pay iron35.2; no above-capacity counterfeit funding");
        refuse(f, table, menu, "wood capacity refuses WHOLE transducer");
        armor.forEach(f.player::setItemSlot); table.setItem(10, transducerPayer); menu.broadcastChanges();
        f.check(List.of("aer", "ignis", "perditio").stream().allMatch(tag -> menu.requiredVis(tag) == 2880), "actual worn10% gives2880 per transducer primal");
        table.setItem(10, wand("greatwood", "thaumium", 5000)); menu.broadcastChanges();
        f.check(List.of("aer", "ignis", "perditio").stream().allMatch(tag -> menu.requiredVis(tag) == 2559),
                "independent THAUMIUM cap.9F minus worn10% preserves actual native float2559, not GOLD1F or rounded2560");
        for (String tag : List.of("aer", "ignis", "perditio")) {
            var shortPayer = transducerPayer.copy(); shortPayer.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(shortPayer).withAmount(tag, 2879));
            table.setItem(10, shortPayer); refuse(f, table, menu, "ONE raw centivis short in EACH primal refuses all nine cells: " + tag);
        }
        table.setItem(10, transducerPayer); menu.broadcastChanges();
        var before = table.saveWithoutMetadata(f.level.registryAccess());
        for (int i = 0; i < 16; i++) f.check(menu.getSlot(0).getItem().is(ThaumcraftNodeDevices.NODE_TRANSDUCER_ITEM.get()), "native repeated preview identifies output but does not extract it");
        f.check(before.equals(table.saveWithoutMetadata(f.level.registryAccess())), "preview never charges wand/ingredient/dependency or grants any finished output");
        KnowledgeAccess.get(f.player).removeResearch("VISPOWER");
        f.check(menu.getSlot(0).remove(1).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), "stale extraction rechecks actual research before spending");
        KnowledgeAccess.get(f.player).addResearch("VISPOWER"); menu.broadcastChanges();
        var stalePoor = transducerPayer.copy(); stalePoor.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(stalePoor).withAmount("aer", 2879)); table.setItem(10, stalePoor);
        before = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.getSlot(0).remove(1).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), "stale extraction rechecks actual current payer; no paid-basic loss/refund");
        table.setItem(10, transducerPayer); fullDestination(f, table, menu);
        menu.clicked(0, 0, ClickType.PICKUP, f.player); ItemStack earnedTransducer = menu.getCarried(); menu.setCarried(ItemStack.EMPTY);
        f.check(earnedTransducer.is(ThaumcraftNodeDevices.NODE_TRANSDUCER_ITEM.get()) && earnedTransducer.getCount() == 1 && earnedTransducer.getComponentsPatch().isEmpty()
                        && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty)
                        && f.player.getInventory().countItem(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get()) == 0,
                "actual payment consumes ALL9 inputs including SAME earned/named basic; no item refund/component leak/unearned second output");
        paid(f, table.getItem(10), transducerPayer, Map.of("aer", 2880, "ignis", 2880, "perditio", 2880)); retainPayer(f, table);
        f.check(menu.getSlot(0).remove(1).isEmpty(), "empty transducer matrix cannot replay stale paid output");

        var relayHolder = f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("arcane/vis_relay")).orElseThrow();
        f.check(relayHolder.value() instanceof ArcaneShapedRecipe, "actual native relay recipe manager holder");
        var relay = (ArcaneShapedRecipe) relayHolder.value();
        literalRecipe(f, relay, "VISPOWER", List.of(" I ", "ISI", " I "), Map.of("ignis", 8, "ordo", 8),
                ThaumcraftNodeDevices.VIS_RELAY_ITEM.get(), 2, Map.of("I", Items.IRON_INGOT, "S", ThaumcraftContent.SHARDS.get("balanced").get()));
        clear(table); for (int slot : new int[] {1, 3, 5, 7}) put(table, slot, Items.IRON_INGOT); put(table, 4, ThaumcraftContent.SHARDS.get("balanced").get());
        ItemStack relayPayer = named(wand("greatwood", "gold", 5000), "declared-independent-relay-payer"); table.setItem(10, relayPayer);
        KnowledgeAccess.get(f.player).removeResearch("VISPOWER"); refuse(f, table, menu, "relay independently refuses locked VISPOWER despite completed NODESTABILIZER");
        KnowledgeAccess.get(f.player).addResearch("VISPOWER");
        correct = matrixCopy(table);
        for (int slot : new int[] {0, 2, 6, 8}) { put(table, slot, Items.STICK); refuse(f, table, menu, "ALL four blank relay corners are literal: " + slot); table.setItem(slot, ItemStack.EMPTY); }
        for (int slot : new int[] {1, 3, 4, 5, 7}) { table.setItem(slot, ItemStack.EMPTY); refuse(f, table, menu, "each of five relay cells required" + slot); table.setItem(slot, correct.get(slot).copy()); }
        for (String primal : WandData.PRIMALS) wrong(f, table, menu, 4, ThaumcraftContent.SHARDS.get(primal).get(), correct);
        wrong(f, table, menu, 4, ThaumcraftContent.CRYSTAL_ESSENCE.get(), correct); wrong(f, table, menu, 1, Items.IRON_NUGGET, correct);
        for (var slot : ARMOR) f.player.setItemSlot(slot, ItemStack.EMPTY);
        menu.broadcastChanges();
        f.check(menu.requiredVis("ignis") == 800 && menu.requiredVis("ordo") == 800, "naked GOLD1F relay price is literal800 EACH");
        armor.forEach(f.player::setItemSlot);
        menu.broadcastChanges(); f.check(menu.craftable() && menu.requiredVis("ignis") == 720 && menu.requiredVis("ordo") == 720,
                "literal relay outputTWO costs whole8/8 -> actual WORN10%720 each, no per-item double bill");
        fullDestination(f, table, menu); menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        f.check(f.player.getInventory().countItem(ThaumcraftNodeDevices.VIS_RELAY_ITEM.get()) == 2
                        && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty) && menu.quickMoveStack(f.player, 0).isEmpty(),
                "actual native QUICK_MOVE pays exactly four iron/balanced1 for TWO relays, no stale replay");
        paid(f, table.getItem(10), relayPayer, Map.of("ignis", 720, "ordo", 720)); retainPayer(f, table);
        f.player.closeContainer();
        for (var slot : ARMOR) f.player.setItemSlot(slot, ItemStack.EMPTY);
        placement(f, earnedTransducer, remove(f, ThaumcraftNodeDevices.VIS_RELAY_ITEM.get(), 1));
    }

    private static void placement(Fixture f, ItemStack transducer, ItemStack relayItem) {
        for (Direction side : Direction.values()) {
            BlockPos support = f.base.east(2), pos = support.relative(side);
            f.put(support, Blocks.STONE); f.put(pos, Blocks.AIR); hand(f, transducer); f.aim(Vec3.atCenterOf(support));
            f.check(f.use(support, side, InteractionHand.MAIN_HAND).consumesAction() && f.player.getMainHandItem().isEmpty(), "actual earned ordinary transducer BlockItem spends1 on face" + side);
            var state = f.level.getBlockState(pos);
            f.check(f.level.getBlockEntity(pos) instanceof TileNodeConverter && state.getValues().isEmpty()
                            && state.getShape(f.level, pos, CollisionContext.empty()).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                            && state.getCollisionShape(f.level, pos, CollisionContext.of(f.player)).bounds().equals(new AABB(0, 0, 0, 1, 1, 1)),
                    "actual transducer FULLCUBE outline AND collision, no invented clicked-facing property");
            var menu = f.player.containerMenu; f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); f.aim(Vec3.atCenterOf(pos)); f.use(pos);
            f.check(f.player.containerMenu == menu, "transducer has no invented GUI");
            f.check(f.level.destroyBlock(pos, true, f.player), "native break of actual paid transducer"); transducer = recover(f, ThaumcraftNodeDevices.NODE_TRANSDUCER_ITEM.get());
            f.put(support, Blocks.AIR);

            f.put(support, Blocks.STONE); f.put(pos, Blocks.AIR); hand(f, relayItem); f.aim(Vec3.atCenterOf(support));
            f.check(f.use(support, side, InteractionHand.MAIN_HAND).consumesAction() && f.player.getMainHandItem().isEmpty(), "actual earned relay BlockItem pays1 on face" + side);
            state = f.level.getBlockState(pos); var relay = (TileVisRelay) f.level.getBlockEntity(pos);
            AABB expected = relayBox(side);
            f.check(state.getValue(BlockVisRelay.FACING) == side && relay.getOrientation() == side && relay.getAttunement() == -1
                            && state.getShape(f.level, pos, CollisionContext.empty()).bounds().equals(expected)
                            && state.getCollisionShape(f.level, pos, CollisionContext.of(f.player)).bounds().equals(expected),
                    "relay stores clicked face DIRECTLY, half-cell axial collision against opposite support, transverse5/16..11/16");
            attunement(f, relay);
            f.put(support, Blocks.WATER);
            f.check(relay.getBlockState().canSurvive(f.level, pos), "native relay support is NON-AIR only; WATER/no-solid-face remains valid");
            relay.dropIfUnsupported(); f.check(f.level.getBlockEntity(pos) == relay && f.pendingRows().isEmpty(), "nonAIR support cannot synthesize a drop");
            f.put(support, Blocks.AIR); relay.dropIfUnsupported();
            f.check(f.level.getBlockState(pos).isAir() && NodeTransductionRuntimeChecks.existing(f, pos) != relay && f.pendingRows().size() == 1,
                    "actual support-AIR action retires ONLY this relay and enqueues ONE plain durable drop");
            relay.dropIfUnsupported(); f.check(f.pendingRows().size() == 1, "retired support-drop callback cannot duplicate output");
            f.flushFresh(); relayItem = recover(f, ThaumcraftNodeDevices.VIS_RELAY_ITEM.get());
        }
        f.check(f.player.getInventory().add(transducer) && f.player.getInventory().add(relayItem)
                        && f.player.getInventory().countItem(ThaumcraftNodeDevices.NODE_TRANSDUCER_ITEM.get()) == 1
                        && f.player.getInventory().countItem(ThaumcraftNodeDevices.VIS_RELAY_ITEM.get()) == 2
                        && f.player.getInventory().countItem(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get()) == 0,
                "six genuine placement/drop cycles conserve EXACT earned transducer1/relays2 and consumed basic0");
    }

    private static void attunement(Fixture f, TileVisRelay relay) {
        ItemStack wand = named(wand("wood", "iron", 0), "EMPTY-real-wand-no-vis-fee"); hand(f, wand); f.aim(Vec3.atCenterOf(relay.getBlockPos()));
        for (int next : new int[] {0, 1, 2, 3, 4, 5, -1}) {
            var before = wand.copy(); f.player.setShiftKeyDown(next % 2 == 0);
            f.check(f.use(relay.getBlockPos()).consumesAction() && relay.getAttunement() == next
                            && f.player.getMainHandItem() == wand && ItemStack.matches(wand, before),
                    "real wand cycle -1..5..-1 costs NO vis/item/components, INCLUDING sneaking dispatch");
        }
        f.player.setShiftKeyDown(false);
        for (String tag : List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio")) {
            int expected = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio").indexOf(tag);
            VisNetRuntimeChecks.attune(f, relay, tag); f.check(relay.getAttunement() == expected, "native shard sets original color index" + expected);
            VisNetRuntimeChecks.attune(f, relay, tag); f.check(relay.getAttunement() == -1, "same shard toggles existing color OFF without consumption");
        }
        VisNetRuntimeChecks.attune(f, relay, "ignis"); VisNetRuntimeChecks.attune(f, relay, "balanced");
        f.check(relay.getAttunement() == -1, "native balanced shard clears color, not a seventh attunement");
        // A balanced shard would leave -1 even if the sneak guard were broken.
        // IGNIS would visibly set1, so this is a real refusal assertion.
        f.player.setItemInHand(InteractionHand.MAIN_HAND,
                named(new ItemStack(ThaumcraftContent.SHARDS.get("ignis").get(), 3), "sneaking-IGNIS-must-not-attune"));
        f.player.setShiftKeyDown(true); var held = f.player.getMainHandItem(); var before = held.copy(); f.use(relay.getBlockPos());
        f.check(relay.getAttunement() == -1 && f.player.getMainHandItem() == held && ItemStack.matches(held, before),
                "sneaking primal shard cannot attune untyped relay to1, consume input or change ANY held components");
        f.player.setShiftKeyDown(false); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static void literalRecipe(Fixture f, ArcaneShapedRecipe recipe, String research, List<String> pattern, Map<String, Integer> vis, Item result, int count, Map<String, Item> keys) {
        f.check(recipe.research().equals(research) && recipe.pattern().equals(pattern) && recipe.vis().equals(vis)
                        && recipe.result().is(result) && recipe.result().getCount() == count && recipe.key().keySet().equals(keys.keySet()),
                "actual manager exact original research/literal pattern/whole vis/output count");
        keys.forEach((symbol, item) -> f.check(recipe.key().get(symbol).getItems().length == 1 && recipe.key().get(symbol).getItems()[0].is(item),
                "literal item only, NOT broadened tags/material/metadata substitutes: " + symbol));
    }
    private static void fullDestination(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu) {
        menu.broadcastChanges(); var old = new ArrayList<ItemStack>();
        for (int i = 0; i < 36; i++) { old.add(f.player.getInventory().getItem(i)); f.player.getInventory().setItem(i, new ItemStack(Items.STONE, 64)); }
        try {
            var before = table.saveWithoutMetadata(f.level.registryAccess());
            f.check(menu.quickMoveStack(f.player, 0).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), "full REAL quick-move destination refuses before ingredients/vis are spent");
        } finally { for (int i = 0; i < 36; i++) f.player.getInventory().setItem(i, old.get(i)); }
    }
    private static void equip(Fixture f) {
        f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
        f.player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get()));
        f.player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ThaumcraftWearables.ROBE_LEGGINGS.get()));
        f.player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ThaumcraftWearables.ROBE_BOOTS.get()));
    }
    private static AABB relayBox(Direction side) {
        double a = 5 / 16D, b = 11 / 16D;
        return switch (side) { case UP -> new AABB(a, 0, a, b, .5, b); case DOWN -> new AABB(a, .5, a, b, 1, b);
            case EAST -> new AABB(0, a, a, .5, b, b); case WEST -> new AABB(.5, a, a, 1, b, b);
            case SOUTH -> new AABB(a, a, 0, b, b, .5); case NORTH -> new AABB(a, a, .5, b, b, 1); };
    }
    private static void clear(TileArcaneWorkbench table) { for (int slot = 0; slot < 9; slot++) table.setItem(slot, ItemStack.EMPTY); }
    private static void put(TileArcaneWorkbench table, int slot, Item item) { table.setItem(slot, named(new ItemStack(item), "raw-cell" + slot)); }
    private static List<ItemStack> matrixCopy(TileArcaneWorkbench table) { return ArcaneRecipeInput.snapshot(table).items().stream().map(ItemStack::copy).toList(); }
    private static void wrong(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu, int slot, Item wrong, List<ItemStack> correct) { table.setItem(slot, new ItemStack(wrong)); refuse(f, table, menu, "literal wrong item at" + slot + ":" + wrong); table.setItem(slot, correct.get(slot).copy()); }
    private static void refuse(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu, String why) { menu.broadcastChanges(); var before = table.saveWithoutMetadata(f.level.registryAccess()); menu.clicked(0, 0, ClickType.PICKUP, f.player); f.check(menu.getCarried().isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), why); }
    private static ItemStack wand(String rod, String cap, int amount) { var amounts = new LinkedHashMap<String, Integer>(); for (String tag : WandData.PRIMALS) amounts.put(tag, amount); var wand = new ItemStack(ThaumcraftContent.WAND.get()); wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData(rod, cap, amounts)); return wand; }
    private static ItemStack named(ItemStack stack, String name) { stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)); var tag = new CompoundTag(); tag.putString("Unrelated", name); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); return stack; }
    private static void paid(Fixture f, ItemStack actual, ItemStack original, Map<String, Integer> costs) { var expected = original.copy(); var data = ItemWandCasting.data(expected); for (var entry : costs.entrySet()) data = data.withAmount(entry.getKey(), 5000 - entry.getValue()); expected.set(ThaumcraftContent.WAND_DATA.get(), data); f.check(ItemStack.matches(actual, expected), "actual native payer exactly debited; ALL unrelated primals/name/components preserved, no refund"); }
    private static void retainPayer(Fixture f, TileArcaneWorkbench table) { f.check(f.player.getInventory().add(table.removeItemNoUpdate(10)), "retain independently debited payer in owned inventory; never silently refill it"); }
    private static ItemStack remove(Fixture f, Item item, int count) { for (int i = 0; i < 36; i++) if (f.player.getInventory().getItem(i).is(item) && f.player.getInventory().getItem(i).getCount() >= count) return f.player.getInventory().removeItem(i, count); throw new AssertionError("Missing genuinely earned " + item); }
    private static void hand(Fixture f, ItemStack stack) { for (int i = 0; i < 9; i++) if (f.player.getInventory().getItem(i).isEmpty()) { f.player.getInventory().selected = i; f.player.setItemInHand(InteractionHand.MAIN_HAND, stack); return; } throw new AssertionError("No free owned hand slot"); }
    private static ItemStack recover(Fixture f, Item item) { var drops = f.drops().stream().filter(drop -> drop.getItem().is(item)).toList(); f.check(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), new ItemStack(item)), "ONE plain OWN paid device drop, no saved power/attunement/components"); var stack = drops.getFirst().getItem().copy(); drops.getFirst().discard(); return stack; }
}
