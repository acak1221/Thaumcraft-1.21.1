package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockManaPod;
import thaumcraft.common.items.CrystalEssenceData;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.tiles.TileManaPod;

public final class ThaumcraftManaBeans {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS=DeferredRegister.create(Thaumcraft.MODID,Registries.DATA_COMPONENT_TYPE);
    public static final TagKey<Biome> MAGICAL_BIOMES=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("thaumcraft","magical_biomes"));
    public static final RegistrySupplier<DataComponentType<CrystalEssenceData>> MANA_BEAN_DATA=COMPONENTS.register("mana_bean_data",()->DataComponentType.<CrystalEssenceData>builder().persistent(CrystalEssenceData.CODEC).networkSynchronized(CrystalEssenceData.STREAM_CODEC).build());
    public static final RegistrySupplier<ItemManaBean> MANA_BEAN=ITEMS.register("mana_bean",()->new ItemManaBean(new Item.Properties()));
    public static final RegistrySupplier<BlockManaPod> MANA_POD=BLOCKS.register("mana_pod",()->new BlockManaPod(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noOcclusion().instabreak().sound(SoundType.GRASS).randomTicks().lightLevel(state->state.getValue(BlockManaPod.AGE)).pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<BlockEntityType<TileManaPod>> MANA_POD_BLOCK_ENTITY=BLOCK_ENTITIES.register("mana_pod",()->BlockEntityType.Builder.of(TileManaPod::new,MANA_POD.get()).build(null));
    private static boolean registered;
    private ThaumcraftManaBeans(){}
    public static void register(){if(registered)return;registered=true;COMPONENTS.register();BLOCKS.register();ITEMS.register();BLOCK_ENTITIES.register();CreativeTabRegistry.append(ThaumcraftContent.TAB,MANA_BEAN);}
}
