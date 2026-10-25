package thaumcraft.common.items.wands;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** One native hand-selection rule shared by focus input and the casting dial. */
public final class WandHands {
    private WandHands() {}

    public static InteractionHand select(Player player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return null;
        if (player.getMainHandItem().getItem() instanceof ItemWandCasting) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof ItemWandCasting) return InteractionHand.OFF_HAND;
        return null;
    }
}
