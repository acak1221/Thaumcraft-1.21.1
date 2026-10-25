package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import java.util.UUID;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileMirror;
import thaumcraft.common.tiles.TileMirrorEssentia;

/** Real item placement and transfer regressions; the fixtures are not survival-acquisition acceptance. */
public final class MirrorRuntimeChecks {
    private MirrorRuntimeChecks() {}

    public static void singletonPlacementRetainsBothMirrorKinds(GameTestHelper test) {
        try (Fixture f = new Fixture(test)) {
            for (boolean essentia : new boolean[] {false, true}) {
                f.player.getInventory().clearContent();
                Item item = essentia ? essentiaRecipeResult(test).getItem() : ThaumcraftEldritchBlocks.MIRROR_ITEM.get();
                BlockPos first = f.origin.offset(essentia ? 0 : -3, 0, 0), second = first.south(3);
                f.placeWithItem(first, new ItemStack(item));
                test.assertTrue(f.player.getMainHandItem().isEmpty(), "the real native placement consumes the singleton survival item");
                f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                f.use(first, Direction.UP);
                test.assertTrue(f.player.getMainHandItem().isEmpty(), "binding consumes the unbound item and returns one linked item to inventory");
                ItemStack bound = ItemStack.EMPTY;
                for (int slot = 0; slot < f.player.getInventory().getContainerSize(); slot++) {
                    ItemStack stack = f.player.getInventory().getItem(slot);
                    if (stack.is(item) && stack.has(ThaumcraftLegacyItems.MIRROR_LINK.get())) {
                        bound = f.player.getInventory().removeItemNoUpdate(slot);
                        break;
                    }
                }
                test.assertTrue(bound.getCount() == 1, "binding returned exactly one physical linked mirror");
                f.placeWithItem(second, bound);
                test.assertTrue(f.player.getMainHandItem().isEmpty(), "placing the linked singleton really reaches count zero");
                BlockEntity a = f.level.getBlockEntity(first), b = f.level.getBlockEntity(second);
                test.assertTrue(valid(a) && valid(b), "both mirrors retain reciprocal links after native singleton consumption");
                test.assertTrue((a instanceof TileMirrorEssentia) == essentia && (b instanceof TileMirrorEssentia) == essentia,
                        "the initial placement state creates the correct BE, not an item BE patched after placement");
                test.assertTrue(f.level.getBlockState(second).getBlock().getCloneItemStack(f.level, second, f.level.getBlockState(second)).is(item),
                        "pick-block keeps the native mirror kind");
                var drops = Block.getDrops(f.level.getBlockState(second), f.level, second, b);
                test.assertTrue(drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().has(net.minecraft.core.component.DataComponents.CUSTOM_DATA),
                        "the real drop keeps its kind and partner data");
                f.put(f.level, second, Blocks.AIR.defaultBlockState());
                f.placeWithItem(second, drops.getFirst());
                test.assertTrue(valid(f.level.getBlockEntity(first)) && valid(f.level.getBlockEntity(second)),
                        "a dropped singleton mirror can restore the same pair");
            }
            test.assertTrue(ThaumcraftEldritchBlocks.MIRROR.get().asItem() == ThaumcraftEldritchBlocks.MIRROR_ITEM.get(),
                    "the new variant does not replace the existing block-to-item identity");
        }
        test.succeed();
    }

    public static void crossDimensionPairsTransferWithoutLosingStock(GameTestHelper test) {
        try (Fixture f = new Fixture(test)) {
            ServerLevel other = f.level.getServer().getLevel(f.level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER);
            test.assertTrue(other != null && other != f.level, "the second real server dimension exists");
            BlockPos local = f.origin, remote = new BlockPos(local.getX(), Math.max(other.getMinBuildHeight() + 4, 80), local.getZ());
            f.hold(other, remote);
            for (boolean essentia : new boolean[] {false, true}) {
                BlockState state = ThaumcraftEldritchBlocks.MIRROR.get().defaultBlockState().setValue(BlockMirror.ESSENTIA, essentia).setValue(BlockMirror.FACING, Direction.UP);
                // Switching a property on the same block must not keep the preceding kind's BE.
                f.put(f.level, local, Blocks.AIR.defaultBlockState()); f.put(other, remote, Blocks.AIR.defaultBlockState());
                f.put(f.level, local.below(), Blocks.STONE.defaultBlockState());
                f.put(other, remote.below(), Blocks.STONE.defaultBlockState());
                f.put(f.level, local, state); f.put(other, remote, state);
                link(f.level.getBlockEntity(local), remote, other);
                restore(f.level.getBlockEntity(local));
                test.assertTrue(valid(f.level.getBlockEntity(local)) && valid(other.getBlockEntity(remote)), "full checks accept both reciprocal cross-dimension links");
                test.assertTrue(simple(f.level.getBlockEntity(local)) && simple(other.getBlockEntity(remote)), "the cheap ticker checks use the same own-world rule");
                if (essentia) {
                    TileJar jar = f.jar(other, remote.above(), 3);
                    test.assertTrue(((TileMirrorEssentia) f.level.getBlockEntity(local)).takeFromContainer(Aspect.AIR, 1) && jar.getAmount() == 2,
                            "a real source beyond the dimension boundary pays exactly one essentia");
                } else {
                    ItemStack input = new ItemStack(Items.DIAMOND, 3);
                    input.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("mirror-regression"));
                    ItemEntity entity = new ItemEntity(f.level, local.getX() + .5, local.getY(), local.getZ() + .5, input.copy());
                    f.entities.add(entity); f.level.addFreshEntity(entity);
                    f.level.getBlockState(local).entityInside(f.level, local, entity);
                    TileMirror twin = (TileMirror) other.getBlockEntity(remote);
                    test.assertTrue(entity.isRemoved() && twin.getOutputStacks().size() == 1 && ItemStack.matches(input, twin.getOutputStacks().getFirst()),
                            "real item ingress pays one exact count/component stack to the cross-dimension buffer");
                }
                for (var endpoint : List.of(new Endpoint(f.level, local), new Endpoint(other, remote))) {
                    BlockEntity before = endpoint.level.getBlockEntity(endpoint.pos);
                    CompoundTag saved = before.saveWithFullMetadata(endpoint.level.registryAccess());
                    endpoint.level.removeBlockEntity(endpoint.pos);
                    BlockEntity loaded = BlockEntity.loadStatic(endpoint.pos, endpoint.level.getBlockState(endpoint.pos), saved, endpoint.level.registryAccess());
                    test.assertTrue(loaded != null, "native BE reload succeeds");
                    endpoint.level.setBlockEntity(loaded);
                }
                test.assertTrue(valid(f.level.getBlockEntity(local)) && valid(other.getBlockEntity(remote)), "cross-dimension links survive native serialization/reload");
            }
        }
        test.succeed();
    }

    public static void essentiaMirrorDiscoversReplacementSourceAfterBackoff(GameTestHelper test) {
        Fixture f = new Fixture(test, 4096);
        try {
            BlockPos local = f.origin, twin = local.east(3), oldSource = twin.above(), newSource = twin.above().south();
            BlockState state = ThaumcraftEldritchBlocks.MIRROR.get().defaultBlockState().setValue(BlockMirror.ESSENTIA, true).setValue(BlockMirror.FACING, Direction.UP);
            for (BlockPos pos : List.of(local, twin)) { f.put(f.level, pos.below(), Blocks.STONE.defaultBlockState()); f.put(f.level, pos, state); }
            link(f.level.getBlockEntity(local), twin, f.level); restore(f.level.getBlockEntity(local));
            var mirror = (TileMirrorEssentia) f.level.getBlockEntity(local);
            TileJar old = f.jar(f.level, oldSource, 2);
            test.assertTrue(mirror.takeFromContainer(Aspect.AIR, 1) && old.getAmount() == 1, "the actual mirror API populates its source cache and debits once");
            f.put(f.level, oldSource, Blocks.AIR.defaultBlockState());
            test.assertTrue(!mirror.takeFromContainer(Aspect.AIR, 1), "the retired cached source cannot pay");
            TileJar next = f.jar(f.level, newSource, 5);
            test.assertTrue(!mirror.takeFromContainer(Aspect.AIR, 1) && next.getAmount() == 5, "real wall-clock backoff prevents immediate re-scan/debit");
            poll(test, f, mirror, next, System.currentTimeMillis() + 5100L, System.nanoTime() + 15_000_000_000L);
        } catch (RuntimeException | Error failure) { f.close(); throw failure; }
    }

    private static void poll(GameTestHelper test, Fixture f, TileMirrorEssentia mirror, TileJar next, long deadline, long hardDeadlineNanos) {
        test.runAfterDelay(1, () -> {
            try {
                test.assertTrue(System.nanoTime() < hardDeadlineNanos, "mirror cache regression completes within fifteen real seconds");
                if (System.currentTimeMillis() < deadline) {
                    if (System.currentTimeMillis() < deadline - 200L) test.assertTrue(!mirror.takeFromContainer(Aspect.AIR, 1) && next.getAmount() == 5,
                            "repeated real requests do not reset or skip the original backoff");
                    poll(test, f, mirror, next, deadline, hardDeadlineNanos); return;
                }
                test.assertTrue(System.currentTimeMillis() >= deadline, "five seconds elapsed without replacing clocks or shortening production backoff");
                test.assertTrue(mirror.takeFromContainer(Aspect.AIR, 1) && next.getAmount() == 4, "the living mirror discovers the new source and consumes exactly one unit");
                f.close(); test.succeed();
            } catch (RuntimeException | Error failure) { f.close(); throw failure; }
        });
    }

    private static ItemStack essentiaRecipeResult(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        InfusionStackRecipe recipe = level.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get()).stream()
                .map(holder -> holder.value()).filter(value -> value.research().equals("MIRRORESSENTIA")).findFirst().orElseThrow();
        var input = new InfusionRecipeInput(new ItemStack(ThaumcraftContent.MATERIALS.get("mirrored_glass").get()),
                List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND), new ItemStack(Items.ENDER_PEARL)));
        test.assertTrue(recipe.matches(input, level) && recipe.instability() == 2 && recipe.aspects().equals(Map.of("iter", 8, "aqua", 8, "permutatio", 8)),
                "the loaded survival infusion recipe retains the original research, components, instability and costs");
        ItemStack result = recipe.assemble(input, level.registryAccess());
        test.assertTrue(result.is(ThaumcraftEldritchBlocks.MIRROR_ESSENTIA_ITEM.get()) && result.getCount() == 1,
                "the actual registered recipe produces the placeable essentia mirror item");
        return result;
    }

    private static boolean valid(BlockEntity tile) { return tile instanceof TileMirror mirror ? mirror.isLinkValid() : tile instanceof TileMirrorEssentia essentiaMirror && essentiaMirror.isLinkValid(); }
    private static boolean simple(BlockEntity tile) { return tile instanceof TileMirror mirror ? mirror.isLinkValidSimple() : tile instanceof TileMirrorEssentia essentiaMirror && essentiaMirror.isLinkValidSimple(); }
    private static void restore(BlockEntity tile) { if (tile instanceof TileMirror mirror) mirror.restoreLink(); else ((TileMirrorEssentia) tile).restoreLink(); }
    private static void link(BlockEntity tile, BlockPos twin, ServerLevel destination) {
        if (tile instanceof TileMirror mirror) { mirror.linkX = twin.getX(); mirror.linkY = twin.getY(); mirror.linkZ = twin.getZ(); mirror.linkDim = destination.dimension().location().toString(); }
        else { var mirror = (TileMirrorEssentia) tile; mirror.linkX = twin.getX(); mirror.linkY = twin.getY(); mirror.linkZ = twin.getZ(); mirror.linkDim = destination.dimension().location().toString(); }
    }

    private record Endpoint(ServerLevel level, BlockPos pos) {}
    private record Saved(BlockState state, CompoundTag tag) {}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test;
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos origin;
        final Map<Endpoint, Saved> saved = new LinkedHashMap<>();
        final Map<ServerLevel, List<ChunkPos>> held = new LinkedHashMap<>();
        final List<ItemEntity> entities = new ArrayList<>();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final List<ItemStack> inventory;
        final Vec3 playerPosition;
        final GameType mode;
        boolean closed;
        Fixture(GameTestHelper test) { this(test, 0); }
        Fixture(GameTestHelper test, int offset) {
            this.test = test; level = test.getLevel(); origin = test.absolutePos(new BlockPos(4, 2, 4)).offset(0, 0, offset);
            var profile = new GameProfile(UUID.randomUUID(), "mirror-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                    "owned connected native survival player, without GameTest mock creative overrides");
            inventory = player.getInventory().items.stream().map(ItemStack::copy).toList();
            playerPosition = player.position(); mode = player.gameMode.getGameModeForPlayer();
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setPos(origin.getX() + 1.5, origin.getY(), origin.getZ() + 1.5);
            try { hold(level, origin); } catch (RuntimeException | Error failure) { close(); throw failure; }
        }
        void hold(ServerLevel world, BlockPos pos) { held.computeIfAbsent(world, ignored -> new ArrayList<>()).addAll(GolemChunks.hold(world, List.of(pos))); }
        void remember(ServerLevel world, BlockPos pos) {
            saved.computeIfAbsent(new Endpoint(world, pos.immutable()), key -> {
                var tile = world.getBlockEntity(pos); return new Saved(world.getBlockState(pos), tile == null ? null : tile.saveWithFullMetadata(world.registryAccess()));
            });
        }
        void put(ServerLevel world, BlockPos pos, BlockState state) { remember(world, pos); world.setBlock(pos, state, Block.UPDATE_ALL); }
        void placeWithItem(BlockPos pos, ItemStack item) {
            put(level, pos.below(), Blocks.STONE.defaultBlockState()); remember(level, pos);
            player.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + 1.5);
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            use(pos.below(), Direction.UP);
            test.assertTrue(level.getBlockState(pos).is(ThaumcraftEldritchBlocks.MIRROR.get()), "native item use placed a mirror");
        }
        void use(BlockPos pos, Direction side) {
            test.assertTrue(player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos).relative(side, .5), side, pos, false)).consumesAction(), "real native item use is accepted");
        }
        TileJar jar(ServerLevel world, BlockPos pos, int amount) {
            put(world, pos, ThaumcraftContent.JAR.get().defaultBlockState());
            TileJar jar = (TileJar) world.getBlockEntity(pos);
            test.assertTrue(jar.addToContainer(Aspect.AIR, amount) == 0, "finite native jar stock fixture is accepted"); return jar;
        }
        @Override public void close() {
            if (closed) return; closed = true;
            try {
                entities.forEach(ItemEntity::discard);
                var entries = new ArrayList<>(saved.entrySet()); java.util.Collections.reverse(entries);
                for (var entry : entries) {
                    var key = entry.getKey(); var value = entry.getValue();
                    key.level.removeBlockEntity(key.pos);
                    key.level.setBlock(key.pos, value.state, Block.UPDATE_ALL);
                    if (value.tag != null && key.level.getBlockEntity(key.pos) != null)
                        key.level.getBlockEntity(key.pos).loadWithComponents(value.tag, key.level.registryAccess());
                }
            } finally {
                try { held.forEach((world, chunks) -> chunks.forEach(chunk -> world.setChunkForced(chunk.x, chunk.z, false))); }
                finally {
                    try { if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player); }
                    finally { player.discard(); channel.finishAndReleaseAll(); }
                }
            }
        }
    }
}
