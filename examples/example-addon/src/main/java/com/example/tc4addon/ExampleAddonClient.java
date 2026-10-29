package com.example.tc4addon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.client.AspectRenderer;
import thaumcraft.api.client.ThaumcraftClientFX;

/**
 * The client half of the example add-on, the Fabric {@code client} entrypoint: it runs only on a client, so it may name
 * client classes. Two things a 1.7.10 add-on drew on the client: Thaumcraft's particles ({@code Thaumcraft.proxy.sparkle},
 * {@code blockRunes}, {@code beamCont}) and the tag of an aspect in a screen ({@code UtilsFX.drawTag}).
 */
public final class ExampleAddonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // [doc:client-fx]
        // While the amethyst core is in the hand: a sparkle at the hand every tick (drawn with the chance and the particle setting
        // of the original), a rune on the block under the feet twice a second, and a beam from the wand tip to the block looked
        // at while use is held. Every call takes the Level and draws only on a client level; a beam must be called every tick,
        // it ends a few ticks after the last call. Thaumcraft steps its beams (and counts the glint of a beam down) at the end of
        // the client tick, so the calls are made at the START of the tick, before that step, as the originals' item ticks were.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            var player = client.player;
            if (player == null || !player.getMainHandItem().is(ExampleAddon.AMETHYST_ROD)) return;
            var level = player.level();
            Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.3, 0);
            ThaumcraftClientFX.sparkle(level, hand.x, hand.y, hand.z, 1.0F, 2, 0.0F);
            if (player.tickCount % 10 == 0) {
                // The position of blockRunes is a block corner: the rune appears half a block from it on every axis, so y + 0.7
                // puts it 1.2 above the bottom of the block under the feet, just over its top face.
                var feet = player.blockPosition().below();
                ThaumcraftClientFX.blockRunes(level, feet.getX(), feet.getY() + 0.7, feet.getZ(), 0.6F, 0.2F, 0.9F, 20, -0.02F);
            }
            if (client.options.keyUse.isDown() && player.pick(8, 1.0F, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                ThaumcraftClientFX.beam(level, player, hit.getLocation(), 0x9933FF, 2, false, 1.0F, 5);
            }
        });
        // [/doc]

        // [doc:aspect-tags]
        // The aspects of the item in the main hand, as tags along the top left of the screen: sixteen pixels each, the amount
        // in the bottom right corner of the tag (AspectRenderer.draw takes the position of the top left corner).
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null || client.options.hideGui) return;
            AspectList aspects = ThaumcraftApiHelper.getObjectAspects(client.player.getMainHandItem(), client.level);
            int x = 8;
            for (Aspect aspect : aspects.getAspectsSorted()) {
                AspectRenderer.draw(graphics, aspect, x, 8, aspects.getAmount(aspect), 0);
                x += AspectRenderer.TAG_SIZE + 2;
            }
        });
        // [/doc]
    }
}
