/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.item.kami.KamiComponents.PearlTarget;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;

/**
 * The Celestial Pearl: right-clicked on a Celestial Gateway it remembers where the gateway is (1.7.10 stack tag
 * {@code x}, {@code y}, {@code z}, {@code dim}; the {@link KamiComponents#PEARL_TARGET} component here), sneak-clicked in
 * the air it forgets. An attuned pearl glows. The pearl has no research of its own: its recipe belongs to the Celestial
 * Gateway entry ({@code WARP_GATE}), which the KamiWorld group registers.
 */
public class ItemSkyPearl extends KamiItemBase {

    public ItemSkyPearl(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isAttuned(ItemStack stack) {
        return stack.has(KamiComponents.PEARL_TARGET.get());
    }

    public static void setValues(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos) {
        stack.set(KamiComponents.PEARL_TARGET.get(), new PearlTarget(dimension, pos.immutable()));
    }

    /** The attuned position; the origin for a pearl that is not attuned (1.7.10 answered 0 for x, y and z). */
    public static BlockPos getPos(ItemStack stack) {
        PearlTarget target = stack.get(KamiComponents.PEARL_TARGET.get());
        return target == null ? BlockPos.ZERO : target.pos();
    }

    public static int getX(ItemStack stack) {
        return getPos(stack).getX();
    }

    public static int getY(ItemStack stack) {
        return getPos(stack).getY();
    }

    public static int getZ(ItemStack stack) {
        return getPos(stack).getZ();
    }

    /** The dimension of the attuned position, or null for a pearl that is not attuned. */
    public static ResourceKey<Level> getDim(ItemStack stack) {
        PearlTarget target = stack.get(KamiComponents.PEARL_TARGET.get());
        return target == null ? null : target.dimension();
    }

    /**
     * The lines of a pearl's tooltip and of the gateway's destination list (1.7.10 {@code addInfo}): the coordinates, in
     * simple mode without the height, and the distance to {@code pos} or a note that the pearl is bound to another dimension.
     */
    public static void addInfo(ItemStack stack, ResourceKey<Level> dimension, Vec3 pos, List<Component> list,
            boolean simpleMode) {
        if (!isAttuned(stack)) return;
        int x = getX(stack);
        int y = getY(stack);
        int z = getZ(stack);
        list.add(Component.literal("X: " + x));
        if (!simpleMode) list.add(Component.literal("Y: " + y));
        list.add(Component.literal("Z: " + z));
        if (!getDim(stack).equals(dimension)) {
            if (!simpleMode) list.add(Component.translatable("ttmisc.differentDim").withStyle(ChatFormatting.RED));
        } else {
            double dx = x - pos.x;
            double dy = simpleMode ? 0 : y - pos.y;
            double dz = z - pos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            list.add(Component.translatable("ttmisc.distance")
                    .append(": " + BigDecimal.valueOf(distance).setScale(2, RoundingMode.UP) + "m")
                    .withStyle(ChatFormatting.BLUE));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Block gate = KamiHooks.optionalBlock(LibBlockNames.WARP_GATE);
        if (player != null && gate != null && level.getBlockState(context.getClickedPos()).is(gate)
                && !isAttuned(stack)) {
            setValues(stack, level.dimension(), context.getClickedPos());
            KamiHooks.playToggleSound(level, player);
        }
        // 1.7.10 answered true for every block: the pearl is never used up by the click, nothing else happens.
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && isAttuned(stack)) {
            KamiHooks.playToggleSound(level, player);
            stack.remove(KamiComponents.PEARL_TARGET.get());
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Player player = KamiHooks.localPlayer();
        if (player == null) return;
        addInfo(stack, player.level().dimension(), player.getBoundingBox().getCenter(), tooltip, false);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isAttuned(stack);
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_SKY_PEARL, LibResearch.KEY_WARP_GATE,
                new ItemStack(this, 2), 6,
                new AspectList().add(Aspect.TRAVEL, 32).add(Aspect.ELDRITCH, 32).add(Aspect.FLIGHT, 32)
                        .add(Aspect.AIR, 16),
                new ItemStack(Items.ENDER_PEARL), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                ItemKamiResource.stack(ItemKamiResource.ENDER_SHARD), new ItemStack(Items.LAPIS_BLOCK),
                new ItemStack(Items.DIAMOND));
    }
}
