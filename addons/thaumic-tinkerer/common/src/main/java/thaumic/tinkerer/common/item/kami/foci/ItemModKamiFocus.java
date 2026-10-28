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
package thaumic.tinkerer.common.item.kami.foci;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;

/**
 * The base of the three Kami foci. 1.7.10: not damageable, no repair, stack size one, rare (the Shadowbeam and the XP
 * Drain are Kami), a wave animation and a use hook that holds the wand while the focus is used. The ornament and depth
 * layers are the textures {@code <name>_orn.png} and {@code <name>_depth.png} of the item.
 *
 * <p>The original also made every upgrade applicable ({@code canApplyUpgrade} true); that is not carried over, because
 * a focus with an upgrade it does not offer cannot be mounted (see {@code docs/API_GAPS.md} 3b). The focal manipulator
 * offers what {@code getPossibleUpgradesByRank} lists, which is every rank of the original.</p>
 */
public abstract class ItemModKamiFocus extends ItemFocusBasic implements ITTinkererRegisterable {

    protected ItemModKamiFocus(Item.Properties properties) {
        super(properties);
    }

    protected boolean hasOrnament() {
        return false;
    }

    protected boolean hasDepth() {
        return false;
    }

    /** True for the foci that are held (the cost is per tick). */
    public boolean isUseItem(ItemStack stack) {
        return isVisCostPerTick(stack);
    }

    /** The Kami foci whose name is in the Kami colour (1.7.10: {@code getRarity} returned {@code kamiRarity}). */
    protected boolean isKamiRarity() {
        return false;
    }

    private ResourceLocation texture(String suffix) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return id.withPath("textures/item/" + id.getPath() + suffix + ".png");
    }

    @Override
    public ResourceLocation getOrnament(ItemStack focus) {
        return hasOrnament() ? texture("_orn") : null;
    }

    @Override
    public ResourceLocation getFocusDepthLayerIcon(ItemStack focus) {
        return hasDepth() ? texture("_depth") : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        return isKamiRarity() ? name.copy().withStyle(ChatFormatting.LIGHT_PURPLE) : name;
    }

    @Override
    public WandFocusAnimation getAnimation(ItemStack focus) {
        return WandFocusAnimation.WAVE;
    }

    @Override
    public ItemStack onFocusRightClick(ItemStack wandstack, Level level, Player player, InteractionHand hand) {
        if (isUseItem(wandstack)) player.startUsingItem(hand);
        return wandstack;
    }

    @Override
    public boolean onFocusBlockStartBreak(ItemStack focusstack, int x, int y, int z, Player player) {
        return false;
    }

    @Override
    public boolean shouldDisplayInTab() {
        return ConfigHandler.enableKami;
    }
}
