package thaumic.tinkerer.fabric.clientsmoke;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * Every texture a model of the add-on refers to exists as a file and is in the block atlas, every face variable is defined
 * and every parent exists. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not
 * distributed.
 *
 * <p>The models are found the way the game finds them: blockstate JSON to the models of its variants, item model JSON plus
 * the models of its overrides, then the {@code parent} chain with the {@code textures} maps merged child over parent.
 * Problems are named by the registry path of the block or item that reaches the model. The models that no block or item
 * reaches are checked too and reported as "unreferenced model". The atlas sources the add-on declares in
 * {@code atlases/blocks.json} must be stitched as well.</p>
 */
final class ModelTexturesCheck implements SmokeCheck {
    @Override
    public String name() {
        return "model_textures";
    }

    @Override
    public void run(SmokeRun run) {
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        TextureAtlas atlas = run.client().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        Set<ResourceLocation> reached = new LinkedHashSet<>();
        int textures = 0;
        for (Entry<net.minecraft.world.level.block.Block> block : run.content().blocks()) {
            ResourceLocation id = block.id();
            List<ResourceLocation> models = files.blockstateModels(id);
            if (models.isEmpty()) {
                // the missing blockstate is reported by BlockModelsCheck; here only a blockstate without any model matters
                if (files.exists(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "blockstates/" + id.getPath() + ".json"))) {
                    run.report().fail(PHASE, "missing_model", block.group(), "block:" + block.path(), "the blockstate names no model");
                }
                continue;
            }
            textures += checkModels(run, files, atlas, block.group(), "block:" + block.path(), models, reached);
        }
        for (Entry<net.minecraft.world.item.Item> item : run.content().items()) {
            ResourceLocation model = ResourceLocation.fromNamespaceAndPath(item.id().getNamespace(), "item/" + item.path());
            textures += checkModels(run, files, atlas, item.group(), "item:" + item.path(), List.of(model), reached);
        }
        // models no block or item reaches
        Map<ResourceLocation, Resource> all = run.client().getResourceManager().listResources("models",
                id -> id.getNamespace().equals(thaumic.tinkerer.common.lib.LibMisc.MOD_ID) && id.getPath().endsWith(".json"));
        List<String> orphans = new ArrayList<>();
        for (ResourceLocation file : all.keySet()) {
            String path = file.getPath().substring("models/".length(), file.getPath().length() - ".json".length());
            ResourceLocation model = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path);
            if (reached.contains(model)) continue;
            orphans.add(path);
            ModelFiles.Resolved resolved = files.resolve(model);
            List<String> problems = problems(files, atlas, resolved);
            if (!problems.isEmpty()) {
                run.report().fail(PHASE, "unreferenced_model_problem", groupByPrefix(run, path), "model:" + path, String.join("; ", problems));
            }
        }
        run.report().info(PHASE, "unreferenced_models", orphans.size() + " model files of the add-on are reached by no blockstate, item model or override: " + orphans);
        checkAtlasSources(run, atlas);
        run.report().info(PHASE, name(), textures + " texture references of reachable models looked at; " + all.size() + " model files in the add-on");
    }

    private int checkModels(SmokeRun run, ModelFiles files, TextureAtlas atlas, String group, String target,
            List<ResourceLocation> models, Set<ResourceLocation> reached) {
        List<String> problems = new ArrayList<>();
        int count = 0;
        List<ResourceLocation> queue = new ArrayList<>(models);
        for (int i = 0; i < queue.size() && i < 64; i++) {
            ResourceLocation model = queue.get(i);
            ModelFiles.Resolved resolved = files.resolve(model);
            reached.addAll(resolved.chain());
            for (ResourceLocation extra : resolved.overrideModels()) {
                if (!queue.contains(extra)) queue.add(extra);
            }
            count += ModelFiles.textureLocations(resolved).size();
            List<String> own = problems(files, atlas, resolved);
            for (String problem : own) problems.add(model + ": " + problem);
        }
        if (problems.isEmpty()) {
            run.report().pass(PHASE, "model_textures", group, target, queue.size() + " models, all textures found and stitched");
        } else {
            for (String problem : problems) {
                String kind = problem.contains("model file") ? "missing_model" : "missing_texture";
                run.report().fail(PHASE, kind, group, target, problem);
            }
        }
        return count;
    }

    private static List<String> problems(ModelFiles files, TextureAtlas atlas, ModelFiles.Resolved resolved) {
        List<String> problems = new ArrayList<>(resolved.problems());
        for (String variable : resolved.unresolvedFaces()) {
            problems.add("face texture variable #" + variable + " is not defined by the model or its parents (renders missingno)");
        }
        for (ResourceLocation texture : ModelFiles.textureLocations(resolved)) {
            ResourceLocation file = ModelFiles.textureFile(texture);
            if (!files.exists(file)) {
                problems.add("texture " + texture + " has no file " + file);
            } else if (atlas.getSprite(texture).contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                problems.add("texture " + texture + " exists as a file but is not in the block atlas (renders missingno)");
            }
        }
        return problems;
    }

    /** The registry path that is the longest prefix of a model file name, or the shared bucket. */
    private static String groupByPrefix(SmokeRun run, String modelPath) {
        String name = modelPath.substring(modelPath.lastIndexOf('/') + 1);
        String best = null;
        for (Entry<net.minecraft.world.level.block.Block> block : run.content().blocks()) {
            if (name.startsWith(block.path()) && (best == null || block.path().length() > best.length())) best = block.group();
        }
        for (Entry<net.minecraft.world.item.Item> item : run.content().items()) {
            if (name.startsWith(item.path()) && (best == null || item.path().length() > best.length())) best = item.group();
        }
        return best == null ? "Unassigned" : best;
    }

    /** {@code atlases/blocks.json} of the add-on: every {@code single} source and every directory source must be stitched. */
    private void checkAtlasSources(SmokeRun run, TextureAtlas atlas) {
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath("minecraft", "atlases/blocks.json");
        for (Resource resource : run.client().getResourceManager().getResourceStack(file)) {
            if (!resource.sourcePackId().contains("thaumictinkerer") && !resource.sourcePackId().contains("thaumic")) continue;
            try (var reader = resource.openAsReader()) {
                JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                int checked = 0;
                for (JsonElement element : root.getAsJsonArray("sources")) {
                    JsonObject source = element.getAsJsonObject();
                    if (!source.has("resource")) continue;
                    ResourceLocation sprite = ResourceLocation.parse(source.get("resource").getAsString());
                    checked++;
                    boolean there = files.exists(ModelFiles.textureFile(sprite));
                    boolean stitched = !atlas.getSprite(sprite).contents().name().equals(MissingTextureAtlasSprite.getLocation());
                    String group = groupByPrefix(run, sprite.getPath());
                    if (there && stitched) run.report().pass(PHASE, "atlas_source", group, "texture:" + sprite, "stitched into the block atlas");
                    else run.report().fail(PHASE, "missing_texture", group, "texture:" + sprite,
                            "atlas source " + sprite + (there ? " has a file but is not stitched" : " has no file " + ModelFiles.textureFile(sprite)));
                }
                run.report().info(PHASE, "atlas_sources", checked + " single sources of " + resource.sourcePackId());
            } catch (java.io.IOException | RuntimeException e) {
                run.report().harnessError(PHASE, "atlas_sources", e);
            }
        }
    }
}
