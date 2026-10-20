package thaumcraft.fabric;

import net.fabricmc.api.ModInitializer;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftWorldgen;
import thaumcraft.common.config.ThaumcraftNodes;
import thaumcraft.common.config.ThaumcraftNaturals;
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
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftMonsters.BRAINY_ZOMBIE.get(),
                thaumcraft.common.entities.monster.EntityBrainyZombie.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get(),
                thaumcraft.common.entities.monster.EntityGiantBrainyZombie.createAttributes());
        BiomeModifications.addSpawn(BiomeSelectors.tag(thaumcraft.common.config.ThaumcraftMonsters.BRAINY_SPAWNS),
                net.minecraft.world.entity.MobCategory.MONSTER, thaumcraft.common.config.ThaumcraftMonsters.BRAINY_ZOMBIE.get(), 10, 1, 1);
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
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftWarp.MIND_SPIDER.get(),
                thaumcraft.common.entities.monster.EntityMindSpider.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                thaumcraft.common.config.ThaumcraftWarp.ELDRITCH_GUARDIAN.get(),
                thaumcraft.common.entities.monster.EntityEldritchGuardian.createAttributes());
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
    }
}
