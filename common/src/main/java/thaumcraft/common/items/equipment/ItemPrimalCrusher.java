package thaumcraft.common.items.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IWarpingGear;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Primal Crusher (original ItemPrimalCrusher). Its material {@code PRIMALVOID} has a harvest level of five, above netherite, five
 * hundred uses, an efficiency of eight, four points of damage against entities and an enchantability of twenty; the tool itself adds
 * three and a half points, the same value the original 1.7 ItemTool was built with.
 *
 * <p>The crusher is a pickaxe and a shovel at once: it is effective against everything either of them can mine. It cannot harvest wood,
 * leaves or cloth, the original's material rule. When a block is broken with it and the player is not sneaking, the eight neighbours in the
 * plane across the face that was looked at are harvested as well, each of them costing the crusher one use. The crusher adds two points of
 * warp to its wielder and mends itself by one point every second.
 */
public class ItemPrimalCrusher extends DiggerItem implements IRepairable, IWarpingGear {
    /** The blocks the crusher mines: the union of the pickaxe's and the shovel's blocks (original tool classes shovel and pickaxe). */
    public static final TagKey<net.minecraft.world.level.block.Block> MINEABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "mineable/primal_crusher"));

    public ItemPrimalCrusher(Tier tier, Properties properties) {
        super(tier, MINEABLE, properties.rarity(Rarity.EPIC).attributes(
                DiggerItem.createAttributes(tier, EquipmentRules.PRIMAL_CRUSHER_TOOL_DAMAGE, EquipmentRules.PRIMAL_CRUSHER_ATTACK_SPEED)));
    }

    /**
     * Original ItemPrimalCrusher.func_150897_b: the crusher never harvests wood, leaves or cloth. In 1.21.1 the tool's own rules decide
     * the drops, so the three kinds are denied here.
     */
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL)) return false;
        return super.isCorrectToolForDrops(stack, state);
    }

    /**
     * Original ItemPrimalCrusher.func_150894_a: breaking a block also breaks the eight neighbours in the plane across the face the player
     * was looking at, unless the player is sneaking. Every neighbour that is harvested costs the crusher one use.
     */
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        boolean result = super.mineBlock(stack, level, state, pos, entity);
        if (level.isClientSide || !(entity instanceof Player player) || player.isShiftKeyDown()) return result;
        if (!effectiveFor(stack, state)) return result;
        Direction face = lookedAtFace(level, player);
        int[] offsets = EquipmentRules.primalCrusherOffsets(face.ordinal());
        for (int i = 0; i < offsets.length; i += 3) {
            BlockPos target = pos.offset(offsets[i], offsets[i + 1], offsets[i + 2]);
            if (!level.isLoaded(target) || !level.mayInteract(player, target)) continue;
            BlockState targetState = level.getBlockState(target);
            if (targetState.isAir() || targetState.getDestroySpeed(level, target) < 0.0F) continue;
            if (!effectiveFor(stack, targetState)) continue;
            stack.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
            level.destroyBlock(target, true, entity);
        }
        return result;
    }

    /** Original ItemPrimalCrusher.onUpdate: the crusher mends itself by one point every twenty ticks of its carrier's age. */
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        // Original damageItem(-1) does nothing for creative players, just like void tools.
        thaumcraft.common.items.VoidMetal.repair(stack, entity);
    }

    /** Original ItemPrimalCrusher.getWarp: two points of warp. */
    @Override public int getWarp(ItemStack stack, Player player) { return EquipmentRules.PRIMAL_CRUSHER_WARP; }

    /**
     * The face the player looked at, the original's {@code this.side}. Original ItemPrimalCrusher.onBlockStartBreak: the face is the
     * one the ray trace of the player hit, so the crusher harvests the plane across it.
     */
    private static Direction lookedAtFace(Level level, Player player) {
        Vec3 eyes = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // Original BlockUtils.getTargetBlock: the ray of the player reaches ten blocks.
        BlockHitResult hit = level.clip(new ClipContext(eyes, eyes.add(look.scale(10.0D)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) return hit.getDirection();
        return Direction.getNearest(look.x, look.y, look.z);
    }

    /** The 1.21.1 form of the original's {@code ForgeHooks.isToolEffective}. */
    private static boolean effectiveFor(ItemStack stack, BlockState state) {
        return EquipmentRules.primalCrusherHarvests(!state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state), false);
    }
}
