package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.projectile.EntityPrimalOrb;
import thaumcraft.common.items.wands.foci.ItemFocusPrimal;

/** Final original focus family and its distinct chaotic projectile. */
public final class ThaumcraftPrimalFoci {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Thaumcraft.MODID,Registries.ENTITY_TYPE);
    public static final RegistrySupplier<ItemFocusPrimal> FOCUS_PRIMAL=ITEMS.register("focus_primal",()->new ItemFocusPrimal(new Item.Properties()));
    public static final RegistrySupplier<EntityType<EntityPrimalOrb>> PRIMAL_ORB=ENTITIES.register("primal_orb",()->EntityType.Builder.<EntityPrimalOrb>of(EntityPrimalOrb::new,MobCategory.MISC)
            .sized(.25F,.25F).eyeHeight(0F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:primal_orb"));
    private static boolean registered;
    private ThaumcraftPrimalFoci(){}
    public static void register(){if(registered)return;registered=true;ITEMS.register();ENTITIES.register();CreativeTabRegistry.append(ThaumcraftContent.TAB,FOCUS_PRIMAL);}
}
