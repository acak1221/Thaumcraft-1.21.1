package thaumcraft.common.mixin;

import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.mods.ChampionData;
import thaumcraft.common.lib.mods.ChampionRules;

/**
 * The synced mirror of the original champion modifier id.
 *
 * <p>The id belongs to the living entity part of the entity tree, so only the living
 * entities define it: the entity data builder refuses to build an entity whose data ids
 * are not a dense sequence, and an id that the entity tree itself defined would have to be
 * defined by every item entity, projectile and vehicle as well. The id is created while the
 * living entity tree initialises, so the client and the server always agree on its
 * number, and it is defined for every living entity in the same place.</p>
 */
@Mixin(LivingEntity.class)
public abstract class ChampionEntityDataMixin {
    /**
     * Fixes the number of the champion data id. The entity tree numbers its data ids in
     * the order they are defined, so the id has to be created while the living entity tree
     * initialises, after its own ids took their numbers. The same static initialiser runs
     * on the client and on the server, so both sides know the same slot.
     */
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void thaumcraft$defineChampionData(CallbackInfo ci) {
        if (ChampionData.CHAMPION == null) {
            ChampionData.CHAMPION = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
        }
    }

    /** Adds the champion modifier id to the entity data of every living entity. */
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void thaumcraft$defineChampionData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        if (ChampionData.CHAMPION == null) {
            ChampionData.CHAMPION = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
        }
        builder.define(ChampionData.CHAMPION, ChampionRules.NO_CHAMPION);
    }
}
