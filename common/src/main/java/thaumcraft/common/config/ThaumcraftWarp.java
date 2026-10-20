package thaumcraft.common.config;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.EntityMindSpider;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.network.WarpEventNetworking;
import thaumcraft.common.lib.potions.*;

/** Native registry roots for the original WarpEvents dependencies. */
public final class ThaumcraftWarp {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Thaumcraft.MODID, Registries.MOB_EFFECT);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    public static final RegistrySupplier<MobEffect> THAUMARHIA = EFFECTS.register("thaumarhia", PotionThaumarhia::new);
    public static final RegistrySupplier<MobEffect> UNNATURAL_HUNGER = EFFECTS.register("unnatural_hunger", PotionUnnaturalHunger::new);
    public static final RegistrySupplier<MobEffect> SUN_SCORNED = EFFECTS.register("sun_scorned", PotionSunScorned::new);
    public static final RegistrySupplier<MobEffect> DEATH_GAZE = EFFECTS.register("death_gaze", PotionDeathGaze::new);
    public static final RegistrySupplier<MobEffect> BLURRED_VISION = EFFECTS.register("blurred_vision", PotionBlurredVision::new);
    public static final RegistrySupplier<MobEffect> WARP_WARD = EFFECTS.register("warp_ward", PotionWarpWard::new);
    public static final RegistrySupplier<EntityType<EntityMindSpider>> MIND_SPIDER = ENTITIES.register("mind_spider", () ->
            EntityType.Builder.<EntityMindSpider>of(EntityMindSpider::new, MobCategory.MONSTER).sized(.3F, .3F).clientTrackingRange(4).build("thaumcraft:mind_spider"));
    public static final RegistrySupplier<EntityType<EntityEldritchGuardian>> ELDRITCH_GUARDIAN = ENTITIES.register("eldritch_guardian", () ->
            EntityType.Builder.<EntityEldritchGuardian>of(EntityEldritchGuardian::new, MobCategory.MONSTER).sized(.8F, 2.25F).eyeHeight(2.1F).clientTrackingRange(5).build("thaumcraft:eldritch_guardian"));
    public static final RegistrySupplier<EntityType<EntityEldritchOrb>> ELDRITCH_ORB = ENTITIES.register("eldritch_orb", () ->
            EntityType.Builder.<EntityEldritchOrb>of(EntityEldritchOrb::new, MobCategory.MISC).sized(.25F, .25F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:eldritch_orb"));
    /** Original ItemEldritchObject metadata zero; its obelisk interaction belongs to the later obelisk unit. */
    public static final RegistrySupplier<Item> ELDRITCH_EYE = ITEMS.register("eldritch_eye", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)) {
        @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
            super.appendHoverText(stack, context, lines, flag);
            lines.add(Component.translatable("item.ItemEldritchObject.text.1").withStyle(ChatFormatting.DARK_PURPLE));
        }
    });
    private static boolean registered;
    private static boolean eventsEnabled;
    private ThaumcraftWarp() {}
    public static Holder<MobEffect> effect(RegistrySupplier<MobEffect> effect) { return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get()); }
    public static void register() {
        if (registered) return;
        registered = true;
        EFFECTS.register(); ENTITIES.register(); ITEMS.register();
        dev.architectury.registry.CreativeTabRegistry.append(ThaumcraftContent.TAB, ELDRITCH_EYE);
        WarpEventNetworking.initServer();
    }
    /** Enabled separately after the complete event dependency group passes validation. */
    public static void enableEvents() {
        if (eventsEnabled) return;
        eventsEnabled = true;
        TickEvent.PLAYER_POST.register(player -> { if (player instanceof ServerPlayer server) WarpEvents.playerTick(server); });
    }
}
