package thaumic.tinkerer.fabric.clientsmoke;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * The model the item renderer picks for the default stack of every item of the add-on is not the missing model. Test-only
 * code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>{@code ItemRenderer.getModel} applies the model overrides (the predicates of the foci, tools and talismans) at their
 * default values, so the same call the game makes for an item in a slot is made here. The model JSON is also looked for by
 * name, to tell a missing file from a model that exists but fails to bake.</p>
 */
final class ItemModelsCheck implements SmokeCheck {
    @Override
    public String name() {
        return "item_models";
    }

    @Override
    public void run(SmokeRun run) {
        BakedModel missing = run.client().getModelManager().getMissingModel();
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        for (Entry<Item> entry : run.content().items()) {
            String target = "item:" + entry.path();
            ResourceLocation modelFile = ModelFiles.modelFile(ResourceLocation.fromNamespaceAndPath(entry.id().getNamespace(), "item/" + entry.path()));
            boolean fileThere = files.exists(modelFile);
            try {
                BakedModel model = run.client().getItemRenderer().getModel(new ItemStack(entry.value()), null, null, 0);
                if (model == missing) {
                    run.report().fail(PHASE, "missing_model", entry.group(), target,
                            "the item renderer returns the missing model; " + modelFile + (fileThere ? " exists (does not bake)" : " does not exist"));
                } else if (model.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                    run.report().fail(PHASE, "missing_texture", entry.group(), target, "the item model's particle sprite is the missing-texture sprite"
                            + (model.isCustomRenderer() ? " (a custom-renderer item whose model declares no particle texture: breaking or eating it shows the purple square)" : ""));
                } else {
                    run.report().pass(PHASE, "item_model", entry.group(), target, (model.isCustomRenderer() ? "custom renderer; " : "") + "model baked");
                }
            } catch (RuntimeException e) {
                run.report().fail(PHASE, "item_model_exception", entry.group(), target, "ItemRenderer.getModel threw " + e);
            }
        }
        run.report().info(PHASE, name(), run.content().items().size() + " items looked at");
    }
}
