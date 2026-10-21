package thaumcraft.common.blocks;

/**
 * The answer of the piston of the 1.7.10 original for the families of the blocks of Thaumcraft.
 *
 * <p>The original asked the question once per class of block and never per metadata: the vanilla
 * 1.7.10 {@code net.minecraft.block.BlockPistonBase#canPushBlock} ended with
 * {@code return !(block instanceof ITileEntityProvider);}. A block whose class implemented that
 * interface was never moved, for any of its metadata, and every other block was moved for every
 * metadata.
 *
 * <p>The class holds no block and builds none, so the answer of the original can be read and
 * tested without a registry, which is what the test of the port does.
 */
public final class OriginalPistonRule {

    private OriginalPistonRule() {
    }

    /**
     * The answer of the piston of the 1.7.10 original for a family of Thaumcraft and a metadata of
     * the original.
     *
     * <p>The metadata is not part of the question of the original: it is a parameter so that the
     * callers and the tests can ask the question the same way for every metadata of a family.
     *
     * @param family the family of the block
     * @param metadata the metadata of the original
     * @return true when the piston of the original moved the block
     */
    public static boolean isPushableInOriginal(Family family, int metadata) {
        return !family.wasTileEntityProvider();
    }

    /**
     * The families of the blocks of Thaumcraft and the class they had in the 1.7.10 original.
     *
     * <p>The names of the families are the names of the 1.7.10 classes; the port splits some of
     * them over several blocks, and the split does not change the answer of the original, because
     * the original answered per class and not per metadata.
     */
    public enum Family {
        /**
         * Oracle BlockCosmeticSolid:60-61 - the 1.7.10 class extended {@code
         * net.minecraft.block.Block} and did not implement {@code ITileEntityProvider}: the piston
         * of the original moved the sixteen stones of the family, the warding stone (the metadata
         * three, the tile at BlockCosmeticSolid:278-296) and the node (the metadata eight)
         * included, and the move destroyed the tile of the two.
         */
        COSMETIC_SOLID(false),
        /**
         * Oracle BlockEldritchNothing:42-43 - the 1.7.10 class extended {@code
         * net.minecraft.block.Block} and did not implement {@code ITileEntityProvider}: the piston
         * of the original moved the filler for every metadata, the exposed filler (the metadata
         * one, the tile at BlockEldritchNothing:81-87) included, and the move destroyed its tile.
         */
        ELDRITCH_NOTHING(false),
        /**
         * Oracle BlockEldritchPortal:38-39 - the 1.7.10 class extended {@code
         * net.minecraft.block.Block} and did not implement {@code ITileEntityProvider}: the piston
         * of the original moved the gate, whose tile existed for every metadata
         * (BlockEldritchPortal:115-121), and the move destroyed the tile.
         */
        ELDRITCH_PORTAL(false),
        /**
         * Oracle BlockMagicalLog:50-51 - the 1.7.10 class extended {@code
         * net.minecraft.block.BlockRotatedPillar} and did not implement {@code
         * ITileEntityProvider}: the piston of the original moved the log for every metadata, the
         * silverwood node (the metadata two, the tile at BlockMagicalLog:160-170) included, and
         * the move destroyed the tile of the node.
         */
        SILVERWOOD_NODE(false),
        /**
         * Oracle BlockManaPod:51-52 - the 1.7.10 class extended {@code
         * net.minecraft.block.Block} and did not implement {@code ITileEntityProvider}: the piston
         * of the original moved the pod, whose tile existed for every metadata
         * (BlockManaPod:232-238), and the move destroyed the tile.
         */
        MANA_POD(false),
        /**
         * Oracle BlockCustomPlant:55-56 - the 1.7.10 class extended {@code
         * net.minecraft.block.BlockBush} and did not implement {@code ITileEntityProvider}: the
         * piston of the original moved the plant for every metadata, the ethereal bloom (the
         * metadata four, the tile at BlockCustomPlant:97-108) included, and the move destroyed
         * the tile of the bloom.
         */
        CUSTOM_PLANT(false),
        /**
         * Oracle BlockCosmeticOpaque:58-59 - the 1.7.10 class extended {@code
         * net.minecraft.block.BlockContainer}, which implements {@code ITileEntityProvider}: the
         * piston of the original never moved the family, not even the amber block and the amber
         * brick, whose tile the family creates for the metadata two only
         * (BlockCosmeticOpaque:214-218).
         */
        COSMETIC_OPAQUE(true),
        /**
         * Oracle BlockEldritch:60-61 - the 1.7.10 class extended {@code
         * net.minecraft.block.BlockContainer}, which implements {@code ITileEntityProvider}: the
         * piston of the original never moved the family, not even the stones without a tile
         * (BlockEldritch:151-153).
         */
        ELDRITCH(true),
        /**
         * Oracle BlockAiry:77-78 - the 1.7.10 class extended {@code
         * net.minecraft.block.BlockContainer}, which implements {@code ITileEntityProvider}: the
         * piston of the original never moved the family, not even the variants without a tile
         * (BlockAiry:313-326).
         */
        AIRY(true);

        /** True when the class of the block in the original implemented {@code ITileEntityProvider}. */
        private final boolean tileEntityProvider;

        Family(boolean tileEntityProvider) {
            this.tileEntityProvider = tileEntityProvider;
        }

        /**
         * True when the class of the block in the 1.7.10 original implemented {@code
         * net.minecraft.block.ITileEntityProvider}: the piston of the original never moved the
         * family.
         */
        public boolean wasTileEntityProvider() {
            return this.tileEntityProvider;
        }

        /**
         * The answer of the piston of the 1.7.10 original for a metadata of the family: the piston
         * moved the block iff the class of the block in the original did not implement {@code
         * ITileEntityProvider}. The metadata was never part of the question.
         *
         * @param metadata the metadata of the original
         * @return true when the piston of the original moved the block
         */
        public boolean isPushableInOriginal(int metadata) {
            return OriginalPistonRule.isPushableInOriginal(this, metadata);
        }
    }
}
