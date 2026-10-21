package thaumcraft.blocks;

import java.lang.reflect.Field;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import thaumcraft.common.blocks.BlockAiry;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.blocks.BlockArcanePressurePlate;
import thaumcraft.common.blocks.BlockCosmeticOpaque;
import thaumcraft.common.blocks.BlockCosmeticSolid;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.blocks.BlockEldritchNothing;
import thaumcraft.common.blocks.BlockEldritchPortal;
import thaumcraft.common.blocks.BlockMagicBox;
import thaumcraft.common.blocks.BlockMetalDevice;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.blocks.BlockStoneDevice;
import thaumcraft.common.blocks.BlockWoodenDevice;

/**
 * The blocks of the port for the render shape and the block entity test.
 *
 * <p>Minecraft 1.21.1 asks the block registry for the intrusive holder of a block in the
 * constructor of the block (Minecraft 1.21.1 {@code net/minecraft/world/level/block/Block}:81) and
 * the frozen registry refuses it, so a block of the port can only be built before the vanilla
 * bootstrap freezes the registries. The test platform opens its session before it runs a single
 * test, so the blocks are built here, the same way the eldritch family builds its blocks
 * ({@code thaumcraft.world.EldritchTestRegistry}).</p>
 *
 * <p><b>Every block of the test is registered under a test only id.</b> A block that is built and
 * never registered keeps its intrusive holder in the block registry, and the freeze of the
 * registry then refuses the whole test suite with {@code Some intrusive holders were not
 * registered} (Minecraft 1.21.1 {@code net/minecraft/core/MappedRegistry}:342-347). The
 * registration binds the holder of the block and removes it from the map of the unregistered
 * holders, so the vanilla bootstrap can freeze the block registry (the same repair the eldritch
 * family registry already does).</p>
 */
public final class BlockVisibilityTestRegistry implements LauncherSessionListener {
    /** The namespace of the test only ids of the block registry. */
    private static final String TEST_NAMESPACE = "thaumcraft_test";

    /** The three groups of the cosmetic solid family. */
    private static BlockCosmeticSolid[] cosmetic;
    /** The six groups of the eldritch family. */
    private static BlockEldritch[] eldritch;
    /** The four groups of the airy family. */
    private static BlockAiry[] airy;
    /** The amber block and the amber brick. */
    private static BlockCosmeticOpaque amber;
    /** The warded glass. */
    private static BlockCosmeticOpaque warded;
    /** The eldritch nothing. */
    private static BlockEldritchNothing nothing;
    /** The eldritch gate. */
    private static BlockEldritchPortal portal;
    /** The mirror. */
    private static BlockMirror mirror;
    /** The metal device. */
    private static BlockMetalDevice metalDevice;
    /** The stone device. */
    private static BlockStoneDevice stoneDevice;
    /** The wooden device. */
    private static BlockWoodenDevice woodenDevice;
    /** The magic box. */
    private static BlockMagicBox magicBox;
    /** The arcane door. */
    private static BlockArcaneDoor arcaneDoor;
    /** The arcane pressure plate. */
    private static BlockArcanePressurePlate plate;

    @Override public void launcherSessionOpened(LauncherSession session) {
        build();
    }

    /** The three groups of the cosmetic solid family, null when they could not be built. */
    public static BlockCosmeticSolid[] cosmetic() { return cosmetic; }
    /** The six groups of the eldritch family, null when they could not be built. */
    public static BlockEldritch[] eldritch() { return eldritch; }
    /** The four groups of the airy family, null when they could not be built. */
    public static BlockAiry[] airy() { return airy; }
    /** The amber block and the amber brick, null when they could not be built. */
    public static BlockCosmeticOpaque amber() { return amber; }
    /** The warded glass, null when it could not be built. */
    public static BlockCosmeticOpaque warded() { return warded; }
    /** The eldritch nothing, null when it could not be built. */
    public static BlockEldritchNothing nothing() { return nothing; }
    /** The eldritch gate, null when it could not be built. */
    public static BlockEldritchPortal portal() { return portal; }
    /** The mirror, null when it could not be built. */
    public static BlockMirror mirror() { return mirror; }
    /** The metal device, null when it could not be built. */
    public static BlockMetalDevice metalDevice() { return metalDevice; }
    /** The stone device, null when it could not be built. */
    public static BlockStoneDevice stoneDevice() { return stoneDevice; }
    /** The wooden device, null when it could not be built. */
    public static BlockWoodenDevice woodenDevice() { return woodenDevice; }
    /** The magic box, null when it could not be built. */
    public static BlockMagicBox magicBox() { return magicBox; }
    /** The arcane door, null when it could not be built. */
    public static BlockArcaneDoor arcaneDoor() { return arcaneDoor; }
    /** The arcane pressure plate, null when it could not be built. */
    public static BlockArcanePressurePlate plate() { return plate; }

    /** Builds the blocks of the port before the vanilla bootstrap freezes the registries. */
    private static void build() {
        boolean raised = raiseBootstrapFlag();
        try {
            cosmetic = new BlockCosmeticSolid[] {BlockCosmeticSolid.group(0, properties()),
                    BlockCosmeticSolid.group(1, properties()), BlockCosmeticSolid.group(2, properties())};
            eldritch = new BlockEldritch[] {BlockEldritch.group(0, properties()),
                    BlockEldritch.group(1, properties()), BlockEldritch.group(2, properties()),
                    BlockEldritch.group(3, properties()), BlockEldritch.group(4, properties()),
                    BlockEldritch.group(5, properties())};
            airy = new BlockAiry[] {BlockAiry.group(0, properties()), BlockAiry.group(1, properties()),
                    BlockAiry.group(2, properties()), BlockAiry.group(3, properties())};
            amber = new BlockCosmeticOpaque(BlockCosmeticOpaque.GROUP_AMBER, properties());
            warded = new BlockCosmeticOpaque(BlockCosmeticOpaque.GROUP_WARDED, properties());
            nothing = new BlockEldritchNothing(properties());
            portal = new BlockEldritchPortal(properties());
            mirror = new BlockMirror(properties());
            metalDevice = new BlockMetalDevice(properties());
            stoneDevice = new BlockStoneDevice(properties());
            woodenDevice = new BlockWoodenDevice(properties());
            magicBox = new BlockMagicBox(properties());
            arcaneDoor = new BlockArcaneDoor(properties());
            plate = new BlockArcanePressurePlate(properties());
            registerAll();
        } catch (RuntimeException | LinkageError failure) {
            // The registry cannot be written any more, the resolver cannot be checked.
            cosmetic = null;
            eldritch = null;
            airy = null;
            amber = null;
            warded = null;
            nothing = null;
            portal = null;
            mirror = null;
            metalDevice = null;
            stoneDevice = null;
            woodenDevice = null;
            magicBox = null;
            arcaneDoor = null;
            plate = null;
        } finally {
            lowerBootstrapFlag(raised);
        }
    }

    /**
     * Registers every block of the test under a test only id.
     *
     * <p>A block that is never registered keeps its intrusive holder in the block registry and the
     * freeze of the registry refuses the whole test suite
     * (Minecraft 1.21.1 {@code net/minecraft/core/MappedRegistry}:342-347).</p>
     */
    private static void registerAll() {
        for (int group = 0; group < cosmetic.length; ++group) {
            register("cosmetic_" + group, cosmetic[group]);
        }
        for (int group = 0; group < eldritch.length; ++group) {
            register("eldritch_" + group, eldritch[group]);
        }
        for (int group = 0; group < airy.length; ++group) {
            register("airy_" + group, airy[group]);
        }
        register("cosmetic_opaque_amber", amber);
        register("cosmetic_opaque_warded", warded);
        register("eldritch_nothing", nothing);
        register("eldritch_portal", portal);
        register("mirror", mirror);
        register("metal_device", metalDevice);
        register("stone_device", stoneDevice);
        register("wooden_device", woodenDevice);
        register("magic_box", magicBox);
        register("arcane_door", arcaneDoor);
        register("arcane_pressure_plate", plate);
    }

    /**
     * Registers one block of the test under a test only id of the block registry.
     *
     * @param path  the path of the test only id
     * @param block the block of the test
     * @return the registered block
     */
    private static <T extends Block> T register(String path, T block) {
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath(TEST_NAMESPACE, path);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) {
            Registry.register(BuiltInRegistries.BLOCK, key, block);
        }
        return block;
    }

    /** The properties of a block of the port. */
    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of();
    }

    /**
     * Raises the bootstrap flag of the vanilla bootstrap, so that the registries can be written
     * before the vanilla bootstrap freezes them.
     *
     * @return true when the flag was raised by this call
     */
    private static boolean raiseBootstrapFlag() {
        try {
            Field flag = bootstrapFlag();
            if (flag.getBoolean(null)) return false;
            flag.setBoolean(null, true);
            return true;
        } catch (ReflectiveOperationException failure) {
            return false;
        }
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
