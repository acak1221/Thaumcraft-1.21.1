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
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.camo.CamoDeviceBlock;
import thaumic.tinkerer.common.block.tile.TileGolemConnector;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Golem Connector: a wooden cube (opaque, unlike the other camouflageable devices) that is bound to a golem. See
 * {@link TileGolemConnector} for what of the 1.7.10 block works here.
 *
 * <p>Opaque means two things here. The cube hides the faces of its neighbours while it is drawn as itself; while it is
 * camouflaged it does not ({@link CamoDeviceBlock#getOcclusionShape}), because it is then drawn by the block entity
 * renderer and may look like glass. And it blocks light in both states, as {@code isOpaqueCube() == true} did
 * ({@link #getLightBlock}).</p>
 */
public class BlockGolemConnector extends CamoDeviceBlock implements ITTinkererRegisterable {
    public static final MapCodec<BlockGolemConnector> CODEC = simpleCodec(BlockGolemConnector::new);

    public BlockGolemConnector() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instabreak());
    }

    protected BlockGolemConnector(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileGolemConnector(pos, state);
    }

    /**
     * Full opacity for light, camouflaged or not. The default derives it from the occlusion shape, which is empty while
     * the connector is camouflaged ({@link CamoDeviceBlock#getOcclusionShape}), and a camouflaged connector would then let
     * the light through; 1.7.10 gave the block one opacity (255, from {@code isOpaqueCube}) whatever it imitated.
     */
    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getMaxLightLevel();
    }

    /**
     * Says what the block can do in this version: it is bound to a golem, but the computer link that reads and commands the
     * golem (and the golem data of Thaumcraft's golems) is not available yet (docs/deviations/TileBlocks.md, 2.4).
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.thaumictinkerer.golem_connector").withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, TileBlocks.golemConnectorType(), TileGolemConnector::serverTick);
    }

    /**
     * The parent of the Golem Connector in the book. 1.7.10 registered this documentation entry in {@code ResearchHelper}
     * only when ComputerCraft was loaded (which was also the only case in which the connector existed); the connector is
     * registered here without a computer mod, so the entry has to exist for it. The Machines group registers the same
     * entry for the Aspectalyzer, and a second registration of the key is ignored by Thaumcraft.
     */
    private static void ensurePeripheralsEntry() {
        if (ResearchCategories.getResearch(LibResearch.KEY_PERIPHERALS) != null) return;
        new TTResearchItem(LibResearch.KEY_PERIPHERALS, new AspectList(), -1, 0, 0, new ItemStack(Items.REDSTONE))
                .setAutoUnlock().setRound().setPages(new ResearchPage("0")).registerResearchItem();
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        ensurePeripheralsEntry();
        return (IRegisterableResearch) new TTResearchItem(
                LibResearch.KEY_GOLEMCONNECTOR,
                new AspectList().add(Aspect.ORDER, 1).add(Aspect.TRAVEL, 2).add(Aspect.TOOL, 1),
                1,
                0,
                0,
                new ItemStack(this)).setParents(LibResearch.KEY_PERIPHERALS).setParentsHidden("GOLEMBELL")
                        .setConcealed().setRound().setPages(
                                new ResearchPage("0"),
                                ResearchHelper.arcaneRecipePage(LibResearch.KEY_GOLEMCONNECTOR),
                                new ResearchPage("1"),
                                ResearchHelper.arcaneRecipePage(LibResearch.KEY_INTERFACE + "1"),
                                new ResearchPage("2"),
                                new ResearchPage("3"));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_GOLEMCONNECTOR,
                LibResearch.KEY_GOLEMCONNECTOR,
                new ItemStack(this),
                new AspectList().add(Aspect.AIR, 20).add(Aspect.ORDER, 5).add(Aspect.ENTROPY, 15),
                "WFW",
                "sIs",
                "WFW",
                'I',
                TileBlocks.legacyItem("itemGolemBell", 0),
                's',
                new ItemStack(Items.ENDER_PEARL),
                'W',
                TileBlocks.legacyBlock("blockMagicalLog", 0),
                'F',
                new ItemStack(Blocks.REDSTONE_BLOCK));
    }
}
