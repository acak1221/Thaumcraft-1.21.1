package thaumcraft.common.lib.mods;

import net.minecraft.network.syncher.EntityDataAccessor;

/**
 * The synced mirror of the original champion modifier id.
 *
 * <p>The 1.7 original kept the modifier id in the custom {@code tc.mobmod} attribute of
 * every hostile mob and read it on the client, where it never arrived, so the original
 * client particle hook could not fire there. The native port keeps the modifier id in one
 * int field of the mob and mirrors it into the entity data, which is the native way to
 * tell the client that a mob is a champion.</p>
 *
 * <p>The data id belongs to the <em>living entity</em> part of the entity tree: the entity
 * data builder refuses to build an entity whose data ids are not a dense sequence, so an id
 * that the entity tree itself defined would have to be defined by every item entity,
 * projectile and vehicle as well. The id is created while the living entity tree
 * initialises, so the client and the server always agree on its number.</p>
 */
public final class ChampionData {
    /** The synced champion modifier id, created while the living entity tree initialises. */
    public static EntityDataAccessor<Integer> CHAMPION;

    private ChampionData() {}
}
