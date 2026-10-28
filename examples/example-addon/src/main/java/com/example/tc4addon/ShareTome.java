package com.example.tc4addon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;

/** Tome of Sharing: use it on another player and they learn what you scanned and which aspects you know. */
// [doc:share-tome]
public class ShareTome extends Item {
    public ShareTome(Properties properties) { super(properties.stacksTo(1)); }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        // The helpers that change knowledge are for the server's player; a client answers PASS and the server decides.
        if (!(player instanceof ServerPlayer teacher) || !(target instanceof ServerPlayer student)) return InteractionResult.PASS;
        // A book is read by someone who has seen one: the thaumometer's record of scanned items.
        if (!ThaumcraftApiHelper.hasScannedItem(teacher, new ItemStack(Items.BOOK))) {
            teacher.displayClientMessage(Component.translatable("tc4_example.share_tome.unread"), true);
            return InteractionResult.FAIL;
        }
        // The scans travel as an immutable snapshot of the mod's own markers; the number is how many were new to the student.
        int scans = ThaumcraftApiHelper.addScans(student, ThaumcraftApiHelper.getScans(teacher));
        // Aspects are learned quietly (no discovery notice, no research points: the pools stay where they are).
        int aspects = 0;
        for (Aspect aspect : ThaumcraftApiHelper.getDiscoveredAspects(teacher).getAspects()) {
            if (ThaumcraftApiHelper.discoverAspect(student, aspect)) aspects++;
        }
        teacher.displayClientMessage(Component.translatable("tc4_example.share_tome.shared", scans, aspects, student.getDisplayName()), true);
        return InteractionResult.SUCCESS;
    }
}
// [/doc]
