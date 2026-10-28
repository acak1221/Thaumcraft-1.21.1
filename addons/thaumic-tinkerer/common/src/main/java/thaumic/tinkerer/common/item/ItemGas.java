/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 22:12:14 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.BlockGas;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererCrucibleRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * A phial of gas. Two items share this class, as in 1.7.10: the Gaseous Tenebrae (the default item, which sets the
 * Gaseous Shadow) and the Gaseous Illuminae (the meta item, which sets the Gaseous Light).
 */
public class ItemGas extends Item implements ITTinkererRegisterable {

    private final boolean shadow;

    public ItemGas(boolean shadow, Properties properties) {
        super(properties);
        this.shadow = shadow;
    }

    /** The gas this phial releases (looked up by registry path, the blocks are registered next to the items). */
    public Block getGasBlock() {
        return TTRegistry.block(shadow ? LibBlockNames.GASEOUS_SHADOW : LibBlockNames.GASEOUS_LIGHT);
    }

    public boolean isShadow() {
        return shadow;
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        if (shadow) {
            return (TTResearchItem) new TTResearchItem(
                    LibResearch.KEY_GASEOUS_SHADOW,
                    new AspectList().add(Aspect.DARKNESS, 2).add(Aspect.AIR, 1).add(Aspect.MOTION, 4),
                    -1,
                    -5,
                    2,
                    new ItemStack(this)).setSecondary().setParents(LibResearch.KEY_GASEOUS_LIGHT).setPages(
                            new ResearchPage("0"),
                            ResearchHelper.crucibleRecipePage(LibResearch.KEY_GASEOUS_SHADOW));
        }
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_GASEOUS_LIGHT,
                new AspectList().add(Aspect.LIGHT, 2).add(Aspect.AIR, 1),
                0,
                -3,
                1,
                new ItemStack(this)).setParents("NITOR").setPages(
                        new ResearchPage("0"),
                        ResearchHelper.crucibleRecipePage(LibResearch.KEY_GASEOUS_LIGHT));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        // itemEssence:0, the empty phial
        ItemStack phial = ItemApi.getItem("itemEssence", 0);
        if (shadow) {
            return new ThaumicTinkererCrucibleRecipe(
                    LibResearch.KEY_GASEOUS_SHADOW,
                    new ItemStack(this),
                    phial,
                    new AspectList().add(Aspect.DARKNESS, 16).add(Aspect.AIR, 10).add(Aspect.MOTION, 8));
        }
        return new ThaumicTinkererCrucibleRecipe(
                LibResearch.KEY_GASEOUS_LIGHT,
                new ItemStack(this),
                phial,
                new AspectList().add(Aspect.LIGHT, 16).add(Aspect.AIR, 10).add(Aspect.MOTION, 8));
    }

    /**
     * 1.7.10 {@code onItemRightClick}: releases the gas one block above the player's feet (the phial is used up even
     * when there is no room, as before), with the pop of a picked-up item.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 1.7.10 cut the coordinates with (int); a floor is the same for positive values and right for negative ones
        BlockPos pos = BlockPos.containing(player.getX(), player.getY(), player.getZ()).above();
        boolean air = BlockGas.isAirLike(level.getBlockState(pos));

        if (!player.getAbilities().instabuild) stack.shrink(1);

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F,
                    0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        }

        if (air) {
            Block gas = getGasBlock();
            if (!level.isClientSide) {
                level.setBlock(pos, gas.defaultBlockState().setValue(BlockGas.LEVEL, BlockGas.RELEASE_LEVEL), Block.UPDATE_CLIENTS);
                level.scheduleTick(pos, gas, 10);
            } else {
                player.swing(hand);
            }
        }

        return InteractionResultHolder.consume(stack);
    }
}
