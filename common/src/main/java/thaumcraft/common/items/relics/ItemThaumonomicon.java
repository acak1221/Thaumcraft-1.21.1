package thaumcraft.common.items.relics;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.OpenThaumonomiconPayload;
import thaumcraft.common.lib.research.ResearchBrowserData;

/** Normal TC4 book. Creative cheat-sheet metadata is not exposed by this slice. */
public final class ItemThaumonomicon extends Item {
    public ItemThaumonomicon(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchBrowserData.applyBookSiblingUnlocks(KnowledgeAccess.get(player));
            KnowledgeEvents.sync(serverPlayer);
            NetworkManager.sendToPlayer(serverPlayer, OpenThaumonomiconPayload.INSTANCE);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
