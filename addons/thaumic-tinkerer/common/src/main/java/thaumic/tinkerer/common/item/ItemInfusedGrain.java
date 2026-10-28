/**
 * Created by pixlepix on 4/22/14.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Imbued Grain: the bounty of the primal crops (aer, ignis, terra, aqua), the ingredient of the infused potions. The
 * four 1.7.10 metadata values are four items; all four are called "Imbued Grain" and name their aspect in the tooltip.
 * The grain is worth its aspect and one point of Messis to the scanner.
 */
public class ItemInfusedGrain extends Item implements IEssentiaContainerItem, ITTinkererRegisterable {

    /** The aspect of each variant, in the order of the 1.7.10 metadata. */
    public static final Aspect[] ASPECTS = { Aspect.AIR, Aspect.FIRE, Aspect.EARTH, Aspect.WATER };
    /** The suffix of the registry path of each variant. */
    public static final String[] SUFFIXES = { "aer", "ignis", "terra", "aqua" };
    /** The translation key all four share (the 1.7.10 {@code item.infusedGrain.name}). */
    public static final String NAME_KEY = "item.thaumictinkerer." + LibItemNames.INFUSED_GRAIN;

    private final int variant;

    public ItemInfusedGrain(int variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    /** The registry path of a variant: {@code infused_grain_aer} and so on. */
    public static String path(int variant) {
        return LibItemNames.INFUSED_GRAIN + "_" + SUFFIXES[variant];
    }

    /** 1.7.10 {@code getMetaForAspect}: the variant of an aspect, 0 for an aspect without grain. */
    public static int getMetaForAspect(Aspect aspect) {
        for (int i = 0; i < ASPECTS.length; i++) {
            if (aspect == ASPECTS[i]) {
                return i;
            }
        }
        return 0;
    }

    /** One grain of the given primal aspect (the registered item of its variant). */
    public static ItemStack stackFor(Aspect aspect) {
        return new ItemStack(TTRegistry.item(path(getMetaForAspect(aspect))));
    }

    public int getVariant() {
        return variant;
    }

    public Aspect getAspect(ItemStack stack) {
        return ASPECTS[variant];
    }

    @Override
    public String getDescriptionId() {
        return NAME_KEY;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return NAME_KEY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal(getAspect(stack).getName()));
    }

    @Override
    public AspectList getAspects(ItemStack itemStack) {
        return new AspectList().add(getAspect(itemStack), 1).add(Aspect.CROP, 1);
    }

    @Override
    public void setAspects(ItemStack itemStack, AspectList aspectList) {}
}
