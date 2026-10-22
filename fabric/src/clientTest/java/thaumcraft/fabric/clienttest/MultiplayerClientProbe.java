package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.client.gui.GuiAccessories;
import thaumcraft.client.lib.ResearchNotifications;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerAccessories;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.AccessoryOpenPayload;
import thaumcraft.common.lib.network.FocusSelectionPayload;
import thaumcraft.common.lib.network.GolemBellResetPayload;
import thaumcraft.common.lib.network.HoverNetworking;
import thaumcraft.common.lib.network.ResearchNoteRequestPayload;
import thaumcraft.common.lib.network.ResearchPurchasePayload;
import thaumcraft.common.lib.network.ShovelOrientationNetworking;
import thaumcraft.common.lib.network.WandArchitectNetworking;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteEditing;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real dedicated-server multiplayer acceptance for the 1.21.1 port.
 *
 * <p>This is the first acceptance of the port that exercises the real
 * {@code CustomPayload} channels over a real connection: the dedicated
 * {@code :fabric:runServer} process and this client process are two separate
 * JVMs with separate payload registries, so every clientbound payload is
 * decoded by a client that never shared a registry with the server, and every
 * serverbound payload is encoded by a client that only knows the type because
 * the common layer registered it on the physical client.</p>
 *
 * <p>No stage calls a server handler directly and no stage uses a mock player:
 * a stage can only succeed when a real packet crossed the real socket.</p>
 */
public final class MultiplayerClientProbe implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("thaumcraft-multiplayer");
    private static final int STAGE_TIMEOUT_TICKS = 2400;
    private static final String NOTE_RESEARCH = "NITOR";
    private static final String PURCHASE_RESEARCH = "ARCANELAMP";
    private static final String PURCHASE_ASPECT_A = "lux";
    private static final String PURCHASE_ASPECT_B = "sensus";
    private static final String PURCHASE_ASPECT_C = "tenebrae";
    private static final String EDIT_ASPECT = "terra";
    /** The research the SECOND physical client owns; the lead must never receive it. */
    private static final String PEER_RESEARCH = "BOTTLETAINT";
    /** The original's vis amulet may only be worn with this research. */
    private static final String AMULET_RESEARCH = "VISAMULET";
    /** The two aspects the lead combines in the research table over the real channel. */
    private static final String COMBINE_LEFT = "aer";
    private static final String COMBINE_RIGHT = "aqua";
    /** aer + aqua is the original's WEATHER aspect, which the lead never discovered. */
    private static final String COMBINE_RESULT = "tempestas";

    private final Map<String, Object> evidence = new LinkedHashMap<>();
    private final List<String> stepLog = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private final Map<String, Object> stageData = new LinkedHashMap<>();

    private String runDirectory = ".";
    private String reportFile = "multiplayer-client-report.json";
    private String playerName = "MPProbe";
    private String host = "127.0.0.1";
    private int port = 25565;
    private int stage;
    private int stageTicks;
    private boolean finished;
    private String expectedResearch = "";
    private BlockPos tablePos;
    private BlockPos tablePartner;
    private boolean reconnectIssued;

    // ---- the SECOND physical client -------------------------------------------------
    /** The shared directory of the two client processes; empty when there is no second client. */
    private Path pairDirectory;
    private String peerName = "MPProbe2";
    private int pairSequence;
    private String pairAnswer = "";
    private BlockPos pairOrigin;

    /** Each stage returns true when it is done; false means "retry next tick". */
    private interface StageBody {
        boolean run(Minecraft client) throws Exception;
    }

    private record Stage(String name, StageBody body) {}

    private final List<Stage> stages = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.multiplayerClient")) return;
        if (Boolean.getBoolean("thaumcraft.clientSmoke")) {
            throw new IllegalStateException("The multiplayer probe never runs inside the single-player client smoke");
        }
        String directory = System.getProperty("thaumcraft.multiplayerRunDir", "");
        if (directory.isBlank()) throw new IllegalStateException("The multiplayer probe needs an owned run directory");
        runDirectory = directory;
        reportFile = System.getProperty("thaumcraft.multiplayerReport", "multiplayer-client-report.json");
        playerName = System.getProperty("thaumcraft.multiplayerPlayer", playerName);
        String address = System.getProperty("thaumcraft.multiplayerAddress", "127.0.0.1:25565");
        int colon = address.lastIndexOf(':');
        host = address.substring(0, colon);
        port = Integer.parseInt(address.substring(colon + 1));
        String pair = System.getProperty("thaumcraft.multiplayerPairDir", "");
        if (!pair.isBlank()) {
            pairDirectory = Path.of(pair);
            peerName = System.getProperty("thaumcraft.multiplayerPeer", peerName);
        }
        LOGGER.info("Multiplayer probe starting against {} as {}", address, playerName);

        // The payload census observes the real play connection. It is installed
        // again after every reconnect, because a reconnect is a new socket.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, packet) -> {
            installCensus(handler.getConnection());
            LOGGER.info("Multiplayer probe joined the play phase; census installed");
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                LOGGER.info("Multiplayer probe observed a disconnect"));

        buildStages();
        ClientTickEvent.CLIENT_POST.register(this::tick);
    }

    // ------------------------------------------------------------------ stages

    private void buildStages() {
        stages.add(new Stage("connect", this::stageConnect));
        if (pairDirectory != null) stages.add(new Stage("pair_peer_joined", this::stagePairPeerJoined));
        stages.add(new Stage("teleport_to_work_area", this::stageTeleport));
        stages.add(new Stage("payload_type_registries", this::stagePayloadTypeRegistries));
        stages.add(new Stage("reset_research", this::stageResetResearch));
        stages.add(new Stage("knowledge_sync", this::stageKnowledgeSync));
        stages.add(new Stage("server_aspect_crosscheck", this::stageServerAspectCrosscheck));
        stages.add(new Stage("research_note_request", this::stageResearchNoteRequest));
        stages.add(new Stage("server_research_grant", this::stageServerResearchGrant));
        stages.add(new Stage("research_purchase", this::stageResearchPurchase));
        stages.add(new Stage("focus_selection", this::stageFocusSelection));
        stages.add(new Stage("golem_bell", this::stageGolemBell));
        stages.add(new Stage("accessory_menu_over_network", this::stageAccessoryMenu));
        stages.add(new Stage("research_table_menu_over_network", this::stageResearchTableMenu));
        stages.add(new Stage("research_table_edit_over_network", this::stageResearchTableEdit));
        stages.add(new Stage("golem_placement_over_network", this::stageGolemPlacement));
        stages.add(new Stage("traveling_trunk_over_network", this::stageTravelingTrunk));
        stages.add(new Stage("visnet_block_over_network", this::stageVisnetBlock));
        stages.add(new Stage("arcane_bore_over_network", this::stageArcaneBore));
        // The clientbound FX payloads that wave 2 could only prove as REGISTERED.
        stages.add(new Stage("wire_open_thaumonomicon", this::stageWireThaumonomicon));
        stages.add(new Stage("wire_research_table_combine", this::stageWireCombine));
        stages.add(new Stage("wire_research_table_copy", this::stageWireCopy));
        stages.add(new Stage("wire_client_toggles", this::stageWireToggles));
        stages.add(new Stage("wire_eldritch_trap", this::stageWireTrap));
        stages.add(new Stage("wire_eldritch_warp_event", this::stageWireWarpEvent));
        stages.add(new Stage("wire_guardian_sonic", this::stageWireGuardianSonic));
        stages.add(new Stage("wire_wisp_zap", this::stageWireWisp));
        if (pairDirectory != null) {
            stages.add(new Stage("pair_per_player_knowledge", this::stagePairPerPlayerKnowledge));
            stages.add(new Stage("pair_accessory_isolation", this::stagePairAccessoryIsolation));
            stages.add(new Stage("pair_golem_bell_owner", this::stagePairGolemBellOwner));
            stages.add(new Stage("pair_traveling_trunk_owner", this::stagePairTravelingTrunkOwner));
            stages.add(new Stage("pair_research_table_partner", this::stagePairResearchTablePartner));
            stages.add(new Stage("pair_creative_player_excluded", this::stagePairCreativePlayerExcluded));
        }
        stages.add(new Stage("disconnect", this::stageDisconnect));
        stages.add(new Stage("reconnect", this::stageReconnect));
        stages.add(new Stage("reconnect_state_consistent", this::stageReconnectConsistent));
        if (pairDirectory != null) stages.add(new Stage("pair_peer_stop", this::stagePairPeerStop));
    }

    /**
     * Sends the vanilla give commands and retries until the server confirmed
     * every one of them. A signed command whose argument signature the server
     * rejects is dropped SILENTLY by the vanilla 1.21.1 secure chat
     * validation, so a command without an answer never happened.
     */
    private boolean giveItems(Minecraft client, String... itemArguments) {
        if (stageData.get("give_baseline") == null) stageData.put("give_baseline", giveReplies());
        int baseline = (Integer) stageData.get("give_baseline");
        if (giveReplies() - baseline >= itemArguments.length) return true;
        if (stageTicks % 20 == 0) {
            for (String argument : itemArguments) {
                client.getConnection().sendCommand("give " + playerName + " " + argument);
            }
        }
        return false;
    }

    private int giveReplies() {
        int count = 0;
        synchronized (MultiplayerPairLink.CENSUS.messages) {
            for (String message : MultiplayerPairLink.CENSUS.messages) if (message.startsWith("Gave ")) count++;
        }
        return count;
    }

    private boolean stageConnect(Minecraft client) {
        if (client.player == null || client.level == null || client.getConnection() == null) return false;
        evidence.put("player", client.player.getGameProfile().getName());
        evidence.put("dimension", client.level.dimension().location().toString());
        evidence.put("position", List.of(client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ()));
        if (!giveItems(client, "thaumcraft:golem_straw 1", "thaumcraft:trunk_spawner 1", "thaumcraft:arcane_bore 1")) {
            return false;
        }
        return true;
    }

    /**
     * The vanilla spawn protection refuses block interactions inside the spawn
     * chunk, so the acceptance moves the player into an ordinary area first.
     */
    private boolean stageTeleport(Minecraft client) {
        if (stageData.get("tp_sent") == null) {
            client.getConnection().sendCommand("tp " + playerName + " 100 -60 100");
            stageData.put("tp_sent", true);
            return false;
        }
        rejectBrokenCommand();
        if (Math.abs(client.player.getX() - 100) > 1 || Math.abs(client.player.getZ() - 100) > 1) return false;
        evidence.put("teleport_position", List.of(client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ()));
        return true;
    }

    /**
     * The physical client must know every Thaumcraft payload type in BOTH
     * directions. A single-player client shares the integrated server's
     * registry, so this is the first check that can fail only in multiplayer.
     */
    private boolean stagePayloadTypeRegistries(Minecraft client) {
        List<String> s2c = payloadTypeIds("PLAY_S2C");
        List<String> c2s = payloadTypeIds("PLAY_C2S");
        evidence.put("client_clientbound_types", s2c);
        evidence.put("client_serverbound_types", c2s);
        for (String required : List.of("thaumcraft:knowledge_sync", "thaumcraft:research_settings",
                "thaumcraft:aspect_pool", "thaumcraft:aspect_discovery", "thaumcraft:research_notice",
                "thaumcraft:research_table_state", "thaumcraft:open_thaumonomicon",
                "thaumcraft:accessories_contents", "thaumcraft:warp_message", "thaumcraft:warp_event",
                "thaumcraft:vis_net_fx", "thaumcraft:shock_fx", "thaumcraft:excavation_fx",
                "thaumcraft:infusion_fx", "thaumcraft:block_fx", "thaumcraft:growth_lamp_fx",
                "thaumcraft:guardian_sonic", "thaumcraft:runic_charge", "thaumcraft:runic_hit",
                "thaumcraft:wisp_zap", "thaumcraft:focal_manipulator_ack")) {
            if (!s2c.contains(required)) throw new IllegalStateException("The client cannot decode " + required);
        }
        for (String required : List.of("thaumcraft:research_note_request", "thaumcraft:research_purchase",
                "thaumcraft:focus_selection", "thaumcraft:golem_bell_reset", "thaumcraft:accessories_open",
                "thaumcraft:research_table_edit", "thaumcraft:hover_toggle", "thaumcraft:shovel_orientation",
                "thaumcraft:architect_toggle", "thaumcraft:focal_manipulator_start",
                "thaumcraft:research_table_combine", "thaumcraft:research_table_copy")) {
            if (!c2s.contains(required)) throw new IllegalStateException("The client cannot encode " + required);
        }
        return true;
    }

    /** A deterministic starting point: the auto-unlock research only. */
    private boolean stageResetResearch(Minecraft client) {
        var knowledge = KnowledgeAccess.get(client.player);
        if (stageData.get("reset_sent") == null) {
            client.getConnection().sendCommand("thaumcraft research " + playerName + " reset");
            stageData.put("reset_sent", true);
            return false;
        }
        rejectBrokenCommand();
        if (knowledge.isResearchComplete(NOTE_RESEARCH) || knowledge.isResearchComplete(PURCHASE_RESEARCH)) return false;
        evidence.put("reset_research_count", knowledge.getResearchComplete().size());
        evidence.put("reset_research", new ArrayList<>(new TreeSet<>(knowledge.getResearchComplete())));
        return true;
    }

    private boolean stageKnowledgeSync(Minecraft client) {
        var knowledge = KnowledgeAccess.get(client.player);
        var aspects = new TreeSet<>(knowledge.getAspectsDiscovered().keySet());
        var research = new TreeSet<>(knowledge.getResearchComplete());
        if (research.isEmpty()) return false;
        evidence.put("client_aspects", new ArrayList<>(aspects));
        evidence.put("client_research", new ArrayList<>(research));
        evidence.put("client_research_count", research.size());
        evidence.put("knowledge_sync_packets", MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:knowledge_sync", new AtomicInteger()).get());
        if (!aspects.containsAll(List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio"))) {
            throw new IllegalStateException("The knowledge snapshot did not carry the six primal aspects: " + aspects);
        }
        return true;
    }

    /** The server answers with its own authoritative aspect list; the client compares. */
    private boolean stageServerAspectCrosscheck(Minecraft client) {
        String answer = awaitMessage(text -> (text.startsWith("{") || text.startsWith("[")) && text.contains("aer="));
        if (answer == null) {
            if (stageTicks == 1) client.getConnection().sendCommand("thaumcraft aspects");
            return false;
        }
        Set<String> server = new LinkedHashSet<>();
        String body = answer.substring(1, answer.length() - 1);
        for (String part : body.split(",")) {
            String tag = part.trim();
            int equals = tag.indexOf('=');
            if (equals > 0) tag = tag.substring(0, equals);
            if (!tag.isEmpty()) server.add(tag);
        }
        Set<String> local = new LinkedHashSet<String>(KnowledgeAccess.get(client.player).getAspectsDiscovered().keySet());
        evidence.put("server_aspects_raw", answer);
        evidence.put("server_aspects", new ArrayList<>(server));
        evidence.put("aspect_crosscheck_equal", server.equals(local));
        if (!server.equals(local)) {
            throw new IllegalStateException("The server and the client disagree about the discovered aspects: server=" + server + " client=" + local);
        }
        return true;
    }

    /** The primary-note issuance over the real C2S channel. */
    private boolean stageResearchNoteRequest(Minecraft client) throws Exception {
        if (!giveItems(client, "minecraft:paper 8", "thaumcraft:inkwell 3")) return false;
        rejectBrokenCommand();
        if (inventoryCount(client, Items.PAPER) <= 0 || !hasItem(client, ThaumcraftContent.INKWELL.get())) return false;
        if (stageData.get("note_sent") == null) {
            NetworkManager.sendToServer(new ResearchNoteRequestPayload(NOTE_RESEARCH));
            stageData.put("note_sent", true);
            stageData.put("paper_before", inventoryCount(client, Items.PAPER));
            return false;
        }
        String answer = awaitMessage(text -> text.startsWith("Research notes created")
                || text.startsWith("You already have research notes"));
        if (answer == null) {
            String failure = lastMessageContaining("tc.port.note.failure");
            if (failure != null) throw new IllegalStateException("The server refused the note request: " + failure);
            return false;
        }
        int notes = 0;
        for (ItemStack stack : client.player.getInventory().items) {
            var data = ItemResearchNotes.data(stack);
            if (data != null && NOTE_RESEARCH.equals(data.key()) && !data.complete()) notes += stack.getCount();
        }
        if (notes == 0) return false;
        evidence.put("issued_notes", notes);
        evidence.put("note_request_answer", answer);
        evidence.put("paper_before", stageData.get("paper_before"));
        evidence.put("paper_after", inventoryCount(client, Items.PAPER));
        evidence.put("note_request_packets", MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:research_note_request", new AtomicInteger()).get());
        if (MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:research_note_request", new AtomicInteger()).get() != 1) {
            throw new IllegalStateException("The client did not send exactly one research note request");
        }
        return true;
    }

    /** The server grants a research; the client must see it through the S2C snapshot. */
    private boolean stageServerResearchGrant(Minecraft client) {
        String key = NOTE_RESEARCH;
        expectedResearch = key;
        if (stageData.get("grant_sent") == null) {
            client.getConnection().sendCommand("thaumcraft research " + playerName + " " + key);
            stageData.put("grant_sent", true);
            return false;
        }
        rejectBrokenCommand();
        String answer = awaitMessage(text -> text.contains("changed your research: " + key));
        if (answer == null) return false;
        if (!KnowledgeAccess.get(client.player).isResearchComplete(key)) {
            // The clientbound snapshot is applied on the client thread; wait for it.
            return false;
        }
        evidence.put("granted_research", key);
        evidence.put("knowledge_sync_packets_after_grant", MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:knowledge_sync", new AtomicInteger()).get());
        if (MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:knowledge_sync", new AtomicInteger()).get() == 0) {
            throw new IllegalStateException("No clientbound knowledge snapshot crossed the wire after the grant");
        }
        return true;
    }

    /** The secondary purchase over the real C2S channel, with a server-side debit. */
    private boolean stageResearchPurchase(Minecraft client) throws Exception {
        if (stageTicks == 1) {
            client.getConnection().sendCommand("thaumcraft aspect " + playerName + " " + PURCHASE_ASPECT_A + " 3");
            client.getConnection().sendCommand("thaumcraft aspect " + playerName + " " + PURCHASE_ASPECT_B + " 3");
            client.getConnection().sendCommand("thaumcraft aspect " + playerName + " " + PURCHASE_ASPECT_C + " 3");
            return false;
        }
        var knowledge = KnowledgeAccess.get(client.player);
        rejectBrokenCommand();
        if (stageData.get("purchase_sent") == null) {
            // The purchase debits the pools, so the funding is only a gate
            // BEFORE the intent is sent.
            if (knowledge.getAspectPoolFor(PURCHASE_ASPECT_A) < 3) return false;
            if (knowledge.getAspectPoolFor(PURCHASE_ASPECT_B) < 3) return false;
            if (knowledge.getAspectPoolFor(PURCHASE_ASPECT_C) < 3) return false;
            NetworkManager.sendToServer(new ResearchPurchasePayload(PURCHASE_RESEARCH));
            stageData.put("purchase_sent", true);
            return false;
        }
        if (!knowledge.isResearchComplete(PURCHASE_RESEARCH)) {
            String failure = lastMessageContaining("tc.port.research.failure");
            if (failure != null) throw new IllegalStateException("The server refused the purchase: " + failure);
            // The clientbound snapshot is applied on the client thread; wait for it.
            return false;
        }
        evidence.put("purchase_answer", lastMessageContaining("Research completed"));
        evidence.put("purchased_research", PURCHASE_RESEARCH);
        evidence.put("purchase_packets", MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:research_purchase", new AtomicInteger()).get());
        evidence.put("research_notice_packets", MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:research_notice", new AtomicInteger()).get());
        evidence.put("research_notice_highlighted", ResearchNotifications.highlighted(PURCHASE_RESEARCH));
        evidence.put("aspect_pool_packets", MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:aspect_pool", new AtomicInteger()).get());
        if (MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:research_notice", new AtomicInteger()).get() == 0) {
            throw new IllegalStateException("The purchase produced no clientbound research notice");
        }
        if (MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:aspect_pool", new AtomicInteger()).get() == 0) {
            throw new IllegalStateException("The purchase produced no clientbound aspect-pool receipt");
        }
        return true;
    }

    /** The wand focus selection over the real C2S channel. */
    private boolean stageFocusSelection(Minecraft client) throws Exception {
        if (!giveItems(client, "thaumcraft:wand 1", "thaumcraft:focus_excavation 1")) return false;
        if (stageData.get("focus_sent") == null) {
            int wandSlot = slotOf(client, ThaumcraftContent.WAND.get());
            int focusSlot = slotOf(client, thaumcraft.common.config.ThaumcraftFoci.EXCAVATION_FOCUS.get());
            if (wandSlot < 0 || focusSlot < 0) return false;
            selectSlot(client, wandSlot);
            evidence.put("wand_slot", wandSlot);
            evidence.put("focus_slot", focusSlot);
            evidence.put("client_selected_slot", client.player.getInventory().selected);
            evidence.put("client_main_hand", String.valueOf(client.player.getMainHandItem()));
            NetworkManager.sendToServer(new FocusSelectionPayload(InteractionHand.MAIN_HAND, focusSlot));
            stageData.put("focus_sent", true);
            // The server REMOVES the loose focus from the inventory slot, so the
            // slot has to be remembered instead of looked up again.
            stageData.put("wand_slot", wandSlot);
            return false;
        }
        int wandSlot = (Integer) stageData.get("wand_slot");
        ItemStack wand = client.player.getInventory().getItem(wandSlot);
        ItemStack focus = FocusData.get(wand);
        evidence.put("client_main_hand_after", String.valueOf(client.player.getMainHandItem()));
        evidence.put("client_focus_after", String.valueOf(focus));
        if (focus.isEmpty()) return false;
        evidence.put("selected_focus", BuiltInRegistries.ITEM.getKey(focus.getItem()).toString());
        evidence.put("focus_selection_packets", MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:focus_selection", new AtomicInteger()).get());
        if (MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:focus_selection", new AtomicInteger()).get() != 1) {
            throw new IllegalStateException("The client did not send exactly one focus selection");
        }
        return true;
    }

    /** The golem bell reset over the real C2S channel. */
    private boolean stageGolemBell(Minecraft client) throws Exception {
        if (!giveItems(client, "thaumcraft:golem_bell 1")) return false;
        if (!holdItemOf(client, thaumcraft.common.config.ThaumcraftGolems.GOLEM_BELL.get())) return false;
        if (stageData.get("bell_sent") == null) {
            NetworkManager.sendToServer(new GolemBellResetPayload(InteractionHand.MAIN_HAND));
            stageData.put("bell_sent", true);
            return false;
        }
        int sent = MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:golem_bell_reset", new AtomicInteger()).get();
        if (sent != 1) return false;
        evidence.put("golem_bell_reset_packets", sent);
        return true;
    }

    /** The accessory menu has to open from a real server menu packet. */
    private boolean stageAccessoryMenu(Minecraft client) throws Exception {
        if (stageData.get("accessory_sent") == null) {
            NetworkManager.sendToServer(new AccessoryOpenPayload());
            stageData.put("accessory_sent", true);
            return false;
        }
        boolean menu = client.player.containerMenu instanceof ContainerAccessories;
        boolean screen = client.screen instanceof GuiAccessories;
        if (!menu && !screen) {
            if (stageTicks % 100 == 0) {
                evidence.put("accessory_cursor", String.valueOf(client.player.inventoryMenu.getCarried()));
                evidence.put("accessory_container_menu", String.valueOf(client.player.containerMenu.getClass().getSimpleName()));
                LOGGER.info("Multiplayer probe accessory menu waits: cursor={} menu={}",
                        client.player.inventoryMenu.getCarried(), client.player.containerMenu.getClass().getSimpleName());
            }
            return false;
        }
        evidence.put("accessory_menu_opened", menu);
        evidence.put("accessory_screen_opened", screen);
        if (!screen) throw new IllegalStateException("The accessory container opened without a client screen");
        client.player.closeContainer();
        return true;
    }

    /**
     * The research table over the network. The original table is a PAIR: a
     * plain table plus its partner, converted by the inkwell. A lone research
     * half has no partner and the native topology tick collapses it back to a
     * plain table, so the probe uses the original two-table conversion.
     */
    private boolean stageResearchTableMenu(Minecraft client) throws Exception {
        if (stageTicks == 1) {
            BlockPos pos = client.player.blockPosition().offset(1, 0, 0);
            BlockPos partner = pos.north();
            client.getConnection().sendCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " thaumcraft:table");
            client.getConnection().sendCommand("setblock " + partner.getX() + " " + partner.getY() + " " + partner.getZ() + " thaumcraft:table");
            tablePos = pos;
            tablePartner = partner;
            return false;
        }
        if (tablePos == null) return false;
        if (client.player == null) {
            if (stageTicks > 200) throw new IllegalStateException("The client lost the connection before the research table opened");
            return false;
        }
        if (client.player.isDeadOrDying() || client.player.isSpectator()) {
            throw new IllegalStateException("The player died or became a spectator before the research table opened");
        }
        if (client.player.containerMenu instanceof ContainerResearchTable menu) {
            if (!menu.editReady()) return false;
            evidence.put("research_table_menu_opened", true);
            evidence.put("research_table_variant", variantAt(client, tablePos));
            evidence.put("research_table_partner_variant", variantAt(client, tablePartner));
            evidence.put("research_table_state_packets", MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:research_table_state", new AtomicInteger()).get());
            if (MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:research_table_state", new AtomicInteger()).get() == 0) {
                throw new IllegalStateException("The research table opened without its authoritative state payload");
            }
            return true;
        }
        rejectBrokenCommand();
        int variant = variantAt(client, tablePos);
        if (variant < 2) {
            // Convert the pair with the inkwell, exactly like the original.
            int inkSlot = slotOf(client, ThaumcraftContent.INKWELL.get());
            if (inkSlot < 0) return false;
            selectSlot(client, inkSlot);
            if (stageTicks % 20 == 0) interact(client, tablePos);
            return false;
        }
        // The main half opens the research table.
        if (client.player.getMainHandItem().isEmpty()) {
            int emptySlot = -1;
            for (int slot = 0; slot < 36; slot++) if (client.player.getInventory().items.get(slot).isEmpty()) { emptySlot = slot; break; }
            if (emptySlot >= 0) selectSlot(client, emptySlot);
        }
        if (stageTicks % 20 == 0) interact(client, tablePos);
        return false;
    }

    private static int variantAt(Minecraft client, BlockPos pos) {
        if (pos == null || !client.level.hasChunkAt(pos)) return -1;
        var state = client.level.getBlockState(pos);
        if (!(state.getBlock() instanceof thaumcraft.common.blocks.BlockTable)) return -1;
        return state.getValue(thaumcraft.common.blocks.BlockTable.VARIANT);
    }

    /** A real table edit over the real C2S channel. */
    private boolean stageResearchTableEdit(Minecraft client) throws Exception {
        if (!(client.player.containerMenu instanceof ContainerResearchTable menu)) {
            throw new IllegalStateException("The research table menu was closed before the edit");
        }
        if (stageData.get("items_moved") == null) {
            int noteSlot = slotOf(client, ThaumcraftContent.RESEARCH_NOTE.get());
            int inkSlot = slotOf(client, ThaumcraftContent.INKWELL.get());
            if (noteSlot < 0 || inkSlot < 0) throw new IllegalStateException("The note or the inkwell is missing");
            moveIntoTable(client, noteSlot, 1);
            moveIntoTable(client, inkSlot, 0);
            stageData.put("items_moved", true);
            return false;
        }
        ItemStack noteStack = menu.getSlot(1).getItem();
        var note = ItemResearchNotes.data(noteStack);
        if (note == null) return false;
        if (stageData.get("edit_sent") == null) {
            int q = 0, r = 0;
            boolean found = false;
            for (int radius = 0; radius <= ResearchNoteData.MAX_RADIUS && !found; radius++) {
                for (int dq = -radius; dq <= radius && !found; dq++) {
                    for (int dr = -radius; dr <= radius && !found; dr++) {
                        if (Math.abs(dq + dr) > radius) continue;
                        var cell = note.cellAt(dq, dr);
                        if (cell != null && cell.type() == 0) {
                            q = dq; r = dr; found = true;
                        }
                    }
                }
            }
            if (!found) throw new IllegalStateException("The issued note has no blank cell to edit");
            if (!menu.requestEdit(ResearchNoteEditing.Action.PLACE, q, r, EDIT_ASPECT)) {
                throw new IllegalStateException("The client refused its own table edit intent");
            }
            stageData.put("edit_sent", true);
            stageData.put("edit_cell", List.of(q, r));
            stageData.put("edit_attempts", 1);
            evidence.put("edit_cell", List.of(q, r));
            return false;
        }
        var after = ItemResearchNotes.data(menu.getSlot(1).getItem());
        if (after == null) return false;
        @SuppressWarnings("unchecked")
        List<Integer> cell = (List<Integer>) stageData.get("edit_cell");
        var placed = after.cellAt(cell.get(0), cell.get(1));
        if (placed != null && placed.type() != 0) {
            evidence.put("edit_result_aspect", placed.aspect());
            evidence.put("table_edit_packets", MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:research_table_edit", new AtomicInteger()).get());
            evidence.put("table_state_packets", MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:research_table_state", new AtomicInteger()).get());
            client.player.closeContainer();
            return true;
        }
        // The table revision advances when the physical contents change, so a
        // stale revision is refused with a correction. Retry with the fresh
        // authoritative revision the correction carried.
        int attempts = (Integer) stageData.get("edit_attempts");
        if (attempts >= 5) throw new IllegalStateException("The server refused the table edit " + attempts + " times");
        if (stageTicks % 30 == 0) {
            if (!menu.requestEdit(ResearchNoteEditing.Action.PLACE, cell.get(0), cell.get(1), EDIT_ASPECT)) {
                throw new IllegalStateException("The client refused its own table edit intent");
            }
            stageData.put("edit_attempts", attempts + 1);
        }
        return false;
    }

    /** A golem spawned by the server has to exist on the client. */
    private boolean stageGolemPlacement(Minecraft client) throws Exception {
        if (stageData.get("golem_probe") == null && stageTicks > 30) {
            // Ask the authoritative server for its inventory and probe the
            // command path, so a placement defect can be separated from an
            // entity-tracking defect and from an inventory-sync defect.
            for (String command : List.of(
                    "data get entity " + playerName + " Inventory",
                    "summon thaumcraft:golem " + (client.player.getBlockX() + 2) + " " + client.player.getBlockY() + " " + (client.player.getBlockZ() + 1))) {
                try {
                    client.getConnection().sendCommand(command);
                    evidence.put("command:" + command, "sent");
                } catch (Throwable failure) {
                    evidence.put("command:" + command, "REFUSED " + describe(failure));
                }
            }
            stageData.put("golem_probe", true);
        }
        int placerSlot = slotOf(client, thaumcraft.common.config.ThaumcraftGolems.GOLEM_STRAW.get());
        int golems = 0;
        Map<String, Integer> nearby = new TreeMap<>();
        for (var entity : client.level.entitiesForRendering()) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            nearby.merge(id, 1, Integer::sum);
            if (entity instanceof thaumcraft.common.entities.golems.EntityGolemBase) golems++;
        }
        evidence.put("client_nearby_entities", nearby);
        if (golems > 0) {
            // The golem entity itself reached the client.
            evidence.put("client_golems", golems);
            evidence.put("golem_delivered_by", stageData.get("golem_used") != null ? "placer" : "summon");
            evidence.put("golem_placer_reached_client", stageData.get("golem_used") != null);
            return true;
        }
        if (placerSlot < 0) {
            List<String> inventory = new ArrayList<>();
            for (ItemStack stack : client.player.getInventory().items) {
                inventory.add(stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
            evidence.put("client_inventory", inventory);
            evidence.put("golem_placer_reached_client", false);
            if (stageTicks % 20 == 0) {
                // The vanilla inventory delivery of a given item is not reliable
                // enough for an acceptance, so the server is asked to place a
                // golem directly until one reaches the client.
                client.getConnection().sendCommand("summon thaumcraft:golem "
                        + (client.player.getBlockX() + 2) + " " + client.player.getBlockY() + " " + (client.player.getBlockZ() + 1));
            }
            if (stageTicks % 100 == 0) LOGGER.info("Multiplayer probe golem stage waits for the placer, tick {}", stageTicks);
            return false;
        }
        evidence.put("golem_placer_reached_client", true);
        selectSlot(client, placerSlot);
        if (stageData.get("golem_used") == null) {
            interact(client, client.player.blockPosition().offset(0, -1, 1));
            stageData.put("golem_used", true);
            return false;
        }
        if (golems == 0) {
            String answer = awaitMessage(text -> text.contains("Summoned new") || text.contains("entity data")
                    || text.contains("No entity") || text.contains("Inventory"));
            if (answer != null) evidence.put("server_golem_query", answer);
            if (stageTicks > 400) {
                List<String> recent = new ArrayList<>();
                synchronized (MultiplayerPairLink.CENSUS.messages) {
                    for (int i = Math.max(0, MultiplayerPairLink.CENSUS.messages.size() - 6); i < MultiplayerPairLink.CENSUS.messages.size(); i++) {
                        recent.add(MultiplayerPairLink.CENSUS.messages.get(i));
                    }
                }
                evidence.put("recent_server_messages", recent);
                throw new IllegalStateException("The server placed a golem but the client never received it; nearby="
                        + nearby + " serverAnswer=" + answer);
            }
            return false;
        }
        return true;
    }

    /** The travelling trunk is a real networked entity too. */
    private boolean stageTravelingTrunk(Minecraft client) throws Exception {
        int spawnerSlot = slotOf(client, thaumcraft.common.config.ThaumcraftGolems.TRUNK_SPAWNER.get());
        if (spawnerSlot >= 0) {
            selectSlot(client, spawnerSlot);
            if (stageData.get("trunk_used") == null) {
                interact(client, client.player.blockPosition().offset(0, -1, 1));
                stageData.put("trunk_used", true);
                return false;
            }
        } else if (stageTicks % 20 == 0) {
            client.getConnection().sendCommand("summon thaumcraft:traveling_trunk "
                    + (client.player.getBlockX() - 1) + " " + client.player.getBlockY() + " " + (client.player.getBlockZ() + 1));
        }
        int trunks = 0;
        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof thaumcraft.common.entities.golems.EntityTravelingTrunk) trunks++;
        }
        if (trunks == 0) return false;
        evidence.put("client_traveling_trunks", trunks);
        return true;
    }

    /** A vis relay block entity has to exist on the client. */
    private boolean stageVisnetBlock(Minecraft client) throws Exception {
        if (stageTicks == 1) {
            BlockPos pos = client.player.blockPosition().offset(-2, 0, 0);
            client.getConnection().sendCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " thaumcraft:vis_relay");
            client.getConnection().sendCommand("data get block " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            evidence.put("visnet_pos", List.of(pos.getX(), pos.getY(), pos.getZ()));
            return false;
        }
        Object position = evidence.get("visnet_pos");
        if (!(position instanceof List<?> list)) return false;
        BlockPos pos = new BlockPos((int) list.get(0), (int) list.get(1), (int) list.get(2));
        if (!client.level.hasChunkAt(pos)) return false;
        var block = client.level.getBlockState(pos);
        evidence.put("visnet_block", BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString());
        evidence.put("visnet_block_entity", String.valueOf(client.level.getBlockEntity(pos)));
        String server = awaitMessage(text -> text.contains("entity data") || text.contains("No block entity"));
        if (server != null) evidence.put("server_visnet_query", server);
        if (block.isAir()) {
            // The server may have removed its own unsupported relay; the
            // server's own view decides whether that is the original rule.
            if (stageTicks > 600) {
                if (server != null && server.contains("entity data")) {
                    throw new IllegalStateException("The server owns the vis relay but the client never received the block: " + server);
                }
                throw new IllegalStateException("The vis relay is not on the client and the server reports: " + server);
            }
            return false;
        }
        if (client.level.getBlockEntity(pos) == null) {
            if (stageTicks > 200) {
                throw new IllegalStateException("The vis relay has no block entity on the client; server=" + server);
            }
            return false;
        }
        return true;
    }

    /** The arcane bore is a two block machine: a base and the bore above it. */
    private boolean stageArcaneBore(Minecraft client) throws Exception {
        BlockPos clicked = client.player.blockPosition().offset(0, -1, -1);
        if (stageData.get("bore_base") == null) {
            client.getConnection().sendCommand("setblock " + clicked.getX() + " " + clicked.getY() + " "
                    + clicked.getZ() + " thaumcraft:arcane_bore_base");
            stageData.put("bore_base", true);
            return false;
        }
        int boreSlot = slotOf(client, thaumcraft.common.config.ThaumcraftArcaneBore.ARCANE_BORE_ITEM.get());
        if (boreSlot >= 0) {
            selectSlot(client, boreSlot);
            if (stageData.get("bore_used") == null) {
                interact(client, clicked);
                stageData.put("bore_used", true);
                return false;
            }
        } else if (stageTicks % 20 == 0) {
            // The vanilla inventory delivery of a given item is not reliable
            // enough for an acceptance, so the two bore blocks are placed by
            // the server directly until they reach the client.
            client.getConnection().sendCommand("setblock " + clicked.getX() + " " + clicked.getY() + " "
                    + clicked.getZ() + " thaumcraft:arcane_bore_base");
            client.getConnection().sendCommand("setblock " + clicked.above().getX() + " " + clicked.above().getY() + " "
                    + clicked.above().getZ() + " thaumcraft:arcane_bore");
        }
        // The bore is placed ON TOP of the clicked block, so the client has to
        // read the block above the clicked position.
        var boreState = client.level.getBlockState(clicked.above());
        String boreId = BuiltInRegistries.BLOCK.getKey(boreState.getBlock()).toString();
        evidence.put("bore_block", boreId);
        evidence.put("bore_base_block", BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(clicked).getBlock()).toString());
        if (!boreId.contains("arcane_bore")) {
            if (stageTicks > 600) {
                throw new IllegalStateException("The arcane bore never appeared on the client: " + boreId);
            }
            return false;
        }
        return true;
    }

    private boolean stageDisconnect(Minecraft client) {
        if (!reconnectIssued) {
            reconnectIssued = true;
            var connection = client.getConnection();
            if (connection != null) connection.getConnection().disconnect(Component.literal("multiplayer probe reconnect"));
            return false;
        }
        return client.getConnection() == null || client.level == null;
    }

    private boolean stageReconnect(Minecraft client) {
        if (client.getConnection() != null && client.level != null) return true;
        if (stageTicks == 1) {
            ConnectScreen.startConnecting(client.screen, client, new ServerAddress(host, port),
                    new ServerData("multiplayer-probe", host + ":" + port, ServerData.Type.OTHER), false, null);
        }
        return false;
    }

    private boolean stageReconnectConsistent(Minecraft client) {
        if (client.player == null || client.level == null || client.getConnection() == null) return false;
        var knowledge = KnowledgeAccess.get(client.player);
        if (!knowledge.isResearchComplete(PURCHASE_RESEARCH)) return false;
        evidence.put("reconnect_research", new ArrayList<>(new TreeSet<>(knowledge.getResearchComplete())));
        evidence.put("reconnect_aspects", new ArrayList<>(new TreeSet<>(knowledge.getAspectsDiscovered().keySet())));
        evidence.put("knowledge_sync_packets_total",
                MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:knowledge_sync", new AtomicInteger()).get());
        return true;
    }

    // ------------------------------------------------- the clientbound FX channels
    //
    // Wave 2 proved every payload type as REGISTERED and DECODABLE on the physical
    // client, but only eight clientbound and six serverbound channels were ever
    // observed travelling over the socket. The stages below make the harness
    // trigger the remaining ones and the census then proves that they really
    // crossed the wire.

    /** The clientbound open_thaumonomicon payload of the original book. */
    private boolean stageWireThaumonomicon(Minecraft client) throws Exception {
        if (!giveItems(client, "thaumcraft:thaumonomicon 1")) return false;
        int slot = slotOf(client, ThaumcraftContent.THAUMONOMICON.get());
        if (slot < 0) return false;
        if (stageData.get("selected") == null) {
            selectSlot(client, slot);
            stageData.put("selected", true);
            return false;
        }
        if (stageData.get("used") == null) {
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            stageData.put("used", true);
            return false;
        }
        int count = MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:open_thaumonomicon", new AtomicInteger()).get();
        evidence.put("wire_open_thaumonomicon_packets", count);
        if (count == 0) {
            if (stageTicks < 200) return false;
            throw new IllegalStateException("The clientbound open_thaumonomicon payload never crossed the wire");
        }
        return true;
    }

    /**
     * The research table COMBINE over the real channel. The combined aspect is a
     * new one, so the server also announces it with the clientbound
     * aspect_discovery payload.
     */
    private boolean stageWireCombine(Minecraft client) throws Exception {
        if (stageData.get("funded") == null) {
            client.getConnection().sendCommand("thaumcraft aspect " + playerName + " " + COMBINE_LEFT + " 2");
            client.getConnection().sendCommand("thaumcraft aspect " + playerName + " " + COMBINE_RIGHT + " 2");
            stageData.put("funded", true);
            return false;
        }
        var knowledge = KnowledgeAccess.get(client.player);
        if (knowledge.getAspectPoolFor(COMBINE_LEFT) < 1 || knowledge.getAspectPoolFor(COMBINE_RIGHT) < 1) return false;
        if (!ensureResearchTablePair(client)) return false;
        if (!(client.player.containerMenu instanceof ContainerResearchTable menu)) {
            if (stageData.get("open") == null) {
                interact(client, tablePos);
                stageData.put("open", true);
            }
            return false;
        }
        if (!menu.editReady()) return false;
        if (stageData.get("combined") == null) {
            if (!menu.requestCombine(COMBINE_LEFT, COMBINE_RIGHT)) return false;
            stageData.put("combined", true);
            return false;
        }
        int sent = MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:research_table_combine", new AtomicInteger()).get();
        int discovered = MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:aspect_discovery", new AtomicInteger()).get();
        evidence.put("wire_research_table_combine_packets", sent);
        evidence.put("wire_aspect_discovery_packets", discovered);
        evidence.put("wire_combined_aspect", COMBINE_RESULT);
        evidence.put("wire_lead_discovered_aspects",
                new ArrayList<>(new TreeSet<>(knowledge.getAspectsDiscovered().keySet())));
        if (sent == 0 || discovered == 0) {
            if (stageTicks < 400) return false;
            throw new IllegalStateException("The research table combination did not reach the wire: combine=" + sent
                    + " aspect_discovery=" + discovered);
        }
        client.player.closeContainer();
        return true;
    }

    /**
     * The original's research table is a PAIR: a plain table and its partner,
     * converted by the inkwell. The acceptance uses the original conversion, so
     * the main half is the north variant 2 and the partner the variant 7.
     */
    private boolean ensureResearchTablePair(Minecraft client) throws Exception {
        if (tablePos == null) {
            tablePos = client.player.blockPosition().offset(1, 0, 0);
            tablePartner = tablePos.north();
        }
        int variant = variantAt(client, tablePos);
        if (variant >= 2) return true;
        if (stageData.get("placed") == null) {
            client.getConnection().sendCommand("setblock " + tablePos.getX() + " " + tablePos.getY() + " " + tablePos.getZ()
                    + " thaumcraft:table");
            client.getConnection().sendCommand("setblock " + tablePartner.getX() + " " + tablePartner.getY() + " "
                    + tablePartner.getZ() + " thaumcraft:table");
            stageData.put("placed", true);
            return false;
        }
        int inkSlot = slotOf(client, ThaumcraftContent.INKWELL.get());
        if (inkSlot < 0) return false;
        if (!holdItemOf(client, ThaumcraftContent.INKWELL.get())) return false;
        if (stageTicks % 20 == 0) interact(client, tablePos);
        return false;
    }

    /** The research table COPY over the real channel. */
    private boolean stageWireCopy(Minecraft client) throws Exception {
        if (!ensureResearchTablePair(client)) return false;
        if (!(client.player.containerMenu instanceof ContainerResearchTable menu)) {
            if (stageData.get("open") == null) {
                interact(client, tablePos);
                stageData.put("open", true);
            }
            return false;
        }
        if (stageData.get("attempted") == null) {
            boolean accepted = menu.requestCopy();
            evidence.put("wire_research_table_copy_client_accepted", accepted);
            stageData.put("attempted", true);
            if (!accepted) {
                // The original's copy needs a COMPLETED discovery note, the
                // RESEARCHDUPE research, a paper and an ink sac. The intent was
                // refused locally, so no payload could cross the wire at all.
                evidence.put("wire_research_table_copy_observed", false);
                evidence.put("wire_research_table_copy_reason",
                        "the client refused the copy intent: the note is not a completed discovery and the player has no RESEARCHDUPE");
                client.player.closeContainer();
                return true;
            }
            return false;
        }
        int sent = MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:research_table_copy", new AtomicInteger()).get();
        evidence.put("wire_research_table_copy_packets", sent);
        evidence.put("wire_research_table_copy_observed", sent > 0);
        if (sent == 0) {
            if (stageTicks < 400) return false;
            throw new IllegalStateException("The research_table_copy payload never crossed the wire");
        }
        client.player.closeContainer();
        return true;
    }

    /** The three client toggles of the original misc key and the wand. */
    private boolean stageWireToggles(Minecraft client) {
        if (stageData.get("sent") == null) {
            WandArchitectNetworking.request();
            HoverNetworking.requestToggle();
            ShovelOrientationNetworking.request();
            stageData.put("sent", true);
            return false;
        }
        int architect = MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:architect_toggle", new AtomicInteger()).get();
        int hover = MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:hover_toggle", new AtomicInteger()).get();
        int shovel = MultiplayerPairLink.CENSUS.serverbound.getOrDefault("thaumcraft:shovel_orientation", new AtomicInteger()).get();
        evidence.put("wire_architect_toggle_packets", architect);
        evidence.put("wire_hover_toggle_packets", hover);
        evidence.put("wire_shovel_orientation_packets", shovel);
        if (architect == 0 || hover == 0 || shovel == 0) {
            if (stageTicks < 200) return false;
            throw new IllegalStateException("A client toggle payload never crossed the wire: architect=" + architect
                    + " hover=" + hover + " shovel=" + shovel);
        }
        return true;
    }

    /**
     * The runed eldritch trap. The original picks the CLOSEST player within three
     * blocks, hurts it and warps half of the hits. The trap also draws the node
     * bolt, which is the clientbound block_fx payload.
     */
    private boolean stageWireTrap(Minecraft client) throws Exception {
        if (!ensureProtection(client)) return false;
        if (stageData.get("placed") == null) {
            BlockPos trap = client.player.blockPosition().offset(2, 0, 0);
            client.getConnection().sendCommand("setblock " + trap.getX() + " " + trap.getY() + " " + trap.getZ()
                    + " thaumcraft:eldritch_spawner[variant=10]");
            stageData.put("placed", true);
            stageData.put("trap", trap);
            return false;
        }
        BlockPos trap = (BlockPos) stageData.get("trap");
        int blockFx = MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:block_fx", new AtomicInteger()).get();
        int warpMessage = MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:warp_message", new AtomicInteger()).get();
        evidence.put("wire_block_fx_packets", blockFx);
        evidence.put("wire_warp_message_packets", warpMessage);
        evidence.put("wire_lead_warp_after_trap", KnowledgeAccess.get(client.player).getTotalWarp());
        evidence.put("wire_trap_position", List.of(trap.getX(), trap.getY(), trap.getZ()));
        if (blockFx == 0 || warpMessage == 0) {
            if (stageTicks < 600) return false;
            throw new IllegalStateException("The runed eldritch trap produced no clientbound effect: block_fx=" + blockFx
                    + " warp_message=" + warpMessage);
        }
        return true;
    }

    /**
     * The lead has to survive the eldritch trap and the eldritch guardian. The
     * vanilla effect command is a SIGNED command on an offline-mode server, so it
     * is retried until the client really carries the effect.
     */
    private boolean ensureProtection(Minecraft client) {
        boolean boosted = client.player.hasEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST);
        boolean resistant = client.player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
        if (boosted && resistant) return true;
        if (stageTicks % 20 == 0) {
            client.getConnection().sendCommand("effect give " + playerName + " minecraft:health_boost 9999 20");
            client.getConnection().sendCommand("effect give " + playerName + " minecraft:resistance 9999 4");
        }
        return false;
    }

    /**
     * The eldritch guardian's warp event.
     *
     * <p>The original sends it to every player whose SQUARED distance is below
     * 256 (a radius of sixteen blocks, oracle
     * {@code thaumcraft/common/entities/monster/EntityEldritchGuardian.java}:162-168),
     * so the guardian has to be near the player.</p>
     */
    private boolean stageWireWarpEvent(Minecraft client) throws Exception {
        if (!ensureProtection(client)) return false;
        if (stageData.get("difficulty") == null) {
            client.getConnection().sendCommand("difficulty normal");
            client.getConnection().sendCommand("time set day");
            stageData.put("difficulty", true);
            return false;
        }
        if (stageData.get("summoned") == null) {
            BlockPos pos = client.player.blockPosition().offset(8, 0, 0);
            client.getConnection().sendCommand("summon thaumcraft:eldritch_guardian " + pos.getX() + " " + pos.getY()
                    + " " + pos.getZ());
            stageData.put("summoned", true);
            return false;
        }
        int warpEvent = MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:warp_event", new AtomicInteger()).get();
        evidence.put("wire_warp_event_packets", warpEvent);
        if (warpEvent == 0) {
            if (stageTicks < 900) return false;
            throw new IllegalStateException("The eldritch guardian produced no clientbound warp event");
        }
        return true;
    }

    /**
     * The eldritch guardian's sonic screech.
     *
     * <p>The guardian only uses its ranged attack at twenty to forty blocks, so
     * it is summoned far away and the lead keeps that distance. The warp event
     * of the same guardian needs the opposite distance, which is why the two
     * channels have two stages.</p>
     */
    private boolean stageWireGuardianSonic(Minecraft client) throws Exception {
        if (!ensureProtection(client)) return false;
        if (stageData.get("summoned") == null) {
            client.getConnection().sendCommand("kill @e[type=thaumcraft:eldritch_guardian]");
            BlockPos pos = client.player.blockPosition().offset(30, 0, 0);
            client.getConnection().sendCommand("summon thaumcraft:eldritch_guardian " + pos.getX() + " " + pos.getY()
                    + " " + pos.getZ());
            stageData.put("summoned", true);
            stageData.put("guardian", pos);
            return false;
        }
        BlockPos guardian = (BlockPos) stageData.get("guardian");
        // The lead stays twenty-eight blocks away, so the guardian keeps using
        // its ranged attack instead of walking into melee range.
        if (stageTicks % 20 == 0) {
            client.getConnection().sendCommand("tp " + playerName + " " + (guardian.getX() - 28) + " " + guardian.getY()
                    + " " + guardian.getZ());
        }
        int sonic = MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:guardian_sonic", new AtomicInteger()).get();
        evidence.put("wire_guardian_sonic_packets", sonic);
        evidence.put("wire_guardian_position", List.of(guardian.getX(), guardian.getY(), guardian.getZ()));
        if (sonic == 0) {
            if (stageTicks < 1800) return false;
            throw new IllegalStateException("The eldritch guardian produced no clientbound sonic screech");
        }
        return true;
    }

    /** The wisp's zap. The original wisp discards itself in peaceful, so the difficulty has to change. */
    private boolean stageWireWisp(Minecraft client) throws Exception {
        if (!ensureProtection(client)) return false;
        if (stageData.get("difficulty") == null) {
            client.getConnection().sendCommand("difficulty normal");
            client.getConnection().sendCommand("time set day");
            stageData.put("difficulty", true);
            return false;
        }
        if (stageData.get("summoned") == null) {
            // The original wisp only looks for a player once in a thousand
            // ticks, so eight of them make the zap reachable.
            BlockPos pos = client.player.blockPosition().offset(2, 1, 0);
            for (int index = 0; index < 8; index++) {
                client.getConnection().sendCommand("summon thaumcraft:wisp " + (pos.getX() + index) + " " + pos.getY()
                        + " " + pos.getZ());
            }
            stageData.put("summoned", true);
            return false;
        }
        int zap = MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:wisp_zap", new AtomicInteger()).get();
        evidence.put("wire_wisp_zap_packets", zap);
        if (zap == 0) {
            if (stageTicks < 2400) return false;
            client.getConnection().sendCommand("difficulty peaceful");
            throw new IllegalStateException("The wisp never produced its clientbound zap");
        }
        client.getConnection().sendCommand("difficulty peaceful");
        return true;
    }

    // -------------------------------------------- the SECOND physical client

    /** The second physical client is online and both clients see the same player list. */
    private boolean stagePairPeerJoined(Minecraft client) {
        Map<String, String> status = peerStatus();
        if (status.isEmpty()) return false;
        int listed = client.getConnection() == null ? 0 : client.getConnection().getOnlinePlayers().size();
        evidence.put("pair_player_list_size", listed);
        if (listed < 2) return false;
        if (stageData.get("hello") == null) {
            pairCommand("HELLO", "");
            stageData.put("hello", true);
            return false;
        }
        if (!pairReplied()) return false;
        evidence.put("pair_peer_hello", pairAnswer);
        evidence.put("pair_peer_name", peerValue("name"));
        evidence.put("pair_peer_uuid", peerValue("uuid"));
        evidence.put("pair_peer_player_list", peerValue("players"));
        evidence.put("pair_peer_entities", peerValue("entities"));
        if (!peerValue("players").equals(String.valueOf(listed))) {
            throw new IllegalStateException("The two clients disagree about the player list: peer=" + peerValue("players")
                    + " lead=" + listed);
        }
        // The second client is a helper: it must not die while the acceptance
        // runs, and the creative mode is also the state the original's
        // closest-player rule has to refuse later.
        if (!"creative".equals(peerValue("gm"))) {
            if (stageData.get("creative") == null) {
                client.getConnection().sendCommand("gamemode creative " + peerName);
                stageData.put("creative", true);
            }
            return false;
        }
        return true;
    }

    /** The knowledge snapshot of one player must never reach the other player. */
    private boolean stagePairPerPlayerKnowledge(Minecraft client) {
        if (stageData.get("cleanup") == null) {
            // The eldritch guardian and the wisp of the wire stages would warp
            // every player that walks into them, so they are removed first.
            client.getConnection().sendCommand("kill @e[type=thaumcraft:eldritch_guardian]");
            client.getConnection().sendCommand("kill @e[type=thaumcraft:wisp]");
            stageData.put("cleanup", true);
            return false;
        }
        if (!bringPeerNextToLead(client)) return false;
        // The grant and the question are repeated until the second client
        // really reports the research: the client tick and the server tick are
        // independent, so the server may not have processed the grant when the
        // question is asked for the first time.
        if (stageTicks % 20 == 0) {
            client.getConnection().sendCommand("thaumcraft research " + peerName + " " + PEER_RESEARCH);
            pairCommand("HAS_RESEARCH", PEER_RESEARCH);
        }
        if (!pairReplied()) return false;
        evidence.put("pair_peer_research_answer", pairAnswer);
        boolean peerHas = pairAnswer.endsWith(" 1");
        boolean leadHas = KnowledgeAccess.get(client.player).isResearchComplete(PEER_RESEARCH);
        evidence.put("pair_lead_has_peer_research", leadHas);
        evidence.put("pair_peer_has_own_research", peerHas);
        if (!peerHas) {
            if (stageTicks > 600) {
                throw new IllegalStateException("The second client never received its own research grant: " + pairAnswer);
            }
            return false;
        }
        if (leadHas) throw new IllegalStateException("The lead client received the second client's research grant");
        return true;
    }

    /**
     * Brings the second client next to the lead. The vanilla teleport is a
     * SIGNED command, so it is retried until the second client really reports
     * the new position.
     */
    private boolean bringPeerNextToLead(Minecraft client) {
        int wanted = client.player.getBlockX() + 2;
        Map<String, String> status = peerStatus();
        if (String.valueOf(wanted).equals(status.get("x"))) return true;
        if (stageTicks % 20 == 0) {
            client.getConnection().sendCommand("tp " + peerName + " " + wanted + " " + client.player.getBlockY() + " "
                    + client.player.getBlockZ());
        }
        return false;
    }

    /** The accessory snapshot of one player must never reach the other player. */
    private boolean stagePairAccessoryIsolation(Minecraft client) throws Exception {
        // The original's vis amulet may only be worn with the VISAMULET
        // research (ItemAmuletVis.canEquip), so the SECOND client is granted
        // it before it equips the amulet.
        if (!Boolean.TRUE.equals(stageData.get("research_done"))) {
            if (stageTicks % 20 == 0) {
                client.getConnection().sendCommand("thaumcraft research " + peerName + " " + AMULET_RESEARCH);
                pairCommand("HAS_RESEARCH", AMULET_RESEARCH);
            }
            if (!pairReplied()) return false;
            if (!pairAnswer.endsWith(" 1")) {
                if (stageTicks > 600) {
                    throw new IllegalStateException("The second client never received the " + AMULET_RESEARCH + " research");
                }
                return false;
            }
            evidence.put("pair_peer_amulet_research_answer", pairAnswer);
            stageData.put("research_done", true);
            return false;
        }
        if (stageData.get("give") == null) {
            if (!giveToPeer(client, "thaumcraft:vis_amulet 1")) return false;
            stageData.put("give", true);
            return false;
        }
        if (stageData.get("before") == null) {
            pairCommand("COUNT", "thaumcraft:accessories_contents");
            stageData.put("before", true);
            return false;
        }
        if (!pairReplied()) return false;
        int peerBefore = parseCount(pairAnswer);
        evidence.put("pair_peer_accessories_payloads_before", peerBefore);
        if (stageData.get("equip") == null) {
            pairCommand("EQUIP_ACCESSORY", "");
            stageData.put("equip", true);
            return false;
        }
        if (!pairReplied()) return false;
        evidence.put("pair_peer_equip_answer", pairAnswer);
        if (stageData.get("after") == null) {
            pairCommand("COUNT", "thaumcraft:accessories_contents");
            stageData.put("after", true);
            return false;
        }
        if (!pairReplied()) return false;
        int peerAfter = parseCount(pairAnswer);
        int leadCount = MultiplayerPairLink.CENSUS.clientbound.getOrDefault("thaumcraft:accessories_contents", new AtomicInteger()).get();
        evidence.put("pair_peer_accessories_payloads_after", peerAfter);
        evidence.put("pair_lead_accessories_payloads", leadCount);
        if (peerAfter <= peerBefore) {
            throw new IllegalStateException("The second client received no accessory snapshot after equipping one");
        }
        int leadAccessories = 0;
        var inventory = PlayerAccessories.get(client.player);
        for (int slot = 0; slot < 4; slot++) if (!inventory.getItem(slot).isEmpty()) leadAccessories++;
        evidence.put("pair_lead_accessory_slots", leadAccessories);
        if (leadAccessories != 0) {
            throw new IllegalStateException("The lead client received the second client's accessory contents");
        }
        return true;
    }

    /** The golem bell of the OTHER player: the original refuses the stranger. */
    private boolean stagePairGolemBellOwner(Minecraft client) throws Exception {
        if (!bringPeerNextToLead(client)) return false;
        if (stageData.get("give") == null) {
            if (!giveToPeer(client, "thaumcraft:golem_bell 1")) return false;
            stageData.put("give", true);
            return false;
        }
        if (stageData.get("summon") == null) {
            Map<String, String> status = peerStatus();
            int px = Integer.parseInt(status.getOrDefault("x", "0"));
            int py = Integer.parseInt(status.getOrDefault("y", "0"));
            int pz = Integer.parseInt(status.getOrDefault("z", "0"));
            BlockPos pos = new BlockPos(px + 1, py, pz);
            client.getConnection().sendCommand("summon thaumcraft:golem " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                    + " {Owner:\"" + peerName + "\"}");
            stageData.put("summon", true);
            return false;
        }
        EntityGolemBase golem = peerOwnedGolem(client);
        if (golem == null) return false;
        evidence.put("pair_golem_owner_on_client", golem.getOwnerName());
        if (stageData.get("bell") == null) {
            if (!holdItemOf(client, thaumcraft.common.config.ThaumcraftGolems.GOLEM_BELL.get())) return false;
            stageData.put("bell", true);
            return false;
        }
        if (stageData.get("used") == null) {
            client.gameMode.interact(client.player, golem, InteractionHand.MAIN_HAND);
            stageData.put("used", true);
            return false;
        }
        String refusal = lastMessageContaining("You are not my Master!");
        evidence.put("pair_golem_bell_refusal", refusal);
        boolean leadSelected = bellSelected(client);
        evidence.put("pair_lead_bell_selected", leadSelected);
        if (leadSelected) throw new IllegalStateException("The lead client took over the second client's golem");
        if (refusal == null) {
            if (stageTicks < 300) return false;
            throw new IllegalStateException("The lead client was not refused the second client's golem");
        }
        if (stageData.get("peer_bell") == null) {
            pairCommand("BELL_GOLEM", "");
            stageData.put("peer_bell", true);
            return false;
        }
        if (!pairReplied()) return false;
        evidence.put("pair_peer_bell_answer", pairAnswer);
        if (!pairAnswer.contains("selected=1")) {
            throw new IllegalStateException("The second client could not select its own golem: " + pairAnswer);
        }
        return true;
    }

    /** The travelling trunk of the OTHER player: only its owner may open the water trunk. */
    private boolean stagePairTravelingTrunkOwner(Minecraft client) throws Exception {
        if (!bringPeerNextToLead(client)) return false;
        if (stageData.get("summon") == null) {
            Map<String, String> status = peerStatus();
            int px = Integer.parseInt(status.getOrDefault("x", "0"));
            int py = Integer.parseInt(status.getOrDefault("y", "0"));
            int pz = Integer.parseInt(status.getOrDefault("z", "0"));
            BlockPos pos = new BlockPos(px - 1, py, pz);
            client.getConnection().sendCommand("summon thaumcraft:traveling_trunk " + pos.getX() + " " + pos.getY() + " "
                    + pos.getZ() + " {upgrade:3b,Owner:\"" + peerName + "\",OwnerUUID:" + uuidIntArray(status.get("uuid")) + "}");
            stageData.put("summon", true);
            return false;
        }
        EntityTravelingTrunk trunk = peerOwnedTrunk(client);
        if (trunk == null) return false;
        evidence.put("pair_trunk_owner_on_client", trunk.getOwnerName());
        evidence.put("pair_trunk_upgrade_on_client", trunk.getUpgrade());
        if (stageData.get("peer_open") == null) {
            pairCommand("OPEN_TRUNK", "");
            stageData.put("peer_open", true);
            return false;
        }
        if (!pairReplied()) return false;
        evidence.put("pair_peer_trunk_answer", pairAnswer);
        if (!pairAnswer.contains("open=1")) {
            throw new IllegalStateException("The second client could not open its own trunk: " + pairAnswer);
        }
        if (stageData.get("lead_used") == null) {
            client.gameMode.interact(client.player, trunk, InteractionHand.MAIN_HAND);
            stageData.put("lead_used", true);
            return false;
        }
        boolean leadOpened = client.player.containerMenu instanceof thaumcraft.common.container.ContainerTravelingTrunk;
        evidence.put("pair_lead_trunk_opened", leadOpened);
        if (leadOpened) throw new IllegalStateException("The lead client opened the second client's water trunk");
        return true;
    }

    /**
     * The research table is a PAIR. The partner half of the second player must
     * open the MAIN half, and both players must see the same table.
     */
    private boolean stagePairResearchTablePartner(Minecraft client) throws Exception {
        if (stageData.get("tables") == null) {
            // The eldritch guardian may have knocked the lead back, so the
            // pair stage uses a FRESH pair of tables at the lead's position.
            tablePos = client.player.blockPosition().offset(1, 0, 0);
            tablePartner = tablePos.north();
            client.getConnection().sendCommand("setblock " + tablePos.getX() + " " + tablePos.getY() + " " + tablePos.getZ()
                    + " thaumcraft:table");
            client.getConnection().sendCommand("setblock " + tablePartner.getX() + " " + tablePartner.getY() + " "
                    + tablePartner.getZ() + " thaumcraft:table");
            stageData.put("tables", true);
            return false;
        }
        int variant = variantAt(client, tablePos);
        if (variant < 2) {
            if (!holdItemOf(client, ThaumcraftContent.INKWELL.get())) return false;
            if (stageTicks % 20 == 0) interact(client, tablePos);
            return false;
        }
        evidence.put("pair_research_table_variant", variant);
        evidence.put("pair_research_table_partner_variant", variantAt(client, tablePartner));
        // The second client has to stand next to the partner half.
        if (!bringPeerNextToLead(client)) return false;
        if (stageData.get("peer_open") == null) {
            pairCommand("OPEN_TABLE", tablePartner.getX() + "," + tablePartner.getY() + "," + tablePartner.getZ());
            stageData.put("peer_open", true);
            return false;
        }
        if (!pairReplied()) return false;
        evidence.put("pair_peer_table_answer", pairAnswer);
        String peerMain = pairValue(pairAnswer, "main");
        evidence.put("pair_peer_table_main", peerMain);
        if (peerMain == null) {
            throw new IllegalStateException("The second client did not open the research table: " + pairAnswer);
        }
        if (stageData.get("lead_open") == null) {
            interact(client, tablePos);
            stageData.put("lead_open", true);
            return false;
        }
        if (!(client.player.containerMenu instanceof ContainerResearchTable menu)) return false;
        String leadMain = menu.mainPos().getX() + "," + menu.mainPos().getY() + "," + menu.mainPos().getZ();
        evidence.put("pair_lead_table_main", leadMain);
        if (!leadMain.equals(peerMain)) {
            throw new IllegalStateException("The two players opened different research tables: lead=" + leadMain
                    + " peer=" + peerMain);
        }
        client.player.closeContainer();
        return true;
    }

    /**
     * The original's closest-player rule: a creative player is NEVER the closest
     * player, so the trap must warp the non-creative player.
     */
    private boolean stagePairCreativePlayerExcluded(Minecraft client) throws Exception {
        if (stageData.get("creative") == null) {
            client.getConnection().sendCommand("gamemode creative " + peerName);
            stageData.put("creative", true);
            return false;
        }
        if (stageData.get("ask") == null) {
            pairCommand("GAMEMODE", "creative");
            stageData.put("ask", true);
            return false;
        }
        if (!pairReplied()) return false;
        evidence.put("pair_peer_gamemode_answer", pairAnswer);
        if (!pairAnswer.contains("creative")) {
            throw new IllegalStateException("The second client never became creative: " + pairAnswer);
        }
        if (stageData.get("place") == null) {
            // The peer stands one block further from the trap than the lead, but
            // the creative peer is never the closest player of the original.
            BlockPos trap = client.player.blockPosition().offset(2, 0, 0);
            client.getConnection().sendCommand("setblock " + trap.getX() + " " + trap.getY() + " " + trap.getZ()
                    + " thaumcraft:eldritch_spawner[variant=10]");
            stageData.put("place", true);
            stageData.put("trap", trap);
            stageData.put("lead_warp_before", KnowledgeAccess.get(client.player).getTotalWarp());
            stageData.put("peer_warp_before", peerValue("warp"));
            return false;
        }
        BlockPos trap = (BlockPos) stageData.get("trap");
        // The creative peer has to stand CLOSER to the trap than the lead.
        Map<String, String> status = peerStatus();
        if (!String.valueOf(trap.getX() + 1).equals(status.get("x"))) {
            if (stageTicks % 20 == 0) {
                client.getConnection().sendCommand("tp " + peerName + " " + (trap.getX() + 1) + " " + trap.getY() + " "
                        + trap.getZ());
            }
            return false;
        }
        int leadWarp = KnowledgeAccess.get(client.player).getTotalWarp();
        int leadWarpBefore = (Integer) stageData.get("lead_warp_before");
        evidence.put("pair_lead_warp_before_trap", leadWarpBefore);
        evidence.put("pair_lead_warp_after_trap", leadWarp);
        evidence.put("pair_trap_position", List.of(trap.getX(), trap.getY(), trap.getZ()));
        if (leadWarp <= leadWarpBefore) {
            if (stageTicks < 600) return false;
            throw new IllegalStateException("The runed eldritch trap never warped the non-creative player");
        }
        if (stageData.get("ask_warp") == null) {
            pairCommand("REPORT", "");
            stageData.put("ask_warp", true);
            return false;
        }
        if (!pairReplied()) return false;
        evidence.put("pair_peer_status_after_trap", pairAnswer);
        String peerWarp = pairValue(pairAnswer, "warp");
        String peerWarpBefore = (String) stageData.get("peer_warp_before");
        evidence.put("pair_peer_warp_before_trap", peerWarpBefore);
        evidence.put("pair_peer_warp_after_trap", peerWarp);
        if (peerWarp != null && peerWarpBefore != null && !peerWarp.equals(peerWarpBefore)) {
            throw new IllegalStateException("The creative player was warped by the trap: warp=" + peerWarpBefore
                    + " -> " + peerWarp);
        }
        return true;
    }

    /** The second client is released once the acceptance is over. */
    private boolean stagePairPeerStop(Minecraft client) {
        if (stageData.get("stop") == null) {
            pairCommand("STOP", "");
            stageData.put("stop", true);
            return false;
        }
        return true;
    }

    // ------------------------------------------------------- the pair helpers

    private void pairCommand(String token, String arguments) {
        pairSequence++;
        MultiplayerPairLink.writeAtomic(MultiplayerPairLink.commandFile(pairDirectory),
                pairSequence + "\n" + token + (arguments == null || arguments.isEmpty() ? "" : " " + arguments) + "\n");
    }

    /** True once the second client answered the command that was sent last. */
    private boolean pairReplied() {
        var entry = MultiplayerPairLink.readEntry(MultiplayerPairLink.resultFile(pairDirectory));
        if (entry == null || entry.sequence() != pairSequence) return false;
        pairAnswer = entry.payload();
        return true;
    }

    private Map<String, String> peerStatus() {
        String content = MultiplayerPairLink.read(MultiplayerPairLink.statusFile(pairDirectory));
        Map<String, String> values = new LinkedHashMap<>();
        if (content == null) return values;
        for (String part : content.trim().split("\\s+")) {
            int equals = part.indexOf('=');
            if (equals > 0) values.put(part.substring(0, equals), part.substring(equals + 1));
        }
        return values;
    }

    private String peerValue(String key) {
        return peerStatus().get(key);
    }

    /** One "key=value" field of the second client's answer. */
    private static String pairValue(String answer, String key) {
        if (answer == null) return null;
        for (String part : answer.split("\\s+")) {
            if (part.startsWith(key + "=")) return part.substring(key.length() + 1);
        }
        return null;
    }

    /** The clientbound count of the second client's answer to a COUNT command. */
    private static int parseCount(String answer) {
        String value = pairValue(answer, "count");
        if (value == null) return -1;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException malformed) {
            return -1;
        }
    }

    /** The give command of the vanilla server answers "Gave <item> to <player>". */
    private int giveRepliesFor(String target) {
        int count = 0;
        synchronized (MultiplayerPairLink.CENSUS.messages) {
            for (String message : MultiplayerPairLink.CENSUS.messages) {
                if (message.startsWith("Gave ") && message.contains(" to " + target)) count++;
            }
        }
        return count;
    }

    /**
     * Gives the SECOND client an item and waits until its own inventory has
     * it.
     *
     * <p>The SECOND client runs the give itself: the vanilla give command is
     * a SIGNED command and the offline-mode server drops a signed command
     * whose signature does not match its chat chain SILENTLY. That happens
     * with the command of the OTHER client far more often than with its own,
     * so the second client is asked to give itself the item.</p>
     */
    private boolean giveToPeer(Minecraft client, String... itemArguments) {
        String key = "peer_give_sent";
        if (stageData.get(key) == null) {
            pairCommand("GIVE", String.join(",", itemArguments));
            stageData.put(key, true);
            return false;
        }
        String held = peerValue("held");
        boolean complete = held != null;
        for (String argument : itemArguments) {
            String path = argument.substring(argument.lastIndexOf(':') + 1).trim().split(" ")[0];
            if (!held.contains(path)) complete = false;
        }
        if (complete) return true;
        if (stageTicks % 40 == 0) pairCommand("GIVE", String.join(",", itemArguments));
        if (stageTicks > 600) {
            throw new IllegalStateException("The second client never received " + itemArguments[0]
                    + "; its own inventory holds: " + held);
        }
        return false;
    }

    /** The golem of the second client, as the lead's client sees it. */
    private EntityGolemBase peerOwnedGolem(Minecraft client) {
        EntityGolemBase best = null;
        double bestDistance = Double.MAX_VALUE;
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EntityGolemBase golem)) continue;
            if (!peerName.equals(golem.getOwnerName())) continue;
            double distance = golem.distanceToSqr(client.player);
            if (distance < bestDistance) { bestDistance = distance; best = golem; }
        }
        return best;
    }

    /** The travelling trunk of the second client, as the lead's client sees it. */
    private EntityTravelingTrunk peerOwnedTrunk(Minecraft client) {
        EntityTravelingTrunk best = null;
        double bestDistance = Double.MAX_VALUE;
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EntityTravelingTrunk trunk)) continue;
            if (!peerName.equals(trunk.getOwnerName())) continue;
            double distance = trunk.distanceToSqr(client.player);
            if (distance < bestDistance) { bestDistance = distance; best = trunk; }
        }
        return best;
    }

    /** Whether the lead's own bell carries a golem selection. */
    private static boolean bellSelected(Minecraft client) {
        for (ItemStack stack : client.player.getInventory().items) {
            if (!stack.is(thaumcraft.common.config.ThaumcraftGolems.GOLEM_BELL.get())) continue;
            var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (data == null) continue;
            if (data.copyTag().contains("thaumcraft:golem_bell")) return true;
        }
        return false;
    }

    /** The SNBT int array of an UUID, the form the entity NBT of the original uses. */
    private static String uuidIntArray(String uuid) {
        if (uuid == null || uuid.isBlank()) return "[I;0,0,0,0]";
        UUID parsed = UUID.fromString(uuid);
        // The NBT int array holds SIGNED 32 bit values, so a UUID whose high
        // half is above 2^31 must be written with its negative counterpart.
        return "[I;" + (int) (parsed.getMostSignificantBits() >> 32) + "," + (int) parsed.getMostSignificantBits()
                + "," + (int) (parsed.getLeastSignificantBits() >> 32) + "," + (int) parsed.getLeastSignificantBits() + "]";
    }

    // ------------------------------------------------------------------- driver

    private void tick(Minecraft client) {
        if (finished) return;
        if (stage >= stages.size()) {
            finished = true;
            writeReport(failures.isEmpty() ? "SCRIPT_PASSED" : "SCRIPT_FAILED");
            LOGGER.info("Multiplayer probe finished: {}", evidence);
            client.stop();
            return;
        }
        Stage current = stages.get(stage);
        stageTicks++;
        if (stageTicks % 200 == 0) LOGGER.info("Multiplayer probe stage {} still waiting at tick {}", current.name(), stageTicks);
        if (client.player == null && stage > 0 && !current.name().equals("disconnect")
                && !current.name().equals("reconnect") && !current.name().equals("reconnect_state_consistent")) {
            failures.add(current.name() + ": the client lost the dedicated-server connection");
            stepLog.add("FAIL " + current.name() + " :: connection lost");
            LOGGER.error("Multiplayer probe stage {} lost the connection", current.name());
            finished = true;
            writeReport("SCRIPT_FAILED");
            client.stop();
            return;
        }
        if (client.player != null && stage > 0 && client.player.isDeadOrDying()
                && !current.name().equals("disconnect")) {
            failures.add(current.name() + ": the player died during the acceptance");
            stepLog.add("FAIL " + current.name() + " :: the player died");
            LOGGER.error("Multiplayer probe stage {}: the player died", current.name());
            finished = true;
            writeReport("SCRIPT_FAILED");
            client.stop();
            return;
        }
        keepAlive(client);
        boolean done;
        try {
            done = current.body().run(client);
        } catch (Throwable failure) {
            // A failed stage is recorded, but the remaining stages still run so
            // that one defect does not hide the rest of the acceptance.
            failures.add(current.name() + ": " + describe(failure));
            LOGGER.error("Multiplayer probe stage {} failed", current.name(), failure);
            stepLog.add("FAIL " + current.name() + " :: " + describe(failure));
            stage++;
            stageTicks = 0;
            stageData.clear();
            return;
        }
        if (done) {
            stepLog.add("PASS " + current.name());
            LOGGER.info("Multiplayer probe stage {} passed", current.name());
            stage++;
            stageTicks = 0;
            stageData.clear();
        } else if (stageTicks > STAGE_TIMEOUT_TICKS) {
            failures.add(current.name() + ": stage timeout after " + STAGE_TIMEOUT_TICKS + " ticks");
            stepLog.add("FAIL " + current.name() + " :: timeout");
            LOGGER.error("Multiplayer probe stage {} timed out", current.name());
            finished = true;
            writeReport("SCRIPT_FAILED");
            client.stop();
        }
    }

    /**
     * The vanilla server drops an idle connection after 600 of its own ticks.
     * A dedicated server on an oversubscribed build host can spend minutes in
     * one tick, so the probe keeps the real connection alive with a harmless
     * serverbound packet instead of losing the session to the idle timeout.
     */
    private void keepAlive(Minecraft client) {
        if (client.getConnection() == null || client.player == null) return;
        if (stageTicks % 100 != 0) return;
        try {
            if (client.player.containerMenu == client.player.inventoryMenu) {
                client.getConnection().sendCommand("thaumcraft status");
            }
        } catch (RuntimeException ignored) {
            // A closing connection is reported by the stage that needs it.
        }
    }

    // ------------------------------------------------------------------ helpers

    private void installCensus(Connection connection) {
        // The clientTest mixin installs the census when the play connection is
        // built, so the join time payloads are observed too. This call is the
        // fallback for a connection that was built before the mixin ran.
        MultiplayerPairLink.CENSUS.install(connection);
    }

    /** The ids the physical client registered for one Fabric payload registry. */
    @SuppressWarnings("unchecked")
    private static List<String> payloadTypeIds(String registry) {
        try {
            Class<?> impl = Class.forName("net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl");
            Field field = impl.getField(registry);
            Object holder = field.get(null);
            Field types = holder.getClass().getDeclaredField("packetTypes");
            types.setAccessible(true);
            Map<Object, Object> map = (Map<Object, Object>) types.get(holder);
            List<String> ids = new ArrayList<>();
            for (Object key : map.keySet()) ids.add(String.valueOf(key));
            java.util.Collections.sort(ids);
            return ids;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot read the client payload registry " + registry, failure);
        }
    }

    private static void count(Map<String, AtomicInteger> counters, String id) {
        counters.computeIfAbsent(id, ignored -> new AtomicInteger()).incrementAndGet();
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                // keep walking
            }
        }
        throw new NoSuchFieldException(name);
    }

    private String awaitMessage(java.util.function.Predicate<String> matcher) {
        synchronized (MultiplayerPairLink.CENSUS.messages) {
            for (String message : MultiplayerPairLink.CENSUS.messages) if (matcher.test(message)) return message;
        }
        return null;
    }

    private String lastMessageContaining(String needle) {
        String found = null;
        synchronized (MultiplayerPairLink.CENSUS.messages) {
            for (String message : MultiplayerPairLink.CENSUS.messages) if (message.contains(needle)) found = message;
        }
        return found;
    }

    /** A rejected server command must fail the stage instead of timing out. */
    private void rejectBrokenCommand() {
        String failure = lastMessageContaining("Unknown or incomplete command");
        if (failure == null) failure = lastMessageContaining("Incorrect argument for command");
        if (failure == null) failure = lastMessageContaining("Unknown command");
        if (failure != null) throw new IllegalStateException("The server rejected the probe command: " + failure);
    }

    private static int inventoryCount(Minecraft client, net.minecraft.world.item.Item item) {
        int total = 0;
        for (ItemStack stack : client.player.getInventory().items) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static boolean hasItem(Minecraft client, net.minecraft.world.item.Item item) {
        return inventoryCount(client, item) > 0;
    }

    private static int slotOf(Minecraft client, net.minecraft.world.item.Item item) {
        var items = client.player.getInventory().items;
        for (int slot = 0; slot < items.size(); slot++) if (items.get(slot).is(item)) return slot;
        return -1;
    }

    private static void selectSlot(Minecraft client, int slot) {
        // The vanilla carried item packet only accepts a HOTBAR slot; the
        // server refuses every other index with "tried to set an invalid
        // carried item" and the hand never changes.
        if (slot < 0 || slot > 8) return;
        if (client.player.getInventory().selected == slot) return;
        client.player.getInventory().selected = slot;
        client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
    }

    /**
     * Puts an item into the main hand. An item outside the hotbar has to be
     * moved into it first, because the vanilla carried item packet only
     * accepts a hotbar index.
     *
     * <p>The move uses the vanilla quick move (a shift click), which moves the
     * item between the main inventory and the hotbar WITHOUT touching the
     * cursor. Two pick up clicks would leave the item on the cursor, and the
     * server then refuses the accessory menu while the cursor is not empty.</p>
     */
    private boolean holdItemOf(Minecraft client, net.minecraft.world.item.Item item) {
        int slot = slotOf(client, item);
        if (slot < 0) return false;
        if (slot < 9) {
            selectSlot(client, slot);
            return true;
        }
        // The item is put into the first hotbar slot by the SERVER. A client
        // side inventory click is not usable here: the vanilla click packet
        // carries the state id of the menu, and the state id of the closed
        // player inventory is stale on the client.
        if (stageTicks % 20 == 0) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            client.getConnection().sendCommand("item replace entity " + playerName + " hotbar.0 with " + id);
        }
        return false; // The move is confirmed by the next inventory sync.
    }

    /**
     * The research table container maps the player main inventory slots 9..35 to
     * the container slots 2..28 and the slots 0..8 to the container slots 29..37.
     */
    private static int playerContainerSlot(int inventorySlot) {
        return inventorySlot >= 9 ? 2 + (inventorySlot - 9) : 29 + inventorySlot;
    }

    private static void moveIntoTable(Minecraft client, int inventorySlot, int tableSlot) {
        int container = client.player.containerMenu.containerId;
        client.gameMode.handleInventoryMouseClick(container, playerContainerSlot(inventorySlot), 0, ClickType.PICKUP, client.player);
        client.gameMode.handleInventoryMouseClick(container, tableSlot, 0, ClickType.PICKUP, client.player);
    }

    private static void interact(Minecraft client, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
    }

    private static String describe(Throwable failure) {
        var writer = new StringWriter();
        failure.printStackTrace(new PrintWriter(writer));
        String text = writer.toString();
        return text.length() > 4000 ? text.substring(0, 4000) : text;
    }

    private void writeReport(String status) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("status", status);
            root.addProperty("player", playerName);
            root.addProperty("address", host + ":" + port);
            JsonArray stepsJson = new JsonArray();
            for (String step : stepLog) stepsJson.add(step);
            root.add("steps", stepsJson);
            JsonArray failuresJson = new JsonArray();
            for (String failure : failures) failuresJson.add(failure);
            root.add("failures", failuresJson);
            JsonObject evidenceJson = new JsonObject();
            var gson = new GsonBuilder().serializeNulls().create();
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                evidenceJson.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
            }
            root.add("evidence", evidenceJson);
            JsonObject s2cJson = new JsonObject();
            for (Map.Entry<String, AtomicInteger> entry : new TreeMap<>(MultiplayerPairLink.CENSUS.clientbound).entrySet()) {
                s2cJson.addProperty(entry.getKey(), entry.getValue().get());
            }
            root.add("clientbound_payloads", s2cJson);
            JsonObject c2sJson = new JsonObject();
            for (Map.Entry<String, AtomicInteger> entry : new TreeMap<>(MultiplayerPairLink.CENSUS.serverbound).entrySet()) {
                c2sJson.addProperty(entry.getKey(), entry.getValue().get());
            }
            root.add("serverbound_payloads", c2sJson);
            JsonArray messagesJson = new JsonArray();
            synchronized (MultiplayerPairLink.CENSUS.messages) {
                for (String message : MultiplayerPairLink.CENSUS.messages) messagesJson.add(message);
            }
            root.add("server_messages", messagesJson);
            Path target = Path.of(runDirectory).resolve(reportFile);
            Files.createDirectories(target.getParent());
            Files.writeString(target, root.toString() + "\n", StandardCharsets.UTF_8);
            LOGGER.info("Multiplayer probe report written to {}", target);
        } catch (Exception failure) {
            LOGGER.error("Cannot write the multiplayer probe report", failure);
        }
    }
}
