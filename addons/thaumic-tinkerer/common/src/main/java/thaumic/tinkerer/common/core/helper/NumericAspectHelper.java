/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import java.util.ArrayList;
import thaumcraft.api.aspects.Aspect;

/**
 * The fixed numbering of the seventeen aspects a soul can be made of: the metadata of the 1.7.10 Mob Aspect item
 * (index into this list), and the order the soul aspects are listed in.
 *
 * <p>1.7.10 filled the list in {@code preInit} ({@code NumericAspectHelper.init()}). Here the list is filled when the
 * class is first used, so any group may use it without caring about the order of the registration phases; {@link #init()}
 * is kept for the call sites that still call it and is harmless to call twice (1.7.10 would have doubled the list).</p>
 */
public class NumericAspectHelper {

    public static ArrayList<NumericAspectHelper> values = new ArrayList<>();
    private static int nextNum = 0;
    public int num;
    private final Aspect aspect;

    NumericAspectHelper(Aspect aspect) {
        this.aspect = aspect;
        this.num = nextNum;
        nextNum++;
        values.add(this);
    }

    static {
        init();
    }

    public static synchronized void init() {
        if (!values.isEmpty()) return;

        new NumericAspectHelper(Aspect.WATER);
        new NumericAspectHelper(Aspect.MAN);
        new NumericAspectHelper(Aspect.AIR);
        new NumericAspectHelper(Aspect.FLIGHT);
        new NumericAspectHelper(Aspect.FIRE);
        new NumericAspectHelper(Aspect.MAGIC);
        new NumericAspectHelper(Aspect.UNDEAD);
        new NumericAspectHelper(Aspect.FLESH);
        new NumericAspectHelper(Aspect.BEAST);
        new NumericAspectHelper(Aspect.POISON);
        new NumericAspectHelper(Aspect.EARTH);
        new NumericAspectHelper(Aspect.ELDRITCH);
        new NumericAspectHelper(Aspect.TRAVEL);
        new NumericAspectHelper(Aspect.METAL);
        new NumericAspectHelper(Aspect.SLIME);
        new NumericAspectHelper(Aspect.CLOTH);
        new NumericAspectHelper(Aspect.CROP);
    }

    public static int getNumber(Aspect aspect) {
        for (NumericAspectHelper e : NumericAspectHelper.values) {
            if (e.getAspect().equals(aspect)) {
                return e.num;
            }
        }

        return -1;
    }

    public static Aspect getAspect(int i) {
        return NumericAspectHelper.values.get(i).getAspect();
    }

    public Aspect getAspect() {
        return aspect;
    }
}
