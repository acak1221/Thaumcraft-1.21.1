package thaumcraft.common.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.mods.ChampionAccess;

/**
 * The original champion modifier id was a custom entity attribute, so it travelled
 * with the entity and reached the client with the entity itself. The native port
 * keeps the id in the mob's own field and mirrors it into the entity's synced data.
 * The entity tree numbers its data ids per class branch in the order they are
 * defined, so the champion id has to be defined while Entity itself initialises,
 * before the living entity ids take their numbers. The same static initialiser runs
 * on the client and on the server, so both sides know the same slot.
 */
@Mixin(Entity.class)
public abstract class ChampionEntityDataMixin {
    /** Defines the champion data id while Entity initialises, never later. */
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void thaumcraft$defineChampionData(CallbackInfo ci) {
        // Reading the id defines it, the entity tree numbers its data ids in order.
        if (ChampionAccess.CHAMPION_DATA.id() < 0) throw new IllegalStateException("The champion data id is missing");
    }
}
