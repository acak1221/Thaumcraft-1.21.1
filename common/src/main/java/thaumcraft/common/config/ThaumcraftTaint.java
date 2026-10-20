package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockTaint;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.blocks.BlockTaintItem;
import thaumcraft.common.blocks.BlockTaintFibresItem;
import thaumcraft.common.entities.EntityFallingTaint;
import thaumcraft.common.entities.monster.EntityTaintSpider;
import thaumcraft.common.entities.monster.EntityTaintSpore;
import thaumcraft.common.entities.monster.EntityTaintSporeSwarmer;
import thaumcraft.common.entities.monster.EntityTaintSwarm;
import thaumcraft.common.entities.monster.EntityTaintacle;
import thaumcraft.common.entities.monster.EntityTaintacleSmall;

/** Original BlockTaint/Fibres family and its actual ecological actors. */
public final class ThaumcraftTaint {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Thaumcraft.MODID,Registries.ENTITY_TYPE);
    private static final SoundEvent GORE=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","gore"));
    private static final SoundType SOUND=new SoundType(.5F,.8F,GORE,GORE,GORE,GORE,GORE);
    public static final RegistrySupplier<BlockTaint> CRUST=BLOCKS.register("taint_crust",()->new BlockTaint(properties(2,6),0));
    public static final RegistrySupplier<BlockTaint> SOIL=BLOCKS.register("taint_soil",()->new BlockTaint(properties(2,6),1));
    // Existing flesh_block already owns original metadata2, including its real recipes and golem container identity.
    public static final RegistrySupplier<Block> FLESH=ThaumcraftGolemMaterials.FLESH_BLOCK;
    public static final RegistrySupplier<BlockTaintFibres> FIBRES=BLOCKS.register("taint_fibres",()->new BlockTaintFibres(properties(1,3).noOcclusion()
            .lightLevel(state->state.getValue(BlockTaintFibres.TYPE)==2?8:state.getValue(BlockTaintFibres.TYPE)==4?10:0)));
    public static final RegistrySupplier<Item> CRUST_ITEM=ITEMS.register("taint_crust",()->new BlockTaintItem(CRUST.get(),new Item.Properties()));
    public static final RegistrySupplier<Item> SOIL_ITEM=ITEMS.register("taint_soil",()->new BlockTaintItem(SOIL.get(),new Item.Properties()));
    private static final List<RegistrySupplier<Item>> FIBRE_ITEMS=fibres();
    private static List<RegistrySupplier<Item>> fibres() {
        String[] ids={"taint_fibres","taint_grass","taint_plant","taint_spore_stalk","taint_spore_stalk_mature"};
        var items=new ArrayList<RegistrySupplier<Item>>();
        for(int type=0;type<ids.length;type++) {final int metadata=type;items.add(ITEMS.register(ids[type],()->new BlockTaintFibresItem(FIBRES.get(),new Item.Properties(),metadata)));}
        return List.copyOf(items);
    }
    public static Item fibreItem(int type) { if(type<0||type>4)throw new IllegalArgumentException("Original fibre metadata "+type);return FIBRE_ITEMS.get(type).get(); }
    public static final RegistrySupplier<EntityType<EntityFallingTaint>> FALLING_TAINT=ENTITIES.register("falling_taint",()->EntityType.Builder.<EntityFallingTaint>of(EntityFallingTaint::new,MobCategory.MISC).sized(.98F,.98F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:falling_taint"));
    public static final RegistrySupplier<EntityType<EntityTaintSpider>> TAINT_SPIDER=ENTITIES.register("taint_spider",()->EntityType.Builder.<EntityTaintSpider>of(EntityTaintSpider::new,MobCategory.MONSTER).sized(.4F,.3F).eyeHeight(.3F*.85F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:taint_spider"));
    public static final RegistrySupplier<EntityType<EntityTaintSpore>> TAINT_SPORE=ENTITIES.register("taint_spore",()->EntityType.Builder.<EntityTaintSpore>of(EntityTaintSpore::new,MobCategory.MONSTER).sized(.5F,.5F).eyeHeight(.5F*.85F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:taint_spore"));
    public static final RegistrySupplier<EntityType<EntityTaintSporeSwarmer>> TAINT_SWARMER=ENTITIES.register("taint_swarmer",()->EntityType.Builder.<EntityTaintSporeSwarmer>of(EntityTaintSporeSwarmer::new,MobCategory.MONSTER).sized(1,1).eyeHeight(.85F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:taint_swarmer"));
    public static final RegistrySupplier<EntityType<EntityTaintSwarm>> TAINT_SWARM=ENTITIES.register("taint_swarm",()->EntityType.Builder.<EntityTaintSwarm>of(EntityTaintSwarm::new,MobCategory.MONSTER).sized(2,2).clientTrackingRange(4).updateInterval(3).build("thaumcraft:taint_swarm"));
    public static final RegistrySupplier<EntityType<EntityTaintacle>> TAINTACLE=ENTITIES.register("taintacle",()->EntityType.Builder.<EntityTaintacle>of(EntityTaintacle::new,MobCategory.MONSTER).sized(.66F,3).eyeHeight(3F*.85F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:taintacle"));
    public static final RegistrySupplier<EntityType<EntityTaintacleSmall>> TAINTACLE_SMALL=ENTITIES.register("taintacle_small",()->EntityType.Builder.<EntityTaintacleSmall>of(EntityTaintacleSmall::new,MobCategory.MONSTER).sized(.22F,1).eyeHeight(.85F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:taintacle_small"));
    private static final RegistrySupplier<SpawnEggItem> SPIDER_EGG=ITEMS.register("taint_spider_spawn_egg",()->new SpawnEggItem(TAINT_SPIDER.get(),0xFFC0FF,0x404040,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> SPORE_EGG=ITEMS.register("taint_spore_spawn_egg",()->new SpawnEggItem(TAINT_SPORE.get(),0xFFC0FF,0x800070,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> SWARMER_EGG=ITEMS.register("taint_swarmer_spawn_egg",()->new SpawnEggItem(TAINT_SWARMER.get(),0xFFC0FF,0x800060,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> SWARM_EGG=ITEMS.register("taint_swarm_spawn_egg",()->new SpawnEggItem(TAINT_SWARM.get(),0xFFC0FF,0x800050,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> TENTACLE_EGG=ITEMS.register("taintacle_spawn_egg",()->new SpawnEggItem(TAINTACLE.get(),0xFFC0FF,0x800080,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> SMALL_EGG=ITEMS.register("taintacle_small_spawn_egg",()->new SpawnEggItem(TAINTACLE_SMALL.get(),0xFFC0FF,0x800090,new Item.Properties()));
    private static boolean registered;
    private ThaumcraftTaint() {}
    private static BlockBehaviour.Properties properties(float hardness,float resistance) {return BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(hardness,resistance).sound(SOUND).randomTicks().pushReaction(PushReaction.DESTROY);}
    public static void register() {
        if(registered)return;registered=true;BLOCKS.register();ENTITIES.register();ITEMS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB,CRUST_ITEM,SOIL_ITEM);
        for(int type=0;type<4;type++)CreativeTabRegistry.append(ThaumcraftContent.TAB,FIBRE_ITEMS.get(type));
        for(var egg:List.of(SPIDER_EGG,SPORE_EGG,SWARMER_EGG,SWARM_EGG,TENTACLE_EGG,SMALL_EGG))CreativeTabRegistry.append(ThaumcraftContent.TAB,egg);
    }
}
