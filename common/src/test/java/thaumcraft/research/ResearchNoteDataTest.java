package thaumcraft.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ResearchNoteDataTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @Test void cellsAreDefensivelyCopiedInTheirOriginalOrder() {
        ArrayList<Cell> input = new ArrayList<>(List.of(new Cell(1, -1, 2, "praecantatio"),
                new Cell(0, 0, 1, "ignis"), new Cell(-1, 0, 0, null)));
        ResearchNoteData note = new ResearchNoteData("FOCUSFIRE", 0x80ff1122, false, 2, input);
        input.clear();
        assertEquals(List.of("1:-1", "0:0", "-1:0"), note.cells().stream().map(Cell::coordinate).toList());
        assertThrows(UnsupportedOperationException.class, () -> note.cells().clear());
    }

    @Test void exactCoordinateLookupDistinguishesBlankCellsAndAbsentHoles() {
        ResearchNoteData note = note();
        assertEquals(new Cell(0, 0, 1, "ignis"), note.cellAt(0, 0));
        assertEquals(new Cell(1, 0, 0, null), note.cellAt(1, 0));
        assertNull(note.cellAt(-1, 0));
        assertNull(note.cellAt(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    @Test void replacingCellsPreservesIdentitySavedColorAndCopyCount() {
        ResearchNoteData original = new ResearchNoteData("FOCUSFIRE", Integer.MIN_VALUE, false, 17, note().cells());
        ResearchNoteData solved = original.withCells(List.of(original.cellAt(0, 0)), true);
        assertEquals(original.key(), solved.key());
        assertEquals(original.color(), solved.color());
        assertEquals(original.copies(), solved.copies());
        assertTrue(solved.complete());
        assertFalse(original.complete());
        assertEquals(3, original.cells().size());
        assertEquals(1, solved.cells().size());
    }

    @Test void structuralValidationAllowsSingleFixedEndpointAndSyntheticInteriorCoordinates() {
        ResearchNoteData compact = new ResearchNoteData("FOCUSFIRE", -1, true, 0,
                List.of(new Cell(0, 0, 1, "aer")));
        assertTrue(compact.complete());
        assertEquals("aer", compact.cellAt(0, 0).aspect());
    }

    @Test void everyAspectfulCatalogKeyIsAcceptedWithoutRenamingOrPrimaryModeGuesses() {
        int accepted = 0;
        for (var research : ResearchCatalog.all()) {
            if (research.aspectCost().isEmpty()) continue;
            var data = new ResearchNoteData(research.key(), 0, false, 0, List.of(new Cell(0, 0, 1, "aer")));
            assertEquals(research.key(), data.key());
            accepted++;
        }
        assertEquals(172, accepted);
    }

    @Test void unknownNoncanonicalAndAspectlessKeysAreRejected() {
        for (String key : Arrays.asList(null, "", "focusfire", "@FOCUSFIRE", "NO_SUCH_RESEARCH", "x".repeat(129))) {
            assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData(key, 0, false, 0, note().cells()));
        }
        String aspectless = ResearchCatalog.all().stream().filter(row -> row.aspectCost().isEmpty()).findFirst().orElseThrow().key();
        assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData(aspectless, 0, false, 0, note().cells()));
    }

    @Test void originalIntegerColorAndNonnegativeCopyCountAreRetainedWithoutClamping() {
        for (int color : new int[]{Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE}) {
            ResearchNoteData data = new ResearchNoteData("FOCUSFIRE", color, false, Integer.MAX_VALUE, note().cells());
            assertEquals(color, data.color());
            assertEquals(Integer.MAX_VALUE, data.copies());
        }
        assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData("FOCUSFIRE", 0, false, -1, note().cells()));
    }

    @Test void blankOrPlacedCellsCannotReplaceEveryFixedEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData("FOCUSFIRE", 0, true, 0,
                List.of(new Cell(0, 0, 0, null), new Cell(1, 0, 2, "ignis"))));
        assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData("FOCUSFIRE", 0, false, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData("FOCUSFIRE", 0, false, 0, null));
    }

    @Test void duplicateCoordinatesAndNullCellsAreRejected() {
        Cell fixed = new Cell(0, 0, 1, "ignis");
        assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData("FOCUSFIRE", 0, false, 0,
                List.of(fixed, new Cell(0, 0, 0, null))));
        assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData("FOCUSFIRE", 0, false, 0,
                Arrays.asList(fixed, null)));
    }

    @Test void fullRadiusFourGridHasExactlySixtyOnePermittedCoordinates() {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int q = -4; q <= 4; q++) {
            for (int r = -4; r <= 4; r++) {
                if (Math.abs(q + r) <= 4) cells.add(new Cell(q, r, q == 0 && r == 0 ? 1 : 0, q == 0 && r == 0 ? "ignis" : null));
            }
        }
        assertEquals(61, cells.size());
        assertEquals(61, new ResearchNoteData("FOCUSFIRE", 0, false, 0, cells).cells().size());
        var error = assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData("FOCUSFIRE", 0, false, 0,
                Collections.nCopies(62, new Cell(0, 0, 1, "ignis"))));
        assertTrue(error.getMessage().contains("61"));
    }

    @Test void axialDistanceAndExtremeCoordinatesAreRejectedWithoutIntegerOverflow() {
        for (int[] pair : new int[][]{{4, 1}, {-4, -1}, {5, -1}, {-5, 1}, {0, 5}, {0, -5},
                {Integer.MAX_VALUE, 1}, {Integer.MIN_VALUE, -1}}) {
            assertThrows(IllegalArgumentException.class, () -> new Cell(pair[0], pair[1], 0, null));
        }
        assertEquals("4:-4", new Cell(4, -4, 0, null).coordinate());
        assertEquals("-4:4", new Cell(-4, 4, 0, null).coordinate());
    }

    @Test void typesAndAspectNullabilityAreValidatedTogether() {
        for (int type : new int[]{-1, 3, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new Cell(0, 0, type, "ignis"));
        }
        assertThrows(IllegalArgumentException.class, () -> new Cell(0, 0, 0, "ignis"));
        for (int type : new int[]{1, 2}) {
            for (String aspect : Arrays.asList(null, "", "IGNIS", "not_an_aspect", "x".repeat(129))) {
                assertThrows(IllegalArgumentException.class, () -> new Cell(0, 0, type, aspect));
            }
        }
    }

    @Test void persistentNbtRoundTripRetainsOriginalFieldMeaningAndBlankOmission() {
        ResearchNoteData original = new ResearchNoteData("FOCUSFIRE", Integer.MIN_VALUE, true, 7, note().cells());
        CompoundTag tag = (CompoundTag) ResearchNoteData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        assertEquals("FOCUSFIRE", tag.getString("key"));
        assertEquals(Integer.MIN_VALUE, tag.getInt("color"));
        assertTrue(tag.getBoolean("complete"));
        assertEquals(7, tag.getInt("copies"));
        var grid = tag.getList("hexgrid", 10);
        assertEquals(3, grid.size());
        assertFalse(grid.getCompound(1).contains("aspect"));
        assertEquals("praecantatio", grid.getCompound(2).getString("aspect"));
        assertEquals(original, ResearchNoteData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow());
    }

    @Test void persistentJsonRoundTripPreservesCellOrderAndTheCompleteFlag() {
        ResearchNoteData original = note();
        JsonElement json = ResearchNoteData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        assertEquals(original, ResearchNoteData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(1, json.getAsJsonObject().getAsJsonArray("hexgrid").get(1).getAsJsonObject().get("hexq").getAsInt());
    }

    @Test void malformedPersistentPrimitiveFieldsReturnErrorsInsteadOfThrowingConstructors() {
        for (String key : new String[]{"key", "color", "complete", "copies", "hexgrid"}) {
            JsonObject json = jsonNote();
            json.remove(key);
            assertPersistentError(json);
        }
        for (String key : new String[]{"", "focusfire", "NO_SUCH_RESEARCH", "x".repeat(129)}) {
            JsonObject json = jsonNote();
            json.addProperty("key", key);
            assertPersistentError(json);
        }
        JsonObject json = jsonNote();
        json.addProperty("copies", -1);
        assertPersistentError(json);
        json = jsonNote();
        json.addProperty("color", "not an integer");
        assertPersistentError(json);
    }

    @Test void malformedPersistentCellsReturnErrorsForBothJsonAndNbt() {
        for (Consumer<JsonObject> change : List.<Consumer<JsonObject>>of(
                cell -> { cell.addProperty("hexq", 4); cell.addProperty("hexr", 1); },
                cell -> cell.addProperty("hexr", 5),
                cell -> cell.addProperty("type", 3),
                cell -> cell.remove("aspect"),
                cell -> cell.addProperty("aspect", "UNKNOWN"),
                cell -> cell.addProperty("aspect", "x".repeat(129)))) {
            JsonObject json = jsonNote();
            JsonObject cell = json.getAsJsonArray("hexgrid").get(2).getAsJsonObject();
            change.accept(cell);
            assertPersistentError(json);
        }
        JsonObject blankWithAspect = jsonNote();
        blankWithAspect.getAsJsonArray("hexgrid").get(1).getAsJsonObject().addProperty("aspect", "ignis");
        assertPersistentError(blankWithAspect);
    }

    @Test void persistentGridBoundsDuplicateCoordinatesAndMissingEndpointsAreRejected() {
        JsonObject empty = jsonNote();
        empty.add("hexgrid", new JsonArray());
        assertPersistentError(empty);
        JsonObject oversized = jsonNote();
        JsonArray grid = new JsonArray();
        for (int i = 0; i < 62; i++) grid.add(jsonNote().getAsJsonArray("hexgrid").get(0).deepCopy());
        oversized.add("hexgrid", grid);
        assertPersistentError(oversized);
        JsonObject duplicate = jsonNote();
        duplicate.getAsJsonArray("hexgrid").add(duplicate.getAsJsonArray("hexgrid").get(0).deepCopy());
        assertPersistentError(duplicate);
        JsonObject noFixed = jsonNote();
        noFixed.getAsJsonArray("hexgrid").get(0).getAsJsonObject().addProperty("type", 2);
        assertPersistentError(noFixed);
    }

    @Test void networkRoundTripCoversAllCanonicalAspectsAndPreservesCompleteCopiesAndColor() {
        for (Aspect aspect : Aspect.aspects.values()) {
            ResearchNoteData original = new ResearchNoteData("FOCUSFIRE", Integer.MIN_VALUE, true, Integer.MAX_VALUE,
                    List.of(new Cell(-4, 4, 1, aspect.getTag()), new Cell(0, 0, 0, null), new Cell(1, -1, 2, "praecantatio")));
            RegistryFriendlyByteBuf buffer = buffer();
            try {
                ResearchNoteData.STREAM_CODEC.encode(buffer, original);
                assertEquals(original, ResearchNoteData.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally { buffer.release(); }
        }
    }

    @Test void framedDecodeLeavesTheFollowingComponentUntouched() {
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            ResearchNoteData.STREAM_CODEC.encode(buffer, note());
            buffer.writeInt(0x12345678);
            assertEquals(note(), ResearchNoteData.STREAM_CODEC.decode(buffer));
            assertEquals(0x12345678, buffer.readInt());
            assertFalse(buffer.isReadable());
        } finally { buffer.release(); }
    }

    @Test void networkByteCountIsBoundedBeforeSlicingOrAllocatingABody() {
        for (int length : new int[]{-1, 0, ResearchNoteData.MAX_ENCODED_BYTES + 1, Integer.MAX_VALUE}) {
            RegistryFriendlyByteBuf buffer = buffer();
            try {
                buffer.writeVarInt(length);
                assertThrows(DecoderException.class, () -> ResearchNoteData.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally { buffer.release(); }
        }
        RegistryFriendlyByteBuf truncated = buffer();
        try {
            truncated.writeVarInt(5);
            truncated.writeByte(0);
            assertThrows(DecoderException.class, () -> ResearchNoteData.STREAM_CODEC.decode(truncated));
            assertEquals(1, truncated.readableBytes(), "Truncated frame is rejected before consuming its body");
        } finally { truncated.release(); }
    }

    @Test void networkCellCountIsBoundedBeforeCreatingTheCellList() {
        for (int count : new int[]{-1, 0, 62, Integer.MAX_VALUE}) {
            assertWireError(body -> {
                writeHeader(body, "FOCUSFIRE", 0, count);
                body.writeZero(3);
            });
        }
        assertWireError(body -> writeHeader(body, "FOCUSFIRE", 0, 61));
    }

    @Test void networkStringsEnforceCharacterAndEncodedLengthBounds() {
        assertWireError(body -> {
            writeHeader(body, "x".repeat(129), 0, 1);
            writeCell(body, 0, 0, 1, "ignis");
        });
        assertWireError(body -> {
            writeHeader(body, "FOCUSFIRE", 0, 1);
            writeCell(body, 0, 0, 1, "x".repeat(129));
        });
        assertWireError(body -> {
            writeHeader(body, "FOCUSFIRE", 0, 1);
            body.writeByte(0).writeByte(0).writeByte(1);
            body.writeVarInt(ResearchNoteData.MAX_ASPECT_LENGTH * 3 + 1);
        });
    }

    @Test void invalidNetworkTypesCoordinatesAndAspectsBecomeDecoderErrors() {
        for (int type : new int[]{-1, 3}) {
            assertWireError(body -> {
                writeHeader(body, "FOCUSFIRE", 0, 1);
                writeCell(body, 0, 0, type, "ignis");
            });
        }
        for (String aspect : new String[]{"", "IGNIS", "not_an_aspect"}) {
            assertWireError(body -> {
                writeHeader(body, "FOCUSFIRE", 0, 1);
                writeCell(body, 0, 0, 1, aspect);
            });
        }
        assertWireError(body -> {
            writeHeader(body, "FOCUSFIRE", 0, 1);
            writeCell(body, 4, 4, 1, "ignis");
        });
    }

    @Test void invalidNetworkIdentityCopiesAndEndpointStateAreRejected() {
        for (String key : new String[]{"focusfire", "NO_SUCH_RESEARCH"}) {
            assertWireError(body -> {
                writeHeader(body, key, 0, 1);
                writeCell(body, 0, 0, 1, "ignis");
            });
        }
        assertWireError(body -> {
            writeHeader(body, "FOCUSFIRE", -1, 1);
            writeCell(body, 0, 0, 1, "ignis");
        });
        assertWireError(body -> {
            writeHeader(body, "FOCUSFIRE", 0, 1);
            writeCell(body, 0, 0, 2, "ignis");
        });
        assertWireError(body -> {
            writeHeader(body, "FOCUSFIRE", 0, 2);
            writeCell(body, 0, 0, 1, "ignis");
            writeCell(body, 0, 0, 0, null);
        });
    }

    @Test void truncatedAspectAndTrailingBodyBytesAreRejected() {
        assertWireError(body -> {
            writeHeader(body, "FOCUSFIRE", 0, 1);
            body.writeByte(0).writeByte(0).writeByte(1);
            body.writeVarInt(5);
            body.writeByte('i');
        });
        assertWireError(body -> {
            writeHeader(body, "FOCUSFIRE", 0, 1);
            writeCell(body, 0, 0, 1, "ignis");
            body.writeByte(0);
        });
    }

    @Test void networkEncoderRejectsMissingComponentPredictably() {
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            assertThrows(EncoderException.class, () -> ResearchNoteData.STREAM_CODEC.encode(buffer, null));
            assertFalse(buffer.isReadable());
        } finally { buffer.release(); }
    }

    private static ResearchNoteData note() {
        return new ResearchNoteData("FOCUSFIRE", 0xff5a01, false, 0,
                List.of(new Cell(0, 0, 1, "ignis"), new Cell(1, 0, 0, null), new Cell(1, -1, 2, "praecantatio")));
    }

    private static JsonObject jsonNote() {
        return ResearchNoteData.CODEC.encodeStart(JsonOps.INSTANCE, note()).getOrThrow().getAsJsonObject();
    }

    private static void assertPersistentError(JsonObject json) {
        var jsonResult = assertDoesNotThrow(() -> ResearchNoteData.CODEC.parse(JsonOps.INSTANCE, json));
        assertTrue(jsonResult.error().isPresent(), json.toString());
        assertTrue(jsonResult.result().isEmpty(), json.toString());
        var nbt = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json);
        var nbtResult = assertDoesNotThrow(() -> ResearchNoteData.CODEC.parse(NbtOps.INSTANCE, nbt));
        assertTrue(nbtResult.error().isPresent(), json.toString());
        assertTrue(nbtResult.result().isEmpty(), json.toString());
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    private static void writeHeader(RegistryFriendlyByteBuf body, String key, int copies, int count) {
        body.writeUtf(key);
        body.writeInt(0xff5a01);
        body.writeBoolean(false);
        body.writeVarInt(copies);
        body.writeVarInt(count);
    }

    private static void writeCell(RegistryFriendlyByteBuf body, int q, int r, int type, String aspect) {
        body.writeByte(q).writeByte(r).writeByte(type);
        if (type != 0) body.writeUtf(aspect);
    }

    private static void assertWireError(Consumer<RegistryFriendlyByteBuf> writer) {
        RegistryFriendlyByteBuf body = buffer();
        RegistryFriendlyByteBuf framed = buffer();
        try {
            writer.accept(body);
            framed.writeVarInt(body.readableBytes());
            framed.writeBytes(body);
            assertThrows(DecoderException.class, () -> ResearchNoteData.STREAM_CODEC.decode(framed));
        } finally {
            framed.release();
            body.release();
        }
    }
}
