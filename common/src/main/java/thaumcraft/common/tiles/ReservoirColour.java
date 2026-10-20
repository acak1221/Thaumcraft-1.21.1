package thaumcraft.common.tiles;

import java.util.List;
import thaumcraft.api.aspects.Aspect;

/**
 * The client colour of the essentia reservoir (original TileEssentiaReservoir.update, client branch). The tank shows one of the
 * stored aspects; every twentieth tick the shown aspect moves on to the next one of the stock and the colour walks one twentieth of
 * the way to that aspect's colour per tick, so it arrives exactly when the next aspect is picked. Without a shown aspect the colour
 * stays white. The stock is given in the original's order: the reservoir's insertion ordered aspect list.
 */
public final class ReservoirColour {
    /** The original picks a new displayed aspect every twentieth tick and moves the colour a twentieth of the way per tick. */
    public static final int INTERVAL = 20;
    private Aspect displayAspect;
    private float red = 1, green = 1, blue = 1;
    private float stepRed, stepGreen, stepBlue;

    public Aspect displayAspect() { return displayAspect; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }

    /**
     * The original's client branch; it only runs while the reservoir holds something. The colour is moved on the same tick the
     * aspect is picked, exactly as the original does.
     */
    public void update(List<Aspect> stored, int count) {
        if (count % INTERVAL == 0 && !stored.isEmpty()) {
            displayAspect = stored.get(count / INTERVAL % stored.size());
            int colour = displayAspect.getColor();
            float targetRed = (colour >> 16 & 255) / 255F;
            float targetGreen = (colour >> 8 & 255) / 255F;
            float targetBlue = (colour & 255) / 255F;
            stepRed = (red - targetRed) / INTERVAL;
            stepGreen = (green - targetGreen) / INTERVAL;
            stepBlue = (blue - targetBlue) / INTERVAL;
        }
        if (displayAspect == null) {
            red = green = blue = 1;
            stepRed = stepGreen = stepBlue = 0;
        } else {
            red -= stepRed;
            green -= stepGreen;
            blue -= stepBlue;
        }
    }
}
