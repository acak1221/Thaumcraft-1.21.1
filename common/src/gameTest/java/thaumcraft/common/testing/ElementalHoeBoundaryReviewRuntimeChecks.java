package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftNaturals;

/** The actual native world border clips an area-use tool, just like its anchor click. */
public final class ElementalHoeBoundaryReviewRuntimeChecks {
    private ElementalHoeBoundaryReviewRuntimeChecks() {}

    public static void nativeWorldBorderStopsNeighbourTilling(GameTestHelper test) {
        var level = test.getLevel();
        var border = level.getWorldBorder();
        var previousBorder = border.createSettings();
        BlockPos origin = test.absolutePos(new BlockPos(5, 2, 5)).atY(170);
        BlockPos clicked = origin.east(2);
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var actor = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "review-hoe"), ClientInformation.createDefault());
        boolean joined = false;
        try {
            for (BlockPos mutable : BlockPos.betweenClosed(clicked.offset(-1, -1, -1), clicked.offset(1, 1, 1))) {
                BlockPos pos = mutable.immutable();
                test.assertTrue(level.getBlockEntity(pos) == null, "hoe border fixture must not replace a foreign block entity");
                saved.put(pos, level.getBlockState(pos));
            }
            for (BlockPos pos : saved.keySet()) level.setBlock(pos,
                    pos.getY() < clicked.getY() ? Blocks.STONE.defaultBlockState()
                            : pos.getY() == clicked.getY() ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            level.getServer().getPlayerList().placeNewPlayer(connection, actor,
                    CommonListenerCookie.createInitial(actor.getGameProfile(), false));
            joined = true;
            actor.setGameMode(GameType.SURVIVAL);
            actor.setPos(clicked.getX() + .5, clicked.getY() + 1, clicked.getZ() + .5);
            actor.setNoGravity(true);
            ItemStack hoe = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "elemental_hoe")));
            test.assertTrue(!hoe.isEmpty(), "the actual Hoe of Growth is registered");
            actor.setItemInHand(InteractionHand.MAIN_HAND, hoe);
            border.setCenter(origin.getX() + 1.0, origin.getZ() + .5);
            border.setSize(4);
            test.assertTrue(border.isWithinBounds(clicked) && !border.isWithinBounds(clicked.east()),
                    "the admitted anchor is inside and its eastern neighbours are outside the real native border");

            var result = hoe.useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)));
            test.assertTrue(result.consumesAction(), "the legitimate in-border area use remains available");
            int tilled = 0;
            for (BlockPos pos : BlockPos.betweenClosed(clicked.offset(-1, 0, -1), clicked.offset(1, 0, 1))) {
                boolean allowed = border.isWithinBounds(pos);
                test.assertTrue(level.getBlockState(pos).is(allowed ? Blocks.FARMLAND : Blocks.DIRT),
                        "Hoe of Growth must retain forbidden neighbouring stock at " + pos + ", allowed=" + allowed);
                if (allowed) tilled++;
            }
            test.assertTrue(tilled == 6 && hoe.getDamageValue() == 6,
                    "only the six legitimate native tills consume tool durability");
            BlockPos refused = clicked.east();
            actor.setShiftKeyDown(true);
            hoe.useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(refused), Direction.UP, refused, false)));
            test.assertTrue(level.getBlockState(refused).is(Blocks.DIRT) && hoe.getDamageValue() == 6,
                    "the direct sneaking single-block path also refuses an out-of-border anchor before durability");
            actor.setShiftKeyDown(false);
            for (BlockPos pos : BlockPos.betweenClosed(clicked.offset(-1, 0, -1), clicked.offset(1, 0, 1)))
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(refused.below(), Blocks.FARMLAND.defaultBlockState(), 3);
            level.setBlock(refused, Blocks.WHEAT.defaultBlockState(), 3);
            hoe.useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(refused), Direction.UP, refused, false)));
            test.assertTrue(level.getBlockState(refused) == Blocks.WHEAT.defaultBlockState() && hoe.getDamageValue() == 6,
                    "the bonemeal fallback cannot grow a forbidden crop or charge wear");
            level.setBlock(refused, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(refused.below(), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(refused, ThaumcraftNaturals.GREATWOOD_SAPLING.get().defaultBlockState(), 3);
            hoe.useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(refused), Direction.UP, refused, false)));
            test.assertTrue(level.getBlockState(refused).is(ThaumcraftNaturals.GREATWOOD_SAPLING.get()) && hoe.getDamageValue() == 6,
                    "the special magical-sapling fallback also respects the forbidden source before growth/wear");
        } finally {
            border.applySettings(previousBorder);
            if (joined) level.getServer().getPlayerList().remove(actor);
            actor.discard();
            channel.finishAndReleaseAll();
            for (var entry : saved.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        test.succeed();
    }
}
