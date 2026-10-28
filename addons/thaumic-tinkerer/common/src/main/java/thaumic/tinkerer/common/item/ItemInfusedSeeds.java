/**
 * Created by pixlepix on 4/14/14.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.block.BlockInfusedGrain;
import thaumic.tinkerer.common.block.tile.TileInfusedGrain;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipeMulti;
import thaumic.tinkerer.common.research.IRegisterableResearch;

/**
 * Infused Seeds. One item carries every aspect: the aspect and the tendencies of the seeds are data components
 * ({@link InfusedSeedComponents}), as the NBT of the 1.7.10 stack was. Right-clicking farmland plants the seeds and
 * turns the soil into infused farmland.
 *
 * <p>The creative tab lists the seeds of every primal aspect (1.7.10 {@code getSubItems}), not the bare item.</p>
 */
public class ItemInfusedSeeds extends Item implements ITTinkererRegisterable {
    /** The registry path of the block of the crop (the shared constant of the lead has a capital letter in it). */
    public static final String GRAIN_BLOCK_PATH = "infused_grain_block";

    public ItemInfusedSeeds(Properties properties) {
        super(properties);
    }

    /** The registered item. */
    public static Item item() {
        return TTRegistry.item(LibItemNames.INFUSED_SEEDS);
    }

    @Nullable
    public static Aspect getAspect(ItemStack stack) {
        String tag = stack.get(InfusedSeedComponents.aspectType());
        return tag == null ? null : Aspect.getAspect(tag);
    }

    public static void setAspect(ItemStack stack, @Nullable Aspect aspect) {
        if (aspect == null) stack.remove(InfusedSeedComponents.aspectType());
        else stack.set(InfusedSeedComponents.aspectType(), aspect.getTag());
    }

    public static AspectList getAspectTendencies(ItemStack stack) {
        return InfusedSeedComponents.toList(stack.get(InfusedSeedComponents.tendenciesType()));
    }

    public static void setAspectTendencies(ItemStack stack, AspectList aspectList) {
        stack.set(InfusedSeedComponents.tendenciesType(), InfusedSeedComponents.toMap(aspectList));
    }

    public static ItemStack getStackFromAspect(Aspect a) {
        ItemStack stack = new ItemStack(item());
        setAspect(stack, a);
        return stack;
    }

    /** The seeds the creative tab lists: one per primal aspect, without tendencies (1.7.10 {@code getSubItems}). */
    public static ItemStack creativeStack(Aspect primal) {
        ItemStack itemStack = new ItemStack(item(), 1);
        setAspect(itemStack, primal);
        setAspectTendencies(itemStack, new AspectList());
        return itemStack;
    }

    @Override
    public boolean shouldDisplayInTab() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Aspect aspect = getAspect(stack);
        if (aspect != null) tooltip.add(Component.literal(aspect.getName()));
        AspectList aspectList = getAspectTendencies(stack);
        for (Aspect a : aspectList.getAspects()) {
            if (a != null) tooltip.add(Component.literal(a.getName() + ": " + aspectList.getAmount(a)));
        }
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return null;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        // itemShard:0..5, the shards of aer, ignis, aqua, terra, ordo and perditio
        Aspect[] primals = { Aspect.AIR, Aspect.FIRE, Aspect.EARTH, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY };
        int[] shards = { 0, 1, 3, 2, 4, 5 };
        ThaumicTinkererRecipeMulti recipes = new ThaumicTinkererRecipeMulti();
        for (int i = 0; i < primals.length; i++) {
            ItemStack shard = ItemApi.getItem("itemShard", shards[i]);
            recipes.addRecipe(new ThaumicTinkererInfusionRecipe(
                    LibResearch.KEY_POTIONS + i,
                    LibResearch.KEY_POTIONS,
                    getStackFromAspect(primals[i]),
                    5,
                    new AspectList().add(Aspect.CROP, 32).add(Aspect.HARVEST, 32),
                    new ItemStack(Items.WHEAT_SEEDS),
                    shard.copy(),
                    shard.copy(),
                    shard.copy(),
                    shard.copy()));
        }
        return recipes;
    }

    /**
     * 1.7.10 {@code onItemUse}: on the top of farmland the soil becomes infused farmland (unless it is already) and
     * the crop is planted above it with the aspect and the tendencies of the stack.
     *
     * <p>Deviation: the crop is planted only into an empty or replaceable cell; 1.7.10 replaced whatever stood above
     * the farmland.</p>
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos above = pos.above();
        BlockState soil = world.getBlockState(pos);
        if (!(soil.getBlock() instanceof FarmBlock) || player == null || !player.mayUseItemAt(pos, Direction.UP, stack)
                || !player.mayUseItemAt(above, Direction.UP, stack)) {
            return InteractionResult.PASS;
        }
        BlockState there = world.getBlockState(above);
        if (!there.isAir() && !there.canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide) {
            Block infusedFarmland = TTRegistry.block(LibBlockNames.BLOCK_INFUSED_FARMLAND);
            if (!soil.is(infusedFarmland)) {
                world.setBlock(pos, infusedFarmland.defaultBlockState(), Block.UPDATE_ALL);
            }
            BlockInfusedGrain grain = (BlockInfusedGrain) TTRegistry.block(GRAIN_BLOCK_PATH);
            Aspect aspect = getAspect(stack);
            if (aspect == null) aspect = Aspect.AIR;
            world.setBlock(above, grain.stateFor(aspect), Block.UPDATE_ALL);
            BlockInfusedGrain.setAspect(world, above, aspect);
            if (world.getBlockEntity(above) instanceof TileInfusedGrain tile) {
                tile.primalTendencies = getAspectTendencies(stack);
                tile.setChanged();
                BlockState planted = world.getBlockState(above);
                world.sendBlockUpdated(above, planted, planted, Block.UPDATE_ALL);
            }
            stack.consume(1, player);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
