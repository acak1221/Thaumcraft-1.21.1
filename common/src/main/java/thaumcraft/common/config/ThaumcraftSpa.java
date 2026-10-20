package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockFluidPure;
import thaumcraft.common.blocks.BlockSpa;
import thaumcraft.common.container.ContainerSpa;
import thaumcraft.common.items.ItemBathSalts;
import thaumcraft.common.items.ItemBucketPure;
import thaumcraft.common.lib.SpaRules;
import thaumcraft.common.lib.fluid.FluidPure;
import thaumcraft.common.tiles.TileSpa;

/** Original stone-device12, real purifying fluid and dropped Bath Salts. */
public final class ThaumcraftSpa {
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Thaumcraft.MODID,Registries.FLUID);
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Thaumcraft.MODID,Registries.MENU);
    public static final RegistrySupplier<FlowingFluid> PURE_FLUID_SOURCE=FLUIDS.register("purifying_fluid",FluidPure.Source::new);
    public static final RegistrySupplier<FlowingFluid> PURE_FLUID_FLOWING=FLUIDS.register("flowing_purifying_fluid",FluidPure.Flowing::new);
    public static final RegistrySupplier<BlockFluidPure> PURE_WATER=BLOCKS.register("purifying_fluid",()->new BlockFluidPure(PURE_FLUID_SOURCE.get(),
            BlockBehaviour.Properties.of().mapColor(MapColor.WATER).replaceable().noCollission().noOcclusion().strength(100)
                    .noLootTable().liquid().pushReaction(PushReaction.DESTROY).sound(SoundType.EMPTY)
                    .lightLevel(state->SpaRules.pureLight(state.getValue(LiquidBlock.LEVEL)))
                    .isSuffocating((state,level,pos)->false).isViewBlocking((state,level,pos)->false)));
    public static final RegistrySupplier<BlockSpa> ARCANE_SPA=BLOCKS.register("arcane_spa",()->new BlockSpa(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3,15).sound(SoundType.STONE)));
    public static final RegistrySupplier<Item> ARCANE_SPA_ITEM=ITEMS.register("arcane_spa",()->new BlockItem(ARCANE_SPA.get(),new Item.Properties()));
    public static final RegistrySupplier<ItemBucketPure> BUCKET_PURE=ITEMS.register("bucket_pure",()->new ItemBucketPure(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<ItemBathSalts> BATH_SALTS=ITEMS.register("bath_salts",()->new ItemBathSalts(new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileSpa>> SPA_TILE=TILES.register("arcane_spa",()->BlockEntityType.Builder.of(TileSpa::new,ARCANE_SPA.get()).build(null));
    public static final RegistrySupplier<MenuType<ContainerSpa>> SPA_MENU=MENUS.register("arcane_spa",()->MenuRegistry.ofExtended(ContainerSpa::new));
    private static boolean registered;
    private ThaumcraftSpa() {}
    public static void register() {
        if(registered)return;registered=true;FLUIDS.register();BLOCKS.register();ITEMS.register();TILES.register();MENUS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB,ARCANE_SPA_ITEM);
        CreativeTabRegistry.append(ThaumcraftContent.TAB,BATH_SALTS);
        CreativeTabRegistry.append(ThaumcraftContent.TAB,BUCKET_PURE);
    }
}
