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
package thaumic.tinkerer.common.block;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.IWandable;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.block.camo.PulseDeviceBlock;
import thaumic.tinkerer.common.block.tile.TileRPlacer;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Remote Placer. Right-click it with a block to camouflage it, with a wand to make it face the side you clicked, and
 * sneak-click it with a wand to open its slot and the distance buttons; a redstone pulse places the block one to four
 * blocks away.
 *
 * <p>The pinned 1.7.10 fork does not register this block ({@code shouldRegister() == false}); it is registered here as
 * the port was asked to (see docs/deviations/TileBlocks.md).</p>
 */
public class BlockRPlacer extends PulseDeviceBlock implements IWandable, ITTinkererRegisterable {
    public static final MapCodec<BlockRPlacer> CODEC = simpleCodec(BlockRPlacer::new);

    public BlockRPlacer() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instabreak().requiresCorrectToolForDrops().noOcclusion());
    }

    protected BlockRPlacer(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileRPlacer(pos, state);
    }

    /** {@code canConnectRedstone}: redstone dust connects to the placer. */
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** The block in the slot is not lost when the placer is broken (the 1.7.10 block let it vanish). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** A wand click while sneaking opens the placer's menu; the plain click is the block's (it turns the placer). */
    @Override
    public int onWandRightClick(Level level, ItemStack stack, Player player, int x, int y, int z, int side, int event) {
        if (!player.isShiftKeyDown()) return -1;
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileRPlacer placer) {
            MenuRegistry.openExtendedMenu(serverPlayer, placer);
        }
        return 1;
    }

    @Override
    public ItemStack onWandRightClick(Level level, ItemStack stack, Player player) {
        return stack;
    }

    @Override
    public void onUsingWandTick(ItemStack stack, Player player, int ticks) {}

    @Override
    public void onWandStoppedUsing(ItemStack stack, Level level, Player player, int ticks) {}

    @Override
    public IRegisterableResearch getResearchItem() {
        return (IRegisterableResearch) new TTResearchItem(
                LibResearch.KEY_REMOTE_PLACER,
                new AspectList().add(Aspect.MECHANISM, 2).add(Aspect.MOTION, 1).add(Aspect.SENSES, 1),
                // 1.7.10 put it at (-6, 3), the cell of Magnets: the pinned fork never registered the block, so the clash
                // never showed. It sits below its parent, the Animation Tablet at (-8, 2), instead (docs/deviations/TileBlocks.md).
                -8,
                3,
                3,
                new ItemStack(this)).setParents(LibResearch.KEY_ANIMATION_TABLET).setConcealed().setPages(
                        new ResearchPage("0"),
                        new ResearchPage("1"),
                        ResearchHelper.arcaneRecipePage(LibResearch.KEY_REMOTE_PLACER));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_REMOTE_PLACER,
                LibResearch.KEY_REMOTE_PLACER,
                new ItemStack(this),
                new AspectList().add(Aspect.AIR, 20).add(Aspect.ORDER, 5).add(Aspect.EARTH, 15).add(Aspect.ENTROPY, 5),
                "ses",
                "sds",
                "sss",
                's',
                // ConfigBlocks.blockStoneDevice without a metadata: any stone device.
                TileBlocks.anyOfLegacyBlocks("blockStoneDevice"),
                'e',
                new ItemStack(Items.ENDER_PEARL),
                'd',
                new ItemStack(Blocks.DISPENSER));
    }
}
