package thaumcraft.common.lib.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.junit.jupiter.api.Test;

/**
 * The 1.21.1 registration guard of the original
 * {@code thaumcraft/common/lib/network/PacketHandler.java:59-97}. In 1.7.10 a
 * simple channel addressed its packets by a numeric discriminator, so the
 * original had to keep the registration order and the count in sync. In
 * 1.21.1 the payload type decides the channel, so the equivalent invariant is
 * the identifier set: every payload has its own {@code thaumcraft:} id and no
 * two payloads share one.
 */
class PayloadRegistryTest {
    private static final List<CustomPacketPayload.Type<?>> SERVERBOUND = List.of(
            AccessoryOpenPayload.TYPE,
            FocalManipulatorStartPayload.TYPE,
            FocusSelectionPayload.TYPE,
            GolemBellResetPayload.TYPE,
            HoverTogglePayload.TYPE,
            ResearchPurchasePayload.TYPE,
            ResearchNoteRequestPayload.TYPE,
            ResearchTableEditPayload.TYPE,
            ResearchTableCombinePayload.TYPE,
            ResearchTableCopyPayload.TYPE,
            ShovelOrientationPayload.TYPE,
            WandArchitectPayload.TYPE);

    private static final List<CustomPacketPayload.Type<?>> CLIENTBOUND = List.of(
            AccessoryContentsPayload.TYPE,
            AspectDiscoveryPayload.TYPE,
            AspectPoolPayload.TYPE,
            ExcavationFxPayload.TYPE,
            FocalManipulatorAckPayload.TYPE,
            GrowthLampFxPayload.TYPE,
            GuardianSonicPayload.TYPE,
            InfusionFxPayload.TYPE,
            KnowledgeSyncPayload.TYPE,
            OpenThaumonomiconPayload.TYPE,
            ResearchNoticePayload.TYPE,
            ResearchSettingsPayload.TYPE,
            ResearchTableStatePayload.TYPE,
            RunicChargePayload.TYPE,
            RunicHitPayload.TYPE,
            ShockFxPayload.TYPE,
            VisNetFxPayload.TYPE,
            WarpEventPayload.TYPE,
            WarpMessagePayload.TYPE,
            WispZapPayload.TYPE);

    private static Set<String> ids(List<CustomPacketPayload.Type<?>> types) {
        Set<String> ids = new LinkedHashSet<>();
        for (var type : types) ids.add(type.id().toString());
        return ids;
    }

    @Test void everyServerboundPayloadHasItsOwnThaumcraftIdentifier() {
        Set<String> ids = ids(SERVERBOUND);
        assertEquals(SERVERBOUND.size(), ids.size(), "a serverbound identifier is used twice: " + ids);
        for (String id : ids) assertTrue(id.startsWith("thaumcraft:"), id);
    }

    @Test void everyClientboundPayloadHasItsOwnThaumcraftIdentifier() {
        Set<String> ids = ids(CLIENTBOUND);
        assertEquals(CLIENTBOUND.size(), ids.size(), "a clientbound identifier is used twice: " + ids);
        for (String id : ids) assertTrue(id.startsWith("thaumcraft:"), id);
    }

    @Test void theTwoDirectionsNeverShareAnIdentifier() {
        Set<String> server = ids(SERVERBOUND);
        Set<String> client = ids(CLIENTBOUND);
        for (String id : client) assertTrue(!server.contains(id), id + " is registered in both directions");
    }

    @Test void theRegisteredIdentifierSetIsTheOneThisPortShips() {
        assertEquals(Set.of("thaumcraft:accessories_open", "thaumcraft:focal_manipulator_start",
                "thaumcraft:focus_selection", "thaumcraft:golem_bell_reset", "thaumcraft:hover_toggle",
                "thaumcraft:research_purchase", "thaumcraft:research_note_request", "thaumcraft:research_table_edit",
                "thaumcraft:research_table_combine", "thaumcraft:research_table_copy",
                "thaumcraft:shovel_orientation", "thaumcraft:architect_toggle"), ids(SERVERBOUND));
        assertEquals(Set.of("thaumcraft:accessories_contents", "thaumcraft:aspect_discovery", "thaumcraft:aspect_pool",
                "thaumcraft:excavation_fx", "thaumcraft:focal_manipulator_ack", "thaumcraft:growth_lamp_fx",
                "thaumcraft:guardian_sonic", "thaumcraft:infusion_fx", "thaumcraft:knowledge_sync",
                "thaumcraft:open_thaumonomicon", "thaumcraft:research_notice", "thaumcraft:research_settings",
                "thaumcraft:research_table_state", "thaumcraft:runic_charge", "thaumcraft:runic_hit",
                "thaumcraft:shock_fx", "thaumcraft:vis_net_fx", "thaumcraft:warp_event", "thaumcraft:warp_message",
                "thaumcraft:wisp_zap"), ids(CLIENTBOUND));
    }

    @Test void theOriginalsThirtyNineRegistrationsAreStillAccountedFor() {
        // Oracle PacketHandler.java:59-97 registers 39 entries (PacketNote
        // twice) for 38 unique classes. The port answers the same 38 classes
        // with 32 identifiers: 6 original channels became native Minecraft
        // traffic (the biome override, the bore dig event, the note block and
        // arcane ear tone, the runic charge state, the four knowledge syncs
        // and the config), 1 original class was split into three intents
        // (PacketItemKeyToServer) and 2 were split into two (the research
        // purchase/note request and the aspect placement/combination).
        assertEquals(32, SERVERBOUND.size() + CLIENTBOUND.size());
    }

    @Test void everyIdentifierIsCanonicalLowercaseUnderscore() {
        for (var type : SERVERBOUND) assertCanonical(type);
        for (var type : CLIENTBOUND) assertCanonical(type);
    }

    private static void assertCanonical(CustomPacketPayload.Type<?> type) {
        String path = type.id().getPath();
        assertTrue(path.matches("[a-z0-9_]+"), path);
        assertEquals("thaumcraft", type.id().getNamespace());
    }
}
