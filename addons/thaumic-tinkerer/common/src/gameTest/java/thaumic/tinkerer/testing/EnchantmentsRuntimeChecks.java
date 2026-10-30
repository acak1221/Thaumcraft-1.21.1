package thaumic.tinkerer.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
import thaumic.tinkerer.common.core.fakeplayer.TinkererFakePlayer;
import thaumic.tinkerer.common.core.helper.ItemNBTHelper;
import thaumic.tinkerer.common.enchantment.EnchantmentFormulas;
import thaumic.tinkerer.common.enchantment.ModEnchantmentHandler;
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
 * refusal without pillars, the locked slots), and the effect of each family of enchantments through the loader events of
 * {@code TinkererEvents} (the damage of a blow, an arrow or thorns and the strike counter, the dig speed and the refused blocks,
 * Vampirism, Ascent Boost, Slow Fall, Shockwave, Quick Draw, Tunnel), with real entities, and the negative case of each: an item,
 * an entity or a player that is not the enchantment's is left alone. The fake player of the Animation Tablet is the cross-check.</p>
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
        static final ResourceKey<Enchantment> BANE_OF_ARTHROPODS = net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS;
        static final ResourceKey<Enchantment> SILK_TOUCH = net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH;
        static final ResourceKey<Enchantment> THORNS = net.minecraft.world.item.enchantment.Enchantments.THORNS;
        static final ResourceKey<Enchantment> FIRE_ASPECT = net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT;
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
    @SuppressWarnings("removal") // the only mock that is a ServerPlayer (game mode, attribute and network paths need one)
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

    // ---- the attacks (LIVING_DAMAGE) --------------------------------------------------------------------------------

    private static Pig toughPig(GameTestHelper test, Vec3 at) {
        Pig pig = test.spawn(EntityType.PIG, new BlockPos((int) at.x, (int) at.y, (int) at.z));
        pig.setNoAi(true);
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
        pig.setHealth(1000F);
        return pig;
    }

    /** A zombie with an item in its main hand, floating (no AI) with a helmet on, so that the sun does not burn it. */
    private static Zombie armedZombie(GameTestHelper test, Vec3 at, ItemStack mainHand) {
        Zombie zombie = test.spawn(EntityType.ZOMBIE, new BlockPos((int) at.x, (int) at.y, (int) at.z));
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, mainHand);
        return zombie;
    }

    private static void near(GameTestHelper test, float actual, float expected, String what) {
        test.assertTrue(Math.abs(actual - expected) < 1.0E-3F, what + ": " + actual + " (expected " + expected + ")");
    }

    /**
     * The health the victim loses to one hit of this size from this source, its invulnerability frames cleared first. The
     * pigs have no armour and nothing else reduces a hit, so this is the amount that the listeners of LIVING_DAMAGE left.
     */
    private static float lost(LivingEntity victim, DamageSource source, float amount) {
        victim.invulnerableTime = 0;
        float before = victim.getHealth();
        victim.hurt(source, amount);
        return before - victim.getHealth();
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.get(DataComponents.CUSTOM_DATA).copyTag();
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
            // the next blow inside the invulnerability frames of the pig is refused by the game before the event: no healing
            player.setHealth(10F);
            player.attack(pig);
            test.assertTrue(player.getHealth() == 10F, "no healing for a blow that does nothing: " + player.getHealth());

            // any living attacker, as in 1.7.10: a zombie with the sword heals itself
            Zombie zombie = armedZombie(test, new Vec3(5, 1, 5), enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.vampirism, 1));
            zombie.setHealth(10F);
            lost(pig, zombie.damageSources().mobAttack(zombie), 3F);
            test.assertTrue(zombie.getHealth() == 11F, "a zombie heals by the level too: " + zombie.getHealth());

            // the item has to be in the hand: in the inventory, or in the off hand, it does nothing
            ItemStack vampire = enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.vampirism, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            player.getInventory().setItem(5, vampire);
            player.setItemInHand(InteractionHand.OFF_HAND, vampire.copy());
            player.setHealth(10F);
            lost(pig, player.damageSources().playerAttack(player), 3F);
            test.assertTrue(player.getHealth() == 10F, "an enchanted sword that is not in the main hand heals nothing: " + player.getHealth());

            // the attacker is the one who heals, not the victim that holds the sword
            Pig holder = toughPig(test, new Vec3(1, 1, 1));
            holder.setItemSlot(EquipmentSlot.MAINHAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.vampirism, 2));
            holder.setHealth(10F);
            lost(holder, player.damageSources().playerAttack(player), 3F);
            test.assertTrue(holder.getHealth() == 7F && player.getHealth() == 10F, "the holder of the sword is only hit: " + holder.getHealth());
            zombie.discard();
            holder.discard();
            finish(test, player);
        });
    }

    public static void valianceMultipliesTheWholeAttackAtLowHealth(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.valiance, 5));
        Pig pig = toughPig(test, new Vec3(3, 1, 6));
        test.runAfterDelay(3, () -> {
            DamageSource blow = player.damageSources().playerAttack(player);
            // Valiance V is 1 + .5 below half of the maximum health, and only below it
            player.setHealth(20F);
            near(test, lost(pig, blow, 10F), 10F, "healthy: the blow is as it was");
            player.setHealth(10F);
            near(test, lost(pig, blow, 10F), 10F, "exactly half is not below half");
            player.setHealth(9F);
            near(test, lost(pig, blow, 10F), 15F, "hurt: the whole blow is multiplied by 1.5");
            near(test, lost(pig, blow, 4F), 6F, "whatever the size of the blow");

            // the whole melee path of Player.attack: (base * (0.2 + 0.8 * strength^2)) * 1.5
            double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            test.assertTrue(base > 0D, "the attack damage is positive: " + base);
            pig.invulnerableTime = 0;
            float strength = player.getAttackStrengthScale(0.5F);
            float before = pig.getHealth();
            player.attack(pig);
            float expected = (float) (base * (0.2F + strength * strength * 0.8F)) * 1.5F;
            test.assertTrue(Math.abs((before - pig.getHealth()) - expected) < 0.01F,
                    "a melee blow of the hurt player: the pig took " + (before - pig.getHealth()) + ", expected " + expected);

            // a sword without the enchantment, or Valiance in the off hand, is not multiplied
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            near(test, lost(pig, blow, 10F), 10F, "a plain sword at low health");
            player.setItemInHand(InteractionHand.OFF_HAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.valiance, 5));
            near(test, lost(pig, blow, 10F), 10F, "Valiance in the off hand");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            near(test, lost(pig, blow, 10F), 10F, "an empty hand");
            finish(test, player);
        });
    }

    /**
     * The 1.7.10 handler answered for any living attacker (the entity that caused the damage) and its main hand: a mob that
     * hits, the shooter of an arrow, the wearer of thorns. And only for the attacker: a victim that holds the sword is not
     * boosted, a hit without an attacker is left alone.
     */
    public static void theEnchantmentsAnswerForAnyLivingAttackerAndItsMainHandOnly(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        Pig pig = toughPig(test, new Vec3(3, 1, 6));
        Zombie zombie = armedZombie(test, new Vec3(5, 1, 5), enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.valiance, 5));
        zombie.setHealth(5F); // a quarter of 20
        Arrow arrow = new Arrow(level, zombie, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        try {
            near(test, lost(pig, level.damageSources().mobAttack(zombie), 10F), 15F, "a mob's blow is multiplied by its sword");
            near(test, lost(pig, level.damageSources().arrow(arrow, zombie), 10F), 15F, "an arrow takes the sword of its shooter");
            near(test, lost(pig, level.damageSources().thorns(zombie), 10F), 15F, "thorns take the sword of the one who wears them");

            zombie.setHealth(20F);
            near(test, lost(pig, level.damageSources().mobAttack(zombie), 10F), 10F, "a healthy zombie is not boosted");
            zombie.setHealth(5F);

            near(test, lost(pig, level.damageSources().fall(), 10F), 10F, "damage with no attacker is left alone");
            Zombie other = armedZombie(test, new Vec3(1, 1, 1), new ItemStack(Items.DIAMOND_SWORD));
            other.setHealth(5F);
            near(test, lost(pig, level.damageSources().mobAttack(other), 10F), 10F, "a zombie with a plain sword is left alone");
            zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            zombie.setItemSlot(EquipmentSlot.OFFHAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.valiance, 5));
            near(test, lost(pig, level.damageSources().mobAttack(zombie), 10F), 10F, "the off hand does not count");
            zombie.setItemSlot(EquipmentSlot.MAINHAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.valiance, 5));

            // the victim that holds the sword (at low health) is only hit
            Pig holder = toughPig(test, new Vec3(1, 1, 5));
            holder.setItemSlot(EquipmentSlot.MAINHAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.valiance, 5));
            holder.setHealth(100F);
            near(test, lost(holder, level.damageSources().mobAttack(other), 3F), 3F, "the holder of the sword as the victim");
            other.discard();
            holder.discard();
        } finally {
            zombie.discard();
            arrow.discard();
        }
        test.succeed();
    }

    /**
     * Focused Strikes (deterministic): 10 / 2 = 5, then 5 + .5 * max(counter, 5) * 5 * level. The counter is kept on the weapon:
     * 0 for a new target, then 5 on the second blow and one more for each blow after it. A blow that the frames of the victim
     * refuse never reaches the event, so it does not move the counter (1.7.10: the event came after the frames as well).
     */
    public static void theStrikeCounterIsKeptOnTheWeapon(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        ItemStack sword = enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.focusedStrike, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        Pig first = toughPig(test, new Vec3(3, 1, 6));
        Pig second = toughPig(test, new Vec3(5, 1, 5));
        test.runAfterDelay(3, () -> {
            ItemStack held = player.getMainHandItem();
            DamageSource blow = player.damageSources().playerAttack(player);
            near(test, lost(first, blow, 10F), 17.5F, "the first blow on a target: 5 + .5 * 5 * 5");
            test.assertTrue(customData(held).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 0, "the counter: 0");
            test.assertTrue(customData(held).getLong(ModEnchantmentHandler.NBT_LAST_TARGET) == first.getUUID().getMostSignificantBits()
                    && customData(held).getLong(ModEnchantmentHandler.NBT_LAST_TARGET_2) == first.getUUID().getLeastSignificantBits(),
                    "the target is remembered");
            near(test, lost(first, blow, 10F), 17.5F, "the second blow on it");
            test.assertTrue(customData(held).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 5, "the counter jumps to 5 (max where min was meant)");
            near(test, lost(first, blow, 10F), 20F, "the third: 5 + .5 * 6 * 5");
            test.assertTrue(customData(held).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 6, "and goes on: 6");

            // a blow in the frames of the victim that is not stronger than the last is refused by the game: the event is not posted
            float before = first.getHealth();
            test.assertTrue(!first.hurt(blow, 10F) && first.getHealth() == before, "refused by the frames");
            test.assertTrue(customData(held).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 6, "and the counter did not move");

            near(test, lost(second, blow, 10F), 17.5F, "another target starts again");
            test.assertTrue(customData(held).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 0
                    && customData(held).getLong(ModEnchantmentHandler.NBT_LAST_TARGET) == second.getUUID().getMostSignificantBits(), "counter 0, new target");

            // Dispersed Strikes on its own sword: 10 * (1 + .6 + s / 5) / (1 + .25 + 2.5 s)
            ItemStack dispersed = enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.dispersedStrikes, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, dispersed);
            near(test, lost(first, blow, 10F), 12.8F, "Dispersed Strikes: the first blow");
            near(test, lost(first, blow, 10F), 10F * (1F + .6F + 1F) / (1F + .25F + 12.5F), "and the second (counter 5)");
            test.assertTrue(customData(player.getMainHandItem()).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 5, "counter 5");
            // an item without either enchantment gets no counter
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            near(test, lost(first, blow, 10F), 10F, "a plain sword");
            test.assertTrue(player.getMainHandItem().get(DataComponents.CUSTOM_DATA) == null, "and no data on it");
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
            DamageSource blow = player.damageSources().playerAttack(player);
            player.setPos(test.absoluteVec(new Vec3(5.5, 1.0, 5.5))); // standing on the floor
            near(test, lost(pig, blow, 10F), 10F, "on the ground: nothing");
            player.setPos(test.absoluteVec(new Vec3(5.5, 3.0, 5.5))); // in the air
            near(test, lost(pig, blow, 10F), 20F, "in the air a blow is multiplied by 1 + .25 * 4 = 2");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            near(test, lost(pig, blow, 10F), 10F, "with nothing in the hand, as in the original");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));

            // only the leggings count, and only a player's
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.CHEST, enchanted(test, new ItemStack(Items.DIAMOND_CHESTPLATE), ModEnchantments.pounce, 4));
            near(test, lost(pig, blow, 10F), 10F, "Pounce on the chest is nothing");

            Zombie zombie = armedZombie(test, new Vec3(5, 3, 5), new ItemStack(Items.DIAMOND_SWORD));
            zombie.setItemSlot(EquipmentSlot.LEGS, enchanted(test, new ItemStack(Items.DIAMOND_LEGGINGS), ModEnchantments.pounce, 4));
            near(test, lost(pig, zombie.damageSources().mobAttack(zombie), 10F), 10F, "a mob with the leggings is not a player");
            zombie.discard();
            finish(test, player);
        });
    }

    /**
     * Final Strike triples the blow when {@code level > nextInt(19)}: 5 of 19 at level V, 1 of 19 at level I. The roll is random,
     * so the check counts 400 blows: each one is the plain blow or three times it, nothing between, level V triples some and
     * not all, about a quarter, and more of them than level I does; a plain sword never.
     */
    public static void finalStrikeTriplesTheBlowOnTheRollOfTheOriginal(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        Pig pig = toughPig(test, new Vec3(3, 1, 6));
        DamageSource blow = player.damageSources().playerAttack(player);
        int[] tripled = new int[3];
        ItemStack[] swords = { enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.finalStrike, 5),
                enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.finalStrike, 1), new ItemStack(Items.DIAMOND_SWORD) };
        for (int which = 0; which < swords.length; which++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, swords[which]);
            for (int i = 0; i < 400; i++) {
                pig.setHealth(1000F);
                float lost = lost(pig, blow, 10F);
                if (Math.abs(lost - 30F) < 1.0E-3F) tripled[which]++;
                else test.assertTrue(Math.abs(lost - 10F) < 1.0E-3F, "a blow of 10 is 10 or 30, not " + lost);
            }
        }
        // expected 105 of 400 at level V (5 / 19) and 21 at level I (1 / 19)
        test.assertTrue(tripled[0] > 50 && tripled[0] < 170, "Final Strike V triples about 5 blows in 19: " + tripled[0] + " of 400");
        test.assertTrue(tripled[1] >= 1 && tripled[1] < 60, "Final Strike I about 1 in 19: " + tripled[1] + " of 400");
        test.assertTrue(tripled[0] > tripled[1], "level V triples more often than level I");
        test.assertTrue(tripled[2] == 0, "a plain sword never: " + tripled[2]);
        finish(test, player);
    }

    /** Ticks of the player until the sword is at full strength again (a sweep needs more than .9 of it); set the health after. */
    private static void recharge(ServerPlayer player) {
        for (int i = 0; i < 14; i++) player.doTick();
        player.setOnGround(true);
    }

    /**
     * The sweep of a sword (Minecraft 1.9 and later; 1.7.10 had none) hurts every entity it reaches with the same source as the
     * blow: the event is posted once for each, and the enchantments answer for each of them. A swing at a pig with another
     * beside it heals Vampirism twice, a swing at a lone pig once; the strike counter ends on the last pig of the swing; a pig
     * outside the reach of the sweep is left alone. This pins the behaviour that docs/deviations/Enchantments.md section 2 states.
     */
    public static void aSweepOfASwordIsAHitForEachEntityItReaches(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setPos(test.absoluteVec(new Vec3(3.5, 1.0, 5.5)));
        ItemStack sword = enchanted(test, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.vampirism, 2),
                ModEnchantments.focusedStrike, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        Pig primary = toughPig(test, new Vec3(3, 1, 3));
        Pig swept = toughPig(test, new Vec3(4, 1, 3));
        Pig outside = toughPig(test, new Vec3(0, 1, 3));
        Pig lone = toughPig(test, new Vec3(7, 1, 0));

        // the control: a swing at a pig with nobody beside it
        recharge(player);
        test.assertTrue(player.getAttackStrengthScale(0.5F) > 0.9F && player.onGround(), "the sword is at full strength, on the ground: a sweep is possible");
        player.setHealth(10F);
        player.attack(lone);
        test.assertTrue(lone.getHealth() < 1000F, "the blow hurt the lone pig");
        test.assertTrue(player.getHealth() == 12F, "one pig, one heal of 2: " + player.getHealth());

        // the swing at a pig with another beside it, a few game ticks later (a different game time than the first swing)
        test.runAfterDelay(3, () -> {
            recharge(player);
            test.assertTrue(player.getAttackStrengthScale(0.5F) > 0.9F && player.onGround(), "full strength again");
            player.setHealth(10F);
            player.attack(primary);
            test.assertTrue(primary.getHealth() < 1000F, "the blow hurt the pig it was aimed at");
            test.assertTrue(swept.getHealth() < 1000F, "the sweep hurt the pig beside it (without that this check proves nothing): " + swept.getHealth());
            test.assertTrue(outside.getHealth() == 1000F, "the pig out of the sweep is left alone: " + outside.getHealth());
            test.assertTrue(player.getHealth() == 14F, "Vampirism heals by its level for each pig the swing hurt: " + player.getHealth());
            CompoundTag data = customData(player.getMainHandItem());
            test.assertTrue(data.getLong(ModEnchantmentHandler.NBT_LAST_TARGET) == swept.getUUID().getMostSignificantBits()
                    && data.getLong(ModEnchantmentHandler.NBT_LAST_TARGET_2) == swept.getUUID().getLeastSignificantBits(),
                    "the strike counter's target is the last pig of the swing, the swept one, not the one aimed at");
            test.assertTrue(data.getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 0,
                    "and its streak starts again: " + data.getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE));
            for (Pig pig : List.of(primary, swept, outside, lone)) pig.discard();
            finish(test, player);
        });
    }

    /**
     * The invulnerability frames compare the boosted amounts, not the raw ones (docs/deviations/Enchantments.md section 2). With a
     * factor above one (Pounce IV, 2): after a blow of 10, dealt as 20, a blow of 15 or 20 inside the frames is refused where
     * 1.7.10 dealt (15 - 10) * 2 = 10, and a blow of 25 deals what 1.7.10 dealt, (25 - 10) * 2 = 30. With a factor below one
     * (Dispersed Strikes I at a counter of 6, 2.6 / 16.25): after a blow of 10, dealt as 1.6, the same raw blow inside the
     * frames is posted (1.7.10 refused it before its event), moves the counter to 7 and then is refused for dealing less.
     */
    public static void theFramesCompareTheBoostedAmountsNotTheRawOnes(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setPos(test.absoluteVec(new Vec3(5.5, 3.0, 5.5))); // in the air
        player.setItemSlot(EquipmentSlot.LEGS, enchanted(test, new ItemStack(Items.DIAMOND_LEGGINGS), ModEnchantments.pounce, 4));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        Pig pig = toughPig(test, new Vec3(3, 1, 3));
        DamageSource blow = player.damageSources().playerAttack(player);

        near(test, lost(pig, blow, 10F), 20F, "the first blow of 10 is dealt as 20");
        float afterFirst = pig.getHealth();
        test.assertTrue(!pig.hurt(blow, 15F) && pig.getHealth() == afterFirst, "15 inside the frames is refused (1.7.10 dealt 10)");
        test.assertTrue(!pig.hurt(blow, 20F) && pig.getHealth() == afterFirst, "20 as well");
        pig.hurt(blow, 25F);
        near(test, afterFirst - pig.getHealth(), 30F, "25 deals 25 * 2 less the 20 of the first, as in 1.7.10: (25 - 10) * 2");

        // a factor below one
        player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        ItemStack dispersed = enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.dispersedStrikes, 1);
        ItemNBTHelper.setLong(dispersed, ModEnchantmentHandler.NBT_LAST_TARGET, pig.getUUID().getMostSignificantBits());
        ItemNBTHelper.setLong(dispersed, ModEnchantmentHandler.NBT_LAST_TARGET_2, pig.getUUID().getLeastSignificantBits());
        ItemNBTHelper.setInt(dispersed, ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE, 5);
        player.setItemInHand(InteractionHand.MAIN_HAND, dispersed);
        pig.setHealth(1000F);
        near(test, lost(pig, blow, 10F), 10F * (1F + .6F + 1F) / (1F + .25F + 15F), "the first blow, the counter 6: 10 * 2.6 / 16.25");
        float afterDispersed = pig.getHealth();
        test.assertTrue(customData(player.getMainHandItem()).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 6, "the counter is 6");
        test.assertTrue(!pig.hurt(blow, 10F) && pig.getHealth() == afterDispersed, "the same raw blow inside the frames deals nothing");
        test.assertTrue(customData(player.getMainHandItem()).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 7,
                "but it was posted (1.7.10 refused it before its event): the counter moved to "
                        + customData(player.getMainHandItem()).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE));
        pig.discard();
        finish(test, player);
    }

    // ---- digging (BREAK_SPEED) ---------------------------------------------------------------------------------------

    /** The player stands on the floor above a block (in the middle of the structure, on survival rules). */
    private static ServerPlayer miner(GameTestHelper test, ItemStack tool) {
        ServerPlayer player = player(test);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    private static float progress(GameTestHelper test, Player digger, BlockPos target) {
        BlockPos absolute = test.absolutePos(target);
        return test.getLevel().getBlockState(absolute).getDestroyProgress(digger, test.getLevel(), absolute);
    }

    public static void desintegrateAndFlamingTouchChangeTheDigSpeedOfTheBlock(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 3);
        ServerPlayer player = miner(test, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.desintegrate, 1));
        ServerPlayer bystander = miner(test, new ItemStack(Items.DIAMOND_PICKAXE));
        test.setBlock(target, Blocks.DIRT);
        // no crosshair, no tick of the player: the speed is asked from the block and the player, as 1.7.10's event was
        test.assertTrue(progress(test, player, target) >= 1F, "dirt breaks at once: " + progress(test, player, target));
        float plain = progress(test, bystander, target);
        test.assertTrue(plain > 0F && plain < 1F, "a plain diamond pickaxe does not: " + plain);

        // stone (hardness 1.5) is still Disintegrate's, cobblestone (2.0) is out of reach
        test.setBlock(target, Blocks.STONE);
        test.assertTrue(progress(test, player, target) >= 1F, "stone has a hardness of 1.5 and goes");
        test.setBlock(target, Blocks.COBBLESTONE);
        test.assertTrue(progress(test, player, target) == 0F, "cobblestone cannot be dug: " + progress(test, player, target));
        test.assertTrue(progress(test, bystander, target) > 0F, "but another player digs it");

        // Flaming Touch: wood only
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_AXE), ModEnchantments.autoSmelt, 1));
        test.setBlock(target, Blocks.OAK_PLANKS);
        test.assertTrue(progress(test, player, target) >= 1F, "planks break at once");
        test.setBlock(target, Blocks.OAK_LOG);
        test.assertTrue(progress(test, player, target) >= 1F, "and logs");
        test.setBlock(target, Blocks.DIRT);
        test.assertTrue(progress(test, player, target) == 0F, "dirt does not");

        // another tool in the hand: no enchantment, no change
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        test.assertTrue(Math.abs(progress(test, player, target) - progress(test, bystander, target)) < 1.0E-6F, "no enchantment, no change");
        // and the enchantment in the off hand or the inventory is nothing
        player.setItemInHand(InteractionHand.OFF_HAND, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.desintegrate, 1));
        player.getInventory().setItem(7, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.desintegrate, 1));
        test.assertTrue(Math.abs(progress(test, player, target) - progress(test, bystander, target)) < 1.0E-6F, "only the main hand counts");
        player.discard();
        finish(test, bystander);
    }

    public static void shatterAndTunnelScaleTheDigSpeed(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 3);
        test.setBlock(target, Blocks.OBSIDIAN);
        ItemStack shatter = enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.shatter, 2);
        ServerPlayer player = miner(test, new ItemStack(Items.DIAMOND_PICKAXE));
        float plain = progress(test, player, target);
        player.setItemInHand(InteractionHand.MAIN_HAND, shatter);
        float shattered = progress(test, player, target);
        test.assertTrue(plain > 0F && Math.abs(shattered / plain - 6F) < 0.01F, "obsidian 6 times faster with Shatter II: " + plain + " -> " + shattered);

        test.setBlock(target, Blocks.STONE);
        float stone = progress(test, player, target);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        float stonePlain = progress(test, player, target);
        test.assertTrue(Math.abs(stone / stonePlain - 0.8F) < 0.01F, "stone at 0.8: " + stonePlain + " -> " + stone);

        // Tunnel: nothing remembered, then the same way, then a turn
        ItemStack tunnel = enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.tunnel, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, tunnel);
        test.assertTrue(Math.abs(progress(test, player, target) / stonePlain - 1F) < 0.01F, "no direction remembered, no change");
        // a real break remembers the direction (Architectury's BlockEvent.BREAK is posted by ServerPlayerGameMode.destroyBlock)
        test.assertTrue(player.gameMode.destroyBlock(test.absolutePos(target)), "the stone is broken");
        float remembered = customData(player.getMainHandItem()).getFloat(ModEnchantmentHandler.NBT_TUNNEL_DIRECTION);
        test.assertTrue(remembered == player.getYRot(), "the direction is kept on the tool: " + remembered);
        test.setBlock(target, Blocks.STONE);
        test.assertTrue(Math.abs(progress(test, player, target) / stonePlain - 1.6F) < 0.01F, "the same way: 1 + .2 * 3");
        player.setYRot(player.getYRot() + 90F);
        test.assertTrue(Math.abs(progress(test, player, target) / stonePlain - 0.3F) < 0.01F, "after a turn: 0.3");
        // the yaw of the server wraps at 180: 179 and -179 are two degrees apart
        ItemNBTHelper.setFloat(player.getMainHandItem(), ModEnchantmentHandler.NBT_TUNNEL_DIRECTION, 179F);
        player.setYRot(-179F);
        test.assertTrue(Math.abs(progress(test, player, target) / stonePlain - 1.6F) < 0.01F, "across the seam of the angle it is the same way");

        // Shatter and Tunnel together multiply, in the hands of the one who holds both
        // (an item with Shatter in the off hand and Tunnel in the main hand is only Tunnel)
        player.setYRot(0F);
        ItemNBTHelper.setFloat(player.getMainHandItem(), ModEnchantmentHandler.NBT_TUNNEL_DIRECTION, 0F);
        player.setItemInHand(InteractionHand.OFF_HAND, shatter.copy());
        test.assertTrue(Math.abs(progress(test, player, target) / stonePlain - 1.6F) < 0.01F, "the off hand adds nothing");

        // the direction is remembered by a tool of Tunnel and by no other: a plain pickaxe and a Shatter one break a block and keep
        // no data at all (the stone is broken for real, through the same call as above)
        for (ItemStack other : List.of(new ItemStack(Items.DIAMOND_PICKAXE), shatter.copy())) {
            test.setBlock(target, Blocks.STONE);
            player.setItemInHand(InteractionHand.MAIN_HAND, other);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            test.assertTrue(player.gameMode.destroyBlock(test.absolutePos(target)), "a tool without Tunnel breaks the stone as well");
            ItemStack held = player.getMainHandItem();
            test.assertTrue(held.get(DataComponents.CUSTOM_DATA) == null || !customData(held).contains(ModEnchantmentHandler.NBT_TUNNEL_DIRECTION),
                    "a tool without Tunnel remembers no direction: " + held.get(DataComponents.CUSTOM_DATA));
        }
        player.discard();
        test.succeed();
    }

    /**
     * Disintegrate refuses a block that is not its own: no progress on the real dig path (the first click, the ticks of the
     * game mode, the finishing packet), where the same pickaxe without the enchantment gets the block down. A block that is its
     * own goes at the first click. The wear of Disintegrate and Flaming Touch is one point for the block, besides the point
     * that breaking a block costs a pickaxe anyway.
     */
    public static void aToolOfDisintegrateRefusesTheOtherBlocksAndWearsDoubleOnItsOwn(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos dirt = new BlockPos(3, 1, 3);
        BlockPos stone = new BlockPos(5, 1, 3);
        BlockPos stone2 = new BlockPos(5, 1, 5);
        test.setBlock(dirt, Blocks.DIRT);
        test.setBlock(stone, Blocks.COBBLESTONE);
        test.setBlock(stone2, Blocks.COBBLESTONE);
        ServerPlayer player = miner(test, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.desintegrate, 1));
        ServerPlayer plainPlayer = miner(test, new ItemStack(Items.DIAMOND_PICKAXE));
        int sequence = 0;

        // the first click on cobblestone, then a long dig, then the packet that says it is finished, then the ticks of the delayed
        // destroy that the game mode keeps for a finish that came early
        player.gameMode.handleBlockBreakAction(test.absolutePos(stone), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.UP, level.getMaxBuildHeight(), ++sequence);
        for (int i = 0; i < 100; i++) player.gameMode.tick();
        player.gameMode.handleBlockBreakAction(test.absolutePos(stone), ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                Direction.UP, level.getMaxBuildHeight(), ++sequence);
        for (int i = 0; i < 100; i++) player.gameMode.tick();
        test.assertTrue(level.getBlockState(test.absolutePos(stone)).is(Blocks.COBBLESTONE), "cobblestone stays under a Disintegrate pickaxe");

        // control: the same dig with the same pickaxe without the enchantment gets it down
        plainPlayer.gameMode.handleBlockBreakAction(test.absolutePos(stone2), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.UP, level.getMaxBuildHeight(), ++sequence);
        for (int i = 0; i < 100; i++) plainPlayer.gameMode.tick();
        plainPlayer.gameMode.handleBlockBreakAction(test.absolutePos(stone2), ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                Direction.UP, level.getMaxBuildHeight(), ++sequence);
        for (int i = 0; i < 100; i++) plainPlayer.gameMode.tick();
        test.assertTrue(level.getBlockState(test.absolutePos(stone2)).isAir(), "the same dig with a plain pickaxe gets the cobblestone down");

        // the block that is its own: one click; the wear is what the pickaxe has lost over the break
        int wornBefore = player.getMainHandItem().getDamageValue();
        player.gameMode.handleBlockBreakAction(test.absolutePos(dirt), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.UP, level.getMaxBuildHeight(), ++sequence);
        test.assertTrue(level.getBlockState(test.absolutePos(dirt)).isAir(), "dirt is gone at the first click");
        int worn = player.getMainHandItem().getDamageValue() - wornBefore;
        test.assertTrue(worn == 2, "one point for the block and one for Disintegrate: " + worn);

        // control: a plain pickaxe breaking the same kind of block is worn by one point
        test.setBlock(dirt, Blocks.DIRT);
        int plainBefore = plainPlayer.getMainHandItem().getDamageValue();
        test.assertTrue(plainPlayer.gameMode.destroyBlock(test.absolutePos(dirt)), "the plain pickaxe breaks dirt");
        int plainWorn = plainPlayer.getMainHandItem().getDamageValue() - plainBefore;
        test.assertTrue(plainWorn == 1, "a plain pickaxe wears one point for it: " + plainWorn);

        // a block that is not Disintegrate's, broken by a direct call (which asks no dig progress, so nothing refuses it): the
        // wear is the pickaxe's own, one point, not the extra one of the enchantment
        BlockPos rock = new BlockPos(1, 1, 1);
        test.setBlock(rock, Blocks.COBBLESTONE);
        int rockBefore = player.getMainHandItem().getDamageValue();
        test.assertTrue(player.gameMode.destroyBlock(test.absolutePos(rock)), "a direct call breaks the cobblestone");
        int rockWorn = player.getMainHandItem().getDamageValue() - rockBefore;
        test.assertTrue(rockWorn == 1, "cobblestone is not Disintegrate's: a plain point of wear: " + rockWorn);

        // Flaming Touch: an axe on planks wears the same two points
        BlockPos planks = new BlockPos(1, 1, 5);
        test.setBlock(planks, Blocks.OAK_PLANKS);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_AXE), ModEnchantments.autoSmelt, 1));
        player.gameMode.handleBlockBreakAction(test.absolutePos(planks), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.UP, level.getMaxBuildHeight(), ++sequence);
        test.assertTrue(level.getBlockState(test.absolutePos(planks)).isAir(), "planks are gone at the first click");
        test.assertTrue(player.getMainHandItem().getDamageValue() == 2, "two points: " + player.getMainHandItem().getDamageValue());

        // and the same axe on a block that is not wood, by a direct call: one point
        test.setBlock(dirt, Blocks.DIRT);
        int axeBefore = player.getMainHandItem().getDamageValue();
        test.assertTrue(player.gameMode.destroyBlock(test.absolutePos(dirt)), "a direct call breaks the dirt");
        int axeWorn = player.getMainHandItem().getDamageValue() - axeBefore;
        test.assertTrue(axeWorn == 1, "dirt is not wood: a plain point of wear for Flaming Touch: " + axeWorn);
        player.discard();
        finish(test, plainPlayer);
    }

    // ---- movement (LIVING_JUMP, LIVING_FALL) --------------------------------------------------------------------------

    private static void wear(GameTestHelper test, LivingEntity wearer, EquipmentSlot slot, Item item, ResourceKey<Enchantment> key, int level) {
        wearer.setItemSlot(slot, enchanted(test, new ItemStack(item), key, level));
    }

    /**
     * Ascent Boost: the vertical motion of the jump, right after the jump set it, is multiplied by (level + 2) / 2, with the
     * highest level on the four armor slots, for a player that does not sneak; nobody else.
     */
    public static void ascentBoostMultipliesTheJumpOfAPlayer(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.jumpFromGround();
        test.assertTrue(Math.abs(player.getDeltaMovement().y - 0.42D) < 1.0E-4D, "without armor a plain jump: " + player.getDeltaMovement().y);

        wear(test, player, EquipmentSlot.LEGS, Items.DIAMOND_LEGGINGS, ModEnchantments.ascentBoost, 3);
        wear(test, player, EquipmentSlot.HEAD, Items.DIAMOND_HELMET, ModEnchantments.ascentBoost, 1);
        player.setDeltaMovement(Vec3.ZERO);
        player.jumpFromGround();
        test.assertTrue(Math.abs(player.getDeltaMovement().y - 0.42D * 2.5D) < 1.0E-4D,
                "the highest level on any slot, 3: 0.42 * 2.5 = 1.05, once: " + player.getDeltaMovement().y);

        player.setShiftKeyDown(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.jumpFromGround();
        test.assertTrue(Math.abs(player.getDeltaMovement().y - 0.42D) < 1.0E-4D, "not while sneaking: " + player.getDeltaMovement().y);
        player.setShiftKeyDown(false);

        // a pig in the same leggings is not a player
        Pig pig = toughPig(test, new Vec3(3, 1, 3));
        pig.setItemSlot(EquipmentSlot.LEGS, enchanted(test, new ItemStack(Items.DIAMOND_LEGGINGS), ModEnchantments.ascentBoost, 4));
        pig.jumpFromGround();
        test.assertTrue(Math.abs(pig.getDeltaMovement().y - 0.42D) < 1.0E-4D, "a mob is not boosted: " + pig.getDeltaMovement().y);
        pig.discard();

        // the player's own path, the jump key on the ground and one tick (the aiStep path of the client's player): level 2 is twice
        ServerPlayer walker = player(test);
        walker.setPos(test.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
        wear(test, walker, EquipmentSlot.FEET, Items.DIAMOND_BOOTS, ModEnchantments.ascentBoost, 2);
        double floorY = test.absoluteVec(new Vec3(0, 1, 0)).y;
        walker.setOnGround(true);
        walker.setJumping(true);
        walker.doTick();
        test.assertTrue(Math.abs(walker.getY() - (floorY + 0.84D)) < 0.02D, "the doubled jump carried the player up: " + (walker.getY() - floorY));
        test.assertTrue(Math.abs(walker.getDeltaMovement().y - 0.7448D) < 0.01D,
                "and what is left of it after the tick is (0.84 - 0.08) * 0.98, not the 0.3332 of a plain jump: " + walker.getDeltaMovement().y);
        walker.discard();
        finish(test, player);
    }

    /**
     * A new server player refuses every hit for 60 ticks ({@code ServerPlayer.tick} counts {@code spawnInvulnerableTime} down). The
     * level does that only for a player in an entity-ticking chunk, which the area of a test may not be yet on a busy server, so the
     * check counts the protection down itself instead of waiting for it.
     */
    private static void endSpawnProtection(ServerPlayer... players) {
        for (ServerPlayer player : players) {
            for (int i = 0; i < 61; i++) player.tick();
        }
    }

    /** The landing of a server player, as the connection does it from the movement packet of a player that touches the ground. */
    private static void land(ServerPlayer player, float fallDistance) {
        player.fallDistance = fallDistance;
        player.setOnGround(true);
        player.doCheckFallDamage(0.0D, 0.0D, 0.0D, true);
    }

    /**
     * Slow Fall: a landing of 2.9 blocks or more is a landing of 2.9 (no damage), for a player with the enchantment on the armor
     * and not sneaking; shorter falls, mobs and players without it are left alone.
     */
    public static void slowFallMakesALandingAFallOfTwoPointNineBlocks(GameTestHelper test) {
        floor(test);
        ServerPlayer slow = player(test);
        ServerPlayer plain = player(test);
        ServerPlayer sneaking = player(test);
        slow.setPos(test.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
        plain.setPos(test.absoluteVec(new Vec3(4.5, 1.0, 2.5)));
        sneaking.setPos(test.absoluteVec(new Vec3(2.5, 1.0, 4.5)));
        wear(test, slow, EquipmentSlot.FEET, Items.DIAMOND_BOOTS, ModEnchantments.slowFall, 1);
        wear(test, sneaking, EquipmentSlot.FEET, Items.DIAMOND_BOOTS, ModEnchantments.slowFall, 3);
        sneaking.setShiftKeyDown(true);
        ServerLevel level = test.getLevel();

        // what the calculation does with the distance (the return value is "some damage was worked out": no spawn protection in it)
        test.assertTrue(!slow.causeFallDamage(10F, 1F, level.damageSources().fall()), "a fall of 10 with Slow Fall works out no damage");
        test.assertTrue(plain.causeFallDamage(10F, 1F, level.damageSources().fall()), "and without it some");
        test.assertTrue(sneaking.causeFallDamage(10F, 1F, level.damageSources().fall()), "sneaking turns Slow Fall off");
        test.assertTrue(!slow.causeFallDamage(2.5F, 1F, level.damageSources().fall()) && !plain.causeFallDamage(2.5F, 1F, level.damageSources().fall()),
                "a short fall is a short fall");
        // the worn level does not change the 2.9: Slow Fall I and III alike
        sneaking.setShiftKeyDown(false);
        test.assertTrue(!sneaking.causeFallDamage(10F, 1F, level.damageSources().fall()), "Slow Fall III as well");

        // a mob in the same boots is not a player
        Zombie zombie = armedZombie(test, new Vec3(6, 1, 2), ItemStack.EMPTY);
        zombie.setItemSlot(EquipmentSlot.FEET, enchanted(test, new ItemStack(Items.DIAMOND_BOOTS), ModEnchantments.slowFall, 3));
        test.assertTrue(zombie.causeFallDamage(10F, 1F, level.damageSources().fall()), "a zombie in Slow Fall boots falls as ever");
        zombie.discard();

        // the real landing of a server player, the path of the movement packet; the damage is the health that is lost once the
        // spawn protection of the new player is over
        endSpawnProtection(slow, plain);
        try {
            slow.setHealth(20F);
            plain.setHealth(20F);
            land(slow, 10F);
            land(plain, 10F);
            test.assertTrue(slow.getHealth() == 20F, "the player in Slow Fall boots lands from 10 blocks unhurt: " + slow.getHealth());
            near(test, plain.getHealth(), 20F - 7F, "the other one takes the 7 of ten blocks");
            test.assertTrue(slow.fallDistance == 0F, "the landing resets the distance");
        } finally {
            slow.discard();
            plain.discard();
            sneaking.discard();
        }
        test.succeed();
    }

    /**
     * Shockwave: a player with the boots lands from more than three blocks, and every other living entity within ten blocks
     * takes .1 * level * distance of fall damage. Not the faller, not for a short fall, not without the boots; a player that
     * can fly never gets to the event. (The blast tests {@code isClientSide}; a server cannot run the client's copy of the event.)
     *
     * <p>Every refusal is tried on creatures that can be hurt: the invulnerability frames of the pigs are cleared (and their
     * health refilled) before each landing, and a landing that must blast comes after the refusals, in the same state, so a
     * wave that did not happen is the rule's doing and not the frames' (a wave of .6 against the last hit of 2.0 would be refused
     * by the game whatever the listener did). The boots that carry Slow Fall as well blast nothing: the listener of Slow Fall
     * runs first and Shockwave sees a fall of 2.9 blocks.</p>
     */
    public static void shockwaveHurtsTheCreaturesAroundALanding(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        wear(test, player, EquipmentSlot.FEET, Items.DIAMOND_BOOTS, ModEnchantments.shockwave, 2);
        Pig near = toughPig(test, new Vec3(3, 1, 3));
        Pig far = toughPig(test, new Vec3(0, 1, 0));
        Pig above = toughPig(test, new Vec3(3, 6, 3));  // inside the box of ten blocks
        Pig outside = toughPig(test, new Vec3(3, 12, 3)); // 11 blocks above the faller: outside
        List<Pig> pigs = List.of(near, far, above, outside);
        ServerPlayer bare = player(test);
        bare.setPos(test.absoluteVec(new Vec3(2.5, 1.0, 5.5)));
        ServerPlayer both = player(test);
        both.setPos(test.absoluteVec(new Vec3(5.5, 1.0, 2.5)));
        wear(test, both, EquipmentSlot.FEET, Items.DIAMOND_BOOTS, ModEnchantments.slowFall, 2);
        both.getItemBySlot(EquipmentSlot.FEET).enchant(holder(test, ModEnchantments.shockwave), 2);
        endSpawnProtection(player, bare, both);
        Runnable fresh = () -> {
            for (Pig pig : pigs) {
                pig.setHealth(1000F);
                pig.invulnerableTime = 0;
            }
            player.setHealth(20F);
            both.setHealth(20F);
            player.invulnerableTime = 0;
            both.invulnerableTime = 0;
        };
        try {
            float expected = EnchantmentFormulas.shockwaveDamage(2, 10F);
            fresh.run();
            land(player, 10F);
            near(test, 1000F - near.getHealth(), expected, "a pig nearby took the wave");
            near(test, 1000F - far.getHealth(), expected, "so did one at the other end of the floor");
            near(test, 1000F - above.getHealth(), expected, "and one above the faller, inside the box");
            test.assertTrue(outside.getHealth() == 1000F, "one 11 blocks above is out of reach: " + outside.getHealth());
            near(test, player.getHealth(), 20F - 7F, "the faller takes its own fall (7) and nothing of its wave");

            // a fall of three blocks, no more, does not shake the ground; the pigs can be hurt (their frames are clear)
            fresh.run();
            land(player, 3F);
            for (Pig pig : pigs) test.assertTrue(pig.getHealth() == 1000F, "a fall of three blocks does not shake the ground: " + pig.getHealth());
            // and a fall without the boots does not either
            fresh.run();
            land(bare, 10F);
            for (Pig pig : pigs) test.assertTrue(pig.getHealth() == 1000F, "a fall without the boots does not either: " + pig.getHealth());
            // a player that can fly does not get to the event
            fresh.run();
            player.getAbilities().mayfly = true;
            land(player, 10F);
            player.getAbilities().mayfly = false;
            for (Pig pig : pigs) test.assertTrue(pig.getHealth() == 1000F, "a player that can fly lands without a wave: " + pig.getHealth());
            // the boots of Slow Fall and Shockwave together: Slow Fall's listener runs first, the wave sees 2.9 blocks and is not made
            fresh.run();
            land(both, 10F);
            for (Pig pig : pigs) test.assertTrue(pig.getHealth() == 1000F, "Slow Fall runs before Shockwave: no wave from 10 blocks: " + pig.getHealth());
            test.assertTrue(both.getHealth() == 20F, "and the wearer lands unhurt: " + both.getHealth());

            // the control, in the same state as the refusals: a little more than three blocks is a wave, of .1 * 2 * 3.5
            fresh.run();
            land(player, 3.5F);
            near(test, 1000F - near.getHealth(), EnchantmentFormulas.shockwaveDamage(2, 3.5F), "a fall of 3.5 blocks is a small wave, so the refusals above were the rule's");
            test.assertTrue(outside.getHealth() == 1000F, "and still not the pig outside the box");
        } finally {
            player.discard();
            bare.discard();
            both.discard();
        }
        test.succeed();
    }

    // ---- Quick Draw (USE_ITEM_TICK) ---------------------------------------------------------------------------------

    /** The handler of 1.7.10, line by line: the count it kept, the decrement of the tick and then its own change. */
    private static int draw1710(int quickDraw, int ticks) {
        int itemInUseCount = 72000;
        for (int tick = 0; tick < ticks; tick++) {
            --itemInUseCount;
            int speedup = quickDraw / 6;
            int level = quickDraw % 6;
            if (itemInUseCount % (6 - level) == 0) speedup++;
            itemInUseCount = Math.max(2, itemInUseCount - speedup);
        }
        return 72000 - itemInUseCount;
    }

    private static double fastestArrowNear(ServerLevel level, ServerPlayer player) {
        double fastest = -1D;
        for (Arrow arrow : level.getEntitiesOfClass(Arrow.class, player.getBoundingBox().inflate(3D))) {
            if (arrow.getOwner() == player) fastest = Math.max(fastest, arrow.getDeltaMovement().length());
        }
        return fastest;
    }

    private static ServerPlayer archer(GameTestHelper test, double x, double z, ItemStack bow) {
        ServerPlayer player = player(test);
        player.setPos(test.absoluteVec(new Vec3(x, 1.0, z)));
        player.getInventory().setItem(0, bow);
        player.getInventory().setItem(1, new ItemStack(Items.ARROW, 8));
        player.getInventory().selected = 0;
        return player;
    }

    /**
     * Quick Draw: while a player draws a bow with the enchantment, the draw clock (what the bow pose, the HUD and the power of the
     * arrow read) runs on by the ticks of the 1.7.10 handler, which this check computes line by line on its own. Level I and II,
     * against a plain bow; the arrow of a released quick bow flies as fast as the longer draw makes it. Food with the
     * enchantment, and a mob that draws, are left alone.
     */
    public static void quickDrawTakesTicksOffTheDrawClockOfABow(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        ServerPlayer plain = archer(test, 2.5, 2.5, new ItemStack(Items.BOW));
        ServerPlayer quick1 = archer(test, 5.5, 2.5, enchanted(test, new ItemStack(Items.BOW), ModEnchantments.quickDraw, 1));
        ServerPlayer quick2 = archer(test, 2.5, 5.5, enchanted(test, new ItemStack(Items.BOW), ModEnchantments.quickDraw, 2));
        ServerPlayer eater = archer(test, 5.5, 5.5, enchanted(test, new ItemStack(Items.APPLE), ModEnchantments.quickDraw, 2));
        Zombie skeletonLike = armedZombie(test, new Vec3(1, 1, 1), enchanted(test, new ItemStack(Items.BOW), ModEnchantments.quickDraw, 2));
        List<ServerPlayer> players = List.of(plain, quick1, quick2, eater);
        test.runAfterDelay(2, () -> {
            try {
                for (ServerPlayer player : players) player.startUsingItem(InteractionHand.MAIN_HAND);
                skeletonLike.startUsingItem(InteractionHand.MAIN_HAND);
                int full = plain.getUseItemRemainingTicks();
                test.assertTrue(full == 72000, "a bow is drawn from 72000: " + full);
                int apple = eater.getUseItemRemainingTicks();
                // nothing ticks a mock player: do what the connection of a real one does
                for (int i = 0; i < 12; i++) {
                    for (ServerPlayer player : players) player.doTick();
                    skeletonLike.tick();
                }
                test.assertTrue(plain.getTicksUsingItem() == 12, "the plain draw clock: " + plain.getTicksUsingItem());
                test.assertTrue(quick1.getTicksUsingItem() == draw1710(1, 12), "Quick Draw I: " + quick1.getTicksUsingItem() + " against " + draw1710(1, 12));
                test.assertTrue(quick2.getTicksUsingItem() == draw1710(2, 12), "Quick Draw II: " + quick2.getTicksUsingItem() + " against " + draw1710(2, 12));
                test.assertTrue(draw1710(2, 12) > 12 && draw1710(1, 12) > 12, "the oracle itself gives more than the plain clock");
                test.assertTrue(eater.getUseItemRemainingTicks() == apple - 12 && eater.getTicksUsingItem() == 12,
                        "the enchantment on food does nothing: " + eater.getUseItemRemainingTicks() + " of " + apple);
                test.assertTrue(skeletonLike.getTicksUsingItem() == 12, "a mob that draws a bow is not a player: " + skeletonLike.getTicksUsingItem());
                for (int i = 12; i < 40; i++) {
                    for (ServerPlayer player : List.of(plain, quick1, quick2)) player.doTick();
                }
                test.assertTrue(plain.getTicksUsingItem() == 40, "the plain clock, 40 ticks: " + plain.getTicksUsingItem());
                test.assertTrue(quick1.getTicksUsingItem() == draw1710(1, 40), "Quick Draw I, 40 ticks: " + quick1.getTicksUsingItem() + " against " + draw1710(1, 40));
                test.assertTrue(quick2.getTicksUsingItem() == draw1710(2, 40), "Quick Draw II, 40 ticks: " + quick2.getTicksUsingItem() + " against " + draw1710(2, 40));
                test.assertTrue(quick2.getUseItemRemainingTicks() == 72000 - draw1710(2, 40), "the count the entity holds is the changed one");
            } finally {
                skeletonLike.discard();
                eater.discard();
            }
            // the arrow: released after the same short draw (restart the draws, 12 ticks, release)
            try {
                for (ServerPlayer player : List.of(plain, quick1, quick2)) {
                    player.stopUsingItem();
                    player.startUsingItem(InteractionHand.MAIN_HAND);
                    for (int i = 0; i < 12; i++) player.doTick();
                }
                for (ServerPlayer player : List.of(plain, quick1, quick2)) player.releaseUsingItem();
                double plainSpeed = fastestArrowNear(level, plain);
                double quick1Speed = fastestArrowNear(level, quick1);
                double quick2Speed = fastestArrowNear(level, quick2);
                double plainExpected = BowItem.getPowerForTime(12) * 3D;
                double quick1Expected = BowItem.getPowerForTime(draw1710(1, 12)) * 3D;
                double quick2Expected = BowItem.getPowerForTime(draw1710(2, 12)) * 3D;
                test.assertTrue(Math.abs(plainSpeed - plainExpected) < 0.2D, "the plain arrow of 12 ticks: " + plainSpeed + " (expected " + plainExpected + ")");
                test.assertTrue(Math.abs(quick1Speed - quick1Expected) < 0.2D, "Quick Draw I: " + quick1Speed + " (expected " + quick1Expected + ")");
                test.assertTrue(Math.abs(quick2Speed - quick2Expected) < 0.2D, "Quick Draw II: " + quick2Speed + " (expected " + quick2Expected + ")");
                test.assertTrue(quick2Speed > plainSpeed + 0.3D, "the quick bow is the stronger: " + quick2Speed + " against " + plainSpeed);
            } finally {
                for (ServerPlayer player : players) player.discard();
            }
            test.succeed();
        });
    }

    // ---- the item types of the 1.7.10 Enchanter ------------------------------------------------------------------------

    private static ItemStack itemOf(GameTestHelper test, String id) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
        test.assertTrue(item != null, "the item " + id + " exists in the world");
        return new ItemStack(item);
    }

    private static boolean offers(GameTestHelper test, ItemStack stack, ResourceKey<Enchantment> key) {
        return EnchantmentManager.canApply(test.getLevel().registryAccess(), stack, key, List.of());
    }

    /**
     * 1.7.10 asked {@code enchant.canApply(stack) && enchant.type.canEnchantItem(item)}, the narrow type: swords for the
     * weapon enchantments, {@code ItemTool} for the diggers (no hoe, no shears), chest armor for Thorns, any armor of the
     * slot for the legs and boots. The Enchanter asks the primary items of 1.21.1 (and refuses hoes and shears) for the same.
     */
    public static void theEnchanterOffersWhatTheTypesOf1710AllowedAndNotWhatTheWiderTagsOf121Allow(GameTestHelper test) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack shovel = new ItemStack(Items.DIAMOND_SHOVEL);
        ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
        ItemStack shears = new ItemStack(Items.SHEARS);
        ItemStack mace = new ItemStack(Items.MACE);

        for (ResourceKey<Enchantment> weapon : List.of(Vanilla.SHARPNESS, Vanilla.SMITE, Vanilla.BANE_OF_ARTHROPODS, Vanilla.FIRE_ASPECT)) {
            test.assertTrue(offers(test, sword, weapon), weapon.location() + " goes on a sword");
            test.assertTrue(!offers(test, axe, weapon), weapon.location() + " does not go on an axe in the Enchanter (an anvil takes it)");
            test.assertTrue(!offers(test, mace, weapon), weapon.location() + " does not go on a mace");
            test.assertTrue(!offers(test, pick, weapon), weapon.location() + " does not go on a pickaxe");
        }
        for (ResourceKey<Enchantment> digger : List.of(Vanilla.EFFICIENCY, Vanilla.FORTUNE, Vanilla.SILK_TOUCH)) {
            for (ItemStack tool : List.of(pick, axe, shovel)) {
                test.assertTrue(offers(test, tool, digger), digger.location() + " goes on " + tool.getItem());
            }
            test.assertTrue(!offers(test, hoe, digger), digger.location() + " does not go on a hoe (ItemHoe was no ItemTool)");
            test.assertTrue(!offers(test, shears, digger), digger.location() + " does not go on shears");
            test.assertTrue(!offers(test, sword, digger), digger.location() + " does not go on a sword");
        }
        test.assertTrue(offers(test, hoe, Vanilla.UNBREAKING) && offers(test, shears, Vanilla.UNBREAKING),
                "Unbreaking goes on everything that wears, as 'breakable' did");

        test.assertTrue(offers(test, new ItemStack(Items.DIAMOND_CHESTPLATE), Vanilla.THORNS), "Thorns on a chestplate");
        for (Item other : List.of(Items.DIAMOND_HELMET, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)) {
            test.assertTrue(!offers(test, new ItemStack(other), Vanilla.THORNS), "Thorns (armor_torso) not on " + other);
        }

        // the items of Thaumcraft that the types accepted and no vanilla tag lists
        ItemStack crimson = itemOf(test, "thaumcraft:crimson_sword");
        for (ResourceKey<Enchantment> key : List.of(Vanilla.SHARPNESS, Vanilla.LOOTING, Vanilla.FIRE_ASPECT, Vanilla.UNBREAKING,
                ModEnchantments.vampirism, ModEnchantments.finalStrike, ModEnchantments.valiance, ModEnchantments.focusedStrike,
                ModEnchantments.dispersedStrikes)) {
            test.assertTrue(offers(test, crimson, key), key.location() + " goes on the Crimson Blade");
        }
        ItemStack crusher = itemOf(test, "thaumcraft:primal_crusher");
        for (ResourceKey<Enchantment> key : List.of(Vanilla.EFFICIENCY, Vanilla.FORTUNE, Vanilla.SILK_TOUCH, Vanilla.UNBREAKING,
                ModEnchantments.shatter, ModEnchantments.tunnel, ModEnchantments.desintegrate, ModEnchantments.autoSmelt)) {
            test.assertTrue(offers(test, crusher, key), key.location() + " goes on the Primal Crusher");
        }
        for (String legs : List.of("cultist_robe_legs", "cultist_plate_legs", "cultist_leader_legs")) {
            ItemStack stack = itemOf(test, "thaumcraft:" + legs);
            test.assertTrue(offers(test, stack, ModEnchantments.ascentBoost) && offers(test, stack, ModEnchantments.pounce),
                    "Ascent Boost and Pounce go on " + legs);
        }
        ItemStack cultistBoots = itemOf(test, "thaumcraft:cultist_boots");
        test.assertTrue(offers(test, cultistBoots, ModEnchantments.slowFall) && offers(test, cultistBoots, ModEnchantments.shockwave),
                "Slow Fall and Shockwave go on the boots of the Crimson Cult");

        if (TTRegistry.hasItem(LibItemNames.BLOOD_SWORD)) {
            ItemStack blade = TTRegistry.stack(LibItemNames.BLOOD_SWORD);
            for (ResourceKey<Enchantment> key : List.of(Vanilla.SHARPNESS, Vanilla.SMITE, Vanilla.KNOCKBACK, Vanilla.FIRE_ASPECT, Vanilla.LOOTING,
                    Vanilla.UNBREAKING, ModEnchantments.finalStrike, ModEnchantments.valiance, ModEnchantments.focusedStrike,
                    ModEnchantments.dispersedStrikes)) {
                test.assertTrue(offers(test, blade, key), key.location() + " goes on the Cursed Spirit's Blade, an ItemSword");
            }
            test.assertTrue(!offers(test, blade, ModEnchantments.vampirism), "but Vampirism does not");
        }
        test.succeed();
    }

    /**
     * The 1.7.10 enchantments had the weight 0, which the anvil turned into no cost per level from a tool and one per level
     * from a book. An anvil cost of 0 in the data gives the same: {@code max(1, 0 / 2)} for a book, and the vanilla anvil
     * refuses a result whose enchantment cost is 0.
     */
    public static void theAnvilChargesOnePerLevelForABookAndNothingForAToolOfTheEnchantmentsOfTheAddOn(GameTestHelper test) {
        ServerPlayer player = player(test);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(holder(test, ModEnchantments.valiance), 5);
        ItemStack donor = enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.valiance, 5);

        AnvilMenu fromBook = new AnvilMenu(0, player.getInventory());
        fromBook.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        fromBook.getSlot(1).set(book);
        fromBook.createResult();
        ItemStack result = fromBook.getSlot(2).getItem();
        test.assertTrue(ModEnchantments.level(result, ModEnchantments.valiance) == 5, "the book gives Valiance V: " + result);
        test.assertTrue(fromBook.getCost() == 5, "one per level from a book: " + fromBook.getCost());

        AnvilMenu fromTool = new AnvilMenu(0, player.getInventory());
        fromTool.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        fromTool.getSlot(1).set(donor);
        fromTool.createResult();
        test.assertTrue(fromTool.getSlot(2).getItem().isEmpty(), "nothing per level from a tool, so the anvil has nothing to charge for: "
                + fromTool.getSlot(2).getItem());
        finish(test, player);
    }

    // ---- breaking the Enchanter --------------------------------------------------------------------------------------

    private static List<ItemEntity> dropsAround(GameTestHelper test, BlockPos relative) {
        return test.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(test.absolutePos(relative)).inflate(2.0D));
    }

    private static boolean dropped(List<ItemEntity> drops, java.util.function.Predicate<ItemStack> what) {
        return drops.stream().anyMatch(drop -> what.test(drop.getItem()));
    }

    /**
     * {@code breakBlock} of 1.7.10 dropped the tool and the wand out of the table, and the block (a stone-like block that
     * needs a pickaxe) dropped itself: with a pickaxe the block, the tool and the wand fall; with a bare hand the contents
     * still fall and the block does not.
     */
    public static void breakingTheEnchanterDropsTheBlockTheToolAndTheWand(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        Item enchanterItem = TTRegistry.block(LibBlockNames.ENCHANTER).asItem();
        BlockPos absolute = test.absolutePos(ENCHANTER);

        for (boolean withPickaxe : new boolean[] { true, false }) {
            String how = withPickaxe ? "with a pickaxe: " : "with a bare hand: ";
            TileEnchanter tile = enchanter(test);
            tile.setItem(TileEnchanter.SLOT_TOOL, new ItemStack(Items.DIAMOND_SWORD));
            tile.setItem(TileEnchanter.SLOT_WAND, fullWand(test));
            player.setItemInHand(InteractionHand.MAIN_HAND, withPickaxe ? new ItemStack(Items.IRON_PICKAXE) : ItemStack.EMPTY);

            test.assertTrue(player.gameMode.destroyBlock(absolute), how + "the block breaks");
            test.assertTrue(test.getLevel().getBlockState(absolute).isAir(), how + "and is gone");
            List<ItemEntity> drops = dropsAround(test, ENCHANTER);
            test.assertTrue(dropped(drops, stack -> stack.is(Items.DIAMOND_SWORD)), how + "the tool fell out of the table");
            test.assertTrue(dropped(drops, WandHelper::isWand), how + "the wand fell out of the table");
            test.assertTrue(dropped(drops, stack -> stack.is(enchanterItem)) == withPickaxe,
                    how + "the block " + (withPickaxe ? "drops itself" : "needs a pickaxe to drop"));
            drops.forEach(ItemEntity::discard);
        }
        finish(test, player);
    }

    // ---- the fake player of the Animation Tablet ----------------------------------------------------------------------

    /** A fake player (never ticked, never in the level) at the given height above the floor, holding {@code held} in its first slot. */
    private static TinkererFakePlayer fakePlayer(GameTestHelper test, ItemStack held, double height) {
        TinkererFakePlayer fake = new TinkererFakePlayer(test.getLevel(), new GameProfile(UUID.randomUUID(), "[TTEnchantTest]"));
        Vec3 at = test.absoluteVec(new Vec3(3.5, height, 3.5));
        fake.placeAt(at.x, at.y, at.z, 0F, 0F);
        fake.getInventory().setItem(0, held);
        fake.getInventory().selected = 0;
        return fake;
    }

    private static TinkererFakePlayer fakePlayer(GameTestHelper test, ItemStack held) {
        return fakePlayer(test, held, 1.0);
    }

    /** The health the pig loses to one blow of the fake player, the frames of the pig cleared first. */
    private static float blowOf(TinkererFakePlayer fake, Pig pig) {
        pig.invulnerableTime = 0;
        float before = pig.getHealth();
        fake.attackEntity(pig);
        return before - pig.getHealth();
    }

    /**
     * The dig speed of a player that is never ticked (the Animation Tablet's fake player): the event is posted inside
     * {@code getDestroyProgress}, so nobody has to call anything before it, as 1.7.10's event fired inside that very call. Dirt goes
     * at once with Disintegrate, cobblestone does not go at all, and a fake player with a plain shovel digs at the plain speed.
     */
    public static void aPlayerThatIsNeverTickedDigsAtTheSpeedOfItsEnchantments(GameTestHelper test) {
        floor(test);
        BlockPos dirt = new BlockPos(3, 1, 2);
        BlockPos stone = new BlockPos(5, 1, 2);
        test.setBlock(dirt, Blocks.DIRT);
        test.setBlock(stone, Blocks.COBBLESTONE);
        TinkererFakePlayer fake = fakePlayer(test, enchanted(test, new ItemStack(Items.DIAMOND_SHOVEL), ModEnchantments.desintegrate, 1));
        TinkererFakePlayer plainFake = fakePlayer(test, new ItemStack(Items.DIAMOND_SHOVEL));

        float plain = progress(test, plainFake, dirt);
        test.assertTrue(plain > 0F && plain < 1F, "a plain shovel digs dirt at its plain speed: " + plain);
        test.assertTrue(progress(test, fake, dirt) >= 1F, "with Disintegrate the dirt goes in one tick, nothing called before: " + progress(test, fake, dirt));
        test.assertTrue(progress(test, fake, stone) == 0F, "cobblestone is not for Disintegrate");
        test.assertTrue(progress(test, plainFake, stone) > 0F, "and the plain shovel digs it");
        test.succeed();
    }

    /**
     * The attack of a fake player: {@code TinkererFakePlayer.attackEntity} is what the Animation Tablet calls, and in 1.7.10 the
     * {@code LivingHurtEvent} multiplied its damage like a player's. Focused Strikes I (deterministic; the fake player's health is
     * fixed, so Valiance cannot be tried with it): the blow is 1.75 times the plain one, the second on the same pig too, then the
     * counter grows. The plain sword of another fake player is left alone.
     */
    public static void aFakePlayerAttackGetsTheEnchantmentsOfItsSword(GameTestHelper test) {
        floor(test);
        TinkererFakePlayer fake = fakePlayer(test, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.focusedStrike, 1));
        TinkererFakePlayer plainFake = fakePlayer(test, new ItemStack(Items.DIAMOND_SWORD));
        Pig pig = toughPig(test, new Vec3(3, 1, 2));
        fake.refreshHeldItemModifiers();
        double base = fake.getAttributeValue(Attributes.ATTACK_DAMAGE);
        test.assertTrue(base > 1D, "the sword is in the attack damage of the fake player: " + base);

        near(test, blowOf(plainFake, pig), (float) base, "the plain sword of a fake player is the plain blow");
        near(test, blowOf(fake, pig), (float) (base / 2D + .5D * 5 * base / 2D), "Focused Strikes I on the first blow: 1.75 times");
        near(test, blowOf(fake, pig), (float) (base / 2D + .5D * 5 * base / 2D), "and on the second, the counter being 5");
        near(test, blowOf(fake, pig), (float) (base / 2D + .5D * 6 * base / 2D), "and on the third, the counter being 6");
        test.assertTrue(customData(fake.getMainHandItem()).getInt(ModEnchantmentHandler.NBT_SUCCESSIVE_STRIKE) == 6, "the counter is on the sword of the fake player");
        near(test, blowOf(plainFake, pig), (float) base, "the plain sword is still the plain blow");
        test.succeed();
    }

    /**
     * The cross-check of the Animation Tablet: its fake player ({@code TinkererFakePlayer}, the one the Tablet's own is made of)
     * digs a block with a Shatter and a Tunnel pickaxe and hits a pig with Pounce leggings, and the enchantments arrive through
     * the real events, with nothing called by the owner of the fake player. A fake player with the same tool without the
     * enchantment is the control.
     */
    public static void theTabletsFakePlayerDigsAndHitsWithTheEnchantments(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 2);
        test.setBlock(target, Blocks.OBSIDIAN);
        BlockPos absolute = test.absolutePos(target);

        // Shatter II: obsidian six times faster, stone at .8
        TinkererFakePlayer plain = fakePlayer(test, new ItemStack(Items.DIAMOND_PICKAXE));
        TinkererFakePlayer shatter = fakePlayer(test, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.shatter, 2));
        float plainObsidian = progress(test, plain, target);
        test.assertTrue(plainObsidian > 0F && Math.abs(progress(test, shatter, target) / plainObsidian - 6F) < 0.01F,
                "Shatter II digs obsidian six times as fast: " + progress(test, shatter, target) + " against " + plainObsidian);
        test.setBlock(target, Blocks.STONE);
        test.assertTrue(Math.abs(progress(test, shatter, target) / progress(test, plain, target) - 0.8F) < 0.01F, "and stone at .8");

        // Tunnel III: the way of the last break is remembered on the tool (a real break by the fake player), and the same way is faster
        TinkererFakePlayer tunneler = fakePlayer(test, enchanted(test, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.tunnel, 3));
        float stonePlain = progress(test, plain, target);
        test.assertTrue(Math.abs(progress(test, tunneler, target) / stonePlain - 1F) < 0.01F, "nothing remembered yet: the plain speed");
        test.assertTrue(tunneler.gameMode.destroyBlock(absolute), "the fake player breaks the stone");
        test.setBlock(target, Blocks.STONE);
        test.assertTrue(customData(tunneler.getMainHandItem()).contains(ModEnchantmentHandler.NBT_TUNNEL_DIRECTION), "and the tool remembers the way");
        test.assertTrue(Math.abs(progress(test, tunneler, target) / stonePlain - 1.6F) < 0.01F, "the same way: 1 + .2 * 3");
        tunneler.setYRot(90F);
        test.assertTrue(Math.abs(progress(test, tunneler, target) / stonePlain - 0.3F) < 0.01F, "after a turn of the tablet: .3");

        // Pounce IV: the blow of the fake player in the air is doubled, on the ground it is not
        Pig pig = toughPig(test, new Vec3(3, 1, 5));
        TinkererFakePlayer aloft = fakePlayer(test, new ItemStack(Items.DIAMOND_SWORD), 4.0);
        TinkererFakePlayer grounded = fakePlayer(test, new ItemStack(Items.DIAMOND_SWORD), 1.0);
        TinkererFakePlayer unarmoured = fakePlayer(test, new ItemStack(Items.DIAMOND_SWORD), 4.0);
        for (TinkererFakePlayer fake : List.of(aloft, grounded)) {
            fake.setItemSlot(EquipmentSlot.LEGS, enchanted(test, new ItemStack(Items.DIAMOND_LEGGINGS), ModEnchantments.pounce, 4));
            fake.refreshHeldItemModifiers();
        }
        unarmoured.refreshHeldItemModifiers();
        float swordBlow = (float) aloft.getAttributeValue(Attributes.ATTACK_DAMAGE);
        test.assertTrue(swordBlow > 1F, "the sword is in the attack damage: " + swordBlow);
        near(test, blowOf(aloft, pig), swordBlow * 2F, "Pounce IV in the air: 1 + .25 * 4");
        near(test, blowOf(grounded, pig), swordBlow, "on the stone floor it does nothing");
        near(test, blowOf(unarmoured, pig), swordBlow, "and the same sword in the air without the leggings is the plain blow");
        test.succeed();
    }

    /**
     * The listeners of the group are registered once: one real blow of a player heals by the level of Vampirism, not twice, and one
     * jump multiplies the jump by its factor, not by its square.
     */
    public static void everyHandlerIsRegisteredOnce(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(test, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.vampirism, 1));
        player.setItemSlot(EquipmentSlot.LEGS, enchanted(test, new ItemStack(Items.DIAMOND_LEGGINGS), ModEnchantments.ascentBoost, 2));
        Pig pig = toughPig(test, new Vec3(3, 1, 6));
        player.setHealth(10F);
        test.runAfterDelay(3, () -> {
            lost(pig, player.damageSources().playerAttack(player), 3F);
            test.assertTrue(player.getHealth() == 11F, "healed once, by the level: " + player.getHealth());
            player.jumpFromGround();
            test.assertTrue(Math.abs(player.getDeltaMovement().y - 0.84D) < 1.0E-4D, "boosted once, by the factor 2: " + player.getDeltaMovement().y);
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
