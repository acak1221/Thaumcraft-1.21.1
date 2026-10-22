package thaumcraft.common.entities.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The greater crimson portal. The 4.2.3.5 oracle registers a single cultist portal
 * (ConfigEntities:129-130, "CultistPortal"); TileEldritchLock:319-321 creates that type,
 * not this alias. This retained later-package alias shares the stage machine but is
 * deliberately not survival-spawned. See docs/audit/ORPHAN_MOB_FINDINGS.md.
 */
public class EntityCultistPortalGreater extends EntityCultistPortal {
    public EntityCultistPortalGreater(EntityType<? extends EntityCultistPortalGreater> type, Level level) {
        super(type, level);
    }
}
