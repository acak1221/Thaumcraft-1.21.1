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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.content.Enchantments;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Osmotic Enchanter. 1.7.10: {@code Material.rock}, hardness 5, resistance 2000 (the same as the vanilla enchanting
 * table, 1200 in 1.21.1), a box of 0.75 blocks (12 pixels) like the enchanting table.
 */
public class BlockEnchanter extends BaseEntityBlock implements ITTinkererRegisterable {

    public static final MapCodec<BlockEnchanter> CODEC = simpleCodec(BlockEnchanter::new);

    /** {@code setBlockBounds(0, 0, 0, 1, 0.75, 1)}. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public BlockEnchanter(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** {@code renderAsNormalBlock() == false}: the light passes through the free quarter above the table. */
    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** {@code onBlockActivated}: opens the screen, with or without an item in the hand. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileEnchanter enchanter) {
            enchanter.sync();
            MenuRegistry.openExtendedMenu(serverPlayer, enchanter);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        useWithoutItem(state, level, pos, player, hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** {@code breakBlock}: the tool and the wand fall out of the table. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEnchanter(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Enchantments.enchanterType(), TileEnchanter::serverTick);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (IRegisterableResearch) new TTResearchItem(LibResearch.KEY_ENCHANTER,
                new AspectList().add(Aspect.MAGIC, 2).add(Aspect.AURA, 1).add(Aspect.ELDRITCH, 1).add(Aspect.DARKNESS, 1)
                        .add(Aspect.MIND, 1),
                5, 4, 5, new ItemStack(this)).setParents(LibResearch.KEY_SPELL_CLOTH).setPages(new ResearchPage("0"),
                        new ResearchPage("1"), new ResearchPage("2"), ResearchHelper.infusionPage(LibResearch.KEY_ENCHANTER));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        // 5 Obsidian Tiles (blockCosmeticSolid:1), 2 Thaumium Ingots (itemResource:2) and the Spellbinding Cloth around the
        // vanilla enchanting table.
        ItemStack obsidianTile = ItemApi.getBlock("blockCosmeticSolid", 1);
        ItemStack thaumiumIngot = ItemApi.getItem("itemResource", 2);
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ENCHANTER, new ItemStack(this), 15,
                new AspectList().add(Aspect.MAGIC, 50).add(Aspect.ENERGY, 20).add(Aspect.ELDRITCH, 20)
                        .add(Aspect.VOID, 20).add(Aspect.MIND, 10),
                new ItemStack(Items.ENCHANTING_TABLE), obsidianTile.copy(), obsidianTile.copy(), obsidianTile.copy(),
                obsidianTile.copy(), obsidianTile.copy(), thaumiumIngot.copy(), thaumiumIngot.copy(),
                TTRegistry.stack(LibItemNames.SPELL_CLOTH));
    }
}
