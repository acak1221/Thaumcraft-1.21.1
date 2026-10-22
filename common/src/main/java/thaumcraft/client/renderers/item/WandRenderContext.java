package thaumcraft.client.renderers.item;

import java.util.ArrayList;
import net.minecraft.world.entity.LivingEntity;

/** Retains the actual item wielder across the builtin renderer callback, including remote players. */
public final class WandRenderContext {
    private static final ThreadLocal<ArrayList<LivingEntity>> WIELDERS = ThreadLocal.withInitial(ArrayList::new);
    private WandRenderContext() {}
    public static void push(LivingEntity entity) { WIELDERS.get().add(entity); }
    public static void pop() {
        var stack = WIELDERS.get();
        stack.removeLast();
        if (stack.isEmpty()) WIELDERS.remove();
    }
    public static LivingEntity wielder() {
        var stack = WIELDERS.get();
        return stack.isEmpty() ? null : stack.getLast();
    }
}
