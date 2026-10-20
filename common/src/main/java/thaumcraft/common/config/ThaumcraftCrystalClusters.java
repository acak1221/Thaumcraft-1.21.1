package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockCrystal;
import thaumcraft.common.blocks.BlockCrystalItem;
import thaumcraft.common.tiles.TileCrystal;

/** Original blockCrystal metadata0..6. Six existing primal IDs are retained; metadata6 is the real mixed cluster. */
public final class ThaumcraftCrystalClusters {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final SoundEvent CRYSTAL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "crystal"));
    public static final SoundType SOUND = new SoundType(1, 1, CRYSTAL, CRYSTAL, CRYSTAL, CRYSTAL, CRYSTAL);
    public static final List<String> TAGS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio");
    public static final RegistrySupplier<BlockCrystal> MIXED = BLOCKS.register("crystal_mixed", () -> new BlockCrystal(properties()));
    public static final RegistrySupplier<Item> MIXED_ITEM = ITEMS.register("crystal_mixed", () -> new BlockCrystalItem(MIXED.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileCrystal>> CRYSTAL_TILE = TILES.register("crystal_cluster", () ->
            BlockEntityType.Builder.of(TileCrystal::new, blocks().toArray(Block[]::new)).build(null));
    private static boolean registered;
    private ThaumcraftCrystalClusters() {}
    // Old setResistance(1) stored3; getExplosionResistance divided by5. Native strength takes the actual .6 resistance.
    public static BlockBehaviour.Properties properties() { return BlockBehaviour.Properties.of().strength(.7F, .6F).lightLevel(state -> 7).sound(SOUND).noOcclusion(); }
    public static List<Block> blocks() {
        var result = new ArrayList<Block>(); for (String tag : TAGS) result.add(ThaumcraftContent.CRYSTALS.get(tag).get()); result.add(MIXED.get()); return List.copyOf(result);
    }
    public static int metadata(BlockState state) {
        for (int metadata = 0; metadata < 6; metadata++) if (state.is(ThaumcraftContent.CRYSTALS.get(TAGS.get(metadata)).get())) return metadata;
        return state.is(MIXED.get()) ? 6 : -1;
    }
    public static void register() {
        if (registered) return; registered = true; BLOCKS.register(); ITEMS.register(); TILES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, MIXED_ITEM);
    }
}
