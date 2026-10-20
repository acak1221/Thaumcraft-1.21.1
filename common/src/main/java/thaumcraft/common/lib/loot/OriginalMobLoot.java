package thaumcraft.common.lib.loot;

import java.util.function.IntUnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Vanilla 1.7.10 EntityLivingBase death gate retained by TC4 custom rare-drop overrides. */
public final class OriginalMobLoot {
    private OriginalMobLoot() {}
    public static boolean rare(int looting, IntUnaryOperator random) { return random.applyAsInt(200) - looting < 5; }
    public static boolean brain(int looting, IntUnaryOperator random) { return random.applyAsInt(10) - looting <= 4; }
    /** Old EntityItem motion used Math.random, not world/entity gameplay RNG. The modern convenience constructor
     * consumes two world draws and would alter the NEXT flesh/brain roll inside a death-drop loop. */
    public static ItemEntity spawn(Entity source, ItemStack stack, float height) {
        if (stack.isEmpty() || !(source.level() instanceof ServerLevel level)) return null;
        var item = new ItemEntity(level, source.getX(), source.getY() + height, source.getZ(), stack,
                (float) (Math.random() * (double) .2F - (double) .1F), .2F,
                (float) (Math.random() * (double) .2F - (double) .1F));
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        return item;
    }
    /** Original death loot reads Looting from a player killer, never an arbitrary armed mob. */
    public static int looting(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof Player player)) return 0;
        return EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LOOTING), player);
    }
}
