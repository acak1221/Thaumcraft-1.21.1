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

import dev.architectury.event.events.common.LootEvent;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The dimensional shards: an Enderman killed by a player in the End drops an Ender Shard once in 32 kills, a Zombified
 * Piglin killed by a player in the Nether a Nether Shard once in 16 (1.7.10 {@code LivingDropsEvent}).
 *
 * <p>Architectury has no event for the drops of a living entity, so the drop is a loot pool added to the two entity loot
 * tables ({@code LootEvent.MODIFY_LOOT_TABLE}), with the three conditions of the original: the damage came from a player
 * ({@code event.source.getEntity() instanceof EntityPlayer}: the attacker of the killing blow, an arrow's shooter
 * included, not "hurt by a player lately"), the entity is in the right dimension, and a random chance. A data pack that
 * replaces the loot table of the Enderman or the Piglin is modified as well.</p>
 */
public final class DimensionalShardDropHandler {
    /** 1.7.10: {@code Math.random() <= 1D / 32D}. */
    public static final float ENDER_SHARD_CHANCE = 1F / 32F;
    /** 1.7.10: {@code Math.random() <= 1D / 16D}. */
    public static final float NETHER_SHARD_CHANCE = 1F / 16F;

    public static final ResourceKey<LootTable> ENDERMAN = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.withDefaultNamespace("entities/enderman"));
    public static final ResourceKey<LootTable> ZOMBIFIED_PIGLIN = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.withDefaultNamespace("entities/zombified_piglin"));

    private DimensionalShardDropHandler() {}

    /** Registers the loot table hook (once, from the registrar). */
    public static void register() {
        LootEvent.MODIFY_LOOT_TABLE.register((id, context, builtin) -> {
            if (id.equals(ENDERMAN)) {
                addPool(context, LibItemNames.KAMI_RESOURCE_NAMES[7], Level.END, ENDER_SHARD_CHANCE);
            } else if (id.equals(ZOMBIFIED_PIGLIN)) {
                addPool(context, LibItemNames.KAMI_RESOURCE_NAMES[6], Level.NETHER, NETHER_SHARD_CHANCE);
            }
        });
    }

    /** Adds the pool; a shard that is not registered (the Kami gear failed to load) must not break the reload of the data packs. */
    private static void addPool(LootEvent.LootTableModificationContext context, String shardPath, ResourceKey<Level> dimension,
            float chance) {
        if (!TTRegistry.hasItem(shardPath)) {
            ThaumicTinkerer.LOG.warn("Kami: the item {} is not registered; its drop is not added to the loot tables", shardPath);
            return;
        }
        context.addPool(pool(TTRegistry.item(shardPath), dimension, chance));
    }

    /** One roll: the shard, when a player's blow killed the mob in that dimension, with the given chance. */
    public static LootPool.Builder pool(Item shard, ResourceKey<Level> dimension, float chance) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                .when(DamageSourceCondition.hasDamageSource(DamageSourcePredicate.Builder.damageType()
                        .source(EntityPredicate.Builder.entity().of(EntityType.PLAYER))))
                .when(LocationCheck.checkLocation(LocationPredicate.Builder.inDimension(dimension)))
                .when(LootItemRandomChanceCondition.randomChance(chance)).add(LootItem.lootTableItem(shard));
    }
}
