/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.enchantment.core.rule;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumic.tinkerer.common.enchantment.core.IEnchantmentRule;

/**
 * The enchantment can only be applied next to the enchantments of a short list: the {@code canApplyTogether} of Flaming
 * Touch, Disintegrate, Dispersed Strikes, Final Strike, Focused Strikes and Valiance in 1.7.10 compared the other
 * enchantment with a handful of ids and refused every other, vanilla or from another mod.
 */
public class WhitelistCompatibilityRule implements IEnchantmentRule {

    private final Set<ResourceKey<Enchantment>> allowed;

    public WhitelistCompatibilityRule(Set<ResourceKey<Enchantment>> allowed) {
        this.allowed = Set.copyOf(allowed);
    }

    @Override
    public boolean cantApplyAlongside(List<ResourceKey<Enchantment>> enchantments) {
        for (ResourceKey<Enchantment> other : enchantments) {
            if (!allowed.contains(other)) return true;
        }
        return false;
    }

    public Set<ResourceKey<Enchantment>> allowed() {
        return allowed;
    }
}
