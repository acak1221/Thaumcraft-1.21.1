package thaumcraft.common.lib.tiles;

/**
 * The decision logic of the remaining TC4 blocks and tiles, without a world, an entity or a registry.
 * <p>
 * Every number here comes from the TC4 4.2.3.5 oracle and is quoted with its original file and line so a
 * reviewer can check the port against the authority:
 * <ul>
 * <li>{@code thaumcraft/common/tiles/TileArcaneFurnace.java} - cooking, bellows, facing and ejection.</li>
 * <li>{@code thaumcraft/common/tiles/TileArcaneFurnaceNozzle.java} - the essentia nozzle.</li>
 * <li>{@code thaumcraft/common/blocks/BlockArcaneFurnace.java} - the multi block shell, its level and its drops.</li>
 * <li>{@code thaumcraft/common/tiles/TileArcaneLampLight.java} - the decaying lamp light.</li>
 * <li>{@code thaumcraft/common/blocks/BlockFluidDeath.java} - the death fluid.</li>
 * <li>{@code thaumcraft/common/tiles/TileTube.java} and its one way / restrict / valve subclasses.</li>
 * <li>{@code thaumcraft/common/tiles/TileWandPedestal.java} - the wand pedestal.</li>
 * <li>{@code thaumcraft/common/tiles/TileMagicWorkbench.java} - the workbench matrix.</li>
 * <li>{@code thaumcraft/common/tiles/TileOwned.java} - ownership and the access list.</li>
 * <li>{@code thaumcraft/common/tiles/TileMemory.java} - the stored block, metadata and tile entity data.</li>
 * </ul>
 * The classes that call these methods own the world dependent part; this class owns the numbers and the
 * decisions, so they can be unit tested without a game runtime.
 */
public final class LegacyTileRules {
    // --------------------------------------------------------------------------------------------------------------
    // Arcane furnace (oracle thaumcraft/common/tiles/TileArcaneFurnace.java)
    // --------------------------------------------------------------------------------------------------------------
    /** TileArcaneFurnace:40 - the furnace holds thirty two slots, not the vanilla three. */
    public static final int FURNACE_SLOTS = 32;
    /** TileArcaneFurnace:81-83 - one stack in one slot holds sixty four items. */
    public static final int FURNACE_STACK_LIMIT = 64;
    /** TileArcaneFurnace:172 - at most three bellows are counted, however many are found. */
    public static final int MAX_BELLOWS = 3;
    /** TileArcaneFurnace:176 - an unspeedy furnace cooks in one hundred and forty ticks. */
    public static final int COOK_TIME_PLAIN = 140;
    /** TileArcaneFurnace:176 - a furnace with vis cooks in eighty ticks. */
    public static final int COOK_TIME_SPEEDY = 80;
    /** TileArcaneFurnace:176 - every bellows takes twenty ticks off the cook time. */
    public static final int COOK_TIME_PER_BELLOWS = 20;
    /** TileArcaneFurnace:132 - the furnace drains five fire vis a tick from the vis network. */
    public static final int FURNACE_VIS_DRAIN = 5;
    /** TileArcaneFurnaceNozzle:43 - one successful essentia draw adds six hundred speedy ticks. */
    public static final int NOZZLE_SPEEDY_GRANT = 600;
    /** TileArcaneFurnaceNozzle:107 - the nozzle only offers suction while the furnace is still hungry. */
    public static final int NOZZLE_SUCTION = 128;
    /** TileArcaneFurnaceNozzle:106 - below this speedy time the nozzle offers its suction. */
    public static final int NOZZLE_SUCTION_CUTOFF = 40;
    /** TileArcaneFurnaceNozzle:53 - the nozzle only draws on every fifth tick. */
    public static final int NOZZLE_DRAW_PERIOD = 5;
    /** BlockArcaneFurnace:215-221 - the core and the output nozzle light up to thirteen. */
    public static final int FURNACE_LIGHT = 13;
    /** TileArcaneFurnace:177 - the item leaves the furnace one and a fifth blocks along the facing. */
    public static final float EJECT_OFFSET = 1.2F;
    /** TileArcaneFurnace:238 - a sideways ejection pushes the item by 0.13 blocks a tick. */
    public static final float EJECT_PUSH = 0.13F;
    /** TileArcaneFurnace:230 - a centred ejection scatters the item by 0.03 blocks a tick. */
    public static final float EJECT_SCATTER = 0.03F;
    /** TileArcaneFurnace:276 - the experience orb scatter is half the item scatter. */
    public static final float EJECT_XP_SCATTER = 0.025F;
    /** TileArcaneFurnace:242 - without bellows one smelt in four pays the bonus. */
    public static final float BONUS_CHANCE_NO_BELLOWS = 0.25F;
    /** TileArcaneFurnace:246 - with bellows every bellows pays the bonus with a 0.44 chance. */
    public static final float BONUS_CHANCE_PER_BELLOWS = 0.44F;

    /** The horizontal facing of the furnace core: the axis it ejects towards. */
    public record Facing(int x, int z) {
        public static final Facing SOUTH = new Facing(0, 1);
        public static final Facing NORTH = new Facing(0, -1);
        public static final Facing WEST = new Facing(-1, 0);
        public static final Facing EAST = new Facing(1, 0);
    }

    /** TileArcaneFurnace:175-177 - the cook time of one item. */
    public static int cookTime(boolean speedy, int bellows) {
        int base = speedy ? COOK_TIME_SPEEDY : COOK_TIME_PLAIN;
        return base - COOK_TIME_PER_BELLOWS * clampBellows(bellows);
    }

    /** TileArcaneFurnace:172 - the bellows count is capped at three. */
    public static int clampBellows(int bellows) { return Math.max(0, Math.min(MAX_BELLOWS, bellows)); }

    /**
     * TileArcaneFurnace:209-219 - the core ejects towards the output nozzle: west first, then east, and
     * otherwise towards the north when there is one, else south.
     */
    public static Facing furnaceFacing(boolean westNozzle, boolean eastNozzle, boolean northNozzle) {
        if (westNozzle) return Facing.WEST;
        if (eastNozzle) return Facing.EAST;
        return northNozzle ? Facing.NORTH : Facing.SOUTH;
    }

    /**
     * BlockArcaneFurnace:194-213 - the render level of one shell block. The original reads the metadata of
     * the block above and below; a metadata of ten (the output nozzle) or zero (air) counts as "the same as
     * this block".
     */
    public static int furnaceLevel(int meta, int metaAbove, int metaBelow, boolean aboveIsFurnace, boolean belowIsFurnace) {
        int above = metaAbove == 10 || metaAbove == 0 ? meta : metaAbove;
        int below = metaBelow == 10 || metaBelow == 0 ? meta : metaBelow;
        if (meta == above && meta == below && aboveIsFurnace && belowIsFurnace) return 9;
        if (meta == above && aboveIsFurnace && (meta != below || !belowIsFurnace)) return 18;
        return 0;
    }

    /** What a broken shell block turns back into. */
    public enum Restored {
        /** BlockArcaneFurnace:347 - the core leaves nothing behind. */
        AIR,
        /** BlockArcaneFurnace:348 - the output nozzle leaves iron bars. */
        IRON_BARS,
        /** BlockArcaneFurnace:348 - the even metadata leaves obsidian. */
        OBSIDIAN,
        /** BlockArcaneFurnace:348 - everything else leaves nether brick. */
        NETHER_BRICK
    }

    /** BlockArcaneFurnace:347-349 - the block a shell piece is restored to when the furnace collapses. */
    public static Restored restoredBlock(int meta) {
        if (meta == 0) return Restored.AIR;
        if (meta == 10) return Restored.IRON_BARS;
        if (meta % 2 == 0 || meta == 5) return Restored.OBSIDIAN;
        return Restored.NETHER_BRICK;
    }

    /**
     * BlockArcaneFurnace:301-319 - the furnace collapses when any of the twenty five cells around its core
     * is no longer part of the furnace. The core itself and the cell directly above it are not checked.
     */
    public static boolean structureIntact(java.util.function.IntPredicate cell) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dy == 0 || dy == 1) && dx == 0 && dz == 0) continue;
                    if (!cell.test(offset(dx, dy, dz))) return false;
                }
            }
        }
        return true;
    }

    /** A cell of the three by three by three shell around the core. */
    public static int offset(int dx, int dy, int dz) { return (dx + 1) * 100 + (dy + 1) * 10 + (dz + 1); }

    /** TileArcaneFurnace:230-231 - the horizontal momentum of an ejected item. */
    public static float ejectMomentum(int facing) {
        return facing == 0 ? EJECT_SCATTER : facing * EJECT_PUSH;
    }

    /** TileArcaneFurnace:239-247 - how many bonus rolls a smelt gets, and the chance of each one. */
    public static int bonusRolls(int bellows) { return bellows <= 0 ? 1 : bellows; }

    /** TileArcaneFurnace:239-247 - the chance that one bonus roll pays out. */
    public static float bonusChance(int bellows) { return bellows <= 0 ? BONUS_CHANCE_NO_BELLOWS : BONUS_CHANCE_PER_BELLOWS; }

    // --------------------------------------------------------------------------------------------------------------
    // Arcane furnace nozzle (oracle thaumcraft/common/tiles/TileArcaneFurnaceNozzle.java)
    // --------------------------------------------------------------------------------------------------------------

    /** TileArcaneFurnaceNozzle:35 - the nozzle faces away from the core it belongs to. */
    public static int nozzleFacing(int directionTowardsCore) { return opposite(directionTowardsCore); }

    /**
     * The opposite of a 0..5 direction index in the original ForgeDirection order:
     * down, up, north, south, west, east.
     */
    public static int opposite(int direction) {
        if (direction < 2) return 1 - direction;
        return direction % 2 == 0 ? direction + 1 : direction - 1;
    }

    /** TileArcaneFurnaceNozzle:103-114 - the suction the nozzle offers, zero once the furnace is full of vis. */
    public static int nozzleSuction(int speedyTime) { return speedyTime < NOZZLE_SUCTION_CUTOFF ? NOZZLE_SUCTION : 0; }

    /** TileArcaneFurnaceNozzle:53 - the nozzle only tries to draw on every fifth tick. */
    public static boolean nozzleDrawTick(int drawDelay) { return (drawDelay + 1) % NOZZLE_DRAW_PERIOD == 0; }

    /** TileArcaneFurnaceNozzle:62 - the draw only succeeds when the neighbour offers less than we do. */
    public static boolean nozzleOutdraws(int neighbourSuction, int ownSuction) { return neighbourSuction < ownSuction; }

    // --------------------------------------------------------------------------------------------------------------
    // Arcane lamp light (oracle thaumcraft/common/tiles/TileArcaneLampLight.java)
    // --------------------------------------------------------------------------------------------------------------

    /** TileArcaneLampLight:30 - the light checks its source on every hundredth tick. */
    public static final int LAMP_LIGHT_CHECK_PERIOD = 100;

    /** TileArcaneLampLight:30 - the light removes itself when its source lamp is gone. */
    public static boolean lampLightExpires(int count, boolean sourceIsLamp) { return count % LAMP_LIGHT_CHECK_PERIOD == 0 && !sourceIsLamp; }

    // --------------------------------------------------------------------------------------------------------------
    // Death fluid (oracle thaumcraft/common/blocks/BlockFluidDeath.java)
    // --------------------------------------------------------------------------------------------------------------

    /** BlockFluidDeath:41 - one block holds four quanta of death fluid. */
    public static final int DEATH_FLUID_QUANTA = 4;
    /** BlockFluidDeath:57 - the damage of the death fluid is its quanta plus one. */
    public static int deathFluidDamage(int quanta) { return quanta + 1; }
    /** BlockFluidDeath:57 - the death fluid never hurts a client side world. */
    public static boolean deathFluidHurts(boolean clientSide) { return !clientSide; }

    /**
     * The level a death fluid cell takes from the strongest cell around it. A cell that has no stronger
     * neighbour loses one quanta on every step, so it dries up again once it reaches the quanta of the
     * block.
     */
    public static int deathFluidNextLevel(int current, int strongestNeighbour) {
        int next = strongestNeighbour >= DEATH_FLUID_QUANTA ? current + 1 : strongestNeighbour + 1;
        return Math.min(next, DEATH_FLUID_QUANTA);
    }

    /**
     * The level a death fluid cell may carry. A state that owns no level property never receives one, so
     * the death fluid block itself has to be placed in a cell that has none; the level of a cell that does
     * own the property never leaves the range of the four quanta of the block.
     */
    public static int deathFluidCellLevel(boolean hasLevelProperty, int requested) {
        if (!hasLevelProperty) return -1;
        return Math.max(0, Math.min(requested, DEATH_FLUID_QUANTA - 1));
    }

    // --------------------------------------------------------------------------------------------------------------
    // Essentia tubes (oracle thaumcraft/common/tiles/TileTube*.java)
    // --------------------------------------------------------------------------------------------------------------

    /** TileTube - a plain tube offers one less than the neighbour's suction. */
    public static int tubeSuction(int neighbourSuction) { return neighbourSuction - 1; }

    /** TileTubeRestrict:13 - a restrict tube halves the neighbour's suction. */
    public static int restrictSuction(int neighbourSuction) { return neighbourSuction / 2; }

    /** TileTubeOneway:13 - a one way tube only pulls from the face opposite its facing. */
    public static boolean oneWayPulls(int facing, int face) { return face != opposite(facing); }

    /**
     * TileTubeValve:36-47 - the valve closes when it is powered and opens again when the power is removed.
     * The edge latch means that a valve which is already closed stays closed.
     */
    public static boolean valveFlowAfterEdge(boolean wasPowered, boolean nowPowered, boolean allowFlow) {
        if (wasPowered && !nowPowered && !allowFlow) return true;
        if (!wasPowered && nowPowered && allowFlow) return false;
        return allowFlow;
    }

    /** TileTubeValve:51-55 - the valve wheel turns twenty degrees a tick towards its state. */
    public static float valveRotation(float rotation, boolean allowFlow) {
        if (!allowFlow && rotation < 360.0F) return rotation + 20.0F;
        if (allowFlow && rotation > 0.0F) return rotation - 20.0F;
        return rotation;
    }

    // --------------------------------------------------------------------------------------------------------------
    // Wand pedestal (oracle thaumcraft/common/tiles/TileWandPedestal.java)
    // --------------------------------------------------------------------------------------------------------------

    /** TileWandPedestal:199 - the pedestal looks for nodes in a cube of sixteen blocks around it. */
    public static final int WAND_PEDESTAL_SCAN_RADIUS = 8;
    /** TileWandPedestal:189 - the pedestal refreshes its node list on every twentieth tick. */
    public static final int WAND_PEDESTAL_REFRESH_PERIOD = 20;
    /** TileWandPedestal:193 - the pedestal draws on every fifth tick. */
    public static final int WAND_PEDESTAL_DRAW_PERIOD = 5;
    /** TileWandPedestal:293 - the pedestal looks for new nodes on every hundredth tick. */
    public static final int WAND_PEDESTAL_RESCAN_PERIOD = 100;
    /** TileWandPedestal:201 - an iron capped or wooden wand may be drained completely. */
    public static int wandPedestalReserve(String cap, String rod) {
        return "iron".equals(cap) || "wood".equals(rod) ? 0 : 1;
    }

    /** TileWandPedestal:344 - the aspect readout of the pedestal is the whole vis of the wand. */
    public static int wandPedestalReadout(int centivis) { return centivis / 100; }

    /** TileWandPedestal:311-312 - the pedestal may only be opened from within sixty four blocks. */
    public static final double WAND_PEDESTAL_USE_RANGE = 64.0D;

    // --------------------------------------------------------------------------------------------------------------
    // Magic workbench (oracle thaumcraft/common/tiles/TileMagicWorkbench.java)
    // --------------------------------------------------------------------------------------------------------------

    /** TileMagicWorkbench:31 - the workbench has eleven slots: a three by three matrix, a result and a wand. */
    public static final int MAGIC_WORKBENCH_SLOTS = 11;
    /** TileMagicWorkbench:158 - only the wand slot is exposed to automation. */
    public static final int MAGIC_WORKBENCH_WAND_SLOT = 10;
    /** TileMagicWorkbench:43-49 - the matrix slot of one row and column. */
    public static int magicWorkbenchSlot(int row, int column) {
        if (row < 0 || row > 2 || column < 0 || column > 2) return -1;
        return row * 3 + column;
    }

    /** TileMagicWorkbench:147-156 - the wand slot only accepts a casting wand that is not a staff. */
    public static boolean magicWorkbenchAcceptsWand(boolean castingWand, boolean staff) { return castingWand && !staff; }

    // --------------------------------------------------------------------------------------------------------------
    // Ownership (oracle thaumcraft/common/tiles/TileOwned.java)
    // --------------------------------------------------------------------------------------------------------------

    /** TileOwned:19-21 - the owner always keeps access to their own block. */
    public static boolean ownedAccess(String owner, java.util.List<String> access, String name) {
        if (owner == null || name == null) return false;
        if (owner.equals(name)) return true;
        return access != null && access.contains(name);
    }

    /** TileOwned:21 - a block is only safe to remove once it has an owner. */
    public static boolean ownedSafeToRemove(String owner) { return owner != null && !owner.isEmpty(); }

    // --------------------------------------------------------------------------------------------------------------
    // Cosmetic blocks (oracle thaumcraft/common/blocks/BlockCosmetic*.java)
    // --------------------------------------------------------------------------------------------------------------

    /** BlockCosmeticSolid:227-239 - the light of a cosmetic solid variant. */
    public static int cosmeticSolidLight(int variant) {
        if (variant == 2) return 9;
        if (variant == 14) return 4;
        return 0;
    }

    /**
     * BlockCosmeticSolid:241-253 - the explosion resistance of a cosmetic solid variant. The default is the
     * original setResistance(10) of the block, which the 1.7 engine stored as thirty and exposed as six.
     */
    public static float cosmeticSolidResistance(int variant) {
        if (variant <= 1 || variant == 8) return 999.0F;
        if (variant == 4 || variant == 6 || variant == 7) return 20.0F;
        return 6.0F;
    }

    /** BlockCosmeticSolid:213-225 - the hardness of a cosmetic solid variant. */
    public static float cosmeticSolidHardness(int variant) {
        if (variant <= 1 || variant == 8) return 30.0F;
        if (variant == 4 || variant == 6 || variant == 7) return 4.0F;
        return 2.0F;
    }

    /** BlockCosmeticOpaque:163-169 - the light opacity of an amber block; the warded glass is see through. */
    public static int cosmeticOpaqueOpacity(int variant) { return variant <= 1 ? 3 : 0; }

    /** BlockCosmeticOpaque:253-259 - the hardness of an amber block; the warded glass is unbreakable. */
    public static float cosmeticOpaqueHardness(int variant) { return variant == 2 ? -1.0F : 1.5F; }

    /** BlockCosmeticOpaque:261-267 - the explosion resistance of an amber block; the warded glass is immune. */
    public static float cosmeticOpaqueResistance(int variant) { return variant == 2 ? 999.0F : 3.0F; }

    /** BlockCosmeticOpaque:84-92 - the warded glass keeps its own texture on every face. */
    public static boolean cosmeticOpaqueWarded(int variant) { return variant == 2; }

    private LegacyTileRules() {}
}
