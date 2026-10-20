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
import thaumcraft.common.blocks.BlockLevitator;
import thaumcraft.common.tiles.TileLevitator;

/** The original blockLifter (the Arcane Levitator); call after ThaumcraftContent.register(). */
public final class ThaumcraftLevitator {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockLevitator> LEVITATOR = BLOCKS.register("levitator",
            // Original setHardness2.5 / setResistance15 (stored x3, exposed /5: 9); wood.
            () -> new BlockLevitator(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 9.0F).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Item> LEVITATOR_ITEM = ITEMS.register("levitator",
            () -> new BlockItem(LEVITATOR.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileLevitator>> LEVITATOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("levitator",
            () -> BlockEntityType.Builder.of(TileLevitator::new, LEVITATOR.get()).build(null));

    private static boolean registered;
    private ThaumcraftLevitator() {}

    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, LEVITATOR_ITEM);
    }
}
