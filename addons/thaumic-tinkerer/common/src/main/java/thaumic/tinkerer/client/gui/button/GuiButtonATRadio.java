/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 16:33:43 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.button;

import java.util.List;
import java.util.function.Consumer;

/** A {@link GuiButtonAT} of a group of which one is on. */
public class GuiButtonATRadio extends GuiButtonAT implements IRadioButton {

    List<IRadioButton> linkedButtons;

    public GuiButtonATRadio(int x, int y, boolean enabled, List<IRadioButton> linkedButtons, Consumer<GuiButtonAT> onClick) {
        super(x, y, enabled, onClick);
        this.linkedButtons = linkedButtons;
    }

    @Override
    public void enableFromClick() {
        setEnabled(true);
        for (IRadioButton button : linkedButtons) if (button != this) button.updateStatus(this);
    }

    @Override
    public void updateStatus(IRadioButton otherButton) {
        if (otherButton.isEnabled()) setEnabled(false);
    }

    @Override
    public boolean isEnabled() {
        return buttonEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.buttonEnabled = enabled;
    }
}
