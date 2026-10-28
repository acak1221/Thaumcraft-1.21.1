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
package thaumic.tinkerer.common.item.kami.wand;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.WandCap;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * The Ichorium Adorned wand cap: tag {@code ICHOR} (research {@code CAP_ICHOR}), a 30% vis discount on everything, assembly
 * cost 10. The constructor registers the cap with Thaumcraft; it has to run once, when Thaumcraft is ready.
 */
public class CapIchor extends WandCap {
    public static final String TAG = "ICHOR";

    public CapIchor(ItemStack item) {
        super(TAG, 0.7F, item, 10);
        // 1.7.10 overrode getTexture(); the port's constructors derive a Thaumcraft path, so the texture is set.
        setTexture(LibMisc.id("textures/models/cap_ichor.png"));
    }
}
