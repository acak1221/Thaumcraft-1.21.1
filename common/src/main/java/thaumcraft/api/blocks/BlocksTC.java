package thaumcraft.api.blocks;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ItemApi;
import thaumcraft.common.config.ThaumcraftContent;

/**
 * The public block registry of the native port.
 *
 * <p>Forced 1.21.1 change: the original TC4 kept its blocks in {@code ConfigBlocks} and the original add-on
 * surface kept them in {@code BlocksTC}; 1.21.1 registers every block in the block registry, so this class
 * resolves the native registry instead of holding a second copy of it. A plain name means
 * {@code thaumcraft:<name>}.</p>
 *
 * <p>An original TC4 field name of {@code ConfigBlocks} ({@code "blockJar"}, {@code "blockCosmeticSolid"}) is
 * accepted too and means the block of its metadata 0; {@link #get(String, int)} and {@link #state(String, int)}
 * select another metadata. The names and metadata resolve through {@link ItemApi#legacyBlockId} and
 * {@link ItemApi#legacyBlockState} ({@code docs/api/LEGACY_IDS.md}).</p>
 */
public final class BlocksTC {
    private BlocksTC() {}

    /** Null/empty names throw IllegalArgumentException; malformed namespaced IDs return null, malformed plain paths throw ResourceLocationException.
     * An original field name answers the block of its metadata 0, or null when 1.21.1 has none (an item field
     * has no block); it never throws. */
    public static ResourceLocation id(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("A block needs a name");
        if (ItemApi.isLegacyName(name)) return ItemApi.legacyBlockId(name, 0);
        return name.contains(":") ? ResourceLocation.tryParse(name) : ResourceLocation.fromNamespaceAndPath("thaumcraft", name);
    }

    /** Missing blocks return null; null/empty and malformed names follow {@link #id(String)} rather than a uniformly forgiving lookup. */
    public static Block get(String name) {
        ResourceLocation id = id(name);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return null;
        return BuiltInRegistries.BLOCK.get(id);
    }

    /**
     * The block of an original field name and metadata ({@code get("blockCosmeticSolid", 4)} is the thaumium
     * block), or null for an unknown metadata, a variant without a 1.21.1 block or an unregistered block. For
     * any other name the metadata is ignored: a native id names one block.
     */
    public static Block get(String name, int metadata) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("A block needs a name");
        if (!ItemApi.isLegacyName(name)) return get(name);
        ResourceLocation id = ItemApi.legacyBlockId(name, metadata);
        return id == null || !BuiltInRegistries.BLOCK.containsKey(id) ? null : BuiltInRegistries.BLOCK.get(id);
    }

    /**
     * The block state of an original field name and metadata ({@code state("blockCosmeticSolid", 1)} is the
     * obsidian tile, {@code cosmetic_solid_tough[variant=1]}), or for any other name the default state of the
     * named block; null when there is no such block.
     */
    public static BlockState state(String name, int metadata) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("A block needs a name");
        if (ItemApi.isLegacyName(name)) return ItemApi.legacyBlockState(name, metadata);
        Block block = get(name);
        return block == null ? null : block.defaultBlockState();
    }

    /** True when the name is an original TC4 {@code ConfigItems} or {@code ConfigBlocks} field. */
    public static boolean isLegacyName(String name) { return ItemApi.isLegacyName(name); }

    /** Null/empty or missing names throw IllegalArgumentException; malformed plain paths throw ResourceLocationException. */
    public static Block require(String name) {
        Block block = get(name);
        if (block == null) throw new IllegalArgumentException("Unknown block: " + name);
        return block;
    }

    /** A stack of the named block. */
    public static ItemStack stack(String name) {
        Block block = require(name);
        return new ItemStack(block);
    }

    /** True when the name is a registered block. */
    public static boolean isRegistered(String name) { return get(name) != null; }

    /** Every registered block of this mod. */
    public static Set<String> names() {
        Set<String> result = new LinkedHashSet<>();
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            if ("thaumcraft".equals(id.getNamespace())) result.add(id.getPath());
        }
        return result;
    }

    /** One of the native primal crystal blocks. */
    public static Block crystal(String aspectTag) {
        var supplier = ThaumcraftContent.CRYSTALS.get(aspectTag);
        if (supplier == null) throw new IllegalArgumentException("Unknown primal crystal: " + aspectTag);
        return supplier.get();
    }

    /** The native jar. */
    public static Block jar() { return ThaumcraftContent.JAR.get(); }

    /** The native void jar. */
    public static Block voidJar() { return ThaumcraftContent.VOID_JAR.get(); }

    /** The native aura node. */
    public static Block auraNode() { return ThaumcraftContent.AURA_NODE.get(); }

    /** The native silverwood knot. */
    public static Block silverwoodKnot() { return ThaumcraftContent.SILVERWOOD_KNOT.get(); }

    /** The native table. */
    public static Block table() { return ThaumcraftContent.TABLE.get(); }
}
