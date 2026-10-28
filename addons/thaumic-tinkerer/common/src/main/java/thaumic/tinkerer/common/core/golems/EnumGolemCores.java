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
package thaumic.tinkerer.common.core.golems;

/** The golem animation cores as the Golem Connector reports them (original names, ids and lang keys). */
public enum EnumGolemCores {

    CoreFill((byte) 0, "item.ItemGolemCore.0.name"),
    CoreEmpty((byte) 1, "item.ItemGolemCore.1.name"),
    CoreGather((byte) 2, "item.ItemGolemCore.2.name"),
    CoreHarvest((byte) 3, "item.ItemGolemCore.3.name"),
    CoreGuard((byte) 4, "item.ItemGolemCore.4.name"),
    CoreDecanting((byte) 5, "item.ItemGolemCore.5.name"),
    CoreAlcemy((byte) 6, "item.ItemGolemCore.6.name"),
    CoreChop((byte) 7, "item.ItemGolemCore.7.name"),
    CoreUse((byte) 8, "item.ItemGolemCore.8.name"),
    CoreButcher((byte) 9, "item.ItemGolemCore.9.name"),
    CoreSorting((byte) 10, "item.ItemGolemCore.10.name"),
    CoreFishing((byte) 11, "item.ItemGolemCore.11.name"),
    CoreBlank((byte) 100, "item.ItemGolemCore.100.name"),;

    private final String name;
    private final byte coreByte;

    EnumGolemCores(byte ch, String name) {
        coreByte = ch;
        this.name = name;
    }

    public static EnumGolemCores getFromByte(byte core) {
        for (EnumGolemCores value : values()) {
            if (value.coreByte == core) return value;
        }
        return null;
    }

    /** The translation key of the core (a key of Thaumcraft's own language file). */
    public String getName() {
        return name;
    }

    public byte getChar() {
        return coreByte;
    }
}
