package thaumcraft.common.container;

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
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.tiles.TileAlchemyFurnace;

/** Original 38-slot layout/quick-move precedence; both furnace slots are REAL inputs. */
public final class ContainerAlchemyFurnace extends AbstractContainerMenu {
    private static final int COOK = 0, BURN = 1, FUEL_TIME = 2, VIS = 3, SMELT = 4, BOOST = 5, BELLOWS = 6;
    private static final int DATA_COUNT = 14;
    private final TileAlchemyFurnace tile;
    private final Player viewer;
    private final BlockPos furnacePos;
    private final ContainerData data;

    public ContainerAlchemyFurnace(int id, Inventory inventory, TileAlchemyFurnace tile) {
        this(id, inventory, tile, tile.getBlockPos(), tile, serverData(tile));
    }

    public ContainerAlchemyFurnace(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, new SimpleContainer(2), data.readBlockPos(), null, new SimpleContainerData(DATA_COUNT));
    }

    private ContainerAlchemyFurnace(int id, Inventory inventory, Container contents, BlockPos pos, TileAlchemyFurnace tile, ContainerData data) {
        super(ThaumcraftDistillation.ALCHEMY_FURNACE_MENU.get(), id);
        this.tile = tile;
        this.data = data;
        viewer = inventory.player;
        furnacePos = pos.immutable();
        checkContainerSize(contents, 2);
        checkContainerDataCount(data, DATA_COUNT);
        addSlot(new SlotLimitedHasAspects(contents, TileAlchemyFurnace.INPUT_SLOT, 80, 8, viewer.level()));
        // Source ContainerAlchemyFurnace:44 uses an unrestricted manual fuel
        // slot. Automation/shift-click still require real native furnace fuel.
        addSlot(new Slot(contents, TileAlchemyFurnace.FUEL_SLOT, 80, 48));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        addDataSlots(data);
    }

    private static ContainerData serverData(TileAlchemyFurnace tile) {
        return new ContainerData() {
            @Override public int get(int index) {
                int value = switch (index / 2) {
                    case COOK -> tile.getCookTime();
                    case BURN -> tile.getBurnTime();
                    case FUEL_TIME -> tile.getCurrentItemBurnTime();
                    case VIS -> tile.getVis();
                    case SMELT -> tile.getSmeltTime();
                    case BOOST -> tile.isSpeedBoost() ? 1 : 0;
                    case BELLOWS -> tile.getBellowsCount();
                    default -> 0;
                };
                // Vanilla menu packets carry signed SHORTS. Two unsigned halves
                // preserve each live int; persistence's original shorts are a
                // separate, deliberate TC4 format choice.
                return (index & 1) == 0 ? value & 0xffff : value >>> 16;
            }
            @Override public void set(int index, int value) {} // Client packets cannot write server state.
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    private int value(int field) { return (data.get(field * 2) & 0xffff) | ((data.get(field * 2 + 1) & 0xffff) << 16); }
    public BlockPos furnacePos() { return furnacePos; }
    public TileAlchemyFurnace tile() { return tile; }
    public int getCookTime() { return value(COOK); }
    public int getBurnTime() { return value(BURN); }
    public int getCurrentItemBurnTime() { return value(FUEL_TIME); }
    public int getVis() { return value(VIS); }
    public int getSmeltTime() { return value(SMELT); }
    public boolean isSpeedBoost() { return value(BOOST) != 0; }
    public int getBellowsCount() { return value(BELLOWS); }
    public boolean isBurning() { return getBurnTime() > 0; }
    public int getCookProgressScaled(int pixels) { return scaled(getCookTime(), Math.max(1, getSmeltTime()), pixels); }
    public int getContentsScaled(int pixels) { return scaled(getVis(), TileAlchemyFurnace.MAX_VIS, pixels); }
    public int getBurnTimeRemainingScaled(int pixels) { return scaled(getBurnTime(), getCurrentItemBurnTime() == 0 ? 200 : getCurrentItemBurnTime(), pixels); }
    private static int scaled(int current, int duration, int pixels) { return duration <= 0 ? 0 : (int) Math.clamp((long) current * pixels / duration, 0, pixels); }

    @Override public boolean stillValid(Player player) {
        return player == viewer && player.isAlive() && !player.isSpectator() && (tile == null
                ? player.level().hasChunkAt(furnacePos) && player.level().getBlockState(furnacePos).is(ThaumcraftDistillation.ALCHEMY_FURNACE.get())
                    && player.distanceToSqr(furnacePos.getX() + .5, furnacePos.getY() + .5, furnacePos.getZ() + .5) <= 64
                : tile.stillValid(player));
    }

    private boolean active(Player player) {
        return tile != null && player instanceof ServerPlayer && viewer.containerMenu == this && stillValid(player);
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || tile != null && !active(player)) return;
        super.clicked(slot, button, type, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!active(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack current = slot.getItem(), original = current.copy();
        boolean moved;
        if (index < 2) moved = moveItemStackTo(current, 2, 38, false);
        else if (TileAlchemyFurnace.isItemFuel(current)) {
            moved = moveItemStackTo(current, 1, 2, false);
            if (!moved) moved = moveItemStackTo(current, 0, 1, false);
        } else if (TileAlchemyFurnace.hasAspects(current, player.level())) moved = moveItemStackTo(current, 0, 1, false);
        else if (index < 29) moved = moveItemStackTo(current, 29, 38, false);
        else moved = moveItemStackTo(current, 2, 29, false);
        if (!moved || current.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (current.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, current);
        return original;
    }
}
