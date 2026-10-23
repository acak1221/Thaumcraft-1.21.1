package thaumcraft.fabric.clienttest;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.tiles.TileMirror;

/** Bind an ordinary supplied mirror item and place it through the real client handler. */
final class MirrorVisualClientSequence {
    private static final BlockPos FIRST = new BlockPos(5, 162, 16);
    private static final BlockPos SECOND = new BlockPos(9, 162, 16);
    private CompletableFuture<Void> action;
    private int step, ticks, boundSlot = -1;

    boolean tick(Minecraft client, Consumer<String> capture) {
        if (++ticks > 1200) throw new IllegalStateException("Mirror visual timeout at " + step);
        switch (step) {
            case 0 -> {
                client.player.getInventory().selected = 0;
                action = EldritchVisualClientSequence.onServer(client, player -> {
                    var level = player.serverLevel();
                    level.setBlockAndUpdate(FIRST.north(), Blocks.QUARTZ_BLOCK.defaultBlockState());
                    level.setBlockAndUpdate(SECOND.north(), Blocks.QUARTZ_BLOCK.defaultBlockState());
                    level.setBlockAndUpdate(FIRST, ThaumcraftEldritchBlocks.MIRROR.get().defaultBlockState().setValue(BlockMirror.FACING, Direction.SOUTH));
                    player.getInventory().clearContent();
                    player.getInventory().selected = 0;
                    player.getInventory().setItem(0, new ItemStack(ThaumcraftEldritchBlocks.MIRROR_ITEM.get()));
                    player.teleportTo(level, 5.5, 161, 20.5, 180, 0);
                });
                next();
            }
            case 1 -> {
                if (!action.isDone()) return false;
                action.join();
                if (!(client.level.getBlockEntity(FIRST) instanceof TileMirror) || ticks < 60
                        || !client.player.getMainHandItem().is(ThaumcraftEldritchBlocks.MIRROR_ITEM.get())) return false;
                EldritchVisualClientSequence.aim(client, Vec3.atCenterOf(FIRST));
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(5.5, 162.5, 16.02), Direction.SOUTH, FIRST, false));
                next();
            }
            case 2 -> {
                for (int i = 0; i < 9; i++) {
                    if (client.player.getInventory().getItem(i).has(ThaumcraftLegacyItems.MIRROR_LINK.get())) boundSlot = i;
                }
                if (boundSlot < 0 || ticks < 20) return false;
                client.player.getInventory().selected = boundSlot;
                action = EldritchVisualClientSequence.onServer(client,
                        player -> player.teleportTo(player.serverLevel(), 9.5, 161, 20.5, 180, 0));
                next();
            }
            case 3 -> {
                if (!action.isDone()) return false;
                action.join();
                if (ticks < 60) return false;
                EldritchVisualClientSequence.aim(client, Vec3.atCenterOf(SECOND));
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(9.5, 162.5, 16), Direction.SOUTH, SECOND.north(), false));
                next();
            }
            case 4 -> {
                if (!(client.level.getBlockEntity(FIRST) instanceof TileMirror first) || !first.isLinked()
                        || !(client.level.getBlockEntity(SECOND) instanceof TileMirror second) || !second.isLinked()) return false;
                action = EldritchVisualClientSequence.onServer(client, player -> {
                    var firstServer = (TileMirror) player.serverLevel().getBlockEntity(FIRST);
                    var secondServer = (TileMirror) player.serverLevel().getBlockEntity(SECOND);
                    if (!firstServer.isLinkValid() || !secondServer.isLinkValid())
                        throw new IllegalStateException("Native placement did not establish a reciprocal mirror link");
                    player.teleportTo(player.serverLevel(), 9.5, 161, 19, 180, 0);
                });
                client.player.getInventory().selected = 8;
                next();
            }
            case 5 -> {
                if (!action.isDone()) return false;
                action.join();
                if (ticks < 60) return false;
                EldritchVisualClientSequence.aim(client, Vec3.atCenterOf(SECOND));
                next();
            }
            case 6 -> {
                if (ticks < 40) return false;
                next(); capture.accept("209-linked-mirror-projected-field-front-en.png");
            }
            case 7 -> {
                action = EldritchVisualClientSequence.onServer(client,
                        player -> player.teleportTo(player.serverLevel(), 11.5, 161, 19, 180, 0));
                next();
            }
            case 8 -> {
                if (!action.isDone()) return false;
                action.join();
                if (ticks < 60) return false;
                EldritchVisualClientSequence.aim(client, Vec3.atCenterOf(SECOND));
                next();
            }
            case 9 -> {
                if (ticks < 40) return false;
                next(); capture.accept("210-linked-mirror-projected-field-angle-en.png");
            }
            default -> { return true; }
        }
        return false;
    }

    private void next() { step++; ticks = 0; }
}
