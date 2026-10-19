package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockFlesh;

/** Original cosmetic-solid4/5 and inert blockTaint2 material blocks.
 * Living golem registrations remain separate from these actual block items. */
public final class ThaumcraftGolemMaterials {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);

    // BlockCosmeticSolid:213-252 overrides hardness/resistance for metadata4.
    public static final RegistrySupplier<Block> THAUMIUM_BLOCK = BLOCKS.register("thaumium_block",
            () -> new Block(rock(4, 20)));
    public static final RegistrySupplier<Item> THAUMIUM_BLOCK_ITEM = ITEMS.register("thaumium_block",
            () -> new BlockItem(THAUMIUM_BLOCK.get(), new Item.Properties()));

    // Metadata5 retains Material.rock, stone sound and base hardness2. Original
    // setResistance(10) stores30; the vanilla getter divides by5, yielding6.
    public static final RegistrySupplier<Block> TALLOW_BLOCK = BLOCKS.register("tallow_block",
            () -> new Block(rock(2, 6)));
    public static final RegistrySupplier<Item> TALLOW_BLOCK_ITEM = ITEMS.register("tallow_block",
            () -> new BlockItem(TALLOW_BLOCK.get(), new Item.Properties()));

    // Original blockTaint:2 only; no crusted/soil taint behavior is imported.
    public static final RegistrySupplier<Block> FLESH_BLOCK = BLOCKS.register("flesh_block",
            () -> new BlockFlesh(BlockBehaviour.Properties.of()));
    public static final RegistrySupplier<Item> FLESH_BLOCK_ITEM = ITEMS.register("flesh_block",
            () -> new BlockItem(FLESH_BLOCK.get(), new Item.Properties()));

    private static boolean registered;

    private ThaumcraftGolemMaterials() {}

    private static BlockBehaviour.Properties rock(float hardness, float resistance) {
        // Original constructor64-70: rock/map-stone, stone sound, random-tick
        // flag. Metadata4/5 have no tick effect, BE, light or fire registration.
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(hardness, resistance)
                .sound(SoundType.STONE).requiresCorrectToolForDrops().randomTicks();
    }

    /** Root calls after ThaumcraftContent.register(); cube JSONs need no renderer hook. */
    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register();
        ITEMS.register();
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(THAUMIUM_BLOCK_ITEM.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(TALLOW_BLOCK_ITEM.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(FLESH_BLOCK_ITEM.get()));
    }
}
