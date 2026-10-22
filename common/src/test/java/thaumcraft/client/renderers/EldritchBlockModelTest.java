package thaumcraft.client.renderers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.block.ArcaneFurnaceBakedModel;
import thaumcraft.client.renderers.block.EldritchCrustBakedModel;
import thaumcraft.client.renderers.block.ObsidianTotemBakedModel;
import thaumcraft.client.renderers.block.WardedGlassBakedModel;
import thaumcraft.client.renderers.models.ModelTunnelField;

/**
 * The original TC4 client geometry of the eldritch, cosmetic and device block
 * families, checked value by value. Every number below is the value the 1.7.10
 * renderer used.
 */
class EldritchBlockModelTest {
    private static final float EPS = 1.0E-5F;

    @Test void eldritchCrustInsetsOnlyTheExposedSides() {
        // The original BlockEldritchRenderer insets the crust by two sixteenths
        // on every side without a solid neighbour (Block.isBlockSolid, material
        // solidity). The mask bit of a face is 1 << Direction.get3DDataValue().
        int bare = 0;
        assertEquals(2.0F, EldritchCrustBakedModel.minX(bare), EPS, "west");
        assertEquals(2.0F, EldritchCrustBakedModel.minY(bare), EPS, "down");
        assertEquals(2.0F, EldritchCrustBakedModel.minZ(bare), EPS, "north");
        assertEquals(14.0F, EldritchCrustBakedModel.maxX(bare), EPS, "east");
        assertEquals(14.0F, EldritchCrustBakedModel.maxY(bare), EPS, "up");
        assertEquals(14.0F, EldritchCrustBakedModel.maxZ(bare), EPS, "south");
        int joined = west | down | north | east | up | south;
        assertEquals(0.0F, EldritchCrustBakedModel.minX(joined), EPS, "west");
        assertEquals(0.0F, EldritchCrustBakedModel.minY(joined), EPS, "down");
        assertEquals(0.0F, EldritchCrustBakedModel.minZ(joined), EPS, "north");
        assertEquals(16.0F, EldritchCrustBakedModel.maxX(joined), EPS, "east");
        assertEquals(16.0F, EldritchCrustBakedModel.maxY(joined), EPS, "up");
        assertEquals(16.0F, EldritchCrustBakedModel.maxZ(joined), EPS, "south");
        int half = west | north | up;
        assertEquals(0.0F, EldritchCrustBakedModel.minX(half), EPS, "west");
        assertEquals(2.0F, EldritchCrustBakedModel.minY(half), EPS, "down");
        assertEquals(0.0F, EldritchCrustBakedModel.minZ(half), EPS, "north");
        assertEquals(14.0F, EldritchCrustBakedModel.maxX(half), EPS, "east");
        assertEquals(16.0F, EldritchCrustBakedModel.maxY(half), EPS, "up");
        assertEquals(14.0F, EldritchCrustBakedModel.maxZ(half), EPS, "south");
    }

    /** The mask bit of one face, 1 << Direction.get3DDataValue(). */
    private static final int down = 1 << Direction.DOWN.get3DDataValue();
    private static final int up = 1 << Direction.UP.get3DDataValue();
    private static final int north = 1 << Direction.NORTH.get3DDataValue();
    private static final int south = 1 << Direction.SOUTH.get3DDataValue();
    private static final int west = 1 << Direction.WEST.get3DDataValue();
    private static final int east = 1 << Direction.EAST.get3DDataValue();

    @Test void arcaneFurnaceKeepsTheOriginalShellTextures() {
        // The original texture selection of the shell blocks.
        assertEquals(7, ArcaneFurnaceBakedModel.textureForSide(0, 0, -1, Direction.NORTH));
        assertEquals(6, ArcaneFurnaceBakedModel.textureForSide(0, 9, -1, Direction.NORTH));
        assertEquals(2, ArcaneFurnaceBakedModel.textureForSide(1, 0, -1, Direction.NORTH));
        assertEquals(1, ArcaneFurnaceBakedModel.textureForSide(2, 0, -1, Direction.NORTH));
        assertEquals(0, ArcaneFurnaceBakedModel.textureForSide(3, 0, -1, Direction.NORTH));
        assertEquals(0, ArcaneFurnaceBakedModel.textureForSide(7, 0, -1, Direction.SOUTH));
        assertEquals(10, ArcaneFurnaceBakedModel.textureForSide(5, 0, -1, Direction.UP));
        assertEquals(10, ArcaneFurnaceBakedModel.textureForSide(5, 0, -1, Direction.DOWN));
        // A nozzle on the same side shifts the texture by three.
        assertEquals(5, ArcaneFurnaceBakedModel.textureForSide(1, 0, Direction.NORTH.get3DDataValue(), Direction.NORTH));
        assertEquals(2, ArcaneFurnaceBakedModel.textureForSide(1, 0, -1, Direction.NORTH));
    }

    @Test void arcaneFurnaceKeepsTheOriginalNozzleSheets() {
        // The original nozzle draws the grate, the grin and the fire.
        ArcaneFurnaceBakedModel.NozzlePlane[] planes = ArcaneFurnaceBakedModel.nozzleGeometry(Direction.WEST);
        assertEquals(3, planes.length, "the original nozzle has three sheets");
        assertEquals(Direction.EAST, planes[0].outwardFace(), "the sheets face away from the core");
        assertEquals(16.0F - ArcaneFurnaceBakedModel.GRATE_DEPTH - ArcaneFurnaceBakedModel.SHEET_THICKNESS,
                planes[0].minX(), EPS, "the grate sits at ten sixteenths and is one sixteenth thick");
        assertEquals(16.0F - ArcaneFurnaceBakedModel.GRATE_DEPTH, planes[0].maxX(), EPS, "the grate ends at ten sixteenths");
        assertEquals(16.0F - ArcaneFurnaceBakedModel.FIRE_DEPTH - ArcaneFurnaceBakedModel.SHEET_THICKNESS,
                planes[2].minX(), EPS, "the fire sits deeper");
        assertEquals(24.0F, planes[2].maxY(), EPS, "the sheets are one and a half blocks high");
    }

    @Test void obsidianTotemKeepsTheOriginalRunes() {
        // The original totem draws the plain tile on the top and the bottom.
        assertEquals(ObsidianTotemBakedModel.TEXTURE_TILE, ObsidianTotemBakedModel.textureIndex(Direction.UP, ObsidianTotemBakedModel.STYLE_BASE, 0));
        assertEquals(ObsidianTotemBakedModel.TEXTURE_TILE, ObsidianTotemBakedModel.textureIndex(Direction.DOWN, ObsidianTotemBakedModel.STYLE_BASE, 0));
        // The plain and the shaded base of the totem.
        assertEquals(ObsidianTotemBakedModel.TEXTURE_BASE, ObsidianTotemBakedModel.textureIndex(Direction.NORTH, ObsidianTotemBakedModel.STYLE_BASE, 0));
        assertEquals(ObsidianTotemBakedModel.TEXTURE_SHADED, ObsidianTotemBakedModel.textureIndex(Direction.NORTH, ObsidianTotemBakedModel.STYLE_SHADED, 0));
        // The four runes of the totem.
        assertEquals(ObsidianTotemBakedModel.TEXTURE_RUNE_START + 2, ObsidianTotemBakedModel.textureIndex(Direction.NORTH, 2, 0));
        assertEquals(ObsidianTotemBakedModel.TEXTURE_RUNE_START + 3, ObsidianTotemBakedModel.textureIndex(Direction.NORTH, 2, 1));
        assertEquals(ObsidianTotemBakedModel.TEXTURE_RUNE_START + 3, ObsidianTotemBakedModel.textureIndex(Direction.SOUTH, 2, 0));
        assertEquals(ObsidianTotemBakedModel.TEXTURE_RUNE_START, ObsidianTotemBakedModel.textureIndex(Direction.WEST, 2, 0));
    }

    @Test void wardedGlassKeepsTheOriginalNeighbourMask() {
        // The original builds a bit mask of the eight neighbours of a face.
        boolean[] none = new boolean[8];
        assertEquals(0, WardedGlassBakedModel.mask(none));
        assertEquals(0, WardedGlassBakedModel.textureIndex(none));
        boolean[] first = new boolean[8];
        first[0] = true;
        assertEquals(1, WardedGlassBakedModel.mask(first));
        boolean[] last = new boolean[8];
        last[7] = true;
        assertEquals(128, WardedGlassBakedModel.mask(last));
        boolean[] all = new boolean[8];
        java.util.Arrays.fill(all, true);
        assertEquals(255, WardedGlassBakedModel.mask(all));
        // The original lookup table of the connected textures.
        assertEquals(256, WardedGlassBakedModel.CONNECTED_TEXTURE.length, "the original table has one entry per mask");
        assertEquals(0, WardedGlassBakedModel.CONNECTED_TEXTURE[0]);
        assertEquals(6, WardedGlassBakedModel.CONNECTED_TEXTURE[2]);
        assertEquals(19, WardedGlassBakedModel.CONNECTED_TEXTURE[10]);
        assertEquals(34, WardedGlassBakedModel.CONNECTED_TEXTURE[254]);
        assertEquals(11, WardedGlassBakedModel.CONNECTED_TEXTURE[255]);
        for (int index : WardedGlassBakedModel.CONNECTED_TEXTURE) {
            assertTrue(index >= 0 && index < WardedGlassBakedModel.TEXTURE_COUNT, "every entry is a rune texture");
        }
    }

    @Test void tunnelFieldKeepsTheOriginalLayers() {
        // The original tunnel has sixteen layers, a tunnel texture and a
        // particle field.
        assertEquals(16, ModelTunnelField.LAYERS);
        assertEquals(31100L, ModelTunnelField.SEED);
        assertEquals(.125F, ModelTunnelField.OUTER_TEXTURE_SCALE, EPS);
        assertEquals(.5F, ModelTunnelField.INNER_TEXTURE_SCALE, EPS);
        assertEquals("thaumcraft:textures/misc/tunnel.png", ModelTunnelField.TUNNEL.toString());
        assertEquals("thaumcraft:textures/misc/particlefield.png", ModelTunnelField.FIELD.toString());
        assertEquals("thaumcraft:textures/misc/particlefield32.png", ModelTunnelField.FIELD_FAR.toString());
    }
}
