package thaumcraft.common.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.mods.ChampionAccess;
import thaumcraft.common.lib.mods.ChampionHooks;

/**
 * The persistent champion modifier id of a mob. The 1.7 original kept it in the custom
 * tc.mobmod attribute, so it travelled with the entity in the entity nbt; the native
 * port keeps it in this one int field and writes it into the entity nbt, so the
 * champion survives a save and a load. The value {@code NOT_ROLLED} means that the
 * original spawn roll never reached this mob, exactly like the original attribute
 * value of -2.0 did.
 */
@Mixin(Mob.class)
public abstract class ChampionMobMixin implements ChampionAccess {
    /** The champion modifier id of the mob, {@code NOT_ROLLED} while it was never rolled. */
    @Unique
    private int thaumcraft$championModifier = ChampionHooks.NOT_ROLLED;

    @Override
    public int thaumcraft$getChampionModifier() { return thaumcraft$championModifier; }

    @Override
    public void thaumcraft$setChampionModifier(int modifier) { thaumcraft$championModifier = modifier; }

    /**
     * Writes the champion modifier id into the entity nbt. Entity declares its own
     * save and load methods abstract in 1.21.1, so the injection stands in the
     * concrete mob implementation that carries them.
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void thaumcraft$saveChampion(CompoundTag tag, CallbackInfo ci) {
        int modifier = thaumcraft$championModifier;
        if (modifier != ChampionHooks.NOT_ROLLED) tag.putInt(ChampionHooks.SAVE_KEY, modifier);
    }

    /** Reads the champion modifier id back from the entity nbt. */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void thaumcraft$loadChampion(CompoundTag tag, CallbackInfo ci) {
        int modifier = tag.contains(ChampionHooks.SAVE_KEY, Tag.TAG_INT) ? tag.getInt(ChampionHooks.SAVE_KEY) : ChampionHooks.NOT_ROLLED;
        ChampionHooks.setChampionModifier((Mob) (Object) this, modifier);
    }
}
