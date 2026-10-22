package thaumcraft.common.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

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
}
