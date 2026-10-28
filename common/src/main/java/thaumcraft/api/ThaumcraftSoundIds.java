package thaumcraft.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * The ids of the sounds Thaumcraft registers, so an add-on that plays "its own" wand, zap or wind sound does not write
 * the strings.
 *
 * <p>1.7.10 add-ons played them by name, {@code "thaumcraft:wand"}, {@code "thaumcraft:zap"},
 * {@code "thaumcraft:wind"}..., in {@code World.playSoundAtEntity}. In 1.21.1 each of them is a registered
 * {@link SoundEvent} with the same path, declared in the mod's {@code sounds.json}. There is one constant for every
 * sound event Thaumcraft registers, in the order it registers them, named after the path in capitals; the id never
 * changes, a resource pack may replace the audio behind it. {@link #event} resolves an id to the registered event,
 * which {@code Level.playSound} and {@code Level.playLocalSound} take.</p>
 *
 * <pre>{@code
 * level.playSound(null, player.blockPosition(), ThaumcraftSoundIds.event(ThaumcraftSoundIds.WAND),
 *         SoundSource.PLAYERS, 1.0F, 1.0F);   // server thread: every player in range hears it
 * }</pre>
 */
public final class ThaumcraftSoundIds {
    public static final ResourceLocation HEARTBEAT = id("heartbeat");
    public static final ResourceLocation SPILL = id("spill");
    public static final ResourceLocation BUBBLE = id("bubble");
    public static final ResourceLocation ALEMBICKNOCK = id("alembicknock");
    public static final ResourceLocation CREAK = id("creak");
    public static final ResourceLocation SQUEEK = id("squeek");
    public static final ResourceLocation GOLEMIRONSHOOT = id("golemironshoot");
    public static final ResourceLocation JAR = id("jar");
    public static final ResourceLocation SWARM = id("swarm");
    public static final ResourceLocation SWARMATTACK = id("swarmattack");
    public static final ResourceLocation FLY = id("fly");
    public static final ResourceLocation KEY = id("key");
    public static final ResourceLocation DOORFAIL = id("doorfail");
    public static final ResourceLocation CAMERATICKS = id("cameraticks");
    public static final ResourceLocation CAMERACLACK = id("cameraclack");
    public static final ResourceLocation PUMP = id("pump");
    public static final ResourceLocation PAGE = id("page");
    public static final ResourceLocation LEARN = id("learn");
    public static final ResourceLocation WRITE = id("write");
    public static final ResourceLocation ERASE = id("erase");
    public static final ResourceLocation BRAIN = id("brain");
    public static final ResourceLocation CRYSTAL = id("crystal");
    public static final ResourceLocation WISPDEAD = id("wispdead");
    public static final ResourceLocation WISPLIVE = id("wisplive");
    public static final ResourceLocation WAND = id("wand");
    public static final ResourceLocation WANDFAIL = id("wandfail");
    public static final ResourceLocation RUMBLE = id("rumble");
    public static final ResourceLocation ICE = id("ice");
    public static final ResourceLocation JACOBS = id("jacobs");
    public static final ResourceLocation HHOFF = id("hhoff");
    public static final ResourceLocation HHON = id("hhon");
    public static final ResourceLocation PECH_IDLE = id("pech_idle");
    public static final ResourceLocation PECH_TRADE = id("pech_trade");
    public static final ResourceLocation PECH_DICE = id("pech_dice");
    public static final ResourceLocation PECH_HIT = id("pech_hit");
    public static final ResourceLocation PECH_DEATH = id("pech_death");
    public static final ResourceLocation PECH_CHARGE = id("pech_charge");
    public static final ResourceLocation SHOCK = id("shock");
    public static final ResourceLocation FIRELOOP = id("fireloop");
    public static final ResourceLocation ZAP = id("zap");
    public static final ResourceLocation CRAFTFAIL = id("craftfail");
    public static final ResourceLocation CRAFTSTART = id("craftstart");
    public static final ResourceLocation RUNICSHIELDEFFECT = id("runicshieldeffect");
    public static final ResourceLocation RUNICSHIELDCHARGE = id("runicshieldcharge");
    public static final ResourceLocation SWING = id("swing");
    public static final ResourceLocation WIND = id("wind");
    public static final ResourceLocation TOOL = id("tool");
    public static final ResourceLocation GORE = id("gore");
    public static final ResourceLocation ROOTS = id("roots");
    public static final ResourceLocation TENTACLE = id("tentacle");
    public static final ResourceLocation UPGRADE = id("upgrade");
    public static final ResourceLocation WHISPERS = id("whispers");
    public static final ResourceLocation MONOLITH = id("monolith");
    public static final ResourceLocation INFUSER = id("infuser");
    public static final ResourceLocation INFUSERSTART = id("infuserstart");
    public static final ResourceLocation EGIDLE = id("egidle");
    public static final ResourceLocation EGATTACK = id("egattack");
    public static final ResourceLocation EGDEATH = id("egdeath");
    public static final ResourceLocation EGSCREECH = id("egscreech");
    public static final ResourceLocation CRABCLAW = id("crabclaw");
    public static final ResourceLocation CRABDEATH = id("crabdeath");
    public static final ResourceLocation CRABTALK = id("crabtalk");
    public static final ResourceLocation CHANT = id("chant");
    public static final ResourceLocation COINS = id("coins");
    public static final ResourceLocation URNBREAK = id("urnbreak");
    public static final ResourceLocation EVILPORTAL = id("evilportal");

    private ThaumcraftSoundIds() {}

    /**
     * The registered sound event of an id.
     *
     * @param id one of the constants of this class, or the id of any other registered sound event
     * @return the registered event, never {@code null}
     * @throws IllegalArgumentException when {@code id} is {@code null} or no sound event is registered under it
     *                                  (an id that does not exist, or Thaumcraft's registration has not run yet)
     */
    public static SoundEvent event(ResourceLocation id) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(id);
        if (event == null) throw new IllegalArgumentException("id: no sound event is registered as " + id);
        return event;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }
}
