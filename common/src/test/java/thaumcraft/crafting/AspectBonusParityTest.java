package thaumcraft.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Original TC4 equipment/enchantment/potion bonuses, with vanilla 1.7.10 superclass and material semantics.
 * The official 1.7.10 client (SHA1 e80d9b3bf5085002218d4be59e668bac718abbc6) has
 * ItemHoe (ada) extend Item (adb), and ItemSword.i() (func_150931_i) return the material's damage.
 * The separate sword attack attribute is four plus that value; it is not the aspect bonus.
 */
class AspectBonusParityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static AspectList bonus(ItemStack stack) {
        return ObjectAspectResolver.bonusTags(stack);
    }

    @Test void swordsUseMaterialDamageRatherThanTheirAttackAttribute() {
        var swords = List.of(Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD);
        int[] expected = {1, 2, 3, 1, 4};
        for (int i = 0; i < swords.size(); i++) {
            AspectList tags = bonus(new ItemStack(swords.get(i)));
            assertEquals(expected[i], tags.getAmount(Aspect.WEAPON), swords.get(i).toString());
            assertEquals(1, tags.size(), "an unenchanted sword contributes only its material WEAPON bonus");
        }
    }

    @Test void hoesReachTheOriginalDurabilityBasedHarvestBranch() {
        var hoes = List.of(Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE);
        int[] expected = {1, 2, 3, 1, 4};
        for (int i = 0; i < hoes.size(); i++) {
            AspectList tags = bonus(new ItemStack(hoes.get(i)));
            assertEquals(expected[i], tags.getAmount(Aspect.HARVEST), hoes.get(i).toString());
            assertEquals(0, tags.getAmount(Aspect.TOOL), "ItemHoe was not an ItemTool in 1.7.10");
            assertEquals(1, tags.size(), "an unenchanted hoe contributes only its durability HARVEST bonus");
        }
    }

    @Test void shearsKeepTheOriginalHarvestBonusAndNoToolBonus() {
        // Oracle getBonusTags: ItemShears extends Item in 1.7.10 and keeps the harvest bonus of
        // the shears/hoe branch. The 1.7.10 shears have 238 uses, which is above the stone uses
        // (131) and the gold uses (32) and below the iron uses (250), so the bonus is three.
        AspectList tags = bonus(new ItemStack(Items.SHEARS));
        assertEquals(3, tags.getAmount(Aspect.HARVEST));
        assertEquals(0, tags.getAmount(Aspect.TOOL));
        assertEquals(1, tags.size());
    }

    @Test void theBowThePickaxeAndTheArmourKeepTheOriginalBonuses() {
        AspectList bow = bonus(new ItemStack(Items.BOW));
        assertEquals(3, bow.getAmount(Aspect.WEAPON));
        assertEquals(1, bow.getAmount(Aspect.FLIGHT));
        assertEquals(1, bonus(new ItemStack(Items.WOODEN_PICKAXE)).getAmount(Aspect.MINE));
        assertEquals(3, bonus(new ItemStack(Items.IRON_PICKAXE)).getAmount(Aspect.MINE));
        assertEquals(4, bonus(new ItemStack(Items.DIAMOND_PICKAXE)).getAmount(Aspect.MINE));
        // The 1.7.10 iron chestplate has damageReduceAmount 6, the 1.21.1 iron chestplate has
        // the same six armour points.
        assertEquals(6, bonus(new ItemStack(Items.IRON_CHESTPLATE)).getAmount(Aspect.ARMOR));
    }

    /**
     * Puts one enchantment on a stack.
     *
     * <p>The enchantments are a <b>dynamic</b> registry in 1.21.1, so a unit test cannot read
     * them from a registry access: {@code Registries.ENCHANTMENT} is not a member of the static
     * registry of the vanilla bootstrap and the lookup of the lookup throws
     * {@code Registry minecraft:enchantment not found}. The production code asks the holder of
     * the enchantment for its key ({@code ObjectAspectResolver}:163), so the test builds a stand
     * alone holder that carries the key of the original enchantment
     * (Minecraft 1.21.1 {@code net/minecraft/core/Holder$Reference}:{@code createStandAlone}).</p>
     */
    private static ItemStack withEnchantment(ItemStack stack, ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> holder = Holder.Reference.createStandAlone(new HolderOwner<Enchantment>() {}, key);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(holder, level);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        return stack;
    }

    @Test void thornsAndSmiteKeepTheOriginalAspects() {
        // Oracle getBonusTags: the enchantment branch compares the stored effect id with the
        // vanilla enchantment constants and merges the enchantment LEVEL. Enchantment.field_92091_k
        // is thorns and is merged into WEAPON, Enchantment.field_77339_k is smite and is merged
        // into ENTROPY. The pickaxe carries no weapon bonus of its own.
        AspectList thorns = bonus(withEnchantment(new ItemStack(Items.IRON_PICKAXE), Enchantments.THORNS, 3));
        assertEquals(3, thorns.getAmount(Aspect.WEAPON));
        assertEquals(0, thorns.getAmount(Aspect.ENTROPY));
        assertEquals(3, thorns.getAmount(Aspect.MAGIC), "the sum of the enchantment levels is magic");
        AspectList smite = bonus(withEnchantment(new ItemStack(Items.IRON_PICKAXE), Enchantments.SMITE, 2));
        assertEquals(2, smite.getAmount(Aspect.ENTROPY));
        assertEquals(0, smite.getAmount(Aspect.WEAPON));
        assertEquals(2, smite.getAmount(Aspect.MAGIC));
    }

    @Test void theOtherOriginalEnchantmentAspectsAreUnchanged() {
        assertEquals(4, bonus(withEnchantment(new ItemStack(Items.IRON_PICKAXE), Enchantments.SHARPNESS, 4)).getAmount(Aspect.WEAPON));
        assertEquals(3, bonus(withEnchantment(new ItemStack(Items.IRON_PICKAXE), Enchantments.EFFICIENCY, 3)).getAmount(Aspect.TOOL));
        assertEquals(3, bonus(withEnchantment(new ItemStack(Items.IRON_PICKAXE), Enchantments.UNBREAKING, 3)).getAmount(Aspect.EARTH));
        assertEquals(2, bonus(withEnchantment(new ItemStack(Items.IRON_PICKAXE), Enchantments.SILK_TOUCH, 2)).getAmount(Aspect.EXCHANGE));
        assertEquals(2, bonus(withEnchantment(new ItemStack(Items.IRON_PICKAXE), Enchantments.FIRE_ASPECT, 2)).getAmount(Aspect.FIRE));
        assertEquals(2, bonus(withEnchantment(new ItemStack(Items.IRON_PICKAXE), Enchantments.KNOCKBACK, 2)).getAmount(Aspect.AIR));
    }

    private static ItemStack potion(ItemStack stack, Holder<MobEffect> effect, int amplifier) {
        PotionContents contents = new PotionContents(Optional.empty(), Optional.empty(),
                List.of(new MobEffectInstance(effect, 200, amplifier)));
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }

    @Test void thePotionAspectsKeepTheOriginalTable() {
        // Oracle getObjectTags: the potion branch merges WATER 1, ENTRPY 2 for a splash potion,
        // MAGIC of two per amplifier level and the effect aspect of three per amplifier level.
        AspectList confusion = ObjectAspectResolver.potionTags(potion(new ItemStack(Items.POTION), MobEffects.CONFUSION, 0));
        assertEquals(1, confusion.getAmount(Aspect.WATER));
        assertEquals(2, confusion.getAmount(Aspect.MAGIC), "two magic per amplifier level");
        assertEquals(3, confusion.getAmount(Aspect.ELDRITCH), "three of the effect aspect per amplifier level");
        assertEquals(0, confusion.getAmount(Aspect.DEATH));
        // The 1.7.10 confusion potion is Potion.confusion and the original merges it into ELDRITCH.
        // The 1.7.10 weakness potion is Potion.weakness and the original merges it into death.
        AspectList weakness = ObjectAspectResolver.potionTags(potion(new ItemStack(Items.POTION), MobEffects.WEAKNESS, 1));
        assertEquals(6, weakness.getAmount(Aspect.DEATH), "three of the effect aspect per amplifier level");
        assertEquals(0, weakness.getAmount(Aspect.SENSES));
        assertEquals(4, weakness.getAmount(Aspect.MAGIC));
        // The 1.7.10 invisibility potion is merged into senses; the port had no entry at all.
        AspectList invisibility = ObjectAspectResolver.potionTags(potion(new ItemStack(Items.POTION), MobEffects.INVISIBILITY, 0));
        assertEquals(3, invisibility.getAmount(Aspect.SENSES));
        // A splash potion carries the entropy of the original.
        AspectList splash = ObjectAspectResolver.potionTags(potion(new ItemStack(Items.SPLASH_POTION), MobEffects.BLINDNESS, 0));
        assertEquals(2, splash.getAmount(Aspect.ENTROPY));
        assertEquals(3, splash.getAmount(Aspect.DARKNESS));
    }
}
