package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockWarded;
import thaumcraft.common.tiles.TileWarded;

/** Internal ward wrapper. It is made and undone with the focus; it has neither a creative item nor ordinary loot. */
public final class ThaumcraftWards {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<BlockWarded> WARDED = BLOCKS.register("warded", () -> new BlockWarded(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1.0F, 599.4F).sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK).lightLevel(state -> state.getValue(BlockWarded.LIGHT)).noLootTable().dynamicShape()
                    .isValidSpawn((state, level, pos, type) -> false)));
    public static final RegistrySupplier<BlockEntityType<TileWarded>> WARDED_TILE = TILES.register("warded", () ->
            BlockEntityType.Builder.of(TileWarded::new, WARDED.get()).build(null));
    private static boolean registered;
    private ThaumcraftWards() {}
    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register(); TILES.register();
    }
}
