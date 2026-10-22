package thaumcraft.common.testing;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.container.ContainerTravelingTrunk;

/** Calls the real menu mutation entry points, not a replica of their guards. */
public final class PermissionRuntimeChecks {
    private PermissionRuntimeChecks() {}

    // Vanilla's makeMockServerPlayerInLevel anonymous subclass hardcodes
    // isSpectator=false and isCreative=true, regardless of setGameMode!
    private static final class Actor implements AutoCloseable {
        private final net.minecraft.network.Connection connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        private final io.netty.channel.embedded.EmbeddedChannel channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        private final ServerPlayer player;

        private Actor(GameTestHelper test) {
            var level = test.getLevel();
            player = new ServerPlayer(level.getServer(), level,
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "permission_actor"),
                    net.minecraft.server.level.ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL);
        }

        @Override public void close() {
            player.containerMenu = player.inventoryMenu;
            player.getInventory().clearContent();
            player.server.getPlayerList().remove(player);
            player.discard();
            connection.disconnect(net.minecraft.network.chat.Component.literal("permission test complete"));
            channel.finishAndReleaseAll();
        }
    }

    public static void trunkRejectsSpectator(GameTestHelper test) { trunk(test, "spectator"); }
    public static void trunkRejectsRemote(GameTestHelper test) { trunk(test, "remote"); }
    public static void trunkRejectsOtherViewer(GameTestHelper test) { trunk(test, "other_viewer"); }
    public static void trunkRejectsClosedMenu(GameTestHelper test) { trunk(test, "closed_menu"); }
    public static void trunkRejectsDeadViewer(GameTestHelper test) { trunk(test, "dead"); }

    public static void spectatorCannotBreakContainers(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        var before = level.getBlockState(pos);
        var actor = new Actor(test);
        ServerPlayer player = actor.player;
        player.setGameMode(GameType.SPECTATOR);
        test.assertTrue(player.isSpectator(), "denied-break actor really is a spectator");
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        var blocks = java.util.List.of(
                thaumcraft.common.config.ThaumcraftArcane.ARCANE_WORKBENCH.get(),
                thaumcraft.common.config.ThaumcraftDistillation.ALCHEMY_FURNACE.get(),
                thaumcraft.common.config.ThaumcraftDeconstruction.TABLE.get(),
                thaumcraft.common.config.ThaumcraftFocalManipulation.FOCAL_MANIPULATOR.get(),
                thaumcraft.common.config.ThaumcraftHungryChest.HUNGRY_CHEST.get());
        try {
            for (var block : blocks) {
                level.setBlockAndUpdate(pos, block.defaultBlockState());
                var tile = level.getBlockEntity(pos);
                var inventory = (net.minecraft.world.Container) tile;
                inventory.setItem(0, new ItemStack(Items.DIAMOND));
                test.assertTrue(inventory.getItem(0).is(Items.DIAMOND), "denied-break fixture contains its diamond: " + block);
                var saved = tile.saveWithFullMetadata(level.registryAccess());
                test.assertTrue(!player.gameMode.destroyBlock(pos), "spectator destroyed " + block);
                test.assertTrue(level.getBlockEntity(pos) == tile && level.getBlockState(pos).is(block)
                        && saved.equals(tile.saveWithFullMetadata(level.registryAccess())),
                        "denied break removed or evacuated " + block);
                inventory.clearContent();
            }
        } finally {
            level.setBlockAndUpdate(pos, before);
            actor.close();
        }
        test.succeed();
    }

    public static void researchConversionRejectsSpectator(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        var other = pos.east();
        var before = level.getBlockState(pos);
        var otherBefore = level.getBlockState(other);
        var table = thaumcraft.common.config.ThaumcraftContent.TABLE.get().defaultBlockState();
        var actor = new Actor(test);
        ServerPlayer player = actor.player;
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        player.setGameMode(GameType.SPECTATOR);
        test.assertTrue(player.isSpectator(), "conversion actor really is a spectator");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(thaumcraft.common.config.ThaumcraftContent.INKWELL.get()));
        level.setBlockAndUpdate(pos, table);
        level.setBlockAndUpdate(other, table);
        try {
            thaumcraft.common.blocks.BlockTable.tryCreateResearchTable(level, pos, player,
                    net.minecraft.world.InteractionHand.MAIN_HAND);
            test.assertTrue(level.getBlockState(pos) == table && level.getBlockState(other) == table,
                    "spectator converted research table blocks");
            test.assertTrue(player.getMainHandItem().getCount() == 1, "spectator conversion consumed inkwell");
        } finally {
            level.setBlockAndUpdate(pos, before);
            level.setBlockAndUpdate(other, otherBefore);
            actor.close();
        }
        test.succeed();
    }

    public static void crucibleRejectsRemoteWand(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        var before = level.getBlockState(pos);
        var actor = new Actor(test);
        ServerPlayer player = actor.player;
        level.setBlockAndUpdate(pos, thaumcraft.common.config.ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState());
        var tile = (thaumcraft.common.tiles.TileCrucible) level.getBlockEntity(pos);
        tile.fillWater(1000, true);
        player.setPos(pos.getX() + 40, pos.getY(), pos.getZ());
        player.setShiftKeyDown(true);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(thaumcraft.common.config.ThaumcraftContent.WAND.get()));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                net.minecraft.core.Direction.UP, pos, false);
        try {
            thaumcraft.common.blocks.BlockCrucible.tryCreate(new net.minecraft.world.item.context.UseOnContext(
                    player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
            test.assertTrue(tile.getWaterAmount() == 1000, "remote wand hook emptied crucible water");
        } finally {
            tile.spillRemnants();
            level.setBlockAndUpdate(pos, before);
            actor.close();
        }
        test.succeed();
    }

    private static void trunk(GameTestHelper test, String refusal) {
        var actors = new java.util.ArrayList<Actor>();
        var actor = new Actor(test);
        actors.add(actor);
        ServerPlayer player = actor.player;
        var trunk = ThaumcraftGolems.TRUNK.get().create(test.getLevel());
        var pos = test.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        trunk.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        trunk.setOwner(player);
        test.getLevel().addFreshEntity(trunk);
        player.setPos(trunk.position());
        var menu = new ContainerTravelingTrunk(42, player.getInventory(), trunk);
        player.containerMenu = menu;
        trunk.inventory.setItem(0, new ItemStack(Items.DIAMOND));
        switch (refusal) {
            case "spectator" -> {
                player.setGameMode(GameType.SPECTATOR);
                test.assertTrue(player.isSpectator(), "trunk actor really is a spectator");
            }
            case "remote" -> player.setPos(trunk.getX() + 40, trunk.getY(), trunk.getZ());
            case "closed_menu" -> player.containerMenu = player.inventoryMenu;
            case "dead" -> player.setHealth(0);
            case "other_viewer" -> {
                var other = new Actor(test);
                actors.add(other);
                player = other.player;
                player.setPos(trunk.position());
                player.containerMenu = menu;
            }
            default -> throw new IllegalArgumentException(refusal);
        }
        try {
            boolean stay = trunk.getStay();
            menu.clickMenuButton(player, ContainerTravelingTrunk.STAY_BUTTON);
            test.assertTrue(trunk.getStay() == stay, "unauthorized actor changed trunk stay state");
            menu.quickMoveStack(player, 0);
            test.assertTrue(trunk.inventory.getItem(0).is(Items.DIAMOND), "unauthorized actor shift-clicked trunk inventory");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(trunk.inventory.getItem(0).is(Items.DIAMOND), "unauthorized actor picked up trunk inventory");
        } finally {
            player.containerMenu = player.inventoryMenu;
            trunk.discard();
            for (var connected : actors) connected.close();
        }
        test.succeed();
    }
}
