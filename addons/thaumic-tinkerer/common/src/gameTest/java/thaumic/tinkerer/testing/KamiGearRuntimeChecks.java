/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): runtime checks of the KamiGear group, run by the Fabric GameTests. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.testing;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.wands.IFocusPouch;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerIchorPouch;
import thaumic.tinkerer.common.block.tile.container.slot.kami.SlotNoPouches;
import thaumic.tinkerer.common.content.KamiGear;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.handler.kami.KamiArmorHandler;
import thaumic.tinkerer.common.item.kami.ItemBlockTalisman;
import thaumic.tinkerer.common.item.kami.ItemIchorPouch;
import thaumic.tinkerer.common.item.kami.ItemPlacementMirror;
import thaumic.tinkerer.common.item.kami.ItemProtoclay;
import thaumic.tinkerer.common.item.kami.ItemSkyPearl;
import thaumic.tinkerer.common.item.kami.KamiComponents;
import thaumic.tinkerer.common.item.kami.KamiComponents.TalismanStore;
import thaumic.tinkerer.common.item.kami.armor.ItemGemChest;
import thaumic.tinkerer.common.item.kami.armor.ItemIchorclothArmorAdv;
import thaumic.tinkerer.common.item.kami.foci.ItemFocusXPDrain;
import thaumic.tinkerer.common.item.kami.foci.ShadowBeam;
import thaumic.tinkerer.common.item.kami.tool.ItemIchorPickAdv;
import thaumic.tinkerer.common.item.kami.tool.ToolHandler;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.TTRecipes;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>Runtime checks of the KamiGear group in a running server: registration, the data pack (tags, nugget recipe), the
 * mode and the persistence of the awakened tools, the Black Hole Ring (binding, filling a container, placing, the worn
 * merge), the Bottomless Pouch's menu, the Placement Mirror, the awakened armour's switches and flight, the Celestial
 * Pearl, the Protoclay, the Experience Drain's curve and the Shadowbeam. They were written without being able to run
 * them; the first real run on a Fabric dedicated server (30 checks) showed wrong assumptions of the checks and no defect
 * of the implementation: the harness' mock player starts in Creative mode (see {@code player(...)}),
 * {@code GameTestHelper.relativePos} is not the inverse of {@code absolutePos} for an unrotated test, a menu is opened for
 * the selected hotbar slot, and the target of a brain mob (piglin) is its {@code ATTACK_TARGET} memory.</p>
 */
public final class KamiGearRuntimeChecks {
    private KamiGearRuntimeChecks() {}

    private static final List<String> TOOLS = List.of(LibItemNames.ICHOR_PICK, LibItemNames.ICHOR_SHOVEL,
            LibItemNames.ICHOR_AXE, LibItemNames.ICHOR_SWORD, LibItemNames.ICHOR_PICK_GEM,
            LibItemNames.ICHOR_SHOVEL_GEM, LibItemNames.ICHOR_AXE_GEM, LibItemNames.ICHOR_SWORD_GEM);
    private static final List<String> ARMOR = List.of(LibItemNames.ICHOR_HELM, LibItemNames.ICHOR_CHEST,
            LibItemNames.ICHOR_LEGS, LibItemNames.ICHOR_BOOTS, LibItemNames.ICHOR_HELM_GEM,
            LibItemNames.ICHOR_CHEST_GEM, LibItemNames.ICHOR_LEGS_GEM, LibItemNames.ICHOR_BOOTS_GEM);
    private static final List<String> OTHERS = List.of(LibItemNames.CAT_AMULET, LibItemNames.ICHOR_POUCH,
            LibItemNames.BLOCK_TALISMAN, LibItemNames.PLACEMENT_MIRROR, LibItemNames.PROTOCLAY, LibItemNames.SKY_PEARL,
            LibItemNames.FOCUS_SHADOWBEAM, LibItemNames.FOCUS_XP_DRAIN, LibItemNames.FOCUS_RECALL);

    // ---- helpers ---------------------------------------------------------------------------------------------------

    /** A floor of stone at relative y = 0, so that items, mobs and the mock player stand. */
    private static void floor(GameTestHelper test) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    /**
     * A mock player (it joins the player list and ticks with the world) put at the relative position with the given
     * look. The harness' mock starts in Creative mode (abilities {@code mayfly} and {@code instabuild}, and no block
     * drops from {@code ServerPlayerGameMode.destroyBlock}), so the game mode is set to Survival here; only the
     * overridden {@code isCreative()} of the mock keeps answering true.
     */
    private static ServerPlayer player(GameTestHelper test, double x, double y, double z, float yaw, float pitch) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = test.absoluteVec(new Vec3(x, y, z));
        player.moveTo(at.x, at.y, at.z, yaw, pitch);
        player.getInventory().clearContent();
        return player;
    }

    private static ItemStack roundTrip(GameTestHelper test, ItemStack stack) {
        Tag saved = stack.save(test.getLevel().registryAccess());
        return ItemStack.parse(test.getLevel().registryAccess(), saved).orElseThrow();
    }

    private static InteractionResult click(GameTestHelper test, ServerPlayer player, BlockPos relative, Direction face) {
        BlockPos pos = test.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
        ItemStack held = player.getMainHandItem();
        return held.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static ResourceLocation blockId(net.minecraft.world.level.block.Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    // ---- registration ----------------------------------------------------------------------------------------------

    public static void registrationIsComplete(GameTestHelper test) {
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());

        for (String path : LibItemNames.KAMI_RESOURCE_NAMES) {
            test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(path)), "item " + path);
        }
        for (String path : TOOLS) test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(path)), "item " + path);
        for (String path : ARMOR) test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(path)), "item " + path);
        for (String path : OTHERS) test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(path)), "item " + path);

        test.assertTrue(ItemIchorPouch.MENU != null && ItemIchorPouch.MENU.isPresent(), "the pouch menu type");
        test.assertTrue(KamiComponents.TOOL_MODE.isPresent() && KamiComponents.PIECE_DISABLED.isPresent()
                && KamiComponents.TALISMAN_ACTIVE.isPresent() && KamiComponents.TALISMAN_STORE.isPresent()
                && KamiComponents.MIRROR_STATE.isPresent() && KamiComponents.MIRROR_SIZE.isPresent()
                && KamiComponents.PEARL_TARGET.isPresent() && KamiComponents.POUCH_CONTENTS.isPresent(),
                "the eight data components");

        // Rarity: the Kami items are epic (light purple), the two shards are common.
        for (String path : TOOLS) test.assertTrue(TTRegistry.stack(path).getRarity() == Rarity.EPIC, path + " is epic");
        for (String path : ARMOR) test.assertTrue(TTRegistry.stack(path).getRarity() == Rarity.EPIC, path + " is epic");
        test.assertTrue(TTRegistry.stack("ichor").getRarity() == Rarity.EPIC, "ichor is epic");
        test.assertTrue(TTRegistry.stack("nether_shard").getRarity() == Rarity.COMMON, "the nether shard is common");
        test.assertTrue(TTRegistry.stack("ender_shard").getRarity() == Rarity.COMMON, "the ender shard is common");

        // The wand parts register with Thaumcraft under the 1.7.10 tags, so that the research keys CAP_ICHOR and
        // ROD_ICHORCLOTH belong to them.
        WandCap cap = WandCap.byTag("ICHOR");
        test.assertTrue(cap != null, "the Ichorium cap");
        test.assertTrue(cap.getBaseCostModifier() == 0.7F && cap.getCraftCost() == 10, "cap discount 0.7, cost 10");
        test.assertTrue(cap.getResearch().equals(LibResearch.KEY_ICHOR_CAP), "cap research " + cap.getResearch());
        test.assertTrue(cap.getItem().is(TTRegistry.item("ichor_cap")), "the cap is made of the ichor cap item");
        WandRod rod = WandRod.byTag("ICHORCLOTH");
        test.assertTrue(rod != null, "the Ichorcloth rod");
        test.assertTrue(rod.getCapacity() == 1000 && rod.getCraftCost() == 10 && rod.isGlowing(), "rod 1000, cost 10, glows");
        test.assertTrue(rod.getResearch().equals(LibResearch.KEY_ICHORCLOTH_ROD), "rod research " + rod.getResearch());
        test.assertTrue(rod.getItem().is(TTRegistry.item("ichorcloth_rod")), "the rod is the ichorcloth rod item");

        for (String key : List.of(LibResearch.KEY_DIMENSION_SHARDS, LibResearch.KEY_ICHOR, LibResearch.KEY_ICHOR_CLOTH,
                LibResearch.KEY_ICHORIUM, LibResearch.KEY_ICHOR_CAP, LibResearch.KEY_ICHORCLOTH_ROD,
                LibResearch.KEY_ICHOR_TOOLS, LibResearch.KEY_ICHOR_PICK_GEM, LibResearch.KEY_ICHOR_SHOVEL_GEM,
                LibResearch.KEY_ICHOR_AXE_GEM, LibResearch.KEY_ICHOR_SWORD_GEM, LibResearch.KEY_ICHORCLOTH_ARMOR,
                LibResearch.KEY_ICHORCLOTH_HELM_GEM, LibResearch.KEY_ICHORCLOTH_CHEST_GEM,
                LibResearch.KEY_ICHORCLOTH_LEGS_GEM, LibResearch.KEY_ICHORCLOTH_BOOTS_GEM, LibResearch.KEY_CAT_AMULET,
                LibResearch.KEY_ICHOR_POUCH, LibResearch.KEY_BLOCK_TALISMAN, LibResearch.KEY_PLACEMENT_MIRROR,
                LibResearch.KEY_PROTOCLAY, LibResearch.KEY_FOCUS_SHADOWBEAM, LibResearch.KEY_FOCUS_XP_DRAIN,
                LibResearch.KEY_FOCUS_RECALL)) {
            test.assertTrue(ResearchCategories.getResearch(key) != null, "research " + key);
        }
        test.assertTrue(KamiGear.ichorResearch() != null, "the ICHOR entry is kept for the dimension group");

        for (String recipe : List.of(LibResearch.KEY_ICHOR, LibResearch.KEY_ICHOR_CLOTH, LibResearch.KEY_ICHORIUM,
                LibResearch.KEY_ICHOR_CAP, LibResearch.KEY_ICHORCLOTH_ROD, LibResearch.KEY_ICHORCLOTH_HELM,
                LibResearch.KEY_ICHORCLOTH_CHEST, LibResearch.KEY_ICHORCLOTH_LEGS, LibResearch.KEY_ICHORCLOTH_BOOTS,
                LibResearch.KEY_ICHORCLOTH_HELM_GEM, LibResearch.KEY_ICHORCLOTH_CHEST_GEM,
                LibResearch.KEY_ICHORCLOTH_LEGS_GEM, LibResearch.KEY_ICHORCLOTH_BOOTS_GEM, LibResearch.KEY_CAT_AMULET,
                LibResearch.KEY_ICHOR_PICK, LibResearch.KEY_ICHOR_SHOVEL, LibResearch.KEY_ICHOR_AXE,
                LibResearch.KEY_ICHOR_SWORD, LibResearch.KEY_ICHOR_PICK_GEM, LibResearch.KEY_ICHOR_SHOVEL_GEM,
                LibResearch.KEY_ICHOR_AXE_GEM, LibResearch.KEY_ICHOR_SWORD_GEM, LibResearch.KEY_ICHOR_POUCH,
                LibResearch.KEY_BLOCK_TALISMAN, LibResearch.KEY_PLACEMENT_MIRROR, LibResearch.KEY_PROTOCLAY,
                LibResearch.KEY_SKY_PEARL, LibResearch.KEY_FOCUS_SHADOWBEAM, LibResearch.KEY_FOCUS_XP_DRAIN,
                LibResearch.KEY_FOCUS_RECALL)) {
            test.assertTrue(TTRecipes.has(recipe), "recipe " + recipe);
        }
        test.succeed();
    }

    // ---- data pack ---------------------------------------------------------------------------------------------------

    public static void dataPackCarriesTheTagsAndTheNuggetRecipe(GameTestHelper test) {
        for (String path : List.of(LibItemNames.ICHOR_PICK, LibItemNames.ICHOR_PICK_GEM)) {
            ItemStack stack = TTRegistry.stack(path);
            test.assertTrue(stack.is(ItemTags.PICKAXES), path + " is a pickaxe");
            test.assertTrue(stack.is(ItemTags.MINING_ENCHANTABLE), path + " takes mining enchantments");
        }
        for (String path : List.of(LibItemNames.ICHOR_SHOVEL, LibItemNames.ICHOR_SHOVEL_GEM)) {
            test.assertTrue(TTRegistry.stack(path).is(ItemTags.SHOVELS), path + " is a shovel");
        }
        for (String path : List.of(LibItemNames.ICHOR_AXE, LibItemNames.ICHOR_AXE_GEM)) {
            test.assertTrue(TTRegistry.stack(path).is(ItemTags.AXES), path + " is an axe");
        }
        for (String path : List.of(LibItemNames.ICHOR_SWORD, LibItemNames.ICHOR_SWORD_GEM)) {
            ItemStack stack = TTRegistry.stack(path);
            test.assertTrue(stack.is(ItemTags.SWORDS), path + " is a sword");
            test.assertTrue(stack.is(ItemTags.SWORD_ENCHANTABLE), path + " takes sword enchantments");
        }
        test.assertTrue(TTRegistry.stack(LibItemNames.ICHOR_HELM).is(ItemTags.HEAD_ARMOR), "helm");
        test.assertTrue(TTRegistry.stack(LibItemNames.ICHOR_HELM_GEM).is(ItemTags.HEAD_ARMOR), "awakened helm");
        test.assertTrue(TTRegistry.stack(LibItemNames.ICHOR_CHEST).is(ItemTags.CHEST_ARMOR), "chest");
        test.assertTrue(TTRegistry.stack(LibItemNames.ICHOR_CHEST_GEM).is(ItemTags.CHEST_ARMOR), "awakened chest");
        test.assertTrue(TTRegistry.stack(LibItemNames.ICHOR_LEGS).is(ItemTags.LEG_ARMOR), "legs");
        test.assertTrue(TTRegistry.stack(LibItemNames.ICHOR_LEGS_GEM).is(ItemTags.LEG_ARMOR), "awakened legs");
        test.assertTrue(TTRegistry.stack(LibItemNames.ICHOR_BOOTS).is(ItemTags.FOOT_ARMOR), "boots");
        test.assertTrue(TTRegistry.stack(LibItemNames.ICHOR_BOOTS_GEM).is(ItemTags.FOOT_ARMOR), "awakened boots");
        test.assertTrue(TTRegistry.stack(LibItemNames.ICHOR_BOOTS_GEM).is(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                "armour takes armour enchantments");

        ServerLevel level = test.getLevel();
        ItemStack ichorium = TTRegistry.stack("ichorium");
        CraftingInput input = CraftingInput.of(1, 1, List.of(ichorium));
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input,
                level);
        test.assertTrue(recipe.isPresent(), "one ichorium has a crafting recipe");
        ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        test.assertTrue(result.is(TTRegistry.item("ichor_nugget")) && result.getCount() == 9,
                "nine nuggets, got " + result);
        test.succeed();
    }

    // ---- tools -------------------------------------------------------------------------------------------------------

    public static void toolsNeverBreakAndTakeEnchantments(GameTestHelper test) {
        for (String path : TOOLS) {
            ItemStack stack = TTRegistry.stack(path);
            test.assertTrue(stack.has(DataComponents.UNBREAKABLE), path + " is unbreakable");
            test.assertTrue(!stack.isDamageableItem(), path + " has no durability to lose");
            test.assertTrue(stack.isEnchantable(), path + " can be enchanted");
            test.assertTrue(stack.getItem().getEnchantmentValue() == 25, path + " enchantability 25");
        }
        for (String path : ARMOR) {
            ItemStack stack = TTRegistry.stack(path);
            test.assertTrue(!stack.isDamageableItem(), path + " has no durability to lose");
            test.assertTrue(stack.isEnchantable(), path + " can be enchanted");
        }
        test.succeed();
    }

    public static void awakenedToolModesCycleAndSurviveSaving(GameTestHelper test) {
        floor(test);
        for (String path : List.of(LibItemNames.ICHOR_PICK_GEM, LibItemNames.ICHOR_SHOVEL_GEM,
                LibItemNames.ICHOR_AXE_GEM, LibItemNames.ICHOR_SWORD_GEM)) {
            ItemStack stack = TTRegistry.stack(path);
            test.assertTrue(ToolHandler.getMode(stack) == 0, path + " starts in mode 0");
            ToolHandler.changeMode(stack);
            test.assertTrue(ToolHandler.getMode(stack) == 1, path + " mode 1");
            ToolHandler.changeMode(stack);
            test.assertTrue(ToolHandler.getMode(stack) == 2, path + " mode 2");

            ItemStack loaded = roundTrip(test, stack);
            test.assertTrue(ToolHandler.getMode(loaded) == 2, path + " keeps mode 2 through saving");
            ToolHandler.changeMode(loaded);
            test.assertTrue(ToolHandler.getMode(loaded) == 0, path + " wraps around to mode 0");
            test.assertTrue(ToolHandler.getMode(stack) == 2, "the copy is independent");
        }

        // Sneak-clicking the awakened pickaxe in the air cycles the mode.
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack pick = TTRegistry.stack(LibItemNames.ICHOR_PICK_GEM);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        player.setShiftKeyDown(true);
        pick.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(ToolHandler.getMode(pick) == 1, "sneak use cycles the mode");
        player.setShiftKeyDown(false);
        pick.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(ToolHandler.getMode(pick) == 1, "a plain use leaves the mode alone");
        test.succeed();
    }

    /** Builds a wall of stone at relative z = 4, x and y from 0 to 6, and a player that looks at its middle. */
    private static ServerPlayer wallAndMiner(GameTestHelper test, ItemStack tool) {
        floor(test);
        for (int x = 0; x <= 6; x++) for (int y = 0; y <= 6; y++) test.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
        ServerPlayer player = player(test, 3.5, 1, 1.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    private static int stoneInWall(GameTestHelper test) {
        int stone = 0;
        for (int x = 0; x <= 6; x++) {
            for (int y = 0; y <= 6; y++) {
                if (test.getBlockState(new BlockPos(x, y, 4)).is(Blocks.STONE)) stone++;
            }
        }
        return stone;
    }

    public static void awakenedPickaxeBreaksOneBlockInModeZeroAndASquareInModeOne(GameTestHelper test) {
        ItemStack pick = TTRegistry.stack(LibItemNames.ICHOR_PICK_GEM);
        ServerPlayer player = wallAndMiner(test, pick);
        // Eye height 2.62 above the floor: the ray meets the north face of the block at (3, 2, 4).
        BlockPos target = new BlockPos(3, 2, 4);
        test.assertTrue(stoneInWall(test) == 49, "the wall is 7 x 7");

        player.gameMode.destroyBlock(test.absolutePos(target));
        test.assertTrue(stoneInWall(test) == 48, "mode 0 breaks exactly one block, " + stoneInWall(test) + " left");

        test.setBlock(target, Blocks.STONE);
        ToolHandler.changeMode(pick);
        test.assertTrue(ToolHandler.getMode(pick) == 1, "square mode");
        player.gameMode.destroyBlock(test.absolutePos(target));
        // 1.7.10: x -2..+2 and y -1..+3 around the broken block, on a wall facing north: 25 blocks.
        int left = stoneInWall(test);
        test.assertTrue(left == 49 - 25, "the square takes 25 blocks: " + left + " left of 49");
        test.assertTrue(test.getBlockState(new BlockPos(0, 3, 4)).is(Blocks.STONE), "outside the square on the left");
        test.assertTrue(test.getBlockState(new BlockPos(6, 3, 4)).is(Blocks.STONE), "outside the square on the right");
        test.assertTrue(test.getBlockState(new BlockPos(3, 0, 4)).is(Blocks.STONE), "the row two below the block stays");
        test.assertTrue(test.getBlockState(new BlockPos(3, 6, 4)).is(Blocks.STONE), "the row four above the block stays");
        test.assertTrue(test.getBlockState(new BlockPos(1, 1, 4)).isAir(), "the square reaches one below the block");
        test.assertTrue(test.getBlockState(new BlockPos(5, 5, 4)).isAir(), "the square reaches three above the block");
        test.succeed();
    }

    public static void leftClickingBedrockWithTheAwakenedPickaxeBreaksTheSurroundings(GameTestHelper test) {
        ItemStack pick = TTRegistry.stack(LibItemNames.ICHOR_PICK_GEM);
        ToolHandler.changeMode(pick);
        ServerPlayer player = wallAndMiner(test, pick);
        BlockPos bedrock = new BlockPos(3, 2, 4);
        test.setBlock(bedrock, Blocks.BEDROCK);
        test.assertTrue(stoneInWall(test) == 48, "the wall is 48 stone and one bedrock");

        // 1.7.10 KamiDimensionHandler: a left click on bedrock runs onBlockStartBreak; in square mode without sneaking the
        // blocks around go (bedrock outside the Bedrock dimension stays), and the click is cancelled.
        var result = ItemIchorPickAdv.onLeftClickBlock(player,
                InteractionHand.MAIN_HAND, test.absolutePos(bedrock), Direction.NORTH);
        test.assertTrue(result.interruptsFurtherEvaluation() && result.isFalse(), "the click is cancelled");
        test.assertTrue(test.getBlockState(bedrock).is(Blocks.BEDROCK), "bedrock outside the Bedrock dimension stays");
        test.assertTrue(stoneInWall(test) == 48 - 24, "the 24 stone blocks around it went: " + stoneInWall(test) + " left");

        // Block mode leaves everything alone, and the click on anything but bedrock is not this handler's.
        ItemStack blockMode = TTRegistry.stack(LibItemNames.ICHOR_PICK_GEM);
        player.setItemInHand(InteractionHand.MAIN_HAND, blockMode);
        test.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
        var untouched = ItemIchorPickAdv.onLeftClickBlock(player,
                InteractionHand.MAIN_HAND, test.absolutePos(bedrock), Direction.NORTH);
        test.assertTrue(untouched.interruptsFurtherEvaluation(), "bedrock in block mode: still cancelled");
        test.assertTrue(test.getBlockState(new BlockPos(3, 3, 4)).is(Blocks.STONE), "and nothing around it breaks");
        var stoneClick = ItemIchorPickAdv.onLeftClickBlock(player,
                InteractionHand.MAIN_HAND, test.absolutePos(new BlockPos(3, 3, 4)), Direction.NORTH);
        test.assertTrue(stoneClick.isEmpty(), "a click on stone is left to the normal break");
        test.succeed();
    }

    public static void awakenedPickaxeLineModeDigsIntoTheWall(GameTestHelper test) {
        ItemStack pick = TTRegistry.stack(LibItemNames.ICHOR_PICK_GEM);
        ToolHandler.changeMode(pick);
        ToolHandler.changeMode(pick);
        ServerPlayer player = wallAndMiner(test, pick);
        for (int z = 5; z <= 7; z++) {
            for (int y = 1; y <= 5; y++) test.setBlock(new BlockPos(3, y, z), Blocks.STONE);
        }
        test.assertTrue(ToolHandler.getMode(pick) == 2, "line mode");
        player.gameMode.destroyBlock(test.absolutePos(new BlockPos(3, 2, 4)));
        for (int z = 4; z <= 7; z++) {
            test.assertTrue(test.getBlockState(new BlockPos(3, 2, z)).isAir(), "the line went through z=" + z);
        }
        test.assertTrue(test.getBlockState(new BlockPos(3, 3, 4)).is(Blocks.STONE), "only the line, not its neighbours");
        test.assertTrue(test.getBlockState(new BlockPos(2, 2, 4)).is(Blocks.STONE), "only the line, not its neighbours");
        test.succeed();
    }

    // ---- Black Hole Ring ---------------------------------------------------------------------------------------------

    public static void talismanBindsToAClickedBlockAndSurvivesSaving(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack ring = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        player.setItemInHand(InteractionHand.MAIN_HAND, ring);

        test.assertTrue(ItemBlockTalisman.getBlock(ring) == Blocks.AIR && ItemBlockTalisman.getBlockCount(ring) == 0,
                "a new ring is empty");
        InteractionResult bound = click(test, player, new BlockPos(2, 0, 2), Direction.UP);
        test.assertTrue(bound.consumesAction(), "the click binds the ring");
        test.assertTrue(ItemBlockTalisman.getBlock(ring) == Blocks.STONE, "bound to stone");
        test.assertTrue(ItemBlockTalisman.getBlockCount(ring) == 0, "with nothing in it");

        ring.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.STONE), 2_000_000_000));
        ring.set(KamiComponents.TALISMAN_ACTIVE.get(), true);
        ItemStack loaded = roundTrip(test, ring);
        test.assertTrue(ItemBlockTalisman.getBlock(loaded) == Blocks.STONE, "the block survives saving");
        test.assertTrue(ItemBlockTalisman.getBlockCount(loaded) == 2_000_000_000, "the count survives saving");
        test.assertTrue(ItemBlockTalisman.isActive(loaded), "the active flag survives saving");

        // remove() answers how many it took and never goes below zero.
        test.assertTrue(ItemBlockTalisman.remove(loaded, 5) == 5, "took five");
        test.assertTrue(ItemBlockTalisman.getBlockCount(loaded) == 2_000_000_000 - 5, "five fewer");
        ItemStack small = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        small.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.STONE), 3));
        test.assertTrue(ItemBlockTalisman.remove(small, 10) == 3, "it can only give what it has");
        test.assertTrue(ItemBlockTalisman.getBlockCount(small) == 0, "and ends empty");
        test.succeed();
    }

    public static void talismanFillsAChestAndLosesNothingWhenItIsFull(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack ring = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        ring.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.STONE), 100));
        player.setItemInHand(InteractionHand.MAIN_HAND, ring);

        BlockPos chestPos = new BlockPos(4, 1, 4);
        test.setBlock(chestPos, Blocks.CHEST);
        BlockEntity entity = test.getLevel().getBlockEntity(test.absolutePos(chestPos));
        test.assertTrue(entity instanceof ChestBlockEntity, "a chest block entity");
        ChestBlockEntity chest = (ChestBlockEntity) entity;

        InteractionResult result = click(test, player, chestPos, Direction.UP);
        test.assertTrue(result.consumesAction(), "the chest took blocks");
        test.assertTrue(chest.getItem(0).is(Items.STONE) && chest.getItem(0).getCount() == 64, "a full stack first");
        test.assertTrue(chest.getItem(1).is(Items.STONE) && chest.getItem(1).getCount() == 36, "then the rest");
        test.assertTrue(chest.getItem(2).isEmpty(), "and nothing more");
        test.assertTrue(ItemBlockTalisman.getBlockCount(ring) == 0, "the ring gave all 100");

        // A chest that cannot take the blocks keeps them in the ring (1.7.10 lost them).
        ItemStack ring2 = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        ring2.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.STONE), 10));
        player.setItemInHand(InteractionHand.MAIN_HAND, ring2);
        BlockPos fullPos = new BlockPos(2, 1, 4);
        test.setBlock(fullPos, Blocks.CHEST);
        ChestBlockEntity full = (ChestBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(fullPos));
        for (int i = 0; i < full.getContainerSize(); i++) full.setItem(i, new ItemStack(Items.DIRT, 64));
        InteractionResult refused = click(test, player, fullPos, Direction.UP);
        test.assertTrue(!refused.consumesAction(), "a full chest takes nothing");
        test.assertTrue(ItemBlockTalisman.getBlockCount(ring2) == 10, "the ring still holds its ten blocks");
        test.succeed();
    }

    public static void talismanPlacesBlocksOneByOneAndKeepsThemWhenBlocked(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack ring = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        ring.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.STONE), 5));
        player.setItemInHand(InteractionHand.MAIN_HAND, ring);

        InteractionResult placed = click(test, player, new BlockPos(1, 0, 1), Direction.UP);
        test.assertTrue(placed.consumesAction(), "the ring placed a block");
        test.assertTrue(test.getBlockState(new BlockPos(1, 1, 1)).is(Blocks.STONE), "on top of the clicked block");
        test.assertTrue(ItemBlockTalisman.getBlockCount(ring) == 4, "one block left the ring");

        // The block would stand inside the player: the placement fails and the ring keeps the block.
        InteractionResult blocked = click(test, player, new BlockPos(6, 0, 6), Direction.UP);
        test.assertTrue(!blocked.consumesAction(), "no block inside the player");
        test.assertTrue(test.getBlockState(new BlockPos(6, 1, 6)).isAir(), "nothing was placed");
        test.assertTrue(ItemBlockTalisman.getBlockCount(ring) == 4, "the ring still holds four blocks");
        test.succeed();
    }

    public static void talismanSneakClickTogglesAndTheRingIsNotEquippedByClicking(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack empty = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        player.setShiftKeyDown(true);
        empty.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(!ItemBlockTalisman.isActive(empty), "an unbound ring does not switch on");

        ItemStack ring = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        ring.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.STONE), 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, ring);
        player.setShiftKeyDown(false);
        ring.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(!ItemBlockTalisman.isActive(ring), "a plain click does not toggle");
        player.setShiftKeyDown(true);
        ring.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemBlockTalisman.isActive(ring), "sneak-clicking switches it on");
        ring.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(!ItemBlockTalisman.isActive(ring), "and off again");

        IBauble bauble = (IBauble) ring.getItem();
        test.assertTrue(!bauble.equipsOnUse(ring, player), "the click toggles the ring, it does not put it on");
        test.assertTrue(!((IBauble) TTRegistry.item(LibItemNames.ICHOR_POUCH)).equipsOnUse(
                TTRegistry.stack(LibItemNames.ICHOR_POUCH), player), "the click opens the pouch, it does not put it on");
        test.succeed();
    }

    public static void talismanWornMergesStacksWithoutLosingBlocks(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack ring = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        ring.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.STONE), 0));
        IBauble bauble = (IBauble) ring.getItem();

        player.getInventory().setItem(9, new ItemStack(Items.STONE, 10));
        player.getInventory().setItem(10, new ItemStack(Items.STONE, 20));
        player.getInventory().setItem(11, new ItemStack(Items.STONE, 5));
        bauble.onWornTick(ring, player);
        test.assertTrue(player.getInventory().getItem(9).getCount() == 10
                && player.getInventory().getItem(10).getCount() == 20 && player.getInventory().getItem(11).getCount() == 5
                && ItemBlockTalisman.getBlockCount(ring) == 0, "an inactive ring does not touch the stacks");

        ring.set(KamiComponents.TALISMAN_ACTIVE.get(), true);
        bauble.onWornTick(ring, player);
        int stone = count(player, Items.STONE);
        test.assertTrue(stone + ItemBlockTalisman.getBlockCount(ring) == 35, "35 blocks in all, found " + stone + " + "
                + ItemBlockTalisman.getBlockCount(ring));
        int stacks = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.STONE)) stacks++;
        }
        test.assertTrue(stacks == 1, "all the stone is in one stack, found " + stacks);
        test.assertTrue(stone == 35, "the stack holds all 35 blocks");

        // With no stone in the inventory the ring hands out a stack.
        player.getInventory().clearContent();
        ring.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.STONE), 100));
        bauble.onWornTick(ring, player);
        test.assertTrue(count(player, Items.STONE) == 64, "a stack of 64 is handed out");
        test.assertTrue(ItemBlockTalisman.getBlockCount(ring) == 36, "36 stay in the ring");
        test.succeed();
    }

    // ---- Bottomless Pouch ----------------------------------------------------------------------------------------------

    public static void pouchKeepsItsSlotsAndRefusesItself(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack pouch = TTRegistry.stack(LibItemNames.ICHOR_POUCH);
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, pouch);

        ContainerIchorPouch menu = new ContainerIchorPouch(7, player.getInventory(), InteractionHand.MAIN_HAND);
        test.assertTrue(menu.slots.size() == ItemIchorPouch.SIZE + 36, "117 pouch slots and the player's 36");
        ItemStack named = new ItemStack(Items.STICK, 3);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("kept"));
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 5));
        menu.getSlot(50).set(named);
        menu.getSlot(ItemIchorPouch.SIZE - 1).set(new ItemStack(Items.APPLE, 64));
        IFocusPouch api = (IFocusPouch) pouch.getItem();
        ItemStack[] stored = api.getInventory(player.getInventory().getItem(0));
        test.assertTrue(stored[0].is(Items.DIAMOND) && stored[0].getCount() == 5, "the pouch saved slot 0 at once");
        menu.removed(player);

        // Through the stack's own saving, as it travels in a chest or on disk.
        ItemStack loaded = roundTrip(test, player.getInventory().getItem(0));
        ItemStack[] back = api.getInventory(loaded);
        test.assertTrue(back.length == ItemIchorPouch.SIZE, "117 slots");
        test.assertTrue(back[0].is(Items.DIAMOND) && back[0].getCount() == 5, "diamonds in slot 0");
        test.assertTrue(back[50].is(Items.STICK) && back[50].getCount() == 3
                && back[50].get(DataComponents.CUSTOM_NAME) != null, "the named sticks in slot 50 keep their name");
        test.assertTrue(back[116].is(Items.APPLE) && back[116].getCount() == 64, "apples in the last slot");
        test.assertTrue(back[1].isEmpty() && back[115].isEmpty(), "the other slots are empty");
        back[0].shrink(1);
        test.assertTrue(api.getInventory(loaded)[0].getCount() == 5, "getInventory hands out copies");
        boolean refused = false;
        try {
            api.setInventory(loaded, new ItemStack[18]);
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        test.assertTrue(refused, "setInventory refuses an array of the wrong length");
        ItemStack[] swapped = api.getInventory(loaded);
        swapped[3] = new ItemStack(Items.GOLD_INGOT, 2);
        api.setInventory(loaded, swapped);
        test.assertTrue(api.getInventory(loaded)[3].is(Items.GOLD_INGOT), "setInventory stores a changed cell");
        test.assertTrue(api.getInventory(loaded)[0].is(Items.DIAMOND), "and keeps the others");

        // The pouch cannot be put into a pouch, its own slot is not to be touched, and the menu closes when it leaves.
        player.getInventory().setItem(0, loaded);
        ContainerIchorPouch again = new ContainerIchorPouch(8, player.getInventory(), InteractionHand.MAIN_HAND);
        test.assertTrue(again.getSlot(0).getItem().is(Items.DIAMOND), "a reopened pouch shows its items");
        test.assertTrue(!again.getSlot(1).mayPlace(TTRegistry.stack(LibItemNames.ICHOR_POUCH)), "no pouch in a pouch");
        test.assertTrue(again.getSlot(1).mayPlace(new ItemStack(Items.DIRT)), "dirt is welcome");
        test.assertTrue(SlotNoPouches.isPouch(TTRegistry.stack(LibItemNames.ICHOR_POUCH)), "the pouch is recognised");
        Item focusPouch = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "focus_pouch"));
        test.assertTrue(focusPouch != Items.AIR && SlotNoPouches.isPouch(new ItemStack(focusPouch)),
                "and so is Thaumcraft's own focus pouch");
        int heldSlot = ItemIchorPouch.SIZE + 27 + player.getInventory().selected;
        test.assertTrue(!again.getSlot(heldSlot).mayPickup(player), "the held pouch cannot be picked up");
        again.clicked(heldSlot, 0, ClickType.PICKUP, player);
        test.assertTrue(again.getCarried().isEmpty() && player.getInventory().getItem(0).getItem() instanceof ItemIchorPouch,
                "clicking the held pouch does nothing");
        test.assertTrue(again.quickMoveStack(player, heldSlot).isEmpty(), "and neither does shift-clicking it");
        test.assertTrue(again.stillValid(player), "valid while the pouch is held");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        test.assertTrue(!again.stillValid(player), "invalid when the pouch is gone");
        test.succeed();
    }

    public static void pouchOpensItsMenuOnUseInTheHandThatHoldsIt(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack pouch = TTRegistry.stack(LibItemNames.ICHOR_POUCH);
        player.setItemInHand(InteractionHand.MAIN_HAND, pouch);
        pouch.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(player.containerMenu instanceof ContainerIchorPouch, "the pouch menu opened");
        test.assertTrue(((ContainerIchorPouch) player.containerMenu).hand() == InteractionHand.MAIN_HAND,
                "from the main hand");
        player.closeContainer();

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ItemStack offhand = TTRegistry.stack(LibItemNames.ICHOR_POUCH);
        player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        offhand.getItem().use(test.getLevel(), player, InteractionHand.OFF_HAND);
        test.assertTrue(player.containerMenu instanceof ContainerIchorPouch
                && ((ContainerIchorPouch) player.containerMenu).hand() == InteractionHand.OFF_HAND, "and from the off hand");
        player.closeContainer();

        // The forbidden dimensions of the configuration keep the pouch shut.
        String[] saved = ConfigHandler.forbiddenDimensions;
        try {
            ConfigHandler.forbiddenDimensions = new String[] { test.getLevel().dimension().location().toString() };
            player.setItemInHand(InteractionHand.MAIN_HAND, pouch);
            pouch.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(!(player.containerMenu instanceof ContainerIchorPouch), "a forbidden dimension: shut");
        } finally {
            ConfigHandler.forbiddenDimensions = saved;
        }
        test.succeed();
    }

    // ---- Placement Mirror ------------------------------------------------------------------------------------------------

    public static void mirrorChoosesItsBlockAndCyclesItsSize(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack mirror = TTRegistry.stack(LibItemNames.PLACEMENT_MIRROR);
        player.setItemInHand(InteractionHand.MAIN_HAND, mirror);
        test.assertTrue(ItemPlacementMirror.getSize(mirror) == 3, "a new mirror is 3 x 3");
        test.assertTrue(ItemPlacementMirror.getBlockState(mirror) == null, "and has no block");

        test.setBlock(new BlockPos(2, 1, 2), Blocks.COBBLESTONE);
        player.setShiftKeyDown(true);
        click(test, player, new BlockPos(2, 1, 2), Direction.UP);
        BlockState chosen = ItemPlacementMirror.getBlockState(mirror);
        test.assertTrue(chosen != null && chosen.is(Blocks.COBBLESTONE), "sneak-clicking a block chooses it");

        int[] expected = { 5, 7, 9, 11, 3 };
        for (int size : expected) {
            mirror.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(ItemPlacementMirror.getSize(mirror) == size, "the next size is " + size);
        }

        ItemStack loaded = roundTrip(test, mirror);
        test.assertTrue(ItemPlacementMirror.getBlockState(loaded) != null
                && ItemPlacementMirror.getBlockState(loaded).is(Blocks.COBBLESTONE), "the block survives saving");
        test.assertTrue(ItemPlacementMirror.getSize(loaded) == 3, "and so does the size");
        test.succeed();
    }

    public static void mirrorPlacesTheSquareOnlyWithEnoughBlocksAndTakesThemFromTheInventory(GameTestHelper test) {
        floor(test);
        for (int x = 0; x <= 6; x++) for (int y = 1; y <= 5; y++) test.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
        ServerPlayer player = player(test, 3.5, 1, 1.5, 0, 0);
        ItemStack mirror = TTRegistry.stack(LibItemNames.PLACEMENT_MIRROR);
        mirror.set(KamiComponents.MIRROR_STATE.get(), Blocks.COBBLESTONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, mirror);
        ItemPlacementMirror item = (ItemPlacementMirror) mirror.getItem();

        // The ray meets the wall at (3, 2, 4); the 3 x 3 square stands in front of it, from that block upward.
        List<BlockPos> square = ItemPlacementMirror.getBlocksToPlace(mirror, player);
        test.assertTrue(square.size() == 9, "nine free positions, found " + square.size());
        // GameTestHelper.relativePos is not the inverse of absolutePos for an unrotated test (it turns the position by 180
        // degrees about the structure origin: x 2, z 3 came back as -2, -3), so the expected cells are compared as absolute
        // positions.
        Set<BlockPos> expectedSquare = new HashSet<>();
        for (int x = 2; x <= 4; x++) for (int y = 2; y <= 4; y++) expectedSquare.add(test.absolutePos(new BlockPos(x, y, 3)));
        for (BlockPos pos : square) {
            test.assertTrue(expectedSquare.contains(pos), "inside the square in front of the wall: " + pos
                    + ", relative to the test " + pos.subtract(test.absolutePos(BlockPos.ZERO)));
        }
        test.assertTrue(new HashSet<>(square).equals(expectedSquare), "and all nine cells of it: " + square);

        player.getInventory().add(new ItemStack(Items.COBBLESTONE, 5));
        item.placeAllBlocks(mirror, player);
        test.assertTrue(test.getBlockState(new BlockPos(3, 2, 3)).isAir(), "five blocks are not enough for nine");
        test.assertTrue(count(player, Items.COBBLESTONE) == 5, "and nothing was taken");

        player.getInventory().add(new ItemStack(Items.COBBLESTONE, 7));
        item.placeAllBlocks(mirror, player);
        for (int x = 2; x <= 4; x++) {
            for (int y = 2; y <= 4; y++) {
                test.assertTrue(test.getBlockState(new BlockPos(x, y, 3)).is(Blocks.COBBLESTONE),
                        "cobblestone at " + x + "," + y);
            }
        }
        test.assertTrue(count(player, Items.COBBLESTONE) == 12 - 9, "nine blocks were used, " + count(player, Items.COBBLESTONE)
                + " left");

        // The Black Hole Rings in the inventory pay for blocks as well.
        for (int x = 2; x <= 4; x++) for (int y = 2; y <= 4; y++) test.setBlock(new BlockPos(x, y, 3), Blocks.AIR);
        player.getInventory().clearContent();
        ItemStack ring = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        ring.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.COBBLESTONE), 20));
        player.getInventory().setItem(0, ring);
        item.placeAllBlocks(mirror, player);
        test.assertTrue(test.getBlockState(new BlockPos(3, 3, 3)).is(Blocks.COBBLESTONE), "placed from the ring");
        test.assertTrue(ItemBlockTalisman.getBlockCount(ring) == 11, "nine blocks left the ring, " + ItemBlockTalisman.getBlockCount(ring)
                + " remain");
        test.succeed();
    }

    // ---- awakened armour --------------------------------------------------------------------------------------------------

    public static void awakenedArmorPiecesSwitchOffOnSneakClickAndKeepTheSwitch(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        player.setShiftKeyDown(true);
        for (String path : List.of(LibItemNames.ICHOR_HELM_GEM, LibItemNames.ICHOR_CHEST_GEM,
                LibItemNames.ICHOR_LEGS_GEM, LibItemNames.ICHOR_BOOTS_GEM)) {
            ItemStack piece = TTRegistry.stack(path);
            test.assertTrue(!ItemIchorclothArmorAdv.isDisabled(piece), path + " starts enabled");
            player.setItemInHand(InteractionHand.MAIN_HAND, piece);
            piece.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(ItemIchorclothArmorAdv.isDisabled(piece), path + ": sneak-click switches it off");
            test.assertTrue(player.getItemBySlot(((net.minecraft.world.item.ArmorItem) piece.getItem()).getEquipmentSlot())
                    .isEmpty(), path + ": and does not put it on");
            ItemStack loaded = roundTrip(test, piece);
            test.assertTrue(ItemIchorclothArmorAdv.isDisabled(loaded), path + ": the switch survives saving");
            piece.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(!ItemIchorclothArmorAdv.isDisabled(piece), path + ": and on again");
        }
        test.succeed();
    }

    public static void awakenedChestplateGrantsFlightOnlyWhileItIsOnAndSwitchedOn(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        boolean flight = ConfigHandler.enableFlight;
        try {
            ConfigHandler.enableFlight = true;
            test.assertTrue(!player.getAbilities().mayfly, "a survival player cannot fly");
            ItemStack chest = TTRegistry.stack(LibItemNames.ICHOR_CHEST_GEM);
            player.setItemSlot(EquipmentSlot.CHEST, chest);
            KamiArmorHandler.onPlayerTick(player);
            test.assertTrue(player.getAbilities().mayfly, "the chestplate grants flight");

            chest.set(KamiComponents.PIECE_DISABLED.get(), true);
            KamiArmorHandler.onPlayerTick(player);
            test.assertTrue(!player.getAbilities().mayfly, "switched off, it takes the flight away");

            chest.set(KamiComponents.PIECE_DISABLED.get(), false);
            KamiArmorHandler.onPlayerTick(player);
            test.assertTrue(player.getAbilities().mayfly, "switched on, it grants it again");

            KamiArmorHandler.setArmorStatus(player, false);
            test.assertTrue(!KamiArmorHandler.getArmorStatus(player), "the toggle key's master switch is off");
            KamiArmorHandler.onPlayerTick(player);
            test.assertTrue(!player.getAbilities().mayfly, "with the master switch off there is no flight");
            KamiArmorHandler.setArmorStatus(player, true);
            test.assertTrue(KamiArmorHandler.getArmorStatus(player), "and on again");
            KamiArmorHandler.onPlayerTick(player);
            test.assertTrue(player.getAbilities().mayfly, "flight is back");

            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            KamiArmorHandler.onPlayerTick(player);
            test.assertTrue(!player.getAbilities().mayfly, "taking the chestplate off ends the flight");

            ConfigHandler.enableFlight = false;
            player.setItemSlot(EquipmentSlot.CHEST, chest);
            KamiArmorHandler.onPlayerTick(player);
            test.assertTrue(!player.getAbilities().mayfly, "the configuration can forbid the flight");
        } finally {
            ConfigHandler.enableFlight = flight;
            KamiArmorHandler.setArmorStatus(player, true);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            KamiArmorHandler.onPlayerTick(player);
        }
        test.succeed();
    }

    // ---- Celestial Pearl, Protoclay ---------------------------------------------------------------------------------------

    public static void skyPearlKeepsItsDestinationUntilSneakClicked(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack pearl = TTRegistry.stack(LibItemNames.SKY_PEARL);
        test.assertTrue(!ItemSkyPearl.isAttuned(pearl) && !pearl.hasFoil(), "a new pearl is not attuned and does not glow");

        BlockPos target = test.absolutePos(new BlockPos(3, 1, 3));
        ItemSkyPearl.setValues(pearl, test.getLevel().dimension(), target);
        test.assertTrue(ItemSkyPearl.isAttuned(pearl) && pearl.hasFoil(), "an attuned pearl glows");
        ItemStack loaded = roundTrip(test, pearl);
        test.assertTrue(ItemSkyPearl.isAttuned(loaded), "attunement survives saving");
        test.assertTrue(ItemSkyPearl.getPos(loaded).equals(target), "the position survives saving");
        test.assertTrue(test.getLevel().dimension().equals(ItemSkyPearl.getDim(loaded)), "so does the dimension");

        List<Component> lines = new java.util.ArrayList<>();
        ItemSkyPearl.addInfo(loaded, test.getLevel().dimension(),
                new Vec3(target.getX(), target.getY(), target.getZ()).add(3, 0, 4), lines, false);
        test.assertTrue(lines.size() == 4, "X, Y, Z and the distance, found " + lines.size());
        test.assertTrue(lines.get(3).getString().endsWith(": 5.00m"), "the distance is 5: " + lines.get(3).getString());
        lines.clear();
        ItemSkyPearl.addInfo(loaded, net.minecraft.world.level.Level.NETHER, Vec3.ZERO, lines, false);
        test.assertTrue(lines.size() == 4 && lines.get(3).getString().length() > 0, "another dimension has a note instead");
        lines.clear();
        ItemSkyPearl.addInfo(loaded, test.getLevel().dimension(), Vec3.ZERO, lines, true);
        test.assertTrue(lines.size() == 3, "simple mode: X, Z and the distance, found " + lines.size());

        player.setItemInHand(InteractionHand.MAIN_HAND, pearl);
        player.setShiftKeyDown(false);
        pearl.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemSkyPearl.isAttuned(pearl), "a plain click keeps the destination");
        player.setShiftKeyDown(true);
        pearl.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(!ItemSkyPearl.isAttuned(pearl), "sneak-clicking forgets it");
        test.succeed();
    }

    public static void protoclayPutsTheRightAwakenedToolInTheHand(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        test.setBlock(new BlockPos(2, 1, 2), Blocks.OAK_LOG);
        test.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
        ItemStack axe = TTRegistry.stack(LibItemNames.ICHOR_AXE_GEM);
        ItemStack pick = TTRegistry.stack(LibItemNames.ICHOR_PICK_GEM);
        ItemStack sword = TTRegistry.stack(LibItemNames.ICHOR_SWORD_GEM);
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, axe);
        player.getInventory().setItem(5, pick);
        player.getInventory().setItem(6, sword);

        // Without the protoclay in the inventory nothing happens.
        var untouched = ItemProtoclay.onLeftClickBlock(player, InteractionHand.MAIN_HAND, test.absolutePos(new BlockPos(3, 1, 2)),
                Direction.UP);
        test.assertTrue(untouched.isEmpty() && player.getMainHandItem() == axe, "no protoclay, no swap");

        player.getInventory().setItem(7, TTRegistry.stack(LibItemNames.PROTOCLAY));
        var onStone = ItemProtoclay.onLeftClickBlock(player, InteractionHand.MAIN_HAND, test.absolutePos(new BlockPos(3, 1, 2)),
                Direction.UP);
        test.assertTrue(player.getMainHandItem() == pick, "stone: the pickaxe came to the hand");
        test.assertTrue(player.getInventory().getItem(5) == axe, "and the axe took its place");
        test.assertTrue(onStone.interruptsFurtherEvaluation() && onStone.isFalse(), "the click that swapped is cancelled");

        var onWood = ItemProtoclay.onLeftClickBlock(player, InteractionHand.MAIN_HAND, test.absolutePos(new BlockPos(2, 1, 2)),
                Direction.UP);
        test.assertTrue(player.getMainHandItem() == axe, "log: the axe came back");
        test.assertTrue(onWood.interruptsFurtherEvaluation() && onWood.isFalse(), "and the click is cancelled again");

        // A tool of the right kind in the hand is left alone.
        var again = ItemProtoclay.onLeftClickBlock(player, InteractionHand.MAIN_HAND, test.absolutePos(new BlockPos(2, 1, 2)),
                Direction.UP);
        test.assertTrue(again.isEmpty() && player.getMainHandItem() == axe, "the right tool stays");

        // A sword is never swapped away.
        player.getInventory().setItem(0, sword);
        var swordHand = ItemProtoclay.onLeftClickBlock(player, InteractionHand.MAIN_HAND, test.absolutePos(new BlockPos(3, 1, 2)),
                Direction.UP);
        test.assertTrue(swordHand.isEmpty() && player.getMainHandItem() == sword, "a sword in hand is left alone");
        test.succeed();
    }

    // ---- Experience Drain, Shadowbeam ------------------------------------------------------------------------------------------

    public static void experienceDrainRemovesExperiencePointsNotLevels(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        player.giveExperiencePoints(500);
        test.assertTrue(ItemFocusXPDrain.getPlayerXP(player) == 500, "500 points, found " + ItemFocusXPDrain.getPlayerXP(player));
        ItemFocusXPDrain.drainPlayerXP(player, 200);
        test.assertTrue(ItemFocusXPDrain.getPlayerXP(player) == 300, "300 points left, found " + ItemFocusXPDrain.getPlayerXP(player));
        test.assertTrue(player.experienceLevel == ItemFocusXPDrain.getLevelForExperience(300),
                "the level follows the points: " + player.experienceLevel);
        ItemFocusXPDrain.drainPlayerXP(player, 5000);
        test.assertTrue(ItemFocusXPDrain.getPlayerXP(player) == 0 && player.experienceLevel == 0, "it cannot go below zero");
        test.succeed();
    }

    public static void shadowbeamHurtsWhatItCrossesAndBouncesOffWalls(GameTestHelper test) {
        floor(test);
        for (int x = 0; x <= 7; x++) for (int y = 1; y <= 7; y++) test.setBlock(new BlockPos(x, y, 7), Blocks.STONE);
        ServerPlayer player = player(test, 3.5, 1, 1.5, 0, 0);
        Zombie zombie = test.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 4));
        float health = zombie.getHealth();

        List<Vec3> path = ShadowBeam.fire(player, 0, 2);
        test.assertTrue(path.size() >= 2, "the beam has a visible path, found " + path.size() + " points");
        test.assertTrue(!zombie.isAlive() || zombie.getHealth() < health, "the zombie in the way was hurt");

        // A potency level makes it hit harder (damage 2 -> 2 + 0.2 * 2 * 5 = 4 at potency 5).
        test.assertTrue(ShadowBeam.damage(2, 5) == 4, "potency raises the damage");
        test.succeed();
    }

    // ---- review round: regressions of the findings --------------------------------------------------------------------------

    /** Wall of stone at z = 4 (x 0..6, y 1..5) and a player in front of it who looks at it; the mirror holds cobblestone. */
    private static ServerPlayer mirrorHolder(GameTestHelper test, ItemStack mirror) {
        floor(test);
        for (int x = 0; x <= 6; x++) for (int y = 1; y <= 5; y++) test.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
        ServerPlayer player = player(test, 3.5, 1, 1.5, 0, 0);
        mirror.set(KamiComponents.MIRROR_STATE.get(), Blocks.COBBLESTONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, mirror);
        return player;
    }

    public static void mirrorNeedsNoBlocksInCreativeMode(GameTestHelper test) {
        ItemStack mirror = TTRegistry.stack(LibItemNames.PLACEMENT_MIRROR);
        ServerPlayer player = mirrorHolder(test, mirror);
        ItemPlacementMirror item = (ItemPlacementMirror) mirror.getItem();
        test.assertTrue(!ItemPlacementMirror.hasBlocks(player, Blocks.COBBLESTONE, 9), "a survival player with nothing has no blocks");
        item.placeAllBlocks(mirror, player);
        test.assertTrue(test.getBlockState(new BlockPos(3, 2, 3)).isAir(), "and the mirror places nothing for them");

        // 1.7.10 hasBlocks: "if (player.capabilities.isCreativeMode) return true"; the placing then consumes nothing.
        player.getAbilities().instabuild = true;
        try {
            test.assertTrue(ItemPlacementMirror.hasBlocks(player, Blocks.COBBLESTONE, 9), "a creative player always has the blocks");
            item.placeAllBlocks(mirror, player);
            for (int x = 2; x <= 4; x++) {
                for (int y = 2; y <= 4; y++) {
                    test.assertTrue(test.getBlockState(new BlockPos(x, y, 3)).is(Blocks.COBBLESTONE), "cobblestone at " + x + "," + y);
                }
            }
            test.assertTrue(count(player, Items.COBBLESTONE) == 0, "from an empty inventory");
        } finally {
            player.getAbilities().instabuild = false;
        }
        test.succeed();
    }

    public static void mirrorPaysFromTheFirstRingThatHasBlocks(GameTestHelper test) {
        ItemStack mirror = TTRegistry.stack(LibItemNames.PLACEMENT_MIRROR);
        ServerPlayer player = mirrorHolder(test, mirror);
        ItemStack empty = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        empty.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.COBBLESTONE), 0));
        ItemStack full = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        full.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(blockId(Blocks.COBBLESTONE), 20));
        player.getInventory().setItem(0, empty);
        player.getInventory().setItem(1, full);

        ((ItemPlacementMirror) mirror.getItem()).placeAllBlocks(mirror, player);
        test.assertTrue(test.getBlockState(new BlockPos(3, 3, 3)).is(Blocks.COBBLESTONE), "the square was placed");
        test.assertTrue(ItemBlockTalisman.getBlockCount(empty) == 0, "the empty ring stays empty");
        test.assertTrue(ItemBlockTalisman.getBlockCount(full) == 11,
                "the full ring paid for all nine blocks, " + ItemBlockTalisman.getBlockCount(full) + " remain");
        test.succeed();
    }

    public static void mirrorRefusesStairsAndTakesSlabs(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack mirror = TTRegistry.stack(LibItemNames.PLACEMENT_MIRROR);
        player.setItemInHand(InteractionHand.MAIN_HAND, mirror);
        player.setShiftKeyDown(true);

        test.setBlock(new BlockPos(2, 1, 2), Blocks.OAK_STAIRS);
        click(test, player, new BlockPos(2, 1, 2), Direction.UP);
        test.assertTrue(ItemPlacementMirror.getBlockState(mirror) == null, "stairs are not remembered (1.7.10 render type 10)");
        test.setBlock(new BlockPos(2, 1, 2), Blocks.OAK_DOOR);
        click(test, player, new BlockPos(2, 1, 2), Direction.UP);
        test.assertTrue(ItemPlacementMirror.getBlockState(mirror) == null, "doors are not remembered");
        test.setBlock(new BlockPos(2, 1, 2), Blocks.STONE_SLAB);
        click(test, player, new BlockPos(2, 1, 2), Direction.UP);
        test.assertTrue(ItemPlacementMirror.getBlockState(mirror) != null && ItemPlacementMirror.getBlockState(mirror).is(Blocks.STONE_SLAB),
                "slabs are");
        test.succeed();
    }

    public static void pouchMenuWritesOnlyToThePouchItWasOpenedFor(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 6.5, 1, 6.5, 0, 0);
        ItemStack first = TTRegistry.stack(LibItemNames.ICHOR_POUCH);
        ItemStack second = TTRegistry.stack(LibItemNames.ICHOR_POUCH);
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, first);
        player.getInventory().setItem(1, second);
        IFocusPouch api = (IFocusPouch) first.getItem();

        ContainerIchorPouch menu = new ContainerIchorPouch(9, player.getInventory(), InteractionHand.MAIN_HAND);
        // A modified client may select another hotbar slot while the menu is open (the server accepts that packet).
        player.getInventory().selected = 1;
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 5));
        test.assertTrue(api.getInventory(first)[0].is(Items.DIAMOND) && api.getInventory(first)[0].getCount() == 5,
                "the pouch the menu was opened for got the diamonds");
        test.assertTrue(api.getInventory(second)[0].isEmpty(), "the pouch that is in the hand now got nothing");
        test.assertTrue(menu.stillValid(player), "the opened pouch has not moved, so the menu stays open");

        // The hotbar key of the pouch's own slot cannot move it, whichever slot is selected.
        menu.clicked(ItemIchorPouch.SIZE, 0, ClickType.SWAP, player);
        test.assertTrue(player.getInventory().getItem(0) == first, "the swap with the pouch's slot is refused");

        // Something else writes the pouch (the wand's focus selector does): the menu closes and does not write over it.
        ItemStack[] outside = api.getInventory(first);
        outside[3] = new ItemStack(Items.GOLD_INGOT, 2);
        api.setInventory(first, outside);
        test.assertTrue(!menu.stillValid(player), "a write behind the open menu closes it");
        menu.getSlot(1).set(new ItemStack(Items.STONE));
        menu.removed(player);
        test.assertTrue(api.getInventory(first)[3].is(Items.GOLD_INGOT), "and the outside write survives");
        test.assertTrue(api.getInventory(first)[1].isEmpty(), "the menu's stale cells were not written");

        // A pouch that is replaced in its slot closes the menu too. The menu is opened for the selected slot, so the
        // selection (moved to slot 1 above) goes back to the slot of the pouch the new menu is meant for.
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, first);
        ContainerIchorPouch again = new ContainerIchorPouch(10, player.getInventory(), InteractionHand.MAIN_HAND);
        test.assertTrue(again.stillValid(player), "freshly opened");
        player.getInventory().setItem(0, second);
        test.assertTrue(!again.stillValid(player), "another pouch in the slot is not the pouch of the menu");
        test.succeed();
    }

    public static void catAmuletScaresCreepersAndLeavesProvokedAndBrainMobsAlone(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0, 0);
        ItemStack amulet = TTRegistry.stack(LibItemNames.CAT_AMULET);
        IBauble bauble = (IBauble) amulet.getItem();
        test.assertTrue(bauble.getBaubleType(amulet) == thaumcraft.api.baubles.BaubleType.AMULET, "an amulet");

        Creeper creeper = test.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(5, 1, 3));
        Sheep sheep = test.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(1, 1, 3));
        Zombie hunter = test.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 6));
        Zombie avenger = test.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        Piglin piglin = test.spawnWithNoFreeWill(EntityType.PIGLIN, new BlockPos(6, 1, 6));
        creeper.setTarget(player);
        hunter.setTarget(player);
        avenger.setTarget(player);
        avenger.setLastHurtByMob(player);
        // A mob that thinks with a brain keeps its target in the ATTACK_TARGET memory: AbstractPiglin.getTarget() reads the
        // memory and ignores the field that Mob.setTarget writes (setTarget(player) would leave getTarget() null).
        piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
        test.assertTrue(creeper.getTarget() == player && hunter.getTarget() == player && avenger.getTarget() == player
                && piglin.getTarget() == player, "set-up: all four go after the wearer");

        // Mobs with no brain memories (all of these but the piglin) used to make the amulet throw.
        try {
            bauble.onWornTick(amulet, player);
        } catch (RuntimeException e) {
            test.fail("the amulet threw: " + e);
        }
        test.assertTrue(creeper.getTarget() == null, "the creeper lost the wearer");
        test.assertTrue(sheep.isAlive() && sheep.getTarget() == null, "the sheep was left alone");
        test.assertTrue(hunter.getTarget() == null, "the zombie that merely hunted the wearer lost them");
        test.assertTrue(avenger.getTarget() == player, "the zombie the wearer hit keeps its revenge");
        test.assertTrue(piglin.getTarget() == player, "a mob that thinks with a brain is not touched");
        test.succeed();
    }

    public static void stuckProjectilesAreNotPushedByTheRobes(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5, 0, 0);
        Arrow flying = test.spawn(EntityType.ARROW, new BlockPos(5, 2, 3));
        Arrow stuck = test.spawn(EntityType.ARROW, new BlockPos(5, 2, 5));
        for (Arrow arrow : new Arrow[] { flying, stuck }) {
            arrow.setNoGravity(true);
            arrow.setDeltaMovement(-1, 0, 0);
        }
        double stuckX = stuck.getX();
        // A flying arrow started its tick one step behind where it is; a stuck one has not moved (its vector is kept in 1.21.1).
        flying.xo = flying.getX() + 1;
        stuck.xo = stuck.getX();
        double flyingX = flying.getX();

        ItemGemChest.protectFromProjectiles(player, 0);
        test.assertTrue(flying.getX() < flyingX - 1, "the flying arrow was carried past the wearer, x = " + flying.getX());
        test.assertTrue(stuck.getX() == stuckX, "the stuck arrow stayed where it is, x = " + stuck.getX());
        test.succeed();
    }

    public static void awakenedPickaxeGathersTheDropsOfTheClickedBlockToo(GameTestHelper test) {
        ItemStack pick = TTRegistry.stack(LibItemNames.ICHOR_PICK_GEM);
        ToolHandler.changeMode(pick);
        ServerPlayer player = wallAndMiner(test, pick);
        player.gameMode.destroyBlock(test.absolutePos(new BlockPos(3, 2, 4)));
        // 1.7.10 broke the clicked block inside the loop and moved its drops too; here they are moved at the end of the tick.
        test.runAfterDelay(2, () -> {
            AABB nearWall = AABB.encapsulatingFullBlocks(test.absolutePos(new BlockPos(-1, 0, -1)), test.absolutePos(new BlockPos(8, 8, 8)));
            int atWall = 0;
            int atPlayer = 0;
            for (ItemEntity item : test.getLevel().getEntitiesOfClass(ItemEntity.class, nearWall)) {
                if (!item.getItem().is(Items.COBBLESTONE)) continue;
                if (item.distanceToSqr(player) < 9) atPlayer += item.getItem().getCount();
                else atWall += item.getItem().getCount();
            }
            test.assertTrue(atWall == 0, atWall + " cobblestone were left at the wall");
            test.assertTrue(atPlayer == 25, "all 25 drops came to the player, found " + atPlayer);
            test.succeed();
        });
    }

    public static void awakenedShovelColumnModeTakesOnlyTheSameBlockAlongTheColumn(GameTestHelper test) {
        floor(test);
        for (int y = 1; y <= 12; y++) test.setBlock(new BlockPos(3, y, 4), Blocks.DIRT);
        test.setBlock(new BlockPos(3, 5, 4), Blocks.STONE);
        test.setBlock(new BlockPos(2, 2, 4), Blocks.DIRT);
        ItemStack shovel = TTRegistry.stack(LibItemNames.ICHOR_SHOVEL_GEM);
        ToolHandler.changeMode(shovel);
        ToolHandler.changeMode(shovel);
        ServerPlayer player = player(test, 3.5, 1, 1.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);

        // The ray meets the block at (3, 2, 4); the column mode takes 8 blocks below it and 7 above (1.7.10), dirt only.
        player.gameMode.destroyBlock(test.absolutePos(new BlockPos(3, 2, 4)));
        for (int y = 1; y <= 9; y++) {
            BlockState state = test.getBlockState(new BlockPos(3, y, 4));
            test.assertTrue(y == 5 ? state.is(Blocks.STONE) : state.isAir(), "y=" + y + " is " + state);
        }
        for (int y = 10; y <= 12; y++) {
            test.assertTrue(test.getBlockState(new BlockPos(3, y, 4)).is(Blocks.DIRT), "y=" + y + " is beyond the seven above");
        }
        test.assertTrue(test.getBlockState(new BlockPos(2, 2, 4)).is(Blocks.DIRT), "the neighbouring column stays");
        test.succeed();
    }
}
