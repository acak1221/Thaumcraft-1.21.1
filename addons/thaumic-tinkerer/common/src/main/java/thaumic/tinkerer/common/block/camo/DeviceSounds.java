/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.block.camo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** The sounds the devices of the TileBlocks group play (Thaumcraft's own sound events, looked up by id). */
public final class DeviceSounds {
    /** {@code thaumcraft:tool}: the click of a wand on a device (1.7.10 {@code playSoundEffect("thaumcraft:tool", 0.6, 1)}). */
    public static final SoundEvent WAND_TOOL = SoundEvent
            .createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "tool"));

    private DeviceSounds() {}
}
