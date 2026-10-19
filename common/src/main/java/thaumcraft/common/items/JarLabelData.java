package thaumcraft.common.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import thaumcraft.api.aspects.Aspect;

/** A label remembers a type, never an amount of essentia. Absence means blank. */
public record JarLabelData(String tag) {
    public static final Codec<JarLabelData> CODEC = Codec.STRING.comapFlatMap(
            tag -> Aspect.getAspect(tag) != null ? DataResult.success(new JarLabelData(tag))
                    : DataResult.error(() -> "Unknown label aspect: " + tag), JarLabelData::tag);
    public static final StreamCodec<ByteBuf, JarLabelData> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(JarLabelData::new, JarLabelData::tag);

    public JarLabelData {
        if (tag == null || Aspect.getAspect(tag) == null) throw new IllegalArgumentException("Unknown label aspect: " + tag);
    }
}
