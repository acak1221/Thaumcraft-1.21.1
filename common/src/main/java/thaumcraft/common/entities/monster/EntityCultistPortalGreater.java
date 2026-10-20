package thaumcraft.common.entities.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The greater crimson portal. The 4.2.3.5 oracle registers a single cultist portal
 * (ConfigEntities: "CultistPortal"); the later package alias keeps the second name for
 * the same entity, so both ids arm the same original stage machine.
 */
public class EntityCultistPortalGreater extends EntityCultistPortal {
    public EntityCultistPortalGreater(EntityType<? extends EntityCultistPortalGreater> type, Level level) {
        super(type, level);
    }
}
