package thaumcraft.common.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.lib.CustomSoundType;

/**
 * The custom block sounds of TC4 4.2.3.5 against the 1.21.1 port.
 *
 * <p>The original had no closed sound enum: {@code thaumcraft.common.blocks.CustomStepSound} and
 * {@code thaumcraft.common.lib.CustomSoundType} are sound types whose name lives in the
 * {@code thaumcraft} namespace, so a block could carry its own sound with its own volume and its
 * own pitch. The port keeps the rule in {@link CustomSoundType}, the 1.21.1 counterpart of the
 * two original classes.</p>
 *
 * <p>No block is constructed here: a constructed block leaves an unregistered intrusive holder
 * behind and breaks the registry freeze of the whole test suite.</p>
 */
class CustomStepSoundParityTest {

    /**
     * Oracle BlockMirror:68 - the mirror plays {@code thaumcraft:jar} with the volume 0.5 and the
     * pitch 2.0.
     */
    @Test void theMirrorPlaysTheOriginalJarSound() {
        assertEquals("thaumcraft:jar", name(ThaumcraftEldritchBlocks.MIRROR_SOUND));
        assertEquals(0.5F, ThaumcraftEldritchBlocks.MIRROR_SOUND.getVolume());
        assertEquals(2.0F, ThaumcraftEldritchBlocks.MIRROR_SOUND.getPitch());
    }

    /**
     * Oracle ConfigBlocks:302 - the loot urn plays {@code thaumcraft:urnbreak} with the volume
     * 1.0 and the pitch 0.7.
     */
    @Test void theLootUrnPlaysTheOriginalUrnBreakSound() {
        assertEquals("thaumcraft:urnbreak", name(ThaumcraftEldritchBlocks.LOOT_URN_SOUND));
        assertEquals(1.0F, ThaumcraftEldritchBlocks.LOOT_URN_SOUND.getVolume());
        assertEquals(0.7F, ThaumcraftEldritchBlocks.LOOT_URN_SOUND.getPitch());
    }

    /**
     * Oracle BlockFluxGoo:50 - the flux goo plays {@code thaumcraft:gore} with the volume 1.0
     * and the pitch 1.0.
     */
    @Test void theFluxGooPlaysTheOriginalGoreSound() {
        assertEquals("thaumcraft:gore", name(ThaumcraftFlux.FLUX_GOO_SOUND));
        assertEquals(1.0F, ThaumcraftFlux.FLUX_GOO_SOUND.getVolume());
        assertEquals(1.0F, ThaumcraftFlux.FLUX_GOO_SOUND.getPitch());
    }

    /**
     * Oracle BlockJar:88 - the jar plays {@code thaumcraft:jar} with the volume 1.0 and the
     * pitch 1.0. The wave of 2026-10-23 already carried this one over.
     */
    @Test void theJarPlaysTheOriginalJarSound() {
        assertEquals("thaumcraft:jar", name(ThaumcraftContent.JAR_SOUND));
        assertEquals(1.0F, ThaumcraftContent.JAR_SOUND.getVolume());
        assertEquals(1.0F, ThaumcraftContent.JAR_SOUND.getPitch());
    }

    /**
     * The sound events of the custom sounds have to exist, otherwise the sound type of the block
     * is silent. The four events of the original that the port uses are in the sound document of
     * the port and their samples are in the sound folder of the port.
     */
    @Test void theSoundEventsOfTheCustomSoundsExist() throws Exception {
        var expected = java.util.Map.of("jar", java.util.List.of("thaumcraft:jar1", "thaumcraft:jar2", "thaumcraft:jar3", "thaumcraft:jar4"),
                "urnbreak", java.util.List.of("thaumcraft:urnbreak"), "gore", java.util.List.of("thaumcraft:gore1", "thaumcraft:gore2"),
                "crystal", java.util.List.of("thaumcraft:crystal"));
        // Original 4.2.3.5 JAR, assets/thaumcraft/sounds/*.ogg; not hashes of the native output.
        var hashes = java.util.Map.of(
                "thaumcraft:jar1", "3d2a75e5dfa5e1afd963692c9d31b89faea48e26df6d55578a52728cfcf3e9e4",
                "thaumcraft:jar2", "b62c39158f4f01cc8d85d642a25401f2c8ce1af645b8fdffe9708535b03037ee",
                "thaumcraft:jar3", "2d776e3469d34e26777d12f1166a4bf22bda8f6dd83d3bc10aec43f4d5247585",
                "thaumcraft:jar4", "c8484342dd475bdff8ffb5b27619fffe82b024a7bcc1aa99624d8d73968fdaf5",
                "thaumcraft:urnbreak", "fffd4297fca2d6b6646e55093858fb2d9d80969c323e940553f6fce2d884f0e9",
                "thaumcraft:gore1", "0c1f6a94bdd10b373740447fa153cfdcfdf4768bb052ea59e3fa707789145fc3",
                "thaumcraft:gore2", "d2053692c388553b83e00a85fb9489319abe09fea761a627b35945e28b8685b3",
                "thaumcraft:crystal", "2737cd4dcda0d15e79c99a15b134e81f643cfff9235ff1277bc51651263fd0d7");
        JsonObject document = sounds();
        for (var entry : expected.entrySet()) {
            assertNotNull(document.get(entry.getKey()), entry.getKey());
            var actual = new java.util.ArrayList<String>();
            for (var sample : document.getAsJsonObject(entry.getKey()).getAsJsonArray("sounds")) {
                String name = sample.isJsonPrimitive() ? sample.getAsString() : sample.getAsJsonObject().get("name").getAsString();
                actual.add(name);
                if (sample.isJsonObject()) assertEquals(false, sample.getAsJsonObject().get("stream").getAsBoolean());
                try (var stream = getClass().getClassLoader().getResourceAsStream("assets/thaumcraft/sounds/"
                        + name.substring(name.indexOf(':') + 1) + ".ogg")) {
                    assertNotNull(stream, name);
                    byte[] bytes = stream.readAllBytes();
                    assertEquals(hashes.get(name), java.util.HexFormat.of().formatHex(
                            java.security.MessageDigest.getInstance("SHA-256").digest(bytes)), name + " original sample bytes");
                    assertTrue(bytes.length > 100, name + " is truncated");
                    assertEquals("OggS", new String(bytes, 0, 4, StandardCharsets.US_ASCII), name);
                    String binary = new String(bytes, StandardCharsets.ISO_8859_1);
                    assertTrue(binary.contains("\u0001vorbis"), name + " lacks Vorbis identification");
                    assertTrue(binary.contains("\u0003vorbis") && binary.contains("\u0005vorbis"), name + " lacks comment/setup packets");
                }
            }
            assertEquals(entry.getValue(), actual, entry.getKey() + " sample binding/order");
        }
    }

    /**
     * The custom sound type of the port names its sound in the {@code thaumcraft} namespace, like
     * the two original classes did.
     */
    @Test void theCustomSoundTypeNamesItsSoundInTheThaumcraftNamespace() {
        CustomSoundType sound = new CustomSoundType("urnbreak", 1.0F, 0.7F);
        assertEquals("thaumcraft:urnbreak", sound.getName());
        assertEquals("urnbreak", sound.getSoundName());
        assertEquals(1.0F, sound.getVolume());
        assertEquals(0.7F, sound.getPitch());
    }

    /** The namespaced name of a custom sound type. */
    private static String name(net.minecraft.world.level.block.SoundType sound) {
        assertInstanceOf(CustomSoundType.class, sound, "the original sound of the block is a custom sound type");
        return ((CustomSoundType) sound).getName();
    }

    /** The sound document of the port. */
    private static JsonObject sounds() throws Exception {
        var stream = CustomStepSoundParityTest.class.getClassLoader()
                .getResourceAsStream("assets/thaumcraft/sounds.json");
        assertNotNull(stream, "the sound document of the port is missing");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject sounds = JsonParser.parseReader(reader).getAsJsonObject();
            assertTrue(sounds.size() > 0, "the sound document of the port is empty");
            return sounds;
        }
    }
}
