package com.example.tc4addon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Scroll of Insight: research points and a hidden research entry for whoever has learned to read it. */
// [doc:scroll]
public class InsightScroll extends Item {
    public InsightScroll(Properties properties) { super(properties.stacksTo(16)); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        // The knowledge helpers read and change the server's copy and synchronise it to the player.
        if (!ThaumcraftApiHelper.isResearchComplete(player, "EXAMPLE_SCROLL")) {
            player.displayClientMessage(Component.translatable("tc4_example.insight_scroll.unreadable"), true);
            return InteractionResultHolder.fail(stack);
        }
        // What the scroll teaches is the data component of the stack, saved with it and synced to the client.
        AspectList taught = stack.getOrDefault(ExampleAddon.STORED_ASPECTS, StoredAspects.NONE).aspects();
        for (Aspect aspect : taught.getAspects()) ThaumcraftApiHelper.addAspectPool(player, aspect, taught.getAmount(aspect));
        ThaumcraftApiHelper.addAspectPool(player, ExampleAddon.TEMPUS, 1);
        // Completes the entry with its parents and pays its warp, as an original ResearchManager.completeResearch.
        if (!ThaumcraftApiHelper.isResearchComplete(player, "EXAMPLE_SECRET")) ThaumcraftApiHelper.completeResearch(player, "EXAMPLE_SECRET");
        player.displayClientMessage(Component.translatable("tc4_example.insight_scroll.read", ThaumcraftApiHelper.getWarpTotal(player)), false);
        stack.consume(1, player);
        return InteractionResultHolder.consume(stack);
    }
}
// [/doc]
