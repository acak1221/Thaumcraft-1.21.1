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
package thaumic.tinkerer.common.enchantment.core;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.AspectList;

/**
 * What the Enchanter knows about one level of one enchantment: the icon of its button, the vis it costs at that level
 * and the research the player needs. 1.7.10 stored one of these per enchantment id and level; the port builds them from the
 * table of {@link EnchantmentManager} on demand, because the highest level of an enchantment is only known to the registry
 * of a running world (the data of a data pack).
 */
public class EnchantmentData {

    public final ResourceLocation texture;
    public final AspectList aspects;
    public final boolean vanilla;
    public final String research;

    public EnchantmentData(String texture, boolean vanilla, AspectList aspects) {
        this(texture, vanilla, aspects, "");
    }

    public EnchantmentData(String texture, boolean vanilla, AspectList aspects, String research) {
        this.texture = ResourceLocation.parse(texture);
        this.vanilla = vanilla;
        this.aspects = aspects;
        this.research = research;
    }
}
