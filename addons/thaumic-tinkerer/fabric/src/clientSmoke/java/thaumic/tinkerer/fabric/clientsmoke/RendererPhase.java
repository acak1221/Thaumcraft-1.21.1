package thaumic.tinkerer.fabric.clientsmoke;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.blocks.BlocksTC;
import thaumic.tinkerer.common.block.camo.CamoDeviceBlockEntity;
import thaumic.tinkerer.common.block.tile.TileCamo;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * Close-ups of every block entity that has a renderer, in a state that makes the renderer draw something: the item the
 * device holds, the camouflage block, the wand in the enchanter. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA
 * 3.0 like the add-on, not distributed.
 *
 * <p>An exception in a block-entity renderer ends the whole game in a crash report, so each renderer is called once on its own
 * first, the way the world render would, and a throw is recorded as a failure of that block and the block is taken out of the
 * world. The state set-up is the one table of this class ({@link #SETUPS}); a type without an entry is shown in its default
 * state. Blocks of types without a renderer are in the block rows, not here.</p>
 */
final class RendererPhase extends ScriptedPhase {
    private static final String PHASE = "renderers";

    private static final Map<String, BiConsumer<ServerLevel, BlockEntity>> SETUPS = Map.of(
            "animation_tablet", (level, tile) -> hold(tile, 0, new ItemStack(Items.DIAMOND_PICKAXE)),
            "enchanter", (level, tile) -> {
                hold(tile, 0, new ItemStack(Items.DIAMOND_SWORD));
                hold(tile, 1, ItemApi.getItem("itemWandCasting", 0));
            },
            "repairer", (level, tile) -> {
                ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
                pickaxe.setDamageValue(100);
                hold(tile, 0, pickaxe);
            },
            // the funnel accepts jars of essentia only (TileFunnel.isFilledJar); hold() skips that check, so a gold ingot would be
            // drawn by the generic item model and not by the jar path the renderer exists for
            "funnel", (level, tile) -> hold(tile, 0, filledJar()),
            "camo", (level, tile) -> camouflage(tile),
            "interface", (level, tile) -> camouflage(tile),
            "dislocator", (level, tile) -> camouflage(tile),
            "remote_placer", (level, tile) -> camouflage(tile),
            "golem_connector", (level, tile) -> camouflage(tile));

    @Override
    public String name() {
        return PHASE;
    }

    @Override
    protected List<Script> scripts(SmokeRun run) {
        List<Script> scripts = new ArrayList<>();
        for (Entry<BlockEntityType<?>> type : run.content().blockEntityTypes()) {
            Block block = blockOf(run, type);
            if (block == null) continue;
            BlockEntity probe = type.value().create(BlockPos.ZERO, block.defaultBlockState());
            if (probe == null || run.client().getBlockEntityRenderDispatcher().getRenderer(probe) == null) continue;
            scripts.add(closeup(run, type, block));
        }
        return scripts;
    }

    private static Block blockOf(SmokeRun run, Entry<BlockEntityType<?>> type) {
        for (Entry<Block> block : run.content().blocks()) {
            if (type.value().isValid(block.value().defaultBlockState())) return block.value();
        }
        return null;
    }

    private Script closeup(SmokeRun run, Entry<BlockEntityType<?>> type, Block block) {
        String name = "renderer-" + type.path();
        Script script = new Script(name);
        BlockPos[] pos = { null };
        BlockState state = camouflaged(block.defaultBlockState());
        script.then("set the stage", run.onServer(server -> {
            ServerLevel level = server.overworld();
            Stage.clear(server, level);
            pos[0] = Stage.cell(level, 0);
            Stage.place(level, pos[0], state);
            BlockEntity tile = level.getBlockEntity(pos[0]);
            BiConsumer<ServerLevel, BlockEntity> setup = SETUPS.get(type.path());
            if (setup != null && tile != null) {
                setup.accept(level, tile);
                tile.setChanged();
                level.sendBlockUpdated(pos[0], state, state, 3);
            }
            Stage.camera(run.serverPlayer(), 1, -2.0);
        }));
        script.until("the client has the block entity", () -> run.client().level.getBlockEntity(pos[0]) != null, 20);
        script.until("the camera", () -> run.client().player.getZ() > Stage.Z + 2, 20);
        script.frames(4);
        script.then("call the renderer once", r -> {
            Minecraft client = r.client();
            BlockEntity tile = client.level.getBlockEntity(pos[0]);
            try {
                PoseStack pose = new PoseStack();
                MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
                client.getBlockEntityRenderDispatcher().render(tile, 0.0F, pose, buffers);
                buffers.endBatch();
            } catch (RuntimeException | LinkageError e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                r.report().fail(PHASE, "renderer_exception", type.group(), "block_entity:" + type.path(),
                        "the renderer " + client.getBlockEntityRenderDispatcher().getRenderer(tile).getClass().getSimpleName() + " threw: " + SmokeReport.stack(cause));
                r.server().execute(() -> r.serverLevel().setBlock(pos[0], Blocks.AIR.defaultBlockState(), 2 | 16 | 32));
                throw new Script.Abort();
            }
            return true;
        });
        script.frames(8);
        String setup = SETUPS.containsKey(type.path()) ? "with its state set up (" + type.path() + ")" : "in its default state";
        script.then("picture", run.shots().shot(name, type.group(), "close-up of " + BuiltInRegistries.BLOCK.getKey(block).getPath() + " " + setup + ", drawn by its block-entity renderer"), 60);
        script.run("verdict", r -> r.report().pass(PHASE, "renderer_closeup", type.group(), "block_entity:" + type.path(),
                "the renderer ran in the world without an exception " + setup));
        return script;
    }

    /** A device with a camouflage property is shown with it on, so that the renderer has something to draw. */
    private static BlockState camouflaged(BlockState state) {
        for (var property : state.getProperties()) {
            if ((property.getName().equals("camo") || property.getName().equals("camouflaged")) && property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty flag) {
                return state.setValue(flag, true);
            }
        }
        return state;
    }

    /** A Thaumcraft jar holding five points of ignis, the stack the funnel really takes. */
    private static ItemStack filledJar() {
        ItemStack jar = new ItemStack(BlocksTC.jar().asItem());
        ((IEssentiaContainerItem) jar.getItem()).setAspects(jar, new AspectList().add(Aspect.FIRE, 5));
        return jar;
    }

    private static void hold(BlockEntity tile, int slot, ItemStack stack) {
        if (tile instanceof Container container && slot < container.getContainerSize()) container.setItem(slot, stack);
    }

    /**
     * Sets the camouflage on the block entity of either camouflage family: the Ethereal Platform's {@code TileCamo} (SimpleBlocks)
     * and the {@code CamoDeviceBlockEntity} of the Interface, Dislocator, Remote Placer and Golem Connector (TileBlocks). A tile of
     * neither kind is a harness error: {@link #camouflaged} has already forced {@code camo=true} on the block, which hides its
     * model, so a close-up without a camouflage would show nothing and the check would pass without having looked at anything.
     */
    private static void camouflage(BlockEntity tile) {
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        if (tile instanceof TileCamo camo) {
            camo.setCamo(planks);
        } else if (tile instanceof CamoDeviceBlockEntity device) {
            device.setCamo(planks);
        } else {
            throw new IllegalStateException("no camouflage can be set on " + tile + ": the close-up would show an invisible block");
        }
    }
}
