/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 3:54:41 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.kami;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;
import thaumic.tinkerer.common.content.KamiWorld;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.KamiResearchItem;
import thaumic.tinkerer.common.research.ResearchHelper;

/**
 * The Celestial Gateway. Right-click it to open its ten pearl slots and the "allow incoming teleports" button; stand on
 * it and sneak to open the map of its destinations. Pearls are attuned to a gateway by right-clicking the gateway with
 * them (the pearl's own code, in the KamiGear group).
 *
 * <p>1.7.10 drew it with a block renderer handler (a cube whose top face has a second, full-brightness layer); that is
 * the JSON model {@code block/warp_gate} now (the glowing frame is an emissive face), and the cubes that circle over it
 * are the block entity renderer of the client registration.</p>
 */
public class BlockWarpGate extends BaseEntityBlock implements ITTinkererRegisterable {
    public static final MapCodec<BlockWarpGate> CODEC = simpleCodec(BlockWarpGate::new);

    public BlockWarpGate() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(5.0F, 2000.0F)
                .requiresCorrectToolForDrops().noOcclusion());
    }

    public BlockWarpGate(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileWarpGate(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, KamiWorld.warpGateType(), TileWarpGate::tick);
    }

    /** 1.7.10 {@code onBlockActivated}: opens the gate's screen (for any hand, as before). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileWarpGate gate) {
            gate.syncToClients();
            MenuRegistry.openExtendedMenu(serverPlayer, gate);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        // 1.7.10 returned true for every item; the Celestial Pearl's own click ("attune") runs before this in the item.
        InteractionResult result = useWithoutItem(state, level, pos, player, hit);
        return result.consumesAction() ? net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide)
                : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 1.7.10 {@code breakBlock}: the pearls fall out in stacks, as from a chest. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TileWarpGate gate) {
            Containers.dropContents(level, pos, gate);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        if (!ThaumcraftApi.isMirrorsAllowed()) return null;
        if (!ConfigHandler.enableKami) return null;
        KamiResearchItem research = new KamiResearchItem(LibResearch.KEY_WARP_GATE,
                new AspectList().add(Aspect.TRAVEL, 2).add(Aspect.ELDRITCH, 1).add(Aspect.FLIGHT, 1)
                        .add(Aspect.MECHANISM, 1),
                19, 6, 5, new ItemStack(this));
        research.setParents(LibResearch.KEY_ICHORCLOTH_CHEST_GEM);
        research.setParentsHidden(LibResearch.KEY_ICHORCLOTH_BOOTS_GEM);
        research.setPages(new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_WARP_GATE),
                new ResearchPage("1"), ResearchHelper.infusionPage(LibResearch.KEY_SKY_PEARL));
        return research;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (!ThaumcraftApi.isMirrorsAllowed()) return null;
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_WARP_GATE, new ItemStack(this), 8,
                new AspectList().add(Aspect.TRAVEL, 64).add(Aspect.ELDRITCH, 50).add(Aspect.FLIGHT, 50),
                // ConfigBlocks.blockCosmeticSolid, 2: the paving stone of travel.
                Objects.requireNonNull(ItemApi.getBlock("blockCosmeticSolid", 2), "Thaumcraft block blockCosmeticSolid:2"),
                // ItemKamiResource 0 (Ichor), 7 (Ender Shard), the Transvector Dislocator, 6 (Nether Shard).
                TTRegistry.stack(LibItemNames.KAMI_RESOURCE_NAMES[0]),
                TTRegistry.stack(LibItemNames.KAMI_RESOURCE_NAMES[7]), TTRegistry.stack(LibBlockNames.DISLOCATOR),
                TTRegistry.stack(LibItemNames.KAMI_RESOURCE_NAMES[6]), new ItemStack(Items.DIAMOND),
                new ItemStack(Items.FEATHER));
    }
}
