package thaumcraft.neoforge;

import java.util.function.Supplier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import thaumcraft.common.items.wands.WandUseCapture;

/**
 * NeoForge half of {@link WandUseCapture}: the wand's block triggers run with block snapshot capture off,
 * as the original's {@code onItemUseFirst} ran before Forge began capturing. Capture is restored afterwards,
 * so snapshots captured by the surrounding use stay in place.
 */
public final class NeoForgeWandUseCapture {
    private NeoForgeWandUseCapture() {}

    public static void init() { WandUseCapture.install(NeoForgeWandUseCapture::outside); }

    private static InteractionResult outside(Level level, Supplier<InteractionResult> action) {
        if (level.isClientSide || !level.captureBlockSnapshots) return action.get();
        level.captureBlockSnapshots = false;
        try {
            return action.get();
        } finally {
            level.captureBlockSnapshots = true;
        }
    }
}
