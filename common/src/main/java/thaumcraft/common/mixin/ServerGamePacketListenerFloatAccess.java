package thaumcraft.common.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The server's count of ticks a player has hovered, whose limit kicks for flying (original {@code Utils.resetFloatCounter}). */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerFloatAccess {
    @Accessor("aboveGroundTickCount")
    int thaumcraft$getAboveGroundTickCount();

    @Accessor("aboveGroundTickCount")
    void thaumcraft$setAboveGroundTickCount(int ticks);
}
