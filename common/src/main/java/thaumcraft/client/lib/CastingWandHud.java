package thaumcraft.client.lib;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.Config;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandHands;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusTrade;
import thaumcraft.common.items.wands.foci.WandFocusActions;

/** Original ClientTickEventsFML:397-513,618-659: wand dial and its focus/trade readout. */
public final class CastingWandHud {
    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/hud.png");
    private static final Samples SAMPLES = new Samples();
    private static final DecimalFormat VIS_FORMAT = new DecimalFormat("#######.##");
    private static final DecimalFormat SECONDS_FORMAT = new DecimalFormat("#######.#");

    private CastingWandHud() {}

    public static void render(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        SAMPLES.owner(mc.level, mc.player);
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        var hand = WandHands.select(mc.player);
        if (hand == null) return;
        var stack = mc.player.getItemInHand(hand);
        var data = ItemWandCasting.data(stack);
        var previous = SAMPLES.previous(hand, hand == InteractionHand.MAIN_HAND ? mc.player.getInventory().selected : 40,
                data, ItemWandCasting.isSceptre(stack), System.currentTimeMillis());
        var focusStack = FocusData.get(stack);
        AspectList costs = focusStack.getItem() instanceof ItemFocusBasic focus ? focus.getVisCost(focusStack) : null;
        var quoted = new LinkedHashMap<String, Float>();
        if (costs != null) for (Aspect aspect : costs.getAspects()) {
            if (aspect != null && costs.getAmount(aspect) > 0)
                // The original UI shows the fractional quotation, before centivis truncation by payment.
                quoted.put(aspect.getTag(), costs.getAmount(aspect) * WandVisDiscount.focusModifier(stack, mc.player, aspect) / 100F);
        }
        ItemStack picked = pickedBlock(stack, focusStack);
        int count = 0;
        if (!picked.isEmpty()) for (ItemStack carried : mc.player.getInventory().items) {
            // Original isItemEqual: item+metadata, not name, count or other NBT.
            if (carried.is(picked.getItem()) && carried.getDamageValue() == picked.getDamageValue()) count += carried.getCount();
        }
        paint(new Frame(data, previous, ItemWandCasting.getMaxVis(stack), costs, quoted, focusStack, picked, count,
                WandFocusActions.cooldownMillis(mc.player), mc.player.isShiftKeyDown(), Config.dialBottom, graphics.guiHeight()),
                new NativeDraw(graphics, mc.font));
    }

    private static ItemStack pickedBlock(ItemStack wand, ItemStack focus) {
        if (!(focus.getItem() instanceof ItemFocusTrade)) return ItemStack.EMPTY;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return ItemStack.EMPTY;
        var tag = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("picked", Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        var state = NbtUtils.readBlockState(mc.level.registryAccess().lookupOrThrow(Registries.BLOCK), tag.getCompound("picked"));
        return state.isAir() ? ItemStack.EMPTY : new ItemStack(state.getBlock().asItem());
    }

    /** Drawing inputs come from the actual selected hand; no server state is mutated by the HUD. */
    record Frame(WandData data, WandData previous, int capacity, AspectList costs, Map<String, Float> quotedCosts,
            ItemStack focus, ItemStack picked, int materialCount, long cooldownMillis, boolean sneaking, boolean bottom, int height) {}

    /** Production drawing operations, observable without a GL context in regression tests. */
    interface Draw {
        void push(); void pop(); void translate(float x, float y, float z); void scale(float x, float y, float z); void rotate(float degrees);
        void blend(boolean enabled); void color(float r, float g, float b, float a);
        void blit(int x, int y, int u, int v, int width, int height);
        void item(ItemStack stack, int x, int y);
        void centered(String text, int x, int y); void text(String text, int x, int y, int color);
        int width(String text); int fontHeight();
    }

    static void paint(Frame frame, Draw draw) {
        draw.blend(true);
        draw.push();
        try {
            if (frame.bottom()) draw.translate(0, frame.height() - 32, 0);
            draw.push();
            try {
                draw.scale(.5F, .5F, .5F);
                draw.blit(0, 0, 0, 0, 64, 64);
            } finally { draw.pop(); }
            draw.translate(16, 16, 0);
            int count = 0;
            for (String tag : WandData.PRIMALS) {
                draw.blend(true);
                Aspect aspect = Aspect.getAspect(tag);
                int amount = frame.data().amount(tag);
                draw.push();
                try {
                    if (!frame.bottom()) draw.rotate(90);
                    draw.rotate(-15 + count * 24);
                    draw.translate(0, -32, 0);
                    draw.scale(.5F, .5F, .5F);
                    int loc = (int) (30F * amount / frame.capacity());
                    int color = aspect.getColor();
                    draw.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, .8F);
                    draw.blit(-4, 35 - loc, 104, 0, 8, loc);
                    draw.color(1, 1, 1, 1);
                    draw.blit(-8, -3, 72, 0, 16, 42);
                    int shift = 0;
                    if (frame.costs() != null && frame.costs().getAmount(aspect) > 0) {
                        draw.blit(-4, -8, 136, 0, 8, 8);
                        shift = 8;
                    }
                    int old = frame.previous().amount(tag);
                    if (old != amount) draw.blit(-4, -8 - shift, old > amount ? 128 : 120, 0, 8, 8);
                    if (frame.sneaking()) {
                        draw.rotate(-90);
                        draw.centered("" + amount / 100, -32, -4);
                        Float quote = frame.quotedCosts().get(tag);
                        if (quote != null) draw.centered(VIS_FORMAT.format(quote), 8, -4);
                    }
                } finally { draw.pop(); }
                count++;
            }
            if (!frame.focus().isEmpty()) {
                draw.push();
                try {
                    draw.translate(0, 0, 90);
                    draw.item(frame.picked().isEmpty() ? frame.focus() : frame.picked(), -8, -8);
                } finally { draw.pop(); }
                if (!frame.picked().isEmpty()) {
                    String amount = "" + frame.materialCount();
                    draw.push();
                    try {
                        draw.translate(0, -draw.fontHeight(), 500);
                        draw.scale(.5F, .5F, .5F);
                        int x = 16 - draw.width(amount);
                        draw.text(amount, x - 1, 24, 0xFF000000);
                        draw.text(amount, x + 1, 24, 0xFF000000);
                        draw.text(amount, x, 23, 0xFF000000);
                        draw.text(amount, x, 25, 0xFF000000);
                        draw.text(amount, x, 24, 0xFFFFFFFF);
                    } finally { draw.pop(); }
                } else if (frame.cooldownMillis() > 0) {
                    draw.push();
                    try {
                        draw.translate(0, 0, 150);
                        draw.scale(.5F, .5F, .5F);
                        draw.centered(SECONDS_FORMAT.format(frame.cooldownMillis() / 1000F) + "s", 0, -4);
                    } finally { draw.pop(); }
                }
            }
        } finally {
            draw.pop();
            draw.color(1, 1, 1, 1);
            draw.blend(false);
        }
    }

    /** Synced packets may replace a stack instance: hand/slot and wand material identify the dial source. */
    static final class Samples {
        private record Key(InteractionHand hand, int slot) {}
        private record Sample(WandData data, boolean sceptre, long expires) {}
        private final Map<Key, Sample> samples = new HashMap<>();
        private Object level, player;
        void owner(Object level, Object player) {
            if (this.level != level || this.player != player) { samples.clear(); this.level = level; this.player = player; }
        }
        WandData previous(InteractionHand hand, int slot, WandData data, boolean sceptre, long now) {
            Key key = new Key(hand, slot);
            Sample previous = samples.get(key);
            if (previous == null || previous.expires() <= now || previous.sceptre() != sceptre
                    || !previous.data().rod().equals(data.rod()) || !previous.data().cap().equals(data.cap())) {
                samples.put(key, new Sample(data, sceptre, now + 1000));
                return data;
            }
            return previous.data();
        }
    }

    private record NativeDraw(GuiGraphics graphics, Font font) implements Draw {
        public void push() { graphics.pose().pushPose(); }
        public void pop() { graphics.pose().popPose(); }
        public void translate(float x, float y, float z) { graphics.pose().translate(x, y, z); }
        public void scale(float x, float y, float z) { graphics.pose().scale(x, y, z); }
        public void rotate(float degrees) { graphics.pose().mulPose(Axis.ZP.rotationDegrees(degrees)); }
        public void blend(boolean enabled) { if (enabled) { RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); } else RenderSystem.disableBlend(); }
        public void color(float r, float g, float b, float a) { graphics.setColor(r, g, b, a); }
        public void blit(int x, int y, int u, int v, int width, int height) { graphics.blit(HUD, x, y, u, v, width, height, 256, 256); }
        public void item(ItemStack stack, int x, int y) { graphics.renderItem(stack, x, y); }
        public void centered(String text, int x, int y) { graphics.drawCenteredString(font, text, x, y, 0xFFFFFFFF); }
        public void text(String text, int x, int y, int color) { graphics.drawString(font, text, x, y, color, false); }
        public int width(String text) { return font.width(text); }
        public int fontHeight() { return font.lineHeight; }
    }
}
