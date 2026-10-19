package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.entities.projectile.EntityEmber;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.lib.network.FocusSelectionPayload;

/** One bounded real-item/recipe/projectile check. Direct receiver calls are not a wire-dispatch claim. */
public final class FocusRuntimeChecks {
    private FocusRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        var inventory = player.getInventory();
        var savedInventory = snapshot(player);
        var knowledge = KnowledgeAccess.get(player).copy();
        var savedEffects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var position = player.position();
        float yaw = player.getYRot(), pitch = player.getXRot();
        var mode = player.gameMode.getGameModeForPlayer();
        int selected = inventory.selected;
        BlockPos pos = test.absolutePos(new BlockPos(3, 3, 3));
        BlockPos nodePos = pos.above(3);
        var nodeState = level.getBlockState(nodePos);
        var oldNode = level.getBlockEntity(nodePos);
        AABB bounds = new AABB(pos).inflate(16);
        var oldEmbers = level.getEntitiesOfClass(EntityEmber.class, bounds).stream().map(EntityEmber::getUUID).collect(Collectors.toSet());
        try {
            player.closeContainer(); inventory.clearContent(); inventory.selected = 0;
            player.setGameMode(GameType.SURVIVAL); player.removeAllEffects();
            player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5); player.setYRot(0); player.setXRot(-90);
            var known = KnowledgeAccess.get(player); known.removeResearch("FOCUSFIRE");
            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            WandData materials = new WandData("greatwood", "gold", Map.of("ignis", 31, "aer", 123));
            wand.set(ThaumcraftContent.WAND_DATA.get(), materials);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            ItemStack a = focus("focus-A"), b = focus("focus-B");
            inventory.setItem(5, a.copy());
            test.assertTrue(select(player, 5) && inventory.getItem(5).isEmpty() && ItemStack.matches(FocusData.get(wand), a), "real inventory focus is moved into the actual held wand");
            ItemStack exposed = FocusData.get(wand); exposed.setCount(2);
            FocusData immutable = new FocusData(a); a.set(DataComponents.CUSTOM_NAME, Component.literal("changed-input"));
            test.assertTrue(FocusData.get(wand).getCount() == 1 && !ItemStack.matches(a, immutable.focus()), "constructor and accessor expose no mutable focus aliases");
            a = FocusData.get(wand);
            test.assertTrue(!FocusData.isSupportedFocus(wand) && !FocusData.isSupportedFocus(a.copyWithCount(2))
                    && !FocusData.isSupportedFocus(new ItemStack(Items.STICK)), "non-focus, nested wand and invalid counts are unsupported");
            ItemStack nested = a.copy(); nested.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(b));
            test.assertTrue(!FocusData.isSupportedFocus(nested), "a focus cannot recursively contain another equipped focus");
            test.assertTrue(ItemStack.matches(wand, ItemStack.parse(level.registryAccess(), wand.save(level.registryAccess())).orElseThrow()), "actual wand save/reload preserves focus native data, materials and vis");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer, wand);
                test.assertTrue(ItemStack.matches(wand, ItemStack.STREAM_CODEC.decode(buffer)), "native equipped ItemStack synchronizes its real component-bearing focus");
                FocusSelectionPayload.STREAM_CODEC.encode(buffer, new FocusSelectionPayload(InteractionHand.OFF_HAND, -1));
                test.assertTrue(buffer.readableBytes() == 2 && FocusSelectionPayload.STREAM_CODEC.decode(buffer).inventorySlot() == -1, "selection intent contains only two hand/slot bytes");
            } finally { buffer.release(); }

            var recipe = (NativeArcaneRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/focus_fire")).orElseThrow().value();
            var grid = new ArrayList<ItemStack>();
            for (int cell = 0; cell < 9; cell++) grid.add(new ItemStack(cell == 4 ? Items.FIRE_CHARGE
                    : cell % 2 == 0 ? ThaumcraftContent.SHARDS.get("ignis").get() : Items.QUARTZ));
            var input = new ArcaneRecipeInput(grid);
            test.assertTrue(recipe.matches(input, level) && recipe.vis().equals(Map.of("ignis", 20, "perditio", 10))
                    && recipe.assemble(input, level.registryAccess()).is(ThaumcraftFoci.FIRE_FOCUS.get()) && !recipe.hasResearch(input, player), "registered original CQC/Q#Q/CQC recipe requires FOCUSFIRE and 20/10 whole vis");
            known.addResearch("FOCUSFIRE");
            test.assertTrue(recipe.hasResearch(input, player), "real actor research unlocks the actual arcane recipe");
            known.removeResearch("FOCUSFIRE");

            inventory.setItem(9, b.copy());
            test.assertTrue(select(player, 9) && ItemStack.matches(inventory.getItem(1), a) && inventory.getItem(9).isEmpty(), "swap extracts source before returning old focus to the FIRST empty inventory slot");
            for (int slot = 1; slot < 36; slot++) inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            inventory.setItem(7, a.copy());
            test.assertTrue(select(player, 7) && ItemStack.matches(FocusData.get(wand), a) && ItemStack.matches(inventory.getItem(7), b), "full-inventory swap reuses its freed source without deleting either focus");
            player.setGameMode(GameType.CREATIVE);
            test.assertTrue(select(player, 7) && ItemStack.matches(FocusData.get(wand), b) && ItemStack.matches(inventory.getItem(7), a), "creative/replayed selection still MOVES one real focus; no free copy");
            var full = snapshot(player);
            test.assertTrue(!select(player, -1) && !select(player, 6), "full removal and non-focus selection refuse without dropping or deleting");
            assertInventory(test, player, full);
            player.setGameMode(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            test.assertTrue(!select(player, 7) && ItemStack.matches(inventory.getItem(7), a), "late selection re-resolves the REAL hand and leaves its source intact");
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            inventory.setItem(2, ItemStack.EMPTY);
            test.assertTrue(select(player, -1) && FocusData.get(wand).isEmpty() && ItemStack.matches(inventory.getItem(2), b), "removal stores the original native focus instead of dropping it");
            test.assertTrue(select(player, 2) && ItemWandCasting.data(wand).equals(materials), "re-equip preserves original rod, cap, capacity and every stored primal");

            int baseline = embers(player, bounds).size();
            test.assertTrue(wand.getUseAnimation() == UseAnim.BOW && player.gameMode.useItem(player, level, wand, InteractionHand.MAIN_HAND).consumesAction()
                    && player.isUsingItem(), "actual wand hook starts basic fire with BOW use animation despite absent FOCUSFIRE research");
            wand.onUseTick(level, player, player.getUseItemRemainingTicks());
            test.assertTrue(ItemWandCasting.getVis(wand, "ignis") == 21 && embers(player, bounds).size() == baseline + 2, "one native paid gold-cap tick debits TEN CENTIVIS and creates two real embers");
            for (EntityEmber ember : embers(player, bounds)) if (!oldEmbers.contains(ember.getUUID())) {
                test.assertTrue(ember.getDamage() == 2F && ember.getScatter() == 15 && ember.getDuration() == 20 && ember.getFireSeconds() == 3, "basic ember carries original damage/scatter/lifetime/ignition values");
            }
            player.addEffect(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 100, 0));
            wand.onUseTick(level, player, player.getUseItemRemainingTicks());
            test.assertTrue(ItemWandCasting.getVis(wand, "ignis") == 10 && embers(player, bounds).size() == baseline + 4, "real player exhaustion applies original float modifier and truncation");
            player.removeAllEffects();
            wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "iron", ItemWandCasting.data(wand).vis()).withAmount("ignis", 11));
            test.assertTrue(ItemFocusFire.tickCost(wand, player) == 11, "iron cap prices basic fire at eleven centivis, not eleven whole vis");
            wand.onUseTick(level, player, player.getUseItemRemainingTicks());
            wand.onUseTick(level, player, player.getUseItemRemainingTicks());
            test.assertTrue(!player.isUsingItem() && ItemWandCasting.getVis(wand, "ignis") == 0 && embers(player, bounds).size() == baseline + 6, "unfunded next tick stops use without another projectile or partial debit");
            player.gameMode.useItem(player, level, wand, InteractionHand.MAIN_HAND);
            test.assertTrue(player.isUsingItem(), "empty wand still begins use; affordability is checked on tick, not activation");
            wand.onUseTick(level, player, player.getUseItemRemainingTicks());
            test.assertTrue(!player.isUsingItem() && embers(player, bounds).size() == baseline + 6, "empty activation cannot emit an unpaid ember");
            player.setGameMode(GameType.CREATIVE);
            wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("ignis", 11));
            player.gameMode.useItem(player, level, wand, InteractionHand.MAIN_HAND);
            wand.onUseTick(level, player, player.getUseItemRemainingTicks());
            test.assertTrue(ItemWandCasting.getVis(wand, "ignis") == 0 && embers(player, bounds).size() == baseline + 8, "creative fire also pays its original real vis cost");
            player.stopUsingItem(); player.setGameMode(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)); player.setItemInHand(InteractionHand.OFF_HAND, wand);
            wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("ignis", 11));
            player.gameMode.useItem(player, level, wand, InteractionHand.OFF_HAND);
            wand.onUseTick(level, player, player.getUseItemRemainingTicks());
            test.assertTrue(ItemWandCasting.getVis(wand, "ignis") == 0 && embers(player, bounds).size() == baseline + 10, "payment commits to the actual native OFF_HAND wand too");
            wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("ignis", 11));
            ItemStack replacement = wand.copy(); player.setItemInHand(InteractionHand.OFF_HAND, replacement);
            wand.onUseTick(level, player, player.getUseItemRemainingTicks());
            test.assertTrue(ItemWandCasting.getVis(wand, "ignis") == 11 && ItemWandCasting.getVis(replacement, "ignis") == 11
                    && embers(player, bounds).size() == baseline + 10, "stale same-item hand identity can neither spend nor emit");
            test.assertTrue(ItemWandCasting.data(replacement).capacity() == 5000 && ItemWandCasting.getVis(replacement, "aer") == 123
                    && ItemStack.matches(FocusData.get(replacement), b), "firing retains other primals, bounded materials and all equipped focus components");
            player.stopUsingItem();
            level.setBlock(nodePos, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            var node = (TileNode) level.getBlockEntity(nodePos);
            node.setAspects(new AspectList().add(Aspect.AIR, 5));
            player.setYRot(0); player.setXRot(-90);
            player.gameMode.useItem(player, level, replacement, InteractionHand.OFF_HAND);
            replacement.onUseTick(level, player, Integer.MAX_VALUE - 2); // original count%5==0
            test.assertTrue(player.isUsingItem() && ItemWandCasting.getVis(replacement, "aer") == 223
                    && ItemWandCasting.getVis(replacement, "ignis") == 11 && node.getAspects().getAmount(Aspect.AIR) == 4
                    && embers(player, bounds).size() == baseline + 10,
                    "actual node activation outranks an equipped fire focus and drains that chosen object without firing");
            player.setXRot(0);
            replacement.onUseTick(level, player, player.getUseItemRemainingTicks());
            test.assertTrue(!player.isUsingItem() && ItemWandCasting.getVis(replacement, "ignis") == 11
                    && embers(player, bounds).size() == baseline + 10,
                    "looking away terminates a node session; the same held use cannot silently become a paid fire channel");
        } finally {
            player.stopUsingItem();
            level.setBlock(nodePos, nodeState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (oldNode != null) { oldNode.clearRemoved(); level.setBlockEntity(oldNode); }
            for (EntityEmber ember : embers(player, bounds)) if (!oldEmbers.contains(ember.getUUID())) ember.discard();
            inventory.clearContent();
            for (int slot = 0; slot < savedInventory.size(); slot++) inventory.setItem(slot, savedInventory.get(slot));
            inventory.selected = selected; player.setGameMode(mode); player.setPos(position); player.setYRot(yaw); player.setXRot(pitch);
            player.removeAllEffects(); savedEffects.forEach(player::addEffect); KnowledgeAccess.get(player).copyFrom(knowledge);
        }
        test.succeed();
    }

    private static ItemStack focus(String name) {
        ItemStack focus = new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get());
        focus.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        CompoundTag tag = new CompoundTag(); tag.putString("preserve", name); focus.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return focus;
    }
    private static boolean select(ServerPlayer player, int slot) { return FocusSelectionNetworking.handle(player, new FocusSelectionPayload(InteractionHand.MAIN_HAND, slot)); }
    private static List<EntityEmber> embers(ServerPlayer player, AABB bounds) { return player.serverLevel().getEntitiesOfClass(EntityEmber.class, bounds, ember -> ember.getOwner() == player && !ember.isRemoved()); }
    private static List<ItemStack> snapshot(ServerPlayer player) {
        var stacks = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) stacks.add(player.getInventory().getItem(slot).copy());
        return stacks;
    }
    private static void assertInventory(GameTestHelper test, ServerPlayer player, List<ItemStack> expected) {
        for (int slot = 0; slot < expected.size(); slot++) test.assertTrue(ItemStack.matches(expected.get(slot), player.getInventory().getItem(slot)), "selection refusal preserves native inventory slot " + slot);
    }
}
