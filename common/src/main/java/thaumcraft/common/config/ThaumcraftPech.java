package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.container.ContainerPech;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.projectile.EntityPechBlast;
import thaumcraft.common.items.wands.foci.ItemFocusPech;

/** One original entity with three saved variants, its real curse and trade menu. */
public final class ThaumcraftPech {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Thaumcraft.MODID,Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Thaumcraft.MODID,Registries.MENU);
    public static final RegistrySupplier<EntityType<EntityPech>> PECH=ENTITIES.register("pech",()->EntityType.Builder.<EntityPech>of(EntityPech::new,MobCategory.MONSTER).sized(.6F,1.8F).eyeHeight(1.1880001F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:pech"));
    public static final RegistrySupplier<EntityType<EntityPechBlast>> PECH_BLAST=ENTITIES.register("pech_blast",()->EntityType.Builder.<EntityPechBlast>of(EntityPechBlast::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:pech_blast"));
    public static final RegistrySupplier<ItemFocusPech> FOCUS_PECH=ITEMS.register("focus_pech",()->new ItemFocusPech(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<SpawnEggItem> PECH_EGG=ITEMS.register("pech_spawn_egg",()->new SpawnEggItem(PECH.get(),0xFFC0FF,0x400040,new Item.Properties()));
    public static final RegistrySupplier<MenuType<ContainerPech>> PECH_MENU=MENUS.register("pech",()->MenuRegistry.ofExtended(ContainerPech::new));
    private static boolean registered;
    private ThaumcraftPech(){}
    public static void register(){if(registered)return;registered=true;ENTITIES.register();ITEMS.register();MENUS.register();CreativeTabRegistry.append(ThaumcraftContent.TAB,FOCUS_PECH);CreativeTabRegistry.append(ThaumcraftContent.TAB,PECH_EGG);}
}
