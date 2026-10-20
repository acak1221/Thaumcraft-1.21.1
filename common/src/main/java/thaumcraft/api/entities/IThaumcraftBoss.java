package thaumcraft.api.entities;

/**
 * Original TC4 {@code EntityThaumcraftBoss} marker.
 *
 * <p>The 1.7 boss base was the only mob the original champion promotion skipped
 * (oracle {@code EntityUtils.makeChampion:243-254}): a boss never took the champion
 * health bonus, never took the doubled champion damage, was never healed for the
 * promotion and never took the ordinary champion name, because a boss already scales
 * with the players that hunt it. Instead the boss named itself after its champion
 * modifier.
 *
 * <p>The native port keeps the base class package private inside the eldritch bestiary,
 * so this marker is the public door of the champion subsystem onto a boss.
 */
public interface IThaumcraftBoss {
    /**
     * Original {@code EntityThaumcraftBoss.generateName}. The base boss does not name
     * itself at all, the eldritch warden and the eldritch construct do.
     */
    void generateName();
}
