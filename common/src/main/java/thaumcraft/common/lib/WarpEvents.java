package thaumcraft.common.lib;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.EntityMindSpider;
import thaumcraft.common.items.armor.FortressRules;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.WarpEventNetworking;
import thaumcraft.common.lib.potions.FluxEffects;

/** Original WarpEvents dispatcher. No missing branch is replaced by an arbitrary vanilla mob/effect. */
public final class WarpEvents {
    private static boolean wussMode;
    private WarpEvents() {}
    public static void setWussMode(boolean value) { wussMode = value; }
    public static boolean isWussMode() { return wussMode; }

    public static void playerTick(ServerPlayer player) {
        if (!player.isAlive() || player.isRemoved()) return;
        if (WarpRules.scheduled(player.tickCount, wussMode, player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD)))) checkWarpEvent(player);
        if (player.tickCount % 10 == 0) checkDeathGaze(player);
    }

    /** A check is distinct from an accepted event: even a failed roll consumes one temporary warp. */
    public static WarpRules.Decision checkWarpEvent(ServerPlayer player) {
        return checkWarpEvent(player, player.serverLevel().random);
    }
    /** Explicit random source keeps the original conditional draws reproducible in native behavioral tests. */
    public static WarpRules.Decision checkWarpEvent(ServerPlayer player, RandomSource random) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Warp events require the server thread");
        var knowledge = KnowledgeAccess.get(player);
        int actual = knowledge.getWarpPerm() + knowledge.getWarpSticky();
        boolean mask = ItemFortressArmor.mask(player.getItemBySlot(EquipmentSlot.HEAD)) == FortressRules.MASK_DEVIL;
        var decision = WarpRules.decide(knowledge.getTotalWarp(), knowledge.getWarpCounter(), WarpGear.fromGear(player), mask, random::nextInt);
        var feedback = new java.util.ArrayList<net.minecraft.network.protocol.common.custom.CustomPacketPayload>();
        if (decision.triggered()) {
            knowledge.setWarpCounter(decision.remainingCounter());
            WarpEventNetworking.send(player, 0);
            apply(player, decision, random, feedback);
            for (String research : WarpRules.unlocks(actual, knowledge.getResearchComplete())) {
                if (research.equals("@BATHSALTS")) message(player, 8);
                else grantResearch(player, research.equals("ELDRITCHMINOR") ? 10 : 20, random, feedback);
                if (knowledge.addResearch(research)) {
                    feedback.add(new thaumcraft.common.lib.network.ResearchNoticePayload(research));
                    var definition = thaumcraft.common.lib.research.ResearchCatalog.get(research);
                    int addedWarp = definition == null ? 0 : definition.currentWarp();
                    if (!wussMode && addedWarp > 0) {
                        knowledge.addWarpPerm(addedWarp - addedWarp / 2);
                        knowledge.addWarpSticky(addedWarp / 2);
                        knowledge.setWarpCounter(knowledge.getTotalWarp());
                    }
                }
            }
        }
        knowledge.addWarpTemp(-1);
        KnowledgeEvents.sync(player);
        // Original per-draw payload totals/order were captured at the actual award, then sent after authoritative full sync.
        for (var packet : feedback) {
            if (packet instanceof thaumcraft.common.lib.network.AspectPoolPayload point) thaumcraft.common.lib.network.KnowledgeNotificationNetworking.pool(player, point);
            else if (packet instanceof thaumcraft.common.lib.network.ResearchNoticePayload notice) thaumcraft.common.lib.network.KnowledgeNotificationNetworking.research(player, notice.key());
            else if (packet instanceof thaumcraft.common.lib.network.WarpMessagePayload warp) thaumcraft.common.lib.network.WarpMessageNetworking.send(player, warp.kind(), warp.delta());
        }
        return decision;
    }

    private static void apply(ServerPlayer player, WarpRules.Decision decision, RandomSource random, List<net.minecraft.network.protocol.common.custom.CustomPacketPayload> feedback) {
        int warp = decision.effectiveWarp(), severity = decision.severity(), amplifier = WarpRules.amplifier(warp);
        switch (decision.event()) {
            case NONE -> {}
            case RESEARCH -> { grantResearch(player, 1, random, feedback); message(player, 3); }
            case NOISE -> message(player, 11);
            case VIS_EXHAUST -> { player.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 5000, amplifier, true, true))); message(player, 1); }
            case THAUMARHIA -> { player.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftWarp.effect(ThaumcraftWarp.THAUMARHIA), Math.min(32000, 10 * warp), 0, true, true))); message(player, 15); }
            case HUNGER -> { player.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER), severity <= 24 ? 5000 : 6000, amplifier, true, true))); message(player, 2); }
            case FOLLOWING -> message(player, 12);
            case MIST -> spawnMist(player, 1);
            case BLURRED -> player.addEffect(new MobEffectInstance(ThaumcraftWarp.effect(ThaumcraftWarp.BLURRED_VISION), Math.min(32000, 10 * warp), 0, true, true));
            case SUN_SCORNED -> { player.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftWarp.effect(ThaumcraftWarp.SUN_SCORNED), 5000, amplifier, true, true))); message(player, 5); }
            case MINING_FATIGUE -> { player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, amplifier, true, true)); message(player, 9); }
            case INFECTIOUS_EXHAUST -> { player.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftFlux.infectiousVisExhaust(), 6000, amplifier, false, true))); message(player, 1); }
            case NIGHT_VISION -> { player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Math.min(40 * warp, 6000), 0, true, true)); message(player, 10); }
            case DEATH_GAZE -> { player.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftWarp.effect(ThaumcraftWarp.DEATH_GAZE), 6000, amplifier, true, true))); message(player, 4); }
            case SPIDERS -> suddenlySpiders(player, warp, false);
            case WATCHING -> message(player, 13);
            case MIST_MANY -> spawnMist(player, severity <= 68 ? warp / 30 : warp / 15);
            case BLINDNESS -> player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(32000, 5 * warp), 0, true, true));
            case CLARITY -> {
                if (KnowledgeAccess.get(player).getWarpSticky() > 0) {
                    KnowledgeAccess.get(player).addWarpSticky(-1);
                    feedback.add(new thaumcraft.common.lib.network.WarpMessagePayload(-1, (byte) 1));
                }
                message(player, 14);
            }
            case RESEARCH_MANY -> { grantResearch(player, warp / 10, random, feedback); message(player, 3); }
            case REAL_SPIDERS -> suddenlySpiders(player, warp, true);
        }
    }
    private static void grantResearch(ServerPlayer player, int times, RandomSource random, List<net.minecraft.network.protocol.common.custom.CustomPacketPayload> feedback) {
        int count = 1 + random.nextInt(times);
        var primals = Aspect.getPrimalAspects();
        for (int i = 0; i < count; i++) {
            String tag = primals.get(random.nextInt(6)).getTag();
            var knowledge = KnowledgeAccess.get(player);
            knowledge.addAspectPool(tag, 1);
            feedback.add(new thaumcraft.common.lib.network.AspectPoolPayload(tag, (short) 1, (short) knowledge.getAspectPoolFor(tag)));
        }
    }
    private static void message(ServerPlayer player, int id) { player.sendSystemMessage(Component.translatable("warp.text." + id).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)); }
    private static void spawnMist(ServerPlayer player, int guardians) {
        WarpEventNetworking.send(player, 1);
        for (int i = 0; i < Math.min(8, guardians); i++) {
            var guardian = ThaumcraftWarp.ELDRITCH_GUARDIAN.get().create(player.serverLevel());
            if (guardian != null && placeNear(player, guardian)) { guardian.setTarget(player); player.serverLevel().addFreshEntity(guardian); }
        }
        message(player, 6);
    }
    private static void suddenlySpiders(ServerPlayer player, int warp, boolean real) {
        for (int i = 0; i < Math.min(50, warp); i++) {
            var spider = ThaumcraftWarp.MIND_SPIDER.get().create(player.serverLevel());
            if (spider == null || !placeNear(player, spider)) continue;
            spider.setTarget(player);
            if (!real) { spider.setViewer(player.getGameProfile().getName()); spider.setHarmless(true); }
            player.serverLevel().addFreshEntity(spider);
        }
        message(player, 7);
    }
    private static boolean placeNear(ServerPlayer player, Mob mob) {
        ServerLevel level = player.serverLevel(); RandomSource random = level.random; BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 50; attempt++) {
            int x = origin.getX() + (7 + random.nextInt(18)) * (random.nextInt(3) - 1);
            int y = origin.getY() + (7 + random.nextInt(18)) * (random.nextInt(3) - 1);
            int z = origin.getZ() + (7 + random.nextInt(18)) * (random.nextInt(3) - 1);
            BlockPos pos = new BlockPos(x, y, z);
            // Do not force-load unknown terrain merely to attempt an ambient event.
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)
                    || !level.getBlockState(pos.below()).isSolidRender(level, pos.below())) continue;
            mob.setPos(x, y, z);
            if (level.isUnobstructed(mob) && level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox())) return true;
        }
        return false;
    }
    public static void checkDeathGaze(ServerPlayer player) {
        var effect = player.getEffect(ThaumcraftWarp.effect(ThaumcraftWarp.DEATH_GAZE));
        if (effect == null) return;
        int range = WarpRules.gazeRange(effect.getAmplifier());
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))) {
            if (target == player || !target.isAlive() || !target.isPickable() || target.hasEffect(MobEffects.WITHER)
                    || target instanceof Player other && !player.server.isPvpAllowed() || !player.hasLineOfSight(target)) continue;
            Vec3 delta = target.getBoundingBox().getCenter().subtract(eye);
            if (!WarpRules.inGazeCone(delta.dot(look), delta.length(), range)) continue;
            target.setLastHurtByMob(player);
            if (target instanceof Mob mob) mob.setTarget(player);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
        }
    }
    public static void addTemporaryWarp(ServerPlayer player, int amount) {
        var knowledge = KnowledgeAccess.get(player);
        knowledge.addWarpTemp(amount); knowledge.setWarpCounter(knowledge.getTotalWarp()); KnowledgeEvents.sync(player);
        thaumcraft.common.lib.network.WarpMessageNetworking.send(player, (byte) 2, amount);
    }
    /** Original ItemCraftedEvent: one sticky amount for the output type, independent of its produced stack count. */
    public static int onCrafted(ServerPlayer player, ItemStack output) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Crafting warp requires the server thread");
        if (wussMode || output == null || output.isEmpty()) return 0;
        int amount = thaumcraft.api.ThaumcraftApi.getWarp(output);
        if (amount > 0) {
            var knowledge = KnowledgeAccess.get(player);
            knowledge.addWarpSticky(amount); knowledge.setWarpCounter(knowledge.getTotalWarp()); KnowledgeEvents.sync(player);
            thaumcraft.common.lib.network.WarpMessageNetworking.send(player, (byte) 1, amount);
        }
        return amount;
    }
    /** Original finish-food callback: flesh/brain shorten 600 ticks and one amplifier; other food merely warns. */
    public static void afterEating(ServerPlayer player, ItemStack before) {
        var effect = player.getEffect(ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER));
        if (effect == null) return;
        if (before.is(Items.ROTTEN_FLESH) || before.is(ThaumcraftContent.MATERIALS.get("zombie_brain").get())) {
            int duration = effect.getDuration() - 600, amplifier = effect.getAmplifier() - 1;
            player.removeEffect(ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER));
            if (duration > 0 && amplifier >= 0) player.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER), duration, amplifier, true, true)));
            player.sendSystemMessage(Component.translatable("warp.text.hunger.2").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        } else player.sendSystemMessage(Component.translatable("warp.text.hunger.1").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
