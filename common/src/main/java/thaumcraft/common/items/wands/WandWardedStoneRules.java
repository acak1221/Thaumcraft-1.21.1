package thaumcraft.common.items.wands;

/**
 * The three "warded stone" blocks a wand can take back.
 *
 * <p>Original {@code thaumcraft/common/items/wands/ItemWandCasting.java:390-415} (oracle
 * {@code ItemWandCasting.onItemUseFirst}): a wand click removes the arcane pressure plate (the wooden
 * device metadata two), the warded glass (the cosmetic opaque metadata two) and the arcane door, and
 * gives the block back as an item.</p>
 *
 * <p>Original {@code ItemWandCasting:390} and {@code :400} gate the removal on the owner: while the
 * "warded stone" option is on, only the owner of the block may take it back; while it is off, anybody
 * may. The oracle compares a {@code GameProfile} with a {@code String} here
 * ({@code player.getGameProfile().equals(((TileOwned) tile).owner)}), which is never equal, so the
 * owner could never reclaim their own block. The native port compares the owner NAME, the way the
 * rest of the native owned-block layer already does ({@code BlockArcaneDoor.useWithoutItem} and
 * {@code ItemKey.useOn}), and OpenTC4 made the same change to the 1.7.10 code.</p>
 *
 * <p>Original {@code ItemWandCasting:407}: the arcane door only drops its item from the lower half
 * while the "warded stone" option is off, because the upper half never carried a drop itself
 * ({@code BlockArcaneDoor:326}).</p>
 */
public final class WandWardedStoneRules {
    private WandWardedStoneRules() {}

    /**
     * Original {@code ItemWandCasting:390} and {@code :400}: the ownership gate of the removal.
     *
     * @param wardedStone the original {@code Config.wardedStone}: while it is on, only the owner may
     *                     take the block back
     * @param owner       the owner name of the block entity, empty when the block has no owner
     * @param playerName  the name of the player who clicked with the wand
     */
    public static boolean mayRemove(boolean wardedStone, String owner, String playerName) {
        if (!wardedStone) return true;
        return owner != null && !owner.isEmpty() && owner.equals(playerName);
    }

    /**
     * Original {@code ItemWandCasting:407}: the arcane door drops its item from the lower half only,
     * unless the "warded stone" option is on and the block can no longer be broken at all.
     */
    public static boolean dropsDoorItem(boolean wardedStone, boolean upperHalf) {
        return wardedStone || !upperHalf;
    }
}
