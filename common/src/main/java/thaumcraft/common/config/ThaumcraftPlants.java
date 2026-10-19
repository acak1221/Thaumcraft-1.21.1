package thaumcraft.common.config;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockCustomPlant;

/** BlockCustomPlant metadata2/3 only; no fake Ethereal Bloom/Vishroom ecology. */
public final class ThaumcraftPlants {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    public static final RegistrySupplier<BlockCustomPlant> SHIMMERLEAF = plant("shimmerleaf", false);
    public static final RegistrySupplier<BlockCustomPlant> CINDERPEARL = plant("cinderpearl", true);
    private ThaumcraftPlants() {}
    private static RegistrySupplier<BlockCustomPlant> plant(String id, boolean cinderpearl) {
        var block = BLOCKS.register(id, () -> new BlockCustomPlant(cinderpearl, BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                .noCollission().noOcclusion().instabreak().sound(SoundType.GRASS).randomTicks().replaceable().lightLevel(state -> 8)));
        ITEMS.register(id, () -> new BlockCustomPlant.PlantItem(block.get(), new Item.Properties()));
        return block;
    }
    public static void register() {
        BLOCKS.register(); ITEMS.register();
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(SHIMMERLEAF.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(CINDERPEARL.get()));
        LifecycleEvent.SETUP.register(() -> {
            ((FireBlock) Blocks.FIRE).setFlammable(SHIMMERLEAF.get(), 60, 100);
            ((FireBlock) Blocks.FIRE).setFlammable(CINDERPEARL.get(), 60, 100);
        });
    }
}
