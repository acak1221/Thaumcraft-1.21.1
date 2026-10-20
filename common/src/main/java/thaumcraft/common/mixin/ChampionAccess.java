package thaumcraft.common.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

/**
 * The champion modifier state of a mob. The 1.7 original kept the modifier id in the
 * custom tc.mobmod attribute of every EntityMob, which 1.21.1 cannot register; the
 * native port keeps the id in one int field of the mob and mirrors it into the
 * entity's synced data, so the client can reproduce the original champion particles.
 * The synced id belongs to the entity tree, so it is defined once while Entity
 * initialises and never moves.
 */
public interface ChampionAccess {
    /** The synced mirror of the original champion modifier id. */
    EntityDataAccessor<Integer> CHAMPION_DATA = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
    /** The persistent champion modifier id of the mob, {@code NOT_ROLLED} while it was never rolled. */
    int thaumcraft$getChampionModifier();
    /** Writes the persistent champion modifier id of the mob. */
    void thaumcraft$setChampionModifier(int modifier);
}
