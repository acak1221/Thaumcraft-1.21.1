package thaumcraft.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The point where an add-on does its Thaumcraft registrations.
 *
 * <p>A 1.7.10 add-on declared {@code dependencies = "after:Thaumcraft"} and registered its aspects, object tags,
 * recipes, research and wand parts in {@code init} or {@code postInit}, when every item of every mod and every
 * Thaumcraft registry existed. The 1.21.1 loaders give no such order: on Fabric the {@code main} entrypoints of mods
 * run in an unspecified order, so an add-on that looks up a Thaumcraft item from its own initializer can run before
 * Thaumcraft registered it. Thaumcraft calls {@link #onThaumcraftReady()} once, when it is safe:</p>
 * <ul>
 *   <li>after every mod registered its items, blocks and entities, and after Thaumcraft registered its content,
 *       aspects, research, wand parts and recipes;</li>
 *   <li>on the main thread, before any world loads, on both physical sides (client and dedicated server), like the
 *       1.7.10 init phases. Register on both sides: a client and its server must know the same research keys.</li>
 * </ul>
 *
 * <p>Registering the hook:</p>
 * <ul>
 *   <li><b>Fabric:</b> list the class under the {@code "thaumcraft"} entrypoint of {@code fabric.mod.json}:
 *       {@code "entrypoints": {"thaumcraft": ["com.example.MyAddon"]}}.</li>
 *   <li><b>NeoForge:</b> annotate the class with {@link Addon}; it needs a public constructor without arguments.
 *       The hook runs from {@code FMLCommonSetupEvent}'s main-thread work queue.</li>
 *   <li><b>Either loader:</b> call {@link ThaumcraftApi#whenReady(Runnable)} from your own initializer.</li>
 * </ul>
 *
 * <p>The order among add-ons is not specified. Items and blocks themselves are registered in your own initializer
 * or registry events as usual; this hook is for the Thaumcraft registrations that refer to them.</p>
 */
@FunctionalInterface
public interface IThaumcraftAddon {
    /** Registers the add-on's Thaumcraft content. Called exactly once per game instance. */
    void onThaumcraftReady();

    /** Marks an {@link IThaumcraftAddon} for discovery on NeoForge. Fabric uses the {@code "thaumcraft"} entrypoint instead. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Addon {}
}
