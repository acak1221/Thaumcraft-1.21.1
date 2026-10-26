package thaumcraft.common.lib.research;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EnumGolemType;

/** Original baseline plus live public entity registrations; the last matching row replaces, never sums earlier rewards. */
public final class EntityAspectBindings {
    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("minecraft:zombie", "Zombie"), Map.entry("minecraft:zombie_villager", "Zombie"), Map.entry("minecraft:giant", "Giant"),
            Map.entry("minecraft:skeleton", "Skeleton"), Map.entry("minecraft:wither_skeleton", "Skeleton"), Map.entry("minecraft:creeper", "Creeper"),
            Map.entry("minecraft:horse", "EntityHorse"), Map.entry("minecraft:donkey", "EntityHorse"), Map.entry("minecraft:mule", "EntityHorse"),
            Map.entry("minecraft:skeleton_horse", "EntityHorse"), Map.entry("minecraft:zombie_horse", "EntityHorse"),
            Map.entry("minecraft:pig", "Pig"), Map.entry("minecraft:experience_orb", "XPOrb"), Map.entry("minecraft:sheep", "Sheep"),
            Map.entry("minecraft:cow", "Cow"), Map.entry("minecraft:mooshroom", "MushroomCow"), Map.entry("minecraft:snow_golem", "SnowMan"),
            Map.entry("minecraft:ocelot", "Ozelot"), Map.entry("minecraft:cat", "Ozelot"), Map.entry("minecraft:chicken", "Chicken"),
            Map.entry("minecraft:squid", "Squid"), Map.entry("minecraft:wolf", "Wolf"), Map.entry("minecraft:bat", "Bat"),
            Map.entry("minecraft:boat", "Boat"), Map.entry("minecraft:spider", "Spider"), Map.entry("minecraft:slime", "Slime"),
            Map.entry("minecraft:ghast", "Ghast"), Map.entry("minecraft:zombified_piglin", "PigZombie"), Map.entry("minecraft:enderman", "Enderman"),
            Map.entry("minecraft:cave_spider", "CaveSpider"), Map.entry("minecraft:silverfish", "Silverfish"), Map.entry("minecraft:blaze", "Blaze"),
            Map.entry("minecraft:magma_cube", "LavaSlime"), Map.entry("minecraft:ender_dragon", "EnderDragon"), Map.entry("minecraft:wither", "WitherBoss"),
            Map.entry("minecraft:witch", "Witch"), Map.entry("minecraft:villager", "Villager"), Map.entry("minecraft:iron_golem", "VillagerGolem"),
            Map.entry("minecraft:minecart", "MinecartRideable"), Map.entry("minecraft:chest_minecart", "MinecartChest"),
            Map.entry("minecraft:furnace_minecart", "MinecartFurnace"), Map.entry("minecraft:tnt_minecart", "MinecartTNT"),
            Map.entry("minecraft:hopper_minecart", "MinecartHopper"), Map.entry("minecraft:spawner_minecart", "MinecartSpawner"),
            Map.entry("minecraft:end_crystal", "EnderCrystal"), Map.entry("minecraft:item_frame", "ItemFrame"), Map.entry("minecraft:painting", "Painting"),
            Map.entry("thaumcraft:golem", "Thaumcraft.Golem"), Map.entry("thaumcraft:brainy_zombie", "Thaumcraft.BrainyZombie"),
            Map.entry("thaumcraft:giant_brainy_zombie", "Thaumcraft.GiantBrainyZombie"), Map.entry("thaumcraft:firebat", "Thaumcraft.Firebat"),
            Map.entry("thaumcraft:wisp", "Thaumcraft.Wisp"), Map.entry("thaumcraft:pech", "Thaumcraft.Pech"),
            Map.entry("thaumcraft:mind_spider", "Thaumcraft.MindSpider"),
            Map.entry("thaumcraft:thaum_slime", "Thaumcraft.ThaumSlime"),
            Map.entry("thaumcraft:tainted_pig", "Thaumcraft.TaintedPig"),
            Map.entry("thaumcraft:tainted_sheep", "Thaumcraft.TaintedSheep"),
            Map.entry("thaumcraft:tainted_cow", "Thaumcraft.TaintedCow"),
            Map.entry("thaumcraft:tainted_chicken", "Thaumcraft.TaintedChicken"),
            Map.entry("thaumcraft:tainted_villager", "Thaumcraft.TaintedVillager"),
            Map.entry("thaumcraft:taint_creeper", "Thaumcraft.TaintedCreeper"),
            Map.entry("thaumcraft:cultist_knight", "Thaumcraft.CultistKnight"),
            Map.entry("thaumcraft:cultist_cleric", "Thaumcraft.CultistCleric"),
            Map.entry("thaumcraft:taint_spider", "Thaumcraft.TaintSpider"),
            Map.entry("thaumcraft:taint_spore", "Thaumcraft.TaintSpore"),
            Map.entry("thaumcraft:taint_swarmer", "Thaumcraft.TaintSwarmer"),
            Map.entry("thaumcraft:taint_swarm", "Thaumcraft.TaintSwarm"),
            Map.entry("thaumcraft:taintacle", "Thaumcraft.Taintacle"),
            Map.entry("thaumcraft:taintacle_small", "Thaumcraft.TaintacleTiny"),
            Map.entry("thaumcraft:primal_orb", "Thaumcraft.PrimalOrb"),
            Map.entry("thaumcraft:eldritch_guardian", "Thaumcraft.EldritchGuardian"), Map.entry("thaumcraft:eldritch_orb", "Thaumcraft.EldritchOrb"));
    private static volatile List<Row> rows;
    private record Row(String name, Map<String, Integer> aspects, String byteName, int byteValue, boolean wisp) {}
    public record Target(String originalName, String signature, AspectList aspects, ResearchClues.Context clue) {}
    private EntityAspectBindings() {}

    public static Target describe(Entity entity) {
        String registryName = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String name = entity instanceof Player player ? "player_" + player.getGameProfile().getName()
                : originalName(registryName);
        var registered = new ArrayList<>(ThaumcraftApi.scanEntities);
        var nbt = new CompoundTag();
        if (registered.stream().anyMatch(row -> matchesName(row.entityName, name, registryName) && row.nbts != null && row.nbts.length > 0))
            nbt = entity.saveWithoutId(nbt);
        // Vanilla split old SkeletonType metadata into two entity IDs. Preserve the original predicate key.
        if ("Skeleton".equals(name)) nbt.putByte("SkeletonType", (byte) (entity.getType() == EntityType.WITHER_SKELETON ? 1 : 0));
        AspectList aspects = entity instanceof Player player ? playerAspects(player.getGameProfile().getName()) : select(registered, name, registryName, nbt);
        if (aspects == null || aspects.size() == 0) return null;
        String hash = name;
        for (var row : registered) {
            if (!matchesName(row.entityName, hash, hash.equals(name) ? registryName : hash) || row.nbts == null || row.nbts.length == 0
                    || !EntityScanPredicates.matches(row.nbts, nbt)) continue;
            for (var predicate : row.nbts) hash += predicate.name + predicate.value;
        }
        if (entity instanceof LivingEntity living && living.isBaby()) hash += "CHILD";
        if (entity instanceof ZombieVillager) hash += "VILLAGER";
        if (entity instanceof Creeper creeper) {
            if (creeper.getSwellDir() == 1) hash += "FLASHING";
            if (creeper.isPowered()) hash += "POWERED";
        }
        if (entity instanceof EntityGolemBase golem) {
            var material = EnumGolemType.getType(golem.getGolemMaterial());
            if (material != null) hash += material.name();
        }
        // Fresh native signatures retain the original variant string without its collision-prone 32-bit hash.
        // Original EntityList had no player entry: its generated player aspect/hash name was not a clue witness.
        return new Target(name, "@entity:" + hash, aspects, entity instanceof Player ? null : ResearchClues.Context.entity(name));
    }

    static String originalName(String registryName) {
        return NAMES.getOrDefault(registryName, registryName);
    }

    /** Original row order: a conditional match replaces the generic aspects instead of adding a second reward. */
    public static AspectList forOriginal(String name, Map<String, Integer> byteValues, String wispType) {
        var data = new CompoundTag();
        for (var entry : byteValues.entrySet()) data.putByte(entry.getKey(), entry.getValue().byteValue());
        if (wispType != null) data.putString("Type", wispType);
        return select(new ArrayList<>(ThaumcraftApi.scanEntities), name, name, data);
    }

    /** The plain row of an entity type, without any data predicate: the port's aspect inference gives it to a spawn egg. */
    public static AspectList forType(net.minecraft.world.entity.EntityType<?> type) {
        String registryName = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        return select(new ArrayList<>(ThaumcraftApi.scanEntities), originalName(registryName), registryName, new CompoundTag());
    }

    public static AspectList forNames(String originalName, String nativeName, CompoundTag data) {
        return select(new ArrayList<>(ThaumcraftApi.scanEntities), originalName, nativeName, data);
    }

    private static AspectList select(List<ThaumcraftApi.EntityTags> registered, String originalName, String nativeName, CompoundTag data) {
        AspectList selected = null;
        for (var row : registered) {
            if (matchesName(row.entityName, originalName, nativeName) && EntityScanPredicates.matches(row.nbts, data))
                selected = row.aspects == null ? null : row.aspects.copy();
        }
        return selected;
    }

    private static boolean matchesName(String registered, String originalName, String nativeName) {
        return registered != null && (registered.equals(originalName) || registered.equals(nativeName));
    }

    /** Expanded public API baseline:69 retained declarations include one loop over48 Wisp aspect variants, yielding116 rows. */
    public static List<ThaumcraftApi.EntityTags> originalTags() {
        var result = new ArrayList<ThaumcraftApi.EntityTags>();
        for (Row row : rows()) {
            var variants = row.wisp ? new ArrayList<>(Aspect.aspects.values()) : java.util.Collections.<Aspect>singletonList(null);
            for (Aspect variant : variants) {
                var aspects = new AspectList();
                if (row.wisp) aspects.add(variant, 2);
                for (var entry : row.aspects.entrySet()) aspects.add(Aspect.getAspect(entry.getKey()), entry.getValue());
                var conditions = row.wisp ? new ThaumcraftApi.EntityTagsNBT[] { new ThaumcraftApi.EntityTagsNBT("Type", variant.getTag()) }
                        : row.byteName == null ? new ThaumcraftApi.EntityTagsNBT[0]
                        : new ThaumcraftApi.EntityTagsNBT[] { new ThaumcraftApi.EntityTagsNBT(row.byteName, (byte) row.byteValue) };
                result.add(new ThaumcraftApi.EntityTags(row.name, aspects, conditions));
            }
        }
        return result;
    }

    public static AspectList playerAspects(String playerName) {
        AspectList tags = new AspectList().add(Aspect.MAN, 4);
        if (playerName.equalsIgnoreCase("azanor")) return tags.add(Aspect.ELDRITCH, 20);
        if (playerName.equalsIgnoreCase("direwolf20")) return tags.add(Aspect.BEAST, 20);
        if (playerName.equalsIgnoreCase("pahimar")) return tags.add(Aspect.EXCHANGE, 20);
        Random privateRandom = new Random(("player_" + playerName).hashCode());
        Aspect[] possible = Aspect.aspects.values().toArray(Aspect[]::new);
        for (int index = 0; index < 3; index++) tags.add(possible[privateRandom.nextInt(possible.length)], 4);
        return tags;
    }

    private static List<Row> rows() {
        var ready = rows;
        if (ready != null) return ready;
        synchronized (EntityAspectBindings.class) {
            if (rows != null) return rows;
            var stream = EntityAspectBindings.class.getClassLoader().getResourceAsStream("data/thaumcraft/tc4/entity_aspects.json");
            if (stream == null) throw new IllegalStateException("Missing original entity aspect bindings");
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                var root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.get("schema_version").getAsInt() != 1) throw new IllegalStateException("Unsupported entity aspect schema");
                var loaded = new ArrayList<Row>();
                for (var value : root.getAsJsonArray("entries")) {
                    var row = value.getAsJsonObject(); var aspects = new LinkedHashMap<String, Integer>();
                    for (var aspect : row.getAsJsonObject("aspects").entrySet()) {
                        if (Aspect.getAspect(aspect.getKey()) == null || aspect.getValue().getAsInt() <= 0) throw new IllegalStateException("Invalid entity aspect");
                        aspects.put(aspect.getKey(), aspect.getValue().getAsInt());
                    }
                    var condition = row.has("byte_condition") ? row.getAsJsonObject("byte_condition") : null;
                    loaded.add(new Row(row.get("original_name").getAsString(), aspects,
                            condition == null ? null : condition.get("name").getAsString(), condition == null ? 0 : condition.get("value").getAsInt(),
                            row.has("dynamic_wisp_type") && row.get("dynamic_wisp_type").getAsBoolean()));
                }
                if (loaded.size() != 69) throw new IllegalStateException("Incomplete original entity aspect registrations");
                rows = List.copyOf(loaded); return rows;
            } catch (java.io.IOException invalid) { throw new IllegalStateException("Cannot load entity aspect bindings", invalid); }
        }
    }
}
