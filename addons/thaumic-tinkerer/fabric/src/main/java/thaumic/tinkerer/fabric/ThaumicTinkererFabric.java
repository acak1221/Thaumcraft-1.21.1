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
package thaumic.tinkerer.fabric;

import net.fabricmc.api.ModInitializer;
import thaumcraft.api.IThaumcraftAddon;
import thaumic.tinkerer.common.ThaumicTinkerer;

/** Fabric {@code main} entrypoint and the {@code "thaumcraft"} entrypoint of the add-on. */
public final class ThaumicTinkererFabric implements ModInitializer, IThaumcraftAddon {
    @Override
    public void onInitialize() {
        ThaumicTinkerer.init();
    }

    @Override
    public void onThaumcraftReady() {
        ThaumicTinkerer.onThaumcraftReady();
    }
}
