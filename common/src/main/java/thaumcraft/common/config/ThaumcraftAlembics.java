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
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.tiles.TileAlembic;

/** Independently registered blockMetalDevice:1 slice; call after ThaumcraftContent.register(). */
public final class ThaumcraftAlembics {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockAlembic> ALEMBIC = BLOCKS.register("alembic",
            () -> new BlockAlembic(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3, 17).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Item> ALEMBIC_ITEM = ITEMS.register("alembic",
            () -> new BlockItem(ALEMBIC.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileAlembic>> ALEMBIC_BLOCK_ENTITY = BLOCK_ENTITIES.register("alembic",
            () -> BlockEntityType.Builder.of(TileAlembic::new, ALEMBIC.get()).build(null));

    private static boolean registered;
    private ThaumcraftAlembics() {}

    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ALEMBIC_ITEM);
    }
}
