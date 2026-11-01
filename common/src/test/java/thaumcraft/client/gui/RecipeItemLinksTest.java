package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RecipeItemLinksTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void ingredientHitboxesUseTheActualTranslatedScaledGuiPose() {
        var pose = new PoseStack();
        pose.translate(10, 20, 0);
        pose.scale(.5F, .5F, 1);
        var stack = new ItemStack(Items.IRON_INGOT, 3);
        try (var links = RecipeItemLinks.capture()) {
            RecipeItemLinks.record(pose, stack, 16, 32);
            var hitbox = links.items().getFirst();
            assertEquals(18, hitbox.left()); assertEquals(36, hitbox.top());
            assertEquals(26, hitbox.right()); assertEquals(44, hitbox.bottom());
            assertTrue(hitbox.contains(22, 40));
            assertFalse(hitbox.contains(26, 40));
            assertFalse(hitbox.contains(22, 44));
            stack.setCount(1);
            assertEquals(3, hitbox.stack().getCount(), "the link describes the actually drawn item snapshot");
        }
    }

    @Test void nestedCapturesAndSubsequentFramesNeverReuseAnOldIngredient() {
        var pose = new PoseStack();
        try (var outer = RecipeItemLinks.capture()) {
            RecipeItemLinks.record(pose, new ItemStack(Items.IRON_INGOT), 0, 0);
            try (var inner = RecipeItemLinks.capture()) {
                RecipeItemLinks.record(pose, new ItemStack(Items.GOLD_INGOT), 20, 0);
                assertEquals(1, inner.items().size());
            }
            RecipeItemLinks.record(pose, new ItemStack(Items.QUARTZ), 40, 0);
            assertEquals(2, outer.items().size());
        }
        RecipeItemLinks.record(pose, new ItemStack(Items.DIAMOND), 60, 0);
        try (var next = RecipeItemLinks.capture()) { assertTrue(next.items().isEmpty()); }
    }
}
