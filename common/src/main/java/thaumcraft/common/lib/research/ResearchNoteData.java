package thaumcraft.common.lib.research;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import thaumcraft.api.aspects.Aspect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Immutable native note data. Cell order and the saved color are retained;
 * holes are absent cells, and a solved note may contain only its connected cells.
 * This component is not a reader for legacy item NBT or a proof of puzzle completion.
 */
public record ResearchNoteData(String key, int color, boolean complete, int copies, List<Cell> cells) {
    public static final int MAX_KEY_LENGTH = 128;
    public static final int MAX_ASPECT_LENGTH = 128;
    public static final int MAX_RADIUS = 4;
    public static final int MAX_CELLS = 61;
    /** Bounds one framed component, independently of other components in the same buffer. */
    public static final int MAX_ENCODED_BYTES = 32_768;

    private static final Codec<List<Cell>> CELLS_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<List<Cell>, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getStream(input).flatMap(stream -> {
                List<T> raw;
                try (stream) {
                    // DFU's ordinary bounded ListCodec still collects every rejected
                    // excess element. Inspect only one beyond the maximum instead.
                    raw = stream.limit(MAX_CELLS + 1L).toList();
                }
                if (raw.isEmpty() || raw.size() > MAX_CELLS) {
                    return DataResult.error(() -> "Invalid research note cell count");
                }
                DataResult<List<Cell>> result = DataResult.success(new ArrayList<>(raw.size()));
                for (T element : raw) {
                    result = result.flatMap(cells -> Cell.CODEC.parse(ops, element).map(cell -> {
                        cells.add(cell);
                        return cells;
                    }));
                }
                return result.map(cells -> Pair.of(List.copyOf(cells), ops.empty()));
            });
        }

        @Override
        public <T> DataResult<T> encode(List<Cell> cells, DynamicOps<T> ops, T prefix) {
            return Codec.list(Cell.CODEC, 1, MAX_CELLS).encode(cells, ops, prefix);
        }
    };

    private static final Codec<RawNote> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.string(1, MAX_KEY_LENGTH).fieldOf("key").forGetter(RawNote::key),
            Codec.INT.fieldOf("color").forGetter(RawNote::color),
            Codec.BOOL.fieldOf("complete").forGetter(RawNote::complete),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("copies").forGetter(RawNote::copies),
            CELLS_CODEC.fieldOf("hexgrid").forGetter(RawNote::cells)
    ).apply(instance, RawNote::new));

    public static final Codec<ResearchNoteData> CODEC = RAW_CODEC.comapFlatMap(
            raw -> validated(() -> new ResearchNoteData(raw.key, raw.color, raw.complete, raw.copies, raw.cells)),
            note -> new RawNote(note.key, note.color, note.complete, note.copies, note.cells));

    /**
     * Length-prefixed body: bounded key, saved color, complete, copies, bounded
     * count, then signed q/r/type bytes and an aspect only for occupied cells.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchNoteData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ResearchNoteData decode(RegistryFriendlyByteBuf buffer) {
            try {
                int byteCount = buffer.readVarInt();
                if (byteCount <= 0 || byteCount > MAX_ENCODED_BYTES) {
                    throw new DecoderException("Invalid research note byte count: " + byteCount);
                }
                if (byteCount > buffer.readableBytes()) throw new DecoderException("Truncated research note body");
                // A slice is a view: no byte array is allocated from an untrusted length.
                RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(buffer.readSlice(byteCount), buffer.registryAccess());
                String key = body.readUtf(MAX_KEY_LENGTH);
                validateKey(key);
                int color = body.readInt();
                boolean complete = body.readBoolean();
                int copies = body.readVarInt();
                if (copies < 0) throw new DecoderException("Negative research note copies");
                int count = body.readVarInt();
                if (count < 1 || count > MAX_CELLS) {
                    throw new DecoderException("Invalid research note cell count: " + count);
                }
                // Every cell needs at least three bytes, even before any aspect string.
                if (count > body.readableBytes() / 3) throw new DecoderException("Truncated research note cells");
                List<Cell> cells = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int q = body.readByte();
                    int r = body.readByte();
                    int type = body.readByte();
                    if (type < 0 || type > 2) throw new DecoderException("Invalid research note cell type: " + type);
                    String aspect = type == 0 ? null : body.readUtf(MAX_ASPECT_LENGTH);
                    cells.add(new Cell(q, r, type, aspect));
                }
                if (body.isReadable()) throw new DecoderException("Trailing bytes in research note body");
                return new ResearchNoteData(key, color, complete, copies, cells);
            } catch (DecoderException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw new DecoderException("Invalid research note data", exception);
            }
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ResearchNoteData note) {
            if (note == null) throw new EncoderException("Missing research note data");
            RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(256, MAX_ENCODED_BYTES), buffer.registryAccess());
            try {
                body.writeUtf(note.key, MAX_KEY_LENGTH);
                body.writeInt(note.color);
                body.writeBoolean(note.complete);
                body.writeVarInt(note.copies);
                body.writeVarInt(note.cells.size());
                for (Cell cell : note.cells) {
                    body.writeByte(cell.q);
                    body.writeByte(cell.r);
                    body.writeByte(cell.type);
                    if (cell.type != 0) body.writeUtf(cell.aspect, MAX_ASPECT_LENGTH);
                }
                buffer.writeVarInt(body.readableBytes());
                buffer.writeBytes(body);
            } finally {
                body.release();
            }
        }
    };

    public ResearchNoteData {
        validateKey(key);
        if (copies < 0) throw new IllegalArgumentException("Negative research note copies");
        if (cells == null || cells.isEmpty() || cells.size() > MAX_CELLS) {
            throw new IllegalArgumentException("Research notes require between 1 and " + MAX_CELLS + " cells");
        }
        HashSet<String> coordinates = new HashSet<>();
        int fixed = 0;
        for (Cell cell : cells) {
            if (cell == null) throw new IllegalArgumentException("Null research note cell");
            if (!coordinates.add(cell.coordinate())) throw new IllegalArgumentException("Duplicate research note cell: " + cell.coordinate());
            if (cell.type == 1) fixed++;
        }
        if (fixed == 0) throw new IllegalArgumentException("Research notes require a fixed endpoint");
        cells = List.copyOf(cells);
    }

    public Cell cellAt(int q, int r) {
        for (Cell cell : cells) if (cell.q == q && cell.r == r) return cell;
        return null;
    }

    public ResearchNoteData withCells(List<Cell> cells, boolean complete) {
        return new ResearchNoteData(key, color, complete, copies, cells);
    }

    private static void validateKey(String key) {
        if (key == null || key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Invalid research note key length");
        }
        ResearchDefinition research = ResearchCatalog.get(key);
        if (research == null || research.aspectCost().isEmpty()) {
            throw new IllegalArgumentException("Unknown or aspectless research note key: " + key);
        }
    }

    private static <T> DataResult<T> validated(Supplier<T> factory) {
        try {
            return DataResult.success(factory.get());
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    public record Cell(int q, int r, int type, String aspect) {
        private static final Codec<RawCell> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(-MAX_RADIUS, MAX_RADIUS).fieldOf("hexq").forGetter(RawCell::q),
                Codec.intRange(-MAX_RADIUS, MAX_RADIUS).fieldOf("hexr").forGetter(RawCell::r),
                Codec.intRange(0, 2).fieldOf("type").forGetter(RawCell::type),
                Codec.string(1, MAX_ASPECT_LENGTH).optionalFieldOf("aspect").forGetter(RawCell::aspect)
        ).apply(instance, RawCell::new));
        public static final Codec<Cell> CODEC = RAW_CODEC.comapFlatMap(
                raw -> validated(() -> new Cell(raw.q, raw.r, raw.type, raw.aspect.orElse(null))),
                cell -> new RawCell(cell.q, cell.r, cell.type, Optional.ofNullable(cell.aspect)));

        public Cell {
            if (q < -MAX_RADIUS || q > MAX_RADIUS || r < -MAX_RADIUS || r > MAX_RADIUS
                    || Math.abs(q + r) > MAX_RADIUS) {
                throw new IllegalArgumentException("Research note coordinate exceeds radius " + MAX_RADIUS);
            }
            if (type < 0 || type > 2) throw new IllegalArgumentException("Invalid research note cell type: " + type);
            if (type == 0) {
                if (aspect != null) throw new IllegalArgumentException("Blank research note cells cannot have an aspect");
            } else if (aspect == null || aspect.isEmpty() || aspect.length() > MAX_ASPECT_LENGTH || Aspect.getAspect(aspect) == null) {
                throw new IllegalArgumentException("Unknown research note aspect: " + aspect);
            }
        }

        public String coordinate() { return q + ":" + r; }
    }

    private record RawNote(String key, int color, boolean complete, int copies, List<Cell> cells) {}
    private record RawCell(int q, int r, int type, Optional<String> aspect) {}
}
