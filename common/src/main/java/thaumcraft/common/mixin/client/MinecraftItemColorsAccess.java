package thaumcraft.common.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read the real native tint table; 1.21.1 exposes no public item-colors getter. */
@Mixin(Minecraft.class)
public interface MinecraftItemColorsAccess {
    @Accessor("itemColors") ItemColors thaumcraft$itemColors();
}
