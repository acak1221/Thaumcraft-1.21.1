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
