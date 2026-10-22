package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.world.ExcavationHarvest;
import thaumcraft.common.tiles.TileJar;

/** One Fabric-first native boundary case, NOT a paid-focus/client/acquisition test.
 * All blocks, contents, enchanted wand and unique real player are explicit INPUTS.
 * Tests the transformed native protection hooks, not manual event dispatch. No
 * GameTest mock is used to claim a survival/creative difference. No execution/PASS
 * is implied by this source; root owns registration and the centralized run. */
public final class ExcavationHarvestRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private ExcavationHarvestRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        try (var check = new Check(test);
             var before = new BreakPhaseProbe(NativeBreakPhases.Phase.BEFORE, check, 1);
             var canceled = new BreakPhaseProbe(NativeBreakPhases.Phase.CANCELED, check, 2);
             var after = new BreakPhaseProbe(NativeBreakPhases.Phase.AFTER, check, 3)) {
            check.start();
            check.boundariesAndProtection();
            check.staleAndNested();
            check.ores();
            check.containers();
            check.blockPolicies();
        }
        test.succeed();
    }

    private static final class Check implements AutoCloseable {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final BlockPos base;
        private BlockPos target;
        private final AABB bounds;
        private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        private final Set<UUID> existing = new HashSet<>();
        private final int[] events = new int[4]; // Architectury, Fabric BEFORE/CANCELED/AFTER.
        private final BlockEvent.Break listener;
        private ItemStack wand;
        private EventResult result = EventResult.pass();
        private Runnable beforeAction = () -> {}, afterAction = () -> {};
        private boolean cancel, registered, worldTouched;

        private Check(GameTestHelper test) {
            this.test = test; level = test.getLevel(); base = test.absolutePos(new BlockPos(3, 3, 3)); target = base;
            bounds = new AABB(Vec3.atLowerCornerOf(base.offset(-3, -3, -3)), Vec3.atLowerCornerOf(base.offset(5, 4, 4)));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-3, -3, -3), base.offset(4, 3, 3))) {
                BlockPos pos = mutable.immutable(); blocks.put(pos, level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
            }
            level.getAllEntities().forEach(entity -> existing.add(entity.getUUID()));
            var profile = new GameProfile(UUID.randomUUID(), "exc-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            listener = (world, pos, state, actor, xp) -> {
                if (world != level || actor != player || !pos.equals(target)) return EventResult.pass();
                events[0]++;
                test.assertTrue(xp == null && actor.getMainHandItem() == wand, "real Fabric BREAK sees real actor/held wand; XP is not manually supplied");
                return result;
            };
        }

        private void start() {
            test.assertTrue(level.getEntities((Entity) null, bounds, entity -> entity instanceof ItemEntity
                    || entity instanceof ExperienceOrb || entity instanceof Silverfish || entity instanceof Bee).isEmpty(),
                    "owned input lane has no pre-existing items/XP/inhabitants to merge, mutate or discard");
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            test.assertTrue(player.level() == level && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS), "actual owned player and native block-drops rule are explicit inputs");
            player.setNoGravity(true);
            wand = new ItemStack(ThaumcraftContent.WAND.get());
            // Deliberately incompatible INPUT enchantments: neither may reach BASIC loot,
            // nor may the adapter strip/change them on the actual held stack.
            var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            wand.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), 3);
            wand.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            BlockEvent.BREAK.register(listener); registered = true;
            worldTouched = true;
            prepare(Blocks.STONE.defaultBlockState(), GameType.SURVIVAL);
            test.assertTrue(level.mayInteract(player, target), "fixture is not under unrelated native spawn protection");
            test.assertTrue(player.gameMode instanceof ExcavationHarvest.NativeScope && !player.hasDisconnected()
                    && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                    "actual owned connected player uses the transformed native game mode");
        }

        private void prepare(BlockState state, GameType mode) {
            purge();
            clearWorld();
            target = base; result = EventResult.pass(); cancel = false; beforeAction = () -> {}; afterAction = () -> {};
            Arrays.fill(events, 0);
            player.setGameMode(mode);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            test.assertTrue(player.isCreative() == (mode == GameType.CREATIVE), "unique REAL player actually changes creative state with native GameType");
            level.setBlock(target.below(), Blocks.STONE.defaultBlockState(), QUIET);
            level.setBlock(target, state, QUIET);
            // The production boundary deliberately refuses unloaded ray chunks.
            // Load this bounded test INPUT lane, never the distant refusal case.
            for (int x = (base.getX() - 10) >> 4; x <= (base.getX() + 5) >> 4; x++)
                for (int z = (base.getZ() - 1) >> 4; z <= (base.getZ() + 1) >> 4; z++) level.getChunk(x, z);
            aim(); feedback();
        }

        private void aim() {
            var shape = level.getBlockState(target).getCollisionShape(level, target, CollisionContext.of(player));
            Vec3 center = shape.isEmpty() ? Vec3.atCenterOf(target) : shape.bounds().getCenter().add(Vec3.atLowerCornerOf(target));
            player.setPos(center.add(3, -player.getEyeHeight(), 0));
            player.setYRot(90); player.setXRot(0); player.setDeltaMovement(Vec3.ZERO);
        }

        private boolean attempt() {
            ItemStack held = wand.copy();
            var food = new CompoundTag(); player.getFoodData().addAdditionalSaveData(food);
            var abilities = new CompoundTag(); player.getAbilities().addSaveData(abilities);
            var mode = player.gameMode.getGameModeForPlayer();
            var mined = Stats.BLOCK_MINED.get(level.getBlockState(target).getBlock());
            int stat = player.getStats().getValue(mined), used = player.getStats().getValue(Stats.ITEM_USED.get(wand.getItem()));
            boolean success = ExcavationHarvest.attempt(player, target, wand);
            var afterFood = new CompoundTag(); player.getFoodData().addAdditionalSaveData(afterFood);
            var afterAbilities = new CompoundTag(); player.getAbilities().addSaveData(afterAbilities);
            test.assertTrue(player.getMainHandItem() == wand && ItemStack.matches(held, wand)
                    && food.equals(afterFood) && abilities.equals(afterAbilities) && mode == player.gameMode.getGameModeForPlayer()
                    && player.getStats().getValue(mined) == stat && player.getStats().getValue(Stats.ITEM_USED.get(wand.getItem())) == used,
                    "boundary never pays/refunds, replaces/changes real wand, wears tools, changes abilities/mode, awards mining stats or adds exhaustion");
            test.assertTrue(feedback() == (success ? 1 : 0), "exactly one native2001 feedback INCLUDING caster only after actual removal witness");
            return success;
        }

        private void counts(int arch, int before, int canceled, int after) {
            test.assertTrue(Arrays.equals(events, new int[]{arch, before, canceled, after}),
                    "ACTUAL transformed native event counts [BREAK,BEFORE,CANCELED,AFTER]=" + Arrays.toString(events));
        }

        private void boundariesAndProtection() {
            test.assertTrue(!ExcavationHarvest.attempt(player, target, wand.copy()), "equal copied stack is not the actual held wand");
            test.assertTrue(!java.util.concurrent.CompletableFuture.supplyAsync(() -> ExcavationHarvest.attempt(player, target, wand)).join(), "worker thread refuses before world/inventory access");
            player.setXRot(-90); test.assertTrue(!attempt(), "final COLLIDER ray must actually hit target"); counts(0, 0, 0, 0);
            aim(); player.setPos(player.position().add(10, 0, 0)); test.assertTrue(!attempt(), "beyond ten blocks refuses without native events");
            BlockPos unloaded = base.offset(4096, 0, 4096);
            test.assertTrue(!level.hasChunkAt(unloaded) && !ExcavationHarvest.attempt(player, unloaded, wand)
                    && !level.hasChunkAt(unloaded), "unloaded target refuses without forcing its chunk"); counts(0, 0, 0, 0);
            prepare(Blocks.STONE.defaultBlockState(), GameType.SURVIVAL);
            level.setBlock(base.east(), Blocks.GLASS.defaultBlockState(), QUIET);
            test.assertTrue(!attempt() && level.getBlockState(base).is(Blocks.STONE), "real nearer COLLIDER glass wall occludes the final target"); counts(0, 0, 0, 0);
            prepare(Blocks.BEDROCK.defaultBlockState(), GameType.SURVIVAL); test.assertTrue(!attempt(), "negative hardness refuses"); counts(0, 0, 0, 0);
            prepare(Blocks.STONE.defaultBlockState(), GameType.ADVENTURE); test.assertTrue(!attempt() && level.getBlockState(target).is(Blocks.STONE), "real native adventure restrictions remain intact"); counts(1, 0, 0, 0);
            for (boolean fabric : List.of(false, true)) {
                prepare(ThaumcraftContent.JAR.get().defaultBlockState(), GameType.SURVIVAL);
                var jar = (TileJar) level.getBlockEntity(target); jar.addToContainer(Aspect.AIR, 17);
                var saved = jar.saveWithoutMetadata(level.registryAccess());
                if (fabric) cancel = true; else result = EventResult.interruptFalse();
                test.assertTrue(!attempt() && level.getBlockEntity(target) == jar && saved.equals(jar.saveWithoutMetadata(level.registryAccess()))
                        && items().isEmpty() && xp() == 0, "real protection denial preserves same live BE, contents, drops and XP");
                if (fabric) counts(1, 1, 1, 0); else counts(1, 0, 0, 0);
            }
            prepare(Blocks.STONE.defaultBlockState(), GameType.CREATIVE); result = EventResult.interruptTrue();
            test.assertTrue(attempt() && count(Items.COBBLESTONE) == 1, "EventResult interruptTrue is NOT cancellation and creative spell harvest still drops"); counts(1, 1, 0, 1);
        }

        private void staleAndNested() {
            prepare(ThaumcraftContent.JAR.get().defaultBlockState(), GameType.SURVIVAL);
            var old = (TileJar) level.getBlockEntity(target); old.addToContainer(Aspect.AIR, 13);
            var replacement = new TileJar(ThaumcraftContent.JAR_BLOCK_ENTITY.get(), target, level.getBlockState(target));
            beforeAction = () -> { level.removeBlockEntity(target); level.setBlockEntity(replacement); };
            test.assertTrue(!attempt() && level.getBlockEntity(target) == replacement && old.getAmount() == 13 && items().isEmpty(), "same-state replacement BE invalidates captured identity; no output/second removal"); counts(1, 1, 0, 0);

            prepare(Blocks.DIAMOND_ORE.defaultBlockState(), GameType.SURVIVAL);
            beforeAction = () -> level.setBlock(target, Blocks.AIR.defaultBlockState(), QUIET);
            test.assertTrue(!attempt() && level.isEmptyBlock(target) && items().isEmpty() && xp() == 0, "native optimistic returntrue AND later air cannot fabricate a removal witness"); counts(1, 1, 0, 0);

            prepare(Blocks.DIAMOND_ORE.defaultBlockState(), GameType.SURVIVAL);
            beforeAction = () -> player.setXRot(-90);
            test.assertTrue(!attempt() && level.getBlockState(target).is(Blocks.DIAMOND_ORE) && items().isEmpty() && xp() == 0, "actor's final authorization/ray is revalidated AFTER native protection callbacks"); counts(1, 1, 0, 0);

            prepare(ThaumcraftContent.JAR.get().defaultBlockState(), GameType.SURVIVAL);
            var captured = (TileJar) level.getBlockEntity(target); captured.addToContainer(Aspect.AIR, 19);
            ItemStack expected = new ItemStack(ThaumcraftContent.JAR.get()); captured.saveToItem(expected, level.registryAccess()); expected.set(DataComponents.MAX_STACK_SIZE, 1);
            afterAction = () -> {
                level.setBlock(target, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
                ((TileJar) level.getBlockEntity(target)).addToContainer(Aspect.FIRE, 7);
            };
            test.assertTrue(attempt() && items().size() == 1 && ItemStack.matches(items().getFirst().getItem(), expected)
                    && level.getBlockEntity(target) instanceof TileJar jar && jar != captured && jar.getAspect() == Aspect.FIRE && jar.getAmount() == 7,
                    "AFTER replacement survives untouched while exactly OLD captured BE resources finish"); counts(1, 1, 0, 1);

            prepare(Blocks.DIAMOND_ORE.defaultBlockState(), GameType.SURVIVAL);
            beforeAction = () -> {
                if (events[1] != 1) return;
                test.assertTrue(!ExcavationHarvest.attempt(player, target, wand), "reentrant spell attempt refused without nested events");
                test.assertTrue(player.gameMode.destroyBlock(target), "ordinary nested SAME actor+position still runs native destroyBlock");
            };
            test.assertTrue(!attempt() && items().isEmpty() && xp() == 0, "nested ordinary wand has NO tier privilege, loot or XP; outer stale attempt cannot duplicate it"); counts(2, 2, 0, 1);

            prepare(Blocks.DIAMOND_ORE.defaultBlockState(), GameType.SURVIVAL);
            var failure = new IllegalStateException("owned native excavation event failure");
            beforeAction = () -> { if (events[1] == 1) player.gameMode.destroyBlock(target); else throw failure; };
            boolean caught = false;
            try { ExcavationHarvest.attempt(player, target, wand); } catch (IllegalStateException expectedFailure) { caught = expectedFailure == failure; }
            test.assertTrue(caught && level.getBlockState(target).is(Blocks.DIAMOND_ORE), "nested native event exception propagates with no premature removal"); counts(2, 2, 0, 0);
            beforeAction = () -> {};
            test.assertTrue(player.gameMode.destroyBlock(target) && items().isEmpty() && xp() == 0 && feedback() == 0, "whole-method finally removes privileges after nested exception; ordinary wrong-tier break stays ordinary");
            prepare(Blocks.DIAMOND_ORE.defaultBlockState(), GameType.SURVIVAL);
            test.assertTrue(attempt() && count(Items.DIAMOND) == 1 && xp() > 0, "fresh authorized attempt works after exception, without stale/reentrant scope");
        }

        private void ores() {
            for (Block ore : List.of(Blocks.DIAMOND_ORE, Blocks.SPAWNER, Blocks.IRON_ORE)) {
                prepare(ore.defaultBlockState(), GameType.SURVIVAL);
                test.assertTrue(ore == Blocks.SPAWNER || !player.hasCorrectToolForDrops(level.getBlockState(target)), "actual wand does not acquire native mining tier");
                test.assertTrue(attempt(), "native spell removes tier ore/spawner"); counts(1, 1, 0, 1);
                test.assertTrue(ore == Blocks.DIAMOND_ORE ? count(Items.DIAMOND) == 1 && xp() >= 3
                        : ore == Blocks.SPAWNER ? items().isEmpty() && xp() >= 15
                        : count(Items.RAW_IRON) == 1 && xp() == 0, "native BASIC fortune0/no-silk loot and generic ore/spawner XP; modern raw iron is explicit policy");
            }
        }

        private void containers() {
            for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
                prepare(Blocks.CHEST.defaultBlockState(), mode);
                var chest = (ChestBlockEntity) level.getBlockEntity(target);
                chest.setItem(0, new ItemStack(Items.EMERALD, 5)); chest.setItem(26, new ItemStack(Items.QUARTZ, 7));
                test.assertTrue(attempt() && count(Items.CHEST) == 1 && count(Items.EMERALD) == 5 && count(Items.QUARTZ) == 7, "filled chest contents emerge ONLY from native onRemove, once, in " + mode); counts(1, 1, 0, 1);
                prepare(ThaumcraftContent.JAR.get().defaultBlockState(), mode);
                var jar = (TileJar) level.getBlockEntity(target); jar.addToContainer(Aspect.AIR, 31); jar.setFilter(Aspect.AIR);
                ItemStack expected = new ItemStack(ThaumcraftContent.JAR.get()); jar.saveToItem(expected, level.registryAccess()); expected.set(DataComponents.MAX_STACK_SIZE, 1);
                test.assertTrue(attempt() && items().size() == 1 && ItemStack.matches(expected, items().getFirst().getItem()), "filled labelled TC jar loot uses captured BE, once, in " + mode); counts(1, 1, 0, 1);
                for (boolean unopened : List.of(false, true)) {
                    prepare(Blocks.SHULKER_BOX.defaultBlockState(), mode);
                    ItemStack input = new ItemStack(Items.SHULKER_BOX);
                    input.set(DataComponents.CUSTOM_NAME, Component.literal("owned excavation box")); input.set(DataComponents.LOCK, new LockCode("exc-key"));
                    if (unopened) input.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(BuiltInLootTables.SIMPLE_DUNGEON, 473L));
                    else input.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.EMERALD, 3), new ItemStack(Items.QUARTZ, 2))));
                    var box = (ShulkerBoxBlockEntity) level.getBlockEntity(target); box.applyComponentsFromItemStack(input);
                    test.assertTrue(attempt() && count(Items.SHULKER_BOX) == 1 && items().size() == 1, "filled/unopened shulker has no creative pre-drop or duplicate in " + mode); counts(1, 1, 0, 1);
                    ItemStack output = items().getFirst().getItem();
                    for (var component : List.of(DataComponents.CUSTOM_NAME, DataComponents.LOCK, DataComponents.CONTAINER_LOOT))
                        test.assertTrue(Objects.equals(input.get(component), output.get(component)), "native shulker retains container/name/lock/unopened loot: " + component);
                    test.assertTrue(input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                            .equals(output.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)), "native shulker keeps exact contents; absent/explicit EMPTY are equivalent for unopened input");
                    test.assertTrue(!unopened || box.getLootTable() == BuiltInLootTables.SIMPLE_DUNGEON, "unopened loot is not unpacked with fake player luck");
                }
            }
        }

        private void blockPolicies() {
            prepare(Blocks.ICE.defaultBlockState(), GameType.SURVIVAL);
            test.assertTrue(attempt() && level.isEmptyBlock(target) && items().isEmpty(), "ice stays AIR: no virtual IceBlock.playerDestroy water, no silk"); counts(1, 1, 0, 1);
            prepare(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true), GameType.SURVIVAL);
            test.assertTrue(attempt() && level.getFluidState(target).getType() == Fluids.WATER && count(Items.OAK_SLAB) == 1, "native waterlogged fluid restoration is intentional");
            for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
                for (boolean upper : List.of(false, true)) {
                    prepare(Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), mode);
                    level.setBlock(base.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), QUIET);
                    if (upper) target = base.above(); aim();
                    test.assertTrue(attempt() && level.isEmptyBlock(base) && level.isEmptyBlock(base.above()) && count(Items.OAK_DOOR) == 1, "both door halves, native neighbour removal and lower loot, in " + mode); counts(1, 1, 0, 1);
                }
                for (boolean head : List.of(false, true)) {
                    prepare(Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT), mode);
                    level.setBlock(base.north(), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD), QUIET);
                    level.setBlock(base.north().below(), Blocks.STONE.defaultBlockState(), QUIET);
                    if (head) target = base.north(); aim();
                    test.assertTrue(attempt() && level.isEmptyBlock(base) && level.isEmptyBlock(base.north()) && count(Items.RED_BED) == 1, "both bed halves, native neighbour removal and head loot, in " + mode); counts(1, 1, 0, 1);
                }
                for (Block hive : List.of(Blocks.BEEHIVE, Blocks.BEE_NEST)) {
                    prepare(hive.defaultBlockState(), mode);
                    var tile = (BeehiveBlockEntity) level.getBlockEntity(target); tile.storeBee(BeehiveBlockEntity.Occupant.create(0));
                    var saved = tile.saveWithoutMetadata(level.registryAccess());
                    test.assertTrue(!attempt() && level.getBlockEntity(target) == tile && saved.equals(tile.saveWithoutMetadata(level.registryAccess())) && items().isEmpty(), "occupied hive/nest conservatively refused; no bees lost, fake silk or hand-spawn in " + mode); counts(0, 0, 0, 0);
                }
            }
            prepare(Blocks.BEEHIVE.defaultBlockState(), GameType.SURVIVAL);
            var newlyOccupied = (BeehiveBlockEntity) level.getBlockEntity(target);
            beforeAction = () -> newlyOccupied.storeBee(BeehiveBlockEntity.Occupant.create(0));
            test.assertTrue(!attempt() && level.getBlockEntity(target) == newlyOccupied && !newlyOccupied.isEmpty() && items().isEmpty(), "hive populated DURING native BEFORE is also conservatively refused at removal"); counts(1, 1, 0, 0);
            prepare(Blocks.INFESTED_STONE.defaultBlockState(), GameType.SURVIVAL);
            test.assertTrue(attempt() && spawned().stream().noneMatch(Silverfish.class::isInstance), "only scoped modern infestation extra-spawn is suppressed"); counts(1, 1, 0, 1);
            prepare(Blocks.INFESTED_STONE.defaultBlockState(), GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            // Observer assertion is about the actual actor's held input, including
            // this intentionally ORDINARY pickaxe control outside the helper.
            ItemStack retained = wand; wand = player.getMainHandItem();
            try { test.assertTrue(player.gameMode.destroyBlock(target) && spawned().stream().filter(Silverfish.class::isInstance).count() == 1, "ordinary external native infestation still spawns exactly one silverfish"); counts(1, 1, 0, 1); }
            finally { wand = retained; player.setItemInHand(InteractionHand.MAIN_HAND, wand); }
        }

        private List<Entity> spawned() {
            return level.getEntities((Entity) null, bounds, entity -> !existing.contains(entity.getUUID())
                    && (entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof Silverfish || entity instanceof Bee));
        }
        private List<ItemEntity> items() { return spawned().stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast).toList(); }
        private int count(Item item) { return items().stream().map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
        private int xp() { return spawned().stream().filter(ExperienceOrb.class::isInstance).map(ExperienceOrb.class::cast).mapToInt(ExperienceOrb::getValue).sum(); }
        private void purge() { spawned().forEach(Entity::discard); }
        private int feedback() {
            channel.runPendingTasks(); int count = 0; Object packet;
            while ((packet = channel.readOutbound()) != null) {
                if (packet instanceof ClientboundLevelEventPacket event && event.getType() == 2001 && event.getPos().equals(target)) count++;
                io.netty.util.ReferenceCountUtil.release(packet);
            }
            return count;
        }
        private void clearWorld() {
            // Remove BE FIRST: fixture cleanup must not unpack unopened containers
            // or produce additional native onRemove contents while restoring world.
            blocks.keySet().forEach(pos -> { level.removeBlockEntity(pos); if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET); });
        }
        @Override public void close() {
            if (registered) BlockEvent.BREAK.unregister(listener);
            try {
                if (worldTouched) {
                    clearWorld(); purge();
                    blocks.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                    tiles.forEach((pos, tag) -> { if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, level.registryAccess()); });
                }
            } finally {
                try { if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player); }
                finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }
    }

    /** The loader's own BEFORE/CANCELED/AFTER phase of one break. The loader owns the
     * mechanism: Fabric decorates its own event invoker, NeoForge observes its native
     * {@code BlockEvent.BreakEvent} and its real removal. No loader class is named here. */
    private static final class BreakPhaseProbe implements AutoCloseable {
        private final NativeBreakPhases probe;
        private BreakPhaseProbe(NativeBreakPhases.Phase phase, Check check, int index) {
            NativeBreakPhases.Listener listener = switch (phase) {
                case BEFORE -> new NativeBreakPhases.Listener() {
                    @Override public boolean before(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
                        if (!observed(check, index, level, player, pos)) return true;
                        check.beforeAction.run();
                        return !check.cancel;
                    }
                };
                case CANCELED -> new NativeBreakPhases.Listener() {
                    @Override public void canceled(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
                        observed(check, index, level, player, pos);
                    }
                };
                case AFTER -> new NativeBreakPhases.Listener() {
                    @Override public void after(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
                        if (!observed(check, index, level, player, pos)) return;
                        check.afterAction.run();
                    }
                };
            };
            probe = NativeBreakPhases.Holder.factory().install(check.level, check.player, phase, listener);
        }
        /** The exact filter of the original loader event: this level, this player, this cell. */
        private static boolean observed(Check check, int index, ServerLevel level, ServerPlayer player, BlockPos pos) {
            if (level != check.level || player != check.player || !check.target.equals(pos)) return false;
            check.events[index]++;
            return true;
        }
        @Override public void close() { probe.close(); }
    }
}
