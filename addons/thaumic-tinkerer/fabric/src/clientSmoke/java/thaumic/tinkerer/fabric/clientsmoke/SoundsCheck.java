package thaumic.tinkerer.fabric.clientsmoke;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * {@code sounds.json} and the sound events of the add-on: every registered sound event has a definition, and every sound a
 * definition names has its {@code .ogg} file. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like
 * the add-on, not distributed.
 *
 * <p>Sound is not played on this machine (no audio device); only the files are looked at.</p>
 */
final class SoundsCheck implements SmokeCheck {
    @Override
    public String name() {
        return "sounds";
    }

    @Override
    public void run(SmokeRun run) {
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        ResourceLocation soundsFile = ResourceLocation.fromNamespaceAndPath(LibMisc.MOD_ID, "sounds.json");
        List<String> events = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.SOUND_EVENT.keySet()) {
            if (TinkererContent.ours(id)) events.add(id.getPath());
        }
        Optional<JsonObject> json = files.readJson(soundsFile);
        if (json.isEmpty()) {
            if (events.isEmpty()) {
                run.report().info(PHASE, name(), "the add-on registers no sound events and ships no sounds.json: nothing to check");
            } else {
                run.report().fail(PHASE, "missing_sound_definition", "Unassigned", "sounds.json", soundsFile + " does not exist but the add-on registers sound events " + events);
            }
            return;
        }
        int sounds = 0;
        for (String event : events) {
            if (!json.get().has(event)) {
                run.report().fail(PHASE, "missing_sound_definition", run.content().groups().groupOf(event), "sound:" + event,
                        "registered sound event has no entry in sounds.json");
            }
        }
        for (Map.Entry<String, JsonElement> entry : json.get().entrySet()) {
            JsonElement list = entry.getValue().getAsJsonObject().get("sounds");
            if (list == null) continue;
            for (JsonElement sound : list.getAsJsonArray()) {
                String name = sound.isJsonObject() ? sound.getAsJsonObject().get("name").getAsString() : sound.getAsString();
                ResourceLocation id = ResourceLocation.parse(name);
                sounds++;
                if (!files.exists(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "sounds/" + id.getPath() + ".ogg"))) {
                    run.report().fail(PHASE, "missing_sound", run.content().groups().groupOf(entry.getKey()), "sound:" + entry.getKey(),
                            "sounds.json names " + id + " but sounds/" + id.getPath() + ".ogg does not exist");
                }
            }
        }
        run.report().info(PHASE, name(), events.size() + " sound events, " + sounds + " sound files named by sounds.json");
    }
}
