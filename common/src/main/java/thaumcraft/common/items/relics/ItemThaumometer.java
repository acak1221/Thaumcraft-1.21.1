package thaumcraft.common.items.relics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.tiles.TileNode;

/** TC4's 25-tick use, completing after 20 stable ticks, on the server. */
public final class ItemThaumometer extends Item {
    public ItemThaumometer(Properties properties) { super(properties); }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 25; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Target target = findTarget(player);
            if (target == null || target.aspects().size() == 0) {
                player.displayClientMessage(Component.translatable("tc.port.scan.unknown"), true);
                return InteractionResultHolder.fail(stack);
            }
            stack.set(ThaumcraftContent.SCAN_SESSION.get(), new ScanSession(target.signature(), level.getGameTime()));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player) || level.isClientSide) return;
        ScanSession session = stack.get(ThaumcraftContent.SCAN_SESSION.get());
        Target target = findTarget(player);
        if (session == null || target == null || !session.signature().equals(target.signature())) {
            stack.remove(ThaumcraftContent.SCAN_SESSION.get());
            player.stopUsingItem();
            return;
        }
        if (level.getGameTime() - session.startTick() >= 20) {
            ScanManager.completeScan(player, target.signature(), target.aspects(), target.kind());
            stack.remove(ThaumcraftContent.SCAN_SESSION.get());
            player.stopUsingItem();
        }
    }

    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!level.isClientSide) stack.remove(ThaumcraftContent.SCAN_SESSION.get());
    }

    private static Target findTarget(Player player) {
        Level level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(10.0D));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        double distance = block.getType() == HitResult.Type.MISS ? 100.0D : start.distanceToSqr(block.getLocation());
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(0.5D),
                candidate -> candidate instanceof ItemEntity && !candidate.isRemoved(), distance);
        if (entity != null && entity.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack item = itemEntity.getItem();
            var id = BuiltInRegistries.ITEM.getKey(item.getItem());
            return new Target("@" + id, ObjectAspectResolver.get(item, level), ScanManager.ScanKind.ITEM);
        }
        if (block.getType() != HitResult.Type.BLOCK || !level.hasChunkAt(block.getBlockPos())) return null;
        if (level.getBlockEntity(block.getBlockPos()) instanceof TileNode node) {
            return new Target("@NODE" + node.getScanId(), ScanManager.nodeScanAspects(node.getAspects()),
                    ScanManager.ScanKind.PHENOMENA);
        }
        var state = level.getBlockState(block.getBlockPos());
        // TC4 scans the stack returned by getPickBlock, not the block registry
        // name. Native clone picking preserves redstone wire/wall/slab item
        // identity even when its block identifier differs from the item id.
        ItemStack picked = state.getBlock().getCloneItemStack(level, block.getBlockPos(), state);
        if (picked.isEmpty()) return null;
        var id = BuiltInRegistries.ITEM.getKey(picked.getItem());
        return new Target("@" + id, ObjectAspectResolver.get(picked, level), ScanManager.ScanKind.ITEM);
    }

    private record Target(String signature, AspectList aspects, ScanManager.ScanKind kind) {}
}
