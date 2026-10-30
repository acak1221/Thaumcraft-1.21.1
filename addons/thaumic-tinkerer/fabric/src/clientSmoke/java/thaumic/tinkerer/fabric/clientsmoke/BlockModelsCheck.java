package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * Every block state of every block of the add-on has a baked model that is not the missing model, and a particle sprite
 * that is not the missing-texture sprite. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the
 * add-on, not distributed.
 *
 * <p>All states are looked at (the baked cache holds one model per state), not only the representative ones of the
 * pictures. The blockstate JSON is read first so that a missing file is named as such and not only as "missing model".</p>
 */
final class BlockModelsCheck implements SmokeCheck {
    private static final int SAMPLES = 4;

    @Override
    public String name() {
        return "block_models";
    }

    @Override
    public void run(SmokeRun run) {
        BlockModelShaper shaper = run.client().getBlockRenderer().getBlockModelShaper();
        BakedModel missing = run.client().getModelManager().getMissingModel();
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        int total = 0;
        for (Entry<net.minecraft.world.level.block.Block> entry : run.content().blocks()) {
            String target = "block:" + entry.path();
            ResourceLocation stateFile = ResourceLocation.fromNamespaceAndPath(entry.id().getNamespace(),
                    "blockstates/" + entry.path() + ".json");
            if (!files.exists(stateFile)) {
                run.report().fail(PHASE, "missing_blockstate", entry.group(), target, stateFile + " does not exist (missing model)");
            }
            List<String> noModel = new ArrayList<>();
            List<String> noParticle = new ArrayList<>();
            List<BlockState> states = entry.value().getStateDefinition().getPossibleStates();
            for (BlockState state : states) {
                BakedModel model = shaper.getBlockModel(state);
                if (model == missing) noModel.add(describe(state));
                else if (isMissing(model)) noParticle.add(describe(state));
            }
            total += states.size();
            if (noModel.isEmpty()) {
                run.report().pass(PHASE, "block_model", entry.group(), target, states.size() + " states have a baked model");
            } else {
                run.report().fail(PHASE, "missing_model", entry.group(), target, noModel.size() + " of " + states.size()
                        + " states bake to the missing model, e.g. " + sample(noModel));
            }
            if (noParticle.isEmpty()) {
                run.report().pass(PHASE, "block_particle_sprite", entry.group(), target, "particle sprite is a real sprite");
            } else {
                run.report().fail(PHASE, "missing_texture", entry.group(), target, "particle sprite is the missing-texture sprite in "
                        + noParticle.size() + " states, e.g. " + sample(noParticle));
            }
        }
        run.report().info(PHASE, name(), run.content().blocks().size() + " blocks, " + total + " block states looked at");
    }

    private static boolean isMissing(BakedModel model) {
        return model.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation());
    }

    private static String describe(BlockState state) {
        String properties = BlockModelShaper.statePropertiesToString(state.getValues());
        return properties.isEmpty() ? "default" : properties;
    }

    private static String sample(List<String> states) {
        return String.join(" | ", states.subList(0, Math.min(SAMPLES, states.size())));
    }
}
