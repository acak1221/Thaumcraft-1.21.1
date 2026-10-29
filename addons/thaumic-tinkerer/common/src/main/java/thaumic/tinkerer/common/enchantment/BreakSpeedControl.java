/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.enchantment;

import dev.architectury.event.EventResult;
import dev.architectury.utils.value.IntValue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import thaumic.tinkerer.common.core.helper.ItemNBTHelper;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Shatter, Tunnel, Disintegrate and Flaming Touch: the speed at which the held tool digs.
 *
 * <p>1.7.10 changed {@code event.newSpeed} of {@code PlayerEvent.BreakSpeed} (and cancelled the event for a block the tool
 * cannot mine). Architectury has no event for the break speed, and the speed is asked from the player's own
 * {@code getDestroySpeed(state)} while the digging goes on, a call with no hook. The speed has a multiplier of its own in
 * 1.21.1, the player attribute {@code BLOCK_BREAK_SPEED}, which the game sends to the client like every attribute of the
 * player, so the server keeps a transient modifier on it that fits the block the player looks at: each tick, from the tool
 * in the main hand and the block under the crosshair ({@link #update}). The digging of that block then runs at the speed of
 * the original, on the server (which decides) and on the client (which predicts). The one tick that the modifier lags behind
 * a new target is the one deviation of the mechanism.</p>
 *
 * <p>The cancelled break-speed event of the original made the block unbreakable for the tool; the speed factor of zero does
 * the same for digging, and {@link #onBlockBreak} refuses the break itself (for a block that would break in one blow, and
 * for a player that claims to have finished it, a fake player such as the tablet's included) unless the player is in
 * creative mode, as the original did not ask the speed there.</p>
 *
 * <p>The fake player of the Animation Tablet digs with {@code state.getDestroyProgress(player, ...)} and is never ticked, so
 * nothing calls {@link #update} for it: its owner has to call {@link #updateFor} before each of those calls
 * ({@code docs/api-requests/Enchantments.md}, section 5). Without the call a tablet digs at the plain speed of the tool,
 * and the veto of {@link #onBlockBreak} still applies to it.</p>
 */
public final class BreakSpeedControl {
    /** Id of the transient attribute modifier on {@code BLOCK_BREAK_SPEED}. */
    public static final ResourceLocation MODIFIER = LibMisc.id("enchant_break_speed");

    /** The key of the direction that Tunnel remembers on the tool, as in 1.7.10 ({@code NBTTunnelDirection}). */
    public static final String NBT_TUNNEL_DIRECTION = "TTEnchantTunnelDir";

    private BreakSpeedControl() {}

    /** Keeps the modifier of the player's dig speed in step with the tool and the target. Server side, once per tick. */
    public static void update(ServerPlayer player) {
        update(player, null);
    }

    /**
     * Like {@link #update(ServerPlayer)} for a player that is not ticked and does not look at what it digs, such as the fake
     * player of the Animation Tablet: the speed is fitted to the block at {@code target} instead of the block under the
     * crosshair, so the owner calls it right before it asks {@code state.getDestroyProgress(player, level, pos)} (in
     * 1.7.10 the {@code BreakSpeed} event fired inside that very call). No lag, as the call comes with the target.
     */
    public static void updateFor(ServerPlayer player, BlockPos target) {
        update(player, target);
    }

    private static void update(ServerPlayer player, @Nullable BlockPos target) {
        AttributeInstance attribute = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (attribute == null) return;

        ItemStack held = player.getMainHandItem();
        int shatter = ModEnchantments.level(held, ModEnchantments.shatter);
        int tunnel = ModEnchantments.level(held, ModEnchantments.tunnel);
        int desintegrate = ModEnchantments.level(held, ModEnchantments.desintegrate);
        int autoSmelt = ModEnchantments.level(held, ModEnchantments.autoSmelt);
        if (shatter <= 0 && tunnel <= 0 && desintegrate <= 0 && autoSmelt <= 0) {
            if (attribute.hasModifier(MODIFIER)) attribute.removeModifier(MODIFIER);
            return;
        }

        double factor = 1;
        BlockPos pos = target;
        if (pos == null) {
            HitResult hit = player.pick(player.blockInteractionRange(), 0.0F, false);
            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) pos = blockHit.getBlockPos();
        }
        if (pos != null) {
            BlockState state = player.level().getBlockState(pos);
            if (!state.isAir()) {
                factor = factorFor(player, held, state, player.level(), pos, shatter, tunnel, desintegrate, autoSmelt);
            }
        }

        setFactor(attribute, factor);
    }

    private static double factorFor(ServerPlayer player, ItemStack held, BlockState state, Level level, BlockPos pos,
            int shatter, int tunnel, int desintegrate, int autoSmelt) {
        float hardness = state.getDestroySpeed(level, pos);
        boolean hasDirection = ItemNBTHelper.verifyExistance(held, NBT_TUNNEL_DIRECTION);
        float angleDifference = 0F;
        if (hasDirection) {
            float oldDirection = ItemNBTHelper.getFloat(held, NBT_TUNNEL_DIRECTION, 0F);
            angleDifference = Math.abs(Mth.wrapDegrees(oldDirection - player.getYRot()));
        }
        return EnchantmentFormulas.breakSpeedFactor(shatter, tunnel, desintegrate, autoSmelt, hardness, isWooden(state),
                hasDirection, angleDifference);
    }

    private static void setFactor(AttributeInstance attribute, double factor) {
        if (Math.abs(factor - 1) < 1.0E-9) {
            if (attribute.hasModifier(MODIFIER)) attribute.removeModifier(MODIFIER);
            return;
        }
        double amount = factor - 1;
        AttributeModifier existing = attribute.getModifier(MODIFIER);
        // Changing the modifier marks the attribute dirty and sends it to the client: only when the amount changed.
        if (existing != null && existing.amount() == amount) return;
        attribute.addOrUpdateTransientModifier(
                new AttributeModifier(MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /**
     * {@code BlockEvent.BreakEvent}: Tunnel remembers the way the player looked at the block, and the tools of Disintegrate and
     * Flaming Touch take one more point of wear for a block that is theirs and cannot break any other (which the original
     * implemented as "no speed, no break", and with the extra wear the {@code damageItem} of the speed event).
     */
    public static EventResult onBlockBreak(Level level, BlockPos pos, BlockState state, ServerPlayer player, IntValue xp) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return EventResult.pass();

        if (ModEnchantments.level(held, ModEnchantments.tunnel) > 0) {
            ItemNBTHelper.setFloat(held, NBT_TUNNEL_DIRECTION, player.getYRot());
        }

        int desintegrate = ModEnchantments.level(held, ModEnchantments.desintegrate);
        int autoSmelt = ModEnchantments.level(held, ModEnchantments.autoSmelt);
        if (desintegrate > 0 || autoSmelt > 0) {
            float hardness = state.getDestroySpeed(level, pos);
            boolean applies = EnchantmentFormulas.desintegrateApplies(desintegrate, hardness)
                    || EnchantmentFormulas.autoSmeltApplies(autoSmelt, isWooden(state));
            if (applies) {
                held.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            } else if (!player.getAbilities().instabuild) {
                return EventResult.interruptFalse();
            }
        }
        return EventResult.pass();
    }

    /**
     * {@code block.getMaterial() == Material.wood}. 1.21.1 has no materials; the wooden blocks of 1.7.10 (logs, planks,
     * fences, doors, chests, the crafting table, the bookshelf, the jukebox, signs...) are the blocks with a wooden step sound
     * and the blocks of the vanilla wood tags.
     */
    public static boolean isWooden(BlockState state) {
        SoundType sound = state.getSoundType();
        return sound == SoundType.WOOD || sound == SoundType.NETHER_WOOD || sound == SoundType.CHERRY_WOOD
                || sound == SoundType.BAMBOO_WOOD || sound == SoundType.STEM || state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.ALL_SIGNS);
    }
}
