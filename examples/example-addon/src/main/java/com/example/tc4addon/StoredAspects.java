package com.example.tc4addon;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The aspects an item carries, as the value of a data component. {@code AspectList} is mutable and compares by identity (as
 * in the original), so a component value wraps it: the wrapper keeps its own copy of the positive entries and compares by
 * content, which lets two stacks with the same aspects stack and match.
 */
// [doc:aspect-component]
public record StoredAspects(AspectList aspects) {
    public static final StoredAspects NONE = new StoredAspects(new AspectList());
    // The codecs of the API do the saving and the syncing; the wrapper only converts.
    public static final Codec<StoredAspects> CODEC = AspectList.CODEC.xmap(StoredAspects::new, StoredAspects::aspects);
    public static final StreamCodec<? super RegistryFriendlyByteBuf, StoredAspects> STREAM_CODEC =
            AspectList.STREAM_CODEC.map(StoredAspects::new, StoredAspects::aspects);

    public StoredAspects {
        // What the codecs would not write (an amount of zero, which AspectList.reduce leaves behind) is not part of the value.
        AspectList positive = new AspectList();
        for (Aspect aspect : aspects.getAspects()) {
            if (aspects.getAmount(aspect) > 0) positive.add(aspect, aspects.getAmount(aspect));
        }
        aspects = positive;
    }

    @Override public AspectList aspects() { return aspects.copy(); }

    @Override public boolean equals(Object other) {
        return other instanceof StoredAspects stored && aspects.aspects.equals(stored.aspects.aspects);
    }

    @Override public int hashCode() { return aspects.aspects.hashCode(); }
}
// [/doc]
