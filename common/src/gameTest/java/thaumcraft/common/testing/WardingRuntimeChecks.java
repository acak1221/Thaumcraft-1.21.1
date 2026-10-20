package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.blocks.BlockWarded;
import thaumcraft.common.config.ThaumcraftArchitect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftWards;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandArchitect;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusWarding;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.network.WandArchitectNetworking;
import thaumcraft.common.lib.world.WardingCasts;
import thaumcraft.common.tiles.TileWarded;

/** Actual connected owners, real held wand dispatch, native explosion callback/piston resolver/mining and exact finite resource checks. */
public final class WardingRuntimeChecks {
    private WardingRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> check.stage(check::first));
        test.runAfterDelay(16, () -> check.stage(check::foreign));
        test.runAfterDelay(27, () -> check.stage(() -> { check.restore(); check.finish(); test.succeed(); }));
    }
    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos base;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>(), wall = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final List<ServerPlayer> players = new ArrayList<>();
        final List<EmbeddedChannel> channels = new ArrayList<>();
        ServerPlayer owner, stranger;
        ItemStack charged, architect;
        BlockState log;
        BlockPos primary, wallRoot;
        boolean finished;

        Check(GameTestHelper test) { this.test = test; level = test.getLevel(); base = test.absolutePos(new BlockPos(3, 100, 3)); }
        void stage(Runnable body) { if (finished) return; try { body.run(); } catch (RuntimeException | Error failure) { finish(); throw failure; } }
        void prepare() {
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-5, -2, -2), base.offset(15, 6, 24))) {
                test.assertTrue(level.getBlockEntity(pos) == null, "ward fixture never overwrites a block inventory");
                saved.put(pos.immutable(), level.getBlockState(pos));
            }
            forced.addAll(GolemChunks.hold(level, saved.keySet()));
            saved.forEach((pos, state) -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
        }
        ServerPlayer player(String name) {
            var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            channels.add(channel); players.add(player);
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setNoGravity(true); player.setGameMode(GameType.SURVIVAL);
            test.assertTrue(level.getServer().getPlayerList().getPlayer(player.getUUID()) == player, "actual connected player: " + name);
            return player;
        }
        ItemStack focus(boolean area) {
            ItemStack focus = new ItemStack(ThaumcraftFoci.WARDING_FOCUS.get());
            if (area) focus.set(ThaumcraftFoci.FOCUS_UPGRADES.get(), FocusUpgradeData.EMPTY.withUpgrade(FocusUpgradeType.architect, 2));
            return focus;
        }
        ItemStack wand(int each, boolean area) {
            Map<String, Integer> amounts = new LinkedHashMap<>();
            for (String tag : WandData.PRIMALS) amounts.put(tag, each);
            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "gold", amounts));
            wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(focus(area)));
            return wand;
        }
        void aim(ServerPlayer player, BlockPos pos, ItemStack wand) {
            player.setPos(pos.getX() - 1.5, pos.getY() - 1.12, pos.getZ() + .5);
            player.setYRot(-90F); player.setXRot(0F);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        }
        void cast(ServerPlayer player, BlockPos pos, ItemStack wand) {
            aim(player, pos, wand);
            WandFocusActions.use(player, InteractionHand.MAIN_HAND);
        }
        TileWarded ward(BlockPos pos) {
            test.assertTrue(level.getBlockState(pos).is(ThaumcraftWards.WARDED.get()), "a real protected block at " + pos);
            test.assertTrue(level.getBlockEntity(pos) instanceof TileWarded, "a real ward tile at " + pos);
            return (TileWarded) level.getBlockEntity(pos);
        }
        void first() {
            owner = player("AaAa"); stranger = player("BBBB");
            test.assertTrue(owner.getGameProfile().getName().hashCode() == stranger.getGameProfile().getName().hashCode()
                            && !owner.getUUID().equals(stranger.getUUID()), "original name hashes collide; actual UUID owners differ");
            numbers();
            charged = wand(1000, false);
            primary = base;
            log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            level.setBlock(primary, log, Block.UPDATE_CLIENTS);
            cast(owner, primary, charged);
            TileWarded ward = ward(primary);
            test.assertTrue(ward.originalState() == log && ward.ownedBy(owner.getUUID()) && !ward.ownedBy(stranger.getUUID()), "exact state and UUID owner");
            WandData paid = ItemWandCasting.data(charged);
            test.assertTrue(paid.amount("terra") == 975 && paid.amount("ordo") == 975 && paid.amount("aqua") == 990
                            && paid.amount("aer") == 1000 && paid.amount("ignis") == 1000 && paid.amount("perditio") == 1000,
                    "one ward pays exactly 25 earth,25 order,10 water, no other aspect");
            cast(owner, primary, charged);
            test.assertTrue(level.getBlockEntity(primary) == ward && ItemWandCasting.data(charged).equals(paid), "coordinate retry delay prevents immediate free undo");
            persistence(ward);
            protectionCallbacks();

            BlockPos bright = base.offset(0, 0, 3);
            level.setBlock(bright, Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            cast(owner, bright, charged);
            test.assertTrue(ward(bright).originalState().is(Blocks.GLOWSTONE) && level.getBlockState(bright).getLightEmission() == 15, "original light 15 is retained");
            BlockPos bedrock = base.offset(0, 0, 6);
            level.setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            cast(owner, bedrock, charged);
            test.assertTrue(ward(bedrock).originalState().is(Blocks.BEDROCK), "original eligible unbreakable solid can be warded without destroying it");

            BlockPos chest = base.offset(0, 0, 9);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            ChestBlockEntity container = (ChestBlockEntity) level.getBlockEntity(chest);
            container.setItem(0, new ItemStack(Items.DIAMOND, 7));
            ItemStack before = charged.copy(); cast(owner, chest, charged);
            test.assertTrue(level.getBlockEntity(chest) == container && container.getItem(0).getCount() == 7 && ItemStack.matches(charged, before),
                    "a real occupied chest and all vis survive refusal");
            BlockPos dry = base.offset(0, 0, 12);
            level.setBlock(dry, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            ItemStack empty = wand(0, false); cast(owner, dry, empty);
            test.assertTrue(level.getBlockState(dry).is(Blocks.STONE) && WandData.PRIMALS.stream().allMatch(tag -> ItemWandCasting.data(empty).amount(tag) == 0), "unfunded means no wrapper and no free block");
            architect();

            Explosion explosion = new Explosion(level, null, primary.getX() + .5, primary.getY() + .5, primary.getZ() + .5,
                    1000F, false, Explosion.BlockInteraction.DESTROY, List.of(primary));
            explosion.finalizeExplosion(false); // The explicit destruction list bypasses the ordinary resistance screen.
            test.assertTrue(level.getBlockEntity(primary) == ward, "even a destruction-selected explosion leaves the original ward in place");
            level.setBlock(primary.west(), Blocks.PISTON.defaultBlockState(), Block.UPDATE_CLIENTS);
            test.assertTrue(!new PistonStructureResolver(level, primary.west(), Direction.EAST, true).resolve(), "the real piston resolver cannot move the wrapper");
            level.setBlock(primary.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            owner.setPos(base.getX() - 4, base.getY() + 5, base.getZ());
        }
        void numbers() {
            ItemStack plain = focus(false), basic = wand(1000, false);
            ItemFocusWarding item = ThaumcraftFoci.WARDING_FOCUS.get();
            test.assertTrue(item.getFocusColor(plain) == 16771535 && item.getSortingHelper(plain).startsWith("BWA")
                            && ItemFocusWarding.activationCost(basic, owner).equals(Map.of("terra", 25, "ordo", 25, "aqua", 10)), "canonical ward color/order/price");
            test.assertTrue(!item.canApplyUpgrade(plain, owner, FocusUpgradeType.enlarge, 3), "Enlarge needs Architect");
            test.assertTrue(item.applyUpgrade(plain, FocusUpgradeType.architect, 2) && item.applyUpgrade(plain, FocusUpgradeType.enlarge, 3)
                            && item.getMaxAreaSize(plain) == 4, "the original rank2 Architect unlocks rank3 Enlarge, radius4");
            var recipe = (InfusionStackRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/focus_warding")).orElseThrow().value();
            test.assertTrue(recipe.result().is(item) && recipe.research().equals("FOCUSWARDING") && recipe.instability() == 4
                            && recipe.components().size() == 8 && recipe.aspects().equals(Map.of("terra", 25, "tutamen", 25, "ordo", 25, "cognitio", 10)), "original infusion gates/essentia/components");
            test.assertTrue(BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("thaumcraft", "warded")).isEmpty(), "wrapper has no obtainable block item");
        }
        void persistence(TileWarded ward) {
            CompoundTag encoded = ward.saveWithoutMetadata(level.registryAccess());
            var restored = new TileWarded(primary, level.getBlockState(primary));
            restored.loadWithComponents(encoded, level.registryAccess());
            test.assertTrue(restored.originalState() == log && restored.ownedBy(owner.getUUID()), "NBT keeps actual block properties and owner");
            var malformed = encoded.copy(); malformed.getCompound("original").putString("Name", "missing:block");
            restored.loadWithComponents(malformed, level.registryAccess());
            test.assertTrue(!restored.validWard() && restored.getUpdateTag(level.registryAccess()).getCompound("RejectedWard").equals(malformed), "unreadable original is preserved and cannot become a free block");
            boolean refused = false;
            try { restored.initialize(log, owner.getUUID()); } catch (IllegalStateException expected) { refused = true; }
            test.assertTrue(refused, "public initialization without the exact paid scope grants no ward");
        }
        void protectionCallbacks() {
            for (int kind = 0; kind < 4; kind++) {
                int action = kind;
                BlockPos pos = base.offset(5, 0, kind * 3);
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                ItemStack wand = wand(1000, false), before = wand.copy();
                BlockEvent.Place listener = (world, target, replacement, actor) -> {
                    if (world != level || !target.equals(pos)) return EventResult.pass();
                    if (action == 0) return EventResult.interruptFalse();
                    if (action == 1) level.setBlock(pos, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                    if (action == 2) owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                    if (action == 3) owner.setPos(owner.getX() - 20, owner.getY(), owner.getZ());
                    return EventResult.pass();
                };
                BlockEvent.PLACE.register(listener);
                try { cast(owner, pos, wand); } finally { BlockEvent.PLACE.unregister(listener); }
                test.assertTrue(level.getBlockState(pos).is(action == 1 ? Blocks.GOLD_BLOCK : Blocks.STONE) && ItemStack.matches(before, wand),
                        "protection veto/state replacement/hand swap/teleport never spends or overwrites: " + action);
            }
        }
        void architect() {
            wallRoot = base.offset(11, 2, 17);
            for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                BlockPos pos = wallRoot.offset(0, y, z);
                BlockState state = (y + z) % 2 == 0 ? log : Blocks.STONE.defaultBlockState();
                wall.put(pos, state); level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
            level.setBlock(wallRoot.south(3), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            architect = wand(1000, true);
            test.assertTrue(WandArchitect.area(architect, FocusData.get(architect)).equals(new WandArchitect.Area(3, 3, 3, 0)), "missing area defaults to maximum3 on all axes");
            cast(owner, wallRoot, architect);
            for (var entry : wall.entrySet()) test.assertTrue(ward(entry.getKey()).originalState() == entry.getValue(), "Architect ward remembers each real connected block");
            test.assertTrue(level.getBlockState(wallRoot.south(3)).is(Blocks.STONE), "a disconnected in-bounds block is untouched");
            WandData paid = ItemWandCasting.data(architect);
            test.assertTrue(paid.amount("terra") == 775 && paid.amount("ordo") == 775 && paid.amount("aqua") == 910, "nine exact per-block prices");
            test.assertTrue(WandArchitectNetworking.handle(owner) && architect.get(ThaumcraftArchitect.AREA.get()).equals(new WandArchitect.Area(0, 0, 0, 0)), "server G handler wraps all maximum radii to zero");
            owner.setShiftKeyDown(true);
            test.assertTrue(WandArchitectNetworking.handle(owner) && architect.get(ThaumcraftArchitect.AREA.get()).axis() == 1, "server Shift-G selects the X radius");
            owner.setShiftKeyDown(false);
            test.assertTrue(WandArchitectNetworking.handle(owner) && architect.get(ThaumcraftArchitect.AREA.get()).equals(new WandArchitect.Area(1, 0, 0, 1)), "G changes just the selected radius");
            architect.remove(ThaumcraftArchitect.AREA.get()); // Explicit fixture reset for the later full-volume owner undo.
        }
        void foreign() {
            stranger.setGameMode(GameType.CREATIVE);
            cast(stranger, primary, wand(1000, false));
            test.assertTrue(ward(primary).ownedBy(owner.getUUID()), "a colliding player name never authorizes unwarding");
            test.assertTrue(!stranger.gameMode.destroyBlock(primary) && !owner.gameMode.destroyBlock(primary), "neither creative stranger nor survival owner mines the wrapper");
        }
        void restore() {
            level.setBlock(primary.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            ItemStack empty = wand(0, false);
            cast(owner, primary, empty);
            test.assertTrue(level.getBlockState(primary) == log && level.getBlockEntity(primary) == null
                            && WandData.PRIMALS.stream().allMatch(tag -> ItemWandCasting.data(empty).amount(tag) == 0),
                    "the real owner with zero vis restores the exact original rotated log, without an extra block");
            cast(owner, wallRoot, architect);
            for (var entry : wall.entrySet()) test.assertTrue(level.getBlockState(entry.getKey()) == entry.getValue() && level.getBlockEntity(entry.getKey()) == null,
                    "Architect undo returns each exact saved block");
            test.assertTrue(ItemWandCasting.data(architect).amount("terra") == 775 && ItemWandCasting.data(architect).amount("aqua") == 910, "all nine undo operations are free");
        }
        void finish() {
            if (finished) return;
            finished = true;
            WardingCasts.clear(level);
            saved.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            for (ServerPlayer player : players) {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                player.discard();
            }
            channels.forEach(EmbeddedChannel::finishAndReleaseAll);
        }
    }
}
