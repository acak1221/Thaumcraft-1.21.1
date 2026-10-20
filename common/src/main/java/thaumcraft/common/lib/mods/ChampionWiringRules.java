package thaumcraft.common.lib.mods;

/**
 * The pure glue that joins the champion system to the Outer Lands labyrinth, to the
 * original eldritch bosses and to the original death reward.
 *
 * <p>Every decision here is a number of the original 1.7 game, so it can be unit tested
 * without a running world:
 * <ul>
 *   <li>Oracle {@code EventHandlerEntity.isDangerousLocation:426-431} - a place of the Outer
 *       Lands is dangerous when the maze cell of its chunk is the key room or the library
 *       room.</li>
 *   <li>Oracle {@code EntityUtils.makeChampion:243-254} - a boss never takes the ordinary
 *       champion bonuses, it names itself after its champion modifier instead.</li>
 *   <li>Oracle {@code EventHandlerEntity.livingDrops:453-461} - the extra experience and the
 *       loot bag a champion mob dropped when it died.</li>
 * </ul>
 */
public final class ChampionWiringRules {
    private ChampionWiringRules() {}

    /** Original labyrinth feature of the key room, one of the two dangerous rooms. */
    public static final int FEATURE_KEY_ROOM = 6;
    /** Original labyrinth feature of the library room, the other dangerous room. */
    public static final int FEATURE_LIBRARY_ROOM = 8;

    /**
     * Original {@code EventHandlerEntity.isDangerousLocation:426-431}. The 1.7 original read
     * the cell of the maze map and called the place dangerous when the cell was the key room
     * or the library room.
     */
    public static boolean dangerousMazeFeature(int feature) {
        return feature == FEATURE_KEY_ROOM || feature == FEATURE_LIBRARY_ROOM;
    }

    /**
     * The chunk of a block position, the cell the original looked up. The 1.7 original
     * shifted the block coordinate by four bits.
     */
    public static int mazeChunk(int blockCoordinate) { return blockCoordinate >> 4; }

    /**
     * The complete original dangerous location test: the place has to stand in the Outer
     * Lands and in a dangerous cell of the labyrinth.
     */
    public static boolean dangerousLocation(boolean outerLands, int feature) {
        return outerLands && dangerousMazeFeature(feature);
    }

    /**
     * Original {@code EntityUtils.makeChampion:243-254}. The 1.7 original skipped the
     * champion health bonus, the champion damage bonus, the promotion heal and the
     * champion name for a boss, because a boss scales with the players that hunt it.
     */
    public static boolean ordinaryChampionBonuses(boolean boss) { return !boss; }

    /**
     * Original {@code EventHandlerEntity.livingDrops:453}. The 1.7 original only paid the
     * champion reward to a mob that was hit by a player, that was no boss and that carried a
     * champion modifier.
     */
    public static boolean championReward(boolean recentlyHit, boolean mob, boolean boss, int championModifier) {
        return recentlyHit && mob && !boss && championModifier >= 0;
    }
}
