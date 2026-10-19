package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IScribeTools;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchNoteEditing;
import thaumcraft.common.lib.research.ScribingInk;

/** Native R08 admission, staged material commit and post-commit authoritative effects. */
public final class ResearchTableEditNetworking {
    private static final SoundEvent LEARN = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "learn"));
    private static boolean initialized;
    private ResearchTableEditNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,
                ResearchTableEditPayload.TYPE, ResearchTableEditPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player, payload);
                }));
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(ResearchTableStatePayload.TYPE, ResearchTableStatePayload.STREAM_CODEC);
        } else {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                    ResearchTableStatePayload.TYPE, ResearchTableStatePayload.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> {
                        if (context.getPlayer() != null && context.getPlayer().containerMenu instanceof ContainerResearchTable menu) {
                            menu.applyState(payload);
                        }
                    }));
        }
    }

    /** Shared production receiver; direct real-loader checks are not a C2S dispatch claim. */
    public static boolean handle(ServerPlayer player, ResearchTableEditPayload payload) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Research table editing requires the server thread");
        if (!(player.containerMenu instanceof ContainerResearchTable menu) || menu.tile() == null) return false;
        var table = menu.tile();
        if (payload == null || payload.containerId() != menu.containerId || payload.sessionToken() != menu.sessionToken()) {
            return correct(player, menu);
        }
        if (!menu.stillValid(player)) return false; // No chunk loads or replacement-BE mutation.
        long revision = table.tableRevision(); // Count/components AND identity, not merely setter calls.
        if (revision == Long.MAX_VALUE || revision != payload.expectedTableRevision()) return correct(player, menu);

        ItemStack noteBefore = table.getItem(1).copy();
        ItemStack toolBefore = table.getItem(0).copy();
        var note = ItemResearchNotes.data(noteBefore);
        if (!noteBefore.is(ThaumcraftContent.RESEARCH_NOTE.get()) || note == null || note.complete()) return correct(player, menu);
        var knowledge = KnowledgeAccess.get(player);
        var stagedKnowledge = knowledge.copy();
        var ink = new ScribingInk.State(!toolBefore.isEmpty() && toolBefore.getItem() instanceof IScribeTools,
                toolBefore.getDamageValue(), toolBefore.getMaxDamage(), toolBefore.isDamageableItem());
        var before = new ResearchNoteEditing.State(note, false, ink, knowledge.getAspectsDiscovered(),
                knowledge.getResearchComplete(), table.bonusPools());
        ResearchNoteEditing.Result result;
        try {
            // The pure validator rejects ink/cell/type/discovery/funding before
            // calling this exact admitted world's RNG, including R2 free rolls.
            result = ResearchNoteEditing.evaluate(before,
                    new ResearchNoteEditing.Edit(payload.action(), payload.q(), payload.r(), payload.aspect()),
                    player.serverLevel().random::nextFloat);
            if (!result.success()) return correct(player, menu);

            ItemStack toolAfter = toolBefore.copy();
            if (!ScribingInk.consumeFromTable(toolAfter, true) || toolAfter.getDamageValue() != result.ink().afterDamage()) {
                throw new IllegalStateException("Table ink adapter disagrees with admitted edit plan");
            }
            // Actual mapped1.21.1 transmuteCopy retains count and the entire
            // component patch, including custom data/names and removals.
            ItemStack noteAfter = result.completesNote()
                    ? noteBefore.transmuteCopy(ThaumcraftContent.DISCOVERY.get()) : noteBefore.copy();
            noteAfter.set(ThaumcraftContent.RESEARCH_NOTE_DATA.get(), result.after().note());
            if (result.poolChange() != null) {
                // A non-null no-op debit still has the original save/correction
                // branch. Store raw after, never queriedTotal's signed short.
                stagedKnowledge.setAspectPool(result.poolChange().aspect(), result.poolChange().after());
            }
            // Pure planning/native stack copies make no world callbacks. A
            // changed revision here is an internal defect, not a post-roll
            // admission refusal or a reason to roll back shared world RNG.
            table.commitEdit(revision, toolAfter, noteAfter, result.after().bonusPools(), knowledge, stagedKnowledge);
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.error("Research table edit planning/commit defect at {} for {}", table.getBlockPos(), player.getUUID(), failure);
            throw failure;
        }

        // All material writes, including actor knowledge and the ONE revision
        // transition, are complete before block updates/listeners/network/sound.
        table.setChanged();
        if (result.poolChange() != null) KnowledgeEvents.sync(player);
        menu.broadcastChanges();
        for (ServerPlayer viewer : player.server.getPlayerList().getPlayers()) {
            if (viewer != player && viewer.containerMenu instanceof ContainerResearchTable other
                    && other.tile() == table && other.stillValid(viewer)) other.broadcastChanges();
        }
        long soundSeed = menu.sessionToken() ^ table.tableRevision() ^ table.getBlockPos().asLong();
        if (result.orbPitch() != null) {
            player.serverLevel().playSeededSound(null, player.getX(), player.getY(), player.getZ(),
                    Holder.direct(SoundEvents.EXPERIENCE_ORB_PICKUP), SoundSource.PLAYERS, 0.2F, result.orbPitch(), soundSeed);
        }
        if (result.completesNote()) {
            var pos = table.getBlockPos();
            player.serverLevel().playSeededSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    Holder.direct(LEARN), SoundSource.PLAYERS, 1.0F, 1.0F, soundSeed);
        }
        return true;
    }

    private static boolean correct(ServerPlayer player, ContainerResearchTable menu) {
        if (player.containerMenu == menu && menu.stillValid(player)) {
            KnowledgeEvents.sync(player);
            // Full slot correction BEFORE State resets the client's pending flag,
            // even when revision did not change (fixed/ink/funding refusal).
            menu.sendAllDataToRemote();
        }
        return false;
    }
}
