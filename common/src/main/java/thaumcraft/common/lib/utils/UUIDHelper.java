package thaumcraft.common.lib.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Original TC4 {@code thaumcraft.common.lib.utils.UUIDHelper}: the player-name/UUID conversions of the original
 * server list and the arcane-scheduling tools.
 *
 * <p>The four local conversions ({@link #cleanUUID}, {@link #fullUUID}, {@link #stringToUUID} and
 * {@link #getOfflineUUID}) are pure and are covered by the unit tests. The two lookups talk to the Mojang session
 * service exactly as the original did; every failure is swallowed and reported as "not found".
 */
public final class UUIDHelper {
    private static final String SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final int TIMEOUT_MS = 5000;

    private UUIDHelper() {}

    /** Original {@code uuidToName}: the name of an online player, or an empty string when it cannot be resolved. */
    public static String uuidToName(String uuid) {
        if (uuid != null && !uuid.isEmpty()) {
            try {
                URL url = new URL(SESSION_URL + cleanUUID(uuid));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject profile = JsonParser.parseReader(reader).getAsJsonObject();
                    return profile.get("name").getAsString().trim();
                }
            } catch (Exception ignored) {
                // The original returned an empty string for every failure, including a missing profile.
            }
        }
        return "";
    }

    /** Original {@code getOnlineUUID}: the account UUID of a name, or null when it cannot be resolved. */
    public static UUID getOnlineUUID(String name) {
        if (name != null && !name.isEmpty()) {
            try {
                URL url = new URL(PROFILE_URL + name);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject profile = JsonParser.parseReader(reader).getAsJsonObject();
                    return UUID.fromString(fullUUID(profile.get("id").getAsString()));
                }
            } catch (Exception ignored) {
                // The original returned null for every failure.
            }
        }
        return null;
    }

    /** Original {@code getOfflineUUID}: the offline-mode UUID of a name. */
    public static UUID getOfflineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    /** Original {@code stringToUUID}: a parsed UUID, or null when the text is malformed. */
    public static UUID stringToUUID(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Original {@code fullUUID}: the hyphenated form of a bare 32 character UUID. */
    public static String fullUUID(String uuid) {
        String cleaned = cleanUUID(uuid);
        return cleaned.substring(0, 8) + "-" + cleaned.substring(8, 12) + "-" + cleaned.substring(12, 16) + "-"
                + cleaned.substring(16, 20) + "-" + cleaned.substring(20, 32);
    }

    /** Original {@code cleanUUID}: everything that is not a letter or a digit is dropped. */
    public static String cleanUUID(String uuid) {
        return uuid.replaceAll("[^a-zA-Z0-9]", "");
    }
}
