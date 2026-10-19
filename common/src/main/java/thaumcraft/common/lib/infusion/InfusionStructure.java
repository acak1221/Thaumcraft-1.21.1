package thaumcraft.common.lib.infusion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileInfusionPillar;
import thaumcraft.common.tiles.TilePedestal;

/** The original altar blueprint, distinct from the much looser already-formed validity check. */
public final class InfusionStructure {
    private static final int QUIET = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    // WandManager:181's lower corners, retaining the original orientation numbers.
    private static final int[][] CORNERS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    private InfusionStructure() {}

    public record Surroundings(List<BlockPos> pedestals, int symmetry) {
        public Surroundings { pedestals = pedestals.stream().map(BlockPos::immutable).toList(); }
    }

    /** TileInfusionMatrix:230-250: center pedestal plus four LOWER pillar BEs, not an air blueprint. */
    public static boolean isValid(Level level, BlockPos matrix) {
        BlockPos center = matrix.below(2);
        if (!loaded(level, center) || !(level.getBlockEntity(center) instanceof TilePedestal)) return false;
        for (int[] corner : CORNERS) {
            BlockPos pos = center.offset(corner[0], 0, corner[1]);
            if (!loaded(level, pos) || !(level.getBlockEntity(pos) instanceof TileInfusionPillar)) return false;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BlockInfusionPillar) || BlockInfusionPillar.getHalf(state) != DoubleBlockHalf.LOWER) return false;
        }
        return true;
    }

    /** TileInfusionMatrix:609-668. No chunk generation, nearest sorting, or item-type symmetry test. */
    public static Surroundings scan(ServerLevel level, BlockPos matrix) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Infusion scan requires the server thread");
        List<BlockPos> pedestals = new ArrayList<>();
        List<BlockPos> stabilisers = new ArrayList<>();
        for (int dx = -12; dx <= 12; ++dx) {
            for (int dz = -12; dz <= 12; ++dz) {
                if (dx == 0 && dz == 0) continue; // The ENTIRE central XZ column is excluded.
                boolean foundPedestal = false;
                for (int yy = -5; yy <= 10; ++yy) {
                    BlockPos pos = matrix.offset(dx, -yy, dz);
                    if (!loaded(level, pos)) continue;
                    BlockEntity tile = level.getBlockEntity(pos);
                    if (!foundPedestal && yy > 0 && Math.abs(dx) <= 8 && Math.abs(dz) <= 8 && tile instanceof TilePedestal) {
                        pedestals.add(pos);
                        foundPedestal = true;
                        continue;
                    }
                    // Finding a pedestal does NOT end the stabiliser scan in this column.
                    if (stabilises(level, level.getBlockState(pos).getBlock(), pos)) stabilisers.add(pos);
                }
            }
        }

        int pedestalSymmetry = 0;
        for (BlockPos pos : pedestals) {
            boolean occupied = false;
            if (loaded(level, pos) && level.getBlockEntity(pos) instanceof TilePedestal pedestal) {
                pedestalSymmetry += 2;
                if (!pedestal.isEmpty()) {
                    ++pedestalSymmetry;
                    occupied = true;
                }
            }
            BlockPos opposite = opposite(matrix, pos);
            // The opposite need not be the topmost/scanned pedestal in its column.
            if (loaded(level, opposite) && level.getBlockEntity(opposite) instanceof TilePedestal pedestal) {
                pedestalSymmetry -= 2;
                if (occupied && !pedestal.isEmpty()) --pedestalSymmetry;
            }
        }

        float stabiliserSymmetry = 0;
        for (BlockPos pos : stabilisers) {
            if (loaded(level, pos) && stabilises(level, level.getBlockState(pos).getBlock(), pos)) stabiliserSymmetry += .1F;
            BlockPos opposite = opposite(matrix, pos);
            // Deliberate original line665 quirk: inspect the opposite block but
            // call ITS hook with the ORIGINAL coordinates, not opposite's.
            if (loaded(level, opposite) && stabilises(level, level.getBlockState(opposite).getBlock(), pos)) stabiliserSymmetry -= .2F;
        }
        return new Surroundings(pedestals, (int) (pedestalSymmetry + stabiliserSymmetry));
    }

    private static BlockPos opposite(BlockPos matrix, BlockPos pos) {
        return new BlockPos(matrix.getX() + (matrix.getX() - pos.getX()), pos.getY(), matrix.getZ() + (matrix.getZ() - pos.getZ()));
    }

    private static boolean stabilises(Level level, Block block, BlockPos callbackPos) {
        // AbstractSkullBlock includes native standing AND wall skull variants.
        return block instanceof AbstractSkullBlock
                || block instanceof IInfusionStabiliser stabiliser && stabiliser.canStabaliseInfusion(level, callbackPos);
    }

    private static boolean loaded(Level level, BlockPos pos) {
        return level instanceof ServerLevel server ? FluxFiniteBlock.loaded(server, pos)
                : !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos);
    }

    /** Called by the native wand before focus use. Only Matrix (ConfigRecipes:94) is a formation trigger. */
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        ItemStack held = context.getItemInHand();
        if (player == null || player.isSpectator() || !player.isAlive() || held.isEmpty()
                || !(held.getItem() instanceof ItemWandCasting) || !loaded(level, pos)
                || !level.getBlockState(pos).is(ThaumcraftInfusion.MATRIX.get())
                || !(level.getBlockEntity(pos) instanceof TileInfusionMatrix matrix)) return InteractionResult.PASS;
        if (!player.canInteractWithBlock(pos, 0) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS; // Prediction never prices/debits/mutates.
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Infusion interaction requires the server thread");
        if (!(player instanceof ServerPlayer actor) || !current(context, server, matrix, held)) return InteractionResult.FAIL;

        // Matrix:675-687 has no research/vis payment for already-formed use.
        // Inactive crafting is a suspended session, not a request to roll a new recipe.
        if (!matrix.isActive() && isValid(server, pos)) {
            matrix.setActive(true);
            return InteractionResult.CONSUME;
        }
        if (matrix.isActive()) {
            if (!matrix.isCrafting()) matrix.startCrafting(actor);
            return InteractionResult.CONSUME; // Never cancel/reroll/charge a live craft.
        }
        if (!KnowledgeAccess.get(actor).isResearchComplete("INFUSION") || !ArcaneVis.isCraftingWand(held)) return InteractionResult.FAIL;

        List<Cell> blueprint = blueprint(server, pos, matrix);
        if (blueprint == null) return InteractionResult.FAIL;
        ItemStack beforeWand = held.copy();
        boolean wasCrafting = matrix.isCrafting();
        for (Cell cell : blueprint) {
            if (cell.after != null && (!server.mayInteract(actor, cell.pos)
                    || !actor.mayUseItemAt(cell.pos, context.getClickedFace(), held))) return InteractionResult.FAIL;
        }
        Map<String, Integer> price = ArcaneVis.price(formationVisCost(), held, actor);
        ItemStack paid = held.copy();
        if (!ArcaneVis.debit(paid, price) || !current(context, server, matrix, held)
                || !ItemStack.matches(beforeWand, held) || matrix.isActive() || matrix.isCrafting() != wasCrafting
                || !KnowledgeAccess.get(actor).isResearchComplete("INFUSION") || !unchanged(server, blueprint)) return InteractionResult.FAIL;

        List<Cell> installed = new ArrayList<>(8);
        Map<BlockPos, BlockEntity> installedEntities = new LinkedHashMap<>();
        for (Cell cell : blueprint) {
            if (cell.after == null) continue;
            // No UPDATE_CLIENTS/UPDATE_NEIGHBORS, and KNOWN_SHAPE suppresses
            // shape callbacks. The eight known plain-stone -> owned-pillar writes
            // all finish before any callback-bearing neighbor/client notification.
            if (!server.setBlock(cell.pos, cell.after, QUIET)) {
                rollback(server, installed, installedEntities);
                return InteractionResult.FAIL;
            }
            installed.add(cell);
            installedEntities.put(cell.pos, server.getBlockEntity(cell.pos));
        }
        boolean complete = current(context, server, matrix, held) && ItemStack.matches(beforeWand, held)
                && !matrix.isActive() && matrix.isCrafting() == wasCrafting && isValid(server, pos);
        for (Cell cell : blueprint) {
            BlockState expected = cell.after == null ? cell.before : cell.after;
            if (!loaded(server, cell.pos) || server.getBlockState(cell.pos) != expected
                    || server.getBlockEntity(cell.pos) != (cell.after == null ? cell.entity : installedEntities.get(cell.pos))) complete = false;
        }
        if (!complete) {
            rollback(server, installed, installedEntities);
            return InteractionResult.FAIL;
        }

        // The actual native hand stack remains the same object. One data write
        // pays all six discounted primals, BEFORE matrix/neighbor notifications.
        held.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
        matrix.setActive(true);
        actor.getInventory().setChanged();
        actor.containerMenu.broadcastChanges();
        for (Cell cell : installed) {
            BlockState now = server.getBlockState(cell.pos);
            server.sendBlockUpdated(cell.pos, cell.before, now, Block.UPDATE_CLIENTS);
            server.updateNeighborsAt(cell.pos, now.getBlock());
            now.updateNeighbourShapes(server, cell.pos, Block.UPDATE_ALL);
        }
        BlockPos minimum = pos.offset(-1, -2, -1);
        server.playSound(null, minimum.getX() + .5, minimum.getY() + .5, minimum.getZ() + .5,
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand")), SoundSource.BLOCKS, 1, 1);
        return InteractionResult.CONSUME;
    }

    /** The same original ConfigRecipes:93 cost is shown by the bounded formation book view. */
    public static Map<String, Integer> formationVisCost() {
        Map<String, Integer> vis = new LinkedHashMap<>();
        for (String primal : WandData.PRIMALS) vis.put(primal, 25);
        return java.util.Collections.unmodifiableMap(vis);
    }

    private static boolean current(UseOnContext context, ServerLevel level, TileInfusionMatrix matrix, ItemStack held) {
        Player player = context.getPlayer();
        return player != null && player.level() == level && player.isAlive() && !player.isSpectator()
                && player.getItemInHand(context.getHand()) == held && !held.isEmpty()
                && held.getItem() instanceof ItemWandCasting && player.canInteractWithBlock(context.getClickedPos(), 0)
                && loaded(level, context.getClickedPos()) && !matrix.isRemoved()
                && level.getBlockState(context.getClickedPos()).is(ThaumcraftInfusion.MATRIX.get())
                && level.getBlockEntity(context.getClickedPos()) == matrix;
    }

    private record Cell(BlockPos pos, BlockState before, BlockEntity entity, BlockState after) {}

    private static List<Cell> blueprint(ServerLevel level, BlockPos matrixPos, TileInfusionMatrix matrix) {
        List<Cell> cells = new ArrayList<>(27);
        for (int depth = 0; depth <= 2; ++depth) {
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    BlockPos pos = matrixPos.offset(dx, -depth, dz);
                    if (!loaded(level, pos)) return null;
                    BlockState before = level.getBlockState(pos);
                    BlockEntity entity = level.getBlockEntity(pos);
                    BlockState after = null;
                    if (dx == 0 && dz == 0 && depth == 0) {
                        if (!before.is(ThaumcraftInfusion.MATRIX.get()) || entity != matrix) return null;
                    } else if (dx == 0 && dz == 0 && depth == 2) {
                        if (!(entity instanceof TilePedestal)) return null;
                    } else if (depth > 0 && dx != 0 && dz != 0) {
                        if (entity != null || !before.is(depth == 1 ? ThaumcraftMasonry.ARCANE_STONE.get()
                                : ThaumcraftMasonry.ARCANE_STONE_BRICKS.get())) return null;
                        Direction facing = dx < 0 ? (dz < 0 ? Direction.NORTH : Direction.SOUTH)
                                : (dz < 0 ? Direction.WEST : Direction.EAST);
                        after = ThaumcraftInfusion.PILLAR.get().defaultBlockState()
                                .setValue(BlockInfusionPillar.HALF, depth == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER)
                                .setValue(BlockInfusionPillar.FACING, facing);
                    } else if (!before.isAir() || entity != null) return null;
                    cells.add(new Cell(pos, before, entity, after));
                }
            }
        }
        return cells;
    }

    private static boolean unchanged(ServerLevel level, List<Cell> cells) {
        for (Cell cell : cells) if (!loaded(level, cell.pos) || level.getBlockState(cell.pos) != cell.before
                || level.getBlockEntity(cell.pos) != cell.entity) return false;
        return true;
    }

    private static void rollback(ServerLevel level, List<Cell> installed, Map<BlockPos, BlockEntity> entities) {
        for (Cell cell : installed) {
            // Do not erase any foreign replacement, even one with the same block state.
            if (loaded(level, cell.pos) && level.getBlockState(cell.pos) == cell.after
                    && level.getBlockEntity(cell.pos) == entities.get(cell.pos)) level.setBlock(cell.pos, cell.before, QUIET);
        }
    }
}
