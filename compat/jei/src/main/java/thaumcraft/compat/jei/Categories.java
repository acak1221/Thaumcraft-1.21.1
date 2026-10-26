package thaumcraft.compat.jei;

import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.client.lib.RecipeViews;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;

/** The five Thaumcraft JEI categories, laid out like the EMI pages of the same recipes. */
final class Categories {
    static final RecipeType<Pages.Arcane> ARCANE = RecipeType.create("thaumcraft", "arcane", Pages.Arcane.class);
    static final RecipeType<Pages.Crucible> CRUCIBLE = RecipeType.create("thaumcraft", "crucible", Pages.Crucible.class);
    static final RecipeType<RecipeViews.Infusion> INFUSION = RecipeType.create("thaumcraft", "infusion", RecipeViews.Infusion.class);
    static final RecipeType<RecipeViews.Infusion> INFUSION_ENCHANTMENT =
            RecipeType.create("thaumcraft", "infusion_enchantment", RecipeViews.Infusion.class);
    static final RecipeType<Pages.Combination> COMBINATION = RecipeType.create("thaumcraft", "aspect_combination", Pages.Combination.class);

    private Categories() {}

    /** The 3x3 grid, the result and the primal vis the wand pays. */
    static final class Arcane extends ThaumcraftCategory<Pages.Arcane> {
        Arcane(IGuiHelper gui) { super(ARCANE, "arcane", ThaumcraftArcane.ARCANE_WORKBENCH_ITEM.get(), gui, 118, 58 + 18 + 11); }

        @Override public void setRecipe(IRecipeLayoutBuilder builder, Pages.Arcane recipe, IFocusGroup focuses) {
            for (int i = 0; i < 9; i++) {
                Ingredient cell = recipe.grid().get(i);
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, i % 3 * 18 + 1, i / 3 * 18 + 1).setStandardSlotBackground();
                if (!cell.isEmpty()) slot.addIngredients(cell);
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 97, 19).setOutputSlotBackground().addItemStack(recipe.result());
            if (recipe.shapeless()) builder.setShapeless(97, 0);
            addAspects(builder, vis(recipe.vis()), 0, 58, 6);
        }

        @Override public void draw(Pages.Arcane recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            gui.getRecipeArrow().draw(graphics, 60, 18);
            super.draw(recipe, slots, graphics, mouseX, mouseY);
        }

        @Override String research(Pages.Arcane recipe) { return recipe.research(); }
        @Override int researchY(Pages.Arcane recipe) { return 58 + rows(recipe.vis().size(), 6) * 18 + 2; }
        @Override public ResourceLocation getRegistryName(Pages.Arcane recipe) { return recipe.id(); }
    }

    /** The catalyst thrown into the essentia the crucible already holds. */
    static final class Crucible extends ThaumcraftCategory<Pages.Crucible> {
        Crucible(IGuiHelper gui) { super(CRUCIBLE, "crucible", ThaumcraftAlchemy.CRUCIBLE_ITEM.get(), gui, 118, 30 + 36 + 11); }

        @Override public void setRecipe(IRecipeLayoutBuilder builder, Pages.Crucible recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 19, 5).setStandardSlotBackground().addIngredients(recipe.catalyst());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 87, 5).setOutputSlotBackground().addItemStack(recipe.result());
            addAspects(builder, aspects(recipe.aspects()), 0, 30, 6);
        }

        @Override public void draw(Pages.Crucible recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            gui.getRecipeArrow().draw(graphics, 50, 5);
            super.draw(recipe, slots, graphics, mouseX, mouseY);
        }

        @Override String research(Pages.Crucible recipe) { return recipe.research(); }
        @Override int researchY(Pages.Crucible recipe) { return 30 + rows(recipe.aspects().size(), 6) * 18 + 2; }
        @Override public ResourceLocation getRegistryName(Pages.Crucible recipe) { return recipe.id(); }
    }

    /**
     * The central item, the components around it and the essentia. The ring follows
     * {@code GuiResearchRecipe}: components start straight above the centre and step clockwise by
     * {@code 360 / count} degrees, an integer division as in the original.
     */
    static final class Infusion extends ThaumcraftCategory<RecipeViews.Infusion> {
        private static final int PER_ROW = 4;

        Infusion(IGuiHelper gui, boolean enchantment) {
            super(enchantment ? INFUSION_ENCHANTMENT : INFUSION, enchantment ? "infusion_enchantment" : "infusion",
                    enchantment ? ThaumcraftInfusion.PEDESTAL_ITEM.get() : ThaumcraftInfusion.MATRIX_ITEM.get(), gui, 92 + 4 + PER_ROW * 18, 130);
        }

        private static int radius(RecipeViews.Infusion recipe) { return recipe.components().size() <= 8 ? 28 : 36; }
        private static int ring(RecipeViews.Infusion recipe) { return (radius(recipe) + 10) * 2; }
        private static int detailTop(RecipeViews.Infusion recipe) { return 30 + rows(recipe.aspects().size(), PER_ROW) * 18 + 2; }
        private static int body(RecipeViews.Infusion recipe) {
            return Math.max(ring(recipe), detailTop(recipe) + recipe.details().size() * 10);
        }

        @Override public void setRecipe(IRecipeLayoutBuilder builder, RecipeViews.Infusion recipe, IFocusGroup focuses) {
            int radius = radius(recipe), center = radius + 10;
            builder.addSlot(RecipeIngredientRole.INPUT, center - 8, center - 8).setStandardSlotBackground().addIngredients(recipe.central());
            int count = recipe.components().size();
            int slice = count == 0 ? 0 : 360 / count;
            for (int i = 0; i < count; i++) {
                double angle = Math.toRadians(-90 + i * slice);
                int x = center + (int) Math.round(Math.cos(angle) * radius) - 8;
                int y = center + (int) Math.round(Math.sin(angle) * radius) - 8;
                builder.addSlot(RecipeIngredientRole.INPUT, x, y).setStandardSlotBackground().addIngredients(recipe.components().get(i));
            }
            int right = ring(recipe) + 4;
            builder.addSlot(RecipeIngredientRole.OUTPUT, right + 33, 5).setOutputSlotBackground().addItemStack(recipe.result());
            addAspects(builder, aspects(recipe.aspects()), right, 30, PER_ROW);
        }

        @Override public void draw(RecipeViews.Infusion recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            int right = ring(recipe) + 4;
            gui.getRecipeArrow().draw(graphics, right, 5);
            var font = Minecraft.getInstance().font;
            int line = detailTop(recipe);
            for (Component detail : recipe.details()) {
                graphics.drawString(font, trim(detail.getString(), getWidth() - right), right, line, TEXT, false);
                line += 10;
            }
            Component stability = Component.translatable("tc.inst").append(" ")
                    .append(Component.translatable("tc.inst." + Math.min(5, recipe.instability() / 2)));
            graphics.drawString(font, stability, 0, body(recipe) + 2, TEXT, false);
            super.draw(recipe, slots, graphics, mouseX, mouseY);
        }

        @Override String research(RecipeViews.Infusion recipe) { return recipe.research(); }
        @Override int researchY(RecipeViews.Infusion recipe) { return body(recipe) + 13; }
        @Override public ResourceLocation getRegistryName(RecipeViews.Infusion recipe) { return recipe.id(); }
    }

    /** A compound aspect and the two aspects the research table combines into it. */
    static final class Combination extends ThaumcraftCategory<Pages.Combination> {
        Combination(IGuiHelper gui) { super(COMBINATION, "aspect_combination", ThaumcraftContent.RESEARCH_TABLE_ITEM.get(), gui, 104, 18); }

        @Override public void setRecipe(IRecipeLayoutBuilder builder, Pages.Combination recipe, IFocusGroup focuses) {
            var components = recipe.aspect().getComponents();
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).addIngredient(AspectIngredient.TYPE, new AspectIngredient.Stack(components[0], 1));
            builder.addSlot(RecipeIngredientRole.INPUT, 37, 1).addIngredient(AspectIngredient.TYPE, new AspectIngredient.Stack(components[1], 1));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 87, 1).addIngredient(AspectIngredient.TYPE, new AspectIngredient.Stack(recipe.aspect(), 1));
        }

        @Override public void draw(Pages.Combination recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            gui.getRecipePlusSign().draw(graphics, 21, 4);
            gui.getRecipeArrow().draw(graphics, 58, 1);
        }

        @Override String research(Pages.Combination recipe) { return ""; }
        @Override int researchY(Pages.Combination recipe) { return 0; }
        @Override public ResourceLocation getRegistryName(Pages.Combination recipe) {
            return ResourceLocation.fromNamespaceAndPath("thaumcraft", "/aspect_combination/" + recipe.aspect().getTag());
        }
    }

    static List<ThaumcraftCategory<?>> all(IGuiHelper gui) {
        return List.of(new Arcane(gui), new Crucible(gui), new Infusion(gui, false), new Infusion(gui, true), new Combination(gui));
    }
}
