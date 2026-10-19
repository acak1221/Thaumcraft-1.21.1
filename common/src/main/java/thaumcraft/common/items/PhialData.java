package thaumcraft.common.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import thaumcraft.api.aspects.Aspect;

/** TC4 phials always contain eight units of exactly one aspect. */
public record PhialData(String tag) {
    public static final int AMOUNT = 8;
    public static final Codec<PhialData> CODEC = Codec.STRING.comapFlatMap(
            tag -> Aspect.getAspect(tag) != null ? DataResult.success(new PhialData(tag))
                    : DataResult.error(() -> "Unknown phial aspect: " + tag), PhialData::tag);
    public static final StreamCodec<ByteBuf, PhialData> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(PhialData::new, PhialData::tag);
    public PhialData {
        if (tag == null || Aspect.getAspect(tag) == null) throw new IllegalArgumentException("Unknown phial aspect: " + tag);
    }
}
