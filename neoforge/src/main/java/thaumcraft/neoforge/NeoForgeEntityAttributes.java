package thaumcraft.neoforge;

import java.util.function.BiConsumer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import thaumcraft.common.config.ThaumcraftCultists;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.config.ThaumcraftTaintCreeper;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityGiantBrainyZombie;
import thaumcraft.common.entities.monster.EntityMindSpider;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.monster.EntityTaintacle;
import thaumcraft.common.entities.monster.EntityTaintacleSmall;
import thaumcraft.common.entities.monster.EntityTaintSpider;
import thaumcraft.common.entities.monster.EntityTaintSpore;
import thaumcraft.common.entities.monster.EntityTaintSporeSwarmer;
import thaumcraft.common.entities.monster.EntityTaintSwarm;
import thaumcraft.common.entities.monster.EntityWisp;

/**
 * NeoForge owner of the TC4 entity attribute catalogue.
 *
 * <p>Fabric registers the same catalogue through {@code FabricDefaultAttributeRegistry}. NeoForge has
 * no such registry: the attributes of an entity type are supplied by
 * {@link EntityAttributeCreationEvent}. The list below is the exact Fabric list of the
 * {@code thaumcraft.fabric.ThaumcraftFabric} bootstrap, so both loaders spawn the same monsters with
 * the same health, damage and speed.</p>
 *
 * <p>The event fires after the entity type registry is frozen, so every {@code RegistrySupplier} is
 * resolved inside the listener and never during mod construction.</p>
 */
final class NeoForgeEntityAttributes {
    private NeoForgeEntityAttributes() {}

    static void register(IEventBus bus) {
        bus.addListener((EntityAttributeCreationEvent event) -> {
            // The nine direct catalogue entries of the Fabric bootstrap.
            put(event, ThaumcraftPech.PECH.get(), EntityPech.createAttributes());
            put(event, ThaumcraftMonsters.FIREBAT.get(), EntityFireBat.createAttributes());
            put(event, ThaumcraftMonsters.WISP.get(), EntityWisp.createAttributes());
            put(event, ThaumcraftTaint.TAINT_SPIDER.get(), EntityTaintSpider.createAttributes());
            put(event, ThaumcraftTaint.TAINT_SPORE.get(), EntityTaintSpore.createAttributes());
            put(event, ThaumcraftTaint.TAINT_SWARMER.get(), EntityTaintSporeSwarmer.createAttributes());
            put(event, ThaumcraftTaint.TAINT_SWARM.get(), EntityTaintSwarm.createAttributes());
            put(event, ThaumcraftTaint.TAINTACLE.get(), EntityTaintacle.createAttributes());
            put(event, ThaumcraftTaint.TAINTACLE_SMALL.get(), EntityTaintacleSmall.createAttributes());
            put(event, ThaumcraftMonsters.BRAINY_ZOMBIE.get(), EntityBrainyZombie.createAttributes());
            put(event, ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get(), EntityGiantBrainyZombie.createAttributes());
            put(event, ThaumcraftGolems.GOLEM.get(), EntityGolemBase.createAttributes());
            put(event, ThaumcraftGolems.TRUNK.get(), EntityTravelingTrunk.createAttributes());
            put(event, ThaumcraftWarp.MIND_SPIDER.get(), EntityMindSpider.createAttributes());
            put(event, ThaumcraftWarp.ELDRITCH_GUARDIAN.get(), EntityEldritchGuardian.createAttributes());
            // The four catalogues the shared module publishes for the loader adapter.
            putAll(event, ThaumcraftEldritchMobs::registerAttributes);
            putAll(event, ThaumcraftCultists::registerAttributes);
            putAll(event, ThaumcraftTaintCreeper::registerAttributes);
            putAll(event, ThaumcraftTaintAnimals::registerAttributes);
        });
    }

    private static void putAll(EntityAttributeCreationEvent event,
                               java.util.function.Consumer<BiConsumer<EntityType<?>, AttributeSupplier.Builder>> registrar) {
        registrar.accept((type, builder) -> put(event, type, builder));
    }

    @SuppressWarnings("unchecked")
    private static void put(EntityAttributeCreationEvent event, EntityType<?> type, AttributeSupplier.Builder builder) {
        if (type == null) {
            throw new IllegalStateException("TC4 entity type is missing and cannot own attributes");
        }
        event.put((EntityType<? extends LivingEntity>) type, builder.build());
    }
}
