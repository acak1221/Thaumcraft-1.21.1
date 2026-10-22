package thaumcraft.fabric;

import net.fabricmc.api.ModInitializer;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftWorldgen;
import thaumcraft.common.config.ThaumcraftNodes;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.config.ThaumcraftVillages;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.levelgen.GenerationStep;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.blocks.BlockTube;

public final class ThaumcraftFabric implements ModInitializer {
    @Override public void onInitialize() {
        Thaumcraft.init();
        thaumcraft.fabric.fluid.FabricSpaStorage.init();
        // The loader independent fluid handler API of the golem (oracle GolemHelper:341-446).
        thaumcraft.fabric.fluid.FabricGolemFluidAccess.init();
        // Original TileCrucible:373-410 - the water tank of the crucible that other mods may read and fill.
        thaumcraft.fabric.fluid.FabricCrucibleStorage.init();
        thaumcraft.fabric.loot.FabricThaumcraftLoot.init();
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftPech.PECH.get(), thaumcraft.common.entities.monster.EntityPech.createAttributes());
        net.minecraft.world.entity.SpawnPlacements.register(thaumcraft.common.config.ThaumcraftPech.PECH.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                thaumcraft.common.entities.monster.EntityPech::checkPechSpawnRules);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftMonsters.FIREBAT.get(), thaumcraft.common.entities.monster.EntityFireBat.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftMonsters.WISP.get(), thaumcraft.common.entities.monster.EntityWisp.createAttributes());
        net.minecraft.world.entity.SpawnPlacements.register(thaumcraft.common.config.ThaumcraftMonsters.FIREBAT.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                thaumcraft.common.entities.monster.EntityFireBat::checkFireBatSpawnRules);
        net.minecraft.world.entity.SpawnPlacements.register(thaumcraft.common.config.ThaumcraftMonsters.WISP.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                thaumcraft.common.entities.monster.EntityWisp::checkWispSpawnRules);
        thaumcraft.fabric.world.FabricBiomeSpawns.init();
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftTaint.TAINT_SPIDER.get(), thaumcraft.common.entities.monster.EntityTaintSpider.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftTaint.TAINT_SPORE.get(), thaumcraft.common.entities.monster.EntityTaintSpore.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftTaint.TAINT_SWARMER.get(), thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftTaint.TAINT_SWARM.get(), thaumcraft.common.entities.monster.EntityTaintSwarm.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftTaint.TAINTACLE.get(), thaumcraft.common.entities.monster.EntityTaintacle.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftTaint.TAINTACLE_SMALL.get(), thaumcraft.common.entities.monster.EntityTaintacleSmall.createAttributes());
        net.minecraft.world.entity.SpawnPlacements.register(thaumcraft.common.config.ThaumcraftTaint.TAINT_SWARM.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                thaumcraft.common.entities.monster.EntityTaintSwarm::checkTaintSwarmSpawnRules);
        net.minecraft.world.entity.SpawnPlacements.register(thaumcraft.common.config.ThaumcraftTaint.TAINTACLE.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                thaumcraft.common.entities.monster.EntityTaintacle::checkSpawnRules);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftMonsters.BRAINY_ZOMBIE.get(),
                thaumcraft.common.entities.monster.EntityBrainyZombie.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get(),
                thaumcraft.common.entities.monster.EntityGiantBrainyZombie.createAttributes());
        net.minecraft.world.entity.SpawnPlacements.register(thaumcraft.common.config.ThaumcraftMonsters.BRAINY_ZOMBIE.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                thaumcraft.common.entities.monster.EntityBrainyZombie::checkBrainySpawnRules);
        net.minecraft.world.entity.SpawnPlacements.register(thaumcraft.common.config.ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                thaumcraft.common.entities.monster.EntityBrainyZombie::checkBrainySpawnRules);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftGolems.GOLEM.get(),
                thaumcraft.common.entities.golems.EntityGolemBase.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftGolems.TRUNK.get(),
                thaumcraft.common.entities.golems.EntityTravelingTrunk.createAttributes());
        thaumcraft.fabric.bore.FabricBorePlatform.init();
        thaumcraft.fabric.golems.FabricGolemHarvestPlatform.init();
        thaumcraft.fabric.golems.FabricGolemUsePlatform.init();
        // Oracle EventHandlerWorld:225-245 - the original cancelled a block placement next to an
        // active eldritch boss of the Outer Lands.
        FabricBossPlacement.register();
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftWarp.MIND_SPIDER.get(),
                thaumcraft.common.entities.monster.EntityMindSpider.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftWarp.ELDRITCH_GUARDIAN.get(),
                thaumcraft.common.entities.monster.EntityEldritchGuardian.createAttributes());
        // The eldritch bestiary owns its attributes in common; the loader owns the registry.
        thaumcraft.common.config.ThaumcraftEldritchMobs.registerAttributes(
                (type, builder) -> net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                        (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity>) type, builder));
        thaumcraft.common.config.ThaumcraftCultists.registerAttributes(
                (type, builder) -> net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                        (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity>) type, builder));
        thaumcraft.common.config.ThaumcraftTaintCreeper.registerAttributes(
                (type, builder) -> net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                        (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity>) type, builder));
        thaumcraft.common.config.ThaumcraftTaintAnimals.registerAttributes(
                (type, builder) -> net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                        (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity>) type, builder));
        thaumcraft.common.lib.world.TradeSwaps.install((level, player, pos, before, after) -> {
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            return UseBlockCallback.EVENT.invoker().interact(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS
                    && net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, before, null)
                    && !dev.architectury.event.events.common.BlockEvent.PLACE.invoker().placeBlock(level, pos, after, player).isFalse();
        });
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, level, hand, pos, face) -> {
            var wand = player.getItemInHand(hand);
            if (!(thaumcraft.common.items.wands.foci.FocusData.get(wand).getItem() instanceof thaumcraft.common.items.wands.foci.ItemFocusTrade))
                return InteractionResult.PASS;
            if (player instanceof net.minecraft.server.level.ServerPlayer server)
                thaumcraft.common.items.wands.foci.ItemFocusTrade.leftClick(server, wand, pos);
            return InteractionResult.SUCCESS;
        });
        // Forge onItemUseFirst preceded EVERY transport block, including addon
        // GUIs. Consume once here; common block dispatch and secondary use share
        // the same validated helper when called outside this Fabric callback.
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            var context = new UseOnContext(player, hand, hit);
            // Original golem deployment is item-use-first even against inventories.
            if (player.getItemInHand(hand).getItem() instanceof thaumcraft.common.items.golems.ItemGolemPlacer placer) {
                InteractionResult placed = placer.useOn(context);
                if (placed != InteractionResult.PASS) return placed;
            }
            // Original bell onItemUseFirst marks the clicked face instead of opening that chest.
            if (player.getItemInHand(hand).getItem() instanceof thaumcraft.common.items.golems.ItemGolemBell bell) {
                InteractionResult marked = bell.useOn(context);
                if (marked != InteractionResult.PASS) return marked;
            }
            InteractionResult inspect = ItemResonator.tryInspect(context);
            if (inspect != InteractionResult.PASS) return inspect;
            // Modern secondary use skips block activation even for a plain
            // stick. Original filter sneak-removal preceded attachment, except
            // item-first tools; preserve that without bypassing wand rotation.
            return player.isShiftKeyDown() && !(player.getItemInHand(hand).getItem() instanceof ItemWandCasting)
                    ? BlockTube.tryLabelInteraction(context) : InteractionResult.PASS;
        });
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.UNDERGROUND_ORES, ThaumcraftWorldgen.ORES_KEY);
        BiomeModifications.addFeature(BiomeSelectors.tag(ThaumcraftNodes.NODE_BIOMES),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ThaumcraftNodes.NODE_KEY);
        BiomeModifications.addFeature(BiomeSelectors.tag(ThaumcraftNaturals.TREE_BIOMES),
                GenerationStep.Decoration.VEGETAL_DECORATION, ThaumcraftNaturals.TREES_KEY);
        // The TC4 surface structures of the exclusive chain (oracle
        // ThaumcraftWorldGenerator:429-453) and the two village components. 1.21.1 has
        // no biome modifier registry, so every loader wires the placed features of the
        // shared datapack into its own biomes. The original world generator ran after
        // the vanilla decoration, so the structures share the last step with the wild
        // aura nodes.
        BiomeModifications.addFeature(BiomeSelectors.tag(ThaumcraftVillages.SURFACE_STRUCTURE_BIOMES),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ThaumcraftOuterLands.RING_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.tag(ThaumcraftVillages.SURFACE_STRUCTURE_BIOMES),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ThaumcraftVillages.MOUND_KEY);
        BiomeModifications.addFeature(BiomeSelectors.tag(ThaumcraftVillages.SURFACE_STRUCTURE_BIOMES),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ThaumcraftVillages.HILLTOP_STONES_KEY);
        BiomeModifications.addFeature(BiomeSelectors.tag(ThaumcraftVillages.SURFACE_STRUCTURE_BIOMES),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ThaumcraftVillages.BANKER_HOMES_KEY);
        BiomeModifications.addFeature(BiomeSelectors.tag(ThaumcraftVillages.SURFACE_STRUCTURE_BIOMES),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ThaumcraftVillages.WIZARD_TOWERS_KEY);
        // The aura totem of the original world generator, one chunk in 360.
        BiomeModifications.addFeature(BiomeSelectors.tag(ThaumcraftVillages.SURFACE_STRUCTURE_BIOMES),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ThaumcraftNodes.TOTEM_KEY);
    }
}
