package thaumcraft.common.lib.mods;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;

/**
 * The champion modifier state of a mob. The 1.7 original kept the modifier id in the
 * custom tc.mobmod attribute of every EntityMob, which 1.21.1 cannot register; the
 * native port keeps the id in one int field of the mob and mirrors it into the
 * entity's synced data, so the client can reproduce the original champion particles.
 * <p>
 * The synced id belongs to the living entity branch of the entity tree: the 1.21.1
 * data ids are numbered per class tree, and an id that is taken in the entity branch
 * but only defined for the living entities leaves a hole in the data of every other
 * entity. The id is therefore taken for {@link LivingEntity}, which is the branch
 * that defines it, and it is defined once while Entity initialises so that the
 * client and the server know the same slot.
 */
public interface ChampionAccess {
    /** The synced mirror of the original champion modifier id. */
    EntityDataAccessor<Integer> CHAMPION_DATA = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    /** The persistent champion modifier id of the mob, {@code NOT_ROLLED} while it was never rolled. */
    int thaumcraft$getChampionModifier();
    /** Writes the persistent champion modifier id of the mob. */
    void thaumcraft$setChampionModifier(int modifier);
}
