package thaumcraft.api;

import java.util.ArrayList;
import java.util.Objects;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.IScanEventHandler;
import thaumcraft.common.lib.research.EntityAspectBindings;

/** Native scan registration facade. Crafting, research registration/completion and the remaining original API are separate units. */
public final class ThaumcraftApi {
    public static ArrayList<IScanEventHandler> scanEventhandlers = new ArrayList<>();
    public static ArrayList<EntityTags> scanEntities = new ArrayList<>(EntityAspectBindings.originalTags());
    private ThaumcraftApi() {}

    public static void registerScanEventhandler(IScanEventHandler handler) { scanEventhandlers.add(Objects.requireNonNull(handler, "handler")); }
    public static void registerEntityTag(String entityName, AspectList aspects, EntityTagsNBT... nbt) {
        if (entityName == null || entityName.isEmpty()) throw new IllegalArgumentException("An entity tag requires its original or native registry name");
        scanEntities.add(new EntityTags(entityName, aspects, nbt));
    }

    /** Fields/references remain mutable as in the original API; a resolved scan copies the selected aspect list. */
    public static class EntityTags {
        public String entityName;
        public EntityTagsNBT[] nbts;
        public AspectList aspects;
        public EntityTags(String entityName, AspectList aspects, EntityTagsNBT... nbts) {
            this.entityName = entityName; this.aspects = aspects; this.nbts = nbts;
        }
    }
    public static class EntityTagsNBT {
        public String name;
        public Object value;
        public EntityTagsNBT(String name, Object value) { this.name = name; this.value = value; }
    }
}
