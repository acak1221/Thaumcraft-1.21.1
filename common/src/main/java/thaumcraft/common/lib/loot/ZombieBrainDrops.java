package thaumcraft.common.lib.loot;

import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;

/** EventHandlerEntity:462-463, finite vanilla Zombie family only. Root calls this
 * from native gated death loot, NEVER Architectury's cancellable LIVING_DEATH. */
public final class ZombieBrainDrops {
    private static final Set<EntityType<?>> TYPES = Set.of(EntityType.ZOMBIE, EntityType.HUSK,
            EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN);

    private ZombieBrainDrops() {}

    private static boolean supported(LivingEntity victim) {
        // A future Brainy Zombie has its own type and must not receive this
        // additional ordinary drop on top of its distinct custom brain rule.
        return victim instanceof Zombie && TYPES.contains(victim.getType());
    }

    /** Exact original world-RNG roll; caller supplies native recentlyHit and
     * effective Looting. Unsupported/ineligible calls consume no random draw. */
    public static boolean isBrainDrop(LivingEntity victim, boolean recentlyHit, int lootingLevel) {
        if (!supported(victim) || !recentlyHit || !(victim.level() instanceof ServerLevel level)
                || !level.getServer().isSameThread()) return false;
        return level.random.nextInt(10) - lootingLevel < 1;
    }

    /** Root bridge at LivingEntity.dropAllDeathLoot's already doMobLoot/shouldDropLoot
     * gated invocation of dropCustomDeathLoot(ServerLevel, DamageSource, boolean).
     * The boolean is that invocation's actual recent-hit argument, not killer != null.
     * The brain is the existing registered MATERIALS item, supplied by root. */
    public static void onNormalDeathLoot(LivingEntity victim, ServerLevel level, DamageSource source,
                                         boolean recentlyHit, ItemLike brain) {
        if (victim.level() != level || !level.getServer().isSameThread() || !victim.isDeadOrDying()
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT) || !supported(victim) || !recentlyHit) return;
        int looting = 0;
        if (source.getEntity() instanceof LivingEntity attacker) {
            var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
            looting = EnchantmentHelper.getEnchantmentLevel(enchantment, attacker);
        }
        if (isBrainDrop(victim, recentlyHit, looting)) {
            // Native loot emission/admission, not a hand-grant or a second loot
            // queue. One whole item at the original victim-eye-height position.
            victim.spawnAtLocation(new ItemStack(brain), victim.getEyeHeight());
        }
    }
}
