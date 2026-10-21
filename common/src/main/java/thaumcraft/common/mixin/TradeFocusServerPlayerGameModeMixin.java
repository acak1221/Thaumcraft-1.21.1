package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.items.wands.foci.ItemFocusTrade;

/**
 * Original {@code thaumcraft/common/items/wands/ItemWandCasting.java:583-590} hands the swing of a wand
 * to its focus, and original {@code thaumcraft/common/items/wands/foci/ItemFocusTrade.java:168-181} uses
 * that swing to swap the block under the crosshair with the block the focus remembered. Minecraft 1.21.1
 * has no item swing callback, so the start of a block break is the native equivalent of the original
 * left click: it is the packet the client sends for the very same gesture.
 *
 * <p>The swap itself is the existing {@link ItemFocusTrade#leftClick} path; this mixin only supplies the
 * gesture. The ordinary block break is not cancelled: the original also let the break continue while it
 * queued the swap.</p>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class TradeFocusServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void thaumcraft$tradeFocusLeftClick(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                                Direction direction, int maxY, int id, CallbackInfo callback) {
        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return;
        ItemFocusTrade.leftClick(player, player.getMainHandItem(), pos);
    }
}
