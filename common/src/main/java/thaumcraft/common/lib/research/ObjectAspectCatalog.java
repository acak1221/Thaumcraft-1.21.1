package thaumcraft.common.lib.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.ThaumcraftApi;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Explicit direct bindings only; recipe-generated/complex tags are not guessed. */
public final class ObjectAspectCatalog {
    private static final String RESOURCE = "data/thaumcraft/tc4/object_aspects.json";
    private static final Logger LOG = LoggerFactory.getLogger("Thaumcraft");
    // Explicit flattening of the retained native metadata, matching the target
    // registry's declared order. These are identifiers, not invented aspects.
    private static final List<String> MATERIAL_IDS = List.of(
            "alumentum", "nitor", "thaumium_ingot", "quick_silver", "magic_tallow", "zombie_brain",
            "amber_gem", "enchanted_fabric", "vis_filter", "knowledge_fragment", "mirrored_glass",
            "tainted_goo", "taint_tendril", "jar_label", "salis_mundus", "primal_charm",
            "void_ingot", "void_seed", "gold_coin");
    private static final List<String> SHARD_IDS = List.of(
            "air_shard", "fire_shard", "water_shard", "earth_shard", "order_shard", "entropy_shard", "balance_shard");
    // Original BlockCustomOre metadata0..7, checked against TC4 ConfigAspects373–380.
    private static final List<String> ORE_IDS = List.of(
            "cinnabar_ore", "air_infused_stone", "fire_infused_stone", "water_infused_stone",
            "earth_infused_stone", "order_infused_stone", "entropy_infused_stone", "amber_ore");
    /**
     * The retained 1.12 fallback corpus rows that the 1.7.10 original never registered. They must
     * not become direct object tags here: the original resolves those items from their recipes.
     */
    private static final Set<String> LEGACY_ONLY_FALLBACK_IDS = Set.of("thaumcraft:balance_shard");
    private static volatile Map<ResourceLocation, AspectList> bindings = Map.of();
    private static Map<ResourceLocation, AspectList> candidateBindings = Map.of();
    private static Set<ResourceLocation> blockCandidateIds = Set.of();
    private static Set<ResourceLocation> expectedContentIds = Set.of();
    private static volatile Set<ResourceLocation> conflictingIds = Set.of();
    private static boolean loaded;
    private static boolean publicTagsInitialized;

    private ObjectAspectCatalog() {}

    public static synchronized void loadBundled() {
        if (loaded) return;
        var stream = ObjectAspectCatalog.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing object aspect corpus: " + RESOURCE);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("schema_version").getAsInt() != 1) throw new IllegalStateException("Unsupported object aspect schema");
            Map<ResourceLocation, AspectList> loaded = new LinkedHashMap<>();
            Set<ResourceLocation> conflicts = new LinkedHashSet<>();
            Set<ResourceLocation> blocks = new LinkedHashSet<>();
            Set<ResourceLocation> ownContent = new LinkedHashSet<>();
            for (JsonElement element : root.getAsJsonArray("bindings")) {
                JsonObject row = element.getAsJsonObject();
                if (!"registerObjectTag".equals(row.get("registration").getAsString())
                        || !row.getAsJsonArray("conditions").isEmpty() || row.get("aspects").isJsonNull()) continue;
                AspectList aspects = new AspectList();
                for (Map.Entry<String, JsonElement> amount : row.getAsJsonObject("aspects").entrySet()) {
                    Aspect aspect = Aspect.getAspect(amount.getKey());
                    if (aspect == null || amount.getValue().getAsInt() <= 0) {
                        throw new IllegalStateException("Invalid aspect binding at original source line " + row.get("source_line"));
                    }
                    aspects.add(aspect, amount.getValue().getAsInt());
                }
                if (aspects.size() == 0) continue;
                for (JsonElement id : row.getAsJsonArray("modern_ids")) {
                    ResourceLocation candidate = ResourceLocation.parse(id.getAsString());
                    putConsistent(loaded, conflicts, candidate, aspects);
                    if (row.get("legacy_target").getAsString().startsWith("Blocks.")) blocks.add(candidate);
                }
                if (row.get("legacy_meta").isJsonPrimitive() && row.get("legacy_meta").getAsJsonPrimitive().isNumber()) {
                    int metadata = row.get("legacy_meta").getAsInt();
                    String target = row.get("legacy_target").getAsString();
                    List<String> flattened = "ConfigItems.itemResource".equals(target) ? MATERIAL_IDS
                            : "ConfigItems.itemShard".equals(target) ? SHARD_IDS
                            : "ConfigBlocks.blockCustomOre".equals(target) ? ORE_IDS : List.of();
                    if (metadata >= 0 && metadata < flattened.size()) {
                        ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath("thaumcraft", flattened.get(metadata));
                        // The 1.12 backport registered a literal object tag for the balanced shard
                        // (src/main/java/thaumcraft/common/config/ConfigAspects.java:340-341) that the
                        // 1.7.10 original never had: the oracle ConfigAspects:415-420 registers only
                        // the six primal shards, and OpenTC4 1.7.10 ConfigAspects:410-415 stops at
                        // shard five. Without a direct registration the original derives the balanced
                        // shard from its crucible recipe (ThaumcraftCraftingManager:513-540), which is
                        // also the base the original salis mundus expression (ConfigAspects:421) uses.
                        if (!LEGACY_ONLY_FALLBACK_IDS.contains(candidate.toString())) {
                            putConsistent(loaded, conflicts, candidate, aspects);
                            ownContent.add(candidate);
                            if ("ConfigBlocks.blockCustomOre".equals(target)) blocks.add(candidate);
                        }
                    }
                }
            }
            // Original TC4 early-material registrations take precedence over
            // the retained 1.12 fallback corpus (e.g. coal includes ignis2).
            EarlyObjectAspectBindings.complex().keySet().forEach(id -> {
                // These selected original registrations had no explicit base.
                // A divergent legacy direct tag must not bypass recipe derivation.
                loaded.remove(id);
                conflicts.remove(id);
            });
            EarlyObjectAspectBindings.direct().forEach((id, aspects) -> {
                loaded.put(id, aspects.copy());
                conflicts.remove(id);
                if ("thaumcraft".equals(id.getNamespace())) ownContent.add(id);
            });
            // Original ConfigAspects431: crystal essence has an explicit EMPTY
            // base. Its IEssentiaContainerItem contents are added once by the
            // resolver; never inherit ethereal auram2, shard or recipe tags.
            var crystal = ResourceLocation.fromNamespaceAndPath("thaumcraft", "crystal_essence");
            loaded.put(crystal, new AspectList());
            conflicts.remove(crystal);
            ownContent.add(crystal);
            // Original ConfigAspects421 registers a direct list derived from
            // the UNCULLED balanced-shard base, not the divergent1.12 literal.
            // The native resolver evaluates that one anchored expression.
            var salis = ResourceLocation.fromNamespaceAndPath("thaumcraft", "salis_mundus");
            loaded.remove(salis);
            conflicts.remove(salis);
            bindings = Collections.unmodifiableMap(loaded);
            candidateBindings = bindings;
            blockCandidateIds = Set.copyOf(blocks);
            expectedContentIds = Set.copyOf(ownContent);
            conflictingIds = Set.copyOf(conflicts);
            ObjectAspectCatalog.loaded = true;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot load object aspect corpus", exception);
        }
    }

    /** Unknown items stay unknown until a real direct or recipe-derived tag exists. */
    public static AspectList get(ResourceLocation id) {
        AspectList aspects = bindings.get(id);
        return aspects == null ? new AspectList() : aspects.copy();
    }

    /** Callers can validate these identifiers after target registries are ready. */
    public static Set<ResourceLocation> registeredIds() { return bindings.keySet(); }

    public static Set<ResourceLocation> conflictingIds() { return conflictingIds; }

    /** One-time native registry seeding. Later public removals/replacements are never silently restored. */
    public static synchronized void initializePublicTags() {
        if (publicTagsInitialized) return;
        loadBundled();
        for (var entry : bindings.entrySet()) {
            if (!BuiltInRegistries.ITEM.containsKey(entry.getKey())) continue;
            var item = BuiltInRegistries.ITEM.get(entry.getKey());
            Integer declared = EarlyObjectAspectBindings.metadata(entry.getKey());
            int metadata = declared != null ? declared : new ItemStack(item).isDamageableItem() ? ObjectAspectRegistry.WILDCARD : 0;
            ThaumcraftApi.objectTags.putIfAbsent(List.of(item, metadata), entry.getValue().copy());
        }
        publicTagsInitialized = true;
    }

    /** Native bundled expressions/complex recipes remain on-demand so RecipeManager reloads still take effect. */
    public static boolean hasDerivedRegistration(ResourceLocation id, int metadata) {
        Integer declared = EarlyObjectAspectBindings.complex().containsKey(id) ? EarlyObjectAspectBindings.metadata(id) : null;
        if (id.equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "thaumonomicon"))
                || id.equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "thaumonomicon_cheat"))) declared = ObjectAspectRegistry.WILDCARD;
        if (id.equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "salis_mundus"))
                || id.equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crucible"))) declared = 0;
        return declared != null && (declared == ObjectAspectRegistry.WILDCARD || declared == metadata
                || metadata == ObjectAspectRegistry.WILDCARD && declared >= 0 && declared < 16);
    }

    /** Run after both vanilla and deferred mod registrations have completed. */
    public static synchronized ValidationReport validateAgainstRegistries() {
        loadBundled();
        ValidationResult result = validate(candidateBindings, blockCandidateIds, conflictingIds,
                BuiltInRegistries.ITEM::containsKey, BuiltInRegistries.BLOCK::containsKey);
        bindings = result.accepted();
        initializePublicTags();
        ValidationReport report = result.report();
        if (!report.missingItemIds().isEmpty()) {
            LOG.warn("TC4 object aspects: unresolved item registry identifiers: {}", sortedIds(report.missingItemIds()));
        }
        if (!report.missingBlockIds().isEmpty()) {
            LOG.warn("TC4 object aspects: unresolved block registry identifiers: {}", sortedIds(report.missingBlockIds()));
        }
        if (!report.conflictingIds().isEmpty()) {
            LOG.warn("TC4 object aspects: conflicting flattened bindings remain unresolved: {}", sortedIds(report.conflictingIds()));
        }
        Set<ResourceLocation> missingOwn = new LinkedHashSet<>(report.missingItemIds());
        missingOwn.addAll(report.missingBlockIds());
        missingOwn.retainAll(expectedContentIds);
        if (!missingOwn.isEmpty()) {
            LOG.error("TC4 object aspects: explicitly registered content is absent: {}", sortedIds(missingOwn));
            throw new IllegalStateException("Expected registered Thaumcraft content is absent: " + sortedIds(missingOwn));
        }
        LOG.info("TC4 object aspect registry validation: {} accepted, {} missing item ids, {} missing block ids, {} conflicts",
                report.acceptedCount(), report.missingItemIds().size(), report.missingBlockIds().size(), report.conflictingIds().size());
        return report;
    }

    static ValidationResult validate(Map<ResourceLocation, AspectList> candidates,
                                     Set<ResourceLocation> blockIds, Set<ResourceLocation> conflicts,
                                     Predicate<ResourceLocation> hasItem, Predicate<ResourceLocation> hasBlock) {
        Map<ResourceLocation, AspectList> accepted = new LinkedHashMap<>();
        Set<ResourceLocation> missingItems = new LinkedHashSet<>();
        Set<ResourceLocation> missingBlocks = new LinkedHashSet<>();
        for (Map.Entry<ResourceLocation, AspectList> candidate : candidates.entrySet()) {
            ResourceLocation id = candidate.getKey();
            boolean itemPresent = hasItem.test(id);
            boolean blockPresent = !blockIds.contains(id) || hasBlock.test(id);
            if (!itemPresent) missingItems.add(id);
            if (!blockPresent) missingBlocks.add(id);
            if (itemPresent && blockPresent && !conflicts.contains(id)) accepted.put(id, candidate.getValue().copy());
        }
        return new ValidationResult(Collections.unmodifiableMap(accepted),
                new ValidationReport(accepted.size(), missingItems, missingBlocks, conflicts));
    }

    private static List<String> sortedIds(Set<ResourceLocation> ids) {
        return ids.stream().map(ResourceLocation::toString).sorted().toList();
    }

    public record ValidationReport(int acceptedCount, Set<ResourceLocation> missingItemIds,
                                   Set<ResourceLocation> missingBlockIds, Set<ResourceLocation> conflictingIds) {
        public ValidationReport {
            missingItemIds = Set.copyOf(missingItemIds);
            missingBlockIds = Set.copyOf(missingBlockIds);
            conflictingIds = Set.copyOf(conflictingIds);
        }
    }

    record ValidationResult(Map<ResourceLocation, AspectList> accepted, ValidationReport report) {}

    private static void putConsistent(Map<ResourceLocation, AspectList> loaded, Set<ResourceLocation> conflicts,
                                      ResourceLocation id, AspectList incoming) {
        if (conflicts.contains(id)) return;
        AspectList previous = loaded.get(id);
        if (previous != null && !previous.aspects.equals(incoming.aspects)) {
            loaded.remove(id);
            conflicts.add(id);
        } else {
            loaded.putIfAbsent(id, incoming.copy());
        }
    }
}
