/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.client.core.handler;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import thaumic.tinkerer.common.enchantment.EnchantmentFormulas;
import thaumic.tinkerer.common.enchantment.ModEnchantments;

/**
 * Ascent Boost, the part that only the client can do: {@code onPlayerJump} of 1.7.10 multiplied the vertical motion of a
 * jump by {@code (boost + 2) / 2}. A player's jump is simulated on its client (the server only checks the positions the
 * client reports), and 1.21.1 has no event for it, so the client looks at the tick of its own player: a jump is a tick that
 * began on the ground, with the jump key held, and ended in the air moving up.
 *
 * <p>The jump of the tick was made with the base speed, so the rest of it is added to the motion that is left, and the way
 * that the boosted jump would have gone in that first tick is moved at once. The result is the path of the multiplied jump:
 * with {@code v0} the speed of the jump, the motion after the tick is {@code (v0 - 0.08) * 0.98}, and the boosted one
 * {@code (k * v0 - 0.08) * 0.98}.</p>
 */
public final class EnchantmentClientHandler {
    private static final double GRAVITY = 0.08D;
    private static final double DRAG = 0.98D;

    private static boolean wasOnGround;

    private EnchantmentClientHandler() {}

    public static void register() {
        TickEvent.PLAYER_PRE.register(EnchantmentClientHandler::beforeTick);
        TickEvent.PLAYER_POST.register(EnchantmentClientHandler::afterTick);
    }

    private static void beforeTick(Player player) {
        if (player.isLocalPlayer()) wasOnGround = player.onGround();
    }

    private static void afterTick(Player player) {
        if (!player.isLocalPlayer() || !(player instanceof LocalPlayer local) || Minecraft.getInstance().player != player) return;
        if (!wasOnGround || player.onGround() || !local.input.jumping) return;
        if (player.isShiftKeyDown() || player.isPassenger() || player.isInWater() || player.isInLava() || player.onClimbable()
                || player.getAbilities().flying) {
            return;
        }

        int boost = ModEnchantments.maxArmorLevel(player, ModEnchantments.ascentBoost);
        if (boost < 1) return;

        Vec3 motion = player.getDeltaMovement();
        if (motion.y <= 0) return;

        double factor = EnchantmentFormulas.ascentBoostFactor(boost);
        double jumpSpeed = motion.y / DRAG + GRAVITY;
        double boosted = (factor * jumpSpeed - GRAVITY) * DRAG;
        player.setDeltaMovement(motion.x, boosted, motion.z);
        // The first tick of the jump went (jumpSpeed) high; the boosted jump goes (factor * jumpSpeed) high in it.
        player.move(MoverType.SELF, new Vec3(0.0D, (factor - 1D) * jumpSpeed, 0.0D));
        wasOnGround = false;
    }
}
