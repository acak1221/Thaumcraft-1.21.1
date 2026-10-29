/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [14 Sep 2013, 15:15:33 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.enchantment.core.rule;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumic.tinkerer.common.enchantment.core.IEnchantmentRule;

/** The enchantment cannot be applied next to one particular other enchantment. */
public class BasicCompatibilityRule implements IEnchantmentRule {

    private final ResourceKey<Enchantment> illegal;

    public BasicCompatibilityRule(ResourceKey<Enchantment> illegal) {
        this.illegal = illegal;
    }

    @Override
    public boolean cantApplyAlongside(List<ResourceKey<Enchantment>> enchantments) {
        return enchantments.contains(illegal);
    }
}
