package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.ExperiencePoints;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** Declared altar/skulls/essentia/XP points are fixtures; all matching, XP and physical ingredient payments use the real matrix. */
public final class EnchantmentInfusionRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private EnchantmentInfusionRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); check.observeNatural(); } catch (RuntimeException | Error failure) { check.failed(failure); throw failure; }
        test.runAfterDelay(5, () -> {
            if (check.started) return;
            check.started = true;
            try { check.stage = "focused run"; check.run(); check.stage = "natural start"; check.startNatural(); }
            catch (RuntimeException | Error failure) { check.failed(failure); throw failure; }
        });
    }
    /** Focused SERVER state checks; inventory, points and an invulnerable payer are declared fixtures. */
    public static void runPointPayments(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); } catch (RuntimeException | Error failure) { check.failed(failure); throw failure; }
        test.runAfterDelay(5, () -> {
            if (check.started) return;
            check.started = true;
            try {
                check.stage = "fixed point payments and saved debt";
                check.pointPayments(); check.finish(); test.succeed();
            } catch (RuntimeException | Error failure) { check.failed(failure); throw failure; }
        });
    }
    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos centerPos, matrixPos;
        final Map<BlockPos, BlockState> oldStates = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final ServerPlayer player;
        TileInfusionMatrix matrix;
        TilePedestal center, first, second;
        TileJar magic, weapon, armor;
        boolean started, naturalStarted, finished;
        String stage = "prepare";
        Check(GameTestHelper test) {
            this.test = test; level = test.getLevel(); centerPos = test.absolutePos(new BlockPos(3, 80, 3)); matrixPos = centerPos.above(2);
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "ench-" + UUID.randomUUID().toString().substring(0, 8)), ClientInformation.createDefault());
        }
        void set(BlockPos pos, BlockState state) { oldStates.putIfAbsent(pos, level.getBlockState(pos)); level.setBlock(pos, state, QUIET); }
        void prepare() {
            forced.addAll(GolemChunks.hold(level, List.of(centerPos.offset(-13, 0, -13), centerPos.offset(13, 3, 13))));
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                set(centerPos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                set(centerPos.offset(x, 0, z), Math.abs(x) <= 1 && Math.abs(z) <= 1 ? Blocks.AIR.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
            }
            set(centerPos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            set(matrixPos, ThaumcraftInfusion.MATRIX.get().defaultBlockState());
            for (int x : new int[]{-1,1}) for (int z : new int[]{-1,1}) {
                set(centerPos.offset(x, 0, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.LOWER));
                set(centerPos.offset(x, 1, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER));
            }
            set(centerPos.west(3), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            set(centerPos.east(3), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            set(centerPos.offset(-8, 0, 9), ThaumcraftContent.JAR.get().defaultBlockState());
            set(centerPos.offset(-7, 0, 9), ThaumcraftContent.JAR.get().defaultBlockState());
            set(centerPos.offset(-6, 0, 9), ThaumcraftContent.JAR.get().defaultBlockState());
            center = (TilePedestal) level.getBlockEntity(centerPos);
            first = (TilePedestal) level.getBlockEntity(centerPos.west(3)); second = (TilePedestal) level.getBlockEntity(centerPos.east(3));
            matrix = (TileInfusionMatrix) level.getBlockEntity(matrixPos);
            magic = (TileJar) level.getBlockEntity(centerPos.offset(-8, 0, 9)); weapon = (TileJar) level.getBlockEntity(centerPos.offset(-7, 0, 9));
            armor = (TileJar) level.getBlockEntity(centerPos.offset(-6, 0, 9));
        }
        void tick(int ticks) { for (int i = 0; i < ticks; i++) TileInfusionMatrix.tick(level, matrixPos, level.getBlockState(matrixPos), matrix); }
        void run() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setPos(Vec3.atCenterOf(matrixPos).add(0, 0, 2)); player.giveExperiencePoints(40);
            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get()); player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            var hit = new BlockHitResult(Vec3.atCenterOf(matrixPos), Direction.UP, matrixPos, false);
            boolean formed = thaumcraft.common.lib.infusion.InfusionStructure.isValid(level, matrixPos);
            var action = wand.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            test.assertTrue(action.consumesAction() && matrix.isActive(), "the real wand activates a genuine formed altar without recipe payment: action=" + action
                    + ", formed=" + formed + ", active=" + matrix.isActive() + ", current=" + (level.getBlockEntity(matrixPos) == matrix)
                    + ", alive=" + player.isAlive() + ", reach=" + player.canInteractWithBlock(matrixPos, 0)
                    + ", build=" + player.mayUseItemAt(matrixPos, hit.getDirection(), wand)
                    + ", native=" + thaumcraft.api.visnet.VisNetHandler.isNativeServerLevel(level));
            var sharp = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
            var smite = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SMITE);
            var unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
            var recipes = level.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.ENCHANTMENT_RECIPE_TYPE.get());
            test.assertTrue(recipes.size() >= 22, "all twenty-two original vanilla enchantment recipes are native");
            InfusionEnchantmentRecipe recipe = recipes.stream().map(h -> h.value()).filter(r -> r.enchantment().equals(ResourceLocation.withDefaultNamespace("sharpness"))).findFirst().orElseThrow();
            ItemStack salis = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "salis_mundus")));
            var components = List.of(new ItemStack(Items.IRON_SWORD), salis);
            ItemStack input = new ItemStack(Items.IRON_SWORD); input.enchant(sharp, 1);
            test.assertTrue(recipe.matches(new InfusionRecipeInput(input, components), level) && recipe.xpLevels(input, level.registryAccess()) == 2 && recipe.xpPoints(input, level.registryAccess()) == 16
                    && recipe.aspectCost(input, level.registryAccess()).getAmount(Aspect.MAGIC) == 8 && recipe.aspectCost(input, level.registryAccess()).getAmount(Aspect.WEAPON) == 16,
                    "raising Sharpness I costs 16 fixed XP points (two nominal levels) and double base essentia");
            ItemStack incompatible = new ItemStack(Items.IRON_SWORD); incompatible.enchant(smite, 1);
            ItemStack maximum = new ItemStack(Items.IRON_SWORD); maximum.enchant(sharp, 5);
            test.assertTrue(!recipe.matches(new InfusionRecipeInput(incompatible, components), level) && !recipe.matches(new InfusionRecipeInput(maximum, components), level)
                    && !recipe.matches(new InfusionRecipeInput(new ItemStack(Items.BOW), components), level), "mutual exclusions, maximum level and wrong item type refuse admission");
            ItemStack other = input.copy(); other.enchant(unbreaking, 3);
            test.assertTrue(recipe.calculatedInstability(other) == 4 && recipe.aspectCost(other, level.registryAccess()).getAmount(Aspect.MAGIC) == 9,
                    "all enchantment levels count for instability and another enchantment adds only its original tenth-per-level surcharge");
            codecs(recipe);
            center.setItem(0, input); first.setItem(0, components.get(0).copy()); second.setItem(0, salis.copy());
            test.assertTrue(magic.addToContainer(Aspect.MAGIC, 32) == 0 && weapon.addToContainer(Aspect.WEAPON, 32) == 0
                    && armor.addToContainer(Aspect.ARMOR, 32) == 0, "finite essentia inputs for both rituals are present before source discovery");
            test.assertTrue(!matrix.startCrafting(player) && !matrix.isCrafting() && hasPoints(40) && magic.getAmount() == 32,
                    "unresearched start pays nothing");
            KnowledgeAccess.get(player).addResearch("INFUSIONENCHANTMENT");
            test.assertTrue(wand.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction()
                    && matrix.isCrafting() && matrix.getRecipeType() == 1 && matrix.getRemainingXPPoints() == 16
                    && matrix.getInstability() < 0 && hasPoints(40) && first.getItem(0).is(Items.IRON_SWORD),
                    "the real wand starts the enchantment ritual; no upfront ingredient, XP or essentia debit");
            player.setPos(Vec3.atCenterOf(matrixPos).add(30, 0, 0)); tick(30);
            test.assertTrue(matrix.getRemainingXPPoints() == 16 && magic.getAmount() == 32 && weapon.getAmount() == 32,
                    "with no player in range the XP phase stalls without shortage rolls or essentia consumption");
            player.setPos(Vec3.atCenterOf(matrixPos).add(0, 0, 2)); tick(10);
            test.assertTrue(hasPoints(32) && matrix.getRemainingXPPoints() == 8 && matrix.getCycleDelay() == 20
                    && magic.getAmount() == 32 && weapon.getAmount() == 32 && !first.getItem(0).isEmpty(), "one live cycle consumes the fixed 8-point quantum before any other resources");
            CompoundTag saved = matrix.saveWithFullMetadata(level.registryAccess());
            test.assertTrue(saved.getInt("recipexp") == 8 && saved.getString("RecipeEnchantment").equals("minecraft:sharpness"), "paid XP progress and enchantment identity persist");
            TileInfusionMatrix stale = matrix;
            level.removeBlockEntity(matrixPos); matrix = new TileInfusionMatrix(matrixPos, level.getBlockState(matrixPos)); matrix.loadWithComponents(saved, level.registryAccess()); level.setBlockEntity(matrix);
            test.assertTrue(matrix.getRemainingXPPoints() == 8 && matrix.getCycleDelay() == 10 && !matrix.startCrafting(player) && !stale.startCrafting(player), "reload resumes the remaining payment without a fresh match");
            tick(10);
            test.assertTrue(hasPoints(24) && matrix.getRemainingXPPoints() == 0 && magic.getAmount() == 32 && weapon.getAmount() == 32, "only the remaining 8 points are paid after reload");
            CompoundTag retained = new CompoundTag(); retained.putString("addedDuringRitual", "preserved");
            center.getItem(0).set(DataComponents.CUSTOM_DATA, CustomData.of(retained)); center.getItem(0).setDamageValue(7);
            for (int i = 0; i < 1000 && matrix.isCrafting(); i++) tick(1);
            ItemStack result = center.getItem(0);
            test.assertTrue(!matrix.isCrafting() && result.is(Items.IRON_SWORD) && result.getEnchantments().getLevel(sharp) == 2 && result.getDamageValue() == 7
                    && result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("addedDuringRitual").equals("preserved"),
                    "the actual retained centre receives one enchantment level, with its current damage and added data preserved");
            test.assertTrue(hasPoints(24) && magic.getAmount() == 24 && weapon.getAmount() == 16 && first.isEmpty() && second.isEmpty(), "sixteen exact XP points, twenty-four real essentia units and both physical components were paid once");
            tick(40);
            test.assertTrue(center.getItem(0).getEnchantments().getLevel(sharp) == 2 && hasPoints(24) && magic.getAmount() == 24, "completed rituals never replay a reward or debit");
        }
        boolean hasPoints(long expected) { return ExperiencePoints.available(player) == expected && player.totalExperience == expected; }
        void points(int amount) {
            player.experienceLevel = 0; player.experienceProgress = 0; player.totalExperience = 0;
            player.giveExperiencePoints(amount);
            test.assertTrue(hasPoints(amount), "declared payer fixture receives exact native XP points");
        }
        void stopPointRitual() {
            CompoundTag saved = matrix.saveWithFullMetadata(level.registryAccess());
            saved.putBoolean("crafting", false);
            matrix.loadWithComponents(saved, level.registryAccess());
        }
        void startPointRitual() {
            stopPointRitual();
            var sharp = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
            ItemStack input = new ItemStack(Items.IRON_SWORD); input.enchant(sharp, 1);
            center.setInfusionStack(input); first.setInfusionStack(new ItemStack(Items.IRON_SWORD));
            second.setInfusionStack(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "salis_mundus"))));
            long before = ExperiencePoints.available(player); int totalBefore = player.totalExperience;
            test.assertTrue(matrix.startCrafting(player) && matrix.isCrafting() && matrix.getRemainingXPPoints() == 16
                    && ExperiencePoints.available(player) == before && player.totalExperience == totalBefore,
                    "the same Sharpness I to II ritual starts with sixteen point debt and no upfront payment");
        }
        void drainPointDebt() {
            for (int i = 0; i < 120 && matrix.getRemainingXPPoints() > 0; i++) tick(1);
            test.assertTrue(matrix.getRemainingXPPoints() == 0 && matrix.isCrafting()
                    && first.getItem(0).is(Items.IRON_SWORD) && !second.isEmpty()
                    && magic.getAmount() == 0 && weapon.getAmount() == 0,
                    "the point phase finishes before any ingredient or essence payment; no free output is produced");
        }
        void reloadPointRitual(CompoundTag saved) {
            level.removeBlockEntity(matrixPos);
            matrix = new TileInfusionMatrix(matrixPos, level.getBlockState(matrixPos));
            matrix.loadWithComponents(saved, level.registryAccess()); level.setBlockEntity(matrix);
        }
        void pointPayments() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setInvulnerable(true);
            player.setPos(Vec3.atCenterOf(matrixPos).add(0, 0, 2));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.WAND.get()));
            var hit = new BlockHitResult(Vec3.atCenterOf(matrixPos), Direction.UP, matrixPos, false);
            test.assertTrue(player.getMainHandItem().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction()
                    && matrix.isActive(), "actual wand activates the point-payment fixture's formed altar");
            KnowledgeAccess.get(player).addResearch("INFUSIONENCHANTMENT");
            for (int initial : new int[]{20, 1412}) {
                points(initial); int initialLevel = player.experienceLevel;
                test.assertTrue(player.experienceProgress > 0, "low/high payer fixtures both have a partial XP bar");
                startPointRitual();
                test.assertTrue(!matrix.startCrafting(player) && hasPoints(initial), "an active ritual rejects a duplicate start without payment");
                drainPointDebt();
                test.assertTrue(hasPoints(initial - 16) && player.experienceProgress > 0
                        && (initial == 20 ? initialLevel == 2 && player.experienceLevel == 0 : initialLevel == 30 && player.experienceLevel == 30),
                        "the same ritual takes exactly sixteen points from low and high levels while retaining partial bars");
            }
            points(1412); player.totalExperience = 0; startPointRitual(); drainPointDebt();
            test.assertTrue(ExperiencePoints.available(player) == 1396 && player.totalExperience == 0,
                    "a stale historical total zero does not block payment from the actual XP bar");
            player.setGameMode(GameType.CREATIVE); points(20); startPointRitual(); drainPointDebt();
            test.assertTrue(hasPoints(4), "the altar preserves the original creative payment rule and charges actual XP points");
            player.setGameMode(GameType.SURVIVAL); points(3); startPointRitual();
            test.assertTrue(player.experienceLevel == 0 && player.experienceProgress > 0, "a level-zero payer still has three spendable points");
            for (int i = 0; i < 40 && matrix.getRemainingXPPoints() == 16; i++) tick(1);
            test.assertTrue(hasPoints(0) && matrix.getRemainingXPPoints() == 13 && !first.isEmpty() && !second.isEmpty(),
                    "a smaller-than-quantum point bank pays exactly its three available points");
            tick(40);
            test.assertTrue(hasPoints(0) && matrix.getRemainingXPPoints() == 13 && matrix.isCrafting(),
                    "an empty point bank stalls without forgiving or increasing XP debt");
            CompoundTag paid = matrix.saveWithFullMetadata(level.registryAccess());
            test.assertTrue(paid.getString("RecipeXPUnit").equals("points") && paid.getInt("recipexp") == 13
                    && paid.getInt("RecipeXPPerCycle") == 8, "new saves preserve exact point debt and the original cycle quantum");
            TileInfusionMatrix stale = matrix; reloadPointRitual(paid);
            test.assertTrue(matrix.getRemainingXPPoints() == 13 && hasPoints(0) && !matrix.startCrafting(player) && !stale.startCrafting(player),
                    "a new point save reloads without another debit or replay admission");
            player.giveExperiencePoints(13);
            for (int i = 0; i < 40 && matrix.getRemainingXPPoints() == 13; i++) tick(1);
            test.assertTrue(matrix.getRemainingXPPoints() == 5 && hasPoints(5), "reload retains quantum eight even when remaining debt is thirteen");
            drainPointDebt(); test.assertTrue(hasPoints(0), "newly earned points resume and settle only the outstanding thirteen points");

            points(40); startPointRitual();
            CompoundTag legacy = matrix.saveWithFullMetadata(level.registryAccess());
            legacy.remove("RecipeXPUnit"); legacy.remove("RecipeXPPerCycle"); legacy.putInt("recipexp", 1);
            points(27); // Declared old save: level four payer has already lost one whole level and retains level three.
            reloadPointRitual(legacy);
            test.assertTrue(matrix.getRemainingXPPoints() == 7 && hasPoints(27),
                    "a legacy save converts only its one unpaid nominal level to seven points and charges nothing on load");
            CompoundTag migrated = matrix.saveWithFullMetadata(level.registryAccess());
            test.assertTrue(migrated.getString("RecipeXPUnit").equals("points") && migrated.getInt("recipexp") == 7,
                    "legacy migration persists an explicit point unit for subsequent reloads");
            reloadPointRitual(migrated);
            test.assertTrue(matrix.getRemainingXPPoints() == 7 && hasPoints(27), "reloading a migrated save never converts or charges it again");
            drainPointDebt(); test.assertTrue(hasPoints(20), "only seven remaining points are paid after legacy migration");
            legacy.putInt("recipexp", 0); reloadPointRitual(legacy);
            test.assertTrue(matrix.getRemainingXPPoints() == 0 && hasPoints(20), "a fully paid legacy save stays paid without restoring the original price");
        }
        void codecs(InfusionEnchantmentRecipe recipe) {
            var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            var json = InfusionEnchantmentRecipe.CODEC.codec().encodeStart(ops, recipe).getOrThrow();
            test.assertTrue(InfusionEnchantmentRecipe.CODEC.codec().parse(ops, json).getOrThrow().enchantment().equals(recipe.enchantment()), "JSON preserves enchantment recipe identity");
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                InfusionEnchantmentRecipe.Serializer.STREAM_CODEC.encode(wire, recipe);
                var decoded = InfusionEnchantmentRecipe.Serializer.STREAM_CODEC.decode(wire);
                test.assertTrue(decoded.enchantment().equals(recipe.enchantment()) && decoded.aspects().equals(recipe.aspects()) && decoded.firstLevelMinimumCost() == 1,
                        "native recipe packet preserves original XP and ordered aspect values");
            } finally { wire.release(); }
        }
        void startNatural() {
            // The second ritual runs entirely on ordinary block-entity/server ticks: no direct tick() calls after this start.
            player.getInventory().add(center.getItem(0).copy()); // Retain the first actual reward while declaring a fresh centre fixture.
            center.setItem(0, new ItemStack(Items.IRON_CHESTPLATE));
            first.setItem(0, new ItemStack(Items.IRON_INGOT));
            second.setItem(0, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "salis_mundus"))));
            // The original range-source list is cached, with a five-second WALL-CLOCK backoff after a miss.
            // All finite jars were therefore installed before the first ritual's source discovery.
            test.assertTrue(armor.getAmount() == 32 && armor.getEssentiaType(null) == Aspect.ARMOR,
                    "the first ritual leaves the declared armor essence untouched for the natural-tick ritual");
            var hit = new BlockHitResult(Vec3.atCenterOf(matrixPos), Direction.UP, matrixPos, false);
            test.assertTrue(player.getMainHandItem().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction()
                    && matrix.isCrafting() && matrix.getRemainingXPPoints() == 7, "the real wand starts a second ritual for ordinary world ticks");
            stage = "natural ticks";
            naturalStarted = true;
        }
        void observeNatural() {
            // Register BEFORE GameTestInfo begins iterating its scheduled-task map.
            // Inserting onEachTick tasks from the delayed start can rehash that map and replay its current task.
            test.onEachTick(() -> {
                if (finished || !naturalStarted) return;
                var protection = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
                try {
                    test.assertTrue(ExperiencePoints.available(player) + (7 - matrix.getRemainingXPPoints()) == 24
                            && player.totalExperience == ExperiencePoints.available(player),
                            "a natural XP payment remains paired with its session progress");
                    if (matrix.isCrafting()) return;
                    test.assertTrue(center.getItem(0).is(Items.IRON_CHESTPLATE) && center.getItem(0).getEnchantments().getLevel(protection) == 1
                            && hasPoints(17) && magic.getAmount() == 20 && weapon.getAmount() == 16 && armor.getAmount() == 24
                            && first.isEmpty() && second.isEmpty(), "ordinary server ticks enchant a real chestplate with exact 7-point XP, essentia and component payment");
                    finish(); test.succeed();
                } catch (RuntimeException | Error failure) { failed(failure); throw failure; }
            });
            test.runAfterDelay(650, () -> {
                if (finished) return;
                String state = "xp=" + matrix.getRemainingXPPoints() + ", demand=" + matrix.getDemand().visSize() + ", ingredients=" + matrix.getRemainingIngredientCount();
                finish(); test.fail("natural enchantment infusion did not finish: " + state);
            });
        }
        void failed(Throwable failure) {
            if (!finished) Thaumcraft.LOG.error("Enchantment infusion fixture first failure before cleanup, stage=" + stage, failure);
            finish();
        }
        void finish() {
            if (finished) return; finished = true;
            try {
                if (matrix != null && !matrix.isRemoved()) {
                    var tag = matrix.saveWithFullMetadata(level.registryAccess()); tag.putBoolean("crafting", false); tag.putBoolean("active", false); matrix.loadWithComponents(tag, level.registryAccess());
                }
                // These are declared fixtures; remove their contents before pedestal removal can evacuate them.
                for (TilePedestal pedestal : new TilePedestal[]{center, first, second}) {
                    if (pedestal != null && !pedestal.isRemoved()) pedestal.setInfusionStack(ItemStack.EMPTY);
                }
                oldStates.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                for (ChunkPos pos : forced) level.setChunkForced(pos.x, pos.z, false);
            } finally {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                player.discard(); channel.finishAndReleaseAll();
            }
        }
    }
}
