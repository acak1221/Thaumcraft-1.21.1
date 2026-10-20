package thaumcraft.common.lib.loot;

import java.util.function.DoubleSupplier;
import net.minecraft.world.phys.Vec3;

/** Original EntityItem constructor uses Math.random, never the world's loot RNG. */
public final class LootBagRules {
    private LootBagRules() {}
    public static Vec3 motion(DoubleSupplier random) {
        // Original hover phase and yaw precede the two physical scatter draws.
        random.getAsDouble(); random.getAsDouble();
        return new Vec3((double)(float)(random.getAsDouble()*(double).2F-(double).1F),
                (double).2F,(double)(float)(random.getAsDouble()*(double).2F-(double).1F));
    }
}
