package thaumic.tinkerer.fabric.clientsmoke;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/**
 * The worn-armor textures of every armor item of the add-on (the layer the piece draws: outer, or inner for leggings) and the
 * status-effect icons of its mob effects. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>Neither appears in a block or item model, so the model check does not see them: a missing armor layer shows as the
 * missing-texture pattern on the player's body, a missing effect icon as a purple square in the inventory.</p>
 */
final class ArmorAndEffectTexturesCheck implements SmokeCheck {
    @Override
    public String name() {
        return "armor_and_effect_textures";
    }

    @Override
    public void run(SmokeRun run) {
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        int layers = 0;
        // the layers a piece really draws: leggings use the inner layer, every other piece the outer one
        for (var item : run.content().items()) {
            if (!(item.value() instanceof ArmorItem armor)) continue;
            boolean inner = armor.getType() == ArmorItem.Type.LEGGINGS;
            for (ArmorMaterial.Layer layer : armor.getMaterial().value().layers()) {
                ResourceLocation texture = layer.texture(inner);
                layers++;
                if (files.exists(texture)) run.report().pass(PHASE, "armor_layer", item.group(), "item:" + item.path(), texture + " exists");
                else run.report().fail(PHASE, "missing_texture", item.group(), "item:" + item.path(), "worn-armor layer " + texture + " does not exist");
            }
        }
        int icons = 0;
        for (ResourceLocation id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            if (!TinkererContent.ours(id)) continue;
            Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElseThrow();
            icons++;
            String group = run.content().groups().groupOf(id.getPath());
            if (run.client().getMobEffectTextures().get(holder).contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                run.report().fail(PHASE, "missing_texture", group, "effect:" + id.getPath(), "the status-effect icon textures/mob_effect/" + id.getPath() + ".png is not in the effect atlas");
            } else {
                run.report().pass(PHASE, "effect_icon", group, "effect:" + id.getPath(), "icon is in the effect atlas");
            }
        }
        run.report().info(PHASE, name(), layers + " armor layer textures and " + icons + " effect icons looked at");
    }
}
