package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ItemApi;
import thaumic.tinkerer.client.gui.kami.GuiWarpGateDestinations;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * Every menu of the add-on opened for real: the server-side player right-clicks the block (or uses the item) that opens it, the
 * integrated server sends the open-screen packet, the client builds the screen. Test-only code of the port (no 1.7.10
 * counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The way each menu is opened is the one table of this class ({@link #HOW}): what the player holds and whether it sneaks. A
 * menu type of the add-on that the table does not know is recorded as not run, never skipped silently. For each menu the check
 * asserts that a container screen of that menu type opens, that drawing it once into a throw-away {@code GuiGraphics}
 * (before the game does) does not throw, and that no suspicious line reached the log; then a picture is taken.</p>
 */
final class MenusPhase extends ScriptedPhase {
    private static final String PHASE = "menus";

    private final String language;

    /** What the player holds and does to open a menu. */
    private enum How { EMPTY_HAND, WAND, WAND_SNEAKING, ITEM_USE }

    /** Menu path to the way of opening it; the block of the menu has the same path, the pouch is an item. */
    private static final Map<String, How> HOW = Map.of(
            "enchanter", How.EMPTY_HAND,
            "animation_tablet", How.EMPTY_HAND,
            "remote_placer", How.WAND_SNEAKING,
            "aspect_analyzer", How.EMPTY_HAND,
            "mob_magnet", How.WAND,
            "warp_gate", How.EMPTY_HAND,
            "ichor_pouch", How.ITEM_USE);

    private final LogWatcher log = new LogWatcher();

    /** @param language "" for the first pass, a language code for the repeat in that language */
    MenusPhase(String language) {
        this.language = language;
    }

    private boolean repeat() {
        return !language.isEmpty();
    }

    @Override
    public String name() {
        return repeat() ? PHASE + "_" + language : PHASE;
    }

    @Override
    protected List<Script> scripts(SmokeRun run) {
        List<Script> scripts = new ArrayList<>();
        scripts.add(new Script("menus:floor").then("floor", run.onServer(server -> {
            Stage.floor(server.overworld());
            Stage.clear(server, server.overworld());
        })));
        for (Entry<MenuType<?>> menu : run.content().menus()) {
            How how = HOW.get(menu.path());
            if (how == null) {
                run.report().add(name(), "menu_screen", menu.group(), "menu:" + menu.path(), SmokeReport.Result.NOT_RUN,
                        "the harness does not know how a player opens this menu (add it to MenusPhase.HOW)");
                continue;
            }
            scripts.add(menuScript(run, menu, how));
        }
        scripts.add(warpGateDestinations(run));
        return scripts;
    }

    private Script menuScript(SmokeRun run, Entry<MenuType<?>> menu, How how) {
        String name = "menu-" + menu.path() + (repeat() ? "-" + language : "");
        Script script = new Script(name);
        BlockPos[] pos = { null };
        InteractionResult[] result = { null };
        int[] mark = { 0 };
        script.then("set the stage", run.onServer(server -> {
            ServerLevel level = server.overworld();
            Stage.clear(server, level);
            pos[0] = Stage.cell(level, 0);
            if (how != How.ITEM_USE) Stage.place(level, pos[0], TTRegistry.block(menu.path()).defaultBlockState());
            ServerPlayer player = run.serverPlayer();
            player.getInventory().clearContent();
            player.setShiftKeyDown(false);
            Stage.camera(player, 1, 0);
        }));
        script.until("the client has the block", () -> how == How.ITEM_USE || run.client().level.getBlockState(pos[0]).is(TTRegistry.block(menu.path())), 20);
        script.until("the camera", () -> run.client().player.getZ() > Stage.Z + 3, 20);
        script.frames(6);
        script.run("mark the log", r -> mark[0] = log.mark());
        script.then("open the menu", run.onServer(server -> {
            ServerLevel level = server.overworld();
            ServerPlayer player = run.serverPlayer();
            ItemStack held = switch (how) {
                case EMPTY_HAND -> ItemStack.EMPTY;
                case WAND, WAND_SNEAKING -> ItemApi.getItem("itemWandCasting", 0);
                case ITEM_USE -> TTRegistry.stack(menu.path());
            };
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            player.setShiftKeyDown(how == How.WAND_SNEAKING);
            if (how == How.ITEM_USE) {
                result[0] = player.gameMode.useItem(player, level, held, InteractionHand.MAIN_HAND);
            } else {
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos[0]).add(0, 0.5, 0), Direction.UP, pos[0], false);
                result[0] = player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND, hit);
            }
            player.setShiftKeyDown(false);
        }));
        script.then("the screen opens", expectScreen(run, menu, how, result), 15);
        script.frames(6);
        script.then("picture", run.shots().shot(name, menu.group(), "the screen of the " + menu.path() + " menu, opened by "
                + how.name().toLowerCase().replace('_', ' ') + (repeat() ? ", in " + language : "")), 60);
        script.run("verdict", r -> {
            List<Map<String, Object>> entries = log.since(mark[0]);
            Screen screen = r.client().screen;
            if (entries.isEmpty()) {
                r.report().pass(name(), "menu_screen", menu.group(), "menu:" + menu.path(),
                        "opened as " + screen.getClass().getSimpleName() + " (" + screen.getClass().getName() + "), drew without an exception, nothing suspicious in the log");
            } else {
                r.report().fail(name(), "screen_exception", menu.group(), "menu:" + menu.path(),
                        "the screen opened as " + screen.getClass().getSimpleName() + " but the log gained: " + LogWatcher.summarize(entries));
            }
            r.client().player.closeContainer();
        });
        script.until("the screen to close", () -> run.client().screen == null, 10);
        return script;
    }

    /** Waits for the container screen of the menu type, draws it once as a guard, records a failure and aborts when it does not come. */
    private Script.Step expectScreen(SmokeRun run, Entry<MenuType<?>> menu, How how, InteractionResult[] result) {
        long[] deadline = { 0 };
        return r -> {
            Minecraft client = r.client();
            if (deadline[0] == 0) deadline[0] = System.nanoTime() + 12_000_000_000L;
            Screen screen = client.screen;
            if (screen instanceof AbstractContainerScreen<?> container && container.getMenu().getType() == menu.value()) {
                String problem = dryRender(client, screen);
                if (problem == null) return true;
                r.report().fail(name(), "screen_exception", menu.group(), "menu:" + menu.path(), "drawing " + screen.getClass().getSimpleName() + " threw " + problem);
                client.setScreen(null);
                throw new Script.Abort();
            }
            if (System.nanoTime() > deadline[0]) {
                r.report().fail(name(), "screen_not_opened", menu.group(), "menu:" + menu.path(), "no container screen of this menu type opened; the interaction returned "
                        + result[0] + ", the client screen is " + (screen == null ? "none" : screen.getClass().getName()) + " (opened by " + how + ")");
                throw new Script.Abort();
            }
            return false;
        };
    }

    /** The warp gate's destinations screen: it is not a menu, the gate's tick opens it for a player that sneaks just above it. */
    private Script warpGateDestinations(SmokeRun run) {
        String path = "warp_gate";
        String group = run.content().groups().groupOf(path);
        Script script = new Script("menu-warp_gate_destinations" + (repeat() ? "-" + language : ""));
        BlockPos[] pos = { null };
        int[] mark = { 0 };
        script.then("set the stage", run.onServer(server -> {
            ServerLevel level = server.overworld();
            Stage.clear(server, level);
            pos[0] = Stage.cell(level, 0);
            Stage.place(level, pos[0], TTRegistry.block(path).defaultBlockState());
            ServerPlayer player = run.serverPlayer();
            player.connection.teleport(pos[0].getX() + 0.5, pos[0].getY() + 1.0, pos[0].getZ() + 0.5, 180.0F, 30.0F);
        }));
        script.until("the client has the gate", () -> run.client().level.getBlockState(pos[0]).is(TTRegistry.block(path)), 20);
        script.until("the player on the gate", () -> run.client().player.distanceToSqr(pos[0].getX() + 0.5, pos[0].getY() + 1.0, pos[0].getZ() + 0.5) < 0.5, 20);
        script.frames(4);
        script.run("sneak", r -> {
            mark[0] = log.mark();
            r.client().options.keyShift.setDown(true);
        });
        script.then("the destinations screen opens", r -> {
            Minecraft client = r.client();
            if (client.screen instanceof GuiWarpGateDestinations screen) {
                client.options.keyShift.setDown(false);
                String problem = dryRender(client, screen);
                if (problem == null) return true;
                r.report().fail(name(), "screen_exception", group, "screen:warp_gate_destinations", "drawing GuiWarpGateDestinations threw " + problem);
                client.setScreen(null);
                throw new Script.Abort();
            }
            return false;
        }, 12);
        script.frames(6);
        script.then("picture", run.shots().shot("menu-warp_gate_destinations" + (repeat() ? "-" + language : ""), group, "the destinations screen of the warp gate, opened by sneaking above it" + (repeat() ? ", in " + language : "")), 60);
        script.run("verdict", r -> {
            List<Map<String, Object>> entries = log.since(mark[0]);
            if (entries.isEmpty()) r.report().pass(name(), "menu_screen", group, "screen:warp_gate_destinations", "opened by the gate's tick for a sneaking player above it, drew without an exception");
            else r.report().fail(name(), "screen_exception", group, "screen:warp_gate_destinations", "the log gained: " + LogWatcher.summarize(entries));
            r.client().setScreen(null);
        });
        return script;
    }

    /** One draw of the screen into a throw-away graphics object; null when it did not throw. */
    private static String dryRender(Minecraft client, Screen screen) {
        try {
            GuiGraphics graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
            screen.render(graphics, -1, -1, 0.0F);
            graphics.flush();
            return null;
        } catch (RuntimeException | LinkageError e) {
            return SmokeReport.stack(e);
        }
    }
}
