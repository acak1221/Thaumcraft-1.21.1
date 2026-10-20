package thaumcraft.common.container;

import java.util.Comparator;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftThaumatorium;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileThaumatorium;
import thaumcraft.api.aspects.Aspect;

/** One genuine catalyst slot and server-authoritative recipe toggles; packets never carry research claims. */
public final class ContainerThaumatorium extends AbstractContainerMenu {
    private static final List<Aspect> ASPECTS = Aspect.aspects.values().stream().sorted(Comparator.comparing(Aspect::getTag)).toList();
    private final TileThaumatorium tile;
    private final Player viewer;
    private final BlockPos pos;
    private final List<RecipeHolder<CrucibleRecipe>> recipes;
    private final ContainerData data;
    public ContainerThaumatorium(int id, Inventory inventory, TileThaumatorium tile) { this(id, inventory, tile, tile.getBlockPos(), tile); }
    public ContainerThaumatorium(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, new SimpleContainer(1), data.readBlockPos(), null); }
    private ContainerThaumatorium(int id, Inventory inventory, Container contents, BlockPos pos, TileThaumatorium tile) {
        super(ThaumcraftThaumatorium.MENU.get(), id); this.tile = tile; this.pos = pos.immutable(); viewer = inventory.player;
        recipes = viewer.level().getRecipeManager().getAllRecipesFor(ThaumcraftAlchemy.CRUCIBLE_RECIPE_TYPE.get()).stream()
                .sorted(Comparator.<RecipeHolder<CrucibleRecipe>>comparingInt(holder -> holder.value().sourceOrder()).thenComparing(holder -> holder.id().toString())).toList();
        int recipeFields = recipes.size() + 1;
        int size = recipeFields + ASPECTS.size() * 2;
        data = tile == null ? new SimpleContainerData(size) : new ContainerData() {
            @Override public int get(int index) {
                if (index == 0) return tile.isHeated() ? 1 : 0;
                if (index >= recipeFields) {
                    int field = index - recipeFields, amount = tile.containerContains(ASPECTS.get(field / 2));
                    return field % 2 == 0 ? amount & 65535 : amount >>> 16;
                }
                var recipe = recipes.get(index - 1);
                if (tile.isRecipeSelected(recipe.id())) return 2;
                return KnowledgeAccess.get((ServerPlayer) viewer).isResearchComplete(recipe.value().research()) && recipe.value().catalyst().test(tile.getItem(0)) ? 1 : 0;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return size; }
        };
        addSlot(new Slot(contents, 0, 48, 16));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        addDataSlots(data);
    }
    public List<RecipeHolder<CrucibleRecipe>> recipes() { return recipes; }
    public int stored(Aspect aspect) {
        int index = ASPECTS.indexOf(aspect);
        if (index < 0) return 0;
        int base = recipes.size() + 1 + index * 2;
        return (data.get(base) & 65535) | ((data.get(base + 1) & 65535) << 16);
    }
    public boolean available(int index) { return index >= 0 && index < recipes.size() && data.get(index + 1) > 0; }
    public boolean selected(int index) { return index >= 0 && index < recipes.size() && data.get(index + 1) == 2; }
    private boolean active(Player player) { return tile != null && player instanceof ServerPlayer server && server.server.isSameThread() && player == viewer && player.containerMenu == this && stillValid(player); }
    @Override public boolean stillValid(Player player) { return player == viewer && !player.isSpectator() && (tile != null ? tile.stillValid(player)
            : player.level().hasChunkAt(pos) && player.level().getBlockState(pos).is(ThaumcraftThaumatorium.THAUMATORIUM.get()) && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64); }
    @Override public boolean clickMenuButton(Player player, int button) {
        return active(player) && button >= 0 && button < recipes.size() && tile.toggleRecipe((ServerPlayer) player, recipes.get(button).id());
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) { if (tile == null || active(player)) super.clicked(slot, button, type, player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!active(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem(), original = source.copy();
        if (!(index == 0 ? moveItemStackTo(source, 1, 37, false) : moveItemStackTo(source, 0, 1, false))) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, source); return original;
    }
}
