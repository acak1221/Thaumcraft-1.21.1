package thaumcraft.common.config;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.monster.EntityTaintChicken;
import thaumcraft.common.entities.monster.EntityTaintCow;
import thaumcraft.common.entities.monster.EntityTaintCrawler;
import thaumcraft.common.entities.monster.EntityTaintPig;
import thaumcraft.common.entities.monster.EntityTaintSeed;
import thaumcraft.common.entities.monster.EntityTaintSeedPrime;
import thaumcraft.common.entities.monster.EntityTaintSheep;
import thaumcraft.common.entities.monster.EntityTaintVillager;
import thaumcraft.common.lib.taint.TaintAnimalRules;
import thaumcraft.common.lib.taint.TaintAnimalRules.TaintedAnimal;

/** Original tainted animals, the taint crawler and the taint seeds.
 *
 * <p>The original1.7 registry names are kept: TaintedChicken, TaintedCow,
 * TaintedPig, TaintedSheep and TaintedVillager. The crawler and the seeds only
 * exist in the later Thaumcraft6 build, which registers them as TaintCrawler,
 * TaintSeed and TaintSeedPrime. */
public final class ThaumcraftTaintAnimals {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Thaumcraft.MODID,Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    public static final RegistrySupplier<EntityType<EntityTaintChicken>> TAINT_CHICKEN=ENTITIES.register("tainted_chicken",()->EntityType.Builder.<EntityTaintChicken>of(EntityTaintChicken::new,MobCategory.MONSTER).sized(.5F,.8F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:tainted_chicken"));
    public static final RegistrySupplier<EntityType<EntityTaintCow>> TAINT_COW=ENTITIES.register("tainted_cow",()->EntityType.Builder.<EntityTaintCow>of(EntityTaintCow::new,MobCategory.MONSTER).sized(.9F,1.3F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:tainted_cow"));
    public static final RegistrySupplier<EntityType<EntityTaintPig>> TAINT_PIG=ENTITIES.register("tainted_pig",()->EntityType.Builder.<EntityTaintPig>of(EntityTaintPig::new,MobCategory.MONSTER).sized(.9F,.9F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:tainted_pig"));
    public static final RegistrySupplier<EntityType<EntityTaintSheep>> TAINT_SHEEP=ENTITIES.register("tainted_sheep",()->EntityType.Builder.<EntityTaintSheep>of(EntityTaintSheep::new,MobCategory.MONSTER).sized(.9F,1.3F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:tainted_sheep"));
    public static final RegistrySupplier<EntityType<EntityTaintVillager>> TAINT_VILLAGER=ENTITIES.register("tainted_villager",()->EntityType.Builder.<EntityTaintVillager>of(EntityTaintVillager::new,MobCategory.MONSTER).sized(.6F,1.8F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:tainted_villager"));
    public static final RegistrySupplier<EntityType<EntityTaintCrawler>> TAINT_CRAWLER=ENTITIES.register("taint_crawler",()->EntityType.Builder.<EntityTaintCrawler>of(EntityTaintCrawler::new,MobCategory.MONSTER).sized(.5F,.4F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:taint_crawler"));
    public static final RegistrySupplier<EntityType<EntityTaintSeed>> TAINT_SEED=ENTITIES.register("taint_seed",()->EntityType.Builder.<EntityTaintSeed>of(EntityTaintSeed::new,MobCategory.MONSTER).sized(1.5F,1.25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:taint_seed"));
    public static final RegistrySupplier<EntityType<EntityTaintSeedPrime>> TAINT_SEED_PRIME=ENTITIES.register("taint_seed_prime",()->EntityType.Builder.<EntityTaintSeedPrime>of(EntityTaintSeedPrime::new,MobCategory.MONSTER).sized(2F,2F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:taint_seed_prime"));
    /** The original TC6 flux crystal: the crawler and the seeds drop it. */
    public static final RegistrySupplier<Item> FLUX_CRYSTAL=ITEMS.register("flux_crystal",()->new Item(new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> CHICKEN_EGG=ITEMS.register("tainted_chicken_spawn_egg",()->new SpawnEggItem(TAINT_CHICKEN.get(),0xFFC0FF,0xC0C0C0,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> COW_EGG=ITEMS.register("tainted_cow_spawn_egg",()->new SpawnEggItem(TAINT_COW.get(),0xFFC0FF,8272443,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> PIG_EGG=ITEMS.register("tainted_pig_spawn_egg",()->new SpawnEggItem(TAINT_PIG.get(),0xFFC0FF,0xEF99EF,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> SHEEP_EGG=ITEMS.register("tainted_sheep_spawn_egg",()->new SpawnEggItem(TAINT_SHEEP.get(),0xFFC0FF,0x808080,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> VILLAGER_EGG=ITEMS.register("tainted_villager_spawn_egg",()->new SpawnEggItem(TAINT_VILLAGER.get(),0xFFC0FF,65535,new Item.Properties()));
    private static final RegistrySupplier<SpawnEggItem> CRAWLER_EGG=ITEMS.register("taint_crawler_spawn_egg",()->new SpawnEggItem(TAINT_CRAWLER.get(),10618530,0x303030,new Item.Properties()));
    private static boolean registered;
    private ThaumcraftTaintAnimals() {}

    /** Original ItemResource metadata11 is tainted goo and12 is taint tendril. */
    public static ItemStack taintMaterial(int metadata) {
        return new ItemStack(ThaumcraftContent.MATERIALS.get(metadata==TaintAnimalRules.TAINTED_GOO?"tainted_goo":"taint_tendril").get());
    }
    /** Original ItemResource metadata18 is the gold coin. */
    public static ItemStack goldCoin() { return new ItemStack(ThaumcraftContent.MATERIALS.get("gold_coin").get()); }

    public static void register() {
        if(registered)return;registered=true;
        ENTITIES.register();ITEMS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB,FLUX_CRYSTAL);
        for(var egg:List.of(CHICKEN_EGG,COW_EGG,PIG_EGG,SHEEP_EGG,VILLAGER_EGG,CRAWLER_EGG))CreativeTabRegistry.append(ThaumcraftContent.TAB,egg);
        EntityEvent.LIVING_DEATH.register(ThaumcraftTaintAnimals::onLivingDeath);
    }

    /** Original1.7 LivingDeathEvent: a flux tainted animal dies and its tainted twin takes its place. */
    private static EventResult onLivingDeath(Entity entity,DamageSource source) {
        if(!(entity instanceof LivingEntity living))return EventResult.pass();
        if(!(living.level() instanceof ServerLevel server))return EventResult.pass();
        if(!TaintAnimalRules.convertsOnDeath(hasFluxTaint(living),living instanceof ITaintedMob))return EventResult.pass();
        TaintedAnimal variant=TaintAnimalRules.taintedVariant(entityId(living));
        if(variant==null)return EventResult.pass();
        var type=typeFor(variant);
        if(type==null)return EventResult.pass();
        var tainted=type.create(server);
        if(tainted==null)return EventResult.pass();
        tainted.moveTo(living.getX(),living.getY(),living.getZ(),living.getYRot(),living.getXRot());
        server.addFreshEntity(tainted);
        // The original vanilla animal still dies with its own loot.
        return EventResult.pass();
    }
    private static boolean hasFluxTaint(LivingEntity living) {
        return living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()));
    }
    private static String entityId(LivingEntity living) {
        return EntityType.getKey(living.getType()).toString();
    }
    private static EntityType<?> typeFor(TaintedAnimal variant) {
        return switch(variant) {
            case CHICKEN -> TAINT_CHICKEN.get();
            case COW -> TAINT_COW.get();
            case PIG -> TAINT_PIG.get();
            case SHEEP -> TAINT_SHEEP.get();
            case VILLAGER -> TAINT_VILLAGER.get();
        };
    }
}
