package thaumcraft.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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

/**
 * The item bonus aspects of the 1.7.10 oracle against the native port.
 *
 * <p>Oracle {@code thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.getBonusTags} (read from
 * the bytecode of {@code Thaumcraft-1.7.10-4.2.3.5.jar}) walks the item class in the order armour,
 * sword, bow, pickaxe, tool, shears/hoe, leaves the chain on every branch and then adds the
 * enchantment bonus. The native port had three deviations:</p>
 *
 * <ul>
 *   <li>the sword bonus was scaled with the tool material
 *       ({@code getTier().getAttackDamageBonus() + 1}) instead of the original constant five;</li>
 *   <li>the hoe took the harvest bonus of the shears instead of the tool bonus of the original,
 *       because the original {@code ItemHoe} extends {@code ItemTool} and is therefore caught by
 *       the tool branch before the shears/hoe branch;</li>
 *   <li>the enchantment table had thorns and smite transposed: the original merges thorns into
 *       WEAPON and smite into ENTROPY.</li>
 * </ul>
 *
 * <p>The same file pins the original potion table of {@code getObjectTags}: the port had
 * confusion and weakness transposed and had no entry at all for the invisibility potion.</p>
 */
class AspectBonusParityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static AspectList bonus(ItemStack stack) {
        return ObjectAspectResolver.bonusTags(stack);
    }

    @Test void everySwordCarriesTheOriginalConstantWeaponBonus() {
        // Oracle getBonusTags: the sword branch merges WEAPON with
        // (int)(ItemSword.func_150931_i() + 1.0F). The 1.7.10 vanilla ItemSword returns the
        // constant base damage 4.0F for every sword, so every sword gets five.
        for (ItemStack sword : new ItemStack[]{
                new ItemStack(Items.WOODEN_SWORD), new ItemStack(Items.STONE_SWORD),
                new ItemStack(Items.IRON_SWORD), new ItemStack(Items.GOLDEN_SWORD),
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.NETHERITE_SWORD)}) {
            AspectList tags = bonus(sword);
            assertEquals(5, tags.getAmount(Aspect.WEAPON), sword.getItem().toString());
            assertEquals(1, tags.size(), sword.getItem().toString() + " carries only the weapon bonus");
        }
        assertEquals(5, ObjectAspectResolver.SWORD_WEAPON_BONUS);
    }

    @Test void aHoeCarriesTheOriginalToolBonusAndNoHarvestBonus() {
        // Oracle getBonusTags: ItemHoe extends ItemTool in 1.7.10, so the tool branch merges TOOL
        // with the harvest level plus one and the shears/hoe branch is never reached.
        for (ItemStack hoe : new ItemStack[]{
                new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.STONE_HOE),
                new ItemStack(Items.IRON_HOE), new ItemStack(Items.DIAMOND_HOE)}) {
            AspectList tags = bonus(hoe);
            assertEquals(0, tags.getAmount(Aspect.HARVEST), hoe.getItem().toString() + " has no harvest bonus");
            assertEquals(1, tags.size(), hoe.getItem().toString() + " carries only the tool bonus");
        }
        assertEquals(1, bonus(new ItemStack(Items.WOODEN_HOE)).getAmount(Aspect.TOOL));
        assertEquals(2, bonus(new ItemStack(Items.STONE_HOE)).getAmount(Aspect.TOOL));
        assertEquals(3, bonus(new ItemStack(Items.IRON_HOE)).getAmount(Aspect.TOOL));
        assertEquals(4, bonus(new ItemStack(Items.DIAMOND_HOE)).getAmount(Aspect.TOOL));
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

    private static ItemStack withEnchantment(ItemStack stack, ResourceKey<Enchantment> key, int level) {
        // The 1.21.1 BuiltInRegistries has no ENCHANTMENT registry, the enchantments live in the
        // dynamic registry of the Registries.ENCHANTMENT key.
        Holder<Enchantment> holder = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
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
