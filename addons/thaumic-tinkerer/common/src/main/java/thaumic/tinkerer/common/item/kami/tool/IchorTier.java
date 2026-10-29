/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami.tool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * {@code EnumHelper.addToolMaterial("ICHOR", 4, -1, 10F, 5F, 25)}: harvest level 4, no durability (the tools never
 * break), efficiency 10, damage bonus 5, enchantability 25.
 *
 * <p>Harvest level 4 is "every block can be harvested": the incorrect-for-drops tag is the empty tag of netherite. The
 * durability of -1 is not a value a 1.21.1 item can have; the tools carry the unbreakable component instead and
 * {@link #getUses()} is only the number the tool constructor reads.</p>
 */
public final class IchorTier implements Tier {
    public static final IchorTier INSTANCE = new IchorTier();

    private IchorTier() {}

    /** Properties of every Ichorium tool: the Kami rarity and no durability (the unbreakable component, tooltip hidden). */
    private static Item.Properties base() {
        return new Item.Properties().rarity(thaumic.tinkerer.common.item.kami.KamiItemBase.KAMI_RARITY)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false));
    }

    /**
     * The attack values are those of 1.7.10: the weapon modifier of a tool was its base (pickaxe 2, shovel 1, axe 3, sword 4)
     * plus the material's damage bonus 5, so a hit of the player (hand 1) dealt 8, 7, 9 and 10. The 1.21.1 vanilla bases (1, 1.5,
     * 5, 3) are not used. The attack speeds are the vanilla ones: 1.7.10 had no attack cooldown at all.
     */
    public static ItemAttributeModifiers pickaxeAttributes() {
        return DiggerItem.createAttributes(INSTANCE, 2.0F, -2.8F);
    }

    public static ItemAttributeModifiers shovelAttributes() {
        return DiggerItem.createAttributes(INSTANCE, 1.0F, -3.0F);
    }

    public static ItemAttributeModifiers axeAttributes() {
        return AxeItem.createAttributes(INSTANCE, 3.0F, -3.0F);
    }

    public static ItemAttributeModifiers swordAttributes() {
        return SwordItem.createAttributes(INSTANCE, 4, -2.4F);
    }

    public static Item.Properties pickaxeProperties() {
        return base().attributes(pickaxeAttributes());
    }

    public static Item.Properties shovelProperties() {
        return base().attributes(shovelAttributes());
    }

    public static Item.Properties axeProperties() {
        return base().attributes(axeAttributes());
    }

    public static Item.Properties swordProperties() {
        return base().attributes(swordAttributes());
    }

    @Override
    public int getUses() {
        return 0;
    }

    @Override
    public float getSpeed() {
        return 10F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 5F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 25;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
