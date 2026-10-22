package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import thaumcraft.common.entities.projectile.EntityPrimalArrow;

/**
 * The primal arrow of the bone bow, drawn with the port's own {@link RenderPrimalArrow} and
 * the two original entity values it reads.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/entity/RenderPrimalArrow.java:39} reads the
 * primal aspect of the arrow ({@code arrow.type}) for the colour of the wisp trail and
 * {@code :64} reads {@code arrow.field_70252_j}, the number of ticks the arrow already
 * spent in the ground, for the fade of the arrow. The original entity raises that counter
 * while the arrow sticks in the same block and dies at a hundred
 * (oracle {@code thaumcraft/common/entities/projectile/EntityPrimalArrow.java:163-165}), which
 * is the native {@code inGroundTime} of the port's arrow.</p>
 */
public final class RenderPrimalArrowEntity extends RenderPrimalArrow<EntityPrimalArrow> {
    public RenderPrimalArrowEntity(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Original EntityPrimalArrow.type: the primal aspect index of the arrow. */
    @Override protected int primalType(EntityPrimalArrow arrow) {
        return arrow.getArrowType();
    }

    /** Original EntityPrimalArrow.field_70252_j: the ticks the arrow spent in the ground. */
    @Override protected int spentLife(EntityPrimalArrow arrow) {
        return arrow.spentLife();
    }
}
