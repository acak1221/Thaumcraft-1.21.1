package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.TubeGeometry;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileTube;

/** Bounded server fixtures, not survival acquisition. No clock/RNG/phase rewrites. */
public final class ValveRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private ValveRuntimeChecks() {}

    /** Central Fabric wrapper supplies its ordinary mock server player. */
    public static void run(GameTestHelper test, ServerPlayer player) {
        try (Fixture f = new Fixture(test, player)) {
            portsAndPersistence(f);
            redstoneAndVenting(f);
            indexedInteractions(f);
            lockedTransferAndDrop(f);
        }
        test.succeed();
    }

    private static void portsAndPersistence(Fixture f) {
        f.reset(); TileTube valve = f.valve();
        f.check(valve.isValve() && !f.stem().isValve() && valve.getType() == f.stem().getType()
                && valve.isFlowAllowed() && !valve.hadPower() && valve.getRotation() == 0
                && valve.getFacing() == Direction.NORTH && valve.getEssentiaCapacity() == 1, "real block kind and fresh valve defaults share the basic BE");
        for (Direction face : Direction.values()) f.check(valve.isSideOpen(face) && valve.canInputFrom(face) && valve.canOutputTo(face)
                && valve.isConnectable(face) == (face != Direction.NORTH), "raw ports are distinct from the permanently excluded stem: " + face);
        f.check(!valve.isSideOpen(null) && !valve.isConnectable(null) && !valve.canInputFrom(null) && !valve.canOutputTo(null)
                && valve.getMinimumSuction() == 0 && !valve.renderExtendedTube(), "UNKNOWN is query-only and valve capacity/render/minimum remain inherited");
        valve.setSuction(Aspect.FIRE, 17);
        f.check(valve.addEssentia(Aspect.FIRE, 100, Direction.NORTH) == 1 && valve.setFlowAllowed(false), "raw stem can fill one bounded unit before closure");
        valve.setSuction(Aspect.ENERGY, 99);
        f.check(valve.getSuctionAmount(null) == 17 && valve.getSuctionType(null) == Aspect.FIRE
                && valve.getEssentiaAmount(null) == 1 && EssentiaTransfers.pullOne(f.stem(), Direction.SOUTH, Aspect.FIRE) == 0,
                "closing keeps cached suction and stock, but the stem is not a network connection");
        f.check(valve.takeEssentia(Aspect.FIRE, 100, Direction.NORTH) == 1 && valve.addEssentia(Aspect.FIRE, 100, Direction.NORTH) == 1,
                "closed valve does not hard-gate inherited raw add/take, including the stem");
        for (int i = 0; i < 5; i++) TileJar.tick(f.level, f.jarPos, f.jar().getBlockState(), f.jar());
        f.check(f.jar().getAmount() == 1 && valve.getEssentiaAmount(null) == 0, "ordinary downstream jar can drain a closed valve's residual unit");
        valve.addEssentia(Aspect.FIRE, 1, Direction.NORTH); f.ticks(2);
        f.check(valve.getSuctionAmount(null) == 0 && valve.getSuctionType(null) == null && valve.getEssentiaAmount(null) == 1,
                "base calculation resets raw suction even though closed setSuction is a no-op");
        f.power(true); f.phase(5, 0); f.ticks(1);
        f.check(valve.hadPower() && !valve.isFlowAllowed(), "an already closed valve still records the sampled power latch");
        valve.setFlowAllowed(true); valve.setSuction(Aspect.FIRE, 17); valve.setFacing(Direction.WEST); valve.setSideOpen(Direction.UP, false);
        CompoundTag saved = valve.saveWithFullMetadata(f.level.registryAccess()), update = valve.getUpdateTag(f.level.registryAccess());
        f.check(saved.getBoolean("flow") && saved.getBoolean("hadpower") && update.getBoolean("flow") && update.getBoolean("hadpower")
                && saved.getInt("samount") == 17 && !update.contains("samount") && !update.contains("stype")
                && !saved.contains("count") && !saved.contains("venting") && !saved.contains("rotation"), "disk saves flow/latch/suction; custom updates omit suction and transient animation/phase");
        TileTube mirror = new TileTube(f.valvePos, valve.getBlockState()); mirror.setSuction(Aspect.ENERGY, 9); mirror.loadWithComponents(update, f.level.registryAccess());
        f.check(mirror.isFlowAllowed() && mirror.hadPower() && mirror.getSuctionAmount(null) == 9 && mirror.getSuctionType(null) == Aspect.ENERGY,
                "custom update applies the shared storage and latch without overwriting local suction");
        CompoundTag missing = saved.copy(); missing.remove("flow"); missing.remove("hadpower"); missing.putBoolean("valve", false);
        TileTube omitted = new TileTube(f.valvePos, valve.getBlockState()); omitted.loadWithComponents(missing, f.level.registryAccess());
        f.check(omitted.isValve() && !omitted.isFlowAllowed() && !omitted.hadPower(), "omitted persisted booleans are false, not fresh-placement defaults or an editable kind");
        CompoundTag forged = saved.copy(); forged.putBoolean("valve", true); forged.putBoolean("flow", false);
        TileTube basic = new TileTube(f.stemPos, f.stem().getBlockState()); basic.loadWithComponents(forged, f.level.registryAccess());
        f.check(!basic.isValve() && basic.isFlowAllowed() && !basic.hadPower() && basic.isConnectable(Direction.WEST) && !basic.toggleFlow(), "saved flags cannot turn a real basic tube into a valve");
        TileTube stale = valve; valve = f.restoreValve(saved);
        f.check(valve.isValve() && valve.isFlowAllowed() && valve.hadPower() && valve.getFacing() == Direction.WEST && !valve.isSideOpen(Direction.UP)
                && valve.getEssentiaAmount(null) == 1 && valve.getEssentiaType(null) == Aspect.FIRE && valve.getSuctionAmount(null) == 17
                && valve.getTickCount() == 0 && valve.getVenting() == 0 && valve.getRotation() == 0 && valve.getUpdatePacket() != null
                && !stale.toggleFlow() && stale.takeEssentia(Aspect.FIRE, 1, Direction.NORTH) == 0, "real BE replacement preserves fields, not transient phases; stale identity cannot mutate");
        f.ticks(1);
        f.check(valve.isFlowAllowed() && valve.hadPower(), "saved hadpower prevents a false rising edge at the fresh count-zero sample");
    }

    private static void redstoneAndVenting(Fixture f) {
        f.reset(); TileTube valve = f.valve(); f.power(true);
        f.check(valve.getTickCount() == 0, "new placement starts at the original unseeded zero");
        f.ticks(1);
        f.check(!valve.isFlowAllowed() && valve.hadPower() && valve.getTickCount() >= 1 && valve.getTickCount() <= 10,
                "initial PREcount-zero power sample precedes the unchanged random seed/base increment");
        f.aim(Vec3.atCenterOf(f.valvePos));
        f.check(f.use(f.valvePos, Direction.SOUTH).consumesAction() && valve.isFlowAllowed(), "native empty hand can reopen while powered");
        f.phase(5, 0); f.ticks(1);
        f.check(valve.isFlowAllowed() && valve.hadPower(), "stable high power does not force a manually reopened valve shut");
        valve.setFlowAllowed(false); f.phase(5, 4); f.power(false); f.ticks(1);
        f.check(!valve.isFlowAllowed() && valve.hadPower() && valve.getTickCount() % 5 == 0, "post-increment divisible count does not sample an edge early");
        f.ticks(1);
        f.check(valve.isFlowAllowed() && !valve.hadPower(), "falling edge opens a closed valve on the next PREcount-divisible sample");
        valve.setFlowAllowed(false); f.phase(5, 0); f.ticks(1);
        f.check(!valve.isFlowAllowed() && !valve.hadPower(), "stable low power likewise preserves a manual closure");

        f.reset(); valve = f.valve(); f.jar().setFilter(Aspect.FIRE); f.phase(10, 9);
        f.source().addToContainer(Aspect.FIRE, 1); f.upper().setSuction(Aspect.ENERGY, 63); f.ticks(1);
        int phase = valve.getTickCount();
        f.check(phase % 10 == 0 && valve.getVenting() == 40 && valve.getSuctionAmount(null) == 63 && valve.getSuctionType(null) == Aspect.FIRE
                && f.source().getAmount() == 0 && valve.getEssentiaAmount(null) == 1, "real conflicting tube starts vent40 and the same suction/pull tick can still move the paid unit");
        TileTube loaded = new TileTube(f.valvePos, valve.getBlockState()); loaded.loadWithComponents(valve.saveWithFullMetadata(f.level.registryAccess()), f.level.registryAccess());
        f.check(loaded.getTickCount() == 0 && loaded.getVenting() == 0 && loaded.getEssentiaAmount(null) == 1, "a genuinely venting save reloads stock but no vent/count");
        f.power(true); f.ticks(1);
        f.check(!valve.isFlowAllowed() && valve.hadPower() && valve.getTickCount() == phase && valve.getVenting() == 39 && valve.getSuctionAmount(null) == 63,
                "divisible vent-frozen phase still samples power BEFORE decrement and keeps cached suction");
        valve.setFlowAllowed(true); f.ticks(1);
        f.check(valve.isFlowAllowed() && valve.hadPower() && valve.getTickCount() == phase, "repeated frozen high samples are not repeated rising edges");
        valve.setFlowAllowed(false); f.power(false); f.ticks(1);
        f.check(valve.isFlowAllowed() && !valve.hadPower() && valve.getTickCount() == phase && valve.getVenting() == 37,
                "falling edge is sampled on the very next frozen divisible tick");
        f.check(valve.takeEssentia(Aspect.FIRE, 100, Direction.NORTH) == 1 && valve.addEssentia(Aspect.FIRE, 100, Direction.NORTH) == 1
                && valve.takeEssentia(Aspect.FIRE, 1, Direction.NORTH) == 1, "venting itself also leaves external one-unit raw add/take available");
        f.upper().setSuction(null, 0); f.ticks(valve.getVenting());
        f.check(valve.getVenting() == 0 && valve.getTickCount() == phase + 1, "only the tick decrementing vent1 to0 resumes the shared phase");
        f.upper().setSuction(Aspect.ENERGY, 63); f.ticks(1); phase = valve.getTickCount(); f.power(true); f.ticks(4);
        f.check(phase % 5 != 0 && valve.getTickCount() == phase && valve.getVenting() == 36 && valve.isFlowAllowed() && !valve.hadPower(),
                "nondivisible vent-frozen phase does NOT sample power, even across several native ticker invocations");
    }

    private static void indexedInteractions(Fixture f) {
        f.reset(); TileTube valve = f.valve(); valve.setFacing(Direction.UP);
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get()); f.player.setItemInHand(InteractionHand.MAIN_HAND, wand.copy());
        f.aim(Vec3.atLowerCornerOf(f.valvePos).add(.5, .85, .5));
        f.check(TubeGeometry.retrace(f.player, valve) != null && TubeGeometry.retrace(f.player, valve).index() == 1, "server retrace resolves the upper STEM arm from real geometry");
        f.check(f.use(f.valvePos, Direction.DOWN).consumesAction() && !valve.isSideOpen(Direction.UP) && !f.upper().isSideOpen(Direction.DOWN)
                && !valve.isConnectable(Direction.UP) && valve.isFlowAllowed(), "wand toggles raw stem flag, not always-false isConnectable, and assigns the same opposing value");
        f.player.setShiftKeyDown(true);
        f.check(f.use(f.valvePos, Direction.NORTH).consumesAction() && valve.isSideOpen(Direction.UP) && f.upper().isSideOpen(Direction.DOWN)
                && !valve.isConnectable(Direction.UP) && ItemStack.matches(wand, f.player.getMainHandItem()), "secondary wand use reopens the still-indexed stem with no vis/item payment");
        f.player.setShiftKeyDown(false); valve.setSideOpen(Direction.NORTH, false); valve.setSideOpen(Direction.SOUTH, false); f.aim(Vec3.atCenterOf(f.valvePos));
        f.check(TubeGeometry.retrace(f.player, valve).index() == 6 && f.use(f.valvePos, Direction.UP).consumesAction()
                && valve.getFacing() == Direction.SOUTH && !valve.isSideOpen(Direction.SOUTH), "valve core skips a raw-closed transport neighbor and chooses a side with NO transport, even when that raw side is closed");
        f.check(valve.getBlockState().useItemOn(wand.copy(), f.level, f.player, InteractionHand.MAIN_HAND, f.hit(f.valvePos, Direction.SOUTH)) == ItemInteractionResult.FAIL,
                "detached/fabricated tool stack cannot configure the real server hand");
        f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        f.check(f.use(f.valvePos, Direction.SOUTH).consumesAction() && !valve.isFlowAllowed() && f.player.getMainHandItem().getCount() == 1, "ordinary non-tool item toggles once without consumption");
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        f.check(f.use(f.valvePos, Direction.SOUTH).consumesAction() && valve.isFlowAllowed(), "empty native hand toggles exactly once through default dispatch");
        f.aim(Vec3.atLowerCornerOf(f.valvePos).add(.61, .33, .5));
        f.check(TubeGeometry.retrace(f.player, valve) == null && TubeGeometry.hitsBlock(f.player, valve)
                && f.use(f.valvePos, Direction.SOUTH).consumesAction() && !valve.isFlowAllowed(),
                "ordinary hand may use the real outer selection outline without hitting a narrower indexed tool part");
        f.check(f.use(f.valvePos, Direction.SOUTH).consumesAction() && valve.isFlowAllowed(), "outer-outline second hand click reopens once");
        f.aim(Vec3.atCenterOf(f.valvePos));
        for (var item : List.of(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get())) {
            f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            f.check(BlockTube.tryHandInteraction(f.context()) == InteractionResult.PASS, "ANY flattened tube BlockItem is excluded from manual toggle");
            f.check(f.use(f.valvePos, Direction.SOUTH).consumesAction() && valve.isFlowAllowed() && f.level.getBlockEntity(f.valvePos.south()) instanceof TileTube placed
                    && placed.getFacing() == Direction.SOUTH && placed.getEssentiaAmount(null) == 0, "excluded tube item actually places, using clicked FACE rather than toggling the old valve");
            f.level.removeBlockEntity(f.valvePos.south()); f.level.setBlock(f.valvePos.south(), Blocks.AIR.defaultBlockState(), QUIET);
        }
        var resonator = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "resonator"));
        f.check(resonator instanceof ItemResonator, "real resonator is registered for item-first dispatch");
        f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(resonator));
        f.check(BlockTube.tryHandInteraction(f.context()) == InteractionResult.PASS && f.use(f.valvePos, Direction.SOUTH).consumesAction() && valve.isFlowAllowed(), "resonator inspects before valve hand activation");
        f.player.setShiftKeyDown(true);
        f.check(f.use(f.valvePos, Direction.SOUTH).consumesAction() && valve.isFlowAllowed(), "secondary resonator path also cannot toggle flow");
        f.player.setShiftKeyDown(false); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); f.aim(Vec3.atCenterOf(f.valvePos));
        UseOnContext staleHand = f.context(); f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        f.check(BlockTube.tryHandInteraction(staleHand) == InteractionResult.FAIL && valve.isFlowAllowed(), "manual interaction cannot reuse a detached previous hand identity");
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        f.level.setBlock(f.valvePos.south(), Blocks.STONE.defaultBlockState(), QUIET);
        f.check(BlockTube.tryHandInteraction(f.context()) == InteractionResult.FAIL && valve.isFlowAllowed(), "manual toggle cannot pass an occluding native block");
        f.level.setBlock(f.valvePos.south(), Blocks.AIR.defaultBlockState(), QUIET); f.player.setPos(f.player.position().add(0, 0, 20));
        f.check(BlockTube.tryHandInteraction(f.context()) == InteractionResult.FAIL && valve.isFlowAllowed(), "manual toggle respects real native reach, not supplied hit coordinates");
        f.aim(Vec3.atCenterOf(f.valvePos)); f.player.setGameMode(GameType.SPECTATOR);
        f.check(BlockTube.tryHandInteraction(f.context()) == InteractionResult.FAIL && valve.isFlowAllowed(), "spectator cannot mutate a valve");
        f.player.setGameMode(GameType.SURVIVAL); f.player.setShiftKeyDown(true);
        f.check(f.use(f.valvePos, Direction.SOUTH).consumesAction() && !valve.isFlowAllowed(), "empty-hand sneaking is not an invented valve activation gate");
        f.player.setShiftKeyDown(false);
    }

    private static void lockedTransferAndDrop(Fixture f) {
        f.reset(); TileTube valve = f.valve(); valve.setSuction(Aspect.FIRE, 17);
        boolean denied = false;
        try { valve.setEssentiaStorageRaw(Aspect.FIRE, 1); } catch (IllegalStateException expected) { denied = true; }
        f.check(denied && valve.getEssentiaAmount(null) == 0, "callback-free raw endpoint is not an external fill API");
        Probe source = f.probe();
        source.query = () -> {
            f.check(!valve.toggleFlow() && !valve.setSideOpen(Direction.EAST, false) && !valve.setFacing(Direction.UP), "sided query cannot reconfigure a locked valve");
            valve.setSuction(Aspect.ENERGY, 99);
            f.check(valve.getSuctionAmount(null) == 17 && valve.getSuctionType(null) == Aspect.FIRE, "locked suction mutation is refused too");
        };
        source.notice = () -> {
            int count = valve.getTickCount(); f.ticks(1);
            f.check(EssentiaTransfers.isLocked(valve) && EssentiaTransfers.isLocked(source) && source.amount == 0 && valve.getEssentiaAmount(null) == 1
                    && valve.getTickCount() == count && valve.takeEssentia(Aspect.FIRE, 1, Direction.DOWN) == 0 && !valve.toggleFlow()
                    && EssentiaTransfers.pullOne(valve, Direction.EAST, Aspect.FIRE) == 0, "notification sees BOTH committed fields with tick/transfer/manual mutations fenced");
            boolean rawDenied = false; try { valve.setEssentiaStorageRaw(null, 0); } catch (IllegalStateException expected) { rawDenied = true; }
            f.check(rawDenied, "notification phase cannot reuse the raw write hook");
            throw new IllegalStateException("valve-notification-fixture");
        };
        boolean threw = false;
        try { EssentiaTransfers.pullOne(valve, Direction.EAST, Aspect.FIRE); }
        catch (IllegalStateException expected) { threw = "valve-notification-fixture".equals(expected.getMessage()); }
        f.check(threw && source.amount == 0 && valve.getEssentiaAmount(null) == 1 && valve.isFlowAllowed() && valve.isSideOpen(Direction.EAST)
                && !EssentiaTransfers.isLocked(valve) && !EssentiaTransfers.isLocked(source), "post-commit failure does not refund/duplicate a unit or retain either lock");
        valve.setFlowAllowed(false); valve.setSideOpen(Direction.UP, false);
        f.check(valve.getBlockState().hasAnalogOutputSignal() && valve.getBlockState().getAnalogOutputSignal(f.level, f.valvePos) == 0, "filled configured valve comparator is still zero");
        f.check(f.level.destroyBlock(f.valvePos, true), "native break removes the actual filled valve");
        var drops = f.drops().stream().filter(drop -> drop.getItem().is(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get())).toList();
        f.check(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get())), "one plain OWN valve item drops, without buffer/config/components");
        f.player.setItemInHand(InteractionHand.MAIN_HAND, drops.getFirst().getItem().copy()); drops.getFirst().discard(); f.aim(Vec3.atCenterOf(f.jarPos));
        f.check(f.use(f.jarPos, Direction.UP).consumesAction() && f.valve().isValve() && f.valve().isFlowAllowed() && !f.valve().hadPower()
                && f.valve().getEssentiaAmount(null) == 0 && f.valve().getFacing() == Direction.UP && f.valve().isSideOpen(Direction.UP), "native own-item placement has fresh defaults and clicked-face stem, not dropped configuration");
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test; final ServerLevel level; final ServerPlayer player;
        final BlockPos valvePos, jarPos, stemPos, upperPos, sourcePos; final AABB bounds;
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>(); final Map<BlockPos, BlockEntity> originals = new LinkedHashMap<>();
        final List<ItemStack> inventory = new ArrayList<>(), crafting = new ArrayList<>(); final Set<UUID> oldDrops;
        final ItemStack cursor; final Vec3 position, motion; final GameType mode; final int selected; final boolean sneaking;
        final float yaw, pitch, headYaw, oldHeadYaw; final CompoundTag abilities = new CompoundTag();
        Fixture(GameTestHelper test, ServerPlayer player) {
            this.test = test; this.level = test.getLevel(); this.player = player;
            valvePos = test.absolutePos(new BlockPos(3, 3, 3)); jarPos = valvePos.below(); stemPos = valvePos.north(); upperPos = valvePos.above(); sourcePos = valvePos.east(); bounds = new AABB(valvePos).inflate(5);
            position = player.position(); motion = player.getDeltaMovement(); mode = player.gameMode.getGameModeForPlayer(); selected = player.getInventory().selected; sneaking = player.isShiftKeyDown();
            yaw = player.getYRot(); pitch = player.getXRot(); headYaw = player.yHeadRot; oldHeadYaw = player.yHeadRotO; player.getAbilities().addSaveData(abilities);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
            for (int slot = 0; slot < player.inventoryMenu.getCraftSlots().getContainerSize(); slot++) crafting.add(player.inventoryMenu.getCraftSlots().getItem(slot).copy());
            cursor = player.inventoryMenu.getCarried().copy(); oldDrops = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
            for (BlockPos mutable : BlockPos.betweenClosed(valvePos.offset(-2, -2, -2), valvePos.offset(2, 2, 2))) {
                BlockPos pos = mutable.immutable(); states.put(pos, level.getBlockState(pos)); if (level.getBlockEntity(pos) != null) originals.put(pos, level.getBlockEntity(pos));
            }
            player.closeContainer(); player.inventoryMenu.getCraftSlots().clearContent(); player.inventoryMenu.setCarried(ItemStack.EMPTY);
        }
        void reset() {
            for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
            for (BlockPos pos : states.keySet()) level.setBlock(pos, pos.getY() == valvePos.getY() - 2 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            player.getInventory().clearContent(); player.getInventory().selected = 0; player.setGameMode(GameType.SURVIVAL); player.setShiftKeyDown(false); player.setDeltaMovement(Vec3.ZERO);
            level.setBlock(valvePos, ThaumcraftTubes.ESSENTIA_VALVE.get().defaultBlockState(), QUIET); level.setBlock(jarPos, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            for (BlockPos pos : List.of(stemPos, upperPos)) level.setBlock(pos, ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), QUIET);
            level.setBlock(sourcePos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), QUIET);
        }
        TileTube valve() { return (TileTube) level.getBlockEntity(valvePos); }
        TileTube stem() { return (TileTube) level.getBlockEntity(stemPos); }
        TileTube upper() { return (TileTube) level.getBlockEntity(upperPos); }
        TileJar jar() { return (TileJar) level.getBlockEntity(jarPos); }
        TileAlembic source() { return (TileAlembic) level.getBlockEntity(sourcePos); }
        void check(boolean condition, String message) { test.assertTrue(condition, message); }
        void power(boolean on) { level.setBlock(valvePos.west(), (on ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState(), QUIET); }
        void ticks(int ticks) { for (int i = 0; i < ticks; i++) { BlockState state = level.getBlockState(valvePos); var ticker = state.getTicker(level, ThaumcraftTubes.TUBE_BLOCK_ENTITY.get()); if (ticker == null) throw new IllegalStateException("Missing real shared tube ticker"); ticker.tick(level, valvePos, state, valve()); } }
        void phase(int modulus, int remainder) { for (int i = 0; i < 20 && valve().getTickCount() % modulus != remainder; i++) ticks(1); check(valve().getTickCount() % modulus == remainder && valve().getVenting() == 0, "bounded real local-counter advance reaches phase " + remainder + "/" + modulus); }
        void aim(Vec3 target) { player.setPos(target.x, target.y - player.getEyeHeight(), target.z + 2); player.setYRot(180); player.setYHeadRot(180); player.yHeadRotO = 180; player.setXRot(0); }
        BlockHitResult hit(BlockPos pos, Direction face) { return new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false); }
        UseOnContext context() { return new UseOnContext(player, InteractionHand.MAIN_HAND, hit(valvePos, Direction.SOUTH)); }
        InteractionResult use(BlockPos pos, Direction face) { return player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos, face)); }
        TileTube restoreValve(CompoundTag saved) { level.removeBlockEntity(valvePos); var tile = (TileTube) ((BlockTube) level.getBlockState(valvePos).getBlock()).newBlockEntity(valvePos, level.getBlockState(valvePos)); tile.loadWithComponents(saved, level.registryAccess()); level.setBlockEntity(tile); return tile; }
        Probe probe() { level.removeBlockEntity(sourcePos); level.setBlock(sourcePos, ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), QUIET); level.removeBlockEntity(sourcePos); Probe probe = new Probe(sourcePos, level.getBlockState(sourcePos)); level.setBlockEntity(probe); return probe; }
        List<ItemEntity> drops() { return level.getEntitiesOfClass(ItemEntity.class, bounds).stream().filter(drop -> !oldDrops.contains(drop.getUUID())).toList(); }
        @Override public void close() {
            drops().forEach(ItemEntity::discard);
            for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), QUIET);
            for (BlockEntity original : originals.values()) { original.clearRemoved(); level.setBlockEntity(original); }
            player.getInventory().clearContent(); for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            for (int slot = 0; slot < crafting.size(); slot++) player.inventoryMenu.getCraftSlots().setItem(slot, crafting.get(slot)); player.inventoryMenu.setCarried(cursor);
            player.getInventory().selected = selected; player.setGameMode(mode); player.getAbilities().loadSaveData(abilities); player.onUpdateAbilities(); player.setShiftKeyDown(sneaking); player.setPos(position); player.setDeltaMovement(motion);
            player.setYRot(yaw); player.setXRot(pitch); player.yHeadRot = headYaw; player.yHeadRotO = oldHeadYaw;
        }
    }

    /** Synchronous adversarial adapter only; gameplay phases above use registered native actors. */
    private static final class Probe extends BlockEntity implements EssentiaStorageEndpoint {
        Aspect type = Aspect.FIRE; int amount = 1; Runnable query, notice;
        Probe(BlockPos pos, BlockState state) { super(ThaumcraftTubes.TUBE_BLOCK_ENTITY.get(), pos, state); }
        @Override public Aspect getStoredEssentiaType() { return type; }
        @Override public int getStoredEssentiaAmount() { return amount; }
        @Override public int getEssentiaCapacity() { return 1; }
        @Override public void setEssentiaStorageRaw(Aspect aspect, int amount) { EssentiaTransfers.requireMutation(this); this.type = aspect; this.amount = amount; }
        @Override public void notifyEssentiaStorageChanged() { if (notice != null) notice.run(); }
        @Override public boolean isConnectable(Direction face) { return face != null; }
        @Override public boolean canInputFrom(Direction face) { return face != null; }
        @Override public boolean canOutputTo(Direction face) { return face != null; }
        @Override public void setSuction(Aspect aspect, int amount) {}
        @Override public Aspect getSuctionType(Direction face) { return null; }
        @Override public int getSuctionAmount(Direction face) { return 0; }
        @Override public Aspect getEssentiaType(Direction face) { if (query != null) { Runnable action = query; query = null; action.run(); } return type; }
        @Override public int getEssentiaAmount(Direction face) { return amount; }
        @Override public int takeEssentia(Aspect aspect, int amount, Direction face) { throw new AssertionError("No callback take/add transfer"); }
        @Override public int addEssentia(Aspect aspect, int amount, Direction face) { throw new AssertionError("No callback take/add transfer"); }
        @Override public int getMinimumSuction() { return 0; }
        @Override public boolean renderExtendedTube() { return false; }
    }
}
