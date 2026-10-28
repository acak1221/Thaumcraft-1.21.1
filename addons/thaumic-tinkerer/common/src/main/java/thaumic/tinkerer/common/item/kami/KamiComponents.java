/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the data components that replace the stack NBT
 * and the damage value of the Kami items. Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see
 * addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The data components of the Kami gear.
 *
 * <p>1.7.10 kept these values in the stack tag ({@code blockName}, {@code blockMeta}, {@code blockCount}, {@code size},
 * {@code x}/{@code y}/{@code z}/{@code dim}, {@code Inventory}) or in the damage value (tool mode, armour piece
 * switched off, talisman active). Metadata no longer exists, so the Block Talisman and the Placement Mirror store the
 * whole block state, and the damage values become the three small components below.</p>
 *
 * <p>The components are registered by {@link #register()}, called from the registrar only when the Kami content is
 * enabled; reading a field before that is a programming error.</p>
 */
public final class KamiComponents {
    /** The mode of an awakened tool: 0, 1 or 2 (1.7.10: the damage value of the tool). */
    public static RegistrySupplier<DataComponentType<Integer>> TOOL_MODE;
    /** True when this awakened armour piece is switched off (1.7.10: damage value 1). */
    public static RegistrySupplier<DataComponentType<Boolean>> PIECE_DISABLED;
    /** True while the Black Hole Ring is active (1.7.10: damage value 1). */
    public static RegistrySupplier<DataComponentType<Boolean>> TALISMAN_ACTIVE;
    /** The block the Black Hole Ring condenses and how many of it it holds. */
    public static RegistrySupplier<DataComponentType<TalismanStore>> TALISMAN_STORE;
    /** The block state the Placement Mirror places. */
    public static RegistrySupplier<DataComponentType<BlockState>> MIRROR_STATE;
    /** The edge length of the Placement Mirror's square: 3, 5, 7, 9 or 11. */
    public static RegistrySupplier<DataComponentType<Integer>> MIRROR_SIZE;
    /** Where a Celestial Pearl is attuned to; absent for an unattuned pearl. */
    public static RegistrySupplier<DataComponentType<PearlTarget>> PEARL_TARGET;
    /** The 117 slots of the Bottomless Pouch. */
    public static RegistrySupplier<DataComponentType<ItemContainerContents>> POUCH_CONTENTS;

    private static boolean registered;

    private KamiComponents() {}

    /** What a Black Hole Ring holds: a block and how many of it (1.7.10: {@code blockName}, {@code blockCount}). */
    public record TalismanStore(ResourceLocation block, int count) {
        public static final Codec<TalismanStore> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("block").forGetter(TalismanStore::block),
                Codec.INT.fieldOf("count").forGetter(TalismanStore::count)).apply(i, TalismanStore::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, TalismanStore> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, TalismanStore::block, ByteBufCodecs.VAR_INT, TalismanStore::count,
                TalismanStore::new);

        public TalismanStore withCount(int newCount) {
            return new TalismanStore(block, newCount);
        }
    }

    /** The place a Celestial Pearl leads to (1.7.10: {@code x}, {@code y}, {@code z}, {@code dim}). */
    public record PearlTarget(ResourceKey<Level> dimension, BlockPos pos) {
        public static final Codec<PearlTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(PearlTarget::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(PearlTarget::pos)).apply(i, PearlTarget::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, PearlTarget> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION), PearlTarget::dimension, BlockPos.STREAM_CODEC,
                PearlTarget::pos, PearlTarget::new);
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        TOOL_MODE = TTRegistry.DATA_COMPONENT_TYPES.register("kami_tool_mode",
                () -> DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 2))
                        .networkSynchronized(ByteBufCodecs.VAR_INT).build());
        PIECE_DISABLED = TTRegistry.DATA_COMPONENT_TYPES.register("kami_piece_disabled",
                () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL)
                        .networkSynchronized(ByteBufCodecs.BOOL).build());
        TALISMAN_ACTIVE = TTRegistry.DATA_COMPONENT_TYPES.register("kami_talisman_active",
                () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL)
                        .networkSynchronized(ByteBufCodecs.BOOL).build());
        TALISMAN_STORE = TTRegistry.DATA_COMPONENT_TYPES.register("kami_talisman_store",
                () -> DataComponentType.<TalismanStore>builder().persistent(TalismanStore.CODEC)
                        .networkSynchronized(TalismanStore.STREAM_CODEC).build());
        MIRROR_STATE = TTRegistry.DATA_COMPONENT_TYPES.register("kami_mirror_state",
                () -> DataComponentType.<BlockState>builder().persistent(BlockState.CODEC)
                        .networkSynchronized(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY)).build());
        MIRROR_SIZE = TTRegistry.DATA_COMPONENT_TYPES.register("kami_mirror_size",
                () -> DataComponentType.<Integer>builder().persistent(Codec.intRange(1, 11))
                        .networkSynchronized(ByteBufCodecs.VAR_INT).build());
        PEARL_TARGET = TTRegistry.DATA_COMPONENT_TYPES.register("kami_pearl_target",
                () -> DataComponentType.<PearlTarget>builder().persistent(PearlTarget.CODEC)
                        .networkSynchronized(PearlTarget.STREAM_CODEC).build());
        POUCH_CONTENTS = TTRegistry.DATA_COMPONENT_TYPES.register("kami_pouch_contents",
                () -> DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC)
                        .networkSynchronized(ItemContainerContents.STREAM_CODEC).build());
    }
}
