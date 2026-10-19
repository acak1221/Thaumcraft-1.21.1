package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteLearning;

/** Native normal/discovery identities preserve presentation separately from solved state. */
public final class ItemResearchNotes extends Item {
    public ItemResearchNotes(Properties properties) { super(properties.stacksTo(1)); }

    public static ResearchNoteData data(ItemStack stack) { return stack.get(ThaumcraftContent.RESEARCH_NOTE_DATA.get()); }

    public static ItemStack create(ResearchNoteData data, boolean discoveryPresentation) {
        var stack = new ItemStack(discoveryPresentation ? ThaumcraftContent.DISCOVERY.get() : ThaumcraftContent.RESEARCH_NOTE.get());
        stack.set(ThaumcraftContent.RESEARCH_NOTE_DATA.get(), data);
        return stack;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var note = data(stack);
        if (note == null) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            var result = ResearchNoteLearning.learn(KnowledgeAccess.get(player), note);
            if (result.success()) {
                // Original decrements even in creative; this is not shrinkInSurvival.
                stack.shrink(1);
                KnowledgeEvents.sync(serverPlayer);
                level.playSound(null, player.blockPosition(),
                        SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "learn")),
                        SoundSource.PLAYERS, 0.75F, 1.0F);
            } else if (result.failure() == ResearchNoteLearning.Failure.MISSING_PREREQUISITES) {
                serverPlayer.sendSystemMessage(Component.translatable("tc.researcherror"));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var data = data(stack);
        if (data == null) return;
        var research = ResearchCatalog.get(data.key());
        if (research == null) return;
        tooltip.add(Component.translatable(research.nameKey()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(research.textKey()).withStyle(ChatFormatting.ITALIC));
        if (research.warp() > 0) {
            // The original warning cap affects presentation, never the actual warp.
            tooltip.add(Component.translatable("tc.port.research.note_forbidden",
                    Component.translatable("tc.forbidden.level." + Math.min(5, research.warp())))
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
