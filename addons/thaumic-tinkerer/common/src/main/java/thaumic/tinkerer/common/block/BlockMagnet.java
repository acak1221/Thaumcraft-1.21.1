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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.block.tile.TileMagnet;
import thaumic.tinkerer.common.block.tile.TileMobMagnet;
import thaumic.tinkerer.common.block.tile.container.ContainerMobMagnet;
import thaumic.tinkerer.common.content.Machines;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipeMulti;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Kinetic Attractor and the Corporeal Attractor. 1.7.10 made them one block with the metadata {@code bit 0} =
 * polarity and {@code bit 1} = mob magnet; here the two are blocks ({@code magnet}, {@code mob_magnet}) and the
 * polarity is the block state {@link #PULLING}.
 */
public class BlockMagnet extends BaseEntityBlock implements ITTinkererRegisterable {

    /** True: the magnet pulls (metadata bit 0 clear, the "blue" magnet); false: it pushes (the "red" one). */
    public static final BooleanProperty PULLING = BooleanProperty.create("pulling");

    public static final MapCodec<BlockMagnet> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(propertiesCodec(), Codec.BOOL.fieldOf("mob").forGetter(block -> block.mob))
                    .apply(instance, BlockMagnet::new));

    /** {@code setBlockBounds(0.0625F, 0F, 0.0625F, 0.9375F, 1F / 16F * 2F, 0.9375F)}. */
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 2, 15);

    private final boolean mob;

    public BlockMagnet(Properties properties, boolean mob) {
        super(properties);
        this.mob = mob;
        registerDefaultState(stateDefinition.any().setValue(PULLING, true));
    }

    public boolean isMobMagnet() {
        return mob;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PULLING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty() && WandHelper.isWand(stack)) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof TileMobMagnet mobMagnet && player instanceof ServerPlayer) {
                player.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(getDescriptionId());
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                        return new ContainerMobMagnet(id, inventory, mobMagnet);
                    }
                });
                level.playSound(null, pos, key(), SoundSource.BLOCKS, 1F, 0.5F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(PULLING), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1F, 0.5F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** {@code thaumcraft:key}, the sound of a wand opening a device. */
    private static SoundEvent key() {
        return SoundEvent.createVariableRangeEvent(ThaumcraftSoundIds.KEY);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TileMobMagnet mobMagnet) {
                MachineDrops.scatter(level, pos, mobMagnet);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mob ? new TileMobMagnet(pos, state) : new TileMagnet(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (mob) return createTickerHelper(type, Machines.TILE_MOB_MAGNET.get(), TileMagnet::tick);
        return createTickerHelper(type, Machines.TILE_MAGNET.get(), TileMagnet::tick);
    }

    // ---- research and recipes (carried by the Kinetic Attractor, as 1.7.10 carried them on the one block) -----

    @Override
    public IRegisterableResearch getResearchItem() {
        if (mob) return null;
        return (IRegisterableResearch) new TTResearchItem(LibResearch.KEY_MAGNETS,
                new AspectList().add(Aspect.MECHANISM, 2).add(Aspect.MOTION, 1).add(Aspect.SENSES, 1), -6, 3, 3,
                new ItemStack(this)).setParents(LibResearch.KEY_INTERFACE)
                        .setParentsHidden(LibResearch.KEY_FOCUS_TELEKINESIS).setConcealed().setPages(
                                new ResearchPage("0"), new ResearchPage("1"),
                                ResearchHelper.arcaneRecipePage(LibResearch.KEY_MAGNET),
                                ResearchHelper.arcaneRecipePage(LibResearch.KEY_MOB_MAGNET),
                                ResearchHelper.crucibleRecipePage(LibResearch.KEY_MAGNETS));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (mob) return null;
        return new ThaumicTinkererRecipeMulti(
                new ThaumicTinkererArcaneRecipe(LibResearch.KEY_MAGNET, LibResearch.KEY_MAGNETS, new ItemStack(this),
                        new AspectList().add(Aspect.AIR, 20).add(Aspect.ORDER, 5).add(Aspect.EARTH, 15)
                                .add(Aspect.ENTROPY, 5),
                        " I ", "SIs", "WFW", 'I', new ItemStack(Items.IRON_INGOT), 's',
                        ItemApi.getItem("itemShard", 3), 'S', ItemApi.getItem("itemShard", 0), 'W',
                        ItemApi.getBlock("blockMagicalLog", 0), 'F', TTRegistry.stack(LibItemNames.FOCUS_TELEKINESIS)),
                new ThaumicTinkererArcaneRecipe(LibResearch.KEY_MOB_MAGNET, LibResearch.KEY_MAGNETS,
                        TTRegistry.stack(LibBlockNames.MOB_MAGNET),
                        new AspectList().add(Aspect.AIR, 20).add(Aspect.ORDER, 5).add(Aspect.EARTH, 15)
                                .add(Aspect.ENTROPY, 5),
                        " G ", "SGs", "WFW",
                        // 1.7.10: RecipeHelper.oreDictOrStack(gold ingot, "ingotCopper"): the copper ingot when the ore
                        // dictionary has one, which is always true in 1.21.1 (vanilla copper).
                        'G', "ingotCopper", 's', ItemApi.getItem("itemShard", 3), 'S', ItemApi.getItem("itemShard", 0),
                        'W', ItemApi.getBlock("blockMagicalLog", 0), 'F', TTRegistry.stack(LibItemNames.FOCUS_TELEKINESIS)));
    }
}
