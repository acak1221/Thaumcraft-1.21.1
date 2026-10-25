package thaumcraft.common.testing;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.impl.NetworkAggregator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusTrade;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.network.AccessoryContentsPayload;
import thaumcraft.common.lib.network.AccessoryNetworking;
import thaumcraft.common.lib.world.TradeSwaps;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Native menu, dimension-transition wire output and trade input regressions from the October25 review.
 * Fixtures declare inputs; no physical-client or survival-acquisition acceptance is claimed. */
public final class ReviewRegressionRuntimeChecks {
    private ReviewRegressionRuntimeChecks() {}

    public static void craftingEventInputs(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var manager = f.level.getRecipeManager();
            var originals = new ArrayList<>(manager.getRecipes());
            var callbacks = new AtomicInteger();
            var observed = new ArrayList<ItemStack>();
            PlayerEvent.CraftItem listener = (actor, output, inputs) -> {
                if (actor != f.player) return;
                callbacks.incrementAndGet();
                var physical = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
                f.check(physical.getItem(0).is(Items.BUCKET) && ItemWandCasting.getVis(physical.getItem(10), "aer") == 900,
                        "the callback runs after real input and vis payment, even though it observes the unpaid snapshot");
                for (int slot = 0; slot < inputs.getContainerSize(); slot++) observed.add(inputs.getItem(slot).copy());
                // An addon editing the event snapshot cannot replace the physical remainder.
                inputs.setItem(0, new ItemStack(Items.DIRT));
            };
            PlayerEvent.CRAFT_ITEM.register(listener);
            try {
                manager.replaceRecipes(List.of(new RecipeHolder<>(ResourceLocation.parse("thaumcraft:review_event_input"),
                        new ArcaneShapelessRecipe("", List.of(Ingredient.of(Items.MILK_BUCKET)),
                                new ItemStack(Items.DIAMOND, 2), Map.of("aer", 1)))));
                f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
                f.check(f.use(f.base).consumesAction(), "native interaction opens arcane workbench");
                var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
                var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
                var milk = new ItemStack(Items.MILK_BUCKET);
                milk.set(DataComponents.CUSTOM_NAME, Component.literal("original recipe input"));
                var wand = wand(1000, false);
                table.setItem(0, milk.copy());
                table.setItem(10, wand.copy());
                for (int slot = 0; slot < 36; slot++) f.player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                menu.broadcastChanges();
                f.check(menu.craftable() && menu.requiredVis("aer") == 100, "declared recipe has a payable native preview");
                menu.clicked(0, 0, ClickType.PICKUP, f.player);
                f.check(callbacks.get() == 1 && observed.size() == 11, "one real extraction emits one complete pre-craft event");
                f.check(ItemStack.matches(observed.get(0), milk) && ItemStack.matches(observed.get(10), wand),
                        "event retains original count, custom component and unpaid wand vis");
                f.check(menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 2
                                && table.getItem(0).is(Items.BUCKET) && table.getItem(0).getCount() == 1
                                && ItemWandCasting.getVis(table.getItem(10), "aer") == 900,
                        "physical payment precedes callback, returns the exact bucket and is isolated from snapshot edits");
            } finally {
                PlayerEvent.CRAFT_ITEM.unregister(listener);
                manager.replaceRecipes(originals);
            }
        }
        test.succeed();
    }

    public static void accessoryDimensionSnapshot(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var belt = new ItemStack(ThaumcraftBaubles.MUNDANE_BELT.get());
            belt.set(DataComponents.CUSTOM_NAME, Component.literal("unchanged across dimensions"));
            PlayerAccessories.get(f.player).setItem(3, belt.copy());
            AccessoryNetworking.sync(f.player);
            f.check(accessoryPackets(f).size() == 1, "initial authoritative accessory snapshot reaches native connection");
            AccessoryNetworking.syncChanged(f.player);
            f.check(accessoryPackets(f).isEmpty(), "unchanged inventory normally sends no redundant snapshot");
            var destination = f.level.getServer().getLevel(Level.NETHER);
            f.check(destination != null && destination != f.level, "native test server owns a second real dimension");
            try {
                f.player.teleportTo(destination, f.base.getX() + .5, f.base.getY(), f.base.getZ() + .5, 0, 0);
                var packets = accessoryPackets(f);
                f.check(f.player.level() == destination && f.level.getServer().getPlayerList().getPlayer(f.player.getUUID()) == f.player,
                        "native dimension transition retains the server actor");
                f.check(packets.size() == 1 && ItemStack.matches(packets.getFirst().stacks().get(3), belt)
                                && ItemStack.matches(PlayerAccessories.get(f.player).getItem(3), belt),
                        "dimension event sends a full unchanged inventory for the replacement client player, without item loss");
                AccessoryNetworking.syncChanged(f.player);
                f.check(accessoryPackets(f).isEmpty(), "dimension snapshot also refreshes deduplication state");
            } finally {
                if (f.player.level() != f.level) f.player.teleportTo(f.level, f.base.getX() + .5, f.base.getY(), f.base.getZ() + .5, 0, 0);
            }
            var returned = accessoryPackets(f);
            f.check(returned.size() == 1 && ItemStack.matches(returned.getFirst().stacks().get(3), belt),
                    "returning dimension sends its own complete snapshot");
        }
        test.succeed();
    }

    private static List<AccessoryContentsPayload> accessoryPackets(TubeVariantsRuntimeChecks.Fixture f) {
        f.connection.flushChannel();
        f.channel.runPendingTasks();
        var result = new ArrayList<AccessoryContentsPayload>();
        Object packet;
        while ((packet = f.channel.readOutbound()) != null) {
            try {
                if (!(packet instanceof ClientboundCustomPayloadPacket custom)
                        || !custom.payload().type().id().equals(AccessoryContentsPayload.TYPE.id())) continue;
                if (custom.payload() instanceof AccessoryContentsPayload payload) result.add(payload);
                else if (custom.payload() instanceof NetworkAggregator.BufCustomPacketPayload wrapped) {
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(wrapped.payload()), f.level.registryAccess());
                    try { result.add(AccessoryContentsPayload.STREAM_CODEC.decode(buffer)); }
                    finally { buffer.release(); }
                } else throw new IllegalStateException("Unknown native accessory payload wrapper");
            } finally { ReferenceCountUtil.release(packet); }
        }
        return List.copyOf(result);
    }

    public static void tradeNativeReach(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var wand = wand(1000, true);
            var target = f.base.offset(3, 0, 3);
            f.player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            f.player.getInventory().setItem(1, new ItemStack(Items.GOLD_BLOCK, 4));
            // The entire diagonal ray stays within the fixture's cleared cells.
            // Its entry into the target is sqrt(4^2+4^2), between five and six blocks.
            f.player.setPos(f.base.getX() - 1, f.base.getY() + .5 - f.player.getEyeHeight(), f.base.getZ() - 1);
            f.player.setYRot(-45); f.player.setYHeadRot(-45); f.player.setXRot(0);
            var range = f.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            f.check(range != null, "actual native interaction-range attribute is available");
            range.setBaseValue(6);
            f.put(target, Blocks.GOLD_BLOCK);
            f.check(f.player.pick(6, 1, false) instanceof net.minecraft.world.phys.BlockHitResult hit
                            && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(target)
                            && f.player.pick(5, 1, false).getType() == net.minecraft.world.phys.HitResult.Type.MISS,
                    "declared native ray reaches the target only beyond five blocks, without harness occlusion");
            f.player.setShiftKeyDown(true);
            f.player.gameMode.useItem(f.player, f.level, wand, InteractionHand.MAIN_HAND);
            f.check(TradeSwaps.selection(f.level, wand) != null && TradeSwaps.selection(f.level, wand).is(Blocks.GOLD_BLOCK),
                    "native extended-range sneak use remembers a block beyond the old five-block ray");
            f.player.setShiftKeyDown(false);
            f.put(target, Blocks.STONE);
            f.check(ItemFocusTrade.leftClick(f.player, wand, target), "extended native reach admits left-click anchor");
            TradeSwaps.tick(f.level);
            f.check(f.level.getBlockState(target).is(Blocks.GOLD_BLOCK)
                            && f.player.getInventory().getItem(1).getCount() == 3
                            && ItemWandCasting.getVis(wand, "terra") == 995,
                    "extended reach swap changes one real block and pays once");
            range.setBaseValue(3);
            f.player.setPos(f.base.getX() + .5, f.base.getY() + .5 - f.player.getEyeHeight(), f.base.getZ() + .5);
            f.put(target, Blocks.EMERALD_BLOCK);
            f.player.setShiftKeyDown(true);
            var before = wand.copy();
            f.player.gameMode.useItem(f.player, f.level, wand, InteractionHand.MAIN_HAND);
            f.player.setShiftKeyDown(false);
            f.check(ItemStack.matches(before, wand) && !ItemFocusTrade.leftClick(f.player, wand, target)
                            && f.level.getBlockState(target).is(Blocks.EMERALD_BLOCK),
                    "decreased native reach refuses remember and swap without spending");
            range.setBaseValue(6);
            f.player.setPos(f.base.getX() - 1, f.base.getY() + .5 - f.player.getEyeHeight(), f.base.getZ() - 1);
            f.put(target, Blocks.STONE);
            f.player.gameMode.useItem(f.player, f.level, wand, InteractionHand.MAIN_HAND);
            TradeSwaps.tick(f.level);
            f.check(f.level.getBlockState(target).is(Blocks.GOLD_BLOCK)
                            && f.player.getInventory().getItem(1).getCount() == 2
                            && ItemWandCasting.getVis(wand, "terra") == 990,
                    "native extended-range right-click also reaches and pays its target");
        }
        test.succeed();
    }

    private static ItemStack wand(int each, boolean trade) {
        var result = new ItemStack(ThaumcraftContent.WAND.get());
        result.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "gold",
                Map.of("aer", each, "terra", each, "ignis", each, "aqua", each, "ordo", each, "perditio", each)));
        if (trade) result.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(new ItemStack(ThaumcraftFoci.TRADE_FOCUS.get())));
        return result;
    }
}
