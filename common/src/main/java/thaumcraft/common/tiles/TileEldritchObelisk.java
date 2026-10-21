package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileEldritchObelisk: the eldritch obelisk. It BUFFS every eldritch
 * mob that comes close: damage boost and regeneration, both for forty ticks.
 *
 * <p>Oracle TileEldritchObelisk.java:47-55 - the mob is skipped while it still
 * carries regeneration, and the pair of effects is handed out otherwise. The
 * original's counter is never raised, so the offer happens on every server
 * tick; the regeneration guard is what limits the effect to one pair every
 * forty ticks. The obelisk stores nothing, so it has no NBT of its own.</p>
 */
public final class TileEldritchObelisk extends BlockEntity {

    public TileEldritchObelisk(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(), pos, state);
    }

    /** Original TileEldritchObelisk:45-63 - the obelisk effect. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchObelisk obelisk) {
        if (level.isClientSide) return;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, EldritchBlockRules.obeliskBox(pos))) {
            if (!EldritchBlockRules.obeliskAffects(entity instanceof IEldritchMob, entity instanceof LivingEntity,
                    entity.hasEffect(MobEffects.REGENERATION))) continue;
            // Oracle TileEldritchObelisk:51-52 - damage boost first, then
            // regeneration, both ambient and forty ticks long.
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EldritchBlockRules.OBELISK_EFFECT_DURATION,
                    EldritchBlockRules.OBELISK_EFFECT_AMPLIFIER, true, true));
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EldritchBlockRules.OBELISK_EFFECT_DURATION,
                    EldritchBlockRules.OBELISK_EFFECT_AMPLIFIER, true, true));
        }
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    /**
     * Original TileEldritchObelisk:40-43 - the obelisk is five blocks high, so the
     * block entity renderer of the obelisk is only culled when the whole tower is
     * outside of the view.
     */
    @Override public AABB getRenderBoundingBox() {
        BlockPos pos = getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 5, pos.getZ() + 1);
    }
}
