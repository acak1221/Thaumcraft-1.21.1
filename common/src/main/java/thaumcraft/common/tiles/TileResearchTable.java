package thaumcraft.common.tiles;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.IScribeTools;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteCopying;
import thaumcraft.common.lib.research.TableTopology;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Physical shared inventory, authoritative revision and original shared-bonus ecology. */
public final class TileResearchTable extends RetainedDataBlockEntity implements Container, MenuProvider {
    public static final int SIZE = 2;
    public static final int MAX_BONUS_ASPECTS = 256;
    private static final Map<String, Aspect> INFUSED_ORES = Map.of(
            "air_infused_stone", Aspect.AIR, "fire_infused_stone", Aspect.FIRE,
            "water_infused_stone", Aspect.WATER, "earth_infused_stone", Aspect.EARTH,
            "order_infused_stone", Aspect.ORDER, "entropy_infused_stone", Aspect.ENTROPY);
    private static final Map<Aspect, TagKey<Block>> ORE_BONUS_TAGS = Map.of(
            Aspect.AIR, bonusTag("air_ore"), Aspect.FIRE, bonusTag("fire_ore"),
            Aspect.WATER, bonusTag("water_ore"), Aspect.EARTH, bonusTag("earth_ore"),
            Aspect.ORDER, bonusTag("order_ore"), Aspect.ENTROPY, bonusTag("entropy_ore"));
    private static final List<Aspect> BONUS_SOURCE_ORDER = List.of(
            Aspect.AIR, Aspect.FIRE, Aspect.EARTH, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY);
    private static final TagKey<Block> GROUND_BONUS = bonusTag("ground");
    private static final TagKey<Block> CIRCUITS_BONUS = bonusTag("circuits");
    private static final TagKey<Block> BRAIN_JAR_BONUS = bonusTag("brain_jar");
    private static final ResourceLocation BRAIN_JAR_ID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "brain_jar");
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private Map<String, Integer> bonusPools = Map.of();
    private Map<String, Integer> observedBonus = Map.of();
    private final ItemStack[] observedReferences = {ItemStack.EMPTY, ItemStack.EMPTY};
    private final ItemStack[] observedStacks = {ItemStack.EMPTY, ItemStack.EMPTY};
    private long editRevision;
    private boolean evacuating;
    private boolean topologyCheckPending = true;
    private int nextBonusRecalc;

    public TileResearchTable(BlockPos pos, BlockState state) {
        super(ThaumcraftContent.RESEARCH_TABLE_BLOCK_ENTITY.get(), pos, state);
    }
    TileResearchTable(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }

    @Override public void clearRemoved() {
        super.clearRemoved();
        topologyCheckPending = true;
    }

    public static void tickTopologyOnLoad(Level level, BlockPos pos, BlockState state, TileResearchTable table) {
        if (table.isDataRejected()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.getServer().isSameThread()) throw new IllegalStateException("Research table ticking requires the server thread");
        if (table.topologyCheckPending) {
            table.topologyCheckPending = false;
            if (state.getBlock() instanceof BlockTable block) block.validateTopology(level, pos);
        }
        // Repair may have collapsed/replaced this BE. Ecology only belongs to a
        // still-loaded physical main, but must keep ticking after the first tick.
        if (table.isRemoved() || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != table
                || !table.isValidBlockState(level.getBlockState(pos))) return;
        if (table.nextBonusRecalc++ > 600) {
            table.nextBonusRecalc = 0; // Original postincrement: a 602-tick period.
            Map<String, Integer> bonus = new LinkedHashMap<>(table.bonusPools);
            table.recalculateBonus(serverLevel, bonus);
            table.setBonusPools(bonus);
        }
    }

    @Override public boolean isValidBlockState(BlockState state) {
        if (!ThaumcraftContent.TABLE.isPresent()) return super.isValidBlockState(state);
        return state.is(ThaumcraftContent.TABLE.get()) && TableTopology.isMain(state.getValue(BlockTable.VARIANT));
    }

    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return isDataRejected()?ItemStack.EMPTY:items.get(slot); }
    @Override public int getMaxStackSize() { return 64; }

    @Override public ItemStack removeItem(int slot, int amount) {
        if (isDataRejected()) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (isDataRejected()) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        refreshRevision();
        return removed;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        if (isDataRejected()) return;
        items.set(slot, Objects.requireNonNull(stack, "stack"));
        // R10's original whole-stack transfers can return a copied note above64.
        // Preserve that owned stock; ordinary tools/items retain the original64 setter limit.
        if (!(stack.getItem() instanceof ItemResearchNotes) && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override public void clearContent() { if (isDataRejected()) return; items.clear(); setChanged(); }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (isDataRejected()) return false;
        if (stack.isEmpty()) return false;
        return switch (slot) {
            case 0 -> stack.getItem() instanceof IScribeTools;
            // Original automation accepts meta<64, whereas the GUI uses the
            // ItemResearchNotes class and also accepts discovery presentation.
            case 1 -> stack.is(ThaumcraftContent.RESEARCH_NOTE.get());
            default -> false;
        };
    }

    @Override public boolean stillValid(Player player) {
        return !isDataRejected() && level != null && !isRemoved() && player.level() == level
                && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this
                && isValidBlockState(level.getBlockState(worldPosition))
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    /** Also detects vanilla getItem() mutations that have not called setChanged yet. */
    public long tableRevision() { refreshRevision(); return editRevision; }
    public Map<String, Integer> bonusPools() { return bonusPools; }
    public int bonusFor(String aspect) { return aspect == null ? 0 : bonusPools.getOrDefault(aspect, 0); }

    /** Shared table material, not a player's personal RP or a discovery grant. */
    public void setBonusPools(Map<String, Integer> pools) {
        if (isDataRejected()) return;
        bonusPools = canonicalBonusPools(pools);
        setChanged();
    }

    public static boolean isCanonicalBonusTag(String tag) {
        if (tag == null || tag.isEmpty() || tag.length() > ResearchNoteData.MAX_ASPECT_LENGTH
                || tag.getBytes(StandardCharsets.UTF_8).length > ResearchNoteData.MAX_ASPECT_LENGTH) return false;
        Aspect aspect = Aspect.getAspect(tag);
        return aspect != null && tag.equals(aspect.getTag());
    }

    /** Bounded, positive, tag-sorted native bonus snapshot, never a mutable shared map. */
    public static Map<String, Integer> canonicalBonusPools(Map<String, Integer> pools) {
        Objects.requireNonNull(pools, "bonus pools");
        if (pools.size() > MAX_BONUS_ASPECTS) throw new IllegalArgumentException("Too many table bonus aspects");
        TreeMap<String, Integer> sorted = new TreeMap<>();
        pools.forEach((tag, amount) -> {
            if (!isCanonicalBonusTag(tag) || amount == null || amount <= 0) {
                throw new IllegalArgumentException("Invalid table bonus aspect");
            }
            sorted.put(tag, amount);
        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    /** TC4 TileResearchTable:189-290, preserving short-circuit draw order and first-success return. */
    private void recalculateBonus(ServerLevel level, Map<String, Integer> bonus) {
        BlockPos above = worldPosition.above();
        // isDay() gained a fixed-time-dimension rule in modern Minecraft; the
        // original condition was exactly skylightSubtracted >= 4 instead.
        if (level.getSkyDarken() >= 4 && level.getMaxLocalRawBrightness(above) < 4
                && !level.canSeeSky(above) && level.random.nextInt(20) == 0) mergeBonus(bonus, Aspect.ENTROPY);
        // getActualHeight was the dimension's usable span (Nether128/other256).
        // Native dimensions provide logicalHeight and can have a negative origin.
        int minimum = level.getMinBuildHeight(), height = level.dimensionType().logicalHeight();
        int altitude = worldPosition.getY() - minimum;
        if ((float) altitude > (float) height * 0.5F && level.random.nextInt(20) == 0) mergeBonus(bonus, Aspect.AIR);
        if ((float) altitude > (float) height * 0.66F && level.random.nextInt(20) == 0) mergeBonus(bonus, Aspect.AIR);
        if ((float) altitude > (float) height * 0.75F && level.random.nextInt(20) == 0) mergeBonus(bonus, Aspect.AIR);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = -8; y <= 8; y++) {
                    int absoluteY = worldPosition.getY() + y;
                    if (absoluteY <= minimum || absoluteY >= minimum + height) continue;
                    cursor.set(worldPosition.getX() + x, absoluteY, worldPosition.getZ() + z);
                    // Unlike the old World getter this must never force chunks.
                    if (!level.hasChunkAt(cursor)) continue;
                    BlockState nearby = level.getBlockState(cursor);
                    BonusProducer producer = bonusProducer(nearby);
                    if (producer != null && bonus.getOrDefault(producer.aspect().getTag(), 0) < 1
                            && level.random.nextInt(producer.chance()) == 0) {
                        mergeBonus(bonus, producer.aspect());
                        return;
                    }
                    // A normal essentia jar or aura node is NOT a brain jar.
                    if ((nearby.is(Blocks.BOOKSHELF) && level.random.nextInt(300) == 0)
                            || (isBrainJar(nearby) && level.random.nextInt(200) == 0)) {
                        Aspect[] aspects = Aspect.aspects.values().toArray(Aspect[]::new);
                        mergeBonus(bonus, aspects[level.random.nextInt(aspects.length)]);
                        return;
                    }
                }
            }
        }
    }

    private static void mergeBonus(Map<String, Integer> bonus, Aspect aspect) {
        // AspectList.merge is max(existing,1), not additive regeneration. Keep
        // externally stored >1 pools intact while bounding newly produced keys.
        if (bonus.getOrDefault(aspect.getTag(), 0) < 1 && bonus.size() < MAX_BONUS_ASPECTS) bonus.put(aspect.getTag(), 1);
    }

    private record BonusProducer(Aspect aspect, int chance) {}

    private static BonusProducer bonusProducer(BlockState state) {
        Aspect ore = infusedOreAspect(state), crystal = crystalAspect(state);
        if (ore == Aspect.AIR) return new BonusProducer(Aspect.AIR, 20);
        if (crystal == Aspect.AIR) return new BonusProducer(Aspect.AIR, 10);
        if (state.is(BlockTags.FIRE) || (state.getBlock() instanceof LiquidBlock && state.getFluidState().is(FluidTags.LAVA))
                || ore == Aspect.FIRE) return new BonusProducer(Aspect.FIRE, 20);
        if (crystal == Aspect.FIRE) return new BonusProducer(Aspect.FIRE, 10);
        if (isOriginalGround(state) || ore == Aspect.EARTH) return new BonusProducer(Aspect.EARTH, 20);
        if (crystal == Aspect.EARTH) return new BonusProducer(Aspect.EARTH, 10);
        if (state.getBlock() instanceof LiquidBlock && state.getFluidState().is(FluidTags.WATER)) return new BonusProducer(Aspect.WATER, 15);
        if (ore == Aspect.WATER) return new BonusProducer(Aspect.WATER, 20);
        if (crystal == Aspect.WATER) return new BonusProducer(Aspect.WATER, 10);
        if (isOriginalCircuitsOrPiston(state) || ore == Aspect.ORDER) return new BonusProducer(Aspect.ORDER, 20);
        if (crystal == Aspect.ORDER) return new BonusProducer(Aspect.ORDER, 10);
        if (ore == Aspect.ENTROPY) return new BonusProducer(Aspect.ENTROPY, 20);
        return crystal == Aspect.ENTROPY ? new BonusProducer(Aspect.ENTROPY, 10) : null;
    }

    private static Aspect crystalAspect(BlockState state) {
        for (var entry : ThaumcraftContent.CRYSTALS.entrySet()) if (state.is(entry.getValue().get())) return Aspect.getAspect(entry.getKey());
        return null;
    }

    private static Aspect infusedOreAspect(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        Aspect aspect = "thaumcraft".equals(id.getNamespace()) ? INFUSED_ORES.get(id.getPath()) : null;
        if (aspect != null) return aspect;
        for (Aspect primal : BONUS_SOURCE_ORDER) if (state.is(ORE_BONUS_TAGS.get(primal))) return primal;
        return null;
    }

    private static boolean isOriginalGround(BlockState state) {
        // Material.ground, NOT grass/sand/rock: in particular do not substitute
        // modern #dirt, which also contains grass, mycelium and moss.
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.FARMLAND) || state.is(GROUND_BONUS);
    }

    private static boolean isOriginalCircuitsOrPiston(BlockState state) {
        Block block = state.getBlock();
        // Native successors of old Material.circuits/piston; not all redstone
        // blocks, logs, pressure plates, conductive blocks or enchantment shelves.
        return block instanceof BaseRailBlock || block instanceof TorchBlock || block instanceof WallTorchBlock
                || block instanceof RedStoneWireBlock || block instanceof DiodeBlock || block instanceof ButtonBlock
                || block instanceof LeverBlock || block instanceof TripWireBlock || block instanceof TripWireHookBlock
                || block instanceof LadderBlock || block instanceof FlowerPotBlock || block instanceof AbstractSkullBlock || block instanceof PistonBaseBlock
                || block instanceof PistonHeadBlock || block instanceof MovingPistonBlock || state.is(CIRCUITS_BONUS);
    }

    private static boolean isBrainJar(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(BRAIN_JAR_ID)
                || state.is(BRAIN_JAR_BONUS);
    }

    private static TagKey<Block> bonusTag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("thaumcraft", "research_bonus/" + path));
    }

    /** Combine-only material commit: inventory references/counts/components are untouched. */
    public void commitKnowledgeBonus(long expectedRevision, Map<String, Integer> bonus,
                                     PlayerKnowledge actor, PlayerKnowledge stagedKnowledge) {
        if (isDataRejected()) throw new IllegalStateException("Unread research table cannot commit");
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getServer().isSameThread()) {
            throw new IllegalStateException("Research table combination requires the server thread");
        }
        if (tableRevision() != expectedRevision || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalStateException("Admitted research table changed during combine planning");
        }
        Map<String, Integer> nextBonus = canonicalBonusPools(bonus);
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(stagedKnowledge, "staged knowledge");
        actor.copyFrom(stagedKnowledge);
        bonusPools = nextBonus;
        editRevision++;
        observedBonus = nextBonus;
    }

    /**
     * Material-only commit after server admission/pure planning. No setters,
     * listeners, dirty/block updates, network or sounds may run between writes.
     * Physical copied-note counts are preserved without changing other slot limits.
     */
    public void commitCopy(long expectedRevision, ItemStack note, ResearchNoteCopying.Plan plan,
                           PlayerKnowledge actor, ItemStack paper, ItemStack blackDye) {
        if (isDataRejected()) throw new IllegalStateException("Unread research table cannot commit");
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getServer().isSameThread()) throw new IllegalStateException("Research copy requires the server thread");
        if (tableRevision() != expectedRevision || expectedRevision == Long.MAX_VALUE || note != items.get(1)
                || note.getCount() == Integer.MAX_VALUE || plan.count() != note.getCount() + 1
                || !note.is(ThaumcraftContent.DISCOVERY.get()) || !ResearchNoteCopying.canPay(actor, ItemResearchNotes.data(note), note.getCount())
                || !paper.is(net.minecraft.world.item.Items.PAPER) || paper.isEmpty() || paper.getDamageValue() != 0
                // Original TileResearchTable:407-441 (oracle) - the copy costs ONE PAPER and the
                // BLACK DYE (the original Items.dye of the metadata zero), not an ink sac.
                || !blackDye.is(net.minecraft.world.item.Items.BLACK_DYE) || blackDye.isEmpty() || blackDye.getDamageValue() != 0) {
            throw new IllegalStateException("Admitted research copy changed before commit");
        }
        var component = ThaumcraftContent.RESEARCH_NOTE_DATA.get();
        ItemStack snapshot = note.copy(); snapshot.set(component, plan.note()); snapshot.setCount(plan.count());
        // The same actual stack is updated. All allocations/validation precede these callback-free owner writes.
        actor.copyFrom(plan.knowledge()); paper.shrink(1); blackDye.shrink(1);
        note.set(component, plan.note()); note.setCount(plan.count());
        editRevision++;
        observedReferences[1] = note; observedStacks[1] = snapshot;
    }

    /** Material-only R08 edit commit; no setters/notifications between the prepared owner changes. */
    public void commitEdit(long expectedRevision, ItemStack tool, ItemStack note, Map<String, Integer> bonus,
                           PlayerKnowledge actor, PlayerKnowledge stagedKnowledge) {
        if (isDataRejected()) throw new IllegalStateException("Unread research table cannot commit");
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getServer().isSameThread()) {
            throw new IllegalStateException("Research table edits require the server thread");
        }
        if (tableRevision() != expectedRevision || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalStateException("Admitted research table changed during pure planning");
        }
        ItemStack nextTool = Objects.requireNonNull(tool, "tool").copy();
        ItemStack nextNote = Objects.requireNonNull(note, "note").copy();
        ItemStack toolSnapshot = nextTool.copy();
        ItemStack noteSnapshot = nextNote.copy();
        Map<String, Integer> nextBonus = canonicalBonusPools(bonus);
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(stagedKnowledge, "staged knowledge");

        actor.copyFrom(stagedKnowledge);
        items.set(0, nextTool);
        items.set(1, nextNote);
        bonusPools = nextBonus;
        editRevision++;
        observedReferences[0] = nextTool;
        observedReferences[1] = nextNote;
        observedStacks[0] = toolSnapshot;
        observedStacks[1] = noteSnapshot;
        observedBonus = nextBonus;
    }

    private void refreshRevision() {
        boolean changed = !bonusPools.equals(observedBonus);
        for (int slot = 0; slot < SIZE; slot++) {
            // Reference identity also records replacing A with an equal new note;
            // a content hash alone would admit a stale same-key request (ABA).
            changed |= items.get(slot) != observedReferences[slot]
                    || !ItemStack.matches(items.get(slot), observedStacks[slot]);
        }
        if (!changed) return;
        // Never wrap. Admission permanently refuses edits on an exhausted BE,
        // while ordinary inventory recovery/extraction must remain possible.
        if (editRevision < Long.MAX_VALUE) editRevision++;
        rememberMaterial();
    }

    private void rememberMaterial() {
        for (int slot = 0; slot < SIZE; slot++) {
            observedReferences[slot] = items.get(slot);
            observedStacks[slot] = items.get(slot).copy();
        }
        observedBonus = bonusPools;
    }

    @Override public Component getDisplayName() { return Component.translatable("container.thaumcraft.research_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerResearchTable(id, inventory, this);
    }

    @Override public void setChanged() {
        refreshRevision();
        super.setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Transfer physical contents once; the safe level-post tick delivers the queued outputs. */
    public void evacuate() {
        if (isDataRejected()) return;
        if (!(level instanceof ServerLevel serverLevel) || evacuating || isEmpty()) return;
        evacuating = true;
        try {
            List<ItemStack> outputs = new ArrayList<>(SIZE);
            for (ItemStack stack : items) if (!stack.isEmpty()) outputs.add(stack.copy());
            DeferredItemDrops.enqueue(serverLevel, worldPosition, outputs);
            // Do not spawn inside BlockTable.onRemove: synchronous entity-join
            // callbacks may replace this block/inventory before LevelChunk has
            // finished removing/recreating its BE, losing the replacement data.
            // The level-post delivery path runs after that mutation completes.
            items.clear();
            setChanged();
        } finally {
            evacuating = false;
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (saveRejected(tag)) return;
        super.saveAdditional(tag, registries);
        refreshRevision();
        ListTag inventory = new ListTag();
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            // Native ItemStack.CODEC caps its count at99. Keep count separately
            // so later R10 overstack is not silently clamped/truncated here.
            // This native schema is not an importer for old signed-byte Count.
            entry.put("Stack", stack.copyWithCount(1).save(registries));
            entry.putInt("StoredCount", stack.getCount());
            inventory.add(entry);
        }
        tag.put("Inventory", inventory);
        tag.putLong("EditRevision", editRevision);
        tag.putInt("NextBonusRecalc", nextBonusRecalc);
        ListTag bonus = new ListTag();
        bonusPools.forEach((aspect, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("tag", aspect);
            entry.putInt("amount", amount);
            bonus.add(entry);
        });
        tag.put("BonusAspects", bonus);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (retainMalformed(tag, () -> {
            validateSlots(tag,"Inventory",SIZE,registries);
            for (Tag value:compoundList(tag,"Inventory")) {
                CompoundTag row=(CompoundTag)value;
                if (!row.contains("StoredCount",Tag.TAG_INT) || row.getInt("StoredCount")<=0) throw new IllegalArgumentException("Inventory: invalid StoredCount");
            }
            ListTag bonus=compoundList(tag,"BonusAspects");
            if (bonus.size()>MAX_BONUS_ASPECTS) throw new IllegalArgumentException("Too many BonusAspects");
            java.util.Set<String> seen=new java.util.HashSet<>();
            for (Tag value:bonus) {
                CompoundTag row=(CompoundTag)value;
                if (!row.contains("tag",Tag.TAG_STRING) || !isCanonicalBonusTag(row.getString("tag")) || !row.contains("amount",Tag.TAG_INT)
                        || row.getInt("amount")<=0 || !seen.add(row.getString("tag"))) throw new IllegalArgumentException("Invalid/duplicate BonusAspects row: "+row);
            }
        })) return;
        super.loadAdditional(tag, registries);
        nextBonusRecalc = Math.clamp(tag.getInt("NextBonusRecalc"), 0, 601);
        items.clear();
        ListTag inventory = tag.getList("Inventory", Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(SIZE, inventory.size()); index++) {
            CompoundTag entry = inventory.getCompound(index);
            int slot = entry.getByte("Slot") & 255;
            int count = entry.getInt("StoredCount");
            if (slot >= SIZE || count <= 0) continue;
            ItemStack stack = requiredStack(registries, entry.get("Stack"));
            if (stack.isEmpty()) continue;
            stack.setCount(count);
            items.set(slot, stack);
        }
        Map<String, Integer> bonus = new LinkedHashMap<>();
        ListTag savedBonus = tag.getList("BonusAspects", Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(MAX_BONUS_ASPECTS, savedBonus.size()); index++) {
            CompoundTag entry = savedBonus.getCompound(index);
            String aspect = entry.getString("tag");
            int amount = entry.getInt("amount");
            if (isCanonicalBonusTag(aspect) && amount > 0) bonus.put(aspect, amount);
        }
        bonusPools = canonicalBonusPools(bonus);
        if (level instanceof ServerLevel) {
            // Loading data into a live instance is a replacement, not permission
            // to rewind the generation held by its already-open menus.
            refreshRevision();
        } else {
            editRevision = Math.max(0L, tag.getLong("EditRevision"));
            rememberMaterial();
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
