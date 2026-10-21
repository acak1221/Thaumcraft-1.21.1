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
import thaumcraft.world.EldritchTestRegistry;

/**
 * The blocks of the port for the render shape and the block entity test.
 *
 * <p>Minecraft 1.21.1 asks the block registry for the intrusive holder of a block in the
 * constructor of the block (Minecraft 1.21.1 {@code net/minecraft/world/level/block/Block}:81) and
 * the frozen registry refuses it, so a block of the port can only be built before the vanilla
 * bootstrap freezes the registries. A block that is built and never registered leaves an
 * intrusive holder behind, and the frozen registry then refuses to start
 * ({@code MappedRegistry}: "Some intrusive holders were not registered"), so the blocks are
 * registered under the ids of the production registration.
 *
 * <p>The test platform opens its session before it runs a single test, so the blocks are built
 * here, the same way the eldritch family builds its blocks
 * ({@link EldritchTestRegistry}).
 */
public final class BlockVisibilityTestRegistry implements LauncherSessionListener {
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
            cosmetic = new BlockCosmeticSolid[] {
                    register("cosmetic_solid", BlockCosmeticSolid.group(0, properties())),
                    register("cosmetic_solid_tough", BlockCosmeticSolid.group(1, properties())),
                    register("cosmetic_solid_thaumium", BlockCosmeticSolid.group(2, properties()))};
            eldritch = eldritchFamily();
            airy = new BlockAiry[] {
                    register("airy", BlockAiry.group(0, properties())),
                    register("airy_node", BlockAiry.group(1, properties())),
                    register("airy_hole", BlockAiry.group(2, properties())),
                    register("airy_unbreakable", BlockAiry.group(3, properties()))};
            amber = register("cosmetic_opaque",
                    new BlockCosmeticOpaque(BlockCosmeticOpaque.GROUP_AMBER, properties()));
            warded = register("cosmetic_opaque_warded",
                    new BlockCosmeticOpaque(BlockCosmeticOpaque.GROUP_WARDED, properties()));
            nothing = register("eldritch_nothing", new BlockEldritchNothing(properties()));
            portal = register("eldritch_portal", new BlockEldritchPortal(properties()));
            mirror = register("mirror", new BlockMirror(properties()));
            metalDevice = register("metal_device", new BlockMetalDevice(properties()));
            stoneDevice = register("stone_device", new BlockStoneDevice(properties()));
            woodenDevice = register("wooden_device", new BlockWoodenDevice(properties()));
            magicBox = register("magic_box", new BlockMagicBox(properties()));
            arcaneDoor = register("arcane_door", new BlockArcaneDoor(properties()));
            plate = register("arcane_pressure_plate", new BlockArcanePressurePlate(properties()));
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
     * The six blocks of the eldritch family, the ones of the test registry of the family when it
     * has built them, the blocks of this registry otherwise.
     */
    private static BlockEldritch[] eldritchFamily() {
        java.util.List<Block> family = EldritchTestRegistry.family();
        if (family.size() == 6) {
            BlockEldritch[] blocks = new BlockEldritch[family.size()];
            for (int index = 0; index < blocks.length; ++index) {
                blocks[index] = (BlockEldritch) family.get(index);
            }
            return blocks;
        }
        return new BlockEldritch[] {
                register("eldritch", BlockEldritch.group(0, properties())),
                register("eldritch_crust", BlockEldritch.group(1, properties())),
                register("eldritch_deco", BlockEldritch.group(2, properties())),
                register("eldritch_doorway", BlockEldritch.group(3, properties())),
                register("eldritch_spawner", BlockEldritch.group(4, properties())),
                register("eldritch_crystal", BlockEldritch.group(5, properties()))};
    }

    /**
     * Registers the block under the id of the production registration, so that the intrusive
     * holder of the block is bound and the frozen registry accepts it.
     */
    private static <T extends Block> T register(String id, T block) {
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath("thaumcraft", id);
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
