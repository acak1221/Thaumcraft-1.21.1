package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.monster.EntityEldritchCrab;
import thaumcraft.common.entities.monster.EntityEldritchGolem;
import thaumcraft.common.entities.monster.EntityEldritchWarden;
import thaumcraft.common.entities.monster.EntityInhabitedZombie;
import thaumcraft.common.entities.monster.EntityTaintacleGiant;
import thaumcraft.common.entities.monster.EntityThaumicSlime;
import thaumcraft.common.entities.monster.EntityWatcher;

/**
 * Original TC4 eldritch bestiary registration. The original registry names were EldritchWarden,
 * EldritchGolem, EldritchCrab, InhabitedZombie, Watcher, ThaumSlime and TaintacleGiant.
 */
public final class ThaumcraftEldritchMobs {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Thaumcraft.MODID,Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    public static final RegistrySupplier<EntityType<EntityEldritchWarden>> ELDRITCH_WARDEN=ENTITIES.register("eldritch_warden",()->
            EntityType.Builder.<EntityEldritchWarden>of(EntityEldritchWarden::new,MobCategory.MONSTER).sized(1.5F,3.5F)
                    .eyeHeight(3.1F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:eldritch_warden"));
    public static final RegistrySupplier<EntityType<EntityEldritchGolem>> ELDRITCH_GOLEM=ENTITIES.register("eldritch_golem",()->
            EntityType.Builder.<EntityEldritchGolem>of(EntityEldritchGolem::new,MobCategory.MONSTER).sized(1.75F,3.5F)
                    .eyeHeight(3F).fireImmune().clientTrackingRange(4).updateInterval(3).build("thaumcraft:eldritch_golem"));
    public static final RegistrySupplier<EntityType<EntityEldritchCrab>> ELDRITCH_CRAB=ENTITIES.register("eldritch_crab",()->
            EntityType.Builder.<EntityEldritchCrab>of(EntityEldritchCrab::new,MobCategory.MONSTER).sized(.8F,.6F)
                    .clientTrackingRange(4).updateInterval(3).build("thaumcraft:eldritch_crab"));
    public static final RegistrySupplier<EntityType<EntityInhabitedZombie>> INHABITED_ZOMBIE=ENTITIES.register("inhabited_zombie",()->
            EntityType.Builder.<EntityInhabitedZombie>of(EntityInhabitedZombie::new,MobCategory.MONSTER).sized(.6F,1.8F)
                    .eyeHeight(1.53F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:inhabited_zombie"));
    public static final RegistrySupplier<EntityType<EntityWatcher>> WATCHER=ENTITIES.register("watcher",()->
            EntityType.Builder.<EntityWatcher>of(EntityWatcher::new,MobCategory.MONSTER).sized(.85F,.85F)
                    .eyeHeight(.85F*.5F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:watcher"));
    public static final RegistrySupplier<EntityType<EntityThaumicSlime>> THAUMIC_SLIME=ENTITIES.register("thaum_slime",()->
            EntityType.Builder.<EntityThaumicSlime>of(EntityThaumicSlime::new,MobCategory.MONSTER).sized(.5F,.5F)
                    .clientTrackingRange(4).updateInterval(3).build("thaumcraft:thaum_slime"));
    public static final RegistrySupplier<EntityType<EntityTaintacleGiant>> TAINTACLE_GIANT=ENTITIES.register("taintacle_giant",()->
            EntityType.Builder.<EntityTaintacleGiant>of(EntityTaintacleGiant::new,MobCategory.MONSTER).sized(1.1F,6F)
                    .clientTrackingRange(4).updateInterval(3).build("thaumcraft:taintacle_giant"));
    public static final RegistrySupplier<SpawnEggItem> ELDRITCH_WARDEN_EGG=ITEMS.register("eldritch_warden_spawn_egg",()->
            new SpawnEggItem(ELDRITCH_WARDEN.get(),0x555555,0x404040,new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> ELDRITCH_GOLEM_EGG=ITEMS.register("eldritch_golem_spawn_egg",()->
            new SpawnEggItem(ELDRITCH_GOLEM.get(),0x555555,0x404040,new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> ELDRITCH_CRAB_EGG=ITEMS.register("eldritch_crab_spawn_egg",()->
            new SpawnEggItem(ELDRITCH_CRAB.get(),0x555555,0x550000,new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> INHABITED_ZOMBIE_EGG=ITEMS.register("inhabited_zombie_spawn_egg",()->
            new SpawnEggItem(INHABITED_ZOMBIE.get(),0x557755,0x550000,new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> WATCHER_EGG=ITEMS.register("watcher_spawn_egg",()->
            new SpawnEggItem(WATCHER.get(),0x555555,0x555555,new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> THAUMIC_SLIME_EGG=ITEMS.register("thaum_slime_spawn_egg",()->
            new SpawnEggItem(THAUMIC_SLIME.get(),0x555555,0x800080,new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> TAINTACLE_GIANT_EGG=ITEMS.register("taintacle_giant_spawn_egg",()->
            new SpawnEggItem(TAINTACLE_GIANT.get(),0xFFC0FF,0x808080,new Item.Properties()));
    private static boolean registered;
    private ThaumcraftEldritchMobs() {}
    /** Loader hook: the attribute suppliers of the eldritch bestiary. The loader passes its own registrar. */
    public static void registerAttributes(java.util.function.BiConsumer<EntityType<?>,net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder> registrar) {
        registrar.accept(ELDRITCH_WARDEN.get(),EntityEldritchWarden.createAttributes());
        registrar.accept(ELDRITCH_GOLEM.get(),EntityEldritchGolem.createAttributes());
        registrar.accept(ELDRITCH_CRAB.get(),EntityEldritchCrab.createAttributes());
        registrar.accept(INHABITED_ZOMBIE.get(),EntityInhabitedZombie.createAttributes());
        registrar.accept(WATCHER.get(),EntityWatcher.createAttributes());
        registrar.accept(THAUMIC_SLIME.get(),EntityThaumicSlime.createAttributes());
        registrar.accept(TAINTACLE_GIANT.get(),EntityTaintacleGiant.createAttributes());
    }
    public static void register() {
        if(registered)return;
        registered=true;
        ENTITIES.register();
        ITEMS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB,ELDRITCH_WARDEN_EGG,ELDRITCH_GOLEM_EGG,ELDRITCH_CRAB_EGG,
                INHABITED_ZOMBIE_EGG,WATCHER_EGG,THAUMIC_SLIME_EGG,TAINTACLE_GIANT_EGG);
    }
}
