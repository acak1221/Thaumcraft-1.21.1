package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Original TC4 RenderFollowingItem: the item alone, without the special item's star.
 * The original renderer built a temporary EntityItem and handed it to the vanilla item
 * renderer; the native port keeps the vanilla item entity rendering directly.
 *
 * <p>Like {@link RenderSpecialItem} the native renderer accepts any {@link ItemEntity}:
 * the 1.21.1 item pickup animation renders a copy of the picked entity that keeps the
 * entity type but not the entity class, so the copy is a plain item entity.
 */
public final class RenderFollowingItem extends EntityRenderer<ItemEntity> {
    private final ItemEntityRenderer itemRenderer;

    public RenderFollowingItem(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .15F;
        itemRenderer = new ItemEntityRenderer(context);
    }

    @Override public ResourceLocation getTextureLocation(ItemEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/potions.png");
    }

    @Override public void render(ItemEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        itemRenderer.render(entity, yaw, partial, pose, buffers, light);
    }
}
