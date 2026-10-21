package thaumcraft.common.items.golems;

/**
 * The original's owner protection of a golem.
 *
 * <p>The original cancelled <em>every</em> interaction with a golem that has an owner when the interacting player was not
 * that owner, and answered with "You are not my Master!" (oracle
 * {@code thaumcraft/common/lib/events/EventHandlerEntity.java}:348-356). The original compared the owner's NAME with the
 * interacting player's name, so this port compares the same two names.
 *
 * <p>A golem without an owner (the owner name is empty) answers to everybody, exactly like the original.
 *
 * <p>Oracle: {@code thaumcraft/common/lib/events/EventHandlerEntity.java}, lines 348-356.
 */
public final class GolemOwnership {
    /** The original's message, kept in the language file as {@code tc.golem.notowner}. */
    public static final String NOT_OWNER_KEY = "tc.golem.notowner";

    private GolemOwnership() {}

    /**
     * Whether the golem belongs to somebody else: the golem has an owner and the player is not that owner.
     *
     * @param ownerName the golem's owner name, empty while the golem has no owner
     * @param playerName the interacting player's name
     */
    public static boolean denies(String ownerName, String playerName) {
        return ownerName != null && !ownerName.isEmpty() && !ownerName.equals(playerName);
    }

    /** Whether the player may interact with and configure the golem. */
    public static boolean allows(String ownerName, String playerName) { return !denies(ownerName, playerName); }
}
