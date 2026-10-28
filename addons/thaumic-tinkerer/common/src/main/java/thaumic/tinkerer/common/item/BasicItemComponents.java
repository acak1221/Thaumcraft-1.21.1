/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the data components that replace the stack tag and
 * the damage value of the BasicItems group. Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see
 * addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The data components of the Experience Talisman, the Talisman of Remedium and the Tome of Knowledge Sharing.
 *
 * <p>1.7.10 kept the stored experience ({@code xp}) and the switch of the Cleansing Talisman ({@code enabled}) in the
 * stack tag, the switch of the Experience Talisman in the damage value (0 off, 1 on) and the tome's player and research
 * snapshot ({@code player}, {@code research}, {@code aspects}) in the stack tag. Each is a component now. A value equal to
 * the default is stored as no component, so a fresh stack and a stack that was switched off again are the same stack.</p>
 *
 * <p>The components are registered by {@link #register()}, called from the registrar of the group; reading a field before
 * that is a programming error.</p>
 */
public final class BasicItemComponents {
    /** The experience points stored in an Experience Talisman (tag {@code xp}); absent means 0. */
    public static RegistrySupplier<DataComponentType<Integer>> XP;
    /** True while an Experience Talisman absorbs orbs (1.7.10: damage value 1); absent means off. */
    public static RegistrySupplier<DataComponentType<Boolean>> ABSORBING;
    /** True while a Talisman of Remedium is switched on (tag {@code enabled}); absent means off. */
    public static RegistrySupplier<DataComponentType<Boolean>> CLEANSING_ENABLED;
    /** The player a Tome of Knowledge Sharing is bound to and the research written to it; absent for an unassigned tome. */
    public static RegistrySupplier<DataComponentType<ShareData>> SHARE_TOME;

    private BasicItemComponents() {}

    public static void register() {
        XP = TTRegistry.DATA_COMPONENT_TYPES.register("xp_talisman_xp", () -> DataComponentType.<Integer>builder()
                .persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        ABSORBING = TTRegistry.DATA_COMPONENT_TYPES.register("xp_talisman_absorbing", () -> DataComponentType
                .<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        CLEANSING_ENABLED = TTRegistry.DATA_COMPONENT_TYPES.register("cleansing_talisman_enabled", () -> DataComponentType
                .<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        SHARE_TOME = TTRegistry.DATA_COMPONENT_TYPES.register("share_tome", () -> DataComponentType.<ShareData>builder()
                .persistent(ShareData.CODEC).networkSynchronized(ShareData.STREAM_CODEC).build());
    }

    /**
     * What a Tome of Knowledge Sharing holds once it is bound: the name of the player (what the tooltip shows, 1.7.10 tag
     * {@code player}), the id of that player (1.21.1 players are found by id, names change), and the snapshot of the player's
     * completed research (tag {@code research}) and discovered aspects (tag {@code aspects}) the tome falls back on when the
     * player is not online.
     */
    public record ShareData(String player, UUID owner, List<String> research, List<String> aspects) {
        public static final Codec<ShareData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("player").forGetter(ShareData::player),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(ShareData::owner),
                Codec.STRING.listOf().fieldOf("research").forGetter(ShareData::research),
                Codec.STRING.listOf().fieldOf("aspects").forGetter(ShareData::aspects)).apply(instance, ShareData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShareData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShareData::player,
                UUIDUtil.STREAM_CODEC, ShareData::owner,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ShareData::research,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ShareData::aspects,
                ShareData::new);

        public ShareData {
            research = List.copyOf(research);
            aspects = List.copyOf(aspects);
        }
    }
}
