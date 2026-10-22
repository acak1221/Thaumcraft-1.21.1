package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import thaumcraft.common.entities.projectile.EntityGolemOrb;

/**
 * Original TC4 RenderElectricOrb drawn for the golem orb: the same additive billboard of
 * the particle sheet, but the red orb uses the second row of the four frame strip.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/entity/RenderElectricOrb.java:45-47}: the row
 * is {@code 0.875} and only an {@code EntityGolemOrb} whose {@code red} flag is set moves it
 * to {@code 0.75}. The original registered the golem orb and the shock orb with the same
 * renderer (oracle {@code thaumcraft/client/ClientProxy.java:476} and {@code :478}).</p>
 */
public final class RenderGolemOrb extends RenderElectricOrb<EntityGolemOrb> {
    public RenderGolemOrb(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Original EntityGolemOrb.red: the red orb is the second row of the strip. */
    @Override protected boolean isRed(EntityGolemOrb entity) {
        return entity.isRed();
    }
}
