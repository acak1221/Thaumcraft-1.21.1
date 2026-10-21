package thaumcraft.common.items.wands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWandEnchantments;
import thaumcraft.common.tiles.TileNode;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.math.BigDecimal;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.IWandable;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
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
    /**
     * Original ItemWandCasting:119-121 and :256-258: the sceptre marker of the original wand.
     *
     * <p>The original wrote the NBT byte tag "sceptre" on the crafted wand (ArcaneSceptreRecipe:68); the
     * native marker is the {@code thaumcraft:wand_sceptre} data component. A sceptre holds one and a half
     * times the capacity of its rod and consumes ten percent less vis.</p>
     */
    public static boolean isSceptre(ItemStack stack) { return ThaumcraftWandEnchantments.isSceptre(stack); }
    /** The capacity of this wand in centivis: the rod capacity, and one and a half times of it for a sceptre. */
    public static int getMaxVis(ItemStack stack) { return WandMaterials.capacity(data(stack).rod(), isSceptre(stack)); }
    public static int addRealVis(ItemStack stack, String tag, int amount, boolean apply) {
        if (amount <= 0 || !WandData.PRIMALS.contains(tag)) return amount;
        WandData data = data(stack);
        int accepted = Math.min(amount, getMaxVis(stack) - data.amount(tag));
        if (apply && accepted > 0) stack.set(ThaumcraftContent.WAND_DATA.get(), data.withAmount(tag, data.amount(tag) + accepted));
        return amount - accepted;
    }

    /**
     * Original setRod: a staff rod writes an attribute modifier "Weapon modifier", +6 attack damage (operation 0) under the base-damage
     * id, so a staff hits for 1 + 6 = 7 bare-handed-plus-staff and shows it in its tooltip like a sword. Here it is the native item
     * component, set when the staff is made ({@link #withData}) and restored on the carrier's tick if a staff lacks it.
     */
    public static final ItemAttributeModifiers STAFF_MODIFIERS = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND).build();

    /** Original {@code isStaff}: the rod is a {@code StaffRod}. */
    public static boolean isStaff(ItemStack stack) { return WandMaterials.rod(data(stack).rod()).staff(); }

    /**
     * Original {@code ItemWandCasting.getFocusPotency}: the potency upgrade of the
     * installed focus plus one when the wand carries a staff rod with runes (only
     * the primal staff has them, {@code ConfigItems:325}). Without a focus the
     * original returns zero, so the rune bonus needs a focus as well.
     */
    public static int focusPotency(ItemStack stack) {
        ItemStack focus = FocusData.get(stack);
        if (focus.isEmpty()) return 0;
        return WandMaterials.focusPotency(FocusUpgradeData.level(focus, FocusUpgradeType.potency),
                WandMaterials.rod(data(stack).rod()));
    }

    /** Writes the wand data and the weapon modifier that belongs to its rod (a staff has it, a wand has not). */
    public static void withData(ItemStack stack, WandData data) {
        stack.set(ThaumcraftContent.WAND_DATA.get(), data);
        normalizeModifiers(stack);
    }

    private static void normalizeModifiers(ItemStack stack) {
        // Every item has the (empty) modifier component by default; a custom one set by someone else is left alone.
        ItemAttributeModifiers current = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (isStaff(stack)) {
            if (current.modifiers().isEmpty()) stack.set(DataComponents.ATTRIBUTE_MODIFIERS, STAFF_MODIFIERS);
        } else if (STAFF_MODIFIERS.equals(current)) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY); // the default again: no patch left on the stack
        }
    }

    /** Original ItemWandCasting.onUpdate: the server asks the rod to act on its carrier's tick (an elemental rod refills its primal). */
    @Override public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        normalizeModifiers(stack);
        WandData before = data(stack);
        // The passive rod refills up to a tenth of the capacity of the wand it sits in (original
        // WandRodPrimalOnUpdate:34); a sceptre holds one and a half times the capacity of its rod.
        WandData after = WandRodPassive.tick(before, getMaxVis(stack), player.tickCount, player.getRandom()::nextInt);
        if (after != before) stack.set(ThaumcraftContent.WAND_DATA.get(), after);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return Integer.MAX_VALUE; }
    @Override public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return FocusData.get(stack).getItem() instanceof thaumcraft.common.items.wands.foci.ItemFocusTrade ? 0 : super.getDestroySpeed(stack, state);
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    /**
     * Original {@code ItemWandCasting.onItemUseFirst}: the block and its block entity
     * answer a wand click through the {@link IWandable} contract. A negative answer
     * means that the click was not handled at all.
     */
    private static InteractionResult dispatchWandable(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (player == null || !level.hasChunkAt(pos)) return InteractionResult.PASS;
        int side = context.getClickedFace().get3DDataValue();
        if (level.getBlockState(pos).getBlock() instanceof IWandable wandable) {
            int result = wandable.onWandRightClick(level, stack, player, pos.getX(), pos.getY(), pos.getZ(), side, 0);
            if (result >= 0) return result == 1 ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (level.getBlockEntity(pos) instanceof IWandable wandable) {
            int result = wandable.onWandRightClick(level, stack, player, pos.getX(), pos.getY(), pos.getZ(), side, 0);
            if (result >= 0) return result == 1 ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        // Original ItemWandCasting:onItemUseFirst - the IWandable contract of the
        // clicked block and of its block entity answers the wand click first.
        InteractionResult wandable = dispatchWandable(context);
        if (wandable != InteractionResult.PASS) return wandable;
        InteractionResult charger = thaumcraft.common.blocks.BlockMagicWorkbenchCharger.tryWandInteraction(context);
        if (charger != InteractionResult.PASS) return charger;
        InteractionResult thaumatorium = thaumcraft.common.lib.ThaumatoriumFormation.tryForm(context);
        if (thaumatorium != InteractionResult.PASS) return thaumatorium;
        InteractionResult advanced = thaumcraft.common.lib.AdvancedFurnaceFormation.tryForm(context);
        if (advanced != InteractionResult.PASS) return advanced;
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
        InteractionResult reservoir = thaumcraft.common.blocks.BlockEssentiaReservoir.tryWandInteraction(context);
        if (reservoir != InteractionResult.PASS) return reservoir;
        InteractionResult alembic = thaumcraft.common.blocks.BlockAlembic.tryWandInteraction(context);
        if (alembic != InteractionResult.PASS) return alembic;
        InteractionResult crucible = thaumcraft.common.blocks.BlockCrucible.tryCreate(context);
        if (crucible != InteractionResult.PASS) return crucible;
        InteractionResult arcane = thaumcraft.common.blocks.BlockArcaneWorkbench.tryCreate(context);
        if (arcane != InteractionResult.PASS) return arcane;
        if (!WandManager.canCreateThaumonomicon(context)) {
            // Original ItemWandCasting:390-415 - the wand takes the warded stone blocks back.
            return WandWardedStone.tryRemove(context);
        }
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        return WandManager.createThaumonomicon(context) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    /**
     * Original {@code ItemWandCasting.onItemRightClick}: the block and its block entity
     * answer a wand click without a target through the {@link IWandable} contract. A
     * null answer means that the click was not handled at all.
     */
    private static ItemStack wandableResult(Level level, Player player, ItemStack stack, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof IWandable wandable) {
            ItemStack result = wandable.onWandRightClick(level, stack, player);
            if (result != null) return result;
        }
        if (level.getBlockEntity(pos) instanceof IWandable wandable) {
            ItemStack result = wandable.onWandRightClick(level, stack, player);
            if (result != null) return result;
        }
        return null;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        nodeUses(player).remove(player);
        // Original ItemWandCasting:492-509 acquires an actual IWandable hit.
        // Native block reach replaces Forge's getBlockReachDistance; a hardcoded
        // ten-block ray incorrectly allowed ordinary survival to drain remotely.
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, true);
        if (hit instanceof BlockHitResult wandableHit && hit.getType() == HitResult.Type.BLOCK
                && level.hasChunkAt(wandableHit.getBlockPos())) {
            // Original ItemWandCasting:492-509 - the IWandable contract without a target.
            ItemStack result = wandableResult(level, player, stack, wandableHit.getBlockPos());
            if (result != null) return InteractionResultHolder.success(result);
        }
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
        int capacity = getMaxVis(stack);
        tooltip.add(Component.translatable("item.capacity.text").append(" " + capacity / 100));
        tooltip.add(Component.translatable("tc.port.wand.base_cost", new BigDecimal(Float.toString(WandMaterials.capModifier(state.cap())))
                .movePointRight(2).stripTrailingZeros().toPlainString()));
        for (String tag : WandData.PRIMALS) if (state.vis().containsKey(tag)) {
            tooltip.add(Component.literal(Aspect.getAspect(tag).getName() + ": "
                    + BigDecimal.valueOf(state.amount(tag), 2).stripTrailingZeros().toPlainString() + "/" + capacity / 100));
        }
        ItemStack focus = FocusData.get(stack);
        if (!focus.isEmpty()) {
            tooltip.add(focus.getHoverName().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN));
            focusUpgradeTooltip.accept(focus, tooltip);
        }
    }
    @Override public Component getName(ItemStack stack) {
        WandData state = data(stack);
        var rod = WandMaterials.rod(state.rod());
        // Original getItemStackDisplayName: a staff is named after the plain rod ("greatwood_staff" -> Greatwood) with the staff noun.
        // A sceptre is named with the sceptre noun (ItemWandCasting:160).
        String noun = rod.staff() ? "staff" : isSceptre(stack) ? "sceptre" : "wand";
        return Component.translatable("tc.port.wand.name", Component.translatable("item.Wand." + state.cap() + ".cap"),
                Component.translatable("item.Wand." + rod.baseTag() + ".rod"), Component.translatable("item.Wand." + noun + ".obj"));
    }
}
