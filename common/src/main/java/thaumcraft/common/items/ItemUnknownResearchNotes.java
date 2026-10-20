package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.UnknownKnowledge;

/** Unknown metadata24/42 retain normal-note rendering, rare rarity and the ordinary completed-note branch's priority. */
public final class ItemUnknownResearchNotes extends ItemResearchNotes {
    private final int originalMetadata;
    public ItemUnknownResearchNotes(Properties properties, int originalMetadata) {
        super(properties);
        if (originalMetadata != 24 && originalMetadata != 42) throw new IllegalArgumentException("Unknown note variant");
        this.originalMetadata = originalMetadata;
    }
    public int originalMetadata() { return originalMetadata; }
    @Override public String getDescriptionId() { return ThaumcraftContent.RESEARCH_NOTE.get().getDescriptionId(); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            var note = data(stack);
            if (note != null && note.complete() && !KnowledgeAccess.get(player).isResearchComplete(note.key())) return super.use(level, player, hand);
            return InteractionResultHolder.consume(UnknownKnowledge.reveal(server, hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.researchnotes.unknown.1").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.researchnotes.unknown.2").withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
