package thaumcraft.common.testing;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionResultPatches;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EnumGolemType;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUpgrades;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * ADVANCEDGOLEM on real content: the loaded infusion recipe and its centre patch (a real recipe manager, the wire and JSON
 * codecs, every placer material), the extra upgrade slot through the real placer, the native upgrade interaction, entity NBT
 * and bell recall/dismantling, and the three ported effects (speed x1.1, range +20% and attack delay 18) decided by real
 * golems and a real Guard against real spiders. Players, golems and spiders are explicit inputs; the slot capacity, the
 * recipe result, the persistence and every effect are never supplied. NOT a full altar ritual with this recipe (the generic
 * ritual is InfusionRuntimeChecks; this recipe's centre patch is checked through the same {@code assemble} the matrix
 * calls), a Guard fight, a client, network or visual check.
 */
public final class GolemAdvancedRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int STRAW = 0, WOOD = 1, TALLOW = 2, IRON = 6, THAUMIUM = 7;
    private static final int AIR = GolemUpgrade.AIR.id, EARTH = GolemUpgrade.EARTH.id, FIRE = GolemUpgrade.FIRE.id,
            WATER = GolemUpgrade.WATER.id, EMPTY = GolemUpgrade.EMPTY;
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemAdvancedRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        try (var check = new Check(test)) {
            check.recipe();
            check.start();
            check.placerSlots();
            check.nativeInstall();
            check.effects();
            check.persistence();
            check.recallAndDismantling();
            check.tooltip();
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
        private final List<ChunkPos> forced = new ArrayList<>();
        private boolean touched;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            lane = new AABB(Vec3.atLowerCornerOf(base.offset(-4, -2, -4)), Vec3.atLowerCornerOf(base.offset(26, 6, 5)));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-4, -2, -4), base.offset(5, 5, 4))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
            level.getAllEntities().forEach(entity -> existing.add(entity.getUUID()));
            var profile = new GameProfile(UUID.randomUUID(), "adv-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        }

        private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", path)); }

        private GolemData config(int core, List<Integer> upgrades, boolean advanced) {
            return new GolemData(core, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, upgrades,
                    List.of(), GolemGuardTargets.DEFAULT, advanced);
        }

        private ItemStack placer(Item item, int core, List<Integer> upgrades, boolean advanced) {
            var stack = new ItemStack(item);
            stack.set(ThaumcraftGolems.GOLEM_DATA.get(), config(core, upgrades, advanced));
            return stack;
        }

        // ---- the infusion recipe and its centre patch -------------------------------------------------------------

        private void recipe() {
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/golem_advanced"));
            test.assertTrue(holder.isPresent() && holder.get().value() instanceof InfusionStackRecipe, "the AdvancedGolem infusion recipe is loaded");
            var recipe = (InfusionStackRecipe) holder.get().value();
            test.assertTrue(recipe.research().equals("ADVANCEDGOLEM") && recipe.instability() == 3
                            && recipe.aspects().equals(Map.of("cognitio", 8, "sensus", 8, "victus", 8))
                            && recipe.components().size() == 5 && recipe.resultPatch().equals(Optional.of(InfusionResultPatches.ADVANCED_GOLEM)),
                    "original ADVANCEDGOLEM: mens8 sensus8 victus8, instability 3, five components, patching its centre");
            var jar = new ItemStack(item("jar"));
            List<ItemStack> components = List.of(new ItemStack(Items.REDSTONE), new ItemStack(Items.GLOWSTONE_DUST),
                    new ItemStack(Items.GUNPOWDER), jar, new ItemStack(item("zombie_brain")));
            for (var material : List.of("straw", "wood", "tallow", "clay", "flesh", "stone", "iron", "thaumium")) {
                var center = new ItemStack(item("golem_" + material));
                test.assertTrue(recipe.matches(new InfusionRecipeInput(center, components), level), material + " placer is an accepted centre");
            }
            var offered = new InfusionRecipeInput(new ItemStack(item("golem_wood")), components);
            test.assertTrue(!recipe.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON), components), level), "a piston is not a golem placer");
            test.assertTrue(!recipe.matches(new InfusionRecipeInput(new ItemStack(item("golem_core_fill")), components), level), "a golem core is not a golem placer");
            for (int missing = 0; missing < components.size(); missing++) {
                var fewer = new ArrayList<>(components);
                fewer.remove(missing);
                test.assertTrue(!recipe.matches(new InfusionRecipeInput(offered.central(), fewer), level), "all five components are needed: without #" + missing);
            }
            var extra = new ArrayList<>(components);
            extra.add(new ItemStack(Items.REDSTONE));
            test.assertTrue(!recipe.matches(new InfusionRecipeInput(offered.central(), extra), level), "an extra component is refused");
            var filledJar = thaumcraft.common.items.ItemJar.withContents(new ItemStack(item("jar")), thaumcraft.api.aspects.Aspect.AIR, 4);
            var withFilled = new ArrayList<>(components);
            withFilled.set(3, filledJar);
            test.assertTrue(!recipe.matches(new InfusionRecipeInput(offered.central(), withFilled), level), "the jar must be empty");

            // The ingredient already limits this recipe to placers; the patch guard must hold on its own as well.
            var loose = new InfusionStackRecipe("", net.minecraft.world.item.crafting.Ingredient.of(Items.PISTON),
                    List.of(net.minecraft.world.item.crafting.Ingredient.of(Items.REDSTONE)), Map.of("aer", 1), 0,
                    new ItemStack(item("golem_straw")), List.of(), Optional.empty(), Optional.of(InfusionResultPatches.ADVANCED_GOLEM));
            test.assertTrue(!loose.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON), List.of(new ItemStack(Items.REDSTONE))), level),
                    "a recipe that patches its centre refuses a centre the patch cannot be applied to, whatever its ingredient says");
            test.assertTrue(loose.assemble(new InfusionRecipeInput(new ItemStack(Items.PISTON), List.of(new ItemStack(Items.REDSTONE))),
                    level.registryAccess()).isEmpty(), "and would assemble nothing from it");

            // The sample shown in the book is the patched placer; the real output patches the actual centre.
            var shown = recipe.getResultItem(level.registryAccess());
            test.assertTrue(shown.is(item("golem_straw")) && shown.get(ThaumcraftGolems.GOLEM_DATA.get()).advanced(), "the book shows an advanced placer");
            var named = placer(item("golem_wood"), GolemData.FILL, List.of(EARTH), false);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Rusty"));
            var output = recipe.assemble(new InfusionRecipeInput(named, components), level.registryAccess());
            var data = output.get(ThaumcraftGolems.GOLEM_DATA.get());
            test.assertTrue(output.is(item("golem_wood")) && output.getCount() == 1 && data.advanced() && data.core() == GolemData.FILL
                            && data.upgrades().equals(List.of(EARTH)) && Component.literal("Rusty").equals(output.get(DataComponents.CUSTOM_NAME)),
                    "the centre keeps its material, core, upgrades and name and gains the flag: " + output);
            test.assertTrue(!named.get(ThaumcraftGolems.GOLEM_DATA.get()).advanced(), "the offered stack itself is not modified");
            var again = recipe.assemble(new InfusionRecipeInput(output, components), level.registryAccess());
            test.assertTrue(ItemStack.matches(again, output), "infusing an advanced placer again is idempotent");
            var bare = recipe.assemble(new InfusionRecipeInput(new ItemStack(item("golem_iron")), components), level.registryAccess());
            test.assertTrue(bare.is(item("golem_iron")) && bare.get(ThaumcraftGolems.GOLEM_DATA.get()).equals(GolemData.EMPTY.withAdvanced(true)),
                    "a placer without data becomes a bare advanced placer");
            test.assertTrue(recipe.assemble(new InfusionRecipeInput(new ItemStack(Items.PISTON), components), level.registryAccess()).isEmpty(),
                    "the patch cannot be applied to a non-placer");

            // JSON and wire keep the patch; an unknown patch is refused; ordinary recipes have none.
            var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            JsonObject json = InfusionStackRecipe.CODEC.codec().encodeStart(ops, recipe).getOrThrow().getAsJsonObject();
            test.assertTrue(json.get("result_patch").getAsString().equals("advanced_golem"), "the JSON form names the patch");
            test.assertTrue(InfusionStackRecipe.CODEC.codec().parse(ops, json).getOrThrow().resultPatch().equals(recipe.resultPatch()), "JSON round trip");
            var bad = json.deepCopy();
            bad.addProperty("result_patch", "no_such_patch");
            test.assertTrue(InfusionStackRecipe.CODEC.codec().parse(ops, bad).error().isPresent(), "an unknown patch is refused by the codec");
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            InfusionStackRecipe.Serializer.STREAM_CODEC.encode(wire, recipe);
            var decoded = InfusionStackRecipe.Serializer.STREAM_CODEC.decode(wire);
            test.assertTrue(decoded.resultPatch().equals(recipe.resultPatch()) && decoded.assemble(offered, level.registryAccess())
                            .get(ThaumcraftGolems.GOLEM_DATA.get()).advanced(), "the wire keeps the patch and still patches the centre");
            wire.release();
            var lumber = (InfusionStackRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/golem_core_lumber")).orElseThrow().value();
            test.assertTrue(lumber.resultPatch().isEmpty() && !InfusionStackRecipe.CODEC.codec().encodeStart(ops, lumber).getOrThrow().getAsJsonObject().has("result_patch"),
                    "ordinary recipes carry no patch and keep their JSON form");
        }

        // ---- real golems ------------------------------------------------------------------------------------------

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

        private EntityGolemBase golem(int material, int core, List<Integer> upgrades, boolean advanced) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(material);
            golem.applyConfiguration(config(core, upgrades, advanced));
            golem.setPos(Vec3.atBottomCenterOf(base));
            test.assertTrue(level.addFreshEntity(golem), "the golem is admitted");
            spawned.add(golem);
            return golem;
        }

        private void hold(GameType mode, ItemStack stack) {
            player.setGameMode(mode);
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setPos(Vec3.atBottomCenterOf(base).add(1.2, 0, 0));
        }

        private InteractionResult place(ItemStack placer) {
            hold(GameType.SURVIVAL, placer);
            var floor = base.below();
            var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false);
            return placer.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        }

        private List<EntityGolemBase> golemsNear() {
            return level.getEntitiesOfClass(EntityGolemBase.class, lane, golem -> !existing.contains(golem.getUUID()) && golem.isAlive());
        }

        private List<ItemStack> items(Item item) {
            return level.getEntitiesOfClass(ItemEntity.class, lane, entity -> !existing.contains(entity.getUUID()) && entity.getItem().is(item))
                    .stream().map(entity -> entity.getItem().copy()).toList();
        }

        private void clearGolems() {
            for (Entity entity : spawned) entity.discard();
            spawned.clear();
            golemsNear().forEach(Entity::discard);
            level.getEntitiesOfClass(ItemEntity.class, lane, item -> !existing.contains(item.getUUID())).forEach(Entity::discard);
        }

        private List<Integer> slots(EntityGolemBase golem) {
            List<Integer> result = new ArrayList<>();
            for (int slot = 0; slot < golem.getUpgradeSlotCount(); slot++) result.add(golem.getUpgrade(slot));
            return result;
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

        /** Capacity is material + 1 when advanced, through the real placer; never silently truncated. */
        private void placerSlots() {
            var thaumium = item("golem_thaumium");
            var all = List.of(EARTH, AIR, WATER);
            var refused = placer(thaumium, GolemData.NO_CORE, all, false);
            test.assertTrue(place(refused) == InteractionResult.FAIL && player.getMainHandItem().getCount() == 1 && golemsNear().isEmpty(),
                    "an ordinary Thaumium placer with three upgrades is refused, kept, and places nothing");
            test.assertTrue(place(placer(thaumium, GolemData.NO_CORE, all, true)) == InteractionResult.CONSUME && player.getMainHandItem().isEmpty(),
                    "the same three upgrades on an advanced Thaumium placer place and are paid");
            var placed = golemsNear();
            test.assertTrue(placed.size() == 1, "exactly one golem was placed");
            var golem = placed.getFirst();
            spawned.add(golem);
            test.assertTrue(golem.isAdvanced() && golem.getUpgradeSlotCount() == 3 && slots(golem).equals(all),
                    "the placed golem is advanced and holds all three upgrades: " + slots(golem));
            clearGolems();

            var iron = item("golem_iron");
            test.assertTrue(place(placer(iron, GolemData.NO_CORE, List.of(EARTH, AIR), true)) == InteractionResult.CONSUME,
                    "an advanced Iron placer (1 + 1 slots) accepts two upgrades");
            clearGolems();
            test.assertTrue(place(placer(iron, GolemData.NO_CORE, all, true)) == InteractionResult.FAIL && golemsNear().isEmpty(),
                    "but not three: the extra slot is exactly one");
            test.assertTrue(place(placer(iron, GolemData.NO_CORE, List.of(EARTH, AIR), false)) == InteractionResult.FAIL && golemsNear().isEmpty(),
                    "and an ordinary Iron placer still has one slot");
        }

        /** The native upgrade interaction fills the extra slot and keeps refusing a third upgrade of one kind. */
        private void nativeInstall() {
            var wood = golem(WOOD, GolemData.NO_CORE, List.of(), true);
            test.assertTrue(wood.getUpgradeSlotCount() == 2 && slots(wood).equals(List.of(EMPTY, EMPTY)), "a fresh advanced Wood golem has two slots");
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.upgradeItem(GolemUpgrade.AIR), 3));
            test.assertTrue(wood.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && slots(wood).equals(List.of(AIR, EMPTY)),
                    "the first upgrade installs in slot 0");
            test.assertTrue(wood.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && slots(wood).equals(List.of(AIR, AIR)),
                    "the second goes into the advanced golem's extra slot");
            test.assertTrue(player.getMainHandItem().getCount() == 1, "two items were paid");
            test.assertTrue(wood.interact(player, InteractionHand.MAIN_HAND) != InteractionResult.CONSUME && slots(wood).equals(List.of(AIR, AIR))
                            && player.getMainHandItem().getCount() == 1, "no third Air and no free slot: nothing installs, nothing is paid");
            clearGolems();
            var plain = golem(WOOD, GolemData.NO_CORE, List.of(), false);
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.upgradeItem(GolemUpgrade.AIR), 2));
            test.assertTrue(plain.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME
                            && plain.interact(player, InteractionHand.MAIN_HAND) != InteractionResult.CONSUME && slots(plain).equals(List.of(AIR)),
                    "an ordinary Wood golem still has only one");
            clearGolems();
        }

        /** Speed x1.1 after Air, range +20% (at least 2) and attack delay 18, decided by the real golem. */
        private void effects() {
            var type = EnumGolemType.getType(THAUMIUM);
            var advanced = golem(THAUMIUM, GolemData.GUARD, List.of(AIR), true);
            test.assertTrue((float) advanced.getAttributeValue(Attributes.MOVEMENT_SPEED) == GolemUpgrades.speed(type.speed, 1, true)
                            && GolemUpgrades.speed(type.speed, 1, true) == type.speed * (1.0F + 0.15F) * 1.1F,
                    "Air then x1.1: the attribute equals the original product");
            var ordinary = golem(THAUMIUM, GolemData.GUARD, List.of(AIR), false);
            test.assertTrue((float) ordinary.getAttributeValue(Attributes.MOVEMENT_SPEED) == GolemUpgrades.speed(type.speed, 1)
                            && advanced.getAttributeValue(Attributes.MOVEMENT_SPEED) > ordinary.getAttributeValue(Attributes.MOVEMENT_SPEED),
                    "an ordinary golem is unchanged and slower");
            test.assertTrue(advanced.getAttackSpeed() == 18 && ordinary.getAttackSpeed() == 20, "attack delay 18 against 20");
            ordinary.discard();
            clearGolems();

            // Range, decided by a real Guard for real spiders at fixed distances from its home.
            var guard = golem(TALLOW, GolemData.GUARD, List.of(), true);
            guard.setHome(base, Direction.UP);
            int[] distances = {18, 19, 20, 23, 24};
            // The row reaches up to two chunks from the home, wherever the random GameTest origin puts the borders. A chunk loaded
            // only by level.getChunk shows its entities a task later, and a spider the level cannot find yet is rightly refused by
            // the Guard, so the row's chunks are forced and awaited until they tick entities.
            for (int distance : distances) {
                var chunk = new ChunkPos(base.offset(distance, 0, 0));
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk); }
            }
            for (int distance : distances) GameTestChunkAwait.entityTicking(test, base.offset(distance, 0, 0));
            List<Mob> targets = new ArrayList<>();
            for (int distance : distances) {
                BlockPos pos = base.offset(distance, 0, 0);
                var spider = EntityType.SPIDER.create(level);
                spider.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
                spider.setNoAi(true);
                spider.setPersistenceRequired();
                test.assertTrue(ChampionJoinRoll.join(level, spider) && spider.isAlive() && level.hasChunkAt(pos), "a real hostile spider stands " + distance + " blocks from the home");
                attackers.add(spider);
                targets.add(spider);
            }
            test.assertTrue(guard.getRange() == 16F + 3.2F && view(guard, targets).equals(List.of(true, true, false, false, false)),
                    "advanced, no Water: range 19.2, spiders at 18 and 19 are targets and 20 is out: " + view(guard, targets));
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.upgradeItem(GolemUpgrade.WATER), 1));
            test.assertTrue(guard.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME
                            && guard.getRange() == 20F + 4F && view(guard, targets).equals(List.of(true, true, true, true, false)),
                    "advanced with one Water: 20 + max(4, 2) = 24, spiders up to 23 are targets and 24 is out: " + view(guard, targets));
            var plainGuard = golem(TALLOW, GolemData.GUARD, List.of(), false);
            plainGuard.setHome(base, Direction.UP);
            test.assertTrue(plainGuard.getRange() == 16F && view(plainGuard, targets).equals(List.of(false, false, false, false, false)),
                    "the same golem without the flag only sees 15: no spider here is a target");
            clearGolems();
        }

        private List<Boolean> view(EntityGolemBase guard, List<Mob> targets) {
            List<Boolean> view = new ArrayList<>();
            for (var target : targets) view.add(guard.isGuardTarget(target));
            return view;
        }

        private void persistence() {
            var advanced = golem(THAUMIUM, GolemData.NO_CORE, List.of(EARTH, AIR, WATER), true);
            var tag = save(advanced);
            test.assertTrue(tag.getBoolean("advanced") && java.util.Arrays.equals(tag.getByteArray("upgrades"), new byte[]{1, 0, 3}),
                    "saved under the original keys: advanced = true and a three-byte upgrade array");
            var loaded = load(tag);
            test.assertTrue(loaded.isAdvanced() && slots(loaded).equals(List.of(EARTH, AIR, WATER)) && loaded.getRange() == advanced.getRange()
                            && (float) loaded.getAttributeValue(Attributes.MOVEMENT_SPEED) == (float) advanced.getAttributeValue(Attributes.MOVEMENT_SPEED)
                            && loaded.configuredData().advanced(),
                    "a reloaded golem keeps the flag, three upgrades, range and speed");
            test.assertTrue(save(loaded).getBoolean("advanced"), "and saves them again");
            loaded.discard();

            var ordinary = golem(THAUMIUM, GolemData.NO_CORE, List.of(EARTH), false);
            test.assertTrue(!save(ordinary).contains("advanced"), "an ordinary golem writes no advanced key: earlier saves keep their form");
            var tooMany = save(advanced);
            tooMany.remove("advanced");
            var unfit = load(tooMany);
            boolean refused;
            try { unfit.configuredData(); refused = false; } catch (IllegalStateException expected) { refused = true; }
            test.assertTrue(refused, "three upgrades without the flag do not fit Thaumium: the golem stays inert instead of losing one");
            unfit.discard();
            var malformed = save(advanced);
            malformed.putByte("advanced", (byte) 2);
            var broken = load(malformed);
            try { broken.configuredData(); refused = false; } catch (IllegalStateException expected) { refused = true; }
            test.assertTrue(refused, "a flag that is not 0 or 1 is kept raw and inert, never read as true");
            broken.discard();
            clearGolems();
        }

        private void recallAndDismantling() {
            var golem = golem(TALLOW, GolemData.GUARD, List.of(EARTH, AIR, WATER), true);
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.GOLEM_BELL.get()));
            test.assertTrue(golem.recall(player, false), "the bell recalls the advanced golem");
            DeferredItemDrops.tick(level);
            var returned = items(item("golem_tallow"));
            test.assertTrue(returned.size() == 1 && returned.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).advanced()
                            && returned.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).upgrades().equals(List.of(EARTH, AIR, WATER))
                            && returned.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).core() == GolemData.GUARD,
                    "the recalled placer keeps the flag, the core and all three upgrades: " + returned);
            clearGolems();

            var dismantled = golem(TALLOW, GolemData.GUARD, List.of(EARTH, AIR, WATER), true);
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.GOLEM_BELL.get()));
            player.setShiftKeyDown(true);
            test.assertTrue(dismantled.recall(player, true), "the sneaking bell dismantles the advanced golem");
            DeferredItemDrops.tick(level);
            var bare = items(item("golem_tallow"));
            test.assertTrue(bare.size() == 1 && bare.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).equals(GolemData.EMPTY.withAdvanced(true)),
                    "the bare placer is still advanced (ItemGolemBell:272): " + bare);
            test.assertTrue(items(item("golem_core_guard")).size() == 1, "and the core comes back as its own item");
            clearGolems();
            player.setShiftKeyDown(false);

            var plain = golem(TALLOW, GolemData.NO_CORE, List.of(), false);
            hold(GameType.SURVIVAL, new ItemStack(ThaumcraftGolems.GOLEM_BELL.get()));
            player.setShiftKeyDown(true);
            test.assertTrue(plain.recall(player, true), "an ordinary golem is dismantled too");
            DeferredItemDrops.tick(level);
            var plainBare = items(item("golem_tallow"));
            test.assertTrue(plainBare.size() == 1 && !plainBare.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).advanced()
                            && plainBare.getFirst().get(ThaumcraftGolems.GOLEM_DATA.get()).equals(GolemData.EMPTY),
                    "and its bare placer is the plain default configuration, not advanced");
            player.setShiftKeyDown(false);
            clearGolems();
        }

        private void tooltip() {
            var lines = placer(item("golem_wood"), GolemData.NO_CORE, List.of(), true)
                    .getTooltipLines(Item.TooltipContext.of(level), player, TooltipFlag.Default.NORMAL);
            test.assertTrue(lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents contents && contents.getKey().equals("tc.adv")),
                    "the advanced placer lists tc.adv");
            var plainLines = placer(item("golem_wood"), GolemData.NO_CORE, List.of(), false)
                    .getTooltipLines(Item.TooltipContext.of(level), player, TooltipFlag.Default.NORMAL);
            test.assertTrue(plainLines.stream().noneMatch(line -> line.getContents() instanceof TranslatableContents contents && contents.getKey().equals("tc.adv")),
                    "an ordinary placer does not");
        }

        @Override public void close() {
            try {
                clearGolems();
                attackers.forEach(Entity::discard);
                if (touched) original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            } finally {
                try {
                    for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
                    if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                } finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }
    }
}
