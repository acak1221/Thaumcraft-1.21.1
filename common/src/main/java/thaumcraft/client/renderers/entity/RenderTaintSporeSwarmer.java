package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.ModelTaintSporeSwarmer;
import thaumcraft.common.entities.monster.EntityTaintSporeSwarmer;

public final class RenderTaintSporeSwarmer extends MobRenderer<EntityTaintSporeSwarmer,ModelTaintSporeSwarmer> {
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/taint_spore.png");
    public RenderTaintSporeSwarmer(EntityRendererProvider.Context context){super(context,new ModelTaintSporeSwarmer(),.25F);}
    @Override public ResourceLocation getTextureLocation(EntityTaintSporeSwarmer entity){return SKIN;}
    @Override protected float getFlipDegrees(EntityTaintSporeSwarmer entity){return 0;}
    @Override protected RenderType getRenderType(EntityTaintSporeSwarmer entity,boolean visible,boolean translucent,boolean glow){return visible||translucent?RenderType.entityTranslucent(SKIN):glow?RenderType.outline(SKIN):null;}
}
