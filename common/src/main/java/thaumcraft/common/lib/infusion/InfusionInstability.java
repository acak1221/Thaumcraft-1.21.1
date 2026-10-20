package thaumcraft.common.lib.infusion;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.potions.FluxEffects;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TilePedestal;

/** Every original Matrix:349-407 outcome, with real server effects rather than a cosmetic event catalogue. */
public final class InfusionInstability {
    private InfusionInstability() {}

    /** The matrix owns the one nextInt(21) roll; this method never rerolls or suppresses an outcome. */
    public static void apply(TileInfusionMatrix matrix, int eventId) {
        if (eventId < 0 || eventId > 20) throw new IllegalArgumentException("Unknown original infusion event " + eventId);
        if (!(matrix.getLevel() instanceof ServerLevel level)) return;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Infusion hazards require the server thread");
        BlockPos pos = matrix.getBlockPos();
        if (matrix.isRemoved() || !FluxFiniteBlock.loaded(level, pos) || level.getBlockEntity(pos) != matrix
                || !level.getBlockState(pos).is(ThaumcraftInfusion.MATRIX.get())) return;
        switch (eventId) {
            case 0, 2, 10, 13 -> eject(matrix, level, 0);
            case 6, 17 -> eject(matrix, level, 1);
            case 1, 11 -> eject(matrix, level, 2);
            case 19 -> eject(matrix, level, 3);
            case 7 -> eject(matrix, level, 4);
            case 4, 15 -> eject(matrix, level, 5);
            case 3, 8, 14 -> zap(matrix, level, false);
            case 12 -> zap(matrix, level, true);
            case 5, 16 -> harm(matrix, level, false);
            case 18 -> harm(matrix, level, true);
            case 9 -> explode(level, pos, 1.5F + level.random.nextFloat());
            case 20 -> warp(matrix, level);
            default -> throw new AssertionError(eventId);
        }
    }

    private static AABB area(TileInfusionMatrix matrix) { return new AABB(matrix.getBlockPos()).inflate(10); }

    private static void zap(TileInfusionMatrix matrix, ServerLevel level, boolean all) {
        // Native query order is retained. There is no random target choice,
        // nearest sorting, alive filtering, or weather/lightning entity here.
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area(matrix))) {
            matrix.sendEffect(3, victim.blockPosition(), victim.getId());
            victim.hurt(level.damageSources().magic(), 4 + level.random.nextInt(4));
            if (!all) break;
        }
    }

    private static void harm(TileInfusionMatrix matrix, ServerLevel level, boolean all) {
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area(matrix))) {
            if (level.random.nextBoolean()) {
                // Use the actual native registered holder, not RegistrySupplier-as-Holder.
                victim.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()), 120, 0, false, true));
            } else {
                victim.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 2400, 0, true, true)));
            }
            if (!all) break;
        }
    }

    private static void warp(TileInfusionMatrix matrix, ServerLevel level) {
        List<Player> players = level.getEntitiesOfClass(Player.class, area(matrix));
        if (players.isEmpty()) return;
        Player selected = players.get(level.random.nextInt(players.size()));
        boolean sticky = level.random.nextFloat() < .25F;
        int amount = sticky ? 1 : 1 + level.random.nextInt(5);
        var knowledge = KnowledgeAccess.get(selected);
        if (sticky) knowledge.addWarpSticky(amount);
        else knowledge.addWarpTemp(amount);
        knowledge.setWarpCounter(knowledge.getTotalWarp());
        if (selected instanceof ServerPlayer player) {
            KnowledgeEvents.sync(player);
            thaumcraft.common.lib.network.WarpMessageNetworking.send(player, (byte) (sticky ? 1 : 2), amount);
        }
    }

    private static void eject(TileInfusionMatrix matrix, ServerLevel level, int type) {
        List<BlockPos> pedestals = matrix.getPedestalPositions();
        for (int attempt = 0; attempt < 50 && !pedestals.isEmpty(); ++attempt) {
            BlockPos pos = pedestals.get(level.random.nextInt(pedestals.size()));
            if (!FluxFiniteBlock.loaded(level, pos) || !(level.getBlockEntity(pos) instanceof TilePedestal pedestal) || pedestal.isEmpty()) continue;
            if (type < 3 || type == 5) pedestal.dropContents();
            else pedestal.setInfusionStack(ItemStack.EMPTY); // Types3/4 really destroy the whole stack.

            BlockPos above = pos.above();
            if ((type == 1 || type == 3) && FluxFiniteBlock.loaded(level, above)) {
                FluxFiniteBlock goo = ThaumcraftFlux.FLUX_GOO.get();
                level.setBlock(above, goo.withGeometry(goo.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, 7), level, above), Block.UPDATE_ALL);
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, .3F, 1);
            } else if ((type == 2 || type == 4) && FluxFiniteBlock.loaded(level, above)) {
                FluxFiniteBlock gas = ThaumcraftFlux.FLUX_GAS.get();
                level.setBlock(above, gas.withGeometry(gas.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, 7), level, above), Block.UPDATE_ALL);
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .3F, 1);
            } else if (type == 5) {
                explode(level, pos, 1);
            }
            matrix.sendEffect(4, pos, type); // Root sends original pedestal sparkle + matrix-to-pedestal zap.
            return;
        }
    }

    private static void explode(ServerLevel level, BlockPos pos, float strength) {
        // Both Matrix:401 and :570 pass smoking=false: entity damage/knockback,
        // NO terrain damage or fire. Breaking a crafting matrix is owned separately.
        level.explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, strength, Level.ExplosionInteraction.NONE);
    }
}
