package thaumic.tinkerer.fabric.clientsmoke;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.fabric.clientsmoke.ItemVariants.Variant;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * The item-model overrides: every predicate that an item model names has a property function registered for the item (else the
 * override can never match), and every state in {@link ItemVariants} makes the item renderer pick a model other than the default's.
 * Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The second half is the one that fails when the JSON says {@code thaumictinkerer:enabled} and the code registers another name,
 * or writes the state in a component the property does not read: the item then stays in its default look.</p>
 */
final class OverridesCheck implements SmokeCheck {
    @Override
    public String name() {
        return "item_overrides";
    }

    @Override
    public void run(SmokeRun run) {
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        int predicates = 0;
        for (Entry<Item> entry : run.content().items()) {
            Optional<JsonObject> json = files.readJson(ModelFiles.modelFile(ResourceLocation.fromNamespaceAndPath(entry.id().getNamespace(), "item/" + entry.path())));
            if (json.isEmpty() || !json.get().has("overrides")) continue;
            ItemStack stack = new ItemStack(entry.value());
            for (JsonElement override : json.get().getAsJsonArray("overrides")) {
                for (String key : override.getAsJsonObject().getAsJsonObject("predicate").keySet()) {
                    predicates++;
                    if (ItemProperties.getProperty(stack, ResourceLocation.parse(key)) == null) {
                        run.report().fail(PHASE, "override_predicate_unregistered", entry.group(), "item:" + entry.path(),
                                "the item model has an override on '" + key + "' but no property function of that name is registered for the item: the override can never match");
                    } else {
                        run.report().pass(PHASE, "override_predicate", entry.group(), "item:" + entry.path(), "'" + key + "' has a property function");
                    }
                }
            }
        }
        for (Variant variant : ItemVariants.all()) {
            String group = run.content().groups().groupOf(variant.path());
            String target = "item:" + variant.path() + " (" + variant.note() + ")";
            try {
                BakedModel plain = run.client().getItemRenderer().getModel(new ItemStack(variant.stack().getItem()), null, null, 0);
                BakedModel changed = run.client().getItemRenderer().getModel(variant.stack(), null, null, 0);
                if (changed == run.client().getModelManager().getMissingModel()) {
                    run.report().fail(PHASE, "missing_model", group, target, "in this state the item renderer returns the missing model");
                } else if (changed == plain) {
                    run.report().fail(PHASE, "override_not_applied", group, target, "the item renderer picks the same model as for the default stack: the state does not switch the model");
                } else {
                    run.report().pass(PHASE, "override_applied", group, target, "the state switches the model");
                }
            } catch (RuntimeException e) {
                run.report().fail(PHASE, "item_model_exception", group, target, "ItemRenderer.getModel threw " + e);
            }
        }
        run.report().info(PHASE, name(), predicates + " override predicates and " + ItemVariants.all().size() + " item states looked at");
    }
}
