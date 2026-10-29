package thaumic.tinkerer.testing;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandHelper;
import thaumcraft.api.wands.WandRod;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.helper.ProjectileHelper;
import thaumic.tinkerer.common.item.foci.FociComponents;
import thaumic.tinkerer.common.item.foci.FociHooks;
import thaumic.tinkerer.common.item.foci.FociComponents.DislocationData;
import thaumic.tinkerer.common.item.foci.ItemFocusDeflect;
import thaumic.tinkerer.common.item.foci.ItemFocusDislocation;
import thaumic.tinkerer.common.item.foci.ItemFocusEnderChest;
import thaumic.tinkerer.common.item.foci.ItemFocusFlight;
import thaumic.tinkerer.common.item.foci.ItemFocusHeal;
import thaumic.tinkerer.common.item.foci.ItemFocusSmelt;
import thaumic.tinkerer.common.item.foci.ItemFocusTelekinesis;
import thaumic.tinkerer.common.item.foci.ItemModFocus;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRecipes;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>Runtime checks of the Foci group in a running server: registration, research and recipes with the numbers of 1.7.10,
 * the data of every focus, the casts of the seven foci with a real wand and a mock player (vis spent, motion, health, blocks
 * smelted, items pulled, projectiles shifted, the ender chest window, a chest taken and put down with its contents) and the
 * persistence of the Dislocation's load through saving the wand and the network, and the held foci through the real use tick of the wand.
 * They were written in a porting session without Gradle; they were run once in a scratch Fabric GameTest server (Fabric Loader's
 * own server entry, the classpath of the Gradle run configuration, only the Foci group of the add-on loaded), where all of them
 * passed after six mistakes of the tests themselves were corrected (a mock player's view follows its head rotation, and nothing
 * ticks a mock player, so the checks that need its use tick call {@code doTick} on every tick).</p>
 */
public final class FociRuntimeChecks {
    private FociRuntimeChecks() {}

    private static final List<String> FOCI = List.of(LibItemNames.FOCUS_DEFLECT, LibItemNames.FOCUS_DISLOCATION,
            LibItemNames.FOCUS_ENDER_CHEST, LibItemNames.FOCUS_FLIGHT, LibItemNames.FOCUS_HEAL, LibItemNames.FOCUS_SMELT,
            LibItemNames.FOCUS_TELEKINESIS);

    // ---- helpers ---------------------------------------------------------------------------------------------------

    /** A floor of stone at relative y = 0, so that the mock player and the items stand. */
    private static void floor(GameTestHelper test) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static ServerPlayer player(GameTestHelper test, double x, double y, double z, float yaw, float pitch) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        Vec3 at = test.absoluteVec(new Vec3(x, y, z));
        player.moveTo(at.x, at.y, at.z, yaw, pitch);
        // The view vector of a living entity follows its head, which moveTo leaves where it was.
        player.setYHeadRot(yaw);
        player.getInventory().clearContent();
        return player;
    }

    /** A staff of silverwood with a thaumium cap (25000 centivis of every primal fit, and the cap discounts), full or empty. */
    private static ItemStack wand(GameTestHelper test, String focusPath, boolean full) {
        return wand(test, focusPath, full, "silverwood_staff");
    }

    private static ItemStack wand(GameTestHelper test, String focusPath, boolean full, String rod) {
        ItemStack wand = ItemApi.getItem("itemWandCasting", 0);
        test.assertTrue(WandHelper.isWand(wand), "a wand can be made");
        WandHelper.setRod(wand, WandRod.byTag(rod));
        WandHelper.setCap(wand, WandCap.byTag("thaumium"));
        if (full) {
            for (Aspect primal : Aspect.getPrimalAspects()) {
                WandHelper.addRealVis(wand, primal, WandHelper.getMaxVis(wand), true);
            }
        }
        ItemStack focus = new ItemStack(TTRegistry.item(focusPath));
        test.assertTrue(WandHelper.setFocus(wand, focus), "the " + focusPath + " mounts on the wand");
        return wand;
    }

    /** The wand of {@link #wand} with an upgrade of the focus in a rank. */
    private static ItemStack upgraded(GameTestHelper test, ItemStack wand, FocusUpgradeType type, int rank) {
        ItemStack focus = WandHelper.getFocusItem(wand);
        test.assertTrue(((ItemFocusBasic) focus.getItem()).applyUpgrade(focus, type, rank), type.name + " fits rank " + rank);
        test.assertTrue(WandHelper.setFocus(wand, focus), "the upgraded focus mounts");
        return wand;
    }

    private static ItemStack roundTrip(GameTestHelper test, ItemStack stack) {
        Tag saved = stack.save(test.getLevel().registryAccess());
        return ItemStack.parse(test.getLevel().registryAccess(), saved).orElseThrow();
    }

    /** The stack as a client reads it: through the stream codec of the item stack with the registries of the server. */
    private static ItemStack wire(GameTestHelper test, ItemStack stack) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), test.getLevel().registryAccess());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    }

    /** What a cast of this cost takes from the wand: each amount times the modifier of the wand, truncated, times the casts. */
    private static int price(ItemStack wand, ServerPlayer player, Aspect aspect, int centivis, int times) {
        return (int) (centivis * WandHelper.getConsumptionModifier(wand, player, aspect, false)) * times;
    }

    private static void assertSpent(GameTestHelper test, ItemStack wand, ServerPlayer player, AspectList before,
            AspectList cost, int times, String what) {
        AspectList after = WandHelper.getAllVis(wand);
        for (Aspect primal : Aspect.getPrimalAspects()) {
            int expected = price(wand, player, primal, cost.getAmount(primal), times);
            int spent = before.getAmount(primal) - after.getAmount(primal);
            test.assertTrue(spent == expected, what + ": " + primal.getTag() + " spent " + spent + ", expected " + expected);
        }
    }

    private static void assertNothingSpent(GameTestHelper test, ItemStack wand, AspectList before, String what) {
        AspectList after = WandHelper.getAllVis(wand);
        for (Aspect primal : Aspect.getPrimalAspects()) {
            test.assertTrue(before.getAmount(primal) == after.getAmount(primal), what + ": " + primal.getTag() + " is unchanged");
        }
    }

    private static boolean near(Vec3 a, Vec3 b, double epsilon) {
        return a.distanceTo(b) <= epsilon;
    }

    private static BlockHitResult hit(GameTestHelper test, BlockPos relative, Direction face) {
        BlockPos pos = test.absolutePos(relative);
        return new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
    }

    // ---- registration, research, recipes --------------------------------------------------------------------------

    public static void registrationIsComplete(GameTestHelper test) {
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());

        for (String path : FOCI) {
            Item item = BuiltInRegistries.ITEM.getOptional(LibMisc.id(path)).orElse(null);
            test.assertTrue(item != null, "item " + path);
            test.assertTrue(item instanceof ItemFocusBasic, path + " is a wand focus");
            test.assertTrue(item instanceof ITTinkererRegisterable, path + " brings research and a recipe");
        }
        test.assertTrue(FociComponents.DISLOCATION != null && FociComponents.DISLOCATION.isPresent(),
                "the data component of the Dislocation");
        test.assertTrue(EntityType.EXPERIENCE_BOTTLE.is(ProjectileHelper.DEFLECT_BLACKLIST_TAG),
                "the data pack's blacklist tag holds the experience bottle");
        test.succeed();
    }

    /** research key, column, row, complexity, parent, mirrors needed, secondary */
    private record Entry(String path, String key, int column, int row, int complexity, String parent, boolean mirrors,
            boolean secondary) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry(LibItemNames.FOCUS_SMELT, LibResearch.KEY_FOCUS_SMELT, -2, -2, 2, "FOCUSEXCAVATION", false, false),
            new Entry(LibItemNames.FOCUS_FLIGHT, LibResearch.KEY_FOCUS_FLIGHT, -3, -4, 2, LibResearch.KEY_FOCUS_SMELT, false, false),
            new Entry(LibItemNames.FOCUS_DEFLECT, LibResearch.KEY_FOCUS_DEFLECT, -4, -3, 3, LibResearch.KEY_FOCUS_SMELT, true, true),
            new Entry(LibItemNames.FOCUS_DISLOCATION, LibResearch.KEY_FOCUS_DISLOCATION, -5, -5, 2, LibResearch.KEY_FOCUS_FLIGHT, true, true),
            new Entry(LibItemNames.FOCUS_TELEKINESIS, LibResearch.KEY_FOCUS_TELEKINESIS, -4, -6, 2, LibResearch.KEY_FOCUS_FLIGHT, false, true),
            new Entry(LibItemNames.FOCUS_HEAL, LibResearch.KEY_FOCUS_HEAL, -6, -4, 2, LibResearch.KEY_FOCUS_DEFLECT, true, true),
            new Entry(LibItemNames.FOCUS_ENDER_CHEST, LibResearch.KEY_FOCUS_ENDER_CHEST, -6, -2, 2, LibResearch.KEY_FOCUS_DEFLECT, true, false));

    public static void researchEntriesHaveTheOriginalPlaces(GameTestHelper test) {
        boolean mirrors = ThaumcraftApi.isMirrorsAllowed();
        for (Entry entry : ENTRIES) {
            ResearchItem research = ResearchCategories.getResearch(entry.key());
            if (entry.mirrors() && !mirrors) {
                test.assertTrue(research == null, entry.key() + " is not offered while Thaumcraft's mirrors are off");
                continue;
            }
            test.assertTrue(research != null, "research " + entry.key());
            test.assertTrue(LibResearch.CATEGORY_THAUMICTINKERER.equals(research.category), entry.key() + " is in the tab of the add-on");
            test.assertTrue(research.displayColumn == entry.column() && research.displayRow == entry.row(),
                    entry.key() + " at " + research.displayColumn + "," + research.displayRow);
            test.assertTrue(research.getComplexity() == entry.complexity(), entry.key() + " complexity " + research.getComplexity());
            test.assertTrue(research.isConcealed(), entry.key() + " is concealed");
            test.assertTrue(research.isSecondary() == entry.secondary(), entry.key() + " secondary " + research.isSecondary());
            test.assertTrue(research.parents != null && research.parents.length == 1 && research.parents[0].equals(entry.parent()),
                    entry.key() + " parents");
            test.assertTrue(research.getPages() != null && research.getPages().length >= 2, entry.key() + " has its pages");
        }
        if (mirrors) test.assertTrue(ThaumcraftApi.getWarp(LibResearch.KEY_FOCUS_ENDER_CHEST) == 1, "the Ender Rift brings one warp");
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        test.succeed();
    }

    private record Infusion(String key, int instability, Map<Aspect, Integer> aspects, int components) {}

    public static void recipesHaveTheOriginalNumbers(GameTestHelper test) {
        List<Infusion> infusions = List.of(
                new Infusion(LibResearch.KEY_FOCUS_FLIGHT, 3, Map.of(Aspect.AIR, 15, Aspect.MOTION, 20, Aspect.TRAVEL, 10), 7),
                new Infusion(LibResearch.KEY_FOCUS_DISLOCATION, 8, Map.of(Aspect.ELDRITCH, 20, Aspect.DARKNESS, 10,
                        Aspect.VOID, 25, Aspect.MAGIC, 20, Aspect.TAINT, 5), 8),
                new Infusion(LibResearch.KEY_FOCUS_TELEKINESIS, 5, Map.of(Aspect.MOTION, 10, Aspect.AIR, 20, Aspect.ENTROPY, 20,
                        Aspect.MIND, 10), 7),
                new Infusion(LibResearch.KEY_FOCUS_HEAL, 4, Map.of(Aspect.HEAL, 10, Aspect.SOUL, 10, Aspect.LIFE, 15), 4),
                new Infusion(LibResearch.KEY_FOCUS_DEFLECT, 5, Map.of(Aspect.AIR, 15, Aspect.ARMOR, 5, Aspect.ORDER, 20), 4));
        for (Infusion expected : infusions) {
            InfusionRecipe recipe = TTRecipes.get(expected.key(), InfusionRecipe.class);
            test.assertTrue(recipe.getInstability() == expected.instability(), expected.key() + " instability " + recipe.getInstability());
            test.assertTrue(recipe.getResearch().equals(expected.key()), expected.key() + " research " + recipe.getResearch());
            test.assertTrue(recipe.getComponents().length == expected.components(), expected.key() + " components " + recipe.getComponents().length);
            test.assertTrue(recipe.getAspects().size() == expected.aspects().size(), expected.key() + " aspects " + recipe.getAspects().aspects);
            for (Map.Entry<Aspect, Integer> aspect : expected.aspects().entrySet()) {
                test.assertTrue(recipe.getAspects().getAmount(aspect.getKey()) == aspect.getValue(),
                        expected.key() + " " + aspect.getKey().getTag());
            }
        }
        // The two arcane recipes: Efreet's Flame always, the Ender Rift while Thaumcraft's mirrors are allowed.
        IArcaneRecipe smelt = TTRecipes.get(LibResearch.KEY_FOCUS_SMELT, IArcaneRecipe.class);
        test.assertTrue(smelt.getResearch().equals(LibResearch.KEY_FOCUS_SMELT), "smelt research");
        test.assertTrue(smelt.getAspects().getAmount(Aspect.FIRE) == 10 && smelt.getAspects().getAmount(Aspect.ORDER) == 5
                && smelt.getAspects().getAmount(Aspect.ENTROPY) == 6, "smelt costs 10 Ignis, 5 Ordo, 6 Perditio");
        test.assertTrue(smelt.getRecipeOutput().is(TTRegistry.item(LibItemNames.FOCUS_SMELT)), "smelt output");
        if (ThaumcraftApi.isMirrorsAllowed()) {
            IArcaneRecipe ender = TTRecipes.get(LibResearch.KEY_FOCUS_ENDER_CHEST, IArcaneRecipe.class);
            test.assertTrue(ender.getAspects().getAmount(Aspect.ORDER) == 10 && ender.getAspects().getAmount(Aspect.ENTROPY) == 10,
                    "ender costs 10 Ordo, 10 Perditio");
            test.assertTrue(ender.getRecipeOutput().is(TTRegistry.item(LibItemNames.FOCUS_ENDER_CHEST)), "ender output");
        } else {
            test.assertTrue(!TTRecipes.has(LibResearch.KEY_FOCUS_ENDER_CHEST), "no Ender Rift recipe while mirrors are off");
        }
        test.succeed();
    }

    // ---- the data of the foci --------------------------------------------------------------------------------------

    private static ItemModFocus focus(String path) {
        return (ItemModFocus) TTRegistry.item(path);
    }

    private static void assertCost(GameTestHelper test, String path, Object... aspectsAndAmounts) {
        AspectList cost = focus(path).getVisCost(ItemStack.EMPTY);
        test.assertTrue(cost.size() == aspectsAndAmounts.length / 2, path + " cost has " + cost.size() + " aspects");
        for (int i = 0; i < aspectsAndAmounts.length; i += 2) {
            Aspect aspect = (Aspect) aspectsAndAmounts[i];
            test.assertTrue(cost.getAmount(aspect) == (Integer) aspectsAndAmounts[i + 1], path + " " + aspect.getTag());
        }
    }

    public static void focusesCarryTheOriginalNumbers(GameTestHelper test) {
        assertCost(test, LibItemNames.FOCUS_FLIGHT, Aspect.AIR, 15);
        assertCost(test, LibItemNames.FOCUS_DEFLECT, Aspect.ORDER, 8, Aspect.AIR, 4);
        assertCost(test, LibItemNames.FOCUS_HEAL, Aspect.EARTH, 45, Aspect.WATER, 45);
        assertCost(test, LibItemNames.FOCUS_SMELT, Aspect.FIRE, 45, Aspect.ENTROPY, 12);
        assertCost(test, LibItemNames.FOCUS_TELEKINESIS, Aspect.AIR, 5, Aspect.ENTROPY, 5);
        assertCost(test, LibItemNames.FOCUS_ENDER_CHEST, Aspect.ENTROPY, 100, Aspect.ORDER, 100);
        assertCost(test, LibItemNames.FOCUS_DISLOCATION, Aspect.ENTROPY, 500, Aspect.ORDER, 500, Aspect.EARTH, 100);

        // A cost is a copy that nobody changes.
        focus(LibItemNames.FOCUS_FLIGHT).getVisCost(ItemStack.EMPTY).add(Aspect.FIRE, 1000);
        assertCost(test, LibItemNames.FOCUS_FLIGHT, Aspect.AIR, 15);

        // Held: the foci that cost per tick, and Mending and Efreet's Flame; per tick: Distortion and Telekinesis.
        for (String path : List.of(LibItemNames.FOCUS_DEFLECT, LibItemNames.FOCUS_TELEKINESIS)) {
            test.assertTrue(focus(path).isVisCostPerTick(ItemStack.EMPTY) && focus(path).isUseItem(ItemStack.EMPTY), path + " per tick");
        }
        for (String path : List.of(LibItemNames.FOCUS_HEAL, LibItemNames.FOCUS_SMELT)) {
            test.assertTrue(!focus(path).isVisCostPerTick(ItemStack.EMPTY) && focus(path).isUseItem(ItemStack.EMPTY), path + " held");
        }
        for (String path : List.of(LibItemNames.FOCUS_FLIGHT, LibItemNames.FOCUS_DISLOCATION, LibItemNames.FOCUS_ENDER_CHEST)) {
            test.assertTrue(!focus(path).isVisCostPerTick(ItemStack.EMPTY) && !focus(path).isUseItem(ItemStack.EMPTY), path + " instant");
        }

        Map<String, Integer> colours = Map.of(LibItemNames.FOCUS_FLIGHT, 0x9EF2FF, LibItemNames.FOCUS_DISLOCATION, 0xFFB200,
                LibItemNames.FOCUS_ENDER_CHEST, 0x132223, LibItemNames.FOCUS_HEAL, 0xFD00D6, LibItemNames.FOCUS_SMELT, 0xFF0000,
                LibItemNames.FOCUS_TELEKINESIS, 0x9C00BE, LibItemNames.FOCUS_DEFLECT, 0xFFFFFF);
        Map<String, String> prefixes = Map.of(LibItemNames.FOCUS_FLIGHT, "TTFL", LibItemNames.FOCUS_DISLOCATION, "TTDL",
                LibItemNames.FOCUS_ENDER_CHEST, "TTEC", LibItemNames.FOCUS_HEAL, "TTHE", LibItemNames.FOCUS_SMELT, "TTEF",
                LibItemNames.FOCUS_TELEKINESIS, "TTTE", LibItemNames.FOCUS_DEFLECT, "TTDF");
        for (String path : FOCI) {
            ItemStack stack = TTRegistry.stack(path);
            test.assertTrue(focus(path).getFocusColor(stack) == colours.get(path), path + " colour");
            test.assertTrue(focus(path).getSortingHelper(stack).startsWith(prefixes.get(path)), path + " sorting " + focus(path).getSortingHelper(stack));
            test.assertTrue(stack.getRarity() == Rarity.RARE, path + " is rare");
            test.assertTrue(stack.getMaxStackSize() == 1, path + " stacks once");
            test.assertTrue(focus(path).getAnimation(stack) == ItemFocusBasic.WandFocusAnimation.WAVE, path + " waves");
        }

        // Textures of the wand: ornaments for four, depth layers for two, none for the Distortion.
        for (String path : List.of(LibItemNames.FOCUS_FLIGHT, LibItemNames.FOCUS_DISLOCATION, LibItemNames.FOCUS_SMELT,
                LibItemNames.FOCUS_TELEKINESIS)) {
            test.assertTrue(focus(path).getOrnament(TTRegistry.stack(path)) != null
                    && focus(path).getOrnament(TTRegistry.stack(path)).getPath().equals("textures/item/" + path + "_orn.png"), path + " ornament");
            test.assertTrue(focus(path).getFocusDepthLayerIcon(TTRegistry.stack(path)) == null, path + " has no depth layer");
        }
        for (String path : List.of(LibItemNames.FOCUS_HEAL, LibItemNames.FOCUS_ENDER_CHEST)) {
            test.assertTrue(focus(path).getFocusDepthLayerIcon(TTRegistry.stack(path)) != null
                    && focus(path).getFocusDepthLayerIcon(TTRegistry.stack(path)).getPath().equals("textures/item/" + path + "_depth.png"),
                    path + " depth layer");
            test.assertTrue(focus(path).getOrnament(TTRegistry.stack(path)) == null, path + " has no ornament");
        }
        ItemStack deflect = TTRegistry.stack(LibItemNames.FOCUS_DEFLECT);
        test.assertTrue(focus(LibItemNames.FOCUS_DEFLECT).getOrnament(deflect) == null
                && focus(LibItemNames.FOCUS_DEFLECT).getFocusDepthLayerIcon(deflect) == null, "the Distortion has neither");

        // Upgrades by rank.
        FocusUpgradeType frugal = FocusUpgradeType.frugal;
        for (String path : List.of(LibItemNames.FOCUS_FLIGHT, LibItemNames.FOCUS_DISLOCATION, LibItemNames.FOCUS_ENDER_CHEST,
                LibItemNames.FOCUS_SMELT, LibItemNames.FOCUS_TELEKINESIS)) {
            for (int rank = 1; rank <= 5; rank++) {
                FocusUpgradeType[] offered = focus(path).getPossibleUpgradesByRank(TTRegistry.stack(path), rank);
                test.assertTrue(offered != null && offered.length == 1 && offered[0] == frugal, path + " rank " + rank);
            }
            test.assertTrue(focus(path).getPossibleUpgradesByRank(TTRegistry.stack(path), 6) == null, path + " rank 6");
        }
        for (int rank = 1; rank <= 5; rank++) {
            FocusUpgradeType[] heal = focus(LibItemNames.FOCUS_HEAL).getPossibleUpgradesByRank(TTRegistry.stack(LibItemNames.FOCUS_HEAL), rank);
            test.assertTrue(heal != null && heal.length == 2 && heal[0] == frugal && heal[1] == FocusUpgradeType.potency, "heal rank " + rank);
            FocusUpgradeType[] shield = focus(LibItemNames.FOCUS_DEFLECT).getPossibleUpgradesByRank(deflect, rank);
            boolean even = rank % 2 == 0;
            test.assertTrue(shield != null && shield[0] == frugal && shield.length == (even ? 2 : 1)
                    && (!even || shield[1] == FocusUpgradeType.enlarge), "deflect rank " + rank);
        }
        test.succeed();
    }

    public static void everyFocusMountsOnAWandAndSurvivesSaving(GameTestHelper test) {
        for (String path : FOCI) {
            ItemStack wand = wand(test, path, true);
            test.assertTrue(WandHelper.getFocus(wand) == TTRegistry.item(path), path + " is the mounted focus");
            ItemStack loaded = roundTrip(test, wand);
            test.assertTrue(WandHelper.getFocus(loaded) == TTRegistry.item(path), path + " survives saving the wand");
            test.assertTrue(WandHelper.getVis(loaded, Aspect.AIR) == WandHelper.getVis(wand, Aspect.AIR), path + " keeps the vis");
        }
        // An upgrade the focus offers is kept; the Distortion's second rank can be enlarge.
        ItemStack wand = wand(test, LibItemNames.FOCUS_DEFLECT, true);
        ItemStack focusStack = WandHelper.getFocusItem(wand);
        test.assertTrue(focusStack.getItem() instanceof ItemFocusDeflect deflect && deflect.applyUpgrade(focusStack, FocusUpgradeType.enlarge, 2),
                "enlarge goes into rank 2 of the Distortion");
        test.assertTrue(WandHelper.setFocus(wand, focusStack), "the upgraded focus mounts");
        test.assertTrue(WandHelper.getFocusEnlarge(wand) == 1, "one level of enlarge");
        test.assertTrue(WandHelper.getFocusEnlarge(roundTrip(test, wand)) == 1, "and it survives saving");
        test.succeed();
    }

    public static void heldFociStartTheUseAndTheOthersDoNot(GameTestHelper test) {
        floor(test);
        for (String path : FOCI) {
            ServerPlayer player = player(test, 3.5, 1, 3.5, 0, 0);
            ItemStack wand = wand(test, path, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            wand.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            boolean held = List.of(LibItemNames.FOCUS_DEFLECT, LibItemNames.FOCUS_HEAL, LibItemNames.FOCUS_SMELT,
                    LibItemNames.FOCUS_TELEKINESIS).contains(path);
            test.assertTrue(player.isUsingItem() == held, path + ": using the wand after a click is " + player.isUsingItem());
            if (player.isUsingItem()) player.stopUsingItem();
            player.closeContainer();
        }
        test.succeed();
    }

    // ---- Uprising --------------------------------------------------------------------------------------------------

    public static void flightLaunchesThePlayerAndPays(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 90.0F, -30.0F);
        ItemStack wand = wand(test, LibItemNames.FOCUS_FLIGHT, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);
        player.fallDistance = 7.0F;

        ItemStack result = ((ItemFocusFlight) TTRegistry.item(LibItemNames.FOCUS_FLIGHT)).onFocusRightClick(wand, test.getLevel(),
                player, InteractionHand.MAIN_HAND);
        test.assertTrue(result == wand, "the cast answers the wand");
        Vec3 expected = player.getLookAngle().scale(2.0 / 3.0);
        test.assertTrue(near(player.getDeltaMovement(), expected, 1.0E-6), "launched along the sight at two thirds: " + player.getDeltaMovement());
        test.assertTrue(player.fallDistance == 0.0F, "the fall is forgotten");
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.AIR, 15), 1, "flight");

        // With the config switch off the focus does nothing and costs nothing.
        boolean flight = ConfigHandler.enableFlight;
        try {
            ConfigHandler.enableFlight = false;
            player.setDeltaMovement(Vec3.ZERO);
            AspectList mid = WandHelper.getAllVis(wand);
            ((ItemFocusFlight) TTRegistry.item(LibItemNames.FOCUS_FLIGHT)).onFocusRightClick(wand, test.getLevel(), player,
                    InteractionHand.MAIN_HAND);
            test.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "no launch while modFlight is off");
            assertNothingSpent(test, wand, mid, "flight while off");
        } finally {
            ConfigHandler.enableFlight = flight;
        }

        // Without vis the cast does nothing either.
        ItemStack empty = wand(test, LibItemNames.FOCUS_FLIGHT, false);
        player.setDeltaMovement(Vec3.ZERO);
        ((ItemFocusFlight) TTRegistry.item(LibItemNames.FOCUS_FLIGHT)).onFocusRightClick(empty, test.getLevel(), player,
                InteractionHand.MAIN_HAND);
        test.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "no launch without vis");
        test.succeed();
    }

    /**
     * The server kicks a player whose "ticks above the ground" counter passes 80 when {@code allow-flight} is off; every cast
     * of the focus must put the counter of his connection back to zero, as 1.7.10 did with {@code floatingTickCount}.
     */
    public static void flightPutsTheKickCounterOfTheConnectionBackToZero(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 90.0F, -30.0F);
        test.assertTrue(player.connection != null, "the mock player has a connection");
        test.assertTrue(FociHooks.canReachCounter(), "the counter field of the connection can be reached");
        ItemStack wand = wand(test, LibItemNames.FOCUS_FLIGHT, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        ItemFocusFlight flight = (ItemFocusFlight) TTRegistry.item(LibItemNames.FOCUS_FLIGHT);

        test.assertTrue(FociHooks.setAboveGroundTicks(player, 79), "the counter can be written");
        test.assertTrue(FociHooks.aboveGroundTicks(player) == 79, "and read back: " + FociHooks.aboveGroundTicks(player));
        flight.onFocusRightClick(wand, test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(FociHooks.aboveGroundTicks(player) == 0, "a cast clears the counter: " + FociHooks.aboveGroundTicks(player));

        // A cast that does nothing (the config switch is off, or there is no vis) does not touch it, as in 1.7.10.
        FociHooks.setAboveGroundTicks(player, 40);
        flight.onFocusRightClick(wand(test, LibItemNames.FOCUS_FLIGHT, false), test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(FociHooks.aboveGroundTicks(player) == 40, "no vis, no reset: " + FociHooks.aboveGroundTicks(player));

        // A loader or the API can take the reset over, and null gives the default back.
        int[] calls = { 0 };
        FociHooks.setFloatingTickReset(who -> calls[0]++);
        try {
            flight.onFocusRightClick(wand, test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(calls[0] == 1 && FociHooks.aboveGroundTicks(player) == 40, "the installed reset runs instead of the default");
        } finally {
            FociHooks.setFloatingTickReset(null);
        }
        flight.onFocusRightClick(wand, test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(FociHooks.aboveGroundTicks(player) == 0 && calls[0] == 1, "the default is back");
        test.succeed();
    }

    // ---- Mending ---------------------------------------------------------------------------------------------------

    public static void healMendsOnePointEveryThirtyTicksAndPaysPerHeal(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0, 0);
        ItemStack wand = wand(test, LibItemNames.FOCUS_HEAL, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        ItemFocusHeal heal = (ItemFocusHeal) TTRegistry.item(LibItemNames.FOCUS_HEAL);
        ServerLevel level = test.getLevel();
        AspectList before = WandHelper.getAllVis(wand);
        player.setHealth(10.0F);

        int ticks = ItemFocusHeal.requiredProgress(0);
        test.assertTrue(ticks == 30, "thirty ticks without potency");
        for (int i = 1; i < ticks; i++) heal.onUsingFocusTick(wand, player, Integer.MAX_VALUE - i);
        test.assertTrue(player.getHealth() == 10.0F, "nothing is healed before the thirtieth tick");
        test.assertTrue(ItemFocusHeal.progressOf(level, player.getUUID()) == ticks - 1, "the progress counts");
        assertNothingSpent(test, wand, before, "mending before the heal");
        heal.onUsingFocusTick(wand, player, Integer.MAX_VALUE - ticks);
        test.assertTrue(player.getHealth() == 11.0F, "one health point at the thirtieth tick: " + player.getHealth());
        test.assertTrue(ItemFocusHeal.progressOf(level, player.getUUID()) == 0, "the progress starts again");
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.EARTH, 45).add(Aspect.WATER, 45), 1, "mending");

        // Releasing the key forgets the progress.
        for (int i = 0; i < 12; i++) heal.onUsingFocusTick(wand, player, Integer.MAX_VALUE - i);
        test.assertTrue(ItemFocusHeal.progressOf(level, player.getUUID()) == 12, "twelve ticks into the next heal");
        heal.onPlayerStoppedUsingFocus(wand, level, player, 0);
        test.assertTrue(ItemFocusHeal.progressOf(level, player.getUUID()) == 0, "released: no progress");

        // A healthy player is not healed and nothing counts.
        player.setHealth(player.getMaxHealth());
        AspectList healthy = WandHelper.getAllVis(wand);
        for (int i = 0; i < 40; i++) heal.onUsingFocusTick(wand, player, Integer.MAX_VALUE - i);
        test.assertTrue(player.getHealth() == player.getMaxHealth(), "full health stays");
        test.assertTrue(ItemFocusHeal.progressOf(level, player.getUUID()) == 0, "no progress at full health");
        assertNothingSpent(test, wand, healthy, "mending at full health");

        // Without vis there is no healing.
        ItemStack empty = wand(test, LibItemNames.FOCUS_HEAL, false);
        player.setHealth(10.0F);
        for (int i = 0; i < 40; i++) heal.onUsingFocusTick(empty, player, Integer.MAX_VALUE - i);
        test.assertTrue(player.getHealth() == 10.0F, "no vis, no healing");
        ItemFocusHeal.forget(player.getUUID());
        test.succeed();
    }

    // ---- Efreet's Flame --------------------------------------------------------------------------------------------

    public static void smeltReadsTheFurnaceRecipesOfTheLevel(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        test.assertTrue(ItemFocusSmelt.smeltResult(level, Blocks.COBBLESTONE.defaultBlockState()).is(Items.STONE), "cobblestone becomes stone");
        test.assertTrue(ItemFocusSmelt.smeltResult(level, Blocks.SAND.defaultBlockState()).is(Items.GLASS), "sand becomes glass");
        test.assertTrue(ItemFocusSmelt.smeltResult(level, Blocks.CLAY.defaultBlockState()).is(Items.TERRACOTTA), "clay becomes terracotta");
        test.assertTrue(ItemFocusSmelt.smeltResult(level, Blocks.OAK_LOG.defaultBlockState()).isEmpty(), "a log becomes charcoal, an item");
        test.assertTrue(ItemFocusSmelt.smeltResult(level, Blocks.IRON_ORE.defaultBlockState()).isEmpty(), "ore becomes an ingot, an item");
        test.assertTrue(ItemFocusSmelt.smeltResult(level, Blocks.DIRT.defaultBlockState()).isEmpty(), "dirt has no recipe");
        test.assertTrue(ItemFocusSmelt.smeltResult(level, Blocks.AIR.defaultBlockState()).isEmpty(), "air has no item");
        test.succeed();
    }

    public static void smeltTurnsCobblestoneIntoStoneAfterTwentyOneTicks(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 1.5, 0.0F, 0.0F);
        BlockPos block = new BlockPos(2, 2, 4);
        test.setBlock(block, Blocks.COBBLESTONE);
        ItemStack wand = wand(test, LibItemNames.FOCUS_SMELT, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        ItemFocusSmelt smelt = (ItemFocusSmelt) TTRegistry.item(LibItemNames.FOCUS_SMELT);
        ServerLevel level = test.getLevel();

        BlockHitResult aim = ItemModFocus.trace(player, ItemFocusSmelt.REACH, false, true);
        test.assertTrue(aim.getType() == HitResult.Type.BLOCK && aim.getBlockPos().equals(test.absolutePos(block)),
                "the mock player looks at the block: " + aim);

        AspectList before = WandHelper.getAllVis(wand);
        for (int i = 0; i < 20; i++) smelt.onUsingFocusTick(wand, player, Integer.MAX_VALUE - i);
        test.assertBlockPresent(Blocks.COBBLESTONE, block);
        test.assertTrue(ItemFocusSmelt.dataOf(level, player.getUUID()) != null
                && ItemFocusSmelt.dataOf(level, player.getUUID()).progress == 1, "one tick to go");
        assertNothingSpent(test, wand, before, "smelting before the block melts");

        smelt.onUsingFocusTick(wand, player, Integer.MAX_VALUE - 20);
        test.assertBlockPresent(Blocks.STONE, block);
        // As in 1.7.10 the block is forgotten and, in the same tick, counted again from twenty.
        test.assertTrue(ItemFocusSmelt.dataOf(level, player.getUUID()) != null
                && ItemFocusSmelt.dataOf(level, player.getUUID()).progress == 20, "a new count starts");
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.FIRE, 45).add(Aspect.ENTROPY, 12), 1, "smelting");

        // Stone melts into smooth stone in the next twenty ticks, if the player keeps on.
        for (int i = 0; i < 19; i++) smelt.onUsingFocusTick(wand, player, Integer.MAX_VALUE - 21 - i);
        test.assertBlockPresent(Blocks.STONE, block);
        smelt.onUsingFocusTick(wand, player, Integer.MAX_VALUE - 40);
        test.assertBlockPresent(Blocks.SMOOTH_STONE, block);
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.FIRE, 45).add(Aspect.ENTROPY, 12), 2, "two smeltings");
        ItemFocusSmelt.forget(player.getUUID());
        test.succeed();
    }

    public static void smeltLeavesWhatTheFurnaceDoesNotMakeIntoABlock(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 1.5, 0.0F, 0.0F);
        BlockPos block = new BlockPos(2, 2, 4);
        test.setBlock(block, Blocks.OAK_LOG);
        ItemStack wand = wand(test, LibItemNames.FOCUS_SMELT, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        ItemFocusSmelt smelt = (ItemFocusSmelt) TTRegistry.item(LibItemNames.FOCUS_SMELT);
        AspectList before = WandHelper.getAllVis(wand);
        for (int i = 0; i < 30; i++) smelt.onUsingFocusTick(wand, player, Integer.MAX_VALUE - i);
        test.assertBlockPresent(Blocks.OAK_LOG, block);
        assertNothingSpent(test, wand, before, "a log does not smelt into a block");

        // And the player who looks away from the block starts again.
        test.setBlock(block, Blocks.SAND);
        for (int i = 0; i < 10; i++) smelt.onUsingFocusTick(wand, player, Integer.MAX_VALUE - i);
        player.moveTo(player.getX(), player.getY(), player.getZ(), 180.0F, 0.0F);
        player.setYHeadRot(180.0F);
        for (int i = 0; i < 30; i++) smelt.onUsingFocusTick(wand, player, Integer.MAX_VALUE - 10 - i);
        test.assertBlockPresent(Blocks.SAND, block);

        // Without vis nothing happens.
        ItemStack empty = wand(test, LibItemNames.FOCUS_SMELT, false);
        player.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
        player.setYHeadRot(0.0F);
        for (int i = 0; i < 40; i++) smelt.onUsingFocusTick(empty, player, Integer.MAX_VALUE - i);
        test.assertBlockPresent(Blocks.SAND, block);
        ItemFocusSmelt.forget(player.getUUID());
        test.succeed();
    }

    // ---- Telekinesis -----------------------------------------------------------------------------------------------

    public static void telekinesisPullsItemsAndPaysOnlyWhenThereAreSome(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0.0F, 0.0F);
        ItemStack wand = wand(test, LibItemNames.FOCUS_TELEKINESIS, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        ItemFocusTelekinesis telekinesis = (ItemFocusTelekinesis) TTRegistry.item(LibItemNames.FOCUS_TELEKINESIS);
        AspectList cost = new AspectList().add(Aspect.AIR, 5).add(Aspect.ENTROPY, 5);

        // Nothing to pull: nothing paid.
        AspectList before = WandHelper.getAllVis(wand);
        telekinesis.onUsingFocusTick(wand, player, Integer.MAX_VALUE);
        assertNothingSpent(test, wand, before, "telekinesis without items");

        // Sneaking: the point is the player's centre, half a block up; an item three blocks away goes to him.
        player.setShiftKeyDown(true);
        ItemEntity item = test.spawnItem(Items.DIAMOND, 3.5F, 2.0F, 6.5F);
        telekinesis.onUsingFocusTick(wand, player, Integer.MAX_VALUE - 1);
        Vec3 motion = item.getDeltaMovement();
        test.assertTrue(Math.abs(motion.length() - ItemFocusTelekinesis.PULL_SPEED) < 1.0E-4, "pulled at a third of a block per tick: " + motion);
        test.assertTrue(motion.z < 0.0, "towards the player: " + motion);
        assertSpent(test, wand, player, before, cost, 1, "telekinesis");

        // Not sneaking: the point is five blocks ahead, so the same item is pulled away from the player.
        player.setShiftKeyDown(false);
        item.setDeltaMovement(Vec3.ZERO);
        item.setPos(test.absoluteVec(new Vec3(3.5, 2.0, 6.5)));
        telekinesis.onUsingFocusTick(wand, player, Integer.MAX_VALUE - 2);
        test.assertTrue(item.getDeltaMovement().z > 0.0, "towards the point ahead: " + item.getDeltaMovement());

        // An item farther than six blocks from the point stays where it is.
        ItemEntity far = test.spawnItem(Items.GOLD_INGOT, 0.5F, 2.0F, 0.5F);
        far.setPos(test.absoluteVec(new Vec3(0.5, 2.0, 0.5)));
        player.setShiftKeyDown(true);
        item.discard();
        AspectList afterwards = WandHelper.getAllVis(wand);
        player.moveTo(test.absoluteVec(new Vec3(7.5, 1, 7.5)));
        telekinesis.onUsingFocusTick(wand, player, Integer.MAX_VALUE - 3);
        test.assertTrue(far.getDeltaMovement().equals(Vec3.ZERO), "out of reach of the pull");
        assertNothingSpent(test, wand, afterwards, "telekinesis with nothing in reach");
        test.succeed();
    }

    // ---- Distortion ------------------------------------------------------------------------------------------------

    private static <T extends Projectile> T arrowLike(GameTestHelper test, EntityType<T> type, Vec3 relative, Vec3 motion) {
        T entity = type.create(test.getLevel());
        entity.setPos(test.absoluteVec(relative));
        entity.setDeltaMovement(motion);
        test.getLevel().addFreshEntity(entity);
        return entity;
    }

    public static void deflectShiftsForeignProjectilesAndSparesTheOwnAndTheBlacklisted(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0.0F, 0.0F);
        ItemStack wand = wand(test, LibItemNames.FOCUS_DEFLECT, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        ItemFocusDeflect deflect = (ItemFocusDeflect) TTRegistry.item(LibItemNames.FOCUS_DEFLECT);
        Vec3 towards = new Vec3(0.0, 0.0, -1.0);

        AbstractArrow foreign = arrowLike(test, EntityType.ARROW, new Vec3(3.5, 2.0, 5.5), towards);
        AbstractArrow own = arrowLike(test, EntityType.ARROW, new Vec3(4.5, 2.0, 5.5), towards);
        own.setOwner(player);
        ThrownExperienceBottle bottle = arrowLike(test, EntityType.EXPERIENCE_BOTTLE, new Vec3(2.5, 2.0, 5.5), towards);
        Snowball snowball = arrowLike(test, EntityType.SNOWBALL, new Vec3(1.5, 2.0, 5.5), towards);
        Projectile fireball = arrowLike(test, EntityType.SMALL_FIREBALL, new Vec3(5.5, 2.0, 5.5), towards);

        Vec3 foreignBefore = foreign.position();
        Vec3 snowballBefore = snowball.position();
        AspectList before = WandHelper.getAllVis(wand);
        deflect.onUsingFocusTick(wand, player, Integer.MAX_VALUE);

        double foreignDistance = foreignBefore.distanceTo(player.position());
        Vec3 expected = foreignBefore.add(towards.scale(foreignDistance * 2));
        test.assertTrue(near(foreign.position(), expected, 1.0E-6), "the foreign arrow went past: " + foreign.position() + " expected " + expected);
        double snowballDistance = snowballBefore.distanceTo(player.position());
        test.assertTrue(near(snowball.position(), snowballBefore.add(towards.scale(snowballDistance * 2)), 1.0E-6), "so did the snowball");
        test.assertTrue(near(own.position(), test.absoluteVec(new Vec3(4.5, 2.0, 5.5)), 1.0E-9), "the player's own arrow stays");
        test.assertTrue(near(bottle.position(), test.absoluteVec(new Vec3(2.5, 2.0, 5.5)), 1.0E-9), "the experience bottle stays");
        test.assertTrue(near(fireball.position(), test.absoluteVec(new Vec3(5.5, 2.0, 5.5)), 1.0E-9), "a fireball is not an arrow or a thrown object");
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.ORDER, 8).add(Aspect.AIR, 4), 1, "distortion");

        // The blacklist is a set of entity-type ids: an addition protects the type, as Botania's compat once did.
        test.assertTrue(ProjectileHelper.isBlacklisted(bottle), "the bottle is blacklisted");
        test.assertTrue(!ProjectileHelper.isBlacklisted(snowball), "the snowball is not");
        var snowballId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.SNOWBALL);
        try {
            ProjectileHelper.DEFLECT_BLACKLIST.add(snowballId);
            test.assertTrue(ProjectileHelper.isBlacklisted(snowball), "after adding it, the snowball is");
        } finally {
            ProjectileHelper.DEFLECT_BLACKLIST.remove(snowballId);
        }
        test.assertTrue(ProjectileHelper.getOwner(own) == player, "the owner of an arrow is its shooter");
        test.assertTrue(ProjectileHelper.getOwner(foreign) == null, "an arrow nobody shot has no owner");
        test.succeed();
    }

    public static void deflectEnlargeWidensTheBox(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0.0F, 0.0F);
        ItemStack plain = wand(test, LibItemNames.FOCUS_DEFLECT, true);
        ItemStack enlarged = wand(test, LibItemNames.FOCUS_DEFLECT, true);
        ItemStack focusStack = WandHelper.getFocusItem(enlarged);
        test.assertTrue(((ItemFocusDeflect) focusStack.getItem()).applyUpgrade(focusStack, FocusUpgradeType.enlarge, 2), "rank 2 takes enlarge");
        test.assertTrue(WandHelper.setFocus(enlarged, focusStack), "the enlarged focus mounts");
        ItemFocusDeflect deflect = (ItemFocusDeflect) TTRegistry.item(LibItemNames.FOCUS_DEFLECT);

        // Four and a half blocks to the west of the player: outside the plain box (4), inside the enlarged one (5).
        AbstractArrow arrow = arrowLike(test, EntityType.ARROW, new Vec3(-1.0, 1.5, 3.5), new Vec3(1.0, 0.0, 0.0));
        Vec3 start = arrow.position();
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        deflect.onUsingFocusTick(plain, player, Integer.MAX_VALUE);
        test.assertTrue(arrow.position().equals(start), "outside the plain box nothing happens: " + arrow.position());
        player.setItemInHand(InteractionHand.MAIN_HAND, enlarged);
        deflect.onUsingFocusTick(enlarged, player, Integer.MAX_VALUE - 1);
        test.assertTrue(arrow.position().x > start.x + 1.0, "inside the enlarged box the arrow is carried past: " + arrow.position());
        test.succeed();
    }

    /** The real thing: two players, an arrow flying at each, and only one of them holds the Focus of Distortion. */
    public static void distortionLetsArrowsFlyPastTheHolderWhileTheOtherPlayerIsHit(GameTestHelper test) {
        floor(test);
        ServerPlayer shielded = player(test, 2.5, 1, 2.5, 0.0F, 0.0F);
        ServerPlayer bare = player(test, 6.5, 1, 2.5, 0.0F, 0.0F);
        ItemStack wand = wand(test, LibItemNames.FOCUS_DEFLECT, true);
        shielded.setItemInHand(InteractionHand.MAIN_HAND, wand);
        wand.use(test.getLevel(), shielded, InteractionHand.MAIN_HAND);
        test.assertTrue(shielded.isUsingItem(), "the shield is up");
        AbstractArrow atShielded = arrowLike(test, EntityType.ARROW, new Vec3(2.5, 1.6, 7.5), new Vec3(0.0, 0.0, -1.0));
        AbstractArrow atBare = arrowLike(test, EntityType.ARROW, new Vec3(6.5, 1.6, 7.5), new Vec3(0.0, 0.0, -1.0));
        atShielded.setNoGravity(true);
        atBare.setNoGravity(true);
        double[] farthest = { Double.MAX_VALUE, Double.MAX_VALUE };
        // A mock player has no connection in the server's list, so nothing ticks it: do what the connection would.
        test.onEachTick(() -> {
            shielded.doTick();
            bare.doTick();
            farthest[0] = Math.min(farthest[0], atShielded.getZ());
            farthest[1] = Math.min(farthest[1], atBare.getZ());
        });
        test.runAfterDelay(12, () -> {
            test.assertTrue(farthest[0] < shielded.getZ() - 0.5, "the arrow flew past the player with the shield: z " + farthest[0]
                    + ", player " + shielded.getZ());
            test.assertTrue(atBare.isRemoved() || farthest[1] > bare.getZ() - 0.5, "the arrow at the other player was stopped by him: z "
                    + farthest[1] + ", player " + bare.getZ() + ", removed " + atBare.getRemovalReason());
            shielded.releaseUsingItem();
            test.succeed();
        });
    }

    // ---- Ender Rift ------------------------------------------------------------------------------------------------

    public static void enderChestOpensTheChestOfThePlayerAndPays(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0.0F, 0.0F);
        player.getEnderChestInventory().clearContent();
        player.getEnderChestInventory().setItem(4, new ItemStack(Items.DIAMOND, 3));
        ItemStack wand = wand(test, LibItemNames.FOCUS_ENDER_CHEST, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);

        ((ItemFocusEnderChest) TTRegistry.item(LibItemNames.FOCUS_ENDER_CHEST)).onFocusRightClick(wand, test.getLevel(), player,
                InteractionHand.MAIN_HAND);
        test.assertTrue(player.containerMenu instanceof ChestMenu, "a chest window opened: " + player.containerMenu);
        ChestMenu menu = (ChestMenu) player.containerMenu;
        test.assertTrue(menu.getContainer() == player.getEnderChestInventory(), "over the ender chest of the player");
        test.assertTrue(menu.getRowCount() == 3, "three rows");
        test.assertTrue(menu.getSlot(4).getItem().is(Items.DIAMOND) && menu.getSlot(4).getItem().getCount() == 3, "with his diamonds");
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.ENTROPY, 100).add(Aspect.ORDER, 100), 1, "ender rift");
        player.closeContainer();

        // Without vis the window stays shut.
        ItemStack empty = wand(test, LibItemNames.FOCUS_ENDER_CHEST, false);
        ((ItemFocusEnderChest) TTRegistry.item(LibItemNames.FOCUS_ENDER_CHEST)).onFocusRightClick(empty, test.getLevel(), player,
                InteractionHand.MAIN_HAND);
        test.assertTrue(player.containerMenu == player.inventoryMenu, "no vis, no window");
        player.getEnderChestInventory().clearContent();
        test.succeed();
    }

    // ---- Dislocation -----------------------------------------------------------------------------------------------

    private static ItemFocusDislocation dislocation() {
        return (ItemFocusDislocation) TTRegistry.item(LibItemNames.FOCUS_DISLOCATION);
    }

    private static ChestBlockEntity chestWithDiamonds(GameTestHelper test, BlockPos relative) {
        test.setBlock(relative, Blocks.CHEST);
        ChestBlockEntity chest = test.getBlockEntity(relative);
        chest.setItem(3, new ItemStack(Items.DIAMOND, 5));
        return chest;
    }

    public static void dislocationCarriesAChestWithItsContentsAndPutsItDown(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        ServerLevel level = test.getLevel();
        BlockPos chestPos = new BlockPos(2, 1, 2);
        chestWithDiamonds(test, chestPos);
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);

        // Take the chest: the block entity is gone with its contents, nothing drops, the focus carries it all, 25/25/5 vis.
        dislocation().dislocate(wand, level, player, InteractionHand.MAIN_HAND, hit(test, chestPos, Direction.UP));
        test.assertBlockPresent(Blocks.AIR, chestPos);
        DislocationData carried = ItemFocusDislocation.getData(wand);
        test.assertTrue(carried != null && carried.state().is(Blocks.CHEST), "the focus carries the chest: " + carried);
        test.assertTrue(carried.hasTile() && carried.tile().toString().contains("minecraft:diamond"), "with its contents: " + carried.tile());
        test.assertItemEntityNotPresent(Items.DIAMOND);
        test.assertTrue(ItemFocusDislocation.getPickedBlock(wand).is(Items.CHEST), "the picked block as an item");
        test.assertTrue(ItemFocusDislocation.getStackTileEntity(wand).toString().contains("minecraft:diamond"), "the saved block entity");
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.ENTROPY, 2500).add(Aspect.ORDER, 2500).add(Aspect.EARTH, 500),
                1, "taking a chest");

        // The load survives saving the wand (the stack the world and the network carry).
        ItemStack loaded = roundTrip(test, wand);
        DislocationData again = ItemFocusDislocation.getData(loaded);
        test.assertTrue(again != null && again.equals(carried), "the load survives saving: " + again);
        test.assertTrue(ItemFocusDislocation.getData(wire(test, wand)) != null && ItemFocusDislocation.getData(wire(test, wand)).equals(carried),
                "the load survives the network");

        // Put it down on the floor: on the side that was hit, with its contents, free of charge.
        AspectList carriedVis = WandHelper.getAllVis(wand);
        dislocation().dislocate(wand, level, player, InteractionHand.MAIN_HAND, hit(test, new BlockPos(5, 0, 5), Direction.UP));
        BlockPos target = new BlockPos(5, 1, 5);
        test.assertBlockPresent(Blocks.CHEST, target);
        ChestBlockEntity placed = test.getBlockEntity(target);
        test.assertTrue(placed.getItem(3).is(Items.DIAMOND) && placed.getItem(3).getCount() == 5, "the diamonds are in the chest");
        test.assertTrue(ItemFocusDislocation.getData(wand) == null, "the focus is empty again");
        assertNothingSpent(test, wand, carriedVis, "putting the block down");
        test.succeed();
    }

    public static void dislocationCastTakesTheBlockTheRayHitsAndPlacesAgainstTheNextOne(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 0.5, 0.0F, 0.0F);
        test.setBlock(new BlockPos(2, 2, 3), Blocks.COBBLESTONE);
        test.setBlock(new BlockPos(2, 2, 4), Blocks.DIRT);
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);

        // First click: the cobblestone is the first block of the ray.
        wand.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 3));
        DislocationData carried = ItemFocusDislocation.getData(wand);
        test.assertTrue(carried != null && carried.state().is(Blocks.COBBLESTONE) && !carried.hasTile(), "the cobblestone is carried");
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.ENTROPY, 500).add(Aspect.ORDER, 500).add(Aspect.EARTH, 100),
                1, "taking a plain block");

        // Second click: the ray now ends on the dirt, and the cobblestone goes where the cobblestone was.
        wand.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(2, 2, 3));
        test.assertTrue(ItemFocusDislocation.getData(wand) == null, "put down");
        test.succeed();
    }

    public static void dislocationRefusesWhatItMust(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        ServerLevel level = test.getLevel();
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);

        // What cannot be broken, pistons, and what the config blacklists stay.
        BlockPos spot = new BlockPos(1, 1, 1);
        for (BlockState refused : List.of(Blocks.BEDROCK.defaultBlockState(), Blocks.PISTON_HEAD.defaultBlockState(),
                Blocks.MOVING_PISTON.defaultBlockState())) {
            test.setBlock(spot, refused);
            dislocation().dislocate(wand, level, player, InteractionHand.MAIN_HAND, hit(test, spot, Direction.UP));
            test.assertTrue(test.getBlockState(spot).is(refused.getBlock()), refused + " stays");
            test.assertTrue(ItemFocusDislocation.getData(wand) == null, refused + " is not carried");
        }
        String[] blacklist = ConfigHandler.blockBlacklist;
        try {
            ConfigHandler.blockBlacklist = new String[] { "minecraft:glass" };
            test.setBlock(spot, Blocks.GLASS);
            dislocation().dislocate(wand, level, player, InteractionHand.MAIN_HAND, hit(test, spot, Direction.UP));
            test.assertBlockPresent(Blocks.GLASS, spot);
            test.assertTrue(ItemFocusDislocation.getData(wand) == null, "the blacklisted glass is not carried");
            test.setBlock(spot, Blocks.STONE);
            dislocation().dislocate(wand, level, player, InteractionHand.MAIN_HAND, hit(test, spot, Direction.UP));
            test.assertTrue(ItemFocusDislocation.getData(wand) != null, "stone is not on the list and is carried");
        } finally {
            ConfigHandler.blockBlacklist = blacklist;
        }
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.ENTROPY, 500).add(Aspect.ORDER, 500).add(Aspect.EARTH, 100),
                1, "only the stone was paid");

        // A load is not put down where the target is not replaceable (the block above the spot is stone): it stays in the focus.
        test.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);
        dislocation().dislocate(wand, level, player, InteractionHand.MAIN_HAND, hit(test, new BlockPos(1, 1, 1), Direction.UP));
        test.assertTrue(ItemFocusDislocation.getData(wand) != null, "no room: the load stays in the focus");
        // ... and not where a player stands.
        ServerPlayer standing = player(test, 4.5, 1, 4.5, 0.0F, 0.0F);
        dislocation().dislocate(wand, level, player, InteractionHand.MAIN_HAND, hit(test, new BlockPos(4, 0, 4), Direction.UP));
        test.assertTrue(ItemFocusDislocation.getData(wand) != null && test.getBlockState(new BlockPos(4, 1, 4)).isAir(),
                "no block is put into a player: " + standing.getName().getString());

        // Without vis nothing is taken.
        ItemStack empty = wand(test, LibItemNames.FOCUS_DISLOCATION, false);
        test.setBlock(new BlockPos(3, 1, 3), Blocks.DIRT);
        dislocation().dislocate(empty, level, player, InteractionHand.MAIN_HAND, hit(test, new BlockPos(3, 1, 3), Direction.UP));
        test.assertBlockPresent(Blocks.DIRT, new BlockPos(3, 1, 3));
        test.assertTrue(ItemFocusDislocation.getData(empty) == null, "no vis, nothing carried");
        test.succeed();
    }

    public static void dislocationRefusesABlockEntityThatIsTooLargeToCarry(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        BlockPos chestPos = new BlockPos(2, 1, 2);
        test.setBlock(chestPos, Blocks.CHEST);
        ChestBlockEntity chest = test.getBlockEntity(chestPos);
        // Twenty-seven stacks that each carry forty thousand bytes of data: more than the million bytes the focus takes.
        for (int slot = 0; slot < 27; slot++) {
            ItemStack stack = new ItemStack(Items.PAPER);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putByteArray("bulk", new byte[40_000]));
            chest.setItem(slot, stack);
        }
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);
        dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, chestPos, Direction.UP));
        test.assertBlockPresent(Blocks.CHEST, chestPos);
        test.assertTrue(ItemFocusDislocation.getData(wand) == null, "nothing is carried");
        assertNothingSpent(test, wand, before, "a chest that is too large");
        test.succeed();
    }

    public static void dislocationCarriesALargeButAllowedChestAcrossTheNetwork(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        BlockPos chestPos = new BlockPos(2, 1, 2);
        test.setBlock(chestPos, Blocks.CHEST);
        ChestBlockEntity chest = test.getBlockEntity(chestPos);
        // Twenty stacks of forty thousand bytes: eight hundred thousand bytes, below the million the focus takes and the two
        // megabytes a client reads.
        for (int slot = 0; slot < 20; slot++) {
            ItemStack stack = new ItemStack(Items.PAPER);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putByteArray("bulk", new byte[40_000]));
            chest.setItem(slot, stack);
        }
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, chestPos, Direction.UP));
        DislocationData carried = ItemFocusDislocation.getData(wand);
        test.assertTrue(carried != null, "the chest is carried");
        ItemStack onTheClient = wire(test, wand);
        DislocationData read = ItemFocusDislocation.getData(onTheClient);
        test.assertTrue(read != null && read.equals(carried), "and a client reads it");
        test.succeed();
    }

    public static void dislocationTakesASpawnerAtTheSpawnerPrice(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        BlockPos pos = new BlockPos(2, 1, 2);
        test.setBlock(pos, Blocks.SPAWNER);
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);
        dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, pos, Direction.UP));
        DislocationData carried = ItemFocusDislocation.getData(wand);
        test.assertTrue(carried != null && carried.state().is(Blocks.SPAWNER) && carried.hasTile(), "the spawner is carried with its data");
        assertSpent(test, wand, player, before, new AspectList().add(Aspect.ENTROPY, 10000).add(Aspect.ORDER, 10000).add(Aspect.EARTH, 5000),
                1, "taking a spawner");
        test.succeed();
    }

    // ---- upgrades and the held cast through the real use tick ------------------------------------------------------

    public static void frugalLowersThePriceAndPotencyQuickensMending(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0.0F, 0.0F);
        ItemStack plain = wand(test, LibItemNames.FOCUS_FLIGHT, true);
        ItemStack frugal = upgraded(test, wand(test, LibItemNames.FOCUS_FLIGHT, true), FocusUpgradeType.frugal, 1);
        test.assertTrue(WandHelper.getConsumptionModifier(frugal, player, Aspect.AIR, false)
                < WandHelper.getConsumptionModifier(plain, player, Aspect.AIR, false), "frugal makes the cast cheaper");
        AspectList before = WandHelper.getAllVis(frugal);
        player.setItemInHand(InteractionHand.MAIN_HAND, frugal);
        ((ItemFocusFlight) TTRegistry.item(LibItemNames.FOCUS_FLIGHT)).onFocusRightClick(frugal, test.getLevel(), player,
                InteractionHand.MAIN_HAND);
        assertSpent(test, frugal, player, before, new AspectList().add(Aspect.AIR, 15), 1, "a frugal flight");

        // Mending with one level of potency: 27 ticks and two health points.
        ItemStack mending = upgraded(test, wand(test, LibItemNames.FOCUS_HEAL, true), FocusUpgradeType.potency, 1);
        test.assertTrue(WandHelper.getFocusPotency(mending) == 1, "one level of potency");
        player.setItemInHand(InteractionHand.MAIN_HAND, mending);
        ItemFocusHeal heal = (ItemFocusHeal) TTRegistry.item(LibItemNames.FOCUS_HEAL);
        player.setHealth(10.0F);
        int ticks = ItemFocusHeal.requiredProgress(1);
        test.assertTrue(ticks == 27, "27 ticks with one level of potency");
        for (int i = 1; i < ticks; i++) heal.onUsingFocusTick(mending, player, Integer.MAX_VALUE - i);
        test.assertTrue(player.getHealth() == 10.0F, "not yet");
        heal.onUsingFocusTick(mending, player, Integer.MAX_VALUE - ticks);
        test.assertTrue(player.getHealth() == 12.0F, "two health points: " + player.getHealth());
        ItemFocusHeal.forget(player.getUUID());
        test.succeed();
    }

    public static void aRunicStaffAddsAPotencyLevelToEfreetsFlame(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 1.5, 0.0F, 0.0F);
        BlockPos block = new BlockPos(2, 2, 4);
        test.setBlock(block, Blocks.COBBLESTONE);
        ItemStack wand = wand(test, LibItemNames.FOCUS_SMELT, true, "primal_staff");
        test.assertTrue(WandHelper.hasRunes(wand) && WandHelper.getFocusPotency(wand) == 1, "the primal staff gives one level of potency");
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        ItemFocusSmelt smelt = (ItemFocusSmelt) TTRegistry.item(LibItemNames.FOCUS_SMELT);
        for (int i = 0; i < 15; i++) smelt.onUsingFocusTick(wand, player, Integer.MAX_VALUE - i);
        test.assertBlockPresent(Blocks.COBBLESTONE, block);
        smelt.onUsingFocusTick(wand, player, Integer.MAX_VALUE - 15);
        test.assertBlockPresent(Blocks.STONE, block);
        ItemFocusSmelt.forget(player.getUUID());
        test.succeed();
    }

    /** The real path: the wand is used, and the server's own ticks of the mock player call the focus. */
    public static void mendingWorksWhileTheWandIsHeldAcrossRealTicks(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0.0F, 0.0F);
        ItemStack wand = wand(test, LibItemNames.FOCUS_HEAL, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.setHealth(10.0F);
        wand.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(player.isUsingItem(), "the use started");
        // A mock player has no connection in the server's list, so nothing ticks it: do what the connection would.
        test.onEachTick(player::doTick);
        test.succeedWhen(() -> {
            test.assertTrue(player.getHealth() > 10.0F, "no heal yet, health " + player.getHealth());
            // The key is let go: the wand's release hook runs, and Mending forgets its progress.
            player.releaseUsingItem();
            test.assertTrue(!player.isUsingItem(), "the use ended");
            test.assertTrue(ItemFocusHeal.progressOf(test.getLevel(), player.getUUID()) == 0, "releasing the key forgets the progress");
            ItemFocusHeal.forget(player.getUUID());
        });
    }

    public static void telekinesisWorksWhileTheWandIsHeldAcrossRealTicks(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0.0F, 0.0F);
        player.setShiftKeyDown(true);
        ItemStack wand = wand(test, LibItemNames.FOCUS_TELEKINESIS, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        ItemEntity item = test.spawnItem(Items.DIAMOND, 3.5F, 2.0F, 6.5F);
        Vec3 start = test.absoluteVec(new Vec3(3.5, 2.0, 6.5));
        wand.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.onEachTick(player::doTick);
        test.succeedWhen(() -> {
            test.assertTrue(item.position().distanceTo(start) > 1.0, "the item has not moved: " + item.position());
            test.assertTrue(item.position().z < start.z, "towards the player: " + item.position());
            player.stopUsingItem();
        });
    }

    // ---- more of the Dislocation -----------------------------------------------------------------------------------

    public static void dislocationRefusesThePortableHoleBlacklist(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);
        BlockPos spot = new BlockPos(1, 1, 1);
        for (BlockState refused : List.of(Blocks.PISTON.defaultBlockState(), Blocks.STICKY_PISTON.defaultBlockState(),
                Blocks.RED_BED.defaultBlockState(), Blocks.OAK_DOOR.defaultBlockState())) {
            test.setBlock(spot, refused);
            dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, spot, Direction.UP));
            test.assertTrue(test.getBlockState(spot).is(refused.getBlock()), refused.getBlock() + " stays");
            test.assertTrue(ItemFocusDislocation.getData(wand) == null, refused.getBlock() + " is not carried");
        }
        assertNothingSpent(test, wand, before, "the blacklisted blocks");
        test.succeed();
    }

    public static void dislocationCarriesWaterAsASource(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 0.5, 0.0F, 0.0F);
        test.setBlock(new BlockPos(2, 2, 3), Blocks.WATER);
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        wand.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        DislocationData carried = ItemFocusDislocation.getData(wand);
        test.assertTrue(carried != null && carried.state().is(Blocks.WATER), "the ray hit the water and the focus carries it: " + carried);
        test.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 3));
        dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, new BlockPos(5, 0, 5), Direction.UP));
        BlockState placed = test.getBlockState(new BlockPos(5, 1, 5));
        test.assertTrue(placed.is(Blocks.WATER) && placed.getFluidState().isSource(), "a source of water is put down: " + placed);
        test.assertTrue(ItemFocusDislocation.getData(wand) == null, "the focus is empty");
        test.succeed();
    }

    /** The ray of the wand stops on a source of water, not on flowing water (1.7.10 {@code BlockLiquid.canCollideCheck}). */
    public static void dislocationDoesNotTakeFlowingWater(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 0.5, 0.0F, 0.0F);
        BlockPos water = new BlockPos(2, 2, 3);
        test.setBlock(water, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);
        wand.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemFocusDislocation.getData(wand) == null, "the ray passes through flowing water: nothing is carried");
        dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, water, Direction.UP));
        test.assertTrue(ItemFocusDislocation.getData(wand) == null, "flowing water is not taken even when it is aimed at by hand");
        test.assertTrue(test.getBlockState(water).is(Blocks.WATER), "and it stays");
        assertNothingSpent(test, wand, before, "flowing water");
        test.succeed();
    }

    /**
     * {@code DoublePlantBlock.setPlacedBy} and {@code BedBlock.setPlacedBy} build their other half over whatever stands there,
     * so the focus carries neither half of a tall plant, and a load of that kind that was made by a command is dropped instead
     * of overwriting the block above (a barrier here).
     */
    public static void dislocationRefusesTheHalvesOfTallPlantsAndNeverOverwritesTheBlockAbove(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        ServerLevel level = test.getLevel();
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        AspectList before = WandHelper.getAllVis(wand);

        BlockPos lower = new BlockPos(2, 1, 2);
        BlockPos upper = lower.above();
        test.setBlock(lower.below(), Blocks.DIRT); // a tall plant stands on dirt, not on the stone of the floor
        test.setBlock(lower, Blocks.ROSE_BUSH.defaultBlockState());
        test.setBlock(upper, Blocks.ROSE_BUSH.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        for (BlockPos half : List.of(lower, upper)) {
            dislocation().dislocate(wand, level, player, InteractionHand.MAIN_HAND, hit(test, half, Direction.EAST));
            test.assertTrue(ItemFocusDislocation.getData(wand) == null, "a half of a rose bush is not carried: " + half);
        }
        test.assertBlockPresent(Blocks.ROSE_BUSH, lower);
        test.assertBlockPresent(Blocks.ROSE_BUSH, upper);
        assertNothingSpent(test, wand, before, "the rose bush");

        // A load made by a command: the lower half of a rose bush, to be put on grass under a barrier.
        BlockPos grass = new BlockPos(5, 1, 5);
        test.setBlock(grass, Blocks.GRASS_BLOCK);
        test.setBlock(grass.above(2), Blocks.BARRIER);
        ItemStack focus = WandHelper.getFocusItem(wand);
        focus.set(FociComponents.DISLOCATION.get(), new DislocationData(Blocks.ROSE_BUSH.defaultBlockState(), new CompoundTag()));
        test.assertTrue(WandHelper.setFocus(wand, focus), "the load can be set");
        dislocation().dislocate(wand, level, player, InteractionHand.MAIN_HAND, hit(test, grass, Direction.UP));
        test.assertBlockPresent(Blocks.BARRIER, grass.above(2));
        test.assertTrue(test.getBlockState(grass.above()).isAir(), "nothing was put down");
        test.assertTrue(ItemFocusDislocation.getData(wand) == null, "the load that could never be put down is dropped, the focus is not stuck");
        test.succeed();
    }

    public static void dislocationKeepsTheStateAndTheItemsOfAFurnace(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        BlockPos furnacePos = new BlockPos(2, 1, 2);
        BlockState state = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.LIT, true);
        test.setBlock(furnacePos, state);
        FurnaceBlockEntity furnace = test.getBlockEntity(furnacePos);
        furnace.setItem(0, new ItemStack(Items.IRON_ORE, 3));
        furnace.setItem(1, new ItemStack(Items.COAL, 5));
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, furnacePos, Direction.UP));
        test.assertItemEntityNotPresent(Items.IRON_ORE);
        test.assertItemEntityNotPresent(Items.COAL);
        test.assertBlockPresent(Blocks.AIR, furnacePos);
        dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, new BlockPos(5, 0, 5), Direction.UP));
        BlockPos at = new BlockPos(5, 1, 5);
        BlockState placed = test.getBlockState(at);
        test.assertTrue(placed.is(Blocks.FURNACE) && placed.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST
                && placed.getValue(BlockStateProperties.LIT), "the state travels, facing east and lit: " + placed);
        AbstractFurnaceBlockEntity moved = test.getBlockEntity(at);
        test.assertTrue(moved.getItem(0).is(Items.IRON_ORE) && moved.getItem(0).getCount() == 3 && moved.getItem(1).is(Items.COAL)
                && moved.getItem(1).getCount() == 5, "the items travel");
        test.succeed();
    }

    public static void aHalfOfADoubleChestIsASingleChestInTheFocus(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        BlockPos left = new BlockPos(2, 1, 2);
        BlockPos right = new BlockPos(3, 1, 2);
        test.setBlock(left, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        test.setBlock(right, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        ItemStack wand = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, left, Direction.UP));
        DislocationData carried = ItemFocusDislocation.getData(wand);
        test.assertTrue(carried != null && carried.state().getValue(ChestBlock.TYPE) == ChestType.SINGLE, "a single chest is carried: " + carried);
        dislocation().dislocate(wand, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, new BlockPos(5, 0, 5), Direction.UP));
        test.assertTrue(test.getBlockState(new BlockPos(5, 1, 5)).getValue(ChestBlock.TYPE) == ChestType.SINGLE, "and put down as a single chest");
        test.succeed();
    }

    public static void twoLoadsHaveTwoKeysInTheFocusSelector(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0.0F, 0.0F);
        test.setBlock(new BlockPos(1, 1, 1), Blocks.DIRT);
        test.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        ItemStack first = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        ItemStack second = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        ItemStack empty = wand(test, LibItemNames.FOCUS_DISLOCATION, true);
        dislocation().dislocate(first, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, new BlockPos(1, 1, 1), Direction.UP));
        dislocation().dislocate(second, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(test, new BlockPos(2, 1, 2), Direction.UP));
        String keyFirst = WandHelper.getFocus(first).getSortingHelper(WandHelper.getFocusItem(first));
        String keySecond = WandHelper.getFocus(second).getSortingHelper(WandHelper.getFocusItem(second));
        String keyEmpty = WandHelper.getFocus(empty).getSortingHelper(WandHelper.getFocusItem(empty));
        test.assertTrue(keyFirst.startsWith("TTDL") && keySecond.startsWith("TTDL") && keyEmpty.startsWith("TTDL"), "prefixes: " + keyFirst);
        test.assertTrue(!keyFirst.equals(keySecond) && !keyFirst.equals(keyEmpty) && !keySecond.equals(keyEmpty), "three different keys");
        test.succeed();
    }

    // ---- the whole add-on ------------------------------------------------------------------------------------------

    public static void registrationHadNoFailures(GameTestHelper test) {
        test.assertTrue(ThaumicTinkerer.isReady(), "the Thaumcraft ready hook of the add-on ran");
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        test.succeed();
    }
}
