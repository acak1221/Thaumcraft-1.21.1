package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.client.ThaumcraftGearModelsClient;
import thaumcraft.client.renderers.models.gear.ModelWand;
import thaumcraft.common.items.wands.WandMaterials;

/**
 * The drawing of add-on wand parts: the part textures (original ModelWand.render binds getRod/getCap().getTexture()), the glow
 * of a glowing rod and the ornament of an add-on focus (original ModelWand.render:414-635 with 1.7.10
 * ItemRenderer.renderItemIn2D), recorded through the production vertex path without a GL context.
 */
class WandAddonRenderingTest {
    private LinkedHashMap<String, WandRod> rods;
    private LinkedHashMap<String, WandCap> caps;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertNotNull(FocusRenderingTestRegistry.ornamented(), "focus fixtures register in the launcher session");
    }

    @BeforeEach void snapshot() { rods = new LinkedHashMap<>(WandRod.rods); caps = new LinkedHashMap<>(WandCap.caps); }
    @AfterEach void restore() {
        WandRod.rods.clear(); WandRod.rods.putAll(rods);
        WandCap.caps.clear(); WandCap.caps.putAll(caps);
    }

    @Test void nativePartsKeepTheirSheetsAndAddonPartsUseTheirOwn() {
        for (var rod : WandMaterials.RODS) assertEquals(ModelWand.rodTexture(rod.baseTag()), ThaumcraftGearModelsClient.rodTexture(rod), rod.tag());
        for (var cap : WandMaterials.CAPS) assertEquals(ModelWand.capTexture(cap.tag()), ThaumcraftGearModelsClient.capTexture(cap), cap.tag());
        var sheet = ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_rod_infernal.png");
        new WandRod("junit_infernal", 150, ItemStack.EMPTY, 12, null, sheet);
        assertEquals(sheet, ThaumcraftGearModelsClient.rodTexture(WandMaterials.rod("junit_infernal")));
        WandRod.rods.get("junit_infernal").setTexture(null);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_junit_infernal.png"),
                ThaumcraftGearModelsClient.rodTexture(WandMaterials.rod("junit_infernal")), "a null texture falls back to the derived sheet");
        var staffSheet = ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_rod_infernal_staff.png");
        new StaffRod("junit_infernal", 250, ItemStack.EMPTY, 20, null, staffSheet);
        assertEquals(staffSheet, ThaumcraftGearModelsClient.rodTexture(WandMaterials.rod("junit_infernal_staff")));
        var cap = new WandCap("junit_alchemical", .9F, ItemStack.EMPTY, 7);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_junit_alchemical.png"),
                ThaumcraftGearModelsClient.capTexture(WandMaterials.cap("junit_alchemical")));
        var capSheet = ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_cap_alchemical.png");
        cap.setTexture(capSheet);
        assertEquals(capSheet, ThaumcraftGearModelsClient.capTexture(WandMaterials.cap("junit_alchemical")), "the texture is read live");
    }

    @Test void aGlowingRodUsesTheOriginalPulsingLightmapValue() {
        assertEquals(205, ModelWand.Geometry.glowLight(0));
        for (int tick = 0; tick < 200; tick++) {
            int light = ModelWand.Geometry.glowLight(tick);
            assertTrue(light >= 200 && light <= 210, "block light only, 200 + sin * 5 + 5: " + light);
        }
        var rod = new WandRod("junit_glow", 50, ItemStack.EMPTY, 3);
        assertFalse(WandMaterials.rod("junit_glow").glowing());
        rod.setGlowing(true);
        assertTrue(WandMaterials.rod("junit_glow").glowing());
        assertTrue(WandRod.rods.get("blaze").isGlowing(), "original ConfigItems:305 lights the blaze rod's API view");
        assertFalse(WandMaterials.BLAZE.glowing(), "the native renderer of the blaze rod is unchanged");
    }

    @Test void anAddonOrnamentIsTwoCrossedExtrudedIconsBeforeTheShell() {
        var focus = FocusRenderingTestRegistry.ornamented();
        var stack = new ItemStack(focus);
        var ornament = focus.getOrnament(stack);
        for (boolean staff : List.of(false, true)) {
            var pose = new PoseStack();
            var before = new Matrix4f(pose.last().pose());
            var recording = new Recording();
            PrimalWandFocusRenderer.renderFocus(new ModelWand(), focus, stack, pose, recording, 77, 0, 3, staff,
                    texture -> PrimalWandFocusRenderer.Animation.STATIC,
                    texture -> { assertEquals(ornament, texture); return new PrimalWandFocusRenderer.Sheet(PrimalWandFocusRenderer.Animation.STATIC, 16, 16); });
            assertEquals(before, pose.last().pose(), "the caller's pose is restored");
            assertEquals(2, recording.batches.size(), "ornament, then the shell; this focus has no depth layer");
            var drawn = recording.batches.getFirst();
            assertSame(RenderType.entityCutoutNoCull(ornament), drawn.type);
            // Two passes of renderItemIn2D: front, back, 16 strips each way on both sides.
            assertEquals(2 * (2 + 2 * 16 + 2 * 16) * 4, drawn.vertices.size());
            assertTrue(drawn.vertices.stream().allMatch(v -> v.light == 77 && v.alpha == 255 && v.red == 255));
            assertTrue(drawn.vertices.stream().allMatch(v -> v.u >= -.0001F && v.u <= 1.0001F && v.v >= -.0001F && v.v <= 1.0001F));
            // Original: rotate 180 about X, translate (-.25, -.1, .0275), scale .5; the icon's (0,0) corner lands here.
            var first = drawn.vertices.getFirst();
            assertEquals(-.25F, first.x, 1e-5F); assertEquals(.1F, first.y, 1e-5F); assertEquals(-.0275F, first.z, 1e-5F);
            assertEquals(1F, first.u, 0F, "renderItemIn2D maps x=0 to the icon's max U"); assertEquals(1F, first.v, 0F);
            var second = drawn.vertices.get(1);
            assertEquals(.25F, second.x, 1e-5F); assertEquals(.1F, second.y, 1e-5F); assertEquals(0F, second.u, 0F);
            var third = drawn.vertices.get(2);
            assertEquals(-.4F, third.y, 1e-5F, "the icon is half a unit tall");
            // The second pass is turned ninety degrees about Y: its front face lies in the X = constant plane.
            int half = drawn.vertices.size() / 2;
            for (int corner = 0; corner < 4; corner++) {
                assertEquals(.0275F, Math.abs(drawn.vertices.get(half + corner).x), 1e-5F, "crossed front face corner " + corner);
            }
            var shell = recording.batches.getLast();
            assertSame(RenderType.entityTranslucent(ModelWand.FOCUS_TEXTURE), shell.type);
            assertTrue(shell.vertices.stream().allMatch(v -> v.red == 0x33 && v.green == 0xCC && v.blue == 0xAA && v.alpha == 242),
                    "the add-on focus colour, without a depth layer");
        }
    }

    @Test void nativeFociWithoutOrnamentDrawExactlyAsBefore() {
        var focus = FocusRenderingTestRegistry.ordinary();
        var recording = new Recording();
        PrimalWandFocusRenderer.renderFocus(new ModelWand(), focus, new ItemStack(focus), new PoseStack(), recording, 100, 0, 0, false,
                texture -> PrimalWandFocusRenderer.Animation.STATIC,
                texture -> { throw new AssertionError("no ornament sheet may be read for " + texture); });
        assertEquals(1, recording.batches.size(), "only the shell");
    }

    private static final class Recording implements MultiBufferSource {
        final List<Batch> batches = new ArrayList<>();
        @Override public VertexConsumer getBuffer(RenderType type) {
            var batch = new Batch(type);
            batches.add(batch);
            return batch;
        }
    }
    private static final class Vertex {
        float x, y, z, u, v;
        int red, green, blue, alpha, light;
    }
    private static final class Batch implements VertexConsumer {
        final RenderType type;
        final List<Vertex> vertices = new ArrayList<>();
        private Vertex vertex;
        Batch(RenderType type) { this.type = type; }
        public VertexConsumer addVertex(float x, float y, float z) {
            vertex = new Vertex(); vertex.x = x; vertex.y = y; vertex.z = z; vertices.add(vertex); return this;
        }
        public VertexConsumer setColor(int r, int g, int b, int a) { vertex.red = r; vertex.green = g; vertex.blue = b; vertex.alpha = a; return this; }
        public VertexConsumer setUv(float u, float v) { vertex.u = u; vertex.v = v; return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { vertex.light = u | v << 16; return this; }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
