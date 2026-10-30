/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.fabric.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import thaumic.tinkerer.common.hooks.FloatingTicks;

/**
 * Makes the connection of a player implement {@link FloatingTicks.Access}: reads and writes the private
 * {@code aboveGroundTickCount} (the ticks a player has been floating, which kick him above 80 on a server without
 * {@code allow-flight}). The loader-neutral {@link FloatingTicks} does the rest.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerFloatingMixin implements FloatingTicks.Access {
    @Shadow
    private int aboveGroundTickCount;

    @Override
    public int thaumictinkerer$getAboveGroundTickCount() {
        return this.aboveGroundTickCount;
    }

    @Override
    public void thaumictinkerer$setAboveGroundTickCount(int ticks) {
        this.aboveGroundTickCount = ticks;
    }
}
