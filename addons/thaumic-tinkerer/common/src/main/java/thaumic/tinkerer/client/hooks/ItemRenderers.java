/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.client.hooks;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The registry of built-in item renderers: an item whose model is {@code minecraft:builtin/entity} is drawn by code, not by
 * a model file (1.7.10: {@code MinecraftForgeClient.registerItemRenderer} with an {@code IItemRenderer}). Architectury has no
 * registry for it, so the loader module provides the backend and the groups call {@link #register}; client only.
 *
 * <p>The only user today is the Mob Display of the Machines group: {@code ItemRenderers.register(TTRegistry.item("mob_display"),
 * RenderMobDisplay::render)} from {@code MachinesClient.register()}.</p>
 *
 * <h2>Backends</h2>
 * <ul>
 *   <li><b>Fabric</b> (done): {@code thaumic.tinkerer.fabric.client.FabricItemRenderers.install()}, called by the
 *       Fabric client entrypoint before the groups register their client side, registers each renderer with Fabric API's
 *       {@code BuiltinItemRendererRegistry} ({@code fabric-rendering-v1}, a {@code DynamicItemRenderer}, which carries the
 *       display context).</li>
 *   <li><b>NeoForge</b> (not written): the backend only collects the items and their renderers, because the registration
 *       point comes later than the mod's client set-up: a listener of {@code RegisterClientExtensionsEvent} on the mod event
 *       bus then calls {@code event.registerItem(IClientItemExtensions, item)} for every collected item, where the
 *       extension's {@code getCustomRenderer()} returns a {@code BlockEntityWithoutLevelRenderer} whose
 *       {@code renderByItem(stack, context, pose, buffers, light, overlay)} calls the {@link Renderer}.</li>
 * </ul>
 *
 * <p>An item has at most one renderer; a second registration for the same item is an error (Fabric API rejects it too).
 * Registrations made before the backend exists wait for it.</p>
 */
public final class ItemRenderers {
    /** Draws the item (the arguments of Fabric's {@code DynamicItemRenderer} and of NeoForge's {@code renderByItem}). */
    @FunctionalInterface
    public interface Renderer {
        void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay);
    }

    /** What a loader module supplies: puts a renderer on the platform's registry. */
    @FunctionalInterface
    public interface Backend {
        void register(Item item, Renderer renderer);
    }

    private static final Map<Item, Renderer> REGISTERED = new IdentityHashMap<>();
    private static final List<Map.Entry<Item, Renderer>> WAITING = new ArrayList<>();
    @Nullable
    private static Backend backend;

    private ItemRenderers() {}

    /**
     * Registers the built-in renderer of an item (client only, from {@code registerClient()}).
     *
     * @throws IllegalStateException if the item has a renderer already
     */
    public static synchronized void register(Item item, Renderer renderer) {
        if (item == null || renderer == null) throw new NullPointerException("item and renderer");
        if (REGISTERED.putIfAbsent(item, renderer) != null) {
            throw new IllegalStateException("the item already has a built-in renderer: " + item);
        }
        if (backend != null) backend.register(item, renderer);
        else WAITING.add(Map.entry(item, renderer));
    }

    /** Installed by the loader module's client entrypoint, before the groups register; hands over what is waiting. */
    public static synchronized void setBackend(Backend newBackend) {
        backend = newBackend;
        List<Map.Entry<Item, Renderer>> waiting = new ArrayList<>(WAITING);
        WAITING.clear();
        for (Map.Entry<Item, Renderer> entry : waiting) newBackend.register(entry.getKey(), entry.getValue());
    }

    /** The renderer registered for the item, or null; for tests and for backends that collect the items. */
    @Nullable
    public static synchronized Renderer get(Item item) {
        return REGISTERED.get(item);
    }

    /** True once a loader module installed its backend. */
    public static synchronized boolean hasBackend() {
        return backend != null;
    }
}
