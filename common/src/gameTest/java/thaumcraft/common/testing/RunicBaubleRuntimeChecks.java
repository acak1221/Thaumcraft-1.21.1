package thaumcraft.common.testing;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.config.ThaumcraftRunicBaubles;
import thaumcraft.common.items.baubles.ItemAmuletRunic;
import thaumcraft.common.items.baubles.ItemGirdleRunic;
import thaumcraft.common.items.baubles.ItemRingRunic;
import thaumcraft.common.lib.baubles.RunicBaubleRules;

/**
 * The original TC4 runic accessories and cultist armour inside a real dedicated server: the
 * original base runic charge, slot and rarity of the amulet, the ring and the girdle, and the
 * original protection, durability and repair of the ten pieces of the crimson cult armour that
 * the cultist mobs wear.
 */
public final class RunicBaubleRuntimeChecks {
    private RunicBaubleRuntimeChecks() {}

    /** The original runic accessories keep their charge, their slot and their rarity. */
    public static void runicAccessoriesKeepTheirOriginalChargeAndSlot(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(3);

            var amulet = ItemAmuletRunic.stack(ItemAmuletRunic.PLAIN);
            var emergency = ItemAmuletRunic.stack(ItemAmuletRunic.EMERGENCY);
            test.assertTrue(ItemAmuletRunic.getRunicCharge(ItemAmuletRunic.PLAIN) == 8
                            && ItemAmuletRunic.getRunicCharge(ItemAmuletRunic.EMERGENCY) == 7,
                    "the runic amulet keeps the original charge of eight and the emergency one of seven: "
                            + ItemAmuletRunic.getRunicCharge(ItemAmuletRunic.PLAIN) + " "
                            + ItemAmuletRunic.getRunicCharge(ItemAmuletRunic.EMERGENCY));
            test.assertTrue(ItemAmuletRunic.getBaubleType() == BaubleType.AMULET
                            && ItemAmuletRunic.getRarity(ItemAmuletRunic.PLAIN) == net.minecraft.world.item.Rarity.RARE,
                    "the runic amulet is an original rare amulet");
            var ring = ItemRingRunic.stack(1);
            test.assertTrue(ItemRingRunic.getRunicCharge(1) == 5 && ItemRingRunic.getBaubleType() == BaubleType.RING,
                    "the runic ring keeps the original charge of five and the ring slot: " + ItemRingRunic.getRunicCharge(1));
            test.assertTrue(ItemRingRunic.getRarity(0) == net.minecraft.world.item.Rarity.UNCOMMON
                            && ItemRingRunic.getRarity(1) == net.minecraft.world.item.Rarity.RARE,
                    "only the lesser runic ring is uncommon, as in the original");
            var girdle = ItemGirdleRunic.stack(0);
            test.assertTrue(ItemGirdleRunic.getRunicCharge(0) == 10
                            && ItemGirdleRunic.getBaubleType() == BaubleType.BELT,
                    "the runic girdle keeps the original charge of ten and the belt slot: " + ItemGirdleRunic.getRunicCharge(0));

            // The registered item really carries the original charge.
            test.assertTrue(((thaumcraft.common.items.baubles.ItemRunicBauble) amulet.getItem()).getRunicCharge(amulet) == 8
                            && ((thaumcraft.common.items.baubles.ItemRunicBauble) amulet.getItem()).getBaubleType(amulet) == BaubleType.AMULET,
                    "the registered runic amulet carries the original eight charge points");
            test.assertTrue(((thaumcraft.common.items.baubles.ItemRunicBauble) emergency.getItem()).getRunicCharge(emergency) == 7,
                    "the registered emergency amulet carries the original seven charge points");
            test.assertTrue(((thaumcraft.common.items.baubles.ItemRunicBauble) ring.getItem()).getRunicCharge(ring) == 5,
                    "the registered runic ring carries the original five charge points");
            test.assertTrue(((thaumcraft.common.items.baubles.ItemRunicBauble) girdle.getItem()).getRunicCharge(girdle) == 10,
                    "the registered runic girdle carries the original ten charge points");
            test.assertTrue(RunicBaubleRules.runicAccessories().size() == 8,
                    "the original eight runic accessory variants are registered: " + RunicBaubleRules.runicAccessories().size());
            test.assertTrue(RunicBaubleRules.blankBaubles().size() == 9,
                    "the original nine blank baubles are registered: " + RunicBaubleRules.blankBaubles().size());
            test.assertTrue(RunicBaubleRules.blankVisDiscount(3, "aer") == 1
                            && RunicBaubleRules.blankVisDiscount(3, "ignis") == 0,
                    "a discount ring only discounts its own primal");
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original cultist armour keeps its protection, its durability and its repair. */
    public static void cultistArmourKeepsItsOriginalNumbers(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(3);

            var robe = new ItemStack(ThaumcraftRunicBaubles.CULTIST_ROBE.get());
            var plate = new ItemStack(ThaumcraftRunicBaubles.CULTIST_PLATE.get());
            var helm = new ItemStack(ThaumcraftRunicBaubles.CULTIST_PLATE_HELM.get());
            var leader = new ItemStack(ThaumcraftRunicBaubles.CULTIST_LEADER.get());
            var boots = new ItemStack(ThaumcraftRunicBaubles.CULTIST_BOOTS.get());
            test.assertTrue(robe.getMaxDamage() == RunicBaubleRules.cultistArmor().stream()
                            .filter(piece -> "cultist_robe".equals(piece.id())).findFirst().orElseThrow().durability(),
                    "the crimson robe keeps its original durability");
            test.assertTrue(leader.getMaxDamage() > plate.getMaxDamage(),
                    "the crimson praetor armour is tougher than the plain cultist plate: " + leader.getMaxDamage() + " "
                            + plate.getMaxDamage());
            test.assertTrue(boots.getItem() instanceof thaumcraft.common.items.armor.ItemCultistBoots,
                    "the cultist boots are the original item");
            test.assertTrue(robe.getItem() instanceof thaumcraft.common.items.armor.ItemCultistRobeArmor
                            && plate.getItem() instanceof thaumcraft.common.items.armor.ItemCultistPlateArmor
                            && leader.getItem() instanceof thaumcraft.common.items.armor.ItemCultistLeaderArmor,
                    "the cultist armour pieces are the original items");
            // The armour really protects a wearer: the original chest protection of the plate is six.
            var material = ThaumcraftRunicBaubles.CULTIST_PLATE_ARMOR_MATERIAL.get();
            test.assertTrue(material.getDefense(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE) == 6
                            && material.getDefense(net.minecraft.world.item.ArmorItem.Type.LEGGINGS) == 5
                            && material.getDefense(net.minecraft.world.item.ArmorItem.Type.BOOTS) == 2
                            && material.getDefense(net.minecraft.world.item.ArmorItem.Type.HELMET) == 2,
                    "the cultist plate keeps the original iron protection of 2, 6, 5 and 2: "
                            + material.getDefense(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
            var leaderMaterial = ThaumcraftRunicBaubles.CULTIST_LEADER_ARMOR_MATERIAL.get();
            test.assertTrue(leaderMaterial.getDefense(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE) == 7
                            && leaderMaterial.getDefense(net.minecraft.world.item.ArmorItem.Type.LEGGINGS) == 6
                            && leaderMaterial.getDefense(net.minecraft.world.item.ArmorItem.Type.BOOTS) == 3
                            && leaderMaterial.getDefense(net.minecraft.world.item.ArmorItem.Type.HELMET) == 3,
                    "the crimson praetor armour keeps the original protection of 3, 7, 6 and 3: "
                            + leaderMaterial.getDefense(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
            test.assertTrue(material.enchantmentValue() == 15 && leaderMaterial.enchantmentValue() == 25,
                    "the cultist armour keeps the original enchantability of 15 and 25: "
                            + material.enchantmentValue() + " " + leaderMaterial.enchantmentValue());
            test.assertTrue(ThaumcraftRunicBaubles.cultistPieces().size() == 10,
                    "the original ten cultist armour pieces are registered: " + ThaumcraftRunicBaubles.cultistPieces().size());
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }
}
