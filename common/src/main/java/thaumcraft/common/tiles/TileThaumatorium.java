package thaumcraft.common.tiles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockThaumatorium;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftThaumatorium;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.crafting.CrucibleRecipeInput;
import thaumcraft.common.lib.ThaumatoriumRules;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.ThaumatoriumOutput;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Original baseline Thaumatorium: one learned recipe, one catalyst, exact essentia, five-tick work and 128 suction. */
public final class TileThaumatorium extends BlockEntity implements WorldlyContainer, MenuProvider, IAspectContainer, EssentiaAtomicEndpoint {
    /**
     * Original TileThaumatorium:323-331 - the block event of the machine starts the vent of
     * the client; the counter itself is runtime state and is never saved.
     */
    public interface ClientEffects {
        /** Original TileThaumatorium:221-230 - the vent particles in front of the machine. */
        void vent(Level level, BlockPos pos, Direction facing);
    }
    private static ClientEffects clientEffects;
    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }

    private ItemStack input = ItemStack.EMPTY, physicalOutput = ItemStack.EMPTY;
    private Map<Aspect, Integer> stock = Map.of();
    private ResourceLocation selected;
    private record Job(ResourceLocation id, java.util.UUID author) {}
    private List<Job> jobs = List.of();
    private int maxRecipes = 1;
    private Aspect suction;
    private int counter;
    private boolean heated, ticking, finishing, evacuating;
    private ThaumatoriumOutput.Plan outputIntent;
    private ItemStack inputIntent;
    private Map<Aspect, Integer> stockIntent;
    private Direction faceIntent;
    private CrucibleRecipe recipeIntent;
    private CompoundTag rejected;
    /** Original TileThaumatorium:75 - the vent counter of the client. */
    private int venting;

    public TileThaumatorium(BlockPos pos, BlockState state) { super(ThaumcraftThaumatorium.TILE.get(), pos, state); }
    public Direction facing() { return getBlockState().getValue(BlockThaumatorium.FACING); }
    public ResourceLocation selectedRecipe() { return selected; }
    public boolean isRecipeSelected(ResourceLocation id) { return jobs.stream().anyMatch(job -> job.id().equals(id)); }
    public int recipeCount() { return jobs.size(); }
    /** Client preview of the selected formulas, in saved order; independent of catalyst presence. */
    public List<ItemStack> previewOutputs() {
        if (level == null) return List.of();
        return jobs.stream().map(job -> resolve(job.id()))
                .map(recipe -> recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess()).copyWithCount(1)).toList();
    }
    public int recipeCapacity() { return maxRecipes; }
    public boolean isHeated() { return heated; }
    public int getCounter() { return counter; }
    public boolean isQuarantined() { return rejected != null; }
    public boolean current() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this && level.getBlockState(worldPosition) == getBlockState(); }
    public boolean structureValid() { return current() && level.hasChunkAt(worldPosition.above()) && level.hasChunkAt(worldPosition.below())
            && level.getBlockState(worldPosition.above()).is(ThaumcraftThaumatorium.THAUMATORIUM_TOP.get()) && level.getBlockEntity(worldPosition.below()) instanceof TileCrucible; }
    private boolean canMutate() { return level instanceof ServerLevel server && server.getServer().isSameThread() && current() && !isQuarantined() && !evacuating && !finishing && !EssentiaTransfers.isMutationActive(); }
    public CrucibleRecipe recipe() {
        if (level == null) return null;
        if (selected != null && isRecipeSelected(selected)) {
            CrucibleRecipe active = resolve(selected);
            if (active != null && active.catalystMatches(input)) return active;
        }
        for (Job job : jobs) { CrucibleRecipe recipe = resolve(job.id()); if (recipe != null && recipe.catalystMatches(input)) return recipe; }
        return null;
    }
    private CrucibleRecipe resolve(ResourceLocation id) { return level == null || id == null ? null : thaumcraft.common.crafting.ProgrammaticRecipes.crucible(level.getRecipeManager(), id); }
    public boolean toggleRecipe(ServerPlayer player, ResourceLocation id) {
        if (!canMutate() || !stillValid(player) || id == null) return false;
        if (isRecipeSelected(id)) { jobs = jobs.stream().filter(job -> !job.id().equals(id)).toList(); selected = null; suction = null; changed(); return true; }
        if (jobs.size() >= maxRecipes) return false;
        CrucibleRecipe recipe = thaumcraft.common.crafting.ProgrammaticRecipes.crucible(level.getRecipeManager(), id);
        if (recipe == null || !recipe.catalystMatches(input) || !recipe.research().isEmpty() && !KnowledgeAccess.get(player).isResearchComplete(recipe.research())) return false;
        var after = new java.util.ArrayList<>(jobs); after.add(new Job(id, player.getUUID())); jobs = List.copyOf(after);
        if (selected == null) selected = id;
        suction = null; changed(); return true;
    }
    public boolean powered() { return level != null && (level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.below()) || level.hasNeighborSignal(worldPosition.above())); }
    private boolean checkHeat() { BlockPos pos = worldPosition.below(2); return level.hasChunkAt(pos) && (level.getBlockState(pos).is(BlockTags.FIRE)
            || level.getBlockState(pos).is(Blocks.LAVA) || level.getBlockEntity(pos) instanceof TileNitor); }

    public static void tick(Level level, BlockPos pos, BlockState state, TileThaumatorium tile) {
        if (level.isClientSide) {
            tile.clientTick();
            return;
        }
        if (!(level instanceof ServerLevel) || !tile.current() || tile.ticking || tile.isQuarantined() || EssentiaTransfers.isMutationActive()) return;
        tile.ticking = true;
        try { tile.serverTick(); } catch (RuntimeException failure) { Thaumcraft.LOG.warn("Thaumatorium work failed at {}; paid stock remains with its actual owner", pos, failure); }
        finally { tile.ticking = tile.finishing = false; tile.outputIntent = null; tile.inputIntent = null; tile.stockIntent = null; }
    }

    /**
     * Original TileThaumatorium:221-230 - the client draws the vent particles while the vent
     * counter of the machine runs.
     */
    private void clientTick() {
        if (venting <= 0) return;
        --venting;
        if (clientEffects != null && level != null) clientEffects.vent(level, worldPosition, facing());
    }

    /**
     * Original TileThaumatorium:323-331 - the block event of the machine starts the vent of
     * the client.
     */
    @Override public boolean triggerEvent(int id, int data) {
        if (id >= 0) {
            if (level != null && level.isClientSide) venting = 7;
            return true;
        }
        return super.triggerEvent(id, data);
    }
    private void serverTick() {
        flushOutput();
        if (!structureValid()) return;
        if (counter % ThaumatoriumRules.HEAT_INTERVAL == 0) { heated = checkHeat(); refreshUpgrades(); }
        ++counter;
        if (!ThaumatoriumRules.workTick(counter, heated, powered()) || !physicalOutput.isEmpty()) return;
        if (input.isEmpty()) { suction = null; return; }
        CrucibleRecipe recipe = recipe();
        if (recipe == null || !recipe.catalystMatches(input)) { suction = null; return; }
        for (Job job : jobs) if (resolve(job.id()) == recipe) { selected = job.id(); break; }
        ItemStack output = recipe.getResultItem(level.registryAccess());
        if (output.isEmpty() || !output.isItemEnabled(level.enabledFeatures())) return;
        ItemStack beforeInput = ItemStack.parse(level.registryAccess(), input.save(level.registryAccess())).orElseThrow(); Map<Aspect, Integer> beforeStock = stock;
        Direction beforeFace = facing();
        var plan = ThaumatoriumOutput.prepare((ServerLevel) level, worldPosition.relative(facing()), facing().getOpposite(), output);
        if (plan == null || !current() || stock != beforeStock || !ItemStack.matches(input, beforeInput) || facing() != beforeFace || recipe() != recipe) return;
        suction = ThaumatoriumRules.missing(recipe.aspects(), getAspects());
        if (suction == null) {
            finishing = true; outputIntent = plan; inputIntent = beforeInput; stockIntent = stock; faceIntent = beforeFace; recipeIntent = recipe;
            try { plan.commit(this); } finally { finishing = false; outputIntent = null; inputIntent = null; stockIntent = null; }
            flushOutput();
        } else fill();
    }
    private void fill() {
        for (int height = 0; height <= 1; height++) {
            BlockEntity receiver = height == 0 ? this : level.getBlockEntity(worldPosition.above());
            if (!(receiver instanceof EssentiaAtomicEndpoint)) continue;
            for (Direction side : Direction.values()) {
                if (side == facing() || side == Direction.DOWN || height == 0 && side == Direction.UP) continue;
                BlockEntity neighbor = EssentiaTransfers.connectableNeighbor(receiver, side);
                if (!(neighbor instanceof IEssentiaTransport source)) continue;
                Direction output = side.getOpposite();
                if (source.getEssentiaAmount(output) <= 0 || source.getSuctionAmount(output) >= ThaumatoriumRules.SUCTION
                        || ThaumatoriumRules.SUCTION < source.getMinimumSuction()) continue;
                if (EssentiaTransfers.pullOne(receiver, side, suction) > 0) return;
            }
        }
    }
    public boolean canCommitOutput(ThaumatoriumOutput.Plan plan) {
        CrucibleRecipe recipe = recipe();
        return finishing && ticking && outputIntent == plan && current() && structureValid() && !isQuarantined()
                && inputIntent != null && ItemStack.matches(input, inputIntent) && stockIntent == stock && physicalOutput.isEmpty()
                && facing() == faceIntent && recipe == recipeIntent && recipe != null && recipe.matches(new CrucibleRecipeInput(input, getAspects()), level)
                && ItemStack.matches(plan.output(), recipe.getResultItem(level.registryAccess()));
    }
    public void applyCraftRaw(ThaumatoriumOutput.Plan plan) {
        if (!plan.ownsRaw(this) || !canCommitOutput(plan)) throw new IllegalStateException("No exact paid Thaumatorium output scope");
        ItemStack remaining = input.copy(); remaining.shrink(1);
        input = remaining;
        stock = Map.of(); // Original completeRecipe clears all accumulated essentia, including leftovers from a changed recipe.
        suction = null;
        if (plan.physical()) physicalOutput = plan.output();
    }
    public void notifyCraftChanged() {
        Job active = jobs.stream().filter(job -> job.id().equals(selected)).findFirst().orElse(null);
        if (active != null && active.author() != null && level instanceof ServerLevel server) {
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(active.author());
            if (player != null && outputIntent != null) thaumcraft.common.lib.WarpEvents.onCrafted(player, outputIntent.output());
        }
        changed();
        if (current()) level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .25F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * .8F);
    }
    private void flushOutput() {
        if (physicalOutput.isEmpty() || !(level instanceof ServerLevel server)) return;
        Direction out = facing();
        DeferredItemDrops.enqueueDirected(server, worldPosition,
                new Vec3(worldPosition.getX() + .5 + out.getStepX() * .66, worldPosition.getY() + .33 - out.getStepY(), worldPosition.getZ() + .5 + out.getStepZ() * .66),
                new Vec3(out.getStepX() * .075F, .025F, out.getStepZ() * .075F), physicalOutput);
        physicalOutput = ItemStack.EMPTY; setChanged();
        // Original TileThaumatorium:252 - the machine vents when its output leaves it.
        server.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0);
    }

    @Override public AspectList getAspects() { var copy = new AspectList(); stock.forEach(copy::add); return copy; }
    @Override public void setAspects(AspectList aspects) {}
    @Override public int addToContainer(Aspect aspect, int amount) {
        CrucibleRecipe recipe = recipe();
        if (amount <= 0 || !canMutate() || !EssentiaAtomicEndpoint.validAspect(aspect) || recipe == null) return amount;
        int accepted = Math.min(amount, Math.max(0, recipe.aspects().getOrDefault(aspect.getTag(), 0) - containerContains(aspect)));
        if (accepted > 0) { var after = new LinkedHashMap<>(stock); after.merge(aspect, accepted, Integer::sum); stock = Collections.unmodifiableMap(after); changed(); }
        return amount - accepted;
    }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        if (!canMutate() || amount <= 0 || containerContains(aspect) < amount) return false;
        var after = new LinkedHashMap<>(stock); int remaining = after.get(aspect) - amount;
        if (remaining == 0) after.remove(aspect); else after.put(aspect, remaining);
        stock = Collections.unmodifiableMap(after); changed(); return true;
    }
    @Override public boolean takeFromContainer(AspectList aspects) { return false; }
    @Override public boolean doesContainerContain(AspectList aspects) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return amount >= 0 && containerContains(aspect) >= amount; }
    @Override public int containerContains(Aspect aspect) { return stock.getOrDefault(aspect, 0); }
    @Override public boolean doesContainerAccept(Aspect aspect) { return EssentiaAtomicEndpoint.validAspect(aspect); }
    @Override public boolean isConnectable(Direction face) { return !isQuarantined() && face != facing(); }
    @Override public boolean canInputFrom(Direction face) { return isConnectable(face); }
    @Override public boolean canOutputTo(Direction face) { return false; }
    @Override public void setSuction(Aspect type, int amount) {}
    @Override public Aspect getSuctionType(Direction face) { return suction; }
    @Override public int getSuctionAmount(Direction face) { return isQuarantined() || suction == null ? 0 : ThaumatoriumRules.SUCTION; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public Aspect getEssentiaType(Direction face) { return null; }
    @Override public int getEssentiaAmount(Direction face) { return 0; }
    @Override public int takeEssentia(Aspect type, int amount, Direction face) { return 0; }
    @Override public int addEssentia(Aspect type, int amount, Direction face) { return canInputFrom(face) ? amount - addToContainer(type, amount) : 0; }
    @Override public boolean renderExtendedTube() { return false; }

    private record StockState(Map<Aspect, Integer> stock, ResourceLocation selected, Aspect suction, ItemStack input) implements State {}
    @Override public State snapshotAtomicState() { return new StockState(stock, selected, suction, input.copy()); }
    @Override public boolean matchesAtomicState(State state) { return state instanceof StockState before && before.stock() == stock && java.util.Objects.equals(before.selected(), selected)
            && before.suction() == suction && ItemStack.matches(before.input(), input) && current() && !isQuarantined(); }
    @Override public Change prepareExtract(State state, Aspect type, int units) { return null; }
    @Override public Change prepareInsert(State state, Aspect type, int units) {
        CrucibleRecipe recipe = recipe();
        if (!(state instanceof StockState before) || !matchesAtomicState(before) || units <= 0 || recipe == null || !EssentiaAtomicEndpoint.validAspect(type)
                || units > recipe.aspects().getOrDefault(type.getTag(), 0) - before.stock().getOrDefault(type, 0)) return null;
        var after = new LinkedHashMap<>(before.stock()); after.merge(type, units, Integer::sum);
        return new Change(this, state, new StockState(Collections.unmodifiableMap(after), selected, suction, before.input()), units, true);
    }
    @Override public void applyAtomicRaw(Change change) { EssentiaTransfers.requireAtomicChange(this, change); stock = ((StockState) change.after()).stock(); }
    void applyProxyRaw(TileThaumatoriumTop top, EssentiaAtomicEndpoint.Change change) {
        if (!EssentiaTransfers.isLocked(top) || top.main() != this || !(change.after() instanceof StockState after)) throw new IllegalStateException("Invalid Thaumatorium top transfer");
        stock = after.stock();
    }
    @Override public void notifyEssentiaStorageChanged() { changed(); }
    private void changed() { setChanged(); if (current()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }
    public void refreshUpgrades() {
        if (!current() || level.isClientSide || finishing || EssentiaTransfers.isMutationActive()) return;
        int matrices = 0;
        for (int y = 0; y <= 1; y++) for (Direction side : Direction.values()) {
            if (side == Direction.DOWN || side == facing()) continue;
            BlockPos pos = worldPosition.above(y).relative(side);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(ThaumcraftThaumatorium.MNEMONIC_MATRIX.get()) && state.getValue(thaumcraft.common.blocks.BlockMnemonicMatrix.FACING) == side.getOpposite()) ++matrices;
        }
        // Oracle TileThaumatorium:494-516 - one free formula, two per matrix.
        int capacity = ThaumatoriumRules.recipeCapacity(matrices);
        if (capacity == maxRecipes) return;
        maxRecipes = capacity;
        if (jobs.size() > capacity) jobs = List.copyOf(jobs.subList(0, capacity));
        if (selected != null && !isRecipeSelected(selected)) selected = null;
        changed();
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return input.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? input : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { if (slot != 0 || !canMutate() || amount <= 0) return ItemStack.EMPTY; ItemStack removed = input.split(amount); changed(); return removed; }
    @Override public ItemStack removeItemNoUpdate(int slot) { if (slot != 0 || !canMutate()) return ItemStack.EMPTY; ItemStack removed = input; input = ItemStack.EMPTY; return removed; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot == 0 && canMutate()) { input = stack; changed(); } }
    @Override public void clearContent() { if (canMutate()) { input = ItemStack.EMPTY; changed(); } }
    @Override public int[] getSlotsForFace(Direction side) { return new int[] {0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0 && canMutate(); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0 && canMutate(); }
    @Override public boolean stillValid(Player player) { return current() && !isQuarantined() && player.level() == level && player.isAlive() && !player.isSpectator()
            && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64; }
    @Override public Component getDisplayName() { return Component.translatable("container.thaumcraft.thaumatorium"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player viewer) { return new ContainerThaumatorium(id, inventory, this); }
    public void evacuate() {
        if (evacuating || !(level instanceof ServerLevel server)) return;
        evacuating = true;
        ItemStack catalyst = input, output = physicalOutput;
        input = physicalOutput = ItemStack.EMPTY; stock = Map.of(); suction = null;
        DeferredItemDrops.enqueue(server, worldPosition, java.util.List.of(catalyst, output));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); write(tag, registries); }
    private void write(CompoundTag tag, HolderLookup.Provider registries) {
        if (rejected != null) { tag.put("RejectedThaumatorium", rejected.copy()); return; }
        if (!input.isEmpty()) tag.put("Input", input.save(registries));
        if (!physicalOutput.isEmpty()) tag.put("PendingOutput", physicalOutput.save(registries));
        var recipes = new ListTag();
        for (Job job : jobs) { var row = new CompoundTag(); row.putString("id", job.id().toString()); if (job.author() != null) row.putUUID("author", job.author()); recipes.add(row); }
        tag.put("Recipes", recipes); tag.putInt("MaxRecipes", maxRecipes);
        if (selected != null) tag.putString("Recipe", selected.toString()); // Also retained as a readable active-recipe hint.
        var aspects = new ListTag(); stock.forEach((type, amount) -> { var row = new CompoundTag(); row.putString("key", type.getTag()); row.putInt("amount", amount); aspects.add(row); }); tag.put("Essentia", aspects);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = physicalOutput = ItemStack.EMPTY; stock = Map.of(); selected = null; jobs = List.of(); maxRecipes = 1; suction = null;
        try {
            if (tag.contains("RejectedThaumatorium")) throw new IllegalArgumentException("Rejected Thaumatorium payload");
            if (tag.contains("Input")) input = ItemStack.parse(registries, tag.get("Input")).orElseThrow();
            if (tag.contains("PendingOutput")) physicalOutput = ItemStack.parse(registries, tag.get("PendingOutput")).orElseThrow();
            if (tag.contains("Recipe")) selected = ResourceLocation.parse(tag.getString("Recipe"));
            if (tag.contains("Recipes")) {
                if (!(tag.get("Recipes") instanceof ListTag list) || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND || list.size() > ThaumatoriumRules.MAX_RECIPES) throw new IllegalArgumentException("Invalid selected formulas");
                var parsedJobs = new java.util.ArrayList<Job>(); var seen = new java.util.HashSet<ResourceLocation>();
                for (Tag raw : tag.getList("Recipes", Tag.TAG_COMPOUND)) { var row = (CompoundTag) raw; ResourceLocation id = ResourceLocation.parse(row.getString("id"));
                    if (!seen.add(id)) throw new IllegalArgumentException("Duplicate selected formula"); parsedJobs.add(new Job(id, row.hasUUID("author") ? row.getUUID("author") : null)); }
                jobs = List.copyOf(parsedJobs);
            } else if (selected != null) {
                // Recipe is only a readable single-recipe hint. Native writers store ownership in Recipes[].author.
                jobs = List.of(new Job(selected, null));
            }
            maxRecipes = Math.clamp(tag.getInt("MaxRecipes"), 1, ThaumatoriumRules.MAX_RECIPES);
            if (tag.contains("Essentia") && (!(tag.get("Essentia") instanceof ListTag list) || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid essentia rows");
            var parsed = new LinkedHashMap<Aspect, Integer>();
            for (Tag raw : tag.getList("Essentia", Tag.TAG_COMPOUND)) {
                var row = (CompoundTag) raw; Aspect aspect = Aspect.getAspect(row.getString("key")); int amount = row.getInt("amount");
                if (aspect == null || amount <= 0 || parsed.put(aspect, amount) != null) throw new IllegalArgumentException("Invalid stored essentia");
            }
            stock = Collections.unmodifiableMap(parsed); rejected = null;
        } catch (RuntimeException invalid) { rejected = tag.contains("RejectedThaumatorium", Tag.TAG_COMPOUND) ? tag.getCompound("RejectedThaumatorium").copy() : tag.copy(); }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { var tag = new CompoundTag(); write(tag, registries); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
