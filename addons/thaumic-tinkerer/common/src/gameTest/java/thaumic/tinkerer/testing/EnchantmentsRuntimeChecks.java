package thaumic.tinkerer.testing;

import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandHelper;
import thaumcraft.api.wands.WandRod;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.block.tile.container.ContainerEnchanter;
import thaumic.tinkerer.common.content.Enchantments;
import thaumic.tinkerer.common.enchantment.AttackBoost;
import thaumic.tinkerer.common.enchantment.BreakSpeedControl;
import thaumic.tinkerer.common.enchantment.EnchantmentFormulas;
import thaumic.tinkerer.common.enchantment.ModEnchantments;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibEnchantIDs;
import thaumic.tinkerer.common.lib.LibEnchantNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.network.packet.PacketEnchanterAddEnchant;
import thaumic.tinkerer.common.network.packet.PacketEnchanterStartWorking;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>Runtime checks of the Enchantments group in a running server: the data pack records and what the world made of them,
 * the research entries, the Osmotic Enchanter (persistence, a whole enchanting from a full wand between six pillars, the
 * refusal without pillars, the locked slots), and the effect of each family of enchantments (the multiplied attack, the
 * dig speed and the refused blocks, Vampirism, Slow Fall, Shockwave, Quick Draw, Tunnel). They were written without being
 * able to run them (no Gradle in the porting session): the first run may show a wrong assumption of a check as well as a
 * defect.</p>
 */
public final class EnchantmentsRuntimeChecks {
    private EnchantmentsRuntimeChecks() {}

    /** The vanilla enchantments the checks use (the add-on's group has a class of its own called Enchantments). */
    private static final class Vanilla {
        static final ResourceKey<Enchantment> SHARPNESS = net.minecraft.world.item.enchantment.Enchantments.SHARPNESS;
        static final ResourceKey<Enchantment> SMITE = net.minecraft.world.item.enchantment.Enchantments.SMITE;
        static final ResourceKey<Enchantment> KNOCKBACK = net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK;
        static final ResourceKey<Enchantment> LOOTING = net.minecraft.world.item.enchantment.Enchantments.LOOTING;
        static final ResourceKey<Enchantment> EFFICIENCY = net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY;
        static final ResourceKey<Enchantment> UNBREAKING = net.minecraft.world.item.enchantment.Enchantments.UNBREAKING;
        static final ResourceKey<Enchantment> FORTUNE = net.minecraft.world.item.enchantment.Enchantments.FORTUNE;
        static final ResourceKey<Enchantment> MENDING = net.minecraft.world.item.enchantment.Enchantments.MENDING;
        static final ResourceKey<Enchantment> FEATHER_FALLING = net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING;
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------

    /** A floor of stone at relative y = 0, so that blocks and mobs stand. */
    private static void floor(GameTestHelper test) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static Registry<Enchantment> registry(GameTestHelper test) {
        return test.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }

    private static Holder<Enchantment> holder(GameTestHelper test, ResourceKey<Enchantment> key) {
        return registry(test).getHolderOrThrow(key);
    }

    private static ItemStack enchanted(GameTestHelper test, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        stack.enchant(holder(test, key), level);
        return stack;
    }

    /**
     * A mock player on the stone floor, away from the blocks the checks place, in survival mode (the mock leaves the game mode
     * to the world's default, and creative would let every block break). Nothing ticks a mock player; the checks call
     * {@code doTick} where time has to pass for it.
     */
    private static ServerPlayer player(GameTestHelper test) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(test.absoluteVec(new Vec3(6.5, 1.0, 6.5)));
        player.getInventory().clearContent();
        return player;
    }

    private static int[] visOf(ItemStack wand) {
        AspectList all = WandHelper.getAllVis(wand);
        Aspect[] primals = TileEnchanter.PRIMAL_ASPECTS;
        int[] amounts = new int[primals.length];
        for (int i = 0; i < primals.length; i++) amounts[i] = all.getAmount(primals[i]);
        return amounts;
    }

    private static void finish(GameTestHelper test, ServerPlayer player) {
        player.discard();
        test.succeed();
    }

    /** A wand of greatwood with a thaumium cap, every primal full. */
    private static ItemStack fullWand(GameTestHelper test) {
        ItemStack wand = ItemApi.getItem("itemWandCasting", 0);
        test.assertTrue(WandHelper.isWand(wand), "a wand can be made");
        WandHelper.setRod(wand, WandRod.byTag("greatwood"));
        WandHelper.setCap(wand, WandCap.byTag("thaumium"));
        for (Aspect primal : Aspect.getPrimalAspects()) WandHelper.addRealVis(wand, primal, WandHelper.getMaxVis(wand), true);
        test.assertTrue(!WandHelper.isStaff(wand), "a wand, not a staff (the Enchanter refuses staffs)");
        return wand;
    }

    private static final BlockPos ENCHANTER = new BlockPos(4, 1, 4);

    /** Six columns of two Obsidian Totems under a Nitor around the enchanter (inside the 8 by 8 area of the test). */
    private static void pillars(GameTestHelper test, int[][] columns) {
        BlockState totem = BlocksTC.state("blockCosmeticSolid", 0);
        BlockState nitor = BlocksTC.state("blockAiry", 1);
        for (int[] column : columns) {
            test.setBlock(new BlockPos(column[0], 1, column[1]), totem);
            test.setBlock(new BlockPos(column[0], 2, column[1]), totem);
            test.setBlock(new BlockPos(column[0], 3, column[1]), nitor);
        }
    }

    private static final int[][] SIX_COLUMNS = { { 1, 4 }, { 7, 4 }, { 4, 1 }, { 4, 7 }, { 1, 1 }, { 7, 7 } };

    private static TileEnchanter enchanter(GameTestHelper test) {
        test.setBlock(ENCHANTER, TTRegistry.block(LibBlockNames.ENCHANTER));
        BlockEntity entity = test.getLevel().getBlockEntity(test.absolutePos(ENCHANTER));
        test.assertTrue(entity instanceof TileEnchanter, "an enchanter block entity, found " + entity);
        return (TileEnchanter) entity;
    }

    // ---- registration ------------------------------------------------------------------------------------------------

    public static void registrationIsComplete(GameTestHelper test) {
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        test.assertTrue(BuiltInRegistries.BLOCK.containsKey(LibMisc.id(LibBlockNames.ENCHANTER)), "the block");
        test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(LibBlockNames.ENCHANTER)), "the item");
        test.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(LibMisc.id(LibBlockNames.ENCHANTER)), "the block entity type");
        test.assertTrue(BuiltInRegistries.MENU.containsKey(LibMisc.id(LibBlockNames.ENCHANTER)), "the menu");
        test.assertTrue(Enchantments.enchanterBlock().defaultBlockState().is(TTRegistry.block(LibBlockNames.ENCHANTER)), "the accessor");
        test.succeed();
    }

    public static void researchEntriesAreRegistered(GameTestHelper test) {
        ResearchItem enchanter = ResearchCategories.getResearch(LibResearch.KEY_ENCHANTER);
        test.assertTrue(enchanter != null, "the entry of the Enchanter");
        test.assertTrue(List.of(enchanter.parents).contains(LibResearch.KEY_SPELL_CLOTH), "below the Spellbinding Cloth");
        test.assertTrue(enchanter.displayColumn == 5 && enchanter.displayRow == 4, "at (5, 4)");

        String[][] entries = { { LibResearch.KEY_ENCHANT_ASCENT_BOOST, "6", "2", LibResearch.KEY_ENCHANTER },
                { LibResearch.KEY_ENCHANT_SLOW_FALL, "7", "3", LibResearch.KEY_ENCHANTER },
                { LibResearch.KEY_ENCHANT_AUTO_SMELT, "7", "5", LibResearch.KEY_ENCHANTER },
                { LibResearch.KEY_ENCHANT_DESINTEGRATE, "6", "6", LibResearch.KEY_ENCHANTER },
                { LibResearch.KEY_ENCHANT_QUICK_DRAW, "4", "6", LibResearch.KEY_ENCHANTER },
                { LibResearch.KEY_ENCHANT_VAMPIRISM, "3", "5", LibResearch.KEY_ENCHANTER },
                { LibResearch.KEY_ENCHANT_FOCUSED_STRIKE, "2", "7", LibResearch.KEY_ENCHANT_VAMPIRISM },
                { LibResearch.KEY_ENCHANT_DISPERSED_STRIKE, "1", "6", LibResearch.KEY_ENCHANT_VAMPIRISM },
                { LibResearch.KEY_ENCHANT_FINAL_STRIKE, "0", "8", LibResearch.KEY_ENCHANT_FOCUSED_STRIKE },
                { LibResearch.KEY_ENCHANT_POUNCE, "7", "0", LibResearch.KEY_ENCHANT_ASCENT_BOOST },
                { LibResearch.KEY_ENCHANT_SHATTER, "5", "8", LibResearch.KEY_ENCHANT_DESINTEGRATE },
                { LibResearch.KEY_ENCHANT_SHOCKWAVE, "9", "2", LibResearch.KEY_ENCHANT_SLOW_FALL },
                { LibResearch.KEY_ENCHANT_TUNNEL, "9", "6", LibResearch.KEY_ENCHANT_AUTO_SMELT },
                { LibResearch.KEY_ENCHANT_VALIANCE, "1", "4", LibResearch.KEY_ENCHANT_VAMPIRISM } };
        for (String[] entry : entries) {
            ResearchItem item = ResearchCategories.getResearch(entry[0]);
            test.assertTrue(item != null, entry[0] + " is registered");
            test.assertTrue(LibResearch.CATEGORY_THAUMICTINKERER.equals(item.category), entry[0] + " is in the add-on's tab");
            test.assertTrue(item.displayColumn == Integer.parseInt(entry[1]) && item.displayRow == Integer.parseInt(entry[2]),
                    entry[0] + " at (" + entry[1] + ", " + entry[2] + ")");
            test.assertTrue(List.of(item.parents).contains(entry[3]), entry[0] + " below " + entry[3]);
            test.assertTrue(item.isSecondary(), entry[0] + " is secondary");
        }
        ResearchItem finalStrike = ResearchCategories.getResearch(LibResearch.KEY_ENCHANT_FINAL_STRIKE);
        test.assertTrue(List.of(finalStrike.parents).contains(LibResearch.KEY_ENCHANT_DISPERSED_STRIKE), "Final Strike has two parents");
        test.succeed();
    }

    // ---- the data pack records -------------------------------------------------------------------------------------

    public static void enchantmentsAreLoadedFromTheDataPack(GameTestHelper test) {
        Registry<Enchantment> registry = registry(test);
        for (String name : LibEnchantNames.ALL) {
            ResourceKey<Enchantment> key = LibEnchantIDs.key(name);
            test.assertTrue(registry.containsKey(key), "the enchantment " + name + " is in the registry of the world");
            Holder<Enchantment> holder = registry.getHolderOrThrow(key);
            test.assertTrue(!holder.is(EnchantmentTags.IN_ENCHANTING_TABLE), name + " is not offered by the enchanting table");
            test.assertTrue(!holder.is(EnchantmentTags.NON_TREASURE), name + " is a treasure");
            test.assertTrue(!holder.is(EnchantmentTags.ON_RANDOM_LOOT), name + " is not in random loot");
            test.assertTrue(!holder.is(EnchantmentTags.TRADEABLE), name + " is not traded");
        }
        test.assertTrue(holder(test, ModEnchantments.ascentBoost).value().getMaxLevel() == 4, "Ascent Boost goes to 4");
        test.assertTrue(holder(test, ModEnchantments.quickDraw).value().getMaxLevel() == 2, "Quick Draw goes to 2");
        test.assertTrue(holder(test, ModEnchantments.autoSmelt).value().getMaxLevel() == 1, "Flaming Touch goes to 1");

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ItemStack legs = new ItemStack(Items.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        ItemStack bow = new ItemStack(Items.BOW);
        for (ResourceKey<Enchantment> weapon : List.of(ModEnchantments.vampirism, ModEnchantments.finalStrike, ModEnchantments.valiance,
                ModEnchantments.focusedStrike, ModEnchantments.dispersedStrikes)) {
            Enchantment enchantment = holder(test, weapon).value();
            test.assertTrue(enchantment.isSupportedItem(sword), weapon.location() + " goes on a sword");
            test.assertTrue(!enchantment.isSupportedItem(pick) && !enchantment.isSupportedItem(legs), weapon.location() + " not on tools or armor");
        }
        for (ResourceKey<Enchantment> digger : List.of(ModEnchantments.shatter, ModEnchantments.tunnel, ModEnchantments.desintegrate,
                ModEnchantments.autoSmelt)) {
            Enchantment enchantment = holder(test, digger).value();
            test.assertTrue(enchantment.isSupportedItem(pick) && enchantment.isSupportedItem(axe), digger.location() + " goes on a pickaxe and an axe");
            test.assertTrue(!enchantment.isSupportedItem(sword), digger.location() + " not on a sword");
        }
        test.assertTrue(holder(test, ModEnchantments.ascentBoost).value().isSupportedItem(legs), "Ascent Boost on leggings");
        test.assertTrue(!holder(test, ModEnchantments.ascentBoost).value().isSupportedItem(boots), "not on boots");
        test.assertTrue(holder(test, ModEnchantments.slowFall).value().isSupportedItem(boots), "Slow Fall on boots");
        test.assertTrue(holder(test, ModEnchantments.shockwave).value().isSupportedItem(boots), "Shockwave on boots");
        test.assertTrue(holder(test, ModEnchantments.quickDraw).value().isSupportedItem(bow), "Quick Draw on a bow");
        test.assertTrue(!holder(test, ModEnchantments.quickDraw).value().isSupportedItem(sword), "not on a sword");
        if (TTRegistry.hasItem(LibItemNames.ICHOR_BOOTS_GEM)) {
            ItemStack gem = TTRegistry.stack(LibItemNames.ICHOR_BOOTS_GEM);
            test.assertTrue(!holder(test, ModEnchantments.slowFall).value().isSupportedItem(gem), "Slow Fall is not for the gem boots");
            test.assertTrue(!holder(test, ModEnchantments.shockwave).value().isSupportedItem(gem), "Shockwave is not for the gem boots");
        }
        if (TTRegistry.hasItem(LibItemNames.BLOOD_SWORD)) {
            test.assertTrue(!holder(test, ModEnchantments.vampirism).value().isSupportedItem(TTRegistry.stack(LibItemNames.BLOOD_SWORD)),
                    "Vampirism is not for the Cursed Spirit's Blade");
        }
        test.succeed();
    }

    public static void exclusiveSetsMatchTheRulesOfTheOriginal(GameTestHelper test) {
        test.assertTrue(!Enchantment.areCompatible(holder(test, ModEnchantments.slowFall), holder(test, Vanilla.FEATHER_FALLING)),
                "Slow Fall and Feather Falling");
        test.assertTrue(!Enchantment.areCompatible(holder(test, ModEnchantments.shockwave), holder(test, ModEnchantments.slowFall)),
                "Shockwave and Slow Fall");
        test.assertTrue(!Enchantment.areCompatible(holder(test, ModEnchantments.vampirism), holder(test, Vanilla.KNOCKBACK)),
                "Vampirism and Knockback");
        test.assertTrue(!Enchantment.areCompatible(holder(test, ModEnchantments.shatter), holder(test, Vanilla.EFFICIENCY)),
                "Shatter and Efficiency");
        test.assertTrue(!Enchantment.areCompatible(holder(test, ModEnchantments.tunnel), holder(test, ModEnchantments.shatter)),
                "Tunnel and Shatter");
        test.assertTrue(!Enchantment.areCompatible(holder(test, ModEnchantments.dispersedStrikes), holder(test, Vanilla.SHARPNESS)),
                "Dispersed Strikes and Sharpness");
        test.assertTrue(Enchantment.areCompatible(holder(test, ModEnchantments.dispersedStrikes), holder(test, Vanilla.LOOTING)),
                "Dispersed Strikes and Looting");
        test.assertTrue(Enchantment.areCompatible(holder(test, ModEnchantments.finalStrike), holder(test, Vanilla.SHARPNESS)),
                "Final Strike and Sharpness");
        test.assertTrue(Enchantment.areCompatible(holder(test, ModEnchantments.autoSmelt), holder(test, Vanilla.UNBREAKING)),
                "Flaming Touch and Unbreaking");
        test.assertTrue(!Enchantment.areCompatible(holder(test, ModEnchantments.autoSmelt), holder(test, Vanilla.FORTUNE)),
                "Flaming Touch and Fortune");
        test.succeed();
    }

    /** What the table offers for a sword, before and after the research of Vampirism, and next to a chosen enchantment. */
    public static void theTableOffersWhatTheOriginalOffered(GameTestHelper test) {
        ServerPlayer player = player(test);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        List<ResourceKey<Enchantment>> offered = EnchantmentManager.offered(player, sword, List.of(), 16);
        test.assertTrue(offered.contains(Vanilla.SHARPNESS), "Sharpness: no research needed");
        test.assertTrue(offered.contains(Vanilla.LOOTING), "Looting");
        test.assertTrue(!offered.contains(ModEnchantments.vampirism), "Vampirism waits for its research");
        test.assertTrue(!offered.contains(Vanilla.EFFICIENCY), "Efficiency is for tools");

        test.assertTrue(ThaumcraftApiHelper.completeResearch(player, LibResearch.KEY_ENCHANT_VAMPIRISM), "the research is done");
        offered = EnchantmentManager.offered(player, sword, List.of(), 16);
        for (ResourceKey<Enchantment> key : List.of(ModEnchantments.vampirism, ModEnchantments.finalStrike, ModEnchantments.valiance,
                ModEnchantments.focusedStrike, ModEnchantments.dispersedStrikes)) {
            test.assertTrue(offered.contains(key), key.location() + " is offered (all of them need only the research of Vampirism)");
        }

        List<ResourceKey<Enchantment>> afterFinal = EnchantmentManager.offered(player, sword, List.of(ModEnchantments.finalStrike), 16);
        test.assertTrue(!afterFinal.contains(ModEnchantments.finalStrike), "a chosen enchantment is not offered again");
        test.assertTrue(!afterFinal.contains(Vanilla.KNOCKBACK), "Knockback is not on the list of Final Strike");
        test.assertTrue(afterFinal.contains(Vanilla.SMITE), "Smite is");

        test.assertTrue(EnchantmentManager.offered(player, enchanted(test, new ItemStack(Items.DIAMOND_SWORD),
                Vanilla.SHARPNESS, 1), List.of(), 16).isEmpty(), "an enchanted item gets no more");
        test.assertTrue(EnchantmentManager.offered(player, ItemStack.EMPTY, List.of(), 16).isEmpty(), "nothing for an empty slot");
        finish(test, player);
    }

    // ---- the Enchanter ---------------------------------------------------------------------------------------------

    public static void enchanterPersists(GameTestHelper test) {
        floor(test);
        TileEnchanter tile = enchanter(test);
        tile.setItem(TileEnchanter.SLOT_TOOL, new ItemStack(Items.IRON_PICKAXE));
        tile.setItem(TileEnchanter.SLOT_WAND, fullWand(test));
        tile.appendEnchant(Vanilla.EFFICIENCY);
        tile.appendLevel(3);
        tile.appendEnchant(ModEnchantments.shatter);
        tile.appendLevel(2);
        tile.updateAspectList();
        tile.currentAspects = new AspectList().add(Aspect.EARTH, 7).add(Aspect.ENTROPY, 2);
        tile.working = true;

        CompoundTag saved = tile.saveWithFullMetadata(test.getLevel().registryAccess());
        TileEnchanter loaded = new TileEnchanter(tile.getBlockPos(), tile.getBlockState());
        loaded.loadWithComponents(saved, test.getLevel().registryAccess());

        test.assertTrue(loaded.enchantments.equals(tile.enchantments), "the chosen enchantments: " + loaded.enchantments);
        test.assertTrue(loaded.levels.equals(List.of(3, 2)), "their levels: " + loaded.levels);
        test.assertTrue(loaded.working, "working");
        test.assertTrue(loaded.currentAspects.getAmount(Aspect.EARTH) == 7 && loaded.currentAspects.getAmount(Aspect.ENTROPY) == 2, "the vis paid so far");
        test.assertTrue(loaded.totalAspects.getAmount(Aspect.ENTROPY) == tile.totalAspects.getAmount(Aspect.ENTROPY)
                && loaded.totalAspects.getAmount(Aspect.ENTROPY) > 0, "the vis it will cost");
        test.assertTrue(loaded.getItem(TileEnchanter.SLOT_TOOL).is(Items.IRON_PICKAXE), "the tool");
        test.assertTrue(WandHelper.isWand(loaded.getItem(TileEnchanter.SLOT_WAND)), "the wand");
        test.assertTrue(loaded.getItem(TileEnchanter.SLOT_WAND).getComponents().equals(tile.getItem(TileEnchanter.SLOT_WAND).getComponents()),
                "with its vis");

        // the packet that the clients get carries the same
        CompoundTag update = tile.getUpdateTag(test.getLevel().registryAccess());
        TileEnchanter client = new TileEnchanter(tile.getBlockPos(), tile.getBlockState());
        client.loadWithComponents(update, test.getLevel().registryAccess());
        test.assertTrue(client.enchantments.equals(tile.enchantments) && client.working, "the update packet");
        test.succeed();
    }

    /** Opens the menu of the enchanter for the player, as a right click does. */
    private static void open(ServerPlayer player, TileEnchanter tile) {
        MenuRegistry.openExtendedMenu(player, tile);
    }

    public static void theMenuLocksTheToolWhileItWorks(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        TileEnchanter tile = enchanter(test);
        open(player, tile);
        test.assertTrue(player.containerMenu instanceof ContainerEnchanter, "the menu opened: " + player.containerMenu);
        ContainerEnchanter menu = (ContainerEnchanter) player.containerMenu;

        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        test.assertTrue(menu.getSlot(0).mayPlace(pickaxe), "a tool goes into the first slot");
        test.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.BOOK)), "not a book");
        test.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.STICK)), "not a stack that cannot wear out");
        test.assertTrue(menu.getSlot(1).mayPlace(fullWand(test)), "a wand goes into the second slot");
        test.assertTrue(!menu.getSlot(1).mayPlace(pickaxe), "a tool does not");
        test.assertTrue(tile.getContainerSize() == 2 && tile.getMaxStackSize() == 1, "two slots of one item");
        test.assertTrue(!tile.canPlaceItem(0, pickaxe), "a hopper cannot put anything in");
        test.assertTrue(tile.getSlotsForFace(net.minecraft.core.Direction.UP).length == 0, "and sees no slot");

        tile.setItem(0, pickaxe);
        tile.working = true;
        test.assertTrue(!menu.getSlot(0).mayPickup(player), "the tool stays while it is enchanted");
        test.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.IRON_AXE)), "and nothing replaces it");
        tile.working = false;
        test.assertTrue(menu.getSlot(0).mayPickup(player), "free again");

        // shift-clicking: from the player's inventory to the right slot
        tile.setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(0, fullWand(test));
        player.getInventory().setItem(1, new ItemStack(Items.IRON_SWORD));
        int wandSlot = 2 + 27; // the hot bar starts after the three rows
        menu.quickMoveStack(player, wandSlot);
        test.assertTrue(WandHelper.isWand(tile.getItem(1)), "a wand goes to the wand slot");
        menu.quickMoveStack(player, wandSlot + 1);
        test.assertTrue(tile.getItem(0).is(Items.IRON_SWORD), "a tool goes to the tool slot");
        finish(test, player);
    }

    public static void aMenuOfAnotherPlayerCannotChangeTheEnchanter(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        TileEnchanter tile = enchanter(test);
        tile.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        // no menu open: the packet is refused
        test.assertTrue(!new PacketEnchanterAddEnchant(tile.getBlockPos(), Vanilla.SHARPNESS.location(), 0).apply(player),
                "without the menu open");
        test.assertTrue(tile.enchantments.isEmpty(), "nothing chosen");
        test.assertTrue(!new PacketEnchanterStartWorking(tile.getBlockPos()).apply(player), "and nothing started");
        finish(test, player);
    }

    public static void choosingAndChangingEnchantmentsFollowsTheRules(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        TileEnchanter tile = enchanter(test);
        tile.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        open(player, tile);

        var sharpness = Vanilla.SHARPNESS;
        var smite = Vanilla.SMITE;
        var knockback = Vanilla.KNOCKBACK;
        test.assertTrue(new PacketEnchanterAddEnchant(tile, sharpness, 0).apply(player), "Sharpness is chosen");
        test.assertTrue(tile.enchantments.equals(List.of(sharpness)) && tile.levels.equals(List.of(1)), "at level 1");
        test.assertTrue(tile.totalAspects.getAmount(Aspect.ORDER) == 12, "for 12 Ordo: " + tile.totalAspects.getAmount(Aspect.ORDER));

        new PacketEnchanterAddEnchant(tile, smite, 0).apply(player);
        test.assertTrue(!tile.enchantments.contains(smite), "Smite is refused next to Sharpness");
        new PacketEnchanterAddEnchant(tile, knockback, 0).apply(player);
        test.assertTrue(tile.enchantments.contains(knockback), "Knockback is allowed");

        new PacketEnchanterAddEnchant(tile, sharpness, 3).apply(player);
        test.assertTrue(tile.levels.get(0) == 3, "the level is raised");
        test.assertTrue(tile.totalAspects.getAmount(Aspect.ORDER) == 48, "Ordo of level 3 = 10 * 4.8: " + tile.totalAspects.getAmount(Aspect.ORDER));
        new PacketEnchanterAddEnchant(tile, sharpness, 99).apply(player);
        test.assertTrue(tile.levels.get(0) == 5, "never above the highest level");
        new PacketEnchanterAddEnchant(tile, knockback, 7).apply(player);
        test.assertTrue(tile.levels.get(tile.enchantments.indexOf(knockback)) == 2, "Knockback stops at 2");
        new PacketEnchanterAddEnchant(tile, sharpness, 0).apply(player);
        test.assertTrue(tile.levels.get(0) == 1, "0 sets level 1");

        new PacketEnchanterAddEnchant(tile, knockback, -1).apply(player);
        test.assertTrue(!tile.enchantments.contains(knockback) && tile.enchantments.size() == tile.levels.size(), "removed");

        // an enchantment the table does not know, and one that needs research
        new PacketEnchanterAddEnchant(tile, Vanilla.MENDING, 0).apply(player);
        test.assertTrue(!tile.enchantments.contains(Vanilla.MENDING), "Mending is not in the table");
        new PacketEnchanterAddEnchant(tile, ModEnchantments.vampirism, 0).apply(player);
        test.assertTrue(!tile.enchantments.contains(ModEnchantments.vampirism), "Vampirism needs its research");
        ThaumcraftApiHelper.completeResearch(player, LibResearch.KEY_ENCHANT_VAMPIRISM);
        new PacketEnchanterAddEnchant(tile, ModEnchantments.vampirism, 0).apply(player);
        test.assertTrue(tile.enchantments.contains(ModEnchantments.vampirism), "Vampirism after the research");

        // while it works nothing changes
        tile.working = true;
        new PacketEnchanterAddEnchant(tile, sharpness, -1).apply(player);
        test.assertTrue(tile.enchantments.contains(sharpness), "no change while working");
        tile.working = false;

        // taking the tool out forgets the choice
        tile.setItem(0, ItemStack.EMPTY);
        tile.setChanged();
        test.assertTrue(tile.enchantments.isEmpty() && tile.levels.isEmpty(), "the choice is forgotten with the tool");
        finish(test, player);
    }

    public static void withoutSixPillarsTheEnchanterDoesNotStart(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        TileEnchanter tile = enchanter(test);
        pillars(test, new int[][] { { 1, 4 }, { 7, 4 }, { 4, 1 } }); // only three
        ItemStack wand = fullWand(test);
        int[] before = visOf(wand);
        tile.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        tile.setItem(1, wand);
        open(player, tile);
        new PacketEnchanterAddEnchant(tile, Vanilla.SHARPNESS, 0).apply(player);
        test.assertTrue(new PacketEnchanterStartWorking(tile).apply(player), "the button works");
        test.assertTrue(tile.working, "it starts to work");

        test.runAfterDelay(5, () -> {
            test.assertTrue(!tile.working, "and stops: there are only three pillars");
            test.assertTrue(java.util.Arrays.equals(before, visOf(wand)), "no vis was taken from the wand");
            test.assertTrue(!tile.getItem(0).isEnchanted(), "nothing was enchanted");
            finish(test, player);
        });
    }

    public static void theEnchanterEnchantsAToolFromAWandBetweenSixPillars(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        TileEnchanter tile = enchanter(test);
        pillars(test, SIX_COLUMNS);
        ItemStack wand = fullWand(test);
        int[] before = visOf(wand);
        tile.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        tile.setItem(1, wand);
        open(player, tile);
        // Sharpness I (12 Ordo) and Knockback I (12 Aer, 6 Perditio): every aspect within what the smallest wand holds
        new PacketEnchanterAddEnchant(tile, Vanilla.SHARPNESS, 0).apply(player);
        new PacketEnchanterAddEnchant(tile, Vanilla.KNOCKBACK, 0).apply(player);
        test.assertTrue(tile.enchantments.equals(List.of(Vanilla.SHARPNESS, Vanilla.KNOCKBACK)), "two enchantments chosen: " + tile.enchantments);
        int totalVis = 0;
        for (Aspect primal : TileEnchanter.PRIMAL_ASPECTS) totalVis += tile.totalAspects.getAmount(primal);
        test.assertTrue(totalVis == 30, "they cost 30 vis in all: " + totalVis);
        for (int amount : before) test.assertTrue(amount >= 1200, "the wand holds at least 12 vis of each primal: " + amount);
        test.assertTrue(new PacketEnchanterStartWorking(tile).apply(player), "started");

        test.succeedWhen(() -> {
            ItemStack tool = tile.getItem(0);
            test.assertTrue(tool.isEnchanted(), "the sword is enchanted (it takes about " + 30 + " ticks)");
            test.assertTrue(ModEnchantments.level(tool, Vanilla.SHARPNESS) == 1, "Sharpness I");
            test.assertTrue(ModEnchantments.level(tool, Vanilla.KNOCKBACK) == 1, "Knockback I");
            test.assertTrue(!tile.working && tile.enchantments.isEmpty() && tile.levels.isEmpty(), "the enchanter is idle and clear");
            int[] after = visOf(wand);
            int spent = 0;
            for (int i = 0; i < after.length; i++) spent += before[i] - after[i];
            test.assertTrue(spent > 0, "the wand paid: " + spent);
            test.assertTrue(spent <= 30 * 100, "and never more than a whole vis for each unit of the cost: " + spent);
            player.discard();
        });
    }

    // ---- the attacks -----------------------------------------------------------------------------------------------

    private static Pig toughPig(GameTestHelper test, Vec3 at) {
        Pig pig = test.spawn(EntityType.PIG, new BlockPos((int) at.x, (int) at.y, (int) at.z));
        pig.setNoAi(true);
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
        pig.setHealth(1000F);
        return pig;
    }

    public static void vampirismHealsTheAttacker(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.vampirism, 2));
        Pig pig = toughPig(test, new Vec3(3, 1, 6));
        player.setHealth(10F);
        test.runAfterDelay(3, () -> {
            player.attack(pig);
            test.assertTrue(pig.getHealth() < 1000F, "the blow hurt the pig");
            test.assertTrue(player.getHealth() == 12F, "the attacker heals by the level: " + player.getHealth());
            // the next blow inside the invulnerability frames of the pig deals nothing and heals nothing
            player.setHealth(10F);
            player.attack(pig);
            test.assertTrue(player.getHealth() == 10F, "no healing for a blow that does nothing: " + player.getHealth());
            finish(test, player);
        });
    }

    public static void valianceMultipliesTheWholeAttackAtLowHealth(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.valiance, 5));
        Pig pig = toughPig(test, new Vec3(3, 1, 6));
        test.runAfterDelay(3, () -> {
            // (A mock player is not ticked, so the attack damage of the sword in the hand may not be in the attribute yet:
            // the checks go by the value the attribute has, whatever it is.)
            double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            test.assertTrue(base > 0D, "the attack damage is positive: " + base);

            // healthy: the factor is 1 and nothing is armed
            player.setHealth(20F);
            AttackBoost.beforeAttack(player, pig);
            test.assertTrue(!player.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(AttackBoost.MODIFIER), "nothing armed at full health");

            // hurt: Valiance V gives 1.5
            player.setHealth(5F);
            float strength = player.getAttackStrengthScale(0.5F);
            AttackBoost.beforeAttack(player, pig);
            test.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(AttackBoost.MODIFIER), "armed");
            test.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - base * 1.5D) < 1.0E-3,
                    "Valiance V multiplies the attack by 1.5: " + base + " -> " + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            float before = pig.getHealth();
            player.attack(pig);
            float dealt = before - pig.getHealth();
            float expected = (float) (base * 1.5D * (0.2F + strength * strength * 0.8F));
            test.assertTrue(Math.abs(dealt - expected) < 0.01F, "the pig took " + dealt + ", expected " + expected);
            test.assertTrue(!player.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(AttackBoost.MODIFIER), "disarmed once the blow arrived");
            test.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - base) < 1.0E-6, "the attack is back to " + base);
            finish(test, player);
        });
    }

    public static void theAttackEventArmsTheAttackOfAPlayer(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.valiance, 5));
        Pig pig = toughPig(test, new Vec3(3, 1, 6));
        test.runAfterDelay(3, () -> {
            player.setHealth(5F);
            PlayerEvent.ATTACK_ENTITY.invoker().attack(player, player.level(), pig, InteractionHand.MAIN_HAND, null);
            test.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(AttackBoost.MODIFIER), "the event armed the attack");
            // a hurt event of the victim ends it
            EntityEvent.LIVING_HURT.invoker().hurt(pig, player.damageSources().playerAttack(player), 1F);
            test.assertTrue(!player.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(AttackBoost.MODIFIER), "the event of the hurt disarmed it");
            // and so does the next tick of the player, if the blow never arrives
            PlayerEvent.ATTACK_ENTITY.invoker().attack(player, player.level(), pig, InteractionHand.MAIN_HAND, null);
            TickEvent.PLAYER_PRE.invoker().tick(player);
            test.assertTrue(!player.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(AttackBoost.MODIFIER), "and so did the tick");
            finish(test, player);
        });
    }

    public static void theStrikeCounterIsKeptOnTheWeapon(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        ItemStack sword = enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.dispersedStrikes, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        Pig first = toughPig(test, new Vec3(3, 1, 6));
        Pig second = toughPig(test, new Vec3(5, 1, 5));
        test.runAfterDelay(3, () -> {
            ItemStack held = player.getMainHandItem();
            AttackBoost.beforeAttack(player, first);
            CompoundTag data = held.get(DataComponents.CUSTOM_DATA).copyTag();
            test.assertTrue(data.getInt(AttackBoost.NBT_SUCCESSIVE_STRIKE) == 0, "the first hit on a target: 0");
            test.assertTrue(data.getLong(AttackBoost.NBT_LAST_TARGET) == first.getUUID().getMostSignificantBits(), "the target is remembered");
            AttackBoost.beforeAttack(player, first);
            test.assertTrue(held.get(DataComponents.CUSTOM_DATA).copyTag().getInt(AttackBoost.NBT_SUCCESSIVE_STRIKE) == 5, "the second: 5");
            AttackBoost.beforeAttack(player, first);
            test.assertTrue(held.get(DataComponents.CUSTOM_DATA).copyTag().getInt(AttackBoost.NBT_SUCCESSIVE_STRIKE) == 6, "the third: 6");
            AttackBoost.beforeAttack(player, second);
            test.assertTrue(held.get(DataComponents.CUSTOM_DATA).copyTag().getInt(AttackBoost.NBT_SUCCESSIVE_STRIKE) == 0, "another target: 0");
            AttackBoost.afterAttack(player);
            finish(test, player);
        });
    }

    public static void pounceDependsOnTheBlockUnderTheFeet(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setItemSlot(EquipmentSlot.LEGS, enchanted(test, new ItemStack(Items.DIAMOND_LEGGINGS), ModEnchantments.pounce, 4));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        Pig pig = toughPig(test, new Vec3(3, 1, 6));
        test.runAfterDelay(3, () -> {
            double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            player.setPos(test.absoluteVec(new Vec3(5.5, 1.0, 5.5))); // standing on the floor
            AttackBoost.beforeAttack(player, pig);
            test.assertTrue(!player.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(AttackBoost.MODIFIER), "on the ground: nothing");
            player.setPos(test.absoluteVec(new Vec3(5.5, 3.0, 5.5))); // in the air
            AttackBoost.beforeAttack(player, pig);
            test.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - base * 2D) < 1.0E-3,
                    "in the air the attack is multiplied by 1 + .25 * 4 = 2: " + base + " -> " + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            AttackBoost.afterAttack(player);
            finish(test, player);
        });
    }

    // ---- digging -----------------------------------------------------------------------------------------------------

    /** The player stands on the floor above a block and looks straight down at it. */
    private static ServerPlayer minerLookingDown(GameTestHelper test, BlockPos target, ItemStack tool) {
        ServerPlayer player = player(test);
        Vec3 at = test.absoluteVec(new Vec3(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5));
        player.moveTo(at.x, at.y, at.z, 0F, 90F);
        player.setYHeadRot(0F); // the view vector follows the head, which moveTo leaves where it was
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    public static void desintegrateAndFlamingTouchChangeTheDigSpeedOfTheBlockUnderTheCrosshair(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 3);
        test.setBlock(target, Blocks.DIRT);
        ServerPlayer player = minerLookingDown(test, target, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.desintegrate, 1));
        test.runAfterDelay(3, () -> {
            BreakSpeedControl.update(player);
            test.assertTrue(player.getAttribute(Attributes.BLOCK_BREAK_SPEED).hasModifier(BreakSpeedControl.MODIFIER), "a modifier on the dig speed");
            test.assertTrue(player.getDestroySpeed(Blocks.DIRT.defaultBlockState()) >= 100F,
                    "dirt breaks at once: " + player.getDestroySpeed(Blocks.DIRT.defaultBlockState()));

            // cobblestone (hardness 2) is out of reach for Disintegrate
            test.setBlock(target, Blocks.COBBLESTONE);
            BreakSpeedControl.update(player);
            test.assertTrue(player.getDestroySpeed(Blocks.COBBLESTONE.defaultBlockState()) == 0F, "cobblestone cannot be dug: " + player.getDestroySpeed(Blocks.COBBLESTONE.defaultBlockState()));

            // Flaming Touch: wood only
            player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_AXE), ModEnchantments.autoSmelt, 1));
            test.setBlock(target, Blocks.OAK_PLANKS);
            BreakSpeedControl.update(player);
            test.assertTrue(player.getDestroySpeed(Blocks.OAK_PLANKS.defaultBlockState()) >= 100F, "planks break at once");
            test.setBlock(target, Blocks.DIRT);
            BreakSpeedControl.update(player);
            test.assertTrue(player.getDestroySpeed(Blocks.DIRT.defaultBlockState()) == 0F, "dirt does not");

            // another tool, another look: the modifier goes
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            BreakSpeedControl.update(player);
            test.assertTrue(!player.getAttribute(Attributes.BLOCK_BREAK_SPEED).hasModifier(BreakSpeedControl.MODIFIER), "no enchantment, no modifier");
            finish(test, player);
        });
    }

    public static void shatterAndTunnelScaleTheDigSpeed(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 3);
        test.setBlock(target, Blocks.OBSIDIAN);
        ServerPlayer player = minerLookingDown(test, target, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.shatter, 2));
        test.runAfterDelay(3, () -> {
            float plain = player.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState());
            BreakSpeedControl.update(player);
            float shattered = player.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState());
            test.assertTrue(Math.abs(shattered / plain - 6F) < 0.01F, "obsidian 6 times faster with Shatter II: " + plain + " -> " + shattered);

            test.setBlock(target, Blocks.STONE);
            BreakSpeedControl.update(player);
            float stone = player.getDestroySpeed(Blocks.STONE.defaultBlockState());
            player.getAttribute(Attributes.BLOCK_BREAK_SPEED).removeModifier(BreakSpeedControl.MODIFIER);
            float stonePlain = player.getDestroySpeed(Blocks.STONE.defaultBlockState());
            test.assertTrue(Math.abs(stone / stonePlain - 0.8F) < 0.01F, "stone at 0.8: " + stonePlain + " -> " + stone);

            // Tunnel: nothing remembered, then the same way, then a turn
            player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.tunnel, 3));
            BreakSpeedControl.update(player);
            test.assertTrue(!player.getAttribute(Attributes.BLOCK_BREAK_SPEED).hasModifier(BreakSpeedControl.MODIFIER), "no direction, no change");
            BlockEvent.BREAK.invoker().breakBlock(player.level(), test.absolutePos(target), Blocks.STONE.defaultBlockState(), player, null);
            float remembered = player.getMainHandItem().get(DataComponents.CUSTOM_DATA).copyTag().getFloat(BreakSpeedControl.NBT_TUNNEL_DIRECTION);
            test.assertTrue(remembered == player.getYRot(), "the direction is kept on the tool: " + remembered);
            float tunnelPlain = player.getDestroySpeed(Blocks.STONE.defaultBlockState());
            BreakSpeedControl.update(player);
            test.assertTrue(Math.abs(player.getDestroySpeed(Blocks.STONE.defaultBlockState()) / tunnelPlain - 1.6F) < 0.01F, "the same way: 1 + .2 * 3");
            player.setYRot(player.getYRot() + 90F);
            BreakSpeedControl.update(player);
            test.assertTrue(Math.abs(player.getDestroySpeed(Blocks.STONE.defaultBlockState()) / tunnelPlain - 0.3F) < 0.01F, "after a turn: 0.3");
            finish(test, player);
        });
    }

    public static void aToolOfDisintegrateRefusesTheOtherBlocksAndWearsDoubleOnItsOwn(GameTestHelper test) {
        floor(test);
        BlockPos dirt = new BlockPos(3, 1, 3);
        BlockPos stone = new BlockPos(5, 1, 3);
        test.setBlock(dirt, Blocks.DIRT);
        test.setBlock(stone, Blocks.COBBLESTONE);
        ServerPlayer player = minerLookingDown(test, dirt, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.desintegrate, 1));
        test.runAfterDelay(3, () -> {
            test.assertTrue(player.gameMode.destroyBlock(test.absolutePos(stone)) == false, "cobblestone is refused");
            test.assertTrue(test.getLevel().getBlockState(test.absolutePos(stone)).is(Blocks.COBBLESTONE), "and stays");
            int worn = player.getMainHandItem().getDamageValue();
            test.assertTrue(player.gameMode.destroyBlock(test.absolutePos(dirt)), "dirt breaks");
            test.assertTrue(test.getLevel().getBlockState(test.absolutePos(dirt)).isAir(), "and is gone");
            test.assertTrue(player.getMainHandItem().getDamageValue() - worn >= 1, "the tool wore: " + (player.getMainHandItem().getDamageValue() - worn));
            finish(test, player);
        });
    }

    // ---- movement --------------------------------------------------------------------------------------------------

    public static void slowFallShortensTheFallOnTheServer(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setItemSlot(EquipmentSlot.FEET, enchanted(test, new ItemStack(Items.DIAMOND_BOOTS), ModEnchantments.slowFall, 3));
        test.runAfterDelay(3, () -> {
            player.setPos(test.absoluteVec(new Vec3(5.5, 6.0, 5.5)));
            player.setOnGround(false);
            player.fallDistance = 10F;
            TickEvent.PLAYER_PRE.invoker().tick(player);
            test.assertTrue(Math.abs(player.fallDistance - 9F) < 1.0E-4F, "10 - 3 / 3 = 9: " + player.fallDistance);

            player.fallDistance = 3F;
            TickEvent.PLAYER_PRE.invoker().tick(player);
            test.assertTrue(Math.abs(player.fallDistance - 2.9F) < 1.0E-4F, "never below 2.9: " + player.fallDistance);

            player.fallDistance = 2.5F;
            TickEvent.PLAYER_PRE.invoker().tick(player);
            test.assertTrue(player.fallDistance == 2.5F, "too short a fall is left alone");

            player.fallDistance = 10F;
            player.setShiftKeyDown(true);
            TickEvent.PLAYER_PRE.invoker().tick(player);
            test.assertTrue(player.fallDistance == 10F, "sneaking turns it off");
            player.setShiftKeyDown(false);
            player.setOnGround(true);
            TickEvent.PLAYER_PRE.invoker().tick(player);
            test.assertTrue(player.fallDistance == 10F, "on the ground there is nothing to slow");
            finish(test, player);
        });
    }

    public static void shockwaveHurtsTheCreaturesAroundALanding(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setItemSlot(EquipmentSlot.FEET, enchanted(test, new ItemStack(Items.DIAMOND_BOOTS), ModEnchantments.shockwave, 2));
        Pig near = toughPig(test, new Vec3(3, 1, 3));
        Pig far = toughPig(test, new Vec3(0, 1, 0));
        test.runAfterDelay(3, () -> {
            player.setPos(test.absoluteVec(new Vec3(5.5, 6.0, 5.5)));
            player.setOnGround(false);
            player.fallDistance = 10F;
            TickEvent.PLAYER_POST.invoker().tick(player); // in the air: the fall is noted
            test.assertTrue(near.getHealth() == 1000F, "nothing yet");

            player.setPos(test.absoluteVec(new Vec3(5.5, 1.0, 5.5)));
            player.setOnGround(true);
            player.fallDistance = 0F; // the game resets it on landing
            TickEvent.PLAYER_POST.invoker().tick(player);
            float expected = EnchantmentFormulas.shockwaveDamage(2, 10F);
            test.assertTrue(Math.abs((1000F - near.getHealth()) - expected) < 0.01F, "a pig nearby took " + (1000F - near.getHealth()) + ", expected " + expected);
            test.assertTrue(Math.abs((1000F - far.getHealth()) - expected) < 0.01F, "so did one ten blocks away (inside the box)");
            test.assertTrue(player.getHealth() == player.getMaxHealth(), "the faller is not hurt by its own wave");

            // a short fall does nothing
            near.setHealth(1000F);
            near.invulnerableTime = 0;
            player.setOnGround(false);
            player.fallDistance = 3F;
            TickEvent.PLAYER_POST.invoker().tick(player);
            player.setOnGround(true);
            player.fallDistance = 0F;
            TickEvent.PLAYER_POST.invoker().tick(player);
            test.assertTrue(near.getHealth() == 1000F, "a fall of three blocks does not shake the ground");
            finish(test, player);
        });
    }

    public static void quickDrawGivesTheArrowTheSpeedOfALongerDraw(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        ItemStack bow = enchanted(test, new ItemStack(Items.BOW), ModEnchantments.quickDraw, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        test.runAfterDelay(2, () -> {
            player.startUsingItem(InteractionHand.MAIN_HAND);
            // nothing ticks a mock player: do what the connection of a real one does, twelve times
            for (int i = 0; i < 12; i++) player.doTick();
            test.assertTrue(player.isUsingItem(), "the bow is drawn");
            int ticks = player.getTicksUsingItem();
            test.assertTrue(ticks >= 10, "the draw clock ran: " + ticks);
            Arrow arrow = new Arrow(player.level(), player, new ItemStack(Items.ARROW), bow);
            float power = BowItem.getPowerForTime(ticks);
            arrow.setDeltaMovement(0, 0, power * 3F);
            int bonus = EnchantmentFormulas.quickDrawBonusTicks(2, ticks);
            test.assertTrue(bonus > 0, "the draw was long enough for a bonus: " + ticks + " ticks");
            EntityEvent.ADD.invoker().add(arrow, player.level());
            float expected = BowItem.getPowerForTime(ticks + bonus) * 3F;
            test.assertTrue(Math.abs(arrow.getDeltaMovement().length() - expected) < 0.01D,
                    "the arrow flies as after " + (ticks + bonus) + " ticks: " + arrow.getDeltaMovement().length() + " (expected " + expected + ")");
            test.assertTrue(arrow.getDeltaMovement().length() > power * 3F, "faster than the real draw gives");

            // an arrow of a player without the enchantment is left alone
            player.stopUsingItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
            player.startUsingItem(InteractionHand.MAIN_HAND);
            for (int i = 0; i < 12; i++) player.doTick();
            Arrow plain = new Arrow(player.level(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            plain.setDeltaMovement(0, 0, 1.5);
            EntityEvent.ADD.invoker().add(plain, player.level());
            test.assertTrue(Math.abs(plain.getDeltaMovement().length() - 1.5D) < 1.0E-6, "an ordinary bow is not touched");
            finish(test, player);
        });
    }

    public static void everyHandlerIsRegisteredOnce(GameTestHelper test) {
        // the group's registration ran once, so the add-on's listeners are not doubled: a hurt event heals exactly once
        floor(test);
        ServerPlayer player = player(test);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.vampirism, 1));
        Pig pig = toughPig(test, new Vec3(3, 1, 6));
        player.setHealth(10F);
        test.runAfterDelay(3, () -> {
            EntityEvent.LIVING_HURT.invoker().hurt(pig, player.damageSources().playerAttack(player), 3F);
            test.assertTrue(player.getHealth() == 11F, "healed once, by the level: " + player.getHealth());
            finish(test, player);
        });
    }

    /** The registry of enchantments of this world holds what the add-on and Thaumcraft define, nothing missing. */
    public static void theTableKnowsOnlyEnchantmentsOfTheWorld(GameTestHelper test) {
        Registry<Enchantment> registry = registry(test);
        for (ResourceKey<Enchantment> key : EnchantmentManager.enchantments()) {
            test.assertTrue(registry.containsKey(key), "the table lists " + key.location() + ", the world must define it");
        }
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        test.succeed();
    }
}
