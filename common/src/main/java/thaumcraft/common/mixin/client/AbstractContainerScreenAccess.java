package thaumcraft.common.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The hovered slot of a native container screen.
 *
 * <p>{@code hoveredSlot} is {@code protected} in {@link AbstractContainerScreen} and has no
 * getter. The aspect row of the original is drawn after the tooltip of <b>every</b> container
 * screen ({@code GuiContainer}), so the mixin that covers the vanilla screens
 * ({@link ContainerScreenAspectRowMixin}) has to read it.</p>
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccess {
    @Accessor("hoveredSlot")
    Slot thaumcraft$hoveredSlot();

    /** The left edge of the screen's panel; it moves when the recipe book opens. */
    @Accessor("leftPos")
    int thaumcraft$leftPos();

    @Accessor("topPos")
    int thaumcraft$topPos();

    /** The slot under a point; inventory scanning needs it while an item is carried, when no slot counts as hovered for a tooltip. */
    @Invoker("findSlot")
    Slot thaumcraft$findSlot(double x, double y);
}
