/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.core.golems;

import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/**
 * The single place that turns a golem entity into a {@link GolemAccess}. The default is the honest stub
 * ({@link UnavailableGolemAccess}) because {@code thaumcraft.api} has no golem view; when the API gets one (request A11)
 * this is the one line to change.
 */
public final class GolemAccessProvider {
    private static volatile Function<Entity, GolemAccess> provider = entity -> UnavailableGolemAccess.INSTANCE;

    private GolemAccessProvider() {}

    public static GolemAccess forEntity(Entity golem) {
        return golem == null || golem.isRemoved() ? UnavailableGolemAccess.INSTANCE : provider.apply(golem);
    }

    /** Installs the real access once the API has one (and for tests). */
    public static void setProvider(Function<Entity, GolemAccess> newProvider) {
        provider = newProvider == null ? entity -> UnavailableGolemAccess.INSTANCE : newProvider;
    }
}
