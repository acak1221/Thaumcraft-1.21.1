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
import thaumcraft.common.blocks.BlockChestHungry;
import thaumcraft.common.tiles.TileChestHungry;

/** Original standalone hungry chest; its menu is vanilla GENERIC_9x3. */
public final class ThaumcraftHungryChest {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockChestHungry> HUNGRY_CHEST = BLOCKS.register("hungry_chest", () -> new BlockChestHungry(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final RegistrySupplier<Item> HUNGRY_CHEST_ITEM = ITEMS.register("hungry_chest",
            () -> new BlockItem(HUNGRY_CHEST.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileChestHungry>> HUNGRY_CHEST_BLOCK_ENTITY = TILES.register("hungry_chest",
            () -> BlockEntityType.Builder.of(TileChestHungry::new, HUNGRY_CHEST.get()).build(null));

    private ThaumcraftHungryChest() {}

    public static void register() {
        BLOCKS.register(); ITEMS.register(); TILES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, HUNGRY_CHEST_ITEM);
    }
}
