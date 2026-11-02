package thaumcraft.common.testing;

import com.example.tc4addon.SparkFocus;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.wands.WandHelper;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.WandFocusActions;

/** Executes the actual documented example source through a registered focus and native wand dispatch. */
public final class ExampleSparkReviewRuntimeChecks {
    private static SparkFocus focus;
    private ExampleSparkReviewRuntimeChecks() {}

    /** Called by the isolated GameTest initializer before the native item registry freezes. */
    public static void registerTestFocus() {
        if (focus != null) return;
        focus = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "review_example_spark"),
                new SparkFocus(new Item.Properties()));
    }

    public static void sparkRespectsWorldBorderAndAdventure(GameTestHelper test) {
        var level = test.getLevel();
        var base = test.absolutePos(new BlockPos(3, 2, 3)).atY(180);
        var target = base.offset(10, 1, 0);
        var fire = target.west();
        var border = level.getWorldBorder();
        double oldX = border.getCenterX(), oldZ = border.getCenterZ(), oldSize = border.getSize();
        test.assertTrue(border.getLerpRemainingTime() == 0, "the fresh test world has no foreign border transition");
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        for (var pos : new BlockPos[]{base, base.above(), target, fire, fire.below()}) {
            test.assertTrue(level.getBlockEntity(pos) == null, "the owned spark fixture replaces no foreign block entity");
            states.put(pos, level.getBlockState(pos));
        }
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "spark-review"), ClientInformation.createDefault());
        boolean joined = false;
        try {
            level.getServer().getPlayerList().placeNewPlayer(connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            joined = true;
            player.getInventory().clearContent();
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);
            player.setInvulnerable(true);
            player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() + .5, -90, 0);
            player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
            Vec3 direction = Vec3.atCenterOf(target).subtract(player.getEyePosition());
            float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
            float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z)));
            player.setYRot(yaw); player.setXRot(pitch); player.setYHeadRot(yaw);
            player.yRotO = yaw; player.xRotO = pitch; player.yHeadRotO = yaw;
            level.setBlock(base, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(base.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(target, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(fire.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(fire, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            border.setCenter(base.getX() + .5, base.getZ() + .5);
            border.setSize(64);
            test.assertTrue(level.hasChunkAt(target) && level.hasChunkAt(fire)
                            && level.getBlockState(target).is(Blocks.STONE)
                            && BaseFireBlock.canBePlacedAt(level, fire, Direction.WEST),
                    "loaded native fire site is otherwise placeable");
            HitResult hit = player.pick(16, 1, false);
            test.assertTrue(hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                            && block.getBlockPos().equals(target) && block.getDirection() == Direction.WEST,
                    "the actual example ray sees its target: hit=" + hit.getType() + "/" + hit.getLocation()
                            + (hit instanceof BlockHitResult actualBlock ? "/" + actualBlock.getBlockPos() + "/" + actualBlock.getDirection() : "")
                            + ", expected=" + target + "/WEST, eye=" + player.getEyePosition()
                            + ", rotation=" + player.getYRot() + "/" + player.getXRot()
                            + ", actorLevel=" + player.level().dimension().location()
                            + ", fixtureLevel=" + level.dimension().location());
            test.assertTrue(player.pick(player.blockInteractionRange(), 1, true).getType() == HitResult.Type.MISS,
                    "ordinary short wand reach misses, so the actual air-use focus dispatcher owns this cast");

            // The independent positive control proves the actual registered example can cast and pay.
            border.setSize(64);
            player.setGameMode(GameType.SURVIVAL);
            ItemStack allowed = chargedWand();
            player.setItemInHand(InteractionHand.MAIN_HAND, allowed);
            var price = FocusVis.price(focus.getVisCost(new ItemStack(focus)), allowed, player);
            var result = player.gameMode.useItem(player, level, allowed, InteractionHand.MAIN_HAND);
            test.assertTrue(result.consumesAction() && level.getBlockState(fire).is(Blocks.FIRE),
                    "allowed native example dispatch actually ignites its declared destination");
            for (String primal : WandData.PRIMALS)
                test.assertTrue(ItemWandCasting.getVis(allowed, primal) == 10000 - price.getOrDefault(primal, 0),
                        "allowed example pays the exact native discounted price: " + primal);
            level.setBlock(fire, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

            for (boolean outside : new boolean[]{true, false}) {
                WandFocusActions.clearCooldown(player);
                border.setSize(outside ? 16 : 64);
                player.setGameMode(outside ? GameType.SURVIVAL : GameType.ADVENTURE);
                test.assertTrue(border.isWithinBounds(player.blockPosition())
                                && border.isWithinBounds(fire) != outside,
                        "the actor stays inside while only the intended destination is border-denied");
                ItemStack refused = chargedWand();
                player.setItemInHand(InteractionHand.MAIN_HAND, refused);
                ItemStack before = refused.copy();
                var denied = player.gameMode.useItem(player, level, refused, InteractionHand.MAIN_HAND);
                test.assertTrue(level.getBlockState(fire).isAir() && ItemStack.matches(before, refused)
                                && !denied.consumesAction(),
                        (outside ? "out-of-border" : "adventure")
                                + " example cast must refuse before ignition, vis payment or stack mutation");
            }
        } finally {
            border.setCenter(oldX, oldZ); border.setSize(oldSize);
            states.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
            WandFocusActions.clearCooldown(player);
            if (joined) level.getServer().getPlayerList().remove(player);
            player.discard(); channel.finishAndReleaseAll();
        }
        test.succeed();
    }

    private static ItemStack chargedWand() {
        if (focus == null) throw new IllegalStateException("Register the actual example test focus before registry freeze");
        var wand = new ItemStack(ThaumcraftContent.WAND.get());
        var vis = new LinkedHashMap<String, Integer>();
        for (String primal : WandData.PRIMALS) vis.put(primal, 10000);
        ItemWandCasting.withData(wand, new WandData("silverwood", "gold", vis));
        if (!WandHelper.setFocus(wand, new ItemStack(focus))) throw new IllegalStateException("Example focus was not mountable");
        return wand;
    }
}
