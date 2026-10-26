package thaumcraft.api.internal;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.WandManager;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.research.ObjectAspectRegistry;
import thaumcraft.common.lib.research.PlayerKnowledge;

/**
 * The native handler bound to the 1.21.1 gameplay internals.
 *
 * <p>Everything the original {@code DummyInternalMethodHandler} refused to answer is answered here from the
 * live native state: the player's own knowledge, the native object-tag registry, the native wand payment and
 * the native warp bookkeeping. Nothing is cached across a player or world change; the server reference used to
 * resolve a player name is weak and follows the native server lifecycle.</p>
 */
public final class ThaumcraftInternalMethods implements IInternalMethodHandler {
    private static final AtomicReference<WeakReference<MinecraftServer>> SERVER = new AtomicReference<>(new WeakReference<>(null));
    public ThaumcraftInternalMethods() {}

    public static void serverStarted(MinecraftServer server) { SERVER.set(new WeakReference<>(server)); }
    public static void serverStopped(MinecraftServer server) {
        WeakReference<MinecraftServer> current = SERVER.get();
        if (current.get() == server) SERVER.compareAndSet(current, new WeakReference<>(null));
    }

    /**
     * @deprecated Inert: numeric Forge dimensions cannot select a native server level. Use
     * {@link thaumcraft.api.visnet.VisNetHandler#generateVisEffect(net.minecraft.server.level.ServerLevel,
     * net.minecraft.core.BlockPos, net.minecraft.core.BlockPos, Aspect)} instead.
     */
    @Deprecated
    @Override public void generateVisEffect(int dim, int x, int y, int z, int x2, int y2, int z2, int color) {
        // The original integer dimension has no 1.21.1 equivalent; the native vis-net owns the effect itself.
    }

    // ------------------------------------------------------------------ 1.21.1 forms of the same services
    // The native-looking overloads of the interface answer from the same native state as the internal handler
    // of the mod; an add-on never needs thaumcraft.common to reach them.

    @Override public void generateVisEffect(net.minecraft.server.level.ServerLevel level, int x, int y, int z, int x2, int y2, int z2, int color) {
        if (level != null) thaumcraft.common.lib.InternalMethodHandler.INSTANCE.generateVisEffect(level, x, y, z, x2, y2, z2, color);
    }

    @Override public void generateVisEffect(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos from,
                                            net.minecraft.core.BlockPos to, Aspect aspect) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && from != null && to != null && aspect != null)
            thaumcraft.api.visnet.VisNetHandler.generateVisEffect(server, from, to, aspect);
    }

    @Override public boolean hasDiscoveredAspect(Player player, Aspect aspect) {
        PlayerKnowledge knowledge = knowledge(player);
        return knowledge != null && aspect != null && knowledge.hasDiscoveredAspect(aspect.getTag());
    }

    @Override public AspectList getDiscoveredAspects(Player player) {
        PlayerKnowledge knowledge = knowledge(player);
        return knowledge == null ? null : discovered(knowledge);
    }

    @Override public AspectList getObjectAspects(net.minecraft.world.level.Level level, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new AspectList();
        return thaumcraft.common.lib.InternalMethodHandler.INSTANCE.getObjectAspects(level, stack);
    }

    @Override public AspectList getBonusObjectTags(net.minecraft.world.level.Level level, ItemStack stack, AspectList bonus) {
        if (stack == null || stack.isEmpty()) return bonus == null ? new AspectList() : bonus.copy();
        return thaumcraft.common.lib.InternalMethodHandler.INSTANCE.getBonusObjectTags(level, stack, bonus);
    }

    @Override public AspectList generateTags(net.minecraft.world.level.Level level, Item item, int metadata) {
        if (item == null) return new AspectList();
        return thaumcraft.common.lib.InternalMethodHandler.INSTANCE.generateTags(level, item, metadata);
    }

    @Override public boolean isResearchComplete(String username, String researchkey) {
        PlayerKnowledge knowledge = knowledge(username);
        return knowledge != null && knowledge.isResearchComplete(researchkey);
    }

    @Override public boolean isResearchComplete(Player player, String researchKey) {
        PlayerKnowledge knowledge = knowledge(player);
        return knowledge != null && knowledge.isResearchComplete(researchKey);
    }

    @Override public boolean hasDiscoveredAspect(String username, Aspect aspect) {
        PlayerKnowledge knowledge = knowledge(username);
        return knowledge != null && aspect != null && knowledge.hasDiscoveredAspect(aspect.getTag());
    }

    @Override public AspectList getDiscoveredAspects(String username) {
        PlayerKnowledge knowledge = knowledge(username);
        return knowledge == null ? null : discovered(knowledge);
    }

    private static AspectList discovered(PlayerKnowledge knowledge) {
        AspectList result = new AspectList();
        for (Map.Entry<String, Integer> entry : knowledge.getAspectsDiscovered().entrySet()) {
            Aspect aspect = Aspect.getAspect(entry.getKey());
            if (aspect != null) result.add(aspect, entry.getValue());
        }
        return result;
    }

    /**
     * The original three by three grid lookup.
     *
     * <p>Original {@code thaumcraft/common/lib/InternalMethodHandler.java} forwards to
     * {@code TileMagicWorkbench.getStackInRowAndColumn(row, column)}, which reads the slot
     * {@code row + column * 3} of the grid (oracle {@code TileMagicWorkbench.getStackInRowAndColumn}).
     * The original recipes call the helper with the ROW first, for example
     * {@code getStackInRowAndColumn(inv, 1, 0)} for the slot 1 of the sceptre grid
     * (oracle {@code thaumcraft/common/lib/crafting/ArcaneSceptreRecipe.java:42-46}).</p>
     */
    @Override public ItemStack getStackInRowAndColumn(Object instance, int row, int column) {
        if (instance == null || row < 0 || row > 2 || column < 0 || column > 2) return ItemStack.EMPTY;
        int index = row + column * 3;
        if (instance instanceof net.minecraft.world.Container container) {
            if (index >= container.getContainerSize()) return ItemStack.EMPTY;
            ItemStack stack = container.getItem(index);
            return stack == null ? ItemStack.EMPTY : stack;
        }
        if (instance instanceof net.minecraft.world.item.crafting.RecipeInput input) {
            ItemStack stack = input.getItem(index);
            return stack == null ? ItemStack.EMPTY : stack;
        }
        return ItemStack.EMPTY;
    }

    @Override public AspectList getObjectAspects(ItemStack is) {
        if (is == null || is.isEmpty()) return new AspectList();
        var match = ObjectAspectRegistry.lookup(thaumcraft.api.ThaumcraftApi.objectTags, is.getItem(), is.getDamageValue());
        return match == null ? new AspectList() : match.aspects().copy();
    }

    @Override public AspectList getBonusObjectTags(ItemStack is, AspectList ot) {
        // The original bonus pass (equipment, enchantments, container contents) needs a world for recipe-derived tags.
        // The level-free part is the registered base the caller already has.
        return ot == null ? new AspectList() : ot.copy();
    }

    @Override public AspectList generateTags(Item item, int meta) {
        if (item == null) return new AspectList();
        var direct = ObjectAspectCatalog.get(BuiltInRegistries.ITEM.getKey(item));
        var match = ObjectAspectRegistry.lookup(thaumcraft.api.ThaumcraftApi.objectTags, item, meta);
        if (match != null) direct.add(match.aspects());
        return direct;
    }

    @Override public boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting) {
        return consume(wand, cost, doit);
    }

    @Override public boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit) {
        return consume(wand, cost, doit);
    }

    @Override public boolean consumeVisFromInventory(Player player, AspectList cost) {
        return WandManager.consumeVisFromInventory(player, cost);
    }

    @Override public void addWarpToPlayer(Player player, int amount, boolean temporary) {
        // The public API must use the original authority/negative guards and
        // committed sync/message path, not mutate client or permanent warp directly.
        thaumcraft.common.lib.InternalMethodHandler.INSTANCE.addWarpToPlayer(player, amount, temporary);
    }

    @Override public void addStickyWarpToPlayer(Player player, int amount) {
        thaumcraft.common.lib.InternalMethodHandler.INSTANCE.addStickyWarpToPlayer(player, amount);
    }

    @Override public boolean completeResearch(Player player, String researchKey) {
        if (!(player instanceof ServerPlayer) || researchKey == null || researchKey.startsWith("@")) return false;
        return thaumcraft.common.lib.research.ResearchManager.INSTANCE.completeResearch(player, researchKey);
    }

    @Override public java.util.Set<String> getCompletedResearch(Player player) {
        PlayerKnowledge knowledge = knowledge(player);
        if (knowledge == null) return java.util.Set.of();
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        for (String key : knowledge.getResearchComplete()) if (!key.startsWith("@")) result.add(key);
        return result;
    }

    @Override public void addAspectPool(Player player, Aspect aspect, int amount) {
        if (!(player instanceof ServerPlayer) || aspect == null || amount == 0) return;
        short clamped = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, amount));
        thaumcraft.common.lib.research.ResearchManager.INSTANCE.completeAspect(player, aspect, clamped);
    }

    @Override public int getAspectPool(Player player, Aspect aspect) {
        PlayerKnowledge knowledge = knowledge(player);
        return knowledge == null || aspect == null ? 0 : knowledge.getAspectPoolFor(aspect.getTag());
    }

    @Override public int[] getWarp(Player player) {
        PlayerKnowledge knowledge = knowledge(player);
        return knowledge == null ? new int[3] : new int[] {knowledge.getWarpPerm(), knowledge.getWarpSticky(), knowledge.getWarpTemp()};
    }

    private static boolean consume(ItemStack wand, AspectList cost, boolean doit) {
        if (wand == null || wand.isEmpty() || cost == null || cost.size() == 0) return false;
        Map<String, Integer> prices = new LinkedHashMap<>();
        for (Aspect aspect : cost.getAspects()) {
            int amount = cost.getAmount(aspect);
            if (aspect == null || amount <= 0) continue;
            prices.put(aspect.getTag(), amount);
        }
        if (prices.isEmpty() || !ArcaneVis.canPay(wand, prices)) return false;
        if (!doit) return true;
        return ArcaneVis.debit(wand, prices);
    }

    private static PlayerKnowledge knowledge(Player player) {
        if (player == null) return null;
        try {
            return KnowledgeAccess.get(player);
        } catch (RuntimeException unavailable) {
            return null; // The knowledge mixin is not installed in this runtime.
        }
    }

    private static PlayerKnowledge knowledge(String username) {
        if (username == null || username.isEmpty()) return null;
        MinecraftServer server = SERVER.get().get();
        if (server == null) return null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(username)) return knowledge(player);
        }
        return null;
    }
}
