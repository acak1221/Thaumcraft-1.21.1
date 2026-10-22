package thaumcraft.common.lib.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.OriginalRegenSettings;

/**
 * The chunk marker of the original and the chunk regeneration queue.
 *
 * <p>Oracle {@code thaumcraft/common/lib/events/EventHandlerWorld.java:114-137} writes the
 * compound {@code Thaumcraft} with the key of the regeneration category into the NBT of every
 * chunk it saves and reads it back when the chunk is loaded, and oracle
 * {@code thaumcraft/common/lib/events/ServerTickEventsFML.java:83-104} regenerates ten chunks
 * of the dimension per world tick with the random of the original.</p>
 *
 * <p>The expected seeds below were measured on the JVM with the formula of the oracle, they are
 * not a transcription of the port.</p>
 */
class ChunkRegenerationParityTest {
    @Test void markerPresenceNotTruthControlsRegeneration() {
        // Oracle EventHandlerWorld:125 calls hasKey, NOT getBoolean.
        CompoundTag chunk = new CompoundTag();
        CompoundTag marker = new CompoundTag();
        marker.putBoolean("DEFAULT", false);
        chunk.put("Thaumcraft", marker);
        assertTrue(ChunkRegeneration.hasMarker(chunk, "DEFAULT"));
        assertFalse(ChunkRegeneration.hasMarker(chunk, "CHANGED"));
        assertFalse(ChunkRegeneration.hasMarker(new CompoundTag(), "DEFAULT"));
        marker.putString("CHANGED", "a present key still counts");
        assertTrue(ChunkRegeneration.hasMarker(chunk, "CHANGED"));
    }

    @Test void theMarkerIsTheCompoundOfTheOriginal() {
        CompoundTag tag = new CompoundTag();
        ChunkRegeneration.writeMarker(tag);
        assertTrue(tag.contains("Thaumcraft"), "the compound of the original is Thaumcraft");
        assertTrue(tag.getCompound("Thaumcraft").getBoolean("DEFAULT"),
                "the key of the original is the regen key of the configuration");
        assertEquals("Thaumcraft", ChunkRegeneration.COMPOUND);
    }

    @Test void theChunkWithoutTheMarkerIsNotMarked() {
        CompoundTag tag = new CompoundTag();
        assertFalse(tag.getCompound("Thaumcraft").getBoolean("DEFAULT"),
                "a chunk of a world without Thaumcraft carries no marker");
    }

    @Test void theChunkSeedIsTheDrawOfTheOriginal() {
        // Oracle ServerTickEventsFML:91-95 - the random of the world, two seeds shifted
        // right by three bits, the chunk and the world seed.
        assertEquals(0L, ChunkRegeneration.chunkSeed(0L, 0, 0));
        assertEquals(-620346058209547737L, ChunkRegeneration.chunkSeed(0L, 1, 0));
        assertEquals(554639222630723095L, ChunkRegeneration.chunkSeed(0L, 0, 1));
        assertEquals(-4765647958939907970L, ChunkRegeneration.chunkSeed(0L, 5, -3));
        assertEquals(-3914656168740558599L, ChunkRegeneration.chunkSeed(0L, -17, 240));
        assertEquals(123456789L, ChunkRegeneration.chunkSeed(123456789L, 0, 0));
        assertEquals(-774675394086904907L, ChunkRegeneration.chunkSeed(123456789L, 1, 0));
        assertEquals(1053659076337871171L, ChunkRegeneration.chunkSeed(123456789L, 0, 1));
        assertEquals(-7034354199560058613L, ChunkRegeneration.chunkSeed(123456789L, 5, -3));
        assertEquals(7793242960979320085L, ChunkRegeneration.chunkSeed(123456789L, -17, 240));
        assertEquals(-42L, ChunkRegeneration.chunkSeed(-42L, 0, 0));
        assertEquals(-628608477515251950L, ChunkRegeneration.chunkSeed(-42L, 1, 0));
        assertEquals(-140540512405832351L, ChunkRegeneration.chunkSeed(-42L, 0, 1));
    }

    @Test void theQueueDrainsTenChunksPerWorldTick() {
        // Oracle ServerTickEventsFML:88 - the loop of the original runs ten times.
        assertEquals(10, ChunkRegeneration.CHUNKS_PER_TICK);
    }

    @Test void everyRegenerationOptionIsOffByDefault() {
        // Oracle Config:381-386 - the six options of the regeneration category are off.
        OriginalRegenSettings defaults = OriginalRegenSettings.DEFAULTS;
        assertFalse(defaults.auraNodes());
        assertFalse(defaults.structures());
        assertFalse(defaults.cinnibarOre());
        assertFalse(defaults.amberOre());
        assertFalse(defaults.infusedStone());
        assertFalse(defaults.trees());
        assertEquals("DEFAULT", defaults.regenKey());
        assertFalse(defaults.anyRegeneration(),
                "without an option the original never queues a chunk");
    }

    @Test void theRegenerationOptionsAreReadFromTheConfiguration() {
        Properties values = new Properties();
        values.setProperty("aura_nodes", "true");
        OriginalRegenSettings settings = OriginalRegenSettings.read(values);
        assertTrue(settings.auraNodes());
        assertTrue(settings.anyRegeneration());
        assertEquals("DEFAULT", settings.regenKey());
        values.setProperty("regen_key", "SECOND");
        assertEquals("SECOND", OriginalRegenSettings.read(values).regenKey());
        values.setProperty("trees", "garbage");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> OriginalRegenSettings.read(values));
    }
}
