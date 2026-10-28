/**
 * Created by pixlepix on 4/19/14.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.potion;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftFX;
import thaumic.tinkerer.common.block.BlockGas;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * What the four imbued effects do (1.7.10 {@code PotionEffectHandlerServer}):
 * <ul>
 * <li>Air: whoever the player hits is flung about for a second (a random velocity every five ticks);</li>
 * <li>Fire: whoever the player hits is set on fire (six seconds, renewed every five ticks) for five minutes;</li>
 * <li>Earth: a hit raises a wall of five by five forcefield blocks half way between the player and the victim;</li>
 * <li>Water: every lava block within two blocks of the player becomes obsidian.</li>
 * </ul>
 *
 * <p>1.7.10 had a client twin ({@code PotionEffectHandlerClient}) that drew the particles on the client's own picture
 * of the same events. A 1.21.1 client does not see the attack of a player (damage is the server's), so the particles are
 * sent by the server instead: bursts for air, the sphere of fire, Thaumcraft's sparkles over the earth wall and a puff
 * where lava turns to obsidian. The victims are remembered by their UUID with the dimension and the game time of the
 * hit (1.7.10 held the entities in a static map); the tables are dropped when the server stops.</p>
 */
public final class PotionEffectHandler {
    private record Hit(ResourceKey<Level> dimension, long time) {}

    private static final Map<UUID, Hit> AIR_HITS = new HashMap<>();
    private static final Map<UUID, Hit> FIRE_HITS = new HashMap<>();
    private static boolean registered;

    private PotionEffectHandler() {}

    /** Hooks the handler into the events of Architectury. Once. */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        EntityEvent.LIVING_HURT.register(PotionEffectHandler::onLivingHurt);
        TickEvent.SERVER_POST.register(PotionEffectHandler::onTickServer);
        TickEvent.PLAYER_POST.register(PotionEffectHandler::onPlayerTick);
        LifecycleEvent.SERVER_STOPPED.register(server -> clear());
    }

    /** Forgets every victim (the server stopped). */
    public static void clear() {
        AIR_HITS.clear();
        FIRE_HITS.clear();
    }

    /** How many victims are remembered (for the tests). */
    public static int airHits() {
        return AIR_HITS.size();
    }

    public static int fireHits() {
        return FIRE_HITS.size();
    }

    // ---- the hit ---------------------------------------------------------------------------------------------

    private static EventResult onLivingHurt(LivingEntity victim, DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof Player p && !p.level().isClientSide) {
            onPlayerHit(p, victim);
        }
        return EventResult.pass();
    }

    /** 1.7.10 {@code onLivingHurt}, for a player who hit a victim on the server. */
    public static void onPlayerHit(Player p, Entity victim) {
        Level world = victim.level();
        if (world.isClientSide) return;
        if (p.hasEffect(ModPotions.air())) {
            AIR_HITS.put(victim.getUUID(), new Hit(world.dimension(), world.getGameTime()));
        }
        if (p.hasEffect(ModPotions.fire())) {
            FIRE_HITS.put(victim.getUUID(), new Hit(world.dimension(), world.getGameTime()));
        }
        if (p.hasEffect(ModPotions.earth())) {
            raiseWall(p, victim, world);
        }
    }

    private static void raiseWall(Player p, Entity victim, Level world) {
        boolean xAxis = Math.abs(victim.getZ() - p.getZ()) < Math.abs(victim.getX() - p.getX());
        int centerX = Mth.floor((victim.getX() + p.getX()) / 2);

        int centerY = Mth.floor(p.getY() + 2);
        int centerZ = Mth.floor((victim.getZ() + p.getZ()) / 2);

        for (int i = -2; i < 3; i++) {
            for (int j = -2; j < 3; j++) {
                BlockPos pos = xAxis ? new BlockPos(centerX, centerY + i, centerZ + j) : new BlockPos(centerX + j, centerY + i, centerZ);
                BlockState there = world.getBlockState(pos);
                if (BlockGas.isAirLike(there)) {
                    world.setBlock(pos, TTRegistry.block(LibBlockNames.FORCEFIELD).defaultBlockState(), 3);
                    if (world instanceof ServerLevel server) {
                        ThaumcraftFX.blockSparkle(server, pos, ThaumcraftFX.RANDOM_GREY);
                    }
                }
            }
        }
    }

    // ---- water ----------------------------------------------------------------------------------------------

    private static void onPlayerTick(Player player) {
        if (player.level().isClientSide || !(player.level() instanceof ServerLevel world)) return;
        if (!player.hasEffect(ModPotions.water())) return;
        for (int x = Mth.floor(player.getX() - 2); x < player.getX() + 2; x++) {
            for (int y = Mth.floor(player.getY() - 2); y < player.getY() + 2; y++) {
                for (int z = Mth.floor(player.getZ() - 2); z < player.getZ() + 2; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockState(pos).is(Blocks.LAVA)) {
                        world.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                        world.sendParticles(ParticleTypes.POOF, x + .5, y + .5, z + .5, 3, 0.25, 0.25, 0.25, 0.01);
                    }
                }
            }
        }
    }

    // ---- air and fire ---------------------------------------------------------------------------------------

    private static void onTickServer(MinecraftServer server) {
        // Air potion
        tickHits(server, AIR_HITS, 20, (target, level) -> {
            Random rand = new Random();
            target.setDeltaMovement(rand.nextFloat() - .5, rand.nextFloat(), rand.nextFloat() - .5);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY(), target.getZ(), 4, 0.25, 0.25, 0.25, 0.02);
        });

        // Fire Potion
        tickHits(server, FIRE_HITS, 6000, (target, level) -> {
            target.igniteForSeconds(6);
            Random rand = new Random();

            for (int i = 0; i < 30; i++) {
                double theta = rand.nextFloat() * 2 * Math.PI;

                double phi = rand.nextFloat() * 2 * Math.PI;
                double r = 2.5;

                double x = r * Math.sin(theta) * Math.cos(phi);

                double y = r * Math.sin(theta) * Math.sin(phi);

                double z = r * Math.cos(theta);

                level.sendParticles(ParticleTypes.FLAME, target.getX() + x, target.getY() + y + 1, target.getZ() + z, 1, 0.0, 0.0, 0.0,
                        0.0);
            }
        });
    }

    private static void tickHits(MinecraftServer server, Map<UUID, Hit> hits, long lifetime, BiConsumer<Entity, ServerLevel> action) {
        Iterator<Map.Entry<UUID, Hit>> iter = hits.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Hit> entry = iter.next();
            Hit hit = entry.getValue();
            ServerLevel level = server.getLevel(hit.dimension());
            if (level == null) {
                iter.remove();
                continue;
            }
            Entity target = level.getEntity(entry.getKey());
            if (target != null && target.isAlive()) {
                if (level.getGameTime() % 5 == 0) {
                    action.accept(target, level);
                }
            }
            if (level.getGameTime() > hit.time() + lifetime) {
                iter.remove();
            }
        }
    }
}
