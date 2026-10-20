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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftArchitect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandArchitect;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusTrade;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.network.WandArchitectNetworking;
import thaumcraft.common.lib.world.TradeSwaps;

/** Real connected holder, actual focus selection/right-click, focused queue failures, then live level-tick 26-neighbour and Architect work. */
public final class TradeRuntimeChecks {
    private TradeRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> check.stage(check::start));
        test.onEachTick(() -> {
            if (!check.running || check.finished) return;
            check.stage(() -> { if (check.poll()) { check.finish(); test.succeed(); } });
        });
        test.runAtTickTime(300, () -> {
            if (check.finished) return;
            String message = "Trade route unfinished: phase=" + check.phase + ", changed=" + check.previous;
            check.finish(); test.fail(message);
        });
    }
    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos base;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final List<BlockPos> tracked = new ArrayList<>();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        ServerPlayer player;
        ItemStack work;
        BlockPos selectionPos;
        int phase, previous, initialMaterial, expected;
        long settled;
        boolean running, finished;

        Check(GameTestHelper test) { this.test = test; level = test.getLevel(); base = test.absolutePos(new BlockPos(3, 110, 3)); }
        void stage(Runnable action) { if (finished) return; try { action.run(); } catch (RuntimeException | Error failure) { finish(); throw failure; } }
        void prepare() {
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-4, -3, -4), base.offset(35, 6, 32))) {
                test.assertTrue(level.getBlockEntity(pos) == null, "Trade fixture does not overwrite a real inventory");
                saved.put(pos.immutable(), level.getBlockState(pos));
            }
            forced.addAll(GolemChunks.hold(level, saved.keySet()));
            saved.forEach((pos, state) -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
        }
        ItemStack wand(int each, FocusUpgradeType type, int rank) {
            var amounts = new LinkedHashMap<String, Integer>();
            for (String tag : WandData.PRIMALS) amounts.put(tag, each);
            var wand = new ItemStack(ThaumcraftContent.WAND.get());
            wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "gold", amounts));
            var focus = new ItemStack(ThaumcraftFoci.TRADE_FOCUS.get());
            if (type != null) focus.set(ThaumcraftFoci.FOCUS_UPGRADES.get(), FocusUpgradeData.EMPTY.withUpgrade(type, rank));
            wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(focus));
            return wand;
        }
        void aim(BlockPos pos, ItemStack wand) {
            player.setPos(pos.getX() - 1.5, pos.getY() - 1.37, pos.getZ() + .5);
            player.setYRot(-90F); player.setYHeadRot(-90F); player.setXRot(0F);
            player.getInventory().selected = 0;
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        }
        void select(ItemStack wand, BlockState state) {
            level.setBlock(selectionPos, state, Block.UPDATE_CLIENTS);
            aim(selectionPos, wand); player.setShiftKeyDown(true);
            WandFocusActions.use(player, InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
        }
        void inputs(ItemStack wand, Item material, int count) {
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            if (count > 0) player.getInventory().setItem(1, new ItemStack(material, count));
        }
        int count(Item item) {
            int count = 0;
            for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).is(item)) count += player.getInventory().getItem(slot).getCount();
            return count;
        }
        int gold() { return (int) tracked.stream().filter(pos -> level.getBlockState(pos).is(Blocks.GOLD_BLOCK)).count(); }
        void start() {
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "trade-" + UUID.randomUUID().toString().substring(0, 7)), ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true);
            test.assertTrue(level.getServer().getPlayerList().getPlayer(player.getUUID()) == player, "an actual connected holder is the input");
            selectionPos = base;
            focused();
            flood();
            running = true;
        }
        void queue(BlockPos pos, ItemStack wand) {
            aim(pos, wand);
            test.assertTrue(ItemFocusTrade.leftClick(player, wand, pos), "a live left-click schedules exactly the target");
        }
        void focused() {
            ItemStack plain = wand(1000, null, 0);
            var item = ThaumcraftFoci.TRADE_FOCUS.get();
            test.assertTrue(item.getVisCost(FocusData.get(plain)).getAmount(thaumcraft.api.aspects.Aspect.EARTH) == 5
                            && item.getFocusColor(FocusData.get(plain)) == 8747923, "original Trade base price and color");
            select(plain, Blocks.GOLD_BLOCK.defaultBlockState());
            test.assertTrue(TradeSwaps.selection(level, plain).is(Blocks.GOLD_BLOCK), "sneak-use remembers the actual looked-at block");
            level.setBlock(selectionPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            aim(selectionPos, plain); player.setShiftKeyDown(true); WandFocusActions.use(player, InteractionHand.MAIN_HAND); player.setShiftKeyDown(false);
            test.assertTrue(TradeSwaps.selection(level, plain).is(Blocks.GOLD_BLOCK), "a container cannot replace the remembered block");

            BlockPos absent = base.offset(3, 0, 24);
            level.setBlock(absent, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            inputs(plain, Items.AIR, 0);
            ItemStack unpaid = plain.copy();
            queue(absent, plain); TradeSwaps.tick(level);
            test.assertTrue(level.getBlockState(absent).is(Blocks.STONE) && ItemStack.matches(unpaid, plain) && count(Items.COBBLESTONE) == 0,
                    "a survival player without target material pays nothing and produces no source drop");
            aim(selectionPos, plain);
            test.assertTrue(!ItemFocusTrade.leftClick(player, plain, selectionPos) && level.getBlockEntity(selectionPos) != null,
                    "an actual block entity is never queued as a source or discarded");

            BlockPos pos = base.offset(0, 0, 5);
            inputs(plain, Items.GOLD_BLOCK, 4);
            level.setBlock(pos, Blocks.COAL_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
            int xp = player.totalExperience;
            queue(pos, plain);
            test.assertTrue(level.getBlockState(pos).is(Blocks.COAL_ORE) && count(Items.GOLD_BLOCK) == 4, "queued jobs own no paid material or output");
            TradeSwaps.tick(level); // Focused real executor check; live one-success-per-world-tick proof follows below.
            test.assertTrue(level.getBlockState(pos).is(Blocks.GOLD_BLOCK) && count(Items.GOLD_BLOCK) == 3 && count(Items.COAL) == 1,
                    "one real ore is replaced for one material and exactly its ordinary drop");
            WandData data = ItemWandCasting.data(plain);
            test.assertTrue(data.amount("terra") == 995 && data.amount("ordo") == 995 && data.amount("perditio") == 995
                            && data.amount("aer") == 1000 && data.amount("ignis") == 1000 && data.amount("aqua") == 1000,
                    "one swap pays exactly five centivis of three primals");
            TradeSwaps.tick(level);
            test.assertTrue(count(Items.COAL) == 1 && ItemWandCasting.data(plain).equals(data) && player.totalExperience == xp
                            && level.getEntitiesOfClass(ExperienceOrb.class, new AABB(base).inflate(40)).isEmpty(), "no duplicate loot/payment or mining XP on a spent queue");

            for (int mode = 0; mode < 4; mode++) {
                ItemStack live = wand(1000, null, 0);
                select(live, Blocks.GOLD_BLOCK.defaultBlockState()); inputs(live, Items.GOLD_BLOCK, 4);
                BlockPos cell = base.offset(5, 0, mode * 3);
                level.setBlock(cell, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                queue(cell, live);
                ItemStack before = live.copy();
                if (mode == 0) player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                if (mode == 1) { player.getInventory().setItem(0, ItemStack.EMPTY); player.getInventory().setItem(2, live); player.getInventory().selected = 2; }
                if (mode == 2) select(live, Blocks.DIAMOND_BLOCK.defaultBlockState());
                if (mode == 3) level.setBlock(cell, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (mode == 2) before = live.copy(); // Only the declared selection edit changed it, not the queued job.
                TradeSwaps.tick(level);
                test.assertTrue(level.getBlockState(cell).is(mode == 3 ? Blocks.DIRT : Blocks.STONE) && count(Items.GOLD_BLOCK) == 4
                                && ItemStack.matches(before, live) && count(Items.COBBLESTONE) == 0, "stale hand/slot/selection/source cannot pay or remove: " + mode);
            }
            for (int mode = 0; mode < 3; mode++) permissions(mode);
            canonicalSelection(false); canonicalSelection(true);
            silk();
            creative();
        }
        void permissions(int mode) {
            ItemStack wand = wand(1000, null, 0);
            select(wand, Blocks.GOLD_BLOCK.defaultBlockState()); inputs(wand, Items.GOLD_BLOCK, 4);
            BlockPos pos = base.offset(9, 0, mode * 3);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            queue(pos, wand);
            ItemStack before = wand.copy();
            BlockEvent.Place listener = (world, cell, state, actor) -> {
                if (world != level || !cell.equals(pos)) return EventResult.pass();
                if (mode == 0) return EventResult.interruptFalse();
                if (mode == 1) player.getInventory().setItem(1, new ItemStack(Items.GOLD_BLOCK, 4));
                if (mode == 2) player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                return EventResult.pass();
            };
            BlockEvent.PLACE.register(listener);
            try { TradeSwaps.tick(level); } finally { BlockEvent.PLACE.unregister(listener); }
            test.assertTrue(level.getBlockState(pos).is(Blocks.STONE) && count(Items.GOLD_BLOCK) == 4 && ItemStack.matches(before, wand)
                            && count(Items.COBBLESTONE) == 0, "native protection veto/stale material/stale hand leaves all resources: " + mode);
        }
        void canonicalSelection(boolean doubled) {
            ItemStack wand = wand(1000, null, 0);
            BlockState chosen = Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, doubled ? SlabType.DOUBLE : SlabType.BOTTOM)
                    .setValue(BlockStateProperties.WATERLOGGED, !doubled);
            select(wand, chosen);
            BlockState picked = TradeSwaps.selection(level, wand);
            test.assertTrue(picked != null && picked.is(Blocks.STONE_SLAB) && picked.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
                            && !picked.getValue(BlockStateProperties.WATERLOGGED), "selection remembers one dry slab item, not free water or two slabs");
            inputs(wand, Items.STONE_SLAB, 1);
            BlockPos pos = base.offset(0, 0, doubled ? 18 : 15);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            queue(pos, wand); TradeSwaps.tick(level);
            test.assertTrue(level.getBlockState(pos) == Blocks.STONE_SLAB.defaultBlockState() && count(Items.STONE_SLAB) == 0 && count(Items.COBBLESTONE) == 1,
                    "one material makes exactly one dry bottom slab and one source drop");
        }
        void silk() {
            ItemStack silk = wand(1000, FocusUpgradeType.silktouch, 5);
            select(silk, Blocks.GOLD_BLOCK.defaultBlockState()); inputs(silk, Items.GOLD_BLOCK, 1);
            BlockPos pos = base.offset(0, 0, 21); level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            queue(pos, silk); TradeSwaps.tick(level);
            WandData data = ItemWandCasting.data(silk);
            test.assertTrue(count(Items.STONE) == 1 && count(Items.COBBLESTONE) == 0 && data.amount("terra") == 994
                            && data.amount("ordo") == 994 && data.amount("perditio") == 994 && data.amount("aer") == 999
                            && data.amount("ignis") == 999 && data.amount("aqua") == 999, "Silk pays six/one centivis and returns the real stone block");
        }
        void creative() {
            player.setGameMode(GameType.CREATIVE);
            ItemStack funded = wand(1000, null, 0); select(funded, Blocks.GOLD_BLOCK.defaultBlockState()); inputs(funded, Items.AIR, 0);
            BlockPos pos = base.offset(0, 0, 24); level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            ItemStack before = funded.copy(); queue(pos, funded); TradeSwaps.tick(level);
            test.assertTrue(level.getBlockState(pos).is(Blocks.GOLD_BLOCK) && ItemStack.matches(before, funded) && count(Items.COBBLESTONE) == 0, "funded creative swaps without material, vis debit or output");
            ItemStack dry = wand(0, null, 0); select(dry, Blocks.GOLD_BLOCK.defaultBlockState()); inputs(dry, Items.AIR, 0);
            BlockPos refused = base.offset(0, 0, 27); level.setBlock(refused, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            queue(refused, dry); TradeSwaps.tick(level);
            test.assertTrue(level.getBlockState(refused).is(Blocks.STONE), "creative still needs the quoted vis balance");
            player.setGameMode(GameType.SURVIVAL);
        }
        void flood() {
            phase = 1; tracked.clear(); previous = 0; settled = 0; expected = 26; initialMaterial = 64;
            work = wand(1000, null, 0); select(work, Blocks.GOLD_BLOCK.defaultBlockState()); inputs(work, Items.GOLD_BLOCK, initialMaterial);
            BlockPos centre = base.offset(17, 1, 5);
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                BlockPos pos = centre.offset(x, y, z);
                if (x != 0 || y != 0 || z != 0) tracked.add(pos);
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            aim(centre.west(), work);
            WandFocusActions.use(player, InteractionHand.MAIN_HAND); // Actual right-click starts the original 26-neighbour flood.
            test.assertTrue(gold() == 0, "enqueue has no immediate world or inventory output");
        }
        void plane() {
            phase = 2; tracked.clear(); previous = 0; settled = 0; expected = 9;
            work = wand(1000, FocusUpgradeType.architect, 3); select(work, Blocks.GOLD_BLOCK.defaultBlockState()); inputs(work, Items.GOLD_BLOCK, 64);
            test.assertTrue(WandArchitectNetworking.handle(player), "G wraps default maximum Architect sizes to zero");
            player.setShiftKeyDown(true); test.assertTrue(WandArchitectNetworking.handle(player), "Shift-G picks first axis");
            player.setShiftKeyDown(false); WandArchitectNetworking.handle(player);
            player.setShiftKeyDown(true); WandArchitectNetworking.handle(player);
            player.setShiftKeyDown(false); WandArchitectNetworking.handle(player);
            player.setShiftKeyDown(true); WandArchitectNetworking.handle(player);
            player.setShiftKeyDown(false);
            test.assertTrue(work.get(ThaumcraftArchitect.AREA.get()).equals(new WandArchitect.Area(1, 0, 1, 0)), "two-axis Trade controls select the requested 3x3 plane");
            BlockPos centre = base.offset(30, 3, 5);
            for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                BlockPos pos = centre.offset(0, y, z); tracked.add(pos); level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            level.setBlock(centre.east(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(centre.south(3), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            aim(centre, work); WandFocusActions.use(player, InteractionHand.MAIN_HAND);
        }
        boolean poll() {
            int changed = gold();
            test.assertTrue(changed >= previous && changed - previous <= 1, "the actual level tick commits at most one Trade success: " + previous + " -> " + changed);
            previous = changed;
            test.assertTrue(count(Items.GOLD_BLOCK) == initialMaterial - changed && count(Items.COBBLESTONE) == changed,
                    "one target material and one actual source drop per changed cell: " + changed);
            WandData data = ItemWandCasting.data(work);
            test.assertTrue(data.amount("terra") == 1000 - 5 * changed && data.amount("ordo") == 1000 - 5 * changed
                            && data.amount("perditio") == 1000 - 5 * changed, "exact cumulative vis, no duplicate jobs paid");
            if (changed != expected) return false;
            if (settled == 0) settled = test.getTick() + 12;
            if (test.getTick() < settled) return false;
            if (phase == 1) {
                test.assertTrue(level.getBlockState(base.offset(17, 1, 5)).is(Blocks.STONE), "the completely enclosed centre stays original: the flood visits exposed blocks only");
                plane(); return false;
            }
            BlockPos centre = base.offset(30, 3, 5);
            test.assertTrue(level.getBlockState(centre.east()).is(Blocks.STONE) && level.getBlockState(centre.south(3)).is(Blocks.STONE), "Architect has no depth flood or disconnected target");
            return true;
        }
        void finish() {
            if (finished) return;
            finished = true;
            if (player != null) {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                player.discard();
            }
            saved.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
            level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(40)).forEach(ItemEntity::discard);
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            channel.finishAndReleaseAll();
        }
    }
}
