package thaumic.tinkerer.fabric.clientsmoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * Block-entity renderers: for every block-entity type of the add-on, the renderer the game would use for an instance of it
 * (the answer of {@code BlockEntityRenderDispatcher.getRenderer}), and every {@code RenderTile*} class of the sources must be
 * the renderer of at least one type. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on,
 * not distributed.
 *
 * <p>A renderer class whose own javadoc says it was never registered in 1.7.10 either is reported as a known, deliberate
 * omission and not as a missing renderer.</p>
 */
final class RenderersCheck implements SmokeCheck {
    @Override
    public String name() {
        return "block_entity_renderers";
    }

    @Override
    public void run(SmokeRun run) throws IOException {
        Map<String, String> used = new LinkedHashMap<>();
        List<String> without = new ArrayList<>();
        for (Entry<BlockEntityType<?>> entry : run.content().blockEntityTypes()) {
            String target = "block_entity:" + entry.path();
            BlockEntity instance = instance(run, entry);
            if (instance == null) {
                run.report().fail(PHASE, "block_entity_not_creatable", entry.group(), target,
                        "no block of the add-on accepts this type, or its constructor threw: no renderer can be asked for");
                continue;
            }
            BlockEntityRenderer<BlockEntity> renderer = run.client().getBlockEntityRenderDispatcher().getRenderer(instance);
            if (renderer == null) {
                without.add(entry.path());
                run.report().info(PHASE, "no_block_entity_renderer", target + " (" + entry.group() + ") is drawn by its block model alone");
            } else {
                used.put(renderer.getClass().getSimpleName(), entry.path());
                run.report().pass(PHASE, "block_entity_renderer", entry.group(), target, "renderer " + renderer.getClass().getSimpleName());
            }
        }
        Path dir = run.sourceRoot().resolve("common/src/main/java/thaumic/tinkerer/client/render/tile");
        TreeSet<String> expected = new TreeSet<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().startsWith("RenderTile")).forEach(p -> expected.add(p.getFileName().toString().replace(".java", "")));
        }
        for (String className : expected) {
            if (used.containsKey(className)) continue;
            String group = run.content().groups().groupOfClass(className);
            String source = Files.readString(dir.resolve(className + ".java"));
            if (source.contains("never registered")) {
                run.report().info(PHASE, "renderer_deliberately_unregistered", className + " is not registered; its javadoc says 1.7.10 never registered it either");
            } else {
                run.report().fail(PHASE, "renderer_missing", group, "renderer:" + className,
                        "the class exists in client/render/tile but no block-entity type of the add-on has it as its renderer");
            }
        }
        run.report().inventory("block_entity_types", TinkererContent.paths(run.content().blockEntityTypes()));
        run.report().inventory("block_entity_types_without_renderer", without);
        run.report().inventory("renderer_classes_in_sources", new ArrayList<>(expected));
    }

    /** A block entity of the type, made on a state of a block that accepts the type. */
    private static BlockEntity instance(SmokeRun run, Entry<BlockEntityType<?>> entry) {
        for (Entry<Block> block : run.content().blocks()) {
            BlockState state = block.value().defaultBlockState();
            if (!entry.value().isValid(state)) continue;
            try {
                return entry.value().create(BlockPos.ZERO, state);
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }
}
