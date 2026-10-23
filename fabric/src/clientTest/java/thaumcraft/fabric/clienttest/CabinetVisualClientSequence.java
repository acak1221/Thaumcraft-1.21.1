package thaumcraft.fabric.clienttest;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.tiles.TileJarNode;

/** A creative display cabinet: supplied contents, no crafting/progression claim. */
final class CabinetVisualClientSequence {
    private int step, ticks;
    private CompletableFuture<Void> action;
    private static final BlockPos JAR = new BlockPos(8, 162, 8);

    boolean tick(Minecraft client, Consumer<String> capture) {
        if (++ticks > 1200) throw new IllegalStateException("Cabinet visual timeout at " + step);
        switch (step) {
            case 0 -> {
                action = EldritchVisualClientSequence.onServer(client, player -> {
                    var level = player.server.getLevel(Level.OVERWORLD);
                    level.setDayTime(6000);
                    for (BlockPos pos : BlockPos.betweenClosed(0, 160, 0, 20, 168, 20))
                        level.setBlockAndUpdate(pos, pos.getY() == 160 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(JAR.below(), Blocks.QUARTZ_BLOCK.defaultBlockState());
                    level.setBlockAndUpdate(JAR, ThaumcraftEldritchBlocks.JAR_NODE.get().defaultBlockState());
                    var jar = (TileJarNode) level.getBlockEntity(JAR);
                    jar.setAspects(new AspectList().add(Aspect.AIR, 40).add(Aspect.FIRE, 40).add(Aspect.WATER, 40).add(Aspect.EARTH, 40));
                    jar.setChanged();
                    level.sendBlockUpdated(JAR, jar.getBlockState(), jar.getBlockState(), 3);
                    player.getInventory().clearContent();
                    player.getInventory().selected = 0;
                    player.getInventory().setItem(0, new ItemStack(ThaumcraftNodeDevices.ADVANCED_NODE_STABILIZER_ITEM.get()));
                    player.getInventory().setItem(1, new ItemStack(ThaumcraftContent.AURA_NODE.get()));
                    player.getInventory().setItem(2, new ItemStack(ThaumcraftContent.THAUMONOMICON.get()));
                    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftEquipment.VOID_ROBE_HELMET.get()));
                    player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ThaumcraftEquipment.VOID_ROBE_CHESTPLATE.get()));
                    player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ThaumcraftEquipment.VOID_ROBE_LEGGINGS.get()));
                    KnowledgeAccess.get(player).addResearch("THAUMONOMICON");
                    KnowledgeAccess.get(player).addResearch("RESEARCH");
                    KnowledgeEvents.sync(player);
                    player.teleportTo(level, 8.5, 161, 12.5, 180, 0);
                });
                next();
            }
            case 1 -> {
                if (!action.isDone()) return false;
                action.join();
                if (client.level.dimension() != Level.OVERWORLD || !(client.level.getBlockEntity(JAR) instanceof TileJarNode)
                        || !client.player.getMainHandItem().is(ThaumcraftNodeDevices.ADVANCED_NODE_STABILIZER_ITEM.get()) || ticks < 100) return false;
                EldritchVisualClientSequence.aim(client, Vec3.atCenterOf(JAR));
                next();
            }
            case 2 -> {
                if (ticks < 40) return false;
                next(); capture.accept("203-advanced-node-stabilizer-held-en.png");
            }
            case 3 -> { client.player.getInventory().selected = 1; next(); }
            case 4 -> {
                if (ticks < 40) return false;
                next(); capture.accept("204-aura-node-held-and-jar-node-en.png");
            }
            case 5 -> {
                client.player.getInventory().selected = 3;
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                client.player.setXRot(0); client.player.xRotO = 0;
                action = EldritchVisualClientSequence.onServer(client,
                        player -> player.teleportTo(player.serverLevel(), 14.5, 161, 15.5, 180, 0));
                next();
            }
            case 6 -> {
                if (!action.isDone()) return false;
                action.join();
                if (ticks < 60) return false;
                next(); capture.accept("205-void-robe-front-en.png");
            }
            case 7 -> { client.options.setCameraType(CameraType.THIRD_PERSON_BACK); next(); }
            case 8 -> {
                if (ticks < 60) return false;
                next(); capture.accept("206-void-robe-back-en.png");
            }
            case 9 -> {
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.player.getInventory().selected = 2;
                next();
            }
            case 10 -> {
                if (ticks < 20) return false;
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                next();
            }
            case 11 -> {
                if (!(client.screen instanceof GuiResearchBrowser) || ticks < 40) return false;
                next(); capture.accept("207-research-graph-parchment-en.png");
            }
            case 12 -> {
                client.setScreen(null);
                client.player.getInventory().selected = 3;
                action = EldritchVisualClientSequence.onServer(client, player -> {
                    var level = player.serverLevel();
                    level.setBlockAndUpdate(JAR, Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(JAR.below(), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(5, 161, 7), ThaumcraftInfusion.TALLOW_CANDLE.get().defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(7, 161, 7), ThaumcraftTaint.CRUST.get().defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(9, 161, 7), ThaumcraftEldritchBlocks.STAIRS_ELDRITCH.get().defaultBlockState());
                    // Contain the real finite gas, rather than freezing its simulation.
                    for (BlockPos pos : BlockPos.betweenClosed(10, 161, 6, 12, 163, 8))
                        level.setBlockAndUpdate(pos, Blocks.GLASS.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(11, 162, 7), ThaumcraftFlux.FLUX_GAS.get().defaultBlockState().setValue(FluxFiniteBlock.LEVEL, 7));
                    player.teleportTo(level, 8.5, 161, 13.5, 180, 0);
                });
                next();
            }
            case 13 -> {
                if (!action.isDone()) return false;
                action.join();
                if (ticks < 60) return false;
                EldritchVisualClientSequence.aim(client, new Vec3(8.5, 161.7, 7.5));
                next();
            }
            case 14 -> {
                if (ticks < 40) return false;
                next(); capture.accept("208-candle-crust-stairs-contained-flux-en.png");
            }
            default -> { return true; }
        }
        return false;
    }

    private void next() { step++; ticks = 0; }
}
