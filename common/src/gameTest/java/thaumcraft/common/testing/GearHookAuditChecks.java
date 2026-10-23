package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Equip registered inputs; observe native menu debits, player attacks and block breaking, never call an item hook directly. */
public final class GearHookAuditChecks {
    private GearHookAuditChecks() {}

    private static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", path)));
    }

    public static void creativeRepair(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        var failures = new java.util.ArrayList<String>();
        for (String id : new String[]{"primal_crusher", "crimson_sword", "void_sword", "void_pickaxe", "void_axe", "void_shovel", "void_hoe",
                "void_helmet", "void_chestplate", "void_leggings", "void_boots", "void_robe_helmet", "void_robe_chestplate", "void_robe_leggings"}) {
            player.getInventory().clearContent();
            ItemStack gear = stack(id);
            gear.setDamageValue(50);
            player.getInventory().setItem(20, gear);
            player.setGameMode(GameType.SURVIVAL);
            player.tickCount = 19; player.doTick();
            test.assertTrue(gear.getDamageValue() == 50, id + " off-cadence damage=" + gear.getDamageValue());
            player.tickCount = 20; player.doTick();
            test.assertTrue(gear.getDamageValue() == 49, id + " survival damage=" + gear.getDamageValue());
            int expected = 49;
            if (gear.getItem() instanceof ArmorItem armor) {
                player.getInventory().setItem(20, ItemStack.EMPTY);
                player.setItemSlot(armor.getEquipmentSlot(), gear);
                player.tickCount = 40; player.doTick();
                expected = 47;
                test.assertTrue(gear.getDamageValue() == expected, id + " worn damage=" + gear.getDamageValue());
            }
            player.setGameMode(GameType.CREATIVE);
            player.tickCount = 60; player.doTick();
            com.mojang.logging.LogUtils.getLogger().info("G28 repair {} packed=49 expectedCreative={} actualCreative={}", id, expected, gear.getDamageValue());
            if (gear.getDamageValue() != expected) failures.add(id + " original creative repair must be zero; damage=" + gear.getDamageValue());
        }
        test.assertTrue(failures.isEmpty(), failures.toString());
        test.succeed();
    }

    public static void discountPayments(GameTestHelper test) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var pos = test.absolutePos(new BlockPos(3, 2, 3));
        test.getLevel().setBlockAndUpdate(pos, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        var tile = (TileArcaneWorkbench) test.getLevel().getBlockEntity(pos);
        KnowledgeAccess.get(player).addResearch("ENCHFABRIC");
        // Independent original percentage expectations, not the production discount helper.
        Map<String, Integer> discounts = Map.ofEntries(
                Map.entry("goggles_revealing", 5), Map.entry("robe_chestplate", 2),
                Map.entry("robe_leggings", 2), Map.entry("robe_boots", 1),
                Map.entry("void_robe_helmet", 5), Map.entry("void_robe_chestplate", 5),
                Map.entry("void_robe_leggings", 5), Map.entry("discount_ring_aer", 1));
        try {
            int baseline = craft(test, player, pos, tile);
            test.assertTrue(baseline == 500, "bare gold-capped payer spends 500 aer: " + baseline);
            for (var entry : discounts.entrySet()) {
                ItemStack gear = stack(entry.getKey());
                if (gear.getItem() instanceof ArmorItem armor) player.setItemSlot(armor.getEquipmentSlot(), gear);
                else PlayerAccessories.get(player).setItem(1, gear);
                int paid = craft(test, player, pos, tile);
                int expected = (int) (500 * (1F - entry.getValue() / 100F));
                test.assertTrue(paid == expected, entry.getKey() + " native debit expected " + expected + ", saw " + paid);
                com.mojang.logging.LogUtils.getLogger().info("G28 discount {} bare={} equipped={}", entry.getKey(), baseline, paid);
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) player.setItemSlot(slot, ItemStack.EMPTY);
                PlayerAccessories.get(player).setItem(1, ItemStack.EMPTY);
            }
        } finally {
            player.closeContainer();
            tile.clearContent();
            test.getLevel().removeBlock(pos, false);
        }
        test.succeed();
    }

    public static void nativeUseAndAmulets(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos center = test.absolutePos(new BlockPos(3, 3, 3));
        try {
            for (boolean elemental : new boolean[]{false, true}) {
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    test.getLevel().setBlockAndUpdate(center.offset(x, 0, z), Blocks.DIRT.defaultBlockState());
                    test.getLevel().removeBlock(center.offset(x, 1, z), false);
                }
                player.setPos(center.getX() + .5, center.getY() + 1, center.getZ() + .5);
                ItemStack hoe = elemental ? stack("elemental_hoe") : new ItemStack(Items.DIAMOND_HOE);
                player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
                player.gameMode.useItemOn(player, test.getLevel(), hoe, InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(center), net.minecraft.core.Direction.UP, center, false));
                int tilled = 0;
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (test.getLevel().getBlockState(center.offset(x, 0, z)).is(Blocks.FARMLAND)) tilled++;
                test.assertTrue(tilled == (elemental ? 9 : 1) && hoe.getDamageValue() == tilled, "native hoe area=" + tilled + " wear=" + hoe.getDamageValue());
                com.mojang.logging.LogUtils.getLogger().info("G28 elementalHoe={} tilled={} wear={}", elemental, tilled, hoe.getDamageValue());
            }
            player.getInventory().clearContent();
            ItemStack bow = new ItemStack(ThaumcraftLegacyItems.BONE_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);
            player.getInventory().setItem(10, new ItemStack(Items.ARROW, 16));
            player.gameMode.useItem(player, test.getLevel(), bow, InteractionHand.MAIN_HAND);
            test.assertTrue(player.isUsingItem(), "native bone bow draw starts");
            for (int tick = 0; tick < 22; tick++) player.doTick();
            var arrows = test.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
                    player.getBoundingBox().inflate(8), arrow -> arrow.getOwner() == player);
            try {
                test.assertTrue(!player.isUsingItem() && bow.getDamageValue() == 1 && player.getInventory().countItem(Items.ARROW) == 15 && arrows.size() == 1,
                        "native held-use ticks auto-release exactly one paid bone arrow; arrows=" + arrows.size() + " wear=" + bow.getDamageValue());
                com.mojang.logging.LogUtils.getLogger().info("G28 boneBow nativeAutoRelease arrows={} wear={} ammo={}", arrows.size(), bow.getDamageValue(), player.getInventory().countItem(Items.ARROW));
            } finally { arrows.forEach(net.minecraft.world.entity.Entity::discard); }
            player.getInventory().clearContent();
            KnowledgeAccess.get(player).addResearch("VISAMULET");
            for (String id : new String[]{"vis_amulet_lesser", "vis_amulet"}) {
                ItemStack amulet = stack(id);
                var item = (thaumcraft.common.items.baubles.ItemAmuletVis) amulet.getItem();
                item.storeVis(amulet, thaumcraft.api.aspects.Aspect.AIR, 10);
                ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
                wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("wood", "gold", Map.of()));
                player.setItemInHand(InteractionHand.MAIN_HAND, wand);
                PlayerAccessories.get(player).setItem(0, amulet);
                player.tickCount = 5; player.doTick();
                test.assertTrue(item.getVis(amulet, thaumcraft.api.aspects.Aspect.AIR) == 5 && ItemWandCasting.getVis(wand, "aer") == 5,
                        id + " native worn tick transfers five centivis to the real held wand");
                com.mojang.logging.LogUtils.getLogger().info("G28 {} amulet10->5 wand0->5", id);
                PlayerAccessories.get(player).setItem(0, ItemStack.EMPTY);
            }
        } finally {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) test.getLevel().removeBlock(center.offset(x, 0, z), false);
        }
        test.succeed();
    }

    private static int craft(GameTestHelper test, ServerPlayer player, BlockPos pos, TileArcaneWorkbench tile) {
        ItemStack payer = new ItemStack(ThaumcraftContent.WAND.get());
        payer.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "gold", Map.of("aer", 1000)));
        tile.clearContent();
        tile.setItem(10, payer);
        for (int slot = 0; slot < 9; slot++) if (slot != 1) tile.setItem(slot, new ItemStack(ThaumcraftContent.MATERIALS.get("enchanted_fabric").get()));
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        player.gameMode.useItemOn(player, test.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
        test.assertTrue(player.containerMenu instanceof ContainerArcaneWorkbench, "actual workbench menu opens");
        var menu = (ContainerArcaneWorkbench) player.containerMenu;
        menu.broadcastChanges();
        test.assertTrue(menu.craftable(), "funded researched robe recipe is craftable");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        test.assertTrue(menu.getCarried().is(stack("robe_chestplate").getItem()), "native result extraction produces a robe");
        int paid = 1000 - ItemWandCasting.getVis(tile.getItem(10), "aer");
        menu.setCarried(ItemStack.EMPTY);
        player.closeContainer();
        return paid;
    }

    public static void crimsonAttack(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        for (boolean crimson : new boolean[]{false, true}) {
            var pig = EntityType.PIG.create(test.getLevel());
            pig.moveTo(test.absolutePos(new BlockPos(3, 2, 3)), 0, 0);
            pig.setNoAi(true);
            test.getLevel().addFreshEntity(pig);
            try {
                player.setItemInHand(InteractionHand.MAIN_HAND, crimson ? new ItemStack(ThaumcraftLegacyItems.CRIMSON_SWORD.get()) : new ItemStack(Items.DIAMOND_SWORD));
                player.attack(pig);
                int weakness = pig.hasEffect(MobEffects.WEAKNESS) ? pig.getEffect(MobEffects.WEAKNESS).getDuration() : 0;
                int hunger = pig.hasEffect(MobEffects.HUNGER) ? pig.getEffect(MobEffects.HUNGER).getDuration() : 0;
                test.assertTrue(weakness == (crimson ? 60 : 0) && hunger == (crimson ? 120 : 0), "native attack sap: weakness=" + weakness + " hunger=" + hunger);
                com.mojang.logging.LogUtils.getLogger().info("G28 crimson={} weakness={} hunger={}", crimson, weakness, hunger);
            } finally { pig.discard(); }
        }
        test.succeed();
    }

    public static void crusherBreak(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos center = test.absolutePos(new BlockPos(3, 4, 3));
        try {
            for (boolean crusher : new boolean[]{false, true}) {
                for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) test.getLevel().setBlockAndUpdate(center.offset(x, y, 0), Blocks.STONE.defaultBlockState());
                player.setPos(center.getX() + .5, center.getY() + .5 - player.getEyeHeight(), center.getZ() - 3);
                player.setYRot(0); player.setXRot(0);
                ItemStack tool = crusher ? new ItemStack(ThaumcraftLegacyItems.PRIMAL_CRUSHER.get()) : new ItemStack(Items.DIAMOND_PICKAXE);
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                test.assertTrue(player.gameMode.destroyBlock(center), "native center break succeeds");
                int remaining = 0;
                for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) if (!test.getLevel().getBlockState(center.offset(x, y, 0)).isAir()) remaining++;
                test.assertTrue(remaining == (crusher ? 0 : 8), "native crusher=" + crusher + " remaining=" + remaining);
                test.assertTrue(tool.getDamageValue() == (crusher ? 9 : 1), "one use per mined block: " + tool.getDamageValue());
                com.mojang.logging.LogUtils.getLogger().info("G28 crusher={} remaining={} durabilitySpent={}", crusher, remaining, tool.getDamageValue());
            }
        } finally {
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) test.getLevel().removeBlock(center.offset(x, y, 0), false);
        }
        test.succeed();
    }
}
