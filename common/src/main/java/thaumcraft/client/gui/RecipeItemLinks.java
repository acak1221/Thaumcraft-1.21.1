package thaumcraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/** Ingredient hitboxes in GUI coordinates, captured from the very pose used to draw the item. */
final class RecipeItemLinks implements AutoCloseable {
    private static final ThreadLocal<RecipeItemLinks> CURRENT = new ThreadLocal<>();
    private final RecipeItemLinks parent;
    private final List<Hitbox> items = new ArrayList<>();

    private RecipeItemLinks() { parent = CURRENT.get(); CURRENT.set(this); }
    static RecipeItemLinks capture() { return new RecipeItemLinks(); }
    List<Hitbox> items() { return List.copyOf(items); }

    static void record(PoseStack pose, ItemStack stack, int x, int y) {
        RecipeItemLinks scope = CURRENT.get();
        if (scope == null || stack.isEmpty()) return;
        var matrix = pose.last().pose();
        Vector3f first = matrix.transformPosition(new Vector3f(x, y, 0));
        Vector3f second = matrix.transformPosition(new Vector3f(x + 16, y + 16, 0));
        scope.items.add(new Hitbox(stack.copy(), Math.min(first.x, second.x), Math.min(first.y, second.y),
                Math.max(first.x, second.x), Math.max(first.y, second.y)));
    }

    @Override public void close() { if (parent == null) CURRENT.remove(); else CURRENT.set(parent); }

    record Hitbox(ItemStack stack, float left, float top, float right, float bottom) {
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
    }
}
