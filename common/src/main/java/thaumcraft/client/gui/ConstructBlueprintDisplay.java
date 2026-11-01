package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.OreDictionaryNames;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;

/**
 * The six remaining original compound pages, not recipes that manufacture a formed machine, and the
 * {@code ResearchPage(List)} compound pages of add-ons, drawn with the same original geometry.
 */
public final class ConstructBlueprintDisplay {
    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchbook_overlay.png");
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/empty.png");
    private static final ResourceLocation LAVA = ResourceLocation.withDefaultNamespace("block/lava_still");

    /** ITEM is a cell of an add-on page: its entry of the page's list is drawn. */
    enum Part { NONE, WAND, BOOKSHELF, TABLE, INKWELL, CAULDRON, NETHER_BRICKS, OBSIDIAN, AIR, LAVA, IRON_BARS, WOODEN_SLAB, GLASS, NODE, ITEM }

    /** Original ConfigRecipes:83-95 stores cells top down, then decreasing Z and X; {@code entries} are an add-on page's list cells. */
    record Blueprint(int width, int height, int depth, List<Part> cells, Map<String, Integer> vis, List<Object> entries) {
        Blueprint(int width, int height, int depth, List<Part> cells, Map<String, Integer> vis) { this(width, height, depth, cells, vis, List.of()); }
        Blueprint {
            cells = List.copyOf(cells);
            vis = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(vis));
            entries = java.util.Collections.unmodifiableList(new ArrayList<>(entries));
            if (cells.size() != width * height * depth) throw new IllegalArgumentException("Invalid original construct volume");
        }
        Part at(int layer, int x, int z) { return cells.get(index(layer, x, z)); }
        Object entry(int layer, int x, int z) { return entries.isEmpty() ? null : entries.get(index(layer, x, z)); }
        private int index(int layer, int x, int z) { return layer * width * depth + (depth - 1 - z) * width + width - 1 - x; }
    }

    private static final Map<String, Blueprint> BLUEPRINTS = blueprints();
    private ConstructBlueprintDisplay() {}

    public static boolean supports(String key) { return key != null && BLUEPRINTS.containsKey(key); }
    static Blueprint blueprint(String key) { return BLUEPRINTS.get(key); }

    /**
     * An add-on's {@code ResearchPage(List)}: original GuiResearchRecipe.drawCompoundCraftingPage reads
     * {@code [AspectList vis, Integer width, Integer height, Integer depth, List cells]}, the cells in the order of
     * {@link Blueprint}. A null cell draws nothing, an empty stack or air is the original's "Empty Space" block
     * ({@code blockHole} metadata 15); a missing cell is null. Null for a list of another shape.
     */
    static Blueprint addon(Object recipe) {
        if (!(recipe instanceof List<?> list) || list.size() < 5 || !(list.get(4) instanceof List<?> entries)) return null;
        if (list.get(0) != null && !(list.get(0) instanceof AspectList)) return null;
        if (!(list.get(1) instanceof Integer width) || !(list.get(2) instanceof Integer height) || !(list.get(3) instanceof Integer depth)) return null;
        if (width < 1 || height < 1 || depth < 1 || width > 32 || height > 32 || depth > 32 || width * height * depth > 4096) return null;
        var vis = new LinkedHashMap<String, Integer>();
        if (list.get(0) instanceof AspectList aspects)
            for (Aspect aspect : aspects.getAspects()) if (aspect != null && aspects.getAmount(aspect) > 0) vis.put(aspect.getTag(), aspects.getAmount(aspect));
        var cells = new ArrayList<Part>();
        var items = new ArrayList<Object>();
        for (int cell = 0; cell < width * height * depth; cell++) {
            Object entry = cell < entries.size() ? entries.get(cell) : null;
            cells.add(entry == null ? Part.NONE : emptySpace(entry) ? Part.AIR : Part.ITEM);
            items.add(entry);
        }
        return new Blueprint(width, height, depth, cells, vis, items);
    }

    private static boolean emptySpace(Object entry) {
        if (entry instanceof ItemStack stack) return stack.isEmpty();
        return entry instanceof Block block ? block.defaultBlockState().isAir() : entry instanceof ItemLike item && item.asItem() == Items.AIR;
    }

    private static Map<String, Blueprint> blueprints() {
        var all = new LinkedHashMap<String, Blueprint>();
        all.put("Thaumonomicon", new Blueprint(1, 2, 1, List.of(Part.WAND, Part.BOOKSHELF), Map.of()));
        all.put("ArcTable", new Blueprint(1, 2, 1, List.of(Part.WAND, Part.TABLE), Map.of()));
        all.put("ResTable", new Blueprint(1, 2, 2, List.of(Part.NONE, Part.INKWELL, Part.TABLE, Part.TABLE), Map.of()));
        all.put("Crucible", new Blueprint(1, 2, 1, List.of(Part.WAND, Part.CAULDRON), Map.of()));
        // Source :89, including the one west-facing iron-bar example, top air and middle lava.
        all.put("InfernalFurnace", new Blueprint(3, 3, 3, List.of(
                Part.NETHER_BRICKS, Part.OBSIDIAN, Part.NETHER_BRICKS, Part.OBSIDIAN, Part.AIR, Part.OBSIDIAN, Part.NETHER_BRICKS, Part.OBSIDIAN, Part.NETHER_BRICKS,
                Part.NETHER_BRICKS, Part.OBSIDIAN, Part.NETHER_BRICKS, Part.OBSIDIAN, Part.LAVA, Part.IRON_BARS, Part.NETHER_BRICKS, Part.OBSIDIAN, Part.NETHER_BRICKS,
                Part.NETHER_BRICKS, Part.OBSIDIAN, Part.NETHER_BRICKS, Part.OBSIDIAN, Part.OBSIDIAN, Part.OBSIDIAN, Part.NETHER_BRICKS, Part.OBSIDIAN, Part.NETHER_BRICKS),
                costs("ignis", 50, "terra", 50)));
        var jar = new ArrayList<Part>();
        for (int cell = 0; cell < 36; cell++) jar.add(cell < 9 ? Part.WOODEN_SLAB : cell == 22 ? Part.NODE : Part.GLASS);
        all.put("NodeJar", new Blueprint(3, 4, 3, jar, costs("ignis", 70, "terra", 70, "aer", 70, "aqua", 70, "ordo", 70, "perditio", 70)));
        return Map.copyOf(all);
    }

    private static Map<String, Integer> costs(Object... pairs) {
        var costs = new LinkedHashMap<String, Integer>();
        for (int pair = 0; pair < pairs.length; pair += 2) costs.put((String) pairs[pair], (Integer) pairs[pair + 1]);
        return costs;
    }

    /** Source drawCompoundCraftingPage: the original isometric grid and its 0.8 scale for four layers. */
    static CellPosition position(Blueprint blueprint, int layer, int x, int z) {
        float shrink = blueprint.height() > 3 ? (blueprint.height() - 3) * .2F : 0;
        int xoff = 64 - (blueprint.width() * 16 + blueprint.depth() * 16) / 2;
        float originX = xoff * (1 + shrink), originY = 108 - blueprint.height() * 25 * (1 - shrink);
        return new CellPosition(originX + (x * 16 + z * 16) * (1 - shrink),
                originY + (-x * 8 + z * 8 + layer * 50) * (1 - shrink), 1 - shrink, 60 - layer * 10);
    }

    record CellPosition(float x, float y, float scale, int depth) {}

    public static ThaumcraftRecipeDisplay.Hover render(String key, GuiGraphics graphics, Font font,
                                                        int x, int y, int width, int height, int mouseX, int mouseY, long cycle) {
        return render(BLUEPRINTS.get(key), graphics, font, x, y, width, height, mouseX, mouseY, cycle);
    }

    static ThaumcraftRecipeDisplay.Hover render(Blueprint blueprint, GuiGraphics graphics, Font font,
                                                int x, int y, int width, int height, int mouseX, int mouseY, long cycle) {
        if (blueprint == null) return null;
        float scale = Math.min(width / 128F, height / 206F);
        if (scale <= 0) return null;
        float originX = x + (width - 128 * scale) / 2 + 8 * scale, originY = y + (height - 206 * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        ThaumcraftRecipeDisplay.Hover hover = null;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(originX, originY, 0);
            graphics.pose().scale(scale, scale, 1);
            Component title = Component.translatable("recipe.type.construct");
            graphics.drawString(font, title, 56 - font.width(title) / 2, 0, 0xFF505050, false);
            float shrink = blueprint.height() > 3 ? (blueprint.height() - 3) * .2F : 0;
            int xoff = 64 - (blueprint.width() * 16 + blueprint.depth() * 16) / 2;
            float bodyX = xoff * (1 + shrink), bodyY = 108 - blueprint.height() * 25 * (1 - shrink);
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(bodyX, bodyY, 0);
                graphics.pose().scale(1 - shrink, 1 - shrink, 1);
                int plateY = -119 + Math.max(3 - blueprint.width(), 3 - blueprint.depth()) * 8
                        + blueprint.width() * 4 + blueprint.depth() * 4 + blueprint.height() * 50;
                graphics.flush(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
                graphics.setColor(1, 1, 1, .5F);
                graphics.blit(OVERLAY, -8 - xoff, plateY, 128, 88, 0, 144, 128, 88, 512, 512);
                graphics.setColor(1, 1, 1, 1);
            } finally { graphics.pose().popPose(); }
            for (int layer = 0; layer < blueprint.height(); layer++) {
                for (int z = blueprint.depth() - 1; z >= 0; z--) for (int cellX = blueprint.width() - 1; cellX >= 0; cellX--) {
                    Part part = blueprint.at(layer, cellX, z);
                    if (part == Part.NONE) continue;
                    CellPosition cell = position(blueprint, layer, cellX, z);
                    ItemStack stack = part == Part.ITEM ? entry(blueprint.entry(layer, cellX, z), cycle) : item(part, cycle);
                    if (part == Part.ITEM && stack.isEmpty()) continue;
                    graphics.pose().pushPose();
                    try {
                        graphics.pose().translate(cell.x(), cell.y(), cell.depth() * cell.scale());
                        graphics.pose().scale(cell.scale(), cell.scale(), cell.scale());
                        if (!stack.isEmpty()) {
                            graphics.renderItem(stack, 0, 0);
                            graphics.renderItemDecorations(font, stack, 0, 0);
                            RecipeItemLinks.record(graphics.pose(), stack, 0, 0);
                        } else if (part == Part.AIR) {
                            graphics.flush(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
                            graphics.blit(EMPTY, 0, 0, 0, 0, 16, 16, 16, 16);
                        } else if (part == Part.LAVA) {
                            var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(LAVA);
                            graphics.blit(0, 0, 0, 16, 16, sprite);
                        }
                    } finally { graphics.pose().popPose(); }
                    if (mx >= cell.x() && mx < cell.x() + 16 * cell.scale() && my >= cell.y() && my < cell.y() + 16 * cell.scale()) {
                        hover = !stack.isEmpty() ? new ThaumcraftRecipeDisplay.Hover(stack, List.of())
                                : new ThaumcraftRecipeDisplay.Hover(ItemStack.EMPTY, List.of(Component.translatable(
                                        part == Part.AIR ? "tc.port.book_recipe.required_air" : "block.minecraft.lava")));
                    }
                }
            }
            var tags = blueprint.vis().keySet().stream().sorted(Comparator.<String>comparingInt(blueprint.vis()::get).reversed()).toList();
            if (!tags.isEmpty()) {
                graphics.flush(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
                graphics.setColor(1, 1, 1, .4F);
                graphics.blit(OVERLAY, 0, 174, 24, 24, 136, 152, 24, 24, 512, 512);
                graphics.setColor(1, 1, 1, 1);
            }
            for (int i = 0; i < tags.size(); i++) {
                Aspect aspect = Aspect.getAspect(tags.get(i));
                int sx = 14 + i * 18 + (5 - tags.size()) * 8;
                ThaumcraftRecipeDisplay.drawAspectTag(graphics, font, aspect, blueprint.vis().get(aspect.getTag()), 0, sx, 182, 1);
                if (mx >= sx && mx < sx + 16 && my >= 182 && my < 198) hover = new ThaumcraftRecipeDisplay.Hover(ItemStack.EMPTY,
                        List.of(Component.literal(aspect.getName()), Component.translatable("tc.aspect." + aspect.getTag())));
            }
        } finally { graphics.pose().popPose(); }
        return hover;
    }

    private static ItemStack item(Part part, long cycle) {
        return switch (part) {
            case WAND -> new ItemStack(ThaumcraftContent.WAND.get());
            case BOOKSHELF -> new ItemStack(Items.BOOKSHELF);
            case TABLE -> new ItemStack(ThaumcraftContent.TABLE_ITEM.get());
            case INKWELL -> new ItemStack(ThaumcraftContent.INKWELL.get());
            case CAULDRON -> new ItemStack(Items.CAULDRON);
            case NETHER_BRICKS -> new ItemStack(Items.NETHER_BRICKS);
            case OBSIDIAN -> new ItemStack(Items.OBSIDIAN);
            case IRON_BARS -> new ItemStack(Items.IRON_BARS);
            case GLASS -> new ItemStack(Items.GLASS);
            // Original ItemNodeRenderer accepts metadata0 and5 but deliberately draws the same normal node fixture.
            case NODE -> new ItemStack(ThaumcraftContent.AURA_NODE.get().asItem());
            case WOODEN_SLAB -> woodenSlab(cycle);
            default -> ItemStack.EMPTY;
        };
    }

    /** Original InventoryUtils.cycleItemStack: an ore dictionary name, a tag or a list shows one choice per second. */
    private static ItemStack entry(Object entry, long cycle) {
        return switch (entry) {
            case ItemStack stack -> stack.copyWithCount(1);
            case ItemLike item -> new ItemStack(item);
            case String name -> choice(tagItems(OreDictionaryNames.itemTag(name)), cycle);
            case TagKey<?> tag -> tag.isFor(Registries.ITEM) ? choice(tagItems(TagKey.create(Registries.ITEM, tag.location())), cycle) : ItemStack.EMPTY;
            case Ingredient ingredient -> choice(List.of(ingredient.getItems()), cycle);
            case List<?> choices -> choices.isEmpty() ? ItemStack.EMPTY : entry(choices.get((int) Math.floorMod(cycle, (long) choices.size())), cycle);
            case null, default -> ItemStack.EMPTY;
        };
    }

    private static List<ItemStack> tagItems(TagKey<Item> tag) {
        return tag == null ? List.of() : BuiltInRegistries.ITEM.getTag(tag).stream().flatMap(named -> named.stream()).map(ItemStack::new).toList();
    }

    private static ItemStack choice(List<ItemStack> stacks, long cycle) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft == null ? null : minecraft.level;
        var choices = stacks.stream().filter(stack -> !stack.isEmpty() && (level == null || stack.isItemEnabled(level.enabledFeatures()))).toList();
        return choices.isEmpty() ? ItemStack.EMPTY : choices.get((int) Math.floorMod(cycle, (long) choices.size())).copyWithCount(1);
    }

    private static ItemStack woodenSlab(long cycle) {
        var level = Minecraft.getInstance().level;
        var choices = BuiltInRegistries.ITEM.getTag(ItemTags.WOODEN_SLABS).stream().flatMap(tag -> tag.stream())
                .map(ItemStack::new).filter(stack -> level == null || stack.isItemEnabled(level.enabledFeatures())).toList();
        return choices.isEmpty() ? ItemStack.EMPTY : choices.get((int) Math.floorMod(cycle, (long) choices.size())).copy();
    }
}
