package thaumcraft.common.items.wands.foci;

import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.IArchitect;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.WandArchitect;
import thaumcraft.common.lib.world.WardingCasts;

/** TC4 ownership warding: an exact remembered block, quarter-vis earth/order and tenth-vis water; owner undo is free. */
public final class ItemFocusWarding extends ItemFocusBasic implements IArchitect {
    public ItemFocusWarding(Properties properties) { super(properties); }
    @Override public AspectList getVisCost(ItemStack focus) {
        return new AspectList().add(Aspect.EARTH, WardingRules.EARTH).add(Aspect.ORDER, WardingRules.ORDER).add(Aspect.WATER, WardingRules.WATER);
    }
    @Override public int getFocusColor(ItemStack focus) { return 16771535; }
    @Override public String getSortingHelper(ItemStack focus) { return "BWA" + super.getSortingHelper(focus); }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1 -> new FocusUpgradeType[]{FocusUpgradeType.frugal};
            case 2 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.architect};
            case 3, 4, 5 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.enlarge};
            default -> new FocusUpgradeType[0];
        };
    }
    @Override public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgradeType type, int rank) {
        return (type != FocusUpgradeType.enlarge || FocusUpgradeData.read(focus).level(FocusUpgradeType.architect) > 0)
                && super.canApplyUpgrade(focus, player, type, rank);
    }
    @Override public int getMaxAreaSize(ItemStack focus) { return 3 + FocusUpgradeData.level(focus, FocusUpgradeType.enlarge); }

    /**
     * Original {@code ItemFocusWarding:196-283} - the blocks the warding focus would touch.
     *
     * <p>The original walked the connected cells of the area in all six directions and accepted
     * only a cell that is not air: a warded tile of the owner when the anchor is a warded tile,
     * a solid block without a tile otherwise.</p>
     */
    @Override public java.util.ArrayList<thaumcraft.api.BlockCoordinates> getArchitectBlocks(ItemStack wand, Level world, int x, int y, int z, int side, Player player) {
        java.util.ArrayList<thaumcraft.api.BlockCoordinates> out = new java.util.ArrayList<>();
        if (world == null || player == null) return out;
        net.minecraft.core.Direction face = WandArchitect.face(side);
        if (face == null) return out;
        net.minecraft.core.BlockPos anchor = new net.minecraft.core.BlockPos(x, y, z);
        if (!world.hasChunkAt(anchor)) return out;
        boolean tiles = world.getBlockEntity(anchor) instanceof thaumcraft.common.tiles.TileWarded;
        var area = WandArchitect.enabled(wand) ? WandArchitect.area(wand, FocusData.get(wand)) : new WandArchitect.Area(0, 0, 0, 0);
        for (var cell : WandArchitect.connected(anchor, face, area, false, candidate -> {
            if (world.isEmptyBlock(candidate)) return false;
            var tile = world.getBlockEntity(candidate);
            if (tiles) return tile instanceof thaumcraft.common.tiles.TileWarded warded && warded.ownedBy(player.getUUID());
            var state = world.getBlockState(candidate);
            return tile == null && !state.isAir() && !state.hasBlockEntity() && !state.canBeReplaced()
                    && state.getFluidState().isEmpty() && state.isSolidRender(world, candidate) && state.isCollisionShapeFullBlock(world, candidate);        })) {
            out.add(new thaumcraft.api.BlockCoordinates(cell.getX(), cell.getY(), cell.getZ()));
        }
        return out;
    }

    /** Original {@code ItemFocusWarding:288-309} - the axes the client draws. */
    @Override public boolean showAxis(ItemStack wand, Level world, Player player, int side, IArchitect.EnumAxis axis) {
        var area = WandArchitect.enabled(wand) ? WandArchitect.area(wand, FocusData.get(wand)) : new WandArchitect.Area(0, 0, 0, 0);
        return WandArchitect.showAxisWarding(area, side, axis);
    }

    public static Map<String, Integer> activationCost(ItemStack wand, Player actor) {
        return FocusVis.price(new AspectList().add(Aspect.EARTH, WardingRules.EARTH).add(Aspect.ORDER, WardingRules.ORDER)
                .add(Aspect.WATER, WardingRules.WATER), wand, actor);
    }
    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player actor, InteractionHand hand) {
        if (!level.isClientSide && actor instanceof ServerPlayer player) WardingCasts.use(player, wand, hand);
        return wand;
    }
}
