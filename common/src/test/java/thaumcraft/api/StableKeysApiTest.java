package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ArmorMaterial;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.entities.ThaumcraftEntities;
import thaumcraft.api.potions.PotionFluxTaint;
import thaumcraft.api.potions.PotionVisExhaust;
import thaumcraft.api.potions.WarpEffects;

/**
 * The stable keys of the add-on API (effects, entities, armour materials, sounds).
 *
 * <p>The keys are plain data, so what can be checked without a registry is checked here: every key lives in the right
 * registry and the {@code thaumcraft} namespace, none repeats, and every key names an id that the mod's own
 * registration code declares (read from the sources, like {@code SoundResourceAuditTest} does for the sounds). That the
 * keys resolve to the live registered objects of the right kind is checked in a running world (GameTest
 * {@code stableKeysResolveToTheRegisteredObjects}); the registries of this JVM hold the vanilla content only, which is
 * what the contract tests at the end use.</p>
 */
class StableKeysApiTest {
    private static final Path CONFIG = Path.of("src/main/java/thaumcraft/common/config");
    private static final Path ENTITY_KEYS = Path.of("src/main/java/thaumcraft/api/entities/ThaumcraftEntities.java");
    /** A key of {@code ThaumcraftEntities} with the class its Javadoc names (the comment line before it reads "Original EntityWisp"). */
    private static final Pattern DOCUMENTED_KEY = Pattern.compile(
            "/\\*\\* Original \\{@code (Entity\\w+)\\}[^*]*\\*/\\s*public static final ResourceKey<EntityType<\\?>> (\\w+) = key\\(\"([a-z_0-9]+)\"\\);");
    /** A registration of an entity type with the class it creates: {@code RegistrySupplier<EntityType<EntityWisp>> WISP = ENTITIES.register("wisp"}. */
    private static final Pattern REGISTERED_TYPE = Pattern.compile(
            "RegistrySupplier<EntityType<(Entity\\w+)>>\\s+\\w+\\s*=\\s*ENTITIES\\.register\\(\"([a-z_0-9]+)\"");

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<ResourceKey<T>> keysOf(Class<?> holder, ResourceKey<? extends net.minecraft.core.Registry<T>> registry) throws IllegalAccessException {
        List<ResourceKey<T>> keys = new ArrayList<>();
        for (Field field : holder.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isStatic(field.getModifiers()) || field.getType() != ResourceKey.class) continue;
            ResourceKey<T> key = (ResourceKey<T>) field.get(null);
            assertEquals(registry.location(), key.registry(), field.getName() + " is a key of the wrong registry");
            assertEquals("thaumcraft", key.location().getNamespace(), field.getName());
            keys.add(key);
        }
        return keys;
    }

    private static Set<String> registeredPaths(String registerVariable) throws IOException {
        Set<String> found = new TreeSet<>();
        Pattern pattern = Pattern.compile("(?<![A-Za-z_])" + registerVariable + "\\.register\\(\"([a-z_0-9]+)\"");
        try (Stream<Path> files = Files.list(CONFIG)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = pattern.matcher(Files.readString(file));
                while (matcher.find()) found.add(matcher.group(1));
            }
        }
        return found;
    }

    @Test void everyWarpEffectKeyIsDistinctAndNamesAnEffectTheModRegisters() throws Exception {
        List<ResourceKey<net.minecraft.world.effect.MobEffect>> keys = keysOf(WarpEffects.class, Registries.MOB_EFFECT);
        assertEquals(7, keys.size());
        assertEquals(keys.size(), new HashSet<>(keys).size(), "no key repeats");
        assertEquals(new HashSet<>(keys), new HashSet<>(WarpEffects.all()), "all() lists exactly the constants");
        assertEquals(keys.size(), WarpEffects.all().size());
        Set<String> registered = registeredPaths("EFFECTS");
        for (ResourceKey<?> key : keys) assertTrue(registered.contains(key.location().getPath()), key + " is not registered by the mod");
        // The two flux effects keep their own handles; the seven keys do not repeat them.
        assertFalse(WarpEffects.all().contains(PotionFluxTaint.KEY));
        assertFalse(WarpEffects.all().contains(PotionVisExhaust.KEY));
        assertTrue(registered.contains(PotionFluxTaint.KEY.location().getPath()));
        assertTrue(registered.contains(PotionVisExhaust.KEY.location().getPath()));
        assertEquals("thaumcraft:warp_ward", WarpEffects.WARP_WARD.location().toString());
        assertEquals("thaumcraft:infectious_vis_exhaust", WarpEffects.INFECTIOUS_VIS_EXHAUST.location().toString());
    }

    @Test void theWarpEffectListIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> WarpEffects.all().add(PotionFluxTaint.KEY));
        assertThrows(UnsupportedOperationException.class, () -> WarpEffects.all().clear());
    }

    @Test void everyEntityKeyIsDistinctAndNamesAnEntityTypeTheModRegisters() throws Exception {
        List<ResourceKey<EntityType<?>>> keys = keysOf(ThaumcraftEntities.class, Registries.ENTITY_TYPE);
        assertTrue(keys.size() >= 30, "the creatures an add-on may recognise are all there: " + keys.size());
        assertEquals(keys.size(), new HashSet<>(keys).size(), "no key repeats");
        Set<String> registered = registeredPaths("ENTITIES");
        for (ResourceKey<?> key : keys) assertTrue(registered.contains(key.location().getPath()), key + " is not an entity type the mod registers");
        // The ones the gap analysis names.
        for (String path : List.of("wisp", "firebat", "brainy_zombie", "golem", "frost_shard")) {
            assertTrue(keys.contains(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("thaumcraft", path))), path);
        }
    }

    @Test void everyEntityKeyIsTiedToTheClassItsJavadocNamesAndTheUnkeyedTypesArePinned() throws Exception {
        // A key whose path points at another existing entity (a swap of two paths) passes the "registered and distinct" check of
        // the test above; this one ties each key to the class its Javadoc promises through the registration that makes the type.
        Map<String, String> documented = new TreeMap<>();
        Matcher keyLine = DOCUMENTED_KEY.matcher(Files.readString(ENTITY_KEYS));
        while (keyLine.find()) documented.put(keyLine.group(3), keyLine.group(1));
        List<ResourceKey<EntityType<?>>> keys = keysOf(ThaumcraftEntities.class, Registries.ENTITY_TYPE);
        assertEquals(keys.size(), documented.size(), "every key has a Javadoc line 'Original {@code EntityX}' this test can read");
        assertEquals(new TreeSet<>(keys.stream().map(key -> key.location().getPath()).toList()), documented.keySet());

        Map<String, String> registered = new TreeMap<>();
        try (Stream<Path> files = Files.list(CONFIG)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher registration = REGISTERED_TYPE.matcher(Files.readString(file));
                while (registration.find()) registered.put(registration.group(2), registration.group(1));
            }
        }
        assertEquals(registeredPaths("ENTITIES"), registered.keySet(), "every entity registration was read with its class");
        for (Map.Entry<String, String> key : documented.entrySet())
            assertEquals(key.getValue(), registered.get(key.getKey()), "the key " + key.getKey() + " creates the class its Javadoc names");

        // The registered types that have no key, pinned like the armour materials are: a new entity type must make someone decide
        // whether an add-on needs to recognise it.
        Set<String> unkeyed = new TreeSet<>(registered.keySet());
        unkeyed.removeAll(documented.keySet());
        assertEquals(new TreeSet<>(List.of("alumentum", "aspect_orb", "bottle_taint", "dart", "eldritch_orb", "ember", "explosive_orb",
                        "falling_taint", "follow_item", "golem_bobber", "golem_orb", "inventory_item", "item_grate", "pech_blast",
                        "permanent_item", "primal_arrow", "primal_orb", "shock_orb", "special_item")), unkeyed,
                "the registered entity types without a key changed: decide whether the new one needs a key in ThaumcraftEntities");
    }

    @Test void theTwoPlainArmourObjectsHoldTheOriginalNumbers() {
        // armorMatSpecial and armorMatVoidFortress are the originals' two materials that were never registered: plain objects with
        // the original defences and enchantability (the durability factor is on the item in 1.21.1, not in the object).
        ArmorMaterial special = ThaumcraftMaterials.ARMORMAT_SPECIAL, fortress = ThaumcraftMaterials.ARMORMAT_VOID_FORTRESS;
        assertSame(special, ThaumcraftApi.armorMatSpecial());
        assertSame(fortress, ThaumcraftApi.armorMatVoidFortress());
        assertEquals(25, special.enchantmentValue(), "armorMatSpecial: enchantability 25");
        assertEquals(10, fortress.enchantmentValue(), "armorMatVoidFortress: enchantability 10");
        assertDefence(special, 1, 3, 2, 1);
        assertDefence(fortress, 4, 8, 7, 4);
        assertEquals(0.0F, special.toughness());
        assertEquals(0.0F, fortress.knockbackResistance());
        assertTrue(special.layers().isEmpty() && fortress.layers().isEmpty(), "no worn layer: an add-on brings its own");
    }

    private static void assertDefence(ArmorMaterial material, int helmet, int chest, int legs, int boots) {
        var defence = material.defense();
        assertEquals(helmet, defence.get(net.minecraft.world.item.ArmorItem.Type.HELMET), "helmet");
        assertEquals(chest, defence.get(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE), "chestplate");
        assertEquals(legs, defence.get(net.minecraft.world.item.ArmorItem.Type.LEGGINGS), "leggings");
        assertEquals(boots, defence.get(net.minecraft.world.item.ArmorItem.Type.BOOTS), "boots");
    }

    @Test void theArmourMaterialKeysNameMaterialsTheModRegisters() throws Exception {
        List<ResourceKey<ArmorMaterial>> keys = List.of(ThaumcraftMaterials.ARMORMAT_THAUMIUM_KEY,
                ThaumcraftMaterials.ARMORMAT_THAUMIUM_FORTRESS_KEY, ThaumcraftMaterials.ARMORMAT_VOID_KEY,
                ThaumcraftMaterials.ARMORMAT_VOID_ROBE_KEY);
        assertEquals(4, new HashSet<>(keys).size());
        Set<String> registered = registeredPaths("ARMOR_MATERIALS");
        for (ResourceKey<ArmorMaterial> key : keys) {
            assertEquals(Registries.ARMOR_MATERIAL.location(), key.registry());
            assertEquals("thaumcraft", key.location().getNamespace());
            assertTrue(registered.contains(key.location().getPath()), key + " is not registered by the mod");
        }
        // The mod registers ten armour materials; the API names the four of the thaumium and void families (the originals
        // armorMatThaumium, armorMatThaumiumFortress and armorMatVoid, plus the robe variant of the void set). The other
        // six are adapters of the mod's own items (the goggles' "special", the "robe" and the four cultist sets), and the two
        // originals armorMatSpecial and armorMatVoidFortress stay plain objects that were never registered under those names.
        assertEquals(new TreeSet<>(List.of("special", "robe", "cultist_robe", "cultist_plate", "cultist_leader", "cultist_boots",
                        "thaumium", "fortress", "void", "void_robe")), registered,
                "the registered materials changed: decide whether the new one needs a key in ThaumcraftMaterials");
        Set<String> named = new TreeSet<>(keys.stream().map(key -> key.location().getPath()).toList());
        assertEquals(new TreeSet<>(List.of("thaumium", "fortress", "void", "void_robe")), named);
    }

    @Test void everySoundConstantIsASoundEventTheModDeclaresAndNoneIsMissing() throws Exception {
        Set<String> declared = new TreeSet<>(JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/assets/thaumcraft/sounds.json"))).getAsJsonObject().keySet());
        Set<String> constants = new TreeSet<>();
        Set<ResourceLocation> seen = new HashSet<>();
        for (Field field : ThaumcraftSoundIds.class.getDeclaredFields()) {
            if (field.getType() != ResourceLocation.class || !Modifier.isPublic(field.getModifiers())) continue;
            ResourceLocation id = (ResourceLocation) field.get(null);
            assertEquals("thaumcraft", id.getNamespace(), field.getName());
            assertEquals(id.getPath().toUpperCase(java.util.Locale.ROOT), field.getName(), "named after the path");
            assertTrue(seen.add(id), "no id repeats: " + id);
            constants.add(id.getPath());
        }
        assertEquals(declared, constants, "one constant for every sound the mod declares, and none for a sound it does not");
        for (String name : List.of("wand", "tool", "zap", "wind", "brain", "egidle", "key"))
            assertTrue(constants.contains(name), name);
    }

    // ---- the contract against the registries of a plain JVM (vanilla content only) ----

    @Test void resolvingAKeyBeforeTheModRegisteredIsAnIllegalStateNotANull() {
        assertThrows(IllegalStateException.class, () -> WarpEffects.holder(WarpEffects.WARP_WARD));
        assertThrows(IllegalStateException.class, () -> WarpEffects.instance(WarpEffects.THAUMARHIA));
        assertThrows(IllegalStateException.class, () -> ThaumcraftEntities.holder(ThaumcraftEntities.WISP));
        assertThrows(IllegalStateException.class, () -> ThaumcraftEntities.type(ThaumcraftEntities.GOLEM));
        assertThrows(IllegalStateException.class, () -> ThaumcraftMaterials.armorMatThaumiumHolder());
        assertThrows(IllegalStateException.class, () -> ThaumcraftMaterials.armorMatVoidRobeHolder());
    }

    @Test void theKeysResolveWhateverTheRegistryHoldsForAVanillaKey() {
        // The helpers are generic over the registry, so a vanilla key resolves; the add-on API adds no policy of its own.
        ResourceKey<net.minecraft.world.effect.MobEffect> speed = ResourceKey.create(Registries.MOB_EFFECT,
                ResourceLocation.withDefaultNamespace("speed"));
        assertNotNull(WarpEffects.holder(speed));
        assertSame(WarpEffects.holder(speed).value(), WarpEffects.instance(speed));
        ResourceKey<EntityType<?>> pig = ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("pig"));
        assertSame(EntityType.PIG, ThaumcraftEntities.type(pig));
        assertSame(EntityType.PIG, ThaumcraftEntities.holder(pig).value());
    }

    @Test void theSoundHelperResolvesRegisteredEventsAndRefusesTheRest() {
        ResourceLocation hurt = ResourceLocation.withDefaultNamespace("entity.player.hurt");
        SoundEvent event = ThaumcraftSoundIds.event(hurt);
        assertEquals(hurt, event.getLocation());
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> ThaumcraftSoundIds.event(ThaumcraftSoundIds.WAND));
        assertTrue(missing.getMessage().contains("thaumcraft:wand"), missing.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftSoundIds.event(null));
    }

    @Test void theEntityRecogniserNeverThrows() {
        assertFalse(ThaumcraftEntities.is(null, ThaumcraftEntities.WISP));
        assertFalse(ThaumcraftEntities.is(null, null));
    }
}
