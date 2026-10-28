/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.common.core.helper.EnumMobAspect;
import thaumic.tinkerer.common.item.ItemMobDisplay;

/**
 * Draws the mob of a {@code mob_display} item in place of the item (the 1.7.10 {@code IItemRenderer}). It has the
 * signature of a builtin item renderer of both loaders ({@code BuiltinItemRendererRegistry} of Fabric,
 * {@code IClientItemExtensions#getCustomRenderer} of NeoForge); the loader hook is a request to the lead (see
 * docs/api-requests/Machines.md) because the common code has no registry for it.
 */
public final class RenderMobDisplay {

    private static final EnumMap<EnumMobAspect, Mob> CACHE = new EnumMap<>(EnumMobAspect.class);

    private RenderMobDisplay() {}

    /** Forget the mobs (they belong to the level that was left). Call when the client leaves a world. */
    public static void clearCache() {
        CACHE.clear();
    }

    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        if (!(stack.getItem() instanceof ItemMobDisplay item)) return;
        EnumMobAspect aspect = item.getEntityType(stack);
        if (aspect == null) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Mob entity = CACHE.get(aspect);
        if (entity == null || entity.level() != level) {
            entity = aspect.createEntity(level);
            if (entity == null) return;
            CACHE.put(aspect, entity);
        }

        float scale = aspect.getScale();
        pose.pushPose();
        try {
            // a builtin model is drawn with the corner of its cell at the origin: the mob stands in the middle
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(-30.0F));
            pose.scale(scale, scale, scale);
            pose.translate(0, (-entity.getBbHeight() / 2) + aspect.getVerticalOffset(), 0);
            Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0, 0, 0, 0, 0, pose, buffers, light);
        } finally {
            pose.popPose();
        }
    }
}
