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
 * resolve a player name is weak and is captured from real player traffic only.</p>
 */
public final class ThaumcraftInternalMethods implements IInternalMethodHandler {
    private static final AtomicReference<WeakReference<MinecraftServer>> SERVER = new AtomicReference<>(new WeakReference<>(null));
    public ThaumcraftInternalMethods() {}

    @Override public void generateVisEffect(int dim, int x, int y, int z, int x2, int y2, int z2, int color) {
        // The original integer dimension has no 1.21.1 equivalent; the native vis-net owns the effect itself.
    }

    @Override public boolean isResearchComplete(String username, String researchkey) {
        PlayerKnowledge knowledge = knowledge(username);
        return knowledge != null && knowledge.isResearchComplete(researchkey);
    }

    @Override public boolean hasDiscoveredAspect(String username, Aspect aspect) {
        PlayerKnowledge knowledge = knowledge(username);
        return knowledge != null && aspect != null && knowledge.hasDiscoveredAspect(aspect.getTag());
    }

    @Override public AspectList getDiscoveredAspects(String username) {
        PlayerKnowledge knowledge = knowledge(username);
        if (knowledge == null) return null;
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
        PlayerKnowledge knowledge = knowledge(player);
        if (knowledge == null) return;
        if (temporary) knowledge.addWarpTemp(amount); else knowledge.addWarpPerm(amount);
        knowledge.setWarpCounter(knowledge.getTotalWarp());
    }

    @Override public void addStickyWarpToPlayer(Player player, int amount) {
        PlayerKnowledge knowledge = knowledge(player);
        if (knowledge == null) return;
        knowledge.addWarpSticky(amount);
        knowledge.setWarpCounter(knowledge.getTotalWarp());
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
        Level level = player.level();
        if (level != null) {
            MinecraftServer server = level.getServer();
            if (server != null) SERVER.set(new WeakReference<>(server));
        }
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
