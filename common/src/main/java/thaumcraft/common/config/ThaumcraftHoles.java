package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockHole;
import thaumcraft.common.lib.world.HoleBlockRules;
import thaumcraft.common.tiles.TileHole;

/** The block a Portable Hole leaves behind (original blockHole + TileHole); it has no item. */
public final class ThaumcraftHoles {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockHole> HOLE_BLOCK = BLOCKS.register("hole", () -> new BlockHole(
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).sound(SoundType.STONE).lightLevel(state -> HoleBlockRules.HOLE_LIGHT)));
    public static final RegistrySupplier<BlockEntityType<TileHole>> HOLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("hole",
            () -> BlockEntityType.Builder.of(TileHole::new, HOLE_BLOCK.get()).build(null));
    private static boolean registered;

    private ThaumcraftHoles() {}

    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register();
        BLOCK_ENTITIES.register();
    }
}
