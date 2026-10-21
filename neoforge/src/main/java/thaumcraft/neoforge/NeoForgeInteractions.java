package thaumcraft.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.common.util.TriState;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusTrade;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.golems.ItemGolemBell;
import thaumcraft.common.items.golems.ItemGolemPlacer;
import thaumcraft.common.lib.world.TradeSwaps;

/**
 * NeoForge half of the loader interaction callbacks of the shared bootstrap.
 *
 * <p>The Fabric bootstrap registers three loader callbacks:</p>
 * <ul>
 *   <li>{@code UseBlockCallback} - the golem placer, the golem bell, the resonator and the
 *       sneak label of a tube filter;</li>
 *   <li>{@code AttackBlockCallback} - the swing of the trade focus;</li>
 *   <li>the protection consultation of {@link TradeSwaps}.</li>
 * </ul>
 *
 * <p>NeoForge owns the cancellable {@link PlayerInteractEvent.RightClickBlock} and
 * {@link PlayerInteractEvent.LeftClickBlock} and the cancellable {@link BlockEvent.BreakEvent} and
 * {@link BlockEvent.EntityPlaceEvent}; they are the exact counterparts of the three Fabric
 * callbacks and are consulted in the same order.</p>
 */
final class NeoForgeInteractions {
    private NeoForgeInteractions() {}

    static void register(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(NeoForgeInteractions::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(NeoForgeInteractions::onLeftClickBlock);
        TradeSwaps.install(NeoForgeInteractions::allowsSwap);
    }

    /** The Fabric UseBlockCallback of the shared bootstrap. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var hand = event.getHand();
        var hit = event.getHitVec();
        var context = new UseOnContext(player, hand, hit);
        // Original golem deployment is item-use-first even against inventories.
        if (player.getItemInHand(hand).getItem() instanceof ItemGolemPlacer placer) {
            InteractionResult placed = placer.useOn(context);
            if (placed != InteractionResult.PASS) {
                consume(event, placed);
                return;
            }
        }
        // Original bell onItemUseFirst marks the clicked face instead of opening that chest.
        if (player.getItemInHand(hand).getItem() instanceof ItemGolemBell bell) {
            InteractionResult marked = bell.useOn(context);
            if (marked != InteractionResult.PASS) {
                consume(event, marked);
                return;
            }
        }
        InteractionResult inspect = ItemResonator.tryInspect(context);
        if (inspect != InteractionResult.PASS) {
            consume(event, inspect);
            return;
        }
        // Modern secondary use skips block activation even for a plain
        // stick. Original filter sneak-removal preceded attachment, except
        // item-first tools; preserve that without bypassing wand rotation.
        if (player.isShiftKeyDown() && !(player.getItemInHand(hand).getItem() instanceof ItemWandCasting)) {
            InteractionResult label = BlockTube.tryLabelInteraction(context);
            if (label != InteractionResult.PASS) {
                consume(event, label);
                return;
            }
        }
    }

    /** The Fabric AttackBlockCallback of the shared bootstrap. */
    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        var player = event.getEntity();
        ItemStack wand = player.getItemInHand(event.getHand());
        if (!(FocusData.get(wand).getItem() instanceof ItemFocusTrade)) return;
        if (player instanceof ServerPlayer server) ItemFocusTrade.leftClick(server, wand, event.getPos());
        event.setCanceled(true);
    }

    /**
     * The protection consultation of the shared bootstrap: a real right click, a real break and a
     * real place must all be allowed before the focus may swap the block.
     */
    private static boolean allowsSwap(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState before, BlockState after) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var use = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        NeoForge.EVENT_BUS.post(use);
        if (use.isCanceled()) return false;
        var brk = new BlockEvent.BreakEvent(level, pos, before, player);
        NeoForge.EVENT_BUS.post(brk);
        if (brk.isCanceled()) return false;
        var place = new BlockEvent.EntityPlaceEvent(
                net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos), before, player);
        NeoForge.EVENT_BUS.post(place);
        return !place.isCanceled();
    }

    private static void consume(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
    }
}
