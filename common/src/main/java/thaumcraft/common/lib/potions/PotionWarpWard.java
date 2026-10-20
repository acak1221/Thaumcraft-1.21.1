package thaumcraft.common.lib.potions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
/** Original beneficial marker that suspends the whole 2000-tick warp check, including temporary-warp decay. */
public final class PotionWarpWard extends MobEffect {
    public PotionWarpWard() { super(MobEffectCategory.BENEFICIAL, 14742263); }
}
