package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;

import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Real registered recipes/menu/node; root supplies the loader wrapper. No recipe injection or fabricated upgraded output. */
public final class WandAssemblyRuntimeChecks {
    private WandAssemblyRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        var registries = level.registryAccess();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3)), nodePos = pos.east(2);
        var oldState = level.getBlockState(pos);
        var oldNodeState = level.getBlockState(nodePos);
        var oldTag = level.getBlockEntity(pos) == null ? null : level.getBlockEntity(pos).saveWithFullMetadata(registries);
        var oldNodeTag = level.getBlockEntity(nodePos) == null ? null : level.getBlockEntity(nodePos).saveWithFullMetadata(registries);
        var knowledge = KnowledgeAccess.get(player).copy();
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var oldPosition = player.position();
        var oldMode = player.gameMode.getGameModeForPlayer();
        int oldSelected = player.getInventory().selected;
        boolean oldSneaking = player.isShiftKeyDown();
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.removeAllEffects();
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            var known = KnowledgeAccess.get(player);
            known.addResearch("CAP_iron"); known.addResearch("ROD_wood");
            known.removeResearch("CAP_gold"); known.removeResearch("ROD_greatwood");
            known.removeResearch("NODETAPPER1"); known.removeResearch("NODETAPPER2"); known.removeResearch("NODEPRESERVE");
            level.setBlock(pos, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), Block.UPDATE_ALL);
            var tile = (TileArcaneWorkbench) level.getBlockEntity(pos);
            ItemStack charged = new ItemStack(ThaumcraftContent.WAND.get());
            for (String primal : WandData.PRIMALS) ItemWandCasting.addRealVis(charged, primal, 2500, true);
            tile.setItem(10, charged);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            test.assertTrue(player.containerMenu instanceof ContainerArcaneWorkbench, "actual block interaction opens the authoritative menu");
            var menu = (ContainerArcaneWorkbench) player.containerMenu;
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/wand_assembly")).orElseThrow();
            test.assertTrue(holder.value() instanceof ArcaneWandRecipe && holder.value().getSerializer() == ThaumcraftWands.ARCANE_WAND_SERIALIZER.get()
                    && holder.value().getType() == ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(), "dynamic wand recipe is loaded by the native RecipeManager");
            var recipe = (ArcaneWandRecipe) holder.value();
            test.assertTrue(recipe.getResultItem(registries).isEmpty() && recipe.vis().isEmpty(), "no invented global dynamic output/cost");
            var recipePacket = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                // Exercise the actual login packet with ALL bundled native
                // recipes. A server-only match never encodes unit stream codecs.
                ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(recipePacket,
                        new ClientboundUpdateRecipesPacket(level.getRecipeManager().getRecipes()));
                var received = ClientboundUpdateRecipesPacket.STREAM_CODEC.decode(recipePacket);
                test.assertTrue(received.getRecipes().stream().anyMatch(entry -> entry.id().equals(holder.id()) && entry.value() instanceof ArcaneWandRecipe),
                        "real update-recipes packet round-trips the dynamically assembled wand recipe");
            } finally { recipePacket.release(); }

            var log = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "greatwood_log"));
            test.assertTrue(log != Items.AIR, "real registered greatwood logs are available");
            tile.setItem(1, new ItemStack(log)); tile.setItem(3, new ItemStack(log));
            menu.broadcastChanges();
            test.assertTrue(menu.getSlot(0).getItem().isEmpty(), "original ROD_greatwood research gates the actual rod recipe");
            known.addResearch("ROD_greatwood"); menu.broadcastChanges();
            test.assertTrue(menu.craftable() && menu.requiredVis("perditio") == 330 && menu.requiredVis("aer") == 0, "greatwood rod costs original three entropy with iron surcharge");
            ItemStack rod = take(menu, player);
            test.assertTrue(rod.is(ThaumcraftWands.GREATWOOD_WAND_ROD.get()) && tile.getItem(1).isEmpty() && tile.getItem(3).isEmpty(), "rod is crafted from two actual logs");

            for (int slot : new int[] {0, 1, 2, 3, 5}) tile.setItem(slot, new ItemStack(Items.GOLD_NUGGET, 3));
            menu.broadcastChanges();
            test.assertTrue(menu.getSlot(0).getItem().isEmpty(), "CAP_gold gates its original five-nugget arcane recipe");
            known.addResearch("CAP_gold"); menu.broadcastChanges();
            test.assertTrue(menu.requiredVis("aer") == 330 && menu.requiredVis("ignis") == 330 && menu.requiredVis("ordo") == 330
                    && menu.requiredVis("perditio") == 0, "gold cap costs exactly three air/fire/order before the cap modifier");
            ItemStack caps = take(menu, player);
            caps.grow(take(menu, player).getCount()); caps.grow(take(menu, player).getCount());
            test.assertTrue(caps.is(ThaumcraftWands.GOLD_WAND_CAP.get()) && caps.getCount() == 3, "all three gold caps are real paid outputs");
            for (String[] pair : new String[][] {{"wood", "gold"}, {"greatwood", "iron"}}) {
                var fixture = ArcaneWandRecipe.displayInput(pair[0], pair[1]);
                var nativeMatch = level.getRecipeManager().getRecipeFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(), fixture, level);
                ItemStack variant = recipe.assemble(fixture, registries);
                test.assertTrue(nativeMatch.isPresent() && nativeMatch.get().id().equals(holder.id()) && recipe.hasResearch(fixture, player)
                        && ItemWandCasting.data(variant).rod().equals(pair[0]) && ItemWandCasting.data(variant).cap().equals(pair[1])
                        && recipe.vis(fixture).size() == 6 && recipe.vis(fixture).values().stream().allMatch(cost -> cost == 3),
                        "other bounded pair uses the same registered dynamic recipe: " + pair[0] + "/" + pair[1]);
            }
            ItemStack cap1 = caps.split(1), cap2 = caps;
            cap2.set(DataComponents.CUSTOM_NAME, Component.literal("asymmetric-cap2-remainder"));
            tile.setItem(6, cap1); tile.setItem(2, cap2); tile.setItem(4, rod);
            var input = ArcaneRecipeInput.snapshot(tile);
            test.assertTrue(recipe.matches(input, level) && recipe.vis(input).values().stream().allMatch(cost -> cost == 9), "fixed diagonal uses cap3 times rod3 for EACH primal");
            tile.setItem(0, new ItemStack(Items.STICK));
            test.assertTrue(!recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "all six otherwise empty cells are significant");
            tile.setItem(0, ItemStack.EMPTY);
            tile.setItem(6, cap2); tile.setItem(2, cap1);
            test.assertTrue(!recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "reversing named cap2 into cap1 preserves original target-NBT asymmetry");
            tile.setItem(6, cap1); tile.setItem(2, cap2);
            tile.setItem(0, tile.removeItemNoUpdate(2)); tile.setItem(8, tile.removeItemNoUpdate(6));
            test.assertTrue(!recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "unused original mirrored field does not accept the opposite diagonal");
            tile.setItem(2, tile.removeItemNoUpdate(0)); tile.setItem(6, tile.removeItemNoUpdate(8));
            for (String key : new String[] {"CAP_gold", "ROD_greatwood"}) {
                known.removeResearch(key); menu.broadcastChanges();
                test.assertTrue(menu.getSlot(0).getItem().isEmpty(), "assembly independently requires " + key);
                known.addResearch(key);
            }
            menu.broadcastChanges();
            test.assertTrue(menu.craftable() && menu.requiredVis("aer") == 990, "actual existing wood/iron crafting wand prices nine of every primal at 990 centivis");
            ItemStack paying = tile.removeItemNoUpdate(10);
            menu.broadcastChanges();
            test.assertTrue(!menu.getSlot(0).getItem().isEmpty() && !menu.craftable(), "researched preview may exist without an affordable inserted wand");
            var before = tile.saveWithoutMetadata(registries);
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(before.equals(tile.saveWithoutMetadata(registries)) && menu.getCarried().isEmpty(), "unfunded take changes neither materials nor output");
            ItemStack shortWand = paying.copy();
            shortWand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(shortWand).withAmount("aer", 989));
            tile.setItem(10, shortWand); menu.broadcastChanges(); before = tile.saveWithoutMetadata(registries);
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(!menu.craftable() && before.equals(tile.saveWithoutMetadata(registries)), "one short primal refuses the entire assembly atomically");
            tile.setItem(10, paying); menu.broadcastChanges();
            known.removeResearch("CAP_gold"); before = tile.saveWithoutMetadata(registries);
            test.assertTrue(menu.quickMoveStack(player, 0).isEmpty() && before.equals(tile.saveWithoutMetadata(registries)), "stale funded preview rechecks actual actor research on take");
            known.addResearch("CAP_gold"); menu.broadcastChanges();
            WandData payerBefore = ItemWandCasting.data(paying);
            ItemStack upgraded = take(menu, player);
            test.assertTrue(upgraded.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(upgraded).equals(new WandData("greatwood", "gold", Map.of()))
                    && upgraded.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), 0) == 9, "native assembly produces empty-vis greatwood/gold with source craft-cost metadata");
            for (String primal : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(tile.getItem(10), primal) == payerBefore.amount(primal) - 990, "one atomic assembly debit: " + primal);
            test.assertTrue(tile.getItem(4).isEmpty() && tile.getItem(6).isEmpty() && ItemStack.matches(cap2.copyWithCount(1), tile.getItem(2))
                    && tile.getItem(9).isEmpty(), "one material consumed per cell; named cap2 leftover retains components; no physical cached output");
            test.assertTrue(menu.quickMoveStack(player, 0).isEmpty() && menu.getSlot(1).mayPlace(upgraded), "replay cannot duplicate output; upgraded wand is accepted in wand slot10");
            test.assertTrue(ArcaneVis.price(Map.of("aer", 1), upgraded, player).get("aer") == 100, "gold uses original 1.0 modifier");
            player.addEffect(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 100, 0));
            test.assertTrue(ArcaneVis.price(Map.of("aer", 1), upgraded, player).get("aer") == 110, "gold does not bypass canonical vis-exhaustion surcharge");
            player.removeAllEffects();

            level.setBlock(nodePos, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), Block.UPDATE_ALL);
            var node = (TileNode) level.getBlockEntity(nodePos);
            node.setAspects(new AspectList().add(Aspect.AIR, 80));
            player.setItemInHand(InteractionHand.MAIN_HAND, upgraded);
            for (int drain = 0; drain < 50; drain++) node.drainIntoWand(player, upgraded);
            test.assertTrue(ItemWandCasting.getVis(upgraded, "aer") == 5000 && node.getAspects().getAmount(Aspect.AIR) == 30, "real node charging fills greatwood to 50, not wood's 25");
            node.drainIntoWand(player, upgraded);
            test.assertTrue(node.getAspects().getAmount(Aspect.AIR) == 30, "full native upgraded wand does not drain extra node vis");
            ArcaneVis.debit(upgraded, Map.of("aer", 100));
            known.addResearch("NODEPRESERVE"); node.setAspects(new AspectList().add(Aspect.AIR, 1));
            node.drainIntoWand(player, upgraded);
            test.assertTrue(ItemWandCasting.getVis(upgraded, "aer") == 4900 && node.getAspects().getAmount(Aspect.AIR) == 1, "greatwood/gold qualifies for original NODEPRESERVE");
            player.setShiftKeyDown(true); node.drainIntoWand(player, upgraded); player.setShiftKeyDown(false);
            test.assertTrue(ItemWandCasting.getVis(upgraded, "aer") == 5000 && node.getAspects().getAmount(Aspect.AIR) == 0, "sneaking overrides preservation exactly as original");
            tile.setItem(10, upgraded.copy()); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var saved = tile.saveWithFullMetadata(registries);
            level.removeBlockEntity(pos);
            var loaded = new TileArcaneWorkbench(pos, level.getBlockState(pos));
            loaded.loadWithComponents(saved, registries); level.setBlockEntity(loaded);
            test.assertTrue(ItemStack.matches(upgraded, loaded.getItem(10)) && ItemWandCasting.getVis(loaded.getItem(10), "aer") == 5000,
                    "actual workbench BE save/reload retains upgraded capacity, vis and craft metadata in slot10");

            var basic = ArcaneWandRecipe.displayInput("wood", "iron");
            for (int slot = 0; slot < 9; slot++) loaded.setItem(slot, basic.getItem(slot));
            var ordinary = ArcaneCrafting.find(player, loaded);
            test.assertTrue(recipe.assemble(basic, registries).isEmpty() && ordinary != null && !ordinary.arcane(), "ordinary wood/iron recipe retains priority over dynamic assembly");
            for (String key : new String[] {"CAP_iron", "ROD_wood"}) {
                known.removeResearch(key);
                test.assertTrue(!recipe.hasResearch(basic, player), "dynamic actor check also respects original auto-unlock key " + key);
                known.addResearch(key);
            }

            // Silverwood's core/caps here are declared intermediate INPUT
            // fixtures; the separate infusion story earns its core physically.
            // The wand output, exact payment and charged component are native.
            for (int slot = 0; slot < 9; slot++) loaded.setItem(slot, ItemStack.EMPTY);
            ItemStack sponsor = loaded.getItem(10);
            for (String primal : WandData.PRIMALS) ItemWandCasting.addRealVis(sponsor, primal, 5000, true);
            loaded.setItem(6, ThaumcraftWands.capItem("gold"));
            loaded.setItem(2, ThaumcraftWands.capItem("gold"));
            loaded.setItem(4, ThaumcraftWands.rodItem("silverwood"));
            player.closeContainer();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            var silverMenu = (ContainerArcaneWorkbench) player.containerMenu;
            known.removeResearch("ROD_silverwood");
            silverMenu.broadcastChanges();
            test.assertTrue(silverMenu.getSlot(0).getItem().isEmpty(), "Silverwood assembly requires its own original infusion research");
            known.addResearch("ROD_silverwood");
            silverMenu.broadcastChanges();
            WandData sponsorBefore = ItemWandCasting.data(sponsor);
            test.assertTrue(silverMenu.craftable() && WandData.PRIMALS.stream().allMatch(primal -> silverMenu.requiredVis(primal) == 2700),
                    "gold3 times Silverwood9 costs exactly27 of EACH primal through the real menu");
            ItemStack silverwood = take(silverMenu, player);
            test.assertTrue(ItemWandCasting.data(silverwood).equals(new WandData("silverwood", "gold", Map.of()))
                    && silverwood.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), 0) == 27,
                    "native Silverwood result starts empty and retains its original craftCost27 component");
            // Native applyCraft installs the paid copy atomically. The old
            // pre-commit sponsor Java object is no longer the slot's storage.
            for (String primal : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(loaded.getItem(10), primal) == sponsorBefore.amount(primal) - 2700,
                    "one original Silverwood assembly payment: " + primal);
            node.setAspects(new AspectList().add(Aspect.AIR, 120));
            player.setItemInHand(InteractionHand.MAIN_HAND, silverwood);
            for (int drain = 0; drain < 100; drain++) node.drainIntoWand(player, silverwood);
            test.assertTrue(ItemWandCasting.getVis(silverwood, "aer") == 10000 && node.getAspects().getAmount(Aspect.AIR) == 20,
                    "real finite node charging passes old50 capacity and stops at original100");
            loaded.setItem(10, silverwood.copy());
            var silverSave = loaded.saveWithFullMetadata(registries);
            level.removeBlockEntity(pos);
            var silverReload = new TileArcaneWorkbench(pos, level.getBlockState(pos));
            silverReload.loadWithComponents(silverSave, registries);
            level.setBlockEntity(silverReload);
            test.assertTrue(ItemStack.matches(silverwood, silverReload.getItem(10)) && ItemWandCasting.getVis(silverReload.getItem(10), "aer") == 10000,
                    "native item/workbench codecs preserve charged100vis and craftCost27 without clamping old material capacities");

            var inertGrid = NonNullList.withSize(9, ItemStack.EMPTY);
            inertGrid.set(6, new ItemStack(ThaumcraftWands.THAUMIUM_WAND_CAP_INERT.get()));
            inertGrid.set(2, new ItemStack(ThaumcraftWands.THAUMIUM_WAND_CAP_INERT.get()));
            inertGrid.set(4, ThaumcraftWands.rodItem("wood"));
            test.assertTrue(!recipe.matches(new ArcaneRecipeInput(inertGrid), level), "inert caps never become usable wand materials without the real infusion");
            // Charged cap/core and paying-wand top-ups are explicit INPUT
            // fixtures here. The focused client earns the caps themselves.
            ItemStack capSponsor = silverReload.getItem(10);
            for (String primal : WandData.PRIMALS) ItemWandCasting.addRealVis(capSponsor, primal, 10000, true);
            for (int slot = 0; slot < 9; slot++) silverReload.setItem(slot, ItemStack.EMPTY);
            silverReload.setItem(6, ThaumcraftWands.capItem("thaumium"));
            silverReload.setItem(2, ThaumcraftWands.capItem("thaumium"));
            silverReload.setItem(4, ThaumcraftWands.rodItem("silverwood"));
            player.closeContainer();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            known.removeResearch("CAP_thaumium");
            player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            var thaumiumMenu = (ContainerArcaneWorkbench) player.containerMenu;
            test.assertTrue(thaumiumMenu.getSlot(0).getItem().isEmpty(), "charged-cap assembly still requires CAP_thaumium research");
            known.addResearch("CAP_thaumium");
            thaumiumMenu.broadcastChanges();
            WandData capSponsorBefore = ItemWandCasting.data(silverReload.getItem(10));
            test.assertTrue(thaumiumMenu.craftable() && WandData.PRIMALS.stream().allMatch(primal -> thaumiumMenu.requiredVis(primal) == 5400),
                    "the GOLD paying wand funds exactly54/primal; output Thaumium discount cannot discount its own assembly");
            ItemStack thaumiumWand = take(thaumiumMenu, player);
            test.assertTrue(ItemWandCasting.data(thaumiumWand).equals(new WandData("silverwood", "thaumium", Map.of()))
                    && thaumiumWand.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), 0) == 54,
                    "native two-charged-cap assembly creates an empty100vis wand with real craftCost54");
            for (String primal : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(silverReload.getItem(10), primal) == capSponsorBefore.amount(primal) - 5400,
                    "one original Thaumium-cap assembly payment: " + primal);
            test.assertTrue(ItemStack.matches(thaumiumWand, ItemStack.parse(registries, thaumiumWand.save(registries)).orElseThrow()),
                    "native item codecs retain craftCost54 and the newly supported cap material");
            test.assertTrue(thaumcraft.common.crafting.ArcaneVis.price(Map.of("ignis", 1), thaumiumWand, player).get("ignis") == 90
                    && thaumcraft.common.items.wands.foci.ItemFocusFire.tickCost(thaumiumWand, player) == 9,
                    "original cap0.9 applies to real arcane and basic-Fire pricing, not capacity or regeneration");
        } finally {
            player.closeContainer();
            if (level.getBlockEntity(pos) instanceof TileArcaneWorkbench table) table.clearContent();
            level.setBlock(pos, oldState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (oldTag != null && level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(oldTag, registries);
            level.setBlock(nodePos, oldNodeState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (oldNodeTag != null && level.getBlockEntity(nodePos) != null) level.getBlockEntity(nodePos).loadWithComponents(oldNodeTag, registries);
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = oldSelected;
            player.setGameMode(oldMode); player.setShiftKeyDown(oldSneaking); player.setPos(oldPosition);
            player.removeAllEffects(); effects.forEach(player::addEffect);
            KnowledgeAccess.get(player).copyFrom(knowledge);
        }
        test.succeed();
    }

    private static ItemStack take(ContainerArcaneWorkbench menu, ServerPlayer player) {
        menu.clicked(0, 0, ClickType.PICKUP, player);
        ItemStack result = menu.getCarried().copy();
        menu.setCarried(ItemStack.EMPTY);
        return result;
    }
}
