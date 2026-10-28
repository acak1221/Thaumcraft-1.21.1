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
package thaumic.tinkerer.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumic.tinkerer.common.content.ContentGroup;
import thaumic.tinkerer.common.content.ContentGroups;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.ResearchHelper;

/**
 * Thaumic Tinkerer for Thaumcraft 4 Reload on Minecraft 1.21.1.
 *
 * <p>The loader entrypoints (Fabric: {@code thaumic.tinkerer.fabric.ThaumicTinkererFabric}) call the three phases
 * below. The phases of the feature groups are described in {@link ContentGroup}.</p>
 */
public final class ThaumicTinkerer {
    public static final Logger LOG = LoggerFactory.getLogger("ThaumicTinkerer");

    private static boolean initialised;
    private static boolean ready;
    private static final List<String> FAILURES = new ArrayList<>();

    private ThaumicTinkerer() {}

    /** Mod initialisation, both sides: loads the config and registers every object. Safe to call twice. */
    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        ConfigHandler.loadConfig();
        for (ContentGroup group : ContentGroups.ALL) {
            guard(group.getClass().getSimpleName() + ".register", group::register);
        }
        TTRegistry.registerAll();
    }

    /**
     * Thaumcraft is ready ({@code IThaumcraftAddon}), both sides: the recipes and research of every registrable
     * object, then the extras of the groups, then the research queue. Runs once.
     */
    public static synchronized void onThaumcraftReady() {
        if (ready) return;
        ready = true;
        guard("research category", ResearchHelper::registerCategory);
        for (ITTinkererRegisterable registerable : TTRegistry.registerables()) {
            guard("recipe of " + registerable.getClass().getSimpleName(), () -> {
                ThaumicTinkererRecipe recipe = registerable.getRecipeItem();
                if (recipe != null) recipe.registerRecipe();
            });
        }
        for (ContentGroup group : ContentGroups.ALL) {
            guard(group.getClass().getSimpleName() + ".ready", group::ready);
        }
        for (ITTinkererRegisterable registerable : TTRegistry.registerables()) {
            guard("research of " + registerable.getClass().getSimpleName(),
                    () -> ResearchHelper.queue(registerable.getResearchItem()));
        }
        guard("research registration", ResearchHelper::flush);
    }

    /** Client entrypoint only: screens, renderers, colours, key mappings of every group. */
    public static synchronized void initClient() {
        for (ContentGroup group : ContentGroups.ALL) {
            guard(group.getClass().getSimpleName() + ".registerClient", group::registerClient);
        }
    }

    public static boolean isReady() {
        return ready;
    }

    /** What went wrong during the phases (each entry names the unit); empty when everything registered. */
    public static synchronized List<String> failures() {
        return Collections.unmodifiableList(new ArrayList<>(FAILURES));
    }

    /**
     * Runs one unit of registration. A broken unit is logged with its stack trace and recorded, and the rest of the
     * mod still loads; the GameTests assert that {@link #failures()} is empty, so a broken unit cannot go unnoticed.
     */
    private static void guard(String unit, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            FAILURES.add(unit + ": " + t);
            LOG.error("Thaumic Tinkerer: {} failed", unit, t);
        }
    }
}
