/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 00:02:52 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.BlockGas;
import thaumic.tinkerer.common.core.helper.SimpleBlockFx;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/** The Fume Dissipator: sneak-use dissolves every gas in a six-block cube around the player. */
public class ItemGasRemover extends Item implements ITTinkererRegisterable {

    public ItemGasRemover(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_GAS_REMOVER,
                new AspectList().add(Aspect.DARKNESS, 2).add(Aspect.LIGHT, 2),
                -2,
                -7,
                0,
                new ItemStack(this)).setRound()
                        .setPages(new ResearchPage("0"), ResearchHelper.arcaneRecipePage(LibResearch.KEY_GAS_REMOVER))
                        .setParents(LibResearch.KEY_GASEOUS_SHADOW);
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        // gasTypes[0] is the Gaseous Tenebrae, gasTypes[1] the Gaseous Illuminae (the item array of the 1.7.10 registry)
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_GAS_REMOVER,
                LibResearch.KEY_GAS_REMOVER,
                new ItemStack(this),
                new AspectList().add(Aspect.AIR, 2).add(Aspect.ORDER, 2),
                "DDD",
                "T G",
                "QQQ",
                'D',
                new ItemStack(TTRegistry.item(LibItemNames.DARK_QUARTZ)),
                'T',
                new ItemStack(TTRegistry.item(LibItemNames.GASEOUS_SHADOW)),
                'G',
                new ItemStack(TTRegistry.item(LibItemNames.GASEOUS_LIGHT)),
                'Q',
                new ItemStack(Items.QUARTZ));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            // the cube xs-3 .. xs+2 (the 1.7.10 loops were "< xs + 3")
            BlockPos origin = BlockPos.containing(player.getX(), player.getY(), player.getZ());
            if (!level.isClientSide) {
                for (int x = origin.getX() - 3; x < origin.getX() + 3; x++) {
                    for (int y = origin.getY() - 3; y < origin.getY() + 3; y++) {
                        for (int z = origin.getZ() - 3; z < origin.getZ() + 3; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            BlockState state = level.getBlockState(pos);
                            if (state.getBlock() instanceof BlockGas gas) {
                                gas.placeParticle(level, pos);
                                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                                        Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
                            }
                        }
                    }
                }
                SimpleBlockFx.wandSound(level, player.getX(), player.getY(), player.getZ(), 0.2F, 1F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }
}
