package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import thaumcraft.common.Thaumcraft;

/** Sound identities are common/server data; audio and subtitles remain client resources. */
public final class ThaumcraftSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Thaumcraft.MODID, Registries.SOUND_EVENT);
    static {
        registerSound("heartbeat");
        registerSound("spill");
        registerSound("bubble");
        registerSound("alembicknock");
        registerSound("creak");
        registerSound("squeek");
        registerSound("golemironshoot");
        registerSound("jar");
        registerSound("swarm");
        registerSound("swarmattack");
        registerSound("fly");
        registerSound("key");
        registerSound("doorfail");
        registerSound("cameraticks");
        registerSound("cameraclack");
        registerSound("pump");
        registerSound("page");
        registerSound("learn");
        registerSound("write");
        registerSound("erase");
        registerSound("brain");
        registerSound("crystal");
        registerSound("wispdead");
        registerSound("wisplive");
        registerSound("wand");
        registerSound("wandfail");
        registerSound("rumble");
        registerSound("ice");
        registerSound("jacobs");
        registerSound("hhoff");
        registerSound("hhon");
        registerSound("pech_idle");
        registerSound("pech_trade");
        registerSound("pech_dice");
        registerSound("pech_hit");
        registerSound("pech_death");
        registerSound("pech_charge");
        registerSound("shock");
        registerSound("fireloop");
        registerSound("zap");
        registerSound("craftfail");
        registerSound("craftstart");
        registerSound("runicshieldeffect");
        registerSound("runicshieldcharge");
        registerSound("swing");
        registerSound("wind");
        registerSound("tool");
        registerSound("gore");
        registerSound("roots");
        registerSound("tentacle");
        registerSound("upgrade");
        registerSound("whispers");
        registerSound("monolith");
        registerSound("infuser");
        registerSound("infuserstart");
        registerSound("egidle");
        registerSound("egattack");
        registerSound("egdeath");
        registerSound("egscreech");
        registerSound("crabclaw");
        registerSound("crabdeath");
        registerSound("crabtalk");
        registerSound("chant");
        registerSound("coins");
        registerSound("urnbreak");
        registerSound("evilportal");
    }
    private ThaumcraftSounds() {}
    private static void registerSound(String name) {
        SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name)));
    }
    public static void register() { SOUNDS.register(); }
}
