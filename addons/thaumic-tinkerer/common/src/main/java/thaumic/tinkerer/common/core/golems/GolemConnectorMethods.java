/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.core.golems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The methods the 1.7.10 Golem Connector offered to ComputerCraft and OpenComputers as the peripheral
 * {@code tt_golemconnector}: {@code getDecorations}, {@code getPosition}, {@code getType}, {@code getHealth},
 * {@code getCore}, {@code getHome}, {@code setHome}, {@code getMarkers}, {@code setMarkers}, {@code newMarker},
 * {@code addMarker}, {@code saveMarker}, {@code deleteMarker}, {@code getMarker}, {@code getMarkerCount}.
 *
 * <p>The arguments and results are plain Java values (numbers as {@link Double}, tables as {@link Map}), as the original
 * received them from the two computer mods, so a bridge to a computer mod only has to translate its own value types. No
 * computer mod can be reached from loader-neutral code, so nothing registers this as a peripheral yet (see
 * docs/deviations/TileBlocks.md). The behaviour, the answers for a connector without a golem and the error messages are
 * the original's.</p>
 */
public final class GolemConnectorMethods {

    /** The name of the peripheral. */
    public static final String TYPE = "tt_golemconnector";

    public static final String[] METHOD_NAMES = { "getDecorations", "getPosition", "getType", "getHealth", "getCore",
            "getHome", "setHome", "getMarkers", "setMarkers", "newMarker", "addMarker", "saveMarker", "deleteMarker",
            "getMarker", "getMarkerCount" };

    /** The error a method raises (1.7.10 {@code LuaException}). */
    public static final class MethodException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public MethodException(String message) {
            super(message);
        }
    }

    private final GolemAccess golem;
    private final BlockPos connectorPos;
    private final String dimension;

    /** @param dimension the dimension of the connector, for {@code newMarker} (its resource location) */
    public GolemConnectorMethods(GolemAccess golem, BlockPos connectorPos, String dimension) {
        this.golem = golem == null ? UnavailableGolemAccess.INSTANCE : golem;
        this.connectorPos = connectorPos;
        this.dimension = dimension;
    }

    private boolean noGolem() {
        return !golem.exists();
    }

    /** Calls the method by its name; the result is the array of return values of the original. */
    public Object[] call(String method, Object... arguments) {
        return switch (method) {
            case "getDecorations" -> getDecorations();
            case "getPosition" -> getPosition();
            case "getType" -> getType();
            case "getHealth" -> getHealth();
            case "getCore" -> getCore();
            case "getHome" -> getHome();
            case "setHome" -> {
                if (arguments.length != 4) throw new MethodException("Invalid arguments");
                yield setHome(number(arguments[0]), number(arguments[1]), number(arguments[2]), number(arguments[3]));
            }
            case "getMarkers" -> getMarkers();
            case "setMarkers" -> {
                if (arguments.length != 1) throw new MethodException("setMarkers takes 1 argument");
                yield setMarkers((Map<?, ?>) arguments[0]);
            }
            case "newMarker" -> newMarker();
            case "addMarker" -> {
                if (arguments.length != 1) throw new MethodException("addMarker must have 1 argument");
                yield addMarker((Map<?, ?>) arguments[0]);
            }
            case "saveMarker" -> {
                if (arguments.length != 2) throw new MethodException("saveMarker must have 2 arguments");
                yield saveMarker(number(arguments[0]), (Map<?, ?>) arguments[1]);
            }
            case "deleteMarker" -> {
                if (arguments.length != 1) throw new MethodException("deleteMarker must have 1 argument");
                yield deleteMarker(number(arguments[0]));
            }
            case "getMarker" -> {
                if (arguments.length != 1) throw new MethodException("getMarker must have 1 argument");
                yield getMarker(number(arguments[0]));
            }
            case "getMarkerCount" -> getMarkerCount();
            default -> null;
        };
    }

    private static double number(Object value) {
        return ((Number) value).doubleValue();
    }

    public Object[] getDecorations() {
        String decoration = golem.decoration();
        if (noGolem() || decoration == null || decoration.isEmpty()) return new String[] {};
        Map<Double, String> decorations = new HashMap<>();
        for (int i = 0; i < decoration.length(); i++) {
            EnumGolemDecorations golemDec = EnumGolemDecorations.getFromChar(decoration.charAt(i));
            if (golemDec == null) throw new MethodException("Golem is wearing unknown accessory: " + decoration.charAt(i));
            decorations.put((double) i, translate(golemDec.getName()));
        }
        return new Object[] { decorations };
    }

    public Object[] getPosition() {
        if (noGolem()) return new Integer[] {};
        return new Integer[] { (int) golem.x(), (int) golem.y(), (int) golem.z() };
    }

    public Object[] getType() {
        if (noGolem()) return new String[] {};
        String name = golem.typeName();
        if (name == null) throw new MethodException("Unknown Golem Type: " + name);
        return new String[] { name };
    }

    public Object[] getHealth() {
        if (noGolem()) return new Float[] {};
        return new Float[] { golem.health() };
    }

    public Object[] getCore() {
        if (noGolem()) return new String[] {};
        if (golem.core() == -1) return new String[] { translate("item.ItemGolemCore.100.name") };
        EnumGolemCores core = EnumGolemCores.getFromByte((byte) golem.core());
        if (core == null) throw new MethodException("Golem has Unknown core: " + golem.core());
        return new String[] { translate(core.getName()) };
    }

    public Object[] getHome() {
        if (noGolem()) return new String[] {};
        BlockPos home = golem.home();
        return new Integer[] { home.getX(), home.getY(), home.getZ(), golem.homeFacing() };
    }

    public Object[] setHome(double x, double y, double z, double facing) {
        if (noGolem()) return new String[] {};
        golem.setHome(new BlockPos((int) x, (int) y, (int) z), (int) facing);
        return getHome();
    }

    public Object[] getMarkers() {
        if (noGolem()) return new String[] {};
        Map<Integer, Map<String, Object>> luaMarkers = new HashMap<>();
        int i = 1;
        for (GolemAccess.Marker mark : golem.markers()) {
            luaMarkers.put(i++, fromMarker(mark));
        }
        return new Object[] { luaMarkers };
    }

    public Object[] setMarkers(Map<?, ?> arguments) {
        if (noGolem()) return new String[] {};
        List<GolemAccess.Marker> list = new ArrayList<>();
        for (Object map : arguments.values()) {
            list.add(toMarker((Map<?, ?>) map));
        }
        golem.setMarkers(list);
        return getMarkers();
    }

    /** A blank marker with sensible defaults: the connector's own position, any colour, the top face. */
    public Object[] newMarker() {
        Map<String, Object> mark = new HashMap<>();
        mark.put("posX", connectorPos.getX());
        mark.put("posY", connectorPos.getY());
        mark.put("posZ", connectorPos.getZ());
        mark.put("dim", dimension);
        mark.put("color", -1);
        mark.put("side", 1);
        return new Object[] { mark };
    }

    public Object[] addMarker(Map<?, ?> arguments) {
        if (noGolem()) return new String[] {};
        List<GolemAccess.Marker> markers = new ArrayList<>(golem.markers());
        markers.add(toMarker(arguments));
        golem.setMarkers(markers);
        return getMarker((double) (markers.size() - 1));
    }

    public Object[] saveMarker(double markerNum, Map<?, ?> markerArg) {
        if (noGolem()) return new String[] {};
        List<GolemAccess.Marker> markers = new ArrayList<>(golem.markers());
        if (markers.size() <= markerNum) throw new MethodException("marker " + (int) markerNum + " does not exist");
        markers.set((int) markerNum, toMarker(markerArg));
        golem.setMarkers(markers);
        return getMarker(markerNum);
    }

    public Object[] deleteMarker(double markerNum) {
        if (noGolem()) return new String[] {};
        List<GolemAccess.Marker> markers = new ArrayList<>(golem.markers());
        if (markers.size() <= markerNum) throw new MethodException("marker " + (int) markerNum + " does not exist");
        markers.remove((int) markerNum);
        golem.setMarkers(markers);
        return new String[] {};
    }

    public Object[] getMarker(double markerNum) {
        if (noGolem()) return new String[] {};
        List<GolemAccess.Marker> markers = golem.markers();
        if (markers.size() <= markerNum) throw new MethodException("marker " + (int) markerNum + " does not exist");
        return new Object[] { fromMarker(markers.get((int) markerNum)) };
    }

    public Object[] getMarkerCount() {
        if (noGolem()) return new String[] {};
        return new Object[] { (double) golem.markers().size() };
    }

    /** {@code toMarkerImplementation}: the table of a script to a marker. */
    static GolemAccess.Marker toMarker(Map<?, ?> markerMap) {
        double posX = number(markerMap.get("posX"));
        double posY = number(markerMap.get("posY"));
        double posZ = number(markerMap.get("posZ"));
        Object dim = markerMap.get("dim");
        double side = number(markerMap.get("side"));
        double color = number(markerMap.get("color"));
        return new GolemAccess.Marker((int) posX, (int) posY, (int) posZ, String.valueOf(dim), (byte) side, (byte) color);
    }

    /** {@code fromMarkerImplementation}: a marker as a table. */
    static Map<String, Object> fromMarker(GolemAccess.Marker mark) {
        Map<String, Object> luaMarker = new HashMap<>();
        luaMarker.put("posX", mark.x());
        luaMarker.put("posY", mark.y());
        luaMarker.put("posZ", mark.z());
        luaMarker.put("dim", mark.dimension());
        luaMarker.put("color", mark.color());
        luaMarker.put("side", mark.side());
        return luaMarker;
    }

    private static String translate(String key) {
        return Component.translatable(key).getString();
    }
}
