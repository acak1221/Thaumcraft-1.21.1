package thaumcraft.fabric.clienttest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import thaumcraft.common.lib.utils.HexUtils;
import thaumcraft.common.tiles.TileResearchTable;

/** Inputs only: the real screen must combine, solve and copy the note. */
final class ResearchHandsClientSequence {
    private enum Step { SETUP, OPEN, INSERT, INSERTED, COMBINE, COMBINED, PLACE, PLACED, COPY, COPIED, DONE }
    final Map<String, Object> evidence = new LinkedHashMap<>();
    private final BlockPos table = new BlockPos(0, 100, 0);
    private Step step = Step.SETUP;
    private int ticks, placement;
    private CompletableFuture<Void> serverAction;

    boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        if (++ticks > 400) throw new IllegalStateException("Research hands timeout: " + step);
        switch (step) {
            case SETUP -> {
                client.setScreen(null);
                onServer(client, () -> {
                    var player = player(client);
                    var level = player.serverLevel();
                    for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                        level.setBlockAndUpdate(table.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                        for (int y = 0; y <= 3; y++) level.setBlockAndUpdate(table.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                    level.setBlockAndUpdate(table, ThaumcraftContent.TABLE.get().defaultBlockState().setValue(BlockTable.VARIANT, 2));
                    level.setBlockAndUpdate(table.north(), ThaumcraftContent.TABLE.get().defaultBlockState().setValue(BlockTable.VARIANT, 7));
                    require(level.getBlockEntity(table) instanceof TileResearchTable, "physical research table fixture");
                    player.setGameMode(GameType.SURVIVAL);
                    player.setInvulnerable(true);
                    player.teleportTo(.5, 100, 2.5);
                    player.getInventory().clearContent();
                    player.getInventory().setItem(0, new ItemStack(ThaumcraftContent.INKWELL.get()));
                    player.getInventory().setItem(1, ItemResearchNotes.create(new ResearchNoteData("NITOR", 0xFFF563, false, 0,
                            List.of(new Cell(0, 0, 1, "aer"), new Cell(1, 0, 0, null),
                                    new Cell(2, 0, 0, null), new Cell(3, 0, 1, "potentia"))), false));
                    player.getInventory().setItem(2, new ItemStack(Items.PAPER));
                    player.getInventory().setItem(3, new ItemStack(Items.BLACK_DYE));
                    var knowledge = KnowledgeAccess.get(player);
                    knowledge.addResearch("RESEARCHDUPE");
                    knowledge.removeResearch("NITOR");
                    knowledge.removeResearch("RESEARCHER1");
                    knowledge.removeResearch("RESEARCHER2");
                    knowledge.setAspectPool("aer", 10);
                    knowledge.setAspectPool("ignis", 10);
                    knowledge.setAspectPool("lux", 5);
                    knowledge.setAspectPool("potentia", 0);
                    KnowledgeEvents.sync(player);
                    player.inventoryMenu.broadcastChanges();
                });
                evidence.put("boundary", "Fresh world; supplied table, unsolved four-cell puzzle, ink, paper, dye, RESEARCHDUPE and finite pools. No solved note, copied note or result injected. No survival acquisition claim.");
                next(Step.OPEN);
            }
            case OPEN -> {
                if (!serverDone() || ticks < 20 || !client.player.getInventory().getItem(1).is(ThaumcraftContent.RESEARCH_NOTE.get())) return false;
                client.player.getInventory().selected = 8;
                if (ticks < 25) return false;
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(table), Direction.UP, table, false));
                next(Step.INSERT);
            }
            case INSERT -> {
                if (!(client.screen instanceof GuiResearchTable) || !menu(client).editReady() || ticks < 8) return false;
                clickSlot(client, 29, 0, ClickType.QUICK_MOVE);
                clickSlot(client, 30, 0, ClickType.QUICK_MOVE);
                next(Step.INSERTED);
            }
            case INSERTED -> {
                if (ticks < 10 || !menu(client).editReady() || menu(client).getSlot(1).getItem().isEmpty()) return false;
                require(menu(client).getSlot(0).getItem().is(ThaumcraftContent.INKWELL.get()), "client ink slot accepted actual inventory tool");
                require(!ItemResearchNotes.data(menu(client).getSlot(1).getItem()).complete(), "client note slot shows unsolved input");
                pools(client, 10, 10, 5);
                onServer(client, () -> require(((TileResearchTable) player(client).serverLevel().getBlockEntity(table)).getItem(1).is(ThaumcraftContent.RESEARCH_NOTE.get()), "server accepted note from native shift-click"));
                next(Step.COMBINE);
                capture.accept("201-research-hands-inserted.png");
            }
            case COMBINE -> {
                if (!serverDone() || ticks < 8 || !menu(client).editReady()) return false;
                selectAspect(client, "aer", 1);
                selectAspect(client, "ignis", 1);
                click(client, 48, 146, 0);
                pools(client, 10, 10, 5);
                require(!menu(client).editReady(), "combine widget sent pending intent without optimistic pool mutation");
                next(Step.COMBINED);
            }
            case COMBINED -> {
                if (!menu(client).editReady() || ticks < 10) return false;
                pools(client, 9, 9, 6);
                onServer(client, () -> serverPools(client, 9, 9, 6));
                evidence.put("combine", Map.of("aer", 9, "ignis", 9, "lux", 6));
                next(Step.PLACE);
                capture.accept("202-research-hands-combined.png");
            }
            case PLACE -> {
                if (!serverDone() || ticks < 8 || !menu(client).editReady()) return false;
                String tag = placement == 0 ? "lux" : "ignis";
                selectAspect(client, tag, 0);
                var hex = new HexUtils.Hex(placement + 1, 0).toPixel(9);
                int left = (client.screen.width - 255) / 2, top = (client.screen.height - 255) / 2;
                client.screen.mouseDragged(left + 169 + hex.x, top + 83 + hex.y, 0, 1, 1);
                client.screen.mouseReleased(left + 169 + hex.x, top + 83 + hex.y, 0);
                next(Step.PLACED);
            }
            case PLACED -> {
                if (!menu(client).editReady() || ticks < 10 || menu(client).getSlot(0).getItem().getDamageValue() != placement + 1) return false;
                require(ItemResearchNotes.data(menu(client).getSlot(1).getItem()).cellAt(placement + 1, 0).type() == 2, "client hex received placement");
                if (++placement < 2) { next(Step.PLACE); return false; }
                require(menu(client).getSlot(1).getItem().is(ThaumcraftContent.DISCOVERY.get()) && ItemResearchNotes.data(menu(client).getSlot(1).getItem()).complete(), "native GUI solving produced discovery on client");
                pools(client, 9, 8, 5);
                require(menu(client).canCopy(), "real copy button is affordable and available");
                onServer(client, () -> {
                    serverPools(client, 9, 8, 5);
                    require(((TileResearchTable) player(client).serverLevel().getBlockEntity(table)).getItem(1).is(ThaumcraftContent.DISCOVERY.get()), "server solved note, not fixture result");
                });
                next(Step.COPY);
                capture.accept("203-research-hands-solved.png");
            }
            case COPY -> {
                if (!serverDone() || ticks < 8) return false;
                click(client, 48, 16, 0);
                require(!menu(client).editReady(), "native copy widget sent intent");
                pools(client, 9, 8, 5);
                next(Step.COPIED);
            }
            case COPIED -> {
                if (!menu(client).editReady() || ticks < 10) return false;
                var stack = menu(client).getSlot(1).getItem();
                require(stack.is(ThaumcraftContent.DISCOVERY.get()) && stack.getCount() == 2 && ItemResearchNotes.data(stack).copies() == 1, "client sees full copied discovery count/components");
                pools(client, 9, 7, 2);
                require(client.player.getInventory().getItem(2).isEmpty() && client.player.getInventory().getItem(3).isEmpty(), "client sees exact paper/dye consumption");
                require(menu(client).getSlot(0).getItem().getDamageValue() == 2, "copy does not consume scribing ink");
                require(!KnowledgeAccess.get(client.player).isResearchComplete("NITOR"), "client has not learned research just by solving/copying");
                require(!menu(client).canCopy(), "updated copy price is unaffordable after exact finite payment");
                var observedCopy = stack.copy();
                onServer(client, () -> {
                    var player = player(client);
                    var tile = (TileResearchTable) player.serverLevel().getBlockEntity(table);
                    serverPools(client, 9, 7, 2);
                    require(tile.getItem(1).getCount() == 2 && ItemResearchNotes.data(tile.getItem(1)).copies() == 1 && tile.getItem(0).getDamageValue() == 2, "server agrees on copied count, counter and ink");
                    require(ItemStack.matches(tile.getItem(1), observedCopy), "server note components exactly match observed client slot");
                    require(player.getInventory().getItem(2).isEmpty() && player.getInventory().getItem(3).isEmpty() && !KnowledgeAccess.get(player).isResearchComplete("NITOR"), "server paid supplies without learning research");
                });
                evidence.put("copy", Map.of("count", 2, "copies", 1, "ink_damage", 2, "aer", 9, "ignis", 7, "lux", 2));
                next(Step.DONE);
                capture.accept("204-research-hands-copied.png");
            }
            case DONE -> { return serverDone(); }
        }
        return false;
    }

    private void selectAspect(Minecraft client, String tag, int button) throws ReflectiveOperationException {
        var tags = KnowledgeAccess.get(client.player).getAspectsDiscovered().keySet().stream().sorted().toList();
        int index = tags.indexOf(tag);
        require(index >= 0, "aspect visible in actual client palette: " + tag);
        int page = (int) BookClientSmoke.field(client.screen, "page");
        while (index < page * 5) { click(client, 32, 124, 0); page--; }
        while (index >= page * 5 + 25) { click(client, 58, 124, 0); page++; }
        int drawn = index - page * 5;
        click(client, 18 + drawn / 5 * 16, 48 + drawn % 5 * 16, button, button != 0);
    }
    private static void click(Minecraft client, int x, int y, int button) { click(client, x, y, button, true); }
    private static void click(Minecraft client, int x, int y, int button, boolean release) {
        int left = (client.screen.width - 255) / 2, top = (client.screen.height - 255) / 2;
        require(client.screen.mouseClicked(left + x, top + y, button), "real screen handled widget");
        if (release) client.screen.mouseReleased(left + x, top + y, button);
    }
    private static void clickSlot(Minecraft client, int slot, int button, ClickType type) {
        client.gameMode.handleInventoryMouseClick(menu(client).containerId, slot, button, type, client.player);
    }
    private static ContainerResearchTable menu(Minecraft client) { return (ContainerResearchTable) client.player.containerMenu; }
    private static ServerPlayer player(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private void onServer(Minecraft client, Runnable action) { serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null || !serverAction.isDone()) return false; serverAction.join(); return true; }
    private static void pools(Minecraft client, int air, int fire, int light) { checkPools(KnowledgeAccess.get(client.player), air, fire, light); }
    private static void serverPools(Minecraft client, int air, int fire, int light) { checkPools(KnowledgeAccess.get(player(client)), air, fire, light); }
    private static void checkPools(thaumcraft.common.lib.research.PlayerKnowledge knowledge, int air, int fire, int light) {
        require(knowledge.getAspectPoolFor("aer") == air && knowledge.getAspectPoolFor("ignis") == fire && knowledge.getAspectPoolFor("lux") == light,
                "observed exact pools " + air + "/" + fire + "/" + light + "; actual=" + knowledge.getAspectsDiscovered());
    }
    private void next(Step next) { step = next; ticks = 0; }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
