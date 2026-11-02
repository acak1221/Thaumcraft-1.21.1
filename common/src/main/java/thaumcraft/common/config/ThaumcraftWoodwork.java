package thaumcraft.common.config;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.fuel.FuelRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.WoodworkBlocks;

/** Owner-requested modern building pieces; existing magical logs and their node mechanics are unchanged. */
public final class ThaumcraftWoodwork {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    public static final Map<String, Family> FAMILIES = new LinkedHashMap<>();
    public static final Family GREATWOOD = family("greatwood", MapColor.COLOR_BROWN);
    public static final Family SILVERWOOD = family("silverwood", MapColor.QUARTZ);
    public static final RegistrySupplier<BlockEntityType<WoodworkBlocks.SignTile>> SIGN_TYPE = TILES.register("woodwork_sign",
            () -> BlockEntityType.Builder.of(WoodworkBlocks.SignTile::new,
                    GREATWOOD.sign.get(), GREATWOOD.wallSign.get(), SILVERWOOD.sign.get(), SILVERWOOD.wallSign.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WoodworkBlocks.HangingSignTile>> HANGING_SIGN_TYPE = TILES.register("woodwork_hanging_sign",
            () -> BlockEntityType.Builder.of(WoodworkBlocks.HangingSignTile::new,
                    GREATWOOD.hangingSign.get(), GREATWOOD.wallHangingSign.get(), SILVERWOOD.hangingSign.get(), SILVERWOOD.wallHangingSign.get()).build(null));
    private static boolean registered;
    private ThaumcraftWoodwork() {}

    public record Family(String name, WoodType woodType, BlockSetType setType,
            RegistrySupplier<Block> door, RegistrySupplier<Block> trapdoor, RegistrySupplier<Block> fence,
            RegistrySupplier<Block> fenceGate, RegistrySupplier<Block> sign, RegistrySupplier<Block> wallSign,
            RegistrySupplier<Block> hangingSign, RegistrySupplier<Block> wallHangingSign,
            RegistrySupplier<Block> button, RegistrySupplier<Block> pressurePlate) {}

    private static Family family(String name, MapColor color) {
        // Vanilla model-layer locations compose this name into a path: use a globally unique
        // path-safe name, while the actual block/item registry IDs keep the thaumcraft namespace.
        var set = BlockSetType.register(new BlockSetType("thaumcraft_" + name));
        var wood = WoodType.register(new WoodType("thaumcraft_" + name, set));
        var door = block(name + "_door", () -> new WoodworkBlocks.Door(set, props(color).noOcclusion()));
        var trapdoor = block(name + "_trapdoor", () -> new WoodworkBlocks.Trapdoor(set, props(color).noOcclusion()));
        var fence = block(name + "_fence", () -> new FenceBlock(props(color)));
        var gate = block(name + "_fence_gate", () -> new FenceGateBlock(wood, props(color)));
        var button = block(name + "_button", () -> new WoodworkBlocks.Button(set, props(color).noCollission().strength(.5F)));
        var plate = block(name + "_pressure_plate", () -> new WoodworkBlocks.Plate(set, props(color).noCollission().strength(.5F)));
        RegistrySupplier<Block> sign = BLOCKS.register(name + "_sign", () -> new WoodworkBlocks.StandingSign(wood, signProps(color)));
        RegistrySupplier<Block> wallSign = BLOCKS.register(name + "_wall_sign", () -> new WoodworkBlocks.WallSign(wood, signProps(color)));
        RegistrySupplier<Block> hanging = BLOCKS.register(name + "_hanging_sign", () -> new WoodworkBlocks.CeilingSign(wood, signProps(color)));
        RegistrySupplier<Block> wallHanging = BLOCKS.register(name + "_wall_hanging_sign", () -> new WoodworkBlocks.WallHangingSign(wood, signProps(color)));
        ITEMS.register(name + "_sign", () -> new SignItem(new Item.Properties().stacksTo(16), sign.get(), wallSign.get()));
        ITEMS.register(name + "_hanging_sign", () -> new HangingSignItem(hanging.get(), wallHanging.get(), new Item.Properties().stacksTo(16)));
        Family family = new Family(name, wood, set, door, trapdoor, fence, gate, sign, wallSign, hanging, wallHanging, button, plate);
        FAMILIES.put(name, family);
        return family;
    }

    private static BlockBehaviour.Properties props(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(2.5F, 6.0F).sound(SoundType.WOOD);
    }
    private static BlockBehaviour.Properties signProps(MapColor color) { return props(color).noCollission().strength(1.0F); }
    private static RegistrySupplier<Block> block(String id, java.util.function.Supplier<Block> factory) {
        var result = BLOCKS.register(id, factory);
        ITEMS.register(id, () -> id.endsWith("_door")
                ? new net.minecraft.world.item.DoubleHighBlockItem(result.get(), new Item.Properties())
                : new BlockItem(result.get(), new Item.Properties()));
        return result;
    }
    public static Family family(WoodType type) {
        for (Family family : FAMILIES.values()) if (family.woodType.equals(type)) return family;
        return null;
    }
    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register(); ITEMS.register(); TILES.register();
        for (Family f : FAMILIES.values()) {
            for (var block : java.util.List.of(f.door, f.trapdoor, f.fence, f.fenceGate, f.button, f.pressurePlate, f.sign, f.hangingSign))
                CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(block.get()));
        }
        LifecycleEvent.SETUP.register(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            for (Family f : FAMILIES.values()) for (var block : java.util.List.of(f.door, f.trapdoor, f.fence, f.fenceGate,
                    f.sign, f.wallSign, f.hangingSign, f.wallHangingSign, f.button, f.pressurePlate)) fire.setFlammable(block.get(), 5, 20);
            for (Family f : FAMILIES.values()) {
                for (var block : java.util.List.of(f.fence, f.fenceGate, f.trapdoor)) FuelRegistry.register(300, block.get().asItem());
                FuelRegistry.register(200, f.door.get().asItem()); FuelRegistry.register(200, f.sign.get().asItem());
                FuelRegistry.register(800, f.hangingSign.get().asItem()); FuelRegistry.register(100, f.button.get().asItem());
                FuelRegistry.register(300, f.pressurePlate.get().asItem());
            }
        });
    }
}
