package thaumcraft.common.testing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.entities.projectile.EntityBottleTaint;
import thaumcraft.common.items.ItemBottleTaint;
import thaumcraft.common.items.relics.ItemSanityChecker;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The original TC4 equipment and relic items inside a real dedicated server: the bone bow with
 * its original durability, enchantment value and draw curve, the crimson blade and the primal
 * crusher with their original damage and warp, the bottle of taint that poisons the ground it
 * bursts on, the sanity checker that does nothing when it is used and the death bucket that
 * pours the original death fluid.
 */
public final class EquipmentItemRuntimeChecks {
    private EquipmentItemRuntimeChecks() {}

    /** The original bone bow keeps its durability, its draw curve and its arrow damage. */
    public static void boneBowKeepsItsOriginalDrawAndDamage(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(3);
            var bow = new ItemStack(ThaumcraftLegacyItems.BONE_BOW.get());
            test.assertTrue(bow.getMaxDamage() == EquipmentRules.BONE_BOW_DURABILITY,
                    "the bone bow keeps the original five hundred and twelve uses: " + bow.getMaxDamage());
            test.assertTrue(bow.getItem().getEnchantmentValue() == EquipmentRules.BONE_BOW_ENCHANTABILITY,
                    "the bone bow keeps the original enchantment value of three: " + bow.getItem().getEnchantmentValue());
            test.assertTrue(bow.getMaxStackSize() == EquipmentRules.BONE_BOW_MAX_STACK_SIZE,
                    "the bone bow never stacks: " + bow.getMaxStackSize());
            test.assertTrue(Math.abs(EquipmentRules.boneBowPower(10) - (1F * 1F + 1F * 2F) / 3F) < 1e-6F,
                    "the bone bow draws at half the vanilla speed: " + EquipmentRules.boneBowPower(10));
            test.assertTrue(EquipmentRules.boneBowPower(100) == 1F && !EquipmentRules.boneBowFires(0)
                            && EquipmentRules.boneBowFires(2),
                    "the bone bow only fires above the original power of 0.1: " + EquipmentRules.boneBowPower(2));
            test.assertTrue(EquipmentRules.boneBowAutoRelease(EquipmentRules.BONE_BOW_MAX_DRAW_TICKS + 1)
                            && !EquipmentRules.boneBowAutoRelease(EquipmentRules.BONE_BOW_MAX_DRAW_TICKS),
                    "the bone bow lets go of the hand after the original eighteen ticks");
            test.assertTrue(Math.abs(EquipmentRules.boneBowArrowDamage(0) - 2.5D) < 1e-9D
                            && Math.abs(EquipmentRules.boneBowArrowDamage(2) - 4D) < 1e-9D,
                    "the arrow of the bone bow keeps the original two and a half base damage: "
                            + EquipmentRules.boneBowArrowDamage(2));
            test.assertTrue(Math.abs(EquipmentRules.boneBowArrowSpeed(1F) - 2.5F) < 1e-6F,
                    "the arrow of the bone bow leaves with the original speed of 2.5: " + EquipmentRules.boneBowArrowSpeed(1F));
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original crimson blade and primal crusher keep their numbers and their warp. */
    public static void crimsonSwordAndPrimalCrusherKeepTheirNumbers(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(3);

            var sword = new ItemStack(ThaumcraftLegacyItems.CRIMSON_SWORD.get());
            test.assertTrue(Math.abs(attackDamage(sword) - EquipmentRules.crimsonSwordDamage()) < 1e-6D,
                    "the crimson blade keeps the original seven and a half attack damage: " + attackDamage(sword));
            test.assertTrue(sword.getMaxDamage() == EquipmentRules.CRIMSON_SWORD_USES,
                    "the crimson blade keeps the original two hundred uses: " + sword.getMaxDamage());
            test.assertTrue(sword.getItem().getEnchantmentValue() == EquipmentRules.CRIMSON_SWORD_ENCHANTABILITY,
                    "the crimson blade keeps the original enchantability of twenty: " + sword.getItem().getEnchantmentValue());
            test.assertTrue(((thaumcraft.api.IWarpingGear) sword.getItem()).getWarp(sword, player) == EquipmentRules.CRIMSON_SWORD_WARP,
                    "the crimson blade carries the original two points of warp");

            var crusher = new ItemStack(ThaumcraftLegacyItems.PRIMAL_CRUSHER.get());
            test.assertTrue(crusher.getMaxDamage() == EquipmentRules.PRIMAL_CRUSHER_USES,
                    "the primal crusher keeps the original five hundred uses: " + crusher.getMaxDamage());
            test.assertTrue(crusher.getItem().getEnchantmentValue() == EquipmentRules.PRIMAL_CRUSHER_ENCHANTABILITY,
                    "the primal crusher keeps the original enchantability of twenty: " + crusher.getItem().getEnchantmentValue());
            test.assertTrue(Math.abs(attackDamage(crusher)
                            - (EquipmentRules.PRIMAL_CRUSHER_TOOL_DAMAGE + EquipmentRules.PRIMAL_CRUSHER_MATERIAL_DAMAGE)) < 1e-6D,
                    "the primal crusher keeps the original three and a half tool damage on top of the original four material damage: "
                            + attackDamage(crusher));
            test.assertTrue(((thaumcraft.api.IWarpingGear) crusher.getItem()).getWarp(crusher, player) == EquipmentRules.PRIMAL_CRUSHER_WARP,
                    "the primal crusher carries the original two points of warp");
            int[] offsets = EquipmentRules.primalCrusherOffsets(0);
            test.assertTrue(offsets.length == 27 && offsets[0] == -1 && offsets[1] == 0 && offsets[2] == -1,
                    "the primal crusher harvests the original nine blocks around the broken one: " + offsets.length);
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The attack damage the real item modifiers give in the main hand. */
    private static double attackDamage(ItemStack stack) {
        double[] total = {0D};
        stack.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) total[0] += modifier.amount();
        });
        return total[0];
    }

    /** The original bottle of taint, sanity checker and death bucket keep their behaviour. */
    public static void relicsAndReliquariesKeepTheirOriginalRules(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(3);
            var fluxTaint = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get());

            // The original bottle of taint poisons everything around the spot it bursts on.
            var victim = lane.add(EntityType.PIG.create(level));
            victim.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            victim.setNoAi(true);
            level.addFreshEntity(victim);
            var bottle = new ItemStack(ThaumcraftLegacyItems.BOTTLE_TAINT.get());
            ItemBottleTaint.burst(level, lane.origin, player);
            test.assertTrue(victim.hasEffect(fluxTaint),
                    "the bottle of taint poisons every living thing around it: " + victim.getEffect(fluxTaint));
            int fibres = 0;
            for (int dx = -4; dx <= 4; dx++)
                for (int dz = -4; dz <= 4; dz++)
                    if (level.getBlockState(lane.origin.offset(dx, 0, dz)).is(thaumcraft.common.config.ThaumcraftTaint.FIBRES.get())) fibres++;
            test.assertTrue(fibres >= 1, "the bottle of taint grows the original taint fibres: " + fibres);
            test.assertTrue(bottle.getMaxStackSize() == EquipmentRules.BOTTLE_TAINT_MAX_STACK_SIZE,
                    "the bottle of taint keeps its original stack size: " + bottle.getMaxStackSize());

            // Oracle ItemSanityChecker:27-55 - the sanity checker of the 4.2.3.5 carries its
            // rarity and nothing else: it does NOTHING when it is used.
            var checker = new ItemStack(ThaumcraftLegacyItems.SANITY_CHECKER.get());
            test.assertTrue(checker.getMaxStackSize() == 1 && checker.getRarity() == net.minecraft.world.item.Rarity.UNCOMMON,
                    "the sanity checker is the original single uncommon relic: " + checker.getRarity());
            player.setItemInHand(InteractionHand.MAIN_HAND, checker);
            var reading = checker.use(level, player, InteractionHand.MAIN_HAND);
            test.assertTrue(reading.getResult() == net.minecraft.world.InteractionResult.PASS,
                    "the sanity checker of the 4.2.3.5 does nothing when it is used: " + reading.getResult());

            // The original death bucket pours the original death fluid.
            var bucket = new ItemStack(ThaumcraftLegacyItems.BUCKET_DEATH.get());
            test.assertTrue(bucket.getItem() instanceof thaumcraft.common.items.ItemBucketDeath,
                    "the death bucket is the original item");
            test.assertTrue(thaumcraft.common.items.ItemBucketDeath.pour(level, lane.origin),
                    "the death bucket pours the original death fluid on the ground");
            test.assertTrue(level.getBlockState(lane.origin).is(thaumcraft.common.config.ThaumcraftLegacyTiles.DEATH_FLUID.get()),
                    "the death fluid really stands where the bucket poured it: " + level.getBlockState(lane.origin));
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }
}
