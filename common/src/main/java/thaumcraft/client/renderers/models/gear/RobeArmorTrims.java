package thaumcraft.client.renderers.models.gear;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** Native atlas trims over the original dyed robe, with its animated panel poses. */
public final class RobeArmorTrims {
    private static ModelRobeTrim armoured, cloth;
    private static long rendered;
    private static ResourceLocation lastSprite;
    private RobeArmorTrims() {}

    public record Diagnostics(long rendered, ResourceLocation lastSprite) {}
    public static Diagnostics diagnostics() { return new Diagnostics(rendered, lastSprite); }

    public static void render(PoseStack pose, MultiBufferSource buffers, ItemStack stack,
                              EquipmentSlot slot, int light, ModelRobe source) {
        var trim = stack.get(DataComponents.TRIM);
        if (trim == null || !(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) return;
        ResourceLocation id = slot == EquipmentSlot.LEGS ? trim.innerTexture(armor.getMaterial()) : trim.outerTexture(armor.getMaterial());
        var sprite = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET).getSprite(id);
        ModelRobeTrim model;
        if (source.armoured()) {
            if (armoured == null) armoured = ModelRobeTrim.create(true);
            model = armoured;
        } else {
            if (cloth == null) cloth = ModelRobeTrim.create(false);
            model = cloth;
        }
        source.copyPropertiesTo(model);
        model.copyPose(source);
        model.copyPanelPoses(source);
        model.showSlot(slot);
        model.renderToBuffer(pose, sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal()))),
                light, OverlayTexture.NO_OVERLAY);
        rendered++;
        lastSprite = id;
    }
}
