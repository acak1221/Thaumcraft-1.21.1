/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumic.tinkerer.common.item.foci.FociFx;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * The owner of a projectile and the rule of the Focus of Distortion that pushes projectiles past the player.
 *
 * <p>1.7.10 asked a table of owner getters keyed by class ({@code EntityArrow.shootingEntity},
 * {@code EntityThrowable.getThrower()}, Thaumcraft's frost shard) and applied the shield to every
 * {@code IProjectile}. 1.21.1 has one base class for all of them, {@code Projectile}, with {@code getOwner()}, so the
 * table only remains for what is not a {@code Projectile} and for a mod that wants another answer. The shield keeps the
 * 1.7.10 reach: arrows and thrown objects (and Thaumcraft's own bolts, which are {@code Projectile}s), not the fireballs,
 * skulls, charges, fireworks and fishing bobbers that were never {@code IProjectile}.</p>
 *
 * <p>The blacklist of 1.7.10 ({@code ItemFocusDeflect.DeflectBlacklist}, which held the class of the experience bottle and,
 * with Botania or Blood Magic, theirs) is a set of entity-type ids now, and the entity-type tag
 * {@code thaumictinkerer:deflect_blacklist} of the data pack is part of it.</p>
 */
public final class ProjectileHelper {
    /** The entity-type tag of projectiles that the Focus of Distortion leaves alone. */
    public static final TagKey<EntityType<?>> DEFLECT_BLACKLIST_TAG = TagKey.create(Registries.ENTITY_TYPE,
            LibMisc.id("deflect_blacklist"));

    /** Entity-type ids that the Focus of Distortion leaves alone (1.7.10: {@code DeflectBlacklist}, a list of classes). */
    public static final Set<ResourceLocation> DEFLECT_BLACKLIST = ConcurrentHashMap.newKeySet();

    private static final Map<Class<? extends Entity>, Function<Entity, Entity>> OWNER_GETTERS = Collections
            .synchronizedMap(new IdentityHashMap<>());

    private ProjectileHelper() {}

    // ---- owners ----------------------------------------------------------------------------------------------------

    /** The entity that shot or threw the projectile, or null when it has no known owner. */
    @Nullable
    public static Entity getOwner(Entity projectile) {
        Function<Entity, Entity> exact = OWNER_GETTERS.get(projectile.getClass());
        if (exact != null) return exact.apply(projectile);

        // Check if we have an owner getter for any of the superclasses.
        synchronized (OWNER_GETTERS) {
            for (Map.Entry<Class<? extends Entity>, Function<Entity, Entity>> ownerGetter : OWNER_GETTERS.entrySet()) {
                if (ownerGetter.getKey().isAssignableFrom(projectile.getClass())) {
                    return ownerGetter.getValue().apply(projectile);
                }
            }
        }

        return projectile instanceof Projectile native1 ? native1.getOwner() : null;
    }

    /** For an entity that is not a {@code Projectile}, or one whose owner the vanilla accessor does not give. */
    public static void registerOwnerGetter(Class<? extends Entity> projectileClass, Function<Entity, Entity> ownerGetter) {
        OWNER_GETTERS.put(projectileClass, ownerGetter);
    }

    // ---- the Focus of Distortion -----------------------------------------------------------------------------------

    /** 1.7.10 {@code ItemFocusDeflect.setupBlackList}: the experience bottle is never deflected. */
    public static void setupBlackList() {
        DEFLECT_BLACKLIST.add(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.EXPERIENCE_BOTTLE));
    }

    /** True when the entity-type is on the blacklist or in its tag (1.7.10 {@code CheckBlackList}). */
    public static boolean isBlacklisted(Entity entity) {
        return DEFLECT_BLACKLIST.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))
                || entity.getType().is(DEFLECT_BLACKLIST_TAG);
    }

    /**
     * Whether the shield works on this entity: a projectile that 1.7.10 knew as {@code IProjectile}. The hurting
     * projectiles (fireballs, wither skulls, wind and breeze charges), fireworks and fishing bobbers are not.
     */
    public static boolean isDeflectable(Entity entity) {
        return entity instanceof Projectile && !(entity instanceof AbstractHurtingProjectile)
                && !(entity instanceof FireworkRocketEntity) && !(entity instanceof FishingHook);
    }

    /** The box of the shield: 4 blocks below and to the west and north of the player, 3 above and to the east and south, plus the range. */
    public static AABB shieldBox(Vec3 player, int range) {
        return new AABB(player.x - (4 + range), player.y - (4 + range), player.z - (4 + range), player.x + (3 + range),
                player.y + (3 + range), player.z + (3 + range));
    }

    /**
     * Where a projectile goes: along its own direction, by twice its distance from the player, which carries it past.
     * A projectile at rest does not move.
     */
    public static Vec3 shieldShift(Vec3 motion, double distance) {
        if (motion.lengthSqr() == 0.0) return Vec3.ZERO;
        return motion.normalize().scale(distance * 2);
    }

    /**
     * 1.7.10 {@code ItemFocusDeflect.protectFromProjectiles(player, stack)}: every projectile in the box that the player
     * did not shoot and that is not blacklisted is moved along its flight by twice its distance from the player.
     *
     * @param range the enlarge upgrade of the focus (zero when no wand is involved, as for the Kami chestplate)
     */
    public static void protectFromProjectiles(Player player, int range) {
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class,
                shieldBox(player.position(), range))) {
            if (!isDeflectable(projectile) || isBlacklisted(projectile) || getOwner(projectile) == player) continue;
            Vec3 shift = shieldShift(projectile.getDeltaMovement(), projectile.position().distanceTo(player.position()));
            for (int i = 0; i < 6; i++) FociFx.whiteSparkle(player.level(), projectile);
            projectile.setPos(projectile.getX() + shift.x, projectile.getY() + shift.y, projectile.getZ() + shift.z);
        }
    }
}
