package thaumcraft.api.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The original TC4 wand focus contract.
 *
 * <p>Forced 1.21.1 changes: {@code EntityPlayer} became {@link Player}, {@code World} became {@link Level},
 * {@code MovingObjectPosition} became {@link net.minecraft.world.phys.BlockHitResult}, the removed
 * {@code IIcon} became a {@link net.minecraft.resources.ResourceLocation} and the numeric enchantment id
     * became a {@link net.minecraft.resources.ResourceKey}.</p>
     * <p>The 1.7.10 no-stack focus shape is retained but is not consumed/implemented by native
     * 1.21.1 focus dispatch; implementing it alone does not register a working focus.</p>
     * @deprecated Extend ItemFocusBasic using its stack/hand-aware methods and the supported FocusUpgradeData path instead.
 */
@Deprecated
public interface IWandFocus {
    int getFocusColor();

    /** The texture of the focus' depth layer; the original returned an atlas icon. */
    net.minecraft.resources.ResourceLocation getFocusDepthLayerIcon();

    /** The texture of the focus' ornament; the original returned an atlas icon. */
    net.minecraft.resources.ResourceLocation getOrnament();

    WandFocusAnimation getAnimation();

    thaumcraft.api.aspects.AspectList getVisCost();

    boolean isVisCostPerTick();

    net.minecraft.world.item.ItemStack onFocusRightClick(net.minecraft.world.item.ItemStack stack, Level level, Player player,
                                                         net.minecraft.world.phys.BlockHitResult target);

    void onUsingFocusTick(net.minecraft.world.item.ItemStack stack, Player player, int ticks);

    void onPlayerStoppedUsingFocus(net.minecraft.world.item.ItemStack stack, Level level, Player player, int ticks);

    String getSortingHelper(net.minecraft.world.item.ItemStack stack);

    boolean onFocusBlockStartBreak(net.minecraft.world.item.ItemStack stack, int x, int y, int z, Player player);

    /** True when the focus accepts the enchantment. */
    boolean acceptsEnchant(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment);

    /** The original focus animation. */
    static enum WandFocusAnimation {
        WAVE,
        CHARGE;
    }
}
