package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Fixed four native ItemStacks; ordinary menu contents still use vanilla packets. */
public record AccessoryContentsPayload(List<ItemStack> stacks) implements CustomPacketPayload {
    public static final Type<AccessoryContentsPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","accessories_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AccessoryContentsPayload> STREAM_CODEC=StreamCodec.of((buf,p)->{
        for(ItemStack stack:p.stacks())ItemStack.OPTIONAL_STREAM_CODEC.encode(buf,stack);
    },buf->{var stacks=new ArrayList<ItemStack>(4);for(int i=0;i<4;i++)stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));if(buf.readableBytes()!=0)throw new DecoderException("Trailing accessory contents");return new AccessoryContentsPayload(stacks);});
    public AccessoryContentsPayload {if(stacks.size()!=4)throw new IllegalArgumentException("Four accessory slots required");stacks=stacks.stream().map(ItemStack::copy).toList();}
    @Override public Type<AccessoryContentsPayload> type(){return TYPE;}
}
