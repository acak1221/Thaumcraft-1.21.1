package thaumcraft.common.items.relics;

import dev.architectury.networking.NetworkManager;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.OpenThaumonomiconPayload;
import thaumcraft.common.lib.research.ResearchBrowserData;
import thaumcraft.common.lib.research.ResearchCheatSheet;

/** Original normal/metadata42 books, flattened into independent native item identities. */
public final class ItemThaumonomicon extends Item {
    private final boolean cheatSheet;
    public ItemThaumonomicon(Properties properties) { this(properties, false); }
    public ItemThaumonomicon(Properties properties, boolean cheatSheet) {
        super(properties.stacksTo(1).rarity(cheatSheet ? Rarity.EPIC : Rarity.UNCOMMON)); this.cheatSheet = cheatSheet;
    }
    public boolean isCheatSheet() { return cheatSheet; }
    @Override public String getDescriptionId() { return "item.thaumcraft.thaumonomicon"; } // Original metadata42 shares the book's name.
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (cheatSheet) tooltip.add(Component.literal("Cheat Sheet"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || held.getItem() != this || player.level() != level) return InteractionResultHolder.fail(held);
        if (player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.server.isSameThread()) throw new IllegalStateException("Thaumonomicon use requires the server thread");
            var knowledge = KnowledgeAccess.get(player); var settings = ThaumcraftSettings.server();
            if (cheatSheet && settings.cheatSheet()) ResearchCheatSheet.grant(knowledge, settings);
            else ResearchBrowserData.applyBookSiblingUnlocks(knowledge);
            KnowledgeEvents.sync(serverPlayer);
            NetworkManager.sendToPlayer(serverPlayer, OpenThaumonomiconPayload.INSTANCE);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
