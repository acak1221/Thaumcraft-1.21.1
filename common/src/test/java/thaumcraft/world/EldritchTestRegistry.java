package thaumcraft.world;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * The native blocks of the eldritch family, registered before the vanilla bootstrap.
 *
 * <p>Minecraft 1.21.1 freezes the block registry in the vanilla bootstrap and a block instance
 * can only be created before that freeze. The test platform opens its session before it runs a
 * single test, so the family is registered here and the tests of the port can resolve the
 * native block of an original metadata.
 *
 * <p>The registration is a test-only mirror of the production registration of
 * {@link ThaumcraftEldritchBlocks}: the same blocks, the same ids and the same properties.
 */
public final class EldritchTestRegistry implements LauncherSessionListener {
    /** The six native blocks of the eldritch family, in the order of the hardness groups. */
    private static final List<Block> FAMILY = new ArrayList<>();
    /** The runed tablet, the key of the eldritch lock of the original. */
    private static Item tablet;

    @Override public void launcherSessionOpened(LauncherSession session) {
        register();
    }

    /** The native blocks of the eldritch family, empty when the registry could not be written. */
    public static List<Block> family() {
        return List.copyOf(FAMILY);
    }

    /** The runed tablet, or null when the item registry could not be written. */
    public static Item tablet() {
        return tablet;
    }

    /** Registers the family of the eldritch blocks and the runed tablet. */
    private static void register() {
        boolean bootstrapped = false;
        try {
            SharedConstants.tryDetectVersion();
            bootstrapped = raiseBootstrapFlag();
            for (int group = 0; group < PATHS.length; ++group) {
                Block block = new BlockEldritch(group, ThaumcraftEldritchBlocks.eldritchProperties(group));
                ResourceLocation key = ResourceLocation.fromNamespaceAndPath("thaumcraft", PATHS[group]);
                if (!BuiltInRegistries.BLOCK.containsKey(key)) {
                    Registry.register(BuiltInRegistries.BLOCK, key, block);
                }
                FAMILY.add(block);
            }
            ResourceLocation tabletKey = ResourceLocation.fromNamespaceAndPath("thaumcraft", "eldritch_tablet");
            if (!BuiltInRegistries.ITEM.containsKey(tabletKey)) {
                tablet = Registry.register(BuiltInRegistries.ITEM, tabletKey, new Item(new Item.Properties()));
            } else {
                tablet = BuiltInRegistries.ITEM.get(tabletKey);
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            // The registry cannot be written any more, the resolver cannot be checked.
            FAMILY.clear();
            tablet = null;
        } finally {
            lowerBootstrapFlag(bootstrapped);
        }
    }

    /** The registry ids of the six native blocks of the eldritch family. */
    private static final String[] PATHS = {"eldritch", "eldritch_crust", "eldritch_deco", "eldritch_doorway",
            "eldritch_spawner", "eldritch_crystal"};

    /**
     * Raises the bootstrap flag of the vanilla bootstrap, so that the registries can be written
     * before the vanilla bootstrap freezes them.
     *
     * @return true when the flag was raised by this call
     */
    private static boolean raiseBootstrapFlag() throws ReflectiveOperationException {
        Field flag = bootstrapFlag();
        if (flag.getBoolean(null)) return false;
        flag.setBoolean(null, true);
        return true;
    }

    /** Lowers the bootstrap flag again, so that the vanilla bootstrap can register its blocks. */
    private static void lowerBootstrapFlag(boolean raised) {
        if (!raised) return;
        try {
            bootstrapFlag().setBoolean(null, false);
        } catch (ReflectiveOperationException ignored) {
            // The vanilla bootstrap then refuses to run, which the test reports.
        }
    }

    /** The bootstrap flag of the vanilla bootstrap. */
    private static Field bootstrapFlag() throws ReflectiveOperationException {
        Field flag = net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");
        flag.setAccessible(true);
        return flag;
    }
}
