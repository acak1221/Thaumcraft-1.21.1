package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.container.ContainerGolem;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * Order Guard target switches on real golems. A real Guard, real animals/monsters/creepers/players and a real menu are explicit INPUTS;
 * who is a target, who is attacked, the mask, the NBT and the placer data are never supplied. Part one is the decision itself
 * ({@code isGuardTarget}) for every one of the 16 masks against twelve kinds of real entity (and PvP off, creative, spectating players);
 * part two drives the real menu with the original button ids 51-54; part three is persistence (NBT, placer, bell, forged data);
 * part four runs four real Guards that WALK and STRIKE: the default Order Guard kills only the spider and leaves cow, creeper
 * and a real player alone even after the spider is dead; one switch at a time then makes the Guard fight exactly that category, and a
 * player-attacking Guard never touches its nearer owner. NOT a client/GUI check (the checkboxes are unseen), a protection-mod
 * check, multiplayer over a network or Butcher.
 */
public final class GolemGuardTargetsRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int TALLOW = 2, IRON = 6, DEADLINE = 1800, SETTLE = 160, PLAYER_HITS = 200;
    private static final int ORDER = GolemUpgrade.ORDER.id, FIRE = GolemUpgrade.FIRE.id;
    private static final int M = GolemGuardTargets.MONSTERS, A = GolemGuardTargets.ANIMALS, P = GolemGuardTargets.PLAYERS, C = GolemGuardTargets.CREEPERS;
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemGuardTargetsRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.start();
            check.installThroughTheNativeInteraction();
            check.theMenuSwitchesUseTheOriginalButtons();
            check.decisionMatrix();
            check.persistence();
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
                Thaumcraft.LOG.error("Guard lanes failed at tick {}: {} ({})", test.getTick(), failure.getMessage(), check.describe());
                check.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (check.finished) return;
            String state = check.describe();
            check.finish();
            test.fail("the Guard lanes did not all finish within " + DEADLINE + " ticks: " + state);
        });
    }

    /** One real connected player. */
    private static final class Actor {
        final ServerPlayer player;
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);

        Actor(ServerLevel level, String prefix) {
            var profile = new GameProfile(UUID.randomUUID(), prefix + "-" + UUID.randomUUID().toString().substring(0, 6));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        }
        String name() { return player.getGameProfile().getName(); }
    }

    /** Health watched every tick, because a hit may be healed before the end of a lane. */
    private static final class Tracked {
        final String name;
        final LivingEntity entity;
        float min;

        Tracked(String name, LivingEntity entity) { this.name = name; this.entity = entity; min = entity.getHealth(); }
        void sample() { min = Math.min(min, entity.isAlive() ? entity.getHealth() : 0F); }
        boolean hurt() { return min < entity.getMaxHealth(); }
    }

    private static final class Check {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final Actor owner, bystander, victim;
        private final BlockPos base;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<Entity> spawned = new ArrayList<>();
        private final List<Lane> lanes = new ArrayList<>();
        private final boolean pvp;
        private boolean touched, finished;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            owner = new Actor(level, "own");
            bystander = new Actor(level, "bys");
            victim = new Actor(level, "vic");
            pvp = level.getServer().isPvpAllowed();
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -2, -8), base.offset(8, 5, 8))) original.put(pos.immutable(), level.getBlockState(pos));
        }

        private void join(Actor actor) {
            level.getServer().getPlayerList().placeNewPlayer(actor.connection, actor.player,
                    CommonListenerCookie.createInitial(actor.player.getGameProfile(), false));
            test.assertTrue(actor.player.level() == level && level.getServer().getPlayerList().getPlayer(actor.player.getUUID()) == actor.player,
                    "an actual connected player is an explicit input");
            actor.player.setNoGravity(true);
            actor.player.setGameMode(GameType.SURVIVAL);
        }

        private void start() {
            join(owner);
            join(bystander);
            join(victim);
            // The attacked players must outlive the lane and must not heal a hit away: huge health, no natural regeneration.
            for (Actor actor : List.of(bystander, victim)) {
                actor.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
                actor.player.setHealth(1000);
                actor.player.getFoodData().setFoodLevel(6);
                actor.player.getFoodData().setSaturation(0);
            }
            owner.player.getFoodData().setFoodLevel(6);
            owner.player.getFoodData().setSaturation(0);
            touched = true;
            level.getServer().setPvpAllowed(true); // The matrix below states PvP explicitly; the original value is restored in finish().
            for (BlockPos pos : original.keySet()) {
                level.setBlock(pos, pos.getY() == base.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            }
            forceChunks(base, 3);
            test.assertTrue(level.mayInteract(owner.player, base), "fixture is not under unrelated spawn protection");
        }

        private void forceChunks(BlockPos around, int halo) {
            var center = new ChunkPos(around);
            for (int dx = -halo; dx <= halo; dx++) for (int dz = -halo; dz <= halo; dz++) {
                var chunk = new ChunkPos(center.x + dx, center.z + dz);
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk); }
            }
        }

        private GolemData config(List<Integer> upgrades, int mask) {
            return new GolemData(GolemData.GUARD, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, upgrades, List.of(), mask);
        }

        private EntityGolemBase guard(int material, List<Integer> upgrades, int mask, BlockPos at) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(material);
            golem.applyConfiguration(config(upgrades, mask));
            golem.setPos(Vec3.atBottomCenterOf(at));
            golem.setHome(at, Direction.UP);
            golem.setOwnerName(owner.name());
            golem.setPersistenceRequired();
            test.assertTrue(ChampionJoinRoll.join(level, golem) && golem.hasSupportedCore(), "a real Guard is admitted");
            spawned.add(golem);
            return golem;
        }

        private <T extends Mob> T mob(EntityType<T> type, BlockPos at) {
            T mob = type.create(level);
            mob.moveTo(Vec3.atBottomCenterOf(at));
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            // The 1.7 original rolled nextInt100 of the world random for every hostile mob that
            // joined the world (oracle EventHandlerEntity.entitySpawns:391) and a promoted mob
            // keeps thirty health more and a name of its own. The fixture declares that draw, so
            // no mob of the lanes is promoted by chance and every tracked mob starts at its own
            // health, exactly like the ordinary mob the lane measures.
            test.assertTrue(ChampionJoinRoll.join(level, mob), "a real " + type.getDescriptionId() + " is admitted");
            spawned.add(mob);
            return mob;
        }

        private void clearEntities() {
            for (Entity entity : spawned) entity.discard();
            spawned.clear();
            level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(10), item -> true).forEach(Entity::discard);
        }

        private void stand(Actor actor, GameType mode, ItemStack held, Vec3 at) {
            actor.player.setGameMode(mode);
            actor.player.setShiftKeyDown(false);
            actor.player.setItemInHand(InteractionHand.MAIN_HAND, held);
            actor.player.setPos(at);
        }

        // ---- part one: a real player installs Order -------------------------------------------------------------------

        private void installThroughTheNativeInteraction() {
            var golem = guard(TALLOW, List.of(), GolemGuardTargets.DEFAULT, base);
            test.assertTrue(!golem.canSwitchGuardTargets() && golem.getGuardTargets() == GolemGuardTargets.DEFAULT,
                    "a Guard without Order has no switches and the default mask");
            for (int category : new int[] {M, A, P, C}) {
                test.assertTrue(!golem.toggleGuardTarget(category) && golem.getGuardTargets() == GolemGuardTargets.DEFAULT,
                        "without Order the switch " + category + " cannot be flipped");
            }
            test.assertTrue(!golem.setGuardTargets(0) && !golem.setGuardTargets(GolemGuardTargets.ALL), "nor can the mask be written");
            stand(owner, GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.UPGRADE_ORDER.get()), Vec3.atBottomCenterOf(base).add(1.2, 0, 0));
            test.assertTrue(golem.interact(owner.player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && owner.player.getMainHandItem().isEmpty()
                            && golem.getUpgradeAmount(GolemUpgrade.ORDER) == 1, "a real player installs Order into the first free slot and pays the item");
            test.assertTrue(golem.canSwitchGuardTargets() && golem.getGuardTargets() == GolemGuardTargets.DEFAULT,
                    "the switches now exist, and installing Order does not change the original's default mask");
            clearEntities();
        }

        // ---- part two: the real menu with the original button ids ------------------------------------------------------

        private void theMenuSwitchesUseTheOriginalButtons() {
            var ordered = guard(TALLOW, List.of(ORDER), GolemGuardTargets.DEFAULT, base);
            stand(owner, GameType.SURVIVAL, ItemStack.EMPTY, Vec3.atBottomCenterOf(base).add(1.2, 0, 0));
            var menu = new ContainerGolem(901, owner.player.getInventory(), ordered);
            owner.player.containerMenu = menu;
            try {
                test.assertTrue(menu.canSwitchGuardTargets() && menu.guardTargets() == 0, "the menu offers four switches, none of them forbidding a category");
                test.assertTrue(ContainerGolem.GUARD_BUTTON == 51 && menu.clickMenuButton(owner.player, 52) && ordered.getGuardTargets() == A
                                && menu.guardTargets() == A, "button 52 is Animals: the golem and the synced menu data both forbid it");
                test.assertTrue(menu.clickMenuButton(owner.player, 53) && menu.clickMenuButton(owner.player, 54) && ordered.getGuardTargets() == (A | P | C)
                                && menu.guardTargets() == (A | P | C), "53 is Players and 54 is Creepers");
                test.assertTrue(menu.clickMenuButton(owner.player, 51) && ordered.getGuardTargets() == GolemGuardTargets.ALL, "51 is Monsters and may be switched off too");
                for (int click = 0; click < 2; click++) test.assertTrue(menu.clickMenuButton(owner.player, 52), "a switch flips back and forth");
                test.assertTrue(ordered.getGuardTargets() == GolemGuardTargets.ALL, "two clicks restore it");
                for (int button : new int[] {50, 55, 56, 57, 60, 61, 99, 100, 105, 0, -1}) {
                    test.assertTrue(!menu.clickMenuButton(owner.player, button) && ordered.getGuardTargets() == GolemGuardTargets.ALL,
                            "button " + button + " is not a Guard switch and changes nothing");
                }
            } finally { menu.removed(owner.player); owner.player.containerMenu = owner.player.inventoryMenu; }
            clearEntities();

            var plain = guard(TALLOW, List.of(), GolemGuardTargets.DEFAULT, base);
            var plainMenu = new ContainerGolem(902, owner.player.getInventory(), plain);
            owner.player.containerMenu = plainMenu;
            try {
                test.assertTrue(!plainMenu.canSwitchGuardTargets(), "a Guard menu without Order shows no switches");
                for (int button = 51; button <= 54; button++) {
                    test.assertTrue(!plainMenu.clickMenuButton(owner.player, button) && plain.getGuardTargets() == GolemGuardTargets.DEFAULT,
                            "a forged click on " + button + " is refused by the server when there is no Order");
                }
            } finally { plainMenu.removed(owner.player); owner.player.containerMenu = owner.player.inventoryMenu; }
            clearEntities();

            // The same ids belong to other cores' menus: a Fill golem with Order has no Guard switches.
            var fill = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            fill.setGolemMaterial(TALLOW);
            fill.applyConfiguration(new GolemData(GolemData.FILL, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, List.of(ORDER)));
            fill.setPos(Vec3.atBottomCenterOf(base));
            fill.setHome(base, Direction.UP);
            test.assertTrue(ChampionJoinRoll.join(level, fill), "a Fill golem with Order is admitted");
            spawned.add(fill);
            var fillMenu = new ContainerGolem(903, owner.player.getInventory(), fill);
            owner.player.containerMenu = fillMenu;
            try {
                test.assertTrue(!fillMenu.canSwitchGuardTargets() && !fill.canSwitchGuardTargets(), "Order on a Fill golem gives no Guard switches");
                for (int button = 51; button <= 54; button++) test.assertTrue(!fillMenu.clickMenuButton(owner.player, button), "button " + button + " on a Fill menu");
                test.assertTrue(fill.getGuardTargets() == GolemGuardTargets.DEFAULT && !fill.toggleGuardTarget(A) && !fill.setGuardTargets(A), "and nothing is written");
            } finally { fillMenu.removed(owner.player); owner.player.containerMenu = owner.player.inventoryMenu; }
            clearEntities();
        }

        // ---- part three: the decision, for every mask and twelve kinds of real entity ---------------------------------

        private record Target(String name, LivingEntity entity, int category) {}

        private void decisionMatrix() {
            var withOrder = guard(IRON, List.of(ORDER), GolemGuardTargets.DEFAULT, base);
            var withoutOrder = guard(IRON, List.of(), GolemGuardTargets.DEFAULT, base.offset(0, 0, -6));
            var targets = new ArrayList<Target>();
            targets.add(new Target("spider", mob(EntityType.SPIDER, base.offset(3, 0, 0)), M));
            targets.add(new Target("zombie", mob(EntityType.ZOMBIE, base.offset(3, 0, 1)), M));
            targets.add(new Target("creeper", mob(EntityType.CREEPER, base.offset(3, 0, 2)), C));
            targets.add(new Target("cow", mob(EntityType.COW, base.offset(-3, 0, 0)), A));
            targets.add(new Target("squid", mob(EntityType.SQUID, base.offset(-3, 0, 1)), A));
            targets.add(new Target("wild wolf", mob(EntityType.WOLF, base.offset(-3, 0, 2)), A));
            var tame = mob(EntityType.WOLF, base.offset(-3, 0, -2));
            tame.tame(owner.player);
            targets.add(new Target("tamed wolf", tame, 0));
            targets.add(new Target("villager", mob(EntityType.VILLAGER, base.offset(0, 0, 3)), 0));
            targets.add(new Target("iron golem", mob(EntityType.IRON_GOLEM, base.offset(1, 0, 3)), 0));
            targets.add(new Target("bat", mob(EntityType.BAT, base.offset(2, 0, 3)), 0));
            // 20 blocks from the home, in a loaded chunk: a category switch never widens the range.
            var farCow = mob(EntityType.COW, base.offset(20, 0, 0));
            test.assertTrue(level.hasChunkAt(farCow.blockPosition()), "the far cow stands in a loaded chunk");
            targets.add(new Target("far cow", farCow, 0));
            stand(bystander, GameType.SURVIVAL, ItemStack.EMPTY, Vec3.atBottomCenterOf(base.offset(0, 0, -3)));
            stand(owner, GameType.SURVIVAL, ItemStack.EMPTY, Vec3.atBottomCenterOf(base.offset(-1, 0, -3)));
            targets.add(new Target("other player", bystander.player, P));
            targets.add(new Target("owner", owner.player, 0));

            for (int mask = 0; mask <= GolemGuardTargets.ALL; mask++) {
                test.assertTrue(withOrder.setGuardTargets(mask) && withOrder.getGuardTargets() == mask, "mask " + mask + " is accepted by an Order Guard");
                for (Target target : targets) {
                    boolean expected = target.category() != 0 && GolemGuardTargets.allows(mask, target.category());
                    test.assertTrue(withOrder.isGuardTarget(target.entity()) == expected,
                            "Order Guard, mask " + mask + ", " + target.name() + ": expected " + expected);
                }
            }
            // Without Order: hostile monsters (not creepers) only, whatever else is nearby.
            for (Target target : targets) {
                test.assertTrue(withoutOrder.isGuardTarget(target.entity()) == (target.category() == M),
                        "Guard without Order, " + target.name() + ": monsters only");
            }

            // Monsters and players left, then the player rules: never creative/spectating, never when the server forbids PvP.
            withOrder.setGuardTargets(A | C);
            test.assertTrue(withOrder.isGuardTarget(bystander.player), "a survival non-owner is a target with Players on");
            bystander.player.setGameMode(GameType.CREATIVE);
            test.assertTrue(!withOrder.isGuardTarget(bystander.player), "a creative player cannot be hurt, so is never chased");
            bystander.player.setGameMode(GameType.SPECTATOR);
            test.assertTrue(!withOrder.isGuardTarget(bystander.player), "nor a spectator");
            bystander.player.setGameMode(GameType.SURVIVAL);
            test.assertTrue(withOrder.isGuardTarget(bystander.player), "and a survival player is a target again");
            level.getServer().setPvpAllowed(false);
            try {
                test.assertTrue(!withOrder.isGuardTarget(bystander.player), "with PvP forbidden by the server the Guard never targets a player");
                test.assertTrue(withOrder.isGuardTarget(targets.getFirst().entity()), "while the hostile rule is unaffected");
            } finally { level.getServer().setPvpAllowed(true); } // This test's own baseline; finish() restores the server's original value.
            test.assertTrue(withOrder.isGuardTarget(bystander.player), "PvP allowed again: the player is a target again");
            // A player with the golem's own name is the owner, whoever stands there.
            withOrder.setOwnerName(bystander.name());
            test.assertTrue(!withOrder.isGuardTarget(bystander.player) && withOrder.isGuardTarget(owner.player),
                    "the owner is decided by name: the former owner is a stranger now and the new owner is spared");
            clearEntities();
        }

        // ---- part four: persistence -------------------------------------------------------------------------------------

        private CompoundTag save(EntityGolemBase golem) {
            var tag = new CompoundTag();
            golem.saveWithoutId(tag);
            return tag;
        }

        private EntityGolemBase load(CompoundTag tag) {
            var golem = ThaumcraftGolems.GOLEM.get().create(level);
            golem.load(tag.copy());
            return golem;
        }

        private boolean unsupported(EntityGolemBase golem) {
            try { golem.configuredData(); return false; } catch (IllegalStateException expected) { return true; }
        }

        private void persistence() {
            var golem = guard(IRON, List.of(ORDER), A | C, base);
            test.assertTrue(golem.getGuardTargets() == (A | C) && golem.configuredData().guardTargets() == (A | C), "the configuration reaches the golem and comes back");
            var tag = save(golem);
            test.assertTrue(tag.contains("GuardTargets", Tag.TAG_BYTE) && tag.getByte("GuardTargets") == (A | C) && !tag.contains("toggles"),
                    "saved under its own key as the original's negative byte; the original's shared `toggles` byte is not reused");
            var reloaded = load(tag);
            test.assertTrue(reloaded.getGuardTargets() == (A | C) && !unsupported(reloaded) && reloaded.configuredData().guardTargets() == (A | C)
                            && reloaded.canSwitchGuardTargets(), "reloading restores the mask");
            reloaded.discard();

            var plain = guard(IRON, List.of(ORDER), GolemGuardTargets.DEFAULT, base.offset(0, 0, 4));
            var plainTag = save(plain);
            test.assertTrue(!plainTag.contains("GuardTargets") && !plainTag.contains("toggles"), "the default is not written: older saves keep their exact form");
            var plainReloaded = load(plainTag);
            test.assertTrue(plainReloaded.getGuardTargets() == GolemGuardTargets.DEFAULT && !unsupported(plainReloaded), "and reads back as the default");
            plainReloaded.discard();
            var none = guard(IRON, List.of(ORDER), M, base.offset(0, 0, 8));
            var noneReloaded = load(save(none));
            test.assertTrue(noneReloaded.getGuardTargets() == M && !unsupported(noneReloaded), "even a switched-off category (Monsters off) survives");
            noneReloaded.discard();

            // Data this port cannot honour is kept and flagged, never flattened into a plain Guard.
            var withoutOrder = guard(IRON, List.of(), GolemGuardTargets.DEFAULT, base.offset(4, 0, 0));
            var forged = save(withoutOrder);
            forged.putByte("GuardTargets", (byte) A);
            var refused = load(forged);
            test.assertTrue(unsupported(refused) && save(refused).getByte("GuardTargets") == A && save(refused).contains("GuardTargets", Tag.TAG_BYTE),
                    "a mask without Order makes the golem unsupported, and the saved byte is kept as it was");
            refused.discard();
            for (byte bad : new byte[] {16, 31, -1, -128}) {
                var outOfRange = save(golem);
                outOfRange.putByte("GuardTargets", bad);
                var loaded = load(outOfRange);
                test.assertTrue(unsupported(loaded) && save(loaded).getByte("GuardTargets") == bad, "mask byte " + bad + " is refused and kept");
                loaded.discard();
            }
            var wrongType = save(golem);
            wrongType.putInt("GuardTargets", A);
            var wrongTypeGolem = load(wrongType);
            test.assertTrue(unsupported(wrongTypeGolem) && save(wrongTypeGolem).getInt("GuardTargets") == A, "a non-byte tag is refused and kept");
            wrongTypeGolem.discard();
            var harvest = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            harvest.setGolemMaterial(IRON);
            harvest.applyConfiguration(new GolemData(GolemData.HARVEST, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, List.of(ORDER)));
            var harvestTag = save(harvest);
            harvestTag.putByte("GuardTargets", (byte) A);
            var harvestForged = load(harvestTag);
            test.assertTrue(unsupported(harvestForged), "a Harvest golem with Order has no target switches either");
            harvestForged.discard();
            var original = save(plain);
            original.putByte("toggles", (byte) 0b11100);
            var originalToggles = load(original);
            test.assertTrue(unsupported(originalToggles) && save(originalToggles).getByte("toggles") == 0b11100,
                    "an original `toggles` byte is still not reinterpreted: unsupported, kept");
            originalToggles.discard();

            // Placer -> golem -> bell -> placer, with the mask.
            var data = golem.configuredData();
            test.assertTrue(data.guardTargets() == (A | C) && data.upgrades().equals(List.of(ORDER)), "the placer data carries the mask and Order");
            var placer = new ItemStack(ThaumcraftGolems.GOLEM_IRON.get());
            placer.set(ThaumcraftGolems.GOLEM_DATA.get(), data);
            clearEntities();
            stand(owner, GameType.SURVIVAL, placer, Vec3.atBottomCenterOf(base).add(1.2, 0, 0));
            var floor = base.below();
            var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false);
            test.assertTrue(placer.getItem().useOn(new UseOnContext(owner.player, InteractionHand.MAIN_HAND, hit)) == InteractionResult.CONSUME
                            && owner.player.getMainHandItem().isEmpty(), "the Iron placer places the Guard and is paid");
            var placed = level.getEntitiesOfClass(EntityGolemBase.class, new AABB(base).inflate(4), g -> g.isAlive() && g.getGolemCore() == GolemData.GUARD);
            test.assertTrue(placed.size() == 1, "exactly one Guard was placed: " + placed.size());
            var placedGolem = placed.getFirst();
            spawned.add(placedGolem);
            test.assertTrue(placedGolem.getGuardTargets() == (A | C) && placedGolem.canSwitchGuardTargets() && placedGolem.getOwnerName().equals(owner.name()),
                    "the placed Guard keeps the mask and is owned by the placing player");
            stand(owner, GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.GOLEM_BELL.get()), Vec3.atBottomCenterOf(base).add(1.2, 0, 0));
            test.assertTrue(placedGolem.recall(owner.player, false), "the bell recalls the Guard");
            DeferredItemDrops.tick(level);
            var returned = level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(6), item -> item.getItem().is(ThaumcraftGolems.GOLEM_IRON.get()));
            test.assertTrue(returned.size() == 1 && returned.getFirst().getItem().get(ThaumcraftGolems.GOLEM_DATA.get()).guardTargets() == (A | C),
                    "the recalled placer still carries the mask: " + returned);
            clearEntities();
        }

        // ---- part five: four real Guards walk and strike ----------------------------------------------------------------

        private void armLanes() {
            int z = 40;
            for (BlockPos pos : List.of(base.offset(0, 0, z), base.offset(0, 0, 2 * z), base.offset(0, 0, 3 * z), base.offset(0, 0, 4 * z))) forceChunks(pos, 3);
            lanes.add(new Lane("MONSTERS", base.offset(0, 0, z), A | P | C, Kind.SPIDER));
            lanes.add(new Lane("ANIMALS", base.offset(0, 0, 2 * z), M | P | C, Kind.COW));
            lanes.add(new Lane("CREEPERS", base.offset(0, 0, 3 * z), M | A | P, Kind.CREEPER));
            lanes.add(new Lane("PLAYERS", base.offset(0, 0, 4 * z), M | A | C, Kind.PLAYER));
            for (Lane lane : lanes) lane.setup();
        }

        private enum Kind { SPIDER, COW, CREEPER, PLAYER }

        /**
         * One Guard with a spider (east), a cow (west), a creeper (north) and a player (south), all within four blocks. The Guard's
         * mask switches off the three categories it may not attack, so exactly one of them is its target: that one must be hurt (and,
         * if mortal, killed so the Guard looks around again), and every other one must never lose a point of health at any tick. Every
         * lane keeps a real, non-owner player and a real creeper and cow at its side the whole time; the original's default forbids
         * nothing, so a lane only ever switches a category off explicitly.
         */
        private final class Lane {
            final String name;
            final BlockPos b;
            final int mask;
            final Kind kind;
            EntityGolemBase golem;
            Tracked positive;
            final List<Tracked> negatives = new ArrayList<>();
            private final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
            long hurtAt = -1, goneAt = -1;
            boolean done;

            Lane(String name, BlockPos b, int mask, Kind kind) {
                this.name = name; this.b = b; this.mask = mask; this.kind = kind;
                for (BlockPos pos : BlockPos.betweenClosed(b.offset(-8, -2, -8), b.offset(8, 5, 8))) saved.put(pos.immutable(), level.getBlockState(pos));
            }

            void setup() {
                for (BlockPos pos : saved.keySet()) {
                    level.setBlock(pos, pos.getY() == b.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
                }
                var spider = mob(EntityType.SPIDER, b.offset(3, 0, 0));
                var cow = mob(EntityType.COW, b.offset(-3, 0, 0));
                var creeper = mob(EntityType.CREEPER, b.offset(0, 0, -3));
                Tracked tSpider = new Tracked("spider", spider), tCow = new Tracked("cow", cow), tCreeper = new Tracked("creeper", creeper);
                if (kind == Kind.PLAYER) {
                    // The owner stands NEARER than the victim: a Guard that skipped the owner by chance would hit it first.
                    stand(owner, GameType.SURVIVAL, ItemStack.EMPTY, Vec3.atBottomCenterOf(b.offset(0, 0, 2)));
                    stand(victim, GameType.SURVIVAL, ItemStack.EMPTY, Vec3.atBottomCenterOf(b.offset(0, 0, 4)));
                    positive = new Tracked("victim", victim.player);
                    negatives.addAll(List.of(new Tracked("owner", owner.player), tSpider, tCow, tCreeper));
                } else {
                    stand(bystander, GameType.SURVIVAL, ItemStack.EMPTY, Vec3.atBottomCenterOf(b.offset(0, 0, 3)));
                    Tracked tPlayer = new Tracked("bystander", bystander.player);
                    positive = switch (kind) { case SPIDER -> tSpider; case COW -> tCow; case CREEPER -> tCreeper; case PLAYER -> tPlayer; };
                    for (Tracked other : List.of(tSpider, tCow, tCreeper, tPlayer)) if (other != positive) negatives.add(other);
                }
                golem = guard(IRON, List.of(ORDER), mask, b);
            }

            String status() {
                StringBuilder out = new StringBuilder(positive.name + " min " + positive.min + (positive.entity.isAlive() ? "" : " dead"));
                for (Tracked negative : negatives) out.append(", ").append(negative.name).append(" min ").append(negative.min);
                return out.append(", golem ").append(golem.isAlive() ? "alive at " + golem.blockPosition().toShortString() : "dead").toString();
            }

            void poll() {
                positive.sample();
                for (Tracked negative : negatives) {
                    negative.sample();
                    test.assertTrue(!negative.hurt(), name + ": the " + negative.name + " is not a target of this Guard yet lost health (min " + negative.min + ")");
                }
                test.assertTrue(golem.isAlive(), name + ": the Guard survived");
                if (hurtAt < 0) {
                    if (positive.hurt()) hurtAt = test.getTick();
                    return;
                }
                if (kind == Kind.PLAYER) {
                    if (test.getTick() >= hurtAt + PLAYER_HITS) { done = true; report(); }
                    return;
                }
                if (goneAt < 0) {
                    if (!positive.entity.isAlive()) goneAt = test.getTick();
                    return;
                }
                if (test.getTick() >= goneAt + SETTLE) { done = true; report(); }
            }
            void report() { Thaumcraft.LOG.info("Guard lane {}: first hit at tick {}, target gone at tick {}, done at tick {} ({})", name, hurtAt, goneAt, test.getTick(), status()); }

            void restore() { saved.forEach((pos, state) -> level.setBlock(pos, state, QUIET)); }
        }

        boolean poll() {
            boolean all = true;
            for (Lane lane : lanes) {
                if (!lane.done) lane.poll();
                all &= lane.done;
            }
            return all;
        }

        String describe() {
            StringBuilder out = new StringBuilder();
            for (Lane lane : lanes) out.append(lane.name).append(lane.done ? " done; " : " pending (" + lane.status() + "); ");
            return out.toString();
        }

        void finish() {
            if (finished) return;
            finished = true;
            try {
                level.getServer().setPvpAllowed(pvp);
                clearEntities();
                if (touched) {
                    for (Lane lane : lanes) lane.restore();
                    original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                }
                for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            } finally {
                for (Actor actor : List.of(owner, bystander, victim)) {
                    try {
                        if (level.getServer().getPlayerList().getPlayer(actor.player.getUUID()) == actor.player) level.getServer().getPlayerList().remove(actor.player);
                    } finally { actor.player.discard(); actor.channel.finishAndReleaseAll(); }
                }
            }
        }
    }
}
