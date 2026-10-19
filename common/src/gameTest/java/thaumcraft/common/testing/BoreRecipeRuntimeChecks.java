package thaumcraft.common.testing;

import java.util.ArrayList;
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
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Isolated SERVER raw recipe/payer fixtures, NOT client acquisition evidence. */
public final class BoreRecipeRuntimeChecks {
    private BoreRecipeRuntimeChecks() {}

    public static void basePaymentAndTopPredicates(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open(); f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get()); f.aim(Vec3.atCenterOf(f.base));
            f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench,
                    "actual connected native arcane menu");
            var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
            var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
            f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
            f.player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get()));
            f.player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ThaumcraftWearables.ROBE_LEGGINGS.get()));
            f.player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ThaumcraftWearables.ROBE_BOOTS.get()));
            var payer = new ItemStack(ThaumcraftContent.WAND.get());
            var amounts = new LinkedHashMap<String, Integer>();
            WandData.PRIMALS.forEach(tag -> amounts.put(tag, 6000));
            payer.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "gold", amounts));
            payer.set(DataComponents.CUSTOM_NAME, Component.literal("finite-Bore-recipe-test-payer"));
            table.setItem(10, payer); ItemStack beforePayer = payer.copy();
            var base = (ArcaneShapedRecipe) f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("arcane/arcane_bore_base")).orElseThrow().value();
            f.check(base.research().equals("ARCANEBORE") && base.pattern().equals(List.of("WIW", "IDI", "WIW"))
                    && base.vis().equals(Map.of("aer", 10, "ordo", 10)) && base.result().is(ThaumcraftArcaneBore.ARCANE_BORE_BASE_ITEM.get())
                    && base.result().getCount() == 1, "original base gate/ordered pattern/two10-vis costs/output");
            Map<String, Item> keys = Map.of("W", ThaumcraftNaturals.GREATWOOD_PLANKS.get().asItem(), "I", Items.IRON_INGOT, "D", Items.DISPENSER);
            f.check(base.key().keySet().equals(keys.keySet()), "only original three literal base ingredients");
            keys.forEach((symbol, item) -> f.check(base.key().get(symbol).getItems().length == 1
                    && base.key().get(symbol).getItems()[0].is(item), "literal ingredient: " + symbol));
            for (int slot = 0; slot < 9; slot++) table.setItem(slot,
                    base.key().get(String.valueOf(base.pattern().get(slot / 3).charAt(slot % 3))).getItems()[0].copyWithCount(1));
            KnowledgeAccess.get(f.player).removeResearch("ARCANEBORE");
            refuse(f, table, menu, "missing concealed PRIMARY cannot spend base input or vis");
            KnowledgeAccess.get(f.player).addResearch("ARCANEBORE");
            for (Item wrong : List.of(Items.DROPPER, Items.CHEST)) {
                table.setItem(4, new ItemStack(wrong)); refuse(f, table, menu, "not a dispenser: " + wrong);
            }
            table.setItem(4, new ItemStack(Items.DISPENSER));
            for (String tag : List.of("aer", "ordo")) {
                var shortPayer = beforePayer.copy(); shortPayer.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(shortPayer).withAmount(tag, 899));
                table.setItem(10, shortPayer); refuse(f, table, menu, "one CV short refuses whole base: " + tag);
            }
            table.setItem(10, beforePayer.copy()); menu.broadcastChanges();
            f.check(menu.craftable() && menu.requiredVis("aer") == 900 && menu.requiredVis("ordo") == 900,
                    "actual worn10% and gold cap produce two900-CV prices");
            var snapshot = table.saveWithoutMetadata(f.level.registryAccess());
            for (int i = 0; i < 6; i++) f.check(menu.getSlot(0).getItem().is(ThaumcraftArcaneBore.ARCANE_BORE_BASE_ITEM.get()), "repeat nonpaying preview");
            f.check(snapshot.equals(table.saveWithoutMetadata(f.level.registryAccess())), "preview changes no stock");
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            var earned = menu.getCarried(); menu.setCarried(ItemStack.EMPTY);
            var expected = beforePayer.copy(); var expectedData = ItemWandCasting.data(expected);
            for (String tag : List.of("aer", "ordo")) expectedData = expectedData.withAmount(tag, 5100);
            expected.set(ThaumcraftContent.WAND_DATA.get(), expectedData);
            f.check(earned.is(ThaumcraftArcaneBore.ARCANE_BORE_BASE_ITEM.get()) && earned.getCount() == 1
                    && earned.getComponentsPatch().isEmpty() && ItemStack.matches(table.getItem(10), expected)
                    && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty),
                    "real native result extraction consumes9 inputs/two900CV exactly, no state-bearing base");
            f.check(menu.getSlot(0).remove(1).isEmpty() && f.player.getInventory().add(earned), "retain actual paid output, cannot replay stale preview");

            var top = (InfusionStackRecipe) f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("infusion/arcane_bore")).orElseThrow().value();
            var costs = new LinkedHashMap<String, Integer>();
            costs.put("potentia", 16); costs.put("perfodio", 32); costs.put("machina", 32); costs.put("vacuos", 16); costs.put("motus", 16);
            f.check(top.research().equals("ARCANEBORE") && top.instability() == 4 && top.aspects().equals(costs)
                    && new ArrayList<>(top.aspects().keySet()).equals(new ArrayList<>(costs.keySet()))
                    && top.result().is(ThaumcraftArcaneBore.ARCANE_BORE_ITEM.get()) && top.result().getCount() == 1,
                    "original112-essentia order/instability4/gate and real registered top output");
            List<Item> declared = List.of(ThaumcraftNaturals.GREATWOOD_PLANKS.get().asItem(), ThaumcraftNaturals.GREATWOOD_PLANKS.get().asItem(),
                    Items.GOLD_INGOT, Items.GOLD_INGOT, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL,
                    ThaumcraftContent.SHARDS.get("aer").get(), ThaumcraftContent.SHARDS.get("terra").get());
            f.check(top.input().getItems().length == 1 && top.input().getItems()[0].is(Items.PISTON) && top.components().size() == 8,
                    "literal piston and all8 separately occupied component pedestals");
            var components = new ArrayList<ItemStack>();
            for (int i = 0; i < declared.size(); i++) {
                f.check(top.components().get(i).getItems().length == 1 && top.components().get(i).getItems()[0].is(declared.get(i)), "original ordered component " + i);
                f.check(top.componentDamage(i).equals(i == 4 || i == 5 ? java.util.Optional.of(0) : java.util.Optional.empty()), "pristine predicate only on diamond components");
                components.add(new ItemStack(declared.get(i)));
            }
            for (int slot : new int[]{4, 5}) {
                components.get(slot).set(DataComponents.CUSTOM_NAME, Component.literal("allowed-pristine-named-tool"));
                f.check(top.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON), components), f.level), "extra native components do not invalidate pristine tool");
                components.get(slot).setDamageValue(1);
                f.check(!top.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON), components), f.level), "actual worn resource recipe tool rejected: " + slot);
                components.get(slot).setDamageValue(0);
            }
            f.check(!top.matches(new InfusionRecipeInput(new ItemStack(Items.STICKY_PISTON), components), f.level), "sticky piston cannot replace center");
            var incomplete = new ArrayList<>(components); incomplete.removeLast();
            f.check(!top.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON), incomplete), f.level), "missing8th pedestal rejected");
        }
        test.succeed();
    }

    private static void refuse(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu, String why) {
        menu.broadcastChanges(); var before = table.saveWithoutMetadata(f.level.registryAccess());
        menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), why);
    }
}
