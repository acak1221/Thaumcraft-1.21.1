package thaumcraft.common.lib;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockArcaneFurnace;
import thaumcraft.common.blocks.BlockArcaneFurnaceNozzle;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/**
 * Original {@code WandManager.createArcaneFurnace} (oracle
 * {@code thaumcraft/common/items/wands/WandManager.java:332-407}), the wand
 * trigger of the original event two that turns a three by three by three shell
 * of nether bricks and obsidian into the arcane furnace.
 *
 * <p>The original registers the trigger in {@code ConfigRecipes.java:90-92}: the
 * nether brick block, the obsidian and the iron bars are the three blocks the
 * wand answers to, and the original dispatch of {@code WandManager.performTrigger}
 * (oracle {@code WandManager.java:718-720}) asks for the complete
 * {@code INFERNALFURNACE} research first.</p>
 *
 * <p>Every condition of the original is kept:</p>
 * <ul>
 *   <li>the original shell is a three by three by three cube: the four corners
 *       of every layer are nether bricks, the edges are obsidian, the middle of
 *       the bottom layer is obsidian, the middle of the middle layer is lava and
 *       the middle of the top layer is air ({@code WandManager.java:351-356});</li>
 *   <li>exactly one cell of the middle layer is iron bars, and it has to sit on
 *       one of the four cross slots of that layer ({@code WandManager.java:357-372});
 *       the original returns false when no iron bar was found, so the furnace
 *       cannot be built without them;</li>
 *   <li>the original searches the twenty seven candidate volumes that contain
 *       the clicked block and takes the first one that fits
 *       ({@code WandManager.java:334-344});</li>
 *   <li>the wand pays fifty vis of fire and fifty vis of earth
 *       ({@code WandManager.java:341}, the original {@code consumeAllVisCrafting}
 *       of {@code ItemWandCasting.java:306-338});</li>
 *   <li>the twenty six cells become arcane furnace blocks whose original
 *       metadata is the position of the cell in its layer, the lava becomes the
 *       furnace core and the iron bars become the output nozzle
 *       ({@code WandManager.java:369-397}), and the wand sound plays.</li>
 * </ul>
 *
 * <p>Forced 1.21.1 changes: the original answered the click through the block
 * metadata of the wand trigger registry, which no longer exists, and the
 * original metadata of the furnace became the {@code variant} block state of
 * {@link BlockArcaneFurnace} plus the separate half block
 * {@link BlockArcaneFurnaceNozzle} of the original metadata ten.</p>
 */
public final class ArcaneFurnaceFormation {
    private ArcaneFurnaceFormation() {}

    /**
     * Original WandManager.java:341 - fifty vis of fire and fifty vis of earth.
     *
     * <p>The original spends them with {@code consumeAllVisCrafting}, which
     * multiplies every amount by one hundred and then applies the consumption
     * modifier of the wand; the native pricing does exactly that.</p>
     */
    public static final Map<String, Integer> FORM_VIS = Map.of("ignis", 50, "terra", 50);

    /**
     * Original ConfigRecipes.java:91-92 - the three blocks the wand answers to.
     *
     * <p>The original nether brick block is the {@code minecraft:nether_bricks}
     * block of the port, the identity the bundled aspect corpus gives to the
     * original {@code Blocks.nether_brick}
     * ({@code data/thaumcraft/tc4/early_object_aspects.json}, the row of
     * {@code ConfigAspects.java:193}).</p>
     */
    public static boolean isTriggerBlock(BlockState state) {
        return state.is(Blocks.NETHER_BRICKS) || state.is(Blocks.OBSIDIAN) || state.is(Blocks.IRON_BARS);
    }

    /**
     * Original WandManager.java:351-356 - the block the original expects in one
     * cell of the shell.
     *
     * @param layer zero is the top layer, one the middle layer and two the bottom layer
     * @param dx the offset of the cell in the layer, zero to two
     * @param dz the offset of the cell in the layer, zero to two
     */
    public static Block expected(int layer, int dx, int dz) {
        if (dx == 1 && dz == 1) {
            return switch (layer) {
                // The original leaves the middle of the top layer empty.
                case 0 -> Blocks.AIR;
                // The original fills the middle of the middle layer with lava.
                case 1 -> Blocks.LAVA;
                default -> Blocks.OBSIDIAN;
            };
        }
        boolean corner = (dx == 0 || dx == 2) && (dz == 0 || dz == 2);
        return corner ? Blocks.NETHER_BRICKS : Blocks.OBSIDIAN;
    }

    /**
     * Original WandManager.java:357-372 - the shell of the original fits.
     *
     * <p>The original compares the block of every cell with the blueprint and
     * accepts one iron bar on a cross slot of the middle layer; the function
     * returns whether such an iron bar was found, so the original furnace can
     * never be built without it.</p>
     *
     * @param base the north west bottom corner of the three by three by three volume
     */
    public static boolean fits(Level level, BlockPos base) {
        boolean ironBarsFound = false;
        for (int layer = 0; layer < 3; layer++) {
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    BlockPos pos = base.offset(dx, 2 - layer, dz);
                    if (!level.hasChunkAt(pos)) return false;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) state = Blocks.AIR.defaultBlockState();
                    if (state.is(expected(layer, dx, dz))) continue;
                    if (layer == 1 && !ironBarsFound && state.is(Blocks.IRON_BARS)
                            && dx != dz && (dx == 1 || dz == 1)) {
                        ironBarsFound = true;
                        continue;
                    }
                    return false;
                }
            }
        }
        return ironBarsFound;
    }

    /**
     * The north west bottom corner of the volume that holds the clicked block,
     * or null when no volume of the original shape fits.
     *
     * <p>Original WandManager.java:334-344: the original walks the twenty seven
     * candidate corners from {@code x - 2} to {@code x} in every axis and takes
     * the first volume that fits.</p>
     */
    public static BlockPos candidate(Level level, BlockPos clicked) {
        for (int dx = -2; dx <= 0; dx++) {
            for (int dy = -2; dy <= 0; dy++) {
                for (int dz = -2; dz <= 0; dz++) {
                    BlockPos base = clicked.offset(dx, dy, dz);
                    if (fits(level, base)) return base;
                }
            }
        }
        return null;
    }

    /**
     * Original WandManager.java:369-397 - the twenty six cells of the shell
     * become arcane furnace blocks.
     *
     * <p>The original metadata of a cell is its position in its layer, the lava
     * becomes the metadata zero core, the iron bars become the metadata ten
     * output nozzle and the empty cell of the top layer is left alone.</p>
     */
    public static void replace(Level level, BlockPos base) {
        for (int layer = 0; layer < 3; layer++) {
            for (int dz = 0; dz < 3; dz++) {
                for (int dx = 0; dx < 3; dx++) {
                    BlockPos pos = base.offset(dx, layer, dz);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState current = level.getBlockState(pos);
                    if (current.isAir()) continue;
                    int metadata = 1 + dz * 3 + dx;
                    BlockState target;
                    if (current.is(Blocks.LAVA) || current.is(Blocks.FLOWING_LAVA)) {
                        target = core();
                    } else if (current.is(Blocks.IRON_BARS)) {
                        target = nozzle();
                    } else {
                        target = shell(metadata);
                    }
                    if (target == null) continue;
                    level.setBlock(pos, target, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    /** The core of the furnace: the original metadata zero block. */
    private static BlockState core() {
        return BlockArcaneFurnace.state(0);
    }

    /** A shell block: the original metadata one to nine. */
    private static BlockState shell(int metadata) {
        return BlockArcaneFurnace.state(metadata);
    }

    /** The output nozzle: the original metadata ten block. */
    private static BlockState nozzle() {
        return BlockArcaneFurnaceNozzle.state();
    }

    /**
     * The native wand click on the shell of the original arcane furnace.
     *
     * <p>Forced 1.21.1 change: the original answered the click through the block
     * metadata of the wand trigger registry, which no longer exists. The native
     * port answers the three blocks the original registered for the event two.</p>
     */
    public static InteractionResult tryForm(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (!(context.getItemInHand().getItem() instanceof ItemWandCasting)) return InteractionResult.PASS;
        if (!level.hasChunkAt(clicked) || !isTriggerBlock(level.getBlockState(clicked))) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.FAIL;
        // Original WandManager.java:719 - the INFERNALFURNACE research comes first.
        if (!KnowledgeAccess.get(player).isResearchComplete("INFERNALFURNACE")) return InteractionResult.PASS;
        BlockPos base = candidate(level, clicked);
        if (base == null) return InteractionResult.PASS;
        for (int layer = 0; layer < 3; layer++) {
            for (int dz = 0; dz < 3; dz++) {
                for (int dx = 0; dx < 3; dx++) {
                    BlockPos pos = base.offset(dx, layer, dz);
                    if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
                        return InteractionResult.FAIL;
                    }
                }
            }
        }
        ItemStack wand = context.getItemInHand();
        ItemStack before = wand.copy();
        Map<String, Integer> price = ArcaneVis.price(FORM_VIS, wand, player);
        ItemStack paid = wand.copy();
        if (!ArcaneVis.debit(paid, price)) return InteractionResult.FAIL;
        if (player.getItemInHand(context.getHand()) != wand || !ItemStack.matches(wand, before)) return InteractionResult.FAIL;
        replace(level, base);
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
        // Original WandManager.java:406 - the wand sound at the corner of the shell.
        for (int layer = 0; layer < 3; layer++) {
            for (int dz = 0; dz < 3; dz++) {
                for (int dx = 0; dx < 3; dx++) {
                    BlockPos pos = base.offset(dx, layer, dz);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof BlockArcaneFurnace) {
                        BlockArcaneFurnace.updateLevel(level, pos, state);
                    }
                    level.updateNeighborsAt(pos, state.getBlock());
                }
            }
        }
        level.playSound(null, base, SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand")), SoundSource.BLOCKS, 1, 1);
        return InteractionResult.CONSUME;
    }
}
