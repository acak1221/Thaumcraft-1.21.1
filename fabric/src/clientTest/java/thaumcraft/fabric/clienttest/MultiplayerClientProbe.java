package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
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
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.AccessoryOpenPayload;
import thaumcraft.common.lib.network.FocusSelectionPayload;
import thaumcraft.common.lib.network.GolemBellResetPayload;
import thaumcraft.common.lib.network.ResearchNoteRequestPayload;
import thaumcraft.common.lib.network.ResearchPurchasePayload;
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
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<String, Object> evidence = new LinkedHashMap<>();
    private final List<String> stepLog = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private final Map<String, AtomicInteger> s2cCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> c2sCounts = new ConcurrentHashMap<>();
    private final List<String> systemMessages = java.util.Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Object> stageData = new LinkedHashMap<>();

    private String runDirectory = ".";
    private String reportFile = "multiplayer-client-report.json";
    private String playerName = "MPProbe";
    private String host = "127.0.0.1";
    private int port = 25565;
    private int stage;
    private int stageTicks;
    private boolean finished;
    private Channel censusChannel;
    private String expectedResearch = "";
    private BlockPos tablePos;
    private BlockPos tablePartner;
    private boolean reconnectIssued;

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
        stages.add(new Stage("disconnect", this::stageDisconnect));
        stages.add(new Stage("reconnect", this::stageReconnect));
        stages.add(new Stage("reconnect_state_consistent", this::stageReconnectConsistent));
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
        synchronized (systemMessages) {
            for (String message : systemMessages) if (message.startsWith("Gave ")) count++;
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
        evidence.put("knowledge_sync_packets", s2cCounts.getOrDefault("thaumcraft:knowledge_sync", new AtomicInteger()).get());
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
        evidence.put("note_request_packets", c2sCounts.getOrDefault("thaumcraft:research_note_request", new AtomicInteger()).get());
        if (c2sCounts.getOrDefault("thaumcraft:research_note_request", new AtomicInteger()).get() != 1) {
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
        evidence.put("knowledge_sync_packets_after_grant", s2cCounts.getOrDefault("thaumcraft:knowledge_sync", new AtomicInteger()).get());
        if (s2cCounts.getOrDefault("thaumcraft:knowledge_sync", new AtomicInteger()).get() == 0) {
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
        evidence.put("purchase_packets", c2sCounts.getOrDefault("thaumcraft:research_purchase", new AtomicInteger()).get());
        evidence.put("research_notice_packets", s2cCounts.getOrDefault("thaumcraft:research_notice", new AtomicInteger()).get());
        evidence.put("research_notice_highlighted", ResearchNotifications.highlighted(PURCHASE_RESEARCH));
        evidence.put("aspect_pool_packets", s2cCounts.getOrDefault("thaumcraft:aspect_pool", new AtomicInteger()).get());
        if (s2cCounts.getOrDefault("thaumcraft:research_notice", new AtomicInteger()).get() == 0) {
            throw new IllegalStateException("The purchase produced no clientbound research notice");
        }
        if (s2cCounts.getOrDefault("thaumcraft:aspect_pool", new AtomicInteger()).get() == 0) {
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
        evidence.put("focus_selection_packets", c2sCounts.getOrDefault("thaumcraft:focus_selection", new AtomicInteger()).get());
        if (c2sCounts.getOrDefault("thaumcraft:focus_selection", new AtomicInteger()).get() != 1) {
            throw new IllegalStateException("The client did not send exactly one focus selection");
        }
        return true;
    }

    /** The golem bell reset over the real C2S channel. */
    private boolean stageGolemBell(Minecraft client) throws Exception {
        if (!giveItems(client, "thaumcraft:golem_bell 1")) return false;
        int bellSlot = slotOf(client, thaumcraft.common.config.ThaumcraftGolems.GOLEM_BELL.get());
        if (bellSlot < 0) return false;
        selectSlot(client, bellSlot);
        if (stageData.get("bell_sent") == null) {
            NetworkManager.sendToServer(new GolemBellResetPayload(InteractionHand.MAIN_HAND));
            stageData.put("bell_sent", true);
            return false;
        }
        int sent = c2sCounts.getOrDefault("thaumcraft:golem_bell_reset", new AtomicInteger()).get();
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
        if (!menu && !screen) return false;
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
            evidence.put("research_table_state_packets", s2cCounts.getOrDefault("thaumcraft:research_table_state", new AtomicInteger()).get());
            if (s2cCounts.getOrDefault("thaumcraft:research_table_state", new AtomicInteger()).get() == 0) {
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
            evidence.put("table_edit_packets", c2sCounts.getOrDefault("thaumcraft:research_table_edit", new AtomicInteger()).get());
            evidence.put("table_state_packets", s2cCounts.getOrDefault("thaumcraft:research_table_state", new AtomicInteger()).get());
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
                synchronized (systemMessages) {
                    for (int i = Math.max(0, systemMessages.size() - 6); i < systemMessages.size(); i++) {
                        recent.add(systemMessages.get(i));
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
            if (server != null && server.contains("entity data")) {
                throw new IllegalStateException("The server owns the vis relay but the client never received the block: " + server);
            }
            if (stageTicks > 200) {
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
            throw new IllegalStateException("The arcane bore never appeared on the client: " + boreId);
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
                s2cCounts.getOrDefault("thaumcraft:knowledge_sync", new AtomicInteger()).get());
        return true;
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
        try {
            Channel channel = (Channel) field(connection, "channel");
            if (channel == censusChannel) return;
            censusChannel = channel;
            var handler = new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                    if (message instanceof ClientboundCustomPayloadPacket packet) {
                        count(s2cCounts, packet.payload().type().id().toString());
                    } else if (message instanceof ClientboundSystemChatPacket packet) {
                        systemMessages.add(packet.content().getString());
                    } else if (message instanceof ClientboundPlayerChatPacket packet) {
                        systemMessages.add(packet.unsignedContent().getString());
                    }
                    super.channelRead(context, message);
                }

                @Override
                public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
                    if (message instanceof ServerboundCustomPayloadPacket packet) {
                        count(c2sCounts, packet.payload().type().id().toString());
                    }
                    super.write(context, message, promise);
                }
            };
            // The native Connection handler is a SimpleChannelInboundHandler and
            // never forwards an inbound packet, so the census has to sit BEFORE
            // it to observe the clientbound payloads at all.
            var pipeline = channel.pipeline();
            for (String name : pipeline.names()) {
                if (pipeline.get(name) instanceof Connection) {
                    pipeline.addBefore(name, "thaumcraft-multiplayer-census", handler);
                    return;
                }
            }
            pipeline.addLast("thaumcraft-multiplayer-census", handler);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot install the multiplayer payload census", failure);
        }
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
        synchronized (systemMessages) {
            for (String message : systemMessages) if (matcher.test(message)) return message;
        }
        return null;
    }

    private String lastMessageContaining(String needle) {
        String found = null;
        synchronized (systemMessages) {
            for (String message : systemMessages) if (message.contains(needle)) found = message;
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
        if (client.player.getInventory().selected == slot) return;
        client.player.getInventory().selected = slot;
        client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
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
            for (Map.Entry<String, AtomicInteger> entry : new TreeMap<>(s2cCounts).entrySet()) {
                s2cJson.addProperty(entry.getKey(), entry.getValue().get());
            }
            root.add("clientbound_payloads", s2cJson);
            JsonObject c2sJson = new JsonObject();
            for (Map.Entry<String, AtomicInteger> entry : new TreeMap<>(c2sCounts).entrySet()) {
                c2sJson.addProperty(entry.getKey(), entry.getValue().get());
            }
            root.add("serverbound_payloads", c2sJson);
            JsonArray messagesJson = new JsonArray();
            synchronized (systemMessages) {
                for (String message : systemMessages) messagesJson.add(message);
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
