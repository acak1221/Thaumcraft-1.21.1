/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [14 Sep 2013, 01:07:25 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.ThaumcraftFX;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.block.tile.container.ContainerEnchanter;
import thaumic.tinkerer.common.content.Enchantments;
import thaumic.tinkerer.common.enchantment.ModEnchantments;
import thaumic.tinkerer.common.enchantment.core.EnchantmentData;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;

/**
 * The block entity of the Osmotic Enchanter: the tool and the wand in its two slots, the enchantments chosen for the tool
 * with their levels and the vis they cost, and the work itself.
 *
 * <p>While it works, the enchanter takes one whole vis of a primal aspect from the wand every tick (a random one of those the
 * wand holds at least one whole vis of and the enchantments still need), from six pillars within four blocks (two to twelve
 * Obsidian Totems on the level of the table, topped by a Nitor), until every primal aspect is paid; then the enchantments
 * are put on the tool. 1.7.10 ran this on both sides, with a copy of the progress on the client; the port runs it on the
 * server and sends the state to the clients.</p>
 */
public class TileEnchanter extends BlockEntity implements WorldlyContainer, ExtendedMenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger("ThaumicTinkerer");

    private static final String TAG_ENCHANTS = "enchants";
    private static final String TAG_LEVELS = "levelsIntArray";
    private static final String TAG_TOTAL_ASPECTS = "totalAspects";
    private static final String TAG_CURRENT_ASPECTS = "currentAspects";
    private static final String TAG_WORKING = "working";

    /** The primal aspects in the order of {@code LibFeatures.PRIMAL_ASPECTS} of 1.7.10. */
    public static final Aspect[] PRIMAL_ASPECTS = { Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER,
            Aspect.ENTROPY };

    /** The slot of the tool to enchant and the slot of the wand. */
    public static final int SLOT_TOOL = 0;
    public static final int SLOT_WAND = 1;

    /** How many pillars the enchanter needs, and how far they may stand from it (horizontally). */
    public static final int PILLARS = 6;
    public static final int PILLAR_RANGE = 4;

    public final List<ResourceKey<Enchantment>> enchantments = new ArrayList<>();
    public final List<Integer> levels = new ArrayList<>();

    public AspectList totalAspects = new AspectList();
    public AspectList currentAspects = new AspectList();

    public boolean working = false;

    private final NonNullList<ItemStack> inventorySlots = NonNullList.withSize(2, ItemStack.EMPTY);

    /** The pillars found: the column (x, z), the level of the table and the height of the Nitor on top. Not saved. */
    private final List<int[]> pillars = new ArrayList<>();

    public TileEnchanter(BlockPos pos, BlockState state) {
        super(Enchantments.enchanterType(), pos, state);
    }

    // ---- the chosen enchantments ------------------------------------------------------------------------------------

    public void clearEnchants() {
        enchantments.clear();
        levels.clear();
    }

    public void appendEnchant(ResourceKey<Enchantment> enchant) {
        enchantments.add(enchant);
    }

    public void appendLevel(int level) {
        levels.add(level);
    }

    public void removeEnchant(int index) {
        enchantments.remove(index);
    }

    public void removeLevel(int index) {
        levels.remove(index);
    }

    public void setEnchant(int index, ResourceKey<Enchantment> enchant) {
        enchantments.set(index, enchant);
    }

    public void setLevel(int index, int level) {
        levels.set(index, level);
    }

    /** The vis of the chosen enchantments at their levels, added up ({@code totalAspects}). */
    public void updateAspectList() {
        totalAspects = new AspectList();
        for (int i = 0; i < enchantments.size() && i < levels.size(); i++) {
            EnchantmentData data = EnchantmentManager.data(enchantments.get(i), levels.get(i));
            if (data == null) continue;
            for (Aspect aspect : data.aspects.getAspectsSorted()) totalAspects.add(aspect, data.aspects.getAmount(aspect));
        }
    }

    // ---- the work -------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEnchanter enchanter) {
        if (level instanceof ServerLevel serverLevel) enchanter.tick(serverLevel);
    }

    private void tick(ServerLevel level) {
        ItemStack tool = getItem(SLOT_TOOL);
        if (tool.isEmpty() && (!enchantments.isEmpty() || !levels.isEmpty())) {
            clearEnchants();
            sync();
        }

        if (!working) return;

        if (tool.isEmpty()) {
            working = false;
            sync();
            return;
        }

        checkPillars(level);

        if (!working) { // the pillars are gone
            sync();
            return;
        }

        if (isPaid()) {
            finish(level, tool);
            return;
        }

        ItemStack wand = getItem(SLOT_WAND);
        if (WandHelper.isWand(wand) && !WandHelper.isStaff(wand)) {
            AspectList wandAspects = WandHelper.getAllVis(wand);

            List<Aspect> aspectsThatCanGet = new ArrayList<>();
            for (Aspect aspect : PRIMAL_ASPECTS) {
                int missing = totalAspects.getAmount(aspect) - currentAspects.getAmount(aspect);
                int onWand = wandAspects.getAmount(aspect);

                if (missing > 0 && onWand >= 100) aspectsThatCanGet.add(aspect);
            }

            int i = aspectsThatCanGet.isEmpty() ? 0 : level.random.nextInt(aspectsThatCanGet.size());
            Aspect aspect = aspectsThatCanGet.isEmpty() ? null : aspectsThatCanGet.get(i);

            if (aspect != null && WandHelper.consumeAllVisCrafting(wand, null, new AspectList().add(aspect, 1), true)) {
                currentAspects.add(aspect, 1);
                if (i < pillars.size()) effects(level, pillars.get(i), aspect);
                // The screen shows the progress: send it every other tick, and the last step at once.
                if ((level.getGameTime() & 1L) == 0L || isPaid()) sync();
            }
        }
    }

    /** Whether every primal aspect of the work has been paid. */
    public boolean isPaid() {
        for (Aspect aspect : PRIMAL_ASPECTS) {
            if (currentAspects.getAmount(aspect) < totalAspects.getAmount(aspect)) return false;
        }
        return true;
    }

    /** All paid: the enchantments go onto the tool and the enchanter is idle again. */
    private void finish(ServerLevel level, ItemStack tool) {
        working = false;
        currentAspects = new AspectList();
        totalAspects = new AspectList();

        for (int i = 0; i < enchantments.size() && i < levels.size(); i++) {
            ResourceKey<Enchantment> key = enchantments.get(i);
            Optional<Holder.Reference<Enchantment>> holder = ModEnchantments.holder(level.registryAccess(), key);
            if (holder.isPresent()) tool.enchant(holder.get(), levels.get(i));
            else LOG.warn("The Osmotic Enchanter has no enchantment {} in this world", key.location());
        }

        clearEnchants();
        playSound(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                ThaumcraftSoundIds.WAND, 1F, 1F);
        sync();
    }

    private void effects(ServerLevel level, int[] pillar, Aspect aspect) {
        if (!level.random.nextBoolean()) return;

        BlockPos top = new BlockPos(pillar[0], pillar[2], pillar[1]);
        ThaumcraftFX.essentiaTrail(level, worldPosition, top, aspect);
        level.sendParticles(ParticleTypes.ENCHANT, pillar[0] + 0.5, pillar[2] + 0.5, pillar[1] + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
        level.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.25,
                worldPosition.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
        if (level.random.nextInt(5) == 0) {
            playSound(level, pillar[0] + 0.5, pillar[3] + 0.5, pillar[1] + 0.5, ThaumcraftSoundIds.EGIDLE, 0.5F, 1F);
        }
    }

    private static void playSound(ServerLevel level, double x, double y, double z, ResourceLocation id, float volume,
            float pitch) {
        try {
            SoundEvent sound = ThaumcraftSoundIds.event(id);
            level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, volume, pitch);
        } catch (IllegalArgumentException e) {
            LOG.debug("No sound {}", id, e);
        }
    }

    // ---- the pillars ----------------------------------------------------------------------------------------------

    /**
     * Looks after the pillars. Returns false (and stops the work, losing the vis paid so far) when there are not six of them.
     * The list is searched again when a pillar was changed.
     */
    public boolean checkPillars(ServerLevel level) {
        if (pillars.isEmpty()) {
            if (assignPillars(level)) {
                working = false;
                currentAspects = new AspectList();
                return false;
            }
            return true;
        }

        for (int[] pillar : pillars) {
            int pillarHeight = findPillar(level, pillar[0], pillar[3], pillar[1]);
            if (pillarHeight == -1) {
                pillars.clear();
                return checkPillars(level);
            } else if (pillarHeight != pillar[2]) pillar[2] = pillarHeight;
        }

        return true;
    }

    /** Collects up to six pillars around the enchanter; returns true when there are fewer (the original's "failed"). */
    public boolean assignPillars(ServerLevel level) {
        int y = worldPosition.getY();
        for (int x = worldPosition.getX() - PILLAR_RANGE; x <= worldPosition.getX() + PILLAR_RANGE; x++) {
            for (int z = worldPosition.getZ() - PILLAR_RANGE; z <= worldPosition.getZ() + PILLAR_RANGE; z++) {
                int height = findPillar(level, x, y, z);
                if (height != -1) pillars.add(new int[] { x, z, height, y });

                if (pillars.size() == PILLARS) return false;
            }
        }

        pillars.clear();
        return true;
    }

    /**
     * The height of the Nitor on top of the column, or -1 when the column is no pillar: from the level of the table upwards
     * a run of two to twelve Obsidian Totems, then a Nitor.
     */
    public int findPillar(Level level, int x, int y, int z) {
        BlockState totem = obsidianTotem();
        Block nitor = nitor();
        int obsidianFound = 0;
        for (int i = 0; ; i++) {
            if (y + i >= level.getMaxBuildHeight()) return -1;

            BlockPos pos = new BlockPos(x, y + i, z);
            if (!level.hasChunkAt(pos)) return -1;
            BlockState state = level.getBlockState(pos);
            if (totem != null && state == totem) {
                ++obsidianFound;
                continue;
            }
            if (nitor != null && state.is(nitor)) {
                if (obsidianFound >= 2 && obsidianFound < 13) return y + i;
                return -1;
            }

            return -1;
        }
    }

    private static BlockState totemState;
    private static Block nitorBlock;

    /** {@code blockCosmeticSolid:0}: the Obsidian Totem of Thaumcraft. */
    private static synchronized BlockState obsidianTotem() {
        if (totemState == null) totemState = BlocksTC.state("blockCosmeticSolid", 0);
        return totemState;
    }

    /** {@code blockAiry:1}: the Nitor that is placed in the world. */
    private static synchronized Block nitor() {
        if (nitorBlock == null) {
            BlockState state = BlocksTC.state("blockAiry", 1);
            if (state != null) nitorBlock = state.getBlock();
        }
        return nitorBlock;
    }

    // ---- saving and syncing -------------------------------------------------------------------------------------

    /**
     * 1.7.10 {@code markDirty}: the inventory changed (a slot of the screen, the tool taken out or put in). While the
     * enchanter is not working that forgets the chosen enchantments, as the original did. Code of the enchanter itself that
     * only wants the chunk saved calls {@link #saveChanges()}.
     */
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide && !working) {
            clearEnchants();
            sync();
        }
    }

    /** Marks the chunk for saving without touching the chosen enchantments. */
    private void saveChanges() {
        if (level != null) setChanged(level, worldPosition, getBlockState());
    }

    /** {@code markBlockForUpdate}: saves the chunk and sends the state to the clients. */
    public void sync() {
        saveChanges();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeCustom(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readCustom(tag, registries);
    }

    public void writeCustom(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag enchants = new ListTag();
        for (ResourceKey<Enchantment> enchantment : enchantments) enchants.add(StringTag.valueOf(enchantment.location().toString()));
        tag.put(TAG_ENCHANTS, enchants);
        tag.putIntArray(TAG_LEVELS, levels.stream().mapToInt(Integer::intValue).toArray());

        totalAspects.writeToNBT(tag, TAG_TOTAL_ASPECTS);
        currentAspects.writeToNBT(tag, TAG_CURRENT_ASPECTS);
        tag.putBoolean(TAG_WORKING, working);
        ContainerHelper.saveAllItems(tag, inventorySlots, true, registries);
    }

    public void readCustom(CompoundTag tag, HolderLookup.Provider registries) {
        working = tag.getBoolean(TAG_WORKING);
        currentAspects = new AspectList();
        currentAspects.readFromNBT(tag, TAG_CURRENT_ASPECTS);
        totalAspects = new AspectList();
        totalAspects.readFromNBT(tag, TAG_TOTAL_ASPECTS);

        enchantments.clear();
        ListTag enchants = tag.getList(TAG_ENCHANTS, Tag.TAG_STRING);
        for (int i = 0; i < enchants.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(enchants.getString(i));
            if (id != null) enchantments.add(ResourceKey.create(Registries.ENCHANTMENT, id));
        }
        levels.clear();
        for (int level : tag.getIntArray(TAG_LEVELS)) levels.add(level);
        // A level for every enchantment and no more: a damaged tag cannot make the lists differ in length.
        while (levels.size() > enchantments.size()) levels.remove(levels.size() - 1);
        while (levels.size() < enchantments.size()) levels.add(1);

        for (int i = 0; i < inventorySlots.size(); i++) inventorySlots.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, inventorySlots, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeCustom(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- WorldlyContainer -------------------------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return inventorySlots.size();
    }

    @Override
    public boolean isEmpty() {
        return inventorySlots.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventorySlots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(inventorySlots, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventorySlots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventorySlots.set(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D) <= 64;
    }

    /** {@code isItemValidForSlot}: false, only the screen puts things in. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
        inventorySlots.clear();
        for (int i = 0; i < inventorySlots.size(); i++) inventorySlots.set(i, ItemStack.EMPTY);
    }

    /** {@code getAccessibleSlotsFromSide}: none, a hopper or a pipe never reaches the slots. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    // ---- the menu -------------------------------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerEnchanter(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
