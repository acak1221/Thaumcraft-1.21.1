package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.EntitySpecialItem;

/**
 * Original TC4 RenderFollowingItem: the item alone, without the special item's star.
 * The original renderer built a temporary EntityItem and handed it to the vanilla item
 * renderer; the native port keeps the vanilla item entity rendering directly.
 */
public final class RenderFollowingItem extends EntityRenderer<EntitySpecialItem> {
    private final ItemEntityRenderer itemRenderer;

    public RenderFollowingItem(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .15F;
        itemRenderer = new ItemEntityRenderer(context);
    }

    @Override public ResourceLocation getTextureLocation(EntitySpecialItem entity) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/potions.png");
    }

    @Override public void render(EntitySpecialItem entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        itemRenderer.render(entity, yaw, partial, pose, buffers, light);
    }
}
