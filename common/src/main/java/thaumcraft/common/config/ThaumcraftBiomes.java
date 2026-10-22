package thaumcraft.common.config;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockCustomPlant;
import thaumcraft.common.blocks.BlockVishroom;
import thaumcraft.common.lib.world.ThaumcraftBiomeDecorationFeature;

/** Actual dynamic biomes are supplied by native worldgen/biome resources.
 * Eerie is converted by DARK ecology; it has no natural climate ticket. */
public final class ThaumcraftBiomes {
    public static final ResourceKey<Biome> MAGICAL_FOREST=biome("magical_forest");
    public static final ResourceKey<Biome> EERIE=biome("eerie");
    public static final ResourceKey<Biome> TAINT=biome("taint");
    /** The Outer Lands biome of the original: the single biome of the eldritch dimension. */
    public static final ResourceKey<Biome> ELDRITCH_LANDS=biome("eldritch_lands");
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Thaumcraft.MODID,Registries.FEATURE);
    public static final RegistrySupplier<BlockVishroom> VISHROOM=BLOCKS.register("vishroom",()->new BlockVishroom(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
        .noCollission().noOcclusion().instabreak().sound(SoundType.GRASS).randomTicks().replaceable().lightLevel(state->8)));
    // Oracle BlockCustomPlantItem:85-101 - the item of the metadata five (the manashroom) asks
    // the soil for a Cave plant.
    public static final RegistrySupplier<Item> VISHROOM_ITEM=ITEMS.register("vishroom",()->new BlockCustomPlant.PlantItem(VISHROOM.get(),new Item.Properties(),BlockCustomPlant.PlantType.CAVE));
    public static final RegistrySupplier<ThaumcraftBiomeDecorationFeature> DECORATION=FEATURES.register("tc4_biome_decoration",ThaumcraftBiomeDecorationFeature::new);
    public static final ResourceKey<PlacedFeature> DECORATION_KEY=ResourceKey.create(Registries.PLACED_FEATURE,id("tc4_biome_decoration"));
    private ThaumcraftBiomes() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID,path); }
    private static ResourceKey<Biome> biome(String path) { return ResourceKey.create(Registries.BIOME,id(path)); }
    public static void register() {
        BLOCKS.register();ITEMS.register();FEATURES.register();
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB,()->new ItemStack(VISHROOM.get()));
        LifecycleEvent.SETUP.register(()->((FireBlock)Blocks.FIRE).setFlammable(VISHROOM.get(),60,100));
    }
}
