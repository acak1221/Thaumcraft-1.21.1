package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import thaumcraft.api.crafting.transfer.RecipeTransferResult;
import thaumcraft.api.crafting.transfer.RecipeTransferTarget;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.transfer.RecipeTransferRecipes;
import thaumcraft.common.crafting.transfer.RecipeTransfers;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** The book and optional viewers share one server admission and ingredient-transfer path. */
public final class RecipeTransferNetworking {
    private static boolean initialized;
    private static RequestSender clientSender = (id, maximum, book) -> {};
    @FunctionalInterface public interface RequestSender {
        void send(ResourceLocation recipeId, boolean maximum, boolean fromBook);
    }
    public static void initClient(RequestSender sender) { clientSender = java.util.Objects.requireNonNull(sender); }
    private RecipeTransferNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RecipeTransferPayload.TYPE,
                RecipeTransferPayload.STREAM_CODEC, (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        RecipeTransferResult result = handle(player, payload);
                        player.displayClientMessage(Component.translatable(result.messageKey()), true);
                    }
                }));
    }

    public static void request(ResourceLocation recipeId, boolean maximum) { request(recipeId, maximum, false); }
    public static void request(ResourceLocation recipeId, boolean maximum, boolean fromBook) {
        clientSender.send(recipeId, maximum, fromBook);
    }

    public static RecipeTransferResult handle(ServerPlayer player, RecipeTransferPayload payload) {
        if (player == null || payload == null || !player.server.isSameThread() || player.connection == null
                || player.hasDisconnected() || player.server.getPlayerList().getPlayer(player.getUUID()) != player
                || !player.isAlive() || player.isSpectator()) return RecipeTransferResult.STALE_MENU;
        AbstractContainerMenu before = player.containerMenu;
        if (before.containerId != payload.containerId()) return RecipeTransferResult.STALE_MENU;
        try {
            if (RecipeTransferRecipes.holder(player.level(), payload.recipeId()) == null) return RecipeTransferResult.UNKNOWN_RECIPE;
            if (!RecipeTransferRecipes.supported(player.level(), payload.recipeId())) return RecipeTransferResult.UNSUPPORTED;
        } catch (RuntimeException invalidRecipe) { return RecipeTransferResult.UNSUPPORTED; }
        boolean opened = false;
        if (!(before instanceof RecipeTransferTarget)) {
            if (!payload.fromBook() || before != player.inventoryMenu) return RecipeTransferResult.NO_TARGET;
            TileArcaneWorkbench table = nearestWorkbench(player);
            if (table == null) return RecipeTransferResult.NO_TARGET;
            open(player, table);
            opened = true;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof RecipeTransferTarget target) || !target.canTransferRecipe(player)) return RecipeTransferResult.NO_TARGET;
        RecipeTransferResult result = RecipeTransfers.transfer(player, target, payload.recipeId(), payload.maximum());
        try { if (player.containerMenu == menu && menu.stillValid(player)) {
            menu.sendAllDataToRemote();
            // A standalone book needs to return to the workbench; EMI already owns its underlying screen.
            if (result == RecipeTransferResult.SUCCESS && payload.fromBook() && !opened
                    && menu instanceof ContainerArcaneWorkbench workbench && workbench.tile() != null)
                open(player, workbench.tile());
        } } catch (RuntimeException notificationFailure) {
            thaumcraft.common.Thaumcraft.LOG.warn("Recipe transfer {} completed before menu notification failed", result, notificationFailure);
        }
        return result;
    }

    private static void open(ServerPlayer player, TileArcaneWorkbench table) {
        MenuRegistry.openExtendedMenu(player, table, buffer -> buffer.writeBlockPos(table.getBlockPos()));
    }

    private static TileArcaneWorkbench nearestWorkbench(ServerPlayer player) {
        var level = player.serverLevel();
        int radius = Math.min(8, Math.max(1, (int) Math.ceil(player.blockInteractionRange()) + 1));
        BlockPos origin = player.blockPosition();
        TileArcaneWorkbench best = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !player.canInteractWithBlock(pos, 0) || !level.mayInteract(player, pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof TileArcaneWorkbench table) || !table.stillValid(player)) continue;
            double candidate = player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
            if (candidate < distance) { best = table; distance = candidate; }
        }
        return best;
    }
}
