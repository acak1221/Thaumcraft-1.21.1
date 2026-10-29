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
package thaumic.tinkerer.common.item.foci;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ItemApi;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;

/**
 * The base of the ordinary wand foci of Thaumic Tinkerer: the ornament and the depth layer by name, the wave animation of
 * the wand, and a held cast ({@link #isUseItem}) for the foci that work while the use key is down.
 *
 * <p>The focus is an item of its own; the wand holds a copy of it (with its upgrades and its data components) and calls the
 * hooks of {@link ItemFocusBasic} with <em>the wand's stack</em>: the cost of the 1.7.10 code is read from
 * {@code getVisCost(WandHelper.getFocusItem(wand))} here, and the vis and the upgrades go through {@link WandHelper}.</p>
 *
 * <p>1.7.10 passed the hit of the player's ray to {@code onFocusRightClick}; the 1.21.1 hook has none (the hand is passed
 * instead), so a focus that needs a block asks {@link #trace}.</p>
 *
 * <p>The 1.7.10 override of {@code canApplyUpgrade} that answered {@code true} for every upgrade is not ported: in 1.21.1 the
 * focus would then accept an upgrade that {@code getPossibleUpgradesByRank} does not offer and become unmountable
 * (see {@code docs/API_GAPS.md}). The focal manipulator offers what each focus lists.</p>
 */
public abstract class ItemModFocus extends ItemFocusBasic implements ITTinkererRegisterable {
    protected ItemModFocus(Item.Properties properties) {
        super(properties);
    }

    protected boolean hasOrnament() {
        return false;
    }

    protected boolean hasDepth() {
        return false;
    }

    private ResourceLocation texture(String suffix) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return id.withPath("textures/item/" + id.getPath() + suffix + ".png");
    }

    @Override
    public ResourceLocation getOrnament(ItemStack focusstack) {
        return hasOrnament() ? texture("_orn") : null;
    }

    @Override
    public ResourceLocation getFocusDepthLayerIcon(ItemStack focusstack) {
        return hasDepth() ? texture("_depth") : null;
    }

    @Override
    public WandFocusAnimation getAnimation(ItemStack focusstack) {
        return WandFocusAnimation.WAVE;
    }

    @Override
    public boolean isVisCostPerTick(ItemStack focusstack) {
        return false;
    }

    /** True for the foci that are held: the wand then calls {@code onUsingFocusTick} every tick while the key is down. */
    public boolean isUseItem(ItemStack focusstack) {
        return isVisCostPerTick(focusstack);
    }

    @Override
    public ItemStack onFocusRightClick(ItemStack wandstack, Level level, Player player, InteractionHand hand) {
        if (isUseItem(WandHelper.getFocusItem(wandstack))) player.startUsingItem(hand);
        return wandstack;
    }

    @Override
    public boolean onFocusBlockStartBreak(ItemStack focusstack, int x, int y, int z, Player player) {
        return false;
    }

    // ---- helpers of the foci ---------------------------------------------------------------------------------------

    /**
     * The block the player looks at, the replacement of the {@code MovingObjectPosition} that 1.7.10 passed to the focus.
     *
     * @param range      the length of the ray
     * @param fluids     true when a fluid counts as a hit (the ray of the wand); false looks through it
     * @param collidable true when only blocks with a collision box count (the ray of {@code BlockUtils.getTargetBlock}, which
     *                   looks through grass and flowers)
     * @return a hit whose {@code getType()} is {@code MISS} when nothing was found
     */
    public static BlockHitResult trace(Player player, double range, boolean fluids, boolean collidable) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(range));
        return player.level().clip(new ClipContext(eye, end,
                collidable ? ClipContext.Block.COLLIDER : ClipContext.Block.OUTLINE,
                fluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, player));
    }

    /** {@code ItemApi.getItem(field, meta)}: a stack of the Thaumcraft item the 1.7.10 field and metadata named. */
    protected static ItemStack tc(String field, int meta) {
        return Objects.requireNonNull(ItemApi.getItem(field, meta), "Thaumcraft item " + field + ":" + meta);
    }

    /** {@code ItemApi.getBlock(field, meta)}: the item of a Thaumcraft block named by its 1.7.10 field and metadata. */
    protected static ItemStack tcBlock(String field, int meta) {
        return Objects.requireNonNull(ItemApi.getBlock(field, meta), "Thaumcraft block " + field + ":" + meta);
    }
}
