package thaumcraft.common.items.relics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ResearchClues;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.lib.research.ScanCallbacks;
import thaumcraft.common.tiles.TileNode;

/** TC4's 25-tick use, completing after 20 stable ticks, on the server. */
public final class ItemThaumometer extends Item {
    public ItemThaumometer(Properties properties) { super(properties); }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 25; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Target target = findTarget(player, stack);
            if (target == null || target.aspects().size() == 0) {
                player.displayClientMessage(Component.translatable("tc.port.scan.unknown"), true);
                return InteractionResultHolder.fail(stack);
            }
            stack.set(ThaumcraftContent.SCAN_SESSION.get(), new ScanSession(target.sessionKey(), level.getGameTime()));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player) || level.isClientSide) return;
        ScanSession session = stack.get(ThaumcraftContent.SCAN_SESSION.get());
        Target target = findTarget(player, stack);
        if (session == null || target == null || !session.signature().equals(target.sessionKey())) {
            stack.remove(ThaumcraftContent.SCAN_SESSION.get());
            player.stopUsingItem();
            return;
        }
        if (level.getGameTime() - session.startTick() >= 20) {
            ScanManager.completeScan(player, target.signature(), target.aspects(), target.kind(), target.clue());
            stack.remove(ThaumcraftContent.SCAN_SESSION.get());
            player.stopUsingItem();
        }
    }

    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!level.isClientSide) stack.remove(ThaumcraftContent.SCAN_SESSION.get());
    }

    private static Target findTarget(Player player, ItemStack instrument) {
        Level level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(10.0D));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        Entity entity = findEntity(player, start, end);
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack item = itemEntity.getItem();
            if (item.isEmpty()) return null;
            var id = BuiltInRegistries.ITEM.getKey(item.getItem());
            return new Target("@" + id, "entity:" + entity.getId(), ObjectAspectResolver.get(item, level), ScanManager.ScanKind.ITEM, ResearchClues.Context.item(item));
        }
        if (entity != null) {
            var scanned = EntityAspectBindings.describe(entity);
            // An unsupported entity occludes the block behind it; its aspects never come from a different target.
            return scanned == null ? null : new Target(scanned.signature(), "entity:" + entity.getId(), scanned.aspects(), ScanManager.ScanKind.ENTITY,
                    scanned.clue());
        }
        if (block.getType() != HitResult.Type.BLOCK) {
            var callback = ScanCallbacks.first(instrument, level, player);
            return callback == null ? null : new Target(callback.signature(), callback.sessionKey(), callback.aspects(), callback.kind(), callback.clue());
        }
        if (!level.hasChunkAt(block.getBlockPos())) return null;
        if (level.getBlockEntity(block.getBlockPos()) instanceof TileNode node) {
            return new Target("@NODE" + node.getScanId(), "node:NODE" + node.getScanId(), node.getScanAspects(),
                    ScanManager.ScanKind.PHENOMENA, null);
        }
        var state = level.getBlockState(block.getBlockPos());
        // TC4 scans the stack returned by getPickBlock, not the block registry
        // name. Native clone picking preserves redstone wire/wall/slab item
        // identity even when its block identifier differs from the item id.
        ItemStack picked = state.getBlock().getCloneItemStack(level, block.getBlockPos(), state);
        if (picked.isEmpty()) return null;
        var id = BuiltInRegistries.ITEM.getKey(picked.getItem());
        return new Target("@" + id, "item:" + id + ":" + picked.getDamageValue(), ObjectAspectResolver.get(picked, level), ScanManager.ScanKind.ITEM, ResearchClues.Context.item(picked));
    }

    /** Original EntityUtils.getPointedEntity(.5,10,0,true): all entities, minimum distance, eye LOS and at least .8 pick padding. */
    private static Entity findEntity(Player player, Vec3 start, Vec3 end) {
        var level = player.level();
        Entity closest = null; double distance = Double.POSITIVE_INFINITY;
        var area = player.getBoundingBox().expandTowards(end.subtract(start));
        for (Entity candidate : level.getEntities(player, area, entity -> !entity.isRemoved() && !entity.isSpectator())) {
            if (candidate.distanceTo(player) < .5F) continue;
            var sight = level.clip(new ClipContext(start, candidate.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (sight.getType() != HitResult.Type.MISS) continue;
            var bounds = candidate.getBoundingBox().inflate(Math.max(.8F, candidate.getPickRadius()));
            var hit = bounds.clip(start, end);
            if (bounds.contains(start)) {
                if (distance >= 0) { closest = candidate; distance = 0; }
            } else if (hit.isPresent()) {
                double selected = start.distanceToSqr(hit.get());
                if (selected < distance || distance == 0) { closest = candidate; distance = selected; }
            }
        }
        return closest;
    }

    private record Target(String signature, String sessionKey, AspectList aspects, ScanManager.ScanKind kind, ResearchClues.Context clue) {}
}
