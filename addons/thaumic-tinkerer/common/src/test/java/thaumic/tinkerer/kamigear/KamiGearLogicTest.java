/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): JUnit tests of the plain rules of the KamiGear group. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.kamigear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.ItemPlacementMirror;
import thaumic.tinkerer.common.item.kami.KamiComponents.PearlTarget;
import thaumic.tinkerer.common.item.kami.KamiComponents.TalismanStore;
import thaumic.tinkerer.common.item.kami.armor.IchorAbsorption;
import thaumic.tinkerer.common.item.kami.foci.ItemFocusXPDrain;
import thaumic.tinkerer.common.item.kami.foci.ShadowBeam;
import thaumic.tinkerer.common.item.kami.tool.IchorTier;
import thaumic.tinkerer.common.item.kami.tool.ToolHandler;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.network.packet.kami.PacketArmorStatus;
import thaumic.tinkerer.common.network.packet.kami.PacketShadowBeam;
import thaumic.tinkerer.common.network.packet.kami.PacketToggleArmor;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The plain rules of the Kami gear: the tool mode cycle, the shape of the Placement Mirror's square, the Shadowbeam's
 * damage and reflection, the experience curve of the Experience Drain focus, the order of the resource family and the
 * codecs of the data components and of the beam packet. Nothing here needs a world.</p>
 */
class KamiGearLogicTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ---- tool modes ----------------------------------------------------------------------------------------------

    @Test
    void theToolModeCyclesThroughThreeModes() {
        assertEquals(1, ToolHandler.getNextMode(0));
        assertEquals(2, ToolHandler.getNextMode(1));
        assertEquals(0, ToolHandler.getNextMode(2));
        int mode = 0;
        for (int i = 0; i < 9; i++) mode = ToolHandler.getNextMode(mode);
        assertEquals(0, mode, "nine clicks are three full turns");
    }

    // ---- the resource family -------------------------------------------------------------------------------------

    @Test
    void theResourceIndicesFollowTheOriginalMetadataOrder() {
        assertEquals("ichor", LibItemNames.KAMI_RESOURCE_NAMES[ItemKamiResource.ICHOR]);
        assertEquals("ichor_cloth", LibItemNames.KAMI_RESOURCE_NAMES[ItemKamiResource.ICHOR_CLOTH]);
        assertEquals("ichorium", LibItemNames.KAMI_RESOURCE_NAMES[ItemKamiResource.ICHORIUM]);
        assertEquals("ichor_nugget", LibItemNames.KAMI_RESOURCE_NAMES[ItemKamiResource.ICHOR_NUGGET]);
        assertEquals("ichor_cap", LibItemNames.KAMI_RESOURCE_NAMES[ItemKamiResource.ICHOR_CAP]);
        assertEquals("ichorcloth_rod", LibItemNames.KAMI_RESOURCE_NAMES[ItemKamiResource.ICHORCLOTH_ROD]);
        assertEquals("nether_shard", LibItemNames.KAMI_RESOURCE_NAMES[ItemKamiResource.NETHER_SHARD]);
        assertEquals("ender_shard", LibItemNames.KAMI_RESOURCE_NAMES[ItemKamiResource.ENDER_SHARD]);
        assertEquals(8, LibItemNames.KAMI_RESOURCE_NAMES.length);
    }

    // ---- the Placement Mirror's square -----------------------------------------------------------------------------

    @Test
    void theSquareIsAlwaysSizeTimesSizeBlocksOnEveryFaceAndLook() {
        for (int size : new int[] { 3, 5, 7, 9, 11 }) {
            for (Direction side : Direction.values()) {
                for (float pitch : new float[] { -80, 0, 74, 76, 90 }) {
                    for (int rotation = 0; rotation < 4; rotation++) {
                        List<int[]> offsets = ItemPlacementMirror.squareOffsets(side, pitch, rotation, size);
                        assertEquals(size * size, offsets.size(),
                                "size " + size + " side " + side + " pitch " + pitch + " rotation " + rotation);
                        Set<String> distinct = new HashSet<>();
                        for (int[] offset : offsets) distinct.add(offset[0] + "," + offset[1] + "," + offset[2]);
                        assertEquals(offsets.size(), distinct.size(), "no position twice");
                    }
                }
            }
        }
    }

    @Test
    void anEvenSizeIsTreatedLikeTheOddSizeAboveIt() {
        // 1.7.10 stored the size as size | 1; squareOffsets itself takes (size ^ 1) / 2 as the half width.
        assertEquals(25, ItemPlacementMirror.squareOffsets(Direction.NORTH, 0, 0, 4).size());
        assertEquals(49, ItemPlacementMirror.squareOffsets(Direction.NORTH, 0, 0, 6).size());
    }

    @Test
    void aWallSquareStandsInFrontOfTheFaceAndRisesFromTheAimedBlock() {
        // Aiming at the north face of a block: the square is one block north of it, x centred, y from the block upward.
        List<int[]> offsets = ItemPlacementMirror.squareOffsets(Direction.NORTH, 0, 0, 3);
        int minX = 99, maxX = -99, minY = 99, maxY = -99;
        for (int[] offset : offsets) {
            assertEquals(-1, offset[2], "one block in front of the north face");
            minX = Math.min(minX, offset[0]);
            maxX = Math.max(maxX, offset[0]);
            minY = Math.min(minY, offset[1]);
            maxY = Math.max(maxY, offset[1]);
        }
        assertEquals(-1, minX);
        assertEquals(1, maxX);
        assertEquals(0, minY);
        assertEquals(2, maxY);
    }

    @Test
    void aFloorSquareLiesOnTheTopFaceWhenLookingStraightDown() {
        List<int[]> offsets = ItemPlacementMirror.squareOffsets(Direction.UP, 80, 0, 5);
        for (int[] offset : offsets) {
            assertEquals(1, offset[1], "one block above the aimed block");
            assertTrue(Math.abs(offset[0]) <= 2 && Math.abs(offset[2]) <= 2);
        }
    }

    @Test
    void aSideFaceSquareFollowsTheFacingOfTheTopFaceWhenLookingSlantedDown() {
        // Looking down at 60 degrees the top-face square stands up instead: rotation decides its orientation.
        List<int[]> alongX = ItemPlacementMirror.squareOffsets(Direction.UP, 60, 0, 3);
        List<int[]> alongZ = ItemPlacementMirror.squareOffsets(Direction.UP, 60, 1, 3);
        assertTrue(alongX.stream().allMatch(offset -> offset[2] == 0), "even rotation: a wall in the x direction");
        assertTrue(alongZ.stream().allMatch(offset -> offset[0] == 0), "odd rotation: a wall in the z direction");
    }

    @Test
    void theMirrorRemembersOnlyTheBlocksOfTheOldPlainBlockModel() {
        var at = BlockPos.ZERO;
        // 1.7.10 render type 0: cubes, slabs, glass, leaves, furnaces, snow layers.
        for (var block : List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.GLASS, Blocks.OAK_LEAVES, Blocks.FURNACE,
                Blocks.OAK_PLANKS, Blocks.STONE_SLAB, Blocks.SNOW, Blocks.DIRT)) {
            assertTrue(ItemPlacementMirror.canRemember(EmptyBlockGetter.INSTANCE, at, block.defaultBlockState()),
                    block + " is remembered");
        }
        // Refused: stairs, fences, walls, doors, beds, torches, panes, ladders, chests, plants, air.
        for (var block : List.of(Blocks.OAK_STAIRS, Blocks.OAK_FENCE, Blocks.COBBLESTONE_WALL, Blocks.OAK_DOOR, Blocks.RED_BED,
                Blocks.TORCH, Blocks.GLASS_PANE, Blocks.LADDER, Blocks.CHEST, Blocks.DANDELION, Blocks.AIR, Blocks.WATER)) {
            assertFalse(ItemPlacementMirror.canRemember(EmptyBlockGetter.INSTANCE, at, block.defaultBlockState()),
                    block + " is refused");
        }
    }

    // ---- the tools -----------------------------------------------------------------------------------------------

    private static double weaponModifier(ItemAttributeModifiers modifiers) {
        return modifiers.modifiers().stream().filter(entry -> entry.attribute().equals(Attributes.ATTACK_DAMAGE))
                .mapToDouble(entry -> entry.modifier().amount()).sum();
    }

    @Test
    void theIchoriumToolsKeepTheDamageOf1710() {
        // 1.7.10: base 2 / 1 / 3 / 4 (pickaxe, shovel, axe, sword) + the material's 5; a hit adds the player's own 1.
        assertEquals(7.0, weaponModifier(IchorTier.pickaxeAttributes()), 1.0E-9);
        assertEquals(6.0, weaponModifier(IchorTier.shovelAttributes()), 1.0E-9);
        assertEquals(8.0, weaponModifier(IchorTier.axeAttributes()), 1.0E-9);
        assertEquals(9.0, weaponModifier(IchorTier.swordAttributes()), 1.0E-9);
    }

    // ---- the Ichorcloth absorption ------------------------------------------------------------------------------

    @Test
    void everyIchorclothPieceTakesFourAndAQuarterPercentOfTheBlowPerArmourPoint() {
        assertEquals(0.0425, IchorAbsorption.RATIO_PER_POINT, 0.0);
        assertEquals(0.1275, IchorAbsorption.ratio(3), 1.0E-12, "helmet and boots, 3 points");
        assertEquals(0.34, IchorAbsorption.ratio(8), 1.0E-12, "chestplate, 8 points");
        assertEquals(0.255, IchorAbsorption.ratio(6), 1.0E-12, "leggings, 6 points");
        double set = IchorAbsorption.ratio(3) + IchorAbsorption.ratio(8) + IchorAbsorption.ratio(6) + IchorAbsorption.ratio(3);
        assertEquals(0.85, set, 1.0E-12, "the four pieces take 85% together");
        assertEquals(0.0, IchorAbsorption.ratio(0), 0.0);
    }

    @Test
    void theFullSetTakesEightyFivePercentOfAnyBlowAndNothingMore() {
        double set = IchorAbsorption.ratio(3) + IchorAbsorption.ratio(8) + IchorAbsorption.ratio(6) + IchorAbsorption.ratio(3);
        for (float blow : new float[] { 1F, 4F, 10F, 40F, 500F }) {
            float absorbed = IchorAbsorption.absorbed(blow, set, 0F);
            assertEquals(blow * 0.85F, absorbed, blow * 1.0E-6F, "85% of a blow of " + blow + ", flat: the 1.21.1 formula would take less of a large one");
            assertEquals(blow * 0.15F, blow - absorbed, blow * 1.0E-6F, "and 15% goes through");
        }
    }

    @Test
    void aPartialSetTakesTheSumOfItsPieces() {
        assertEquals(1.275F, IchorAbsorption.absorbed(10F, IchorAbsorption.ratio(3), 0F), 1.0E-5F, "a helmet alone");
        assertEquals(5.95F, IchorAbsorption.absorbed(10F, IchorAbsorption.ratio(8) + IchorAbsorption.ratio(6), 0F), 1.0E-5F, "chestplate and leggings");
        assertEquals(0F, IchorAbsorption.absorbed(10F, 0.0, 0F), 0F, "no piece, no absorption");
    }

    @Test
    void otherArmourAddsItsOwnShareAndTheSumNeverExceedsTheBlow() {
        // 1.7.10 ApplyArmor added the ratios of every piece of one priority
        assertEquals(3.4F + 1.6F, IchorAbsorption.absorbed(10F, IchorAbsorption.ratio(8), 1.6F), 1.0E-5F, "an Ichorcloth chestplate and iron armour that takes 1.6");
        assertEquals(10F, IchorAbsorption.absorbed(10F, 0.85, 3F), 0F, "85% and 30% is not 115% of the blow");
        assertEquals(1.6F, IchorAbsorption.absorbed(10F, 0.0, 1.6F), 0F, "without Ichorcloth the other armour's amount is returned as it is");
    }

    // ---- the Shadowbeam -------------------------------------------------------------------------------------------

    @Test
    void theShadowbeamDamageGrowsByTwentyPercentOfTheBasePerPotencyLevel() {
        assertEquals(10, ShadowBeam.damage(10, 0));
        assertEquals(12, ShadowBeam.damage(10, 1));
        assertEquals(14, ShadowBeam.damage(10, 2));
        assertEquals(16, ShadowBeam.damage(10, 3));
        assertEquals(11, ShadowBeam.damage(7, 3), "truncated, as the int cast of 1.7.10");
    }

    @Test
    void aBeamBouncesOffAWallLikeALightRay() {
        Vec3 reflected = ShadowBeam.reflect(new Vec3(1, 0, 0), Direction.WEST);
        assertEquals(-1, reflected.x, 1.0E-9);
        assertEquals(0, reflected.y, 1.0E-9);
        assertEquals(0, reflected.z, 1.0E-9);

        Vec3 slanted = ShadowBeam.reflect(new Vec3(0.5, -0.25, 0.1), Direction.UP);
        assertEquals(0.5, slanted.x, 1.0E-9);
        assertEquals(0.25, slanted.y, 1.0E-9, "only the component along the normal flips");
        assertEquals(0.1, slanted.z, 1.0E-9);
    }

    @Test
    void theBeamStartsBesideTheEyeAndStepsAlongTheLookWithTheOriginalBias() {
        Vec3[] launch = ShadowBeam.launch(new Vec3(10, 20, 30), 0, new Vec3(0, 0, 1));
        // yaw 0: cos = 1, sin = 0
        assertEquals(10 - 0.16, launch[0].x, 1.0E-9);
        assertEquals(20 - 0.1, launch[0].y, 1.0E-9);
        assertEquals(30, launch[0].z, 1.0E-9);
        assertEquals(0.0075 * 0.15, launch[1].x, 1.0E-9);
        assertEquals((1 + 0.0075) * 0.15, launch[1].z, 1.0E-9);
    }

    // ---- the experience curve -------------------------------------------------------------------------------------

    @Test
    void theExperienceOfALevelFollowsTheVanillaCurve() {
        assertEquals(0, ItemFocusXPDrain.getExperienceForLevel(0));
        assertEquals(7, ItemFocusXPDrain.getExperienceForLevel(1));
        assertEquals(352, ItemFocusXPDrain.getExperienceForLevel(16));
        assertEquals(394, ItemFocusXPDrain.getExperienceForLevel(17));
        assertEquals(1395, ItemFocusXPDrain.getExperienceForLevel(30));
        assertEquals(1507, ItemFocusXPDrain.getExperienceForLevel(31));
        assertEquals(1628, ItemFocusXPDrain.getExperienceForLevel(32));
        assertEquals(0, ItemFocusXPDrain.getExperienceForLevel(-4));
    }

    @Test
    void theLevelOfAnAmountOfExperienceInvertsTheCurve() {
        for (int level = 0; level <= 80; level++) {
            int experience = ItemFocusXPDrain.getExperienceForLevel(level);
            assertEquals(level, ItemFocusXPDrain.getLevelForExperience(experience), "exactly level " + level);
            if (level > 0) {
                assertEquals(level - 1, ItemFocusXPDrain.getLevelForExperience(experience - 1), "one point short");
            }
        }
    }

    // ---- data components and packets -----------------------------------------------------------------------------

    @Test
    void theTalismanStoreSurvivesItsCodecs() {
        TalismanStore store = new TalismanStore(ResourceLocation.withDefaultNamespace("cobblestone"), 123456);
        var json = TalismanStore.CODEC.encodeStart(JsonOps.INSTANCE, store).getOrThrow();
        assertEquals(store, TalismanStore.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(7, store.withCount(7).count());
        assertEquals(store.block(), store.withCount(7).block());

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        TalismanStore.STREAM_CODEC.encode(buf, store);
        assertEquals(store, TalismanStore.STREAM_CODEC.decode(buf));
    }

    @Test
    void thePearlTargetSurvivesItsCodecs() {
        PearlTarget target = new PearlTarget(Level.NETHER, new BlockPos(-12, 64, 3000));
        var json = PearlTarget.CODEC.encodeStart(JsonOps.INSTANCE, target).getOrThrow();
        assertEquals(target, PearlTarget.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        PearlTarget.STREAM_CODEC.encode(buf, target);
        assertEquals(target, PearlTarget.STREAM_CODEC.decode(buf));
    }

    @Test
    void theBeamPacketCarriesItsPointsAndCapsTheirNumber() {
        List<Vec3> points = List.of(new Vec3(1.5, 2.5, 3.5), new Vec3(-4, 5, 6.25));
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        PacketShadowBeam.CODEC.encode(buf, new PacketShadowBeam(points));
        assertEquals(points, PacketShadowBeam.CODEC.decode(buf).points());

        List<Vec3> many = new ArrayList<>();
        for (int i = 0; i < PacketShadowBeam.MAX_POINTS + 50; i++) many.add(new Vec3(i, 0, 0));
        RegistryFriendlyByteBuf big = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        PacketShadowBeam.CODEC.encode(big, new PacketShadowBeam(many));
        List<Vec3> read = PacketShadowBeam.CODEC.decode(big).points();
        assertEquals(PacketShadowBeam.MAX_POINTS, read.size());
        assertFalse(big.isReadable(), "nothing but the capped points was written");
    }

    @Test
    void theArmourSwitchPacketsCarryTheirFlag() {
        for (boolean status : new boolean[] { true, false }) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            PacketToggleArmor.CODEC.encode(buf, new PacketToggleArmor(status));
            assertEquals(status, PacketToggleArmor.CODEC.decode(buf).armorStatus(), "client to server");
            assertFalse(buf.isReadable());

            PacketArmorStatus.CODEC.encode(buf, new PacketArmorStatus(status));
            assertEquals(status, PacketArmorStatus.CODEC.decode(buf).armorStatus(), "server to client");
            assertFalse(buf.isReadable());
        }
        assertEquals("toggle_armor", PacketToggleArmor.TYPE.id().getPath());
        assertEquals("armor_status", PacketArmorStatus.TYPE.id().getPath());
    }
}
