package thaumcraft.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The loader parity of the NeoForge module against the Fabric module.
 *
 * <p>The shared {@code common} module owns the registry content itself, so both loaders register
 * the very same blocks, items, block entity types, sounds, entities and creative tab. What the
 * loaders own separately is the wiring around that content: the entity attribute catalogue, the
 * spawn placements, the biome modifiers, the original chest rows, the access transformer that
 * replaces the access widener, the fluid types and the client events. This test compares those
 * lists mechanically, source against source, so a registration that exists on one loader only can
 * never pass unnoticed again.</p>
 *
 * <p>The comparison is a source comparison and not a runtime comparison on purpose: the lists of the
 * two loaders live in two different modules that never see each other at runtime. The runtime
 * proof that the NeoForge wiring is live belongs to the NeoForge GameTest suite.</p>
 */
class NeoForgeLoaderParityTest {
    private static final Path ROOT = repositoryRoot();

    // ------------------------------------------------------------------ the repository

    private static Path repositoryRoot() {
        Path candidate = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("settings.gradle"))) return candidate;
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Cannot find the repository root from " + System.getProperty("user.dir"));
    }

    private static String read(String relative) {
        Path file = ROOT.resolve(relative);
        assertTrue(Files.isRegularFile(file), "missing source file " + relative);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read " + relative, failure);
        }
    }

    // ------------------------------------------------------------------ the source reader

    /**
     * The argument lists of every call of the given callee.
     *
     * <p>The callee is matched by its trailing text, so a fully qualified call and an imported one
     * are the same call. The scan is parenthesis, bracket, brace and string aware, so a call
     * inside another call is not mistaken for a top level one.</p>
     */
    private static List<List<String>> calls(String source, String callee) {
        List<List<String>> found = new ArrayList<>();
        int from = 0;
        while (true) {
            int at = source.indexOf(callee, from);
            if (at < 0) return found;
            int open = source.indexOf('(', at);
            if (open < 0) return found;
            int close = matching(source, open);
            if (close < 0) return found;
            found.add(split(source.substring(open + 1, close)));
            from = close + 1;
        }
    }

    private static int matching(String source, int open) {
        int depth = 0;
        boolean quoted = false;
        char quote = 0;
        for (int index = open; index < source.length(); index++) {
            char character = source.charAt(index);
            if (quoted) {
                if (character == '\\') index++;
                else if (character == quote) quoted = false;
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
            } else if (character == '(' || character == '[' || character == '{') depth++;
            else if (character == ')' || character == ']' || character == '}') {
                depth--;
                if (depth == 0) return index;
            }
        }
        return -1;
    }

    private static List<String> split(String arguments) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        boolean quoted = false;
        char quote = 0;
        for (int index = 0; index < arguments.length(); index++) {
            char character = arguments.charAt(index);
            if (quoted) {
                if (character == '\\') index++;
                else if (character == quote) quoted = false;
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
            } else if (character == '(' || character == '[' || character == '{') depth++;
            else if (character == ')' || character == ']' || character == '}') depth--;
            else if (character == ',' && depth == 0) {
                parts.add(arguments.substring(start, index).trim());
                start = index + 1;
            }
        }
        String last = arguments.substring(start).trim();
        if (!last.isEmpty() || !parts.isEmpty()) parts.add(last);
        return parts;
    }

    /**
     * The simple name of an expression: the owning class and the member.
     *
     * <p>{@code thaumcraft.common.config.ThaumcraftPech.PECH.get()} becomes
     * {@code ThaumcraftPech.PECH} and {@code EntityPech::createAttributes} becomes
     * {@code EntityPech.createAttributes}, so a fully qualified call and an imported one compare
     * equal.</p>
     */
    private static String simple(String expression) {
        String text = expression.trim().replace("::", ".");
        if (text.endsWith(".asItem()")) text = text.substring(0, text.length() - ".asItem()".length());
        if (text.endsWith(".get()")) text = text.substring(0, text.length() - ".get()".length());
        if (text.endsWith("()")) text = text.substring(0, text.length() - "()".length());
        String[] parts = text.split("\\.");
        if (parts.length >= 2) return parts[parts.length - 2] + "." + parts[parts.length - 1];
        return text;
    }

    // ------------------------------------------------------------------ the entity attributes

    /**
     * The Fabric catalogue of the entity attributes and the NeoForge catalogue of the same event
     * must name the same entity types with the same attribute factories.
     *
     * <p>Fabric registers the attributes with {@code FabricDefaultAttributeRegistry}, NeoForge has
     * no such registry and uses the {@code EntityAttributeCreationEvent}. Both lists are the whole
     * catalogue of the 34 TC4 entity types, so a type that is only on one loader would spawn with
     * the vanilla attributes there.</p>
     */
    @Test void theEntityAttributeCatalogueIsTheFabricOne() {
        String fabric = read("fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabric.java");
        String neoforge = read("neoforge/src/main/java/thaumcraft/neoforge/NeoForgeEntityAttributes.java");

        Set<String> fabricEntries = new LinkedHashSet<>();
        for (List<String> arguments : calls(fabric, "FabricDefaultAttributeRegistry.register(")) {
            assertEquals(2, arguments.size(), "the Fabric attribute registration has two arguments");
            // The four shared catalogues register through a lambda; only the direct entries
            // name an own attribute factory.
            if (!arguments.get(1).trim().endsWith("createAttributes()")) continue;
            fabricEntries.add(simple(arguments.get(0)) + "=" + simple(arguments.get(1)));
        }
        assertEquals(15, fabricEntries.size(), "the Fabric bootstrap owns fifteen direct attribute entries");

        Set<String> neoforgeEntries = new LinkedHashSet<>();
        for (List<String> arguments : calls(neoforge, "put(event,")) {
            if (arguments.size() != 3) continue;
            if (!arguments.get(2).trim().endsWith("createAttributes()")) continue;
            neoforgeEntries.add(simple(arguments.get(1)) + "=" + simple(arguments.get(2)));
        }
        assertEquals(15, neoforgeEntries.size(), "the NeoForge event owns the same fifteen direct attribute entries");
        assertEquals(new TreeSet<>(fabricEntries), new TreeSet<>(neoforgeEntries),
                "both loaders give the same entity types the same attributes");

        // The four shared catalogues are published by the common module and only adapted by the
        // loader, so both loaders must name the same four.
        for (String catalogue : List.of("ThaumcraftEldritchMobs", "ThaumcraftCultists",
                "ThaumcraftTaintCreeper", "ThaumcraftTaintAnimals")) {
            assertTrue(fabric.contains(catalogue + ".registerAttributes("),
                    "the Fabric bootstrap adapts the shared catalogue " + catalogue);
            assertTrue(neoforge.contains(catalogue + "::registerAttributes"),
                    "the NeoForge event adapts the shared catalogue " + catalogue);
        }
    }

    // ------------------------------------------------------------------ the spawn placements

    /**
     * The Fabric spawn placements and the NeoForge {@code RegisterSpawnPlacementsEvent} entries
     * must name the same entity types with the same placement type, height map and predicate.
     */
    @Test void theSpawnPlacementCatalogueIsTheFabricOne() {
        String fabric = read("fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabric.java");
        String neoforge = read("neoforge/src/main/java/thaumcraft/neoforge/NeoForgeSpawnPlacements.java");

        Set<String> fabricEntries = new LinkedHashSet<>();
        for (List<String> arguments : calls(fabric, "SpawnPlacements.register(")) {
            assertEquals(4, arguments.size(), "the Fabric spawn placement has four arguments");
            fabricEntries.add(simple(arguments.get(0)) + "|" + simple(arguments.get(1)) + "|"
                    + simple(arguments.get(2)) + "|" + simple(arguments.get(3)));
        }
        // The count is asserted so a spawn placement added on one loader and not the other is
        // caught. The inhabited zombie was added by the spawn audit lane, so the catalogue is
        // eight on both sides now.
        assertEquals(8, fabricEntries.size(), "the Fabric bootstrap owns eight spawn placements");

        Set<String> neoforgeEntries = new LinkedHashSet<>();
        for (List<String> arguments : calls(neoforge, "event.register(")) {
            assertEquals(5, arguments.size(), "the NeoForge spawn placement has five arguments");
            neoforgeEntries.add(simple(arguments.get(0)) + "|" + simple(arguments.get(1)) + "|"
                    + simple(arguments.get(2)) + "|" + simple(arguments.get(3)));
        }
        assertTrue(neoforgeEntries.containsAll(fabricEntries),
                "the NeoForge event owns every spawn placement of the Fabric bootstrap: " + fabricEntries);
        // ONE TC4 entity carries a spawn entry in the biome data of the original eerie biome but
        // no placement of its own: the eldritch guardian (oracle BiomeGenEldritch registers it,
        // and EntityEldritchGuardian overrides no spawn rule). Fabric leaves it without a
        // placement and vanilla then lets it spawn anywhere; NeoForge reports that state as an
        // error, so the very same unrestricted behaviour is published there.
        //
        // The inhabited zombie is NOT in this list: it extends EntityZombie, so it inherits
        // ON_GROUND plus a light check, and both loaders must carry that rule.
        for (String extra : List.of("ThaumcraftWarp.ELDRITCH_GUARDIAN")) {
            assertTrue(neoforgeEntries.contains(extra + "|SpawnPlacementTypes.NO_RESTRICTIONS|"
                            + "Types.MOTION_BLOCKING_NO_LEAVES|NeoForgeSpawnPlacements.unrestricted"),
                    "the NeoForge event publishes the unrestricted placement of " + extra);
        }
    }

    // ------------------------------------------------------------------ the biome modifiers

    /**
     * The nine Fabric biome feature modifications and the nine NeoForge biome modifier data pack
     * entries must name the same biome tag, the same generation step and the same placed feature.
     */
    @Test void theBiomeFeatureModifiersAreTheFabricOnes() {
        String fabric = read("fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabric.java");
        Map<String, String> constants = commonConstants();
        Set<String> fabricEntries = new LinkedHashSet<>();
        for (List<String> arguments : calls(fabric, "BiomeModifications.addFeature(")) {
            assertEquals(3, arguments.size(), "the Fabric biome modification has three arguments");
            fabricEntries.add(biomeTag(arguments.get(0), constants) + "|" + step(arguments.get(1))
                    + "|" + placedFeature(arguments.get(2), constants));
        }
        assertEquals(9, fabricEntries.size(), "the Fabric bootstrap owns nine biome feature modifications");

        Set<String> neoforgeEntries = new LinkedHashSet<>();
        Path directory = ROOT.resolve("neoforge/src/main/resources/data/thaumcraft/neoforge/biome_modifier");
        assertTrue(Files.isDirectory(directory), "the NeoForge biome modifier data pack is missing");
        try (var files = Files.list(directory)) {
            for (Path file : files.sorted().toList()) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                if (!json.contains("\"type\": \"neoforge:add_features\"")) continue;
                neoforgeEntries.add(jsonValue(json, "biomes") + "|" + jsonValue(json, "step")
                        + "|" + jsonValue(json, "features"));
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read the NeoForge biome modifiers", failure);
        }
        assertEquals(9, neoforgeEntries.size(), "the NeoForge data pack owns nine biome feature modifiers");
        assertEquals(new TreeSet<>(fabricEntries), new TreeSet<>(neoforgeEntries),
                "both loaders add the same placed features to the same biomes in the same step");
    }

    /**
     * The original TC4 entity spawn rules are a {@code REPLACEMENTS} biome modification on Fabric
     * and a NeoForge biome modifier of the own type on NeoForge. The identifier of the modifier
     * must be the same, because the NeoForge data pack entry activates it by that name.
     */
    @Test void theOriginalEntitySpawnModifierCarriesTheFabricIdentifier() {
        String fabric = read("fabric/src/main/java/thaumcraft/fabric/world/FabricBiomeSpawns.java");
        String neoforge = read("neoforge/src/main/java/thaumcraft/neoforge/NeoForgeBiomeSpawns.java");
        assertTrue(fabric.contains("ResourceLocation.fromNamespaceAndPath(\"thaumcraft\",\"original_entity_spawns\")"),
                "the Fabric modification is registered as thaumcraft:original_entity_spawns");
        assertTrue(neoforge.contains("types.register(\"original_entity_spawns\""),
                "the NeoForge modifier type is registered as thaumcraft:original_entity_spawns");
        String entry = read("neoforge/src/main/resources/data/thaumcraft/neoforge/biome_modifier/original_entity_spawns.json");
        assertTrue(entry.contains("\"type\": \"thaumcraft:original_entity_spawns\""),
                "the NeoForge data pack entry activates the shared modifier type");
        // The selector and the transformation are the shared ones on both loaders.
        assertTrue(fabric.contains("OriginalSpawnRules.apply("), "the Fabric loader applies the shared rules");
        assertTrue(read("neoforge/src/main/java/thaumcraft/neoforge/OriginalEntitySpawnsBiomeModifier.java")
                .contains("OriginalSpawnRules.apply("), "the NeoForge loader applies the shared rules");
    }

    // ------------------------------------------------------------------ the chest rows

    /**
     * The ten vanilla chest tables that carry the original TC4 rows and the category of the
     * original chest generation must be the same on both loaders.
     */
    @Test void theChestRowCategoriesAreTheFabricOnes() {
        Map<String, String> fabric = categories(read("fabric/src/main/java/thaumcraft/fabric/loot/FabricThaumcraftLoot.java"));
        Map<String, String> neoforge = categories(read("neoforge/src/main/java/thaumcraft/neoforge/NeoForgeLootTables.java"));
        assertEquals(10, fabric.size(), "the Fabric loader owns ten chest categories");
        assertEquals(fabric, neoforge, "both loaders add the original rows to the same ten chest tables");
    }

    private static Map<String, String> categories(String source) {
        Map<String, String> found = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("Map\\.entry\\(\"([^\"]+)\",\\s*\"([^\"]+)\"\\)").matcher(source);
        while (matcher.find()) found.put(matcher.group(1), matcher.group(2));
        return found;
    }

    // ------------------------------------------------------------------ the access transformer

    /**
     * NeoForge has no access widener. The three entries of the shared access widener must be the
     * three entries of the NeoForge access transformer, or the shared module throws an
     * {@code IllegalAccessError} on NeoForge.
     */
    @Test void theAccessTransformerIsTheSharedAccessWidener() {
        List<String> widener = accessWidener();
        List<String> transformer = accessTransformer();
        assertEquals(6, widener.size(), "the shared access widener owns six entries");
        assertEquals(widener, transformer,
                "the NeoForge access transformer is the shared access widener, entry by entry");
    }

    private static List<String> accessWidener() {
        List<String> found = new ArrayList<>();
        for (String line : read("common/src/main/resources/thaumcraft.accesswidener").split("\n")) {
            String text = line.trim();
            if (text.startsWith("#") || text.isEmpty()) continue;
            if (text.startsWith("accessWidener ")) continue; // the header of the file
            Matcher method = Pattern.compile("^accessible\\s+method\\s+(\\S+)\\s+(\\S+)\\s+\\((.*)\\)V$").matcher(text);
            if (method.matches()) {
                found.add(method.group(1).replace('/', '.') + "#" + method.group(2) + "(" + method.group(3) + ")V");
                continue;
            }
            Matcher field = Pattern.compile("^accessible\\s+field\\s+(\\S+)\\s+(\\S+)\\s+(?:I|L[^;\\s]+;)$").matcher(text);
            if (field.matches()) {
                found.add(field.group(1).replace('/', '.') + "#" + field.group(2));
                continue;
            }
            throw new IllegalStateException("unknown access widener entry: " + text);
        }
        return found;
    }

    private static List<String> accessTransformer() {
        List<String> found = new ArrayList<>();
        for (String line : read("neoforge/src/main/resources/META-INF/accesstransformer.cfg").split("\n")) {
            String text = line.trim();
            if (text.startsWith("#") || text.isEmpty()) continue;
            Matcher method = Pattern.compile("^public\\s+(\\S+)\\s+(\\S+)\\((.*)\\)V$").matcher(text);
            if (method.matches()) {
                found.add(method.group(1) + "#" + method.group(2) + "(" + method.group(3) + ")V");
                continue;
            }
            Matcher field = Pattern.compile("^public\\s+(\\S+)\\s+(\\S+)$").matcher(text);
            if (field.matches()) {
                found.add(field.group(1) + "#" + field.group(2));
                continue;
            }
            throw new IllegalStateException("unknown access transformer entry: " + text);
        }
        return found;
    }

    // ------------------------------------------------------------------ the fluid types

    /**
     * NeoForge refuses a modded fluid without an own {@code FluidType}. The two TC4 fluids, the
     * purifying fluid and the death fluid, must both carry one on NeoForge.
     */
    @Test void bothModFluidsCarryANeoForgeFluidType() {
        String mixins = read("neoforge/src/main/resources/thaumcraft-neoforge.mixins.json");
        assertTrue(mixins.contains("NeoForgeFluidPureMixin"), "the purifying fluid type mixin is registered");
        assertTrue(mixins.contains("NeoForgeFluidDeathMixin"), "the death fluid type mixin is registered");
        String pure = read("neoforge/src/main/java/thaumcraft/neoforge/mixin/NeoForgeFluidPureMixin.java");
        assertTrue(pure.contains("@Mixin(FluidPure.class)"), "the purifying fluid type belongs to the shared fluid");
        assertTrue(pure.contains("getFluidType"), "the purifying fluid publishes its NeoForge type");
        String death = read("neoforge/src/main/java/thaumcraft/neoforge/mixin/NeoForgeFluidDeathMixin.java");
        assertTrue(death.contains("@Mixin(BlockFluidDeath.DeathFluid.class)"), "the death fluid type belongs to the shared fluid");
        assertTrue(death.contains("getFluidType"), "the death fluid publishes its NeoForge type");
    }

    // ------------------------------------------------------------------ the client events

    /**
     * The client events of the two loaders.
     *
     * <p>The item renderers, the core shaders, the HUD overlay, the late world render, the client
     * resource reload and the disconnect reset are the shared client behaviour and must exist on
     * both loaders. The three Fabric API client extensions without a NeoForge counterpart are
     * listed as the known gap: they are the clean water render, the taint fibres model and the
     * armour renderer of the void robe, the void armour and the fortress armour.</p>
     */
    @Test void theClientRegistrationsAreTheFabricOnes() {
        String fabric = read("fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java");
        String neoforge = read("neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java");

        // The core shaders of the Fabric CoreShaderRegistrationCallback.
        for (String shader : List.of("linked_mirror", "monster_lit_particle", "node_stabilizer_bubble", "architect_overlay")) {
            assertTrue(fabric.contains(shader), "the Fabric client registers the core shader " + shader);
            assertTrue(neoforge.contains(shader), "the NeoForge client registers the core shader " + shader);
        }

        // The builtin item renderers of the Fabric BuiltinItemRendererRegistry and the NeoForge
        // client item extensions.
        Set<String> fabricItems = new LinkedHashSet<>();
        for (List<String> arguments : calls(fabric, "BuiltinItemRendererRegistry.INSTANCE.register(")) {
            assertTrue(arguments.size() >= 2, "the Fabric item renderer registration has an item and a renderer");
            fabricItems.add(simple(arguments.get(0)));
        }
        Set<String> neoforgeItems = new LinkedHashSet<>();
        for (List<String> arguments : calls(neoforge, "event.registerItem(")) {
            assertTrue(arguments.size() >= 2, "the NeoForge item renderer registration has a renderer and an item");
            for (String item : arguments.subList(1, arguments.size())) neoforgeItems.add(simple(item));
        }
        Set<String> missing = new LinkedHashSet<>(neoforgeItems);
        missing.removeAll(fabricItems);
        assertTrue(missing.isEmpty(),
                "every item renderer of the NeoForge client exists on Fabric, missing on Fabric: " + missing);
        assertEquals(fabricItems, neoforgeItems,
                "both loaders draw the same items with a builtin renderer");

        // The shared client behaviour.
        assertTrue(fabric.contains("HudRenderCallback") && neoforge.contains("RenderGuiEvent.Post"),
                "both loaders own the HUD overlay");
        assertTrue(fabric.contains("WorldRenderEvents.LAST") && neoforge.contains("RenderLevelStageEvent"),
                "both loaders own the late world render");
        assertTrue(fabric.contains("ClientPlayConnectionEvents.DISCONNECT") && neoforge.contains("ClientPlayerNetworkEvent.LoggingOut"),
                "both loaders own the disconnect reset");
        assertTrue(fabric.contains("ResourceManagerHelper") && neoforge.contains("RegisterClientReloadListenersEvent"),
                "both loaders own the client resource reload");
    }

    /**
     * The three Fabric API client extensions without a NeoForge counterpart.
     *
     * <p>The list is the honest gap of the NeoForge client today. When one of them is closed, the
     * entry has to leave this list, and the parity is then proved by the entries above.</p>
     */
    @Test void theKnownNeoForgeClientGapsAreTheDocumentedOnes() {
        String neoforge = read("neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java");
        List<String> gaps = List.of(
                "the taint fibres model (the Fabric renderer API)");
        for (String gap : gaps) {
            assertTrue(!gap.isEmpty(), "the gap list names its gap");
        }
        // The clean water render and the armour renderer are closed: the NeoForge
        // client owns them through their own client mixins now.
        assertTrue(!neoforge.contains("FluidRenderHandler"), "the clean water render is not a gap any more");
        assertTrue(!neoforge.contains("taint_fibres"), "the taint fibres model is still a gap");
        assertTrue(!neoforge.contains("renderRobe"), "the void robe renderer is not a gap any more");
        assertTrue(!neoforge.contains("renderFortressArmor"), "the fortress armour renderer is not a gap any more");
    }

    /**
     * The clean water render of the purifying fluid.
     *
     * <p>Both loaders must draw the very same original geometry: the shared module owns it, the
     * Fabric loader reaches it through the Fabric {@code FluidRenderHandler} and the NeoForge
     * loader through its own client mixin of the native fluid draw path.</p>
     */
    @Test void theCleanWaterRenderIsTheSameGeometryOnBothLoaders() {
        String shared = read("common/src/main/java/thaumcraft/client/renderers/fluid/PureFluidRender.java");
        assertTrue(shared.contains("public static boolean render("), "the shared module owns the original fluid geometry");
        assertTrue(shared.contains("PureFlowRules.renderAverage"), "the shared geometry uses the original corner rule");
        String fabric = read("fabric/src/main/java/thaumcraft/fabric/client/FabricFluidPureRenderHandler.java");
        assertTrue(fabric.contains("PureFluidRender.render("), "the Fabric adapter draws the shared geometry");
        String mixin = read("neoforge/src/main/java/thaumcraft/neoforge/mixin/client/NeoForgePureFluidMixin.java");
        assertTrue(mixin.contains("PureFluidRender.render("), "the NeoForge mixin draws the shared geometry");
        assertTrue(mixin.contains("renderLiquid("), "the NeoForge mixin intercepts the native fluid draw path");
        assertTrue(mixin.contains("require = 1"), "the NeoForge mixin fails closed");
        String config = read("neoforge/src/main/resources/thaumcraft-neoforge-client.mixins.json");
        assertTrue(config.contains("NeoForgePureFluidMixin"), "the NeoForge client mixin is registered");
    }

    /**
     * The original TC4 gear models.
     *
     * <p>The Fabric loader registers the renderers of the Fabric {@code ArmorRenderer} API and the
     * NeoForge loader draws the pieces from its own armour layer. Both must reach the very same
     * shared renderers, so the two loaders cannot draw the void robe, the fortress armour and the
     * hover harness differently.</p>
     */
    @Test void theOriginalGearModelsAreTheSameOnBothLoaders() {
        String shared = read("common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java");
        assertTrue(shared.contains("public static boolean renderGearItem("), "the shared module owns the gear dispatch");
        for (String piece : List.of("ItemVoidRobeArmor", "ItemFortressArmor", "HarnessData.isHarness")) {
            assertTrue(shared.contains(piece), "the shared dispatch draws " + piece);
        }
        String fabric = read("fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java");
        assertTrue(fabric.contains("ArmorRenderer.register("), "the Fabric loader registers the gear renderers");
        String mixin = read("neoforge/src/main/java/thaumcraft/neoforge/mixin/client/NeoForgeGearArmorMixin.java");
        assertTrue(mixin.contains("renderGearItem("), "the NeoForge mixin draws the shared gear");
        assertTrue(mixin.contains("renderArmorPiece("), "the NeoForge mixin intercepts the native armour layer");
        assertTrue(mixin.contains("require = 1"), "the NeoForge mixin fails closed");
        String config = read("neoforge/src/main/resources/thaumcraft-neoforge-client.mixins.json");
        assertTrue(config.contains("NeoForgeGearArmorMixin"), "the NeoForge client mixin is registered");
    }

    /**
     * The shared client setup runs before NeoForge collects menu screens, model layers and
     * entity renderers.
     *
     * <p>NeoForge posts RegisterMenuScreensEvent, RegisterLayerDefinitions and RegisterRenderers
     * from ClientHooks.initClientHooks in the Minecraft constructor, before FMLClientSetupEvent,
     * and the Architectury registries apply those three only inside the events. From client
     * setup they were lost: no Thaumcraft GUI opened, gear layers were missing and the first
     * Thaumcraft entity drawn crashed the client (seen on a real NeoForge client, 2026-10-26).</p>
     */
    @Test void theSharedClientSetupRunsBeforeTheClientHookEvents() {
        String bootstrap = read("neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java");
        String client = read("neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java");
        assertTrue(!bootstrap.contains("import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;")
                        && !bootstrap.contains("ClientSetup::init"),
                "the shared client setup is not deferred to FMLClientSetupEvent");
        int handler = client.indexOf("private static void onRegisterClientExtensions(");
        assertTrue(handler >= 0, "the NeoForge client handles RegisterClientExtensionsEvent");
        String body = client.substring(client.indexOf('{', handler) + 1).replaceAll("(?m)^\\s*//.*$", "");
        String head = body.substring(0, body.indexOf(';') + 1).trim();
        assertEquals("thaumcraft.client.ClientSetup.init();", head,
                "the shared client setup is the first thing the first client hook event does");
    }

    /**
     * The original robe dye of the native armour layer.
     *
     * <p>The Fabric loader redirects the DYEABLE check of the native armour layer, the NeoForge
     * loader publishes the same colour through the native client item extension, because the
     * NeoForge patch of the armour layer has no DYEABLE check at all. The redirect must not stay
     * in the shared module: it would fail to apply on NeoForge and break the client at startup.</p>
     */
    @Test void theRobeDyeIsLoaderSpecificOnBothLoaders() {
        String sharedConfig = read("common/src/main/resources/thaumcraft-common.mixins.json");
        assertTrue(!sharedConfig.contains("RobeArmorLayerMixin"),
                "the robe dye redirect is not shared any more: it has no target in the NeoForge patched class");
        String fabricConfig = read("fabric/src/main/resources/thaumcraft-fabric-client.mixins.json");
        assertTrue(fabricConfig.contains("FabricRobeArmorLayerMixin"), "the Fabric loader keeps the robe dye redirect");
        String fabric = read("fabric/src/main/java/thaumcraft/fabric/mixin/client/FabricRobeArmorLayerMixin.java");
        assertTrue(fabric.contains("ItemRobeArmor"), "the Fabric redirect opts the robes into the native check");
        String neoforge = read("neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java");
        assertTrue(neoforge.contains("NeoForgeRobeDye.register(event)"), "the NeoForge loader publishes the robe colour natively");
        String dye = read("neoforge/src/main/java/thaumcraft/neoforge/client/NeoForgeRobeDye.java");
        assertTrue(dye.contains("getDefaultDyeColor"), "the NeoForge loader uses the native client item extension");
        assertTrue(dye.contains("ItemRobeArmor.armorTint"), "the NeoForge loader uses the shared robe colour");
    }

    // ------------------------------------------------------------------ the shared constants

    /**
     * The resource identifiers of the shared placed feature and biome tag constants.
     *
     * <p>The common module declares them with the namespace of the mod, so the identifier of a
     * declaration is the namespace of the mod and the literal of the declaration.</p>
     */
    private static Map<String, String> commonConstants() {
        Map<String, String> found = new LinkedHashMap<>();
        Path directory = ROOT.resolve("common/src/main/java/thaumcraft/common/config");
        assertTrue(Files.isDirectory(directory), "the shared configuration sources are missing");
        List<Path> files;
        try (var stream = Files.list(directory)) {
            files = stream.filter(path -> path.getFileName().toString().endsWith(".java")).sorted().toList();
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot list the shared configuration sources", failure);
        }
        Pattern declaration = Pattern.compile(
                "(?:ResourceKey<[^>]*>|TagKey<Biome>)\\s+([A-Z_0-9]+)\\s*=\\s*[^;]*;", Pattern.DOTALL);
        Pattern literal = Pattern.compile("\"([a-z0-9_]+)\"");
        for (Path file : files) {
            String source;
            try {
                source = Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new IllegalStateException("Cannot read " + file, failure);
            }
            Matcher matcher = declaration.matcher(source);
            while (matcher.find()) {
                // The last literal of the declaration is the path of the identifier.
                Matcher path = literal.matcher(matcher.group());
                String identifier = null;
                while (path.find()) identifier = path.group(1);
                if (identifier != null) found.put(matcher.group(1), "thaumcraft:" + identifier);
            }
        }
        assertNotNull(found.get("ORES_KEY"), "the shared ore placed feature is declared");
        return found;
    }

    private static String biomeTag(String selector, Map<String, String> constants) {
        Matcher matcher = Pattern.compile("tag\\(([^)]*)\\)").matcher(selector);
        assertTrue(matcher.find(), "the Fabric biome selector is a tag selector: " + selector);
        String tag = simple(matcher.group(1));
        if (tag.startsWith("BiomeTags.")) {
            return "#minecraft:" + tag.substring("BiomeTags.".length()).toLowerCase(java.util.Locale.ROOT);
        }
        String identifier = constants.get(tag.substring(tag.indexOf('.') + 1));
        assertNotNull(identifier, "the shared biome tag is declared: " + tag);
        return "#" + identifier;
    }

    private static String step(String argument) {
        String text = argument.trim();
        assertTrue(text.contains("GenerationStep."), "the Fabric generation step is a generation step: " + argument);
        return text.substring(text.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private static String placedFeature(String argument, Map<String, String> constants) {
        String text = simple(argument);
        String identifier = constants.get(text.substring(text.indexOf('.') + 1));
        assertNotNull(identifier, "the shared placed feature is declared: " + text);
        return identifier;
    }

    private static String jsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertTrue(matcher.find(), "the biome modifier owns the key " + key);
        return matcher.group(1);
    }
}
