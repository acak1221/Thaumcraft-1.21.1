package thaumcraft.common.items.relics;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Sanity Checker (original ItemSanityChecker), a relic of uncommon rarity that reads how much warp its holder carries.
 *
 * <p>The original 4.2.3.5 item only carries its rarity; the reading itself was added to the item in a later version of Thaumcraft, where
 * it shows the permanent, the sticky and the temporary warp together. The reading is kept here, as the relic is pointless without it.
 */
public final class ItemSanityChecker extends Item {
    public ItemSanityChecker(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** The warp the checker reads: the permanent, the sticky and the temporary warp together. */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var knowledge = KnowledgeAccess.get(player);
            int total = EquipmentRules.sanity(knowledge.getWarpPerm(), knowledge.getWarpSticky(), knowledge.getWarpTemp());
            player.displayClientMessage(Component.translatable(EquipmentRules.SANITY_MESSAGE, total), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The detailed reading of the three kinds of warp. */
    public static Component detail(int permanent, int sticky, int temporary) {
        return Component.translatable(EquipmentRules.SANITY_DETAIL_MESSAGE, permanent, sticky, temporary)
                .withStyle(ChatFormatting.GRAY);
    }
}
