package thaumic.tinkerer.fabric.clientsmoke;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Every particle type the add-on registers has a particle JSON with textures that exist. Test-only code of the port (no
 * 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The add-on's 1.7.10 effects are drawn with the Thaumcraft API's effect renderer or with vanilla particles, so the
 * registry may hold no particle of its own; that is stated, not passed silently.</p>
 */
final class ParticlesCheck implements SmokeCheck {
    @Override
    public String name() {
        return "particles";
    }

    @Override
    public void run(SmokeRun run) {
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        List<String> types = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            if (!TinkererContent.ours(id)) continue;
            types.add(id.getPath());
            String group = run.content().groups().groupOf(id.getPath());
            Optional<JsonObject> json = files.readJson(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "particles/" + id.getPath() + ".json"));
            if (json.isEmpty()) {
                run.report().fail(PHASE, "missing_particle_definition", group, "particle:" + id.getPath(), "particles/" + id.getPath() + ".json does not exist");
                continue;
            }
            boolean ok = true;
            if (json.get().has("textures")) {
                for (JsonElement texture : json.get().getAsJsonArray("textures")) {
                    ResourceLocation sprite = ResourceLocation.parse(texture.getAsString());
                    if (!files.exists(ModelFiles.textureFile(ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "particle/" + sprite.getPath())))) {
                        ok = false;
                        run.report().fail(PHASE, "missing_texture", group, "particle:" + id.getPath(), "particle texture " + sprite + " has no file");
                    }
                }
            }
            if (ok) run.report().pass(PHASE, "particle", group, "particle:" + id.getPath(), "definition and textures exist");
        }
        if (types.isEmpty()) run.report().info(PHASE, name(), "the add-on registers no particle type: nothing to check");
        else run.report().inventory("particle_types", types);
    }
}
