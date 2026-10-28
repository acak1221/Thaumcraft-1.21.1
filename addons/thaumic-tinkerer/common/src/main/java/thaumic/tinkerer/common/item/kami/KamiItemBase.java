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
package thaumic.tinkerer.common.item.kami;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;

/**
 * Replaces {@code ItemKamiBase}: the base of the plain Kami items. 1.7.10 gave them {@code shouldRegister()} and
 * {@code shouldDisplayInTab()} from {@code ConfigHandler.enableKami}; the registrar registers nothing when the option
 * is off, so only the tab flag is left.
 *
 * <p>The 1.7.10 rarity {@code kamiRarity} ({@code EnumHelper.addEnum}, light purple, "Kami") cannot be created in
 * 1.21.1, where {@link Rarity} is a fixed enum; {@link Rarity#EPIC} is light purple as well, so the name of a Kami item
 * has the same colour (see {@code docs/deviations/KamiGear.md}).</p>
 */
public abstract class KamiItemBase extends Item implements ITTinkererRegisterable {
    /** The rarity that stands for 1.7.10's {@code TTCommonProxy.kamiRarity}. */
    public static final Rarity KAMI_RARITY = Rarity.EPIC;

    protected KamiItemBase(Item.Properties properties) {
        super(properties.rarity(KAMI_RARITY));
    }

    protected KamiItemBase(Item.Properties properties, Rarity rarity) {
        super(properties.rarity(rarity));
    }

    @Override
    public boolean shouldDisplayInTab() {
        return ConfigHandler.enableKami;
    }
}
