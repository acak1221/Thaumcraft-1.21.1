package thaumcraft.common.lib.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.equipment.ElementalSwordRules;
import thaumcraft.common.items.equipment.ItemElementalSword;
import thaumcraft.common.mixin.LivingEntityAttackStrengthAccess;

/**
 * The Sword of the Zephyr's sweep (original {@code onLeftClickEntity}): before a player holding it strikes a living target, every other
 * living mob within 1.2 x 1.1 x 1.2 blocks of the target's box is struck first, each by an ordinary blow of the player (the weapon's damage,
 * enchantments and one use each). Players, the player's own golems and tamed animals, the dead and anything that is not a mob are spared.
 * The blows are the player's own {@code attack}, so the 1.9+ attack cooldown is kept as it was for the blow that follows, and a nested blow
 * never sweeps again.
 */
public final class ElementalSwordSweep {
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private static final SoundEvent SWING = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "swing"));

    private ElementalSwordSweep() {}

    /** Whether the player's sweep spares this entity (the target itself is handled by the caller). */
    public static boolean spared(Player player, Entity other) {
        return !(other instanceof Mob) || !other.isAlive()
                || other instanceof EntityGolemBase golem && golem.getOwnerName().equals(player.getGameProfile().getName())
                || other instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID());
    }

    /** Called at the head of {@code Player.attack}. */
    public static void before(Player player, Entity target) {
        if (!(player.level() instanceof ServerLevel level) || ACTIVE.get() != null) return;
        if (!(player.getMainHandItem().getItem() instanceof ItemElementalSword) || !target.isAlive()) return;
        List<Entity> struck = new ArrayList<>();
        for (Entity other : level.getEntities(player, target.getBoundingBox().inflate(ElementalSwordRules.SWEEP_X, ElementalSwordRules.SWEEP_Y, ElementalSwordRules.SWEEP_Z))) {
            if (other != target && !spared(player, other)) struck.add(other);
        }
        if (struck.isEmpty()) return;
        var cooldown = (LivingEntityAttackStrengthAccess) player;
        int ticker = cooldown.thaumcraft$getAttackStrengthTicker();
        int hits = 0;
        ACTIVE.set(Boolean.TRUE);
        try {
            for (Entity other : struck) {
                ItemStack held = player.getMainHandItem();
                if (!(held.getItem() instanceof ItemElementalSword) || !other.isAlive()) break;
                player.attack(other);
                cooldown.thaumcraft$setAttackStrengthTicker(ticker);
                ++hits;
            }
        } finally {
            ACTIVE.remove();
            cooldown.thaumcraft$setAttackStrengthTicker(ticker);
        }
        if (hits > 0) level.playSound(null, target.getX(), target.getY(), target.getZ(), SWING, SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
    }
}
