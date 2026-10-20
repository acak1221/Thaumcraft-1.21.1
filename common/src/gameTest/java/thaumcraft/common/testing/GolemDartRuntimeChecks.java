package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.projectile.EntityDart;
import thaumcraft.common.items.golems.GolemDartRules;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/**
 * The golem dart launcher on real content: the loaded arcane recipe, installation by a real connected player, the dart's launch
 * geometry, and four real golems on separate lanes that WALK towards real spiders: a Guard with the launcher fires darts at its own
 * interval (30 ticks, shorter with Air upgrades) and one of them hurts the spider by the dart's damage, while a Guard without the
 * launcher and a Fill golem that wears one fire nothing. Players, golems and spiders are explicit inputs; when and how often a dart
 * leaves, its flight and damage are never supplied. NOT a client or visual check (the dart renderer and arm pose are unseen), a
 * Butcher check, accuracy statistics, multiplayer over a network or a saved-world check.
 */
public final class GolemDartRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int THAUMIUM = 7, DEADLINE = 1400, DISTANCE = 14, NEAR = 12, CLOSE = 2, PLAIN_SHOTS = 3, QUICK_SHOTS = 5, IDLE_TICKS = 220;
    private static final float WALK = 0.12F; // about a tenth of a block per tick: slow enough for a few shots between the reach and the melee range
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemDartRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.recipeAndRegistration();
            check.start();
            check.installation();
            check.launchGeometry();
            check.armLanes();
        } catch (RuntimeException failure) {
            check.finish();
            throw failure;
        }
        test.onEachTick(() -> {
            if (check.finished) return;
            try {
                if (check.poll()) { check.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.error("Dart lanes failed at tick {}: {} ({})", test.getTick(), failure.getMessage(), check.describe());
                check.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (check.finished) return;
            String state = check.describe();
            check.finish();
            test.fail("the dart lanes did not finish within " + DEADLINE + " ticks: " + state);
        });
    }

    private static final class Check {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final BlockPos base;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final Set<UUID> existing = new HashSet<>();
        private final List<Entity> spawned = new ArrayList<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<Lane> lanes = new ArrayList<>();
        private boolean touched, finished;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            // A small footprint: neighbouring tests keep their own fixtures within a few blocks of their structures and must not be wiped.
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(2, 2, 1))) original.put(mutable.immutable(), level.getBlockState(mutable));
            level.getAllEntities().forEach(entity -> existing.add(entity.getUUID()));
            var profile = new GameProfile(UUID.randomUUID(), "dart-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        }

        private GolemData config(int core, String decoration, List<Integer> upgrades) {
            return new GolemData(core, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, upgrades,
                    List.of(), GolemGuardTargets.DEFAULT, false, decoration);
        }

        private void forceChunks(BlockPos around, int halo) {
            var center = new ChunkPos(around);
            for (int dx = -halo; dx <= halo; dx++) for (int dz = -halo; dz <= halo; dz++) {
                var chunk = new ChunkPos(center.x + dx, center.z + dz);
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk); }
            }
        }

        private EntityGolemBase golem(BlockPos at, int core, String decoration, List<Integer> upgrades) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(THAUMIUM);
            golem.applyConfiguration(config(core, decoration, upgrades));
            golem.setPos(Vec3.atBottomCenterOf(at));
            golem.setHome(at, Direction.UP);
            golem.setOwnerName(player.getGameProfile().getName());
            golem.setPersistenceRequired();
            test.assertTrue(level.addFreshEntity(golem), "a real golem is admitted");
            spawned.add(golem);
            return golem;
        }

        private Mob spider(BlockPos at) {
            var spider = EntityType.SPIDER.create(level);
            spider.moveTo(Vec3.atBottomCenterOf(at));
            spider.setNoAi(true);
            spider.setNoGravity(true);
            spider.setPersistenceRequired();
            spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            spider.setHealth(1000);
            test.assertTrue(level.addFreshEntity(spider), "a real spider is admitted");
            spawned.add(spider);
            return spider;
        }

        // ---- the loaded recipe and the registered types -------------------------------------------------------------

        private void recipeAndRegistration() {
            var dartItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "golem_decoration_dart"));
            test.assertTrue(dartItem != Items.AIR && dartItem == ThaumcraftGolems.decorationItem('R'), "the dart launcher item is registered and is the item of letter R");
            var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "dart"));
            test.assertTrue(type == ThaumcraftGolems.DART.get() && type.getWidth() == 0.5F && type.getHeight() == 0.5F, "the dart entity is registered, half a block across");
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/golem_decoration_dart"));
            test.assertTrue(holder.isPresent() && holder.get().value() instanceof ArcaneShapedRecipe, "the dart launcher arcane recipe is loaded");
            var recipe = (ArcaneShapedRecipe) holder.get().value();
            test.assertTrue(recipe.research().equals("TINYDART") && recipe.pattern().equals(List.of("AIA", "ADA", "AIA"))
                            && recipe.vis().equals(Map.of("aer", 4, "ignis", 4)) && recipe.result().is(dartItem) && recipe.result().getCount() == 1
                            && recipe.key().keySet().equals(Set.of("A", "I", "D")),
                    "research TINYDART, arrows and iron around a dispenser, aer 4 + ignis 4, one launcher");
            test.assertTrue(recipe.key().get("A").test(new ItemStack(Items.ARROW)) && recipe.key().get("I").test(new ItemStack(Items.IRON_INGOT))
                            && recipe.key().get("D").test(new ItemStack(Items.DISPENSER)) && !recipe.key().get("D").test(new ItemStack(Items.DROPPER)),
                    "the ingredients are an arrow, an iron ingot and a dispenser (not a dropper)");
        }

        private void start() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                    "an actual connected player is an explicit input");
            player.setNoGravity(true);
            touched = true;
            for (BlockPos mutable : original.keySet()) {
                level.setBlock(mutable, mutable.getY() == base.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            }
            test.assertTrue(level.mayInteract(player, base), "fixture is not under unrelated spawn protection");
        }

        private void hold(GameType mode, ItemStack stack) {
            player.setGameMode(mode);
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setPos(Vec3.atBottomCenterOf(base).add(1.2, 0, 0));
        }

        private void installation() {
            var dartItem = ThaumcraftGolems.DECORATION_DART.get();
            var guard = golem(base, GolemData.GUARD, "", List.of());
            hold(GameType.SURVIVAL, new ItemStack(dartItem, 2));
            test.assertTrue(guard.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && guard.getDecoration().equals("R")
                            && player.getMainHandItem().getCount() == 1, "a real player installs the launcher on a Guard and pays one item");
            test.assertTrue(guard.interact(player, InteractionHand.MAIN_HAND) != InteractionResult.CONSUME && guard.getDecoration().equals("R")
                            && player.getMainHandItem().getCount() == 1, "a second launcher is refused without payment");
            for (Entity entity : spawned) entity.discard();
            spawned.clear();

            // On a core without combat the original also accepted the letter; it only does nothing there.
            var fill = golem(base, GolemData.FILL, "", List.of());
            hold(GameType.SURVIVAL, new ItemStack(dartItem));
            test.assertTrue(fill.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && fill.getDecoration().equals("R"),
                    "the launcher installs on a Fill golem too (inert there)");
            var tag = new CompoundTag();
            fill.saveWithoutId(tag);
            var reloaded = ThaumcraftGolems.GOLEM.get().create(level);
            reloaded.load(tag.copy());
            test.assertTrue(tag.getString("Decoration").equals("R") && reloaded.getDecoration().equals("R") && reloaded.configuredData().decoration().equals("R"),
                    "the letter is saved under the original key and survives a reload");
            reloaded.discard();
            for (Entity entity : spawned) entity.discard();
            spawned.clear();
        }

        // ---- the dart itself ------------------------------------------------------------------------------------------

        private void launchGeometry() {
            var shooter = golem(base, GolemData.GUARD, "R", List.of());
            var target = spider(base.offset(2, 0, 1));
            var dart = new EntityDart(level, shooter, target, GolemDartRules.SPEED, 0F);
            // The original arithmetic, written out again: a start just above the eye line, the aim at the target's chest lifted by a
            // fifth of the horizontal distance, the speed 1.6 along it (no spread was asked for).
            double y = shooter.getY() + shooter.getEyeHeight() - 0.1F;
            double dx = target.getX() - shooter.getX(), dy = target.getY() + target.getEyeHeight() - 0.7F - y, dz = target.getZ() - shooter.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            Vec3 expected = new Vec3(dx, dy + (float) horizontal * 0.2F, dz).normalize().scale(GolemDartRules.SPEED);
            test.assertTrue(dart.getOwner() == shooter && dart.pickup == AbstractArrow.Pickup.DISALLOWED,
                    "the golem owns the dart and nobody can pick it up");
            test.assertTrue(dart.getDeltaMovement().distanceTo(expected) < 1.0E-4 && Math.abs(dart.getDeltaMovement().length() - 1.6) < 1.0E-4,
                    "the dart leaves at speed 1.6 along the lifted aim: " + dart.getDeltaMovement() + " expected " + expected);
            test.assertTrue(Math.abs(dart.getX() - (shooter.getX() + dx / horizontal / 5.0)) < 1.0E-6 && Math.abs(dart.getY() - y) < 1.0E-6
                            && Math.abs(dart.getZ() - (shooter.getZ() + dz / horizontal / 5.0)) < 1.0E-6,
                    "and starts a fifth of a block towards the target, just above the shooter's eye line: " + dart.position());
            // A flying dart must be saveable (the world saves with darts in the air) and comes back as the same kind of dart.
            level.addFreshEntity(dart);
            var saved = new CompoundTag();
            dart.saveWithoutId(saved);
            test.assertTrue(saved.contains("item") && saved.getByte("pickup") == AbstractArrow.Pickup.DISALLOWED.ordinal(),
                    "the dart saves as an arrow that nobody may pick up: " + saved);
            var restored = ThaumcraftGolems.DART.get().create(level);
            restored.load(saved.copy());
            test.assertTrue(restored.pickup == AbstractArrow.Pickup.DISALLOWED && restored.getDeltaMovement().distanceTo(dart.getDeltaMovement()) < 1.0E-6,
                    "and loads with its motion and without pickup");
            restored.discard();
            // With a spread the direction wanders, but the speed is a plain scaled vector only without it.
            var scattered = new EntityDart(level, shooter, target, GolemDartRules.SPEED, GolemDartRules.inaccuracy(0));
            test.assertTrue(scattered.getDeltaMovement().distanceTo(expected) < 1.0, "a spread of seven keeps the dart near the aim");
            dart.discard();
            scattered.discard();
            for (Entity entity : spawned) entity.discard();
            spawned.clear();
        }

        // ---- lanes: real golems that walk and shoot ------------------------------------------------------------------

        private void armLanes() {
            // Far from the lanes other tests use (z + 40, 80, 120, 160): a Guard targets the NEAREST hostile in its range, so a stray
            // spider of a neighbouring test would silently become its target.
            List<BlockPos> bases = List.of(base.offset(0, 0, 240), base.offset(0, 0, 280), base.offset(0, 0, 320), base.offset(0, 0, 360), base.offset(0, 0, 400),
                    base.offset(0, 0, 440));
            for (BlockPos pos : bases) forceChunks(pos, 2);
            // PLAIN and INSTALLED-LATE start inside the reach (80% of 16 = 12.8 blocks), so their first dart tells the half-interval wait;
            // QUICK starts beyond it, so its first dart must wait until the golem has walked into the reach.
            lanes.add(new ShootLane("PLAIN", bases.get(0), List.of(), PLAIN_SHOTS, NEAR, false));
            lanes.add(new ShootLane("QUICK", bases.get(1), List.of(GolemUpgrade.AIR.id, GolemUpgrade.AIR.id), QUICK_SHOTS, DISTANCE, false));
            lanes.add(new ShootLane("INSTALLED-LATE", bases.get(2), List.of(), 2, NEAR, true));
            lanes.add(new IdleLane("NO-LAUNCHER", bases.get(3), GolemData.GUARD, "", DISTANCE, false));
            lanes.add(new IdleLane("FILL", bases.get(4), GolemData.FILL, "R", DISTANCE, false));
            // A spider two blocks away is melee range, not dart range: the launcher stays quiet and the Guard fights with its hands.
            lanes.add(new IdleLane("TOO-CLOSE", bases.get(5), GolemData.GUARD, "R", CLOSE, true));
            for (Lane each : lanes) each.setup();
        }

        private abstract class Lane {
            final String name;
            final BlockPos b;
            final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
            final AABB region;
            long startedAt;
            boolean done;

            Lane(String name, BlockPos b) {
                this.name = name; this.b = b;
                for (BlockPos pos : BlockPos.betweenClosed(b.offset(-4, -2, -4), b.offset(4, 5, DISTANCE + 4))) saved.put(pos.immutable(), level.getBlockState(pos));
                region = new AABB(Vec3.atLowerCornerOf(b.offset(-5, -2, -5)), Vec3.atLowerCornerOf(b.offset(5, 6, DISTANCE + 5)));
            }

            void setup() {
                for (BlockPos pos : saved.keySet()) {
                    level.setBlock(pos, pos.getY() == b.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
                }
                startedAt = test.getTick();
                arm();
            }
            abstract void arm();
            abstract void poll();
            abstract String status();
            void restore() { saved.forEach((pos, state) -> level.setBlock(pos, state, QUIET)); }

            EntityGolemBase walker(int core, String decoration, List<Integer> upgrades) {
                var golem = golem(b, core, decoration, upgrades);
                golem.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(WALK); // Slow and identical, so the shots are told apart by their interval.
                return golem;
            }
        }

        /** A Guard with the launcher walks to a spider 14 blocks away; its darts are counted, timed and one of them must hurt. */
        private final class ShootLane extends Lane {
            final List<Integer> upgrades;
            final int wanted;
            final int distance;
            final boolean late;
            boolean armReset;
            EntityGolemBase guard;
            Mob target;
            final Set<UUID> seen = new HashSet<>();
            final List<Long> shots = new ArrayList<>();
            double nearest = Double.MAX_VALUE, farthestShot = 0;
            double hit;
            int maxArm;

            ShootLane(String name, BlockPos b, List<Integer> upgrades, int wanted, int distance, boolean late) {
                super(name, b);
                this.upgrades = upgrades; this.wanted = wanted; this.distance = distance; this.late = late;
            }

            @Override void arm() {
                target = spider(b.offset(0, 0, distance));
                guard = walker(GolemData.GUARD, late ? "" : "R", upgrades);
                if (late) {
                    // The launcher is fitted afterwards by a real player: the goal must appear without any other rebuild.
                    player.setGameMode(GameType.SURVIVAL);
                    player.setShiftKeyDown(false);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftGolems.DECORATION_DART.get()));
                    player.setPos(Vec3.atBottomCenterOf(b).add(1.2, 0, 0));
                    test.assertTrue(guard.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && player.getMainHandItem().isEmpty(),
                            name + ": a real player fits the launcher on the walking Guard");
                    guard.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(WALK); // Fitting an accessory re-derives the material speed.
                }
                test.assertTrue(guard.hasSupportedCore() && guard.getDecoration().equals("R") && guard.getUpgradeAmount(GolemUpgrade.AIR) == upgrades.size(),
                        name + ": the Guard wears the launcher with " + upgrades.size() + " Air upgrades");
            }

            @Override void poll() {
                test.assertTrue(guard.isAlive(), name + ": the Guard survived");
                double distanceSq = guard.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
                nearest = Math.min(nearest, distanceSq);
                maxArm = Math.max(maxArm, guard.getLeftArmTimer());
                if (!shots.isEmpty() && guard.getLeftArmTimer() == 0) armReset = true;
                // Only the Guard's own darts are timed: a neighbouring test's Guard with a launcher may shoot into the lane from outside.
                for (EntityDart dart : level.getEntitiesOfClass(EntityDart.class, region, entity -> !existing.contains(entity.getUUID()) && entity.getOwner() == guard)) {
                    if (!seen.add(dart.getUUID())) continue;
                    test.assertTrue(guard.getTarget() == target,
                            name + ": the Guard shoots at the lane's spider, not at a stray target: " + guard.getTarget());
                    double attack = (float) guard.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F;
                    test.assertTrue(dart.getBaseDamage() == attack && dart.getBaseDamage() > 0,
                            name + ": the dart does 0.4 of the golem's attack: " + dart.getBaseDamage() + " vs " + attack);
                    shots.add(test.getTick());
                    farthestShot = Math.max(farthestShot, distanceSq);
                    double distance = Math.sqrt(distanceSq);
                    test.assertTrue(distance >= 2.7 && distance <= guard.getRange() * 0.8 + 0.3,
                            name + ": a dart left at distance " + distance + ", outside the band from three blocks to 80% of the range " + guard.getRange());
                }
                float lost = target.getMaxHealth() - target.getHealth();
                if (hit == 0 && lost > 0) {
                    test.assertTrue(distanceSq >= 9.0, name + ": the first wound came from a dart, not from melee (distance " + Math.sqrt(distanceSq) + ")");
                    hit = lost;
                }
                if (shots.size() >= wanted) {
                    int interval = GolemDartRules.interval(upgrades.size());
                    long first = shots.getFirst() - startedAt, delay = GolemDartRules.firstDelay(interval);
                    test.assertTrue(first >= delay && (distance > NEAR || first <= delay + 8),
                            name + ": the first dart waits half an interval (" + delay + " ticks) after the Guard takes its target, saw " + first);
                    long minGap = Long.MAX_VALUE;
                    for (int index = 1; index < shots.size(); index++) minGap = Math.min(minGap, shots.get(index) - shots.get(index - 1));
                    test.assertTrue(minGap >= interval && minGap <= interval + 2,
                            name + ": consecutive darts are one interval (" + interval + " ticks) apart: shots at " + shots + ", smallest gap " + minGap);
                    test.assertTrue(maxArm > 0 && armReset, name + ": the left arm was raised by a shot and came down again");
                    if (hit > 0) {
                        // Whether a given dart happens to hit is the spread's business; when one did, it took what the arrow rule says.
                        // The dart does 0.4 of the attack attribute times the speed it still has on arrival, rounded up. That speed is the
                        // launch speed 1.6 less the drag of the flight, but the spread is added to the unit direction without renormalising,
                        // so an inaccurate shot can arrive up to about 6% faster than it left (1.7): a hit of 5.0 for 2.4 was seen.
                        double damage = (float) guard.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F;
                        int low = (int) Math.ceil(1.2 * damage), high = (int) Math.ceil(1.75 * damage);
                        test.assertTrue(hit >= low && hit <= high, name + ": a dart took " + hit + " health; ceil(speed * " + damage + ") lies between " + low + " and " + high);
                    }
                    Thaumcraft.LOG.info("Dart lane {}: shots at {} (minimum gap {}), nearest {} blocks, first dart wound {}", name, shots, minGap, Math.sqrt(nearest), hit);
                    done = true;
                }
            }

            @Override String status() {
                return "shots " + shots + ", nearest " + (nearest == Double.MAX_VALUE ? "?" : String.format("%.1f", Math.sqrt(nearest))) + ", wound " + hit
                        + ", golem at " + guard.blockPosition().toShortString();
            }
        }

        /** A golem that must NOT shoot, standing against a spider it may well attack in melee (and, when {@code fights}, must). */
        private final class IdleLane extends Lane {
            final int core;
            final String decoration;
            final int distance;
            final boolean fights;
            EntityGolemBase golem;
            Mob target;
            double nearest = Double.MAX_VALUE;
            int darts;
            boolean fought;

            IdleLane(String name, BlockPos b, int core, String decoration, int distance, boolean fights) {
                super(name, b);
                this.core = core; this.decoration = decoration; this.distance = distance; this.fights = fights;
            }

            @Override void arm() {
                target = spider(b.offset(0, 0, distance));
                golem = walker(core, decoration, List.of());
            }

            @Override void poll() {
                test.assertTrue(golem.isAlive(), name + ": the golem survived");
                nearest = Math.min(nearest, golem.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()));
                // Only this golem's own darts count: a neighbouring test's Guard with a launcher may shoot into the lane from outside.
                darts += level.getEntitiesOfClass(EntityDart.class, region, entity -> !existing.contains(entity.getUUID()) && entity.getOwner() == golem).size();
                test.assertTrue(darts == 0, name + ": no dart may leave");
                fought |= target.getHealth() < target.getMaxHealth();
                if (test.getTick() >= startedAt + IDLE_TICKS) {
                    test.assertTrue(golem.getLeftArmTimer() == 0, name + ": the left arm never rises without a shot");
                    test.assertTrue(!fights || fought && golem.getTarget() == target,
                            name + ": the Guard took the spider as its target and hurt it in melee (otherwise the quiet launcher proves nothing)");
                    done = true;
                }
            }

            @Override String status() { return "darts " + darts + ", nearest " + (nearest == Double.MAX_VALUE ? "?" : String.format("%.1f", Math.sqrt(nearest))); }
        }

        boolean poll() {
            boolean all = true;
            for (Lane each : lanes) {
                if (!each.done) each.poll();
                all &= each.done;
            }
            return all;
        }

        String describe() {
            StringBuilder out = new StringBuilder();
            for (Lane each : lanes) out.append(each.name).append(each.done ? " done; " : " pending (" + each.status() + "); ");
            return out.toString();
        }

        void finish() {
            if (finished) return;
            finished = true;
            try {
                for (Entity entity : spawned) entity.discard();
                spawned.clear();
                for (Lane each : lanes) {
                    level.getEntitiesOfClass(EntityDart.class, each.region, entity -> true).forEach(Entity::discard);
                    level.getEntitiesOfClass(ItemEntity.class, each.region, item -> !existing.contains(item.getUUID())).forEach(Entity::discard);
                }
                if (touched) {
                    for (Lane each : lanes) each.restore();
                    original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                }
                for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            } finally {
                try {
                    if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                } finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }
    }
}
