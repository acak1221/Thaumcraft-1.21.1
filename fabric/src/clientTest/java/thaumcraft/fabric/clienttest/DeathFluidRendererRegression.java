package thaumcraft.fabric.clienttest;

import java.util.Map;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import thaumcraft.client.renderers.fluid.DeathFluidAppearance;
import thaumcraft.fabric.client.FabricFluidDeathRenderHandler;

/**
 * Standalone native-handler regression. It uses declared atlas/sprite handles without creating a GL
 * context: the real TextureAtlas lookup and Fabric handler reload are exercised, not a rendered frame.
 * Run with the named Minecraft/common/Fabric classpath after compiling the production handler.
 */
public final class DeathFluidRendererRegression {
    public static void main(String[] args) {
        try {
            var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            var allocator = (sun.misc.Unsafe) field.get(null);
            // Only getSprite is called. The two distinct handles model atlas replacements after F3+T.
            var atlas = (TextureAtlas) allocator.allocateInstance(TextureAtlas.class);
            var original = (TextureAtlasSprite) allocator.allocateInstance(TextureAtlasSprite.class);
            var reloaded = (TextureAtlasSprite) allocator.allocateInstance(TextureAtlasSprite.class);
            var textures = TextureAtlas.class.getDeclaredField("texturesByName");
            textures.setAccessible(true);
            textures.set(atlas, Map.of(DeathFluidAppearance.TEXTURE, original));

            var handler = new FabricFluidDeathRenderHandler();
            handler.reloadTextures(atlas);
            var sprites = handler.getFluidSprites(null, null, null);
            check(sprites.length == 2 && sprites[0] == original && sprites[1] == original,
                    "still and flowing death fluid use the original fluiddeath sprite");
            check(handler.getFluidColor(null, null, null) == 0xFFFFFFFF,
                    "death fluid has no vanilla biome-water tint");
            textures.set(atlas, Map.of(DeathFluidAppearance.TEXTURE, reloaded));
            handler.reloadTextures(atlas);
            sprites = handler.getFluidSprites(null, null, null);
            check(sprites[0] == reloaded && sprites[1] == reloaded,
                    "resource reload replaces both sprite handles");
            try (var output = new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out))) {
                output.println("Fabric death-fluid handler regression PASS: still/flowing sprite, white tint and atlas reload");
            }
        } catch (Throwable failure) {
            try (var output = new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.err))) {
                failure.printStackTrace(output);
            }
            System.exit(1);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
