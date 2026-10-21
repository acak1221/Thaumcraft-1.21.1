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
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileNode.NodeModifier;
import thaumcraft.common.tiles.TileNode.NodeType;

/**
 * Original TileJarNode: the node jar. It keeps the aspects and the identity
 * of a node until the player turns it back into a node with a wand.
 */
public final class TileJarNode extends BlockEntity implements IWandable {
    private final AspectList aspects = new AspectList();
    private final AspectList aspectsBase = new AspectList();
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier = null;
    private String id = "";
    public long animate = 0L;
    public boolean drop = true;

    public TileJarNode(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.JAR_NODE_TILE.get(), pos, state);
    }

    public AspectList getAspects() { return aspects.copy(); }
    public AspectList getAspectsBase() { return aspectsBase.copy(); }
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

    /**
     * Original TileJarNode:222-241 - the wand turns the jar back into a node and
     * hands the stored aspects, the node type, the node modifier and the identity
     * of the node to it. The original never consumes the wand.
     */
    @Override public int onWandRightClick(Level level, ItemStack stack, Player player, int x, int y, int z, int side, int event) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isClientSide) {
            drop = false;
            level.setBlock(pos, ThaumcraftEldritchBlocks.AIRY.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.BlockAiry.VARIANT, 0), 3);
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
