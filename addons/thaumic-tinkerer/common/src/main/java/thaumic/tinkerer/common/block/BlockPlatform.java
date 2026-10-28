/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Dec 27, 2013, 6:36:19 PM (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.tile.TileCamo;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Ethereal Platform: solid only for whoever stands above it. Players are solid above two blocks of it (their feet
 * at 0.38 above the bottom of the block, with the 1.62 eye height the 1.7.10 client compared) and while not sneaking;
 * every other entity is solid when its feet are above the bottom of the block. Everything else walks through.
 */
public class BlockPlatform extends BlockCamo implements ITTinkererRegisterable {
    public static final MapCodec<BlockPlatform> CODEC = simpleCodec(BlockPlatform::new);

    /** The eye height the 1.7.10 client player had in {@code posY}. */
    private static final double EYE_HEIGHT = 1.62D;

    public BlockPlatform(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Wood, hardness 2, resistance 5 in the 1.7.10 setter (3 now); no occlusion, it lets light and sight through. */
    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)
                .noOcclusion().ignitedByLava();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileCamo(pos, state);
    }

    /** 1.7.10 {@code addCollisionBoxesToList}: nothing without an entity, a full cube for those above, nothing else. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ec) {
            Entity entity = ec.getEntity();
            if (entity != null && solidFor(entity, pos)) return Shapes.block();
        }
        return Shapes.empty();
    }

    /** Whether the platform at {@code pos} is solid for the entity. */
    public static boolean solidFor(Entity entity, BlockPos pos) {
        boolean player = entity instanceof Player;
        double y = player ? entity.getY() + EYE_HEIGHT : entity.getY();
        return y > pos.getY() + (player ? 2 : 0) && (!player || !entity.isShiftKeyDown());
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_PLATFORM,
                new AspectList().add(Aspect.SENSES, 2).add(Aspect.TREE, 1).add(Aspect.MOTION, 1),
                -2,
                6,
                3,
                new ItemStack(this)).setConcealed().setParents(LibResearch.KEY_CLEANSING_TALISMAN)
                        .setPages(new ResearchPage("0"), ResearchHelper.arcaneRecipePage(LibResearch.KEY_PLATFORM))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_PLATFORM,
                LibResearch.KEY_PLATFORM,
                new ItemStack(this, 2),
                new AspectList().add(Aspect.AIR, 2).add(Aspect.ENTROPY, 4),
                " S ",
                "G G",
                'G',
                ItemApi.getBlock("blockWoodenDevice", 6),
                'S',
                ItemApi.getBlock("blockWoodenDevice", 7));
    }
}
