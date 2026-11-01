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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BannerData;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.items.ItemBucketDeath;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemKey;
import thaumcraft.common.tiles.TileArcanePressurePlate;
import thaumcraft.common.tiles.TileBanner;
import thaumcraft.common.tiles.TileOwned;

/** November review regressions exercise native item/block dispatch, rather than prefilled tile state. */
public final class OwnedBlocksReviewRuntimeChecks {
    private OwnedBlocksReviewRuntimeChecks() {}

    public static void doorPlacesAndBothHalvesOpen(GameTestHelper test) {
        try (Fixture f = new Fixture(test)) {
            BlockPos lower = f.lane.origin, upper = lower.above();
            f.track(lower); f.track(upper); f.track(upper.above());
            var door = new ItemStack(ThaumcraftEldritchBlocks.ARCANE_DOOR_ITEM.get());
            f.player().setItemInHand(InteractionHand.MAIN_HAND, door);
            test.assertTrue(f.use(f.player(), lower.below(), Direction.UP).consumesAction(), "ordinary click on solid ground places the door");
            test.assertTrue(f.lane.level.getBlockState(lower).getBlock() instanceof BlockArcaneDoor
                    && f.lane.level.getBlockState(lower).getValue(BlockArcaneDoor.HALF) == Half.BOTTOM
                    && f.lane.level.getBlockState(upper).getValue(BlockArcaneDoor.HALF) == Half.TOP
                    && f.player().getMainHandItem().isEmpty(), "survival consumes one item and places exactly two correct halves");
            for (BlockPos pos : List.of(lower, upper)) {
                var owned = (TileOwned) f.lane.level.getBlockEntity(pos);
                test.assertTrue(owned.getOwner().equals(f.player().getGameProfile().getName()), "both halves belong to the real placer");
            }
            f.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            f.use(f.player(), upper, Direction.NORTH);
            test.assertTrue(f.lane.level.getBlockState(lower).getValue(BlockArcaneDoor.OPEN)
                    && f.lane.level.getBlockState(upper).getValue(BlockArcaneDoor.OPEN)
                    && f.lane.level.getBlockState(upper.above()).isAir(), "upper-half click opens the pair without touching air above it");
            f.use(f.player(), lower, Direction.NORTH);
            test.assertTrue(!f.lane.level.getBlockState(lower).getValue(BlockArcaneDoor.OPEN)
                    && !f.lane.level.getBlockState(upper).getValue(BlockArcaneDoor.OPEN), "lower-half click closes the same pair");

            BlockPos refused = lower.east(2); f.track(refused); f.track(refused.above());
            var spare = new ItemStack(ThaumcraftEldritchBlocks.ARCANE_DOOR_ITEM.get());
            f.player().setItemInHand(InteractionHand.MAIN_HAND, spare);
            f.player().setGameMode(GameType.ADVENTURE);
            f.use(f.player(), refused.below(), Direction.UP);
            test.assertTrue(spare.getCount() == 1 && f.lane.level.getBlockState(refused).isAir(), "Adventure placement without CanPlaceOn preserves stock/world");
        }
        test.succeed();
    }

    public static void ownedPlacementKeysAndReclaim(GameTestHelper test) {
        try (Fixture f = new Fixture(test)) {
            var owner = f.player(); var guest = f.actor().player;
            BlockPos platePos = f.lane.origin, glassPos = platePos.east(2);
            f.place(platePos, new ItemStack(ThaumcraftEldritchBlocks.ARCANE_PRESSURE_PLATE_ITEM.get()));
            var plate = (TileArcanePressurePlate) f.lane.level.getBlockEntity(platePos);
            test.assertTrue(plate.getOwner().equals(owner.getGameProfile().getName()), "ordinary BlockItem placement assigns the plate owner");
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            f.use(owner, platePos, Direction.UP);
            test.assertTrue(plate.getSetting() == 1, "the placer can configure the new plate");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftLegacyItems.KEY_GOLD.get()));
            f.use(owner, platePos, Direction.UP);
            ItemStack key = ItemStack.EMPTY;
            for (int slot = 0; slot < owner.getInventory().items.size(); slot++) {
                ItemStack candidate = owner.getInventory().items.get(slot);
                if (!candidate.isEmpty() && ItemKey.link(candidate) != null) {
                    key = candidate.copy(); owner.getInventory().items.set(slot, ItemStack.EMPTY); break;
                }
            }
            test.assertTrue(!key.isEmpty() && ItemKey.link(key).type() == 1, "a normal key click binds to the separate plate block");
            guest.setItemInHand(InteractionHand.MAIN_HAND, key);
            f.use(guest, platePos, Direction.UP);
            test.assertTrue(guest.getMainHandItem().isEmpty() && plate.getAccessList().contains("1" + guest.getGameProfile().getName()),
                    "the bound gold key is consumed once and grants its recipient access");
            var savedPlate = plate.saveWithFullMetadata(f.lane.level.registryAccess());
            plate.loadWithComponents(savedPlate, f.lane.level.registryAccess());
            test.assertTrue(plate.getOwner().equals(owner.getGameProfile().getName()) && plate.getAccessList().contains("1" + guest.getGameProfile().getName()),
                    "owner and granted access survive a real block entity save/load");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.WAND.get()));
            f.use(owner, platePos, Direction.UP);
            test.assertTrue(f.lane.level.getBlockState(platePos).isAir() && f.dropped(ThaumcraftEldritchBlocks.ARCANE_PRESSURE_PLATE_ITEM.get()) == 1,
                    "an ordinary wand click reclaims one owned plate rather than cycling its setting");

            f.place(glassPos, new ItemStack(ThaumcraftEldritchBlocks.WARDED_GLASS_ITEM.get()));
            var glass = (TileOwned) f.lane.level.getBlockEntity(glassPos);
            test.assertTrue(glass.getOwner().equals(owner.getGameProfile().getName()), "warded glass also belongs to its actual placer");
            var savedGlass = glass.saveWithFullMetadata(f.lane.level.registryAccess());
            glass.loadWithComponents(savedGlass, f.lane.level.registryAccess());
            guest.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.WAND.get()));
            f.use(guest, glassPos, Direction.UP);
            test.assertTrue(f.lane.level.getBlockState(glassPos).is(ThaumcraftEldritchBlocks.COSMETIC_OPAQUE_WARDED.get()), "a stranger cannot reclaim warded glass");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.WAND.get()));
            f.use(owner, glassPos, Direction.UP);
            test.assertTrue(f.lane.level.getBlockState(glassPos).isAir() && f.dropped(ThaumcraftEldritchBlocks.WARDED_GLASS_ITEM.get()) == 1,
                    "the owner reclaims one pane after its persisted owner was reloaded");
        }
        test.succeed();
    }

    public static void bannerUsesActualPhialAndHand(GameTestHelper test) {
        try (Fixture f = new Fixture(test)) {
            BlockPos pos = f.lane.origin;
            ItemStack bannerItem = new ItemStack(ThaumcraftEldritchBlocks.BANNER_ITEM.get());
            bannerItem.set(ThaumcraftEldritchBlocks.BANNER_DATA.get(), new BannerData(4, ""));
            f.place(pos, bannerItem);
            var banner = (TileBanner) f.lane.level.getBlockEntity(pos);
            var phials = ItemEssence.filled("aer"); phials.setCount(2);
            f.player().setItemInHand(InteractionHand.MAIN_HAND, phials);
            f.use(f.player(), pos, Direction.NORTH);
            test.assertTrue(banner.getAspect() == Aspect.AIR && phials.getCount() == 1, "a filled native phial applies its aspect and pays one unit");
            f.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var offhand = ItemEssence.filled("ignis");
            f.player().setItemInHand(InteractionHand.OFF_HAND, offhand);
            f.use(f.player(), pos, Direction.NORTH, InteractionHand.OFF_HAND);
            test.assertTrue(banner.getAspect() == Aspect.FIRE && offhand.isEmpty(), "the actual offhand phial is consumed, regardless of the last active hand");
            f.player().setGameMode(GameType.CREATIVE);
            f.player().setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled("aqua"));
            f.use(f.player(), pos, Direction.NORTH);
            test.assertTrue(banner.getAspect() == Aspect.WATER && f.player().getMainHandItem().isEmpty(), "original creative banner decoration also consumes its phial");
            f.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()));
            f.use(f.player(), pos, Direction.NORTH);
            test.assertTrue(banner.getAspect() == Aspect.WATER && f.player().getMainHandItem().getCount() == 1, "empty phials cannot invent an aspect or disappear");
            f.player().setShiftKeyDown(true);
            f.use(f.player(), pos, Direction.NORTH);
            test.assertTrue(banner.getAspect() == null && f.player().getMainHandItem().getCount() == 1, "secondary-use with a phial clears the aspect without spending another phial");
            var saved = banner.saveWithFullMetadata(f.lane.level.registryAccess());
            banner.loadWithComponents(saved, f.lane.level.registryAccess());
            test.assertTrue(banner.getAspect() == null && banner.getColor() == 4, "decoration state persists independently of the banner colour");
        }
        test.succeed();
    }

    public static void deathBucketHonorsPermissionsSolidsAndDrops(GameTestHelper test) {
        try (Fixture f = new Fixture(test)) {
            BlockPos pos = f.lane.origin;
            f.track(pos); f.track(pos.above()); f.track(pos.above(2)); f.track(pos.above(3));
            var player = f.player(); player.setPos(pos.getX() + .5, pos.getY() + 2, pos.getZ() + .5); player.setXRot(90); player.setYRot(0);
            var bucket = new ItemStack(ThaumcraftLegacyItems.BUCKET_DEATH.get()); player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
            player.setGameMode(GameType.ADVENTURE);
            test.assertTrue(!player.mayUseItemAt(pos, Direction.UP, bucket), "fixture is an actual Adventure player without placement permission");
            player.gameMode.useItem(player, f.lane.level, bucket, InteractionHand.MAIN_HAND);
            test.assertTrue(player.getMainHandItem() == bucket && bucket.getCount() == 1 && f.lane.level.getBlockState(pos).isAir(), "Adventure cannot pour without CanPlaceOn");
            player.setGameMode(GameType.SURVIVAL);
            player.gameMode.useItem(player, f.lane.level, bucket, InteractionHand.MAIN_HAND);
            test.assertTrue(player.getMainHandItem().is(Items.BUCKET) && f.lane.level.getBlockState(pos).is(thaumcraft.common.config.ThaumcraftLegacyTiles.DEATH_FLUID.get()),
                    "native survival use places death fluid and returns one empty bucket");
            f.lane.put(pos, Blocks.AIR.defaultBlockState());
            player.setGameMode(GameType.CREATIVE); bucket = new ItemStack(ThaumcraftLegacyItems.BUCKET_DEATH.get()); player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
            player.gameMode.useItem(player, f.lane.level, bucket, InteractionHand.MAIN_HAND);
            test.assertTrue(player.getMainHandItem() == bucket && bucket.getCount() == 1 && f.lane.level.getBlockState(pos).is(thaumcraft.common.config.ThaumcraftLegacyTiles.DEATH_FLUID.get()),
                    "creative pouring keeps its filled bucket while placing the same fluid");
            for (var block : List.of(Blocks.OAK_FENCE, Blocks.STONE_STAIRS, Blocks.GLASS)) {
                f.lane.put(pos, block.defaultBlockState());
                test.assertTrue(!ItemBucketDeath.pour(f.lane.level, pos) && f.lane.level.getBlockState(pos).is(block), "solid material survives even when it is not a full opaque cube: " + block);
            }
            f.lane.put(pos.below(), Blocks.DIRT.defaultBlockState()); f.lane.put(pos, Blocks.DANDELION.defaultBlockState());
            int before = f.dropped(Items.DANDELION);
            test.assertTrue(ItemBucketDeath.pour(f.lane.level, pos) && f.dropped(Items.DANDELION) == before + 1
                    && f.lane.level.getBlockState(pos).is(thaumcraft.common.config.ThaumcraftLegacyTiles.DEATH_FLUID.get()), "a replaceable flower drops exactly once before death fluid replaces it");
        }
        test.succeed();
    }

    private static final class Actor implements AutoCloseable {
        final ServerPlayer player;
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        Actor(Fixture f) {
            var profile = new GameProfile(UUID.randomUUID(), "owned-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(f.lane.level.getServer(), f.lane.level, profile, ClientInformation.createDefault());
            f.lane.level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true);
            player.setPos(f.lane.origin.getX() + 2.5, f.lane.origin.getY(), f.lane.origin.getZ() + 2.5);
        }
        @Override public void close() {
            try { player.server.getPlayerList().remove(player); }
            finally { player.discard(); channel.finishAndReleaseAll(); }
        }
    }

    private static final class Fixture implements AutoCloseable {
        final WaveLane lane;
        final List<Actor> actors = new ArrayList<>();
        Fixture(GameTestHelper test) { lane = new WaveLane(test, 3, 2, 3); lane.floor(3); actor(); }
        Actor actor() { var actor = new Actor(this); actors.add(actor); return actor; }
        ServerPlayer player() { return actors.getFirst().player; }
        void track(BlockPos pos) { lane.put(pos, Blocks.AIR.defaultBlockState()); }
        InteractionResult use(ServerPlayer player, BlockPos pos, Direction face) { return use(player, pos, face, InteractionHand.MAIN_HAND); }
        InteractionResult use(ServerPlayer player, BlockPos pos, Direction face, InteractionHand hand) {
            return player.gameMode.useItemOn(player, lane.level, player.getItemInHand(hand), hand,
                    new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false));
        }
        void place(BlockPos pos, ItemStack item) { track(pos); player().setItemInHand(InteractionHand.MAIN_HAND, item); use(player(), pos.below(), Direction.UP); }
        int dropped(Item item) {
            return lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(4), entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        }
        @Override public void close() {
            try { for (Actor actor : actors) actor.close(); }
            finally { lane.clean(); }
        }
    }
}
