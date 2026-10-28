/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Created by Katrina on 11/03/14.
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumic.tinkerer.common.core.helper.EnumMobAspect;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.research.IRegisterableResearch;

/**
 * An item that stands for a mob: it is shown as the mob, named as the mob, and is meant for recipe viewers (the output
 * of a necromancy recipe). It is not in the creative tab. The mob is the name of an {@link EnumMobAspect} in the key
 * {@code type} of the custom data, as in 1.7.10.
 */
public class ItemMobDisplay extends Item implements ITTinkererRegisterable {

    public static final String TAG_TYPE = "type";

    public ItemMobDisplay(Properties properties) {
        super(properties);
    }

    @Override
    public boolean shouldDisplayInTab() {
        return false;
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return null;
    }

    public EnumMobAspect getEntityType(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return EnumMobAspect.get(data == null ? "" : data.copyTag().getString(TAG_TYPE));
    }

    public void setEntityType(ItemStack stack, EnumMobAspect type) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TAG_TYPE, type.name()));
    }

    /** A stack of this item that displays the given mob. */
    public static ItemStack stackFor(EnumMobAspect type) {
        ItemStack stack = TTRegistry.stack(LibItemNames.MOB_DISPLAY);
        ((ItemMobDisplay) stack.getItem()).setEntityType(stack, type);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        EnumMobAspect aspect = getEntityType(stack);
        if (aspect == null) return super.getName(stack);
        return aspect.getDisplayName();
    }
}
