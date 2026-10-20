package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileEldritchObelisk: the eldritch obelisk. Every eldritch mob that
 * comes close is slowed down and weakened.
 */
public final class TileEldritchObelisk extends BlockEntity {
    private int counter = 0;

    public TileEldritchObelisk(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(), pos, state);
    }

    /** Original TileEldritchObelisk:69-92 - the obelisk effect. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchObelisk obelisk) {
        obelisk.counter++;
        if (level.isClientSide) return;
        if (obelisk.counter % EldritchBlockRules.OBELISK_TICK_PERIOD != 0) return;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, EldritchBlockRules.obeliskBox(pos))) {
            if (!EldritchBlockRules.obeliskAffects(entity instanceof IEldritchMob, entity instanceof LivingEntity,
                    entity.hasEffect(MobEffects.DIG_SLOWDOWN))) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EldritchBlockRules.OBELISK_EFFECT_DURATION,
                    EldritchBlockRules.OBELISK_EFFECT_AMPLIFIER));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EldritchBlockRules.OBELISK_EFFECT_DURATION,
                    EldritchBlockRules.OBELISK_EFFECT_AMPLIFIER));
        }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        counter = tag.getInt("counter");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("counter", counter);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
