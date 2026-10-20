package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.config.ThaumcraftThaumatorium;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** The upper half exposes the same real catalyst and prepared stock; every access resolves the current lower half afresh. */
public final class TileThaumatoriumTop extends BlockEntity implements WorldlyContainer, EssentiaAtomicEndpoint, IAspectContainer {
    public TileThaumatoriumTop(BlockPos pos, BlockState state) { super(ThaumcraftThaumatorium.TOP_TILE.get(), pos, state); }
    public TileThaumatorium main() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition.below())
            && level.getBlockEntity(worldPosition.below()) instanceof TileThaumatorium main && main.structureValid() ? main : null; }
    @Override public boolean isConnectable(Direction face) { return main() != null && main().isConnectable(face); }
    @Override public boolean canInputFrom(Direction face) { return isConnectable(face); }
    @Override public boolean canOutputTo(Direction face) { return false; }
    @Override public void setSuction(Aspect aspect, int amount) {}
    @Override public Aspect getSuctionType(Direction face) { return main() == null ? null : main().getSuctionType(face); }
    @Override public int getSuctionAmount(Direction face) { return main() == null ? 0 : main().getSuctionAmount(face); }
    @Override public int getMinimumSuction() { return 0; }
    @Override public Aspect getEssentiaType(Direction face) { return null; }
    @Override public int getEssentiaAmount(Direction face) { return 0; }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) { return 0; }
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) { return canInputFrom(face) ? amount - main().addToContainer(aspect, amount) : 0; }
    @Override public boolean renderExtendedTube() { return false; }
    @Override public AspectList getAspects() { return main() == null ? new AspectList() : main().getAspects(); }
    @Override public void setAspects(AspectList aspects) {}
    @Override public int addToContainer(Aspect aspect, int amount) { return main() == null ? amount : main().addToContainer(aspect, amount); }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) { return main() != null && main().takeFromContainer(aspect, amount); }
    @Override public boolean takeFromContainer(AspectList aspects) { return false; }
    @Override public boolean doesContainerContain(AspectList aspects) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return main() != null && main().doesContainerContainAmount(aspect, amount); }
    @Override public int containerContains(Aspect aspect) { return main() == null ? 0 : main().containerContains(aspect); }
    @Override public boolean doesContainerAccept(Aspect aspect) { return main() != null && main().doesContainerAccept(aspect); }
    private record ProxyState(TileThaumatorium main, State state) implements State {}
    @Override public State snapshotAtomicState() { TileThaumatorium main = main(); return new ProxyState(main, main == null ? null : main.snapshotAtomicState()); }
    @Override public boolean matchesAtomicState(State before) { return before instanceof ProxyState saved && main() == saved.main() && main() != null && main().matchesAtomicState(saved.state()); }
    @Override public Change prepareExtract(State before, Aspect aspect, int amount) { return null; }
    @Override public Change prepareInsert(State before, Aspect aspect, int amount) {
        if (!(before instanceof ProxyState saved) || !matchesAtomicState(saved)) return null;
        Change change = saved.main().prepareInsert(saved.state(), aspect, amount);
        return change == null ? null : new Change(this, before, new ProxyState(saved.main(), change.after()), amount, true);
    }
    @Override public void applyAtomicRaw(Change change) {
        EssentiaTransfers.requireAtomicChange(this, change);
        ProxyState after = (ProxyState) change.after();
        after.main().applyProxyRaw(this, new Change(after.main(), ((ProxyState) change.before()).state(), after.state(), change.units(), change.changed()));
    }
    @Override public void notifyEssentiaStorageChanged() { if (main() != null) main().notifyEssentiaStorageChanged(); }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return main() == null || main().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return main() == null ? ItemStack.EMPTY : main().getItem(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return main() == null ? ItemStack.EMPTY : main().removeItem(slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return main() == null ? ItemStack.EMPTY : main().removeItemNoUpdate(slot); }
    @Override public void setItem(int slot, ItemStack stack) { if (main() != null) main().setItem(slot, stack); }
    @Override public void clearContent() { if (main() != null) main().clearContent(); }
    @Override public int[] getSlotsForFace(Direction side) { return main() == null ? new int[0] : new int[] {0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return main() != null && main().canPlaceItemThroughFace(slot, stack, face); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return main() != null && main().canTakeItemThroughFace(slot, stack, face); }
    @Override public boolean stillValid(Player player) { return main() != null && main().stillValid(player); }
}
