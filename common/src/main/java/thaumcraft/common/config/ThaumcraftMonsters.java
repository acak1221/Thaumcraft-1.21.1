package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityGiantBrainyZombie;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.lib.network.WispZapNetworking;

/** TC4 monster identities; loader adapters register attributes, placement predicates and biome population. */
public final class ThaumcraftMonsters {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    public static final RegistrySupplier<EntityType<EntityBrainyZombie>> BRAINY_ZOMBIE = ENTITIES.register("brainy_zombie", () ->
            EntityType.Builder.<EntityBrainyZombie>of(EntityBrainyZombie::new, MobCategory.MONSTER).sized(.6F, 1.8F).eyeHeight(1.53F).clientTrackingRange(4).build("thaumcraft:brainy_zombie"));
    public static final RegistrySupplier<EntityType<EntityGiantBrainyZombie>> GIANT_BRAINY_ZOMBIE = ENTITIES.register("giant_brainy_zombie", () ->
            EntityType.Builder.<EntityGiantBrainyZombie>of(EntityGiantBrainyZombie::new, MobCategory.MONSTER).sized(.6F, 1.8F).eyeHeight(1.53F).clientTrackingRange(4).build("thaumcraft:giant_brainy_zombie"));
    public static final RegistrySupplier<EntityType<EntityFireBat>> FIREBAT = ENTITIES.register("firebat", () ->
            EntityType.Builder.<EntityFireBat>of(EntityFireBat::new, MobCategory.MONSTER).sized(.5F, .9F).eyeHeight(.765F).fireImmune().clientTrackingRange(4).build("thaumcraft:firebat"));
    public static final RegistrySupplier<EntityType<EntityWisp>> WISP = ENTITIES.register("wisp", () ->
            EntityType.Builder.<EntityWisp>of(EntityWisp::new, MobCategory.MONSTER).sized(.9F, .9F).eyeHeight(.765F).clientTrackingRange(4).build("thaumcraft:wisp"));
    public static final RegistrySupplier<SpawnEggItem> BRAINY_ZOMBIE_EGG = ITEMS.register("brainy_zombie_spawn_egg", () -> new SpawnEggItem(BRAINY_ZOMBIE.get(), 0xFFC0FF, 0x008000, new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> GIANT_BRAINY_ZOMBIE_EGG = ITEMS.register("giant_brainy_zombie_spawn_egg", () -> new SpawnEggItem(GIANT_BRAINY_ZOMBIE.get(), 0xFFC0FF, 0x004000, new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> WISP_EGG = ITEMS.register("wisp_spawn_egg", () -> new SpawnEggItem(WISP.get(), 0xFFC0FF, 0xFFFFFF, new Item.Properties()));
    public static final TagKey<Biome> BRAINY_SPAWNS = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("thaumcraft", "brainy_zombie_spawns"));
    private static boolean registered;
    private ThaumcraftMonsters() {}
    public static void register() {
        if (registered) return; registered = true; ENTITIES.register(); ITEMS.register();
        WispZapNetworking.initServer();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, BRAINY_ZOMBIE_EGG); CreativeTabRegistry.append(ThaumcraftContent.TAB, GIANT_BRAINY_ZOMBIE_EGG);
        CreativeTabRegistry.append(ThaumcraftContent.TAB, WISP_EGG);
    }
}
