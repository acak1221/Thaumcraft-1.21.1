package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.wands.IWandable;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.tiles.TileParityRules;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

/**
 * Original TileJarNode: the node jar. It keeps the aspects and the identity
 * of a node until the player turns it back into a node with a wand.
 *
 * <p>Original TileJarNode extends TileJar and implements {@code IAspectContainer}: the stored
 * aspects may never grow past the stored base of the node.
 */
public final class TileJarNode extends BlockEntity implements IWandable, IAspectContainer {
    /**
     * The client event of the original that starts the one second animation of a node jar.
     *
     * <p>Oracle {@code thaumcraft/common/tiles/TileJarNode.java:204-219}: the event nine is the client
     * half of the block event that the original wand ritual sends when it traps a node in a jar. The
     * client answers it with a burst of block sparkles over the whole jar and with the one second
     * animation timer.</p>
     */
    public static final int ANIMATE_EVENT = 9;
    /** The particle count of one block sparkle of the original burst. */
    public static final int SPARKLE_COUNT = 5;
    /**
     * The volume of the original burst of block sparkles.
     *
     * <p>Oracle {@code thaumcraft/common/tiles/TileJarNode.java:206-213}: the burst runs over three
     * blocks in X, four in Y and three in Z around the jar, so thirty six sparkles.</p>
     */
    public static final int SPARKLE_X = 3, SPARKLE_Y = 4, SPARKLE_Z = 3;
    /** The number of block sparkles of the original burst. */
    public static int sparkleCount() { return SPARKLE_X * SPARKLE_Y * SPARKLE_Z; }
    /** The lifetime of the animation of the original, in milliseconds. */
    public static final long ANIMATE_MILLIS = 1000L;
    private final AspectList aspects = new AspectList();
    private final AspectList aspectsBase = new AspectList();
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier = null;
    private String id = "";
    public long animate = 0L;
    public boolean drop = true;

    /** The client only effects of the node jar, installed by the client of the port. */
    public interface ClientEffects {
        void nodeJarEvent(Level level, BlockPos pos, int event, int data);
    }
    private static ClientEffects clientEffects;
    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }

    public TileJarNode(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.JAR_NODE_TILE.get(), pos, state);
    }

    public NodeType getNodeType() { return nodeType; }
    public NodeModifier getNodeModifier() { return nodeModifier; }
    public String getId() { return id; }

    public void setId(String id) { this.id = id == null ? "" : id; }

    /** Original TileJarNode:83-87 - the aspects of the stored node. */
    public void setAspects(AspectList values) {
        aspects.aspects.clear();
        aspectsBase.aspects.clear();
        if (values != null) {
            for (Aspect aspect : values.getAspects()) {
                if (aspect == null) continue;
                aspects.add(aspect, values.getAmount(aspect));
                aspectsBase.add(aspect, values.getAmount(aspect));
            }
        }
    }
    // ---------------------------------------------------------------- aspect container
    /** Original TileJarNode:110-131 - the jar is an aspect container whose stored aspects
     * never grow past the stored base of the node. */
    @Override public AspectList getAspects() { return aspects.copy(); }

    /** Original TileJarNode:getAspectsBase - the base of the stored node. */
    public AspectList getAspectsBase() { return aspectsBase.copy(); }

    /** Original TileJarNode:255-257 - the jar accepts every aspect. */
    @Override public boolean doesContainerAccept(Aspect aspect) { return true; }

    /**
     * Original TileJarNode:118-127 - the jar takes only what fits above the base of the
     * node and returns the rest.
     */
    @Override public int addToContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) return amount;
        int accepted = TileParityRules.jarNodeAccepted(aspects.getAmount(aspect), aspectsBase.getAmount(aspect), amount);
        if (accepted <= 0) return amount;
        aspects.add(aspect, accepted);
        changed();
        return amount - accepted;
    }

    /** Original TileJarNode:129-138 - the jar gives an aspect back when it holds enough. */
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        if (aspect == null || !TileParityRules.jarNodeGives(aspects.getAmount(aspect), amount)) return false;
        aspects.remove(aspect, amount);
        changed();
        return true;
    }

    /** Original TileJarNode:140-142 - the jar never takes a list of aspects. */
    @Override @Deprecated public boolean takeFromContainer(AspectList values) { return false; }

    /** Original TileJarNode:142-144 - the jar holds as much of the aspect as it was given. */
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return aspects.getAmount(aspect) >= amount;
    }

    /** Original TileJarNode:146-153 - the jar holds every aspect of the list. */
    @Override @Deprecated public boolean doesContainerContain(AspectList values) {
        if (values == null) return false;
        for (Aspect aspect : values.getAspects()) {
            if (aspects.getAmount(aspect) >= values.getAmount(aspect)) continue;
            return false;
        }
        return true;
    }

    /** Original TileJarNode:155-157 - the stored amount of an aspect. */
    @Override public int containerContains(Aspect aspect) { return aspects.getAmount(aspect); }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Original TileJarNode:222-241 - the wand turns the jar back into a node and
     * hands the stored aspects, the node type, the node modifier and the identity
     * of the node to it. The original never consumes the wand.
     */
    @Override public int onWandRightClick(Level level, ItemStack stack, Player player, int x, int y, int z, int side, int event) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isClientSide) {
            drop = false;
            level.setBlock(pos, thaumcraft.common.lib.world.FamilyVariant.with(ThaumcraftEldritchBlocks.AIRY.get().defaultBlockState(), 0), 3);
            if (level.getBlockEntity(pos) instanceof TileNode node) {
                node.restoreFromJar(getAspects(), getNodeType(), getNodeModifier(), getId());
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
            level.levelEvent(2001, pos, 0);
            level.playSound(null, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D,
                    net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F, .9F + level.random.nextFloat() * .2F);
        }
        player.swing(InteractionHand.MAIN_HAND);
        return 0;
    }

    /** Original TileJarNode:243-245 - the contract without a target does nothing. */
    @Override public ItemStack onWandRightClick(Level level, ItemStack stack, Player player) { return null; }

    /** Original TileJarNode:247-249 - the jar does not use the wand over time. */
    @Override public void onUsingWandTick(ItemStack stack, Player player, int ticks) {}

    /** Original TileJarNode:251-253 - the jar does not react to a stopped wand. */
    @Override public void onWandStoppedUsing(ItemStack stack, Level level, Player player, int ticks) {}

    /**
     * Original TileJarNode:204-219 - the client event nine of the original: the burst of block
     * sparkles over the jar and the one second animation timer.
     *
     * <p>The original growth itself is dead code: oracle
     * {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:107-115} applies
     * {@code GL11.glScalef(size, size, size)} <em>after</em> every draw call of that renderer and pops
     * the matrix immediately, so the scale never reaches a single vertex. The native tree therefore
     * keeps the timer of the original and does not grow the node.</p>
     */
    @Override public boolean triggerEvent(int id, int data) {
        if (id == ANIMATE_EVENT) {
            if (level != null && level.isClientSide) {
                animate = System.currentTimeMillis() + ANIMATE_MILLIS;
                if (clientEffects != null) clientEffects.nodeJarEvent(level, worldPosition, id, data);
            }
            return true;
        }
        return super.triggerEvent(id, data);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        aspects.aspects.clear();
        aspectsBase.aspects.clear();
        AspectList stored = new AspectList();
        ListTag list = tag.getList("Aspects", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains("key")) continue;
            Aspect aspect = Aspect.getAspect(entry.getString("key"));
            if (aspect == null) continue;
            stored.add(aspect, entry.getInt("amount"));
        }
        aspectsBase.merge(stored);
        aspects.merge(stored);
        id = tag.getString("nodeId");
        ListTag base = tag.getList("AspectsBase", Tag.TAG_COMPOUND);
        if (base.size() > 0) {
            aspectsBase.aspects.clear();
            for (int i = 0; i < base.size(); i++) {
                CompoundTag entry = base.getCompound(i);
                if (!entry.contains("key")) continue;
                Aspect aspect = Aspect.getAspect(entry.getString("key"));
                if (aspect == null) continue;
                aspectsBase.add(aspect, entry.getInt("amount"));
            }
        }
        int type = tag.getByte("type");
        NodeType[] types = NodeType.values();
        nodeType = type >= 0 && type < types.length ? types[type] : NodeType.NORMAL;
        int modifier = tag.getByte("modifier");
        NodeModifier[] modifiers = NodeModifier.values();
        nodeModifier = modifier >= 0 && modifier < modifiers.length ? modifiers[modifier] : null;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("key", aspect.getTag());
            entry.putInt("amount", aspects.getAmount(aspect));
            list.add(entry);
        }
        tag.put("Aspects", list);
        ListTag base = new ListTag();
        for (Aspect aspect : aspectsBase.getAspects()) {
            if (aspect == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("key", aspect.getTag());
            entry.putInt("amount", aspectsBase.getAmount(aspect));
            base.add(entry);
        }
        tag.put("AspectsBase", base);
        tag.putString("nodeId", id);
        tag.putByte("type", (byte) nodeType.ordinal());
        tag.putByte("modifier", nodeModifier == null ? (byte) -1 : (byte) nodeModifier.ordinal());
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
