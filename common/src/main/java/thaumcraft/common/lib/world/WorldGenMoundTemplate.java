package thaumcraft.common.lib.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * The nineteen by nineteen by sixteen block template of the TC4 mound.
 *
 * <p>The template is the literal content of the original
 * {@code thaumcraft/common/lib/world/WorldGenMound.java:63-2549}: the original writes every
 * one of its 2441 blocks with an explicit {@code setBlock} call, so the structure is a fixed
 * shape and not a procedural one. The table below holds that shape, layer by layer, with one
 * character per cell; the character is read in the order of the original, from the low x to
 * the high x and from the low z to the high z of a layer.
 *
 * <p>The table was taken from the oracle and not from the 1.12 reference: the two were
 * compared cell by cell and are identical, all sixteen layers, all 2441 cells.
 * {@code scripts/porting/extract_mound_template.py} rebuilds the table from the oracle and
 * prints its checksum, so the claim can be reproduced.
 *
 * <p>The blocks of the original and their 1.7.10 metadata:
 * <ul>
 *   <li>{@code A} the air of the rooms of the mound,</li>
 *   <li>{@code C} the cobblestone of the walls,</li>
 *   <li>{@code D} the dirt of the shell,</li>
 *   <li>{@code G} the grass of the shell,</li>
 *   <li>{@code I} the iron bars of the windows,</li>
 *   <li>{@code M} the mossy cobblestone of the walls,</li>
 *   <li>{@code T} the tall grass of the shell, metadata one,</li>
 *   <li>{@code B} the chiselled stone bricks, metadata three,</li>
 *   <li>{@code L} the ladder, metadata five,</li>
 *   <li>{@code 0} to {@code 7} the stone stairs of metadata zero to seven.</li>
 * </ul>
 */
public final class WorldGenMoundTemplate {
    /** The width and the depth of the mound in blocks. */
    public static final int SIZE = 19;
    /** The height of the mound in blocks. */
    public static final int LAYERS = 16;

    /**
     * The sixteen layers of the mound. Every layer holds the nineteen rows of the layer,
     * every row the nineteen cells of the row.
     */
    private static final String[][] TEMPLATE = {
        // layer 0
        {
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "......CCCCCCC......",
                "......CMCCMCC......",
                "......CCCMMCC......",
                "......CCCCCCC......",
                "......CMCMCMC......",
                "......CMCMMCC......",
                "......CCCCCCC......",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
        },
        // layer 1
        {
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "......MMCMCCM......",
                "......CAA.ACC......",
                "......CAAAACC......",
                "......MLAA.CM......",
                "......MAAAACC......",
                "......CAA.ACM......",
                "......CCMMCCM......",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
        },
        // layer 2
        {
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "......CCCCCCC......",
                "......CAAAACC......",
                "......CAAAACC......",
                "......CLAAACC......",
                "......CAAAAMC......",
                "......CAAAACC......",
                "......CCCCCCC......",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
        },
        // layer 3
        {
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "......CCCCCCC......",
                "......CAAAACC......",
                "......MAAAACC......",
                "......CLAAAMC......",
                "......CAAAACC......",
                "......CAAAACC......",
                "......CCCMCCC......",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
        },
        // layer 4
        {
                "...................",
                "...................",
                "...................",
                "...CCCCCCCCCCCCC...",
                "...CCCCCCCMCCCCC...",
                "...CCCCCMCCCCCCC...",
                "...CCCC3333CCCCC...",
                "...CCC1AAAA0CCCC...",
                "...CCC1AAAA0MMCC...",
                "...CCCCLAAA0CCCC...",
                "...CCM1AAAA0CCCC...",
                "...CCC1AAAA0CCCC...",
                "...CCCC2222CCCCC...",
                "...CCMCCCCCCMCCC...",
                "...CCCCCCCCCCCMC...",
                "...CCCCCCCCCCCCC...",
                "...................",
                "...................",
                "...................",
        },
        // layer 5
        {
                "...................",
                "...................",
                "...................",
                "...CCCCCCCCCCCCC...",
                "...C.AAAAAAAAACC...",
                "...CAAAAAAAAAACC...",
                "...CAAMAAAACAACC...",
                "...MAAAAAAAAAACC...",
                "...MAAAAAAAAAA3C...",
                "...CAAAAAAAAAAAC...",
                "...CAAAAAAAAAAAC...",
                "...CAAAAAAAAAAAC...",
                "...CAACAAAACAAAC...",
                "...CAAAAAAAAAAAC...",
                "...C.AAAAAAAAAAC...",
                "...CCCMCCCCCCCCC...",
                "...................",
                "...................",
                "...................",
        },
        // layer 6
        {
                "...................",
                "...................",
                "...................",
                "...CCMCCMCCMCCCC...",
                "...CAAAAAAAAAACC...",
                "...CAAAAAAAAAAMC...",
                "...CAAMAAAAMAACC...",
                "...CAAAAAAAAAA3C...",
                "...CAAAAAAAAAAAC...",
                "...CAAAAAAAAAAAC...",
                "...CAAAAAAAAAAAM...",
                "...CAAAAAAAAAAAC...",
                "...CAACAAAACAAAC...",
                "...MAAAAAAAAAAAC...",
                "...CAAAAAAAAAAAC...",
                "...CCCCMCCMCCCMC...",
                "...................",
                "...................",
                "...................",
        },
        // layer 7
        {
                "...................",
                "...................",
                "...................",
                "...CMCCCCCCCCCCC...",
                "...CAAAAAAAAAACC...",
                "...CAAAAAAAAAACC...",
                "...CAACAAAACAA3C...",
                "...CAAAAAAAAAAAC...",
                "...CAAAAAAAAAAAC...",
                "...CAAAAAAAAAAAC...",
                "...CAAAAAAAAAAAC...",
                "...MAAAAAAAAAAAM...",
                "...CAACAAAAMAAAC...",
                "...CAAAAAAAAAAAC...",
                "...CAAAAAAAAAAAC...",
                "...CCCCCCCCCCCCC...",
                "...................",
                "...................",
                "...................",
        },
        // layer 8
        {
                "....DDDDDDDDDDD....",
                "...DDDDDDDDDDDDD...",
                "..DDDDDDDDDDDDDDD..",
                ".DDCCCCCCCCCCCCCDD.",
                "DDDCMMCCCCMCMCCCDDD",
                "DDDCMCCCMCCCCC3CDDD",
                "DDDCCCC7777CCCACDDD",
                "DDCMCC5AAAA4MCACDDD",
                "CCCCCC5AAAA4MCACDDD",
                "CCMCMC5AAAA4CCACDDD",
                "CCCCMC5AAAA4CCCCDDD",
                "DDCCCC5AAAA4CMMCDDD",
                "DDDCCCM6666CCCMCDDD",
                "DDDCCMCCCMCCCCCCDDD",
                "DDDCCCCMCCCCMMCCDDD",
                ".DDCCCCCCCCCCCCCDD.",
                "..DDDDDDDDDDDDDDD..",
                "...DDDDDDDDDDDDD...",
                "....DDDDDDDDDDD....",
        },
        // layer 9
        {
                "....GGGGGDDGGGG....",
                "...GDDDDDDDDDDDG...",
                "..GDDDDDDDDDDDDDG..",
                ".GDCCCCCCCCCCCCCDG.",
                "GDDCAAAAAAAAAAACDDG",
                "GDDCAAAAAAAAAAACDDG",
                "GDDCAAMAAAACAAACDDG",
                "GDDCAAAAAAAAAAAMDDD",
                "0CCCAAAAAAAAAAACDDD",
                "AAAIAAAAAAAAAAACDDG",
                "0CCCAAAAAAAAAAACDDG",
                "GDDCAAAAAAAAAAACDDG",
                "GDDMAACAAAAMAAACDDG",
                "GDDCAAAAAAAAAAACDDG",
                "GDDCAAAAAAAAAAACDDG",
                ".GDCCCCCCMCCCCCCDG.",
                "..GDDDDDDDDDDDDDG..",
                "...GDDDDDDDDDDDD...",
                "....GGGGGGGGGGG....",
        },
        // layer 10
        {
                ".........GG..T.....",
                "....GGGDGDDDGGG....",
                "...GDDDDDDDDDDDD...",
                ".TGCCCCCMMCCCMCCD..",
                "TGDMAAAAAAAAAAACDG.",
                "TGDMAAAAAAAAAAAMDGT",
                ".DDCAACAAAACAAACDG.",
                ".DDMAAAAAAAAAAACDGG",
                "A0CCAAAAAAAAAAAMDGG",
                "AAAIAAAAAAAAAAACDG.",
                "A0MCAAAAAAAAAAAMDG.",
                ".GDCAAAAAAAAAAAMDD.",
                ".GDCAAMAAAAMAAACDGT",
                ".GDCAAAAAAAAAAACDGT",
                ".GDCAAAAAAAAAA.MDGT",
                ".TGCMCCCMMCMCCCCG..",
                "...GDDDDDDDDDDDD...",
                "....GGGDDGGDDDGG...",
                ".........TT........",
        },
        // layer 11
        {
                "...................",
                ".......G.GGG.TT....",
                "....GDGGGDGGDDGG...",
                "..TGDCCMCCCMCCDDG..",
                ".TGDCAAAAAAAAACDD..",
                ".TGCAA6AAAA6AAAMD..",
                ".GDCA4C5AA4C5AACG..",
                ".GDCAA7AAA.7AAACGT.",
                ".ACCAAAAAAAAAAACG..",
                ".ABMAAAAAAAAAAACG..",
                ".AMCAAAAAAAAAAAMG..",
                "..DCAA6AAAA6AAACDG.",
                ".TGCA4C5AA4C5AACD..",
                ".TGCAA7AAAA7AAACG..",
                ".TGDMAAAAAAAAACDG..",
                "...GDCMCCCCCCCGG...",
                "....GGGGGDDGGGGG...",
                "......TGG.TGGG.....",
                "...................",
        },
        // layer 12
        {
                "...................",
                "...................",
                ".....G.T.GT.GG.T...",
                "....GGGGGGGGGGGGT..",
                "...GGCCCCCCCCCGGG..",
                "...GCCCCCCCCCCCGG..",
                "..GGCCMCCCCCMCCGT..",
                "..GGCCCCMMCCCCCGT..",
                "..GGCCCCCCCCCCCGT..",
                "..GGCCMMCCMCCCCD...",
                "..GGCCCCCCCCCCCDT..",
                "..GGCCCCCCCCMCCGG..",
                "...GCMCCCCMCCCCGG..",
                "...GCCCCCCCCCCCG...",
                "..TGDCCCCCCCCCGG...",
                "....GDGGGGGGGGTT...",
                ".........GG...TT...",
                ".............T.....",
                "...................",
        },
        // layer 13
        {
                "...................",
                "...................",
                ".....T...T.........",
                "....TT...TTT...T...",
                "....TGGGGGGGGG.T...",
                "....GGGGGGDGGGG....",
                "....GGGGDDDDGGG....",
                "...TGGGDDDGDDGGT...",
                "...TGGGDDDDDDGG....",
                "....GGGDDDDDGGGG...",
                "....GGGGDDDDDGGG...",
                "....GGGGGDDGGGG....",
                "....GGGGGGGGGGGT...",
                "....GGGGGGGGGGGT...",
                "....GGGGGGGGGG.T...",
                ".....G.............",
                "...................",
                "...................",
                "...................",
        },
        // layer 14
        {
                "...................",
                "...................",
                "...................",
                "...................",
                ".........T.........",
                "..........G........",
                "....T...GGGG.......",
                "....T..GGG.GG......",
                ".......GGGGGG......",
                "....T..GGGGG.......",
                "....T...GGGGG......",
                ".....T...GG.TTT....",
                "....TTTTT.TTTTT....",
                "....TTT.TT.TTTT....",
                "....TTTTT...T......",
                ".....T.............",
                "...................",
                "...................",
                "...................",
        },
        // layer 15
        {
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "..........T........",
                "...................",
                "........TTTT.......",
                "........TTTT.......",
                ".........TTTT......",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
        },

    };

    private WorldGenMoundTemplate() {}

    /**
     * The state of one cell of the template, or null when the cell carries nothing.
     *
     * <p>The metadata of the 1.7.10 blocks became the block state of the 1.21.1 blocks:
     * the stairs keep their facing and their half, the ladder keeps its facing and the
     * chiselled stone bricks are the block the metadata three stands for.
     */
    public static BlockState stateFor(char symbol) {
        return switch (symbol) {
            case 'A' -> Blocks.AIR.defaultBlockState();
            case 'C' -> Blocks.COBBLESTONE.defaultBlockState();
            case 'D' -> Blocks.DIRT.defaultBlockState();
            case 'G' -> Blocks.GRASS_BLOCK.defaultBlockState();
            case 'I' -> Blocks.IRON_BARS.defaultBlockState();
            case 'M' -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 'T' -> Blocks.SHORT_GRASS.defaultBlockState();
            case 'B' -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            case 'L' -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
            case '0' -> stairs(Direction.EAST, Half.BOTTOM);
            case '1' -> stairs(Direction.WEST, Half.BOTTOM);
            case '2' -> stairs(Direction.SOUTH, Half.BOTTOM);
            case '3' -> stairs(Direction.NORTH, Half.BOTTOM);
            case '4' -> stairs(Direction.EAST, Half.TOP);
            case '5' -> stairs(Direction.WEST, Half.TOP);
            case '6' -> stairs(Direction.SOUTH, Half.TOP);
            case '7' -> stairs(Direction.NORTH, Half.TOP);
            default -> null;
        };
    }

    /** The stone stairs of the original metadata. */
    private static BlockState stairs(Direction facing, Half half) {
        return Blocks.STONE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, half);
    }

    /** The number of blocks the template writes. */
    public static int blockCount() {
        int count = 0;
        for (String[] layer : TEMPLATE) {
            for (String row : layer) {
                for (int index = 0; index < row.length(); index++) {
                    if (row.charAt(index) != '.') count++;
                }
            }
        }
        return count;
    }

    /** The cell of the template at the given layer, row and column. */
    public static char cell(int layer, int z, int x) {
        return TEMPLATE[layer][z].charAt(x);
    }

    /** The block of a cell of the template, or null when the cell carries nothing. */
    public static Block block(int layer, int z, int x) {
        BlockState state = stateFor(cell(layer, z, x));
        return state == null ? null : state.getBlock();
    }
}
