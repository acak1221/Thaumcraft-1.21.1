package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.ItemCrimsonRites;
import thaumcraft.common.items.ItemSanitySoap;

/** Two actual functional identities. Eye/Pearl keep their existing registry owners; tablet/placer are absent. */
public final class ThaumcraftEldritchItems {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    public static final RegistrySupplier<ItemSanitySoap> SANITY_SOAP=ITEMS.register("sanity_soap",()->new ItemSanitySoap(new Item.Properties()));
    public static final RegistrySupplier<ItemCrimsonRites> CRIMSON_RITES=ITEMS.register("crimson_rites",()->new ItemCrimsonRites(new Item.Properties()));
    private static boolean registered;
    private ThaumcraftEldritchItems(){}
    public static void register(){if(registered)return;registered=true;ITEMS.register();CreativeTabRegistry.append(ThaumcraftContent.TAB,SANITY_SOAP);CreativeTabRegistry.append(ThaumcraftContent.TAB,CRIMSON_RITES);}
}
