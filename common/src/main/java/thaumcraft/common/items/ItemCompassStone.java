package thaumcraft.common.items;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.WorldCoordinates;
import thaumcraft.common.lib.items.EquipmentRules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The Compass Stone (original ItemCompassStone), the sinister stone: a rare relic that points at the sinister nodes. While a sinister node
 * is in sight of its holder the stone is active, and it is that state the client shows with the active texture.
 *
 * <p>The original keeps the known sinister nodes in a static map that the world generator fills. The map is kept here as well; the entries
 * that are older than ten seconds are dropped, as the original does.
 */
public final class ItemCompassStone extends Item {
    /** The known sinister nodes and the moment they were seen, the original's static map. */
    public static final Map<WorldCoordinates, Long> SINISTER_NODES = new HashMap<>();

    public ItemCompassStone(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    /** Adds a sinister node to the map the compass stone reads. */
    public static void registerSinisterNode(WorldCoordinates pos) { SINISTER_NODES.put(pos, System.currentTimeMillis()); }

    /** Original ItemCompassStone.onUpdate: the stone looks for a sinister node on the client. */
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide && entity != null) isSinisterVisible(level, entity);
    }

    /**
     * Whether a sinister node is in sight of the holder. Original ItemCompassStone: the node has to be in the same dimension, within the
     * range and in front of the holder; the entries that are older than ten seconds are dropped.
     */
    public static boolean isSinisterVisible(Level level, Entity entity) {
        if (level == null || entity == null || !(entity instanceof LivingEntity living)) return false;
        boolean active = false;
        long cutoff = System.currentTimeMillis();
        Iterator<Map.Entry<WorldCoordinates, Long>> it = SINISTER_NODES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<WorldCoordinates, Long> entry = it.next();
            WorldCoordinates pos = entry.getKey();
            if (EquipmentRules.compassStoneExpired(entry.getValue(), cutoff)) {
                it.remove();
                continue;
            }
            if (!pos.dimension().equals(level.dimension())) continue;
            if (!visibleTo(living, pos)) continue;
            active = true;
        }
        return active;
    }

    /** Original EntityUtils.isVisibleTo: the node has to be within the range and in front of the holder. */
    private static boolean visibleTo(LivingEntity entity, WorldCoordinates pos) {
        Vec3 eyes = new Vec3(entity.getX(), entity.getEyePosition().y, entity.getZ());
        Vec3 target = new Vec3(pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D);
        Vec3 toTarget = target.subtract(eyes);
        double distance = toTarget.length();
        double dot = toTarget.normalize().dot(entity.getLookAngle().normalize());
        return EquipmentRules.compassStoneVisible(distance, EquipmentRules.COMPASS_STONE_RANGE, dot);
    }
}
