package thaumcraft.fabric.clienttest;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.items.CrystalEssenceData;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.JarLabelData;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.WispEssenceData;
import thaumcraft.common.items.relics.ScanSession;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.items.wands.foci.ItemFocusFrost;
import thaumcraft.common.items.wands.foci.ItemFocusShock;
import thaumcraft.common.lib.research.ResearchNoteData;

/** Private READ-ONLY observation of an ACTUAL stack, not stock or admission.
 * No live stack copy/save/asPatch, live collection views, expected-tag decoder,
 * loot/gameplay calls, inventory installation, owner selection or world loads.
 * Known codecs only, Mojmap dev only. Callers contain/latch all failures. */
final class BorePassiveStackSnapshot {
    private static final int MAX_COMPONENTS = 256, MAX_TEXT = 65_536, MAX_NBT_NODES = 8192, MAX_DEPTH = 64;
    private static final int MAX_ARRAY = 65_536, MAX_ENCHANT_TABLE = 4096;
    private static final Set<String> IMMUTABLE_TAGS = Set.of("net.minecraft.nbt.EndTag", "net.minecraft.nbt.ByteTag",
            "net.minecraft.nbt.ShortTag", "net.minecraft.nbt.IntTag", "net.minecraft.nbt.LongTag",
            "net.minecraft.nbt.FloatTag", "net.minecraft.nbt.DoubleTag", "net.minecraft.nbt.StringTag");

    private BorePassiveStackSnapshot() { }

    /** Exact persistent native ItemStack.CODEC-shaped typed tag, from actual raw
     * fields only. Empty stacks are handled by the caller's EMPTY/null schema. */
    static CompoundTag nativeTag(ItemStack actual, ServerLevel level) {
        return tag(capture(actual, level, false, false), level);
    }

    /** Same serializer for the finite ambient item domain, NEVER TC components.
     * Flesh requires an empty raw patch. This is not a birth admission decision. */
    static CompoundTag ambientTag(ItemStack actual, ServerLevel level) {
        return tag(capture(actual, level, true, false), level);
    }

    /** Actual old-jar comparison ONLY, never installed as physical stock. The
     * exact raw patch is cloned without fromPatch/ItemStack codec normalization.
     * Mutable payloads are detached; only the canonical immutable Item prototype
     * and registry identities are shared. No expected/example tag input exists. */
    static ItemStack copyForComparison(ItemStack actual, ServerLevel level) {
        Captured captured = capture(actual, level, false, true);
        require(captured.item.getClass() == ItemJar.class && captured.id.equals("thaumcraft:jar") && captured.count == 1,
                "Comparison clone is ONLY the actual original count1 jar");
        require(captured.prototype == read(captured.item, "components"), "Comparison requires the actual canonical Item prototype");
        try {
            // Normal native ItemStack constructors invoke this virtual hook.
            // Only the pinned native Item no-op implementation is supported;
            // no BlockItem/custom verifier or gameplay override may execute.
            require(captured.item.getClass().getMethod("verifyComponentsAfterLoad", ItemStack.class).getDeclaringClass() == Item.class,
                    "Comparison constructor requires the inspected native no-op verifier");
            require(captured.item.getClass().getMethod("asItem").getDeclaringClass() == Item.class,
                    "Comparison constructor requires the inspected native identity ItemLike getter");
            Constructor<PatchedDataComponentMap> mapConstructor = PatchedDataComponentMap.class.getDeclaredConstructor(
                    DataComponentMap.class, Reference2ObjectMap.class, boolean.class);
            mapConstructor.setAccessible(true);
            PatchedDataComponentMap privateComponents = mapConstructor.newInstance(captured.prototype,
                    read(captured.patch, "map"), true);
            Constructor<ItemStack> stackConstructor = ItemStack.class.getDeclaredConstructor(ItemLike.class,
                    int.class, PatchedDataComponentMap.class);
            stackConstructor.setAccessible(true);
            ItemStack privateComparison = stackConstructor.newInstance(captured.item, captured.count, privateComponents);
            require(privateComparison != actual && read(privateComparison, "components") != read(actual, "components"),
                    "Comparison clone must not alias the actual source stack/map");
            require(tag(captured, level).equals(nativeTag(privateComparison, level)), "Exact actual typed raw-patch clone, no normalization");
            return privateComparison;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Private actual-stack comparison constructor binding changed", error);
        }
    }

    private static Captured capture(ItemStack actual, ServerLevel level, boolean ambient, boolean comparison) {
        return capture(actual, level, ambient, comparison, 0);
    }

    private static Captured capture(ItemStack actual, ServerLevel level, boolean ambient, boolean comparison, int focusDepth) {
        require(focusDepth >= 0 && focusDepth <= 1, "Only one known FocusData backing-stack edge");
        require(level != null && level.getServer().isSameThread(), "Passive stack observation requires actual owning server thread");
        require(actual != null && !actual.isEmpty(), "Actual nonempty stack required");
        Item item = (Item) read(actual, "item");
        var itemKey = BuiltInRegistries.ITEM.getKey(item);
        require(itemKey != null, "Actual registered item required");
        String id = itemKey.toString();
        if (ambient) require(id.equals("minecraft:bone") || id.equals("minecraft:arrow") || id.equals("minecraft:rotten_flesh"), "Only the three exact supported ambient item ids");
        int count = ((Number) read(actual, "count")).intValue();
        require(count >= 1 && count <= 99, "Native persistent stack codec count range");
        if (focusDepth == 1) require(count == 1 && nativeFocusItem(item, id), "Exact supported count1 inner focus required");
        if (comparison) require(item.getClass() == ItemJar.class && id.equals("thaumcraft:jar") && count == 1,
                "Only actual count1 jar payloads may reach comparison cloning");
        Object components = read(actual, "components"), rawPatch = read(components, "patch");
        require(components.getClass() == PatchedDataComponentMap.class, "Exact native patched component map required");
        String mapClass = rawPatch.getClass().getName();
        boolean emptyMap = mapClass.equals("it.unimi.dsi.fastutil.objects.Reference2ObjectMaps$EmptyMap");
        require(emptyMap || mapClass.equals("it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap"),
                "No foreign/lazy-view patch map implementation");
        int size = emptyMap ? 0 : ((Number) read(rawPatch, "size")).intValue();
        require(size >= 0 && size <= MAX_COMPONENTS, "Raw component count bound");
        if (ambient && id.equals("minecraft:rotten_flesh")) require(size == 0, "Ambient flesh is plain only; no explicit patches or removals");
        var detached = DataComponentPatch.builder();
        CompoundTag manualFocus = null;
        boolean focusSeen = false;
        if (!emptyMap) {
            Object[] keys = (Object[]) read(rawPatch, "key"), values = (Object[]) read(rawPatch, "value");
            require(size <= keys.length && size <= values.length, "Raw component array bounds changed");
            for (int i = 0; i < size; i++) {
                require(keys[i] instanceof DataComponentType<?> && values[i] instanceof Optional<?>, "Raw component entry shape changed");
                DataComponentType<?> type = (DataComponentType<?>) keys[i];
                Optional<?> value = (Optional<?>) values[i];
                require(type.getClass().getName().equals("net.minecraft.core.component.DataComponentType$Builder$SimpleType"),
                        "No foreign component-type method dispatch");
                var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
                require(key != null && (!ambient || key.getNamespace().equals("minecraft")), "No custom/foreign ambient components");
                String componentId = key.toString();
                if (componentId.equals("thaumcraft:wand_focus")) {
                    require(!ambient && !comparison && focusDepth == 0 && !focusSeen && item.getClass() == ItemWandCasting.class
                            && id.equals("thaumcraft:wand") && count == 1, "FocusData is ONLY one actual count1 wand's direct component");
                    require(read(type, "codec") == FocusData.CODEC, "Authoritative FocusData codec identity changed");
                    focusSeen = true;
                    if (value.isEmpty()) detached.remove(type); // Native removal encodes no payload/getter.
                    else {
                        Object data = value.orElseThrow();
                        require(data.getClass() == FocusData.class, "Exact authoritative FocusData payload required");
                        Object backing = read(data, "focus"); // NEVER data.focus(): its getter copies the live stack.
                        require(backing instanceof ItemStack && backing != actual, "Actual nonaliased FocusData backing stack required");
                        manualFocus = tag(capture((ItemStack) backing, level, false, false, focusDepth + 1), level);
                    }
                    continue; // NEVER put a FocusData payload through DataComponentPatch.CODEC.
                }
                if (focusDepth == 1) require(vanilla(type) || componentId.equals("thaumcraft:focus_upgrades"),
                        "Inner focus permits only reviewed vanilla metadata and appropriate focus upgrades, never wand data/nesting");
                Codec<?> codec = reviewedCodec(type, key.toString());
                if (codec == null) {
                    require(!ambient && !comparison && key.toString().equals("thaumcraft:scan_session")
                            && (value.isEmpty() || value.orElseThrow().getClass() == ScanSession.class), "Only known nonpersistent scan session may be omitted");
                    continue; // Native persistent patch codec omits this entry too.
                }
                add(detached, type, value, codec, level, comparison);
            }
        }
        Captured captured = new Captured(item, id, count, (DataComponentMap) read(components, "prototype"), detached.build(), size != 0, manualFocus);
        if (focusDepth == 1) validateFocus(captured);
        return captured;
    }

    private static boolean nativeFocusItem(Item item, String id) {
        return item.getClass() == ItemFocusFire.class && id.equals("thaumcraft:focus_fire")
                || item.getClass() == ItemFocusFrost.class && id.equals("thaumcraft:focus_frost")
                || item.getClass() == ItemFocusShock.class && id.equals("thaumcraft:focus_shock")
                || item.getClass() == ItemFocusExcavation.class && id.equals("thaumcraft:focus_excavation");
    }

    private static void validateFocus(Captured actual) {
        require(actual.manualFocus == null && actual.count == 1 && nativeFocusItem(actual.item, actual.id), "No second FocusData edge");
        require(actual.prototype == read(actual.item, "components")
                && actual.prototype.getClass().getName().equals("net.minecraft.core.component.DataComponentMap$Builder$SimpleMap"),
                "Inner focus requires its actual canonical native Item prototype");
        var effective = new HashMap<String, Object>();
        focusProperties(read(actual.prototype, "map"), false, effective);
        focusProperties(read(actual.patch, "map"), true, effective); // Own detached patch, including removals.
        Object maxValue = effective.get("minecraft:max_stack_size");
        require(maxValue == null || maxValue instanceof Integer, "Native focus max-stack type changed");
        int maximum = maxValue == null ? 1 : (Integer) maxValue;
        // Mirrors STRICT_CODEC validateStrict/validateComponents on this bounded
        // slice. Containers/nested stacks are forbidden instead of traversed.
        require(maximum >= actual.count && maximum <= 99 && !(effective.containsKey("minecraft:max_damage") && maximum > 1)
                && !effective.containsKey("minecraft:container"), "Inner focus violates native strict stack component/count rules");
        Object upgrades = effective.get("thaumcraft:focus_upgrades");
        if (upgrades == null) return; // The actual missing component, not an invented upgrade object.
        require(upgrades.getClass() == FocusUpgradeData.class, "Exact authoritative focus upgrade payload required");
        List<?> ranks = (List<?>) read(upgrades, "ranks");
        require(ranks.getClass().getName().equals("java.util.ImmutableCollections$ListN") && ranks.size() == 5,
                "Exact immutable five-slot focus ranks required");
        boolean excavation = actual.item.getClass() == ItemFocusExcavation.class;
        for (int i = 0; i < 5; i++) {
            require(ranks.get(i) instanceof Short, "Native focus upgrade ID scalar type changed");
            short rank = (Short) ranks.get(i);
            // Faithful current production IDs: potency0/frugal1 for all five
            // ranks; Excavation adds treasure2 at1/3/5 and enlarge3 at2/4.
            // Do not call supportedForFocus/getPossibleUpgradesByRank or a
            // RegistrySupplier.get; holes/order remain exactly as observed.
            require(rank == -1 || rank == 0 || rank == 1
                    || excavation && (rank == 2 && i % 2 == 0 || rank == 3 && i % 2 == 1), "Unsupported focus-specific rank ID");
        }
    }

    private static void focusProperties(Object raw, boolean patch, Map<String, Object> effective) {
        String name = raw.getClass().getName();
        if (name.equals("it.unimi.dsi.fastutil.objects.Reference2ObjectMaps$EmptyMap")) return;
        boolean array = name.equals("it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap");
        boolean open = name.equals("it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap");
        require(array || open, "Exact native focus-property backing map required");
        Object[] keys = (Object[]) read(raw, "key"), values = (Object[]) read(raw, "value");
        int size = ((Number) read(raw, "size")).intValue();
        require(size >= 0 && size <= MAX_COMPONENTS && keys.length == values.length && size <= keys.length
                && (!open || keys.length <= MAX_ENCHANT_TABLE && !((Boolean) read(raw, "containsNullKey"))), "Bounded native focus-property arrays required");
        int seen = 0;
        for (int i = 0, limit = array ? size : keys.length; i < limit; i++) {
            if (open && keys[i] == null) continue;
            require(keys[i] instanceof DataComponentType<?>, "Native focus-property type changed");
            DataComponentType<?> type = (DataComponentType<?>) keys[i];
            require(type.getClass().getName().equals("net.minecraft.core.component.DataComponentType$Builder$SimpleType"),
                    "No foreign focus-prototype component dispatch");
            var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            require(key != null, "Registered native focus-property type required");
            String id = key.toString();
            require(vanilla(type) || id.equals("thaumcraft:focus_upgrades") && read(type, "codec") == FocusUpgradeData.CODEC,
                    "No wand data, second focus, foreign or unreviewed component in inner prototype/patch");
            if (patch) {
                require(values[i] instanceof Optional<?>, "Native focus-property patch value changed");
                Optional<?> value = (Optional<?>) values[i];
                if (value.isPresent()) effective.put(id, value.orElseThrow());
                else effective.remove(id);
            } else {
                require(values[i] != null, "Native focus prototype cannot contain null values");
                effective.put(id, values[i]);
            }
            seen++;
        }
        require(seen == size, "Complete actual focus-property snapshot required");
    }

    private static Codec<?> reviewedCodec(DataComponentType<?> type, String id) {
        Codec<?> actual = (Codec<?>) read(type, "codec");
        if (vanilla(type)) { require(actual != null, "Known persistent vanilla codec required"); return actual; }
        Codec<?> expected = switch (id) {
            case "thaumcraft:wand_data" -> WandData.CODEC;
            case "thaumcraft:jar_label_data" -> JarLabelData.CODEC;
            case "thaumcraft:focus_upgrades" -> FocusUpgradeData.CODEC;
            case "thaumcraft:phial_data" -> PhialData.CODEC;
            case "thaumcraft:crystal_essence_data" -> CrystalEssenceData.CODEC;
            case "thaumcraft:research_note_data" -> ResearchNoteData.CODEC;
            case "thaumcraft:wisp_essence_data" -> WispEssenceData.CODEC;
            case "thaumcraft:wand_craft_cost" -> (Codec<?>) staticField(ThaumcraftWands.class, "CRAFT_COST_CODEC");
            case "thaumcraft:scan_session" -> null;
            default -> throw new IllegalStateException("Unreviewed/foreign/nested-ItemStack component codec: " + id);
        };
        require(actual == expected, "Authoritative production component codec identity changed: " + id);
        return expected;
    }

    // Identity whitelist, not a namespace-wide codec permission. In particular
    // FOOD/CONTAINER/BUNDLE_CONTENTS/CHARGED_PROJECTILES and generic TC wand_focus
    // codec dispatch are OUT. The sole known FocusData edge is manual above.
    private static boolean vanilla(DataComponentType<?> type) {
        return type == DataComponents.MAX_STACK_SIZE || type == DataComponents.MAX_DAMAGE || type == DataComponents.DAMAGE
                || type == DataComponents.CUSTOM_DATA || type == DataComponents.BLOCK_ENTITY_DATA
                || type == DataComponents.CUSTOM_NAME || type == DataComponents.ITEM_NAME || type == DataComponents.LORE
                || type == DataComponents.RARITY || type == DataComponents.UNBREAKABLE
                || type == DataComponents.ENCHANTMENTS || type == DataComponents.STORED_ENCHANTMENTS
                || type == DataComponents.ATTRIBUTE_MODIFIERS || type == DataComponents.CUSTOM_MODEL_DATA
                || type == DataComponents.REPAIR_COST || type == DataComponents.DYED_COLOR
                || type == DataComponents.ENCHANTMENT_GLINT_OVERRIDE || type == DataComponents.HIDE_TOOLTIP
                || type == DataComponents.HIDE_ADDITIONAL_TOOLTIP || type == DataComponents.FIRE_RESISTANT
                || type == DataComponents.TOOL;
    }

    @SuppressWarnings("unchecked")
    private static <T> void add(DataComponentPatch.Builder detached, DataComponentType<T> type, Optional<?> value,
                                Codec<?> reviewed, ServerLevel level, boolean comparison) {
        if (value.isEmpty()) { detached.remove(type); return; }
        Object safe = value.orElseThrow();
        if (type == DataComponents.CUSTOM_DATA || type == DataComponents.BLOCK_ENTITY_DATA) {
            require(safe.getClass() == CustomData.class, "Exact native CustomData required");
            // CustomData/CompoundTag codecs copy through map views. Deep-snapshot
            // raw NBT FIRST so those writes affect only observer-private maps.
            safe = CustomData.of((CompoundTag) nbt((Tag) read(safe, "tag"), new NbtBudget(), 0));
        } else if (type == DataComponents.ENCHANTMENTS || type == DataComponents.STORED_ENCHANTMENTS) {
            safe = enchantments(safe);
        }
        if (comparison) {
            Codec<T> codec = (Codec<T>) reviewed;
            var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag encoded = codec.encodeStart(ops, (T) safe).getOrThrow();
            require(encoded.toString().length() <= MAX_TEXT, "Comparison payload text bound");
            safe = codec.parse(ops, encoded).getOrThrow(); // Actual payload only, no ItemStack/expected-tag decoder.
        }
        detached.set(type, (T) safe); // Own builder/map ONLY.
    }

    private static ItemEnchantments enchantments(Object actual) {
        require(actual.getClass() == ItemEnchantments.class, "Exact native enchantments required");
        Object raw = read(actual, "enchantments");
        require(raw.getClass() == Object2IntOpenHashMap.class, "Exact native enchantment backing map required");
        Object[] keys = (Object[]) read(raw, "key");
        int[] values = (int[]) read(raw, "value");
        int size = ((Number) read(raw, "size")).intValue();
        require(size >= 0 && size <= MAX_COMPONENTS && keys.length == values.length && keys.length <= MAX_ENCHANT_TABLE
                && !((Boolean) read(raw, "containsNullKey")), "Bounded nonnull native enchantment map");
        var detached = new Object2IntOpenHashMap<Holder<Enchantment>>();
        int observed = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i] != null) {
            require(keys[i].getClass() == Holder.Reference.class, "Exact native enchantment registry reference required");
            @SuppressWarnings("unchecked") Holder<Enchantment> holder = (Holder<Enchantment>) keys[i];
            detached.put(holder, values[i]); // New map, never touches actual's entry views.
            observed++;
        }
        require(observed == size, "Complete raw enchantment map snapshot");
        try {
            Constructor<ItemEnchantments> constructor = ItemEnchantments.class.getDeclaredConstructor(Object2IntOpenHashMap.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(detached, (Boolean) read(actual, "showInTooltip"));
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Native private enchantment constructor changed", error); }
    }

    private static Tag nbt(Tag actual, NbtBudget budget, int depth) {
        require(actual != null && depth <= MAX_DEPTH && ++budget.nodes <= MAX_NBT_NODES, "Raw NBT depth/node bound");
        Class<?> type = actual.getClass();
        if (type == CompoundTag.class) {
            Map<?, ?> raw = (Map<?, ?>) read(actual, "tags");
            require(raw.getClass() == HashMap.class && raw.size() <= MAX_NBT_NODES, "Exact bounded native NBT HashMap required");
            var detached = new CompoundTag();
            // Exact HashMap.forEach traverses its backing nodes without making
            // a live entrySet/keySet/values view (read-only JDK bytecode checked).
            raw.forEach((key, value) -> {
                require(key instanceof String && value instanceof Tag, "Native compound entry shape changed");
                budget.text(((String) key).length());
                detached.put((String) key, nbt((Tag) value, budget, depth + 1));
            });
            return detached;
        }
        if (type == ListTag.class) {
            List<?> raw = (List<?>) read(actual, "list");
            String name = raw.getClass().getName();
            require((raw.getClass() == ArrayList.class || name.equals("java.util.ImmutableCollections$ListN")
                    || name.equals("java.util.ImmutableCollections$List12")) && raw.size() <= MAX_NBT_NODES,
                    "Exact bounded native NBT indexed list required");
            var detached = new ArrayList<Tag>(raw.size());
            for (int i = 0; i < raw.size(); i++) {
                require(raw.get(i) instanceof Tag, "Native NBT list element shape changed");
                detached.add(nbt((Tag) raw.get(i), budget, depth + 1));
            }
            try {
                Constructor<ListTag> constructor = ListTag.class.getDeclaredConstructor(List.class, byte.class);
                constructor.setAccessible(true);
                return constructor.newInstance(detached, ((Number) read(actual, "type")).byteValue());
            } catch (ReflectiveOperationException error) { throw new IllegalStateException("Typed private NBT list constructor changed", error); }
        }
        if (type == ByteArrayTag.class) {
            byte[] raw = (byte[]) read(actual, "data"); require(raw.length <= MAX_ARRAY, "NBT byte-array bound");
            return new ByteArrayTag(raw.clone());
        }
        if (type == IntArrayTag.class) {
            int[] raw = (int[]) read(actual, "data"); require(raw.length <= MAX_ARRAY, "NBT int-array bound");
            return new IntArrayTag(raw.clone());
        }
        if (type == LongArrayTag.class) {
            long[] raw = (long[]) read(actual, "data"); require(raw.length <= MAX_ARRAY, "NBT long-array bound");
            return new LongArrayTag(raw.clone());
        }
        require(IMMUTABLE_TAGS.contains(type.getName()), "Unsupported native NBT implementation");
        if (type.getName().equals("net.minecraft.nbt.StringTag")) budget.text(actual.getAsString().length());
        return actual; // Exact native scalar tags are immutable; preserve scalar types.
    }

    private static CompoundTag tag(Captured actual, ServerLevel level) {
        var detached = new CompoundTag();
        detached.putString("id", actual.id); detached.putInt("count", actual.count);
        // A raw transient-only patch still emits components:{} in native CODEC.
        if (actual.rawPatchNonempty) {
            Tag encoded = DataComponentPatch.CODEC.encodeStart(
                    level.registryAccess().createSerializationContext(NbtOps.INSTANCE), actual.patch).getOrThrow();
            require(encoded instanceof CompoundTag, "Native persistent patch codec map shape changed");
            CompoundTag components = (CompoundTag) encoded;
            if (actual.manualFocus != null) {
                require(!components.contains("thaumcraft:wand_focus"), "FocusData must not also enter generic codec dispatch");
                // FocusData.CODEC is STRICT_CODEC.flatXmap(..., data.focus()).
                // Encode the actual backing stack tag directly, with no wrapper,
                // getter/copy, FocusData construction or expected metadata.
                components.put("thaumcraft:wand_focus", actual.manualFocus);
            }
            detached.put("components", components);
        }
        require(detached.toString().length() <= MAX_TEXT, "Full typed native stack text bound");
        return detached;
    }

    private static Object read(Object actual, String name) {
        return field(actual.getClass(), name, actual);
    }
    private static Object staticField(Class<?> owner, String name) { return field(owner, name, null); }
    private static Object field(Class<?> owner, String name, Object actual) {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(actual); }
            catch (NoSuchFieldException absent) { }
            catch (ReflectiveOperationException error) { throw new IllegalStateException("Raw passive stack field unreadable: " + name, error); }
        }
        throw new IllegalStateException("Raw passive stack field missing: " + name);
    }
    private record Captured(Item item, String id, int count, DataComponentMap prototype, DataComponentPatch patch, boolean rawPatchNonempty, CompoundTag manualFocus) { }
    private static final class NbtBudget {
        int nodes, text;
        void text(int chars) { require(chars >= 0 && chars <= MAX_TEXT - text, "Raw NBT text bound"); text += chars; }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
