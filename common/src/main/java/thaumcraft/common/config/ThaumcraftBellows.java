package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockBellows;
import thaumcraft.common.tiles.TileBellows;

public final class ThaumcraftBellows {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    // BlockWoodenDevice:80-85: hardness2.5; old setResistance10 gave effective6.
    public static final RegistrySupplier<BlockBellows> BELLOWS = BLOCKS.register("bellows", () -> new BlockBellows(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 6).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final RegistrySupplier<Item> BELLOWS_ITEM = ITEMS.register("bellows", () -> new BlockItem(BELLOWS.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileBellows>> BELLOWS_TILE = TILES.register("bellows",
            () -> BlockEntityType.Builder.of(TileBellows::new, BELLOWS.get()).build(null));
    private ThaumcraftBellows() {}
    public static void register() {
        BLOCKS.register(); ITEMS.register(); TILES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, BELLOWS_ITEM);
    }
}
