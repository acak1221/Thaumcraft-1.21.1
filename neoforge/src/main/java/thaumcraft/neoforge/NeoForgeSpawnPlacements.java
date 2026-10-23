package thaumcraft.neoforge;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.monster.EntityTaintacle;
import thaumcraft.common.entities.monster.EntityTaintSwarm;
import thaumcraft.common.entities.monster.EntityWisp;

/**
 * NeoForge owner of the TC4 spawn placements.
 *
 * <p>Fabric calls the vanilla {@code SpawnPlacements.register}. That method is private in 1.21.1 and
 * NeoForge additionally deprecates it in favour of
 * {@link RegisterSpawnPlacementsEvent}; the NeoForge event is therefore the exact equivalent of the
 * Fabric call and registers the identical placement type, height map and predicate.</p>
 *
 * <p>The list is the exact Fabric list of the {@code thaumcraft.fabric.ThaumcraftFabric} bootstrap:
 * the brainy zombie, the giant brainy zombie, the fire bat, the wisp, the pech, the taint swarm and
 * the taintacle. Every other TC4 entity keeps the vanilla behaviour of an entity type without a
 * natural placement, exactly as on Fabric.</p>
 */
final class NeoForgeSpawnPlacements {
    private NeoForgeSpawnPlacements() {}

    static void register(IEventBus bus) {
        bus.addListener((RegisterSpawnPlacementsEvent event) -> {
            event.register(ThaumcraftPech.PECH.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityPech::checkPechSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ThaumcraftMonsters.FIREBAT.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityFireBat::checkFireBatSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ThaumcraftMonsters.WISP.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityWisp::checkWispSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ThaumcraftTaint.TAINT_SWARM.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityTaintSwarm::checkTaintSwarmSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ThaumcraftTaint.TAINTACLE.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityTaintacle::checkSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityBrainyZombie::checkBrainySpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityBrainyZombie::checkBrainySpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // Two TC4 entities carry a spawn entry in the biome data of the original
            // eerie biome but no placement of their own. Fabric leaves them without a
            // placement, and vanilla then lets them spawn anywhere. NeoForge reports
            // that state as an error and asks for an explicit placement, so the very
            // same unrestricted behaviour is published here.
            event.register(ThaumcraftWarp.ELDRITCH_GUARDIAN.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NeoForgeSpawnPlacements::unrestricted,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // The inhabited zombie is NOT unrestricted: it extends EntityZombie, whose 1.7.10
            // placement is ON_GROUND with a light check, and the spawn audit gave it exactly
            // that rule on Fabric. Only the guardian above is genuinely placement-free.
            event.register(ThaumcraftEldritchMobs.INHABITED_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    thaumcraft.common.entities.monster.EntityInhabitedZombie::checkInhabitedSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }

    /** The vanilla behaviour of an entity type without a registered placement. */
    private static boolean unrestricted(net.minecraft.world.entity.EntityType<?> type,
                                         net.minecraft.world.level.ServerLevelAccessor level,
                                         net.minecraft.world.entity.MobSpawnType reason,
                                         net.minecraft.core.BlockPos pos,
                                         net.minecraft.util.RandomSource random) {
        return true;
    }
}
