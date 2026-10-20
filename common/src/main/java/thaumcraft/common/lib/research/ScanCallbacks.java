package thaumcraft.common.lib.research;

import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.tiles.TileNode;

/** Original callback registration order and first nonnull result, resolved on the server rather than trusting an arbitrary C2S DTO. */
public final class ScanCallbacks {
    public record Target(String signature, String sessionKey, AspectList aspects, ScanManager.ScanKind kind, ResearchClues.Context clue) {}
    private ScanCallbacks() {}

    /** Invoke ONLY after the physical entity/block ray has no target, never after a physical target failed its aspect preflight. */
    public static Target first(ItemStack instrument, Level world, Player player) {
        if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer server) || server.level() != level
                || !server.server.isSameThread()) return null;
        return resolve(level, dispatch(instrument, world, player));
    }

    static ScanResult dispatch(ItemStack instrument, Level world, Player player) {
        // The public list remains mutable. A registration during one callback belongs to the next dispatch,
        // rather than reproducing ArrayList's original ConcurrentModificationException mid-scan.
        for (var handler : new ArrayList<>(ThaumcraftApi.scanEventhandlers)) {
            ScanResult result = handler.scanPhenomena(instrument, world, player);
            if (result != null) return result; // Invalid first results also occlude later callbacks, as in the original.
        }
        return null;
    }

    public static Target resolve(ServerLevel level, ScanResult result) {
        if (result == null || !level.getServer().isSameThread()) return null;
        if (result.type == 1) return item(level, itemWitness(result));
        if (result.type == 2) {
            var entity = result.entity;
            if (entity == null || entity.isRemoved() || entity.level() != level || level.getEntity(entity.getId()) != entity) return null;
            if (entity instanceof ItemEntity drop) {
                var target = item(level, drop.getItem());
                return target == null ? null : new Target(target.signature(), "entity:" + entity.getId(), target.aspects(), target.kind(), target.clue());
            }
            var target = EntityAspectBindings.describe(entity);
            return target == null ? null : new Target(target.signature(), "entity:" + entity.getId(), target.aspects(), ScanManager.ScanKind.ENTITY, target.clue());
        }
        if (result.type == 3 && result.phenomena != null && result.phenomena.startsWith("NODE") && result.nodePosition != null
                && level.dimension().equals(result.nodeDimension) && level.hasChunkAt(result.nodePosition)
                && level.getBlockEntity(result.nodePosition) instanceof TileNode node
                && result.phenomena.equals("NODE" + node.getScanId())) {
            AspectList aspects = NodeScanAspects.get(node.getAspects(), node.getNodeType());
            return new Target("@" + result.phenomena, "node:" + result.phenomena, aspects, ScanManager.ScanKind.PHENOMENA, null);
        }
        // Original completeScan implements NODE phenomena only; a new arbitrary string never becomes free research points.
        return null;
    }

    private static ItemStack itemWitness(ScanResult result) {
        if (result.stack != null) return result.stack.copyWithCount(1);
        if (result.id < 0) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.byId(result.id);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        var stack = new ItemStack(item);
        // Old subtype metadata became distinct registry IDs. Numeric meta is still native durability on damageable items.
        if (result.meta != 0) {
            if (!stack.isDamageableItem()) return ItemStack.EMPTY;
            stack.setDamageValue(Math.max(0, result.meta));
        }
        return stack;
    }
    private static Target item(ServerLevel level, ItemStack witness) {
        if (witness == null || witness.isEmpty()) return null;
        ItemStack item = witness.copyWithCount(1);
        var id = BuiltInRegistries.ITEM.getKey(item.getItem());
        return new Target("@" + id, "item:" + id + ":" + item.getDamageValue(), ObjectAspectResolver.get(item, level),
                ScanManager.ScanKind.ITEM, ResearchClues.Context.item(item));
    }
}
