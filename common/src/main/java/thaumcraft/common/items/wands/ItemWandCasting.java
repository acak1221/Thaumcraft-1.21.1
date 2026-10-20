package thaumcraft.common.items.wands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.tiles.TileNode;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.math.BigDecimal;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.WandFocusActions;

/** Component-backed centivis wand. The bounded material catalogue does not imply focus/staff parity. */
public final class ItemWandCasting extends Item {
    // Client installs only the original SHIFT-upgrade tooltip presentation.
    // Default no-op keeps common/dedicated-server loading free of client types.
    private static volatile BiConsumer<ItemStack, List<Component>> focusUpgradeTooltip = (focus, tooltip) -> {};
    public static void setFocusUpgradeTooltip(BiConsumer<ItemStack, List<Component>> renderer) {
        focusUpgradeTooltip = Objects.requireNonNull(renderer, "renderer");
    }
    // Original ObjectInUse is a USE SESSION, not the current crosshair. Keep it
    // off the persistent item data: a reloaded player is not still using an item.
    // Native Entity.equals compares numeric IDs, NOT Java object identity.
    // Integrated client/server players therefore compare equal. Separate sides
    // prevent client release from erasing the still-active SERVER node session
    // before its release packet arrives (and accidentally starting its focus).
    // Values retain neither a Level nor a player; weak keys remain ephemeral.
    private static final Map<LivingEntity, NodeUse> SERVER_NODE_USES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<LivingEntity, NodeUse> CLIENT_NODE_USES = Collections.synchronizedMap(new WeakHashMap<>());
    private record NodeUse(BlockPos pos, InteractionHand hand, ItemStack stack) {}
    private static Map<LivingEntity, NodeUse> nodeUses(LivingEntity entity) {
        return entity.level().isClientSide ? CLIENT_NODE_USES : SERVER_NODE_USES;
    }
    /** Read-only actual held node-session state, not persisted item data. */
    public static boolean isUsingNode(LivingEntity entity) { return nodeUses(entity).containsKey(entity); }
    public static void clearNodeUses() { SERVER_NODE_USES.clear(); CLIENT_NODE_USES.clear(); }
    public ItemWandCasting(Properties properties) { super(properties); }
    public static WandData data(ItemStack stack) { return stack.getOrDefault(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY); }
    public static int getVis(ItemStack stack, String tag) { return data(stack).amount(tag); }
    public static int getMaxVis(ItemStack stack) { return data(stack).capacity(); }
    public static int addRealVis(ItemStack stack, String tag, int amount, boolean apply) {
        if (amount <= 0 || !WandData.PRIMALS.contains(tag)) return amount;
        WandData data = data(stack);
        int accepted = Math.min(amount, data.capacity() - data.amount(tag));
        if (apply && accepted > 0) stack.set(ThaumcraftContent.WAND_DATA.get(), data.withAmount(tag, data.amount(tag) + accepted));
        return amount - accepted;
    }

    /** Original ItemWandCasting.onUpdate: the server asks the rod to act on its carrier's tick (an elemental rod refills its primal). */
    @Override public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        WandData before = data(stack);
        WandData after = WandRodPassive.tick(before, player.tickCount);
        if (after != before) stack.set(ThaumcraftContent.WAND_DATA.get(), after);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return Integer.MAX_VALUE; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override public InteractionResult useOn(UseOnContext context) {
        InteractionResult bore = thaumcraft.common.blocks.BlockArcaneBore.tryWandInteraction(context);
        if (bore != InteractionResult.PASS) return bore;
        InteractionResult boreBase = thaumcraft.common.blocks.BlockArcaneBoreBase.tryWandInteraction(context);
        if (boreBase != InteractionResult.PASS) return boreBase;
        InteractionResult infusion = thaumcraft.common.lib.infusion.InfusionStructure.tryWandInteraction(context);
        if (infusion != InteractionResult.PASS) return infusion;
        InteractionResult relay = thaumcraft.common.blocks.BlockVisRelay.tryWandInteraction(context);
        if (relay != InteractionResult.PASS) return relay;
        InteractionResult tube = thaumcraft.common.blocks.BlockTube.tryWandInteraction(context);
        if (tube != InteractionResult.PASS) return tube;
        InteractionResult buffer = thaumcraft.common.blocks.BlockTubeBuffer.tryWandInteraction(context);
        if (buffer != InteractionResult.PASS) return buffer;
        InteractionResult alembic = thaumcraft.common.blocks.BlockAlembic.tryWandInteraction(context);
        if (alembic != InteractionResult.PASS) return alembic;
        InteractionResult crucible = thaumcraft.common.blocks.BlockCrucible.tryCreate(context);
        if (crucible != InteractionResult.PASS) return crucible;
        InteractionResult arcane = thaumcraft.common.blocks.BlockArcaneWorkbench.tryCreate(context);
        if (arcane != InteractionResult.PASS) return arcane;
        if (!WandManager.canCreateThaumonomicon(context)) return InteractionResult.PASS;
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        return WandManager.createThaumonomicon(context) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        nodeUses(player).remove(player);
        // Original ItemWandCasting:492-509 acquires an actual IWandable hit.
        // Native block reach replaces Forge's getBlockReachDistance; a hardcoded
        // ten-block ray incorrectly allowed ordinary survival to drain remotely.
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, true);
        if (hit instanceof BlockHitResult crucibleHit && hit.getType() == HitResult.Type.BLOCK
                && level.hasChunkAt(crucibleHit.getBlockPos())
                && level.getBlockEntity(crucibleHit.getBlockPos()) instanceof thaumcraft.common.tiles.TileCrucible crucible) {
            if (!level.mayInteract(player, crucibleHit.getBlockPos())
                    || !player.mayUseItemAt(crucibleHit.getBlockPos(), crucibleHit.getDirection(), stack)) return InteractionResultHolder.fail(stack);
            if (!level.isClientSide && player.isShiftKeyDown()) crucible.spillRemnants();
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !level.hasChunkAt(blockHit.getBlockPos()) || !(level.getBlockEntity(blockHit.getBlockPos()) instanceof TileNode)) {
            return WandFocusActions.use(player, hand);
        }
        if (!level.mayInteract(player, blockHit.getBlockPos())
                || !player.mayUseItemAt(blockHit.getBlockPos(), blockHit.getDirection(), stack)) return InteractionResultHolder.fail(stack);
        // Starting use does not move vis; only native server ticks do.
        nodeUses(player).put(player, new NodeUse(blockHit.getBlockPos().immutable(), hand, stack));
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        NodeUse target = nodeUses(entity).get(entity);
        if (target != null) {
            if (level.isClientSide) return; // Server owns stop/drain; synced slots may replace client stack identities.
            if (!(entity instanceof Player player)) return;
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, true);
            if (target.stack != stack || player.getItemInHand(target.hand) != stack
                    || !(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                    || !target.pos.equals(blockHit.getBlockPos()) || !level.hasChunkAt(target.pos)
                    || !(level.getBlockEntity(target.pos) instanceof TileNode node)) {
                nodeUses(entity).remove(entity);
                player.stopUsingItem();
                return;
            }
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && remaining % 5 == 0) {
                node.drainIntoWand(serverPlayer, stack);
            }
            return; // A node session can NEVER become a paid fire tick mid-use.
        }
        WandFocusActions.onUseTick(level, entity, stack, remaining);
    }

    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (nodeUses(entity).remove(entity) == null) WandFocusActions.onRelease(stack, level, entity, remaining);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        WandData state = data(stack);
        tooltip.add(Component.translatable("item.capacity.text").append(" " + state.capacity() / 100));
        tooltip.add(Component.translatable("tc.port.wand.base_cost", new BigDecimal(Float.toString(WandMaterials.capModifier(state.cap())))
                .movePointRight(2).stripTrailingZeros().toPlainString()));
        for (String tag : WandData.PRIMALS) if (state.vis().containsKey(tag)) {
            tooltip.add(Component.literal(Aspect.getAspect(tag).getName() + ": "
                    + BigDecimal.valueOf(state.amount(tag), 2).stripTrailingZeros().toPlainString() + "/" + state.capacity() / 100));
        }
        ItemStack focus = FocusData.get(stack);
        if (!focus.isEmpty()) {
            tooltip.add(focus.getHoverName().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN));
            focusUpgradeTooltip.accept(focus, tooltip);
        }
    }
    @Override public Component getName(ItemStack stack) {
        WandData state = data(stack);
        return Component.translatable("tc.port.wand.name", Component.translatable("item.Wand." + state.cap() + ".cap"),
                Component.translatable("item.Wand." + state.rod() + ".rod"), Component.translatable("item.Wand.wand.obj"));
    }
}
