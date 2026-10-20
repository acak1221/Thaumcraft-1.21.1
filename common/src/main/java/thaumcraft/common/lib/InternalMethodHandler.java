package thaumcraft.common.lib;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandManager;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.WarpMessageNetworking;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.utils.Utils;

/**
 * Original TC4 {@code thaumcraft.common.lib.InternalMethodHandler}: the implementation of the internal services.
 *
 * <p>The original delegated every call to the original's own managers. The native delegates are the research
 * knowledge of the player, the object aspect resolver, the wand manager and the warp messages.
 */
public final class InternalMethodHandler implements IInternalMethodHandler {
    /** The shared handler of the mod. */
    public static final IInternalMethodHandler INSTANCE = new InternalMethodHandler();

    private InternalMethodHandler() {}

    @Override public void generateVisEffect(ServerLevel level, int x, int y, int z, int x2, int y2, int z2, int color) {
        Utils.generateVisEffect(level, x, y, z, x2, y2, z2, color);
    }

    @Override public boolean isResearchComplete(Player player, String researchKey) {
        if (player == null || researchKey == null) return false;
        return KnowledgeAccess.get(player).isResearchComplete(researchKey);
    }

    @Override public boolean hasDiscoveredAspect(Player player, Aspect aspect) {
        if (player == null || aspect == null) return false;
        return KnowledgeAccess.get(player).hasDiscoveredAspect(aspect.getTag());
    }

    @Override public AspectList getDiscoveredAspects(Player player) {
        AspectList list = new AspectList();
        if (player == null) return list;
        for (Map.Entry<String, Integer> entry : KnowledgeAccess.get(player).getAspectsDiscovered().entrySet()) {
            Aspect aspect = Aspect.getAspect(entry.getKey());
            if (aspect == null || entry.getValue() == null || entry.getValue() <= 0) continue;
            list.add(aspect, entry.getValue());
        }
        return list;
    }

    /**
     * Original {@code getStackInRowAndColumn(Object, int, int)}.
     *
     * <p>The original read the three by three grid of a magic workbench. The native grid is the matrix of the
     * arcane workbench, whose slots 0..8 are the same grid, and the original's order was {@code row + column * 3}.
     */
    @Override public ItemStack getStackInRowAndColumn(Object instance, int row, int column) {
        if (!(instance instanceof Container container)) return ItemStack.EMPTY;
        if (row < 0 || row >= 3 || column < 0 || column >= 3) return ItemStack.EMPTY;
        int slot = row + column * 3;
        if (slot >= container.getContainerSize()) return ItemStack.EMPTY;
        ItemStack stack = container.getItem(slot);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    @Override public AspectList getObjectAspects(Level level, ItemStack stack) {
        return ObjectAspectResolver.baseTags(stack, level);
    }

    @Override public AspectList getBonusObjectTags(Level level, ItemStack stack, AspectList bonus) {
        AspectList tags = ObjectAspectResolver.get(stack, level);
        if (bonus != null) tags.add(bonus);
        return tags;
    }

    @Override public AspectList generateTags(Level level, Item item, int metadata) {
        return ObjectAspectResolver.generatedTags(item, level);
    }

    /**
     * Original {@code consumeVisFromWand(ItemStack, EntityPlayer, AspectList, boolean, boolean)}.
     *
     * <p>The original paid the cost from the wand's own vis. The native wand keeps its vis in its own component,
     * and the cost never splits across wands.
     */
    @Override public boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting) {
        if (wand == null || wand.isEmpty() || !(wand.getItem() instanceof ItemWandCasting)) return false;
        if (cost == null || cost.size() == 0) return true;
        WandData data = ItemWandCasting.data(wand);
        Map<String, Integer> prices = new LinkedHashMap<>();
        for (Aspect aspect : cost.getAspects()) {
            int amount = cost.getAmount(aspect);
            if (data.amount(aspect.getTag()) < amount) return false;
            prices.put(aspect.getTag(), amount);
        }
        if (!doit) return true;
        for (Map.Entry<String, Integer> entry : prices.entrySet()) {
            data = data.withAmount(entry.getKey(), data.amount(entry.getKey()) - entry.getValue());
        }
        wand.set(ThaumcraftContent.WAND_DATA.get(), data);
        return true;
    }

    @Override public boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit) {
        return consumeVisFromWand(wand, player, cost, doit, true);
    }

    @Override public boolean consumeVisFromInventory(Player player, AspectList cost) {
        return WandManager.consumeVisFromInventory(player, cost);
    }

    /**
     * Original {@code addWarpToPlayer(EntityPlayer, int, boolean)}.
     *
     * <p>The original refused a negative permanent warp, refused a temporary warp that would go below zero and
     * always set the warp counter to the new total.
     */
    @Override public void addWarpToPlayer(Player player, int amount, boolean temporary) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) return;
        if (server.level().isClientSide) return;
        var knowledge = KnowledgeAccess.get(server);
        if (!temporary && amount < 0) return;
        if (amount == 0) return;
        if (temporary) {
            if (amount < 0 && knowledge.getWarpTemp() <= 0) return;
            knowledge.addWarpTemp(amount);
        } else {
            knowledge.addWarpPerm(amount);
        }
        knowledge.setWarpCounter(knowledge.getTotalWarp());
        KnowledgeEvents.sync(server);
        WarpMessageNetworking.send(server, (byte) (temporary ? 2 : 0), amount);
    }

    /** Original {@code addStickyWarpToPlayer(EntityPlayer, int)}. */
    @Override public void addStickyWarpToPlayer(Player player, int amount) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) return;
        if (server.level().isClientSide) return;
        var knowledge = KnowledgeAccess.get(server);
        if (amount == 0) return;
        if (amount < 0 && knowledge.getWarpSticky() <= 0) return;
        knowledge.addWarpSticky(amount);
        knowledge.setWarpCounter(knowledge.getTotalWarp());
        KnowledgeEvents.sync(server);
        WarpMessageNetworking.send(server, (byte) 1, amount);
    }
}
