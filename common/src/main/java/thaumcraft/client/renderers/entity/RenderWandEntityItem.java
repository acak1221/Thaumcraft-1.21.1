package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Legacy 1.12 RenderWandEntityItem: the vanilla item entity rendering with the wand
 * special item renderer context opened around it. The native port draws wands with
 * native baked item models, so there is no special renderer context left to open; this
 * renderer therefore keeps the vanilla item entity rendering unchanged.
 */
public final class RenderWandEntityItem extends ItemEntityRenderer {
    public RenderWandEntityItem(EntityRendererProvider.Context context) {
        super(context);
    }
}
