package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.lib.research.ResearchCatalog;

public final class ModernUtilityRuntimeChecks {
    private ModernUtilityRuntimeChecks() {}
    private record Cut(String source, String result, int count) {}
    private static final List<Cut> CUTS = List.of(
            new Cut("arcane_stone", "arcane_stone_bricks", 1),
            new Cut("arcane_stone", "arcane_stone_brick_stairs", 1),
            new Cut("arcane_stone", "arcane_stone_brick_slab", 2),
            new Cut("arcane_stone_bricks", "arcane_stone_brick_stairs", 1),
            new Cut("arcane_stone_bricks", "arcane_stone_brick_slab", 2));

    public static void stonecutterUsesPaidNativeMenu(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2)).atY(160);
        var before = level.getBlockState(pos);
        test.assertTrue(level.getBlockEntity(pos) == null, "owned stonecutter fixture does not replace a foreign block entity");
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        ServerPlayer actor = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "modern-cutter"), ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, actor,
                CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        try {
            actor.setGameMode(GameType.SURVIVAL);
            actor.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            level.setBlock(pos, Blocks.STONECUTTER.defaultBlockState(), 3);
            var menu = new StonecutterMenu(17, actor.getInventory(), ContainerLevelAccess.create(level, pos));
            actor.containerMenu = menu;
            for (Cut cut : CUTS) {
                var input = BuiltInRegistries.ITEM.get(id(cut.source));
                var output = BuiltInRegistries.ITEM.get(id(cut.result));
                menu.setCarried(ItemStack.EMPTY);
                menu.getSlot(0).set(new ItemStack(input, 2));
                int selected = -1;
                ResourceLocation recipeId = id("stonecutting/" + cut.result + "_from_" + cut.source);
                for (int index = 0; index < menu.getRecipes().size(); index++)
                    if (menu.getRecipes().get(index).id().equals(recipeId)) selected = index;
                test.assertTrue(selected >= 0 && menu.clickMenuButton(actor, selected), "loaded native stonecutter offers " + recipeId);
                test.assertTrue(menu.getSlot(1).getItem().is(output) && menu.getSlot(1).getItem().getCount() == cut.count,
                        "native preview retains the building output count");
                menu.clicked(1, 0, ClickType.PICKUP, actor);
                test.assertTrue(menu.getSlot(0).getItem().getCount() == 1 && menu.getCarried().is(output)
                                && menu.getCarried().getCount() == cut.count,
                        "taking the result pays exactly one arcane building block");
                menu.clicked(1, 0, ClickType.PICKUP, actor);
                test.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getCarried().getCount() == 2 * cut.count,
                        "the second result consumes the remaining actual input");
                menu.clicked(1, 0, ClickType.PICKUP, actor);
                test.assertTrue(menu.getSlot(1).getItem().isEmpty() && menu.getCarried().getCount() == 2 * cut.count,
                        "a repeated result click cannot mint an unpaid building block");
            }
            menu.setCarried(ItemStack.EMPTY);
            menu.getSlot(0).set(new ItemStack(Items.DIRT));
            test.assertTrue(menu.getRecipes().isEmpty() && menu.getSlot(1).getItem().isEmpty(),
                    "ordinary dirt cannot be converted into magical building materials");
            actor.closeContainer();
        } finally {
            level.getServer().getPlayerList().remove(actor);
            actor.discard();
            channel.finishAndReleaseAll();
            level.setBlock(pos, before, 3);
        }
        test.succeed();
    }

    public static void modernResearchKeepsOriginalCorpus(GameTestHelper test) {
        test.assertTrue(ResearchCatalog.bundled().size() == 201, "the exact original research corpus is retained");
        var ordinary = ResearchCatalog.get("MODERNINFUSION");
        var deep = ResearchCatalog.get("DEEPINFUSION");
        test.assertTrue(ordinary != null && deep != null
                        && ordinary.parents().contains("INFUSIONENCHANTMENT")
                        && deep.parents().contains("MODERNINFUSION") && deep.parentsHidden().contains("ELDRITCHMINOR"),
                "modern and treasure enchantments have real visible and hidden research prerequisites");
        for (String key : List.of("MODERNINFUSION", "DEEPINFUSION")) {
            var research = ResearchCategories.getResearch(key);
            int resolved = 0;
            for (var page : research.getPages()) if (page.recipeIds != null) for (var recipe : page.recipeIds) {
                test.assertTrue(test.getLevel().getRecipeManager().byKey(recipe).isPresent(), "the book page resolves an actual loaded recipe: " + recipe);
                resolved++;
            }
            test.assertTrue(resolved == (key.equals("MODERNINFUSION") ? 9 : 3), "all modern recipes have registered book pages");
        }
        test.succeed();
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
}
