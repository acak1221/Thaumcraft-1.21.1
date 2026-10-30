package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.common.core.helper.EnumMobAspect;
import thaumic.tinkerer.common.item.ItemMobDisplay;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.fabric.clientsmoke.ItemGridScreen.Cell;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * Every item of the add-on on an inventory-like screen, pages grouped by content group, plus the tooltip of every stack.
 * Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The stacks are the ones of the add-on's creative tab (which carries the variants: seeds of each aspect, a wand with vis),
 * then the items the tab does not list, then a Mob Display for every mob it can show. Each page is drawn once into a throw-away
 * {@code GuiGraphics} first: an item whose renderer throws would otherwise end the whole game in a crash report, here it is
 * named, left off the page and recorded.</p>
 */
final class ItemGridPhase extends ScriptedPhase {
    private static final int PER_PAGE = ItemGridScreen.COLUMNS * ItemGridScreen.ROWS;

    private final String language;

    /** @param language "" for the first pass (the run's English), a language code for a repeat in that language (pictures only) */
    ItemGridPhase(String language) {
        this.language = language;
    }

    private boolean repeat() {
        return !language.isEmpty();
    }

    private String suffix() {
        return repeat() ? "-" + language : "";
    }

    @Override
    public String name() {
        return repeat() ? "item_grid_" + language : "item_grid";
    }

    @Override
    protected List<Script> scripts(SmokeRun run) {
        List<Cell> all = stacks(run);
        List<Script> scripts = new ArrayList<>();
        if (!repeat()) {
            run.report().fact("item_grid_cells", all.size());
            scripts.add(new Script("item_tooltips").run("tooltips", r -> tooltips(r, all)));
        }
        Map<String, List<Cell>> byGroup = new LinkedHashMap<>();
        for (String group : GroupIndex.GROUPS) byGroup.put(group, new ArrayList<>());
        byGroup.put("ForeignInTab", new ArrayList<>());
        byGroup.put("Unassigned", new ArrayList<>());
        for (Cell cell : all) byGroup.computeIfAbsent(groupOf(run, cell.path()), g -> new ArrayList<>()).add(cell);
        for (Map.Entry<String, List<Cell>> group : byGroup.entrySet()) {
            List<Cell> cells = group.getValue();
            int pages = (cells.size() + PER_PAGE - 1) / PER_PAGE;
            for (int page = 0; page < pages; page++) {
                scripts.add(pageScript(run, group.getKey(), page + 1, pages, cells.subList(page * PER_PAGE, Math.min(cells.size(), (page + 1) * PER_PAGE))));
            }
        }
        scripts.add(new Script("item_grid:close").run("close the screen", r -> r.client().setScreen(null)));
        return scripts;
    }

    private Script pageScript(SmokeRun run, String group, int page, int pages, List<Cell> cells) {
        String name = String.format("items-%s-%02d%s", group.toLowerCase(), page, suffix());
        ItemGridScreen[] screen = { null };
        Script script = new Script(name);
        script.run("open the page", r -> {
            Minecraft client = r.client();
            ItemGridScreen draft = new ItemGridScreen("Items of " + group + ", page " + page + " of " + pages + (repeat() ? " (" + language + ")" : ""), cells);
            draft.init(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
            List<String> broken = draft.dryRun(new GuiGraphics(client, client.renderBuffers().bufferSource()));
            List<Cell> shown = new ArrayList<>(cells);
            for (String problem : broken) {
                String path = problem.substring(0, problem.indexOf(':'));
                run.report().fail(name(), "item_render_exception", group, "item:" + path, "rendering the stack in a GUI threw " + problem.substring(path.length() + 2));
                shown.removeIf(c -> c.path().equals(path));
            }
            screen[0] = broken.isEmpty() ? draft : new ItemGridScreen("Items of " + group + ", page " + page + " of " + pages + (repeat() ? " (" + language + ")" : ""), shown);
            client.setScreen(screen[0]);
        });
        script.until("the screen", () -> run.client().screen == screen[0], 20);
        script.frames(6);
        StringBuilder shows = new StringBuilder("Row by row, left to right: ");
        for (int i = 0; i < cells.size(); i++) shows.append(i == 0 ? "" : "; ").append(cells.get(i).path()).append(cells.get(i).note().isEmpty() ? "" : " " + cells.get(i).note());
        script.then("picture", run.shots().shot(name, group, "item grid page " + page + " of " + pages + " of " + group + (repeat() ? ", in " + language + " (names that are still English are untranslated)" : "") + ". " + shows), 60);
        return script;
    }

    /** A stack of another mod that the add-on's creative tab lists (a wand with vis) belongs to no registry path of the add-on. */
    private static String groupOf(SmokeRun run, String path) {
        return path.contains(":") ? "ForeignInTab" : run.content().groups().groupOf(path);
    }

    /** The stacks to show: the creative tab, the items the tab omits, the mob displays. */
    private static List<Cell> stacks(SmokeRun run) {
        Minecraft client = run.client();
        CreativeModeTabs.tryRebuildTabContents(client.level.enabledFeatures(), true, client.level.registryAccess());
        List<ItemStack> stacks = new ArrayList<>(TTRegistry.CREATIVE_TAB.get().getDisplayItems());
        Set<Item> listed = new LinkedHashSet<>();
        for (ItemStack stack : stacks) listed.add(stack.getItem());
        for (Entry<Item> entry : run.content().items()) {
            if (!listed.contains(entry.value())) stacks.add(new ItemStack(entry.value()));
        }
        for (EnumMobAspect mob : EnumMobAspect.values()) stacks.add(ItemMobDisplay.stackFor(mob));
        List<Cell> cells = new ArrayList<>();
        for (ItemVariants.Variant variant : ItemVariants.all()) {
            cells.add(new Cell(variant.stack(), variant.path(), variant.stack().getHoverName().getString(), "(" + variant.note() + ")"));
        }
        for (ItemStack stack : stacks) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String path = TinkererContent.ours(id) ? id.getPath() : id.toString();
            ItemStack shown = stack.copy();
            String note = "";
            if (shown.isDamageableItem()) {
                shown.setDamageValue(shown.getMaxDamage() / 3);
                note = "(damaged)";
            } else if (shown.getMaxStackSize() > 1 && shown.getCount() == 1) {
                shown.setCount(2);
            }
            cells.add(new Cell(shown, path, shown.getHoverName().getString(), note));
        }
        return cells;
    }

    private static void tooltips(SmokeRun run, List<Cell> cells) {
        Minecraft client = run.client();
        Map<String, List<String>> lines = new LinkedHashMap<>();
        int problems = 0;
        for (Cell cell : cells) {
            try {
                TooltipCheck.Result result = TooltipCheck.of(client, cell.stack());
                lines.merge(cell.path(), result.lines(), (a, b) -> a.size() >= b.size() ? a : b);
                String group = groupOf(run, cell.path());
                for (String problem : result.problems()) {
                    problems++;
                    run.report().fail("item_grid", "missing_translation", group, "item:" + cell.path(), "tooltip: " + problem);
                }
            } catch (RuntimeException | LinkageError e) {
                run.report().fail("item_grid", "tooltip_exception", groupOf(run, cell.path()), "item:" + cell.path(), "building the tooltip threw " + e);
            }
        }
        run.report().fact("tooltips", lines);
        run.report().info("item_grid", "tooltips", cells.size() + " stacks, tooltip built for each; " + problems + " problems");
    }
}
