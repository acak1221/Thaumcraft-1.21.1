package thaumcraft.common.items.wands.foci;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.world.PortableHoles;

/**
 * Original ItemFocusPortableHole. A right click on a block measures how many blocks in a row (a block with a tile counts) can be passed
 * from the clicked face inward (33 plus 8 for each Enlarge), pays entropy and air in proportion (10 centivis each per block, at least
 * for one), and makes a hole of that chain: each block 3 x 3 across, lasting 120 ticks plus 60 for each Extend ({@code TileHole}). The Outer
 * Lands refuse it. The payment is taken even when nothing could be made a hole.
 */
public final class ItemFocusPortableHole extends ItemFocusBasic {
    private static final SoundEvent WANDFAIL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wandfail"));

    public ItemFocusPortableHole(Properties properties) { super(properties); }

    @Override public AspectList getVisCost(ItemStack focus) {
        return new AspectList().add(Aspect.ENTROPY, PortableHoleRules.BASE_COST).add(Aspect.AIR, PortableHoleRules.BASE_COST);
    }
    @Override public int getFocusColor(ItemStack focus) { return 594985; }
    @Override public String getSortingHelper(ItemStack focus) { return "BPH" + super.getSortingHelper(focus); }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return rank >= 1 && rank <= 5 ? new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.enlarge, FocusUpgradeType.extend} : new FocusUpgradeType[0];
    }

    /** CENTIVIS of entropy and air for a measured depth, through the wand's own modifiers and the Frugal upgrade. */
    public static Map<String, Integer> activationCost(ItemStack wand, Player actor, int depth) {
        int each = PortableHoleRules.cost(depth);
        return FocusVis.price(new AspectList().add(Aspect.ENTROPY, each).add(Aspect.AIR, each), wand, actor);
    }

    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) use(wand, level, serverPlayer, hand);
        return wand;
    }

    private static void use(ItemStack wand, Level level, ServerPlayer player, InteractionHand hand) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Portable Hole use requires the server thread");
        if (!(level instanceof net.minecraft.server.level.ServerLevel server) || player.level() != level || !player.isAlive() || player.isSpectator()
                || player.getItemInHand(hand) != wand || wand.getCount() != 1 || !ArcaneVis.isCraftingWand(wand)
                || !(FocusData.get(wand).getItem() instanceof ItemFocusPortableHole)) return;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (PortableHoleRules.refusedIn(level.dimension().location().toString()) || !level.mayInteract(player, pos)) {
            // The Outer Lands' own refusal; the protection check of 1.21.1 is a declared addition.
            level.playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, WANDFAIL, SoundSource.PLAYERS, 1F, 1F);
            player.swing(hand);
            return;
        }
        ItemStack focus = FocusData.get(wand);
        int max = PortableHoleRules.maxDepth(FocusUpgradeData.level(focus, FocusUpgradeType.enlarge));
        Direction inward = hit.getDirection().getOpposite();
        BlockPos cursor = pos;
        int depth = 0;
        while (depth < max && server.isLoaded(cursor) && PortableHoles.passable(server, cursor, server.getBlockState(cursor))) {
            cursor = cursor.relative(inward);
            depth++;
        }
        ItemStack before = wand.copy(), paid = before.copy();
        Map<String, Integer> costs = activationCost(before, player, depth);
        // The vis is taken whether or not the first block can become a hole.
        if (FocusVis.debit(paid, costs)) {
            wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
            player.getInventory().setChanged();
            PortableHoles.create(server, pos, hit.getDirection().get3DDataValue(), PortableHoleRules.chainLength(depth),
                    PortableHoleRules.duration(FocusUpgradeData.level(focus, FocusUpgradeType.extend)));
        }
        player.swing(hand);
        level.playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1F, 1F);
        player.inventoryMenu.broadcastChanges();
    }
}
