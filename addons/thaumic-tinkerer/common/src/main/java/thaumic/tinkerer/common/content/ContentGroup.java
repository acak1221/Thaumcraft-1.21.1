/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.content;

/**
 * One feature group of the add-on. The loader entry calls the three phases of every group in the order of
 * {@link ContentGroups#ALL}:
 * <ol>
 *   <li>{@link #register()} during mod initialisation, on both sides: add blocks, items, block entities, menus,
 *       entities, effects, sounds and network payloads to the {@code TTRegistry} deferred registers;</li>
 *   <li>{@link #ready()} when Thaumcraft is ready (after the generic recipe and research registration of every
 *       {@code ITTinkererRegisterable}), on both sides: wand parts, object tags, extra research, warp, event
 *       handlers that need Thaumcraft;</li>
 *   <li>{@link #registerClient()} from the client entrypoint only: screens, renderers, colours, item properties,
 *       key mappings. Client classes are referenced only from here and from classes of {@code thaumic.tinkerer.client}.</li>
 * </ol>
 */
public interface ContentGroup {
    default void register() {}

    default void ready() {}

    default void registerClient() {}
}
