package thaumic.tinkerer.fabric.clientsmoke;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Proves that the resource checks can fail: it asks the game for things that do not exist and expects the answers the checks
 * rely on. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>If the game ever stops answering "missing model" with the missing-model object, or "missing sprite" with the missing
 * sprite, every {@code PASS} of the model and texture checks would be worthless; these results say it did not happen.</p>
 */
final class CanaryCheck implements SmokeCheck {
    @Override
    public String name() {
        return "canaries";
    }

    @Override
    public void run(SmokeRun run) {
        var models = run.client().getModelManager();
        boolean missingModel = models.getModel(new ModelResourceLocation(LibMisc.id("no_such_item_canary"), "inventory")) == models.getMissingModel();
        report(run, "unknown_model_is_the_missing_model", missingModel);

        TextureAtlas atlas = models.getAtlas(TextureAtlas.LOCATION_BLOCKS);
        boolean missingSprite = atlas.getSprite(LibMisc.id("block/no_such_texture_canary")).contents().name().equals(MissingTextureAtlasSprite.getLocation());
        report(run, "unknown_sprite_is_missingno", missingSprite);

        report(run, "unknown_key_is_not_translated", !Language.getInstance().has("thaumictinkerer.canary.no_such_key"));
        report(run, "a_known_key_is_translated", Language.getInstance().has("gui.done"));

        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        ModelFiles.Resolved ghost = files.resolve(ResourceLocation.fromNamespaceAndPath(LibMisc.MOD_ID, "block/no_such_model_canary"));
        report(run, "unknown_model_file_is_reported", !ghost.problems().isEmpty());

        report(run, "raw_key_in_a_tooltip_is_found", TooltipCheck.isRawKey("ttmisc.something") && !TooltipCheck.isRawKey("Smokey Quartz"));
    }

    private static void report(SmokeRun run, String what, boolean ok) {
        if (ok) run.report().pass(PHASE, "canary", "", "canary:" + what, "behaves as the checks assume");
        else run.report().add(PHASE, "canary_failed", "", "canary:" + what, SmokeReport.Result.HARNESS_ERROR,
                "the game answered differently from what the checks rely on: every pass of the related check is in doubt");
    }
}
