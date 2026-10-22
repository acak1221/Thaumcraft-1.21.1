package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.container.ContainerAccessories;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.container.ContainerTravelingTrunk;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.AccessoryOpenPayload;

/**
 * The SECOND physical client of the two-player acceptance.
 *
 * <p>This process is a real Minecraft client in its own JVM: it has its own
 * Fabric payload registry, its own GL context and its own connection. It never
 * mocks a player and never calls a server handler. Everything it reports was
 * observed on its own socket.</p>
 *
 * <p>The LEAD client drives this process through the three files of
 * {@link MultiplayerPairLink}. The peer answers every command with the real
 * result it saw, so the lead can assert a two-player behaviour that a single
 * client can never see.</p>
 */
public final class MultiplayerPeerProbe implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("thaumcraft-multiplayer-peer");
    private static final int PENDING_TIMEOUT_TICKS = 200;

    private final MultiplayerPairLink.Census census = new MultiplayerPairLink.Census();
    private final Map<String, Object> evidence = new LinkedHashMap<>();
    private final List<String> stepLog = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    private String runDirectory = ".";
    private String reportFile = "multiplayer-peer-report.json";
    private String playerName = "MPProbe2";
    private String host = "127.0.0.1";
    private int port = 25565;
    private Path pairDirectory;

    private int handledSequence = -1;
    private int issuedSequence = 0;
    private String pendingToken;
    private String pendingArguments = "";
    private int pendingTicks;
    private boolean pendingClicked;
    private boolean finished;
    private boolean reported;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.multiplayerPeer")) return;
        if (Boolean.getBoolean("thaumcraft.clientSmoke")) {
            throw new IllegalStateException("The multiplayer peer never runs inside the single-player client smoke");
        }
        String directory = System.getProperty("thaumcraft.multiplayerRunDir", "");
        if (directory.isBlank()) throw new IllegalStateException("The multiplayer peer needs an owned run directory");
        runDirectory = directory;
        reportFile = System.getProperty("thaumcraft.multiplayerReport", "multiplayer-peer-report.json");
        playerName = System.getProperty("thaumcraft.multiplayerPlayer", playerName);
        String address = System.getProperty("thaumcraft.multiplayerAddress", "127.0.0.1:25565");
        int colon = address.lastIndexOf(':');
        host = address.substring(0, colon);
        port = Integer.parseInt(address.substring(colon + 1));
        String pair = System.getProperty("thaumcraft.multiplayerPairDir", "");
        if (pair.isBlank()) throw new IllegalStateException("The multiplayer peer needs the shared pair directory");
        pairDirectory = Path.of(pair);
        LOGGER.info("Multiplayer peer starting against {} as {}", address, playerName);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, packet) -> {
            census.install(handler.getConnection());
            LOGGER.info("Multiplayer peer joined the play phase; census installed");
        });
        ClientTickEvent.CLIENT_POST.register(this::tick);
    }

    // ------------------------------------------------------------------ driver

    private void tick(Minecraft client) {
        if (finished) return;
        if (client.player == null || client.level == null) {
            if (client.getConnection() != null) return;
            return;
        }
        if (!reported) {
            reported = true;
            evidence.put("player", client.player.getGameProfile().getName());
            evidence.put("uuid", client.player.getUUID().toString());
            evidence.put("address", host + ":" + port);
            stepLog.add("PASS connect");
            LOGGER.info("Multiplayer peer connected as {}", client.player.getGameProfile().getName());
        }
        writeStatus(client);
        if (pendingToken != null) {
            pendingTicks++;
            if (continuePending(client)) return;
            if (pendingTicks > PENDING_TIMEOUT_TICKS) {
                reply("TIMEOUT " + pendingToken);
                pendingToken = null;
            }
            return;
        }
        int sequence = MultiplayerPairLink.readSequence(MultiplayerPairLink.commandFile(pairDirectory));
        if (sequence < 0 || sequence <= handledSequence) return;
        String payload = MultiplayerPairLink.readPayload(MultiplayerPairLink.commandFile(pairDirectory));
        handledSequence = sequence;
        if (payload == null) return;
        int space = payload.indexOf(' ');
        String token = space < 0 ? payload : payload.substring(0, space);
        String arguments = space < 0 ? "" : payload.substring(space + 1).trim();
        LOGGER.info("Multiplayer peer received {}", token);
        pendingClicked = false;
        execute(client, token, arguments);
    }

    private void execute(Minecraft client, String token, String arguments) {
        switch (token) {
            case "HELLO", "REPORT" -> reply(token + " " + status(client));
            case "WAIT" -> reply("WAIT ok");
            case "OPEN_ACCESSORIES" -> {
                pendingToken = "ACCESSORIES";
                pendingArguments = "";
                pendingTicks = 0;
                NetworkManager.sendToServer(new AccessoryOpenPayload());
            }
            case "EQUIP_ACCESSORY" -> {
                pendingToken = "EQUIP";
                pendingArguments = "";
                pendingTicks = 0;
                equipAccessory(client);
            }
            case "BELL_GOLEM" -> {
                pendingToken = "BELL";
                pendingArguments = "";
                pendingTicks = 0;
                bellGolem(client);
            }
            case "OPEN_TRUNK" -> {
                pendingToken = "TRUNK";
                pendingArguments = "";
                pendingTicks = 0;
                openTrunk(client);
            }
            case "OPEN_TABLE" -> {
                pendingToken = "TABLE";
                pendingArguments = arguments;
                pendingTicks = 0;
                openTable(client, arguments);
            }
            case "GAMEMODE" -> {
                pendingToken = "GM";
                pendingArguments = arguments;
                pendingTicks = 0;
            }
            case "HAS_RESEARCH" -> {
                pendingToken = "HAS_RESEARCH";
                pendingArguments = arguments;
                pendingTicks = 0;
            }
            case "COUNT" -> {
                pendingToken = "COUNT";
                pendingArguments = arguments;
                pendingTicks = 0;
            }
            case "STOP" -> {
                reply("STOP ok");
                stepLog.add("PASS stop");
                writeReport("SCRIPT_PASSED");
                finished = true;
                client.stop();
            }
            default -> reply("ERR unknown=" + token);
        }
    }

    /** The peer's own view of the world, used for the two-player assertions. */
    private String status(Minecraft client) {
        var player = client.player;
        var knowledge = KnowledgeAccess.get(player);
        int accessories = 0;
        var inventory = PlayerAccessories.get(player);
        for (int slot = 0; slot < 4; slot++) if (!inventory.getItem(slot).isEmpty()) accessories++;
        int players = client.getConnection() == null ? 0 : client.getConnection().getOnlinePlayers().size();
        return "name=" + player.getGameProfile().getName()
                + " uuid=" + player.getUUID()
                + " x=" + player.getBlockX() + " y=" + player.getBlockY() + " z=" + player.getBlockZ()
                + " gm=" + (player.getAbilities().instabuild ? "creative" : "survival")
                + " warp=" + knowledge.getTotalWarp()
                + " research=" + knowledge.getResearchComplete().size()
                + " accessories=" + accessories
                + " players=" + players
                + " entities=" + entityCount(client);
    }

    private boolean continuePending(Minecraft client) {
        switch (pendingToken) {
            case "ACCESSORIES" -> {
                if (client.player.containerMenu instanceof ContainerAccessories) {
                    int count = accessoryCount(client);
                    reply("ACCESSORIES open=1 count=" + count);
                    pendingToken = null;
                    return true;
                }
                return false;
            }
            case "EQUIP" -> {
                if (!(client.player.containerMenu instanceof ContainerAccessories)) {
                    // The accessory menu is opened by a real server packet; the
                    // peer asks for it exactly like a player would.
                    if (pendingTicks % 20 == 0) NetworkManager.sendToServer(new AccessoryOpenPayload());
                    return false;
                }
                if (!pendingClicked) {
                    pendingClicked = true;
                    equipAccessory(client);
                    return false;
                }
                if (accessoryCount(client) > 0) {
                    reply("EQUIP count=" + accessoryCount(client));
                    pendingToken = null;
                    return true;
                }
                return false;
            }
            case "BELL" -> {
                if (bellSelected(client)) {
                    reply("BELL selected=1");
                    pendingToken = null;
                    return true;
                }
                return false;
            }
            case "TRUNK" -> {
                if (client.player.containerMenu instanceof ContainerTravelingTrunk) {
                    reply("TRUNK open=1");
                    pendingToken = null;
                    return true;
                }
                return false;
            }
            case "TABLE" -> {
                if (client.player.containerMenu instanceof ContainerResearchTable menu) {
                    var main = menu.mainPos();
                    reply("TABLE open=1 main=" + main.getX() + "," + main.getY() + "," + main.getZ());
                    pendingToken = null;
                    return true;
                }
                return false;
            }
            case "GM" -> {
                boolean creative = client.player.getAbilities().instabuild;
                if (creative == "creative".equals(pendingArguments)) {
                    reply("GM " + pendingArguments);
                    pendingToken = null;
                    return true;
                }
                return false;
            }
            case "HAS_RESEARCH" -> {
                boolean complete = KnowledgeAccess.get(client.player).isResearchComplete(pendingArguments);
                reply("HAS " + pendingArguments + " " + (complete ? "1" : "0"));
                pendingToken = null;
                return true;
            }
            case "COUNT" -> {
                int clientbound = census.clientbound(pendingArguments);
                int serverbound = census.serverbound(pendingArguments);
                reply("COUNT " + pendingArguments + " count=" + (clientbound + serverbound));
                pendingToken = null;
                return true;
            }
            default -> {
                pendingToken = null;
                return true;
            }
        }
    }

    // ------------------------------------------------------------------ actions

    /**
     * Moves the peer's own amulet into the first accessory slot. The amulet
     * belongs to the AMULET slot type, so the server accepts it.
     */
    private void equipAccessory(Minecraft client) {
        var player = client.player;
        int source = -1;
        var items = player.getInventory().items;
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof thaumcraft.api.baubles.IBauble) {
                source = slot;
                break;
            }
        }
        if (source < 0 || !(player.containerMenu instanceof ContainerAccessories)) {
            reply("EQUIP count=0");
            pendingToken = null;
            return;
        }
        int container = player.containerMenu.containerId;
        // The accessory container maps the hotbar 0..8 to the container slots 31..39.
        int containerSlot = source < 9 ? 31 + source : 4 + (source - 9);
        client.gameMode.handleInventoryMouseClick(container, containerSlot, 0, ClickType.PICKUP, player);
        client.gameMode.handleInventoryMouseClick(container, 0, 0, ClickType.PICKUP, player);
    }

    /** The peer's own bell on the peer's own golem. */
    private void bellGolem(Minecraft client) {
        var player = client.player;
        int bellSlot = -1;
        var items = player.getInventory().items;
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.get(slot).is(ThaumcraftGolems.GOLEM_BELL.get())) { bellSlot = slot; break; }
        }
        EntityGolemBase golem = closestGolem(client);
        if (bellSlot < 0 || golem == null) {
            reply("BELL selected=0");
            pendingToken = null;
            return;
        }
        if (player.getInventory().selected != bellSlot) {
            player.getInventory().selected = bellSlot;
            client.getConnection().send(new ServerboundSetCarriedItemPacket(bellSlot));
        }
        client.gameMode.interact(player, golem, InteractionHand.MAIN_HAND);
    }

    /** The peer's own travelling trunk. */
    private void openTrunk(Minecraft client) {
        var player = client.player;
        EntityTravelingTrunk trunk = closestTrunk(client);
        if (trunk == null) {
            reply("TRUNK open=0");
            pendingToken = null;
            return;
        }
        client.gameMode.interact(player, trunk, InteractionHand.MAIN_HAND);
    }

    /** The peer's own right click on the research table's PARTNER half. */
    private void openTable(Minecraft client, String arguments) {
        var player = client.player;
        String[] parts = arguments.split(",");
        if (parts.length != 3) {
            reply("TABLE open=0");
            pendingToken = null;
            return;
        }
        BlockPos pos = new BlockPos(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                Integer.parseInt(parts[2].trim()));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
    }

    /** The number of entities the peer can see. */
    private static int entityCount(Minecraft client) {
        int count = 0;
        for (var ignored : client.level.entitiesForRendering()) count++;
        return count;
    }

    private static int accessoryCount(Minecraft client) {        var inventory = PlayerAccessories.get(client.player);
        int count = 0;
        for (int slot = 0; slot < 4; slot++) if (!inventory.getItem(slot).isEmpty()) count++;
        return count;
    }

    /** The bell's own selection data, the exact thing the other player must not get. */
    private static boolean bellSelected(Minecraft client) {
        for (ItemStack stack : client.player.getInventory().items) {
            if (!stack.is(ThaumcraftGolems.GOLEM_BELL.get())) continue;
            var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (data == null) continue;
            if (data.copyTag().contains("thaumcraft:golem_bell")) return true;
        }
        return false;
    }

    private static EntityGolemBase closestGolem(Minecraft client) {
        EntityGolemBase best = null;
        double bestDistance = Double.MAX_VALUE;
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EntityGolemBase golem)) continue;
            double distance = golem.distanceToSqr(client.player);
            if (distance < bestDistance) { bestDistance = distance; best = golem; }
        }
        return best;
    }

    private static EntityTravelingTrunk closestTrunk(Minecraft client) {
        EntityTravelingTrunk best = null;
        double bestDistance = Double.MAX_VALUE;
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EntityTravelingTrunk trunk)) continue;
            double distance = trunk.distanceToSqr(client.player);
            if (distance < bestDistance) { bestDistance = distance; best = trunk; }
        }
        return best;
    }

    // ------------------------------------------------------------------ plumbing

    private void writeStatus(Minecraft client) {
        MultiplayerPairLink.writeAtomic(MultiplayerPairLink.statusFile(pairDirectory), status(client) + "\n");
    }

    private void reply(String text) {
        issuedSequence = handledSequence;
        MultiplayerPairLink.writeAtomic(MultiplayerPairLink.resultFile(pairDirectory),
                handledSequence + "\n" + text + "\n");
        LOGGER.info("Multiplayer peer answered {}", text);
    }

    private static String describe(Throwable failure) {
        var writer = new StringWriter();
        failure.printStackTrace(new PrintWriter(writer));
        String text = writer.toString();
        return text.length() > 4000 ? text.substring(0, 4000) : text;
    }

    /** The peer stops itself once the lead closed the acceptance. */
    public void stop(Minecraft client) {
        finished = true;
        writeReport("SCRIPT_PASSED");
        client.stop();
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
            JsonObject clientboundJson = new JsonObject();
            for (Map.Entry<String, java.util.concurrent.atomic.AtomicInteger> entry : new TreeMap<>(census.clientbound).entrySet()) {
                clientboundJson.addProperty(entry.getKey(), entry.getValue().get());
            }
            root.add("clientbound_payloads", clientboundJson);
            JsonObject serverboundJson = new JsonObject();
            for (Map.Entry<String, java.util.concurrent.atomic.AtomicInteger> entry : new TreeMap<>(census.serverbound).entrySet()) {
                serverboundJson.addProperty(entry.getKey(), entry.getValue().get());
            }
            root.add("serverbound_payloads", serverboundJson);
            JsonArray messagesJson = new JsonArray();
            synchronized (census.messages) {
                for (String message : census.messages) messagesJson.add(message);
            }
            root.add("server_messages", messagesJson);
            Path target = Path.of(runDirectory).resolve(reportFile);
            Files.createDirectories(target.getParent());
            Files.writeString(target, root.toString() + "\n", StandardCharsets.UTF_8);
            LOGGER.info("Multiplayer peer report written to {}", target);
        } catch (Exception failure) {
            LOGGER.error("Cannot write the multiplayer peer report", failure);
        }
    }
}
