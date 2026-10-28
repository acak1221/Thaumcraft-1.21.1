/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 18:40:22 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.button;

import java.util.List;
import net.minecraft.network.chat.Component;

/** A radio button: switching one on switches the linked ones off. */
public class GuiButtonMMRadio extends GuiButtonMM {

    private final List<GuiButtonMMRadio> linkedButtons;

    public GuiButtonMMRadio(int x, int y, boolean enabled, Component message, List<GuiButtonMMRadio> linkedButtons,
            Runnable onClick) {
        super(x, y, enabled, message, onClick);
        this.linkedButtons = linkedButtons;
    }

    /** The button was clicked: it is on now, and the others are off. */
    public void enableFromClick() {
        setEnabled(true);
        for (GuiButtonMMRadio button : linkedButtons) if (button != this) button.updateStatus(this);
    }

    public void updateStatus(GuiButtonMMRadio otherButton) {
        if (otherButton.isEnabled()) setEnabled(false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onPress() {
        enableFromClick();
        super.onPress();
    }
}
