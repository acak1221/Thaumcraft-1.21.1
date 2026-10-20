package thaumcraft.api;

import java.util.ArrayList;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.IScanEventHandler;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.research.ObjectAspectRegistry;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Native scan/object-tag registration facade. Remaining original API areas are separate units. */
public final class ThaumcraftApi {
    public static ArrayList<IScanEventHandler> scanEventhandlers = new ArrayList<>();
    public static ArrayList<EntityTags> scanEntities = new ArrayList<>(EntityAspectBindings.originalTags());
    public static ConcurrentHashMap<List<?>, AspectList> objectTags = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<List<?>, int[]> groupedObjectTags = new ConcurrentHashMap<>();
    private ThaumcraftApi() {}

    public static void registerScanEventhandler(IScanEventHandler handler) { scanEventhandlers.add(Objects.requireNonNull(handler, "handler")); }
    public static void registerEntityTag(String entityName, AspectList aspects, EntityTagsNBT... nbt) {
        if (entityName == null || entityName.isEmpty()) throw new IllegalArgumentException("An entity tag requires its original or native registry name");
        scanEntities.add(new EntityTags(entityName, aspects, nbt));
    }

    /** Item identity and native damage metadata only: count, names and other NBT/components never narrow this registration. */
    public static void registerObjectTag(ItemStack item, AspectList aspects) {
        if (item == null || item.isEmpty()) return;
        registerObjectTag(item.getItem(), item.getDamageValue(), aspects);
    }
    public static void registerObjectTag(Item item, int metadata, AspectList aspects) {
        ObjectAspectRegistry.register(objectTags, item, metadata, aspects);
    }
    public static void registerObjectTag(ResourceLocation item, int metadata, AspectList aspects) {
        if (!BuiltInRegistries.ITEM.containsKey(item)) throw new IllegalArgumentException("Unknown native object-tag item: " + item);
        registerObjectTag(BuiltInRegistries.ITEM.get(item), metadata, aspects);
    }
    public static void registerObjectTag(ItemStack item, int[] metadata, AspectList aspects) {
        if (item == null || item.isEmpty()) return;
        ObjectAspectRegistry.register(objectTags, groupedObjectTags, item.getItem(), metadata, aspects);
    }
    /** Current native tag membership is snapshotted immediately, as original OreDictionary membership was. */
    public static void registerObjectTag(TagKey<Item> tag, AspectList aspects) {
        AspectList shared = aspects == null ? new AspectList() : aspects;
        BuiltInRegistries.ITEM.getTag(tag).ifPresent(items -> items.forEach(item -> registerObjectTag(item.value(), 0, shared)));
    }
    /** Native namespaced item tags replace the unavailable Forge OreDictionary; old ore names require an explicit caller mapping. */
    public static void registerObjectTag(String nativeItemTag, AspectList aspects) {
        ResourceLocation id = ResourceLocation.tryParse(nativeItemTag);
        if (id == null || !nativeItemTag.contains(":"))
            throw new IllegalArgumentException("Use a native namespaced item tag or TagKey<Item>, not a legacy OreDictionary name: " + nativeItemTag);
        registerObjectTag(TagKey.create(Registries.ITEM, id), aspects);
    }
    public static boolean exists(Item item, int metadata) {
        return ObjectAspectRegistry.exists(objectTags, item, metadata)
                || ObjectAspectCatalog.hasDerivedRegistration(BuiltInRegistries.ITEM.getKey(item), metadata);
    }
    public static void registerComplexObjectTag(ItemStack item, AspectList aspects) {
        registerComplexObjectTag(item, aspects, ObjectAspectRegistry.requireContext());
    }
    public static void registerComplexObjectTag(ItemStack item, AspectList aspects, Level world) {
        if (item == null || item.isEmpty()) throw new IllegalArgumentException("Complex object tags require an item");
        registerComplexObjectTag(item, item.getDamageValue(), aspects, world);
    }
    public static void registerComplexObjectTag(Item item, int metadata, AspectList aspects, Level world) {
        if (item == null) throw new IllegalArgumentException("Complex object tags require an item");
        registerComplexObjectTag(new ItemStack(item), metadata, aspects, world);
    }
    private static void registerComplexObjectTag(ItemStack item, int metadata, AspectList aspects, Level world) {
        var level = ObjectAspectRegistry.requireContext(world);
        Objects.requireNonNull(aspects, "Complex object-tag additions");
        ObjectAspectCatalog.initializePublicTags();
        AspectList result = ObjectAspectRegistry.complex(exists(item.getItem(), metadata), aspects,
                () -> ObjectAspectResolver.baseTags(item, level, metadata), () -> ObjectAspectResolver.generatedTags(item.getItem(), level));
        registerObjectTag(item.getItem(), metadata, result);
    }

    /** Original public registration keeps the supplied stack reference and silently ignores unknown rarities. */
    public static void addLootBagItem(ItemStack item, int weight, int... bagTypes) {
        if (bagTypes == null || bagTypes.length == 0) {
            thaumcraft.api.internal.WeightedRandomLoot.lootBagCommon.add(new thaumcraft.api.internal.WeightedRandomLoot(item, weight));
            return;
        }
        for (int rarity : bagTypes) {
            var entry = new thaumcraft.api.internal.WeightedRandomLoot(item, weight);
            switch (rarity) {
                case 0 -> thaumcraft.api.internal.WeightedRandomLoot.lootBagCommon.add(entry);
                case 1 -> thaumcraft.api.internal.WeightedRandomLoot.lootBagUncommon.add(entry);
                case 2 -> thaumcraft.api.internal.WeightedRandomLoot.lootBagRare.add(entry);
                default -> { }
            }
        }
    }

    /** Fields/references remain mutable as in the original API; a resolved scan copies the selected aspect list. */
    public static class EntityTags {
        public String entityName;
        public EntityTagsNBT[] nbts;
        public AspectList aspects;
        public EntityTags(String entityName, AspectList aspects, EntityTagsNBT... nbts) {
            this.entityName = entityName; this.aspects = aspects; this.nbts = nbts;
        }
    }
    public static class EntityTagsNBT {
        public String name;
        public Object value;
        public EntityTagsNBT(String name, Object value) { this.name = name; this.value = value; }
    }
}
