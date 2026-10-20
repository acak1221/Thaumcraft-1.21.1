package thaumcraft.client.renderers.entity;

import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityInhabitedZombie;

/**
 * Original TC4 RenderInhabitedZombie: the vanilla zombie mesh with the cultist plate
 * sheet. The original swapped in the 1.7 zombie villager mesh for a child zombie; the
 * native zombie model scales its own child variant instead.
 */
public final class RenderInhabitedZombie extends MobRenderer<EntityInhabitedZombie, ZombieModel<EntityInhabitedZombie>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/czombie.png");

    public RenderInhabitedZombie(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), .5F);
    }

    @Override public ResourceLocation getTextureLocation(EntityInhabitedZombie zombie) { return SKIN; }
}
