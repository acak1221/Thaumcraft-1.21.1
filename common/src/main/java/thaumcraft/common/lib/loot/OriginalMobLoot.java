package thaumcraft.common.lib.loot;

import java.util.function.IntUnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Vanilla 1.7.10 EntityLivingBase death gate retained by TC4 custom rare-drop overrides. */
public final class OriginalMobLoot {
    private OriginalMobLoot() {}
    public static boolean rare(int looting, IntUnaryOperator random) { return random.applyAsInt(200) - looting < 5; }
    public static boolean brain(int looting, IntUnaryOperator random) { return random.applyAsInt(10) - looting <= 4; }
    /** Original death loot reads Looting from a player killer, never an arbitrary armed mob. */
    public static int looting(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof Player player)) return 0;
        return EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LOOTING), player);
    }
}
