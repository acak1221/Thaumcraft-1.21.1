package thaumcraft.common.container;

import java.util.List;
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
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftDeconstruction;
import thaumcraft.common.tiles.TileDeconstructionTable;

/** Reward button carries no aspect or quantity chosen by the client. */
public final class ContainerDeconstructionTable extends AbstractContainerMenu {
    private static final List<Aspect> PRIMALS = List.of(Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY);
    private final TileDeconstructionTable table;
    private final Player viewer;
    private final BlockPos pos;
    private final ContainerData data;
    public ContainerDeconstructionTable(int id, Inventory inventory, TileDeconstructionTable table) { this(id, inventory, table, table.getBlockPos(), table); }
    public ContainerDeconstructionTable(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, new SimpleContainer(1), buffer.readBlockPos(), null); }
    private ContainerDeconstructionTable(int id, Inventory inventory, Container input, BlockPos pos, TileDeconstructionTable table) {
        super(ThaumcraftDeconstruction.MENU.get(), id); this.table = table; viewer = inventory.player; this.pos = pos.immutable();
        data = table == null ? new SimpleContainerData(2) : new ContainerData() {
            @Override public int get(int index) {
                if (index == 0) return table.breakTime();
                if (index != 1) return 0;
                Aspect reward = table.reward();
                return reward == null ? 0 : PRIMALS.indexOf(reward) + 1;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 2; }
        };
        addSlot(new SlotLimitedHasAspects(input, 0, 64, 16, viewer.level()) {
            @Override public boolean mayPlace(ItemStack stack) { return super.mayPlace(stack) && (table == null || table.canPlaceItem(0, stack)); }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        addDataSlots(data);
    }
    public int breakTime() { return data.get(0); }
    public Aspect reward() { int id = data.get(1) - 1; return id >= 0 && id < PRIMALS.size() ? PRIMALS.get(id) : null; }
    public boolean owns(TileDeconstructionTable expected, ServerPlayer player) {
        return table == expected && player == viewer && player.server.isSameThread() && player.containerMenu == this && stillValid(player);
    }
    private boolean active(Player player) { return table != null && player instanceof ServerPlayer server && owns(table, server); }
    @Override public boolean stillValid(Player player) {
        return player == viewer && !player.isSpectator() && (table != null ? table.stillValid(player) : player.level().hasChunkAt(pos)
                && player.level().getBlockState(pos).is(ThaumcraftDeconstruction.TABLE.get()) && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64);
    }
    @Override public boolean clickMenuButton(Player player, int button) { return button == 1 && active(player) && table.takeReward((ServerPlayer) player, this); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) { if (table == null || active(player)) super.clicked(slot, button, type, player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!active(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem(), original = source.copy();
        boolean moved;
        if (index == 0) moved = moveItemStackTo(source, 1, 37, false);
        else if (table.canPlaceItem(0, source)) moved = moveItemStackTo(source, 0, 1, false);
        else moved = index < 28 ? moveItemStackTo(source, 28, 37, false) : moveItemStackTo(source, 1, 28, false);
        if (!moved || source.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, source); return original;
    }
}
