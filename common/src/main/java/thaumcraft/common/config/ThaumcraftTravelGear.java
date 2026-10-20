package thaumcraft.common.config;

import com.mojang.serialization.Codec;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.container.ContainerHoverHarness;
import thaumcraft.common.items.armor.ItemBootsTraveller;
import thaumcraft.common.items.armor.ItemHoverHarness;

public final class ThaumcraftTravelGear {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final DeferredRegister<ArmorMaterial> MATERIALS=DeferredRegister.create(Thaumcraft.MODID,Registries.ARMOR_MATERIAL);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS=DeferredRegister.create(Thaumcraft.MODID,Registries.DATA_COMPONENT_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Thaumcraft.MODID,Registries.MENU);
    public static final RegistrySupplier<ArmorMaterial> TRAVELLER_MATERIAL=MATERIALS.register("traveller",()->material("traveller"));
    public static final RegistrySupplier<ArmorMaterial> HARNESS_MATERIAL=MATERIALS.register("hover_harness",()->material("hover_harness"));
    public static final RegistrySupplier<DataComponentType<Integer>> HOVER_CHARGE=COMPONENTS.register("hover_charge",()->DataComponentType.<Integer>builder().persistent(Codec.intRange(0,360)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final RegistrySupplier<DataComponentType<Boolean>> HOVER_ENABLED=COMPONENTS.register("hover_enabled",()->DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    public static final RegistrySupplier<ItemBootsTraveller> BOOTS_TRAVELLER=ITEMS.register("boots_traveller",()->new ItemBootsTraveller(TRAVELLER_MATERIAL,new Item.Properties()));
    public static final RegistrySupplier<ItemHoverHarness> HOVER_HARNESS=ITEMS.register("hover_harness",()->new ItemHoverHarness(HARNESS_MATERIAL,new Item.Properties()));
    public static final RegistrySupplier<MenuType<ContainerHoverHarness>> HARNESS_MENU=MENUS.register("hover_harness",()->MenuRegistry.ofExtended(ContainerHoverHarness::new));
    private static boolean registered;
    private ThaumcraftTravelGear() {}
    private static ArmorMaterial material(String texture) { return new ArmorMaterial(Map.of(ArmorItem.Type.HELMET,1,ArmorItem.Type.CHESTPLATE,3,ArmorItem.Type.LEGGINGS,2,ArmorItem.Type.BOOTS,1),25,
            SoundEvents.ARMOR_EQUIP_LEATHER,()->Ingredient.EMPTY,List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID,texture))),0,0); }
    public static void register() {if(registered)return;registered=true;MATERIALS.register();COMPONENTS.register();ITEMS.register();MENUS.register();CreativeTabRegistry.append(ThaumcraftContent.TAB,BOOTS_TRAVELLER);CreativeTabRegistry.append(ThaumcraftContent.TAB,HOVER_HARNESS);}
}
