package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.mods.ChampionHooks;
import thaumcraft.common.lib.mods.ChampionModifier;
import thaumcraft.common.lib.mods.ChampionRules;
import thaumcraft.common.entities.EntitySpecialItem;

/**
 * Original TC4 {@code thaumcraft.common.lib.utils.EntityUtils}: the entity helpers of the wands, the pech and the
 * champion monsters.
 *
 * <p>The pointing helpers keep the original's own geometry: the eye of the searcher, the look vector, a padded
 * bounding box of every candidate and the nearest hit along that ray. The 1.7.10 bounding boxes are rebuilt
 * explicitly, because the native {@code AABB.expandTowards} only grows in one direction while the original grew
 * in every direction.
 */
public final class EntityUtils {
    /** Original {@code CHAMPION_HEALTH}: the original modifier UUID a62bef38-48cc-42a6-ac5e-ef913841c4fd. */
    public static final AttributeModifier CHAMPION_HEALTH = modifier("a62bef38-48cc-42a6-ac5e-ef913841c4fd", 30.0, AttributeModifier.Operation.ADD_VALUE);
    /** Original {@code CHAMPION_DAMAGE}: the original modifier UUID a340d2db-d881-4c25-ac62-f0ad14cd63b0. */
    public static final AttributeModifier CHAMPION_DAMAGE = modifier("a340d2db-d881-4c25-ac62-f0ad14cd63b0", 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    /** Original {@code BOLDBUFF}: the original modifier UUID 4b1edd33-caa9-47ae-a702-d86c05701037. */
    public static final AttributeModifier BOLDBUFF = modifier("4b1edd33-caa9-47ae-a702-d86c05701037", 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    /** Original {@code MIGHTYBUFF}: the original modifier UUID 7163897f-07f5-49b3-9ce4-b74beb83d2d3. */
    public static final AttributeModifier MIGHTYBUFF = modifier("7163897f-07f5-49b3-9ce4-b74beb83d2d3", 3.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    /** Original {@code HPBUFF}: the five health modifiers of the champion tiers. */
    public static final AttributeModifier[] HPBUFF = new AttributeModifier[]{
            modifier("54d621c1-dd4d-4b43-8bd2-5531c8875797", 50.0, AttributeModifier.Operation.ADD_VALUE),
            modifier("f51257dc-b7fa-4f7a-92d7-75d68e8592c4", 50.0, AttributeModifier.Operation.ADD_VALUE),
            modifier("3d6b2e42-4141-4364-b76d-0e8664bbd0bb", 50.0, AttributeModifier.Operation.ADD_VALUE),
            modifier("02c97a08-801c-4131-afa2-1427a6151934", 50.0, AttributeModifier.Operation.ADD_VALUE),
            modifier("0f354f6a-33c5-40be-93be-81b1338567f1", 50.0, AttributeModifier.Operation.ADD_VALUE)};
    /** Original {@code DMGBUFF}: the five damage modifiers of the champion tiers. */
    public static final AttributeModifier[] DMGBUFF = new AttributeModifier[]{
            modifier("534f8c57-929a-48cf-bbd6-0fd851030748", 0.5, AttributeModifier.Operation.ADD_VALUE),
            modifier("d317a76e-0e7c-4c61-acfd-9fa286053b32", 0.5, AttributeModifier.Operation.ADD_VALUE),
            modifier("ff462d63-26a2-4363-830e-143ed97e2a4f", 0.5, AttributeModifier.Operation.ADD_VALUE),
            modifier("cf1eb39e-0c67-495f-887c-0d3080828d2f", 0.5, AttributeModifier.Operation.ADD_VALUE),
            modifier("3cfab9da-2701-43d8-ac07-885f16fa4117", 0.5, AttributeModifier.Operation.ADD_VALUE)};

    /**
     * One champion modifier: the attribute modifier it applies and the name the original showed.
     *
     * <p>Forced 1.21.1 change: the original champion modifier table lived in the monster layer and was registered
     * here by its owner. The native port owns the table in {@link ChampionModifier}, so this record only remains
     * as the public shape of one table entry.</p>
     */
    public record ChampionMod(AttributeModifier attributeMod, String name) {}

    /** The original player's default eye height of 1.62, used by the client side ray prediction. */
    private static final float DEFAULT_EYE_HEIGHT = 1.62F;

    private EntityUtils() {}

    /**
     * The original champion modifier table, read from the native champion modifier list.
     *
     * <p>The 1.7 original registered the table here; the native port keeps the table in
     * {@link ChampionModifier}, so this method answers the same list without a second copy.</p>
     */
    public static List<ChampionMod> championMods() {
        List<ChampionMod> mods = new ArrayList<>();
        for (ChampionModifier modifier : ChampionModifier.mods) {
            mods.add(new ChampionMod(modifierAttribute(modifier), modifier.displayName));
        }
        return List.copyOf(mods);
    }

    /** The native attribute modifier of one original champion modifier. */
    private static AttributeModifier modifierAttribute(ChampionModifier modifier) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath("thaumcraft",
                modifier.attributeModifierId.toString()), modifier.attributeAmount, AttributeModifier.Operation.ADD_VALUE);
    }

    /**
     * The native form of the original's {@code AttributeModifier}: the original identified a modifier by a UUID,
     * 1.21.1 by a resource location. The original UUID is kept as the path so the identity survives the port.
     */
    private static AttributeModifier modifier(String uuid, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath("thaumcraft", uuid), amount, operation);
    }

    // ------------------------------------------------------------------- pointing

    /** Original {@code getPointedEntity(World, Entity, double, double, float)}. */
    public static Entity getPointedEntity(Level level, Entity entityplayer, double minrange, double range, float padding) {
        return getPointedEntity(level, entityplayer, minrange, range, padding, false);
    }

    /**
     * Original {@code getPointedEntity(World, Entity, double, double, float, boolean)}.
     *
     * <p>The original took the first candidate that was nearer than the last one, ignored every candidate that
     * was too close, that could not be collided with (unless the caller allowed that) or that was hidden by a
     * block.
     */
    public static Entity getPointedEntity(Level level, Entity entityplayer, double minrange, double range, float padding, boolean nonCollide) {
        if (level == null || entityplayer == null) return null;
        Entity pointedEntity = null;
        double d = range;
        Vec3 vec3d = new Vec3(entityplayer.getX(), entityplayer.getY() + entityplayer.getEyeHeight(), entityplayer.getZ());
        Vec3 vec3d1 = entityplayer.getLookAngle();
        Vec3 vec3d2 = vec3d.add(vec3d1.x * d, vec3d1.y * d, vec3d1.z * d);
        float f1 = padding;
        List<Entity> candidates = level.getEntities(entityplayer, expand(entityplayer.getBoundingBox(), vec3d1.x * d, vec3d1.y * d, vec3d1.z * d).inflate(f1));
        double d2 = 0.0;
        for (Entity entity : candidates) {
            if (belowMinRange(entity.distanceToSqr(entityplayer), minrange)) continue;
            if (!entity.canBeCollidedWith() && !nonCollide) continue;
            if (rayTrace(level, vec3d, new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ())) != null) continue;
            AABB box = entity.getBoundingBox().inflate(pickPadding(entity.getBbWidth()));
            java.util.Optional<Vec3> hit = box.clip(vec3d, vec3d2);
            if (box.contains(vec3d)) {
                if (!(0.0 < d2) && d2 != 0.0) continue;
                pointedEntity = entity;
                d2 = 0.0;
                continue;
            }
            if (hit.isEmpty()) continue;
            double d3 = vec3d.distanceTo(hit.get());
            if (!(d3 < d2) && d2 != 0.0) continue;
            pointedEntity = entity;
            d2 = d3;
        }
        return pointedEntity;
    }

    /**
     * Original {@code getPointedEntity(World, EntityPlayer, double, Class)}.
     *
     * <p>The original's class argument EXCLUDED that kind of entity from the result, exactly like the 1.12
     * reference names it {@code excludedClass}.
     */
    public static Entity getPointedEntity(Level level, Player entityplayer, double range, Class<?> excluded) {
        if (level == null || entityplayer == null) return null;
        Entity pointedEntity = null;
        double d = range;
        Vec3 vec3d = new Vec3(entityplayer.getX(), entityplayer.getY() + entityplayer.getEyeHeight(), entityplayer.getZ());
        Vec3 vec3d1 = entityplayer.getLookAngle();
        Vec3 vec3d2 = vec3d.add(vec3d1.x * d, vec3d1.y * d, vec3d1.z * d);
        float f1 = 1.1f;
        List<Entity> candidates = level.getEntities(entityplayer, expand(entityplayer.getBoundingBox(), vec3d1.x * d, vec3d1.y * d, vec3d1.z * d).inflate(f1));
        double d2 = 0.0;
        for (Entity entity : candidates) {
            if (!entity.canBeCollidedWith()) continue;
            if (rayTrace(level, vec3d, new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ())) != null) continue;
            if (excluded != null && excluded.isInstance(entity)) continue;
            AABB box = entity.getBoundingBox().inflate(pickPadding(entity.getBbWidth()));
            java.util.Optional<Vec3> hit = box.clip(vec3d, vec3d2);
            if (box.contains(vec3d)) {
                if (!(0.0 < d2) && d2 != 0.0) continue;
                pointedEntity = entity;
                d2 = 0.0;
                continue;
            }
            if (hit.isEmpty()) continue;
            double d3 = vec3d.distanceTo(hit.get());
            if (!(d3 < d2) && d2 != 0.0) continue;
            pointedEntity = entity;
            d2 = d3;
        }
        return pointedEntity;
    }

    /**
     * The original {@code Entity.getCollisionBorderSize()} of 1.7.10, which is half the width of the entity.
     *
     * <p>Original {@code EntityUtils.getPointedEntity} used it as the padding of the candidate box
     * ({@code Math.max(0.8F, entity.getCollisionBorderSize())}), so the pad of an ordinary mob is the 0.8 of the
     * original and only a very wide entity grows the box.</p>
     */
    static float pickPadding(float width) {
        return Math.max(0.8f, width * 0.5f);
    }

    /**
     * The original minimum-distance test of {@code EntityUtils.getPointedEntity}.
     *
     * <p>The original compared the LINEAR distance ({@code Entity.getDistanceToEntity}, the square root of the
     * squared distance) with the minimum range; comparing the squared distance with the range instead would
     * refuse every candidate that lies between the square root of the range and the range itself.</p>
     */
    static boolean belowMinRange(double squaredDistance, double minRange) {
        return squaredDistance < minRange * minRange;
    }

    /**
     * The original's mid-body point of the target entity: {@code boundingBox.minY + height / 2}.
     *
     * <p>The bottom of the box is the original's anchor, not its top: the top would move the target a whole
     * entity height too high.</p>
     */
    static double[] midBody(double x, double minY, double height, double z) {
        return new double[]{x, minY + height / 2.0f, z};
    }

    /**
     * The original's eye point of the searching entity: {@code boundingBox.minY + eyeHeight}.
     */
    static double[] eyePoint(double x, double minY, double eyeHeight, double z) {
        return new double[]{x, minY + eyeHeight, z};
    }

    /**
     * The original's symmetric box growth: the 1.7.10 {@code AxisAlignedBB.expand} grew in every direction,
     * while the native {@code AABB.expandTowards} only grows towards the vector.
     */
    private static AABB expand(AABB box, double x, double y, double z) {
        return new AABB(box.minX - x, box.minY - y, box.minZ - z, box.maxX + x, box.maxY + y, box.maxZ + z);
    }

    // -------------------------------------------------------------------- vision

    /** Original {@code canEntityBeSeen(Entity, TileEntity)}. */
    public static boolean canEntityBeSeen(Entity entity, BlockEntity te) {
        if (entity == null || te == null || te.getLevel() == null) return false;
        return rayTrace(te.getLevel(), new Vec3(te.getBlockPos().getX() + 0.5, te.getBlockPos().getY() + 1.25, te.getBlockPos().getZ() + 0.5),
                new Vec3(entity.getX(), entity.getY(), entity.getZ())) == null;
    }

    /** Original {@code canEntityBeSeen(Entity, double, double, double)}. */
    public static boolean canEntityBeSeen(Entity entity, double x, double y, double z) {
        if (entity == null || entity.level() == null) return false;
        return rayTrace(entity.level(), new Vec3(x, y, z), new Vec3(entity.getX(), entity.getY(), entity.getZ())) == null;
    }

    /** Original {@code canEntityBeSeen(Entity, Entity)}. */
    public static boolean canEntityBeSeen(Entity entity, Entity other) {
        if (entity == null || other == null || entity.level() == null) return false;
        return rayTrace(entity.level(), new Vec3(entity.getX(), entity.getY(), entity.getZ()),
                new Vec3(other.getX(), other.getY(), other.getZ())) == null;
    }

    /**
     * The native block ray trace of the original's {@code World.getIntercept} for the vision tests.
     *
     * <p>The native trace uses the collision shape of the blocks and never stops at a fluid, which is the 1.21.1
     * form of "is the line of sight blocked".
     */
    private static BlockHitResult rayTrace(Level level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
    }

    /**
     * Original {@code setRecentlyHit(EntityLivingBase, int)}.
     *
     * <p>The original wrote the private {@code recentlyHit} field through reflection. 1.21.1 counts the same ticks
     * in the public {@code hurtTime} field.
     */
    public static void setRecentlyHit(LivingEntity ent, int hit) {
        if (ent == null) return;
        ent.hurtTime = hit;
    }

    /** Original {@code getRecentlyHit(EntityLivingBase)}. */
    public static int getRecentlyHit(LivingEntity ent) {
        return ent == null ? 0 : ent.hurtTime;
    }

    /** Original {@code getMovingObjectPositionFromPlayer(World, EntityPlayer, boolean)}. */
    public static BlockHitResult getMovingObjectPositionFromPlayer(Level level, Player player, boolean par3) {
        if (level == null || player == null) return null;
        float f = 1.0f;
        float f1 = player.xRotO + (player.getXRot() - player.xRotO) * f;
        float f2 = player.yRotO + (player.getYRot() - player.yRotO) * f;
        double d0 = player.xo + (player.getX() - player.xo) * f;
        double d1 = player.yo + (player.getY() - player.yo) * f + (level.isClientSide ? player.getEyeHeight() - DEFAULT_EYE_HEIGHT : player.getEyeHeight());
        double d2 = player.zo + (player.getZ() - player.zo) * f;
        Vec3 vec3 = new Vec3(d0, d1, d2);
        float f3 = (float) Math.cos(-f2 * ((float) Math.PI / 180) - (float) Math.PI);
        float f4 = (float) Math.sin(-f2 * ((float) Math.PI / 180) - (float) Math.PI);
        float f5 = -(float) Math.cos(-f1 * ((float) Math.PI / 180));
        float f6 = (float) Math.sin(-f1 * ((float) Math.PI / 180));
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0;
        if (player instanceof net.minecraft.server.level.ServerPlayer server) d3 = server.blockInteractionRange();
        Vec3 vec31 = vec3.add(f7 * d3, f6 * d3, f8 * d3);
        return level.clip(new ClipContext(vec3, vec31,
                par3 ? ClipContext.Block.OUTLINE : ClipContext.Block.COLLIDER,
                par3 ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY, player));
    }

    // ------------------------------------------------------------------ neighbours

    /** Original {@code getEntitiesInRange(World, double, double, double, Entity, Class, double)}. */
    public static <T extends Entity> List<T> getEntitiesInRange(Level level, double x, double y, double z, Entity entity, Class<T> clazz, double range) {
        List<T> out = new ArrayList<>();
        if (level == null || clazz == null) return out;
        AABB box = new AABB(x - range, y - range, z - range, x + range, y + range, z + range);
        for (T candidate : level.getEntitiesOfClass(clazz, box)) {
            // The original skipped the entities with the same world id, which is the searching entity itself.
            if (entity != null && entity == candidate) continue;
            out.add(candidate);
        }
        return out;
    }

    /** Original {@code isVisibleTo(float, Entity, Entity, float)}. */
    public static boolean isVisibleTo(float fov, Entity ent, Entity ent2, float range) {
        if (ent == null || ent2 == null) return false;
        double[] x = midBody(ent2.getX(), ent2.getBoundingBox().minY, ent2.getBbHeight(), ent2.getZ());
        double[] t = eyePoint(ent.getX(), ent.getBoundingBox().minY, ent.getEyeHeight(), ent.getZ());
        Vec3 q = ent.getLookAngle();
        q = new Vec3(q.x * range, q.y * range, q.z * range);
        Vec3 l = q.add(ent.getX(), ent.getBoundingBox().minY + ent.getEyeHeight(), ent.getZ());
        double[] b = new double[]{l.x, l.y, l.z};
        return Utils.isLyingInCone(x, t, b, fov);
    }

    /** Original {@code isVisibleTo(float, Entity, double, double, double, float)}. */
    public static boolean isVisibleTo(float fov, Entity ent, double xx, double yy, double zz, float range) {
        if (ent == null) return false;
        double[] x = new double[]{xx, yy, zz};
        double[] t = eyePoint(ent.getX(), ent.getBoundingBox().minY, ent.getEyeHeight(), ent.getZ());
        Vec3 q = ent.getLookAngle();
        q = new Vec3(q.x * range, q.y * range, q.z * range);
        Vec3 l = q.add(ent.getX(), ent.getBoundingBox().minY + ent.getEyeHeight(), ent.getZ());
        double[] b = new double[]{l.x, l.y, l.z};
        return Utils.isLyingInCone(x, t, b, fov);
    }

    // ----------------------------------------------------------------------- drops

    /** Original {@code entityDropSpecialItem(Entity, ItemStack, float)}. */
    public static EntitySpecialItem entityDropSpecialItem(Entity entity, ItemStack stack, float dropheight) {
        if (entity == null || stack == null || stack.isEmpty()) return null;
        Level level = entity.level();
        if (!(level instanceof ServerLevel server)) return null;
        EntitySpecialItem item = ThaumcraftContent.SPECIAL_ITEM.get().create(server);
        if (item == null) return null;
        item.setPos(entity.getX(), entity.getY() + dropheight, entity.getZ());
        item.setItem(stack.copy());
        item.setPickUpDelay(10);
        item.setDeltaMovement(0.1f, 0.0f, 0.0f);
        server.addFreshEntity(item);
        return item;
    }

    // ------------------------------------------------------------------- champions

    /**
     * Original {@code makeChampion(EntityMob, boolean)}.
     *
     * <p>The original picked a random champion modifier, made a creeper always the first modifier, applied the
     * health and damage modifiers, healed the monster to 25 and renamed it. 1.21.1 keeps the modifiers as
     * permanent attribute modifiers and the health as the entity's health value.
     *
     * <p>Forced 1.21.1 change: the original kept the modifier id in the custom {@code tc.mobmod} attribute, an
     * attribute 1.21.1 cannot register. The native port keeps the modifier id in the mob's own champion field
     * ({@link ChampionHooks}), so this method writes the id there and lets {@link ChampionRules} apply the
     * modifiers; the two systems therefore always agree on the champion state of a mob.</p>
     */
    public static void makeChampion(LivingEntity entity, boolean persist) {
        if (!(entity instanceof Monster mob)) return;
        int type = ChampionRules.Adapter.makeChampion(mob, mob.getRandom(), mob instanceof Creeper);
        ChampionHooks.setChampionModifier(mob, type);
        if (persist) mob.setPersistenceRequired();
    }

    /** The champion modifier of a given type applied to a monster. */
    public static void makeChampion(LivingEntity entity, int type, boolean persist) {
        if (!(entity instanceof Monster mob)) return;
        if (ChampionModifier.byId(type) == null) return;
        ChampionHooks.setChampionModifier(mob, type);
        ChampionRules.Adapter.promote(mob, type);
        if (persist) mob.setPersistenceRequired();
    }

    /**
     * The champion modifier id of a mob, or {@link ChampionHooks#NOT_ROLLED} when the mob was never rolled.
     *
     * <p>This is the answer the original {@code EntityUtils.CHAMPION_MOD} attribute gave: the 1.7 original kept
     * the modifier id in a custom attribute of every {@code EntityMob}, the native port keeps it in the mob's own
     * champion field and mirrors it into the synced entity data.</p>
     */
    public static int championId(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.Mob mob)) return ChampionHooks.NOT_ROLLED;
        return ChampionHooks.championId(mob);
    }

    /** Whether the mob carries a champion modifier. */
    public static boolean isChampion(LivingEntity entity) {
        return championId(entity) >= 0;
    }

    /** The UUID form of a modifier id, for the callers that still speak the original identifiers. */
    public static UUID modifierUuid(AttributeModifier modifier) {
        String path = modifier.id().getPath();
        try {
            return UUID.fromString(path);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
