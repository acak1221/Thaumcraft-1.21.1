package thaumcraft.common.testing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Isolated SERVER inputs include shards/stone/slab/table and one funded payer.
 * Actual native result extraction earns the charm, then consumes THAT charm to
 * earn the manipulator. Test setup is not client/survival acquisition evidence. */
public final class FocalRecipeRuntimeChecks {
    private FocalRecipeRuntimeChecks() {}

    public static void realCharmAndManipulatorPayments(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
            f.aim(Vec3.atCenterOf(f.base));
            f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench,
                    "connected survival actor uses actual native arcane menu");
            var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
            var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
            f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
            f.player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get()));
            f.player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ThaumcraftWearables.ROBE_LEGGINGS.get()));
            f.player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ThaumcraftWearables.ROBE_BOOTS.get()));
            ItemStack payer = payer();
            // Frugal must affect casting, NEVER this all-primal recipe quote.
            var focus = new ItemStack(ThaumcraftFoci.EXCAVATION_FOCUS.get());
            var basic = (ItemFocusBasic) focus.getItem();
            f.check(basic.applyUpgrade(focus, FocusUpgradeType.get(1), 1), "declared isolated Frugal payer fixture");
            payer.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(focus));
            table.setItem(10, payer);
            ItemStack beforePayer = payer.copy();

            var charm = recipe(f, "primal_charm", "BASICARTIFACE", List.of("123", "ISI", "456"), 25,
                    ThaumcraftContent.MATERIALS.get("primal_charm").get(), Map.of(
                            "1", shard("aer"), "2", shard("ignis"), "3", shard("aqua"),
                            "4", shard("terra"), "5", shard("ordo"), "6", shard("perditio"),
                            "I", Items.GOLD_INGOT, "S", shard("balanced")));
            fill(table, charm);
            KnowledgeAccess.get(f.player).removeResearch("BASICARTIFACE");
            refuse(f, table, menu, "missing real charm research cannot spend any ingredient or vis");
            KnowledgeAccess.get(f.player).addResearch("BASICARTIFACE");
            for (String wrong : WandData.PRIMALS) {
                ItemStack saved = table.getItem(4);
                table.setItem(4, new ItemStack(shard(wrong)));
                refuse(f, table, menu, "each individual primal is NOT the required balanced center");
                table.setItem(4, saved);
            }
            menu.broadcastChanges();
            f.check(menu.craftable() && WandData.PRIMALS.stream().allMatch(tag -> menu.requiredVis(tag) == 2250),
                    "GOLD1F minus actual worn10% is2250 EACH; nested Frugal adds NO recipe discount");
            ItemStack earnedCharm = extract(f, table, menu, ThaumcraftContent.MATERIALS.get("primal_charm").get());
            f.check(earnedCharm.getMaxStackSize() == 1, "original charm stack limit1, no full passive-callback parity claim");
            debitMatches(f, table.getItem(10), beforePayer, 2250);

            var manipulator = recipe(f, "focal_manipulator", "FOCALMANIPULATION", List.of("IQI", "SPS", "GTG"), 32,
                    ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_ITEM.get(), Map.of(
                            "I", Items.IRON_INGOT, "Q", ThaumcraftMasonry.SLAB.get().asItem(),
                            "S", ThaumcraftMasonry.ARCANE_STONE.get().asItem(), "P", earnedCharm.getItem(),
                            "G", Items.GOLD_INGOT, "T", ThaumcraftContent.TABLE_ITEM.get()));
            // Never discard the genuinely paid dependency and insert a fabricated
            // output. Its actual stack, including this name, is consumed once.
            earnedCharm.set(DataComponents.CUSTOM_NAME, Component.literal("actual-paid-charm"));
            fill(table, manipulator, earnedCharm);
            KnowledgeAccess.get(f.player).removeResearch("FOCALMANIPULATION");
            refuse(f, table, menu, "BASICARTIFACE/VISPOWER do not replace FOCALMANIPULATION");
            KnowledgeAccess.get(f.player).addResearch("FOCALMANIPULATION");
            var correct = ArcaneRecipeInput.snapshot(table).items().stream().map(ItemStack::copy).toList();
            for (int slot = 0; slot < 9; slot++) {
                table.setItem(slot, ItemStack.EMPTY);
                refuse(f, table, menu, "all9 original machine cells required, including the paid charm:" + slot);
                table.setItem(slot, correct.get(slot).copy());
            }
            for (int slot : new int[] {3, 5}) {
                table.setItem(slot, new ItemStack(ThaumcraftMasonry.ARCANE_STONE_BRICKS.get()));
                refuse(f, table, menu, "bricks cannot replace literal arcane stone");
                table.setItem(slot, correct.get(slot).copy());
            }
            ItemStack afterCharm = table.getItem(10).copy();
            for (String tag : WandData.PRIMALS) {
                ItemStack insufficient = afterCharm.copy();
                insufficient.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(insufficient).withAmount(tag, 2879));
                table.setItem(10, insufficient);
                refuse(f, table, menu, "one centivis short refuses whole machine for EACH primal:" + tag);
            }
            table.setItem(10, afterCharm);
            menu.broadcastChanges();
            f.check(menu.craftable() && WandData.PRIMALS.stream().allMatch(tag -> menu.requiredVis(tag) == 2880),
                    "all6 literal32 costs become2880, never discounted by payer Frugal");
            fullInventoryRefuses(f, table, menu);
            var earnedMachine = extract(f, table, menu, ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_ITEM.get());
            debitMatches(f, table.getItem(10), beforePayer, 5130);
            f.check(menu.getSlot(0).remove(1).isEmpty(), "stale output cannot extract another charm or machine");
            f.check(f.player.getInventory().add(earnedMachine) && f.player.getInventory().add(table.removeItemNoUpdate(10)),
                    "retain actual new machine and precisely debited payer without refill/refund");
            f.check(f.player.getInventory().countItem(earnedCharm.getItem()) == 0,
                    "exactly one paid charm consumed, not refunded or duplicated into inventory");
        }
        test.succeed();
    }

    private static ArcaneShapedRecipe recipe(Fixture f, String id, String research, List<String> pattern, int cost, Item output, Map<String, Item> keys) {
        var holder = f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("arcane/" + id)).orElseThrow();
        f.check(holder.value() instanceof ArcaneShapedRecipe, "real RecipeManager arcane holder:" + id);
        var recipe = (ArcaneShapedRecipe) holder.value();
        var costs = new LinkedHashMap<String, Integer>();
        for (String tag : WandData.PRIMALS) costs.put(tag, cost);
        f.check(recipe.research().equals(research) && recipe.pattern().equals(pattern) && recipe.vis().equals(costs)
                        && recipe.result().is(output) && recipe.result().getCount() == 1 && recipe.key().keySet().equals(keys.keySet()),
                "exact original research/pattern/cost/output, not a catalogue alias");
        keys.forEach((symbol, item) -> f.check(recipe.key().get(symbol).getItems().length == 1
                && recipe.key().get(symbol).getItems()[0].is(item), "literal singleton ingredient:" + symbol));
        return recipe;
    }
    private static void fill(TileArcaneWorkbench tile, ArcaneShapedRecipe recipe) {
        fill(tile, recipe, null);
    }
    private static void fill(TileArcaneWorkbench tile, ArcaneShapedRecipe recipe, ItemStack paidCenter) {
        for (int slot = 0; slot < 9; slot++) {
            if (slot == 4 && paidCenter != null) { tile.setItem(slot, paidCenter); continue; }
            String symbol = String.valueOf(recipe.pattern().get(slot / 3).charAt(slot % 3));
            tile.setItem(slot, recipe.key().get(symbol).getItems()[0].copyWithCount(1));
        }
    }
    private static ItemStack extract(Fixture f, TileArcaneWorkbench tile, ContainerArcaneWorkbench menu, Item output) {
        menu.broadcastChanges();
        var before = tile.saveWithoutMetadata(f.level.registryAccess());
        for (int i = 0; i < 8; i++) f.check(menu.getSlot(0).getItem().is(output), "preview is not an economic mutation");
        f.check(before.equals(tile.saveWithoutMetadata(f.level.registryAccess())), "repeated previews preserve physical inputs/payer");
        menu.clicked(0, 0, ClickType.PICKUP, f.player);
        ItemStack result = menu.getCarried(); menu.setCarried(ItemStack.EMPTY);
        f.check(result.is(output) && result.getCount() == 1 && result.getComponentsPatch().isEmpty()
                        && ArcaneRecipeInput.snapshot(tile).items().stream().allMatch(ItemStack::isEmpty),
                "native result extraction pays all9 cells once, does not copy charm name to new machine");
        return result;
    }
    private static void refuse(Fixture f, TileArcaneWorkbench tile, ContainerArcaneWorkbench menu, String why) {
        menu.broadcastChanges(); var before = tile.saveWithoutMetadata(f.level.registryAccess());
        menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().isEmpty() && before.equals(tile.saveWithoutMetadata(f.level.registryAccess())), why);
    }
    private static void fullInventoryRefuses(Fixture f, TileArcaneWorkbench tile, ContainerArcaneWorkbench menu) {
        var saved = new java.util.ArrayList<ItemStack>();
        for (int slot = 0; slot < 36; slot++) { saved.add(f.player.getInventory().getItem(slot)); f.player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64)); }
        try {
            var before = tile.saveWithoutMetadata(f.level.registryAccess());
            f.check(menu.quickMoveStack(f.player, 0).isEmpty() && before.equals(tile.saveWithoutMetadata(f.level.registryAccess())),
                    "full quick-move destination preserves paid charm and all vis");
        } finally { for (int slot = 0; slot < 36; slot++) f.player.getInventory().setItem(slot, saved.get(slot)); }
    }
    private static void debitMatches(Fixture f, ItemStack actual, ItemStack before, int total) {
        var expected = before.copy(); var data = ItemWandCasting.data(before);
        for (String tag : WandData.PRIMALS) data = data.withAmount(tag, data.amount(tag) - total);
        expected.set(ThaumcraftContent.WAND_DATA.get(), data);
        f.check(ItemStack.matches(actual, expected), "all6 exact debits, nested upgrade and unrelated native components preserved");
    }
    private static ItemStack payer() {
        var stack = new ItemStack(ThaumcraftContent.WAND.get());
        var amounts = new LinkedHashMap<String, Integer>();
        for (String tag : WandData.PRIMALS) amounts.put(tag, 6000);
        stack.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "gold", amounts));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("declared-finite-frugal-payer"));
        return stack;
    }
    private static Item shard(String tag) { return ThaumcraftContent.SHARDS.get(tag).get(); }
}
