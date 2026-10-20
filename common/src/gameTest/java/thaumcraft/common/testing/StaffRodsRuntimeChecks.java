package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.Tag;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/**
 * The nine staff rods on real content: the loaded recipes (eight arcane "S at the top, two plain rods on the diagonal" recipes and the
 * primal staff's infusion), the arcane wand assembly with a staff rod (price rod x cap, locked behind the staff's research, a weapon
 * modifier on the result, a save and a load of the biggest staff), a staff refused by the workbench's wand slot, its tile, its menu and
 * the table conversion while a wand is accepted, and a REAL connected player: a staff in hand gives +6 attack damage (a hit of 7 on a
 * real mob, a wand's 1), a staff's rod refills its primal like an elemental wand rod over a bigger capacity, and the primal staff adds
 * one vis to one low primal every fiftieth tick. Players, wands and mobs are explicit inputs; the recipes, the assembled staves, the
 * modifier and the refills are never supplied. NOT a client check (the staff models and icons are unseen) or a full altar ritual.
 */
public final class StaffRodsRuntimeChecks {
    private record Staff(String tag, int cost) {}

    // Original ConfigRecipes:220-227: "  S", " G ", "G  " with the primal charm and the plain rod; the order costs the staff's price.
    private static final List<Staff> ARCANE = List.of(new Staff("greatwood", 8), new Staff("obsidian", 14), new Staff("silverwood", 24),
            new Staff("ice", 14), new Staff("quartz", 14), new Staff("reed", 14), new Staff("blaze", 14), new Staff("bone", 14));
    private static final List<String> ALL = List.of("greatwood", "obsidian", "blaze", "ice", "quartz", "bone", "reed", "silverwood", "primal");

    private StaffRodsRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation recipeId(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        recipes(test, level);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "staff-" + UUID.randomUUID().toString().substring(0, 8)),
                ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        BlockPos table = test.absolutePos(new BlockPos(3, 2, 3));
        BlockState oldState = level.getBlockState(table);
        try {
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player, "a real connected player carries the staves");
            assembly(test, level, player);
            workbench(test, level, player, table);
            weaponAndRefill(test, level, connection, channel, player, oldState, table);
        } catch (RuntimeException failure) {
            cleanup(level, connection, channel, player, table, oldState);
            throw failure;
        }
    }

    // ---- recipes ------------------------------------------------------------------------------------------------------

    private static void recipes(GameTestHelper test, ServerLevel level) {
        for (Staff staff : ARCANE) {
            var holder = level.getRecipeManager().byKey(recipeId("arcane/staff_rod_" + staff.tag())).orElseThrow(
                    () -> new AssertionError("no arcane recipe for the " + staff.tag() + " staff core"));
            test.assertTrue(holder.value() instanceof ArcaneShapedRecipe, staff.tag() + ": an arcane shaped recipe");
            var recipe = (ArcaneShapedRecipe) holder.value();
            test.assertTrue(recipe.research().equals("ROD_" + staff.tag() + "_staff") && recipe.pattern().equals(List.of("  S", " G ", "G  "))
                            && recipe.vis().equals(Map.of("ordo", staff.cost())) && recipe.result().is(item("thaumcraft:staff_rod_" + staff.tag()))
                            && recipe.result().getCount() == 1,
                    staff.tag() + ": ROD_" + staff.tag() + "_staff, the original pattern, " + staff.cost() + " order, one core: " + recipe.pattern() + " " + recipe.vis());
            test.assertTrue(recipe.key().get("S").test(new ItemStack(item("thaumcraft:primal_charm"))) && !recipe.key().get("S").test(new ItemStack(item("thaumcraft:wand_rod_" + staff.tag()))),
                    staff.tag() + ": S is the primal charm");
            test.assertTrue(recipe.key().get("G").test(ThaumcraftWands.rodItem(staff.tag())) && !recipe.key().get("G").test(new ItemStack(item("thaumcraft:primal_charm"))),
                    staff.tag() + ": G is the " + staff.tag() + " wand rod");
            NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
            grid.set(2, new ItemStack(item("thaumcraft:primal_charm")));
            grid.set(4, ThaumcraftWands.rodItem(staff.tag()));
            grid.set(6, ThaumcraftWands.rodItem(staff.tag()));
            test.assertTrue(recipe.matches(new ArcaneRecipeInput(grid), level), staff.tag() + ": the charm and two rods on the diagonal make the core");
            grid.set(6, ThaumcraftWands.rodItem(staff.tag().equals("bone") ? "reed" : "bone"));
            test.assertTrue(!recipe.matches(new ArcaneRecipeInput(grid), level), staff.tag() + ": a different second rod does not");
        }
        var holder = level.getRecipeManager().byKey(recipeId("infusion/staff_rod_primal")).orElseThrow(() -> new AssertionError("no infusion recipe for the primal staff core"));
        var recipe = (InfusionStackRecipe) holder.value();
        test.assertTrue(recipe.research().equals("ROD_primal_staff") && recipe.instability() == 8 && recipe.result().is(item("thaumcraft:staff_rod_primal"))
                        && recipe.result().getCount() == 1, "the primal core: ROD_primal_staff, instability 8, one core");
        test.assertTrue(recipe.aspects().equals(Map.of("aer", 32, "ignis", 32, "aqua", 32, "terra", 32, "ordo", 32, "perditio", 32, "praecantatio", 64)),
                "the primal core costs 32 of each primal and 64 magic: " + recipe.aspects());
        test.assertTrue(recipe.input().test(new ItemStack(item("thaumcraft:wand_rod_silverwood"))) && recipe.components().size() == 8,
                "the centre is the silverwood rod with eight components");
        List<ItemStack> parts = List.of(new ItemStack(item("thaumcraft:primal_charm")), ThaumcraftWands.rodItem("obsidian"), ThaumcraftWands.rodItem("ice"),
                ThaumcraftWands.rodItem("quartz"), new ItemStack(item("thaumcraft:primal_charm")), ThaumcraftWands.rodItem("reed"), ThaumcraftWands.rodItem("blaze"),
                ThaumcraftWands.rodItem("bone"));
        test.assertTrue(recipe.matches(new InfusionRecipeInput(ThaumcraftWands.rodItem("silverwood"), parts), level), "two charms and the six elemental rods complete it");
        List<ItemStack> short1 = parts.subList(0, 7);
        test.assertTrue(!recipe.matches(new InfusionRecipeInput(ThaumcraftWands.rodItem("silverwood"), short1), level), "without the bone rod it does not");
        test.assertTrue(!recipe.matches(new InfusionRecipeInput(ThaumcraftWands.rodItem("greatwood"), parts), level), "another centre does not");
    }

    // ---- assembly -----------------------------------------------------------------------------------------------------

    private static void assembly(GameTestHelper test, ServerLevel level, ServerPlayer player) {
        ArcaneWandRecipe recipe = (ArcaneWandRecipe) level.getRecipeManager().byKey(recipeId("arcane/wand_assembly")).orElseThrow().value();
        var knowledge = KnowledgeAccess.get(player);
        for (String tag : ALL) {
            String rod = tag + "_staff";
            WandMaterials.Rod material = WandMaterials.rod(rod);
            NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
            ItemStack cap = ThaumcraftWands.capItem("thaumium");
            grid.set(6, cap.copy());
            grid.set(2, cap.copy());
            grid.set(4, ThaumcraftWands.rodItem(rod));
            var input = new ArcaneRecipeInput(grid);
            test.assertTrue(recipe.matches(input, level), tag + ": the staff core and two thaumium caps make a staff");
            ItemStack staff = recipe.assemble(input, level.registryAccess());
            WandData data = ItemWandCasting.data(staff);
            int cost = material.craftCost() * 6;
            test.assertTrue(data.rod().equals(rod) && data.cap().equals("thaumium") && data.vis().isEmpty() && data.capacity() == material.capacity(),
                    tag + ": the assembled staff holds " + material.capacity() / 100 + " vis and starts empty: " + data);
            test.assertTrue(staff.get(ThaumcraftWands.WAND_CRAFT_COST.get()) == cost && recipe.vis(input).values().stream().allMatch(price -> price == cost)
                            && recipe.vis(input).size() == 6, tag + ": the price is core " + material.craftCost() + " x cap 6 = " + cost + " of each primal");
            test.assertTrue(ItemWandCasting.isStaff(staff) && ItemWandCasting.STAFF_MODIFIERS.equals(staff.get(DataComponents.ATTRIBUTE_MODIFIERS)),
                    tag + ": the staff carries the +6 attack damage modifier of the original setRod");
            test.assertTrue(staff.getHoverName().getContents() instanceof TranslatableContents name && name.getArgs().length == 3
                            && name.getArgs()[1] instanceof net.minecraft.network.chat.Component rodName && rodName.getContents() instanceof TranslatableContents rodKey
                            && rodKey.getKey().equals("item.Wand." + tag + ".rod")
                            && name.getArgs()[2] instanceof net.minecraft.network.chat.Component noun && noun.getContents() instanceof TranslatableContents nounKey
                            && nounKey.getKey().equals("item.Wand.staff.obj"),
                    tag + ": it is named after the plain rod with the staff noun: " + staff.getHoverName());
            knowledge.addResearch("CAP_thaumium");
            test.assertTrue(!recipe.hasResearch(input, player), tag + ": locked behind ROD_" + rod);
            knowledge.addResearch("ROD_" + rod);
            test.assertTrue(recipe.hasResearch(input, player), tag + ": open with ROD_" + rod);
            knowledge.removeResearch("ROD_" + rod);
            knowledge.removeResearch("CAP_thaumium");
        }
        // A wand rod makes a wand without the modifier, and the biggest staff survives a save and a load (the component limits are ours).
        NonNullList<ItemStack> plain = NonNullList.withSize(9, ItemStack.EMPTY);
        plain.set(6, ThaumcraftWands.capItem("gold")); plain.set(2, ThaumcraftWands.capItem("gold")); plain.set(4, ThaumcraftWands.rodItem("greatwood"));
        ItemStack wand = recipe.assemble(new ArcaneRecipeInput(plain), level.registryAccess());
        test.assertTrue(!ItemWandCasting.isStaff(wand) && wand.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().isEmpty()
                && wand.getComponentsPatch().get(DataComponents.ATTRIBUTE_MODIFIERS) == null, "a greatwood wand has no weapon modifier and no patch for it");
        ItemStack primal = new ItemStack(ThaumcraftContent.WAND.get());
        ItemWandCasting.withData(primal, new WandData("primal_staff", "thaumium", Map.of("aer", 25000, "terra", 1234)));
        primal.set(ThaumcraftWands.WAND_CRAFT_COST.get(), 32 * 6);
        Tag saved = primal.save(level.registryAccess());
        ItemStack loaded = ItemStack.parse(level.registryAccess(), saved).orElseThrow(() -> new AssertionError("the primal staff could not be loaded back"));
        test.assertTrue(ItemStack.matches(primal, loaded) && ItemWandCasting.data(loaded).amount("aer") == 25000 && loaded.get(ThaumcraftWands.WAND_CRAFT_COST.get()) == 192
                        && ItemWandCasting.STAFF_MODIFIERS.equals(loaded.get(DataComponents.ATTRIBUTE_MODIFIERS)),
                "a full primal staff (250 vis, price 192) survives a save and a load");
    }

    // ---- the workbench ------------------------------------------------------------------------------------------------

    private static void workbench(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos table) {
        ItemStack staff = staff("greatwood_staff", Map.of());
        ItemStack wand = staff("greatwood", Map.of());
        test.assertTrue(ArcaneVis.isCraftingWand(staff) && ArcaneVis.isCraftingWand(wand), "both are wands with supported parts (foci and the altar take a staff)");
        test.assertTrue(!ArcaneVis.isWorkbenchWand(staff) && ArcaneVis.isWorkbenchWand(wand), "only the wand is a workbench wand");
        // The table converts to the arcane workbench for a wand and not for a staff (original BlockTable:211).
        level.setBlock(table, ThaumcraftContent.TABLE.get().state(1), Block.UPDATE_ALL);
        player.closeContainer();
        player.setShiftKeyDown(false);
        player.setPos(table.getX() + .5, table.getY() + 1, table.getZ() + .5);
        player.setItemInHand(InteractionHand.MAIN_HAND, staff.copy());
        use(player, table);
        test.assertTrue(!(level.getBlockEntity(table) instanceof TileArcaneWorkbench) && player.getMainHandItem().getCount() == 1 && !player.getMainHandItem().isEmpty(),
                "a staff does not turn the table into a workbench and is kept");
        player.setItemInHand(InteractionHand.MAIN_HAND, wand.copy());
        test.assertTrue(use(player, table).consumesAction() && level.getBlockEntity(table) instanceof TileArcaneWorkbench, "a wand turns the same table into the workbench");
        var tile = (TileArcaneWorkbench) level.getBlockEntity(table);
        test.assertTrue(!tile.canPlaceItem(TileArcaneWorkbench.WAND_SLOT, staff) && tile.canPlaceItem(TileArcaneWorkbench.WAND_SLOT, wand),
                "the workbench's wand slot refuses a staff and takes a wand");
        test.assertTrue(!tile.canPlaceItemThroughFace(TileArcaneWorkbench.WAND_SLOT, staff, Direction.UP) && tile.canPlaceItemThroughFace(TileArcaneWorkbench.WAND_SLOT, wand, Direction.UP),
                "and so does the automation face");
        var menu = new ContainerArcaneWorkbench(1, player.getInventory(), tile);
        test.assertTrue(!menu.getSlot(1).mayPlace(staff) && menu.getSlot(1).mayPlace(wand), "and the menu's wand slot");
        tile.clearContent();
    }

    private static net.minecraft.world.InteractionResult use(ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(new Vec3(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5), Direction.UP, pos, false);
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    // ---- damage and refill, over real ticks of a real player ---------------------------------------------------------

    private static void weaponAndRefill(GameTestHelper test, ServerLevel level, Connection connection, EmbeddedChannel channel, ServerPlayer player,
                                        BlockState oldState, BlockPos table) {
        level.setBlock(table, oldState, Block.UPDATE_ALL);
        ItemStack staff = staff("silverwood_staff", Map.of());
        ItemStack wand = staff("greatwood", Map.of());
        Mob hitByStaff = victim(level, test, 0);
        Mob hitByWand = victim(level, test, 3);
        // The refilling staves ride in the inventory: a fixed-aspect one (two hundredth tick) and the primal one (fiftieth).
        ItemStack obsidian = staff("obsidian_staff", Map.of());
        ItemStack primal = staff("primal_staff", Map.of("aer", 5000, "terra", 5000, "ignis", 5000, "aqua", 5000, "ordo", 5000));
        ItemStack primalFull = staff("primal_staff", Map.of("aer", 2500, "terra", 2500, "ignis", 2500, "aqua", 2500, "ordo", 2500, "perditio", 2500));
        player.getInventory().setItem(20, obsidian);
        player.getInventory().setItem(21, primal);
        player.getInventory().setItem(22, primalFull);
        player.getInventory().setItem(23, staff("greatwood_staff", Map.of()));
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        player.tickCount = 190;
        long start = test.getTick();
        // A player that no network handler ticks does not tick its inventory or its equipment either; doTick is what the connection calls.
        test.onEachTick(player::doTick);
        test.runAtTickTime(start + 14, () -> {
            try {
                test.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 7.0, "holding a staff the player's attack damage is 1 + 6: " + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
                float before = hitByStaff.getHealth();
                player.attack(hitByStaff);
                test.assertTrue(Math.abs((before - hitByStaff.getHealth()) - 7.0F) < 1.0E-4F, "a staff hits for 7: " + (before - hitByStaff.getHealth()));
                // The refills happened during the ticks 190..204: the two hundredth tick passed once, the fiftieth tick once (at 200).
                test.assertTrue(player.tickCount >= 200 && player.tickCount < 215, "the player passed tick 200: " + player.tickCount);
                test.assertTrue(ItemWandCasting.data(obsidian).vis().equals(Map.of("terra", 100)), "the obsidian staff refilled terra by one vis: " + ItemWandCasting.data(obsidian));
                WandData grown = ItemWandCasting.data(primal);
                test.assertTrue(grown.amount("perditio") == 100 && grown.amount("aer") == 5000 && grown.amount("terra") == 5000,
                        "the primal staff added one vis, and only to the one primal below a tenth (entropy): " + grown);
                test.assertTrue(ItemWandCasting.data(primalFull).vis().values().stream().allMatch(amount -> amount == 2500),
                        "a primal staff with every primal at a tenth is left alone: " + ItemWandCasting.data(primalFull));
                test.assertTrue(ItemWandCasting.data(player.getInventory().getItem(23)).vis().isEmpty(), "a greatwood staff core refills nothing");
                player.setItemInHand(InteractionHand.MAIN_HAND, wand);
                test.runAtTickTime(start + 28, () -> {
                    try {
                        test.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1.0, "holding a wand the attack damage is the bare-hand 1: " + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
                        float wandBefore = hitByWand.getHealth();
                        player.attack(hitByWand);
                        test.assertTrue(Math.abs((wandBefore - hitByWand.getHealth()) - 1.0F) < 1.0E-4F, "a wand hits for 1: " + (wandBefore - hitByWand.getHealth()));
                        test.succeed();
                    } catch (RuntimeException failure) {
                        throw failure;
                    } finally { cleanup(level, connection, channel, player, table, oldState); hitByStaff.discard(); hitByWand.discard(); }
                });
            } catch (RuntimeException failure) {
                cleanup(level, connection, channel, player, table, oldState);
                hitByStaff.discard();
                hitByWand.discard();
                throw failure;
            }
        });
    }

    private static Mob victim(ServerLevel level, GameTestHelper test, int dx) {
        BlockPos at = test.absolutePos(new BlockPos(3 + dx, 2, 5));
        Mob pig = EntityType.PIG.create(level);
        pig.moveTo(Vec3.atBottomCenterOf(at));
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setPersistenceRequired();
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        pig.setHealth(100);
        test.assertTrue(level.addFreshEntity(pig), "a real pig is admitted");
        return pig;
    }

    private static ItemStack staff(String rod, Map<String, Integer> vis) {
        ItemStack stack = new ItemStack(ThaumcraftContent.WAND.get());
        ItemWandCasting.withData(stack, new WandData(rod, "iron", vis));
        return stack;
    }

    private static void cleanup(ServerLevel level, Connection connection, EmbeddedChannel channel, ServerPlayer player, BlockPos table, BlockState oldState) {
        try {
            if (level.getBlockEntity(table) instanceof TileArcaneWorkbench bench) bench.clearContent();
            level.setBlock(table, oldState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
        } finally { player.discard(); channel.finishAndReleaseAll(); }
    }
}
