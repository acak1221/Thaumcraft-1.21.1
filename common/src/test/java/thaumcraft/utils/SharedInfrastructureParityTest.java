package thaumcraft.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.lib.RefGui;
import thaumcraft.common.lib.utils.Utils;

/**
 * The shared runtime substrate of the original TC4 4.2.3.5: the numeric GUI ids of
 * {@code thaumcraft.common.lib.RefGui}, the effective server defaults of the original
 * {@code thaumcraft.common.config.Config} and the pure helpers of
 * {@code thaumcraft.common.lib.utils.Utils}.
 *
 * <p>Every expectation is the value of the original oracle files
 * ({@code OpenTC4/src/main/java/thaumcraft/common/...}).</p>
 *
 * <p>These tests are AUTHORED BUT NOT EXECUTED: the wave that produced them may not run Gradle
 * (the shared Loom cache is owned by the leader).</p>
 */
class SharedInfrastructureParityTest {

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ------------------------------------------------------------------- the GUI ids

    /**
     * Original {@code thaumcraft.common.lib.RefGui} (oracle {@code RefGui.java:3-21}): the seventeen
     * numeric window ids of the original, including the gaps (4, 6, 7 and 14 are unused).
     */
    @Test void theGuiIdsAreTheOriginalOnes() {
        assertEquals(0, RefGui.GUI_GOLEM);
        assertEquals(1, RefGui.GUI_PECH);
        assertEquals(2, RefGui.GUI_TRUNK);
        assertEquals(3, RefGui.GUI_THAUMATORIUM);
        assertEquals(5, RefGui.GUI_FOCUS_POUCH);
        assertEquals(8, RefGui.GUI_DECON_TABLE);
        assertEquals(9, RefGui.GUI_ALCHEMY_FURNACE);
        assertEquals(10, RefGui.GUI_RESEARCH_TABLE);
        assertEquals(11, RefGui.GUI_CHEST_HUNGRY);
        assertEquals(12, RefGui.GUI_THAUMONOMICON);
        assertEquals(13, RefGui.GUI_ARCANE_WORKBENCH);
        assertEquals(15, RefGui.GUI_ARCANE_BORE);
        assertEquals(16, RefGui.GUI_HAND_MIRROR);
        assertEquals(17, RefGui.GUI_HOVER_HARNESS);
        assertEquals(18, RefGui.GUI_MAGIC_BOX);
        assertEquals(19, RefGui.GUI_SPA);
        assertEquals(20, RefGui.GUI_FOCAL_MANUPULATOR);
    }

    // -------------------------------------------------------------- the server defaults

    /**
     * Original {@code thaumcraft.common.config.Config} (oracle {@code Config.java:392-425}): the
     * effective defaults of the seven research facts the original sends to the client.
     *
     * <p>The original writes the defaults into its Forge configuration with
     * {@code config.get(category, key, default)}: 0 for the research difficulty, 100 for the aspect
     * cap, true for the warded stone, the mirrors and the hard nodes, false for the cheat sheet and
     * for the wuss mode.</p>
     */
    @Test void theResearchDefaultsAreTheOriginalOnes() {
        ResearchSettings defaults = ResearchSettings.DEFAULTS;
        assertFalse(defaults.cheatSheet(), "the original default of allow_cheat_sheet is false");
        assertTrue(defaults.wardedStone());
        assertTrue(defaults.mirrors());
        assertTrue(defaults.hardNodes());
        assertFalse(defaults.wuss());
        assertEquals(0, defaults.difficulty());
        assertEquals(100, defaults.aspectCap());
    }

    /** A configuration file without the research keys must answer the original defaults. */
    @Test void anEmptyConfigurationAnswersTheOriginalDefaults() {
        ResearchSettings read = ResearchSettings.read(new Properties());
        assertFalse(read.cheatSheet());
        assertTrue(read.wardedStone());
        assertTrue(read.mirrors());
        assertTrue(read.hardNodes());
        assertFalse(read.wuss());
        assertEquals(0, read.difficulty());
        assertEquals(100, read.aspectCap());
    }

    /**
     * Original {@code Config.researchDifficulty} (oracle {@code Config.java:392-394}): 0 buys the
     * secondary research, -1 buys everything and 1 forces the research table.
     */
    @Test void theResearchDifficultyKeepsTheOriginalThreeSteps() {
        assertTrue(ResearchSettings.DEFAULTS.purchases(true), "difficulty 0 buys the secondary research");
        assertFalse(ResearchSettings.DEFAULTS.purchases(false), "difficulty 0 does not buy the primary research");
        assertTrue(new ResearchSettings(false, true, true, true, false, -1, 100).purchases(false),
                "difficulty -1 buys everything");
        assertFalse(new ResearchSettings(false, true, true, true, false, 1, 100).purchases(true),
                "difficulty 1 forces the research table");
    }

    // ---------------------------------------------------------------- the pure helpers

    /** Original {@code Utils.pack} and {@code Utils.unpack} (oracle {@code Utils.java:234-252}). */
    @Test void theBitPackingIsTheOriginalOne() {
        boolean[] values = new boolean[]{true, false, true, true, false, false, true, false};
        byte packed = Utils.pack(values);
        boolean[] unpacked = Utils.unpack(packed);
        for (int i = 0; i < values.length; i++) assertEquals(values[i], unpacked[i], "bit " + i);
        assertEquals((byte) 0b10110010, packed);
        assertEquals((byte) 0, Utils.pack(new boolean[8]));
    }

    /** Original {@code Utils.getNBTDataFromId} (oracle {@code Utils.java:254-281}). */
    @Test void theNbtTypeIdsAreTheOriginalOnes() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("b", (byte) 3);
        tag.putShort("h", (short) 7);
        tag.putInt("i", 11);
        tag.putLong("l", 13L);
        tag.putFloat("f", 1.5F);
        tag.putDouble("d", 2.5D);
        tag.putString("s", "value");
        assertEquals(Byte.valueOf((byte) 3), Utils.getNBTDataFromId(tag, (byte) 1, "b"));
        assertEquals(Short.valueOf((short) 7), Utils.getNBTDataFromId(tag, (byte) 2, "h"));
        assertEquals(Integer.valueOf(11), Utils.getNBTDataFromId(tag, (byte) 3, "i"));
        assertEquals(Long.valueOf(13L), Utils.getNBTDataFromId(tag, (byte) 4, "l"));
        assertEquals(Float.valueOf(1.5F), Utils.getNBTDataFromId(tag, (byte) 5, "f"));
        assertEquals(Double.valueOf(2.5D), Utils.getNBTDataFromId(tag, (byte) 6, "d"));
        assertEquals("value", Utils.getNBTDataFromId(tag, (byte) 8, "s"));
        assertNull(Utils.getNBTDataFromId(tag, (byte) 12, "i"), "an unknown type id answers null");
    }

    /**
     * Original {@code Utils.isLyingInCone} (oracle {@code Utils.java:312-323}): the point has to be
     * inside the infinite cone and in front of the round cap of the axis.
     */
    @Test void theConeTestIsTheOriginalOne() {
        double[] apex = new double[]{0.0, 0.0, 0.0};
        double[] axisEnd = new double[]{0.0, 0.0, 10.0};
        assertTrue(Utils.isLyingInCone(new double[]{0.0, 0.0, 5.0}, apex, axisEnd, 1.0F),
                "a point on the axis is inside the cone");
        assertFalse(Utils.isLyingInCone(new double[]{0.0, 0.0, -5.0}, apex, axisEnd, 1.0F),
                "a point behind the apex is outside the cone");
        assertFalse(Utils.isLyingInCone(new double[]{0.0, 0.0, 20.0}, apex, axisEnd, 1.0F),
                "a point behind the round cap is outside the cone");
        assertFalse(Utils.isLyingInCone(new double[]{5.0, 0.0, 5.0}, apex, axisEnd, 0.1F),
                "a point beside the axis is outside a narrow cone");
    }

    /** Original {@code Utils.calculateVelocity} (oracle {@code Utils.java:337-354}). */
    @Test void theLaunchVelocityIsTheOriginalOne() {
        Vec3 velocity = Utils.calculateVelocity(new Vec3(0.0, 0.0, 0.0), new Vec3(10.0, 0.0, 0.0), 1.0D, 1.0D);
        assertEquals(2.5D, velocity.x, 1.0E-9);
        assertEquals(1.0D, velocity.y, 1.0E-9);
        assertEquals(0.0D, velocity.z, 1.0E-9);
    }

    /** Original {@code Utils.distanceSquared2d} and {@code Utils.distanceSquared3d}. */
    @Test void theDistanceHelpersAreTheOriginalOnes() {
        Vec3 from = new Vec3(1.0, 2.0, 3.0);
        Vec3 to = new Vec3(4.0, 6.0, 3.0);
        assertEquals(9.0D, Utils.distanceSquared2d(from, to));
        assertEquals(25.0D, Utils.distanceSquared3d(from, to));
    }
}
