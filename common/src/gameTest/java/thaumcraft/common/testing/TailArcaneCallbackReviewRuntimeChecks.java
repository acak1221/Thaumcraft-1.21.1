package thaumcraft.common.testing;

import dev.architectury.event.events.common.PlayerEvent;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
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

/** Paid output must survive an observable addon exception, including its live component changes. */
public final class TailArcaneCallbackReviewRuntimeChecks {
    private TailArcaneCallbackReviewRuntimeChecks() {}

    public static void paidCraftHookFailuresPreserveLiveOutputAndRethrow(GameTestHelper test) {
        var registration = ThaumcraftApi.addShapelessArcaneCraftingRecipe("", new ItemStack(Items.EMERALD, 4),
                new AspectList().add(Aspect.AIR, 1), Items.STICK);
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var missing = new ArrayList<String>();
            for (boolean closes : new boolean[]{false, true}) {
                for (boolean error : new boolean[]{false, true}) {
                    f.reset();
                    f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
                    f.check(f.use(f.base).consumesAction(), "actual connected survival actor opens the registered workbench");
                    var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
                    var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
                    var wand = new ItemStack(ThaumcraftContent.WAND.get());
                    wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "gold", Map.of("aer", 1000)));
                    table.setItem(10, wand);
                    table.setItem(0, new ItemStack(Items.STICK));
                    // Error cases exercise paid output custody when no native inventory slot can accept it.
                    if (error) for (int slot = 0; slot < 36; slot++)
                        f.player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                    menu.broadcastChanges();
                    f.check(menu.craftable() && menu.requiredVis("aer") == 100 && menu.getSlot(0).getItem().getCount() == 4,
                            "real public registration has exactly one funded one-vis craft before the callback");
                    String scenario = (error ? "Error" : "RuntimeException") + (closes ? "/closed" : "/open");
                    Component marker = Component.literal("paid-live-output-" + scenario);
                    Throwable failure = error ? new AssertionError(scenario) : new IllegalStateException(scenario);
                    var callbacks = new AtomicInteger();
                    PlayerEvent.CraftItem listener = (actor, output, inputs) -> {
                        if (actor != f.player) return;
                        callbacks.incrementAndGet();
                        f.check(table.getItem(0).isEmpty() && ItemWandCasting.getVis(table.getItem(10), "aer") == 900
                                        && inputs.getItem(0).is(Items.STICK) && inputs.getItem(0).getCount() == 1,
                                "callback observes paid physical input/vis and its independent pre-payment input snapshot");
                        output.set(DataComponents.CUSTOM_NAME, marker);
                        if (closes) actor.closeContainer();
                        if (failure instanceof RuntimeException runtime) throw runtime;
                        throw (Error) failure;
                    };
                    PlayerEvent.CRAFT_ITEM.register(listener);
                    try {
                        Throwable observed = null;
                        try { menu.clicked(0, 0, ClickType.PICKUP, f.player); }
                        catch (RuntimeException | Error thrown) { observed = thrown; }
                        f.check(observed == failure && callbacks.get() == 1,
                                "the exact original callback exception propagates once: " + scenario);
                        f.check(table.getItem(0).isEmpty() && ItemWandCasting.getVis(table.getItem(10), "aer") == 900,
                                "failed post-payment callback does not refund or double-debit the physical recipe: " + scenario);
                        f.flushFresh();
                        var accessible = new ArrayList<ItemStack>();
                        for (int slot = 0; slot < f.player.getInventory().getContainerSize(); slot++)
                            accessible.add(f.player.getInventory().getItem(slot));
                        accessible.add(f.player.containerMenu.getCarried());
                        f.drops().forEach(drop -> accessible.add(drop.getItem()));
                        int all = accessible.stream().filter(stack -> stack.is(Items.EMERALD)).mapToInt(ItemStack::getCount).sum();
                        int live = accessible.stream().filter(stack -> stack.is(Items.EMERALD)
                                && marker.equals(stack.get(DataComponents.CUSTOM_NAME))).mapToInt(ItemStack::getCount).sum();
                        if (all != 4 || live != 4) missing.add(scenario + " accessible=" + all + " live-components=" + live);
                        f.check(!error || f.player.getInventory().countItem(Items.COBBLESTONE) == 36 * 64,
                                "durable fallback cannot overwrite the pre-existing full inventory");
                        menu.clicked(0, 0, ClickType.PICKUP, f.player);
                        f.flushFresh();
                        f.check(callbacks.get() == 1 && table.getItem(0).isEmpty()
                                        && ItemWandCasting.getVis(table.getItem(10), "aer") == 900,
                                "stale retry after the thrown callback cannot craft or pay again: " + scenario);
                        int after = f.player.getInventory().countItem(Items.EMERALD)
                                + (f.player.containerMenu.getCarried().is(Items.EMERALD) ? f.player.containerMenu.getCarried().getCount() : 0)
                                + f.drops().stream().filter(drop -> drop.getItem().is(Items.EMERALD)).mapToInt(drop -> drop.getItem().getCount()).sum();
                        if (after != all) missing.add(scenario + " retry changed stock " + all + "->" + after);
                    } finally { PlayerEvent.CRAFT_ITEM.unregister(listener); }
                }
            }
            f.check(missing.isEmpty(), "every paid craft retains exactly four accessible emeralds with the callback's live name: " + missing);
        } finally { ThaumcraftApi.unregisterCraftingRecipe(registration); }
        test.succeed();
    }
}
