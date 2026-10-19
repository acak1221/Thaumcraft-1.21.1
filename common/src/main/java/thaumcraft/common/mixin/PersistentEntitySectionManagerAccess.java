package thaumcraft.common.mixin;

import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PersistentEntitySectionManager.class)
public interface PersistentEntitySectionManagerAccess {
    @Accessor("knownUuids")
    Set<UUID> thaumcraft$getKnownUuids();

    @Accessor("sectionStorage")
    EntitySectionStorage<Entity> thaumcraft$getSectionStorage();
}
