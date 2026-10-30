package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandHelper;
import thaumcraft.api.wands.WandRod;
import thaumic.tinkerer.client.core.handler.kami.SoulHeartClientHandler;
import thaumic.tinkerer.common.item.foci.ItemFocusDislocation;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The overlays the add-on draws on the HUD, the ones that can be triggered without a key press: the Dislocation readout (a wand
 * whose focus carries a chest) and the soul hearts. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the
 * add-on, not distributed.
 *
 * <p>The interface is shown only for these pictures (the block pictures hide it). The soul hearts are set by writing the value
 * the S2C packet would write, so the packet path is not covered; the tool-mode tooltip needs a key press on a Kami tool and is
 * recorded as not run.</p>
 */
final class HudPhase extends ScriptedPhase {
    private static final String PHASE = "hud";

    @Override
    public String name() {
        return PHASE;
    }

    @Override
    protected List<Script> scripts(SmokeRun run) {
        List<Script> scripts = new ArrayList<>();
        scripts.add(dislocation(run));
        scripts.add(soulHearts(run));
        run.report().notRun(PHASE, "tool_mode_hud", "the Kami tool-mode tooltip appears on a key press on an awakened tool (KamiHooks.setTooltipSink); no input device here");
        return scripts;
    }

    private Script dislocation(SmokeRun run) {
        Script script = new Script("hud-dislocation");
        BlockPos[] chest = { null };
        script.then("a wand that carries a chest", run.onServer(server -> {
            ServerLevel level = server.overworld();
            ServerPlayer player = run.serverPlayer();
            Stage.clear(server, level);
            chest[0] = Stage.cell(level, 0);
            Stage.place(level, chest[0], Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) level.getBlockEntity(chest[0])).setItem(3, new ItemStack(Items.DIAMOND, 5));
            ItemStack wand = ItemApi.getItem("itemWandCasting", 0);
            WandHelper.setRod(wand, WandRod.byTag("silverwood_staff"));
            WandHelper.setCap(wand, WandCap.byTag("thaumium"));
            for (Aspect primal : Aspect.getPrimalAspects()) WandHelper.addRealVis(wand, primal, WandHelper.getMaxVis(wand), true);
            WandHelper.setFocus(wand, TTRegistry.stack(LibItemNames.FOCUS_DISLOCATION));
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            ((ItemFocusDislocation) TTRegistry.item(LibItemNames.FOCUS_DISLOCATION)).dislocate(wand, level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(chest[0]), Direction.UP, chest[0], false));
            Stage.camera(player, 1, 0);
        }));
        script.until("the client holds the wand with the load", () -> {
            ItemStack held = run.client().player.getMainHandItem();
            return WandHelper.isWand(held) && ItemFocusDislocation.getData(held) != null;
        }, 20);
        script.until("the camera", () -> run.client().player.getZ() > Stage.Z + 3, 20);
        script.run("show the interface", r -> r.client().options.hideGui = false);
        script.frames(10);
        script.then("picture", run.shots().shot("hud-dislocation", "Foci", "the Dislocation readout under the wand's dial: the chest it carries, 'Currently Dislocating', 'Extra Contents'"), 60);
        script.run("verdict", r -> {
            r.report().pass(PHASE, "dislocation_hud", "Foci", "hud:dislocation", "the focus carried a chest (server) and the client held the loaded wand while the HUD drew; read the picture for the look");
            r.client().options.hideGui = true;
        });
        return script;
    }

    private Script soulHearts(SmokeRun run) {
        Script script = new Script("hud-soul-hearts");
        // the hearts are drawn only for a player that can be hurt: creative players cannot
        script.then("survival mode", run.onServer(server -> run.serverPlayer().setGameMode(GameType.SURVIVAL)));
        script.until("the client in survival", () -> run.client().gameMode.canHurtPlayer(), 15);
        script.run("seven soul half-hearts", r -> {
            SoulHeartClientHandler.clientPlayerHP = 7;
            r.client().options.hideGui = false;
        });
        script.frames(10);
        script.then("picture", run.shots().shot("hud-soul-hearts", "KamiWorld", "the soul hearts (7 half-hearts) next to the health bar, in survival mode; the value is written directly, not received as a packet"), 60);
        script.run("verdict", r -> {
            SoulHeartClientHandler.clientPlayerHP = 0;
            r.client().options.hideGui = true;
            r.report().pass(PHASE, "soul_hearts_hud", "KamiWorld", "hud:soul_hearts", "the overlay drew with 7 half-hearts without an exception; read the picture for the look");
        });
        script.then("creative mode again", run.onServer(server -> run.serverPlayer().setGameMode(GameType.CREATIVE)));
        return script;
    }
}
