package thaumic.tinkerer.fabric.clientsmoke;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * The checks that need no world: they run on the title screen, once the first resource reload is over. Test-only code of the
 * port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 */
final class ResourcesPhase implements Phase {
    private static final String NAME = "resources";

    private final List<SmokeCheck> checks = List.of(new CanaryCheck(), new BlockModelsCheck(), new ItemModelsCheck(), new OverridesCheck(), new ModelTexturesCheck(),
            new TranslationsCheck(), new SourceKeysCheck(), new ResearchTextCheck(), new SoundsCheck(), new ParticlesCheck(),
            new RenderersCheck(), new ScreensCheck(), new GuiTexturesCheck(), new ArmorAndEffectTexturesCheck());
    private int framesOnTitle;
    private boolean done;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean tick(SmokeRun run) {
        Minecraft client = run.client();
        if (client.getOverlay() != null || !(client.screen instanceof TitleScreen)) return false;
        // a few frames on the title screen: the atlas and the model bakery are finished with the reload
        if (framesOnTitle++ < 20) return false;
        if (done) return true;
        done = true;
        run.report().inventory("blocks", TinkererContent.paths(run.content().blocks()));
        run.report().inventory("items", TinkererContent.paths(run.content().items()));
        run.report().fact("group_assignment", run.content().groups().asMap(allPaths(run)));
        if (!run.content().groups().problems().isEmpty()) run.report().fact("group_index_problems", run.content().groups().problems());
        for (SmokeCheck check : checks) {
            long before = run.report().checks().size();
            try {
                check.run(run);
            } catch (Exception | LinkageError e) {
                run.report().harnessError(NAME, check.name(), e);
            }
            run.report().info(NAME, "ran:" + check.name(), (run.report().checks().size() - before) + " result lines");
        }
        return true;
    }

    private static List<String> allPaths(SmokeRun run) {
        return java.util.stream.Stream.concat(run.content().blocks().stream().map(TinkererContent.Entry::path),
                run.content().items().stream().map(TinkererContent.Entry::path)).distinct().toList();
    }
}
