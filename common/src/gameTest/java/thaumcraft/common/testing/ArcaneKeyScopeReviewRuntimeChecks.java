package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.items.ItemKey;
import thaumcraft.common.lib.items.EquipmentRules;
import thaumcraft.common.tiles.TileOwned;

/** Real item linking in one installed dimension cannot authorize an unrelated same-coordinate owner in another. */
public final class ArcaneKeyScopeReviewRuntimeChecks {
    private ArcaneKeyScopeReviewRuntimeChecks() {}

    public static void keysCannotGrantAcrossDimensionsOrBlockKinds(GameTestHelper test) {
        try (var fixture = new Fixture(test)) {
            ServerLevel targetWorld = test.getLevel();
            ServerLevel sourceWorld = targetWorld.getServer().getLevel(Level.NETHER);
            test.assertTrue(sourceWorld != null && sourceWorld != targetWorld, "two actual installed server dimensions are available");
            BlockPos pos = test.absolutePos(new BlockPos(6, 2, 6)).atY(170);
            ServerPlayer owner = fixture.actor(sourceWorld, pos, "scope-owner");
            ServerPlayer guest = fixture.actor(sourceWorld, pos, "scope-guest");
            TileOwned source = fixture.plate(sourceWorld, pos, owner.getGameProfile().getName());
            TileOwned foreign = fixture.plate(targetWorld, pos, "unrelated-owner");
            ItemStack key = fixture.link(owner, pos);
            ItemStack loaded = ItemStack.parse(sourceWorld.registryAccess(), key.save(sourceWorld.registryAccess())).orElseThrow();
            test.assertTrue(ItemStack.matches(key, loaded), "the actual bound key preserves every component through native save/load");
            owner.getInventory().clearContent();
            owner.setItemInHand(InteractionHand.MAIN_HAND, loaded);
            owner.teleportTo(targetWorld, pos.getX() + .5, pos.getY() + 1, pos.getZ() + 1.5, 0, 0);
            fixture.use(owner, pos);
            test.assertTrue(!foreign.getAccessList().contains("1" + owner.getGameProfile().getName())
                            && owner.getMainHandItem().getCount() == 1,
                    "a key legitimately bound to the player's Nether plate cannot grant access to another owner's same-coordinate plate");

            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            guest.setItemInHand(InteractionHand.MAIN_HAND, loaded);
            source.setOwner("replacement-owner");
            fixture.use(guest, pos);
            test.assertTrue(source.getAccessList().isEmpty() && guest.getMainHandItem().getCount() == 1,
                    "a correctly dimensioned key from a former owner cannot authorize a replacement owner's same-kind lock at the same coordinates");
            source.setOwner(owner.getGameProfile().getName());
            // Keep every actual scope component, changing only the public legacy block-kind field.
            var original = ItemKey.link(loaded);
            loaded.set(ThaumcraftLegacyItems.KEY_LINK.get(), new ItemKey.KeyLink(original.location(), 0));
            fixture.use(guest, pos);
            test.assertTrue(!source.getAccessList().contains("1" + guest.getGameProfile().getName())
                            && guest.getMainHandItem().getCount() == 1,
                    "a door-kind link cannot authorize a pressure plate even in its own dimension");
            loaded.set(ThaumcraftLegacyItems.KEY_LINK.get(), original);
            fixture.use(guest, pos);
            test.assertTrue(source.getAccessList().contains("1" + guest.getGameProfile().getName()) && guest.getMainHandItem().isEmpty(),
                    "a correctly scoped gold key still transfers and spends exactly one physical key for a legitimate recipient");
            ItemStack delegated = fixture.link(guest, pos, false);
            var delegatedScope = delegated.get(ThaumcraftLegacyItems.KEY_SCOPE.get());
            test.assertTrue(delegated.is(ThaumcraftLegacyItems.KEY_IRON.get()) && delegatedScope != null
                            && delegatedScope.owner().equals(owner.getGameProfile().getName()),
                    "a gold-authorized issuer binds an iron key to the actual lock owner, not to the issuer's identity");
            var third = fixture.actor(sourceWorld, pos, "scope-third");
            third.setItemInHand(InteractionHand.MAIN_HAND, delegated);
            fixture.use(third, pos);
            test.assertTrue(source.getAccessList().contains("0" + third.getGameProfile().getName())
                            && !source.getAccessList().contains("1" + third.getGameProfile().getName())
                            && third.getMainHandItem().isEmpty(),
                    "the delegated iron key grants only ordinary access and pays exactly one physical key");
        }
        test.succeed();
    }

    public static void legacyUnscopedKeysDoNotGrantAccess(GameTestHelper test) {
        try (var fixture = new Fixture(test)) {
            var level = test.getLevel();
            BlockPos pos = test.absolutePos(new BlockPos(6, 2, 6)).atY(170);
            var guest = fixture.actor(level, pos, "legacy-guest");
            var foreign = fixture.plate(level, pos, "unrelated-owner");
            var key = new ItemStack(ThaumcraftLegacyItems.KEY_GOLD.get());
            var link = new ItemKey.KeyLink(EquipmentRules.keyLocation(pos.getX(), pos.getY(), pos.getZ()), 1);
            key.set(ThaumcraftLegacyItems.KEY_LINK.get(), link);
            guest.setItemInHand(InteractionHand.MAIN_HAND, key);
            fixture.use(guest, pos);
            test.assertTrue(foreign.getAccessList().isEmpty() && key.getCount() == 1 && link.equals(ItemKey.link(key)),
                    "an old key with no provable dimension retains its data/stock without gaining an arbitrary current-world owner scope");
            var owner = fixture.actor(level, pos, "legacy-owner");
            foreign.setOwner(owner.getGameProfile().getName());
            guest.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            owner.setItemInHand(InteractionHand.MAIN_HAND, key);
            fixture.use(owner, pos);
            test.assertTrue(key.getCount() == 1 && link.equals(ItemKey.link(key)) && foreign.getAccessList().isEmpty(),
                    "the proven current owner may explicitly rebind the existing legacy key without manufacturing stock or changing its public link");
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            guest.setItemInHand(InteractionHand.MAIN_HAND, key);
            fixture.use(guest, pos);
            test.assertTrue(guest.getMainHandItem().isEmpty() && foreign.getAccessList().contains("1" + guest.getGameProfile().getName()),
                    "only after the owner's explicit rebind does a legacy key grant a legitimate recipient access");
        }
        test.succeed();
    }

    private static final class Fixture implements AutoCloseable {
        private record Actor(ServerPlayer player, EmbeddedChannel channel) {}
        private record Cell(ServerLevel level, BlockPos pos, BlockState before) {}
        final GameTestHelper test;
        final List<Actor> actors = new ArrayList<>();
        final List<Cell> cells = new ArrayList<>();
        Fixture(GameTestHelper test) { this.test = test; }

        ServerPlayer actor(ServerLevel level, BlockPos pos, String name) {
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, actor,
                    CommonListenerCookie.createInitial(actor.getGameProfile(), false));
            actors.add(new Actor(actor, channel));
            actor.setGameMode(GameType.SURVIVAL); actor.setNoGravity(true);
            actor.getInventory().clearContent();
            actor.teleportTo(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + 1.5, 0, 0);
            test.assertTrue(actor.serverLevel() == level, "the real actor is in the intended installed source world");
            return actor;
        }

        TileOwned plate(ServerLevel level, BlockPos pos, String owner) {
            test.assertTrue(level.getBlockEntity(pos) == null, "the scope fixture cannot replace a foreign block entity");
            cells.add(new Cell(level, pos.immutable(), level.getBlockState(pos)));
            level.setBlock(pos, ThaumcraftEldritchBlocks.ARCANE_PRESSURE_PLATE.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            var owned = (TileOwned) level.getBlockEntity(pos);
            owned.setOwner(owner); owned.setChanged();
            return owned;
        }

        ItemStack link(ServerPlayer owner, BlockPos pos) {
            return link(owner, pos, true);
        }

        ItemStack link(ServerPlayer owner, BlockPos pos, boolean gold) {
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(gold
                    ? ThaumcraftLegacyItems.KEY_GOLD.get() : ThaumcraftLegacyItems.KEY_IRON.get()));
            use(owner, pos);
            ItemStack linked = ItemStack.EMPTY;
            for (int slot = 0; slot < owner.getInventory().items.size(); slot++) {
                ItemStack candidate = owner.getInventory().items.get(slot);
                if (!candidate.isEmpty() && ItemKey.link(candidate) != null) {
                    linked = candidate.copy(); owner.getInventory().items.set(slot, ItemStack.EMPTY); break;
                }
            }
            test.assertTrue(!linked.isEmpty() && ItemKey.link(linked).type() == 1,
                    "an ordinary authorized click binds the physical key to its real plate");
            return linked;
        }

        void use(ServerPlayer player, BlockPos pos) {
            var stack = player.getMainHandItem();
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        }

        @Override public void close() {
            for (var actor : actors) {
                actor.player().getServer().getPlayerList().remove(actor.player());
                actor.player().discard(); actor.channel().finishAndReleaseAll();
            }
            for (var cell : cells) cell.level().setBlock(cell.pos(), cell.before(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
