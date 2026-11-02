package thaumcraft.common.testing;

import dev.architectury.event.events.common.PlayerEvent;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Native registered recipe and menu: a craft callback may close its window after payment. */
public final class ComprehensiveArcaneReviewRuntimeChecks {
    private ComprehensiveArcaneReviewRuntimeChecks() {}

    public static void craftCallbackCloseRetainsPaidOutput(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            AtomicInteger callbacks = new AtomicInteger();
            PlayerEvent.CraftItem listener = (actor, output, ingredients) -> {
                if (actor != f.player) return;
                callbacks.incrementAndGet();
                f.check(output.is(Items.OAK_PLANKS) && output.getCount() == 4,
                        "the native oak-log recipe emits four paid planks");
                actor.closeContainer();
            };
            PlayerEvent.CRAFT_ITEM.register(listener);
            try {
                for (int carried : new int[]{0, 2}) {
                    f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
                    f.check(f.use(f.base).consumesAction(), "native block interaction opens the actual workbench menu");
                    var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
                    var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
                    table.setItem(0, new ItemStack(Items.OAK_LOG));
                    menu.setCarried(carried == 0 ? ItemStack.EMPTY : new ItemStack(Items.OAK_PLANKS, carried));
                    menu.broadcastChanges();
                    int before = f.player.getInventory().countItem(Items.OAK_PLANKS);
                    int eventsBefore = callbacks.get();
                    f.check(menu.craftable() && menu.getSlot(0).getItem().is(Items.OAK_PLANKS),
                            "the loaded vanilla recipe has a real payable preview");
                    menu.clicked(0, 0, ClickType.PICKUP, f.player);
                    f.check(callbacks.get() == eventsBefore + 1 && f.player.containerMenu == f.player.inventoryMenu
                                    && table.getItem(0).isEmpty(),
                            "one native craft event closes the window only after its actual log is consumed");
                    f.check(menu.getCarried().isEmpty()
                                    && f.player.getInventory().countItem(Items.OAK_PLANKS) == before + carried + 4,
                            "closing during the craft hook retains all paid output in accessible player inventory, never the abandoned menu cursor");
                    menu.clicked(0, 0, ClickType.PICKUP, f.player);
                    f.check(callbacks.get() == eventsBefore + 1
                                    && f.player.getInventory().countItem(Items.OAK_PLANKS) == before + carried + 4,
                            "a stale repeated click neither crafts nor duplicates the returned paid output");
                }
            } finally {
                PlayerEvent.CRAFT_ITEM.unregister(listener);
            }
        }
        test.succeed();
    }

    public static void unrepresentableApiRecipeIsRefusedWithoutBlockingValidRecipe(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var invalid = ThaumcraftApi.addShapelessArcaneCraftingRecipe("", new ItemStack(Items.DIAMOND),
                    new AspectList().add(Aspect.AIR, 42_949_673), Items.STICK);
            thaumcraft.api.crafting.ShapelessArcaneRecipe valid = null;
            try {
                f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
                f.check(f.use(f.base).consumesAction(), "the invalid API registration cannot crash opening an empty workbench");
                var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
                var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
                var wand = new ItemStack(ThaumcraftContent.WAND.get());
                wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "gold", java.util.Map.of("aer", 1000)));
                table.setItem(10, wand);
                table.setItem(0, new ItemStack(Items.STICK));
                menu.broadcastChanges();
                f.check(menu.getSlot(0).getItem().isEmpty() && !menu.craftable(),
                        "a positive whole-vis declaration outside the centivis range has no payable preview, without throwing");
                menu.clicked(0, 0, ClickType.PICKUP, f.player);
                f.check(table.getItem(0).is(Items.STICK) && menu.getCarried().isEmpty()
                                && ItemWandCasting.getVis(table.getItem(10), "aer") == 1000,
                        "refusing an overflowing declaration preserves actual ingredients and wand vis");
                valid = ThaumcraftApi.addShapelessArcaneCraftingRecipe("", new ItemStack(Items.EMERALD),
                        new AspectList().add(Aspect.AIR, 1), Items.STICK);
                menu.broadcastChanges();
                f.check(menu.getSlot(0).getItem().is(Items.EMERALD) && menu.craftable() && menu.requiredVis("aer") == 100,
                        "an unrepresentable earlier API recipe cannot hide a later valid recipe");
                menu.clicked(0, 0, ClickType.PICKUP, f.player);
                f.check(menu.getCarried().is(Items.EMERALD) && table.getItem(0).isEmpty()
                                && ItemWandCasting.getVis(table.getItem(10), "aer") == 900,
                        "the valid native result click pays the actual one-vis price exactly once");
            } finally {
                if (valid != null) ThaumcraftApi.unregisterCraftingRecipe(valid);
                ThaumcraftApi.unregisterCraftingRecipe(invalid);
            }
        }
        test.succeed();
    }

    public static void craftCallbacksRetainReplacementCursorAndOverflow(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            boolean[] full = {false};
            PlayerEvent.CraftItem listener = (actor, output, ingredients) -> {
                if (actor != f.player) return;
                if (full[0]) actor.closeContainer();
                else actor.containerMenu.setCarried(actor.getInventory().removeItemNoUpdate(8));
            };
            PlayerEvent.CRAFT_ITEM.register(listener);
            try {
                for (boolean inventoryFull : new boolean[]{false, true}) {
                    f.reset(); full[0] = inventoryFull;
                    f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
                    f.check(f.use(f.base).consumesAction(), "native workbench opens before the declared callback scenario");
                    var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
                    var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
                    table.setItem(0, new ItemStack(Items.OAK_LOG));
                    if (inventoryFull) for (int slot = 0; slot < 36; slot++) f.player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                    else f.player.getInventory().setItem(8, new ItemStack(Items.DIAMOND, 3));
                    menu.broadcastChanges();
                    menu.clicked(0, 0, ClickType.PICKUP, f.player);
                    f.check(table.getItem(0).isEmpty(), "the actual log is paid before either callback");
                    if (inventoryFull) {
                        f.flushFresh();
                        int dropped = f.drops().stream().filter(drop -> drop.getItem().is(Items.OAK_PLANKS))
                                .mapToInt(drop -> drop.getItem().getCount()).sum();
                        f.check(menu.getCarried().isEmpty() && dropped == 4
                                        && f.player.getInventory().countItem(Items.COBBLESTONE) == 36 * 64,
                                "a retired menu with full inventory preserves four paid planks through real durable drop delivery");
                    } else {
                        f.check(menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 3
                                        && f.player.getInventory().countItem(Items.DIAMOND) == 0
                                        && f.player.getInventory().countItem(Items.OAK_PLANKS) == 4,
                                "a craft hook moving three real diamonds onto the current cursor cannot be overwritten by paid planks");
                    }
                }
            } finally {
                PlayerEvent.CRAFT_ITEM.unregister(listener);
            }
        }
        test.succeed();
    }
}
