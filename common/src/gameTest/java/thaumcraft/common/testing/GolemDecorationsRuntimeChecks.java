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
import net.minecraft.network.chat.contents.TranslatableContents;
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
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EnumGolemType;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemDecorations;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemUpgrades;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * The seven ported golem accessories on real content: the loaded arcane recipes, installation by a real connected player through
 * the native interaction (payment, repeats, the three partner pairs, a coreless golem), every effect on real golems (health,
 * armour, speed, damage, range decided by a real Guard for real spiders, the fez's repair cadence measured over real ticks), the
 * visor's experience from a real Guard's real kill, entity NBT, bell recall (kept) and dismantling (lost) and the placer text.
 * Players, golems and spiders are explicit inputs; the letters, effects, payment and drops are never supplied. NOT the dart
 * launcher (GolemDartRuntimeChecks), a client, network, multiplayer or visual check.
 */
public final class GolemDecorationsRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int WOOD = 1, TALLOW = 2, IRON = 6, DEADLINE = 1500, RANGE_DELAY = 5;
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemDecorationsRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.recipes();
            check.start();
            check.holdRangeArea();
            check.installation();
            check.effects();
            check.persistence();
            check.recallAndDismantling();
            check.tooltip();
        } catch (RuntimeException failure) {
            check.finish();
            throw failure;
        }
        test.runAfterDelay(RANGE_DELAY, () -> {
            if (check.finished) return;
            try {
                check.range();
                check.armLanes();
                check.ready = true;
            } catch (RuntimeException failure) {
                check.finish();
                throw failure;
            }
        });
        test.onEachTick(() -> {
            if (check.finished || !check.ready) return;
            try {
                if (check.poll()) { check.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.error("Decoration lanes failed at tick {}: {} ({})", test.getTick(), failure.getMessage(), check.describe());
                check.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (check.finished) return;
            String state = check.describe();
            check.finish();
            test.fail("the decoration lanes did not finish within " + DEADLINE + " ticks: " + state);
        });
    }

    private static final class Check {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final BlockPos base;
        private final AABB lane;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final Set<UUID> existing = new HashSet<>();
        private final List<Entity> spawned = new ArrayList<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<Lane> lanes = new ArrayList<>();
        private boolean touched, finished, ready;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            lane = new AABB(Vec3.atLowerCornerOf(base.offset(-4, -2, -4)), Vec3.atLowerCornerOf(base.offset(26, 6, 5)));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-4, -2, -4), base.offset(5, 5, 4))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
            level.getAllEntities().forEach(entity -> existing.add(entity.getUUID()));
            var profile = new GameProfile(UUID.randomUUID(), "deco-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        }

        private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", path)); }

        private GolemData config(int core, String decoration) {
            return new GolemData(core, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, List.of(),
                    List.of(), GolemGuardTargets.DEFAULT, false, decoration);
        }

        // ---- the loaded recipes -----------------------------------------------------------------------------------

        private void recipes() {
            record Row(String name, String research, List<String> pattern, Map<String, String> key, Map<String, Integer> vis) {}
            for (Row row : List.of(
                    new Row("tophat", "TINYHAT", List.of(" C ", " G ", "CCC"), Map.of("C", "minecraft:black_wool", "G", "minecraft:gold_ingot"), Map.of("ordo", 8, "ignis", 8)),
                    new Row("glasses", "TINYGLASSES", List.of("GIG"), Map.of("G", "minecraft:glass", "I", "minecraft:iron_ingot"), Map.of("aer", 4, "aqua", 4)),
                    new Row("bowtie", "TINYBOWTIE", List.of("CSC", "C C"), Map.of("C", "minecraft:black_wool", "S", "minecraft:string"), Map.of("aer", 4, "ordo", 4)),
                    new Row("fez", "TINYFEZ", List.of("CCS", "CCS", "  S"), Map.of("C", "minecraft:red_wool", "S", "minecraft:string"), Map.of("aqua", 4, "terra", 4)),
                    new Row("visor", "TINYVISOR", List.of("IHI"), Map.of("I", "minecraft:iron_ingot", "H", "minecraft:leather_helmet"), Map.of("terra", 4, "aqua", 4)),
                    new Row("armor", "TINYARMOR", List.of("I I", "IAI"), Map.of("I", "minecraft:iron_ingot", "A", "minecraft:leather_chestplate"), Map.of("terra", 8)),
                    new Row("mace", "TINYHAMMER", List.of("III", "III", " I "), Map.of("I", "minecraft:iron_ingot"), Map.of("terra", 4, "ignis", 4)))) {
                var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/golem_decoration_" + row.name()));
                test.assertTrue(holder.isPresent() && holder.get().value() instanceof ArcaneShapedRecipe, row.name() + " arcane recipe is loaded");
                var recipe = (ArcaneShapedRecipe) holder.get().value();
                test.assertTrue(recipe.research().equals(row.research()) && recipe.pattern().equals(row.pattern()) && recipe.vis().equals(row.vis())
                                && recipe.result().is(item("golem_decoration_" + row.name())) && recipe.result().getCount() == 1
                                && recipe.key().keySet().equals(row.key().keySet()),
                        row.name() + ": research " + row.research() + ", the original pattern and vis, one accessory");
                row.key().forEach((symbol, id) -> test.assertTrue(recipe.key().get(symbol).test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)))),
                        row.name() + ": " + symbol + " accepts " + id));
            }
            // Leather armour of any wear is accepted, an iron one is not.
            var visor = (ArcaneShapedRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/golem_decoration_visor")).orElseThrow().value();
            var worn = new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET);
            worn.setDamageValue(30);
            test.assertTrue(visor.key().get("H").test(worn) && !visor.key().get("H").test(new ItemStack(net.minecraft.world.item.Items.IRON_HELMET)),
                    "a worn leather helmet is accepted, an iron helmet is not");
            // The launcher's own recipe and item are checked with the launcher itself (GolemDartRuntimeChecks).
        }

        // ---- real golems and a real player -------------------------------------------------------------------------

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

        private EntityGolemBase golem(int material, int core, String decoration) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(material);
            golem.applyConfiguration(config(core, decoration));
            golem.setPos(Vec3.atBottomCenterOf(base));
            test.assertTrue(ChampionJoinRoll.join(level, golem), "the golem is admitted");
            spawned.add(golem);
            return golem;
        }

        private void hold(GameType mode, ItemStack stack) {
            player.setGameMode(mode);
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setPos(Vec3.atBottomCenterOf(base).add(1.2, 0, 0));
        }

        private InteractionResult use(EntityGolemBase golem) { return golem.interact(player, InteractionHand.MAIN_HAND); }

        private void clearGolems() {
            for (Entity entity : spawned) entity.discard();
            spawned.clear();
            level.getEntitiesOfClass(EntityGolemBase.class, lane, golem -> !existing.contains(golem.getUUID())).forEach(Entity::discard);
            level.getEntitiesOfClass(ItemEntity.class, lane, item -> !existing.contains(item.getUUID())).forEach(Entity::discard);
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

        private List<ItemStack> items(Item item) {
            return level.getEntitiesOfClass(ItemEntity.class, lane, entity -> !existing.contains(entity.getUUID()) && entity.getItem().is(item))
                    .stream().map(entity -> entity.getItem().copy()).toList();
        }

        /** Right-click installation: payment, repeats and partners, on a golem that has no core at all. */
        private void installation() {
            var golem = golem(IRON, GolemData.NO_CORE, "");
            test.assertTrue(golem.getDecoration().isEmpty(), "a fresh golem wears nothing");
            hold(GameType.SURVIVAL, new ItemStack(item("golem_decoration_tophat"), 3));
            test.assertTrue(use(golem) == InteractionResult.CONSUME && golem.getDecoration().equals("H") && player.getMainHandItem().getCount() == 2,
                    "the top hat installs on a coreless golem and pays one item");
            test.assertTrue(use(golem) != InteractionResult.CONSUME && golem.getDecoration().equals("H") && player.getMainHandItem().getCount() == 2,
                    "a second top hat is refused without payment");
            hold(GameType.SURVIVAL, new ItemStack(item("golem_decoration_fez")));
            test.assertTrue(use(golem) != InteractionResult.CONSUME && golem.getDecoration().equals("H") && player.getMainHandItem().getCount() == 1,
                    "the fez is the hat's partner: refused, kept");
            hold(GameType.SURVIVAL, new ItemStack(item("golem_decoration_glasses")));
            test.assertTrue(use(golem) == InteractionResult.CONSUME && golem.getDecoration().equals("HG") && player.getMainHandItem().isEmpty(), "spectacles install after the hat");
            hold(GameType.SURVIVAL, new ItemStack(item("golem_decoration_visor")));
            test.assertTrue(use(golem) != InteractionResult.CONSUME && golem.getDecoration().equals("HG") && player.getMainHandItem().getCount() == 1,
                    "the visor is the spectacles' partner: refused, kept");
            hold(GameType.SURVIVAL, new ItemStack(item("golem_decoration_bowtie")));
            test.assertTrue(use(golem) == InteractionResult.CONSUME && golem.getDecoration().equals("HGB"), "the bowtie installs");
            hold(GameType.SURVIVAL, new ItemStack(item("golem_decoration_armor")));
            test.assertTrue(use(golem) != InteractionResult.CONSUME && golem.getDecoration().equals("HGB") && player.getMainHandItem().getCount() == 1,
                    "the plating is the bowtie's partner: refused, kept");
            hold(GameType.SURVIVAL, new ItemStack(item("golem_decoration_mace")));
            test.assertTrue(use(golem) == InteractionResult.CONSUME && golem.getDecoration().equals("HGBM"), "the mace installs: four letters, installation order");
            clearGolems();

            // Creative pays nothing; sneaking installs nothing (the golem ignores a sneaking player).
            var other = golem(WOOD, GolemData.NO_CORE, "");
            hold(GameType.CREATIVE, new ItemStack(item("golem_decoration_mace")));
            test.assertTrue(use(other) == InteractionResult.CONSUME && other.getDecoration().equals("M") && player.getMainHandItem().getCount() == 1,
                    "creative installs without paying");
            hold(GameType.SURVIVAL, new ItemStack(item("golem_decoration_bowtie")));
            player.setShiftKeyDown(true);
            test.assertTrue(use(other) != InteractionResult.CONSUME && other.getDecoration().equals("M"), "a sneaking player installs nothing");
            player.setShiftKeyDown(false);
            clearGolems();
        }

        /** The effect of every letter on the real golem's attributes and decisions. */
        private void effects() {
            var type = EnumGolemType.getType(IRON);
            var plain = golem(IRON, GolemData.GUARD, "");
            double health = plain.getMaxHealth(), armor = plain.getAttributeValue(Attributes.ARMOR), speed = plain.getAttributeValue(Attributes.MOVEMENT_SPEED),
                    damage = plain.getAttributeValue(Attributes.ATTACK_DAMAGE);
            test.assertTrue(health == type.health && armor == type.armor && damage == plain.getMeleeDamage(), "the undecorated Iron golem has the material values");
            clearGolems();

            var hat = golem(IRON, GolemData.GUARD, "H");
            test.assertTrue(hat.getMaxHealth() == health + 5 && hat.getHealth() == hat.getMaxHealth(), "the top hat adds five maximum health, and a placed golem starts full");
            var plating = golem(IRON, GolemData.GUARD, "P");
            test.assertTrue(plating.getAttributeValue(Attributes.ARMOR) == Math.min(20, type.armor + 4)
                            && (float) plating.getAttributeValue(Attributes.MOVEMENT_SPEED) == GolemUpgrades.speed(type.speed, 0, false, "P")
                            && plating.getAttributeValue(Attributes.MOVEMENT_SPEED) < speed,
                    "the plating adds four armour (capped at 20) and slows to 0.88");
            var visor = golem(IRON, GolemData.GUARD, "V");
            test.assertTrue(visor.getAttributeValue(Attributes.ARMOR) == Math.min(20, type.armor + 1), "the visor adds one armour");
            var bowtie = golem(IRON, GolemData.GUARD, "B");
            test.assertTrue((float) bowtie.getAttributeValue(Attributes.MOVEMENT_SPEED) == GolemUpgrades.speed(type.speed, 0, false, "B")
                            && bowtie.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed, "the bowtie speeds the golem up by 1.1");
            var mace = golem(IRON, GolemData.GUARD, "M");
            test.assertTrue(mace.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage + 2 && mace.getMeleeDamage() == plain.getMeleeDamage() + 2,
                    "the mace adds two melee damage to the attribute and to the hit");
            var fez = golem(WOOD, GolemData.NO_CORE, "F");
            test.assertTrue(fez.getRegenDelay() == (int) (EnumGolemType.getType(WOOD).regenDelay * 0.66f)
                            && golem(WOOD, GolemData.NO_CORE, "").getRegenDelay() == EnumGolemType.getType(WOOD).regenDelay,
                    "the fez shortens the repair delay to two thirds");
            clearGolems();

            // Installing later changes the attributes at once, without healing the new maximum.
            var late = golem(IRON, GolemData.GUARD, "");
            late.setHealth(late.getMaxHealth() - 3);
            hold(GameType.SURVIVAL, new ItemStack(item("golem_decoration_tophat")));
            test.assertTrue(use(late) == InteractionResult.CONSUME && late.getMaxHealth() == type.health + 5 && late.getHealth() == type.health - 3,
                    "a hat installed later raises the maximum but does not heal");
            clearGolems();
        }

        /** The Guard sees a spider only when the 48 blocks around both are loaded; where the test landed in its chunk is random. */
        private void holdRangeArea() { forced.addAll(GolemChunks.hold(level, List.of(base, base.offset(22, 0, 0)))); }

        /**
         * Range, decided by a real Guard for real spiders: spectacles 16 -> 17.6, the advanced golem on top of that. Runs a few ticks
         * after {@link #holdRangeArea}: a chunk that has just been forced is loaded at once but its entities stay hidden from the
         * world's lookup (so no Guard can see them) until the chunk status has moved on, which takes a tick or two.
         */
        private void range() {
            // Range, decided by a real Guard for real spiders: spectacles 16 -> 17.6, the advanced golem on top of that.
            var guard = golem(TALLOW, GolemData.GUARD, "G");
            guard.setHome(base, Direction.UP);
            int[] distances = {16, 17, 18, 21, 22};
            List<Mob> targets = new ArrayList<>();
            for (int distance : distances) {
                BlockPos pos = base.offset(distance, 0, 0);
                var spider = EntityType.SPIDER.create(level);
                spider.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
                spider.setNoAi(true);
                spider.setPersistenceRequired();
                test.assertTrue(ChampionJoinRoll.join(level, spider) && spider.isAlive() && level.hasChunkAt(pos) && level.getEntity(spider.getId()) == spider,
                        "a real spider stands " + distance + " blocks from the home and the world can look it up");
                spawned.add(spider);
                targets.add(spider);
            }
            List<Boolean> view = new ArrayList<>();
            for (var target : targets) view.add(guard.isGuardTarget(target));
            test.assertTrue(guard.getRange() == 16F + 16F * 0.1F && view.equals(List.of(true, true, false, false, false)),
                    "spectacles: range 17.6, spiders at 16 and 17 are targets and 18 is out: " + view);
            var advancedGuard = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            advancedGuard.setGolemMaterial(TALLOW);
            advancedGuard.applyConfiguration(config(GolemData.GUARD, "G").withAdvanced(true));
            advancedGuard.setPos(Vec3.atBottomCenterOf(base));
            advancedGuard.setHome(base, Direction.UP);
            test.assertTrue(ChampionJoinRoll.join(level, advancedGuard), "an advanced Guard in spectacles is admitted");
            spawned.add(advancedGuard);
            view.clear();
            for (var target : targets) view.add(advancedGuard.isGuardTarget(target));
            float expectedRange = (16F + 16F * 0.1F) + Math.max((16F + 16F * 0.1F) * 0.2F, 2F);
            test.assertTrue(advancedGuard.getRange() == expectedRange && view.equals(List.of(true, true, true, true, false)),
                    "advanced spectacles: range " + expectedRange + ", spiders up to 21 are targets and 22 is out: " + view);
            var noGlasses = golem(TALLOW, GolemData.GUARD, "HM");
            noGlasses.setHome(base, Direction.UP);
            test.assertTrue(noGlasses.getRange() == 16F, "other accessories leave the range at 16");
            clearGolems();
        }

        private void persistence() {
            var golem = golem(TALLOW, GolemData.NO_CORE, "HGBM");
            var tag = save(golem);
            test.assertTrue(tag.contains("Decoration", Tag.TAG_STRING) && tag.getString("Decoration").equals("HGBM"), "saved under the original key, in installation order");
            var loaded = load(tag);
            test.assertTrue(loaded.getDecoration().equals("HGBM") && loaded.getMaxHealth() == golem.getMaxHealth()
                            && loaded.getAttributeValue(Attributes.ATTACK_DAMAGE) == golem.getAttributeValue(Attributes.ATTACK_DAMAGE)
                            && (float) loaded.getAttributeValue(Attributes.MOVEMENT_SPEED) == (float) golem.getAttributeValue(Attributes.MOVEMENT_SPEED)
                            && loaded.getRange() == golem.getRange() && loaded.configuredData().decoration().equals("HGBM"),
                    "a reloaded golem keeps the accessories and re-derives their effects");
            test.assertTrue(save(loaded).getString("Decoration").equals("HGBM"), "and saves them again");
            loaded.discard();
            test.assertTrue(!save(golem(TALLOW, GolemData.NO_CORE, "")).contains("Decoration"), "an undecorated golem writes no key: earlier saves keep their form");

            for (String bad : List.of("HF", "GV", "BP", "HH", "RR", "X")) {
                var broken = tag.copy();
                broken.putString("Decoration", bad);
                var unsupported = load(broken);
                boolean refused;
                try { unsupported.configuredData(); refused = false; } catch (IllegalStateException expected) { refused = true; }
                test.assertTrue(refused, "a decoration " + bad + " could not have been installed: kept raw and inert, never trimmed");
                unsupported.discard();
            }
            var wrongType = tag.copy();
            wrongType.putInt("Decoration", 5);
            var inert = load(wrongType);
            boolean refused;
            try { inert.configuredData(); refused = false; } catch (IllegalStateException expected) { refused = true; }
            test.assertTrue(refused, "a decoration that is not a string is kept raw and inert");
            inert.discard();
            clearGolems();
        }

        private void recallAndDismantling() {
            var golem = golem(TALLOW, GolemData.GUARD, "FVM");
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.GOLEM_BELL.get()));
            test.assertTrue(golem.recall(player, false), "the bell recalls the decorated golem");
            DeferredItemDrops.tick(level);
            var returned = items(item("golem_tallow"));
            test.assertTrue(returned.size() == 1 && returned.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).decoration().equals("FVM")
                            && returned.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).core() == GolemData.GUARD,
                    "the recalled placer keeps core and accessories: " + returned);
            clearGolems();

            // The recalled placer places the accessories again.
            hold(GameType.SURVIVAL, returned.getFirst());
            var floor = base.below();
            var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false);
            test.assertTrue(returned.getFirst().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)) == InteractionResult.CONSUME,
                    "the placer places the decorated golem");
            var placed = level.getEntitiesOfClass(EntityGolemBase.class, lane, e -> !existing.contains(e.getUUID()) && e.isAlive());
            test.assertTrue(placed.size() == 1 && placed.getFirst().getDecoration().equals("FVM"), "and its accessories are already on it");
            spawned.add(placed.getFirst());
            clearGolems();

            var dismantled = golem(TALLOW, GolemData.GUARD, "FVM");
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.GOLEM_BELL.get()));
            player.setShiftKeyDown(true);
            test.assertTrue(dismantled.recall(player, true), "the sneaking bell dismantles it");
            DeferredItemDrops.tick(level);
            var bare = items(item("golem_tallow"));
            test.assertTrue(bare.size() == 1 && bare.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).decoration().isEmpty(),
                    "the bare placer carries no accessories");
            for (String name : List.of("tophat", "glasses", "bowtie", "fez", "visor", "armor", "mace")) {
                test.assertTrue(items(item("golem_decoration_" + name)).isEmpty(), "dismantling does not return the " + name + " (original ItemGolemBell)");
            }
            player.setShiftKeyDown(false);
            clearGolems();
        }

        private void tooltip() {
            var placer = new ItemStack(item("golem_wood"));
            placer.set(ThaumcraftGolems.GOLEM_DATA.get(), config(GolemData.NO_CORE, "HM"));
            var lines = placer.getTooltipLines(Item.TooltipContext.of(level), player, TooltipFlag.Default.NORMAL);
            boolean listed = lines.stream().anyMatch(line -> line.getSiblings().stream().anyMatch(sibling ->
                    sibling.getContents() instanceof TranslatableContents contents && contents.getKey().equals("item.thaumcraft.golem_decoration_tophat")));
            boolean mace = lines.stream().anyMatch(line -> line.getSiblings().stream().anyMatch(sibling ->
                    sibling.getContents() instanceof TranslatableContents contents && contents.getKey().equals("item.thaumcraft.golem_decoration_mace")));
            test.assertTrue(listed && mace, "the placer lists its accessories: " + lines);
            var itemLines = new ItemStack(item("golem_decoration_tophat")).getTooltipLines(Item.TooltipContext.of(level), player, TooltipFlag.Default.NORMAL);
            test.assertTrue(itemLines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("tooltip.thaumcraft.golem.decoration_h")), "an accessory item describes itself");
        }

        // ---- real Guards kill real spiders; two golems repair themselves ---------------------------------------------

        private void forceChunks(BlockPos around, int halo) {
            var center = new ChunkPos(around);
            for (int dx = -halo; dx <= halo; dx++) for (int dz = -halo; dz <= halo; dz++) {
                var chunk = new ChunkPos(center.x + dx, center.z + dz);
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk); }
            }
        }

        private void armLanes() {
            BlockPos visorBase = base.offset(0, 0, 40), plainBase = base.offset(0, 0, 80), regenBase = base.offset(0, 0, 120);
            for (BlockPos pos : List.of(visorBase, plainBase, regenBase)) forceChunks(pos, 2);
            lanes.add(new KillLane("VISOR", visorBase, "V", true));
            lanes.add(new KillLane("PLAIN", plainBase, "", false));
            lanes.add(new RegenLane("REGEN", regenBase));
            for (Lane each : lanes) each.setup();
        }

        private abstract class Lane {
            final String name;
            final BlockPos b;
            final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
            boolean done;

            Lane(String name, BlockPos b) {
                this.name = name; this.b = b;
                for (BlockPos pos : BlockPos.betweenClosed(b.offset(-8, -2, -8), b.offset(8, 5, 8))) saved.put(pos.immutable(), level.getBlockState(pos));
            }

            void setup() {
                for (BlockPos pos : saved.keySet()) {
                    level.setBlock(pos, pos.getY() == b.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
                }
                arm();
            }
            abstract void arm();
            abstract void poll();
            abstract String status();
            void restore() { saved.forEach((pos, state) -> level.setBlock(pos, state, QUIET)); }
        }

        /** A Guard (with or without a visor) kills a spider; experience orbs appear exactly when the visor marked the victim. */
        private final class KillLane extends Lane {
            final String decoration;
            final boolean expectsExperience;
            EntityGolemBase guard;
            Mob spider;
            Vec3 deathPos;
            long goneAt = -1;
            int orbs = -1;

            KillLane(String name, BlockPos b, String decoration, boolean expectsExperience) {
                super(name, b);
                this.decoration = decoration;
                this.expectsExperience = expectsExperience;
            }

            @Override void arm() {
                spider = EntityType.SPIDER.create(level);
                spider.moveTo(Vec3.atBottomCenterOf(b.offset(3, 0, 0)));
                spider.setNoAi(true);
                spider.setPersistenceRequired();
                test.assertTrue(ChampionJoinRoll.join(level, spider), "a real spider is admitted");
                spawned.add(spider);
                guard = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
                guard.setGolemMaterial(IRON);
                guard.applyConfiguration(config(GolemData.GUARD, decoration));
                guard.setPos(Vec3.atBottomCenterOf(b));
                guard.setHome(b, Direction.UP);
                guard.setOwnerName(player.getGameProfile().getName());
                guard.setPersistenceRequired();
                test.assertTrue(ChampionJoinRoll.join(level, guard) && guard.hasSupportedCore(), "a real Guard is admitted");
                spawned.add(guard);
            }

            @Override void poll() {
                test.assertTrue(guard.isAlive(), name + ": the Guard survived");
                if (spider.isAlive()) { deathPos = spider.position(); return; }
                if (goneAt < 0) { goneAt = test.getTick(); return; }
                if (orbs < 0 && test.getTick() >= goneAt + 2) {
                    orbs = level.getEntitiesOfClass(ExperienceOrb.class, new AABB(deathPos, deathPos).inflate(6)).stream().mapToInt(ExperienceOrb::getValue).sum();
                    boolean awarded = orbs > 0;
                    test.assertTrue(awarded == expectsExperience, name + ": the spider's death gave " + orbs + " experience; a golem "
                            + (decoration.isEmpty() ? "without" : "with") + " a visor " + (expectsExperience ? "must" : "must not") + " award it");
                    level.getEntitiesOfClass(ExperienceOrb.class, new AABB(deathPos, deathPos).inflate(6)).forEach(Entity::discard);
                    done = true;
                    Thaumcraft.LOG.info("Decoration lane {}: spider gone at tick {}, {} experience, done at tick {}", name, goneAt, orbs, test.getTick());
                }
            }

            @Override String status() {
                return "spider " + (spider.isAlive() ? "alive at " + spider.blockPosition().toShortString() + " hp " + spider.getHealth() : "dead") + ", orbs " + orbs;
            }
        }

        /** Two real golems, one in a fez, are kept three health short; the gap between two repairs is the repair delay. */
        private final class RegenLane extends Lane {
            EntityGolemBase fez, plain;
            final List<Long> fezHeals = new ArrayList<>(), plainHeals = new ArrayList<>();
            float fezLast, plainLast;

            RegenLane(String name, BlockPos b) { super(name, b); }

            @Override void arm() {
                fez = lone(b.offset(-2, 0, 0), "F");
                plain = lone(b.offset(2, 0, 0), "");
                fezLast = fez.getHealth();
                plainLast = plain.getHealth();
            }

            private EntityGolemBase lone(BlockPos at, String decoration) {
                var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
                golem.setGolemMaterial(WOOD);
                golem.applyConfiguration(config(GolemData.NO_CORE, decoration));
                golem.setPos(Vec3.atBottomCenterOf(at));
                golem.setHome(at, Direction.UP);
                golem.setPersistenceRequired();
                test.assertTrue(ChampionJoinRoll.join(level, golem), "a real golem is admitted");
                spawned.add(golem);
                golem.setHealth(golem.getMaxHealth() - 5);
                return golem;
            }

            private float watch(EntityGolemBase golem, float last, List<Long> heals) {
                float now = golem.getHealth();
                if (now > last) {
                    heals.add(test.getTick());
                    golem.setHealth(golem.getMaxHealth() - 5);
                    return golem.getHealth();
                }
                return now;
            }

            @Override void poll() {
                test.assertTrue(fez.isAlive() && plain.isAlive(), name + ": both golems survived");
                fezLast = watch(fez, fezLast, fezHeals);
                plainLast = watch(plain, plainLast, plainHeals);
                if (fezHeals.size() >= 4 && plainHeals.size() >= 3) {
                    long fezGap = fezHeals.get(3) - fezHeals.get(2), plainGap = plainHeals.get(2) - plainHeals.get(1);
                    int fezDelay = fez.getRegenDelay(), plainDelay = plain.getRegenDelay();
                    test.assertTrue(plainDelay == EnumGolemType.getType(WOOD).regenDelay && fezDelay == (int) (plainDelay * 0.66f),
                            name + ": the delays are 75 and 49 ticks");
                    test.assertTrue(plainGap == plainDelay + 1 && fezGap == fezDelay + 1,
                            name + ": two repairs of a real golem are delay + 1 ticks apart: fez " + fezGap + ", plain " + plainGap);
                    done = true;
                    Thaumcraft.LOG.info("Decoration lane {}: fez repairs {} (gap {}), plain repairs {} (gap {})", name, fezHeals, fezGap, plainHeals, plainGap);
                }
            }

            @Override String status() { return "fez heals " + fezHeals + ", plain heals " + plainHeals; }
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
                clearGolems();
                level.getEntitiesOfClass(ExperienceOrb.class, lane.inflate(200), orb -> true).forEach(Entity::discard);
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
