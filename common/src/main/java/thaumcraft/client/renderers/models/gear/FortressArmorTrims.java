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

/** Native atlas/material/pattern rendering, fitted to the fortress's original metal plates. */
public final class FortressArmorTrims {
    private static ModelFortressTrim model;
    private static long rendered;
    private static ResourceLocation lastSprite;
    private FortressArmorTrims() {}

    public record Diagnostics(long rendered, ResourceLocation lastSprite) {}
    public static Diagnostics diagnostics() { return new Diagnostics(rendered, lastSprite); }

    public static void render(PoseStack pose, MultiBufferSource buffers, ItemStack stack,
                              EquipmentSlot slot, int light, ModelFortressArmor source) {
        var trim = stack.get(DataComponents.TRIM);
        if (trim == null || !(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) return;
        ResourceLocation id = slot == EquipmentSlot.LEGS ? trim.innerTexture(armor.getMaterial()) : trim.outerTexture(armor.getMaterial());
        // Resolve the live atlas for every pass: native resource reloads replace its sprites.
        var sprite = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET).getSprite(id);
        if (model == null) model = ModelFortressTrim.create();
        source.copyPropertiesTo(model);
        model.copyPose(source);
        model.copyPlatePoses(source);
        model.showSlot(slot);
        model.renderToBuffer(pose, sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal()))),
                light, OverlayTexture.NO_OVERLAY);
        rendered++;
        lastSprite = id;
    }
}
