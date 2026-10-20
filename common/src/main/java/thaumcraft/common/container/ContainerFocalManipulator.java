package thaumcraft.common.container;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.tiles.TileFocalManipulator;

/** Original37-slot ordering; start authority additionally binds UUID session and
 * the server's inventory/job revision, never a stale ID-only vanilla button. */
public final class ContainerFocalManipulator extends AbstractContainerMenu {
    private final TileFocalManipulator serverTile;
    private final Player viewer;
    private final BlockPos pos;
    private final UUID session, lifetime;
    private final ContainerData revisionData;
    private boolean admitting;

    public ContainerFocalManipulator(int id, Inventory inventory, TileFocalManipulator tile) {
        this(id, inventory, tile, UUID.randomUUID());
    }
    public ContainerFocalManipulator(int id, Inventory inventory, TileFocalManipulator tile, UUID session) {
        this(id, inventory, tile, tile.getBlockPos(), tile, session, tile.lifetimeId(), serverData(tile));
    }
    public ContainerFocalManipulator(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, new SimpleContainer(1), data.readBlockPos(), null, data.readUUID(), null, new SimpleContainerData(4));
    }
    private ContainerFocalManipulator(int id, Inventory inventory, Container contents, BlockPos pos,
                                      TileFocalManipulator tile, UUID session, UUID lifetime, ContainerData data) {
        super(ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_MENU.get(), id);
        this.pos = pos.immutable(); this.session = session; this.lifetime = lifetime; serverTile = tile;
        viewer = inventory.player; revisionData = data;
        checkContainerSize(contents, 1); checkContainerDataCount(data, 4);
        addSlot(new SlotLimitedByClass(ItemFocusBasic.class, contents, 0, 88, 60));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 16 + column * 18, 151 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 16 + column * 18, 209));
        addDataSlots(data);
    }
    private static ContainerData serverData(TileFocalManipulator tile) {
        return new ContainerData() {
            @Override public int get(int index) { return (int) (tile.getSelectionRevision() >>> (index * 16)) & 0xffff; }
            @Override public void set(int index, int value) {} // Native S2C data is not server mutation authority.
            @Override public int getCount() { return 4; }
        };
    }
    public UUID sessionId() { return session; }
    public BlockPos tablePos() { return pos; }
    public TileFocalManipulator tile() {
        if (serverTile != null) return serverTile;
        return viewer.level().hasChunkAt(pos) && viewer.level().getBlockEntity(pos) instanceof TileFocalManipulator tile ? tile : null;
    }
    public long selectionRevision() {
        if (serverTile != null) return serverTile.getSelectionRevision();
        long value = 0;
        for (int word = 0; word < 4; word++) value |= (long) (revisionData.get(word) & 0xffff) << (word * 16);
        return value;
    }
    @Override public boolean stillValid(Player player) {
        if (player != viewer || !player.isAlive() || player.isSpectator()) return false;
        if (serverTile != null) return lifetime.equals(serverTile.lifetimeId()) && serverTile.stillValid(player);
        return player.level().hasChunkAt(pos) && player.level().getBlockState(pos).is(ThaumcraftFocalManipulation.FOCAL_MANIPULATOR.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
    private boolean active(Player player) {
        return serverTile != null && player == viewer && player instanceof ServerPlayer sender
                && sender.server.getPlayerList().getPlayer(sender.getUUID()) == sender
                && sender.containerMenu == this && stillValid(sender);
    }

    public boolean requestStart(ServerPlayer sender, UUID expectedSession, long expectedRevision, int upgradeId) {
        if (!sender.server.isSameThread()) throw new IllegalStateException("Focal requests require the server thread");
        if (admitting || !active(sender) || expectedSession == null || !session.equals(expectedSession)
                || expectedRevision < 0 || expectedRevision != selectionRevision() || upgradeId < 0 || upgradeId > Short.MAX_VALUE) {
            if (serverTile != null) serverTile.failStart();
            return false;
        }
        boolean started;
        admitting = true;
        try { started = serverTile.startCraft(upgradeId, sender, expectedRevision); }
        finally { admitting = false; }
        if (!started) serverTile.failStart();
        broadcastChanges();
        return started;
    }
    /** Internal admission witness checked again AFTER overridable focus queries. */
    public boolean authorizesStart(TileFocalManipulator tile, ServerPlayer sender, long expectedRevision) {
        return admitting && tile == serverTile && active(sender) && expectedRevision == tile.getSelectionRevision();
    }
    @Override public boolean clickMenuButton(Player player, int button) { return false; }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (!stillValid(player) || serverTile != null && !active(player)) return;
        super.clicked(index, button, type, player);
        if (serverTile != null) { serverTile.getSelectionRevision(); broadcastChanges(); }
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!active(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack current = slot.getItem(), original = current.copy();
        boolean moved;
        if (index == 0) moved = moveItemStackTo(current, 1, 37, false);
        else if (current.getItem() instanceof ItemFocusBasic) moved = moveItemStackTo(current, 0, 1, false);
        else if (index < 28) moved = moveItemStackTo(current, 28, 37, false);
        else moved = moveItemStackTo(current, 1, 28, false);
        if (!moved || current.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (current.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, current);
        serverTile.getSelectionRevision();
        return original;
    }
}
