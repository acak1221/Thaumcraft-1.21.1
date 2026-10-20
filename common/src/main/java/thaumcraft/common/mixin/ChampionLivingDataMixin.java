package thaumcraft.common.mixin;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.mods.ChampionAccess;
import thaumcraft.common.lib.mods.ChampionRules;

/**
 * The synced mirror of the original champion modifier id. The 1.7 original read the
 * champion modifier from a custom entity attribute, which the client never received,
 * so the original client particle hook could not fire there. The native port keeps
 * the modifier id in the mob's own field and sends it to the client with the entity
 * data, which is the native way to tell the client that a mob is a champion.
 */
@Mixin(LivingEntity.class)
public abstract class ChampionLivingDataMixin {
    /** Adds the champion modifier id to the entity data of every living entity. */
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void thaumcraft$defineChampionData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(ChampionAccess.CHAMPION_DATA, ChampionRules.NO_CHAMPION);
    }
}
