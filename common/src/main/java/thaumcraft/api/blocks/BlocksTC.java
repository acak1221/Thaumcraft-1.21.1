package thaumcraft.api.blocks;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.config.ThaumcraftContent;

/**
 * The public block registry of the native port.
 *
 * <p>Forced 1.21.1 change: the original TC4 kept its blocks in {@code ConfigBlocks} and the original add-on
 * surface kept them in {@code BlocksTC}; 1.21.1 registers every block in the block registry, so this class
 * resolves the native registry instead of holding a second copy of it. A plain name means
 * {@code thaumcraft:<name>}.</p>
 */
public final class BlocksTC {
    private BlocksTC() {}

    /** Null/empty names throw IllegalArgumentException; malformed namespaced IDs return null, malformed plain paths throw ResourceLocationException. */
    public static ResourceLocation id(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("A block needs a name");
        return name.contains(":") ? ResourceLocation.tryParse(name) : ResourceLocation.fromNamespaceAndPath("thaumcraft", name);
    }

    /** Missing blocks return null; null/empty and malformed names follow {@link #id(String)} rather than a uniformly forgiving lookup. */
    public static Block get(String name) {
        ResourceLocation id = id(name);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return null;
        return BuiltInRegistries.BLOCK.get(id);
    }

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
