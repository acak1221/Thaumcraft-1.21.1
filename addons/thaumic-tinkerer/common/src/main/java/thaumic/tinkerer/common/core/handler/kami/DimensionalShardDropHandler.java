/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Dec 27, 2013, 11:05:53 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.handler.kami;

import dev.architectury.event.EventResult;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.hooks.TinkererEvents;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The dimensional shards: an Enderman killed by a player in the End drops an Ender Shard once in 32 kills, a Zombified
 * Piglin killed by a player in the Nether a Nether Shard once in 16 (1.7.10 {@code LivingDropsEvent}).
 *
 * <p>A listener of {@link TinkererEvents#LIVING_DROPS}, the {@code LivingDropsEvent} of the original, with its three
 * conditions: the damage came from a player ({@code event.source.getEntity() instanceof EntityPlayer}: the attacker of the
 * killing blow, an arrow's shooter included, not "hurt by a player lately"), the entity is of the right kind in the right
 * dimension ({@code instanceof}, so a subclass of the two mobs counts as it did), and a random chance. The shard is added to
 * the list of drops as a separate item entity at the mob's position, as the original added an {@code EntityItem}; what
 * the other listeners and the loot table dropped is left alone and Looting does not change the chance. The NeoForge poster of
 * the event does not exist yet: until it does no shard drops there.</p>
 */
public final class DimensionalShardDropHandler {
    /** 1.7.10: {@code Math.random() <= 1D / 32D}. */
    public static final double ENDER_SHARD_CHANCE = 1D / 32D;
    /** 1.7.10: {@code Math.random() <= 1D / 16D}. */
    public static final double NETHER_SHARD_CHANCE = 1D / 16D;

    /** The dice of the chance rolls ({@code Math.random()} in the original). Tests replace it to make a roll certain; nothing else does. */
    public static volatile DoubleSupplier dice = Math::random;

    private static boolean warnedMissingShard;

    private DimensionalShardDropHandler() {}

    /** Registers the listener (once, from the registrar). */
    public static void register() {
        TinkererEvents.LIVING_DROPS.register(DimensionalShardDropHandler::onLivingDrops);
    }

    /** 1.7.10 {@code onEntityLivingDrops}: a player's kill of an Enderman in the End or a Zombified Piglin in the Nether may add a shard. */
    public static EventResult onLivingDrops(LivingEntity victim, DamageSource source, List<ItemEntity> drops, int lootingLevel,
            boolean recentlyHit) {
        if (!(source.getEntity() instanceof Player)) return EventResult.pass();
        if (victim instanceof EnderMan && inDimension(victim, Level.END) && dice.getAsDouble() <= ENDER_SHARD_CHANCE) {
            addShard(drops, victim, LibItemNames.KAMI_RESOURCE_NAMES[7]);
        }
        if (victim instanceof ZombifiedPiglin && inDimension(victim, Level.NETHER) && dice.getAsDouble() <= NETHER_SHARD_CHANCE) {
            addShard(drops, victim, LibItemNames.KAMI_RESOURCE_NAMES[6]);
        }
        return EventResult.pass();
    }

    private static boolean inDimension(LivingEntity victim, ResourceKey<Level> dimension) {
        return victim.level().dimension() == dimension;
    }

    /** One shard at the victim's position; a shard that is not registered (the Kami gear failed to load) must not break a mob's death. */
    private static void addShard(List<ItemEntity> drops, LivingEntity victim, String shardPath) {
        if (!TTRegistry.hasItem(shardPath)) {
            if (!warnedMissingShard) {
                warnedMissingShard = true;
                ThaumicTinkerer.LOG.warn("Kami: the item {} is not registered; its drop is not added to the drops", shardPath);
            }
            return;
        }
        drops.add(new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), new ItemStack(TTRegistry.item(shardPath))));
    }
}
