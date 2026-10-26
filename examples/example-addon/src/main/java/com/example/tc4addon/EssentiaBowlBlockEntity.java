package com.example.tc4addon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftFX;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.api.wands.IWandable;

/**
 * The block entity of the essentia bowl: up to 32 essentia of one aspect, shown by goggles of revealing
 * ({@link IAspectContainer}), given to an essentia tube below it ({@link IEssentiaTransport}), and answering a wand
 * ({@link IWandable}): a click empties it, a sneaking click condenses one Ordo from the vis network.
 */
// [doc:bowl-entity]
public class EssentiaBowlBlockEntity extends TileThaumcraft implements IAspectContainer, IEssentiaTransport, IWandable {
    public static final int CAPACITY = 32;
    private Aspect aspect;
    private int amount;

    public EssentiaBowlBlockEntity(BlockPos pos, BlockState state) { super(ExampleAddon.ESSENTIA_BOWL_TYPE, pos, state); }

    // TileThaumcraft saves this custom data, sends it with the chunk and in the update packet.
    @Override public void readCustomNBT(CompoundTag tag) {
        aspect = Aspect.getAspect(tag.getString("Aspect"));
        amount = aspect == null ? 0 : tag.getShort("Amount");
    }

    @Override public void writeCustomNBT(CompoundTag tag) {
        if (aspect == null) return;
        tag.putString("Aspect", aspect.getTag());
        tag.putShort("Amount", (short) amount);
    }

    /** 1.7.10 markDirty + markBlockForUpdate: save the chunk and resend the custom data to the clients. */
    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }
    // [/doc]

    // [doc:bowl-container]
    @Override public AspectList getAspects() { return aspect == null ? new AspectList() : new AspectList().add(aspect, amount); }

    @Override public void setAspects(AspectList aspects) {
        Aspect[] list = aspects == null ? new Aspect[0] : aspects.getAspects();
        aspect = list.length == 0 ? null : list[0];
        amount = aspect == null ? 0 : Math.min(CAPACITY, aspects.getAmount(aspect));
        changed();
    }

    @Override public boolean doesContainerAccept(Aspect tag) { return tag != null && (aspect == null || aspect == tag); }

    /** Returns what did not fit. */
    @Override public int addToContainer(Aspect tag, int add) {
        if (!doesContainerAccept(tag) || add <= 0) return add;
        int taken = Math.min(add, CAPACITY - amount);
        if (taken > 0) {
            aspect = tag;
            amount += taken;
            changed();
        }
        return add - taken;
    }

    @Override public boolean takeFromContainer(Aspect tag, int take) {
        if (tag == null || tag != aspect || take <= 0 || amount < take) return false;
        amount -= take;
        if (amount == 0) aspect = null;
        changed();
        return true;
    }

    @Override @Deprecated public boolean takeFromContainer(AspectList aspects) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect tag, int need) { return tag != null && tag == aspect && amount >= need; }
    @Override @Deprecated public boolean doesContainerContain(AspectList aspects) { return false; }
    @Override public int containerContains(Aspect tag) { return tag != null && tag == aspect ? amount : 0; }
    // [/doc]

    // [doc:bowl-transport]
    // A source like the alembic: it connects downwards only, never pulls, and gives its essentia to a tube that sucks.
    @Override public boolean isConnectable(Direction face) { return face == Direction.DOWN; }
    @Override public boolean canInputFrom(Direction face) { return false; }
    @Override public boolean canOutputTo(Direction face) { return face == Direction.DOWN; }
    @Override public void setSuction(Aspect tag, int suction) {}
    @Override public Aspect getSuctionType(Direction face) { return null; }
    @Override public int getSuctionAmount(Direction face) { return 0; }
    @Override public Aspect getEssentiaType(Direction face) { return aspect; }
    @Override public int getEssentiaAmount(Direction face) { return amount; }

    @Override public int takeEssentia(Aspect tag, int take, Direction face) {
        return canOutputTo(face) && takeFromContainer(tag, take) ? take : 0;
    }

    @Override public int addEssentia(Aspect tag, int add, Direction face) { return 0; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public boolean renderExtendedTube() { return true; }
    // [/doc]

    // [doc:bowl-wandable]
    /** A negative answer lets the wand go on; 1 handles the click and swings the arm. */
    @Override public int onWandRightClick(Level world, ItemStack wand, Player player, int x, int y, int z, int side, int event) {
        if (player.isShiftKeyDown()) {
            if (world.isClientSide) return 1;
            // One whole vis of Ordo from the vis network (the network counts in centivis) makes one Ordo essentia.
            if (doesContainerAccept(Aspect.ORDER) && amount < CAPACITY && VisNetHandler.drainVis(world, worldPosition, Aspect.ORDER, 100) == 100) {
                addToContainer(Aspect.ORDER, 1);
                ThaumcraftFX.blockSparkle((ServerLevel) world, worldPosition, Aspect.ORDER.getColor());
            }
            return 1;
        }
        if (aspect == null) return -1;
        if (!world.isClientSide) {
            player.displayClientMessage(Component.translatable("tc4_example.essentia_bowl.emptied", amount, aspect.getName()), true);
            ThaumcraftFX.bubbles((ServerLevel) world, worldPosition, aspect.getColor());
            setAspects(null);
        }
        return 1;
    }

    /** The click without a target: not used by the bowl. */
    @Override public ItemStack onWandRightClick(Level world, ItemStack wand, Player player) { return null; }
    @Override public void onUsingWandTick(ItemStack wand, Player player, int ticks) {}
    @Override public void onWandStoppedUsing(ItemStack wand, Level world, Player player, int ticks) {}
    // [/doc]
}
