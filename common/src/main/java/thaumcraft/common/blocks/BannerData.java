package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * The data of the original banner item.
 *
 * <p>Original BlockWoodenDevice:121-124 writes the NBT byte {@code color} and the original
 * BlockWoodenDevice:425-428 writes the NBT string {@code aspect} of the dropped banner; the native
 * banner item carries both in one data component.</p>
 */
public record BannerData(int color, String aspectTag) {
    /** A banner without a colour and without an aspect: the original colour byte minus one. */
    public static final BannerData NONE = new BannerData(EldritchBlockRules.BANNER_COLOUR_NONE, "");
    public static final Codec<BannerData> CODEC = RecordCodecBuilder.<BannerData>create(instance -> instance.group(
            Codec.intRange(EldritchBlockRules.BANNER_COLOUR_NONE, 15).fieldOf("color").forGetter(BannerData::color),
            Codec.STRING.optionalFieldOf("aspect", "").forGetter(BannerData::aspectTag)
    ).apply(instance, BannerData::new));
    public static final StreamCodec<ByteBuf, BannerData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public BannerData {
        if (color < EldritchBlockRules.BANNER_COLOUR_NONE || color > 15) throw new IllegalArgumentException("Invalid banner colour: " + color);
        aspectTag = aspectTag == null ? "" : aspectTag;
    }

    /** True when the banner carries one of the sixteen original colours. */
    public boolean hasColor() { return color >= 0; }

    /** The aspect of the banner, or null when it carries none. */
    public Aspect aspect() {
        if (aspectTag.isEmpty()) return null;
        return Aspect.getAspect(aspectTag);
    }
}
