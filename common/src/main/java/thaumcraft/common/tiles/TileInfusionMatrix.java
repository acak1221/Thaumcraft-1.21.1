package thaumcraft.common.tiles;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.hooks.item.ItemStackHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.crafting.InfusionComponentPredicate;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.config.ThaumcraftRunic;
import thaumcraft.common.lib.runic.RunicRules;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.crafting.InfusionMatching;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.InfusionDemandEndpoint;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.infusion.InfusionInstability;
import thaumcraft.common.lib.infusion.InfusionStructure;
import thaumcraft.common.lib.network.InfusionFxNetworking;

/** Original TC4 matrix session/cycle for stack and enchantment outputs. */
public final class TileInfusionMatrix extends BlockEntity implements InfusionDemandEndpoint {
    private boolean active;
    private boolean crafting;
    private int instability;
    private AspectList recipeEssentia = new AspectList();
    private AspectList recipeCost = new AspectList();
    private final List<InfusionComponentPredicate> recipeIngredients = new ArrayList<>();
    private ItemStack recipeInput = ItemStack.EMPTY;
    private ItemStack recipeOutput = ItemStack.EMPTY;
    private String recipeOutputLabel = "";
    private int recipeInstability;
    private int recipeType;
    private int recipeXP;
    private ResourceLocation recipeEnchantment;
    private int countDelay = 10;
    private UUID recipePlayer;
    private String recipePlayerName = "";

    // Original count/itemCount/delay/symmetry/scan/FX are not disk state.
    private int count;
    private int itemCount;
    private int symmetry;
    private boolean scanNeeded = true;
    private boolean refreshSourcesOnScan = true;
    private List<BlockPos> pedestals = List.of();
    private BlockPos ingredientTarget;
    private boolean inCycle;

    public TileInfusionMatrix(BlockPos pos, BlockState state) {
        super(ThaumcraftInfusion.MATRIX_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isActive() { return active; }
    public boolean isCrafting() { return crafting; }
    public int getInstability() { return instability; }
    public int getSymmetry() { return symmetry; }
    public int getRecipeInstability() { return recipeInstability; }
    public AspectList getDemand() { return recipeEssentia.copy(); }
    public AspectList getRecipeCost() { return recipeCost.copy(); }
    public List<BlockPos> getPedestalPositions() { return pedestals; }
    public BlockPos getCenterPos() { return worldPosition.below(2); }
    public int getRemainingIngredientCount() { return recipeIngredients.size(); }
    public int getTickCount() { return count; }
    public int getRecipeType() { return recipeType; }
    public int getRemainingXPLevels() { return recipeXP; }
    public int getCycleDelay() { return countDelay; }
    public ResourceLocation getRecipeEnchantment() { return recipeEnchantment; }

    /** Actual consumption target/countdown only; never a preview/output item. */
    public BlockPos getIngredientTarget() { return ingredientTarget; }
    public int getIngredientCountdown() { return Math.clamp(itemCount, 0, 5); }
    public float getIngredientProgress() {
        return ingredientTarget == null ? 0 : Math.clamp((6 - itemCount) / 6.0F, 0, 1);
    }

    /** Broken structures pause, rather than erase, an already paid session. */
    public void setActive(boolean value) {
        if (!canMutate() || active == value) return;
        inCycle = true;
        try {
            if (value && !InfusionStructure.isValid(level, worldPosition) || !current()) return;
            active = value;
            changed();
            if (value) sendEffect(7, worldPosition, 0);
        } finally {
            inCycle = false;
        }
    }

    /** Research is checked once at start; the player's later logout never owns/blocks the session. */
    public boolean startCrafting(ServerPlayer player) {
        if (!canMutate() || !active || crafting || player == null || player.isRemoved() || player.isSpectator()
                || player.level() != level || !player.canInteractWithBlock(worldPosition, 0)
                || !level.mayInteract(player, worldPosition)) return false;
        inCycle = true;
        try {
            if (!InfusionStructure.isValid(level, worldPosition)) {
                active = false;
                changed();
                return false;
            }
            ServerLevel server = (ServerLevel) level;
            scanSurroundings(server);
            if (!current()) return false;
            TilePedestal center = pedestal(getCenterPos());
            if (center == null || center.getItem(0).isEmpty()) return false;
            ItemStack central = center.getItem(0).copy();
            List<ItemStack> components = new ArrayList<>();
            for (BlockPos pos : pedestals) {
                TilePedestal pedestal = pedestal(pos);
                if (pedestal != null && !pedestal.getItem(0).isEmpty()) components.add(pedestal.getItem(0).copy());
            }
            if (components.isEmpty()) return false;
            InfusionRecipeInput offered = new InfusionRecipeInput(central, components);
            for (var holder : server.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get())) {
                InfusionStackRecipe recipe = holder.value();
                if (!recipe.research().isEmpty() && !KnowledgeAccess.get(player).isResearchComplete(recipe.research())
                        || !recipe.matches(offered, server)) continue;
                ItemStack output = recipe.assemble(offered, server.registryAccess());
                if (output.isEmpty() || !output.isItemEnabled(server.enabledFeatures()) || !current()) continue;
                // M252-308: snapshots only. No component/central/vis/essentia debit at start.
                recipeInput = central.copy();
                recipeIngredients.clear();
                recipeIngredients.addAll(recipe.selectedComponents());
                recipeOutput = output.copy();
                recipeOutputLabel = "";
                recipeInstability = recipe.instability();
                recipeType = 0;
                recipeXP = 0;
                recipeEnchantment = null;
                recipeEssentia = recipe.aspectCost();
                recipeCost = recipeEssentia.copy();
                recipePlayer = player.getUUID();
                recipePlayerName = player.getGameProfile().getName();
                instability = symmetry + recipeInstability; // Intentionally not clamped to zero (or 25).
                // Original M252-333 deliberately retains a partial ingredient
                // countdown from an aborted session. Only reload resets it.
                ingredientTarget = null;
                crafting = true;
                changed();
                sendEffect(0, worldPosition, 0);
                return true;
            }
            for (var holder : server.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.ENCHANTMENT_RECIPE_TYPE.get())) {
                InfusionEnchantmentRecipe recipe = holder.value();
                if (!recipe.research().isEmpty() && !KnowledgeAccess.get(player).isResearchComplete(recipe.research())
                        || !recipe.matches(offered, server) || !current()) continue;
                recipeInput = central.copy();
                recipeIngredients.clear();
                recipeIngredients.addAll(recipe.selectedComponents());
                recipeOutput = ItemStack.EMPTY;
                recipeOutputLabel = "";
                recipeEnchantment = recipe.enchantment();
                recipeInstability = recipe.calculatedInstability(central);
                recipeType = 1;
                recipeXP = recipe.xpLevels(central, server.registryAccess());
                recipeEssentia = recipe.aspectCost(central, server.registryAccess());
                recipeCost = recipeEssentia.copy();
                recipePlayer = player.getUUID();
                recipePlayerName = player.getGameProfile().getName();
                instability = symmetry + recipeInstability;
                ingredientTarget = null;
                crafting = true;
                changed();
                sendEffect(0, worldPosition, 0);
                return true;
            }
            for (var holder : server.getRecipeManager().getAllRecipesFor(ThaumcraftRunic.AUGMENT_TYPE.get())) {
                InfusionRunicAugmentRecipe recipe = holder.value();
                if (!KnowledgeAccess.get(player).isResearchComplete(recipe.research()) || !recipe.matches(offered,server) || !current()) continue;
                ItemStack output = recipe.assemble(offered,server.registryAccess());
                if (output.isEmpty() || !output.isItemEnabled(server.enabledFeatures()) || !current()) continue;
                // Original runic recipe is an ordinary prepared STACK output, not an enchantment/XP ritual.
                recipeInput = central.copy(); recipeIngredients.clear(); recipeIngredients.addAll(recipe.selectedComponents(central));
                recipeOutput = output.copy(); recipeOutputLabel = ""; recipeInstability = recipe.instability(central);
                recipeType = 0; recipeXP = 0; recipeEnchantment = null;
                recipeEssentia = recipe.aspectCost(central); recipeCost = recipeEssentia.copy();
                recipePlayer = player.getUUID(); recipePlayerName = player.getGameProfile().getName();
                instability = symmetry + recipeInstability; ingredientTarget = null; crafting = true;
                changed(); sendEffect(0,worldPosition,0); return true;
            }
            return false;
        } finally {
            inCycle = false;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileInfusionMatrix matrix) {
        if (!(level instanceof ServerLevel server) || matrix.getLevel() != level || !pos.equals(matrix.worldPosition)
                || !matrix.canMutate()) return;
        matrix.inCycle = true;
        try {
            ++matrix.count;
            if (matrix.scanNeeded) matrix.scanSurroundings(server);
            if (!matrix.current()) return;
            // M217-226: local counter; structure checks precede a coincident cycle.
            if (matrix.count % (matrix.crafting ? 20 : 100) == 0 && !InfusionStructure.isValid(level, pos)) {
                matrix.active = false;
                matrix.changed();
                return; // Crucially: crafting, demand, input and output survive.
            }
            if (matrix.active && matrix.crafting && matrix.count % matrix.countDelay == 0) {
                matrix.craftCycle(server);
                if (matrix.current()) matrix.setChanged();
            }
        } finally {
            matrix.inCycle = false;
        }
    }

    private void craftCycle(ServerLevel server) {
        // A foreign/corrupt saved session cannot turn an unsupported output or empty ingredient into a reward.
        if (!supportedOutput(server) || recipeXP < 0 || recipeInstability < 0 || recipeInstability > 24
                || recipeIngredients.stream().anyMatch(required -> required.ingredient().isEmpty())) {
            active = false;
            changed();
            return;
        }
        TilePedestal center = pedestal(getCenterPos());
        boolean valid = center != null && InfusionMatching.centralStillMatches(center.getItem(0), recipeInput);
        // M335-419: invalid center does not draw nextInt(500); exactly one of all
        // 21 outcomes is selected. A valid hazard consumes this entire cycle.
        if (!valid || instability > 0 && server.random.nextInt(500) <= instability) {
            InfusionInstability.apply(this, server.random.nextInt(21));
            if (!current() || valid) return;
        }
        if (!valid) {
            instability = 0;
            crafting = false;
            recipeEssentia = new AspectList();
            ingredientTarget = null;
            // M412-419 clears demand, not itemCount. A restart in the same
            // loaded tile inherits that original countdown quirk.
            changed();
            sendEffect(6, worldPosition, 0);
            return;
        }

        // Original M421-451: enchantments first take one entire XP LEVEL from a nearby player per cycle.
        if (recipeType == 1 && recipeXP > 0) {
            ingredientTarget = null;
            takeEnchantmentXP(server);
            return;
        }
        if (recipeType == 1) countDelay = 10;

        // Original visSize int sum overflows at runic finalCharge25 and grants free essence.
        // Preserve each declared int amount, but never skip positive debt because its SUM wrapped.
        if (RunicRules.hasDemand(recipeEssentia.aspects)) {
            ingredientTarget = null;
            // M453-473: insertion order; stop after the first successful unit.
            // The atomic operation has ALREADY reduced demand before callbacks.
            for (Aspect aspect : recipeEssentia.getAspects()) {
                if (recipeEssentia.getAmount(aspect) <= 0) continue;
                if (EssentiaHandler.drainEssentia(this, aspect)) return;
                if (!current()) return;
                if (server.random.nextInt(100 - recipeInstability * 3) == 0) ++instability;
                if (instability > 25) instability = 25;
                changed();
                if (!current()) return;
            }
            scanNeeded = true;
            return;
        }

        if (!recipeIngredients.isEmpty()) {
            ingredientTarget = null;
            for (int index = 0; index < recipeIngredients.size(); index++) {
                InfusionComponentPredicate required = recipeIngredients.get(index);
                for (BlockPos pos : pedestals) {
                    TilePedestal pedestal = pedestal(pos);
                    if (pedestal == null || !required.matches(pedestal.getItem(0))) continue;
                    ingredientTarget = pos;
                    if (itemCount == 0) {
                        itemCount = 5;
                        changed();
                        sendEffect(2, pos, 0);
                    } else if (itemCount-- <= 1) {
                        ItemStack before = pedestal.getItem(0).copy();
                        ItemStack replacement = ItemStackHooks.getCraftingRemainingItem(before.copy()).copy();
                        if (!current() || pedestal(pos) != pedestal || !ItemStack.matches(before, pedestal.getItem(0))) return;
                        // Commit remaining/session fields BEFORE the pedestal's
                        // callback-bearing setter. The whole physical stack is
                        // replaced, as in M483-485 (not shrunk by one).
                        recipeIngredients.remove(index);
                        ingredientTarget = null;
                        // Dirty only; BlockEntity.setChanged would notify
                        // comparators before the paired pedestal field write.
                        server.blockEntityChanged(worldPosition);
                        pedestal.setInfusionStack(replacement);
                        if (current()) changed();
                    } else changed();
                    return; // Five decrements after the first beam = SIX successful cycles.
                }
                // M489-500: use the missing ingredient's CURRENT list index,
                // include zero-cost keys, and do not stop looking for later items.
                Aspect[] aspects = recipeEssentia.getAspects();
                if (aspects.length == 0 || server.random.nextInt(1 + index) != 0) continue;
                recipeEssentia.add(aspects[server.random.nextInt(aspects.length)], 1);
                if (server.random.nextInt(50 - recipeInstability * 2) == 0) ++instability;
                if (instability > 25) instability = 25;
                changed();
                if (!current()) return;
            }
            return;
        }
        finishCrafting(server, center);
    }

    private boolean supportedOutput(ServerLevel server) {
        return recipeType == 0 ? !recipeOutput.isEmpty()
                : recipeType == 1 && recipeEnchantment != null && recipeOutput.isEmpty()
                && server.registryAccess().registryOrThrow(Registries.ENCHANTMENT).containsKey(recipeEnchantment);
    }

    private void takeEnchantmentXP(ServerLevel server) {
        var players = server.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(10));
        for (ServerPlayer target : players) {
            if (target.isRemoved() || target.experienceLevel <= 0) continue;
            // Both owners are paid before damage, effects or update callbacks. A later failure never refunds a level.
            --recipeXP;
            countDelay = 20;
            server.blockEntityChanged(worldPosition);
            target.giveExperienceLevels(-1);
            target.hurt(server.damageSources().magic(), server.random.nextInt(2));
            if (!current()) return;
            changed();
            sendEffect(3, target.blockPosition(), target.getId());
            server.playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                    1.0F, 2.0F + server.random.nextFloat() * .4F);
            return;
        }
        // No players is a plain stall. Present, empty-level players have the original 1/3 shortage roll.
        Aspect[] aspects = recipeEssentia.getAspects();
        if (!players.isEmpty() && aspects.length > 0 && server.random.nextInt(3) == 0) {
            recipeEssentia.add(aspects[server.random.nextInt(aspects.length)], 1);
            if (server.random.nextInt(50 - recipeInstability * 2) == 0) ++instability;
            if (instability > 25) instability = 25;
            changed();
        }
    }

    private void finishCrafting(ServerLevel server, TilePedestal center) {
        ItemStack output = recipeType == 1
                ? InfusionEnchantmentRecipe.raise(center.getItem(0), recipeEnchantment, server.registryAccess()) : recipeOutput.copy();
        UUID crafter = recipePlayer;
        // M504-509/578-606: finish the session before any output/comparator/addon
        // callback. No output rematch, start debit, or ordinary slot-limit clamp.
        crafting = false;
        instability = 0;
        recipeEssentia = new AspectList();
        recipeOutput = ItemStack.EMPTY;
        recipeEnchantment = null;
        recipeXP = 0;
        ingredientTarget = null;
        itemCount = 0;
        server.blockEntityChanged(worldPosition);
        center.setInfusionStack(output);
        if (!current()) return;
        changed();
        sendEffect(5, getCenterPos(), 0);
        // Original recipePlayer was a username resolved in this world solely
        // for the crafting event. Native UUID attribution avoids name reuse;
        // logout/dimension changes skip the callback, NEVER the physical output.
        ServerPlayer player = crafter == null ? null : server.getServer().getPlayerList().getPlayer(crafter);
        if (player != null && player.level() == server && !player.isRemoved()
                && pedestal(getCenterPos()) == center) {
            // Original InventoryFake(recipeIngredients) is empty by this point.
            PlayerEvent.CRAFT_ITEM.invoker().craft(player, center.getItem(0), new SimpleContainer(0));
        }
    }

    private void scanSurroundings(ServerLevel server) {
        scanNeeded = false;
        if (refreshSourcesOnScan) {
            refreshSourcesOnScan = false;
            EssentiaHandler.refreshSources(this);
        }
        var surroundings = InfusionStructure.scan(server, worldPosition);
        pedestals = surroundings.pedestals().stream().map(BlockPos::immutable).toList();
        symmetry = surroundings.symmetry();
    }

    private TilePedestal pedestal(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(ThaumcraftInfusion.PEDESTAL.get())) return null;
        return level.getBlockEntity(pos) instanceof TilePedestal pedestal && !pedestal.isRemoved()
                && pedestal.getLevel() == level ? pedestal : null;
    }

    @Override public int getPendingEssentia(Aspect aspect) {
        return aspect != null && active && crafting && current() && level instanceof ServerLevel server
                && supportedOutput(server) && (recipeType == 0 || recipeXP == 0) ? recipeEssentia.getAmount(aspect) : 0;
    }

    @Override public void consumeInfusionEssentiaRaw(Aspect aspect) {
        EssentiaTransfers.requireMutation(this);
        recipeEssentia.reduce(aspect, 1); // Preserve the original zero-valued key. NO callbacks here.
    }

    @Override public void notifyInfusionEssentiaChanged() {
        if (current()) changed();
    }

    /** Event IDs/packet implementation are owned by root, not a parallel FX state machine. */
    public void sendEffect(int event, BlockPos target, int data) {
        if (current() && target != null) InfusionFxNetworking.send(this, event, target, data);
    }

    private boolean canMutate() {
        if (inCycle || EssentiaTransfers.isLocked(this) || !current()) return false;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Infusion changes require the server thread");
        return true;
    }

    private boolean current() {
        return level instanceof ServerLevel && !isRemoved() && level.hasChunkAt(worldPosition)
                && level.getBlockState(worldPosition).is(ThaumcraftInfusion.MATRIX.get())
                && level.getBlockEntity(worldPosition) == this;
    }

    private void changed() {
        if (!current()) return;
        setChanged();
        if (current()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override public void setRemoved() {
        EssentiaHandler.refreshSources(this);
        super.setRemoved();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("active", active);
        tag.putBoolean("crafting", crafting);
        tag.putInt("instability", instability);
        recipeEssentia.writeToNBT(tag);
        recipeCost.writeToNBT(tag, "RecipeCost");
        ListTag ingredients = new ListTag();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (InfusionComponentPredicate ingredient : recipeIngredients) {
            // Persist the selected native predicate itself, not a recipe id that
            // would repeat matching or replace a partly paid session after reload.
            ingredients.add(InfusionComponentPredicate.CODEC.encodeStart(ops, ingredient).getOrThrow());
        }
        tag.put("recipein", ingredients);
        tag.put("recipeinput", saveStack(recipeInput, registries));
        tag.put("recipeout", saveStack(recipeOutput, registries));
        tag.putString("rotype", recipeType == 1 ? "enchantment" : "@");
        if (recipeEnchantment != null) tag.putString("RecipeEnchantment", recipeEnchantment.toString());
        tag.putString("recipeOutputLabel", recipeOutputLabel);
        tag.putInt("recipeinst", recipeInstability);
        tag.putInt("recipetype", recipeType);
        tag.putInt("recipexp", recipeXP);
        tag.putString("recipeplayer", recipePlayerName);
        if (recipePlayer != null) tag.putUUID("RecipePlayerId", recipePlayer);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // Loading into a live object is not a way to rewind a transfer/cycle
        // from one of its synchronous notifications.
        if (inCycle || EssentiaTransfers.isLocked(this)) return;
        super.loadAdditional(tag, registries);
        active = tag.getBoolean("active");
        crafting = tag.getBoolean("crafting");
        instability = tag.getInt("instability");
        recipeEssentia = new AspectList();
        recipeEssentia.readFromNBT(tag);
        recipeCost = new AspectList();
        recipeCost.readFromNBT(tag, "RecipeCost");
        recipeIngredients.clear();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag ingredients = tag.getList("recipein", Tag.TAG_COMPOUND);
        for (int index = 0; index < ingredients.size(); index++) {
            // An explicit malformed requirement must never become a plain
            // ingredient on reload. Keep an invalid sentinel that pauses safely.
            recipeIngredients.add(InfusionComponentPredicate.CODEC.parse(ops, ingredients.getCompound(index))
                    .result().orElseGet(() -> new InfusionComponentPredicate(Ingredient.EMPTY)));
        }
        recipeInput = loadStack(tag.getCompound("recipeinput"), registries);
        recipeOutput = tag.getString("rotype").equals("@") ? loadStack(tag.getCompound("recipeout"), registries) : ItemStack.EMPTY;
        recipeOutputLabel = tag.getString("recipeOutputLabel");
        recipeInstability = tag.getInt("recipeinst");
        recipeType = tag.getInt("recipetype");
        recipeXP = tag.getInt("recipexp");
        recipeEnchantment = tag.contains("RecipeEnchantment", Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString("RecipeEnchantment")) : null;
        countDelay = 10; // Original transient delay resets on load, even in the middle of XP payment.
        recipePlayerName = tag.getString("recipeplayer");
        recipePlayer = tag.hasUUID("RecipePlayerId") ? tag.getUUID("RecipePlayerId") : null;
        count = 0;
        itemCount = 0;
        symmetry = 0;
        pedestals = List.of();
        ingredientTarget = null;
        scanNeeded = true;
        refreshSourcesOnScan = true;
        // Render-only fields are supplied by getUpdateTag, never by disk saves.
        if (tag.getBoolean("InfusionRenderState")) {
            itemCount = Math.clamp(tag.getInt("IngredientCountdown"), 0, 5);
            symmetry = tag.getInt("Symmetry");
            if (tag.contains("IngredientTarget", Tag.TAG_LONG)) ingredientTarget = BlockPos.of(tag.getLong("IngredientTarget"));
        }
    }

    private static CompoundTag saveStack(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!stack.isEmpty()) {
            // The ordinary native codec caps count at 99; preserve full output
            // and central snapshot counts separately, together with ALL components.
            tag.put("Stack", stack.copyWithCount(1).save(registries));
            tag.putInt("StoredCount", stack.getCount());
        }
        return tag;
    }

    private static ItemStack loadStack(CompoundTag tag, HolderLookup.Provider registries) {
        int amount = tag.getInt("StoredCount");
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.parse(registries, tag.getCompound("Stack")).orElse(ItemStack.EMPTY);
        if (!stack.isEmpty()) stack.setCount(amount);
        return stack;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putBoolean("InfusionRenderState", true);
        tag.putInt("IngredientCountdown", getIngredientCountdown());
        tag.putInt("Symmetry", symmetry);
        if (ingredientTarget != null) tag.putLong("IngredientTarget", ingredientTarget.asLong());
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
