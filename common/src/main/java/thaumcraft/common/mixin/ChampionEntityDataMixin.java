package thaumcraft.common.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.entity.Entity;

/**
 * The original champion modifier id was a custom entity attribute, so it travelled
 * with the entity and reached the client with the entity itself. The native port
 * keeps the id in the mob's own field and mirrors it into the entity's synced data.
 * The synced id has to exist before the first entity is built, because the data
 * array of an entity is sized by the ids its class tree knows; this mixin therefore
 * owns the id in the static initialiser of Entity itself, which runs at the same
 * moment on the client and on the server.
 */
@Mixin(Entity.class)
public abstract class ChampionEntityDataMixin {
    /** Defines the champion data id while Entity initialises, never later. */
    @Unique
    private static final EntityDataAccessor<Integer> thaumcraft$championData = ChampionAccess.CHAMPION_DATA;
}
