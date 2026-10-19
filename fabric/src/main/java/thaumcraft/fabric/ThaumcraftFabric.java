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
                thaumcraft.common.config.ThaumcraftGolems.GOLEM.get(),
                thaumcraft.common.entities.golems.EntityGolemBase.createAttributes());
        thaumcraft.fabric.bore.FabricBorePlatform.init();
        thaumcraft.fabric.golems.FabricGolemHarvestPlatform.init();
        thaumcraft.fabric.golems.FabricGolemUsePlatform.init();
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
