package thaumcraft.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.world.OuterLandsRules;

/**
 * The client effects of the TC4 Outer Lands.
 *
 * <p>Minecraft 1.21.1 asks the client for the {@link DimensionSpecialEffects} of a dimension
 * with the effects location of the dimension type, and a dimension without an entry leaves the
 * client without a sky, a fog and a lightmap. The original 1.7 dimension provider supplied them
 * itself: no sky at all, a constant fog of the original colour and a constant lightmap.
 *
 * <p>Oracle sources:
 * <ul>
 *   <li>{@code WorldProviderOuter.func_76571_f} - the cloud height of the Outer Lands.</li>
 *   <li>{@code WorldProviderOuter.func_76572_b} - the Outer Lands has no sky.</li>
 *   <li>{@code WorldProviderOuter.func_76562_b} - the fog colour 0xA080A0 scaled by 0.15.</li>
 *   <li>{@code WorldProviderOuter.func_76568_b} - the Outer Lands always shows fog.</li>
 * </ul>
 */
public final class OuterLandsDimensionEffects extends DimensionSpecialEffects {
    /** The single instance of the Outer Lands effects. */
    public static final OuterLandsDimensionEffects INSTANCE = new OuterLandsDimensionEffects();

    private OuterLandsDimensionEffects() {
        // Oracle WorldProviderOuter.func_76571_f and func_76572_b: the cloud height of the
        // Outer Lands, no sky and no ground. The Outer Lands has no sky light and no ambient
        // light, so its lightmap is constant like the lightmap of the nether.
        super(OuterLandsRules.CLOUD_HEIGHT, false, SkyType.NONE, true, true);
    }

    /** Oracle WorldProviderOuter.func_76562_b - the fog colour 0xA080A0 scaled by 0.15. */
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        int packed = OuterLandsRules.packedFogColor();
        return new Vec3((packed >> 16 & 0xFF) / 255.0F, (packed >> 8 & 0xFF) / 255.0F, (packed & 0xFF) / 255.0F);
    }

    /** Oracle WorldProviderOuter.func_76568_b - the Outer Lands always shows fog. */
    @Override public boolean isFoggyAt(int x, int z) {
        return true;
    }
}
