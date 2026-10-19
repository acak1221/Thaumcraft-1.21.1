package thaumcraft.fabric.clienttest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileResearchTable;

/** Actual GUI/packet actions in the owned smoke world; supplied materials/node are NOT a survival claim. */
final class EarlyContentClientSequence {
    private enum Step {
        COMBINE_SETUP, COMBINE_OPEN, COMBINE_SELECT, COMBINE_SEND, COMBINE_WAIT,
        WORKSHOP_SETUP, WORKSHOP_CONVERT, WORKSHOP_FILL, WORKSHOP_OPEN, UNFUNDED,
        TAKE_WAND, WAIT_WAND, START_DRAIN, DRAIN, WORLD, INSERT_WAND, FUNDED, CRAFT, CRAFTED, BOOK, DONE
    }

    final Map<String, Object> evidence = new LinkedHashMap<>();
    private final BlockPos research, workbench, node;
    private Step step = Step.COMBINE_SETUP;
    private int ticks;
    private CompletableFuture<Void> serverAction;
    private Map<String, Integer> charged;

    EarlyContentClientSequence(BlockPos research) {
        this.research = research;
        workbench = research.south(2);
        node = research.offset(1, 2, 1);
        evidence.put("fixture_boundary", "Supplied primal points, materials and six-primal normal node; not natural survival progression");
    }

    boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ticks++;
        if (ticks > 600) throw new IllegalStateException("Early-content client step timed out: " + step);
        switch (step) {
            case COMBINE_SETUP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var knowledge = KnowledgeAccess.get(player);
                    // Remove only this fixture discovery on repeated owned runs.
                    var saved = knowledge.save();
                    var aspects = saved.getList("discoveredAspects", Tag.TAG_COMPOUND);
                    for (int i = aspects.size() - 1; i >= 0; i--) if (aspects.getCompound(i).getString("key").equals("tempestas")) aspects.remove(i);
                    knowledge.load(saved);
                    knowledge.setAspectPool("aer", 1);
                    knowledge.setAspectPool("aqua", 0);
                    ((TileResearchTable) player.serverLevel().getBlockEntity(research)).setBonusPools(Map.of("aqua", 1));
                    KnowledgeEvents.sync(player);
                });
                next(Step.COMBINE_OPEN);
            }
            case COMBINE_OPEN -> {
                if (!serverDone() || ticks < 15) return false;
                useBlock(client, research);
                next(Step.COMBINE_SELECT);
            }
            case COMBINE_SELECT -> {
                if (!(client.screen instanceof GuiResearchTable) || ticks < 10) return false;
                var menu = (ContainerResearchTable) client.player.containerMenu;
                if (!menu.editReady() || menu.bonusFor("aqua") != 1 || KnowledgeAccess.get(client.player).hasDiscoveredAspect("tempestas")) return false;
                chooseAspect(client, "aer");
                chooseAspect(client, "aqua");
                next(Step.COMBINE_SEND);
                capture.accept("21-combine-primal-and-table-bonus-en.png");
            }
            case COMBINE_SEND -> {
                var before = KnowledgeAccess.get(client.player).save();
                int x = (client.screen.width - 255) / 2 + 51, y = (client.screen.height - 255) / 2 + 147;
                client.screen.mouseClicked(x, y, 0);
                client.screen.mouseReleased(x, y, 0);
                require(before.equals(KnowledgeAccess.get(client.player).save()), "combination must not optimistically discover/debit on the client");
                next(Step.COMBINE_WAIT);
            }
            case COMBINE_WAIT -> {
                var menu = (ContainerResearchTable) client.player.containerMenu;
                var knowledge = KnowledgeAccess.get(client.player);
                if (!menu.editReady() || knowledge.getAspectPoolFor("tempestas") != 3 || menu.bonusFor("aqua") != 0) return false;
                require(knowledge.getAspectPoolFor("aer") == 0 && knowledge.getAspectPoolFor("aqua") == 0 && menu.getSlot(0).getItem().getDamageValue() == 4,
                        "GUI combination spends RP1+bonus1, awards discovery3 and leaves ink unchanged");
                evidence.put("gui_combine", Map.of("left", "aer", "right", "aqua", "output", "tempestas", "new_rp", 3, "aqua_bonus_after", 0, "ink_damage", 4));
                next(Step.WORKSHOP_SETUP);
                capture.accept("22-combined-new-compound-en.png");
            }
            case WORKSHOP_SETUP -> {
                client.player.closeContainer();
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var level = player.serverLevel();
                    level.setBlockAndUpdate(workbench, ThaumcraftContent.TABLE.get().state(0));
                    level.setBlockAndUpdate(node, ThaumcraftContent.AURA_NODE.get().defaultBlockState());
                    var aspects = new AspectList();
                    for (String tag : WandData.PRIMALS) aspects.add(Aspect.getAspect(tag), 10);
                    ((TileNode) level.getBlockEntity(node)).setAspects(aspects);
                    int index = 0;
                    for (var ore : ThaumcraftOres.INFUSED.values()) level.setBlockAndUpdate(research.offset(-2, index / 3, index++ % 3), ore.get().defaultBlockState());
                    level.setBlockAndUpdate(research.offset(-2, 2, 0), ThaumcraftOres.CINNABAR.get().defaultBlockState());
                    level.setBlockAndUpdate(research.offset(-2, 2, 1), ThaumcraftOres.AMBER.get().defaultBlockState());
                    for (int x = -3; x <= 5; x++) for (int z = -2; z <= 5; z++) level.setBlockAndUpdate(research.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                    player.getInventory().clearContent();
                    player.getInventory().setItem(0, new ItemStack(ThaumcraftContent.WAND.get()));
                    player.getInventory().selected = 0;
                    KnowledgeAccess.get(player).addResearch("ENCHFABRIC");
                    player.inventoryMenu.broadcastChanges();
                    KnowledgeEvents.sync(player);
                });
                next(Step.WORKSHOP_CONVERT);
            }
            case WORKSHOP_CONVERT -> {
                if (!serverDone() || ticks < 15 || !client.player.getInventory().getItem(0).is(ThaumcraftContent.WAND.get())) return false;
                client.player.getInventory().selected = 0;
                useBlock(client, workbench);
                next(Step.WORKSHOP_FILL);
            }
            case WORKSHOP_FILL -> {
                if (!(client.level.getBlockEntity(workbench) instanceof TileArcaneWorkbench) || !client.player.getMainHandItem().isEmpty() || ticks < 10) return false;
                onServer(client, () -> {
                    var tile = (TileArcaneWorkbench) serverPlayer(client).serverLevel().getBlockEntity(workbench);
                    for (int slot : new int[] {1, 3, 5, 7}) tile.setItem(slot, new ItemStack(Items.STRING));
                    tile.setItem(4, new ItemStack(Items.BLUE_WOOL));
                });
                evidence.put("wand_conversion", "Actual useItemOn converted base table and transferred empty wand");
                next(Step.WORKSHOP_OPEN);
            }
            case WORKSHOP_OPEN -> {
                if (!serverDone() || ticks < 10) return false;
                useBlock(client, workbench);
                next(Step.UNFUNDED);
            }
            case UNFUNDED -> {
                if (!(client.screen instanceof GuiArcaneWorkbench) || ticks < 15) return false;
                var menu = arcane(client);
                if (!menu.getSlot(0).getItem().is(ThaumcraftContent.MATERIALS.get("enchanted_fabric").get())) return false;
                require(!menu.craftable() && menu.requiredVis("aer") == 110, "empty iron wand exposes actual unaffordable recipe at 110 centivis per primal");
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.PICKUP, client.player);
                next(Step.TAKE_WAND);
                capture.accept("23-arcane-workbench-insufficient-vis-en.png");
            }
            case TAKE_WAND -> {
                if (ticks < 10) return false;
                var menu = arcane(client);
                require(menu.getCarried().isEmpty() && menu.getSlot(6).getItem().is(Items.BLUE_WOOL), "unfunded native result click refuses without consuming the matrix");
                client.gameMode.handleInventoryMouseClick(menu.containerId, 1, 0, ClickType.SWAP, client.player);
                next(Step.WAIT_WAND);
            }
            case WAIT_WAND -> {
                if (!arcane(client).getSlot(1).getItem().isEmpty() || !client.player.getInventory().getItem(0).is(ThaumcraftContent.WAND.get()) || ticks < 10) return false;
                client.player.closeContainer();
                client.player.getInventory().selected = 0;
                lookAt(client, Vec3.atCenterOf(node));
                next(Step.START_DRAIN);
            }
            case START_DRAIN -> {
                lookAt(client, Vec3.atCenterOf(node));
                if (ticks < 10) return false;
                client.options.keyUse.setDown(true);
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                next(Step.DRAIN);
            }
            case DRAIN -> {
                lookAt(client, Vec3.atCenterOf(node));
                if (WandData.PRIMALS.stream().anyMatch(tag -> ItemWandCasting.getVis(client.player.getMainHandItem(), tag) < 200)) return false;
                client.options.keyUse.setDown(false);
                client.gameMode.releaseUsingItem(client.player);
                next(Step.WORLD);
            }
            case WORLD -> {
                if (ticks < 15) return false;
                charged = new LinkedHashMap<>(ItemWandCasting.data(client.player.getMainHandItem()).vis());
                evidence.put("native_node_drain_centivis", charged);
                evidence.put("node_is_supplied_render_and_drain_fixture", true);
                lookAt(client, Vec3.atCenterOf(node).add(0, -.5, 0));
                next(Step.INSERT_WAND);
                capture.accept("24-ores-node-and-arcane-workbench-world-en.png");
            }
            case INSERT_WAND -> {
                if (client.screen == null) { useBlock(client, workbench); ticks = 0; return false; }
                if (!(client.screen instanceof GuiArcaneWorkbench) || ticks < 10) return false;
                var menu = arcane(client);
                client.gameMode.handleInventoryMouseClick(menu.containerId, 38, 0, ClickType.QUICK_MOVE, client.player);
                next(Step.FUNDED);
            }
            case FUNDED -> {
                var menu = arcane(client);
                if (!menu.craftable() || !menu.getSlot(1).getItem().is(ThaumcraftContent.WAND.get()) || ticks < 15) return false;
                for (String tag : WandData.PRIMALS) require(menu.requiredVis(tag) == 110 && ItemWandCasting.getVis(menu.getSlot(1).getItem(), tag) == charged.get(tag), "server-priced native recipe uses the wand actually charged through item-use ticks");
                next(Step.CRAFT);
                capture.accept("25-arcane-workbench-charged-recipe-en.png");
            }
            case CRAFT -> {
                var menu = arcane(client);
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.PICKUP, client.player);
                require(menu.getCarried().isEmpty(), "result is not optimistically granted on the client");
                next(Step.CRAFTED);
            }
            case CRAFTED -> {
                var menu = arcane(client);
                if (!menu.getCarried().is(ThaumcraftContent.MATERIALS.get("enchanted_fabric").get()) || ticks < 10) return false;
                require(menu.getCarried().getCount() == 1, "actual result count is one enchanted fabric");
                for (String tag : WandData.PRIMALS) require(ItemWandCasting.getVis(menu.getSlot(1).getItem(), tag) == charged.get(tag) - 110, "craft debits exactly 110 centivis from every primal");
                for (int slot = 2; slot < 11; slot++) require(menu.getSlot(slot).getItem().isEmpty(), "all five real ingredients consumed once");
                evidence.put("gui_arcane_craft", Map.of("recipe", "thaumcraft:arcane/enchanted_fabric", "output_count", 1, "each_primal_debit", 110, "unfunded_refused", true));
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var tile = (TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench);
                    require(player.containerMenu.getCarried().is(ThaumcraftContent.MATERIALS.get("enchanted_fabric").get()), "authoritative cursor contains crafted output");
                    for (String tag : WandData.PRIMALS) require(ItemWandCasting.getVis(tile.getItem(10), tag) == charged.get(tag) - 110, "authoritative wand debit matches client");
                });
                client.player.closeContainer();
                next(Step.BOOK);
            }
            case BOOK -> {
                if (!serverDone() || ticks < 15) return false;
                client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("ENCHFABRIC")));
                next(Step.DONE);
            }
            case DONE -> {
                if (ticks == 10) {
                    var colors = (ItemColors) BookClientSmoke.field(client, "itemColors");
                    for (String tag : WandData.PRIMALS) {
                        var shard = new ItemStack(ThaumcraftContent.SHARDS.get(tag).get());
                        require((colors.getColor(shard, 0) >>> 24) == 255, "native shard tint must be opaque ARGB, not invisible legacy RGB");
                    }
                    evidence.put("native_shard_item_alpha", 255);
                    int page = (int) BookClientSmoke.field(client.screen, "page");
                    var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
                    boolean visible = false;
                    for (int i = page; i < Math.min(page + 2, pages.size()); i++) visible |= BookClientSmoke.field(pages.get(i), "recipe") != null;
                    if (!visible) {
                        var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                                .filter(widget -> widget.getMessage().getString().equals("→") && widget.active).findFirst().orElseThrow();
                        client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0);
                        client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
                        ticks = 0;
                        return false;
                    }
                    evidence.put("native_arcane_recipe_page_visible", true);
                    capture.accept("26-arcane-recipe-book-en.png");
                }
                return ticks > 10;
            }
        }
        return false;
    }

    private void next(Step next) { step = next; ticks = 0; }
    private boolean serverDone() { if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void onServer(Minecraft client, Runnable action) { serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private static net.minecraft.server.level.ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void useBlock(Minecraft client, BlockPos pos) {
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
    }
    private static void lookAt(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        client.player.setYRot(yaw); client.player.yRotO = yaw;
        client.player.setXRot(pitch); client.player.xRotO = pitch;
    }
    private static void chooseAspect(Minecraft client, String tag) {
        int left = (client.screen.width - 255) / 2, top = (client.screen.height - 255) / 2;
        var tags = KnowledgeAccess.get(client.player).getAspectsDiscovered().keySet().stream().sorted().toList();
        int index = tags.indexOf(tag);
        require(index >= 0, "palette contains " + tag);
        for (int i = 0; i < 10; i++) client.screen.mouseClicked(left + 39, top + 125, 0);
        while (index >= 25) { client.screen.mouseClicked(left + 63, top + 125, 0); index -= 5; }
        int x = left + 18 + index / 5 * 16, y = top + 48 + index % 5 * 16;
        client.screen.mouseClicked(x, y, 1);
        client.screen.mouseReleased(x, y, 1);
    }
}
