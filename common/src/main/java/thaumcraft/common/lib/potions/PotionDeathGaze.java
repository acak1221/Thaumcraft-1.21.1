package thaumcraft.common.lib.potions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
/** Passive original marker; the server player event owns the ten-tick gaze query. */
public final class PotionDeathGaze extends MobEffect {
    public PotionDeathGaze() { super(MobEffectCategory.HARMFUL, 0x664433); }
}
