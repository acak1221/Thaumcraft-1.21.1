package thaumcraft.common.items.wands.foci;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.IArchitect;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.WandArchitect;
import thaumcraft.common.lib.world.TradeSwaps;

/** Original Trade: sneak to remember a block; right click spreads, left click swaps only the targeted block. */
public final class ItemFocusTrade extends ItemFocusBasic implements IArchitect {
    public ItemFocusTrade(Properties properties) { super(properties); }
    @Override public int getFocusColor(ItemStack focus) { return 8747923; }
    @Override public String getSortingHelper(ItemStack focus) { return "BT" + super.getSortingHelper(focus); }
    @Override public AspectList getVisCost(ItemStack focus) {
        AspectList cost = new AspectList().add(Aspect.ENTROPY, 5).add(Aspect.EARTH, 5).add(Aspect.ORDER, 5);
        if (FocusUpgradeData.level(focus, FocusUpgradeType.silktouch) > 0) {
            for (Aspect primal : new Aspect[]{Aspect.AIR, Aspect.FIRE, Aspect.EARTH, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY}) cost.add(primal, 1);
        }
        return cost;
    }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2, 4 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.enlarge};
            case 3 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.enlarge, FocusUpgradeType.treasure, FocusUpgradeType.architect};
            case 5 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.enlarge, FocusUpgradeType.silktouch};
            default -> new FocusUpgradeType[0];
        };
    }
    @Override public int getMaxAreaSize(ItemStack focus) { return 3 + FocusUpgradeData.level(focus, FocusUpgradeType.enlarge) * 2; }
    @Override public int getAxisModes() { return 2; }

    /**
     * Original {@code ItemFocusTrade:229-297} - the blocks the trade focus would touch.
     *
     * <p>The original walked the connected cells of the area, skipped the face of the anchor and
     * its opposite and accepted only a cell of the same block and metadata as the anchor that is
     * exposed, that is not air, that is not softer than zero and that the player may use.</p>
     */
    @Override public java.util.ArrayList<thaumcraft.api.BlockCoordinates> getArchitectBlocks(ItemStack wand, Level world, int x, int y, int z, int side, Player player) {
        java.util.ArrayList<thaumcraft.api.BlockCoordinates> out = new java.util.ArrayList<>();
        if (world == null || player == null) return out;
        net.minecraft.core.Direction face = WandArchitect.face(side);
        if (face == null) return out;
        net.minecraft.core.BlockPos anchor = new net.minecraft.core.BlockPos(x, y, z);
        if (!world.hasChunkAt(anchor)) return out;
        var source = world.getBlockState(anchor);
        var area = WandArchitect.enabled(wand) ? WandArchitect.area(wand, FocusData.get(wand)) : new WandArchitect.Area(0, 0, 0, 0);
        for (var cell : WandArchitect.planarBlocks(anchor, face, area, candidate ->
                world.getBlockState(candidate) == source && TradeSwaps.exposed(world, candidate)
                        && !world.isEmptyBlock(candidate) && world.getBlockState(candidate).getDestroySpeed(world, candidate) >= 0.0F
                        && world.mayInteract(player, candidate))) {
            out.add(new thaumcraft.api.BlockCoordinates(cell.getX(), cell.getY(), cell.getZ()));
        }
        return out;
    }

    /** Original {@code ItemFocusTrade:301-319} - the axes the client draws. */
    @Override public boolean showAxis(ItemStack wand, Level world, Player player, int side, IArchitect.EnumAxis axis) {
        var area = WandArchitect.enabled(wand) ? WandArchitect.area(wand, FocusData.get(wand)) : new WandArchitect.Area(0, 0, 0, 0);
        return WandArchitect.showAxis(area, side, axis);
    }

    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer actor) || level.isClientSide || !TradeSwaps.actor(actor, wand)) return wand;
        HitResult ray = player.pick(player.blockInteractionRange(), 1, false);
        if (!(ray instanceof BlockHitResult hit) || ray.getType() != HitResult.Type.BLOCK) return wand;
        if (player.isShiftKeyDown()) TradeSwaps.remember(actor, wand, hit.getBlockPos());
        else if (WandArchitect.enabled(wand)) {
            var source = level.getBlockState(hit.getBlockPos());
            for (var pos : WandArchitect.planarBlocks(hit.getBlockPos(), hit.getDirection(), WandArchitect.area(wand, FocusData.get(wand)),
                    cell -> TradeSwaps.available(actor, cell) && level.getBlockState(cell) == source && TradeSwaps.exposed(level, cell))) {
                TradeSwaps.enqueue(actor, wand, pos, 0);
            }
        } else TradeSwaps.enqueue(actor, wand, hit.getBlockPos(), 3 + FocusUpgradeData.level(FocusData.get(wand), FocusUpgradeType.enlarge));
        player.swing(hand);
        return wand;
    }
    public static boolean leftClick(ServerPlayer player, ItemStack wand, net.minecraft.core.BlockPos pos) {
        // Original ItemFocusTrade:168-181 - the swing of the wand swaps the targeted block only.
        if (!TradeSwaps.actor(player, wand)) return false;
        HitResult ray = player.pick(player.blockInteractionRange(), 1, false);
        return ray instanceof BlockHitResult hit && ray.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)
                && TradeSwaps.enqueue(player, wand, pos, 0);
    }
}
