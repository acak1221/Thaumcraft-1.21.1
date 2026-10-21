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
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.LumberGeometry;
import thaumcraft.common.items.golems.GolemAspectCargo;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.items.golems.GolemWaterCargo;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * Golem upgrades on real golems and a real connected player: the recipes and items, installation through
 * the native interaction, persistence (entity NBT, placer data, placement, bell recall and dismantling) and
 * the ported effects (Earth strength/damage and carrying capacity, Air speed, Water range, Entropy thorns).
 * Golems, pigs, spiders and the player are explicit INPUTS; installation, slot choice, payment, saving, loading,
 * thorns, the capacity limits, the Guard range decision and the returned items are never supplied. NOT a Guard
 * fight, a work-speed measurement, a real water-draining or essentia-moving run at the raised capacity, a
 * multiplayer, network or visual check. The work radius of real Lumber and Harvest workers is
 * {@link GolemWaterRangeRuntimeChecks}; Fire ignition by a real Guard and Order replanting by real Harvest workers
 * are {@link GolemFireOrderRuntimeChecks}.
 */
public final class GolemUpgradeRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int STRAW = 0, WOOD = 1, TALLOW = 2, IRON = 6, THAUMIUM = 7;
    private static final int AIR = GolemUpgrade.AIR.id, EARTH = GolemUpgrade.EARTH.id, WATER = GolemUpgrade.WATER.id,
            ENTROPY = GolemUpgrade.ENTROPY.id, EMPTY = GolemUpgrade.EMPTY;
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemUpgradeRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        try (var check = new Check(test)) {
            check.start();
            check.recipesAndItems();
            check.installationAndPayment();
            check.effectsOfEarthAndAir();
            check.entropyThorns();
            check.entityPersistence();
            check.placementRecallAndDismantling();
            check.earthCapacity();
            check.earthCargoCustody();
            check.waterRange();
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
        private final AABB lane;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final Set<UUID> existing = new HashSet<>();
        private final List<Entity> spawned = new ArrayList<>(), attackers = new ArrayList<>();
        private boolean touched;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            lane = new AABB(Vec3.atLowerCornerOf(base.offset(-4, -2, -4)), Vec3.atLowerCornerOf(base.offset(6, 6, 5)));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-4, -2, -4), base.offset(5, 5, 4))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
            level.getAllEntities().forEach(entity -> existing.add(entity.getUUID()));
            var profile = new GameProfile(UUID.randomUUID(), "upg-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
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

        private GolemData config(int core, List<Integer> upgrades) {
            return new GolemData(core, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, upgrades);
        }

        private EntityGolemBase golem(int material, int core, List<Integer> upgrades) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(material);
            golem.applyConfiguration(config(core, upgrades));
            golem.setPos(Vec3.atBottomCenterOf(base));
            test.assertTrue(level.addFreshEntity(golem), "the golem is admitted");
            spawned.add(golem);
            return golem;
        }

        /** The player stands at the golem's side in the given mode with one stack in the main hand. */
        private void hold(GameType mode, ItemStack stack) {
            player.setGameMode(mode);
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setPos(Vec3.atBottomCenterOf(base).add(1.2, 0, 0));
        }

        private InteractionResult interact(EntityGolemBase golem) { return golem.interact(player, InteractionHand.MAIN_HAND); }

        private Item upgrade(GolemUpgrade kind) { return ThaumcraftGolems.upgradeItem(kind); }

        private void clearGolems() {
            for (Entity entity : spawned) entity.discard();
            spawned.clear();
            level.getEntitiesOfClass(ItemEntity.class, lane, item -> !existing.contains(item.getUUID())).forEach(Entity::discard);
        }

        private List<Integer> slots(EntityGolemBase golem) {
            List<Integer> result = new ArrayList<>();
            for (int slot = 0; slot < golem.getUpgradeSlotCount(); slot++) result.add(golem.getUpgrade(slot));
            return result;
        }

        private void recipesAndItems() {
            record Row(String kind, GolemUpgrade type, String research, String shard, String vis) {}
            for (Row row : List.of(new Row("air", GolemUpgrade.AIR, "UPGRADEAIR", "air_shard", "aer"),
                    new Row("earth", GolemUpgrade.EARTH, "UPGRADEEARTH", "earth_shard", "terra"),
                    new Row("fire", GolemUpgrade.FIRE, "UPGRADEFIRE", "fire_shard", "ignis"),
                    new Row("water", GolemUpgrade.WATER, "UPGRADEWATER", "water_shard", "aqua"),
                    new Row("order", GolemUpgrade.ORDER, "UPGRADEORDER", "order_shard", "ordo"),
                    new Row("entropy", GolemUpgrade.ENTROPY, "UPGRADEENTROPY", "entropy_shard", "perditio"))) {
                var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/golem_upgrade_" + row.kind()));
                test.assertTrue(holder.isPresent() && holder.get().value() instanceof ArcaneShapedRecipe, row.kind() + " arcane recipe loaded");
                var recipe = (ArcaneShapedRecipe) holder.get().value();
                test.assertTrue(recipe.research().equals(row.research()) && recipe.pattern().equals(List.of("NNN", "NCN", "NNN"))
                                && recipe.key().keySet().equals(Set.of("N", "C"))
                                && recipe.key().get("N").test(new ItemStack(Items.GOLD_NUGGET))
                                && recipe.key().get("C").test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", row.shard()))))
                                && !recipe.key().get("N").test(new ItemStack(Items.GOLD_INGOT))
                                && recipe.vis().equals(Map.of(row.vis(), 10))
                                && recipe.result().is(upgrade(row.type())) && recipe.result().getCount() == 1,
                        row.kind() + ": research " + row.research() + ", eight gold nuggets around its own shard, vis " + row.vis() + " 10");
                var stack = new ItemStack(upgrade(row.type()));
                test.assertTrue(stack.getMaxStackSize() == 64 && stack.getRarity() == Rarity.UNCOMMON,
                        row.kind() + " item: stack 64, uncommon");
            }
            // All six original types now have an item and a recipe; the icon-only "empty" slot is not an item.
            for (String absent : List.of("empty", "none")) {
                test.assertTrue(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "golem_upgrade_" + absent))
                                && level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/golem_upgrade_" + absent)).isEmpty(),
                        absent + " is not an upgrade type: no item or recipe exists");
            }
        }

        private void installationAndPayment() {
            // Tallow holds two upgrades. Survival: one item is paid per installation, in the first free slot.
            var tallow = golem(TALLOW, GolemData.NO_CORE, List.of());
            test.assertTrue(tallow.getUpgradeSlotCount() == 2 && slots(tallow).equals(List.of(EMPTY, EMPTY)), "a fresh Tallow golem has two empty slots");
            hold(GameType.SURVIVAL, new ItemStack(upgrade(GolemUpgrade.EARTH), 2));
            test.assertTrue(interact(tallow) == InteractionResult.CONSUME && slots(tallow).equals(List.of(EARTH, EMPTY))
                            && player.getMainHandItem().getCount() == 1,
                    "right-click installs into the FIRST free slot and pays exactly one item");
            test.assertTrue(interact(tallow) == InteractionResult.CONSUME && slots(tallow).equals(List.of(EARTH, EARTH))
                            && player.getMainHandItem().isEmpty(),
                    "a second Earth still fits (the limit is two of one kind) and uses up the stack");
            hold(GameType.SURVIVAL, new ItemStack(upgrade(GolemUpgrade.AIR), 3));
            test.assertTrue(interact(tallow) == InteractionResult.PASS && slots(tallow).equals(List.of(EARTH, EARTH))
                            && player.getMainHandItem().getCount() == 3,
                    "no free slot: refused, nothing paid and no menu opened");

            // One-slot Wood: sneaking never installs; a creative player installs without paying.
            var wood = golem(WOOD, GolemData.NO_CORE, List.of());
            test.assertTrue(wood.getUpgradeSlotCount() == 1, "Wood has one slot");
            hold(GameType.SURVIVAL, new ItemStack(upgrade(GolemUpgrade.ENTROPY), 2));
            player.setShiftKeyDown(true);
            test.assertTrue(interact(wood) == InteractionResult.PASS && slots(wood).equals(List.of(EMPTY)) && player.getMainHandItem().getCount() == 2,
                    "a sneaking click never installs");
            hold(GameType.CREATIVE, new ItemStack(upgrade(GolemUpgrade.ENTROPY), 2));
            test.assertTrue(interact(wood) == InteractionResult.CONSUME && slots(wood).equals(List.of(ENTROPY)) && player.getMainHandItem().getCount() == 2,
                    "creative installs without paying (same rule as core installation)");
            test.assertTrue(interact(wood) == InteractionResult.PASS && slots(wood).equals(List.of(ENTROPY)),
                    "a full one-slot golem refuses a second upgrade");

            // An upgrade is fitted to a working golem and leaves its core alone.
            var lumber = golem(IRON, GolemData.LUMBER, List.of());
            hold(GameType.SURVIVAL, new ItemStack(upgrade(GolemUpgrade.AIR)));
            test.assertTrue(interact(lumber) == InteractionResult.CONSUME && slots(lumber).equals(List.of(AIR)) && lumber.getGolemCore() == GolemData.LUMBER
                            && lumber.hasSupportedCore(),
                    "a Lumber golem takes the upgrade and keeps its core");
            clearGolems();
        }

        private void effectsOfEarthAndAir() {
            var tallow = golem(TALLOW, GolemData.NO_CORE, List.of());
            double health = tallow.getAttributeValue(Attributes.MAX_HEALTH), armor = tallow.getAttributeValue(Attributes.ARMOR);
            test.assertTrue(tallow.getGolemStrength() == 2 && tallow.getMeleeDamage() == 4F
                            && tallow.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4D
                            && (float) tallow.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.33F
                            && tallow.getCarryLimit() == 8 && tallow.getFluidCarryLimit() == 2000,
                    "baseline Tallow: strength 2, damage 4, speed 0.33, carries 8 items / 2000 mB");
            hold(GameType.SURVIVAL, new ItemStack(upgrade(GolemUpgrade.EARTH), 2));
            interact(tallow);
            test.assertTrue(tallow.getGolemStrength() == 3 && tallow.getUpgradeAmount(GolemUpgrade.EARTH) == 1
                            && tallow.getMeleeDamage() == 6F && tallow.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6D
                            && tallow.getCarryLimit() == 16 && tallow.getFluidCarryLimit() == 4000,
                    "one Earth: strength +1, melee damage 2 + 3 + 1 (Earth counted twice, as in the original), capacity 8 + 8");
            interact(tallow);
            test.assertTrue(tallow.getGolemStrength() == 4 && tallow.getMeleeDamage() == 8F && tallow.getAttributeValue(Attributes.ATTACK_DAMAGE) == 8D
                            && LumberGeometry.delay(tallow.getGolemStrength(), 2.0F) == 16
                            && tallow.getCarryLimit() == 24 && tallow.getFluidCarryLimit() == 4000,
                    "two Earth: strength 4, damage 8, capacity 24 (water floor(sqrt(24)) = 4 buckets), and a hardness-2 log takes (20-12)*2 = 16 ticks instead of 28");
            test.assertTrue(tallow.getAttributeValue(Attributes.MAX_HEALTH) == health && tallow.getAttributeValue(Attributes.ARMOR) == armor
                            && (float) tallow.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.33F,
                    "health, armor and speed are untouched by Earth");
            clearGolems();

            var iron = golem(IRON, GolemData.NO_CORE, List.of(EARTH));
            test.assertTrue(iron.getGolemStrength() == 5 && iron.getMeleeDamage() == 2 + 5 + 1
                            && LumberGeometry.delay(iron.getGolemStrength(), 2.0F) == 10,
                    "Iron (strength 4) with Earth works a hardness-2 log in (20-15)*2 = 10 ticks, not 16");
            clearGolems();

            var straw = golem(STRAW, GolemData.NO_CORE, List.of());
            hold(GameType.SURVIVAL, new ItemStack(upgrade(GolemUpgrade.AIR)));
            interact(straw);
            float expected = 0.38F * (1.0F + 1 * 0.15F);
            test.assertTrue((float) straw.getAttributeValue(Attributes.MOVEMENT_SPEED) == expected && expected > 0.43F && expected < 0.44F
                            && straw.getGolemStrength() == 0 && straw.getMeleeDamage() == 2F,
                    "one Air multiplies the Straw speed 0.38 by 1.15 and nothing else: " + straw.getAttributeValue(Attributes.MOVEMENT_SPEED));
            clearGolems();
        }

        /** One hit on the golem with fresh health and no invulnerability; returns the health the pig lost. */
        private float hit(EntityGolemBase golem, Pig pig, DamageSource source) {
            pig.setHealth(pig.getMaxHealth());
            pig.invulnerableTime = 0;
            golem.setHealth(golem.getMaxHealth());
            golem.invulnerableTime = 0;
            float before = pig.getHealth();
            golem.hurt(source, 1F);
            return before - pig.getHealth();
        }

        private void entropyThorns() {
            Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 3, 1));
            attackers.add(pig);
            test.assertTrue(pig.isAlive() && pig.getMaxHealth() == 10F, "the attacker is a real pig with no armor");
            var plain = golem(WOOD, GolemData.NO_CORE, List.of());
            test.assertTrue(hit(plain, pig, level.damageSources().mobAttack(pig)) == 0F, "no Entropy: nothing is reflected");
            clearGolems();

            var one = golem(WOOD, GolemData.NO_CORE, List.of(ENTROPY));
            Set<Integer> seen = new HashSet<>();
            for (int repeat = 0; repeat < 40; repeat++) {
                float lost = hit(one, pig, level.damageSources().mobAttack(pig));
                test.assertTrue(lost == 2F || lost == 3F, "one Entropy reflects 2 + nextInt(2) = 2 or 3, saw " + lost);
                seen.add((int) lost);
            }
            test.assertTrue(seen.equals(Set.of(2, 3)), "the random part is actually drawn: " + seen);
            clearGolems();

            var two = golem(TALLOW, GolemData.NO_CORE, List.of(ENTROPY, ENTROPY));
            seen.clear();
            for (int repeat = 0; repeat < 60; repeat++) {
                float lost = hit(two, pig, level.damageSources().mobAttack(pig));
                test.assertTrue(lost >= 4F && lost <= 7F, "two Entropy reflect 4 + nextInt(4) = 4..7, saw " + lost);
                seen.add((int) lost);
            }
            test.assertTrue(seen.size() >= 3, "and the 4..7 spread is drawn: " + seen);
            float noSource = hit(two, pig, level.damageSources().generic());
            test.assertTrue(noSource == 0F, "damage without an attacker reflects nothing");
            float selfHurt = hit(two, pig, level.damageSources().mobAttack(two));
            test.assertTrue(selfHurt == 0F, "damage the golem deals itself is never reflected");
            clearGolems();

            // The original recurses without bound between two Entropy golems; here each reflected hit stays single.
            var left = golem(IRON, GolemData.NO_CORE, List.of(ENTROPY));
            var right = golem(IRON, GolemData.NO_CORE, List.of(ENTROPY));
            right.setPos(Vec3.atBottomCenterOf(base).add(2, 0, 0));
            float leftHealth = left.getHealth(), rightHealth = right.getHealth();
            left.invulnerableTime = 0; right.invulnerableTime = 0;
            left.hurt(level.damageSources().mobAttack(right), 1F);
            test.assertTrue(left.getHealth() < leftHealth && right.getHealth() < rightHealth,
                    "two Entropy golems hurt each other once each and the call returns (no unbounded recursion)");
            clearGolems();
        }

        private CompoundTag save(EntityGolemBase golem) {
            var tag = new CompoundTag();
            golem.saveWithoutId(tag);
            return tag;
        }

        private EntityGolemBase load(CompoundTag tag) {
            var copy = ThaumcraftGolems.GOLEM.get().create(level);
            copy.load(tag.copy());
            return copy;
        }

        private void entityPersistence() {
            var original = golem(TALLOW, GolemData.NO_CORE, List.of(EARTH, AIR));
            var tag = save(original);
            test.assertTrue(tag.contains("upgrades", Tag.TAG_BYTE_ARRAY) && java.util.Arrays.equals(tag.getByteArray("upgrades"), new byte[]{1, 0}),
                    "saved under the original key as the original byte array, Earth = 1, Air = 0");
            var loaded = load(tag);
            test.assertTrue(slots(loaded).equals(List.of(EARTH, AIR)) && loaded.configuredData().upgrades().equals(List.of(EARTH, AIR))
                            && loaded.getGolemStrength() == 3 && loaded.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6D
                            && (float) loaded.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.33F * (1.0F + 0.15F),
                    "a reloaded golem keeps both upgrades and re-derives the strength, damage and speed");
            test.assertTrue(java.util.Arrays.equals(save(loaded).getByteArray("upgrades"), new byte[]{1, 0}), "and saves them again unchanged");
            loaded.discard();

            // A short array is padded like the original; a missing tag is an empty golem.
            var shortTag = tag.copy();
            shortTag.putByteArray("upgrades", new byte[]{1});
            var padded = load(shortTag);
            test.assertTrue(slots(padded).equals(List.of(EARTH, EMPTY)) && java.util.Arrays.equals(save(padded).getByteArray("upgrades"), new byte[]{1, -1}),
                    "a one-entry array fills the first slot and pads the second with -1");
            padded.discard();
            var absent = tag.copy();
            absent.remove("upgrades");
            var none = load(absent);
            test.assertTrue(slots(none).equals(List.of(EMPTY, EMPTY)) && none.getGolemStrength() == 2, "no tag means no upgrades");
            none.discard();

            // Never truncated, never converted: the raw array is retained and the golem stays inert.
            for (byte[] raw : List.of(new byte[]{1, 1, 0}, new byte[]{6, -1}, new byte[]{1, 7})) {
                var future = tag.copy();
                future.putByteArray("upgrades", raw);
                var inert = load(future);
                boolean unsupported;
                try { inert.configuredData(); unsupported = false; } catch (IllegalStateException refused) { unsupported = true; }
                test.assertTrue(unsupported && java.util.Arrays.equals(save(inert).getByteArray("upgrades"), raw),
                        "an array the port cannot honour is retained verbatim: " + java.util.Arrays.toString(raw));
                inert.discard();
            }
            var wrongType = tag.copy();
            wrongType.putIntArray("upgrades", new int[]{1, 0});
            var wrong = load(wrongType);
            boolean refused;
            try { wrong.configuredData(); refused = false; } catch (IllegalStateException unsupported) { refused = true; }
            test.assertTrue(refused && wrong.getUpgradeAmount(GolemUpgrade.EARTH) == 0, "a non-byte-array tag is unsupported data, not an empty golem");
            wrong.discard();
            clearGolems();
        }

        private void placementRecallAndDismantling() {
            var placerData = config(GolemData.NO_CORE, List.of(EARTH, AIR));
            var placer = new ItemStack(ThaumcraftGolems.GOLEM_TALLOW.get());
            placer.set(ThaumcraftGolems.GOLEM_DATA.get(), placerData);
            var floor = base.below();
            var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false);

            // A one-slot material refuses an over-full placer outright instead of truncating it.
            var ironPlacer = new ItemStack(ThaumcraftGolems.GOLEM_IRON.get());
            ironPlacer.set(ThaumcraftGolems.GOLEM_DATA.get(), placerData);
            hold(GameType.SURVIVAL, ironPlacer);
            test.assertTrue(ironPlacer.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)) == InteractionResult.FAIL
                            && player.getMainHandItem().getCount() == 1 && golemsNear().isEmpty(),
                    "two upgrades on a one-slot Iron placer: refused, kept, nothing placed");

            hold(GameType.SURVIVAL, placer);
            test.assertTrue(placer.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)) == InteractionResult.CONSUME
                            && player.getMainHandItem().isEmpty(), "the Tallow placer places a golem and is paid");
            var placed = golemsNear();
            test.assertTrue(placed.size() == 1, "exactly one golem was placed");
            var golem = placed.getFirst();
            spawned.add(golem);
            test.assertTrue(slots(golem).equals(List.of(EARTH, AIR)) && golem.getGolemStrength() == 3
                            && (float) golem.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.33F * (1.0F + 0.15F),
                    "the placer's upgrades reach the placed golem, with their effects");

            // Plain bell recall keeps the upgrades in the returned placer.
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.GOLEM_BELL.get()));
            test.assertTrue(golem.recall(player, false), "the bell recalls the golem");
            DeferredItemDrops.tick(level);
            var returned = items(ThaumcraftGolems.GOLEM_TALLOW.get());
            test.assertTrue(returned.size() == 1 && returned.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).upgrades().equals(List.of(EARTH, AIR)),
                    "the recalled placer carries both upgrades in its golem data: " + returned);
            test.assertTrue(items(upgrade(GolemUpgrade.EARTH)).isEmpty() && items(upgrade(GolemUpgrade.AIR)).isEmpty(),
                    "and no loose upgrade item is dropped by a plain recall");
            clearGolems();

            // Sneak-recall: a bare placer, and each upgrade comes back only on a coin toss, in slot order.
            long seed = -1;
            for (long candidate = 0; candidate < 4096 && seed < 0; candidate++) {
                RandomSource twin = RandomSource.create(candidate);
                if (twin.nextBoolean() && !twin.nextBoolean()) seed = candidate;
            }
            test.assertTrue(seed >= 0, "a coin sequence of keep, lose exists");
            var dismantled = golem(TALLOW, GolemData.NO_CORE, List.of(EARTH, AIR));
            dismantled.getRandom().setSeed(seed);
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.GOLEM_BELL.get()));
            player.setShiftKeyDown(true);
            test.assertTrue(dismantled.recall(player, true), "the sneaking bell dismantles the golem");
            DeferredItemDrops.tick(level);
            var bare = items(ThaumcraftGolems.GOLEM_TALLOW.get());
            test.assertTrue(bare.size() == 1 && bare.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).upgrades().isEmpty(),
                    "the dismantled placer is bare");
            test.assertTrue(items(upgrade(GolemUpgrade.EARTH)).size() == 1 && items(upgrade(GolemUpgrade.AIR)).isEmpty(),
                    "first coin kept the Earth upgrade, the second lost the Air upgrade");
            clearGolems();
            player.setShiftKeyDown(false);
        }

        private ItemStack placerStack(Item item, int core, List<Integer> upgrades) {
            var stack = new ItemStack(item);
            stack.set(ThaumcraftGolems.GOLEM_DATA.get(), config(core, upgrades));
            return stack;
        }

        /** The real placement hook with the player in survival; the stack is paid only when a golem is placed. */
        private InteractionResult place(ItemStack placer) {
            hold(GameType.SURVIVAL, placer);
            var floor = base.below();
            var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false);
            return placer.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        }

        private EntityGolemBase placedGolem() {
            var placed = golemsNear();
            test.assertTrue(placed.size() == 1, "exactly one golem was placed: " + placed.size());
            spawned.add(placed.getFirst());
            return placed.getFirst();
        }

        /** Capacity of real golems of every interesting material, with the Earth upgrades fitted by configuration. */
        private void earthCapacity() {
            record Row(int material, List<Integer> upgrades, int carry, int fluid) {}
            for (Row row : List.of(
                    new Row(STRAW, List.of(), 1, 1000), new Row(STRAW, List.of(EARTH), 5, 2000),
                    new Row(WOOD, List.of(EARTH), 8, 2000),
                    new Row(TALLOW, List.of(), 8, 2000), new Row(TALLOW, List.of(EARTH), 16, 4000),
                    new Row(TALLOW, List.of(EARTH, EARTH), 24, 4000),
                    new Row(IRON, List.of(), 32, 5000), new Row(IRON, List.of(EARTH), 48, 6000),
                    new Row(THAUMIUM, List.of(EARTH, AIR), 48, 6000), new Row(THAUMIUM, List.of(EARTH, EARTH), 64, 8000))) {
                var golem = golem(row.material(), GolemData.NO_CORE, row.upgrades());
                test.assertTrue(golem.getCarryLimit() == row.carry() && golem.getFluidCarryLimit() == row.fluid(),
                        "material " + row.material() + " with " + row.upgrades() + " carries " + row.carry() + " items / " + row.fluid()
                                + " mB, saw " + golem.getCarryLimit() + " / " + golem.getFluidCarryLimit());
                clearGolems();
            }
        }

        /**
         * The water and essentia limits are material + the golem's OWN Earth count, in the placer, on the entity, across save/load and
         * through the bell. Stock above the old material limit is accepted only with the Earth upgrades that pay for it, is never lost to
         * a dismantling (which refuses while stock is held) and, if the Earth count is not what the stock needs, stays as raw data.
         */
        private void earthCargoCustody() {
            Item thaumium = ThaumcraftGolems.GOLEM_THAUMIUM.get();
            Item bell = ThaumcraftGolems.GOLEM_BELL.get();
            // The pre-Earth limit of Thaumium and Iron (5000 mB) still holds without Earth, and a bigger stock needs the upgrades.
            for (Object[] row : new Object[][]{{List.<Integer>of(), 6000, InteractionResult.FAIL}, {List.<Integer>of(), 5000, InteractionResult.CONSUME},
                    {List.of(EARTH), 8000, InteractionResult.FAIL}, {List.of(EARTH), 6000, InteractionResult.CONSUME},
                    {List.of(EARTH, EARTH), 8000, InteractionResult.CONSUME}}) {
                @SuppressWarnings("unchecked") List<Integer> upgrades = (List<Integer>) row[0];
                int amount = (Integer) row[1];
                var placer = placerStack(thaumium, GolemData.LIQUID, upgrades);
                new GolemWaterCargo(amount).writeToPlacer(placer);
                var result = place(placer);
                test.assertTrue(result == row[2], amount + " mB with " + upgrades.size() + " Earth: placement " + result + ", expected " + row[2]);
                if (result == InteractionResult.CONSUME) {
                    var golem = placedGolem();
                    test.assertTrue(player.getMainHandItem().isEmpty() && golem.getWaterAmountMb() == amount && golem.getGolemCore() == GolemData.LIQUID
                            && golem.getFluidCarryLimit() >= amount, amount + " mB reaches the placed golem and the placer is paid");
                } else {
                    test.assertTrue(!player.getMainHandItem().isEmpty() && golemsNear().isEmpty()
                                    && GolemWaterCargo.readPlacer(player.getMainHandItem()).amountMb() == amount,
                            "a refused placer is kept untouched with its water and nothing is placed");
                }
                clearGolems();
            }

            // Full round trip of the maximum: placer -> golem -> NBT -> bell -> placer -> golem.
            var maximum = placerStack(thaumium, GolemData.LIQUID, List.of(EARTH, EARTH));
            new GolemWaterCargo(8000).writeToPlacer(maximum);
            test.assertTrue(place(maximum) == InteractionResult.CONSUME, "a Thaumium Decanting placer with two Earth places 8000 mB");
            var golem = placedGolem();
            test.assertTrue(golem.getWaterAmountMb() == 8000 && golem.getWaterCargo().equals(new GolemWaterCargo(8000))
                            && golem.getUpgradeAmount(GolemUpgrade.EARTH) == 2 && golem.getFluidCarryLimit() == 8000,
                    "the golem holds 8000 of its 8000 mB");

            hold(GameType.SURVIVAL, new ItemStack(bell));
            player.setShiftKeyDown(true);
            boolean dismantled = golem.recall(player, true);
            player.setShiftKeyDown(false);
            test.assertTrue(!dismantled && golem.isAlive() && !golem.isRemoved() && golem.getWaterAmountMb() == 8000
                            && golem.getUpgradeAmount(GolemUpgrade.EARTH) == 2 && items(thaumium).isEmpty(),
                    "dismantling is refused while water is held, so the Earth upgrades that pay for it cannot be lost with the water");

            var tag = save(golem);
            var reloaded = load(tag);
            test.assertTrue(reloaded.getWaterAmountMb() == 8000 && reloaded.getUpgradeAmount(GolemUpgrade.EARTH) == 2,
                    "save/load keeps 8000 mB with its two Earth upgrades");
            reloaded.discard();
            var tampered = tag.copy();
            tampered.putByteArray("upgrades", new byte[]{(byte) EARTH, (byte) EMPTY});
            var inert = load(tampered);
            boolean refused;
            try { inert.configuredData(); refused = false; } catch (IllegalStateException unsupported) { refused = true; }
            test.assertTrue(refused && save(inert).getCompound("WaterCargo").equals(tag.getCompound("WaterCargo")),
                    "8000 mB cannot be honoured with one Earth: the stock is retained verbatim as raw data, never clamped or dropped");
            inert.discard();

            hold(GameType.SURVIVAL, new ItemStack(bell));
            test.assertTrue(golem.recall(player, false), "the plain bell recalls the golem");
            DeferredItemDrops.tick(level);
            var returned = items(thaumium);
            test.assertTrue(returned.size() == 1 && returned.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).upgrades().equals(List.of(EARTH, EARTH))
                            && GolemWaterCargo.readPlacer(returned.getFirst()).amountMb() == 8000,
                    "the returned placer holds both Earth upgrades and all 8000 mB");
            clearGolems();
            test.assertTrue(place(returned.getFirst()) == InteractionResult.CONSUME && placedGolem().getWaterAmountMb() == 8000,
                    "and it places again with the whole stock");
            clearGolems();

            // Essentia: carry limit 32 without Earth, 48 with one, 64 with two (Thaumium).
            for (Object[] row : new Object[][]{{List.<Integer>of(), 33, InteractionResult.FAIL}, {List.<Integer>of(), 32, InteractionResult.CONSUME},
                    {List.of(EARTH), 64, InteractionResult.FAIL}, {List.of(EARTH), 48, InteractionResult.CONSUME},
                    {List.of(EARTH, EARTH), 64, InteractionResult.CONSUME}}) {
                @SuppressWarnings("unchecked") List<Integer> upgrades = (List<Integer>) row[0];
                int amount = (Integer) row[1];
                var placer = placerStack(thaumium, GolemData.ALCHEMY, upgrades);
                new GolemAspectCargo(Aspect.AIR, amount).writeToPlacer(placer);
                var result = place(placer);
                test.assertTrue(result == row[2], amount + " essentia with " + upgrades.size() + " Earth: placement " + result + ", expected " + row[2]);
                if (result == InteractionResult.CONSUME) {
                    var alchemist = placedGolem();
                    test.assertTrue(alchemist.getEssentiaAmount() == amount && alchemist.getEssentiaAspect() == Aspect.AIR
                                    && alchemist.getCarryLimit() >= amount, amount + " essentia reaches the placed golem");
                }
                clearGolems();
            }
            var alchemyMaximum = placerStack(thaumium, GolemData.ALCHEMY, List.of(EARTH, EARTH));
            new GolemAspectCargo(Aspect.AIR, 64).writeToPlacer(alchemyMaximum);
            test.assertTrue(place(alchemyMaximum) == InteractionResult.CONSUME, "64 essentia need Thaumium plus two Earth");
            var alchemist = placedGolem();
            hold(GameType.SURVIVAL, new ItemStack(bell));
            player.setShiftKeyDown(true);
            boolean alchemyDismantled = alchemist.recall(player, true);
            player.setShiftKeyDown(false);
            test.assertTrue(!alchemyDismantled && alchemist.isAlive() && alchemist.getEssentiaAmount() == 64,
                    "an essentia-laden golem cannot be dismantled either");
            hold(GameType.SURVIVAL, new ItemStack(bell));
            test.assertTrue(alchemist.recall(player, false), "the plain bell recalls it");
            DeferredItemDrops.tick(level);
            var essentiaPlacer = items(thaumium);
            test.assertTrue(essentiaPlacer.size() == 1 && GolemAspectCargo.readPlacer(essentiaPlacer.getFirst()).equals(new GolemAspectCargo(Aspect.AIR, 64))
                            && essentiaPlacer.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).upgrades().equals(List.of(EARTH, EARTH)),
                    "the placer keeps all 64 essentia together with the Earth upgrades");
            clearGolems();
        }

        /**
         * Water range 16 + 4n, decided by a real Guard golem for real hostile mobs placed at fixed distances from its home:
         * strictly inside the sphere of radius getRange(), so 16 itself is out without Water and 20 is out with one.
         */
        private void waterRange() {
            var guard = golem(TALLOW, GolemData.GUARD, List.of());
            guard.setHome(base, Direction.UP);
            int[] distances = {15, 16, 19, 20, 23, 24};
            List<net.minecraft.world.entity.Mob> targets = new ArrayList<>();
            for (int distance : distances) {
                BlockPos pos = base.offset(distance, 0, 0);
                level.getChunk(pos); // The decision is made on loaded ground; nothing is loaded by the golem itself.
                var spider = EntityType.SPIDER.create(level);
                spider.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
                spider.setNoAi(true);
                spider.setPersistenceRequired();
                test.assertTrue(ChampionJoinRoll.join(level, spider) && spider.isAlive() && level.hasChunkAt(pos), "a real hostile spider stands " + distance + " blocks from the home");
                attackers.add(spider);
                targets.add(spider);
            }
            List<Boolean> expectedRange16 = List.of(true, false, false, false, false, false);
            List<Boolean> expectedRange20 = List.of(true, true, true, false, false, false);
            List<Boolean> expectedRange24 = List.of(true, true, true, true, true, false);
            test.assertTrue(guard.getRange() == 16F && guardView(guard, targets).equals(expectedRange16),
                    "no Water: only the spider at 15 is a target (the sphere is strict, 16 is out): " + guardView(guard, targets));

            hold(GameType.SURVIVAL, new ItemStack(upgrade(GolemUpgrade.WATER), 2));
            test.assertTrue(interact(guard) == InteractionResult.CONSUME && player.getMainHandItem().getCount() == 1 && slots(guard).equals(List.of(WATER, EMPTY)),
                    "the Water upgrade installs through the native interaction and pays one item");
            test.assertTrue(guard.getRange() == 20F && guard.getUpgradeAmount(GolemUpgrade.WATER) == 1 && guardView(guard, targets).equals(expectedRange20),
                    "one Water: range 20, spiders up to 19 are targets, 20 is out: " + guardView(guard, targets));
            test.assertTrue(guard.getGolemStrength() == 2 && guard.getCarryLimit() == 8 && guard.getMeleeDamage() == 4F
                            && (float) guard.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.33F,
                    "Water changes neither strength, damage, capacity nor speed");
            test.assertTrue(interact(guard) == InteractionResult.CONSUME && player.getMainHandItem().isEmpty() && slots(guard).equals(List.of(WATER, WATER)),
                    "a second Water fits and a third would not");
            test.assertTrue(guard.getRange() == 24F && guardView(guard, targets).equals(expectedRange24),
                    "two Water: range 24, spiders up to 23 are targets, 24 is out: " + guardView(guard, targets));

            var saved = save(guard);
            test.assertTrue(java.util.Arrays.equals(saved.getByteArray("upgrades"), new byte[]{(byte) WATER, (byte) WATER}),
                    "saved under the original key with the original id 3 for Water");
            var reloaded = load(saved);
            test.assertTrue(reloaded.getRange() == 24F && slots(reloaded).equals(List.of(WATER, WATER)), "a reloaded golem keeps its range");
            reloaded.discard();
            clearGolems();

            var placer = placerStack(ThaumcraftGolems.GOLEM_TALLOW.get(), GolemData.GUARD, List.of(WATER));
            test.assertTrue(place(placer) == InteractionResult.CONSUME, "a Tallow placer carrying a Water upgrade places");
            var placed = placedGolem();
            test.assertTrue(placed.getRange() == 20F && slots(placed).equals(List.of(WATER, EMPTY)), "its range is already 20 on the placed golem");
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.GOLEM_BELL.get()));
            test.assertTrue(placed.recall(player, false), "the bell recalls it");
            DeferredItemDrops.tick(level);
            var returned = items(ThaumcraftGolems.GOLEM_TALLOW.get());
            test.assertTrue(returned.size() == 1 && returned.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).upgrades().equals(List.of(WATER)),
                    "and the recalled placer keeps the Water upgrade");
            clearGolems();
        }

        private List<Boolean> guardView(EntityGolemBase guard, List<net.minecraft.world.entity.Mob> targets) {
            List<Boolean> view = new ArrayList<>();
            for (var target : targets) view.add(guard.isGuardTarget(target));
            return view;
        }

        private List<EntityGolemBase> golemsNear() {
            return level.getEntitiesOfClass(EntityGolemBase.class, lane, golem -> !existing.contains(golem.getUUID()) && golem.isAlive());
        }

        private List<ItemStack> items(Item item) {
            return level.getEntitiesOfClass(ItemEntity.class, lane, entity -> !existing.contains(entity.getUUID()) && entity.getItem().is(item))
                    .stream().map(entity -> entity.getItem().copy()).toList();
        }

        @Override public void close() {
            try {
                clearGolems();
                attackers.forEach(Entity::discard);
                if (touched) original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            } finally {
                try {
                    if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                } finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }
    }
}
