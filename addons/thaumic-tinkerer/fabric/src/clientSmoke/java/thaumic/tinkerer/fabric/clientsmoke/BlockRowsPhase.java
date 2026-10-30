package thaumic.tinkerer.fabric.clientsmoke;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * Every block of the add-on, in rows by content group, framed from a fixed camera: six cells per picture, each cell one
 * representative state, a name tag over each. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the
 * add-on, not distributed.
 *
 * <p>"Representative" means the default state plus every other value of every property (the number of states per block is
 * capped, see {@link Stage#representatives}); the baked-model check of the resource phase already looked at all of them. The
 * blocks are put in place the way a command would, without neighbour updates, so a block that needs a support stands anyway
 * and its own tick is what moves it. The interface is hidden: the HUD has no business in a block picture.</p>
 */
final class BlockRowsPhase extends ScriptedPhase {
    private static final int STATES_PER_BLOCK = 8;

    /** One cell: the block, the state, the label. */
    private record Cell(String group, String path, BlockState state, String label) {}

    @Override
    public String name() {
        return "block_rows";
    }

    @Override
    protected List<Script> scripts(SmokeRun run) {
        Map<String, List<Cell>> byGroup = new LinkedHashMap<>();
        for (Entry<Block> entry : run.content().blocks()) {
            BlockState defaults = entry.value().defaultBlockState();
            for (BlockState state : Stage.representatives(entry.value(), STATES_PER_BLOCK)) {
                String detail = Stage.describe(state, defaults);
                byGroup.computeIfAbsent(entry.group(), g -> new ArrayList<>())
                        .add(new Cell(entry.group(), entry.path(), state, detail.isEmpty() ? entry.path() : entry.path() + " " + detail));
            }
        }
        List<Script> scripts = new ArrayList<>();
        scripts.add(new Script("block_rows:floor").run("hide the interface", r -> r.client().options.hideGui = true)
                .then("floor", run.onServer(server -> Stage.floor(server.overworld()))));
        // the empty stage seen by the camera of each row size: what a cell is compared with to tell whether it can be seen
        java.util.TreeSet<Integer> sizes = new java.util.TreeSet<>();
        for (List<Cell> cells : byGroup.values()) {
            for (int from = 0; from < cells.size(); from += Stage.CELLS_PER_ROW) sizes.add(Math.min(Stage.CELLS_PER_ROW, cells.size() - from));
        }
        for (int size : sizes) scripts.add(referenceScript(run, size));
        for (Map.Entry<String, List<Cell>> group : byGroup.entrySet()) {
            List<Cell> cells = group.getValue();
            int pictures = (cells.size() + Stage.CELLS_PER_ROW - 1) / Stage.CELLS_PER_ROW;
            for (int picture = 0; picture < pictures; picture++) {
                List<Cell> row = cells.subList(picture * Stage.CELLS_PER_ROW, Math.min(cells.size(), (picture + 1) * Stage.CELLS_PER_ROW));
                scripts.add(rowScript(run, group.getKey(), picture + 1, pictures, row));
            }
        }
        return scripts;
    }

    private Script referenceScript(SmokeRun run, int size) {
        String name = "reference-empty-stage-" + size;
        Script script = new Script(name);
        script.then("clear the stage", run.onServer(server -> {
            Stage.clear(server, server.overworld());
            Stage.camera(run.serverPlayer(), size, 0);
        }));
        script.until("the camera", () -> run.client().player.getZ() > Stage.Z + 3, 20);
        script.until("the chunk sections to be built", () -> run.client().levelRenderer.hasRenderedAllSections(), 40);
        script.frames(10);
        script.then("picture", run.shots().shot(name, "Core", "the empty stage seen by the camera of a row of " + size + " cells: the reference the cells are compared with"), 60);
        return script;
    }

    private Script rowScript(SmokeRun run, String group, int picture, int pictures, List<Cell> row) {
        String name = String.format("blocks-%s-%02d", group.toLowerCase(), picture);
        Script script = new Script(name);
        BlockPos[] positions = new BlockPos[row.size()];
        script.then("build the row", run.onServer(server -> {
            ServerLevel level = server.overworld();
            Stage.clear(server, level);
            for (int i = 0; i < row.size(); i++) {
                positions[i] = Stage.cell(level, i);
                Stage.place(level, positions[i], row.get(i).state());
                Stage.label(server, positions[i], row.get(i).label(), i % 3);
            }
            Stage.camera(run.serverPlayer(), row.size(), 0);
        }));
        script.until("the client has the blocks", () -> {
            for (int i = 0; i < row.size(); i++) {
                if (positions[i] == null || !run.client().level.getBlockState(positions[i]).is(row.get(i).state().getBlock())) return false;
            }
            return true;
        }, 20);
        script.until("the camera", () -> run.client().player.getZ() > Stage.Z + 3, 20);
        script.run("log the camera", r -> SmokeRun.LOG.info("camera {}: eye {} pitch {} yaw {} flying {} pose {}", name, r.client().player.getEyePosition(),
                r.client().player.getXRot(), r.client().player.getYRot(), r.client().player.getAbilities().flying, r.client().player.getPose()));
        script.until("the chunk sections to be built", () -> run.client().levelRenderer.hasRenderedAllSections(), 40);
        script.frames(10);
        StringBuilder shows = new StringBuilder("Left to right: ");
        for (int i = 0; i < row.size(); i++) shows.append(i == 0 ? "" : "; ").append(row.get(i).label());
        Visibility.Camera[] camera = { null };
        script.run("the camera of the picture", r -> camera[0] = Visibility.Camera.of(r.client()));
        script.then("picture", run.shots().shot(name, group, "block row " + picture + " of " + pictures + " of " + group + ". " + shows), 60);
        script.run("which cells can be seen", r -> {
            Path dir = r.report().outDir().resolve("screenshots");
            for (int i = 0; i < row.size(); i++) {
                double share = Visibility.changed(dir.resolve(name + ".png"), dir.resolve("reference-empty-stage-" + row.size() + ".png"), camera[0], positions[i]);
                String target = "block:" + row.get(i).path();
                String detail = "state '" + row.get(i).label() + "': " + (share < 0 ? "the picture could not be compared" : Math.round(share * 1000) / 10.0 + "% of the pixels of its cube differ from the empty stage");
                if (share >= Visibility.SEEN) r.report().pass("block_rows", "block_visible", row.get(i).group(), target, detail);
                else r.report().add("block_rows", "block_not_visible", row.get(i).group(), target, SmokeReport.Result.INFO,
                        detail + " (nothing is drawn at its place; intended for a gas, a force field or an empty camouflage, a defect otherwise)");
            }
        });
        return script;
    }
}
